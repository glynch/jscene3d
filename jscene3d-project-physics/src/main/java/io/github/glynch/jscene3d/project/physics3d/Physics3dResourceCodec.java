/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.joml.Vector3f;

/** Versioned private binary codec behind payload-backed physics resources. */
final class Physics3dResourceCodec {
    private static final int TRIANGLE_MESH_MAGIC = 0x4a33434d;
    private static final int TRIANGLE_MESH_VERSION = 1;
    private static final int MAX_SCALARS = 100_000_000;

    /** Prevents construction of this stateless codec. */
    private Physics3dResourceCodec() {
        throw new AssertionError("Physics3dResourceCodec cannot be instantiated");
    }

    /** Writes one validated collision mesh without closing caller-owned output. */
    static void writeTriangleMesh(OutputStream output, float[] positions, int[] indices) throws IOException {
        TriangleMeshShape shape = new TriangleMeshShape(
                Objects.requireNonNull(positions, "positions"), Objects.requireNonNull(indices, "indices"));
        DataOutputStream data = new DataOutputStream(Objects.requireNonNull(output, "output"));
        data.writeInt(TRIANGLE_MESH_MAGIC);
        data.writeInt(TRIANGLE_MESH_VERSION);
        data.writeInt(shape.vertexCount() * 3);
        Vector3f vertex = new Vector3f();
        for (int vertexIndex = 0; vertexIndex < shape.vertexCount(); vertexIndex++) {
            shape.vertex(vertexIndex, vertex);
            data.writeFloat(vertex.x);
            data.writeFloat(vertex.y);
            data.writeFloat(vertex.z);
        }
        int indexCount = shape.triangleCount() * 3;
        data.writeInt(indexCount);
        for (int index = 0; index < indexCount; index++) {
            data.writeInt(shape.index(index));
        }
        data.flush();
    }

    /** Reads one complete collision-mesh payload without closing caller-owned input. */
    static TriangleMeshCollisionShape3dResource readTriangleMesh(InputStream input) throws IOException {
        DataInputStream data = new DataInputStream(Objects.requireNonNull(input, "input"));
        require(data.readInt() == TRIANGLE_MESH_MAGIC, "triangle-mesh collision payload has an invalid magic value");
        require(data.readInt() == TRIANGLE_MESH_VERSION, "unsupported triangle-mesh collision payload version");
        int positionCount = triangleScalars(data.readInt(), 9, "position count");
        float[] positions = new float[positionCount];
        for (int index = 0; index < positionCount; index++) {
            positions[index] = data.readFloat();
        }
        int indexCount = triangleScalars(data.readInt(), 3, "index count");
        int[] indices = new int[indexCount];
        for (int index = 0; index < indexCount; index++) {
            indices[index] = data.readInt();
        }
        require(data.read() == -1, "triangle-mesh collision payload contains trailing data");
        return new TriangleMeshCollisionShape3dResource(positions, indices);
    }

    /** Requires a bounded scalar count containing complete XYZ or triangle triples. */
    private static int triangleScalars(int value, int minimum, String label) throws IOException {
        require(value >= minimum && value <= MAX_SCALARS, label + " is outside its supported range: " + value);
        require(value % 3 == 0, label + " is not divisible by three");
        return value;
    }

    /** Requires one binary-format invariant. */
    private static void require(boolean condition, String message) throws IOException {
        if (!condition) {
            throw new IOException(message);
        }
    }
}
