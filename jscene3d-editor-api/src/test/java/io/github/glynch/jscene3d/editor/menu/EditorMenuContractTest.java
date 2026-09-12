/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.command.EditorCommandLocations;
import org.junit.jupiter.api.Test;

final class EditorMenuContractTest {
    @Test
    void identifiesMenuByItsCommandLocation() {
        EditorMenuContribution menu = new EditorMenuContribution(EditorCommandLocations.FILE_MENU, "File", 20);

        assertThat(menu.location()).isEqualTo(EditorCommandLocations.FILE_MENU);
        assertThat(menu.title()).isEqualTo("File");
        assertThat(menu.order()).isEqualTo(20);
    }

    @Test
    @SuppressWarnings("NullAway")
    void validatesMenuMetadata() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorMenuContribution(null, "File", 20))
                .withMessage("location");
        assertThatThrownBy(() -> new EditorMenuContribution(EditorCommandLocations.FILE_MENU, " ", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
    }
}
