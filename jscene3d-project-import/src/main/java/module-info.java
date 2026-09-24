/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Deterministic source inspection, import preparation, and cache publication. */
module io.github.glynch.jscene3d.project.importing {
    requires io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.project;
    requires com.fasterxml.jackson.databind;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.importing;
    exports io.github.glynch.jscene3d.project.importing.extension;

    opens io.github.glynch.jscene3d.project.importing.internal to
            com.fasterxml.jackson.databind;

    uses io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
}
