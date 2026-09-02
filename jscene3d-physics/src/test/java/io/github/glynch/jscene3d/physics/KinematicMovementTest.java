/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.physics.movement.KinematicContact;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveSettings;
import io.github.glynch.jscene3d.physics.movement.OverlapPhase;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.assertj.core.groups.Tuple;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class KinematicMovementTest {
    private static final Quaternionf IDENTITY = new Quaternionf();
    private static final float TOLERANCE_VALUE = 5.0E-3F;
    private static final Offset<Float> TOLERANCE = Offset.offset(TOLERANCE_VALUE);
    private static final KinematicMoveSettings NO_STEP =
            KinematicMoveSettings.DEFAULT.withMaximumStepHeight(0.0F).withGroundSnapDistance(0.0F);

    @Test
    void appliesUnobstructedTranslationAndUpdatesTheBodyAndCollider() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f());
        Collider collider = mover.colliders().getFirst();

        KinematicMoveResult result = world.move(mover, new Vector3f(2.0F, 1.0F, -3.0F), NO_STEP);

        assertVector(result.appliedTranslation(new Vector3f()), 2.0F, 1.0F, -3.0F);
        assertVector(result.remainingTranslation(new Vector3f()), 0.0F, 0.0F, 0.0F);
        assertVector(mover.position(new Vector3f()), 2.0F, 1.0F, -3.0F);
        assertVector(collider.position(new Vector3f()), 2.0F, 1.0F, -3.0F);
        assertThat(result.contacts()).isEmpty();
    }

    @Test
    void stopsAtWallAndSlidesAlongIt() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f());
        Collider wall = addStatic(world, new BoxShape(1.0F, 5.0F, 10.0F), new Vector3f(2.0F, 0.0F, 0.0F));

        KinematicMoveResult result = world.move(mover, new Vector3f(4.0F, 0.0F, 3.0F), NO_STEP);

        Vector3f position = mover.position(new Vector3f());
        assertThat(position.x).isCloseTo(0.999F, TOLERANCE);
        assertThat(position.z).isCloseTo(3.0F, TOLERANCE);
        assertThat(result.contacts()).extracting(contact -> contact.collider()).contains(wall);
    }

    @Test
    void resolvesEveryColliderOnACompoundKinematicBody() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = world.addKinematicBody();
        mover.addCollider(new CapsuleShape(0.5F, 1.0F), new Vector3f(0.0F, 5.0F, 0.0F), IDENTITY);
        mover.addCollider(new CapsuleShape(0.5F, 1.0F));
        Collider wall = addStatic(world, new BoxShape(1.0F, 4.0F, 4.0F), new Vector3f(2.0F, 0.0F, 0.0F));

        KinematicMoveResult result = world.move(mover, new Vector3f(4.0F, 0.0F, 0.0F), NO_STEP);

        assertThat(mover.position(new Vector3f()).x).isCloseTo(0.999F, TOLERANCE);
        assertThat(result.contacts()).extracting(KinematicContact::collider).contains(wall);
    }

    @Test
    void detectsGroundAndKeepsConfiguredSkinWidth() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f(0.0F, 3.0F, 0.0F));
        Collider floor = addStatic(world, new BoxShape(20.0F, 1.0F, 20.0F), new Vector3f(0.0F, -0.5F, 0.0F));

        KinematicMoveResult result = world.move(mover, new Vector3f(0.0F, -5.0F, 0.0F), NO_STEP);

        assertThat(mover.position(new Vector3f()).y).isCloseTo(1.001F, TOLERANCE);
        assertThat(result.isGrounded()).isTrue();
        assertThat(result.groundNormal(new Vector3f()).y).isCloseTo(1.0F, TOLERANCE);
        assertThat(result.contacts()).extracting(contact -> contact.collider()).contains(floor);
    }

    @Test
    void preservesHorizontalMovementWhenGravityPressesAGroundedCapsuleIntoTheFloor() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f(-4.0F, 0.951F, 0.0F));
        addStatic(world, new BoxShape(16.0F, 1.0F, 12.0F), new Vector3f(2.0F, -0.5F, 0.0F));
        Vector3f fixedStepMovement = new Vector3f(-0.033333F, -0.00125F, 0.0F);

        List<KinematicMoveResult> results = IntStream.range(0, 10)
                .mapToObj(ignored -> world.move(mover, fixedStepMovement))
                .toList();

        assertThat(results)
                .allSatisfy(result -> assertThat(result.appliedTranslation(new Vector3f()).x)
                        .isCloseTo(fixedStepMovement.x, TOLERANCE));
        assertThat(mover.position(new Vector3f()).x).isCloseTo(-4.33333F, TOLERANCE);
        assertThat(mover.position(new Vector3f()).y).isCloseTo(0.951F, TOLERANCE);
    }

    @Test
    void traversesOnlyStepsWithinConfiguredHeight() {
        StepScenario low = stepWorld(0.4F);
        KinematicMoveResult lowResult = low.world().move(low.mover(), new Vector3f(3.0F, 0.0F, 0.0F));

        StepScenario tall = stepWorld(0.8F);
        KinematicMoveResult tallResult = tall.world().move(tall.mover(), new Vector3f(3.0F, 0.0F, 0.0F));

        assertThat(lowResult.stepped()).isTrue();
        assertThat(low.mover().position(new Vector3f()).x).isGreaterThan(2.0F);
        assertThat(low.mover().position(new Vector3f()).y).isCloseTo(1.401F, TOLERANCE);
        assertThat(tallResult.stepped()).isFalse();
        assertThat(tall.mover().position(new Vector3f()).x).isLessThan(0.1F);
    }

    @ParameterizedTest
    @ValueSource(ints = {60, 120, 240})
    void crossesAReachableStepUsingRepeatedFixedStepMovementAndGravity(int updatesPerSecond) {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f(-4.0F, 0.951F, 0.0F));
        addStatic(world, new BoxShape(16.0F, 1.0F, 12.0F), new Vector3f(2.0F, -0.5F, 0.0F));
        addStatic(world, new BoxShape(2.0F, 0.4F, 3.0F), new Vector3f(0.5F, 0.2F, 0.0F));
        float fixedSeconds = 1.0F / updatesPerSecond;
        float verticalVelocity = 0.0F;
        boolean stepped = false;

        for (int step = 0; step < updatesPerSecond * 2; step++) {
            verticalVelocity -= 18.0F * fixedSeconds;
            Vector3f requested = new Vector3f(4.0F * fixedSeconds, verticalVelocity * fixedSeconds, 0.0F);
            KinematicMoveResult result = world.move(mover, requested);
            stepped |= result.stepped();
            assertThat(result.appliedTranslation(new Vector3f()).x).isLessThanOrEqualTo(requested.x + TOLERANCE_VALUE);
            if (result.isGrounded() && verticalVelocity < 0.0F) {
                verticalVelocity = 0.0F;
            }
        }

        assertThat(stepped).isTrue();
        assertThat(mover.position(new Vector3f()).x).isGreaterThan(2.0F);
    }

    @Test
    void respectsMutualCollisionFilters() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f());
        mover.colliders().getFirst().setCollisionFilter(new CollisionFilter(1, 1));
        Collider ignored = addStatic(world, new BoxShape(1.0F, 4.0F, 4.0F), new Vector3f(2.0F, 0.0F, 0.0F));
        ignored.setCollisionFilter(new CollisionFilter(2, -1));

        KinematicMoveResult result = world.move(mover, new Vector3f(4.0F, 0.0F, 0.0F), NO_STEP);

        assertThat(mover.position(new Vector3f()).x).isCloseTo(4.0F, TOLERANCE);
        assertThat(result.contacts()).isEmpty();
    }

    @Test
    void reportsDeterministicOverlapLifecycleWithoutBlockingMovement() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f());
        CollisionSensor firstSensor = sensor(world, 2.0F);
        CollisionSensor secondSensor = sensor(world, 2.0F);

        KinematicMoveResult entered = world.move(mover, new Vector3f(2.0F, 0.0F, 0.0F), NO_STEP);
        KinematicMoveResult stayed = world.move(mover, new Vector3f(), NO_STEP);
        KinematicMoveResult exited = world.move(mover, new Vector3f(3.0F, 0.0F, 0.0F), NO_STEP);

        assertThat(entered.overlapEvents())
                .extracting(event -> event.sensor(), event -> event.phase())
                .containsExactly(
                        Tuple.tuple(firstSensor, OverlapPhase.ENTER), Tuple.tuple(secondSensor, OverlapPhase.ENTER));
        assertThat(stayed.overlapEvents()).allMatch(event -> event.phase() == OverlapPhase.STAY);
        assertThat(exited.overlapEvents()).allMatch(event -> event.phase() == OverlapPhase.EXIT);
        assertThat(mover.position(new Vector3f()).x).isCloseTo(5.0F, TOLERANCE);
    }

    @Test
    void reportsOneSensorEventWhenCompoundObjectsHaveSeveralOverlappingColliders() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f());
        mover.addCollider(new CapsuleShape(0.25F, 0.5F), new Vector3f(0.25F, 0.0F, 0.0F), IDENTITY);
        CollisionSensor sensor = world.addCollisionSensor();
        sensor.addCollider(new BoxShape(3.0F, 3.0F, 3.0F));
        sensor.addCollider(new BoxShape(2.0F, 2.0F, 2.0F));

        KinematicMoveResult result = world.move(mover, new Vector3f(), NO_STEP);

        assertThat(result.overlapEvents()).singleElement().satisfies(event -> {
            assertThat(event.sensor()).isSameAs(sensor);
            assertThat(event.phase()).isEqualTo(OverlapPhase.ENTER);
        });
    }

    @Test
    void repeatedWorldConstructionProducesIdenticalResults() {
        List<Vector3f> positions = IntStream.range(0, 8)
                .mapToObj(ignored -> {
                    PhysicsWorld world = new PhysicsWorld();
                    KinematicBody mover = addMover(world, new Vector3f());
                    addStatic(world, new BoxShape(1.0F, 5.0F, 10.0F), new Vector3f(2.0F, 0.0F, 0.0F));
                    world.move(mover, new Vector3f(4.0F, 0.0F, 3.0F), NO_STEP);
                    return mover.position(new Vector3f());
                })
                .toList();

        assertThat(positions).allSatisfy(position -> assertVector(position, 0.999F, 0.0F, 3.0F));
    }

    private static StepScenario stepWorld(float stepHeight) {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody mover = addMover(world, new Vector3f(0.0F, 1.001F, 0.0F));
        addStatic(world, new BoxShape(20.0F, 1.0F, 20.0F), new Vector3f(0.0F, -0.5F, 0.0F));
        addStatic(world, new BoxShape(3.0F, stepHeight, 4.0F), new Vector3f(2.0F, stepHeight * 0.5F, 0.0F));
        return new StepScenario(world, mover);
    }

    private static KinematicBody addMover(PhysicsWorld world, Vector3f position) {
        KinematicBody mover = world.addKinematicBody(position, IDENTITY);
        mover.addCollider(new CapsuleShape(0.5F, 1.0F));
        return mover;
    }

    private static Collider addStatic(PhysicsWorld world, BoxShape shape, Vector3f position) {
        return world.addStaticBody(position, IDENTITY).addCollider(shape);
    }

    private static CollisionSensor sensor(PhysicsWorld world, float x) {
        CollisionSensor sensor = world.addCollisionSensor(new Vector3f(x, 0.0F, 0.0F), IDENTITY);
        sensor.addCollider(new BoxShape(2.0F, 4.0F, 4.0F));
        return sensor;
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertThat(actual.x).isCloseTo(x, TOLERANCE);
        assertThat(actual.y).isCloseTo(y, TOLERANCE);
        assertThat(actual.z).isCloseTo(z, TOLERANCE);
    }

    private record StepScenario(PhysicsWorld world, KinematicBody mover) {}
}
