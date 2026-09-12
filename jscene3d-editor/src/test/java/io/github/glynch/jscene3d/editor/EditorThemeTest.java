/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Verifies installation and semantic foundations of the packaged editor theme. */
final class EditorThemeTest {
    /** Keeps the accepted semantic colour roles in the packaged stylesheet. */
    @Test
    void declaresSemanticColourRoles() throws IOException {
        String stylesheet = stylesheet();

        assertThat(stylesheet)
                .contains(
                        "-jscene-canvas:",
                        "-jscene-chrome:",
                        "-jscene-panel:",
                        "-jscene-selected:",
                        "-jscene-accent:",
                        "-jscene-focus:",
                        "-jscene-divider:",
                        "-jscene-text-strong:",
                        "-jscene-text:",
                        "-jscene-text-muted:",
                        "-jscene-success:",
                        "-jscene-warning:",
                        "-jscene-error:");
    }

    /** Keeps the richer Project-list rows from inheriting the compact generic list height. */
    @Test
    void projectAssetListOverridesTheGenericListRowHeight() throws IOException {
        String stylesheet = stylesheet();
        int genericListRule = stylesheet.indexOf(".tree-view,\n.list-view {");
        int projectListRule = stylesheet.indexOf(".list-view.editor-asset-list {");

        assertThat(genericListRule).isGreaterThanOrEqualTo(0);
        assertThat(projectListRule).isGreaterThan(genericListRule);
        assertThat(stylesheet.substring(projectListRule)).contains("-fx-fixed-cell-size: 68px;");
    }

    /** Keeps the three-line Extensions rows from inheriting the compact generic list height. */
    @Test
    void extensionsListOverridesTheGenericListRowHeight() throws IOException {
        String stylesheet = stylesheet();
        int genericListRule = stylesheet.indexOf(".tree-view,\n.list-view {");
        int extensionsListRule = stylesheet.indexOf(".list-view.editor-extensions-list {");

        assertThat(genericListRule).isGreaterThanOrEqualTo(0);
        assertThat(extensionsListRule).isGreaterThan(genericListRule);
        assertThat(stylesheet.substring(extensionsListRule)).contains("-fx-fixed-cell-size: 78px;");
    }

    /** Keeps resource and project dirty state prominent in the accepted editor theme. */
    @Test
    void givesDirtyStateProminentResourceAndProjectMarkers() throws IOException {
        assertThat(stylesheet())
                .contains(
                        ".editor-project-context:dirty {",
                        "-fx-font-weight: 700;",
                        ".editor-editor-tab-dirty {",
                        "-fx-background-color: -jscene-warning;",
                        "-fx-min-height: 9px;",
                        "-fx-min-width: 9px;",
                        ".editor-tree-item-decoration {",
                        "-fx-opacity: 1;");
    }

    /** Distinguishes persistent tab selection from hover and transient keyboard focus. */
    @Test
    void distinguishesSelectedEditorTabsFromHoverAndKeyboardFocus() throws IOException {
        assertThat(stylesheet())
                .contains(
                        ".editor-area-tabs > .tab-header-area > .headers-region > .tab:hover {",
                        ".editor-area-tabs > .tab-header-area > .headers-region > .tab:selected {",
                        "-fx-border-color: -jscene-accent -jscene-divider -jscene-canvas -jscene-divider;",
                        ".editor-area-tabs:focused > .tab-header-area > .headers-region > .tab:selected {",
                        "-fx-border-color: -jscene-focus -jscene-divider -jscene-canvas -jscene-divider;",
                        ".editor-area-tabs:focused > .tab-header-area > .headers-region > .tab:selected .focus-indicator {",
                        "-fx-border-color: transparent;",
                        ".editor-area-tabs > .tab-header-area > .headers-region > .tab:selected .label {",
                        "-fx-text-fill: -jscene-text-strong;",
                        ".editor-area-tabs > .tab-header-area > .headers-region > .tab:selected .tab-close-button {");
    }

    /** Styles the no-project action in Welcome rather than retaining a permanent toolbar action. */
    @Test
    void stylesTheWelcomeProjectAction() throws IOException {
        assertThat(stylesheet())
                .contains(
                        ".editor-welcome {",
                        ".editor-welcome-content {",
                        ".editor-welcome-title {",
                        ".editor-welcome-subtitle {",
                        ".editor-welcome-action {",
                        ".editor-welcome-action:hover {",
                        ".editor-welcome-action:focused {")
                .doesNotContain(".editor-open-project-button {");
    }

    /** Loads the packaged editor stylesheet as UTF-8 text. */
    private static String stylesheet() throws IOException {
        try (InputStream input =
                Objects.requireNonNull(EditorTheme.class.getResourceAsStream("editor.css"), "editor.css")) {
            return new String(input.readAllBytes(), UTF_8);
        }
    }
}
