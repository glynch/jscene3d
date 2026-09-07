/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Immutable description of one successfully assembled relocatable application directory. */
public final class ApplicationDirectory {
    private final Path root;
    private final String launcherName;
    private final List<Path> runtimeArtifacts;

    /** Stores output paths produced by the exporter. */
    ApplicationDirectory(Path root, String launcherName, List<Path> runtimeArtifacts) {
        this.root = Objects.requireNonNull(root, "root");
        this.launcherName = Objects.requireNonNull(launcherName, "launcherName");
        this.runtimeArtifacts = List.copyOf(runtimeArtifacts);
    }

    /**
     * Returns the application-directory root.
     *
     * @return normalized absolute output root
     */
    public Path root() {
        return root;
    }

    /**
     * Returns the packaged authored project directory.
     *
     * @return project directory below the application root
     */
    public Path projectDirectory() {
        return root.resolve("project");
    }

    /**
     * Returns the packaged published-content directory.
     *
     * @return content directory below the application root
     */
    public Path publishedContentDirectory() {
        return root.resolve("content");
    }

    /**
     * Returns packaged runtime JARs in deterministic filename order.
     *
     * @return immutable output artifact paths
     */
    public List<Path> runtimeArtifacts() {
        return runtimeArtifacts;
    }

    /**
     * Returns the generated POSIX launcher.
     *
     * @return launcher path below the application root
     */
    public Path posixLauncher() {
        return root.resolve("bin").resolve(launcherName);
    }

    /**
     * Returns the generated Windows launcher.
     *
     * @return command-file path below the application root
     */
    public Path windowsLauncher() {
        return root.resolve("bin").resolve(launcherName + ".cmd");
    }
}
