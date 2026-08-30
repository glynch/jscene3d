/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Library-owned non-negative 32-bit vertex indices.
 *
 * <p>Construction and snapshot access copy data. Controlled edits mutate owned storage directly
 * and maintain one upload-visible version. When shared by geometries, edits are checked against
 * every attached vertex count. Instances are mutable and are not thread-safe.
 */
public final class IndexBuffer {
    private final int[] data;
    private final BufferUsage usage;
    private final TreeMap<Integer, Integer> attachedVertexCounts;
    private final EditorImpl editor;

    private int maximumIndex;
    private long version;
    private boolean editing;

    private IndexBuffer(int[] data, BufferUsage usage, int maximumIndex) {
        this.data = data;
        this.usage = usage;
        this.maximumIndex = maximumIndex;
        attachedVertexCounts = new TreeMap<>();
        editor = new EditorImpl();
    }

    /**
     * Creates a static index buffer by copying caller-owned data.
     *
     * @param data indices to copy
     * @return the created index buffer
     * @throws NullPointerException if {@code data} is {@code null}
     * @throws IllegalArgumentException if any index is negative
     */
    public static IndexBuffer of(int[] data) {
        return of(data, BufferUsage.STATIC);
    }

    /**
     * Creates an index buffer by copying caller-owned data.
     *
     * @param data indices to copy
     * @param usage expected mutation frequency
     * @return the created index buffer
     * @throws NullPointerException if {@code data} or {@code usage} is {@code null}
     * @throws IllegalArgumentException if any index is negative
     */
    public static IndexBuffer of(int[] data, BufferUsage usage) {
        int[] validData = Objects.requireNonNull(data, "data");
        BufferUsage validUsage = Objects.requireNonNull(usage, "usage");
        int[] copiedData = validData.clone();
        int maximumIndex = -1;
        for (int index = 0; index < copiedData.length; index++) {
            int value = Preconditions.requireNonNegative(copiedData[index], "data[" + index + "]");
            maximumIndex = Math.max(maximumIndex, value);
        }
        return new IndexBuffer(copiedData, validUsage, maximumIndex);
    }

    /**
     * Returns the number of indices.
     *
     * @return the index count, possibly zero
     */
    public int count() {
        return data.length;
    }

    /**
     * Returns the expected mutation frequency.
     *
     * @return the usage hint
     */
    public BufferUsage usage() {
        return usage;
    }

    /**
     * Returns the current upload-visible data version.
     *
     * @return the version, initially zero
     */
    public long version() {
        return version;
    }

    /**
     * Returns an index.
     *
     * @param index zero-based index position
     * @return the stored non-negative vertex index
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer
     */
    public int value(int index) {
        return data[Objects.checkIndex(index, data.length)];
    }

    /**
     * Changes an index and records an immediate version change when its value differs.
     *
     * @param index zero-based index position
     * @param value non-negative vertex index valid for every attached geometry
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer
     * @throws IllegalArgumentException if {@code value} is negative or outside an attached
     *     geometry's vertex range
     * @throws IllegalStateException if called from this buffer's active scoped edit
     */
    public void set(int index, int value) {
        requireNotEditing();
        int dataIndex = Objects.checkIndex(index, data.length);
        int validValue = requireValidValue(value);
        if (data[dataIndex] != validValue) {
            int oldValue = data[dataIndex];
            data[dataIndex] = validValue;
            updateMaximumAfterChange(oldValue, validValue);
            version++;
        }
    }

    /**
     * Performs a bounded batch edit directly against owned storage.
     *
     * <p>The editor expires when the callback returns. Actual changes produce exactly one version
     * increment, even when the callback later throws; changes are not rolled back.
     *
     * @param operation edit callback
     * @throws NullPointerException if {@code operation} is {@code null}
     * @throws IllegalStateException if an edit is already active
     */
    public void edit(Consumer<Editor> operation) {
        Consumer<Editor> validOperation = Objects.requireNonNull(operation, "operation");
        if (editing) {
            throw new IllegalStateException("Nested IndexBuffer edits are unsupported");
        }
        editing = true;
        editor.activate();
        try {
            validOperation.accept(editor);
        } finally {
            boolean changed = editor.deactivate();
            editing = false;
            if (changed) {
                recomputeMaximumIndex();
                version++;
            }
        }
    }

