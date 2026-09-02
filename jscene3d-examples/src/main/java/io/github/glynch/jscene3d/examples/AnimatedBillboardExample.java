/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.animation.LoopMode;
import io.github.glynch.jscene3d.animation.SpriteAnimation;
import io.github.glynch.jscene3d.animation.SpriteAnimationEvent;
import io.github.glynch.jscene3d.animation.SpriteAnimationSet;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.loaders.TextureLoader;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.AnimatedBillboard;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureRegion;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Demonstrates named sprite animations backed by one shared CC0 character atlas. */
public final class AnimatedBillboardExample {
    private static final String ATLAS_RESOURCE =
            "/META-INF/jscene3d/examples/assets/kenney/new-platformer-pack/characters.png";
    private static final int FRAME_SIZE = 128;
    private static final int ATLAS_SIZE = 902;
    private static final List<ControlPanel.Choice<CharacterStyle>> CHARACTER_CHOICES = List.of(
            new ControlPanel.Choice<>(CharacterStyle.GREEN, "green explorer"),
            new ControlPanel.Choice<>(CharacterStyle.PINK, "pink explorer"),
            new ControlPanel.Choice<>(CharacterStyle.PURPLE, "purple explorer"));
    private static final List<ControlPanel.Choice<String>> ANIMATION_CHOICES = List.of(
            new ControlPanel.Choice<>("idle", "idle"),
            new ControlPanel.Choice<>("walk", "walk"),
            new ControlPanel.Choice<>("celebrate", "celebrate once"));

