/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Complete shell-free launch configuration for one language-server process.
 *
 * @param command non-empty executable and argument list
 * @param workingDirectory absolute process working directory
 * @param errorLog absolute file receiving the server's standard-error stream
 */
public record LanguageServerProcessConfiguration(List<String> command, Path workingDirectory, Path errorLog) {
    /** Copies and validates one process configuration. */
    public LanguageServerProcessConfiguration {
        command = List.copyOf(Objects.requireNonNull(command, "command"));
        if (command.isEmpty() || command.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("command must contain only non-blank arguments");
        }
        workingDirectory = requireAbsolute(workingDirectory, "workingDirectory");
        errorLog = requireAbsolute(errorLog, "errorLog");
    }

    private static Path requireAbsolute(Path path, String name) {
        Path candidate = Objects.requireNonNull(path, name).normalize();
        if (!candidate.isAbsolute()) {
            throw new IllegalArgumentException(name + " must be absolute");
        }
        return candidate;
    }
}
