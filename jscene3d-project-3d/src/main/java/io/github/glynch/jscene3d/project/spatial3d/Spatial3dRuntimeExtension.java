/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.util.Objects;

/** Executable Transform3d factory corresponding exactly to {@link Spatial3dDescriptors}. */
public final class Spatial3dRuntimeExtension implements ComponentRuntimeExtension {
    /** Creates the stateless built-in runtime contribution. */
    public Spatial3dRuntimeExtension() {
        // Explicit construction supports ordinary runtime-extension registration.
    }

    @Override
    public String id() {
        return Spatial3dDescriptors.extensionId();
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        Objects.requireNonNull(registry, "registry").register(Spatial3dDescriptors.transformType(), context -> {
            AuthoredTransform3d authored = AuthoredTransform3d.from(context.properties());
            Spatial3dWorldModule spatial = context.world().requireModule(Spatial3dWorldModule.class);
            return spatial.createTransform(
                    context.owner(), authored.position(), authored.orientation(), authored.scale());
        });
    }
}
