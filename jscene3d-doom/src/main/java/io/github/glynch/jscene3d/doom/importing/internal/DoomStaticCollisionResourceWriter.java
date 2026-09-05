/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import io.github.glynch.jscene3d.project.runtime.scene3d.Scene3dTypes;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/** Writes derived Doom map collision as a native triangle-mesh-shape-3d resource. */
final class DoomStaticCollisionResourceWriter {
    /** Prevents construction of this stateless serializer. */
    private DoomStaticCollisionResourceWriter() {
        throw new AssertionError("DoomStaticCollisionResourceWriter cannot be instantiated");
    }

    /** Writes one complete pretty-printed triangle-mesh resource. */
    static void write(OutputStream output, DoomStaticCollisionMesh mesh) throws IOException {
        Objects.requireNonNull(mesh, "mesh");
        try (JsonGenerator generator = DoomJsonGenerators.create(output)) {
            generator.writeStartObject();
            generator.writeNumberField("schemaVersion", 1);
            generator.writeStringField("type", Scene3dTypes.TRIANGLE_MESH_SHAPE_3D.id());
            generator.writeNumberField("typeVersion", Scene3dTypes.TRIANGLE_MESH_SHAPE_3D.version());
            generator.writeObjectFieldStart("properties");
            writePositions(generator, mesh.positions());
            writeIndices(generator, mesh.indices());
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeRaw('\n');
        }
    }

    /** Writes flattened XYZ coordinates without changing numeric precision. */
    private static void writePositions(JsonGenerator generator, float[] positions) throws IOException {
        generator.writeArrayFieldStart("positions");
        for (float position : positions) {
            generator.writeNumber(position);
        }
        generator.writeEndArray();
    }

    /** Writes flattened triangle-index triples. */
    private static void writeIndices(JsonGenerator generator, int[] indices) throws IOException {
        generator.writeArrayFieldStart("indices");
        for (int index : indices) {
            generator.writeNumber(index);
        }
        generator.writeEndArray();
    }
}
