/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.animation.AnimationAction;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationMixer;
import io.github.glynch.jscene3d.animation.Interpolation;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.SkinnedMesh;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureColorSpace;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import io.github.glynch.jscene3d.textures.TextureCoordinateSet;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GltfLoaderTest {
    @TempDir
    private Path temporaryDirectory;

    /** Loads external buffers and images into an owned, selected scene. */
    @Test
    void loadsTexturedTriangleWithPbrState() throws IOException {
        Path source = GltfTestAssets.writeTexturedTriangle(temporaryDirectory);

        try (LoadedGltf loaded = GltfLoader.load(source)) {
            Scene scene = loaded.scene();
            Object3D node = scene.children().getFirst();
            Mesh mesh = (Mesh) node.children().getFirst();

            assertTransform(node);
            assertGeometry(mesh.geometry());
            assertMaterial((StandardMaterial) mesh.material());
            assertSharedMeshResources(node, mesh);
            assertThat(loaded.isClosed()).isFalse();
        }
    }

    /** Loads the same public scene abstraction from binary GLB storage. */
    @Test
    void loadsBinaryGlb() throws IOException {
        Path source = GltfTestAssets.writeGlbTriangle(temporaryDirectory);

        try (LoadedGltf loaded = GltfLoader.load(source)) {
            Mesh mesh = (Mesh) loaded.scene().children().getFirst().children().getFirst();
            assertThat(mesh.geometry().vertexCount()).isEqualTo(3);
            assertThat(mesh.geometry().index()).isNull();
            assertThat(mesh.material()).isInstanceOf(StandardMaterial.class);
        }
    }

    /** Converts glTF transform channels into playable typed animation tracks. */
    @Test
    void loadsTransformAnimations() throws IOException {
        Path source = GltfTestAssets.writeAnimatedTriangle(temporaryDirectory);

        try (LoadedGltf loaded = GltfLoader.load(source)) {
            Object3D target = loaded.scene().children().getFirst();
            AnimationClip clip = loaded.animations().getFirst();
            AnimationAction action = new AnimationMixer().action(clip);

            action.setTime(0.5f);

            assertThat(clip.name()).isEqualTo("Transform interpolation");
            assertThat(clip.duration()).isEqualTo(2.0f);
            assertThat(clip.tracks())
                    .extracting(track -> track.interpolation())
                    .containsExactly(Interpolation.LINEAR, Interpolation.STEP, Interpolation.CUBIC_SPLINE);
            assertThat(target.position().x()).isEqualTo(1.0f);
            assertThat(target.position().y()).isEqualTo(2.0f);
            assertThat(target.position().z()).isEqualTo(3.0f);
            assertThat(target.quaternion().w()).isEqualTo(1.0f);
            assertThat(target.scale().x()).isEqualTo(2.0f);
            assertThat(target.scale().y()).isEqualTo(2.0f);
            assertThat(target.scale().z()).isEqualTo(2.0f);
        }
    }

    /** Makes loaded ownership terminal and idempotent. */
    @Test
    void closesOwnedResourcesExactlyOnce() throws IOException {
        LoadedGltf loaded = GltfLoader.load(GltfTestAssets.writeTexturedTriangle(temporaryDirectory));
        Mesh mesh = (Mesh) loaded.scene().children().getFirst().children().getFirst();
        BufferGeometry geometry = mesh.geometry();
        StandardMaterial material = (StandardMaterial) mesh.material();
        Texture texture = material.colorMap().orElseThrow();

        loaded.close();
        loaded.close();

        assertThat(loaded.isClosed()).isTrue();
        assertThat(geometry.isClosed()).isTrue();
        assertThat(material.isClosed()).isTrue();
        assertThat(texture.isClosed()).isTrue();
        assertThatIllegalStateException().isThrownBy(loaded::scene);
        assertThatIllegalStateException().isThrownBy(loaded::animations);
    }

    /** Preserves source-aware diagnostics and validates the public null contract. */
    @Test
    @SuppressWarnings("NullAway")
    void reportsReadFailures() {
        Path missing = temporaryDirectory.resolve("missing.gltf");

        assertThatNullPointerException().isThrownBy(() -> GltfLoader.load(null));
        assertThatThrownBy(() -> GltfLoader.load(missing))
                .isInstanceOf(GltfLoadException.class)
                .extracting(exception -> ((GltfLoadException) exception).source())
                .isEqualTo(missing);
    }

    /** Rejects unsupported required extensions before attempting partial conversion. */
    @Test
    void rejectsRequiredExtensions() throws IOException {
        Path source = GltfTestAssets.writeJson(temporaryDirectory, "extension.gltf", """
                {"asset":{"version":"2.0"},"extensionsUsed":["VENDOR_feature"],
                "extensionsRequired":["VENDOR_feature"],"scenes":[{}]}
                """);

        assertThatThrownBy(() -> GltfLoader.load(source))
                .isInstanceOf(GltfLoadException.class)
                .hasMessageContaining("required extensions")
                .hasMessageContaining("VENDOR_feature");
    }

    /** Loads a decomposable node matrix into controlled JScene3D transform state. */
    @Test
    void loadsNodeMatrix() throws IOException {
        Path source = GltfTestAssets.writeSimpleTriangle(
                temporaryDirectory,
                "matrix.gltf",
                "{\"mesh\":0,\"matrix\":[2,0,0,0,0,3,0,0,0,0,4,0,1,2,3,1]}",
                "{\"attributes\":{\"POSITION\":0},\"material\":0}",
                ",\"materials\":[{\"alphaMode\":\"MASK\",\"alphaCutoff\":0.2}]");

        try (LoadedGltf loaded = GltfLoader.load(source)) {
            Object3D node = loaded.scene().children().getFirst();
            StandardMaterial material =
                    (StandardMaterial) ((Mesh) node.children().getFirst()).material();
            assertTransform(node);
            assertThat(material.alphaMode()).isEqualTo(AlphaMode.MASK);
            assertThat(material.alphaCutoff()).isEqualTo(0.2f);
        }
    }

    /** Applies glTF defaults for a texture without an explicit sampler. */
    @Test
    void appliesDefaultTextureSampler() throws IOException {
        try (LoadedGltf loaded = GltfLoader.load(GltfTestAssets.writeDefaultTextureTriangle(temporaryDirectory))) {
            StandardMaterial material = material(loaded);
            Texture texture = material.colorMap().orElseThrow();
            assertThat(texture.magnificationFilter()).isEqualTo(TextureFilter.LINEAR);
            assertThat(texture.minificationFilter()).isEqualTo(TextureFilter.LINEAR);
            assertThat(texture.horizontalWrap()).isEqualTo(TextureWrap.REPEAT);
            assertThat(texture.verticalWrap()).isEqualTo(TextureWrap.REPEAT);
        }
    }

    /** Allows an asset with no declared scenes to load as an empty scene. */
    @Test
    void loadsEmptyAssetWithoutScenes() throws IOException {
        Path source = GltfTestAssets.writeJson(temporaryDirectory, "empty.gltf", "{\"asset\":{\"version\":\"2.0\"}}");

        try (LoadedGltf loaded = GltfLoader.load(source)) {
            assertThat(loaded.scene().children()).isEmpty();
        }
    }

    /** Rejects primitive modes that cannot be represented by Mesh. */
    @Test
    void rejectsNonTrianglePrimitive() throws IOException {
        Path source = GltfTestAssets.writeSimpleTriangle(
                temporaryDirectory, "lines.gltf", "{\"mesh\":0}", "{\"attributes\":{\"POSITION\":0},\"mode\":1}", "");

        assertUnsupported(source, "non-triangle primitive mode");
    }

    /** Rejects a primitive that has no positions. */
    @Test
    void rejectsMissingPositions() throws IOException {
        Path source = GltfTestAssets.writeSimpleTriangle(
                temporaryDirectory, "missing-position.gltf", "{\"mesh\":0}", "{\"attributes\":{}}", "");

        assertThatThrownBy(() -> GltfLoader.load(source))
                .isInstanceOf(GltfLoadException.class)
                .hasMessageContaining("missing POSITION");
    }

    /** Rejects texture-coordinate sets beyond the two supported glTF core sets. */
    @Test
    void rejectsUnsupportedTextureCoordinateSet() throws IOException {
        GltfTestAssets.writePixel(temporaryDirectory.resolve("secondary.png"));
        Path source = GltfTestAssets.writeSimpleTriangle(
                temporaryDirectory,
                "secondary-uv.gltf",
                "{\"mesh\":0}",
                "{\"attributes\":{\"POSITION\":0},\"material\":0}",
                ",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0,\"texCoord\":2}}}],"
                        + "\"textures\":[{\"source\":0}],\"images\":[{\"uri\":\"secondary.png\"}]");

        assertUnsupported(source, "texture coordinate set 2");
    }

    /** Rejects malformed image data with a loader-owned diagnostic. */
    @Test
    void rejectsUndecodableImage() throws IOException {
        Files.writeString(temporaryDirectory.resolve("invalid.png"), "not an image");
        Path source = GltfTestAssets.writeSimpleTriangle(
                temporaryDirectory,
                "invalid-image.gltf",
                "{\"mesh\":0}",
                "{\"attributes\":{\"POSITION\":0},\"material\":0}",
                ",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0}}}],"
                        + "\"textures\":[{\"source\":0}],\"images\":[{\"uri\":\"invalid.png\"}]");

        assertThatThrownBy(() -> GltfLoader.load(source))
                .isInstanceOf(GltfLoadException.class)
                .hasMessageContaining("decode glTF image");
    }

    /** Converts skin joints, inverse bind matrices, and four-influence vertex attributes. */
    @Test
    void loadsSkins() throws IOException {
        Path source = GltfTestAssets.writeSkinnedTriangle(temporaryDirectory);

        try (LoadedGltf loaded = GltfLoader.load(source)) {
            SkinnedMesh mesh = (SkinnedMesh)
                    loaded.scene().children().getFirst().children().getFirst();

            assertThat(mesh.skeleton().jointCount()).isOne();
            assertThat(mesh.geometry().attribute(BufferGeometry.JOINTS)).isNotNull();
            assertThat(mesh.geometry().attribute(BufferGeometry.WEIGHTS)).isNotNull();
        }
    }

    /** Decodes a Draco-compressed primitive through the public glTF loader. */
    @Test
    void loadsDracoCompressedPrimitive() throws Exception {
        Path source = GltfTestAssets.writeDracoTriangle(temporaryDirectory);

        try (LoadedGltf loaded = GltfLoader.load(source)) {
            Mesh mesh = (Mesh) loaded.scene().children().getFirst().children().getFirst();
            BufferAttribute positions =
                    Objects.requireNonNull(mesh.geometry().attribute(BufferGeometry.POSITION), "positions");

            assertThat(mesh.geometry().vertexCount()).isEqualTo(3);
            assertThat(Objects.requireNonNull(mesh.geometry().index(), "index").toArray())
                    .containsExactly(0, 1, 2);
            assertThat(positions.toArray()).hasSize(9);
        }
    }

    /** Rejects embedded cameras while camera import is outside the initial profile. */
    @Test
    void rejectsEmbeddedCameras() throws IOException {
        Path source = GltfTestAssets.writeSimpleTriangle(
                temporaryDirectory,
                "camera.gltf",
                "{\"mesh\":0,\"camera\":0}",
                "{\"attributes\":{\"POSITION\":0}}",
                ",\"cameras\":[{\"type\":\"perspective\",\"perspective\":{\"yfov\":1,\"znear\":0.1}}]");

        assertUnsupported(source, "embedded cameras");
    }

    /** Verifies the selected non-first scene and explicit node transform. */
    private static void assertTransform(Object3D node) {
        assertThat(node.position().x()).isEqualTo(1.0f);
        assertThat(node.position().y()).isEqualTo(2.0f);
        assertThat(node.position().z()).isEqualTo(3.0f);
        assertThat(node.scale().x()).isEqualTo(2.0f);
        assertThat(node.scale().y()).isEqualTo(3.0f);
        assertThat(node.scale().z()).isEqualTo(4.0f);
    }

    /** Verifies decoded float, normalized integer, colour, and index accessors. */
    private static void assertGeometry(BufferGeometry geometry) {
        BufferAttribute uv = Objects.requireNonNull(geometry.attribute(BufferGeometry.UV), "uv");
        BufferAttribute uv1 = Objects.requireNonNull(geometry.attribute(BufferGeometry.UV1), "uv1");
        BufferAttribute color = Objects.requireNonNull(geometry.attribute(BufferGeometry.COLOR), "color");
        assertThat(geometry.vertexCount()).isEqualTo(3);
        assertThat(Objects.requireNonNull(geometry.index(), "index").count()).isEqualTo(3);
        assertThat(geometry.attribute(BufferGeometry.NORMAL)).isNotNull();
        assertThat(uv).isNotNull();
        assertThat(uv.value(1, 0)).isEqualTo(1.0f);
        assertThat(uv.value(2, 0)).isCloseTo(0.5000076f, within(0.000001f));
        assertThat(uv1.toArray()).containsExactly(uv.toArray());
        assertThat(color).isNotNull();
        assertThat(color.itemSize()).isEqualTo(4);
        assertThat(color.value(1, 3)).isCloseTo(128.0f / 255.0f, within(0.000001f));
    }

    /** Verifies factors, state, colour spaces, sampler mapping, and texture deduplication. */
    private static void assertMaterial(StandardMaterial material) {
        Texture color = material.colorMap().orElseThrow();
        Texture data = material.normalMap().orElseThrow();
        assertMaterialFactors(material);
        assertThat(material.usesVertexColors()).isTrue();
        assertThat(material.alphaMode()).isEqualTo(AlphaMode.BLEND);
        assertThat(material.side()).isEqualTo(MaterialSide.DOUBLE);
        assertThat(color.colorSpace()).isEqualTo(TextureColorSpace.SRGB);
        assertThat(data.colorSpace()).isEqualTo(TextureColorSpace.LINEAR);
        assertThat(color.coordinateOrigin()).isEqualTo(TextureCoordinateOrigin.TOP_LEFT);
        assertThat(data.coordinateOrigin()).isEqualTo(TextureCoordinateOrigin.TOP_LEFT);
        assertThat(material.emissiveMap()).containsSame(color);
        assertThat(material.metalnessRoughnessMap()).containsSame(data);
        assertThat(material.occlusionMap()).containsSame(data);
        assertThat(material.occlusionMapCoordinateSet()).isEqualTo(TextureCoordinateSet.SECONDARY);
        assertSampler(color);
    }

    /** Verifies scalar and colour factors independently of texture state. */
    private static void assertMaterialFactors(StandardMaterial material) {
        assertThat(material.color()).isEqualTo(Color.linear(0.25f, 0.5f, 0.75f));
        assertThat(material.opacity()).isEqualTo(0.6f);
        assertThat(material.metalness()).isEqualTo(0.8f);
        assertThat(material.roughness()).isEqualTo(0.3f);
        assertThat(material.emissive()).isEqualTo(Color.linear(0.1f, 0.2f, 0.3f));
        assertThat(material.occlusionStrength()).isEqualTo(0.7f);
        assertThat(material.normalScale(new Vector2f())).isEqualTo(new Vector2f(0.4f));
        assertThat(material.alphaCutoff()).isEqualTo(0.35f);
    }

    /** Verifies sampler constant mapping. */
    private static void assertSampler(Texture texture) {
        assertThat(texture.magnificationFilter()).isEqualTo(TextureFilter.NEAREST);
        assertThat(texture.minificationFilter()).isEqualTo(TextureFilter.LINEAR_MIPMAP_NEAREST);
        assertThat(texture.horizontalWrap()).isEqualTo(TextureWrap.MIRRORED_REPEAT);
        assertThat(texture.verticalWrap()).isEqualTo(TextureWrap.CLAMP_TO_EDGE);
    }

    /** Verifies hierarchy conversion and resource deduplication across mesh instances. */
    private static void assertSharedMeshResources(Object3D node, Mesh firstMesh) {
        Object3D childNode = node.children().get(1);
        Mesh secondMesh = (Mesh) childNode.children().getFirst();
        assertThat(childNode.position().y()).isEqualTo(1.0f);
        assertThat(secondMesh.geometry()).isSameAs(firstMesh.geometry());
        assertThat(secondMesh.material()).isSameAs(firstMesh.material());
    }

    /** Returns the first mesh's StandardMaterial from a loaded one-node fixture. */
    private static StandardMaterial material(LoadedGltf loaded) {
        Object3D node = loaded.scene().children().getFirst();
        return (StandardMaterial) ((Mesh) node.children().getFirst()).material();
    }

    /** Asserts a source-aware unsupported-capability result. */
    private static void assertUnsupported(Path source, String capability) {
        assertThatThrownBy(() -> GltfLoader.load(source))
                .isInstanceOf(GltfLoadException.class)
                .hasMessageContaining("Unsupported glTF capability")
                .hasMessageContaining(capability);
    }

    /** Creates an AssertJ absolute-offset for float comparisons. */
    private static org.assertj.core.data.Offset<Float> within(float value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
