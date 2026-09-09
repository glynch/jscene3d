/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.ResourceContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureColorSpace;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** Exercises the complete version-one spatial resource storage boundary. */
final class Spatial3dResourceTest {
    private static final ResourceReference PAYLOAD = ResourceReference.imported("model/payloads/triangle.mesh");

    /** Round-trips ordered attributes, indices, and an explicit draw range through the runtime loader. */
    @Test
    void roundTripsMeshResource() throws IOException {
        BufferGeometry source = new BufferGeometry();
        source.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(new float[] {-1.0F, -1.0F, 0.0F, 1.0F, -1.0F, 0.0F, 0.0F, 1.0F, 0.0F}, 3));
        source.setAttribute(BufferGeometry.UV, BufferAttribute.of(new float[] {0.0F, 0.0F, 1.0F, 0.0F, 0.5F, 1.0F}, 2));
        source.setIndex(IndexBuffer.of(new int[] {0, 1, 2}));
        source.setDrawRange(0, 3);
        ByteArrayOutputStream definitionOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream payloadOutput = new ByteArrayOutputStream();

        Spatial3dResourceWriter.writeMesh(definitionOutput, payloadOutput, source, PAYLOAD);
        RuntimeResourceLoader<Mesh3dResource> loader = meshLoader();
        ResourceDefinition definition = new ResourceDefinition(
                URI.create("import:model/resources/triangle"),
                Spatial3dDescriptors.meshResourceType(),
                Map.of("payload", new ProjectValue.ReferenceValue(PAYLOAD)));
        Mesh3dResource resource =
                loader.load(definition, reference -> new ByteArrayInputStream(payloadOutput.toByteArray()));

