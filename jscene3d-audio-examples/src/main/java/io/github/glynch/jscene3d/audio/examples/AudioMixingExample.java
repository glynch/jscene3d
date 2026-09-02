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
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
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

/** Demonstrates relative music and interface playback with independent volume categories. */
public final class AudioMixingExample {
    private static final String MUSIC_RESOURCE = "/io/github/glynch/jscene3d/audio/examples/audio/jingles_NES00.ogg";
    private static final String INTERFACE_RESOURCE = "/io/github/glynch/jscene3d/audio/examples/audio/select_001.ogg";

    /** Prevents instantiation of this example entry point. */
    private AudioMixingExample() {
        throw new AssertionError("AudioMixingExample cannot be instantiated");
    }

    /**
     * Opens the example until the native window closes.
     *
     * <p>Play the longer music jingle and short interface selection independently, then use the
     * master, music, and effects sliders to hear how their gains compose.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Music and Effects Mixing", AudioMixingExample::create);
    }

    /** Creates the shared hosted implementation used by browser and standalone launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry floorGeometry = PlaneGeometry.create(20.0F, 16.0F);
        BufferGeometry consoleGeometry = BoxGeometry.create(3.2F, 1.3F, 1.6F);
        BufferGeometry musicGeometry = TorusGeometry.create(1.25F, 0.35F, 20, 64);
        BufferGeometry effectGeometry = SphereGeometry.create(0.7F, 32, 20);
        LambertMaterial floorMaterial = new LambertMaterial(Color.srgb(0x111D31));
        floorMaterial.setSide(MaterialSide.DOUBLE);
        PhongMaterial consoleMaterial = new PhongMaterial(Color.srgb(0x283C59));
        PhongMaterial musicMaterial = new PhongMaterial(Color.srgb(0x39D5FF));
        musicMaterial.setSpecular(Color.WHITE);
        musicMaterial.setShininess(96.0F);
        PhongMaterial effectMaterial = new PhongMaterial(Color.srgb(0xFF4F9A));
        effectMaterial.setSpecular(Color.WHITE);
        effectMaterial.setShininess(96.0F);

        Group musicVisual = createMusicVisual(consoleGeometry, musicGeometry, consoleMaterial, musicMaterial);
        Group effectVisual = createEffectVisual(consoleGeometry, effectGeometry, consoleMaterial, effectMaterial);
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x050D19));
        scene.add(createFloor(floorGeometry, floorMaterial));
        scene.add(musicVisual);
        scene.add(effectVisual);
        scene.add(new HemisphereLight(Color.srgb(0xBDEBFF), Color.srgb(0x151225), 1.35F));
        DirectionalLight keyLight = new DirectionalLight(Color.srgb(0xFFF0D0), 2.5F);
        keyLight.setPosition(-4.0F, 8.0F, 6.0F);
        scene.add(keyLight);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1F, 60.0F);
        camera.setPosition(8.0F, 5.3F, 11.0F);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0F, 1.1F, 0.0F);
        controls.setDistanceLimits(5.0F, 24.0F);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        AudioEngine engine = AudioEngine.create();
        AudioClip musicClip = engine.loadClip(ExampleCatalog.class, MUSIC_RESOURCE);
        AudioClip interfaceClip = engine.loadClip(ExampleCatalog.class, INTERFACE_RESOURCE);
        AudioSource musicSource = engine.createSource(musicClip, AudioCategory.MUSIC);
        musicSource.setRelative(true);
        AudioSource interfaceSource = engine.createSource(interfaceClip, AudioCategory.EFFECTS);
        interfaceSource.setRelative(true);
        MixingDemo demo = new MixingDemo(musicSource, interfaceSource, musicVisual, effectVisual);

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(floorGeometry);
        example.own(consoleGeometry);
        example.own(musicGeometry);
        example.own(effectGeometry);
        example.own(floorMaterial);
        example.own(consoleMaterial);
        example.own(musicMaterial);
        example.own(effectMaterial);
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

    /** Creates the cyan music console and ring visualization. */
    private static Group createMusicVisual(
            BufferGeometry consoleGeometry,
            BufferGeometry musicGeometry,
            PhongMaterial consoleMaterial,
            PhongMaterial musicMaterial) {
        Group visual = new Group();
        visual.setPosition(-2.8F, 1.0F, 0.0F);
        visual.add(new Mesh(consoleGeometry, consoleMaterial));
        Mesh ring = new Mesh(musicGeometry, musicMaterial);
        ring.setPosition(0.0F, 1.45F, 0.0F);
        ring.rotateX(PI_OVER_TWO);
        visual.add(ring);
        return visual;
    }

