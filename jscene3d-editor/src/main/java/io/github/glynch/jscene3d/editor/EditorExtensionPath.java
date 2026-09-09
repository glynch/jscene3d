/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Locates installed extension artifacts whose descriptors are safe for the editor to inspect. */
final class EditorExtensionPath {
    static final String PROPERTY = "jscene3d.editor.extensionPath";

    private EditorExtensionPath() {}

    /** Returns configured artifact files and directories in declaration order. */
    static List<Path> configured() {
        String configured = System.getProperty(PROPERTY, "");
        if (configured.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configured.split(File.pathSeparator, -1))
                .filter(element -> !element.isBlank())
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .toList();
    }
}
