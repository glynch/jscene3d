/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Creates the representative filesystem used by Workspace Explorer tests. */
final class WorkspaceExplorerTestFixture {
    private WorkspaceExplorerTestFixture() {}

    static void create(Path workspace) throws IOException {
        Path root = Objects.requireNonNull(workspace, "workspace");
        write(root, "src/main/java/example/Player.java", "final class Player {}\n");
        write(root, "pom.xml", "<project/>\n");
        write(root, "local.env", "private\n");
        write(root, "target/classes/Player.class", "generated\n");
        write(root, ".git/config", "private\n");
        write(root, ".jscene3d/cache/index", "generated\n");
        write(root, ".jscene3d/settings.json", "{}\n");
    }

    private static void write(Path workspace, String relativePath, String content) throws IOException {
        Path file = workspace.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
