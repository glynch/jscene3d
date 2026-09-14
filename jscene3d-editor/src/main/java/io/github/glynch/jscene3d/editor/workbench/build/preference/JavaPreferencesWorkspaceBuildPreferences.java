/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.preference;

import io.github.glynch.jscene3d.editor.EditorApplication;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.prefs.Preferences;

/** Persists per-workspace build choices in the operating system's user preference store. */
public final class JavaPreferencesWorkspaceBuildPreferences implements WorkspaceBuildPreferences {
    private static final String AUTOMATIC_BUILD = "automaticBuild";
    private static final String WORKSPACES = "workspaces";

    private final Preferences preferences;

    /** Creates the production per-user workspace-build preference adapter. */
    public JavaPreferencesWorkspaceBuildPreferences() {
        this(Preferences.userNodeForPackage(EditorApplication.class).node("build"));
    }

    JavaPreferencesWorkspaceBuildPreferences(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    @Override
    public boolean automaticBuild(Path workspaceRoot) {
        return workspace(workspaceRoot).getBoolean(AUTOMATIC_BUILD, true);
    }

    @Override
    public void saveAutomaticBuild(Path workspaceRoot, boolean enabled) {
        workspace(workspaceRoot).putBoolean(AUTOMATIC_BUILD, enabled);
    }

    private Preferences workspace(Path workspaceRoot) {
        return preferences.node(WORKSPACES).node(identity(workspaceRoot));
    }

    private static String identity(Path workspaceRoot) {
        String normalized = Objects.requireNonNull(workspaceRoot, "workspaceRoot")
                .toAbsolutePath()
                .normalize()
                .toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
