/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Application-owned, renderer-independent triangle geometry.
 *
 * <p>Attributes retain library-owned {@link BufferAttribute} values in deterministic insertion
 * order. All attributes in one geometry have the same item count. An optional shared {@link
 * IndexBuffer} selects vertices; otherwise drawing is non-indexed. Mutable arrays and GPU state are
 * never exposed.
 *
 * <p>Closure is terminal and idempotent. Other operations fail after closure. Instances and their
 * attributes are mutable and are not thread-safe.
 */
public final class BufferGeometry implements AutoCloseable {
    /** Standard position attribute name. */
    public static final String POSITION = "position";

    /** Standard normal attribute name. */
    public static final String NORMAL = "normal";

    /** Standard texture-coordinate attribute name. */
    public static final String UV = "uv";

    /** Standard vertex-color attribute name. */
    public static final String COLOR = "color";

    private final Map<String, BufferAttribute> attributes;
    private final Map<String, BufferAttribute> attributesView;

    private @Nullable IndexBuffer index;
    private @Nullable BoundingBox boundingBox;
    private @Nullable BoundingSphere boundingSphere;
    private @Nullable BufferAttribute boundingBoxPositionAttribute;
    private @Nullable BufferAttribute boundingSpherePositionAttribute;
    private long boundingBoxPositionVersion;
    private long boundingSpherePositionVersion;
    private boolean computedBoundingBox;
    private boolean computedBoundingSphere;
    private boolean explicitDrawRange;
    private int drawRangeStart;
    private int drawRangeCount;
    private long version;
    private boolean closed;

    /** Creates empty, open triangle geometry. */
    public BufferGeometry() {
        attributes = new LinkedHashMap<>();
        attributesView = Collections.unmodifiableMap(attributes);
    }

    /**
     * Creates a builder for atomic initial geometry configuration.
     *
     * @return a new one-use builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the geometry-level structural version.
     *
     * <p>Attribute and index scalar data have their own versions. This version changes when an
     * attribute mapping, index buffer, draw range, or explicit bound changes.
     *
     * @return the structural version, initially zero
     * @throws IllegalStateException if this geometry is closed
     */
    public long version() {
        requireOpen();
        return version;
    }

    /**
     * Returns the stable unmodifiable live view of named attributes.
     *
     * @return attributes in deterministic insertion order
     * @throws IllegalStateException if this geometry is closed
     */
    public Map<String, BufferAttribute> attributes() {
        requireOpen();
        return attributesView;
    }

    /**
     * Returns a named attribute.
     *
     * @param name non-empty attribute name
     * @return the attribute, or {@code null} when absent
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is empty
     * @throws IllegalStateException if this geometry is closed
     */
    public @Nullable BufferAttribute attribute(String name) {
        requireOpen();
        return attributes.get(Preconditions.requireNonEmpty(name, "name"));
    }

    /**
     * Sets or replaces a named attribute.
     *
     * <p>All attributes must have the same item count. The standard position attribute must have
     * exactly three components per item.
     *
     * @param name non-empty attribute name
     * @param attribute attribute to retain
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the name is empty, counts are incompatible, the position
     *     item size is not three, or an existing index or draw range would become invalid
     * @throws IllegalStateException if this geometry is closed
     */
    public void setAttribute(String name, BufferAttribute attribute) {
        requireOpen();
        String validName = Preconditions.requireNonEmpty(name, "name");
        BufferAttribute validAttribute = Objects.requireNonNull(attribute, "attribute");
        if (POSITION.equals(validName) && validAttribute.itemSize() != 3) {
            throw new IllegalArgumentException("position attribute itemSize must be 3: " + validAttribute.itemSize());
        }

        BufferAttribute existingAttribute = attributes.get(validName);
        if (existingAttribute == validAttribute) {
            return;
        }
        requireCompatibleAttributeCount(validName, validAttribute.count());

        int currentVertexCount = vertexCountUnchecked();
        if (index == null && currentVertexCount != validAttribute.count()) {
            requireValidExplicitDrawRange(validAttribute.count());
        }

        if (POSITION.equals(validName)) {
            validatePositionReplacement(validAttribute);
        }
        attributes.put(validName, validAttribute);
        if (POSITION.equals(validName)) {
            updateIndexVertexCountAttachment(existingAttribute, validAttribute);
            invalidateComputedBounds();
        }
        version++;
    }

