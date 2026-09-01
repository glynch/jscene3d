/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Optional glTF 2.0 and GLB loading for JScene3D. */
module io.github.glynch.jscene3d.gltf {
    requires transitive io.github.glynch.jscene3d.core;
    requires java.desktop;
    requires jgltf.impl.v2;
    requires jgltf.model;
    requires drako;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.gltf;
}