    /** Creates the magenta effects console and signal visualization. */
    private static Group createEffectVisual(
            BufferGeometry consoleGeometry,
            BufferGeometry effectGeometry,
            PhongMaterial consoleMaterial,
            PhongMaterial effectMaterial) {
        Group visual = new Group();
        visual.setPosition(2.8F, 1.0F, 0.0F);
        visual.add(new Mesh(consoleGeometry, consoleMaterial));
        Mesh signal = new Mesh(effectGeometry, effectMaterial);
        signal.setPosition(0.0F, 1.45F, 0.0F);
        visual.add(signal);
        return visual;
    }

    /** Creates the horizontal stage beneath both audio categories. */
    private static Mesh createFloor(BufferGeometry geometry, LambertMaterial material) {
        Mesh floor = new Mesh(geometry, material);
        floor.rotateX(-PI_OVER_TWO);
        return floor;
    }

    /** Creates music, interface-effect, volume, state, and view controls. */
    private static ControlPanel createPanel(
            ExampleContext context, MixingDemo demo, AudioEngine engine, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Music and Effects Mixing");
        ControlPanel.Section playback = panel.addSection("Playback");
        playback.addAudioPlayer(demo.musicPlayer());
        playback.addButton("play interface effect", demo::playInterfaceEffect);
        ControlPanel.Section volume = panel.addSection("Volume mixing");
        volume.addFloat("master", engine::masterGain, engine::setMasterGain, 0.0F, 1.0F);
        volume.addFloat(
                "music",
                () -> engine.categoryGain(AudioCategory.MUSIC),
                value -> engine.setCategoryGain(AudioCategory.MUSIC, value),
                0.0F,
                1.0F);
        volume.addFloat(
                "effects",
                () -> engine.categoryGain(AudioCategory.EFFECTS),
                value -> engine.setCategoryGain(AudioCategory.EFFECTS, value),
                0.0F,
                1.0F);
        ControlPanel.Section diagnostics = panel.addSection("Diagnostics");
        diagnostics.addText("music", demo::musicState);
        diagnostics.addText("interface effect", demo::effectState);
        ControlPanel.Section view = panel.addSection("View");
        view.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Coordinates independent music and effects playback with simple visual feedback. */
    private static final class MixingDemo {
        private final AudioSource music;
        private final AudioSource effect;
        private final AudioSourcePlayerBinding musicPlayer;
        private final Group musicVisual;
        private final Group effectVisual;

        private float elapsedTime;
        private float effectFlash;

        /** Retains both relative sources and their category visualizations. */
        private MixingDemo(AudioSource music, AudioSource effect, Group musicVisual, Group effectVisual) {
            this.music = music;
            this.effect = effect;
            musicPlayer = new AudioSourcePlayerBinding(music);
            this.musicVisual = musicVisual;
            this.effectVisual = effectVisual;
        }

        /** Updates visual feedback without changing playback state. */
        private void update(float elapsedSeconds) {
            elapsedTime += elapsedSeconds;
            if (music.state() == AudioPlaybackState.PLAYING) {
                musicVisual.rotateY(elapsedSeconds * 0.8F);
                float pulse = 1.0F + (float) Math.sin(elapsedTime * 8.0F) * 0.035F;
                musicVisual.setScale(pulse, pulse, pulse);
            } else {
                musicVisual.setScale(1.0F, 1.0F, 1.0F);
            }
            effectFlash = Math.max(effectFlash - elapsedSeconds * 3.5F, 0.0F);
            float effectScale = 1.0F + effectFlash * 0.18F;
            effectVisual.setScale(effectScale, effectScale, effectScale);
        }

        /** Returns the music source's media-player binding. */
        private AudioSourcePlayerBinding musicPlayer() {
            return musicPlayer;
        }

        /** Restarts the independent non-positional interface effect. */
        private void playInterfaceEffect() {
            effect.stop();
            effect.play();
            effectFlash = 1.0F;
        }

        /** Formats the current music source state. */
        private String musicState() {
            return music.state().name().toLowerCase(Locale.ROOT);
        }

        /** Formats the current interface source state. */
        private String effectState() {
            return effect.state().name().toLowerCase(Locale.ROOT);
        }
    }
}