    /**
     * Copies all indices into a new array.
     *
     * @return an independent snapshot
     */
    public int[] toArray() {
        return Arrays.copyOf(data, data.length);
    }

    void attachVertexCount(int vertexCount) {
        requireCompatibleVertexCount(vertexCount);
        attachedVertexCounts.merge(vertexCount, 1, Integer::sum);
    }

    void detachVertexCount(int vertexCount) {
        Integer attachmentCount = attachedVertexCounts.get(vertexCount);
        if (attachmentCount == null) {
            throw new IllegalStateException("IndexBuffer vertex-count attachment is inconsistent");
        }
        if (attachmentCount == 1) {
            attachedVertexCounts.remove(vertexCount);
        } else {
            attachedVertexCounts.put(vertexCount, attachmentCount - 1);
        }
    }

    void replaceVertexCount(int oldVertexCount, int newVertexCount) {
        requireCompatibleVertexCount(newVertexCount);
        detachVertexCount(oldVertexCount);
        attachedVertexCounts.merge(newVertexCount, 1, Integer::sum);
    }

    private int requireValidValue(int value) {
        int validValue = Preconditions.requireNonNegative(value, "value");
        if (!attachedVertexCounts.isEmpty() && validValue >= attachedVertexCounts.firstKey()) {
            throw new IllegalArgumentException(
                    "value must be less than every attached geometry vertex count: " + validValue);
        }
        return validValue;
    }

    private void requireCompatibleVertexCount(int vertexCount) {
        int validVertexCount = Preconditions.requireNonNegative(vertexCount, "vertexCount");
        if (maximumIndex >= validVertexCount) {
            throw new IllegalArgumentException(
                    "maximum index must be less than vertexCount: " + maximumIndex + " >= " + validVertexCount);
        }
    }

    private void updateMaximumAfterChange(int oldValue, int newValue) {
        if (newValue > maximumIndex) {
            maximumIndex = newValue;
        } else if (oldValue == maximumIndex && newValue < oldValue) {
            recomputeMaximumIndex();
        }
    }

    private void recomputeMaximumIndex() {
        maximumIndex = -1;
        for (int value : data) {
            maximumIndex = Math.max(maximumIndex, value);
        }
    }

    private void requireNotEditing() {
        if (editing) {
            throw new IllegalStateException("Use the scoped editor while an IndexBuffer edit is active");
        }
    }

    /** A short-lived controlled view used only during {@link #edit(Consumer)}. */
    public interface Editor {
        /**
         * Changes one index.
         *
         * @param index zero-based index position
         * @param value non-negative vertex index valid for every attached geometry
         * @throws IndexOutOfBoundsException if {@code index} is outside the buffer
         * @throws IllegalArgumentException if {@code value} is negative or outside an attached
         *     geometry's vertex range
         * @throws IllegalStateException if this editor is no longer active
         */
        void set(int index, int value);
    }

    private final class EditorImpl implements Editor {
        private boolean active;
        private boolean changed;

        @Override
        public void set(int index, int value) {
            requireActive();
            int dataIndex = Objects.checkIndex(index, data.length);
            int validValue = requireValidValue(value);
            if (data[dataIndex] != validValue) {
                data[dataIndex] = validValue;
                changed = true;
            }
        }

        void activate() {
            active = true;
            changed = false;
        }

        boolean deactivate() {
            active = false;
            return changed;
        }

        private void requireActive() {
            if (!active) {
                throw new IllegalStateException("IndexBuffer editor is no longer active");
            }
        }
    }
}
