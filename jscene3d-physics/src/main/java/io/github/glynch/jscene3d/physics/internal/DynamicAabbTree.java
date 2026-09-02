/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Incrementally maintained bounding-volume hierarchy for broad-phase queries. */
final class DynamicAabbTree {
    private static final float FAT_MARGIN = 0.1F;

    private final Map<Collider, Node> leaves = new IdentityHashMap<>();
    private @Nullable Node root;

    void add(Collider collider, Aabb bounds) {
        Node leaf = Node.leaf(collider, bounds.expanded(FAT_MARGIN));
        leaves.put(collider, leaf);
        insert(leaf);
    }

    void remove(Collider collider) {
        Node leaf = leaves.remove(collider);
        if (leaf != null) {
            detach(leaf);
        }
    }

    void update(Collider collider, Aabb bounds) {
        Node leaf = leaves.get(collider);
        if (leaf == null) {
            throw new IllegalArgumentException("collider is not indexed");
        }
        if (leaf.bounds.contains(bounds)) {
            return;
        }
        detach(leaf);
        leaf.bounds = bounds.expanded(FAT_MARGIN);
        insert(leaf);
    }

    List<Collider> query(Aabb bounds) {
        List<Collider> candidates = new ArrayList<>();
        visit(bounds, candidates);
        return candidates;
    }

    List<Collider> queryRay(Vector3fc origin, Vector3fc direction, float maximumDistance) {
        List<Collider> candidates = new ArrayList<>();
        Node currentRoot = root;
        if (currentRoot == null) {
            return candidates;
        }
        Deque<Node> pending = new ArrayDeque<>();
        pending.push(currentRoot);
        while (!pending.isEmpty()) {
            Node node = pending.pop();
            if (!node.bounds.intersectsRay(origin, direction, maximumDistance)) {
                continue;
            }
            addLeafOrChildren(node, candidates, pending);
        }
        return candidates;
    }

    void clear() {
        root = null;
        leaves.clear();
    }

    private void visit(Aabb bounds, List<Collider> candidates) {
        Node currentRoot = root;
        if (currentRoot == null) {
            return;
        }
        Deque<Node> pending = new ArrayDeque<>();
        pending.push(currentRoot);
        while (!pending.isEmpty()) {
            Node node = pending.pop();
            if (!node.bounds.overlaps(bounds)) {
                continue;
            }
            addLeafOrChildren(node, candidates, pending);
        }
    }

    private static void addLeafOrChildren(Node node, List<Collider> candidates, Deque<Node> pending) {
        if (node.collider != null) {
            candidates.add(node.collider);
            return;
        }
        pending.push(node.requireLeft());
        pending.push(node.requireRight());
    }

    private void insert(Node leaf) {
        Node currentRoot = root;
        if (currentRoot == null) {
            root = leaf;
            leaf.parent = null;
            return;
        }

        Node sibling = chooseSibling(currentRoot, leaf.bounds);
        Node oldParent = sibling.parent;
        Node newParent = Node.branch(sibling, leaf);
        newParent.parent = oldParent;
        if (oldParent == null) {
            root = newParent;
        } else {
            oldParent.replaceChild(sibling, newParent);
        }
        refit(newParent);
    }

    private static Node chooseSibling(Node start, Aabb leafBounds) {
        Node candidate = start;
        while (!candidate.isLeaf()) {
            Node left = candidate.requireLeft();
            Node right = candidate.requireRight();
            float leftCost = insertionCost(left, leafBounds);
            float rightCost = insertionCost(right, leafBounds);
            candidate = leftCost <= rightCost ? left : right;
        }
        return candidate;
    }

    private static float insertionCost(Node node, Aabb leafBounds) {
        return Aabb.combine(node.bounds, leafBounds).surfaceArea() - node.bounds.surfaceArea();
    }

    private void detach(Node leaf) {
        if (leaf == root) {
            root = null;
            return;
        }
        Node parent = leaf.requireParent();
        Node sibling = parent.otherChild(leaf);
        Node grandparent = parent.parent;
        if (grandparent == null) {
            root = sibling;
            sibling.parent = null;
        } else {
            grandparent.replaceChild(parent, sibling);
            refit(grandparent);
        }
        leaf.parent = null;
    }

    private static void refit(Node start) {
        @Nullable Node node = start;
        while (node != null) {
            node.refit();
            node = node.parent;
        }
    }

    private static final class Node {
        private Aabb bounds;
        private final @Nullable Collider collider;
        private @Nullable Node parent;
        private @Nullable Node left;
        private @Nullable Node right;

        private Node(Aabb bounds, @Nullable Collider collider) {
            this.bounds = bounds;
            this.collider = collider;
        }

        static Node leaf(Collider collider, Aabb bounds) {
            return new Node(bounds, collider);
        }

        static Node branch(Node left, Node right) {
            Node branch = new Node(Aabb.combine(left.bounds, right.bounds), null);
            branch.left = left;
            branch.right = right;
            left.parent = branch;
            right.parent = branch;
            return branch;
        }

        boolean isLeaf() {
            return collider != null;
        }

        Node requireParent() {
            if (parent == null) {
                throw new IllegalStateException("node has no parent");
            }
            return parent;
        }

        Node requireLeft() {
            if (left == null) {
                throw new IllegalStateException("branch has no left child");
            }
            return left;
        }

        Node requireRight() {
            if (right == null) {
                throw new IllegalStateException("branch has no right child");
            }
            return right;
        }

        Node otherChild(Node child) {
            Node leftChild = requireLeft();
            return leftChild == child ? requireRight() : leftChild;
        }

        void replaceChild(Node previous, Node replacement) {
            if (left == previous) {
                left = replacement;
            } else if (right == previous) {
                right = replacement;
            } else {
                throw new IllegalArgumentException("node is not a child of this branch");
            }
            replacement.parent = this;
        }

        void refit() {
            if (!isLeaf()) {
                bounds = Aabb.combine(requireLeft().bounds, requireRight().bounds);
            }
        }
    }
}
