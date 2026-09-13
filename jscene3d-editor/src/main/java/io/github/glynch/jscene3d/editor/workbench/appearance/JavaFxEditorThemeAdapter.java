/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.ACCENT;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.ACCENT_HOVER;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.ACCENT_MUTED;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.ACCENT_SUBTLE;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.CANVAS;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.CHROME;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.DIVIDER;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.ERROR;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.FIELD;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.FOCUS;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.FOCUS_FAINT;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.FOCUS_SHADOW;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.FOCUS_SHADOW_STRONG;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.HOVER;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.INFORMATION;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.MODAL_SHADOW;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.PANEL;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.PANEL_RAISED;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.SELECTED;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.SUCCESS;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.TEXT;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.TEXT_MUTED;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.TEXT_MUTED_BORDER;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.TEXT_MUTED_FAINT;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.TEXT_MUTED_OVERLAY;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.TEXT_MUTED_TRACK;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.TEXT_STRONG;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.VIEWPORT_OVERLAY;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.VIEWPORT_SHADE;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.VIEWPORT_SHADE_SOFT;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.VIEWPORT_SHADE_STRONG;
import static io.github.glynch.jscene3d.editor.theme.EditorColorTokens.WARNING;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.theme.EditorColor;
import io.github.glynch.jscene3d.editor.theme.EditorColorTokenId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javafx.scene.Parent;

/** Applies toolkit-independent semantic colors to JavaFX looked-up CSS properties. */
public final class JavaFxEditorThemeAdapter implements AutoCloseable {
    private static final Map<String, EditorColorTokenId> CSS_TOKENS = cssTokens();
    private final Parent root;
    private final EditorRegistration registration;

    /** Observes and applies the active appearance to one JavaFX scene root. */
    public JavaFxEditorThemeAdapter(Parent root, EditorColorThemeRegistry themes) {
        this.root = Objects.requireNonNull(root, "root");
        registration = Objects.requireNonNull(themes, "themes").observeAppearance(this::apply);
    }

    @Override
    public void close() {
        registration.close();
    }

    private void apply(EditorAppearanceSnapshot appearance) {
        StringBuilder style = new StringBuilder();
        CSS_TOKENS.forEach((property, token) -> style.append(property)
                .append(": ")
                .append(css(appearance.colorTheme().color(token)))
                .append("; "));
        root.setStyle(style.toString());
    }

    private static String css(EditorColor color) {
        if (color.alpha() == 255) {
            return color.toHex();
        }
        return String.format(
                Locale.ROOT, "rgba(%d, %d, %d, %.4f)", color.red(), color.green(), color.blue(), color.alpha() / 255.0);
    }

    private static Map<String, EditorColorTokenId> cssTokens() {
        Map<String, EditorColorTokenId> result = new LinkedHashMap<>();
        result.put("-jscene-canvas", CANVAS);
        result.put("-jscene-chrome", CHROME);
        result.put("-jscene-panel", PANEL);
        result.put("-jscene-panel-raised", PANEL_RAISED);
        result.put("-jscene-hover", HOVER);
        result.put("-jscene-selected", SELECTED);
        result.put("-jscene-accent", ACCENT);
        result.put("-jscene-accent-hover", ACCENT_HOVER);
        result.put("-jscene-accent-subtle", ACCENT_SUBTLE);
        result.put("-jscene-accent-muted", ACCENT_MUTED);
        result.put("-jscene-focus", FOCUS);
        result.put("-jscene-focus-faint", FOCUS_FAINT);
        result.put("-jscene-focus-shadow", FOCUS_SHADOW);
        result.put("-jscene-focus-shadow-strong", FOCUS_SHADOW_STRONG);
        result.put("-jscene-divider", DIVIDER);
        result.put("-jscene-field", FIELD);
        result.put("-jscene-text-strong", TEXT_STRONG);
        result.put("-jscene-text", TEXT);
        result.put("-jscene-text-muted", TEXT_MUTED);
        result.put("-jscene-text-muted-faint", TEXT_MUTED_FAINT);
        result.put("-jscene-text-muted-border", TEXT_MUTED_BORDER);
        result.put("-jscene-text-muted-track", TEXT_MUTED_TRACK);
        result.put("-jscene-text-muted-overlay", TEXT_MUTED_OVERLAY);
        result.put("-jscene-success", SUCCESS);
        result.put("-jscene-information", INFORMATION);
        result.put("-jscene-warning", WARNING);
        result.put("-jscene-error", ERROR);
        result.put("-jscene-modal-shadow", MODAL_SHADOW);
        result.put("-jscene-viewport-shade", VIEWPORT_SHADE);
        result.put("-jscene-viewport-shade-strong", VIEWPORT_SHADE_STRONG);
        result.put("-jscene-viewport-shade-soft", VIEWPORT_SHADE_SOFT);
        result.put("-jscene-viewport-overlay", VIEWPORT_OVERLAY);
        return Map.copyOf(result);
    }
}