        assertThat(new String(definitionOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("\"type\" : \"io.github.glynch.jscene3d.spatial3d/mesh-resource\"")
                .contains("\"$ref\" : \"import:model/payloads/triangle.mesh\"");
        assertThat(resource.geometry().attributes().keySet()).containsExactly("position", "uv");
        assertThat(Objects.requireNonNull(resource.geometry().attribute(BufferGeometry.POSITION))
                        .toArray())
                .containsExactly(Objects.requireNonNull(source.attribute(BufferGeometry.POSITION))
                        .toArray());
        assertThat(Objects.requireNonNull(resource.geometry().index()).toArray())
                .containsExactly(0, 1, 2);
        assertThat(resource.geometry().hasExplicitDrawRange()).isTrue();
        resource.close();
        assertThat(resource.isClosed()).isTrue();
        source.close();
    }

    /** Round-trips every texture-free material property and ownership state. */
    @Test
    void roundTripsMaterialResource() throws IOException {
        StandardMaterial source = new StandardMaterial(Color.linear(0.25F, 0.5F, 0.75F));
        source.setMetalness(0.8F);
        source.setRoughness(0.3F);
        source.setEmissive(Color.linear(0.1F, 0.2F, 0.3F));
        source.setEmissiveIntensity(2.0F);
        source.setOpacity(0.6F);
        source.setAlphaMode(AlphaMode.BLEND);
        source.setAlphaCutoff(0.2F);
        source.setSide(MaterialSide.DOUBLE);
        source.setUsesVertexColors(true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Spatial3dResourceWriter.writeMaterial(output, source);
        Material3dResource resource = materialLoader().load(materialDefinition(), reference -> {
            throw new AssertionError("material has no payload");
        });
        StandardMaterial loaded = (StandardMaterial) resource.material();

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .contains("standard-material-resource", "\"alpha-mode\" : \"blend\"");
        assertThat(loaded.color()).isEqualTo(source.color());
        assertThat(loaded.metalness()).isEqualTo(0.8F);
        assertThat(loaded.roughness()).isEqualTo(0.3F);
        assertThat(loaded.emissive()).isEqualTo(source.emissive());
        assertThat(loaded.emissiveIntensity()).isEqualTo(2.0F);
        assertThat(loaded.opacity()).isEqualTo(0.6F);
        assertThat(loaded.alphaMode()).isEqualTo(AlphaMode.BLEND);
        assertThat(loaded.alphaCutoff()).isEqualTo(0.2F);
        assertThat(loaded.side()).isEqualTo(MaterialSide.DOUBLE);
        assertThat(loaded.usesVertexColors()).isTrue();
        resource.close();
        source.close();
    }

    /** Round-trips a shared texture and retains it for the lifetime of an unlit material resource. */
    @Test
    void roundTripsTextureBackedBasicMaterial() throws IOException {
        byte opaque = (byte) 0xff;
        Texture sourceTexture = Texture.baseColor(2, 1, new byte[] {opaque, 0, 0, opaque, 0, opaque, 0, opaque});
        sourceTexture.setCoordinateOrigin(TextureCoordinateOrigin.TOP_LEFT);
        sourceTexture.setHorizontalWrap(TextureWrap.REPEAT);
        sourceTexture.setVerticalWrap(TextureWrap.MIRRORED_REPEAT);
        sourceTexture.setMinificationFilter(TextureFilter.NEAREST_MIPMAP_NEAREST);
        sourceTexture.setMagnificationFilter(TextureFilter.NEAREST);
        ByteArrayOutputStream textureDefinitionOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream texturePayloadOutput = new ByteArrayOutputStream();
        ResourceReference texturePayload = ResourceReference.imported("model/payloads/checker.rgba8");
        Spatial3dResourceWriter.writeTexture(
                textureDefinitionOutput, texturePayloadOutput, sourceTexture, texturePayload);

        Texture3dResource textureResource = textureLoader()
                .load(
                        textureDefinition(texturePayload),
                        reference -> new ByteArrayInputStream(texturePayloadOutput.toByteArray()));
        BasicMaterial sourceMaterial = new BasicMaterial(Color.linear(0.25F, 0.5F, 0.75F));
        sourceMaterial.setColorMap(sourceTexture);
        sourceMaterial.setAlphaMode(AlphaMode.MASK);
        ByteArrayOutputStream materialOutput = new ByteArrayOutputStream();
        ResourceReference textureReference = ResourceReference.imported("model/resources/checker");
        Spatial3dResourceWriter.writeBasicMaterial(materialOutput, sourceMaterial, textureReference);
        AtomicBoolean released = new AtomicBoolean();

        Material3dResource materialResource = basicMaterialLoader()
                .load(basicMaterialDefinition(textureReference), new ResourceContentStub(textureResource, released));
        BasicMaterial loadedMaterial = (BasicMaterial) materialResource.material();

        assertThat(new String(textureDefinitionOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("rgba8-texture-resource", "\"horizontal-wrap\" : \"repeat\"");
        assertThat(textureResource.texture().width()).isEqualTo(2);
        assertThat(textureResource.texture().coordinateOrigin()).isEqualTo(TextureCoordinateOrigin.TOP_LEFT);
        assertThat(new String(materialOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("basic-material-resource", "\"$ref\" : \"import:model/resources/checker\"");
        assertThat(loadedMaterial.color()).isEqualTo(sourceMaterial.color());
        assertThat(loadedMaterial.alphaMode()).isEqualTo(AlphaMode.MASK);
        assertThat(loadedMaterial.colorMap()).containsSame(textureResource.texture());
        materialResource.close();
        assertThat(released).isTrue();
        assertThat(textureResource.isClosed()).isTrue();
        sourceMaterial.close();
        sourceTexture.close();
    }

    /** Round-trips an unlit material which deliberately has no texture dependency. */
    @Test
    void roundTripsTextureFreeBasicMaterial() throws IOException {
        BasicMaterial source = new BasicMaterial(Color.linear(0.2F, 0.4F, 0.6F));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Spatial3dResourceWriter.writeBasicMaterial(output, source);
        ResourceDefinition definition = basicMaterialDefinition();

        Material3dResource resource = basicMaterialLoader().load(definition, reference -> {
            throw new AssertionError("texture-free material must not acquire content");
        });
        BasicMaterial loaded = (BasicMaterial) resource.material();

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).doesNotContain("color-map");
        assertThat(loaded.colorMap()).isEmpty();
        resource.close();
        source.close();
    }

    /** Enforces texture and basic-material writer preconditions without leaking their inputs. */
    @Test
    void rejectsMismatchedBasicMaterialTextureReferences() {
        Texture texture = Texture.baseColor(1, 1, new byte[] {0, 0, 0, (byte) 0xff});
        BasicMaterial textured = new BasicMaterial();
        textured.setColorMap(texture);
        BasicMaterial textureFree = new BasicMaterial();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ResourceReference reference = ResourceReference.imported("model/resources/checker");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Spatial3dResourceWriter.writeBasicMaterial(output, textured))
                .withMessageContaining("color-map reference is required");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Spatial3dResourceWriter.writeBasicMaterial(output, textureFree, reference))
                .withMessageContaining("must have a color map");

        textured.close();
        textureFree.close();
        texture.close();
    }

    /** Reconstructs linear texture data and enforces terminal texture-resource ownership. */
    @Test
    void loadsLinearTextureAndEnforcesOwnership() throws IOException {
        ResourceReference payload = ResourceReference.imported("model/payloads/data.rgba8");
        ResourceDefinition definition = textureDefinition(payload, 1, 1, "linear");
        byte[] pixels = new byte[] {1, 2, 3, 4};

        Texture3dResource resource = textureLoader().load(definition, reference -> new ByteArrayInputStream(pixels));

        Texture loaded = resource.texture();
        assertThat(loaded.colorSpace()).isEqualTo(TextureColorSpace.LINEAR);
        resource.close();
        resource.close();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Texture3dResource.owning(loaded))
                .withMessageContaining("must be open");
        assertThatThrownBy(resource::texture)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Copies one loaded sRGB texture into immutable screen-overlay storage. */
    @Test
    void createsOverlayImageFromSrgbTexture() throws IOException {
        ResourceReference payload = ResourceReference.imported("model/payloads/overlay.rgba8");
        byte[] pixels = new byte[] {1, 2, 3, 4};
        Texture3dResource resource = textureLoader()
                .load(textureDefinition(payload, 1, 1, "srgb"), reference -> new ByteArrayInputStream(pixels));

        var image = resource.overlayImage();

        assertThat(image.width()).isEqualTo(1);
        assertThat(image.height()).isEqualTo(1);
        resource.close();
    }

    /** Rejects linear data textures because overlays require an explicit sRGB interpretation. */
    @Test
    void rejectsOverlayImageFromLinearTexture() throws IOException {
        ResourceReference payload = ResourceReference.imported("model/payloads/data.rgba8");
        Texture3dResource resource = textureLoader()
                .load(
                        textureDefinition(payload, 1, 1, "linear"),
                        reference -> new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));

        assertThatThrownBy(resource::overlayImage)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sRGB");
        resource.close();
    }

    /** Rejects malformed texture documents and pixel envelopes before publishing runtime state. */
    @Test
    void rejectsInvalidTextureContent() {
        ResourceReference payload = ResourceReference.imported("model/payloads/data.rgba8");
        ResourceDefinition wrongPayload = withTextureProperty("payload", new ProjectValue.TextValue("wrong"));
        ResourceDefinition shortPayload = textureDefinition(payload, 1, 1, "srgb");
        ResourceDefinition longPayload = textureDefinition(payload, 1, 1, "srgb");
        ResourceDefinition oversized = textureDefinition(payload, 30_000, 30_000, "srgb");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> textureLoader().load(wrongPayload, reference -> InputStreamStub.EMPTY))
                .withMessageContaining("payload must be a reference");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> textureLoader().load(shortPayload, reference -> InputStreamStub.EMPTY))
                .withMessageContaining("length must be 4 bytes");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> textureLoader()
                        .load(longPayload, reference -> new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5})))
                .withMessageContaining("length must be 4 bytes");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> textureLoader().load(oversized, reference -> InputStreamStub.EMPTY))
                .withMessageContaining("too large");
    }

    /** Rejects unsupported mesh format features and corrupt payload envelopes. */
    @Test
    void rejectsUnsupportedAndInvalidMeshContent() throws IOException {
        BufferGeometry empty = new BufferGeometry();
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        Spatial3dResourceWriter.writeMeshPayload(payload, empty);
        byte[] corrupted = payload.toByteArray();
        corrupted[0] = 0;
        ByteArrayInputStream corruptedInput = new ByteArrayInputStream(corrupted);

        assertThatThrownBy(() -> Spatial3dResourceCodec.readMesh(corruptedInput))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("magic");
        ResourceDefinition wrongPayload = new ResourceDefinition(
                URI.create("import:model/resources/broken"),
                Spatial3dDescriptors.meshResourceType(),
                Map.of("payload", new ProjectValue.TextValue("not a reference")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> meshLoader().load(wrongPayload, reference -> InputStreamStub.EMPTY))
                .withMessageContaining("must be a reference");
        empty.close();
    }

    /** Publishes exact resource descriptor types and property contracts. */
    @Test
    void describesSpatialResources() {
        assertThat(Spatial3dResourceLoaders.all())
                .extracting(RuntimeResourceLoader::type)
                .containsExactly(
                        Spatial3dDescriptors.meshResourceType(),
                        Spatial3dDescriptors.materialResourceType(),
                        Spatial3dDescriptors.textureResourceType(),
                        Spatial3dDescriptors.basicMaterialResourceType());
        assertThat(Spatial3dDescriptors.extensionDescriptor().types())
                .extracting(descriptor -> descriptor.type())
                .containsExactly(
                        Spatial3dDescriptors.meshResourceType(),
                        Spatial3dDescriptors.materialResourceType(),
                        Spatial3dDescriptors.textureResourceType(),
                        Spatial3dDescriptors.basicMaterialResourceType());
        assertThat(Spatial3dDescriptors.extensionDescriptor().types().getFirst().properties())
                .containsOnlyKeys("payload");
    }

    /** Returns the typed mesh loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<Mesh3dResource> meshLoader() {
        return (RuntimeResourceLoader<Mesh3dResource>)
                Spatial3dResourceLoaders.all().getFirst();
    }

    /** Returns the typed material loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<Material3dResource> materialLoader() {
        return (RuntimeResourceLoader<Material3dResource>)
                Spatial3dResourceLoaders.all().get(1);
    }

    /** Returns the typed texture loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<Texture3dResource> textureLoader() {
        return (RuntimeResourceLoader<Texture3dResource>)
                Spatial3dResourceLoaders.all().get(2);
    }

    /** Returns the typed basic-material loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<Material3dResource> basicMaterialLoader() {
        return (RuntimeResourceLoader<Material3dResource>)
                Spatial3dResourceLoaders.all().get(3);
    }

    /** Builds all properties normally reconstructed from the canonical material document. */
    private static ResourceDefinition materialDefinition() {
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        values.put("color", numbers(0.25F, 0.5F, 0.75F));
        values.put("metalness", number(0.8F));
        values.put("roughness", number(0.3F));
        values.put("emissive", numbers(0.1F, 0.2F, 0.3F));
        values.put("emissive-intensity", number(2.0F));
        values.put("opacity", number(0.6F));
        values.put("alpha-mode", new ProjectValue.TextValue("blend"));
        values.put("alpha-cutoff", number(0.2F));
        values.put("side", new ProjectValue.TextValue("double"));
        values.put("vertex-colors", new ProjectValue.BooleanValue(true));
        return new ResourceDefinition(
                URI.create("import:model/resources/material"), Spatial3dDescriptors.materialResourceType(), values);
    }

    /** Builds one exact texture definition matching the generated payload. */
    private static ResourceDefinition textureDefinition(ResourceReference payload) {
        return textureDefinition(payload, 2, 1, "srgb");
    }

    /** Builds one texture definition with caller-selected image metadata. */
    private static ResourceDefinition textureDefinition(
            ResourceReference payload, int width, int height, String colorSpace) {
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        values.put("payload", new ProjectValue.ReferenceValue(payload));
        values.put("width", number(width));
        values.put("height", number(height));
        values.put("color-space", new ProjectValue.TextValue(colorSpace));
        values.put("minification-filter", new ProjectValue.TextValue("nearest-mipmap-nearest"));
        values.put("magnification-filter", new ProjectValue.TextValue("nearest"));
        values.put("horizontal-wrap", new ProjectValue.TextValue("repeat"));
        values.put("vertical-wrap", new ProjectValue.TextValue("mirrored-repeat"));
        values.put("coordinate-origin", new ProjectValue.TextValue("top-left"));
        values.put("mipmap-mode", new ProjectValue.TextValue("generate"));
        return new ResourceDefinition(
                URI.create("import:model/resources/checker"), Spatial3dDescriptors.textureResourceType(), values);
    }

    /** Builds a valid texture definition with one caller-selected replacement property. */
    private static ResourceDefinition withTextureProperty(String name, ProjectValue value) {
        Map<String, ProjectValue> values =
                new LinkedHashMap<>(textureDefinition(PAYLOAD).properties());
        values.put(name, value);
        return new ResourceDefinition(
                URI.create("import:model/resources/checker"), Spatial3dDescriptors.textureResourceType(), values);
    }

    /** Builds one unlit material definition referencing the generated texture. */
    private static ResourceDefinition basicMaterialDefinition(ResourceReference texture) {
        Map<String, ProjectValue> values = basicMaterialProperties();
        values.put("color-map", new ProjectValue.ReferenceValue(texture));
        return basicMaterialDefinition(values);
    }

    /** Builds one texture-free unlit material definition. */
    private static ResourceDefinition basicMaterialDefinition() {
        return basicMaterialDefinition(basicMaterialProperties());
    }

    /** Builds the common unlit material properties. */
    private static Map<String, ProjectValue> basicMaterialProperties() {
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        values.put("color", numbers(0.25F, 0.5F, 0.75F));
        values.put("opacity", number(1.0F));
        values.put("alpha-mode", new ProjectValue.TextValue("mask"));
        values.put("alpha-cutoff", number(0.5F));
        values.put("side", new ProjectValue.TextValue("front"));
        values.put("vertex-colors", new ProjectValue.BooleanValue(false));
        return values;
    }

    /** Creates one material definition from already validated properties. */
    private static ResourceDefinition basicMaterialDefinition(Map<String, ProjectValue> values) {
        return new ResourceDefinition(
                URI.create("import:model/resources/basic-material"),
                Spatial3dDescriptors.basicMaterialResourceType(),
                values);
    }

    /** Creates one portable number. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> result = new ArrayList<>();
        for (float value : values) {
            result.add(number(value));
        }
        return new ProjectValue.ArrayValue(result);
    }

    /** Avoids an allocation in a branch that must never inspect content. */
    private static final class InputStreamStub {
        private static final ByteArrayInputStream EMPTY = new ByteArrayInputStream(new byte[0]);

        /** Prevents construction. */
        private InputStreamStub() {
            throw new AssertionError("InputStreamStub cannot be instantiated");
        }
    }

    /** Supplies one retained nested texture resource to the basic-material loader. */
    private static final class ResourceContentStub implements ResourceContent {
        private final Texture3dResource texture;
        private final AtomicBoolean released;

        /** Stores the nested resource and its observable lease-release state. */
        private ResourceContentStub(Texture3dResource texture, AtomicBoolean released) {
            this.texture = texture;
            this.released = released;
        }

        @Override
        public ByteArrayInputStream openPayload(ResourceReference reference) {
            throw new AssertionError("basic material does not open payloads directly");
        }

        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            return RuntimeResourceLease.of(valueType.cast(texture), () -> {
                released.set(true);
                texture.close();
            });
        }
    }
}
