/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Base component for an ordered, cycle-free scene hierarchy.
 *
 * <p>Each object has at most one parent. Adding an object to a new parent performs reparenting and
 * preserves its local state. Hierarchy access and mutation are confined to the caller's scene
 * thread; concurrent use from multiple threads is unsupported.
 *
 * <p>Structural mutation of an affected tree during traversal is rejected before the mutation
 * occurs. Nested read-only traversal is supported.
 */
// Parent and child relationships deliberately use stable Object3D reference identity.
@SuppressWarnings("ReferenceEquality")
public class Object3D {
    private static final String TRAVERSAL_MUTATION_MESSAGE = "Scene-graph structure cannot change during traversal";

    private final List<Object3D> children;
    private final List<Object3D> childrenView;
    private final Vector3f position;
    private final Quaternionf quaternion;
    private final Vector3f scale;
    private final Matrix4f matrix;
    private final Matrix4f matrixWorld;

    private @Nullable Object3D parent;
    private @Nullable Object3D resolvedWorldParent;
    private boolean visible;
    private int activeTraversalCount;
    private long localTransformVersion;
    private long localMatrixVersion;
    private long resolvedWorldLocalMatrixVersion;
    private long resolvedParentWorldMatrixVersion;
    private long worldMatrixVersion;

    /** Creates a visible, unparented object with no children. */
    public Object3D() {
        children = new ArrayList<>();
        childrenView = Collections.unmodifiableList(children);
        position = new Vector3f();
        quaternion = new Quaternionf();
        scale = new Vector3f(1.0f);
        matrix = new Matrix4f();
        matrixWorld = new Matrix4f();
        visible = true;
    }

    /**
     * Returns the live read-only view of this object's local position.
     *
     * @return the stable local-position view
     */
    public final Vector3fc position() {
        return position;
    }

    /**
     * Returns the live read-only view of this object's normalized local orientation.
     *
     * @return the stable local-quaternion view
     */
    public final Quaternionfc quaternion() {
        return quaternion;
    }

    /**
     * Returns the live read-only view of this object's local scale.
     *
     * @return the stable local-scale view
     */
    public final Vector3fc scale() {
        return scale;
    }

    /**
     * Sets this object's local position.
     *
     * @param x local X coordinate
     * @param y local Y coordinate
     * @param z local Z coordinate
     * @throws IllegalArgumentException if any coordinate is not finite
     */
    public final void setPosition(float x, float y, float z) {
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        float validZ = Preconditions.requireFinite(z, "z");
        if (!position.equals(validX, validY, validZ)) {
            position.set(validX, validY, validZ);
            markLocalTransformChanged();
        }
    }

    /**
     * Copies an existing value into this object's local position.
     *
     * @param position position to copy
     * @throws NullPointerException if {@code position} is {@code null}
     * @throws IllegalArgumentException if any coordinate is not finite
     */
    public final void setPosition(Vector3fc position) {
        Vector3fc validPosition = Preconditions.requireFinite(position, "position");
        setPosition(validPosition.x(), validPosition.y(), validPosition.z());
    }

    /**
     * Sets and normalizes this object's local orientation.
     *
     * @param x quaternion X component
     * @param y quaternion Y component
     * @param z quaternion Z component
     * @param w quaternion W component
     * @throws IllegalArgumentException if any component is not finite or all components are zero
     */
    public final void setQuaternion(float x, float y, float z, float w) {
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        float validZ = Preconditions.requireFinite(z, "z");
        float validW = Preconditions.requireFinite(w, "w");
        float largestComponent =
                Math.max(Math.max(Math.abs(validX), Math.abs(validY)), Math.max(Math.abs(validZ), Math.abs(validW)));
        if (largestComponent == 0.0f) {
            throw new IllegalArgumentException("quaternion must not have zero length");
        }
        float scaledX = validX / largestComponent;
        float scaledY = validY / largestComponent;
        float scaledZ = validZ / largestComponent;
        float scaledW = validW / largestComponent;
        float inverseLength = (float)
                (1.0 / Math.sqrt(scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ + scaledW * scaledW));
        float normalizedX = scaledX * inverseLength;
        float normalizedY = scaledY * inverseLength;
        float normalizedZ = scaledZ * inverseLength;
        float normalizedW = scaledW * inverseLength;
        if (!quaternion.equals(normalizedX, normalizedY, normalizedZ, normalizedW)) {
            quaternion.set(normalizedX, normalizedY, normalizedZ, normalizedW);
            markLocalTransformChanged();
        }
    }

