/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.value.ResourceReference;

/**
 * Host seam which acquires shared immutable runtime resources for composed worlds.
 *
 * <p>The provider remains owned by its caller. Each successful acquisition returns a distinct lease handle; providers
 * may share the underlying immutable value across leases and worlds. Acquisition is synchronous and may resolve nested
 * resources. Implementations are responsible for any thread-affinity requirements of acquisition and lease release.
 */
public interface RuntimeResourceProvider {
    /**
     * Acquires one retained runtime value of the required Java type.
     *
     * @param <T> required runtime value type
     * @param reference portable project-resource reference
     * @param valueType required runtime Java type
     * @return a new open lease retaining the shared value
     * @throws IllegalArgumentException if an argument is invalid
     * @throws IllegalStateException if acquisition fails or the value has the wrong type
     */
    <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType);
}
