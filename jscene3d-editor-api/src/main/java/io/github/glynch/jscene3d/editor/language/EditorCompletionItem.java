/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Describes a completion candidate returned by a language service.
 *
 * <p>The label is the text presented to the user. Other properties provide optional semantic and
 * descriptive information about the candidate. A text edit is present when the language service
 * supplies an ordinary replacement edit that can be represented by {@link EditorTextEdit}.
 *
 * <p>This type does not currently represent snippet insertion, additional text edits, commands, or
 * distinct insert and replace ranges.
 */
public final class EditorCompletionItem {

    private final String label;
    private final @Nullable EditorCompletionItemKind kind;
    private final @Nullable String detail;
    private final @Nullable String documentation;
    private final @Nullable EditorTextEdit textEdit;

    /**
     * Creates a completion item.
     *
     * @param label the non-empty display label
     * @param kind the semantic completion kind, or {@code null} when unspecified
     * @param detail additional descriptive information, or {@code null} when unspecified
     * @param documentation documentation for the candidate, or {@code null} when unspecified
     * @param textEdit the text edit supplied for the candidate, or {@code null} when unavailable
     * @throws NullPointerException if {@code label} is {@code null}
     * @throws IllegalArgumentException if {@code label} is empty
     */
    public EditorCompletionItem(
            String label,
            @Nullable EditorCompletionItemKind kind,
            @Nullable String detail,
            @Nullable String documentation,
            @Nullable EditorTextEdit textEdit) {
        this.label = Objects.requireNonNull(label, "label");
        if (label.isEmpty()) {
            throw new IllegalArgumentException("label must not be empty");
        }
        this.kind = kind;
        this.detail = detail;
        this.documentation = documentation;
        this.textEdit = textEdit;
    }

    /**
     * Returns the display label.
     *
     * @return the completion label
     */
    public String label() {
        return label;
    }

    /**
     * Returns the semantic kind when supplied by the language service.
     *
     * @return the completion kind
     */
    public Optional<EditorCompletionItemKind> kind() {
        return Optional.ofNullable(kind);
    }

    /**
     * Returns additional descriptive information when available.
     *
     * @return the completion detail
     */
    public Optional<String> detail() {
        return Optional.ofNullable(detail);
    }

    /**
     * Returns documentation for the completion candidate when available.
     *
     * @return the completion documentation
     */
    public Optional<String> documentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * Returns the replacement edit supplied by the language service when it can be represented by
     * the editor text-edit model.
     *
     * @return the completion text edit
     */
    public Optional<EditorTextEdit> textEdit() {
        return Optional.ofNullable(textEdit);
    }
}
