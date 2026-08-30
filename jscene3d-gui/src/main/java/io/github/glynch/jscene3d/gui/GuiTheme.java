/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.core.Color;
import java.util.Objects;

/** Immutable palette shared by JScene3D GUI overlays. */
public final class GuiTheme {
    private static final GuiTheme DARK = dark(Color.srgb(0x38bdf8));

    private final Color shadow;
    private final Color panel;
    private final Color title;
    private final Color section;
    private final Color row;
    private final Color rowHover;
    private final Color border;
    private final Color text;
    private final Color secondaryText;
    private final Color mutedText;
    private final Color control;
    private final Color accent;

    /** Creates the fixed dark palette with one validated accent color. */
    private GuiTheme(Color accent) {
        shadow = Color.BLACK;
        panel = Color.srgb(0x16191f);
        title = Color.srgb(0x111319);
        section = Color.srgb(0x22262e);
        row = Color.srgb(0x1b1f26);
        rowHover = Color.srgb(0x252b34);
        border = Color.srgb(0x3a414d);
        text = Color.srgb(0xf3f4f6);
        secondaryText = Color.srgb(0xb8c0cc);
        mutedText = Color.srgb(0x7f8998);
        control = Color.srgb(0x343a46);
        this.accent = Objects.requireNonNull(accent, "accent");
    }

    /**
     * Returns the shared dark theme with a cyan accent.
     *
     * @return immutable default theme
     */
    public static GuiTheme dark() {
        return DARK;
    }

    /**
     * Creates the dark theme with a caller-selected accent color.
     *
     * @param accent linear-sRGB accent used by active controls
     * @return immutable dark theme
     * @throws NullPointerException if {@code accent} is {@code null}
     */
    public static GuiTheme dark(Color accent) {
        return new GuiTheme(accent);
    }

    /**
     * Returns the shadow color.
     *
     * @return shadow color
     */
    public Color shadow() {
        return shadow;
    }

    /**
     * Returns the main panel color.
     *
     * @return main panel color
     */
    public Color panel() {
        return panel;
    }

    /**
     * Returns the title-bar color.
     *
     * @return title-bar color
     */
    public Color title() {
        return title;
    }

    /**
     * Returns the section-heading color.
     *
     * @return section-heading color
     */
    public Color section() {
        return section;
    }

    /**
     * Returns the ordinary row color.
     *
     * @return ordinary row color
     */
    public Color row() {
        return row;
    }

    /**
     * Returns the hovered row color.
     *
     * @return hovered row color
     */
    public Color rowHover() {
        return rowHover;
    }

    /**
     * Returns the subtle border color.
     *
     * @return subtle border color
     */
    public Color border() {
        return border;
    }

    /**
     * Returns the primary text color.
     *
     * @return primary text color
     */
    public Color text() {
        return text;
    }

    /**
     * Returns the secondary text color.
     *
     * @return secondary text color
     */
    public Color secondaryText() {
        return secondaryText;
    }

    /**
     * Returns the muted text color.
     *
     * @return muted text color
     */
    public Color mutedText() {
        return mutedText;
    }

    /**
     * Returns the inactive control color.
     *
     * @return inactive control color
     */
    public Color control() {
        return control;
    }

    /**
     * Returns the active control color.
     *
     * @return active control color
     */
    public Color accent() {
        return accent;
    }
}
