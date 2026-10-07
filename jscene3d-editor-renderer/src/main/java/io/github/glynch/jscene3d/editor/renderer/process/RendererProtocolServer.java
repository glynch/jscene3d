/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocol;
import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocolVersion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/** Drives a renderer session from the versioned line protocol on standard I/O. */
final class RendererProtocolServer {
    private final RendererSession session;
    private final RendererProtocolVersion version;
    private final PrintWriter output;
    private final PrintWriter diagnostics;
    private boolean paused;

    RendererProtocolServer(
            RendererSession session, RendererProtocolVersion version, PrintWriter output, PrintWriter diagnostics) {
        this.session = session;
        this.version = version;
        this.output = output;
        this.diagnostics = diagnostics;
    }

    void run(BufferedReader input) throws IOException {
        send(RendererProtocol.EVENT_PROTOCOL_VERSION + " " + version);
        send(RendererProtocol.EVENT_RENDERER_READY);
        String line;
        while ((line = input.readLine()) != null) {
            RendererCommand command;
            try {
                command = RendererCommand.parse(line);
            } catch (RendererProtocolException exception) {
                diagnostics.println("[JScene3D renderer] Rejected command: " + exception.getMessage());
                send(RendererProtocol.EVENT_ERROR + " " + exception.category() + " command");
                continue;
            }
            if (!execute(command)) {
                return;
            }
        }
        diagnostics.println("[JScene3D renderer] Protocol input closed");
    }

    private boolean execute(RendererCommand command) {
        try {
            return switch (command) {
                case RendererCommand.Frame ignored -> executeFrame();
                case RendererCommand.Drag drag -> executeDrag(drag);
                case RendererCommand.SceneSnapshot snapshot -> executeSceneSnapshot(snapshot);
                case RendererCommand.ScenePick pick -> executeScenePick(pick);
                case RendererCommand.SceneSelect select -> executeSceneSelect(select);
                case RendererCommand.ReceiveSurface ignored -> executeSurfaceReplacement();
                case RendererCommand.Pause ignored -> executePause();
                case RendererCommand.Resume ignored -> executeResume();
                case RendererCommand.Shutdown ignored -> executeShutdown();
                case RendererCommand.Quit ignored -> false;
            };
        } catch (RuntimeException | LinkageError failure) {
            String operation = operationName(command);
            send(RendererProtocol.EVENT_ERROR + " RUNTIME_FAILURE " + operation);
            diagnostics.println("[JScene3D renderer] " + operation + " failed: " + failure);
            throw failure;
        }
    }

    private boolean executeFrame() {
        if (paused) {
            send(RendererProtocol.EVENT_ERROR + " INVALID_STATE frame-paused");
            return true;
        }
        session.renderFrame();
        send(RendererProtocol.EVENT_FRAME_READY);
        return true;
    }

    private boolean executeDrag(RendererCommand.Drag drag) {
        session.applyValidationDrag(drag.horizontal(), drag.vertical());
        return true;
    }

    private boolean executeSceneSnapshot(RendererCommand.SceneSnapshot snapshot) {
        session.replaceSceneViewSnapshot(SceneViewSnapshotCodec.decode(snapshot.encodedSnapshot()));
        return true;
    }

    private boolean executeScenePick(RendererCommand.ScenePick pick) {
        SceneViewSelectionResult result = session.pickSceneView(pick.revision(), pick.horizontal(), pick.vertical());
        String selection =
                switch (result.status()) {
                    case SELECTED ->
                        SceneViewOccurrenceCodec.encode(result.occurrence().orElseThrow());
                    case CLEARED -> "NONE";
                    case STALE -> "STALE";
                };
        send(RendererProtocol.EVENT_SCENE_SELECTION + " " + pick.requestToken() + " " + result.revision() + " "
                + selection);
        return true;
    }

    private boolean executeSceneSelect(RendererCommand.SceneSelect select) {
        session.selectSceneView(select.revision(), select.occurrence());
        return true;
    }

    private boolean executeSurfaceReplacement() {
        RendererSession.SurfaceSize size = session.receiveReplacementSurface();
        send(RendererProtocol.EVENT_SURFACE_READY + " " + size.width() + " " + size.height());
        return true;
    }

    private boolean executePause() {
        if (paused) {
            send(RendererProtocol.EVENT_ERROR + " INVALID_STATE already-paused");
            return true;
        }
        paused = true;
        send(RendererProtocol.EVENT_PAUSED);
        return true;
    }

    private boolean executeResume() {
        if (!paused) {
            send(RendererProtocol.EVENT_ERROR + " INVALID_STATE not-paused");
            return true;
        }
        paused = false;
        send(RendererProtocol.EVENT_RESUMED);
        return true;
    }

    private boolean executeShutdown() {
        send(RendererProtocol.EVENT_SHUTDOWN_READY);
        return false;
    }

    private static String operationName(RendererCommand command) {
        return switch (command) {
            case RendererCommand.Frame ignored -> "frame";
            case RendererCommand.Drag ignored -> "drag";
            case RendererCommand.SceneSnapshot ignored -> "scene-snapshot";
            case RendererCommand.ScenePick ignored -> "scene-pick";
            case RendererCommand.SceneSelect ignored -> "scene-select";
            case RendererCommand.ReceiveSurface ignored -> "surface-replacement";
            case RendererCommand.Pause ignored -> "pause";
            case RendererCommand.Resume ignored -> "resume";
            case RendererCommand.Shutdown ignored -> "shutdown";
            case RendererCommand.Quit ignored -> "shutdown";
        };
    }

    private void send(String event) {
        output.println(event);
        if (output.checkError()) {
            throw new IllegalStateException("Renderer protocol output failed");
        }
    }
}
