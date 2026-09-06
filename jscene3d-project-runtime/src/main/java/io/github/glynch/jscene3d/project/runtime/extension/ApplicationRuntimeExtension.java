/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.runtime.HostedProject;

/** Manifest-selected executable application entry point discovered by the generic project host. */
public interface ApplicationRuntimeExtension extends ComponentRuntimeExtension {
    /**
     * Prepares application-owned definitions and state after composition and before activation.
     *
     * <p>The extension is selected by {@code runtime.applicationExtension}; implementing this interface alone never
     * causes participation. The method runs on the caller's host thread and must not activate the world.
     *
     * @param project composed inactive project
     */
    void prepare(HostedProject project);
}
