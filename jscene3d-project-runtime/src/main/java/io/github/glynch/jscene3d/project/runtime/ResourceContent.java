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
}
