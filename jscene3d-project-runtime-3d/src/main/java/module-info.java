/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Platform-neutral native 3d scene composition for declarative projects. */
module io.github.glynch.jscene3d.project.runtime.scene3d {
    requires transitive io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.project.runtime;
    requires org.joml;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.project.runtime.scene3d;
}
