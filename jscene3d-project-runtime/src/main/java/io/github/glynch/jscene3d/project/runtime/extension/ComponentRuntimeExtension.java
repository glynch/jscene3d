/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

/** Trusted executable component-factory contribution from an extension artifact. */
public interface ComponentRuntimeExtension {
    /**
     * Returns the extension descriptor identifier implemented by this provider.
     *
     * @return stable extension identifier
     */
    String id();

    /**
     * Registers executable component factories without creating runtime values.
     *
     * @param registry bounded component-factory registration scope
     */
    void register(ComponentFactoryRegistry registry);
}
