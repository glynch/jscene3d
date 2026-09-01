/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf.internal;

import dev.fileformat.drako.Draco;
import dev.fileformat.drako.DracoMesh;
import dev.fileformat.drako.DracoPointCloud;
import dev.fileformat.drako.DrakoException;
import dev.fileformat.drako.PointAttribute;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Converts one {@code KHR_draco_mesh_compression} payload into primitive arrays. */
public final class DracoDecoder {
    /** Prevents instantiation of this static decoder utility. */
    private DracoDecoder() {
        throw new AssertionError("DracoDecoder cannot be instantiated");
    }

    /**
     * Decodes the compressed buffer view and requested semantic-to-attribute mapping.
     *
     * @param compressedData compressed Draco buffer-view bytes
     * @param attributeIds glTF semantic to Draco unique attribute identifier
     * @param componentCounts required component count for every semantic
     * @param attributeMinimums optional decoded-space minimum components from glTF accessors
     * @return decoded point attributes and triangle indices
     * @throws NullPointerException if an argument or mapping entry is {@code null}
     * @throws IllegalArgumentException if the payload is not a triangle mesh or does not match its
     *     descriptor
     */
    public static DecodedPrimitive decode(
            ByteBuffer compressedData,
            Map<String, Integer> attributeIds,
            Map<String, Integer> componentCounts,
            Map<String, float[]> attributeMinimums) {
        ByteBuffer source =
                Objects.requireNonNull(compressedData, "compressedData").duplicate();
        Map<String, Integer> ids = Map.copyOf(Objects.requireNonNull(attributeIds, "attributeIds"));
        Map<String, Integer> counts = Map.copyOf(Objects.requireNonNull(componentCounts, "componentCounts"));
        Map<String, float[]> minimums = copyArrays(attributeMinimums, "attributeMinimums");
        byte[] encoded = new byte[source.remaining()];
        source.get(encoded);
        DracoPointCloud decoded;
        try {
            decoded = Draco.decode(encoded);
        } catch (DrakoException exception) {
            throw new IllegalArgumentException("Invalid Draco mesh payload", exception);
        }
        if (!(decoded instanceof DracoMesh mesh)) {
            throw new IllegalArgumentException("Draco payload is not a triangle mesh");
        }

        Map<String, float[]> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : ids.entrySet()) {
            String semantic = Objects.requireNonNull(entry.getKey(), "attribute semantic");
            int attributeId = Objects.requireNonNull(entry.getValue(), semantic + " attribute id");
            int components = Objects.requireNonNull(counts.get(semantic), semantic + " component count");
            PointAttribute attribute = findAttribute(mesh, attributeId);
            if (attribute.getComponentsCount() != components) {
                throw new IllegalArgumentException(semantic + " Draco component count differs from its accessor: "
                        + attribute.getComponentsCount() + " != " + components);
            }
            float[] values = decodeAttribute(mesh, attribute, components);
            restoreAttributeMinimums(values, components, minimums.get(semantic), semantic);
            attributes.put(semantic, values);
        }
        return new DecodedPrimitive(attributes, decodeFaces(mesh), mesh.getNumPoints());
    }

    /** Defensively copies one semantic-to-array descriptor map. */
    private static Map<String, float[]> copyArrays(Map<String, float[]> source, String name) {
        Map<String, float[]> copy = new LinkedHashMap<>();
        Objects.requireNonNull(source, name)
                .forEach((semantic, values) -> copy.put(
                        Objects.requireNonNull(semantic, "semantic"),
                        Objects.requireNonNull(values, semantic).clone()));
        return Map.copyOf(copy);
    }

    /** Finds an attribute using the extension's stable unique identifier. */
    private static PointAttribute findAttribute(DracoPointCloud pointCloud, int uniqueId) {
        for (int index = 0; index < pointCloud.getNumAttributes(); index++) {
            PointAttribute attribute = pointCloud.attribute(index);
            if (Short.toUnsignedInt(attribute.getUniqueId()) == uniqueId) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Draco payload has no attribute with unique id " + uniqueId);
    }

    /** Expands a point-mapped attribute into one packed value per decoded point. */
    private static float[] decodeAttribute(DracoPointCloud pointCloud, PointAttribute attribute, int components) {
        float[] values = new float[Math.multiplyExact(pointCloud.getNumPoints(), components)];
        float[] element = new float[components];
        for (int point = 0; point < pointCloud.getNumPoints(); point++) {
            attribute.getValue(attribute.mappedIndex(point), element);
            System.arraycopy(element, 0, values, point * components, components);
        }
        return values;
    }

    /**
     * Restores component offsets omitted by Drako when decoding some externally produced streams.
     *
     * <p>Draco quantization preserves each component's range. The glTF accessor retains the
     * decoded-space minima, allowing the omitted per-component offsets to be restored without
     * changing the quantized range.
     */
    private static void restoreAttributeMinimums(
            float[] values, int components, float @Nullable [] expectedMinimums, String semantic) {
        if (expectedMinimums == null || values.length == 0) {
            return;
        }
        if (expectedMinimums.length != components) {
            throw new IllegalArgumentException(semantic + " accessor minimum component count differs from its type: "
                    + expectedMinimums.length + " != " + components);
        }
        float[] decodedMinimums = new float[components];
        Arrays.fill(decodedMinimums, Float.POSITIVE_INFINITY);
        for (int index = 0; index < values.length; index++) {
            int component = index % components;
            decodedMinimums[component] = Math.min(decodedMinimums[component], values[index]);
        }
        for (int index = 0; index < values.length; index++) {
            int component = index % components;
            values[index] += expectedMinimums[component] - decodedMinimums[component];
        }
    }

    /** Expands decoded triangle faces into glTF-compatible integer indices. */
    private static int[] decodeFaces(DracoMesh mesh) {
        int[] indices = new int[Math.multiplyExact(mesh.getNumFaces(), 3)];
        int[] face = new int[3];
        for (int faceIndex = 0; faceIndex < mesh.getNumFaces(); faceIndex++) {
            mesh.readFace(faceIndex, face);
            System.arraycopy(face, 0, indices, faceIndex * 3, 3);
        }
        return indices;
    }

    /**
     * Immutable decoded compressed triangle primitive with content-based value semantics.
     *
     * <p>Packed arrays are copied on input and output.
     */
    public static final class DecodedPrimitive {
        private final Map<String, float[]> attributes;
        private final int[] indices;
        private final int vertexCount;

        /**
         * Creates a decoded primitive by defensively copying all packed arrays.
         *
         * @param attributes semantic-to-packed-float mapping
         * @param indices packed triangle point indices
         * @param vertexCount decoded point count
         */
        public DecodedPrimitive(Map<String, float[]> attributes, int[] indices, int vertexCount) {
            Map<String, float[]> copiedAttributes = new LinkedHashMap<>();
            Objects.requireNonNull(attributes, "attributes")
                    .forEach((semantic, values) -> copiedAttributes.put(
                            Objects.requireNonNull(semantic, "semantic"),
                            Objects.requireNonNull(values, semantic).clone()));
            if (vertexCount < 0) {
                throw new IllegalArgumentException("vertexCount must not be negative: " + vertexCount);
            }
            this.attributes = Map.copyOf(copiedAttributes);
            this.indices = Objects.requireNonNull(indices, "indices").clone();
            this.vertexCount = vertexCount;
        }

        /**
         * Returns defensive copies of the packed attributes.
         *
         * @return immutable semantic-to-packed-float mapping
         */
        public Map<String, float[]> attributes() {
            Map<String, float[]> copy = new LinkedHashMap<>();
            attributes.forEach((semantic, values) -> copy.put(semantic, values.clone()));
            return Map.copyOf(copy);
        }

        /**
         * Returns a defensive copy of the packed triangle indices.
         *
         * @return packed triangle point indices
         */
        public int[] indices() {
            return indices.clone();
        }

        /**
         * Returns the decoded point count.
         *
         * @return non-negative point count
         */
        public int vertexCount() {
            return vertexCount;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof DecodedPrimitive primitive
                    && vertexCount == primitive.vertexCount
                    && Arrays.equals(indices, primitive.indices)
                    && attributesEqual(primitive.attributes);
        }

        @Override
        public int hashCode() {
            int attributeHash = 0;
            for (Map.Entry<String, float[]> entry : attributes.entrySet()) {
                attributeHash += entry.getKey().hashCode() ^ Arrays.hashCode(entry.getValue());
            }
            return Objects.hash(attributeHash, Arrays.hashCode(indices), vertexCount);
        }

        @Override
        public String toString() {
            return "DecodedPrimitive[attributes=" + attributes.keySet() + ", indexCount=" + indices.length
                    + ", vertexCount=" + vertexCount + "]";
        }

        /** Compares packed attribute arrays by semantic and content. */
        private boolean attributesEqual(Map<String, float[]> otherAttributes) {
            if (!attributes.keySet().equals(otherAttributes.keySet())) {
                return false;
            }
            for (Map.Entry<String, float[]> entry : attributes.entrySet()) {
                if (!Arrays.equals(entry.getValue(), otherAttributes.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
