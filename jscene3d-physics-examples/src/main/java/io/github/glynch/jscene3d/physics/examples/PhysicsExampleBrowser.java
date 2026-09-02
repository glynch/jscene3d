/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleBrowserHost;

/** Opens the searchable visual physics example suite. */
public final class PhysicsExampleBrowser {
    /** Prevents instantiation of this entry point. */
    private PhysicsExampleBrowser() {
        throw new AssertionError("PhysicsExampleBrowser cannot be instantiated");
    }

    /**
     * Runs the complete physics suite.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleBrowserHost.launch(ExampleCatalog.suite());
    }
}
