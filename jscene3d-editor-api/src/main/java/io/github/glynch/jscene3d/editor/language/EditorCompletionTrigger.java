/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes how an editor completion request was triggered.
 *
 * <p>A completion may be explicitly requested by the user or triggered automatically by a
 * character entered into the document.
 */
public final class EditorCompletionTrigger {

    /** Describes the origin of a completion request. */
    public enum Kind {
        /** Completion was explicitly requested by the user. */
        MANUAL,

        /** Completion was triggered by a character entered into the document. */
        TRIGGER_CHARACTER
    }

    private static final EditorCompletionTrigger MANUAL = new EditorCompletionTrigger(Kind.MANUAL, null);

    private final Kind kind;
    private final String triggerCharacter;

    private EditorCompletionTrigger(Kind kind, String triggerCharacter) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.triggerCharacter = triggerCharacter;
    }

    /**
     * Returns a trigger representing an explicit completion request.
     *
     * @return the manual completion trigger
     */
    public static EditorCompletionTrigger manual() {
        return MANUAL;
    }

    /**
     * Creates a trigger for a character entered into the document.
     *
     * @param triggerCharacter the character that triggered completion
     * @return the character completion trigger
     * @throws NullPointerException if {@code triggerCharacter} is {@code null}
     * @throws IllegalArgumentException if {@code triggerCharacter} is not exactly one Unicode code
     *     point
     */
    public static EditorCompletionTrigger character(String triggerCharacter) {
        Objects.requireNonNull(triggerCharacter, "triggerCharacter");
        if (triggerCharacter.codePointCount(0, triggerCharacter.length()) != 1) {
            throw new IllegalArgumentException("triggerCharacter must contain exactly one Unicode code point");
        }
        return new EditorCompletionTrigger(Kind.TRIGGER_CHARACTER, triggerCharacter);
    }

    /**
     * Returns the origin of the completion request.
     *
     * @return the trigger kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the character that triggered completion, when applicable.
     *
     * @return the trigger character, or an empty optional for manual completion
     */
    public Optional<String> triggerCharacter() {
        return Optional.ofNullable(triggerCharacter);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EditorCompletionTrigger that)) {
            return false;
        }
        return kind == that.kind && Objects.equals(triggerCharacter, that.triggerCharacter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, triggerCharacter);
    }

    @Override
    public String toString() {
        return "EditorCompletionTrigger[kind=" + kind + ", triggerCharacter=" + triggerCharacter + ']';
    }
}
