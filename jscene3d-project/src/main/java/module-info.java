/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Headless loading and validation of versioned JScene3D game projects. */
module io.github.glynch.jscene3d.project {
    requires com.fasterxml.jackson.databind;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project;

    opens io.github.glynch.jscene3d.project.internal to
            com.fasterxml.jackson.databind;
}
