/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.resource.ResourceWriter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Publishes canonical version-one authored collision-shape resources. */
public final class Physics3dResourceWriter {
    /** Prevents construction of this stateless writer. */
    private Physics3dResourceWriter() {
        throw new AssertionError("Physics3dResourceWriter cannot be instantiated");
    }

    /**
     * Writes one box resource without closing caller-owned output.
     *
     * @param output destination
     * @param width positive finite full X extent
     * @param height positive finite full Y extent
     * @param depth positive finite full Z extent
     * @throws IOException when serialization fails
     */
    public static void writeBox(OutputStream output, float width, float height, float depth) throws IOException {
        Map<String, ProjectValue> properties = new LinkedHashMap<>();
        properties.put("width", number(CollisionPreconditions.requirePositive(width, "width")));
        properties.put("height", number(CollisionPreconditions.requirePositive(height, "height")));
        properties.put("depth", number(CollisionPreconditions.requirePositive(depth, "depth")));
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"), Physics3dDescriptors.boxResourceType(), properties);
    }

    /**
     * Writes one sphere resource without closing caller-owned output.
     *
     * @param output destination
     * @param radius positive finite radius
     * @throws IOException when serialization fails
     */
    public static void writeSphere(OutputStream output, float radius) throws IOException {
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"),
                Physics3dDescriptors.sphereResourceType(),
                Map.of("radius", number(CollisionPreconditions.requirePositive(radius, "radius"))));
    }

    /**
     * Writes one triangle-mesh resource document and its distinct binary collision payload.
     *
     * <p>The method consumes but does not close either output and defensively validates both arrays.
     *
     * @param resourceOutput resource-document destination
     * @param payloadOutput binary-payload destination
     * @param positions consecutive finite XYZ coordinates
     * @param indices consecutive vertex-index triples
     * @param payloadReference reference stored in the resource document
     * @throws IOException when either destination cannot be written
     */
    public static void writeTriangleMesh(
            OutputStream resourceOutput,
            OutputStream payloadOutput,
            float[] positions,
            int[] indices,
            ResourceReference payloadReference)
            throws IOException {
        writeTriangleMeshPayload(payloadOutput, positions, indices);
        writeTriangleMeshDefinition(resourceOutput, payloadReference);
    }

    /**
     * Writes one triangle-mesh resource document for an independently published collision payload.
     *
     * @param output resource-document destination
     * @param payloadReference reference stored in the resource document
     * @throws IOException when the destination cannot be written
     */
    public static void writeTriangleMeshDefinition(OutputStream output, ResourceReference payloadReference)
            throws IOException {
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"),
                Physics3dDescriptors.triangleMeshResourceType(),
                Map.of(
                        "payload",
                        new ProjectValue.ReferenceValue(Objects.requireNonNull(payloadReference, "payloadReference"))));
    }

    /**
     * Writes one validated binary triangle collision mesh without closing caller-owned output.
     *
     * @param output binary-payload destination
     * @param positions consecutive finite XYZ coordinates
     * @param indices consecutive vertex-index triples
     * @throws IOException when the destination cannot be written
     */
    public static void writeTriangleMeshPayload(OutputStream output, float[] positions, int[] indices)
            throws IOException {
        Physics3dResourceCodec.writeTriangleMesh(
                Objects.requireNonNull(output, "output"),
                Objects.requireNonNull(positions, "positions"),
                Objects.requireNonNull(indices, "indices"));
    }

    /** Creates one portable exact finite decimal. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }
}
