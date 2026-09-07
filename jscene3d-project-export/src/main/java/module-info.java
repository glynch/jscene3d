/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Generic application-directory and native-image export for JScene3D game projects. */
module io.github.glynch.jscene3d.project.exporting {
    requires io.github.glynch.jscene3d.project;
    requires io.github.glynch.jscene3d.project.desktop;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.exporting;
}
