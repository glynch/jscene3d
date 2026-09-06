/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import java.util.Objects;

/**
 * Explicit host binding from one stable world-module interface to its world-scoped adapter.
 *
 * <p>The interface type, rather than the adapter's implementation class, is the lookup identity. Supplying this value
 * to composition does not itself transfer ownership. A successfully composed world takes ownership; after failed
 * composition the caller retains it.
 *
 * @param <T> stable world-module interface
 */
public final class WorldModuleBinding<T extends WorldModule> {
    private final Class<T> type;
    private final T module;

    /** Validates one exact stable-interface binding. */
    private WorldModuleBinding(Class<T> type, T module) {
        this.type = Objects.requireNonNull(type, "type");
        this.module = Objects.requireNonNull(module, "module");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("world module type must be an interface: " + type.getName());
        }
        if (!type.isInstance(module)) {
            throw new IllegalArgumentException("world module does not implement " + type.getName());
        }
    }

    /**
     * Binds one world-scoped adapter to the stable interface callers use to access it.
     *
     * @param type exact stable module interface
     * @param module world-scoped adapter
     * @param <T> stable module interface
     * @return validated binding
     */
    public static <T extends WorldModule> WorldModuleBinding<T> of(Class<T> type, T module) {
        return new WorldModuleBinding<>(type, module);
    }

    /**
     * Returns the exact stable lookup interface.
     *
     * @return module interface
     */
    public Class<T> type() {
        return type;
    }

    /**
     * Returns the world-scoped adapter transferred after successful composition.
     *
     * @return bound adapter
     */
    public T module() {
        return module;
    }
}
