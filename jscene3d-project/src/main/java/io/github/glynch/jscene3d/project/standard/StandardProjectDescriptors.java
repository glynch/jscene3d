/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.standard;

import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.standard.application.StandardGameApplicationDescriptors;
import io.github.glynch.jscene3d.project.standard.game3d.StandardGame3dDescriptors;
import io.github.glynch.jscene3d.project.standard.physics3d.StandardPhysics3dDescriptors;
import io.github.glynch.jscene3d.project.standard.presentation.StandardGamePresentationDescriptors;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import java.util.List;

/** Safe built-in descriptor catalog shared by project tooling and runtime hosts. */
public final class StandardProjectDescriptors {
    private static final List<ExtensionDescriptor> ALL = List.of(
            StandardGameApplicationDescriptors.extensionDescriptor(),
            StandardSpatial3dDescriptors.extensionDescriptor(),
            StandardPhysics3dDescriptors.extensionDescriptor(),
            StandardGame3dDescriptors.extensionDescriptor(),
            StandardGamePresentationDescriptors.extensionDescriptor());

    private StandardProjectDescriptors() {
        throw new AssertionError("StandardProjectDescriptors cannot be instantiated");
    }

    /**
     * Returns the immutable built-in catalog in dependency order.
     *
     * @return safe built-in extension descriptors
     */
    public static List<ExtensionDescriptor> all() {
        return ALL;
    }
}
