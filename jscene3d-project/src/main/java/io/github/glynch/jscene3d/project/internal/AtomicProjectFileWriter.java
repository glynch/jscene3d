/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Atomically replaces complete project files through a sibling temporary file. */
public final class AtomicProjectFileWriter {
    /** Prevents construction of this stateless writer. */
    private AtomicProjectFileWriter() {
        throw new AssertionError("AtomicProjectFileWriter cannot be instantiated");
    }

    /**
     * Creates parent directories and atomically replaces one complete target file.
     *
     * @param target target project file
     * @param content complete replacement content
     * @throws IOException when the complete file cannot be written and moved atomically
     */
    public static void write(Path target, byte[] content) throws IOException {
        Path absoluteTarget =
                Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
        byte[] replacement = Objects.requireNonNull(content, "content");
        Path parent = Objects.requireNonNull(absoluteTarget.getParent(), "target must have a parent directory");
        Path fileName = Objects.requireNonNull(absoluteTarget.getFileName(), "target must name a file");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "." + fileName + ".", ".tmp");
        boolean replaced = false;
        try {
            Files.write(temporary, replacement);
            Files.move(temporary, absoluteTarget, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            replaced = true;
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
