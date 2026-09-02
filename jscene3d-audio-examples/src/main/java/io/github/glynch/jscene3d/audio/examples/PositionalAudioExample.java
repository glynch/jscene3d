/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.audio.AudioCategory;
import io.github.glynch.jscene3d.audio.AudioClip;
import io.github.glynch.jscene3d.audio.AudioEngine;
import io.github.glynch.jscene3d.audio.AudioPlaybackState;
import io.github.glynch.jscene3d.audio.AudioSource;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.geometries.TorusGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.Locale;
import org.joml.Vector3f;

/** Demonstrates mono positional playback, distance attenuation, and camera listener updates. */
public final class PositionalAudioExample {
    private static final String EFFECT_RESOURCE =
            "/io/github/glynch/jscene3d/audio/examples/audio/engineCircular_000.ogg";

    /** Prevents instantiation of this example entry point. */
    private PositionalAudioExample() {
        throw new AssertionError("PositionalAudioExample cannot be instantiated");
    }

    /**
     * Opens the example until the native window closes.
     *
     * <p>Listen as the cyan source travels around the camera listener. Drag to orbit the camera and
     * use the panel to replay the sound or compare attenuation and volume settings.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Positional Audio", PositionalAudioExample::create);
    }

    /** Creates the shared hosted implementation used by browser and standalone launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry floorGeometry = PlaneGeometry.create(24.0F, 24.0F);
        BufferGeometry sourceGeometry = SphereGeometry.create(0.72F, 32, 20);
        BufferGeometry ringGeometry = TorusGeometry.create(1.05F, 0.055F, 10, 48);
        BufferGeometry orbitGeometry = TorusGeometry.create(5.0F, 0.025F, 8, 96);
        BufferGeometry listenerGeometry = CylinderGeometry.create(0.62F, 0.24F);
        LambertMaterial floorMaterial = new LambertMaterial(Color.srgb(0x14263A));
        floorMaterial.setSide(MaterialSide.DOUBLE);
        PhongMaterial sourceMaterial = new PhongMaterial(Color.srgb(0x12D7D0));
        sourceMaterial.setSpecular(Color.WHITE);
        sourceMaterial.setShininess(90.0F);
        LambertMaterial ringMaterial = new LambertMaterial(Color.srgb(0x6CF8F2));
        LambertMaterial orbitMaterial = new LambertMaterial(Color.srgb(0x287F8D));
        PhongMaterial listenerMaterial = new PhongMaterial(Color.srgb(0xFFB84D));

        Group sourceVisual = createSourceVisual(sourceGeometry, ringGeometry, sourceMaterial, ringMaterial);
        Mesh listenerVisual = new Mesh(listenerGeometry, listenerMaterial);
        listenerVisual.setPosition(0.0F, 0.18F, 0.0F);
        Mesh orbitVisual = new Mesh(orbitGeometry, orbitMaterial);
        orbitVisual.rotateX(PI_OVER_TWO);
        orbitVisual.setPosition(0.0F, 0.035F, 0.0F);

        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x06111E));
        scene.add(createFloor(floorGeometry, floorMaterial));
        scene.add(sourceVisual);
        scene.add(orbitVisual);
        scene.add(listenerVisual);
        scene.add(new HemisphereLight(Color.srgb(0xBDEBFF), Color.srgb(0x101A26), 1.4F));
        DirectionalLight keyLight = new DirectionalLight(Color.srgb(0xFFF2D8), 2.4F);
        keyLight.setPosition(-5.0F, 8.0F, 5.0F);
        scene.add(keyLight);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1F, 80.0F);
        camera.setPosition(7.0F, 5.0F, 9.0F);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0F, 0.8F, 0.0F);
        controls.setDistanceLimits(4.0F, 22.0F);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        AudioEngine engine = AudioEngine.create();
        AudioClip effectClip = engine.loadClip(ExampleCatalog.class, EFFECT_RESOURCE);
        AudioSource effectSource = engine.createSource(effectClip, AudioCategory.EFFECTS);
        effectSource.setAttenuation(1.5F, 25.0F, 1.0F);
        PositionalDemo demo = new PositionalDemo(camera, engine, effectSource, sourceVisual);

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(floorGeometry);
        example.own(sourceGeometry);
        example.own(ringGeometry);
        example.own(orbitGeometry);
        example.own(listenerGeometry);
        example.own(floorMaterial);
        example.own(sourceMaterial);
        example.own(ringMaterial);
        example.own(orbitMaterial);
        example.own(listenerMaterial);
        example.own(engine);
        ControlPanel panel = example.addOverlay(createPanel(context, demo, engine, controls));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0F, 16.0F);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            demo.update(frame.elapsedSeconds());
            fps.update();
        });
        return example;
    }

    /** Creates the moving source orb and two visible sound-wave rings. */
    private static Group createSourceVisual(
            BufferGeometry sourceGeometry,
            BufferGeometry ringGeometry,
            PhongMaterial sourceMaterial,
            LambertMaterial ringMaterial) {
        Group source = new Group();
        source.add(new Mesh(sourceGeometry, sourceMaterial));
        Mesh innerRing = new Mesh(ringGeometry, ringMaterial);
        innerRing.rotateX(PI_OVER_TWO);
        source.add(innerRing);
        Mesh outerRing = new Mesh(ringGeometry, ringMaterial);
        outerRing.rotateX(PI_OVER_TWO);
        outerRing.setScale(1.35F, 1.35F, 1.35F);
        source.add(outerRing);
        return source;
    }

