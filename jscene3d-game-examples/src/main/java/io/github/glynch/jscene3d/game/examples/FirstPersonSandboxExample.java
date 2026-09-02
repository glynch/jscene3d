/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.PointerLockControls;
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
import io.github.glynch.jscene3d.physics.CharacterController;
import io.github.glynch.jscene3d.physics.KinematicBody;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.movement.CharacterMoveResult;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.scenes.Scene;
import java.time.Duration;
import java.util.Locale;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Demonstrates the complete game-runtime seam in a small first-person movement sandbox. */
public final class FirstPersonSandboxExample {
    /** Prevents instantiation of this example entry point. */
    private FirstPersonSandboxExample() {
        throw new AssertionError("FirstPersonSandboxExample cannot be instantiated");
    }

    /**
     * Opens the example until the window closes.
     *
     * <p>Click the rendered view to capture the pointer, use W/S or Up/Down to move, A/D to
     * strafe, Left/Right or the mouse to turn, Space to jump, and Escape to release the pointer.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - First-Person Game Sandbox", FirstPersonSandboxExample::create);
    }

    /** Creates the shared hosted implementation used by browser and standalone launch modes. */
    static HostedExample create(ExampleContext context) {
        SandboxApplication application = new SandboxApplication(context);
        ControlPanel panel = createPanel(context, application);
        return new HostedGameExample(context, application, panel);
    }

    /** Builds live instructions and game-loop diagnostics. */
    private static ControlPanel createPanel(ExampleContext context, SandboxApplication application) {
        ControlPanel panel = new ControlPanel(context.window(), "First-Person Game Sandbox");
        ControlPanel.Section controls = panel.addSection("Controls");
        controls.addText("capture pointer", () -> "click the rendered view");
        controls.addText("forward / back", () -> "W S / Up Down");
        controls.addText("strafe", () -> "A D");
        controls.addText("turn", () -> "Left Right / mouse");
        controls.addText("jump", () -> "Space");
        controls.addText("release pointer", () -> "Escape");
        controls.addButton("capture pointer", () -> !application.isPointerLocked(), application::lockPointer);
        controls.addButton("reset player", application::reset);
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
            capturePointer(frame.pointerCaptured());
            InputCapture capture = new InputCapture(frame.keyboardCaptured(), panel.capturesPointer());
            ActionSnapshot input = application.inputMap.sample(context.window().input(), capture);
            application.updateView(input, frame.elapsedSeconds());
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

        /** Captures a primary click in the rendered viewport when the host UI does not own it. */
        private void capturePointer(boolean hostPointerCaptured) {
            if (!application.isPointerLocked()
                    && !hostPointerCaptured
                    && !panel.capturesPointer()
                    && context.containsPointer()
                    && context.window().input().wasMouseButtonPressed(MouseButton.LEFT)) {
                application.lockPointer();
            }
        }

        /** Converts the example host's finite seconds to nanosecond game-loop time. */
        private static Duration elapsed(float elapsedSeconds) {
            return Duration.ofNanos(Math.round(elapsedSeconds * 1_000_000_000.0));
        }
    }

    /** Owns the example's game-specific world, rules, presentation, and resources. */
    private static final class SandboxApplication implements GameApplication {
        private static final InputAction MOVE_FORWARD = new InputAction("move-forward");
        private static final InputAction MOVE_BACKWARD = new InputAction("move-backward");
        private static final InputAction MOVE_LEFT = new InputAction("move-left");
        private static final InputAction MOVE_RIGHT = new InputAction("move-right");
        private static final InputAction TURN_LEFT = new InputAction("turn-left");
        private static final InputAction TURN_RIGHT = new InputAction("turn-right");
        private static final InputAction JUMP = new InputAction("jump");
        private static final CharacterMovementActions MOVEMENT_ACTIONS =
                new CharacterMovementActions(MOVE_FORWARD, MOVE_BACKWARD, MOVE_LEFT, MOVE_RIGHT, JUMP);
        private static final float MOVE_SPEED = 5.0F;
        private static final float KEYBOARD_TURN_SPEED = 2.2F;
        private static final float EYE_OFFSET = 0.62F;

