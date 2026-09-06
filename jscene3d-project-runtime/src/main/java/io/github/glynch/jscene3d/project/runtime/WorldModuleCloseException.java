/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import java.util.Objects;

/** Identifies a world module whose adapter failed while the world continued reverse-order cleanup. */
public final class WorldModuleCloseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Registered stable interface whose adapter failed. */
    private final Class<? extends WorldModule> moduleType;

    /**
     * Wraps one adapter cleanup failure with its registered stable interface.
     *
     * @param moduleType registered stable module interface
     * @param cause adapter failure
     */
    public WorldModuleCloseException(Class<? extends WorldModule> moduleType, RuntimeException cause) {
        super(
                "world module cleanup failed: "
                        + Objects.requireNonNull(moduleType, "moduleType").getName(),
                cause);
        this.moduleType = moduleType;
    }

    /**
     * Returns the registered stable interface whose adapter failed.
     *
     * @return module interface
     */
    public Class<? extends WorldModule> moduleType() {
        return moduleType;
    }
}