    /** Prevents instantiation of this example entry point. */
    private AnimatedBillboardExample() {
        throw new AssertionError("AnimatedBillboardExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Animated Billboards", AnimatedBillboardExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        Texture atlas = loadAtlas();
        BasicMaterial spriteMaterial = createSpriteMaterial(atlas);
        AnimatedBillboard hero =
                createCharacter(spriteMaterial, createAnimationSet(CharacterStyle.GREEN), "walk", 1.0f);
        AnimatedBillboard companion =
                createCharacter(spriteMaterial, createAnimationSet(CharacterStyle.PINK), "walk", 0.65f);
        AnimatedBillboard observer =
                createCharacter(spriteMaterial, createAnimationSet(CharacterStyle.PURPLE), "idle", 1.0f);
        placeCharacter(hero, 0.0f, 0.14f, 0.5f, 2.8f);
        placeCharacter(companion, -3.0f, 0.14f, -0.7f, 2.4f);
        placeCharacter(observer, 3.0f, 0.14f, -0.7f, 2.4f);
        companion.setFrameAndProgress(1, 0.5f);

        BufferGeometry groundGeometry = PlaneGeometry.create(14.0f, 10.0f);
        BasicMaterial groundMaterial = new BasicMaterial(Color.srgb(0x14283b));
        groundMaterial.setSide(MaterialSide.DOUBLE);
        BufferGeometry platformGeometry = CylinderGeometry.create(1.2f, 0.28f);
        BasicMaterial heroPlatformMaterial = new BasicMaterial(Color.srgb(0x227b79));
        BasicMaterial companionPlatformMaterial = new BasicMaterial(Color.srgb(0x944765));
        BasicMaterial observerPlatformMaterial = new BasicMaterial(Color.srgb(0x63558f));
        Scene scene = createScene();
        addGround(scene, groundGeometry, groundMaterial);
        addPlatform(scene, platformGeometry, heroPlatformMaterial, 0.0f, 0.5f);
        addPlatform(scene, platformGeometry, companionPlatformMaterial, -3.0f, -0.7f);
        addPlatform(scene, platformGeometry, observerPlatformMaterial, 3.0f, -0.7f);
        scene.add(hero);
        scene.add(companion);
        scene.add(observer);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_FOUR, context.aspectRatio(), 0.1f, 50.0f);
        camera.setPosition(6.5f, 4.3f, 9.0f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setTarget(0.0f, 1.3f, 0.0f);
        controls.setDistanceLimits(5.0f, 22.0f);
        controls.setDampingEnabled(true);
        controls.update();
        controls.saveState();

        PlaybackControls playback = new PlaybackControls(hero, companion, observer);
        SceneExample example = new SceneExample(context, scene, camera, controls);
        ownResources(
                example,
                atlas,
                spriteMaterial,
                groundGeometry,
                groundMaterial,
                platformGeometry,
                heroPlatformMaterial,
                companionPlatformMaterial,
                observerPlatformMaterial,
                hero,
                companion,
                observer);
        ControlPanel panel = example.addOverlay(createPanel(context, playback, controls));
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            updateBillboards(frame.elapsedSeconds(), hero, companion, observer);
            panel.update();
        });
        return example;
    }

    /** Creates one palette-specific animation set from the shared Kenney atlas. */
    private static SpriteAnimationSet createAnimationSet(CharacterStyle style) {
        return switch (style) {
            case GREEN ->
                createAnimationSet(
                        region(645, 129),
                        region(0, 258),
                        region(258, 258),
                        region(387, 258),
                        region(129, 258),
                        region(774, 129));
            case PINK ->
                createAnimationSet(
                        region(0, 387),
                        region(258, 387),
                        region(516, 387),
                        region(645, 387),
                        region(387, 387),
                        region(129, 387));
            case PURPLE ->
                createAnimationSet(
                        region(258, 516),
                        region(516, 516),
                        region(774, 516),
                        region(0, 645),
                        region(645, 516),
                        region(387, 516));
        };
    }

    /** Creates named playback resources from one character variant's atlas regions. */
    private static SpriteAnimationSet createAnimationSet(
            TextureRegion front,
            TextureRegion idlePose,
            TextureRegion walkA,
            TextureRegion walkB,
            TextureRegion jump,
            TextureRegion hit) {
        SpriteAnimation idle =
                SpriteAnimation.uniform("idle", List.of(idlePose, front, idlePose), 2.0f, LoopMode.REPEAT);
        SpriteAnimation walk = SpriteAnimation.uniform("walk", List.of(walkA, walkB), 6.0f, LoopMode.REPEAT);
        SpriteAnimation celebrate =
                SpriteAnimation.uniform("celebrate", List.of(front, jump, hit, jump, front), 6.0f, LoopMode.ONCE);
        return new SpriteAnimationSet(List.of(idle, walk, celebrate));
    }

    /** Creates one bottom-centred upright animated sprite. */
    private static AnimatedBillboard createCharacter(
            BasicMaterial material, SpriteAnimationSet animationSet, String animation, float speed) {
        AnimatedBillboard billboard = new AnimatedBillboard(material, animationSet);
        billboard.setAlignment(BillboardAlignment.CYLINDRICAL);
        billboard.setAnchor(0.5f, 0.0f);
        billboard.setPlaybackSpeed(speed);
        billboard.play(animation);
        return billboard;
    }

    /** Positions and uniformly sizes one character without mixing placement into resource setup. */
    private static void placeCharacter(AnimatedBillboard billboard, float x, float y, float z, float size) {
        billboard.setPosition(x, y, z);
        billboard.setScale(size, size, 1.0f);
    }

    /** Creates the moonbase-inspired gallery background. */
    private static Scene createScene() {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x071525));
        return scene;
    }

    /** Adds the horizontal reference plane slightly below the display platforms. */
    private static void addGround(Scene scene, BufferGeometry geometry, BasicMaterial material) {
        Mesh ground = new Mesh(geometry, material);
        ground.rotateX(-PI_OVER_TWO);
        ground.setPosition(0.0f, -0.15f, 0.0f);
        scene.add(ground);
    }

    /** Adds one colour-coded display platform beneath a character variant. */
    private static void addPlatform(Scene scene, BufferGeometry geometry, BasicMaterial material, float x, float z) {
        Mesh platform = new Mesh(geometry, material);
        platform.setPosition(x, 0.0f, z);
        scene.add(platform);
    }

    /** Creates controls resembling the runtime properties a future inspector will edit. */
    private static ControlPanel createPanel(ExampleContext context, PlaybackControls playback, OrbitControls controls) {
        ControlPanel panel = new ControlPanel(context.window(), "Animated Billboards");
        ControlPanel.Section resource = panel.addSection("Animation resource");
        resource.addText("animations", () -> "idle / walk / celebrate");
        resource.addText("artwork", () -> "Kenney New Platformer Pack");
        resource.addText("storage", () -> "one atlas, three animation sets");
        ControlPanel.Section playbackSection = panel.addSection("Selected billboard");
        playbackSection.addChoice(
                "character", playback::selectedCharacter, playback::selectCharacter, CHARACTER_CHOICES);
        playbackSection.addChoice("animation", playback::animation, playback::setAnimation, ANIMATION_CHOICES);
        playbackSection.addFloat("speed", playback::speed, playback::setSpeed, -2.0f, 3.0f);
        playbackSection.addButton("play / resume", playback::play);
        playbackSection.addButton("pause", playback::pause);
        playbackSection.addButton("stop", playback::stop);
        ControlPanel.Section group = panel.addSection("All billboards");
        group.addButton("play / resume all", playback::playAll);
        group.addButton("pause all", playback::pauseAll);
        group.addButton("stop all", playback::stopAll);
        ControlPanel.Section state = panel.addSection("Live state and signals");
        state.addText("playback", playback::state);
        state.addText("frame", playback::frame);
        state.addText("last event", playback::lastEvent);
        ControlPanel.Section view = panel.addSection("View");
        view.addText("camera", () -> "drag / scroll");
        view.addButton("reset camera", controls::reset);
        return panel;
    }

    /** Advances independently owned playback state for all sprites sharing the resource. */
    private static void updateBillboards(float elapsedSeconds, AnimatedBillboard... billboards) {
        for (AnimatedBillboard billboard : billboards) {
            billboard.update(elapsedSeconds);
        }
    }

    /** Registers resources in dependency order so dependants close first. */
    private static void ownResources(SceneExample example, AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            example.own(resource);
        }
    }

    /** Creates a smoothly blended material using the shared sprite atlas. */
    private static BasicMaterial createSpriteMaterial(Texture atlas) {
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(atlas);
        material.setAlphaMode(AlphaMode.BLEND);
        material.setDepthWriteEnabled(false);
        return material;
    }

    /** Loads the required archive-safe CC0 character atlas. */
    private static Texture loadAtlas() {
        return TextureLoader.load(path(AnimatedBillboardExample.class.getResource(ATLAS_RESOURCE), ATLAS_RESOURCE));
    }

    /** Returns one top-row-first region from Kenney's documented atlas metadata. */
    private static TextureRegion region(int x, int y) {
        return TextureRegion.fromPixels(x, y, FRAME_SIZE, FRAME_SIZE, ATLAS_SIZE, ATLAS_SIZE);
    }

    /** Character colour variants selected from the shared atlas. */
    private enum CharacterStyle {
        GREEN,
        PINK,
        PURPLE
    }

    /** GUI-facing adapter around one animated billboard's explicit runtime state. */
    private static final class PlaybackControls {
        private final EnumMap<CharacterStyle, AnimatedBillboard> billboards = new EnumMap<>(CharacterStyle.class);
        private final EnumMap<CharacterStyle, String> lastEvents = new EnumMap<>(CharacterStyle.class);

        private CharacterStyle selectedCharacter = CharacterStyle.GREEN;

        /** Retains all independently controlled billboards and observes their runtime events. */
        private PlaybackControls(AnimatedBillboard green, AnimatedBillboard pink, AnimatedBillboard purple) {
            register(CharacterStyle.GREEN, green);
            register(CharacterStyle.PINK, pink);
            register(CharacterStyle.PURPLE, purple);
        }

        /** Returns the character currently targeted by the selected-billboard controls. */
        private CharacterStyle selectedCharacter() {
            return selectedCharacter;
        }

        /** Changes which independently playing character the selected-billboard controls target. */
        private void selectCharacter(CharacterStyle character) {
            selectedCharacter = character;
        }

        /** Returns the selected named animation. */
        private String animation() {
            return selectedBillboard().animationName();
        }

        /** Selects and starts one named animation. */
        private void setAnimation(String animation) {
            selectedBillboard().play(animation);
        }

        /** Returns the current playback multiplier. */
        private float speed() {
            return selectedBillboard().playbackSpeed();
        }

        /** Applies a finite playback multiplier. */
        private void setSpeed(float speed) {
            selectedBillboard().setPlaybackSpeed(speed);
        }

        /** Starts or resumes playback. */
        private void play() {
            selectedBillboard().play();
        }

        /** Pauses playback without losing frame progress. */
        private void pause() {
            selectedBillboard().pause();
        }

        /** Stops playback and restores the first frame. */
        private void stop() {
            selectedBillboard().stop();
        }

        /** Starts or resumes every billboard without changing its selected animation. */
        private void playAll() {
            billboards.values().forEach(AnimatedBillboard::play);
        }

        /** Pauses every billboard without losing its frame progress. */
        private void pauseAll() {
            billboards.values().forEach(AnimatedBillboard::pause);
        }

        /** Stops every billboard and restores each selected animation's first frame. */
        private void stopAll() {
            billboards.values().forEach(AnimatedBillboard::stop);
        }

        /** Returns a concise live playback state. */
        private String state() {
            AnimatedBillboard billboard = selectedBillboard();
            if (!billboard.isRunning()) {
                return "stopped";
            }
            return billboard.isPaused() ? "paused" : "playing";
        }

        /** Returns the selected frame and within-frame progress. */
        private String frame() {
            AnimatedBillboard billboard = selectedBillboard();
            return String.format(
                    Locale.ROOT, "%d at %.0f%%", billboard.frameIndex(), billboard.frameProgress() * 100.0f);
        }

        /** Returns the latest emitted runtime event. */
        private String lastEvent() {
            return Objects.requireNonNull(lastEvents.get(selectedCharacter));
        }

        /** Registers one character's independently owned playback and event state. */
        private void register(CharacterStyle character, AnimatedBillboard billboard) {
            billboards.put(character, billboard);
            lastEvents.put(character, "none yet");
            billboard.addAnimationListener(event -> handleAnimationEvent(character, event));
        }

        /** Returns the billboard selected through the character choice control. */
        private AnimatedBillboard selectedBillboard() {
            return Objects.requireNonNull(billboards.get(selectedCharacter));
        }

        /** Handles one event in a compact inspector-friendly form. */
        private void handleAnimationEvent(CharacterStyle character, SpriteAnimationEvent event) {
            lastEvents.put(
                    character,
                    String.format(
                            Locale.ROOT,
                            "%s: frame %d",
                            event.type().name().toLowerCase(Locale.ROOT),
                            event.frameIndex()));
        }
    }
}
