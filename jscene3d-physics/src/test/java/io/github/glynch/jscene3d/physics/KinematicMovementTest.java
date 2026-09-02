/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.physics.movement.KinematicMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveSettings;
import io.github.glynch.jscene3d.physics.movement.TriggerEventType;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.assertj.core.groups.Tuple;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class KinematicMovementTest {
    private static final Quaternionf IDENTITY = new Quaternionf();
    private static final Offset<Float> TOLERANCE = Offset.offset(5.0E-3F);
    private static final KinematicMoveSettings NO_STEP =
            KinematicMoveSettings.DEFAULT.withMaximumStepHeight(0.0F).withGroundSnapDistance(0.0F);

    @Test
    void appliesUnobstructedTranslationAndUpdatesCollider() {
        PhysicsWorld world = new PhysicsWorld();
        Collider mover = world.addCollider(new CapsuleShape(0.5F, 1.0F));

        KinematicMoveResult result = world.move(mover, new Vector3f(2.0F, 1.0F, -3.0F), NO_STEP);

        assertVector(result.appliedTranslation(new Vector3f()), 2.0F, 1.0F, -3.0F);
        assertVector(result.remainingTranslation(new Vector3f()), 0.0F, 0.0F, 0.0F);
        assertVector(mover.position(new Vector3f()), 2.0F, 1.0F, -3.0F);
        assertThat(result.contacts()).isEmpty();
    }

    @Test
    void stopsAtWallAndSlidesAlongIt() {
        PhysicsWorld world = new PhysicsWorld();
        Collider mover = world.addCollider(new CapsuleShape(0.5F, 1.0F));
        Collider wall = world.addCollider(new BoxShape(1.0F, 5.0F, 10.0F), new Vector3f(2.0F, 0.0F, 0.0F), IDENTITY);

        KinematicMoveResult result = world.move(mover, new Vector3f(4.0F, 0.0F, 3.0F), NO_STEP);

        Vector3f position = mover.position(new Vector3f());
        assertThat(position.x).isCloseTo(0.999F, TOLERANCE);
        assertThat(position.z).isCloseTo(3.0F, TOLERANCE);
        assertThat(result.contacts()).extracting(contact -> contact.collider()).contains(wall);
    }

    @Test
    void detectsGroundAndKeepsConfiguredSkinWidth() {
        PhysicsWorld world = new PhysicsWorld();
        Collider mover = world.addCollider(new CapsuleShape(0.5F, 1.0F), new Vector3f(0.0F, 3.0F, 0.0F), IDENTITY);
        Collider floor = world.addCollider(new BoxShape(20.0F, 1.0F, 20.0F), new Vector3f(0.0F, -0.5F, 0.0F), IDENTITY);

        KinematicMoveResult result = world.move(mover, new Vector3f(0.0F, -5.0F, 0.0F), NO_STEP);

        assertThat(mover.position(new Vector3f()).y).isCloseTo(1.001F, TOLERANCE);
        assertThat(result.isGrounded()).isTrue();
        assertThat(result.groundNormal(new Vector3f()).y).isCloseTo(1.0F, TOLERANCE);
        assertThat(result.contacts()).extracting(contact -> contact.collider()).contains(floor);
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

    @Test
    void respectsMutualCollisionFilters() {
        PhysicsWorld world = new PhysicsWorld();
        Collider mover = world.addCollider(new CapsuleShape(0.5F, 1.0F));
        mover.setCollisionFilter(new CollisionFilter(1, 1));
        Collider ignored = world.addCollider(new BoxShape(1.0F, 4.0F, 4.0F), new Vector3f(2.0F, 0.0F, 0.0F), IDENTITY);
        ignored.setCollisionFilter(new CollisionFilter(2, -1));

        KinematicMoveResult result = world.move(mover, new Vector3f(4.0F, 0.0F, 0.0F), NO_STEP);

        assertThat(mover.position(new Vector3f()).x).isCloseTo(4.0F, TOLERANCE);
        assertThat(result.contacts()).isEmpty();
    }

    @Test
    void reportsDeterministicTriggerLifecycleWithoutBlockingMovement() {
        PhysicsWorld world = new PhysicsWorld();
        Collider mover = world.addCollider(new CapsuleShape(0.5F, 1.0F));
        Collider firstTrigger = trigger(world, 2.0F);
        Collider secondTrigger = trigger(world, 2.0F);

        KinematicMoveResult entered = world.move(mover, new Vector3f(2.0F, 0.0F, 0.0F), NO_STEP);
        KinematicMoveResult stayed = world.move(mover, new Vector3f(), NO_STEP);
        KinematicMoveResult exited = world.move(mover, new Vector3f(3.0F, 0.0F, 0.0F), NO_STEP);

        assertThat(entered.triggerEvents())
                .extracting(event -> event.trigger(), event -> event.type())
                .containsExactly(
                        Tuple.tuple(firstTrigger, TriggerEventType.ENTER),
                        Tuple.tuple(secondTrigger, TriggerEventType.ENTER));
        assertThat(stayed.triggerEvents()).allMatch(event -> event.type() == TriggerEventType.STAY);
        assertThat(exited.triggerEvents()).allMatch(event -> event.type() == TriggerEventType.EXIT);
        assertThat(mover.position(new Vector3f()).x).isCloseTo(5.0F, TOLERANCE);
    }

    @Test
    void repeatedWorldConstructionProducesIdenticalResults() {
        List<Vector3f> positions = IntStream.range(0, 8)
                .mapToObj(ignored -> {
                    PhysicsWorld world = new PhysicsWorld();
                    Collider mover = world.addCollider(new CapsuleShape(0.5F, 1.0F));
                    world.addCollider(new BoxShape(1.0F, 5.0F, 10.0F), new Vector3f(2.0F, 0.0F, 0.0F), IDENTITY);
                    world.move(mover, new Vector3f(4.0F, 0.0F, 3.0F), NO_STEP);
                    return mover.position(new Vector3f());
                })
                .toList();

        assertThat(positions).allSatisfy(position -> assertVector(position, 0.999F, 0.0F, 3.0F));
    }

    private static StepScenario stepWorld(float stepHeight) {
        PhysicsWorld world = new PhysicsWorld();
        Collider mover = world.addCollider(new CapsuleShape(0.5F, 1.0F), new Vector3f(0.0F, 1.001F, 0.0F), IDENTITY);
        world.addCollider(new BoxShape(20.0F, 1.0F, 20.0F), new Vector3f(0.0F, -0.5F, 0.0F), IDENTITY);
        world.addCollider(new BoxShape(3.0F, stepHeight, 4.0F), new Vector3f(2.0F, stepHeight * 0.5F, 0.0F), IDENTITY);
        return new StepScenario(world, mover);
    }

    private static Collider trigger(PhysicsWorld world, float x) {
        Collider trigger = world.addCollider(new BoxShape(2.0F, 4.0F, 4.0F), new Vector3f(x, 0.0F, 0.0F), IDENTITY);
        trigger.setTrigger(true);
        return trigger;
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertThat(actual.x).isCloseTo(x, TOLERANCE);
        assertThat(actual.y).isCloseTo(y, TOLERANCE);
        assertThat(actual.z).isCloseTo(z, TOLERANCE);
    }

    private record StepScenario(PhysicsWorld world, Collider mover) {}
}
