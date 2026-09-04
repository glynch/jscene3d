/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import io.github.glynch.jscene3d.doom.internal.DoomTypes;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import java.util.Objects;

/** Creates typed runtime values from native resources owned by the Doom extension. */
public final class DoomRuntimeExtension implements ProjectRuntimeExtension {
    /** Creates the stateless service-discovered Doom runtime extension. */
    public DoomRuntimeExtension() {
        // Public construction is required by ServiceLoader on the class path.
    }

    @Override
    public String id() {
        return DoomTypes.EXTENSION_IDENTIFIER;
    }

    @Override
    public void register(ProjectRuntimeRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .registerResource(DoomTypes.MAP_RESOURCE, DoomMapResourceDecoder::decode);
    }
}