    /**
     * Removes a named attribute.
     *
     * @param name non-empty attribute name
     * @return {@code true} when an attribute was removed
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is empty or removing positions would leave
     *     an index buffer attached
     * @throws IllegalStateException if this geometry is closed
     */
    public boolean removeAttribute(String name) {
        requireOpen();
        String validName = Preconditions.requireNonEmpty(name, "name");
        BufferAttribute existingAttribute = attributes.get(validName);
        if (existingAttribute == null) {
            return false;
        }
        if (POSITION.equals(validName) && index != null) {
            throw new IllegalArgumentException("position attribute cannot be removed while an index buffer is set");
        }
        int remainingVertexCount = vertexCountExcluding(existingAttribute);
        if (index == null && remainingVertexCount != existingAttribute.count()) {
            requireValidExplicitDrawRange(remainingVertexCount);
        }
        attributes.remove(validName);
        if (POSITION.equals(validName)) {
            invalidateComputedBounds();
        }
        version++;
        return true;
    }

    /**
     * Returns the shared index buffer.
     *
     * @return the index buffer, or {@code null} for non-indexed geometry
     * @throws IllegalStateException if this geometry is closed
     */
    public @Nullable IndexBuffer index() {
        requireOpen();
        return index;
    }

    /**
     * Sets the index buffer used for triangle drawing.
     *
     * @param index index buffer to retain
     * @throws NullPointerException if {@code index} is {@code null}
     * @throws IllegalArgumentException if no position attribute exists, an index exceeds the
     *     vertex count, or the existing draw range would become invalid
     * @throws IllegalStateException if this geometry is closed
     */
    public void setIndex(IndexBuffer index) {
        requireOpen();
        IndexBuffer validIndex = Objects.requireNonNull(index, "index");
        if (this.index == validIndex) {
            return;
        }
        BufferAttribute positions = requirePositionAttribute();
        validIndex.attachVertexCount(positions.count());
        try {
            requireValidExplicitDrawRange(validIndex.count());
        } catch (RuntimeException exception) {
            validIndex.detachVertexCount(positions.count());
            throw exception;
        }
        IndexBuffer previousIndex = this.index;
        this.index = validIndex;
        if (previousIndex != null) {
            previousIndex.detachVertexCount(positions.count());
        }
        version++;
    }

    /**
     * Clears indexed drawing.
     *
     * @return {@code true} when an index buffer was removed
     * @throws IllegalArgumentException if the existing draw range would be invalid for non-indexed
     *     drawing
     * @throws IllegalStateException if this geometry is closed
     */
    public boolean clearIndex() {
        requireOpen();
        IndexBuffer existingIndex = index;
        if (existingIndex == null) {
            return false;
        }
        BufferAttribute positions = requirePositionAttribute();
        requireValidExplicitDrawRange(positions.count());
        existingIndex.detachVertexCount(positions.count());
        index = null;
        version++;
        return true;
    }

    /**
     * Returns the common number of vertex items.
     *
     * @return zero for empty geometry, otherwise the common attribute count
     * @throws IllegalStateException if this geometry is closed
     */
    public int vertexCount() {
        requireOpen();
        return vertexCountUnchecked();
    }

    /**
     * Returns the effective draw-range start.
     *
     * @return zero unless an explicit range is set
     * @throws IllegalStateException if this geometry is closed
     */
    public int drawRangeStart() {
        requireOpen();
        return explicitDrawRange ? drawRangeStart : 0;
    }

