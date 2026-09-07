/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable description of one native application image produced for the current host platform. */
public final class ApplicationImage {
    private final Path root;
    private final Path launcher;

    /** Stores paths validated by the native-image exporter. */
    ApplicationImage(Path root, Path launcher) {
        this.root = Objects.requireNonNull(root, "root");
        this.launcher = Objects.requireNonNull(launcher, "launcher");
    }

    /**
     * Returns the native application-image root.
     *
     * @return normalized absolute application-image root
     */
    public Path root() {
        return root;
    }

    /**
     * Returns the native executable launcher.
     *
     * @return launcher within the application image
     */
    public Path launcher() {
        return launcher;
    }
}