        private final ExampleContext context;
        private final Scene scene = new Scene();
        private final PerspectiveCamera camera;
        private final PointerLockControls pointerControls;
        private final PhysicsWorld physicsWorld = new PhysicsWorld();
        private final KinematicBody playerBody;
        private final CharacterController characterController;
        private final CharacterMovementController movementController;
        private final Mesh playerPresentation;
        private final PhysicsBinding playerBinding;
        private final BufferGeometry boxGeometry = BoxGeometry.create(1.0F, 1.0F, 1.0F);
        private final BufferGeometry playerGeometry = CylinderGeometry.create(0.45F, 1.9F);
        private final LambertMaterial floorMaterial = new LambertMaterial(Color.srgb(0x263746));
        private final LambertMaterial wallMaterial = new LambertMaterial(Color.srgb(0x2A9D8F));
        private final LambertMaterial landmarkMaterial = new LambertMaterial(Color.srgb(0xE9C46A));
        private final LambertMaterial playerMaterial = new LambertMaterial(Color.CYAN);
        private final InputMap inputMap = InputMap.builder()
                .bind(MOVE_FORWARD, Key.W)
                .bind(MOVE_FORWARD, Key.UP)
                .bind(MOVE_BACKWARD, Key.S)
                .bind(MOVE_BACKWARD, Key.DOWN)
                .bind(MOVE_LEFT, Key.A)
                .bind(MOVE_RIGHT, Key.D)
                .bind(TURN_LEFT, Key.LEFT)
                .bind(TURN_RIGHT, Key.RIGHT)
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
            camera.setPosition(0.0F, 1.57F, 6.0F);
            camera.lookAt(0.0F, 1.2F, 0.0F);
            pointerControls = new PointerLockControls(camera, context.window());
            createScene();
            playerBody = physicsWorld.addKinematicBody(new Vector3f(0.0F, 0.951F, 6.0F), new Quaternionf());
            playerBody.addCollider(new CapsuleShape(0.45F, 1.0F));
            characterController = new CharacterController(physicsWorld, playerBody);
            movementController = new CharacterMovementController(characterController, MOVEMENT_ACTIONS, MOVE_SPEED);
            playerPresentation = new Mesh(playerGeometry, playerMaterial);
            playerPresentation.setVisible(false);
            scene.add(playerPresentation);
            playerBinding = new PhysicsBinding(playerBody, playerPresentation);
            movement = characterController.move(new Vector3f(), 1.0F / 120.0F);
        }

        @Override
        public void start() {
            playerBinding.snap();
            updateCameraPosition();
        }

        @Override
        public void fixedUpdate(FixedUpdate update) {
            input = update.input();
            Vector3f viewForward = new Vector3f(0.0F, 0.0F, -1.0F).rotate(camera.quaternion());
            movement = movementController.move(input, viewForward, update.step());
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
            updateCameraPosition();
            context.renderer().render(scene, camera);
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            pointerControls.close();
            playerMaterial.close();
            landmarkMaterial.close();
            wallMaterial.close();
            floorMaterial.close();
            playerGeometry.close();
            boxGeometry.close();
            closed = true;
        }

        private void resize() {
            camera.setAspectRatio(context.aspectRatio());
        }

        private void updateView(ActionSnapshot actions, float elapsedSeconds) {
            pointerControls.update();
            float turn = actions.axis(TURN_RIGHT, TURN_LEFT);
            if (turn != 0.0F) {
                float yaw = pointerControls.yaw() + turn * KEYBOARD_TURN_SPEED * elapsedSeconds;
                pointerControls.setAngles(yaw, pointerControls.pitch());
            }
        }

        private boolean isPointerLocked() {
            return pointerControls.isLocked();
        }

        private void lockPointer() {
            pointerControls.lock();
        }

        private void reset() {
            characterController.teleport(new Vector3f(0.0F, 0.951F, 6.0F), new Quaternionf());
            playerBinding.snap();
            pointerControls.setAngles(0.0F, 0.0F);
            updateCameraPosition();
        }

        private String tickStatus() {
            long tick = simulationNanos / frameStepNanos();
            return Long.toString(tick);
        }

        private String fixedUpdateStatus() {
            return Integer.toString(fixedUpdateCount);
        }

        private String interpolationStatus() {
            return String.format(Locale.ROOT, "%.3f", interpolation);
        }

        private String positionStatus() {
            Vector3f position = playerBody.position(new Vector3f());
            return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", position.x, position.y, position.z);
        }

        private String groundedStatus() {
            return Boolean.toString(movement.isGrounded());
        }

        private String movementStatus() {
            return String.format(
                    Locale.ROOT,
                    "strafe %.0f / forward %.0f / turn %.0f",
                    input.axis(MOVE_LEFT, MOVE_RIGHT),
                    input.axis(MOVE_BACKWARD, MOVE_FORWARD),
                    input.axis(TURN_RIGHT, TURN_LEFT));
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

        /** Moves the first-person eye to the interpolated presentation position. */
        private void updateCameraPosition() {
            camera.setPosition(
                    playerPresentation.position().x(),
                    playerPresentation.position().y() + EYE_OFFSET,
                    playerPresentation.position().z());
        }

        /** Returns the default fixed-step nanoseconds used by this example. */
        private static long frameStepNanos() {
            return GameLoopSettings.DEFAULT.fixedStep().toNanos();
        }
    }
}
