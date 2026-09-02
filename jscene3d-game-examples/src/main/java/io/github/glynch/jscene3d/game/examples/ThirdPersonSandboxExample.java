/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleFrame;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.game.FixedUpdate;
import io.github.glynch.jscene3d.game.FrameUpdate;
import io.github.glynch.jscene3d.game.GameApplication;
import io.github.glynch.jscene3d.game.GameLoopSettings;
import io.github.glynch.jscene3d.game.GameRuntime;
import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputCapture;
import io.github.glynch.jscene3d.game.input.InputMap;
import io.github.glynch.jscene3d.game.physics.CharacterMovementActions;
import io.github.glynch.jscene3d.game.physics.CharacterMovementController;
import io.github.glynch.jscene3d.game.physics.PhysicsBinding;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.physics.CharacterController;
import io.github.glynch.jscene3d.physics.KinematicBody;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.movement.CharacterMoveResult;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.scenes.Scene;
import java.time.Duration;
import java.util.Locale;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Demonstrates camera-relative character movement with an orbiting third-person camera. */
public final class ThirdPersonSandboxExample {
    /** Prevents instantiation of this example entry point. */
    private ThirdPersonSandboxExample() {
        throw new AssertionError("ThirdPersonSandboxExample cannot be instantiated");
    }

    /**
     * Opens the example until the window closes.
     *
     * <p>Use W A S D or the arrow keys to move, drag to orbit, scroll to zoom, and Space to jump.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Third-Person Game Sandbox", ThirdPersonSandboxExample::create);
    }

    /** Creates the shared hosted implementation used by browser and standalone launch modes. */
    static HostedExample create(ExampleContext context) {
        SandboxApplication application = new SandboxApplication(context);
        ControlPanel panel = createPanel(context, application);
        return new HostedGameExample(context, application, panel);
    }

    /** Builds live instructions and game-loop diagnostics. */
    private static ControlPanel createPanel(ExampleContext context, SandboxApplication application) {
        ControlPanel panel = new ControlPanel(context.window(), "Third-Person Game Sandbox");
        ControlPanel.Section controls = panel.addSection("Controls");
        controls.addText("move", () -> "W A S D / arrow keys");
        controls.addText("orbit camera", () -> "drag");
        controls.addText("zoom", () -> "scroll");
        controls.addText("jump", () -> "Space");
        controls.addButton("reset player and camera", application::reset);
        ControlPanel.Section runtime = panel.addSection("Runtime");
        runtime.addText("fixed tick", application::tickStatus);
        runtime.addText("fixed updates", application::fixedUpdateStatus);
        runtime.addText("interpolation", application::interpolationStatus);
        runtime.addText("position", application::positionStatus);
        runtime.addText("grounded", application::groundedStatus);
        runtime.addText("movement action", application::movementStatus);
        return panel;
    }

    /** Adapts the shared example host lifecycle to the reusable game runtime. */
    private static final class HostedGameExample implements HostedExample {
        private final ExampleContext context;
        private final SandboxApplication application;
        private final ControlPanel panel;
        private final GameRuntime runtime;

        private HostedGameExample(ExampleContext context, SandboxApplication application, ControlPanel panel) {
            this.context = context;
            this.application = application;
            this.panel = panel;
            runtime = new GameRuntime(application);
            runtime.start();
        }

        @Override
        public void resize() {
            application.resize();
        }

        @Override
        public void update(ExampleFrame frame) {
            panel.update();
            boolean pointerCaptured = frame.pointerCaptured() || panel.capturesPointer();
            application.updateCamera(frame.elapsedSeconds(), pointerCaptured);
            InputCapture capture = new InputCapture(frame.keyboardCaptured(), pointerCaptured);
            ActionSnapshot input = application.inputMap.sample(context.window().input(), capture);
            runtime.advance(elapsed(frame.elapsedSeconds()), input);
        }

