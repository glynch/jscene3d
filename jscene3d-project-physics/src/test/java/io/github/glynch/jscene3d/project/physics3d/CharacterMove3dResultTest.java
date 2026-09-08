/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Exercises the defensive public character movement value contract. */
final class CharacterMove3dResultTest {
    /** Copies every input and exposes all movement state without backend values. */
    @Test
    void preservesImmutableMovementState() {
        Vector3f requested = new Vector3f(1.0F, 2.0F, 3.0F);
        Vector3f applied = new Vector3f(4.0F, 5.0F, 6.0F);
        Vector3f velocity = new Vector3f(7.0F, 8.0F, 9.0F);
        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
        CharacterMove3dState state = new CharacterMove3dState(true, true, false);
        CharacterMove3dResult result =
                new CharacterMove3dResult(requested, applied, velocity, normal, state, List.of());

        requested.zero();
        applied.zero();
        velocity.zero();
        normal.zero();

        assertThat(result.requestedTranslation(new Vector3f())).isEqualTo(new Vector3f(1.0F, 2.0F, 3.0F));
        assertThat(result.appliedTranslation(new Vector3f())).isEqualTo(new Vector3f(4.0F, 5.0F, 6.0F));
        assertThat(result.velocity(new Vector3f())).isEqualTo(new Vector3f(7.0F, 8.0F, 9.0F));
        assertThat(result.groundNormal(new Vector3f())).isEqualTo(new Vector3f(0.0F, 1.0F, 0.0F));
        assertThat(result.isGrounded()).isTrue();
        assertThat(result.stepped()).isTrue();
        assertThat(result.jumped()).isFalse();
        assertThat(result.contacts()).isEmpty();
    }

    /** Rejects non-finite movement data and every invalid public character setting. */
    @Test
    void rejectsInvalidValues() {
        Vector3f zero = new Vector3f();
        Vector3f nonFinite = new Vector3f(Float.NaN, 0.0F, 0.0F);
        CharacterMove3dState state = new CharacterMove3dState(false, false, false);
        List<CharacterContact3d> noContacts = List.of();

        assertThatThrownBy(() -> new CharacterMove3dResult(nonFinite, zero, zero, zero, state, noContacts))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CharacterBody3dSettings(-1.0F, 1.0F, 1.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CharacterBody3dSettings(1.0F, -1.0F, 1.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CharacterBody3dSettings(1.0F, 1.0F, -1.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CharacterBody3dSettings(1.0F, 1.0F, 1.0F, Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
