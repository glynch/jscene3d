/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.LoopMode;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.DirectionalLightShadow;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.EnumMap;
import java.util.List;

/** Recreates the Three.js Soldier demonstration of skeletal animation blending. */
public final class SoldierAnimationBlendingExample {
    private static final Color BACKGROUND = Color.srgb(0xa0a0a0);
    private static final String MODEL_RESOURCE = "/io/github/glynch/jscene3d/examples/soldier/Soldier.glb";

    /** Prevents instantiation of this example entry point. */
    private SoldierAnimationBlendingExample() {
        throw new AssertionError("SoldierAnimationBlendingExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Soldier Animation Blending", SoldierAnimationBlendingExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        LoadedGltf loaded = GltfLoader.load(path(SoldierAnimationBlendingExample.class, MODEL_RESOURCE));
        Scene scene = loaded.scene();
        scene.setBackground(BACKGROUND);
        Object3D model = scene.children().getFirst();
        configureModel(model);

        BufferGeometry groundGeometry = PlaneGeometry.create(40.0f, 40.0f);
        LambertMaterial groundMaterial = new LambertMaterial(Color.srgb(0xcbcbcb));
        groundMaterial.setSide(MaterialSide.DOUBLE);
        Mesh ground = new Mesh(groundGeometry, groundMaterial);
        ground.rotateX(-PI_OVER_TWO);
        ground.setShadowReceivingEnabled(true);
        scene.add(ground);
        scene.add(new HemisphereLight(Color.WHITE, Color.srgb(0x8d8d8d), 3.0f));
        scene.add(createKeyLight());

        BlendState settings = new BlendState(model, new AnimationMixer(), loaded.animations());
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(1.0f, 2.0f, -3.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 1.0f, 0.0f);
        controls.setDistanceLimits(2.2f, 16.0f);
        controls.setPolarAngleLimits(0.0f, PI_OVER_TWO);
        controls.setDampingEnabled(true);
        controls.update();

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(groundGeometry);
        example.own(groundMaterial);
        example.own(loaded);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            settings.update(frame.elapsedSeconds());
            panel.update();
            fpsMonitor.update();
        });
        return example;
    }

    /** Enables shadow casting for every imported mesh beneath the model root. */
    private static void configureModel(Object3D model) {
        model.traverse(object -> {
            if (object instanceof Mesh mesh) {
                mesh.setShadowCastingEnabled(true);
            }
        });
    }

    /** Creates the parallel key light and its focused shadow volume. */
    private static DirectionalLight createKeyLight() {
        DirectionalLight light = new DirectionalLight(Color.WHITE, 2.8f);
        light.setPosition(-3.0f, 10.0f, -10.0f);
        light.setTarget(0.0f, 1.0f, 0.0f);
        DirectionalLightShadow shadow = light.shadow();
        shadow.setMapSize(1024, 1024);
        shadow.setCameraBounds(-4.0f, 4.0f, -4.0f, 4.0f);
        shadow.setCameraRange(0.5f, 24.0f);
        shadow.setBias(0.0015f);
        shadow.setNormalBias(0.035f);
        light.setShadowCastingEnabled(true);
        return light;
    }

    /** Creates controls corresponding to the useful portions of the Three.js reference panel. */
    private static ControlPanel createPanel(ExampleContext context, BlendState settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Skeletal Animation Blending");
        ControlPanel.Section visibility = panel.addSection("Visibility");
        visibility.addBoolean("show model", settings::modelVisible, settings::setModelVisible);

        ControlPanel.Section activation = panel.addSection("Activation/Deactivation");
        activation.addButton("deactivate all", settings::active, settings::deactivateAll);
        activation.addButton("activate all", settings::inactive, settings::activateAll);

        ControlPanel.Section stepping = panel.addSection("Pausing/Stepping");
        stepping.addButton("pause/continue", settings::active, settings::pauseContinue);
        stepping.addButton("make single step", settings::active, settings::makeSingleStep);
        stepping.addFloat("modify step size", settings::stepSize, settings::setStepSize, 0.01f, 0.1f);

        ControlPanel.Section crossFading = panel.addSection("Cross-fading");
        crossFading.addButton("from walk to idle", settings::canWalkToIdle, settings::walkToIdle);
        crossFading.addButton("from idle to walk", settings::canIdleToWalk, settings::idleToWalk);
        crossFading.addButton("from walk to run", settings::canWalkToRun, settings::walkToRun);
        crossFading.addButton("from run to walk", settings::canRunToWalk, settings::runToWalk);
        crossFading.addBoolean("use default duration", settings::usesDefaultDuration, settings::setUsesDefaultDuration);
        crossFading.addFloat("set custom duration", settings::customDuration, settings::setCustomDuration, 0.0f, 10.0f);

        ControlPanel.Section weights = panel.addSection("Blend weights");
        weights.addFloat("modify idle weight", settings::idleWeight, settings::setIdleWeight, 0.0f, 1.0f);
        weights.addFloat("modify walk weight", settings::walkWeight, settings::setWalkWeight, 0.0f, 1.0f);
        weights.addFloat("modify run weight", settings::runWeight, settings::setRunWeight, 0.0f, 1.0f);

        ControlPanel.Section speed = panel.addSection("General Speed");
        speed.addFloat("modify time scale", settings::speed, settings::setSpeed, 0.0f, 1.5f);
        return panel;
    }

    /** Movement clips used by the blending controls. */
    private enum Motion {
        IDLE("Idle"),
        WALK("Walk"),
        RUN("Run");

        private final String clipName;

        /** Retains the exact imported clip name. */
        Motion(String clipName) {
            this.clipName = clipName;
        }
    }

    /** Stable and transitional states of the movement cross-fade graph. */
    private enum MovementState {
        IDLE,
        WALKING,
        RUNNING,
        TRANSITIONING,
        CUSTOM_BLEND
    }

    /** Owns the three actions and all live panel state for this example. */
    private static final class BlendState {
        private static final float EXCLUSIVE_WEIGHT_TOLERANCE = 0.0001f;

        private final Object3D model;
        private final AnimationMixer mixer;
        private final EnumMap<Motion, AnimationAction> actions = new EnumMap<>(Motion.class);
        private final EnumMap<Motion, Float> activationWeights = new EnumMap<>(Motion.class);

        private MovementState movementState = MovementState.WALKING;
        private Motion transitionDestination = Motion.WALK;
        private boolean active = true;
        private boolean paused;
        private boolean usesDefaultDuration = true;
        private float stepSize = 0.05f;
        private float pendingStep;
        private float customDuration = 3.5f;
        private float speed = 1.0f;

        /** Creates and activates idle, walk, and run actions with walking initially visible. */
        private BlendState(Object3D model, AnimationMixer mixer, List<AnimationClip> clips) {
            this.model = model;
            this.mixer = mixer;
            for (Motion motion : Motion.values()) {
                actions.put(
                        motion,
                        mixer.action(requireClip(clips, motion.clipName)).setLoopMode(LoopMode.REPEAT));
            }
            setStoredWeight(Motion.IDLE, 0.0f);
            setStoredWeight(Motion.WALK, 1.0f);
            setStoredWeight(Motion.RUN, 0.0f);
            activateAll();
        }

        /** Advances ordinary playback or consumes one requested single-step interval. */
        private void update(float elapsedSeconds) {
            if (!active) {
                return;
            }
            float updateSeconds = paused ? pendingStep : elapsedSeconds;
            pendingStep = 0.0f;
            if (updateSeconds > 0.0f) {
                mixer.update(updateSeconds);
                rememberEffectiveWeights();
                refreshTransitionState();
            }
        }

        /** Returns whether the model is shown. */
        private boolean modelVisible() {
            return model.isVisible();
        }

        /** Shows or hides the complete imported model. */
        private void setModelVisible(boolean visible) {
            model.setVisible(visible);
        }

        /** Returns whether actions currently contribute and advance. */
        private boolean active() {
            return active;
        }

        /** Returns whether every action is currently deactivated. */
        private boolean inactive() {
            return !active;
        }

        /** Toggles ordinary elapsed-time playback while retaining the current blended pose. */
        private void pauseContinue() {
            paused = !paused;
        }

        /** Returns the interval consumed by the next single-step request. */
        private float stepSize() {
            return stepSize;
        }

        /** Changes the interval consumed by subsequent single-step requests. */
        private void setStepSize(float stepSize) {
            this.stepSize = stepSize;
        }

        /** Pauses ordinary playback and requests one exact mixer update. */
        private void makeSingleStep() {
            paused = true;
            pendingStep = stepSize;
        }

        /** Returns the common playback-rate multiplier. */
        private float speed() {
            return speed;
        }

        /** Applies one playback-rate multiplier to all three actions. */
        private void setSpeed(float speed) {
            this.speed = speed;
            actions.values().forEach(action -> action.setTimeScale(speed));
        }

        /** Returns whether each transition uses its reference default duration. */
        private boolean usesDefaultDuration() {
            return usesDefaultDuration;
        }

        /** Selects reference-specific or shared custom transition duration. */
        private void setUsesDefaultDuration(boolean usesDefaultDuration) {
            this.usesDefaultDuration = usesDefaultDuration;
        }

        /** Returns the custom transition duration in seconds. */
        private float customDuration() {
            return customDuration;
        }

        /** Changes the custom transition duration in seconds. */
        private void setCustomDuration(float customDuration) {
            this.customDuration = customDuration;
        }

        /** Starts the reference walk-to-idle transition. */
        private void walkToIdle() {
            crossFade(Motion.WALK, Motion.IDLE, 1.0f);
        }

        /** Starts the reference idle-to-walk transition. */
        private void idleToWalk() {
            crossFade(Motion.IDLE, Motion.WALK, 0.5f);
        }

        /** Starts the reference walk-to-run transition. */
        private void walkToRun() {
            crossFade(Motion.WALK, Motion.RUN, 2.5f);
        }

        /** Starts the reference run-to-walk transition. */
        private void runToWalk() {
            crossFade(Motion.RUN, Motion.WALK, 5.0f);
        }

        /** Returns whether walking can currently transition to idle. */
        private boolean canWalkToIdle() {
            return canTransitionFrom(MovementState.WALKING);
        }

        /** Returns whether idle can currently transition to walking. */
        private boolean canIdleToWalk() {
            return canTransitionFrom(MovementState.IDLE);
        }

        /** Returns whether walking can currently transition to running. */
        private boolean canWalkToRun() {
            return canTransitionFrom(MovementState.WALKING);
        }

        /** Returns whether running can currently transition to walking. */
        private boolean canRunToWalk() {
            return canTransitionFrom(MovementState.RUNNING);
        }

        /** Returns the current idle contribution. */
        private float idleWeight() {
            return action(Motion.IDLE).effectiveWeight();
        }

        /** Sets the current idle contribution directly. */
        private void setIdleWeight(float weight) {
            setManualWeight(Motion.IDLE, weight);
        }

        /** Returns the current walking contribution. */
        private float walkWeight() {
            return action(Motion.WALK).effectiveWeight();
        }

        /** Sets the current walking contribution directly. */
        private void setWalkWeight(float weight) {
            setManualWeight(Motion.WALK, weight);
        }

        /** Returns the current running contribution. */
        private float runWeight() {
            return action(Motion.RUN).effectiveWeight();
        }

        /** Sets the current running contribution directly. */
        private void setRunWeight(float weight) {
            setManualWeight(Motion.RUN, weight);
        }

        /** Starts one immediate cross-fade after making its source the complete current pose. */
        private void crossFade(Motion sourceMotion, Motion destinationMotion, float defaultDuration) {
            MovementState requiredState = stableState(sourceMotion);
            if (!canTransitionFrom(requiredState)) {
                throw new IllegalStateException(
                        "Cannot cross-fade from " + sourceMotion + " while movement state is " + movementState);
            }
            active = true;
            paused = false;
            pendingStep = 0.0f;
            zeroOtherActions(sourceMotion, destinationMotion);
            AnimationAction source = prepareSource(sourceMotion);
            AnimationAction destination = action(destinationMotion).setWeight(1.0f);
            movementState = MovementState.TRANSITIONING;
            transitionDestination = destinationMotion;
            mixer.crossFade(source, destination, usesDefaultDuration ? defaultDuration : customDuration);
            refreshTransitionState();
        }

        /** Sets any third action to zero so it cannot distort a named two-action transition. */
        private void zeroOtherActions(Motion source, Motion destination) {
            for (Motion motion : Motion.values()) {
                if (motion != source && motion != destination) {
                    action(motion).stopFading().setWeight(0.0f);
                }
            }
        }

        /** Makes a selected source action active with complete influence without resetting time. */
        private AnimationAction prepareSource(Motion motion) {
            return action(motion).play().fadeIn(0.0f).setWeight(1.0f);
        }

        /** Applies one exact manual weight after preserving and cancelling the current fade. */
        private void setManualWeight(Motion motion, float weight) {
            active = true;
            cancelFadesAtCurrentWeights();
            action(motion).setWeight(weight);
            rememberEffectiveWeights();
            movementState = resolveMovementState();
        }

        /** Converts every in-progress fade contribution into an ordinary action weight. */
        private void cancelFadesAtCurrentWeights() {
            for (Motion motion : Motion.values()) {
                AnimationAction currentAction = action(motion);
                float currentWeight = currentAction.effectiveWeight();
                currentAction.play().fadeIn(0.0f).setWeight(currentWeight);
            }
        }

        /** Restores all remembered action weights and playback states. */
        private void activateAll() {
            active = true;
            for (Motion motion : Motion.values()) {
                action(motion).play().fadeIn(0.0f).setWeight(activationWeights.getOrDefault(motion, 0.0f));
            }
            movementState = resolveMovementState();
        }

        /** Removes every contribution after remembering the currently visible blend. */
        private void deactivateAll() {
            rememberEffectiveWeights();
            movementState = resolveMovementState();
            actions.values().forEach(action -> action.stopFading().setWeight(0.0f));
            active = false;
        }

        /** Returns whether the current stable movement state permits a transition. */
        private boolean canTransitionFrom(MovementState sourceState) {
            return active && movementState == sourceState;
        }

        /** Completes a transition state once its destination owns the complete blend. */
        private void refreshTransitionState() {
            if (movementState == MovementState.TRANSITIONING && hasExclusiveWeight(transitionDestination)) {
                movementState = stableState(transitionDestination);
            }
        }

        /** Resolves one stable movement or the manual mixed-weight state. */
        private MovementState resolveMovementState() {
            for (Motion motion : Motion.values()) {
                if (hasExclusiveWeight(motion)) {
                    return stableState(motion);
                }
            }
            return MovementState.CUSTOM_BLEND;
        }

        /** Returns whether one action owns effectively the complete movement blend. */
        private boolean hasExclusiveWeight(Motion selectedMotion) {
            for (Motion motion : Motion.values()) {
                float expectedWeight = motion == selectedMotion ? 1.0f : 0.0f;
                if (Math.abs(action(motion).effectiveWeight() - expectedWeight) > EXCLUSIVE_WEIGHT_TOLERANCE) {
                    return false;
                }
            }
            return true;
        }

        /** Maps one movement clip to its stable state-machine state. */
        private static MovementState stableState(Motion motion) {
            return switch (motion) {
                case IDLE -> MovementState.IDLE;
                case WALK -> MovementState.WALKING;
                case RUN -> MovementState.RUNNING;
            };
        }

        /** Captures the current effective blend for later reactivation. */
        private void rememberEffectiveWeights() {
            for (Motion motion : Motion.values()) {
                activationWeights.put(motion, action(motion).effectiveWeight());
            }
        }

        /** Stores one initial activation weight. */
        private void setStoredWeight(Motion motion, float weight) {
            activationWeights.put(motion, weight);
        }

        /** Returns the required action registered for one movement. */
        private AnimationAction action(Motion motion) {
            AnimationAction action = actions.get(motion);
            if (action == null) {
                throw new IllegalStateException("Missing animation action: " + motion);
            }
            return action;
        }

        /** Finds one required clip by its exact imported name. */
        private static AnimationClip requireClip(List<AnimationClip> clips, String name) {
            return clips.stream()
                    .filter(clip -> clip.name().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Missing required animation clip: " + name));
        }
    }
}
