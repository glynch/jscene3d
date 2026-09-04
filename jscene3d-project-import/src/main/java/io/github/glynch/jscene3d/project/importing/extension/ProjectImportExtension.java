/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.extension;

/** Trusted executable import contribution discovered from an extension artifact. */
public interface ProjectImportExtension {
    /**
     * Returns the safe extension descriptor identifier implemented by this provider.
     *
     * @return stable extension identifier
     */
    String id();

    /**
     * Registers source importers without inspecting or importing source content.
     *
     * @param registry bounded registration scope
     */
    void register(ProjectImportRegistry registry);
}
