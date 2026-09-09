/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d.internal.resource;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Versioned private binary codec behind the public spatial-resource interface. */
public final class Spatial3dResourceCodec {
    private static final int MAGIC = 0x4a334d48;
    private static final int VERSION = 1;
    private static final int MAX_ATTRIBUTES = 64;
    private static final int MAX_NAME_BYTES = 1024;
    private static final int MAX_SCALARS = 100_000_000;

    /** Prevents construction of this stateless codec. */
    private Spatial3dResourceCodec() {
        throw new AssertionError("Spatial3dResourceCodec cannot be instantiated");
    }

    /**
     * Writes one complete mesh payload without closing caller-owned output.
     *
     * @param output destination for the encoded mesh
     * @param geometry mesh geometry to encode
     * @throws IOException when the payload cannot be written
     */
    public static void writeMesh(OutputStream output, BufferGeometry geometry) throws IOException {
        if (!geometry.morphTargets().isEmpty()) {
            throw new IllegalArgumentException("mesh resource version 1 does not support morph targets");
        }
        DataOutputStream data = new DataOutputStream(output);
        data.writeInt(MAGIC);
        data.writeInt(VERSION);
        data.writeInt(geometry.attributes().size());
        for (Map.Entry<String, BufferAttribute> entry : geometry.attributes().entrySet()) {
            writeText(data, entry.getKey());
            BufferAttribute attribute = entry.getValue();
            float[] values = attribute.toArray();
            data.writeInt(attribute.itemSize());
            data.writeInt(values.length);
            for (float value : values) {
                data.writeFloat(value);
            }
        }
        IndexBuffer index = geometry.index();
        data.writeBoolean(index != null);
        if (index != null) {
            int[] values = index.toArray();
            data.writeInt(values.length);
            for (int value : values) {
                data.writeInt(value);
            }
        }
        data.writeBoolean(geometry.hasExplicitDrawRange());
        if (geometry.hasExplicitDrawRange()) {
            data.writeInt(geometry.drawRangeStart());
            data.writeInt(geometry.drawRangeCount());
        }
        data.flush();
    }

    /**
     * Reads one complete mesh payload without closing caller-owned input.
     *
     * @param input encoded mesh payload
     * @return decoded mesh geometry
     * @throws IOException when the payload is invalid or cannot be read
     */
    public static BufferGeometry readMesh(InputStream input) throws IOException {
        DataInputStream data = new DataInputStream(input);
        require(data.readInt() == MAGIC, "mesh payload has an invalid magic value");
        require(data.readInt() == VERSION, "unsupported mesh payload version");
        int attributeCount = bounded(data.readInt(), 0, MAX_ATTRIBUTES, "attribute count");
        BufferGeometry geometry = new BufferGeometry();
        try {
            for (int index = 0; index < attributeCount; index++) {
                String name = readText(data);
                int itemSize = bounded(data.readInt(), 1, 16, "attribute item size");
                int scalarCount = bounded(data.readInt(), 0, MAX_SCALARS, "attribute scalar count");
                require(scalarCount % itemSize == 0, "attribute scalar count is not divisible by its item size");
                float[] values = new float[scalarCount];
                for (int scalar = 0; scalar < scalarCount; scalar++) {
                    values[scalar] = data.readFloat();
                }
                geometry.setAttribute(name, BufferAttribute.of(values, itemSize));
            }
            if (data.readBoolean()) {
                int indexCount = bounded(data.readInt(), 0, MAX_SCALARS, "index count");
                int[] values = new int[indexCount];
                for (int index = 0; index < indexCount; index++) {
                    values[index] = data.readInt();
                }
                geometry.setIndex(IndexBuffer.of(values));
            }
            if (data.readBoolean()) {
                geometry.setDrawRange(data.readInt(), data.readInt());
            }
            require(data.read() == -1, "mesh payload contains trailing data");
            return geometry;
        } catch (IOException | RuntimeException failure) {
            geometry.close();
            throw failure;
        }
    }

    /** Writes one bounded UTF-8 string independently of Java modified UTF-8. */
    private static void writeText(DataOutputStream output, String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_NAME_BYTES) {
            throw new IllegalArgumentException("attribute name is too long");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    /** Reads one bounded UTF-8 string. */
    private static String readText(DataInputStream input) throws IOException {
        int length = bounded(input.readInt(), 1, MAX_NAME_BYTES, "attribute name length");
        byte[] encoded = input.readNBytes(length);
        if (encoded.length != length) {
            throw new EOFException("mesh payload ended inside an attribute name");
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /** Requires one binary format invariant. */
    private static void require(boolean condition, String message) throws IOException {
        if (!condition) {
            throw new IOException(message);
        }
    }

    /** Requires one bounded count before allocating storage. */
    private static int bounded(int value, int minimum, int maximum, String label) throws IOException {
        require(value >= minimum && value <= maximum, label + " is outside its supported range: " + value);
        return value;
    }
}
