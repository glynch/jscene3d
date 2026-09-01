/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.Interpolation;
import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.objects.Mesh;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies that bundled showcase models remain inside the supported glTF capability profile. */
final class BundledModelCompatibilityTest {
    private static final String WATER_BOTTLE_RESOURCE =
            "/io/github/glynch/jscene3d/examples/water-bottle/WaterBottle.glb";
    private static final String BOOM_BOX_RESOURCE = "/io/github/glynch/jscene3d/examples/boom-box/BoomBox.glb";
    private static final String INTERPOLATION_TEST_RESOURCE =
            "/io/github/glynch/jscene3d/examples/interpolation-test/InterpolationTest.glb";
    private static final String FOX_RESOURCE = "/io/github/glynch/jscene3d/examples/fox/Fox.glb";
    private static final String SOLDIER_RESOURCE = "/io/github/glynch/jscene3d/examples/soldier/Soldier.glb";

    /** Ensures Soldier retains the four skeletal clips used by the Three.js reference example. */
    @Test
    void loadsSoldierAnimationClips() {
        try (LoadedGltf loaded = GltfLoader.load(path(getClass(), SOLDIER_RESOURCE))) {
            assertThat(loaded.animations())
                    .extracting(AnimationClip::name)
                    .containsExactlyInAnyOrder("Idle", "Run", "TPose", "Walk");

            AnimationMixer mixer = new AnimationMixer();
            AnimationAction idle =
                    mixer.action(requireClip(loaded.animations(), "Idle")).play();
            AnimationAction walk = mixer.action(requireClip(loaded.animations(), "Walk"));

            mixer.crossFade(idle, walk, 0.5f);
            mixer.update(0.25f);

            assertThat(idle.effectiveWeight()).isEqualTo(0.5f);
            assertThat(walk.effectiveWeight()).isEqualTo(0.5f);
        }
    }

    /** Ensures Fox retains the compatible idle, walking, and running skeletal clips. */
    @Test
    void loadsFoxAnimationClips() {
        try (LoadedGltf loaded = GltfLoader.load(path(getClass(), FOX_RESOURCE))) {
            assertThat(loaded.animations())
                    .extracting(AnimationClip::name)
                    .containsExactlyInAnyOrder("Survey", "Walk", "Run");

            AnimationMixer mixer = new AnimationMixer();
            AnimationClip survey = loaded.animations().stream()
                    .filter(clip -> clip.name().equals("Survey"))
                    .findFirst()
                    .orElseThrow();
            AnimationClip walk = loaded.animations().stream()
                    .filter(clip -> clip.name().equals("Walk"))
                    .findFirst()
                    .orElseThrow();
            AnimationAction surveyAction = mixer.action(survey).play();
            AnimationAction walkAction = mixer.action(walk);

            mixer.crossFade(surveyAction, walkAction, 0.5f);
            mixer.update(0.25f);

            assertThat(surveyAction.effectiveWeight()).isEqualTo(0.5f);
            assertThat(walkAction.effectiveWeight()).isEqualTo(0.5f);
        }
    }

    /** Ensures the official interpolation fixture retains all imported transform clips. */
    @Test
    void loadsInterpolationTestAnimations() {
        try (LoadedGltf loaded = GltfLoader.load(path(getClass(), INTERPOLATION_TEST_RESOURCE))) {
            assertThat(loaded.animations())
                    .hasSize(9)
                    .extracting(AnimationClip::name)
                    .containsExactly(
                            "Step Scale",
                            "Linear Scale",
                            "CubicSpline Scale",
                            "Step Rotation",
                            "CubicSpline Rotation",
                            "Linear Rotation",
                            "Step Translation",
                            "CubicSpline Translation",
                            "Linear Translation");
            assertThat(loaded.animations())
                    .flatExtracting(AnimationClip::tracks)
                    .extracting(track -> track.interpolation())
                    .contains(Interpolation.STEP, Interpolation.LINEAR, Interpolation.CUBIC_SPLINE);

            AnimationMixer mixer = new AnimationMixer();
            loaded.animations().forEach(clip -> mixer.action(clip).play());
            mixer.update(0.75f);

            assertThat(mixer.action(loaded.animations().getFirst()).time()).isEqualTo(0.75f);
        }
    }

    /** Ensures Water Bottle exercises every currently supported StandardMaterial texture role. */
    @Test
    void loadsWaterBottleMaterialMaps() {
        try (LoadedGltf loaded = GltfLoader.load(path(getClass(), WATER_BOTTLE_RESOURCE))) {
            List<Mesh> meshes = new ArrayList<>();
            loaded.scene().traverse(object -> {
                if (object instanceof Mesh mesh) {
                    meshes.add(mesh);
                }
            });

            assertThat(meshes).singleElement().satisfies(mesh -> {
                assertThat(mesh.material()).isInstanceOf(StandardMaterial.class);
                StandardMaterial material = (StandardMaterial) mesh.material();
                assertThat(material.colorMap()).isPresent();
                assertThat(material.metalnessRoughnessMap()).isPresent();
                assertThat(material.normalMap()).isPresent();
                assertThat(material.occlusionMap()).isPresent();
                assertThat(material.emissiveMap()).isPresent();
            });
        }
    }

    /** Ensures Boom Box preserves its metallic, occlusion, normal, and glowing-panel material maps. */
    @Test
    void loadsBoomBoxMaterialMaps() {
        try (LoadedGltf loaded = GltfLoader.load(path(getClass(), BOOM_BOX_RESOURCE))) {
            List<Mesh> meshes = new ArrayList<>();
            loaded.scene().traverse(object -> {
                if (object instanceof Mesh mesh) {
                    meshes.add(mesh);
                }
            });

            assertThat(meshes).singleElement().satisfies(mesh -> {
                assertThat(mesh.material()).isInstanceOf(StandardMaterial.class);
                StandardMaterial material = (StandardMaterial) mesh.material();
                assertThat(material.colorMap()).isPresent();
                assertThat(material.metalnessRoughnessMap()).isPresent();
                assertThat(material.normalMap()).isPresent();
                assertThat(material.occlusionMap()).isPresent();
                assertThat(material.emissiveMap()).isPresent();
            });
        }
    }

    /** Finds one required animation fixture clip by exact name. */
    private static AnimationClip requireClip(List<AnimationClip> clips, String name) {
        return clips.stream()
                .filter(clip -> clip.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing required animation clip: " + name));
    }
}
