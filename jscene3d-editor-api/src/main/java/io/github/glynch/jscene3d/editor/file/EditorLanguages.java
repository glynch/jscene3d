/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.file;

/** Well-known language identities supported by the built-in source editor. */
public final class EditorLanguages {
    /** Java source. */
    public static final EditorLanguageId JAVA = new EditorLanguageId("java");

    /** JavaScript Object Notation. */
    public static final EditorLanguageId JSON = new EditorLanguageId("json");

    /** Extensible Markup Language, including Maven project files. */
    public static final EditorLanguageId XML = new EditorLanguageId("xml");

    /** Text without richer language support. */
    public static final EditorLanguageId PLAIN_TEXT = new EditorLanguageId("plaintext");

    private EditorLanguages() {}
}
