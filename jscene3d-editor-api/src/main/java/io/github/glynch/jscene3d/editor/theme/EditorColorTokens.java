/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import java.util.Set;

/** Standard semantic workbench colors understood by the built-in toolkit adapters. */
public final class EditorColorTokens {
    private static final String PREFIX = "io.github.glynch.jscene3d.editor.color.";

    /** Workbench canvas background. */
    public static final EditorColorTokenId CANVAS = token("canvas");

    /** Top-level workbench chrome. */
    public static final EditorColorTokenId CHROME = token("chrome");

    /** Standard panel background. */
    public static final EditorColorTokenId PANEL = token("panel");

    /** Raised panel background. */
    public static final EditorColorTokenId PANEL_RAISED = token("panel-raised");

    /** Hovered item background. */
    public static final EditorColorTokenId HOVER = token("hover");

    /** Selected item background. */
    public static final EditorColorTokenId SELECTED = token("selected");

    /** Primary accent color. */
    public static final EditorColorTokenId ACCENT = token("accent");

    /** Hovered primary accent color. */
    public static final EditorColorTokenId ACCENT_HOVER = token("accent-hover");

    /** Subtle accent background. */
    public static final EditorColorTokenId ACCENT_SUBTLE = token("accent-subtle");

    /** Muted accent color. */
    public static final EditorColorTokenId ACCENT_MUTED = token("accent-muted");

    /** Keyboard-focus indicator. */
    public static final EditorColorTokenId FOCUS = token("focus");

    /** Faint keyboard-focus indicator. */
    public static final EditorColorTokenId FOCUS_FAINT = token("focus-faint");

    /** Keyboard-focus shadow. */
    public static final EditorColorTokenId FOCUS_SHADOW = token("focus-shadow");

    /** Strong keyboard-focus shadow. */
    public static final EditorColorTokenId FOCUS_SHADOW_STRONG = token("focus-shadow-strong");

    /** Structural divider. */
    public static final EditorColorTokenId DIVIDER = token("divider");

    /** Editable-field background. */
    public static final EditorColorTokenId FIELD = token("field");

    /** Strong-emphasis text. */
    public static final EditorColorTokenId TEXT_STRONG = token("text-strong");

    /** Standard text. */
    public static final EditorColorTokenId TEXT = token("text");

    /** Muted text. */
    public static final EditorColorTokenId TEXT_MUTED = token("text-muted");

    /** Faint muted text. */
    public static final EditorColorTokenId TEXT_MUTED_FAINT = token("text-muted-faint");

    /** Border derived from the muted text color. */
    public static final EditorColorTokenId TEXT_MUTED_BORDER = token("text-muted-border");

    /** Track derived from the muted text color. */
    public static final EditorColorTokenId TEXT_MUTED_TRACK = token("text-muted-track");

    /** Overlay derived from the muted text color. */
    public static final EditorColorTokenId TEXT_MUTED_OVERLAY = token("text-muted-overlay");

    /** Successful-state indicator. */
    public static final EditorColorTokenId SUCCESS = token("success");

    /** Informational-state indicator. */
    public static final EditorColorTokenId INFORMATION = token("information");

    /** Warning-state indicator. */
    public static final EditorColorTokenId WARNING = token("warning");

    /** Error-state indicator. */
    public static final EditorColorTokenId ERROR = token("error");

    /** Modal-dialog shadow. */
    public static final EditorColorTokenId MODAL_SHADOW = token("modal-shadow");

    /** Standard viewport shade. */
    public static final EditorColorTokenId VIEWPORT_SHADE = token("viewport-shade");

    /** Strong viewport shade. */
    public static final EditorColorTokenId VIEWPORT_SHADE_STRONG = token("viewport-shade-strong");

    /** Soft viewport shade. */
    public static final EditorColorTokenId VIEWPORT_SHADE_SOFT = token("viewport-shade-soft");

    /** Viewport overlay. */
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

    /**
     * Returns every standard token required by a complete resolved theme.
     *
     * @return immutable required-token set
     */
    public static Set<EditorColorTokenId> required() {
        return REQUIRED;
    }

    private static EditorColorTokenId token(String suffix) {
        return new EditorColorTokenId(PREFIX + suffix);
    }

    private EditorColorTokens() {}
}
