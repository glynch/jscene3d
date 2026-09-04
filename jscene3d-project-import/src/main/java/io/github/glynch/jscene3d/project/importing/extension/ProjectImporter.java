/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.extension;

import java.io.IOException;

/** Format-specific adapter at the deterministic source-import seam. */
public interface ProjectImporter {
    /**
     * Inspects source content without writing imported artifacts or project files.
     *
     * @param context engine-owned inspection context
     * @throws IOException if source content cannot be read
     */
    void inspect(ImportInspectionContext context) throws IOException;

    /**
     * Writes a complete candidate artifact set into engine-owned staging storage.
     *
     * @param context engine-owned preparation context
     * @throws IOException if source content cannot be converted or staged
     */
    void prepare(ImportPreparationContext context) throws IOException;
}
