/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocol;
import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocolVersion;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Process entry point for one native JScene3D Editor renderer session. */
public final class EditorRendererMain {
    private EditorRendererMain() {
        throw new AssertionError("EditorRendererMain cannot be instantiated");
    }

    /**
     * Acquires the Electron-owned IOSurface and serves renderer commands until shutdown or EOF.
     *
     * @param arguments optional protocol version, Electron bundle ID, width and height
     */
    public static void main(String[] arguments) {
        PrintWriter output = new PrintWriter(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        PrintWriter diagnostics =
                new PrintWriter(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);

        RendererConfiguration configuration;
        try {
            configuration = RendererConfiguration.from(arguments);
        } catch (IllegalArgumentException failure) {
            output.println(RendererProtocol.EVENT_PROTOCOL_VERSION + " " + RendererProtocolVersion.CURRENT);
            output.println(RendererProtocol.EVENT_ERROR + " MALFORMED_REQUEST startup-arguments");
            diagnostics.println("[JScene3D renderer] Invalid startup arguments: " + failure.getMessage());
            throw failure;
        }

        ValidationRendererSession rendererSession;
        try {
            rendererSession = ValidationRendererSession.create(configuration);
        } catch (RuntimeException | LinkageError failure) {
            output.println(RendererProtocol.EVENT_PROTOCOL_VERSION + " " + configuration.protocolVersion());
            output.println(RendererProtocol.EVENT_ERROR + " RUNTIME_FAILURE startup");
            diagnostics.println("[JScene3D renderer] Renderer startup failed: " + failure);
            throw failure;
        }

        try (ValidationRendererSession session = rendererSession;
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(new FileInputStream(FileDescriptor.in), StandardCharsets.UTF_8))) {
            RendererProtocolServer server =
                    new RendererProtocolServer(session, configuration.protocolVersion(), output, diagnostics);
            server.run(input);
        } catch (IOException failure) {
            output.println(RendererProtocol.EVENT_ERROR + " RUNTIME_FAILURE protocol-transport");
            diagnostics.println("[JScene3D renderer] Protocol transport failed: " + failure);
            throw new UncheckedIOException("Renderer protocol transport failed", failure);
        } catch (RuntimeException | LinkageError failure) {
            diagnostics.println("[JScene3D renderer] Renderer process failed: " + failure);
            throw failure;
        }
    }
}
