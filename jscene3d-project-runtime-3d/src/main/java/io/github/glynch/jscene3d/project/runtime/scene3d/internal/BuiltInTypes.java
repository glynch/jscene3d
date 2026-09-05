/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;

/** Exact version-one identities registered by the built-in extension. */
final class BuiltInTypes {
    private static final String PREFIX = "io.github.glynch.jscene3d/";

    static final RegisteredType GROUP_3D = type("group-3d");
    static final RegisteredType MESH_INSTANCE_3D = type("mesh-instance-3d");
    static final RegisteredType PERSPECTIVE_CAMERA_3D = type("perspective-camera-3d");
    static final RegisteredType AMBIENT_LIGHT_3D = type("ambient-light-3d");
    static final RegisteredType BOX_GEOMETRY_3D = type("box-geometry-3d");
    static final RegisteredType LAMBERT_MATERIAL_3D = type("lambert-material-3d");

    private BuiltInTypes() {
        throw new AssertionError("not instantiable");
    }

    /** Creates one built-in registered type identity. */
    private static RegisteredType type(String id) {
        return new RegisteredType(PREFIX + id, 1);
    }
}