    /**
     * Copies and normalizes an existing value into this object's local orientation.
     *
     * @param quaternion quaternion to copy
     * @throws NullPointerException if {@code quaternion} is {@code null}
     * @throws IllegalArgumentException if any component is not finite or all components are zero
     */
    public final void setQuaternion(Quaternionfc quaternion) {
        Quaternionfc validQuaternion = Preconditions.requireFinite(quaternion, "quaternion");
        setQuaternion(validQuaternion.x(), validQuaternion.y(), validQuaternion.z(), validQuaternion.w());
    }

    /**
     * Sets this object's local scale.
     *
     * @param x local X scale
     * @param y local Y scale
     * @param z local Z scale
     * @throws IllegalArgumentException if any component is not finite
     */
    public final void setScale(float x, float y, float z) {
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        float validZ = Preconditions.requireFinite(z, "z");
        if (!scale.equals(validX, validY, validZ)) {
            scale.set(validX, validY, validZ);
            markLocalTransformChanged();
        }
    }

    /**
     * Copies an existing value into this object's local scale.
     *
     * @param scale scale to copy
     * @throws NullPointerException if {@code scale} is {@code null}
     * @throws IllegalArgumentException if any component is not finite
     */
    public final void setScale(Vector3fc scale) {
        Vector3fc validScale = Preconditions.requireFinite(scale, "scale");
        setScale(validScale.x(), validScale.y(), validScale.z());
    }

    /**
     * Applies a local X-axis rotation in radians.
     *
     * @param angle angle in radians
     * @throws IllegalArgumentException if {@code angle} is not finite
     */
    public final void rotateX(float angle) {
        float validAngle = Preconditions.requireFinite(angle, "angle");
        if (validAngle != 0.0f) {
            quaternion.rotateX(validAngle).normalize();
            markLocalTransformChanged();
        }
    }

    /**
     * Applies a local Y-axis rotation in radians.
     *
     * @param angle angle in radians
     * @throws IllegalArgumentException if {@code angle} is not finite
     */
    public final void rotateY(float angle) {
        float validAngle = Preconditions.requireFinite(angle, "angle");
        if (validAngle != 0.0f) {
            quaternion.rotateY(validAngle).normalize();
            markLocalTransformChanged();
        }
    }

    /**
     * Applies a local Z-axis rotation in radians.
     *
     * @param angle angle in radians
     * @throws IllegalArgumentException if {@code angle} is not finite
     */
    public final void rotateZ(float angle) {
        float validAngle = Preconditions.requireFinite(angle, "angle");
        if (validAngle != 0.0f) {
            quaternion.rotateZ(validAngle).normalize();
            markLocalTransformChanged();
        }
    }

    /**
     * Replaces this object's local orientation from Euler angles in radians.
     *
     * @param x rotation about the X axis in radians
     * @param y rotation about the Y axis in radians
     * @param z rotation about the Z axis in radians
     * @param order rotation order
     * @throws NullPointerException if {@code order} is {@code null}
     * @throws IllegalArgumentException if any angle is not finite
     */
    public final void setRotationFromEuler(float x, float y, float z, RotationOrder order) {
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        float validZ = Preconditions.requireFinite(z, "z");
        RotationOrder validOrder = Objects.requireNonNull(order, "order");
        validOrder.setQuaternion(quaternion, validX, validY, validZ);
        quaternion.normalize();
        markLocalTransformChanged();
    }

    /**
     * Returns the current local transform matrix, composing it lazily when required.
     *
     * @return the stable live read-only local-matrix view
     */
    public final Matrix4fc matrix() {
        updateLocalMatrix();
        return matrix;
    }

    /**
     * Returns the current world transform matrix, updating the ancestor path iteratively.
     *
     * @return the stable live read-only world-matrix view
     */
    public final Matrix4fc matrixWorld() {
        updateWorldMatrix();
        return matrixWorld;
    }

