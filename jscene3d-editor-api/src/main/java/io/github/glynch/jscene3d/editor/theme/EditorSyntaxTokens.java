/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

/** Standard semantic source-code categories understood by the built-in editor adapter. */
public final class EditorSyntaxTokens {
    private static final String PREFIX = "io.github.glynch.jscene3d.editor.syntax.";

    public static final EditorSyntaxTokenId COMMENT = token("comment");
    public static final EditorSyntaxTokenId KEYWORD = token("keyword");
    public static final EditorSyntaxTokenId TYPE = token("type");
    public static final EditorSyntaxTokenId STRING = token("string");
    public static final EditorSyntaxTokenId NUMBER = token("number");
    public static final EditorSyntaxTokenId ANNOTATION = token("annotation");

    private static EditorSyntaxTokenId token(String suffix) {
        return new EditorSyntaxTokenId(PREFIX + suffix);
    }

    private EditorSyntaxTokens() {}
}
