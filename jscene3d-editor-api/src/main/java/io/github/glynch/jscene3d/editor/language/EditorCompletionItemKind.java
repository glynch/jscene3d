/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

/**
 * Describes the semantic kind of an editor completion item.
 *
 * <p>The kinds are independent of any particular language-server protocol and may be used by
 * editor clients to choose an appropriate visual representation for a completion candidate.
 */
public enum EditorCompletionItemKind {
    /** Plain text completion. */
    TEXT,

    /** Method completion. */
    METHOD,

    /** Function completion. */
    FUNCTION,

    /** Constructor completion. */
    CONSTRUCTOR,

    /** Field completion. */
    FIELD,

    /** Variable completion. */
    VARIABLE,

    /** Class completion. */
    CLASS,

    /** Interface completion. */
    INTERFACE,

    /** Module completion. */
    MODULE,

    /** Property completion. */
    PROPERTY,

    /** Unit completion. */
    UNIT,

    /** Value completion. */
    VALUE,

    /** Enumeration type completion. */
    ENUM,

    /** Language keyword completion. */
    KEYWORD,

    /** Snippet completion. */
    SNIPPET,

    /** Color value completion. */
    COLOR,

    /** File completion. */
    FILE,

    /** Reference completion. */
    REFERENCE,

    /** Folder completion. */
    FOLDER,

    /** Enumeration member completion. */
    ENUM_MEMBER,

    /** Constant completion. */
    CONSTANT,

    /** Structure type completion. */
    STRUCT,

    /** Event completion. */
    EVENT,

    /** Operator completion. */
    OPERATOR,

    /** Type parameter completion. */
    TYPE_PARAMETER
}
