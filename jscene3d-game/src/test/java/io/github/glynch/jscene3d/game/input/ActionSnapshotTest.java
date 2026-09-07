/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class ActionSnapshotTest {
    private static final InputAction LEFT = new InputAction("left");
    private static final InputAction RIGHT = new InputAction("right");

    @Test
    void buildsAndQueriesSemanticState() {
        ActionSnapshot snapshot = ActionSnapshot.builder()
                .pressed(RIGHT)
                .released(LEFT)
                .pointerDelta(2.5, -1.5)
                .build();

        assertThat(snapshot.isDown(RIGHT)).isTrue();
        assertThat(snapshot.wasPressed(RIGHT)).isTrue();
        assertThat(snapshot.wasReleased(LEFT)).isTrue();
        assertThat(snapshot.axis(LEFT, RIGHT)).isEqualTo(1.0F);
        assertThat(snapshot.pointerDeltaX()).isEqualTo(2.5);
        assertThat(snapshot.pointerDeltaY()).isEqualTo(-1.5);
    }

    @Test
    void mergesBufferedTransitionsAndConsumesThem() {
        ActionSnapshot older =
                ActionSnapshot.builder().pressed(LEFT).pointerDelta(1.0, 2.0).build();
        ActionSnapshot newer = ActionSnapshot.builder()
                .released(LEFT)
                .down(RIGHT)
                .pointerDelta(3.0, 4.0)
                .build();

        ActionSnapshot merged = older.merge(newer);
        ActionSnapshot held = merged.heldOnly();

        assertThat(merged.wasPressed(LEFT)).isTrue();
        assertThat(merged.wasReleased(LEFT)).isTrue();
        assertThat(merged.isDown(LEFT)).isFalse();
        assertThat(merged.isDown(RIGHT)).isTrue();
        assertThat(merged.pointerDeltaX()).isEqualTo(4.0);
        assertThat(merged.pointerDeltaY()).isEqualTo(6.0);
        assertThat(held.isDown(RIGHT)).isTrue();
        assertThat(held.wasPressed(LEFT)).isFalse();
        assertThat(held.wasReleased(LEFT)).isFalse();
        assertThat(held.pointerDeltaX()).isZero();
    }

    @Test
    void suppliesValueSemanticsAndValidatesPointerMovement() {
        ActionSnapshot first =
                ActionSnapshot.builder().down(LEFT).pointerDelta(1.0, 2.0).build();
        ActionSnapshot second =
                ActionSnapshot.builder().down(LEFT).pointerDelta(1.0, 2.0).build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("left", "pointerDeltaX=1.0");
        assertThat(ActionSnapshot.empty().heldOnly()).isSameAs(ActionSnapshot.empty());
        ActionSnapshot.Builder builder = ActionSnapshot.builder();
        assertThatThrownBy(() -> builder.pointerDelta(Double.NaN, 0.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retainsTypedAxesWhileConsumingTransitions() {
        ActionSnapshot older = ActionSnapshot.builder()
                .axis1d(LEFT, -0.5F)
                .axis2d(RIGHT, 0.25F, -0.75F)
                .build();
        ActionSnapshot newer = ActionSnapshot.builder()
                .axis1d(LEFT, 2.0F)
                .axis2d(RIGHT, -0.5F, 0.5F)
                .build();

        ActionSnapshot merged = older.merge(newer);
        ActionSnapshot held = merged.heldOnly();

        assertThat(merged.axis1d(LEFT)).isEqualTo(1.0F);
        assertThat(merged.axis2d(RIGHT)).isEqualTo(new InputVector2(-0.5F, 0.5F));
        assertThat(held.axis1d(LEFT)).isEqualTo(1.0F);
        assertThat(held.axis2d(RIGHT)).isEqualTo(new InputVector2(-0.5F, 0.5F));
        assertThat(ActionSnapshot.builder()
                        .axis1d(LEFT, 0.0F)
                        .axis2d(RIGHT, 0.0F, 0.0F)
                        .build())
                .isEqualTo(ActionSnapshot.empty());
    }

    @Test
    void rejectsNonFiniteOrOutOfRangeAxisValues() {
        ActionSnapshot.Builder builder = ActionSnapshot.builder();

        assertThatThrownBy(() -> builder.axis1d(LEFT, Float.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InputVector2(Float.POSITIVE_INFINITY, 0.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InputVector2(0.0F, -2.0F)).isInstanceOf(IllegalArgumentException.class);
    }
}
