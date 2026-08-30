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

    private @Nullable Object3D parent;
    private boolean visible;
    private int activeTraversalCount;

    /** Creates a visible, unparented object with no children. */
    public Object3D() {
        children = new ArrayList<>();
        childrenView = Collections.unmodifiableList(children);
        visible = true;
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
}