    /**
     * Copies this object's current world position into caller-owned storage.
     *
     * @param destination vector receiving the world position
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public final Vector3f worldPosition(Vector3f destination) {
        Vector3f validDestination = Objects.requireNonNull(destination, "destination");
        updateWorldMatrix();
        return matrixWorld.getTranslation(validDestination);
    }

    /**
     * Copies this object's current world orientation into caller-owned storage.
     *
     * @param destination quaternion receiving the normalized world orientation
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public final Quaternionf worldQuaternion(Quaternionf destination) {
        Quaternionf validDestination = Objects.requireNonNull(destination, "destination");
        updateWorldMatrix();
        return matrixWorld.getUnnormalizedRotation(validDestination).normalize();
    }

    /**
     * Copies this object's current world scale into caller-owned storage.
     *
     * <p>When the world transform contains a reflection, its sign is reported on the X component,
     * matching conventional affine decomposition.
     *
     * @param destination vector receiving the world scale
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public final Vector3f worldScale(Vector3f destination) {
        Vector3f validDestination = Objects.requireNonNull(destination, "destination");
        updateWorldMatrix();
        matrixWorld.getScale(validDestination);
        if (matrixWorld.determinant3x3() < 0.0f) {
            validDestination.set(-validDestination.x(), validDestination.y(), validDestination.z());
        }
        return validDestination;
    }

    /**
     * Returns this object's parent.
     *
     * @return the parent, or {@code null} when this object is a hierarchy root
     */
    public final @Nullable Object3D parent() {
        return parent;
    }

    /**
     * Returns the ordered children of this object.
     *
     * <p>The returned list is a stable, unmodifiable live view. Hierarchy changes must use {@link
     * #add(Object3D)}, {@link #remove(Object3D)}, {@link #detach()}, or {@link #clear()}.
     *
     * @return the stable children view in insertion order
     */
    public final List<Object3D> children() {
        return childrenView;
    }

    /**
     * Returns whether this object participates in visible traversal.
     *
     * @return {@code true} by default
     */
    public final boolean isVisible() {
        return visible;
    }

    /**
     * Changes whether this object and its descendants participate in visible traversal.
     *
     * @param visible whether this object is visible
     */
    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Adds or reparents a child at the end of this object's child order.
     *
     * <p>The complete relationship is validated before an existing parent is changed. Adding the
     * same child to the same parent is an idempotent no-op and does not reorder it.
     *
     * @param child the child to add
     * @return {@code true} when the hierarchy changed
     * @throws NullPointerException if {@code child} is {@code null}
     * @throws IllegalArgumentException if the relationship would be self-parenting or cyclic
     * @throws ConcurrentModificationException if an affected tree is being traversed
     */
    public final boolean add(Object3D child) {
        Object3D validChild = Objects.requireNonNull(child, "child");
        validateNewChild(validChild);
        if (validChild.parent == this) {
            return false;
        }

        Object3D newTreeRoot = treeRoot();
        Object3D oldTreeRoot = validChild.treeRoot();
        requireStructureMutable(newTreeRoot);
        if (oldTreeRoot != newTreeRoot) {
            requireStructureMutable(oldTreeRoot);
        }

        Object3D oldParent = validChild.parent;
        if (oldParent != null) {
            oldParent.removeKnownChild(validChild);
        }
        children.add(validChild);
        validChild.parent = this;
        return true;
    }

    /**
     * Removes a direct child while preserving the child's local state.
     *
     * @param child the child to remove
     * @return {@code true} when the hierarchy changed
     * @throws NullPointerException if {@code child} is {@code null}
     * @throws ConcurrentModificationException if this tree is being traversed
     */
    public final boolean remove(Object3D child) {
        Object3D validChild = Objects.requireNonNull(child, "child");
        if (validChild.parent != this) {
            return false;
        }
        requireStructureMutable(treeRoot());
        removeKnownChild(validChild);
        return true;
    }

    /**
     * Detaches this object from its parent while preserving its local state.
     *
     * @return {@code true} when this object previously had a parent
     * @throws ConcurrentModificationException if this tree is being traversed
     */
    public final boolean detach() {
        Object3D currentParent = parent;
        return currentParent != null && currentParent.remove(this);
    }

    /**
     * Removes every direct child while preserving each child's local state.
     *
     * @throws ConcurrentModificationException if this tree is being traversed
     */
    public final void clear() {
        if (children.isEmpty()) {
            return;
        }
        requireStructureMutable(treeRoot());
        for (int index = 0; index < children.size(); index++) {
            children.get(index).parent = null;
        }
        children.clear();
    }

    /**
     * Visits this object and every descendant in deterministic depth-first preorder.
     *
     * <p>Structural mutation of this tree from the visitor is unsupported and rejected. Nested
     * read-only traversal is permitted.
     *
     * @param visitor operation invoked once for each object
     * @throws NullPointerException if {@code visitor} is {@code null}
     * @throws ConcurrentModificationException if the visitor attempts structural mutation
     */
    public final void traverse(Consumer<Object3D> visitor) {
        Consumer<Object3D> validVisitor = Objects.requireNonNull(visitor, "visitor");
        Object3D root = treeRoot();
        root.activeTraversalCount++;
        try {
            traverseDepthFirst(validVisitor);
        } finally {
            root.activeTraversalCount--;
        }
    }

