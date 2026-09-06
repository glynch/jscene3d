/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.WorldModule;

/** Internal required-lookup failure converted to a component-local structured composition diagnostic. */
final class MissingWorldModuleException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    private final Class<? extends WorldModule> moduleType;

    /** Records the exact absent stable interface. */
    MissingWorldModuleException(Class<? extends WorldModule> moduleType) {
        super("required world module is not bound: " + moduleType.getName());
        this.moduleType = moduleType;
    }

    /** Returns the absent stable interface. */
    Class<? extends WorldModule> moduleType() {
        return moduleType;
    }
}
