/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.importing.ImportExecution;
import io.github.glynch.jscene3d.project.manifest.GameProject;

/** Concrete read-only adapter context used during source inspection. */
public final class InspectionContext extends AbstractImportContext {
    /**
     * Creates one inspection context.
     *
     * @param project containing project
     * @param asset authoritative source asset
     * @param execution caller-owned execution policy
     */
    public InspectionContext(GameProject project, GameProject.AssetSource asset, ImportExecution execution) {
        super(project, asset, execution);
    }
}