        @Override
        public void render() {
            runtime.render();
            context.renderer().render(panel);
        }

        @Override
        public void renderThumbnail() {
            runtime.render();
        }

        @Override
        public void close() {
            runtime.close();
        }

        /** Converts the example host's finite seconds to nanosecond game-loop time. */
        private static Duration elapsed(float elapsedSeconds) {
            return Duration.ofNanos(Math.round(elapsedSeconds * 1_000_000_000.0));
        }
    }

    /** Owns the example's third-person world, rules, presentation, and resources. */
    private static final class SandboxApplication implements GameApplication {
        private static final InputAction MOVE_FORWARD = new InputAction("move-forward");
        private static final InputAction MOVE_BACKWARD = new InputAction("move-backward");
        private static final InputAction MOVE_LEFT = new InputAction("move-left");
        private static final InputAction MOVE_RIGHT = new InputAction("move-right");
        private static final InputAction JUMP = new InputAction("jump");
        private static final CharacterMovementActions MOVEMENT_ACTIONS =
                new CharacterMovementActions(MOVE_FORWARD, MOVE_BACKWARD, MOVE_LEFT, MOVE_RIGHT, JUMP);
        private static final float MOVE_SPEED = 4.5F;
        private static final float TARGET_HEIGHT = 0.75F;

        private final ExampleContext context;
        private final Scene scene = new Scene();
        private final PerspectiveCamera camera;
        private final OrbitControls orbitControls;
        private final PhysicsWorld physicsWorld = new PhysicsWorld();
        private final KinematicBody playerBody;
        private final CharacterController characterController;
        private final CharacterMovementController movementController;
        private final Object3D playerAnchor = new Object3D();
        private final Object3D playerModel = new Object3D();
        private final PhysicsBinding playerBinding;
        private final Vector3f followTarget = new Vector3f();
        private final BufferGeometry boxGeometry = BoxGeometry.create(1.0F, 1.0F, 1.0F);
        private final BufferGeometry playerGeometry = CylinderGeometry.create(0.45F, 1.9F);
        private final LambertMaterial floorMaterial = new LambertMaterial(Color.srgb(0x263746));
        private final LambertMaterial wallMaterial = new LambertMaterial(Color.srgb(0x2A9D8F));
        private final LambertMaterial landmarkMaterial = new LambertMaterial(Color.srgb(0xE9C46A));
        private final LambertMaterial playerMaterial = new LambertMaterial(Color.CYAN);
        private final LambertMaterial facingMaterial = new LambertMaterial(Color.srgb(0xF72585));
        private final InputMap inputMap = InputMap.builder()
                .bind(MOVE_FORWARD, Key.W)
                .bind(MOVE_FORWARD, Key.UP)
                .bind(MOVE_BACKWARD, Key.S)
                .bind(MOVE_BACKWARD, Key.DOWN)
                .bind(MOVE_LEFT, Key.A)
                .bind(MOVE_LEFT, Key.LEFT)
                .bind(MOVE_RIGHT, Key.D)
                .bind(MOVE_RIGHT, Key.RIGHT)
                .bind(JUMP, Key.SPACE)
                .build();

        private CharacterMoveResult movement;
        private ActionSnapshot input = ActionSnapshot.empty();
        private long simulationNanos;
        private int fixedUpdateCount;
        private float interpolation;
        private boolean closed;

        private SandboxApplication(ExampleContext context) {
            this.context = context;
            camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.05F, 100.0F);
            camera.setPosition(0.0F, 4.6F, 12.0F);
            orbitControls = new OrbitControls(camera, context.window());
            configureCamera();
            createScene();
            playerBody = physicsWorld.addKinematicBody(new Vector3f(0.0F, 0.951F, 6.0F), new Quaternionf());
            playerBody.addCollider(new CapsuleShape(0.45F, 1.0F));
            characterController = new CharacterController(physicsWorld, playerBody);
            movementController = new CharacterMovementController(characterController, MOVEMENT_ACTIONS, MOVE_SPEED);
            createPlayerPresentation();
            playerBinding = new PhysicsBinding(playerBody, playerAnchor);
            movement = characterController.move(new Vector3f(), 1.0F / 120.0F);
        }

