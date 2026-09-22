/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.iosurface.macos.codeoss;

/** Existing line protocol between one Electron native session and its Java child. */
final class RendererProtocol {
    static final String COMMAND_DRAG = "DRAG";
    static final String COMMAND_FRAME = "FRAME";
    static final String COMMAND_RECEIVE_SURFACE = "RECEIVE_SURFACE";
    static final String COMMAND_QUIT = "QUIT";
    static final String EVENT_RENDERER_READY = "RENDERER_READY";
    static final String EVENT_FRAME_READY = "FRAME_READY";
    static final String EVENT_SURFACE_READY = "SURFACE_READY";

    private RendererProtocol() {
        throw new AssertionError("RendererProtocol cannot be instantiated");
    }
}
