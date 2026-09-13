/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeKind;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Top-chrome action that switches between the preferred light and dark themes. */
public final class JavaFxColorSchemeToggle implements AutoCloseable {
    private final Button button = new Button();
    private final JavaFxIconRenderer icons;
    private final EditorRegistration appearanceRegistration;

    /** Creates a live theme-family toggle backed by the editor command model. */
    public JavaFxColorSchemeToggle(
            EditorColorThemeRegistry themes, JavaFxIconRenderer icons, Runnable toggleColorScheme) {
        EditorColorThemeRegistry registry = Objects.requireNonNull(themes, "themes");
        this.icons = Objects.requireNonNull(icons, "icons");
        Runnable toggle = Objects.requireNonNull(toggleColorScheme, "toggleColorScheme");
        button.getStyleClass().add(EditorStyleClasses.EDITOR_THEME_TOGGLE);
        button.setOnAction(ignored -> toggle.run());
        appearanceRegistration = registry.observeAppearance(
                appearance -> update(appearance.colorTheme().kind()));
    }

    /** Returns the workbench-owned toolbar button. */
    public Button button() {
        return button;
    }

    @Override
    public void close() {
        appearanceRegistration.close();
    }

    private void update(EditorColorThemeKind kind) {
        boolean dark = kind == EditorColorThemeKind.DARK || kind == EditorColorThemeKind.HIGH_CONTRAST_DARK;
        String label = dark ? "Switch to Light Theme" : "Switch to Dark Theme";
        button.setGraphic(icons.create(new EditorIcon(dark ? EditorIcons.SUN : EditorIcons.MOON, label)));
        button.setAccessibleText(label);
        button.setTooltip(new Tooltip(label));
    }
}
