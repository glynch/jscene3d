/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import java.util.List;
import java.util.Locale;

/** Serializes editor diagnostics into Monaco marker data. */
final class MonacoDiagnosticsJson {
    private MonacoDiagnosticsJson() {}

    static String encode(List<EditorDiagnosticSnapshot> diagnostics) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (EditorDiagnosticSnapshot snapshot : diagnostics) {
            EditorDiagnostic diagnostic = snapshot.diagnostic();
            if (diagnostic.range().isEmpty()) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            first = false;
            appendMarker(json, snapshot, diagnostic.range().orElseThrow());
        }
        return json.append(']').toString();
    }

    private static void appendMarker(StringBuilder json, EditorDiagnosticSnapshot snapshot, EditorTextRange range) {
        json.append('{');
        number(json, "startLineNumber", range.start().line() + 1).append(',');
        number(json, "startColumn", range.start().character() + 1).append(',');
        number(json, "endLineNumber", range.end().line() + 1).append(',');
        number(json, "endColumn", range.end().character() + 1).append(',');
        string(json, "severity", snapshot.diagnostic().severity().name()).append(',');
        string(json, "message", snapshot.diagnostic().message()).append(',');
        string(json, "code", snapshot.diagnostic().code()).append(',');
        String source = snapshot.diagnostic().source();
        string(json, "source", source.isEmpty() ? snapshot.collection().value() : source);
        json.append('}');
    }

    private static StringBuilder number(StringBuilder json, String name, int value) {
        name(json, name);
        return json.append(value);
    }

    private static StringBuilder string(StringBuilder json, String name, String value) {
        name(json, name);
        return appendString(json, value);
    }

    private static void name(StringBuilder json, String value) {
        appendString(json, value).append(':');
    }

    private static StringBuilder appendString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> appendCharacter(json, character);
            }
        }
        return json.append('"');
    }

    private static void appendCharacter(StringBuilder json, char character) {
        if (character < 0x20) {
            json.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
        } else {
            json.append(character);
        }
    }
}
