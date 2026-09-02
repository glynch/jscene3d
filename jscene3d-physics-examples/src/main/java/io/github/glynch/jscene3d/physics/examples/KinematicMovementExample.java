/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleDiagnostics;
import io.github.glynch.jscene3d.examples.framework.ExampleFrame;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.BufferUsage;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.debug.PhysicsDebugLine;
import io.github.glynch.jscene3d.physics.debug.PhysicsDebugSnapshot;
import io.github.glynch.jscene3d.physics.examples.MovementDiagnostics.MovementKeys;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveResult;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;
import java.util.Locale;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Demonstrates explicit fixed-step kinematic movement through a small obstacle course. */
public final class KinematicMovementExample {
    private static final float FIXED_SECONDS = 1.0F / 120.0F;

    /** Prevents instantiation of this example entry point. */
    private KinematicMovementExample() {
        throw new AssertionError("KinematicMovementExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Kinematic Movement", KinematicMovementExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        Resources resources = new Resources();
        Scene scene = createScene();
        Course course = new Course(scene, resources);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1F, 100.0F);
        camera.setPosition(11.0F, 10.0F, 14.0F);
        camera.lookAt(2.0F, 0.5F, 0.0F);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDampingEnabled(true);
        controls.setDistanceLimits(8.0F, 30.0F);
        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(resources);

        PhysicsDebugLines debugLines = example.own(new PhysicsDebugLines(course.world()));
        scene.add(debugLines.lines());
        MovementDiagnostics diagnostics = new MovementDiagnostics(new ExampleDiagnostics());
        MovementState state = new MovementState(context, course, debugLines, diagnostics);
        ControlPanel panel = example.addOverlay(createPanel(context, state, debugLines, diagnostics));
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> update(frame, panel, state));
        return example;
    }

    private static Scene createScene() {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x07101A));
        scene.add(new AmbientLight(Color.WHITE, 0.3F));
        DirectionalLight light = new DirectionalLight(Color.srgb(0xFFF0D8), 2.5F);
        light.setPosition(6.0F, 10.0F, 8.0F);
        scene.add(light);
        return scene;
    }

    private static ControlPanel createPanel(
            ExampleContext context,
            MovementState state,
            PhysicsDebugLines debugLines,
            MovementDiagnostics diagnostics) {
        ControlPanel panel = new ControlPanel(context.window(), "Kinematic Movement");
        ControlPanel.Section instructions = panel.addSection("Controls");
        instructions.addText("player", () -> "cyan capsule");
        instructions.addText("move player", () -> "W A S D");
        instructions.addText("camera", () -> "drag / scroll");
        instructions.addText("goal", () -> "yellow step, then magenta trigger");
        instructions.addButton("reset player", state::reset);
        ControlPanel.Section diagnosticSection = panel.addSection("Diagnostics");
        diagnosticSection.addBoolean("enabled", diagnostics::isEnabled, diagnostics::setEnabled);
        diagnosticSection.addText("window", diagnostics::windowFocusStatus);
        diagnosticSection.addText("keyboard", diagnostics::keyboardOwnershipStatus);
        diagnosticSection.addText("raw keys", diagnostics::keyStatus);
        diagnosticSection.addText("movement input", diagnostics::movementInputStatus);
        diagnosticSection.addText("requested", diagnostics::requestedDisplacementStatus);
        diagnosticSection.addText("actual", diagnostics::actualDisplacementStatus);
        diagnosticSection.addText("fixed steps", diagnostics::fixedStepStatus);
        diagnosticSection.addText("diagnosis", diagnostics::diagnosis);
        diagnosticSection.setExpanded(diagnostics.isEnabled());
        ControlPanel.Section status = panel.addSection("Movement result");
        status.addText("position", state::positionStatus);
        status.addText("grounded", () -> Boolean.toString(state.grounded()));
        status.addText("stepped", () -> Boolean.toString(state.stepped()));
        status.addText("contacts", () -> Integer.toString(state.contactCount()));
        status.addText("trigger", state::triggerStatus);
        ControlPanel.Section visualization = panel.addSection("Visualization");
        visualization.addBoolean("physics debug lines", debugLines::isVisible, debugLines::setVisible);
        return panel;
    }

    private static void update(ExampleFrame frame, ControlPanel panel, MovementState state) {
        panel.update();
        state.update(frame.elapsedSeconds(), frame.keyboardCaptured());
    }

    /** Owns reusable geometry and materials shared by course meshes. */
    private static final class Resources implements AutoCloseable {
        private final BufferGeometry unitBox = BoxGeometry.create(1.0F, 1.0F, 1.0F);
        private final BufferGeometry playerGeometry = CylinderGeometry.create(0.45F, 1.9F);
        private final LambertMaterial floorMaterial = new LambertMaterial(Color.srgb(0x283544));
        private final LambertMaterial obstacleMaterial = new LambertMaterial(Color.srgb(0x2A9D8F));
        private final LambertMaterial stepMaterial = new LambertMaterial(Color.srgb(0xE9C46A));
        private final LambertMaterial playerMaterial = new LambertMaterial(Color.CYAN);
        private final LambertMaterial triggerMaterial = triggerMaterial();

        @Override
        public void close() {
            triggerMaterial.close();
            playerMaterial.close();
            stepMaterial.close();
            obstacleMaterial.close();
            floorMaterial.close();
            playerGeometry.close();
            unitBox.close();
        }

        private static LambertMaterial triggerMaterial() {
            LambertMaterial material = new LambertMaterial(Color.MAGENTA);
            material.setTransparent(true);
            material.setOpacity(0.28F);
            material.setDepthWriteEnabled(false);
            return material;
        }
    }

    /** Builds matching rendered and collision representations of the course. */
    private static final class Course {
        private final PhysicsWorld world = new PhysicsWorld();
        private final Collider player;
        private final Mesh playerMesh;

        private Course(Scene scene, Resources resources) {
            addBox(scene, resources, new Vector3f(2.0F, -0.5F, 0.0F), new Vector3f(16.0F, 1.0F, 12.0F), false);
            addBox(scene, resources, new Vector3f(0.5F, 0.2F, 0.0F), new Vector3f(2.0F, 0.4F, 3.0F), true);
            addBox(scene, resources, new Vector3f(4.0F, 0.65F, -2.5F), new Vector3f(1.0F, 1.3F, 5.0F), false);
            addBox(scene, resources, new Vector3f(5.5F, 1.0F, 2.0F), new Vector3f(4.0F, 2.0F, 1.0F), false);
            addTrigger(scene, resources, new Vector3f(7.0F, 1.0F, -2.0F), new Vector3f(2.0F, 2.0F, 2.0F));
            player = world.addCollider(
                    new CapsuleShape(0.45F, 1.0F), new Vector3f(-4.0F, 0.951F, 0.0F), new Quaternionf());
            playerMesh = new Mesh(resources.playerGeometry, resources.playerMaterial);
            syncPlayerMesh();
            scene.add(playerMesh);
        }

        private void addBox(Scene scene, Resources resources, Vector3f position, Vector3f dimensions, boolean step) {
            world.addCollider(new BoxShape(dimensions.x, dimensions.y, dimensions.z), position, new Quaternionf());
            Mesh mesh = new Mesh(resources.unitBox, step ? resources.stepMaterial : resources.obstacleMaterial);
            if (position.y < 0.0F) {
                mesh.setMaterial(resources.floorMaterial);
            }
            mesh.setPosition(position);
            mesh.setScale(dimensions);
            scene.add(mesh);
        }

        private void addTrigger(Scene scene, Resources resources, Vector3f position, Vector3f dimensions) {
            Collider trigger = world.addCollider(
                    new BoxShape(dimensions.x, dimensions.y, dimensions.z), position, new Quaternionf());
            trigger.setTrigger(true);
            Mesh mesh = new Mesh(resources.unitBox, resources.triggerMaterial);
            mesh.setPosition(position);
            mesh.setScale(dimensions);
            scene.add(mesh);
        }

        private void syncPlayerMesh() {
            playerMesh.setPosition(player.position(new Vector3f()));
        }

        private PhysicsWorld world() {
            return world;
        }
    }

    /** Integrates caller-owned intent and gravity before each explicit world move. */
    private static final class MovementState {
        private static final float SPEED = 4.0F;
        private static final float GRAVITY = 18.0F;

        private final ExampleContext context;
        private final Course course;
        private final PhysicsDebugLines debugLines;
        private final MovementDiagnostics diagnostics;
        private float accumulator;
        private float verticalVelocity;
        private KinematicMoveResult result;
        private String triggerStatus = "outside";

        private MovementState(
                ExampleContext context, Course course, PhysicsDebugLines debugLines, MovementDiagnostics diagnostics) {
            this.context = context;
            this.course = course;
            this.debugLines = debugLines;
            this.diagnostics = diagnostics;
            result = course.world().move(course.player, new Vector3f());
        }

        private void update(float elapsedSeconds, boolean keyboardCaptured) {
            accumulator += Math.clamp(elapsedSeconds, 0.0F, 0.1F);
            int stepCount = Math.clamp((int) (accumulator / FIXED_SECONDS), 0, 12);
            InputState input = context.window().input();
            Vector3f rawMovementInput = movementInput(input);
            Vector3f effectiveMovementInput = keyboardCaptured ? new Vector3f() : rawMovementInput;
            diagnostics.beginFrame(
                    context.window().isFocused(),
                    keyboardCaptured,
                    movementKeys(input),
                    effectiveMovementInput,
                    stepCount);
            for (int step = 0; step < stepCount; step++) {
                fixedUpdate(effectiveMovementInput);
                accumulator -= FIXED_SECONDS;
            }
        }

        private void fixedUpdate(Vector3f movementInput) {
            Vector3f horizontal = new Vector3f(movementInput);
            if (horizontal.lengthSquared() > 1.0F) {
                horizontal.normalize();
            }
            horizontal.mul(SPEED * FIXED_SECONDS);
            verticalVelocity -= GRAVITY * FIXED_SECONDS;
            Vector3f desired = horizontal.add(0.0F, verticalVelocity * FIXED_SECONDS, 0.0F);
            result = course.world().move(course.player, desired);
            diagnostics.recordStep(desired, result.appliedTranslation(new Vector3f()));
            if (result.isGrounded() && verticalVelocity < 0.0F) {
                verticalVelocity = 0.0F;
            }
            updateTriggerStatus(result);
            course.syncPlayerMesh();
            debugLines.update();
        }

        private void updateTriggerStatus(KinematicMoveResult movementResult) {
            movementResult.triggerEvents().stream()
                    .findFirst()
                    .ifPresent(event -> triggerStatus = switch (event.type()) {
                        case ENTER -> "entered trigger";
                        case STAY -> "inside trigger";
                        case EXIT -> "exited trigger";
                    });
        }

        private void reset() {
            course.player.setTransform(new Vector3f(-4.0F, 0.951F, 0.0F), new Quaternionf());
            verticalVelocity = 0.0F;
            triggerStatus = "outside";
            result = course.world().move(course.player, new Vector3f());
            course.syncPlayerMesh();
            debugLines.update();
        }

        private boolean grounded() {
            return result.isGrounded();
        }

        private boolean stepped() {
            return result.stepped();
        }

        private int contactCount() {
            return result.contacts().size();
        }

        private String triggerStatus() {
            return triggerStatus;
        }

        private String positionStatus() {
            Vector3f position = course.player.position(new Vector3f());
            return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", position.x, position.y, position.z);
        }

        private static Vector3f movementInput(InputState input) {
            float x = axis(input, Key.D, Key.A);
            float z = axis(input, Key.S, Key.W);
            return new Vector3f(x, 0.0F, z);
        }

        private static MovementKeys movementKeys(InputState input) {
            return new MovementKeys(
                    input.isKeyDown(Key.W), input.isKeyDown(Key.A), input.isKeyDown(Key.S), input.isKeyDown(Key.D));
        }

        private static float axis(InputState input, Key positive, Key negative) {
            return (input.isKeyDown(positive) ? 1.0F : 0.0F) - (input.isKeyDown(negative) ? 1.0F : 0.0F);
        }
    }

    /** Bridges renderer-independent snapshot lines to one dynamic line geometry. */
    private static final class PhysicsDebugLines implements AutoCloseable {
        private final PhysicsWorld world;
        private final BufferGeometry geometry;
        private final BufferAttribute positions;
        private final LineBasicMaterial material;
        private final LineSegments lines;

        private PhysicsDebugLines(PhysicsWorld world) {
            this.world = world;
            PhysicsDebugSnapshot snapshot = world.debugSnapshot();
            positions = BufferAttribute.of(new float[snapshot.lines().size() * 6], 3, BufferUsage.DYNAMIC);
            geometry = new BufferGeometry();
            geometry.setAttribute(BufferGeometry.POSITION, positions);
            material = new LineBasicMaterial(Color.srgb(0x66FFAA));
            lines = new LineSegments(geometry, material);
            update(snapshot.lines());
        }

        private LineSegments lines() {
            return lines;
        }

        private boolean isVisible() {
            return lines.isVisible();
        }

        private void setVisible(boolean visible) {
            lines.setVisible(visible);
        }

        private void update() {
            update(world.debugSnapshot().lines());
        }

        private void update(List<PhysicsDebugLine> debugLines) {
            if (debugLines.size() * 2 != positions.count()) {
                throw new IllegalStateException("Physics debug topology changed");
            }
            positions.edit(editor -> {
                for (int index = 0; index < debugLines.size(); index++) {
                    PhysicsDebugLine line = debugLines.get(index);
                    Vector3f start = line.start(new Vector3f());
                    Vector3f end = line.end(new Vector3f());
                    editor.setXYZ(index * 2, start.x, start.y, start.z);
                    editor.setXYZ(index * 2 + 1, end.x, end.y, end.z);
                }
            });
        }

        @Override
        public void close() {
            material.close();
            geometry.close();
        }
    }
}
