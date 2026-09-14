/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.preference;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;

/** Verifies workspace isolation and persistence of automatic-build choices. */
final class WorkspaceBuildPreferencesTest {
    @Test
    void inMemoryPreferencesDefaultToEnabledAndIsolateWorkspaces() {
        InMemoryWorkspaceBuildPreferences preferences = new InMemoryWorkspaceBuildPreferences();
        Path first = Path.of("projects/first");
        Path second = Path.of("projects/second");

        preferences.saveAutomaticBuild(first, false);

        assertThat(preferences.automaticBuild(first)).isFalse();
        assertThat(preferences.automaticBuild(second)).isTrue();
    }

    @Test
    void javaPreferencesSurviveAdapterReplacementAndNormalizeWorkspaceIdentity() throws BackingStoreException {
        Preferences node = Preferences.userRoot().node("io/github/glynch/jscene3d/tests/build/" + UUID.randomUUID());
        try {
            Path workspace = Path.of("projects/example");
            JavaPreferencesWorkspaceBuildPreferences first = new JavaPreferencesWorkspaceBuildPreferences(node);
            first.saveAutomaticBuild(workspace, false);

            JavaPreferencesWorkspaceBuildPreferences replacement = new JavaPreferencesWorkspaceBuildPreferences(node);

            assertThat(replacement.automaticBuild(workspace.toAbsolutePath().normalize()))
                    .isFalse();
            assertThat(replacement.automaticBuild(Path.of("projects/other"))).isTrue();
        } finally {
            node.removeNode();
        }
    }
}