    /**
     * Returns the effective number of indices or vertices to draw.
     *
     * @return explicit count when set, otherwise the complete available count
     * @throws IllegalStateException if this geometry is closed
     */
    public int drawRangeCount() {
        requireOpen();
        return explicitDrawRange ? drawRangeCount : availableElementCount();
    }

    /**
     * Returns whether an explicit draw range overrides the complete available range.
     *
     * @return {@code true} when explicit
     * @throws IllegalStateException if this geometry is closed
     */
    public boolean hasExplicitDrawRange() {
        requireOpen();
        return explicitDrawRange;
    }

    /**
     * Sets a validated index or vertex draw range.
     *
     * @param start non-negative first element
     * @param count non-negative number of elements
     * @throws IllegalArgumentException if either value is negative or the range exceeds available
     *     index or vertex data
     * @throws IllegalStateException if this geometry is closed
     */
    public void setDrawRange(int start, int count) {
        requireOpen();
        int validStart = Preconditions.requireNonNegative(start, "start");
        int validCount = Preconditions.requireNonNegative(count, "count");
        requireRangeWithin(validStart, validCount, availableElementCount());
        if (!explicitDrawRange || drawRangeStart != validStart || drawRangeCount != validCount) {
            explicitDrawRange = true;
            drawRangeStart = validStart;
            drawRangeCount = validCount;
            version++;
        }
    }

    /**
     * Restores drawing of every available index or vertex.
     *
     * @throws IllegalStateException if this geometry is closed
     */
    public void clearDrawRange() {
        requireOpen();
        if (explicitDrawRange) {
            explicitDrawRange = false;
            version++;
        }
    }

    /**
     * Returns current cached or explicitly supplied box bounds.
     *
     * <p>Computed bounds become absent automatically when position data changes.
     *
     * @return current bounds, or {@code null} when none are current
     * @throws IllegalStateException if this geometry is closed
     */
    public @Nullable BoundingBox boundingBox() {
        requireOpen();
        refreshComputedBounds();
        return boundingBox;
    }

    /**
     * Computes and caches exact axis-aligned bounds from positions.
     *
     * @return the computed bounds
     * @throws IllegalStateException if this geometry is closed or has no position items
     */
    public BoundingBox computeBoundingBox() {
        requireOpen();
        BufferAttribute positions = requireNonEmptyPositionAttribute();
        BoundingBox computed = calculateBoundingBox(positions);
        boundingBox = computed;
        boundingBoxPositionAttribute = positions;
        boundingBoxPositionVersion = positions.version();
        computedBoundingBox = true;
        return computed;
    }

    /**
     * Supplies box bounds explicitly.
     *
     * <p>Explicit bounds remain current across position edits until replaced or cleared.
     *
     * @param boundingBox immutable bounds to retain
     * @throws NullPointerException if {@code boundingBox} is {@code null}
     * @throws IllegalStateException if this geometry is closed
     */
    public void setBoundingBox(BoundingBox boundingBox) {
        requireOpen();
        BoundingBox validBoundingBox = Objects.requireNonNull(boundingBox, "boundingBox");
        if (!Objects.equals(this.boundingBox, validBoundingBox) || computedBoundingBox) {
            this.boundingBox = validBoundingBox;
            boundingBoxPositionAttribute = null;
            computedBoundingBox = false;
            version++;
        }
    }

    /**
     * Clears box bounds.
     *
     * @throws IllegalStateException if this geometry is closed
     */
    public void clearBoundingBox() {
        requireOpen();
        if (boundingBox != null) {
            boundingBox = null;
            boundingBoxPositionAttribute = null;
            computedBoundingBox = false;
            version++;
        }
    }

