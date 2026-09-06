/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/**
 * World-scoped runtime facility supplied by the host through a stable interface.
 *
 * <p>Rendering, physics, audio, input, and similar facilities define focused public interfaces extending this type.
 * Their adapters remain independent of the entity model. A successfully composed {@link World} owns each bound
 * module and closes it after releasing every entity component. Implementations must make repeated closure harmless.
 */
public interface WorldModule extends AutoCloseable {
    /** Releases this world's participation in the underlying facility. */
    @Override
    void close();
}
