/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.project.resource.ResourceWriter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.textures.Texture;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Publishes canonical version-one spatial resources without exposing their storage formats. */
public final class Spatial3dResourceWriter {
    /** Prevents construction of this stateless writer. */
    private Spatial3dResourceWriter() {
        throw new AssertionError("Spatial3dResourceWriter cannot be instantiated");
    }

    /**
     * Writes one mesh resource document and its referenced binary payload.
     *
     * <p>The method consumes but does not close either output.
     *
     * @param resourceOutput resource-document destination
     * @param payloadOutput binary-payload destination
     * @param geometry open renderer-independent geometry
     * @param payloadReference reference stored in the resource document
     * @throws IOException when either destination cannot be written
     */
    public static void writeMesh(
            OutputStream resourceOutput,
            OutputStream payloadOutput,
            BufferGeometry geometry,
            ResourceReference payloadReference)
            throws IOException {
        OutputStream validResourceOutput = Objects.requireNonNull(resourceOutput, "resourceOutput");
        OutputStream validPayloadOutput = Objects.requireNonNull(payloadOutput, "payloadOutput");
        BufferGeometry validGeometry = Objects.requireNonNull(geometry, "geometry");
        ProjectValue.ReferenceValue payload =
                new ProjectValue.ReferenceValue(Objects.requireNonNull(payloadReference, "payloadReference"));
        writeMeshPayload(validPayloadOutput, validGeometry);
        writeMeshDefinition(validResourceOutput, payload.reference());
    }

