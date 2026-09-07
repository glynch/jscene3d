/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Executes the {@code jpackage} tool belonging to the Java runtime running the exporter. */
final class SystemJpackageTool implements JpackageTool {
    private final Path executable;

    /** Resolves the packaging tool from the current Java runtime. */
    SystemJpackageTool() {
        executable = Path.of(System.getProperty("java.home"), "bin", executableName());
    }

    @Override
    public JpackageToolResult execute(List<String> arguments) throws IOException {
        if (!Files.isRegularFile(executable)) {
            throw new IOException("current Java runtime does not provide jpackage: " + executable);
        }
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(executable.toString());
        command.addAll(arguments);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (InputStream processOutput = process.getInputStream()) {
            output = new String(processOutput.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new JpackageToolResult(waitFor(process), output);
    }

    /** Waits for tool completion while retaining Java's interruption contract. */
    private static int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for jpackage", exception);
        }
    }

    /** Returns the host-specific JDK tool filename. */
    private static String executableName() {
        return System.getProperty("os.name").startsWith("Windows") ? "jpackage.exe" : "jpackage";
    }
}
