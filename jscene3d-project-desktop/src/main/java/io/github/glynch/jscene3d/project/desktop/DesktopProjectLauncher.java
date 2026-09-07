/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

/** Command-line entry point for a packaged desktop project.
 *
 * <p>The packaging layer supplies the engine version, packaged project root, and published-content
 * root as three command-line arguments or through the documented launch properties. Project-specific
 * behavior is discovered from the manifest-selected runtime extension on the application class path;
 * this launcher contains no application-specific lifecycle hooks.
 */
public final class DesktopProjectLauncher {
    /** Launch property containing the exact JScene3D engine version. */
    public static final String ENGINE_VERSION_PROPERTY = "jscene3d.launch.engine.version";

    /** Launch property containing the packaged authored-project directory. */
    public static final String PROJECT_DIRECTORY_PROPERTY = "jscene3d.launch.project.directory";

    /** Launch property containing the packaged published-content directory. */
    public static final String CONTENT_DIRECTORY_PROPERTY = "jscene3d.launch.content.directory";

    /** Prevents construction of this command-line entry point. */
    private DesktopProjectLauncher() {
        throw new AssertionError("DesktopProjectLauncher cannot be instantiated");
    }

    /**
     * Runs one packaged project until its native window requests closure.
     *
     * <p>Three arguments supply the engine version, project directory, and published-content directory
     * directly. With no arguments, all three values are read from {@link #ENGINE_VERSION_PROPERTY},
     * {@link #PROJECT_DIRECTORY_PROPERTY}, and {@link #CONTENT_DIRECTORY_PROPERTY}. This second form lets
     * a native packager resolve paths relative to its installed application directory before Java starts.
     *
     * @param arguments either no arguments or the three ordered launch values
     */
    public static void main(String[] arguments) {
        DesktopLaunchConfiguration configuration =
                DesktopLaunchConfiguration.resolve(arguments, System.getProperties());
        DesktopProjectRunner runner = new DesktopProjectRunner(
                configuration.engineVersion(),
                DesktopProjectLauncher.class.getClassLoader(),
                configuration.contentDirectory());
        runner.run(configuration.projectDirectory());
    }
}