    /**
     * Visits visible objects in deterministic depth-first preorder.
     *
     * <p>An invisible object and its complete subtree are skipped. Structural mutation follows the
     * same rules as {@link #traverse(Consumer)}.
     *
     * @param visitor operation invoked once for each visible object
     * @throws NullPointerException if {@code visitor} is {@code null}
     * @throws ConcurrentModificationException if the visitor attempts structural mutation
     */
    public final void traverseVisible(Consumer<Object3D> visitor) {
        Consumer<Object3D> validVisitor = Objects.requireNonNull(visitor, "visitor");
        Object3D root = treeRoot();
        root.activeTraversalCount++;
        try {
            traverseVisibleDepthFirst(validVisitor);
        } finally {
            root.activeTraversalCount--;
        }
    }

    private void validateNewChild(Object3D child) {
        if (child == this) {
            throw new IllegalArgumentException("An Object3D cannot be its own child");
        }
        for (Object3D ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor == child) {
                throw new IllegalArgumentException("Adding the child would create a scene-graph cycle");
            }
        }
    }

    private Object3D treeRoot() {
        Object3D root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root;
    }

    private static void requireStructureMutable(Object3D root) {
        if (root.activeTraversalCount > 0) {
            throw new ConcurrentModificationException(TRAVERSAL_MUTATION_MESSAGE);
        }
    }

    private void removeKnownChild(Object3D child) {
        int childIndex = -1;
        for (int index = 0; index < children.size(); index++) {
            if (children.get(index) == child) {
                childIndex = index;
                break;
            }
        }
        if (childIndex < 0) {
            throw new IllegalStateException("Scene-graph parent and child state is inconsistent");
        }
        children.remove(childIndex);
        child.parent = null;
    }

    private void traverseDepthFirst(Consumer<Object3D> visitor) {
        ArrayDeque<Object3D> pending = new ArrayDeque<>();
        pending.push(this);
        while (!pending.isEmpty()) {
            Object3D current = pending.pop();
            visitor.accept(current);
            for (int index = current.children.size() - 1; index >= 0; index--) {
                pending.push(current.children.get(index));
            }
        }
    }

    private void traverseVisibleDepthFirst(Consumer<Object3D> visitor) {
        ArrayDeque<Object3D> pending = new ArrayDeque<>();
        pending.push(this);
        while (!pending.isEmpty()) {
            Object3D current = pending.pop();
            if (!current.visible) {
                continue;
            }
            visitor.accept(current);
            for (int index = current.children.size() - 1; index >= 0; index--) {
                pending.push(current.children.get(index));
            }
        }
    }

    private void markLocalTransformChanged() {
        localTransformVersion++;
    }

    private void updateLocalMatrix() {
        if (localMatrixVersion != localTransformVersion) {
            matrix.translationRotateScale(position, quaternion, scale);
            localMatrixVersion = localTransformVersion;
        }
    }

    private void updateWorldMatrix() {
        ArrayDeque<Object3D> ancestorPath = new ArrayDeque<>();
        for (Object3D ancestor = this; ancestor != null; ancestor = ancestor.parent) {
            ancestorPath.push(ancestor);
        }

        Object3D resolvedParent = null;
        while (!ancestorPath.isEmpty()) {
            Object3D current = ancestorPath.pop();
            current.updateLocalMatrix();
            current.resolveWorldMatrix(resolvedParent);
            resolvedParent = current;
        }
    }

    final long matrixWorldVersion() {
        return worldMatrixVersion;
    }

    private void resolveWorldMatrix(@Nullable Object3D resolvedParent) {
        long parentWorldVersion = resolvedParent == null ? 0L : resolvedParent.worldMatrixVersion;
        if (resolvedWorldLocalMatrixVersion == localMatrixVersion
                && resolvedWorldParent == resolvedParent
                && resolvedParentWorldMatrixVersion == parentWorldVersion) {
            return;
        }

        if (resolvedParent == null) {
            matrixWorld.set(matrix);
        } else {
            resolvedParent.matrixWorld.mul(matrix, matrixWorld);
        }
        resolvedWorldLocalMatrixVersion = localMatrixVersion;
        resolvedWorldParent = resolvedParent;
        resolvedParentWorldMatrixVersion = parentWorldVersion;
        worldMatrixVersion++;
    }
}