        @Override
        public void start() {
            playerBinding.snap();
            resetCamera();
        }

        @Override
        public void fixedUpdate(FixedUpdate update) {
            input = update.input();
            Vector3f viewForward = new Vector3f(0.0F, 0.0F, -1.0F).rotate(camera.quaternion());
            movement = movementController.move(input, viewForward, update.step());
            faceMovement(movementController.desiredVelocity(new Vector3f()));
            playerBinding.capture();
        }

        @Override
        public void update(FrameUpdate update) {
            input = update.input();
            simulationNanos = update.simulationTime().toNanos();
            fixedUpdateCount = update.fixedUpdateCount();
            interpolation = update.interpolation();
        }

        @Override
        public void render(FrameUpdate update) {
            playerBinding.apply(update.interpolation());
            followPlayer();
            context.renderer().render(scene, camera);
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            facingMaterial.close();
            playerMaterial.close();
            landmarkMaterial.close();
            wallMaterial.close();
            floorMaterial.close();
            playerGeometry.close();
            boxGeometry.close();
            closed = true;
        }

        /** Applies mouse orbit and zoom without letting orbit controls consume movement keys. */
        private void updateCamera(float elapsedSeconds, boolean pointerCaptured) {
            if (pointerCaptured) {
                orbitControls.updateWithoutUserInput(elapsedSeconds);
            } else {
                orbitControls.updateWithoutKeyboardInput(elapsedSeconds);
            }
        }

        /** Updates the projection after the hosted viewport changes. */
        private void resize() {
            camera.setAspectRatio(context.aspectRatio());
        }

        /** Returns the player and camera to their initial transforms. */
        private void reset() {
            characterController.teleport(new Vector3f(0.0F, 0.951F, 6.0F), new Quaternionf());
            playerModel.setRotationFromEuler(0.0F, 0.0F, 0.0F, RotationOrder.YXZ);
            playerBinding.snap();
            resetCamera();
        }

        /** Configures the camera as an orbiting follow camera rather than a scene editor camera. */
        private void configureCamera() {
            orbitControls.setPanningEnabled(false);
            orbitControls.setDampingEnabled(true);
            orbitControls.setDampingFactor(0.12F);
            orbitControls.setDistanceLimits(3.5F, 10.0F);
            orbitControls.setPolarAngleLimits(0.35F, 1.45F);
        }

        /** Builds a visible character with a magenta marker identifying its forward direction. */
        private void createPlayerPresentation() {
            Mesh body = new Mesh(playerGeometry, playerMaterial);
            Mesh facingMarker = new Mesh(boxGeometry, facingMaterial);
            facingMarker.setPosition(0.0F, 0.2F, -0.43F);
            facingMarker.setScale(0.28F, 0.28F, 0.16F);
            playerModel.add(body);
            playerModel.add(facingMarker);
            playerAnchor.add(playerModel);
            scene.add(playerAnchor);
        }