    /** Creates the horizontal stage under the listener and moving source. */
    private static Mesh createFloor(BufferGeometry geometry, LambertMaterial material) {
        Mesh floor = new Mesh(geometry, material);
        floor.rotateX(-PI_OVER_TWO);
        floor.setPosition(0.0F, -0.01F, 0.0F);
        return floor;
    }

    /** Creates playback, mixing, diagnostics, and camera controls. */
    private static ControlPanel createPanel(
            ExampleContext context, PositionalDemo demo, AudioEngine engine, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Positional Audio");
        ControlPanel.Section playback = panel.addSection("Playback");
        playback.addAudioPlayer(demo.player());
        playback.addBoolean("repeat effect", demo::automaticReplay, demo::setAutomaticReplay);
        playback.addBoolean("orbit source", demo::orbiting, demo::setOrbiting);
        ControlPanel.Section volume = panel.addSection("Volume");
        volume.addFloat("master", engine::masterGain, engine::setMasterGain, 0.0F, 1.0F);
        volume.addFloat(
                "effects",
                () -> engine.categoryGain(AudioCategory.EFFECTS),
                value -> engine.setCategoryGain(AudioCategory.EFFECTS, value),
                0.0F,
                1.0F);
        ControlPanel.Section diagnostics = panel.addSection("Diagnostics");
        diagnostics.addText("source", demo::positionStatus);
        diagnostics.addText("distance", demo::distanceStatus);
        diagnostics.addText("state", demo::stateStatus);
        ControlPanel.Section view = panel.addSection("View");
        view.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Owns the moving visual and synchronizes source and listener transforms every frame. */
    private static final class PositionalDemo {
        private static final float ORBIT_RADIUS = 5.0F;
        private static final float REPLAY_INTERVAL_SECONDS = 1.15F;

        private final PerspectiveCamera camera;
        private final AudioEngine engine;
        private final AudioSource source;
        private final AudioSourcePlayerBinding player;
        private final Group sourceVisual;
        private final Vector3f sourcePosition = new Vector3f();
        private final Vector3f forward = new Vector3f();
        private final Vector3f up = new Vector3f();

        private float angle;
        private float replayCountdown;
        private boolean automaticReplay = true;
        private boolean orbiting = true;

        /** Retains the audio and scene objects that represent one moving effect. */
        private PositionalDemo(PerspectiveCamera camera, AudioEngine engine, AudioSource source, Group sourceVisual) {
            this.camera = camera;
            this.engine = engine;
            this.source = source;
            player = new AudioSourcePlayerBinding(source);
            this.sourceVisual = sourceVisual;
            updateSourcePosition();
            updateListener();
        }

        /** Advances source motion and automatic effect replay. */
        private void update(float elapsedSeconds) {
            if (orbiting) {
                angle += elapsedSeconds * 0.72F;
                updateSourcePosition();
            }
            updateListener();
            updateReplay(elapsedSeconds);
        }

        /** Restarts the positional effect immediately. */
        private void playNow() {
            source.stop();
            source.play();
            replayCountdown = REPLAY_INTERVAL_SECONDS;
        }

        /** Returns the source's media-player binding. */
        private AudioSourcePlayerBinding player() {
            return player;
        }

        /** Returns whether automatic replay is enabled. */
        private boolean automaticReplay() {
            return automaticReplay;
        }

        /** Enables automatic replay and makes its effect immediate. */
        private void setAutomaticReplay(boolean value) {
            automaticReplay = value;
            if (value && source.state() != AudioPlaybackState.PLAYING) {
                replayCountdown = 0.0F;
            }
        }

        /** Replays only after natural completion, while preserving an explicit pause. */
        private void updateReplay(float elapsedSeconds) {
            AudioPlaybackState state = source.state();
            if (state == AudioPlaybackState.PLAYING) {
                replayCountdown = REPLAY_INTERVAL_SECONDS;
                return;
            }
            if (state == AudioPlaybackState.PAUSED || !automaticReplay) {
                return;
            }
            replayCountdown -= elapsedSeconds;
            if (replayCountdown <= 0.0F) {
                playNow();
            }
        }

        /** Returns whether the source is moving around the listener marker. */
        private boolean orbiting() {
            return orbiting;
        }

        /** Enables or freezes orbital source motion. */
        private void setOrbiting(boolean value) {
            orbiting = value;
        }

        /** Formats the current source coordinates. */
        private String positionStatus() {
            return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", sourcePosition.x, sourcePosition.y, sourcePosition.z);
        }

        /** Formats current source-to-listener distance. */
        private String distanceStatus() {
            return String.format(Locale.ROOT, "%.2f", sourcePosition.distance(camera.position()));
        }

        /** Formats current native playback state. */
        private String stateStatus() {
            return source.state().name().toLowerCase(Locale.ROOT);
        }

        /** Synchronizes the visible orb and native source position. */
        private void updateSourcePosition() {
            sourcePosition.set((float) Math.cos(angle) * ORBIT_RADIUS, 1.25F, (float) Math.sin(angle) * ORBIT_RADIUS);
            sourceVisual.setPosition(sourcePosition);
            source.setPosition(sourcePosition);
        }

        /** Derives the listener orientation from the current camera transform. */
        private void updateListener() {
            forward.set(0.0F, 0.0F, -1.0F).rotate(camera.quaternion());
            up.set(0.0F, 1.0F, 0.0F).rotate(camera.quaternion());
            engine.listener().setTransform(camera.position(), forward, up);
        }
    }
}