    /**
     * Writes only one mesh resource document for artifact stores with independent destinations.
     *
     * @param output resource-document destination
     * @param payloadReference reference stored in the resource document
     * @throws IOException when the destination cannot be written
     */
    public static void writeMeshDefinition(OutputStream output, ResourceReference payloadReference) throws IOException {
        ProjectValue.ReferenceValue payload =
                new ProjectValue.ReferenceValue(Objects.requireNonNull(payloadReference, "payloadReference"));
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"),
                Spatial3dDescriptors.meshResourceType(),
                Map.of("payload", payload));
    }

    /**
     * Writes only one binary mesh payload for artifact stores with independent destinations.
     *
     * @param output binary-payload destination
     * @param geometry open renderer-independent geometry
     * @throws IOException when the destination cannot be written
     */
    public static void writeMeshPayload(OutputStream output, BufferGeometry geometry) throws IOException {
        Spatial3dResourceCodec.writeMesh(
                Objects.requireNonNull(output, "output"), Objects.requireNonNull(geometry, "geometry"));
    }

    /**
     * Writes one texture resource document and its referenced raw RGBA8 payload.
     *
     * @param resourceOutput resource-document destination
     * @param payloadOutput raw-pixel destination
     * @param texture open texture to serialize
     * @param payloadReference reference stored in the resource document
     * @throws IOException when either destination cannot be written
     */
    public static void writeTexture(
            OutputStream resourceOutput,
            OutputStream payloadOutput,
            Texture texture,
            ResourceReference payloadReference)
            throws IOException {
        Texture validTexture = Objects.requireNonNull(texture, "texture");
        writeTexturePayload(Objects.requireNonNull(payloadOutput, "payloadOutput"), validTexture);
        writeTextureDefinition(
                Objects.requireNonNull(resourceOutput, "resourceOutput"), validTexture, payloadReference);
    }

    /**
     * Writes one texture resource document for an independently published pixel payload.
     *
     * @param output resource-document destination
     * @param texture open texture supplying image and sampler metadata
     * @param payloadReference reference stored in the resource document
     * @throws IOException when the destination cannot be written
     */
    public static void writeTextureDefinition(OutputStream output, Texture texture, ResourceReference payloadReference)
            throws IOException {
        Texture validTexture = Objects.requireNonNull(texture, "texture");
        Map<String, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                "payload",
                new ProjectValue.ReferenceValue(Objects.requireNonNull(payloadReference, "payloadReference")));
        properties.put("width", number(validTexture.width()));
        properties.put("height", number(validTexture.height()));
        properties.put("color-space", text(validTexture.colorSpace()));
        properties.put("minification-filter", text(validTexture.minificationFilter()));
        properties.put("magnification-filter", text(validTexture.magnificationFilter()));
        properties.put("horizontal-wrap", text(validTexture.horizontalWrap()));
        properties.put("vertical-wrap", text(validTexture.verticalWrap()));
        properties.put("coordinate-origin", text(validTexture.coordinateOrigin()));
        properties.put("mipmap-mode", text(validTexture.mipmapMode()));
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"), Spatial3dDescriptors.textureResourceType(), properties);
    }

    /**
     * Writes a texture's exact top-row-first RGBA8 image bytes.
     *
     * @param output pixel-payload destination
     * @param texture open texture to serialize
     * @throws IOException when the destination cannot be written
     */
    public static void writeTexturePayload(OutputStream output, Texture texture) throws IOException {
        OutputStream validOutput = Objects.requireNonNull(output, "output");
        Texture validTexture = Objects.requireNonNull(texture, "texture");
        ByteBuffer pixels = ByteBuffer.allocate(validTexture.pixelByteCount());
        validTexture.copyPixelsTo(pixels);
        validOutput.write(pixels.array());
    }

    /**
     * Writes one texture-free unlit material resource.
     *
     * @param output resource-document destination
     * @param material open unlit material without a color map
     * @throws IOException when the destination cannot be written
     */
    public static void writeBasicMaterial(OutputStream output, BasicMaterial material) throws IOException {
        BasicMaterial validMaterial = Objects.requireNonNull(material, "material");
        if (validMaterial.colorMap().isPresent()) {
            throw new IllegalArgumentException("a color-map reference is required for a textured basic material");
        }
        writeBasicMaterialProperties(output, validMaterial, Map.of());
    }

    /**
     * Writes one unlit material referencing its separately published color-map resource.
     *
     * @param output resource-document destination
     * @param material open unlit material with a color map
     * @param colorMapReference shared texture-resource reference
     * @throws IOException when the destination cannot be written
     */
    public static void writeBasicMaterial(
            OutputStream output, BasicMaterial material, ResourceReference colorMapReference) throws IOException {
        BasicMaterial validMaterial = Objects.requireNonNull(material, "material");
        if (validMaterial.colorMap().isEmpty()) {
            throw new IllegalArgumentException("material must have a color map");
        }
        ResourceReference validReference = Objects.requireNonNull(colorMapReference, "colorMapReference");
        writeBasicMaterialProperties(
                output, validMaterial, Map.of("color-map", new ProjectValue.ReferenceValue(validReference)));
    }

    /**
     * Writes one texture-free standard-material resource document.
     *
     * <p>The method consumes but does not close {@code output}. Texture-backed materials are deliberately rejected by
     * resource version one instead of being serialized incompletely.
     *
     * @param output resource-document destination
     * @param material open material to serialize
     * @throws IOException when the destination cannot be written
     */
    public static void writeMaterial(OutputStream output, StandardMaterial material) throws IOException {
        StandardMaterial validMaterial = Objects.requireNonNull(material, "material");
        requireTextureFree(validMaterial);
        Map<String, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                "color",
                numbers(
                        validMaterial.color().red(),
                        validMaterial.color().green(),
                        validMaterial.color().blue()));
        properties.put("metalness", number(validMaterial.metalness()));
        properties.put("roughness", number(validMaterial.roughness()));
        properties.put(
                "emissive",
                numbers(
                        validMaterial.emissive().red(),
                        validMaterial.emissive().green(),
                        validMaterial.emissive().blue()));
        properties.put("emissive-intensity", number(validMaterial.emissiveIntensity()));
        properties.put("opacity", number(validMaterial.opacity()));
        properties.put(
                "alpha-mode",
                new ProjectValue.TextValue(validMaterial.alphaMode().name().toLowerCase(Locale.ROOT)));
        properties.put("alpha-cutoff", number(validMaterial.alphaCutoff()));
        properties.put(
                "side", new ProjectValue.TextValue(validMaterial.side().name().toLowerCase(Locale.ROOT)));
        properties.put("vertex-colors", new ProjectValue.BooleanValue(validMaterial.usesVertexColors()));
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"), Spatial3dDescriptors.materialResourceType(), properties);
    }

    /** Writes the shared unlit material properties with an optional texture-resource reference. */
    private static void writeBasicMaterialProperties(
            OutputStream output, BasicMaterial material, Map<String, ProjectValue> textureProperties)
            throws IOException {
        Map<String, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                "color",
                numbers(
                        material.color().red(),
                        material.color().green(),
                        material.color().blue()));
        writeRenderState(properties, material);
        properties.put("vertex-colors", new ProjectValue.BooleanValue(material.usesVertexColors()));
        properties.putAll(textureProperties);
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"), Spatial3dDescriptors.basicMaterialResourceType(), properties);
    }

    /** Writes render-state properties shared by portable material profiles. */
    private static void writeRenderState(Map<String, ProjectValue> properties, Material material) {
        properties.put("opacity", number(material.opacity()));
        properties.put("alpha-mode", text(material.alphaMode()));
        properties.put("alpha-cutoff", number(material.alphaCutoff()));
        properties.put("side", text(material.side()));
    }

    /** Requires the deliberately bounded version-one material profile. */
    private static void requireTextureFree(StandardMaterial material) {
        if (material.colorMap().isPresent()
                || material.metalnessRoughnessMap().isPresent()
                || material.normalMap().isPresent()
                || material.occlusionMap().isPresent()
                || material.emissiveMap().isPresent()) {
            throw new IllegalArgumentException("standard-material resource version 1 does not support textures");
        }
    }

    /** Creates one portable exact decimal from a finite float. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }

    /** Creates one stable lower-kebab-case enum value. */
    private static ProjectValue.TextValue text(Enum<?> value) {
        return new ProjectValue.TextValue(value.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(number(value));
        }
        return new ProjectValue.ArrayValue(result);
    }
}
