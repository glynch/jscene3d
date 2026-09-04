/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;

/** Shared registered-type identities owned by the Doom content extension. */
public final class DoomTypes {
    /** Extension descriptor and executable-provider identity. */
    public static final String EXTENSION_IDENTIFIER = "io.github.glynch.jscene3d.doom";

    /** Current version of the Doom extension's registered types. */
    public static final int TYPE_VERSION = 1;

    /** Portable classic-map resource type identity. */
    public static final String MAP_RESOURCE_IDENTIFIER = EXTENSION_IDENTIFIER + "/map";

    /** Classic-map source importer type. */
    public static final RegisteredType MAP_IMPORTER = new RegisteredType(EXTENSION_IDENTIFIER + "/maps", TYPE_VERSION);

    /** Portable classic-map resource type. */
    public static final RegisteredType MAP_RESOURCE = new RegisteredType(MAP_RESOURCE_IDENTIFIER, TYPE_VERSION);

    /** Prevents construction of this type-identity namespace. */
    private DoomTypes() {
        throw new AssertionError("DoomTypes cannot be instantiated");
    }
}
