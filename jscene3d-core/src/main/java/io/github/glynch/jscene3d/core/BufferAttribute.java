/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Library-owned floating-point vertex data grouped into fixed-size items.
 *
 * <p>Construction copies the supplied array once. Snapshot access copies data back out, while
 * controlled setters and scoped editing mutate the owned storage directly. Each actual scalar
 * change increments {@link #version()} immediately; a scoped edit increments it at most once.
 * Instances are mutable and are not thread-safe.
 */
public final class BufferAttribute {
    private final float[] data;
    private final int itemSize;
    private final int count;
    private final BufferUsage usage;
    private final EditorImpl editor;

    private long version;
    private boolean editing;

    /** Retains validated, exclusively owned attribute data. */
    private BufferAttribute(float[] data, int itemSize, BufferUsage usage) {
        this.data = data;
        this.itemSize = itemSize;
        count = data.length / itemSize;
        this.usage = usage;
        editor = new EditorImpl();
    }

    /**
     * Creates a static attribute by copying caller-owned data.
     *
     * @param data flat item data to copy
     * @param itemSize positive number of scalar components in each item
     * @return the created attribute
     * @throws NullPointerException if {@code data} is {@code null}
     * @throws IllegalArgumentException if {@code itemSize} is not positive, the data length is not
     *     divisible by it, or any value is not finite
     */
    public static BufferAttribute of(float[] data, int itemSize) {
        return of(data, itemSize, BufferUsage.STATIC);
    }

    /**
     * Creates an attribute by copying caller-owned data.
     *
     * @param data flat item data to copy
     * @param itemSize positive number of scalar components in each item
     * @param usage expected mutation frequency
     * @return the created attribute
     * @throws NullPointerException if {@code data} or {@code usage} is {@code null}
     * @throws IllegalArgumentException if {@code itemSize} is not positive, the data length is not
     *     divisible by it, or any value is not finite
     */
    public static BufferAttribute of(float[] data, int itemSize, BufferUsage usage) {
        float[] validData = Objects.requireNonNull(data, "data");
        int validItemSize = Preconditions.requirePositive(itemSize, "itemSize");
        BufferUsage validUsage = Objects.requireNonNull(usage, "usage");
        if (validData.length % validItemSize != 0) {
            throw new IllegalArgumentException(
                    "data length must be divisible by itemSize: " + validData.length + " % " + validItemSize);
        }
        float[] copiedData = validData.clone();
        for (int index = 0; index < copiedData.length; index++) {
            Preconditions.requireFinite(copiedData[index], "data[" + index + "]");
        }
        return new BufferAttribute(copiedData, validItemSize, validUsage);
    }

    /**
     * Returns the scalar components in each item.
     *
     * @return the positive item size
     */
    public int itemSize() {
        return itemSize;
    }

    /**
     * Returns the number of complete items.
     *
     * @return the item count, possibly zero
     */
    public int count() {
        return count;
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
     * Returns one component of an item.
     *
     * @param itemIndex zero-based item index
     * @param componentIndex zero-based component index within the item
     * @return the stored value
     * @throws IndexOutOfBoundsException if either index is outside its valid range
     */
    public float value(int itemIndex, int componentIndex) {
        return data[dataIndex(itemIndex, componentIndex)];
    }

    /**
     * Changes one component and records an immediate version change when its value differs.
     *
     * @param itemIndex zero-based item index
     * @param componentIndex zero-based component index within the item
     * @param value finite replacement value
     * @throws IndexOutOfBoundsException if either index is outside its valid range
     * @throws IllegalArgumentException if {@code value} is not finite
     * @throws IllegalStateException if called from this attribute's active scoped edit
     */
    public void set(int itemIndex, int componentIndex, float value) {
        requireNotEditing();
        if (setValue(itemIndex, componentIndex, value)) {
            version++;
        }
    }

    /**
     * Changes the first component of an item.
     *
     * @param itemIndex zero-based item index
     * @param x finite first component
     * @throws IndexOutOfBoundsException if the item or component does not exist
     * @throws IllegalArgumentException if {@code x} is not finite
     * @throws IllegalStateException if called from this attribute's active scoped edit
     */
    public void setX(int itemIndex, float x) {
        set(itemIndex, 0, x);
    }

    /**
     * Changes the first two components of an item with one version change.
     *
     * @param itemIndex zero-based item index
     * @param x finite first component
     * @param y finite second component
     * @throws IndexOutOfBoundsException if the item or either component does not exist
     * @throws IllegalArgumentException if a value is not finite
     * @throws IllegalStateException if called from this attribute's active scoped edit
     */
    public void setXY(int itemIndex, float x, float y) {
        setComponents(itemIndex, x, y, 0.0f, 0.0f, 2);
    }

    /**
     * Changes the first three components of an item with one version change.
     *
     * @param itemIndex zero-based item index
     * @param x finite first component
     * @param y finite second component
     * @param z finite third component
     * @throws IndexOutOfBoundsException if the item or a component does not exist
     * @throws IllegalArgumentException if a value is not finite
     * @throws IllegalStateException if called from this attribute's active scoped edit
     */
    public void setXYZ(int itemIndex, float x, float y, float z) {
        setComponents(itemIndex, x, y, z, 0.0f, 3);
    }

    /**
     * Changes the first four components of an item with one version change.
     *
     * @param itemIndex zero-based item index
     * @param x finite first component
     * @param y finite second component
     * @param z finite third component
     * @param w finite fourth component
     * @throws IndexOutOfBoundsException if the item or a component does not exist
     * @throws IllegalArgumentException if a value is not finite
     * @throws IllegalStateException if called from this attribute's active scoped edit
     */
    public void setXYZW(int itemIndex, float x, float y, float z, float w) {
        setComponents(itemIndex, x, y, z, w, 4);
    }

    /**
     * Performs a bounded batch edit directly against owned storage.
     *
     * <p>The supplied editor is valid only during the callback. Actual changes produce exactly one
     * version increment, including when the callback later throws; changes are not rolled back and
     * the original exception is propagated.
     *
     * @param operation edit callback
     * @throws NullPointerException if {@code operation} is {@code null}
     * @throws IllegalStateException if an edit is already active
     */
    public void edit(Consumer<Editor> operation) {
        Consumer<Editor> validOperation = Objects.requireNonNull(operation, "operation");
        if (editing) {
            throw new IllegalStateException("Nested BufferAttribute edits are unsupported");
        }
        editing = true;
        editor.activate();
        try {
            validOperation.accept(editor);
        } finally {
            boolean changed = editor.deactivate();
            editing = false;
            if (changed) {
                version++;
            }
        }
    }

    /**
     * Copies all scalar data into a new array.
     *
     * @return an independent snapshot
     */
    public float[] toArray() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Copies all scalar data into an existing array.
     *
     * @param destination array whose length must equal the scalar-data length
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalArgumentException if the destination length differs from the data length
     */
    public void copyTo(float[] destination) {
        float[] validDestination = Objects.requireNonNull(destination, "destination");
        if (validDestination.length != data.length) {
            throw new IllegalArgumentException(
                    "destination length must equal data length: " + validDestination.length + " != " + data.length);
        }
        System.arraycopy(data, 0, validDestination, 0, data.length);
    }

    /** Replaces one validated component and reports whether its value changed. */
    private boolean setValue(int itemIndex, int componentIndex, float value) {
        int index = dataIndex(itemIndex, componentIndex);
        float validValue = Preconditions.requireFinite(value, "value");
        if (data[index] == validValue) {
            return false;
        }
        data[index] = validValue;
        return true;
    }

    /** Replaces a validated item prefix as one versioned mutation. */
    private void setComponents(int itemIndex, float x, float y, float z, float w, int componentCount) {
        requireNotEditing();
        Objects.checkIndex(itemIndex, count);
        if (itemSize < componentCount) {
            throw new IndexOutOfBoundsException(
                    "itemSize " + itemSize + " does not contain " + componentCount + " components");
        }
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        float validZ = componentCount >= 3 ? Preconditions.requireFinite(z, "z") : z;
        float validW = componentCount >= 4 ? Preconditions.requireFinite(w, "w") : w;
        int start = itemIndex * itemSize;
        boolean changed = data[start] != validX || data[start + 1] != validY;
        if (componentCount >= 3) {
            changed |= data[start + 2] != validZ;
        }
        if (componentCount >= 4) {
            changed |= data[start + 3] != validW;
        }
        if (changed) {
            data[start] = validX;
            data[start + 1] = validY;
            if (componentCount >= 3) {
                data[start + 2] = validZ;
            }
            if (componentCount >= 4) {
                data[start + 3] = validW;
            }
            version++;
        }
    }

    /** Returns the checked flat-array index for an item component. */
    private int dataIndex(int itemIndex, int componentIndex) {
        Objects.checkIndex(itemIndex, count);
        Objects.checkIndex(componentIndex, itemSize);
        return itemIndex * itemSize + componentIndex;
    }

    /** Rejects direct mutation while the scoped editor owns changes. */
    private void requireNotEditing() {
        if (editing) {
            throw new IllegalStateException("Use the scoped editor while a BufferAttribute edit is active");
        }
    }

    /** A short-lived controlled view used only during {@link #edit(Consumer)}. */
    public interface Editor {
        /**
         * Changes one component.
         *
         * @param itemIndex zero-based item index
         * @param componentIndex zero-based component index
         * @param value finite replacement value
         * @throws IndexOutOfBoundsException if either index is outside its valid range
         * @throws IllegalArgumentException if {@code value} is not finite
         * @throws IllegalStateException if this editor is no longer active
         */
        void set(int itemIndex, int componentIndex, float value);

        /**
         * Changes the first component.
         *
         * @param itemIndex zero-based item index
         * @param x finite first component
         * @throws IndexOutOfBoundsException if the item or component does not exist
         * @throws IllegalArgumentException if {@code x} is not finite
         * @throws IllegalStateException if this editor is no longer active
         */
        void setX(int itemIndex, float x);

        /**
         * Changes the first two components.
         *
         * @param itemIndex zero-based item index
         * @param x finite first component
         * @param y finite second component
         * @throws IndexOutOfBoundsException if the item or either component does not exist
         * @throws IllegalArgumentException if a value is not finite
         * @throws IllegalStateException if this editor is no longer active
         */
        void setXY(int itemIndex, float x, float y);

        /**
         * Changes the first three components.
         *
         * @param itemIndex zero-based item index
         * @param x finite first component
         * @param y finite second component
         * @param z finite third component
         * @throws IndexOutOfBoundsException if the item or a component does not exist
         * @throws IllegalArgumentException if a value is not finite
         * @throws IllegalStateException if this editor is no longer active
         */
        void setXYZ(int itemIndex, float x, float y, float z);

        /**
         * Changes the first four components.
         *
         * @param itemIndex zero-based item index
         * @param x finite first component
         * @param y finite second component
         * @param z finite third component
         * @param w finite fourth component
         * @throws IndexOutOfBoundsException if the item or a component does not exist
         * @throws IllegalArgumentException if a value is not finite
         * @throws IllegalStateException if this editor is no longer active
         */
        void setXYZW(int itemIndex, float x, float y, float z, float w);
    }

    /** Reusable scoped editor whose access is guarded by the active edit operation. */
    private final class EditorImpl implements Editor {
        private boolean active;
        private boolean changed;

        @Override
        public void set(int itemIndex, int componentIndex, float value) {
            requireActive();
            changed |= setValue(itemIndex, componentIndex, value);
        }

        @Override
        public void setX(int itemIndex, float x) {
            set(itemIndex, 0, x);
        }

        @Override
        public void setXY(int itemIndex, float x, float y) {
            set(itemIndex, 0, x);
            set(itemIndex, 1, y);
        }

        @Override
        public void setXYZ(int itemIndex, float x, float y, float z) {
            set(itemIndex, 0, x);
            set(itemIndex, 1, y);
            set(itemIndex, 2, z);
        }

        @Override
        public void setXYZW(int itemIndex, float x, float y, float z, float w) {
            set(itemIndex, 0, x);
            set(itemIndex, 1, y);
            set(itemIndex, 2, z);
            set(itemIndex, 3, w);
        }

        /** Starts an edit operation and clears its change marker. */
        void activate() {
            active = true;
            changed = false;
        }

        /** Ends an edit operation and reports whether it changed any component. */
        boolean deactivate() {
            active = false;
            return changed;
        }

        /** Rejects use of this editor outside its scoped operation. */
        private void requireActive() {
            if (!active) {
                throw new IllegalStateException("BufferAttribute editor is no longer active");
            }
        }
    }
}
