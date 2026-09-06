/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import java.util.Objects;

/** Identifies a world module which failed while advancing one fixed simulation step. */
public final class WorldModuleUpdateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Exact host binding interface for the failed module. */
    private final Class<? extends WorldModule> moduleType;

    /** Zero-based fixed tick which failed. */
    private final long tick;

    /**
     * Creates one module-attributed update failure.
     *
     * @param moduleType exact host binding interface
     * @param tick fixed tick which failed
     * @param cause module failure
     */
    public WorldModuleUpdateException(Class<? extends WorldModule> moduleType, long tick, RuntimeException cause) {
        super(
                "World module "
                        + Objects.requireNonNull(moduleType, "moduleType").getName() + " failed during fixed tick "
                        + tick,
                Objects.requireNonNull(cause, "cause"));
        this.moduleType = moduleType;
        this.tick = tick;
    }

    /**
     * Returns the exact interface used to bind the failed module.
     *
     * @return module binding interface
     */
    public Class<? extends WorldModule> moduleType() {
        return moduleType;
    }

    /**
     * Returns the fixed tick which failed.
     *
     * @return zero-based tick
     */
    public long tick() {
        return tick;
    }
}
