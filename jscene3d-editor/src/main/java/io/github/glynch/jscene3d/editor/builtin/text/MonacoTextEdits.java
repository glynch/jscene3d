/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.language.EditorTextEdit;
import java.util.ArrayList;
import java.util.List;
import netscape.javascript.JSObject;

/** Converts Monaco's native change array into toolkit-independent incremental text edits. */
final class MonacoTextEdits {
    private MonacoTextEdits() {}

    static List<EditorTextEdit> from(JSObject changes) {
        int length = number(changes, "length");
        List<EditorTextEdit> edits = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            JSObject change = object(changes.getSlot(index), "change");
            JSObject range = object(change.getMember("range"), "range");
            edits.add(new EditorTextEdit(
                    new EditorTextRange(
                            new EditorTextPosition(
                                    number(range, "startLineNumber") - 1, number(range, "startColumn") - 1),
                            new EditorTextPosition(number(range, "endLineNumber") - 1, number(range, "endColumn") - 1)),
                    String.valueOf(change.getMember("text"))));
        }
        return List.copyOf(edits);
    }

    private static JSObject object(Object value, String name) {
        if (value instanceof JSObject object) {
            return object;
        }
        throw new IllegalArgumentException(name + " must be a JavaScript object");
    }

    private static int number(JSObject object, String member) {
        Object value = object.getMember(member);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException(member + " must be numeric");
    }
}