    /**
     * Returns current cached or explicitly supplied spherical bounds.
     *
     * <p>Computed bounds become absent automatically when position data changes.
     *
     * @return current bounds, or {@code null} when none are current
     * @throws IllegalStateException if this geometry is closed
     */
    public @Nullable BoundingSphere boundingSphere() {
        requireOpen();
        refreshComputedBounds();
        return boundingSphere;
    }

    /**
     * Computes and caches spherical bounds from positions.
     *
     * @return the computed bounds
     * @throws IllegalStateException if this geometry is closed, has no position items, or the
     *     resulting radius is not representable as a finite float
     */
    public BoundingSphere computeBoundingSphere() {
        requireOpen();
        BufferAttribute positions = requireNonEmptyPositionAttribute();
        BoundingBox box = calculateBoundingBox(positions);
        double centerX = ((double) box.minimum().x() + box.maximum().x()) * 0.5;
        double centerY = ((double) box.minimum().y() + box.maximum().y()) * 0.5;
        double centerZ = ((double) box.minimum().z() + box.maximum().z()) * 0.5;
        double maximumDistanceSquared = 0.0;
        for (int vertexIndex = 0; vertexIndex < positions.count(); vertexIndex++) {
            double offsetX = positions.value(vertexIndex, 0) - centerX;
            double offsetY = positions.value(vertexIndex, 1) - centerY;
            double offsetZ = positions.value(vertexIndex, 2) - centerZ;
            double distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
            maximumDistanceSquared = Math.max(maximumDistanceSquared, distanceSquared);
        }
        float radius = (float) Math.sqrt(maximumDistanceSquared);
        if (!Float.isFinite(radius)) {
            throw new IllegalStateException("Computed bounding-sphere radius must be finite");
        }
        BoundingSphere computed = new BoundingSphere((float) centerX, (float) centerY, (float) centerZ, radius);
        boundingSphere = computed;
        boundingSpherePositionAttribute = positions;
        boundingSpherePositionVersion = positions.version();
        computedBoundingSphere = true;
        return computed;
    }

    /**
     * Supplies spherical bounds explicitly.
     *
     * <p>Explicit bounds remain current across position edits until replaced or cleared.
     *
     * @param boundingSphere immutable bounds to retain
     * @throws NullPointerException if {@code boundingSphere} is {@code null}
     * @throws IllegalStateException if this geometry is closed
     */
    public void setBoundingSphere(BoundingSphere boundingSphere) {
        requireOpen();
        BoundingSphere validBoundingSphere = Objects.requireNonNull(boundingSphere, "boundingSphere");
        if (!Objects.equals(this.boundingSphere, validBoundingSphere) || computedBoundingSphere) {
            this.boundingSphere = validBoundingSphere;
            boundingSpherePositionAttribute = null;
            computedBoundingSphere = false;
            version++;
        }
    }

    /**
     * Clears spherical bounds.
     *
     * @throws IllegalStateException if this geometry is closed
     */
    public void clearBoundingSphere() {
        requireOpen();
        if (boundingSphere != null) {
            boundingSphere = null;
            boundingSpherePositionAttribute = null;
            computedBoundingSphere = false;
            version++;
        }
    }

    /**
     * Returns whether terminal closure has occurred.
     *
     * @return {@code true} after the first call to {@link #close()}
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Permanently closes this geometry and releases its retained CPU-side descriptions.
     *
     * <p>Repeated closure is a no-op. Closing a geometry does not close shared attributes or index
     * buffers.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        IndexBuffer existingIndex = index;
        BufferAttribute positions = attributes.get(POSITION);
        if (existingIndex != null && positions != null) {
            existingIndex.detachVertexCount(positions.count());
        }
        attributes.clear();
        index = null;
        boundingBox = null;
        boundingSphere = null;
        boundingBoxPositionAttribute = null;
        boundingSpherePositionAttribute = null;
        closed = true;
    }

    /** Builds one initially configured {@link BufferGeometry}. */
    public static final class Builder {
        private final Map<String, BufferAttribute> attributes = new LinkedHashMap<>();

