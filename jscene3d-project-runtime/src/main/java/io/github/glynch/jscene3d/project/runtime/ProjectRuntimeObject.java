/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** One executable object owned by a composed project runtime. */
public interface ProjectRuntimeObject extends AutoCloseable {
    /** Starts the fully connected object. */
    void start();

    /** Releases owned resources; implementations must be idempotent. */
    @Override
    void close();
}
