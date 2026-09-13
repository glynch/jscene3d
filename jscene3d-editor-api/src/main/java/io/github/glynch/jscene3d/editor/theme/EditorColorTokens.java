/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import java.util.Set;

/** Standard semantic workbench colors understood by the built-in toolkit adapters. */
public final class EditorColorTokens {
    private static final String PREFIX = "io.github.glynch.jscene3d.editor.color.";

    public static final EditorColorTokenId CANVAS = token("canvas");
    public static final EditorColorTokenId CHROME = token("chrome");
    public static final EditorColorTokenId PANEL = token("panel");
    public static final EditorColorTokenId PANEL_RAISED = token("panel-raised");
    public static final EditorColorTokenId HOVER = token("hover");
    public static final EditorColorTokenId SELECTED = token("selected");
    public static final EditorColorTokenId ACCENT = token("accent");
    public static final EditorColorTokenId ACCENT_HOVER = token("accent-hover");
    public static final EditorColorTokenId ACCENT_SUBTLE = token("accent-subtle");
    public static final EditorColorTokenId ACCENT_MUTED = token("accent-muted");
    public static final EditorColorTokenId FOCUS = token("focus");
    public static final EditorColorTokenId FOCUS_FAINT = token("focus-faint");
    public static final EditorColorTokenId FOCUS_SHADOW = token("focus-shadow");
    public static final EditorColorTokenId FOCUS_SHADOW_STRONG = token("focus-shadow-strong");
    public static final EditorColorTokenId DIVIDER = token("divider");
    public static final EditorColorTokenId FIELD = token("field");
    public static final EditorColorTokenId TEXT_STRONG = token("text-strong");
    public static final EditorColorTokenId TEXT = token("text");
    public static final EditorColorTokenId TEXT_MUTED = token("text-muted");
    public static final EditorColorTokenId TEXT_MUTED_FAINT = token("text-muted-faint");
    public static final EditorColorTokenId TEXT_MUTED_BORDER = token("text-muted-border");
    public static final EditorColorTokenId TEXT_MUTED_TRACK = token("text-muted-track");
    public static final EditorColorTokenId TEXT_MUTED_OVERLAY = token("text-muted-overlay");
    public static final EditorColorTokenId SUCCESS = token("success");
    public static final EditorColorTokenId INFORMATION = token("information");
    public static final EditorColorTokenId WARNING = token("warning");
    public static final EditorColorTokenId ERROR = token("error");
    public static final EditorColorTokenId MODAL_SHADOW = token("modal-shadow");
    public static final EditorColorTokenId VIEWPORT_SHADE = token("viewport-shade");
    public static final EditorColorTokenId VIEWPORT_SHADE_STRONG = token("viewport-shade-strong");
    public static final EditorColorTokenId VIEWPORT_SHADE_SOFT = token("viewport-shade-soft");
    public static final EditorColorTokenId VIEWPORT_OVERLAY = token("viewport-overlay");

    private static final Set<EditorColorTokenId> REQUIRED = Set.of(
            CANVAS,
            CHROME,
            PANEL,
            PANEL_RAISED,
            HOVER,
            SELECTED,
            ACCENT,
            ACCENT_HOVER,
            ACCENT_SUBTLE,
            ACCENT_MUTED,
            FOCUS,
            FOCUS_FAINT,
            FOCUS_SHADOW,
            FOCUS_SHADOW_STRONG,
            DIVIDER,
            FIELD,
            TEXT_STRONG,
            TEXT,
            TEXT_MUTED,
            TEXT_MUTED_FAINT,
            TEXT_MUTED_BORDER,
            TEXT_MUTED_TRACK,
            TEXT_MUTED_OVERLAY,
            SUCCESS,
            INFORMATION,
            WARNING,
            ERROR,
            MODAL_SHADOW,
            VIEWPORT_SHADE,
            VIEWPORT_SHADE_STRONG,
            VIEWPORT_SHADE_SOFT,
            VIEWPORT_OVERLAY);

    /** Returns every standard token required by a complete resolved theme. */
    public static Set<EditorColorTokenId> required() {
        return REQUIRED;
    }

    private static EditorColorTokenId token(String suffix) {
        return new EditorColorTokenId(PREFIX + suffix);
    }

    private EditorColorTokens() {}
}
