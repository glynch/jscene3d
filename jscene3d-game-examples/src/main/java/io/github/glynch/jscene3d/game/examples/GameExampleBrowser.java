/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleBrowserHost;

/** Opens the searchable visual game-runtime example suite. */
public final class GameExampleBrowser {
    /** Prevents instantiation of this entry point. */
    private GameExampleBrowser() {
        throw new AssertionError("GameExampleBrowser cannot be instantiated");
    }

    /**
     * Runs the complete game-runtime suite.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleBrowserHost.launch(ExampleCatalog.suite());
    }
}
