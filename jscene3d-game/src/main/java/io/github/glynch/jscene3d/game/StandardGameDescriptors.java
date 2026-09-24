/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.standard.StandardProjectDescriptors;
import java.util.List;

/** Standard descriptor set understood by JScene3D game hosts and authoring tools. */
public final class StandardGameDescriptors {
    private static final List<ExtensionDescriptor> ALL = StandardProjectDescriptors.all();

    private StandardGameDescriptors() {
        throw new AssertionError("StandardGameDescriptors cannot be instantiated");
    }

    /**
     * Returns the immutable standard descriptor set in dependency order.
     *
     * <p>The returned metadata is safe for authoring tools which validate or preview project content without executing
     * the corresponding runtime implementations.
     *
     * @return spatial, physics, game-3D, and game-presentation descriptors
     */
    public static List<ExtensionDescriptor> all() {
        return ALL;
    }
}
