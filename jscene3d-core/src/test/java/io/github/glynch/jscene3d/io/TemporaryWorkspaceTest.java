/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises temporary workspace creation, isolation, and cleanup through its public interface. */
final class TemporaryWorkspaceTest {
    @TempDir
    private Path temporaryDirectory;

    /** Creates isolated child resources and removes their complete tree on close. */
    @Test
    void ownsTemporaryTree() throws IOException {
        TemporaryWorkspace workspace = TemporaryWorkspace.create("jscene3d-test-");
        Path root = workspace.root();
        Path directory = workspace.createDirectory("directory-");
        Path file = workspace.createFile("resource-", ".json");

        assertThat(root).isAbsolute().isDirectory();
        assertOwnerOnlyPermissions(root);
        assertThat(directory).isDirectory().hasParent(root);
        assertThat(file).isRegularFile().hasParent(root);
        assertThat(workspace.isClosed()).isFalse();

        workspace.close();

        assertThat(workspace.isClosed()).isTrue();
        assertThat(Files.exists(root)).isFalse();
        workspace.close();
    }

    /** Creates a secure workspace directly beneath a caller-supplied directory. */
    @Test
    void createsWorkspaceBeneathSuppliedDirectory() throws IOException {
        TemporaryWorkspace workspace = TemporaryWorkspace.create(temporaryDirectory, "staging-");
        Path root = workspace.root();

        assertThat(root).isDirectory().hasParent(temporaryDirectory.toRealPath());
        assertOwnerOnlyPermissions(root);

        workspace.close();

        assertThat(root).doesNotExist();
    }

    /** Confirms restrictive root permissions on filesystems supporting POSIX attributes. */
    private static void assertOwnerOnlyPermissions(Path root) throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            assertThat(Files.getPosixFilePermissions(root))
                    .containsExactlyInAnyOrderElementsOf(PosixFilePermissions.fromString("rwx------"));
        }
    }

    /** Makes closure terminal for every operation other than lifecycle inspection and close. */
    @Test
    void rejectsOperationsAfterClose() {
        TemporaryWorkspace workspace = TemporaryWorkspace.create("jscene3d-test-");
        workspace.close();

        assertThatIllegalStateException().isThrownBy(workspace::root).withMessage("Temporary workspace is closed");
        assertThatIllegalStateException()
                .isThrownBy(() -> workspace.createDirectory("directory-"))
                .withMessage("Temporary workspace is closed");
        assertThatIllegalStateException()
                .isThrownBy(() -> workspace.createFile("resource-", ".json"))
                .withMessage("Temporary workspace is closed");
    }

    /** Rejects names that could escape or ambiguously identify workspace children. */
    @Test
    void rejectsInvalidNameParts() {
        assertThatNullPointerException().isThrownBy(() -> TemporaryWorkspace.create(null));
        assertThatIllegalArgumentException().isThrownBy(() -> TemporaryWorkspace.create("ab"));

        Path missingParent = temporaryDirectory.resolve("missing");
        assertThatIllegalArgumentException().isThrownBy(() -> TemporaryWorkspace.create(missingParent, "staging-"));

        try (TemporaryWorkspace workspace = TemporaryWorkspace.create("jscene3d-test-")) {
            assertThatIllegalArgumentException().isThrownBy(() -> workspace.createDirectory("../outside"));
            assertThatIllegalArgumentException().isThrownBy(() -> workspace.createFile("resource-", "../outside"));
        }
    }
}
