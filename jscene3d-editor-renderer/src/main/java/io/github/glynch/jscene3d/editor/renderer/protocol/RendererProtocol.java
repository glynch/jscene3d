/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.protocol;

/** Stable command and event vocabulary for one editor-renderer process. */
public final class RendererProtocol {
    /** Protocol-version event prefix. */
    public static final String EVENT_PROTOCOL_VERSION = "PROTOCOL_VERSION";

    /** Renderer-ready event. */
    public static final String EVENT_RENDERER_READY = "RENDERER_READY";

    /** Completed-frame event. */
    public static final String EVENT_FRAME_READY = "FRAME_READY";

    /** Replacement-surface event prefix. */
    public static final String EVENT_SURFACE_READY = "SURFACE_READY";

    /** Pause acknowledgement. */
    public static final String EVENT_PAUSED = "PAUSED";

    /** Resume acknowledgement. */
    public static final String EVENT_RESUMED = "RESUMED";

    /** Shutdown acknowledgement. */
    public static final String EVENT_SHUTDOWN_READY = "SHUTDOWN_READY";

    /** Structured error prefix. */
    public static final String EVENT_ERROR = "ERROR";

    /** Frame command. */
    public static final String COMMAND_FRAME = "FRAME";

    /** Surface-replacement command. */
    public static final String COMMAND_RECEIVE_SURFACE = "RECEIVE_SURFACE";

    /** Pause command. */
    public static final String COMMAND_PAUSE = "PAUSE";

    /** Resume command. */
    public static final String COMMAND_RESUME = "RESUME";

    /** Product shutdown command. */
    public static final String COMMAND_SHUTDOWN = "SHUTDOWN";

    /** Shutdown command retained for the Stage 1 native session boundary. */
    public static final String COMMAND_QUIT = "QUIT";

    /** Validation-scene drag command retained for Stage 1 compatibility. */
    public static final String COMMAND_DRAG = "DRAG";

    private RendererProtocol() {
        throw new AssertionError("RendererProtocol cannot be instantiated");
    }
}
