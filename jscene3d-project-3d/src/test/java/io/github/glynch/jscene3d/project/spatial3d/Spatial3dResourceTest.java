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
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
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
                .containsExactly(Spatial3dDescriptors.meshResourceType(), Spatial3dDescriptors.materialResourceType());
        assertThat(Spatial3dDescriptors.extensionDescriptor().types())
                .extracting(descriptor -> descriptor.type())
                .containsExactly(Spatial3dDescriptors.meshResourceType(), Spatial3dDescriptors.materialResourceType());
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
}
