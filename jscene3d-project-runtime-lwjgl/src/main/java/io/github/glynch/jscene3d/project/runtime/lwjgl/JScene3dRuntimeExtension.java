/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl;

import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.runtime.lwjgl.internal.LwjglScene3dRenderHost;
import io.github.glynch.jscene3d.project.runtime.lwjgl.internal.Scene3dComposition;
import io.github.glynch.jscene3d.project.runtime.lwjgl.internal.Scene3dRenderHost;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.Objects;

/** Single-runtime built-in extension for declarative JScene3D scene and resource types. */
public final class JScene3dRuntimeExtension implements ProjectRuntimeExtension {
    /** Stable extension identity used by project manifests and registered type identifiers. */
    public static final String EXTENSION_ID = "io.github.glynch.jscene3d";

    private final Scene3dComposition composition;
    private boolean registered;

    /**
     * Creates an extension bound to one graphical host.
     *
     * <p>Each project runtime requires its own extension instance. The caller retains ownership of
     * the window and renderer.
     *
     * @param window open window providing the drawable dimensions
     * @param renderer open renderer used by the project runtime
     */
    public JScene3dRuntimeExtension(Window window, Renderer renderer) {
        this(new LwjglScene3dRenderHost(window, renderer));
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

    /** Creates an extension around one internal render-host adapter. */
    JScene3dRuntimeExtension(Scene3dRenderHost host) {
        composition = new Scene3dComposition(Objects.requireNonNull(host, "host"));
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
