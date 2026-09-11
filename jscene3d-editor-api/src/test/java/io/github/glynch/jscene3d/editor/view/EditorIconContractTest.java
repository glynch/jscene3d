/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class EditorIconContractTest {
    @Test
    void suppliesStableBuiltInSemanticIdentitiesWithAccessibleText() {
        EditorIcon icon = new EditorIcon(EditorIcons.READ_ONLY, "Read-only");

        assertThat(icon.id()).isEqualTo(new EditorIconId("io.github.glynch.jscene3d.editor.icon.read-only"));
        assertThat(icon.tooltip()).isEqualTo("Read-only");
        assertThat(EditorIcons.WORLD).isNotEqualTo(EditorIcons.ENTITY);
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void validatesIconIdentityAndTooltip() {
        assertThatThrownBy(() -> new EditorIconId("world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be a lowercase dotted namespaced identity");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorIcon(null, "World"))
                .withMessage("id");
        assertThatThrownBy(() -> new EditorIcon(EditorIcons.WORLD, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tooltip must not be blank");
    }
}
