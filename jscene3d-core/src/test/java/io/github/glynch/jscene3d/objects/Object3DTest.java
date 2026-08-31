/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

final class Object3DTest {
    @Test
    void startsAsAVisibleUnparentedLeafWithAStableReadOnlyChildrenView() {
        Object3D object = new Object3D();
        List<Object3D> children = object.children();

        assertThat(object.parent()).isNull();
        assertThat(object.isVisible()).isTrue();
        assertThat(object.isFrustumCullingEnabled()).isTrue();
        assertThat(object.renderOrder()).isZero();
        assertThat(children).isSameAs(object.children()).isEmpty();
        Object3D rejectedChild = new Object3D();
        assertThatThrownBy(() -> children.add(rejectedChild)).isInstanceOf(UnsupportedOperationException.class);

        Object3D child = new Object3D();
        object.add(child);
        assertThat(children).containsExactly(child);
    }

    @Test
    void controlsWhetherTheObjectMayBeFrustumCulled() {
        Object3D object = new Object3D();

        object.setFrustumCullingEnabled(false);

        assertThat(object.isFrustumCullingEnabled()).isFalse();
    }

    @Test
    void controlsExplicitRenderOrder() {
        Object3D object = new Object3D();

        object.setRenderOrder(-3);

        assertThat(object.renderOrder()).isEqualTo(-3);
    }

    @Test
    void addsChildrenInOrderWithoutDuplicatingOrReorderingThem() {
        Object3D parent = new Object3D();
        Object3D first = new Object3D();
        Object3D second = new Object3D();

        assertThat(parent.add(first)).isTrue();
        assertThat(parent.add(second)).isTrue();
        assertThat(parent.add(first)).isFalse();

        assertThat(parent.children()).containsExactly(first, second);
        assertThat(first.parent()).isSameAs(parent);
        assertThat(second.parent()).isSameAs(parent);
    }

    @Test
    void reparentsOnlyAfterValidatingTheNewRelationship() {
        Object3D oldParent = new Object3D();
        Object3D newParent = new Object3D();
        Object3D child = new Object3D();
        oldParent.add(child);

        assertThat(newParent.add(child)).isTrue();

        assertThat(oldParent.children()).isEmpty();
        assertThat(newParent.children()).containsExactly(child);
        assertThat(child.parent()).isSameAs(newParent);
    }

    @Test
    void rejectsSelfParentingAndCyclesWithoutChangingTheHierarchy() {
        Object3D root = new Object3D();
        Object3D child = new Object3D();
        Object3D grandchild = new Object3D();
        root.add(child);
        child.add(grandchild);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> root.add(root))
                .withMessage("An Object3D cannot be its own child");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> grandchild.add(root))
                .withMessage("Adding the child would create a scene-graph cycle");

        assertThat(root.parent()).isNull();
        assertThat(root.children()).containsExactly(child);
        assertThat(child.children()).containsExactly(grandchild);
        assertThat(grandchild.children()).isEmpty();
    }

    @Test
    void removesDetachesAndClearsBothSidesOfRelationships() {
        Object3D parent = new Object3D();
        Object3D first = new Object3D();
        Object3D second = new Object3D();
        Object3D stranger = new Object3D();
        parent.add(first);
        parent.add(second);

        assertThat(parent.remove(stranger)).isFalse();
        assertThat(first.detach()).isTrue();
        assertThat(first.detach()).isFalse();
        assertThat(first.parent()).isNull();
        assertThat(parent.children()).containsExactly(second);

        parent.clear();
        assertThat(parent.children()).isEmpty();
        assertThat(second.parent()).isNull();
    }

    @Test
    void traversesDepthFirstInStablePreorder() {
        Object3D root = new Object3D();
        Object3D first = new Object3D();
        Object3D firstChild = new Object3D();
        Object3D second = new Object3D();
        root.add(first);
        first.add(firstChild);
        root.add(second);
        List<Object3D> visited = new ArrayList<>();

        root.traverse(visited::add);

        assertThat(visited).containsExactly(root, first, firstChild, second);
    }

    @Test
    void visibleTraversalSkipsInvisibleSubtrees() {
        Object3D root = new Object3D();
        Object3D hidden = new Object3D();
        Object3D hiddenChild = new Object3D();
        Object3D visible = new Object3D();
        hidden.add(hiddenChild);
        hidden.setVisible(false);
        root.add(hidden);
        root.add(visible);
        List<Object3D> visited = new ArrayList<>();

        root.traverseVisible(visited::add);

        assertThat(visited).containsExactly(root, visible);
    }

    @Test
    void traversesVeryDeepHierarchiesWithoutUsingTheCallStack() {
        Object3D root = new Object3D();
        int expectedCount = 25_000;
        for (int index = 1; index < expectedCount; index++) {
            Object3D newRoot = new Object3D();
            newRoot.add(root);
            root = newRoot;
        }
        Object3D hierarchyRoot = root;
        int[] visitedCount = new int[1];

        hierarchyRoot.traverse(ignored -> visitedCount[0]++);

        assertThat(visitedCount[0]).isEqualTo(expectedCount);
    }

    @Test
    void rejectsStructuralMutationOfAnAffectedTreeDuringTraversal() {
        Object3D root = new Object3D();
        Object3D child = new Object3D();
        root.add(child);
        Consumer<Object3D> detachingVisitor = ignored -> child.detach();

        assertThatThrownBy(() -> root.traverse(detachingVisitor))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessage("Scene-graph structure cannot change during traversal");

        assertThat(root.children()).containsExactly(child);
        assertThat(child.parent()).isSameAs(root);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullArgumentsAtPublicBoundaries() {
        Object3D object = new Object3D();

        assertThatNullPointerException().isThrownBy(() -> object.add(null)).withMessage("child");
        assertThatNullPointerException().isThrownBy(() -> object.remove(null)).withMessage("child");
        assertThatNullPointerException().isThrownBy(() -> object.traverse(null)).withMessage("visitor");
        assertThatNullPointerException()
                .isThrownBy(() -> object.traverseVisible(null))
                .withMessage("visitor");
    }
}
