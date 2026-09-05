/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.physics.movement.CharacterControllerSettings;
import io.github.glynch.jscene3d.physics.movement.CharacterMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveSettings;
import io.github.glynch.jscene3d.physics.movement.OverlapPhase;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class CharacterControllerTest {
    private static final Quaternionf IDENTITY = new Quaternionf();
    private static final float FIXED_SECONDS = 1.0F / 120.0F;
    private static final Offset<Float> TOLERANCE = Offset.offset(5.0E-3F);

    @Test
    void appliesGravityAndRetainsWalkableGround() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterController controller = groundedController(world);

        CharacterMoveResult result = controller.move(new Vector3f(), FIXED_SECONDS);

        assertThat(result.isGrounded()).isTrue();
        assertThat(controller.isGrounded()).isTrue();
        assertThat(result.groundNormal(new Vector3f()).y).isCloseTo(1.0F, TOLERANCE);
        assertThat(controller.verticalVelocity(new Vector3f()).lengthSquared()).isZero();
    }

    @Test
    void projectsVerticalCallerVelocityOntoTheMovementPlane() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterControllerSettings settings = CharacterControllerSettings.DEFAULT.withGravity(0.0F);
        KinematicBody body = addCharacterBody(world, new Vector3f());
        CharacterController controller = new CharacterController(world, body, settings);

        CharacterMoveResult result = controller.move(new Vector3f(2.0F, 100.0F, -3.0F), 0.5F);

        assertVector(result.requestedTranslation(new Vector3f()), 1.0F, 0.0F, -1.5F);
        assertVector(body.position(new Vector3f()), 1.0F, 0.0F, -1.5F);
    }

    @Test
    void slidesAlongWalls() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterControllerSettings settings = CharacterControllerSettings.DEFAULT
                .withGravity(0.0F)
                .withMovementSettings(KinematicMoveSettings.DEFAULT
                        .withMaximumStepHeight(0.0F)
                        .withGroundSnapDistance(0.0F));
        KinematicBody body = addCharacterBody(world, new Vector3f());
        CharacterController controller = new CharacterController(world, body, settings);
        addStaticBox(world, new Vector3f(2.0F, 0.0F, 0.0F), new Vector3f(1.0F, 5.0F, 10.0F));

        CharacterMoveResult result = controller.move(new Vector3f(4.0F, 0.0F, 3.0F), 1.0F);

        assertThat(body.position(new Vector3f()).x).isCloseTo(0.999F, TOLERANCE);
        assertThat(body.position(new Vector3f()).z).isCloseTo(3.0F, TOLERANCE);
        assertThat(result.contacts()).isNotEmpty();
    }

    @Test
    void traversesAReachableStepInBothDirectionsAcrossRepeatedFixedUpdates() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addCharacterBody(world, new Vector3f(-4.0F, 0.951F, 0.0F));
        addStaticBox(world, new Vector3f(2.0F, -0.5F, 0.0F), new Vector3f(16.0F, 1.0F, 12.0F));
        addStaticBox(world, new Vector3f(0.5F, 0.2F, 0.0F), new Vector3f(2.0F, 0.4F, 3.0F));
        CharacterController controller = new CharacterController(world, body);
        boolean stepped = false;

        for (int update = 0; update < 240; update++) {
            CharacterMoveResult result = controller.move(new Vector3f(4.0F, 0.0F, 0.0F), FIXED_SECONDS);
            stepped |= result.stepped();
        }

        assertThat(stepped).isTrue();
        assertThat(body.position(new Vector3f()).x).isGreaterThan(2.0F);

        for (int update = 0; update < 240; update++) {
            controller.move(new Vector3f(-4.0F, 0.0F, 0.0F), FIXED_SECONDS);
        }

        assertThat(body.position(new Vector3f()).x).isLessThan(-2.0F);
    }

    @Test
    void traversesAReachableTriangleMeshStepInBothDirections() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addCharacterBody(world, new Vector3f(-4.0F, 1.001F, 0.0F));
        addStaticTriangleStep(world, 0.5F);
        CharacterController controller = new CharacterController(world, body);

        for (int update = 0; update < 240; update++) {
            controller.move(new Vector3f(4.0F, 0.0F, 0.0F), FIXED_SECONDS);
        }
        assertThat(body.position(new Vector3f()).x).isGreaterThan(2.0F);

        for (int update = 0; update < 240; update++) {
            controller.move(new Vector3f(-4.0F, 0.0F, 0.0F), FIXED_SECONDS);
        }

        assertThat(body.position(new Vector3f()).x).isLessThan(-2.0F);
    }

    @Test
    void doesNotClimbATriangleMeshLedgeAboveTheMaximumStepHeight() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addCharacterBody(world, new Vector3f(-4.0F, 1.001F, 0.0F));
        addStaticTriangleStep(world, 1.0F);
        CharacterController controller = new CharacterController(world, body);

        for (int update = 0; update < 240; update++) {
            controller.move(new Vector3f(4.0F, 0.0F, 0.0F), FIXED_SECONDS);
        }

        assertThat(body.position(new Vector3f()).x).isLessThan(-0.49F);
        assertThat(body.position(new Vector3f()).y).isCloseTo(1.001F, TOLERANCE);
    }

    @Test
    void descendsAnOrientedTriangleMeshStepWithoutHittingItsRiser() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addCharacterBody(world, new Vector3f(-4.0F, 1.501F, 0.0F));
        addStaticDescendingTriangleStep(world, 0.5F);
        CharacterController controller = new CharacterController(world, body);

        for (int update = 0; update < 240; update++) {
            controller.move(new Vector3f(4.0F, 0.0F, 0.0F), FIXED_SECONDS);
        }

        assertThat(body.position(new Vector3f()).x).isGreaterThan(2.0F);
        assertThat(body.position(new Vector3f()).y).isCloseTo(1.001F, TOLERANCE);
    }

    @Test
    void doesNotClimbAnOrientedTriangleMeshLedgeAboveTheMaximumStepHeight() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addCharacterBody(world, new Vector3f(4.0F, 1.001F, 3.9F));
        addStaticDescendingTriangleStep(world, 1.75F);
        CharacterController controller = new CharacterController(world, body);

        for (int update = 0; update < 240; update++) {
            controller.move(new Vector3f(-4.0F, 0.0F, 0.0F), FIXED_SECONDS);
        }

        assertThat(body.position(new Vector3f()).x).isGreaterThan(0.49F);
        assertThat(body.position(new Vector3f()).y).isCloseTo(1.001F, TOLERANCE);
    }

    @Test
    void traversesAMeshPortalWithDifferentFloorAndCeilingHeightsInBothDirections() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addDoomCharacterBody(world, new Vector3f(-3.0F, 0.876F, 6.0F));
        addStaticMapPortal(world);
        KinematicMoveSettings movement =
                KinematicMoveSettings.DEFAULT.withMaximumStepHeight(0.75F).withGroundSnapDistance(0.25F);
        CharacterControllerSettings settings = CharacterControllerSettings.DEFAULT
                .withMovementSettings(movement)
                .withJumpSpeed(0.0F);
        CharacterController controller = new CharacterController(world, body, settings);

        for (int update = 0; update < 30; update++) {
            controller.move(new Vector3f(8.0F, 0.0F, 0.0F), 1.0F / 60.0F);
        }

        assertThat(body.position(new Vector3f()).x).isGreaterThan(0.0F);
        assertThat(body.position(new Vector3f()).y).isCloseTo(0.376F, TOLERANCE);

        for (int update = 0; update < 30; update++) {
            controller.move(new Vector3f(-8.0F, 0.0F, 0.0F), 1.0F / 60.0F);
        }

        assertThat(body.position(new Vector3f()).x).isLessThan(-2.0F);
        assertThat(body.position(new Vector3f()).y).isCloseTo(0.876F, TOLERANCE);
    }

    @Test
    void recognizesWalkableSlopesUsingTheConfiguredLimit() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addCharacterBody(world, new Vector3f(0.0F, 3.0F, 0.0F));
        Quaternionf slopeOrientation = new Quaternionf().rotateZ((float) Math.toRadians(30.0));
        world.addStaticBody(new Vector3f(), slopeOrientation).addCollider(new BoxShape(10.0F, 1.0F, 10.0F));
        CharacterController controller = new CharacterController(world, body);

        CharacterMoveResult result = controller.move(new Vector3f(), 0.5F);

        assertThat(result.isGrounded()).isTrue();
        assertThat(result.groundNormal(new Vector3f()).y).isGreaterThan(0.8F);
    }

    @Test
    void jumpsOnlyFromWalkableGround() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterController controller = groundedController(world);
        controller.move(new Vector3f(), FIXED_SECONDS);

        boolean accepted = controller.tryJump();
        CharacterMoveResult result = controller.move(new Vector3f(), FIXED_SECONDS);

        assertThat(accepted).isTrue();
        assertThat(result.jumped()).isTrue();
        assertThat(result.isGrounded()).isFalse();
        assertThat(result.appliedTranslation(new Vector3f()).y).isPositive();
        assertThat(controller.tryJump()).isFalse();
    }

    @Test
    void preservesCollisionSensorEvents() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = addCharacterBody(world, new Vector3f());
        CharacterControllerSettings settings = CharacterControllerSettings.DEFAULT.withGravity(0.0F);
        CharacterController controller = new CharacterController(world, body, settings);
        CollisionSensor sensor = world.addCollisionSensor(new Vector3f(2.0F, 0.0F, 0.0F), IDENTITY);
        sensor.addCollider(new BoxShape(2.0F, 4.0F, 4.0F));

        CharacterMoveResult result = controller.move(new Vector3f(2.0F, 0.0F, 0.0F), 1.0F);

        assertThat(result.overlapEvents()).singleElement().satisfies(event -> {
            assertThat(event.sensor()).isSameAs(sensor);
            assertThat(event.phase()).isEqualTo(OverlapPhase.ENTER);
        });
    }

    @Test
    void teleportClearsMovementState() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterController controller = groundedController(world);
        controller.move(new Vector3f(), FIXED_SECONDS);
        controller.tryJump();
        Vector3f destination = new Vector3f(5.0F, 8.0F, -2.0F);
        Quaternionf orientation = new Quaternionf().rotateY(0.5F);

        controller.teleport(destination, orientation);

        assertVector(controller.body().position(new Vector3f()), 5.0F, 8.0F, -2.0F);
        assertThat(controller.isGrounded()).isFalse();
        assertThat(controller.verticalVelocity(new Vector3f()).lengthSquared()).isZero();
        assertThat(controller.move(new Vector3f(), FIXED_SECONDS).jumped()).isFalse();
    }

    @Test
    void rejectsForeignBodiesAndInvalidUpdateArguments() {
        PhysicsWorld world = new PhysicsWorld();
        PhysicsWorld otherWorld = new PhysicsWorld();
        KinematicBody foreignBody = otherWorld.addKinematicBody();
        CharacterControllerSettings settings = CharacterControllerSettings.DEFAULT;
        KinematicBody body = addCharacterBody(world, new Vector3f());
        CharacterController controller = new CharacterController(world, body);
        Vector3f invalidVelocity = new Vector3f(Float.NaN, 0.0F, 0.0F);
        Vector3f zeroVelocity = new Vector3f();

        assertThatThrownBy(() -> new CharacterController(world, foreignBody, settings))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.move(invalidVelocity, FIXED_SECONDS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.move(zeroVelocity, 0.0F)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void settingsAreImmutableValues() {
        KinematicMoveSettings movement = KinematicMoveSettings.DEFAULT.withMaximumStepHeight(0.25F);
        CharacterControllerSettings first = CharacterControllerSettings.DEFAULT
                .withMovementSettings(movement)
                .withGravity(12.0F)
                .withJumpSpeed(5.0F);
        CharacterControllerSettings second = CharacterControllerSettings.DEFAULT
                .withMovementSettings(movement)
                .withGravity(12.0F)
                .withJumpSpeed(5.0F);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("gravity=12.0", "jumpSpeed=5.0");
        assertThat(first).isNotEqualTo(first.withGravity(13.0F));
    }

    private static CharacterController groundedController(PhysicsWorld world) {
        KinematicBody body = addCharacterBody(world, new Vector3f(0.0F, 1.001F, 0.0F));
        addStaticBox(world, new Vector3f(0.0F, -0.5F, 0.0F), new Vector3f(20.0F, 1.0F, 20.0F));
        return new CharacterController(world, body);
    }

    private static KinematicBody addCharacterBody(PhysicsWorld world, Vector3f position) {
        KinematicBody body = world.addKinematicBody(position, IDENTITY);
        body.addCollider(new CapsuleShape(0.5F, 1.0F));
        return body;
    }

    private static KinematicBody addDoomCharacterBody(PhysicsWorld world, Vector3f position) {
        KinematicBody body = world.addKinematicBody(position, IDENTITY);
        body.addCollider(new CapsuleShape(0.5F, 0.75F));
        return body;
    }

    private static void addStaticBox(PhysicsWorld world, Vector3f position, Vector3f dimensions) {
        world.addStaticBody(position, IDENTITY).addCollider(new BoxShape(dimensions.x, dimensions.y, dimensions.z));
    }

    private static void addStaticTriangleStep(PhysicsWorld world, float height) {
        float[] positions = {
            -8.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 4.0F, -8.0F, 0.0F, 4.0F, 0.0F, height, -4.0F, 8.0F,
            height, -4.0F, 8.0F, height, 4.0F, 0.0F, height, 4.0F, 0.0F, 0.0F, -4.0F, 0.0F, height, -4.0F, 0.0F, height,
            4.0F, 0.0F, 0.0F, 4.0F
        };
        int[] indices = {0, 2, 1, 0, 3, 2, 4, 6, 5, 4, 7, 6, 8, 9, 10, 8, 10, 11};
        world.addStaticBody().addCollider(new TriangleMeshShape(positions, indices));
    }

    private static void addStaticDescendingTriangleStep(PhysicsWorld world, float height) {
        float[] positions = {
            -8.0F, height, -4.0F, 0.0F, height, -4.0F, 0.0F, height, 4.0F, -8.0F, height, 4.0F, 0.0F, 0.0F, -4.0F, 8.0F,
            0.0F, -4.0F, 8.0F, 0.0F, 4.0F, 0.0F, 0.0F, 4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 4.0F, 0.0F, height, 4.0F,
            0.0F, height, -4.0F
        };
        int[] indices = {0, 2, 1, 0, 3, 2, 4, 6, 5, 4, 7, 6, 8, 9, 10, 8, 10, 11};
        world.addStaticBody().addCollider(new TriangleMeshShape(positions, indices));
    }

    private static void addStaticMapPortal(PhysicsWorld world) {
        float[] positions = {
            -3.75F, 0.0F, 10.0F, -1.0F, 0.0F, 10.0F, -1.0F, 0.0F, 0.0F, -3.75F, 0.0F, 0.0F,
            -3.75F, 4.0F, 10.0F, -1.0F, 4.0F, 10.0F, -1.0F, 4.0F, 0.0F, -3.75F, 4.0F, 0.0F,
            -1.0F, -0.5F, 8.0F, 2.0F, -0.5F, 8.0F, 2.0F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F,
            -1.0F, 2.0F, 8.0F, 2.0F, 2.0F, 8.0F, 2.0F, 2.0F, 0.0F, -1.0F, 2.0F, 0.0F,
            -1.0F, -0.5F, 8.0F, -1.0F, -0.5F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 8.0F,
            -1.0F, 2.0F, 8.0F, -1.0F, 2.0F, 0.0F, -1.0F, 4.0F, 0.0F, -1.0F, 4.0F, 8.0F
        };
        int[] indices = {
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        };
        world.addStaticBody().addCollider(new TriangleMeshShape(positions, indices));
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertThat(actual.x).isCloseTo(x, TOLERANCE);
        assertThat(actual.y).isCloseTo(y, TOLERANCE);
        assertThat(actual.z).isCloseTo(z, TOLERANCE);
    }
}
