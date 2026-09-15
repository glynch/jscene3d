/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.language.EditorTextEdit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import netscape.javascript.JSObject;
import org.junit.jupiter.api.Test;

final class JavaFxMonacoInteropTest {
    @Test
    void convertsMonacoChangesToZeroBasedEditorTextEdits() {
        FakeJsObject range = new FakeJsObject()
                .member("startLineNumber", 2)
                .member("startColumn", 3)
                .member("endLineNumber", 4)
                .member("endColumn", 5);
        FakeJsObject change = new FakeJsObject().member("range", range).member("text", "replacement");
        FakeJsObject changes = new FakeJsObject().member("length", 1).slot(0, change);

        assertThat(JavaFxMonacoInterop.textEdits(changes))
                .containsExactly(new EditorTextEdit(
                        new EditorTextRange(new EditorTextPosition(1, 2), new EditorTextPosition(3, 4)),
                        "replacement"));
    }

    @Test
    void rejectsValuesThatAreNotJavaScriptObjects() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> JavaFxMonacoInterop.textEdits(List.of()))
                .withMessage("changes must be a JavaScript object");
    }

    @Test
    void rejectsNonNumericJavaScriptMembers() {
        FakeJsObject changes = new FakeJsObject().member("length", "one");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> JavaFxMonacoInterop.textEdits(changes))
                .withMessage("length must be numeric");
    }

    @AllowJavaFxWebInterop
    private static final class FakeJsObject extends JSObject {
        private final Map<String, Object> members = new HashMap<>();
        private final Map<Integer, Object> slots = new HashMap<>();

        private FakeJsObject member(String name, Object value) {
            members.put(name, value);
            return this;
        }

        private FakeJsObject slot(int index, Object value) {
            slots.put(index, value);
            return this;
        }

        @Override
        public Object call(String methodName, Object... arguments) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object eval(String script) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getMember(String name) {
            return members.get(name);
        }

        @Override
        public void setMember(String name, Object value) {
            members.put(name, value);
        }

        @Override
        public void removeMember(String name) {
            members.remove(name);
        }

        @Override
        public Object getSlot(int index) {
            return slots.get(index);
        }

        @Override
        public void setSlot(int index, Object value) {
            slots.put(index, value);
        }
    }
}
