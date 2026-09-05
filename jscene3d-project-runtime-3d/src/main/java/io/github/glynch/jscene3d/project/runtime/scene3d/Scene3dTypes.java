/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d;

import io.github.glynch.jscene3d.project.extension.RegisteredType;

/** Stable registered-type identities supplied by the built-in 3d runtime extension. */
public final class Scene3dTypes {
    private static final String PREFIX = JScene3dRuntimeExtension.EXTENSION_ID + "/";

    /** Transform-group scene-node type. */
    public static final RegisteredType GROUP_3D = type("group-3d");

    /** Renderable mesh scene-node type. */
    public static final RegisteredType MESH_INSTANCE_3D = type("mesh-instance-3d");

    /** Perspective-camera scene-node type. */
    public static final RegisteredType PERSPECTIVE_CAMERA_3D = type("perspective-camera-3d");

    /** Ambient-light scene-node type. */
    public static final RegisteredType AMBIENT_LIGHT_3D = type("ambient-light-3d");

    /** Static physics-body scene-node type. */
    public static final RegisteredType STATIC_BODY_3D = type("static-body-3d");

    /** Kinematic character physics-body scene-node type. */
    public static final RegisteredType CHARACTER_BODY_3D = type("character-body-3d");

    /** Collision-shape scene-node type. */
    public static final RegisteredType COLLISION_SHAPE_3D = type("collision-shape-3d");

    /** Generated box-geometry resource type. */
    public static final RegisteredType BOX_GEOMETRY_3D = type("box-geometry-3d");

    /** Diffuse Lambert-material resource type. */
    public static final RegisteredType LAMBERT_MATERIAL_3D = type("lambert-material-3d");

    /** Box collision-shape resource type. */
    public static final RegisteredType BOX_SHAPE_3D = type("box-shape-3d");

    /** Sphere collision-shape resource type. */
    public static final RegisteredType SPHERE_SHAPE_3D = type("sphere-shape-3d");

    /** Capsule collision-shape resource type. */
    public static final RegisteredType CAPSULE_SHAPE_3D = type("capsule-shape-3d");

    /** Static indexed triangle-mesh collision-shape resource type. */
    public static final RegisteredType TRIANGLE_MESH_SHAPE_3D = type("triangle-mesh-shape-3d");

    /** Prevents construction of this registered-type namespace. */
    private Scene3dTypes() {
        throw new AssertionError("Scene3dTypes cannot be instantiated");
    }

    /** Creates one version-one registered type identity. */
    private static RegisteredType type(String id) {
        return new RegisteredType(PREFIX + id, 1);
    }
}
