/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleBrowserHost;

/** Opens the searchable rendering and asset-loading example suite. */
public final class ExampleBrowser {
    /** Prevents instantiation of this entry point. */
    private ExampleBrowser() {
        throw new AssertionError("ExampleBrowser cannot be instantiated");
    }

    /**
     * Runs the complete rendering and asset-loading suite.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleBrowserHost.launch(ExampleCatalog.suite());
    }
}
