/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.resource.ResourceWriter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
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

    /** Creates one portable exact finite decimal. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }
}
