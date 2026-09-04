/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.extension;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.importing.ImportProgress;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.nio.file.Path;
import java.util.Map;

/** Engine-owned context exposed to a format adapter during read-only source inspection. */
public interface ImportInspectionContext {
    /**
     * Returns the containing validated project.
     *
     * @return containing validated project
     */
    GameProject project();

    /**
     * Returns the authoritative source asset.
     *
     * @return authoritative source asset
     */
    GameProject.AssetSource asset();

    /**
     * Records one source dependency whose content participates in invalidation.
     *
     * @param path absolute path or path relative to the project root
     */
    void dependency(Path path);

    /**
     * Adds one discovered source item.
     *
     * @param item immutable source item
     */
    void sourceItem(SourceItem item);

    /**
     * Adds a non-terminal inspection warning.
     *
     * @param code stable adapter-qualified diagnostic identity
     * @param location source-local location or empty string
     * @param details immutable values describing this occurrence
     */
    void warning(DiagnosticCode code, String location, Map<String, String> details);

    /**
     * Adds a terminal inspection or preparation error.
     *
     * @param code stable adapter-qualified diagnostic identity
     * @param location source-local location or empty string
     * @param details immutable values describing this occurrence
     */
    void error(DiagnosticCode code, String location, Map<String, String> details);

    /**
     * Reports adapter-specific progress synchronously.
     *
     * @param progress immutable progress update
     */
    void progress(ImportProgress progress);

    /**
     * Stops with a distinct cancellation outcome when requested by the caller.
     *
     * <p>Importers must call this at I/O boundaries and bounded intervals in long loops.
     */
    void checkCancelled();
}
