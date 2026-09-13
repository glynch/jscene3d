/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies safe, composable workspace exclusion policy behavior. */
final class WorkspaceExplorerExclusionPolicyTest {
    @TempDir
    private Path workspace;

    /** Applies each built-in exclusion category without conflating file and directory names. */
    @Test
    void appliesDefaultExclusionCategories() throws IOException {
        Path git = Files.createDirectory(workspace.resolve(".git"));
        Path targetDirectory = Files.createDirectory(workspace.resolve("target"));
        Path targetFile = Files.writeString(
                Files.createDirectory(workspace.resolve("files")).resolve("target"), "source");
        Path metadataFile = Files.writeString(workspace.resolve(".DS_Store"), "metadata");
        Path metadataDirectory = Files.createDirectory(
                Files.createDirectory(workspace.resolve("directories")).resolve(".DS_Store"));
        Path editorCache = Files.createDirectories(workspace.resolve(".jscene3d/cache/indexes"));
        Path ordinaryCache = Files.createDirectory(workspace.resolve("cache"));

        WorkspaceExplorerExclusionPolicy policy = WorkspaceExplorerExclusionPolicy.defaults();

        assertThat(policy.includes(workspace, git)).isFalse();
        assertThat(policy.includes(workspace, targetDirectory)).isFalse();
        assertThat(policy.includes(workspace, targetFile)).isTrue();
        assertThat(policy.includes(workspace, metadataFile)).isFalse();
        assertThat(policy.includes(workspace, metadataDirectory)).isTrue();
        assertThat(policy.includes(workspace, editorCache)).isFalse();
        assertThat(policy.includes(workspace, ordinaryCache)).isTrue();
    }

    /** Never exposes symlinks or normalized paths outside the workspace. */
    @Test
    void enforcesWorkspaceSafetyInvariants() throws IOException {
        Path source = Files.writeString(workspace.resolve("source.txt"), "source");
        Path link = Files.createSymbolicLink(workspace.resolve("link.txt"), source);
        Path outside = workspace.resolve("../outside.txt");
        WorkspaceExplorerExclusionPolicy policy = WorkspaceExplorerExclusionPolicy.defaults();

        assertThat(policy.includes(workspace, link)).isFalse();
        assertThat(policy.includes(workspace, outside)).isFalse();
    }

    /** Combines exclusions supplied by independent policy sources. */
    @Test
    void combinesContributedExclusions() throws IOException {
        Path generated = Files.createDirectory(workspace.resolve("generated"));
        Path secrets = Files.writeString(workspace.resolve("secrets.json"), "{}");
        Path reports = Files.createDirectories(workspace.resolve("build/reports/tests"));
        WorkspaceExplorerExclusionPolicy contributed = WorkspaceExplorerExclusionPolicy.of(
                Set.of("generated"), Set.of("secrets.json"), Set.of(Path.of("build", "reports")));
        WorkspaceExplorerExclusionPolicy policy =
                WorkspaceExplorerExclusionPolicy.defaults().plus(contributed);

        assertThat(policy.includes(workspace, generated)).isFalse();
        assertThat(policy.includes(workspace, secrets)).isFalse();
        assertThat(policy.includes(workspace, reports)).isFalse();
    }

    /** Rejects malformed configured names and workspace-relative trees. */
    @Test
    void rejectsInvalidConfiguration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WorkspaceExplorerExclusionPolicy.of(Set.of("nested/name"), Set.of(), Set.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WorkspaceExplorerExclusionPolicy.of(Set.of(), Set.of(), Set.of(Path.of("../out"))));
    }
}
