/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleBrowserHost;

/** Opens the searchable browser containing every JScene3D audio example. */
public final class AudioExampleBrowser {
    /** Prevents instantiation of this browser entry point. */
    private AudioExampleBrowser() {
        throw new AssertionError("AudioExampleBrowser cannot be instantiated");
    }

    /**
     * Opens the audio example browser until the native window is closed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleBrowserHost.launch(ExampleCatalog.suite());
    }
}
