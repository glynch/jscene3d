/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.command;

/**
 * Build capabilities currently available to project command surfaces.
 *
 * @param buildAvailable whether the active build adapter can accept a build request
 * @param building whether a build is currently starting or running
 * @param outputAvailable whether the current project session has build output
 */
public record ProjectBuildCommandAvailability(boolean buildAvailable, boolean building, boolean outputAvailable) {
    /** Availability before a project build coordinator is attached. */
    public static final ProjectBuildCommandAvailability UNAVAILABLE =
            new ProjectBuildCommandAvailability(false, false, false);
}
