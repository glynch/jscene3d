/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import java.nio.file.Path;

/** Command-line entry point for a packaged desktop project.
 *
 * <p>The packaging layer supplies the engine version, packaged project root, and published-content
 * root. Project-specific behavior is discovered from the manifest-selected runtime extension on
 * the application class path; this launcher contains no application-specific lifecycle hooks.
 */
public final class DesktopProjectLauncher {
    private static final int ARGUMENT_COUNT = 3;

    /** Prevents construction of this command-line entry point. */
    private DesktopProjectLauncher() {
        throw new AssertionError("DesktopProjectLauncher cannot be instantiated");
    }

    /** Runs one packaged project until its native window requests closure.
     *
     * @param arguments engine version, project directory, and published-content directory
     */
    public static void main(String[] arguments) {
        if (arguments.length != ARGUMENT_COUNT) {
            throw new IllegalArgumentException(
                    "expected engine-version, project-directory, and published-content-directory arguments");
        }
        DesktopProjectRunner runner = new DesktopProjectRunner(
                arguments[0], DesktopProjectLauncher.class.getClassLoader(), Path.of(arguments[2]));
        runner.run(Path.of(arguments[1]));
    }
}