        private @Nullable IndexBuffer index;
        private boolean explicitDrawRange;
        private int drawRangeStart;
        private int drawRangeCount;
        private boolean built;

        /** Restricts builder creation to {@link BufferGeometry#builder()}. */
        private Builder() {}

        /**
         * Sets a named attribute.
         *
         * <p>This is the general construction path for custom attributes, explicit usage policies,
         * and standard attributes that need direct {@link BufferAttribute} control.
         *
         * @param name non-empty attribute name
         * @param attribute attribute to retain
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the name is empty
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder attribute(String name, BufferAttribute attribute) {
            requireNotBuilt();
            attributes.put(Preconditions.requireNonEmpty(name, "name"), Objects.requireNonNull(attribute, "attribute"));
            return this;
        }

        /**
         * Sets static three-component vertex positions.
         *
         * @param values flat XYZ values whose length is divisible by three
         * @return this builder
         * @throws NullPointerException if {@code values} is {@code null}
         * @throws IllegalArgumentException if the values do not form complete finite positions
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder positions(float... values) {
            requireNotBuilt();
            return attribute(POSITION, BufferAttribute.of(values, 3));
        }

        /**
         * Sets static three-component vertex normals.
         *
         * @param values flat XYZ values whose length is divisible by three
         * @return this builder
         * @throws NullPointerException if {@code values} is {@code null}
         * @throws IllegalArgumentException if the values do not form complete finite normals
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder normals(float... values) {
            requireNotBuilt();
            return attribute(NORMAL, BufferAttribute.of(values, 3));
        }

        /**
         * Sets static two-component texture coordinates.
         *
         * @param values flat UV values whose length is divisible by two
         * @return this builder
         * @throws NullPointerException if {@code values} is {@code null}
         * @throws IllegalArgumentException if the values do not form complete finite coordinates
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder uvs(float... values) {
            requireNotBuilt();
            return attribute(UV, BufferAttribute.of(values, 2));
        }

        /**
         * Sets static RGB vertex colors from linear-sRGB color values.
         *
         * @param colors one color per vertex
         * @return this builder
         * @throws NullPointerException if the array or a color is {@code null}
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder vertexColors(Color... colors) {
            requireNotBuilt();
            Color[] validColors = Objects.requireNonNull(colors, "colors");
            float[] values = new float[Preconditions.requireArrayLength(validColors.length, 3, "colors")];
            for (int colorIndex = 0; colorIndex < validColors.length; colorIndex++) {
                Color color = Objects.requireNonNull(validColors[colorIndex], "colors[" + colorIndex + "]");
                int offset = colorIndex * 3;
                values[offset] = color.red();
                values[offset + 1] = color.green();
                values[offset + 2] = color.blue();
            }
            return attribute(COLOR, BufferAttribute.of(values, 3));
        }

        /**
         * Sets a shared index buffer.
         *
         * @param index index buffer to retain
         * @return this builder
         * @throws NullPointerException if {@code index} is {@code null}
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder index(IndexBuffer index) {
            requireNotBuilt();
            this.index = Objects.requireNonNull(index, "index");
            return this;
        }

        /**
         * Sets static vertex indices.
         *
         * @param values non-negative indices
         * @return this builder
         * @throws NullPointerException if {@code values} is {@code null}
         * @throws IllegalArgumentException if an index is negative
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder indices(int... values) {
            requireNotBuilt();
            return index(IndexBuffer.of(values));
        }

        /**
         * Sets an explicit initial index or vertex draw range.
         *
         * @param start non-negative first element
         * @param count non-negative number of elements
         * @return this builder
         * @throws IllegalArgumentException if either value is negative
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public Builder drawRange(int start, int count) {
            requireNotBuilt();
            drawRangeStart = Preconditions.requireNonNegative(start, "start");
            drawRangeCount = Preconditions.requireNonNegative(count, "count");
            explicitDrawRange = true;
            return this;
        }

        /**
         * Builds the configured geometry.
         *
         * @return a new open geometry
         * @throws IllegalArgumentException if attribute counts, positions, indices, or the draw
         *     range are incompatible
         * @throws IllegalStateException if this builder has already built a geometry
         */
        public BufferGeometry build() {
            requireNotBuilt();
            validateConfiguration();

            BufferGeometry geometry = new BufferGeometry();
            for (Map.Entry<String, BufferAttribute> entry : attributes.entrySet()) {
                geometry.setAttribute(entry.getKey(), entry.getValue());
            }
            IndexBuffer configuredIndex = index;
            if (configuredIndex != null) {
                geometry.setIndex(configuredIndex);
            }
            if (explicitDrawRange) {
                geometry.setDrawRange(drawRangeStart, drawRangeCount);
            }
            built = true;
            return geometry;
        }

