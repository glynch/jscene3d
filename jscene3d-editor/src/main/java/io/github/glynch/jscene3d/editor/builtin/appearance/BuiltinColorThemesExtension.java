/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.appearance;

import static io.github.glynch.jscene3d.editor.theme.EditorColor.parseHex;
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
import static io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokens.ANNOTATION;
import static io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokens.COMMENT;
import static io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokens.KEYWORD;
import static io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokens.NUMBER;
import static io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokens.STRING;
import static io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokens.TYPE;

import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.theme.EditorColorTheme;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeKind;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemes;
import io.github.glynch.jscene3d.editor.theme.EditorFontStyle;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxStyle;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Contributes the built-in dark and light palettes through the public theme contract. */
public final class BuiltinColorThemesExtension implements EditorExtension {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.editor.builtin.color-themes";
    public static final EditorColorThemeId DARK = new EditorColorThemeId("io.github.glynch.jscene3d.editor.theme.dark");
    public static final EditorColorThemeId LIGHT =
            new EditorColorThemeId("io.github.glynch.jscene3d.editor.theme.light");

    @Override
    public String id() {
        return EXTENSION_ID;
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                EXTENSION_ID,
                "Color Themes",
                "Supplies the built-in dark and light editor color themes.",
                "JScene3D",
                Optional.empty(),
                true);
    }

    @Override
    public void activate(EditorExtensionContext editor) {
        registerThemes(editor.colorThemes(), editor.subscriptions()::add);
    }

    /** Registers both built-in themes through the same boundary used by external extensions. */
    public static void registerThemes(EditorColorThemes themes, Consumer<EditorRegistration> registrations) {
        EditorColorThemes registry = Objects.requireNonNull(themes, "themes");
        Consumer<EditorRegistration> owner = Objects.requireNonNull(registrations, "registrations");
        owner.accept(registry.register(dark()));
        owner.accept(registry.register(light()));
    }

    private static EditorColorTheme dark() {
        return new EditorColorTheme(
                DARK,
                "JScene3D Dark",
                EditorColorThemeKind.DARK,
                Optional.empty(),
                Map.ofEntries(
                        Map.entry(CANVAS, parseHex("#0d1118")),
                        Map.entry(CHROME, parseHex("#121722")),
                        Map.entry(PANEL, parseHex("#171d28")),
                        Map.entry(PANEL_RAISED, parseHex("#1d2431")),
                        Map.entry(HOVER, parseHex("#252d3b")),
                        Map.entry(SELECTED, parseHex("#45409a")),
                        Map.entry(ACCENT, parseHex("#7562f3")),
                        Map.entry(ACCENT_HOVER, parseHex("#8876ff")),
                        Map.entry(ACCENT_SUBTLE, parseHex("#7562f329")),
                        Map.entry(ACCENT_MUTED, parseHex("#7562f338")),
                        Map.entry(FOCUS, parseHex("#9a8bff")),
                        Map.entry(FOCUS_FAINT, parseHex("#9a8bff38")),
                        Map.entry(FOCUS_SHADOW, parseHex("#9a8bff47")),
                        Map.entry(FOCUS_SHADOW_STRONG, parseHex("#9a8bff57")),
                        Map.entry(DIVIDER, parseHex("#303746")),
                        Map.entry(FIELD, parseHex("#121821")),
                        Map.entry(TEXT_STRONG, parseHex("#edf0f6")),
                        Map.entry(TEXT, parseHex("#c9ced8")),
                        Map.entry(TEXT_MUTED, parseHex("#8e97a8")),
                        Map.entry(TEXT_MUTED_FAINT, parseHex("#8e97a81f")),
                        Map.entry(TEXT_MUTED_BORDER, parseHex("#8e97a85c")),
                        Map.entry(TEXT_MUTED_TRACK, parseHex("#8e97a847")),
                        Map.entry(TEXT_MUTED_OVERLAY, parseHex("#8e97a866")),
                        Map.entry(SUCCESS, parseHex("#62c9a5")),
                        Map.entry(INFORMATION, parseHex("#69aef8")),
                        Map.entry(WARNING, parseHex("#e2b45e")),
                        Map.entry(ERROR, parseHex("#ff7182")),
                        Map.entry(MODAL_SHADOW, parseHex("#00000094")),
                        Map.entry(VIEWPORT_SHADE, parseHex("#06090f5c")),
                        Map.entry(VIEWPORT_SHADE_STRONG, parseHex("#070a118c")),
                        Map.entry(VIEWPORT_SHADE_SOFT, parseHex("#070a114d")),
                        Map.entry(VIEWPORT_OVERLAY, parseHex("#080c14e6"))),
                Map.of(
                        COMMENT, new EditorSyntaxStyle(parseHex("#8e97a8"), Set.of(EditorFontStyle.ITALIC)),
                        KEYWORD, EditorSyntaxStyle.plain(parseHex("#c792ea")),
                        TYPE, EditorSyntaxStyle.plain(parseHex("#82aaff")),
                        STRING, EditorSyntaxStyle.plain(parseHex("#c3e88d")),
                        NUMBER, EditorSyntaxStyle.plain(parseHex("#f78c6c")),
                        ANNOTATION, EditorSyntaxStyle.plain(parseHex("#ffcb6b"))));
    }

    private static EditorColorTheme light() {
        return new EditorColorTheme(
                LIGHT,
                "JScene3D Light",
                EditorColorThemeKind.LIGHT,
                Optional.empty(),
                Map.ofEntries(
                        Map.entry(CANVAS, parseHex("#d8dde6")),
                        Map.entry(CHROME, parseHex("#e7eaf0")),
                        Map.entry(PANEL, parseHex("#d2d8e2")),
                        Map.entry(PANEL_RAISED, parseHex("#eceff3")),
                        Map.entry(HOVER, parseHex("#c7ced9")),
                        Map.entry(SELECTED, parseHex("#c5baf0")),
                        Map.entry(ACCENT, parseHex("#6047d9")),
                        Map.entry(ACCENT_HOVER, parseHex("#5138c7")),
                        Map.entry(ACCENT_SUBTLE, parseHex("#6047d929")),
                        Map.entry(ACCENT_MUTED, parseHex("#6047d938")),
                        Map.entry(FOCUS, parseHex("#573bd7")),
                        Map.entry(FOCUS_FAINT, parseHex("#573bd738")),
                        Map.entry(FOCUS_SHADOW, parseHex("#573bd747")),
                        Map.entry(FOCUS_SHADOW_STRONG, parseHex("#573bd757")),
                        Map.entry(DIVIDER, parseHex("#a8b2c0")),
                        Map.entry(FIELD, parseHex("#f1f3f6")),
                        Map.entry(TEXT_STRONG, parseHex("#1d2430")),
                        Map.entry(TEXT, parseHex("#303846")),
                        Map.entry(TEXT_MUTED, parseHex("#606c7e")),
                        Map.entry(TEXT_MUTED_FAINT, parseHex("#606c7e1f")),
                        Map.entry(TEXT_MUTED_BORDER, parseHex("#606c7e5c")),
                        Map.entry(TEXT_MUTED_TRACK, parseHex("#606c7e47")),
                        Map.entry(TEXT_MUTED_OVERLAY, parseHex("#606c7e66")),
                        Map.entry(SUCCESS, parseHex("#16785d")),
                        Map.entry(INFORMATION, parseHex("#1667b8")),
                        Map.entry(WARNING, parseHex("#96630a")),
                        Map.entry(ERROR, parseHex("#c7354b")),
                        Map.entry(MODAL_SHADOW, parseHex("#1c253342")),
                        Map.entry(VIEWPORT_SHADE, parseHex("#e7eaf038")),
                        Map.entry(VIEWPORT_SHADE_STRONG, parseHex("#e7eaf066")),
                        Map.entry(VIEWPORT_SHADE_SOFT, parseHex("#e7eaf02e")),
                        Map.entry(VIEWPORT_OVERLAY, parseHex("#e7eaf0eb"))),
                Map.of(
                        COMMENT, new EditorSyntaxStyle(parseHex("#5e6d7a"), Set.of(EditorFontStyle.ITALIC)),
                        KEYWORD, EditorSyntaxStyle.plain(parseHex("#7c3aed")),
                        TYPE, EditorSyntaxStyle.plain(parseHex("#1d4ed8")),
                        STRING, EditorSyntaxStyle.plain(parseHex("#147d3f")),
                        NUMBER, EditorSyntaxStyle.plain(parseHex("#b45309")),
                        ANNOTATION, EditorSyntaxStyle.plain(parseHex("#9a3412"))));
    }
}
