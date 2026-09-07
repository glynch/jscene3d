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
import java.util.List;

/** Creates compressed macOS disk images directly through the non-interactive {@code hdiutil} command. */
final class SystemDiskImageTool implements DiskImageTool {
    private static final Path EXECUTABLE = Path.of("/usr/bin/hdiutil");

    @Override
    public void create(Path sourceDirectory, String volumeName, Path destination) throws IOException {
        requireExecutable();
        List<String> command = List.of(
                EXECUTABLE.toString(),
                "create",
                "-quiet",
                "-srcfolder",
                sourceDirectory.toString(),
                "-volname",
                volumeName,
                "-fs",
                "HFS+",
                "-format",
                "UDZO",
                "-ov",
                destination.toString());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (InputStream processOutput = process.getInputStream()) {
            output = new String(processOutput.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exitCode = waitFor(process);
        if (exitCode != 0) {
            throw new IOException("hdiutil failed with exit code " + exitCode + ":\n" + output);
        }
    }

    /** Requires the standard macOS disk-image command to exist before starting it. */
    private static void requireExecutable() throws IOException {
        if (!Files.isRegularFile(EXECUTABLE) || !Files.isExecutable(EXECUTABLE)) {
            throw new IOException("macOS disk-image tool is unavailable: " + EXECUTABLE);
        }
    }

    /** Waits for command completion while retaining Java's interruption contract. */
    private static int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for hdiutil", exception);
        }
    }
}
