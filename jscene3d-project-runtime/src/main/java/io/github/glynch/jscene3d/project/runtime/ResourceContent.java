/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.io.InputStream;

/** Opens immutable project, source-asset, or imported payload content for a runtime resource loader. */
public interface ResourceContent {
    /**
     * Opens a new caller-owned stream for one portable content reference.
     *
     * @param reference project content reference
     * @return new stream which the caller must close
     * @throws IOException when the referenced content cannot be opened
     * @throws IllegalArgumentException when the reference is unknown or does not identify readable content
     */
    InputStream openPayload(ResourceReference reference) throws IOException;

    /**
     * Acquires one nested runtime resource needed by the resource currently being loaded.
     *
     * <p>The caller owns and must close the returned lease. Resource providers which do not support nested resources
     * retain the default failure, allowing payload-only test adapters to remain functional interfaces.
     *
     * @param <T> required runtime value type
     * @param reference portable resource reference
     * @param valueType required runtime Java type
     * @return independently releasable lease retaining the nested resource
     * @throws UnsupportedOperationException when this content adapter supports payloads only
     */
    default <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
        throw new UnsupportedOperationException("nested runtime resources are not supported");
    }
}
