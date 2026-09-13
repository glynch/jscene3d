/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.theme.EditorColor;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeKind;
import io.github.glynch.jscene3d.editor.theme.EditorColorTokenId;
import io.github.glynch.jscene3d.editor.theme.EditorColorTokens;
import io.github.glynch.jscene3d.editor.theme.EditorFontStyle;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxStyle;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokenId;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokens;
import io.github.glynch.jscene3d.editor.workbench.appearance.EditorAppearanceSnapshot;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Serializes resolved workbench appearance into Monaco's narrow theme contract. */
final class MonacoAppearanceJson {
    private MonacoAppearanceJson() {}

    static String encode(EditorAppearanceSnapshot appearance) {
        EditorAppearanceSnapshot snapshot = Objects.requireNonNull(appearance, "appearance");
        var theme = snapshot.colorTheme();
        StringBuilder json = new StringBuilder("{");
        property(json, "id", theme.id().value()).append(',');
        property(json, "base", monacoBase(theme.kind())).append(',');
        property(json, "fontFamily", snapshot.editorFontFamily()).append(',');
        property(json, "error", theme.color(EditorColorTokens.ERROR).toHex()).append(',');
        json.append("\"fontSize\":").append(snapshot.editorFontSize()).append(',');
        appendColors(json, theme::color);
        appendSyntaxRules(json, theme.syntaxStyles());
        return json.append('}').toString();
    }

    private static void appendColors(StringBuilder json, Function<EditorColorTokenId, EditorColor> color) {
        Map<String, EditorColorTokenId> colors = new LinkedHashMap<>();
        colors.put("editor.background", EditorColorTokens.CANVAS);
        colors.put("editor.foreground", EditorColorTokens.TEXT);
        colors.put("editorLineNumber.foreground", EditorColorTokens.TEXT_MUTED);
        colors.put("editorLineNumber.activeForeground", EditorColorTokens.TEXT_STRONG);
        colors.put("editorCursor.foreground", EditorColorTokens.FOCUS);
        colors.put("editor.selectionBackground", EditorColorTokens.ACCENT_MUTED);
        colors.put("editor.inactiveSelectionBackground", EditorColorTokens.ACCENT_SUBTLE);
        colors.put("editorWidget.background", EditorColorTokens.PANEL_RAISED);
        colors.put("editorWidget.border", EditorColorTokens.DIVIDER);
        colors.put("input.background", EditorColorTokens.FIELD);
        colors.put("input.foreground", EditorColorTokens.TEXT);
        colors.put("input.border", EditorColorTokens.DIVIDER);
        colors.put("focusBorder", EditorColorTokens.FOCUS);
        json.append("\"colors\":{");
        boolean first = true;
        for (Map.Entry<String, EditorColorTokenId> entry : colors.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            property(json, entry.getKey(), color.apply(entry.getValue()).toHex());
        }
        json.append("},");
    }

    private static void appendSyntaxRules(StringBuilder json, Map<EditorSyntaxTokenId, EditorSyntaxStyle> styles) {
        Map<EditorSyntaxTokenId, String> syntaxTokens = Map.of(
                EditorSyntaxTokens.COMMENT, "comment",
                EditorSyntaxTokens.KEYWORD, "keyword",
                EditorSyntaxTokens.TYPE, "type,type.identifier",
                EditorSyntaxTokens.STRING, "string",
                EditorSyntaxTokens.NUMBER, "number",
                EditorSyntaxTokens.ANNOTATION, "annotation");
        json.append("\"rules\":[");
        boolean first = true;
        for (Map.Entry<EditorSyntaxTokenId, String> entry : syntaxTokens.entrySet()) {
            EditorSyntaxStyle style = styles.get(entry.getKey());
            if (style == null) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            first = false;
            appendSyntaxRule(json, entry.getValue(), style);
        }
        json.append(']');
    }

    private static void appendSyntaxRule(StringBuilder json, String token, EditorSyntaxStyle style) {
        json.append('{');
        property(json, "token", token).append(',');
        property(json, "foreground", foreground(style.foreground()));
        if (!style.fontStyles().isEmpty()) {
            json.append(',');
            property(json, "fontStyle", fontStyle(style));
        }
        json.append('}');
    }

    private static String monacoBase(EditorColorThemeKind kind) {
        return switch (kind) {
            case DARK -> "vs-dark";
            case LIGHT -> "vs";
            case HIGH_CONTRAST_DARK -> "hc-black";
            case HIGH_CONTRAST_LIGHT -> "hc-light";
        };
    }

    private static String foreground(EditorColor color) {
        return String.format(Locale.ROOT, "%02x%02x%02x", color.red(), color.green(), color.blue());
    }

    private static String fontStyle(EditorSyntaxStyle style) {
        StringBuilder result = new StringBuilder();
        appendStyle(result, style, EditorFontStyle.ITALIC, "italic");
        appendStyle(result, style, EditorFontStyle.BOLD, "bold");
        appendStyle(result, style, EditorFontStyle.UNDERLINE, "underline");
        return result.toString();
    }

    private static void appendStyle(
            StringBuilder result, EditorSyntaxStyle style, EditorFontStyle candidate, String value) {
        if (style.fontStyles().contains(candidate)) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(value);
        }
    }

    private static StringBuilder property(StringBuilder json, String name, String value) {
        appendJsonString(json, name).append(':');
        return appendJsonString(json, value);
    }

    private static StringBuilder appendJsonString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            appendJsonCharacter(json, value.charAt(index));
        }
        return json.append('"');
    }

    private static void appendJsonCharacter(StringBuilder json, char character) {
        switch (character) {
            case '"' -> json.append("\\\"");
            case '\\' -> json.append("\\\\");
            case '\b' -> json.append("\\b");
            case '\f' -> json.append("\\f");
            case '\n' -> json.append("\\n");
            case '\r' -> json.append("\\r");
            case '\t' -> json.append("\\t");
            default -> appendUnescaped(json, character);
        }
    }

    private static void appendUnescaped(StringBuilder json, char character) {
        if (character < 0x20) {
            json.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
        } else {
            json.append(character);
        }
    }
}
