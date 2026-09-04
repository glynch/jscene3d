/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

/** Trusted executable contribution discovered from an extension artifact. */
public interface ProjectRuntimeExtension {
    /**
     * Returns the extension descriptor identifier implemented by this provider.
     *
     * @return stable extension identifier
     */
    String id();

    /**
     * Registers executable factories without creating runtime objects.
     *
     * @param registry bounded registration scope
     */
    void register(ProjectRuntimeRegistry registry);
}