        /** Validates cross-attribute counts, indices, and an explicit draw range. */
        private void validateConfiguration() {
            BufferAttribute positions = attributes.get(POSITION);
            if (positions != null && positions.itemSize() != 3) {
                throw new IllegalArgumentException("position attribute itemSize must be 3: " + positions.itemSize());
            }

            int vertexCount = -1;
            for (BufferAttribute attribute : attributes.values()) {
                if (vertexCount < 0) {
                    vertexCount = attribute.count();
                } else if (attribute.count() != vertexCount) {
                    throw new IllegalArgumentException(
                            "all attribute counts must match: " + attribute.count() + " != " + vertexCount);
                }
            }

            IndexBuffer configuredIndex = index;
            if (configuredIndex != null) {
                if (positions == null) {
                    throw new IllegalArgumentException("position attribute must be set before an index");
                }
                configuredIndex.requireCompatibleVertexCount(positions.count());
            }

            if (explicitDrawRange) {
                int availableCount = configuredIndex == null ? Math.max(vertexCount, 0) : configuredIndex.count();
                requireRangeWithin(drawRangeStart, drawRangeCount, availableCount);
            }
        }

        /** Rejects reuse after ownership has transferred to a built geometry. */
        private void requireNotBuilt() {
            if (built) {
                throw new IllegalStateException("BufferGeometry.Builder has already built a geometry");
            }
        }
    }

    /** Requires one builder attribute to match the configured vertex count. */
    private void requireCompatibleAttributeCount(String name, int attributeCount) {
        for (Map.Entry<String, BufferAttribute> entry : attributes.entrySet()) {
            if (!entry.getKey().equals(name) && entry.getValue().count() != attributeCount) {
                throw new IllegalArgumentException("attribute count must match existing attributes: "
                        + attributeCount
                        + " != "
                        + entry.getValue().count());
            }
        }
    }

    /** Validates a replacement position attribute against indices and peer attributes. */
    private void validatePositionReplacement(BufferAttribute replacementPosition) {
        IndexBuffer existingIndex = index;
        if (existingIndex != null) {
            existingIndex.attachVertexCount(replacementPosition.count());
            existingIndex.detachVertexCount(replacementPosition.count());
        }
    }

    /** Moves the index buffer's registered constraint when the position count changes. */
    private void updateIndexVertexCountAttachment(
            @Nullable BufferAttribute existingPosition, BufferAttribute replacementPosition) {
        IndexBuffer existingIndex = index;
        if (existingIndex != null && existingPosition != null) {
            existingIndex.replaceVertexCount(existingPosition.count(), replacementPosition.count());
        }
    }

    /** Returns the first attribute count other than an optional excluded attribute. */
    private int vertexCountExcluding(@Nullable BufferAttribute excluded) {
        for (BufferAttribute attribute : attributes.values()) {
            if (attribute != excluded) {
                return attribute.count();
            }
        }
        return 0;
    }

