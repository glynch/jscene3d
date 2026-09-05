/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d;

import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.runtime.scene3d.internal.Scene3dComposition;
import java.util.Objects;

/** Single-runtime built-in extension for declarative JScene3D scene and resource types. */
public final class JScene3dRuntimeExtension implements ProjectRuntimeExtension {
    /** Stable extension identity used by project manifests and registered type identifiers. */
    public static final String EXTENSION_ID = "io.github.glynch.jscene3d";

    private final Scene3dComposition composition;
    private final PhysicsWorld physicsWorld;
    private boolean registered;

    /**
     * Creates an extension bound to one render host.
     *
     * <p>Each project runtime requires its own extension instance. The caller retains ownership of
     * the host and any render resources behind it.
     *
     * @param host render submission destination
     */
    public JScene3dRuntimeExtension(Scene3dRenderHost host) {
        this(host, new PhysicsWorld());
    }

    /**
     * Creates an extension that contributes declarative collision objects to an existing world.
     *
     * <p>The extension removes only the objects it creates; ownership of the supplied world remains
     * with the caller.
     *
     * @param host render submission destination
     * @param physicsWorld physics world shared with application-authored runtime code
     */
    public JScene3dRuntimeExtension(Scene3dRenderHost host, PhysicsWorld physicsWorld) {
        this.physicsWorld = Objects.requireNonNull(physicsWorld, "physicsWorld");
        composition = new Scene3dComposition(Objects.requireNonNull(host, "host"), physicsWorld);
    }

    /**
     * Creates a built-in 3d extension that composes scene objects without submitting frames.
     *
     * @return single-use headless extension instance
     */
    public static JScene3dRuntimeExtension headless() {
        return new JScene3dRuntimeExtension((scene, camera) -> {
            // Headless composition deliberately has no render target.
        });
    }

    /**
     * Returns the physics world populated by declarative collision nodes.
     *
     * @return retained physics world
     */
    public PhysicsWorld physicsWorld() {
        return physicsWorld;
    }

    @Override
    public String id() {
        return EXTENSION_ID;
    }

    @Override
    public void register(ProjectRuntimeRegistry registry) {
        if (registered) {
            throw new IllegalStateException("JScene3dRuntimeExtension instances are single-use");
        }
        registered = true;
        composition.register(Objects.requireNonNull(registry, "registry"));
    }
}
