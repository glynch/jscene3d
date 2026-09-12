/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.stage.StageStyle;
import org.junit.jupiter.api.Test;

/** Protects the compact application-owned modal presentation. */
final class JavaFxModalDialogsTest {
    @Test
    void usesCompactChromeFreePresentation() {
        assertThat(JavaFxModalDialogs.CARD_WIDTH).isBetween(300.0, 380.0);
        assertThat(JavaFxModalDialogs.WINDOW_STYLE).isEqualTo(StageStyle.TRANSPARENT);
    }
}