        /** Builds the rendered room and matching static collision bodies. */
        private void createScene() {
            scene.setBackground(Color.srgb(0x07101A));
            scene.add(new AmbientLight(Color.WHITE, 0.35F));
            DirectionalLight light = new DirectionalLight(Color.srgb(0xFFF0D8), 2.4F);
            light.setPosition(6.0F, 10.0F, 8.0F);
            scene.add(light);
            addBox(new Vector3f(0.0F, -0.25F, 0.0F), new Vector3f(20.0F, 0.5F, 20.0F), floorMaterial);
            addBox(new Vector3f(0.0F, 2.0F, -9.5F), new Vector3f(20.0F, 4.0F, 1.0F), wallMaterial);
            addBox(new Vector3f(-9.5F, 2.0F, 0.0F), new Vector3f(1.0F, 4.0F, 20.0F), wallMaterial);
            addBox(new Vector3f(9.5F, 2.0F, 0.0F), new Vector3f(1.0F, 4.0F, 20.0F), wallMaterial);
            addBox(new Vector3f(0.0F, 0.15F, 3.0F), new Vector3f(3.0F, 0.3F, 1.0F), landmarkMaterial);
            addBox(new Vector3f(-3.0F, 1.0F, 0.0F), new Vector3f(2.0F, 2.0F, 2.0F), landmarkMaterial);
            addBox(new Vector3f(3.5F, 0.5F, -3.0F), new Vector3f(4.0F, 1.0F, 1.5F), landmarkMaterial);
        }

        /** Adds matching visible and collision boxes. */
        private void addBox(Vector3f position, Vector3f dimensions, LambertMaterial material) {
            physicsWorld
                    .addStaticBody(position, new Quaternionf())
                    .addCollider(new BoxShape(dimensions.x, dimensions.y, dimensions.z));
            Mesh mesh = new Mesh(boxGeometry, material);
            mesh.setPosition(position);
            mesh.setScale(dimensions);
            scene.add(mesh);
        }

        /** Turns the character model toward its requested horizontal travel direction. */
        private void faceMovement(Vector3f velocity) {
            if (velocity.lengthSquared() <= 1.0E-6F) {
                return;
            }
            float yaw = (float) Math.atan2(-velocity.x, -velocity.z);
            playerModel.setRotationFromEuler(0.0F, yaw, 0.0F, RotationOrder.YXZ);
        }

        /** Moves both orbit target and camera by the player's interpolated displacement. */
        private void followPlayer() {
            Vector3f nextTarget = new Vector3f(playerAnchor.position()).add(0.0F, TARGET_HEIGHT, 0.0F);
            Vector3f targetMovement = nextTarget.sub(followTarget, new Vector3f());
            camera.setPosition(new Vector3f(camera.position()).add(targetMovement));
            followTarget.set(nextTarget);
            orbitControls.setTarget(followTarget);
            orbitControls.updateWithoutUserInput(0.0F);
        }

        /** Restores the follow camera at a stable offset behind the initial player pose. */
        private void resetCamera() {
            followTarget.set(playerAnchor.position()).add(0.0F, TARGET_HEIGHT, 0.0F);
            camera.setPosition(followTarget.x, followTarget.y + 3.0F, followTarget.z + 6.0F);
            orbitControls.setTarget(followTarget);
            orbitControls.updateWithoutUserInput(0.0F);
        }

        /** Returns the current fixed simulation tick. */
        private String tickStatus() {
            long tick = simulationNanos / GameLoopSettings.DEFAULT.fixedStep().toNanos();
            return Long.toString(tick);
        }

        /** Returns the number of fixed updates in the most recent rendered frame. */
        private String fixedUpdateStatus() {
            return Integer.toString(fixedUpdateCount);
        }

        /** Returns the current fixed-state interpolation fraction. */
        private String interpolationStatus() {
            return String.format(Locale.ROOT, "%.3f", interpolation);
        }

        /** Returns the authoritative player position. */
        private String positionStatus() {
            Vector3f position = playerBody.position(new Vector3f());
            return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", position.x, position.y, position.z);
        }

        /** Returns whether the character controller currently has walkable support. */
        private String groundedStatus() {
            return Boolean.toString(movement.isGrounded());
        }

        /** Returns the current semantic movement axes. */
        private String movementStatus() {
            return String.format(
                    Locale.ROOT,
                    "left/right %.0f / forward/back %.0f",
                    input.axis(MOVE_LEFT, MOVE_RIGHT),
                    input.axis(MOVE_BACKWARD, MOVE_FORWARD));
        }
    }
}