    /** Returns the position count, or zero when no position attribute exists. */
    private int vertexCountUnchecked() {
        return attributes.isEmpty() ? 0 : attributes.values().iterator().next().count();
    }

    /** Returns the current indexed or non-indexed element capacity. */
    private int availableElementCount() {
        IndexBuffer currentIndex = index;
        return currentIndex == null ? vertexCountUnchecked() : currentIndex.count();
    }

    /** Revalidates an explicitly configured draw range against current capacity. */
    private void requireValidExplicitDrawRange(int availableCount) {
        if (explicitDrawRange) {
            requireRangeWithin(drawRangeStart, drawRangeCount, availableCount);
        }
    }

    /** Requires a start/count pair to remain within an available element count. */
    private static void requireRangeWithin(int start, int count, int availableCount) {
        if (start > availableCount || count > availableCount - start) {
            throw new IllegalArgumentException("draw range must fit available data: start="
                    + start
                    + ", count="
                    + count
                    + ", available="
                    + availableCount);
        }
    }

    /** Returns the position attribute or rejects geometry that has none. */
    private BufferAttribute requirePositionAttribute() {
        BufferAttribute positions = attributes.get(POSITION);
        if (positions == null) {
            throw new IllegalArgumentException("position attribute must be set first");
        }
        return positions;
    }

    /** Returns a non-empty position attribute or rejects unavailable bounds input. */
    private BufferAttribute requireNonEmptyPositionAttribute() {
        BufferAttribute positions = attributes.get(POSITION);
        if (positions == null || positions.count() == 0) {
            throw new IllegalStateException("BufferGeometry must have at least one position item");
        }
        return positions;
    }

    /** Invalidates only bounds still owned by automatic computation. */
    private void invalidateComputedBounds() {
        if (computedBoundingBox) {
            boundingBox = null;
            boundingBoxPositionAttribute = null;
            computedBoundingBox = false;
        }
        if (computedBoundingSphere) {
            boundingSphere = null;
            boundingSpherePositionAttribute = null;
            computedBoundingSphere = false;
        }
    }

    /** Recomputes stale automatically managed bounds before access. */
    private void refreshComputedBounds() {
        BufferAttribute positions = attributes.get(POSITION);
        if (computedBoundingBox
                && (boundingBoxPositionAttribute != positions
                        || positions == null
                        || boundingBoxPositionVersion != positions.version())) {
            boundingBox = null;
            boundingBoxPositionAttribute = null;
            computedBoundingBox = false;
        }
        if (computedBoundingSphere
                && (boundingSpherePositionAttribute != positions
                        || positions == null
                        || boundingSpherePositionVersion != positions.version())) {
            boundingSphere = null;
            boundingSpherePositionAttribute = null;
            computedBoundingSphere = false;
        }
    }

    /** Calculates axis-aligned bounds from three-component positions. */
    private static BoundingBox calculateBoundingBox(BufferAttribute positions) {
        float minimumX = Float.POSITIVE_INFINITY;
        float minimumY = Float.POSITIVE_INFINITY;
        float minimumZ = Float.POSITIVE_INFINITY;
        float maximumX = Float.NEGATIVE_INFINITY;
        float maximumY = Float.NEGATIVE_INFINITY;
        float maximumZ = Float.NEGATIVE_INFINITY;
        for (int vertexIndex = 0; vertexIndex < positions.count(); vertexIndex++) {
            float x = positions.value(vertexIndex, 0);
            float y = positions.value(vertexIndex, 1);
            float z = positions.value(vertexIndex, 2);
            minimumX = Math.min(minimumX, x);
            minimumY = Math.min(minimumY, y);
            minimumZ = Math.min(minimumZ, z);
            maximumX = Math.max(maximumX, x);
            maximumY = Math.max(maximumY, y);
            maximumZ = Math.max(maximumZ, z);
        }
        return new BoundingBox(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ);
    }

    /** Rejects access after this geometry has been closed. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("BufferGeometry is closed");
        }
    }
}
