/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.process;

import io.github.glynch.jscene3d.editor.authoring.protocol.internal.AuthoringProtocolServer;
import io.github.glynch.jscene3d.editor.authoring.service.AuthoringProjectService;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Process entry point for the persistent headless authoring service. */
public final class AuthoringServiceMain {
    private static final System.Logger LOGGER = System.getLogger(AuthoringServiceMain.class.getName());

    private AuthoringServiceMain() {}

    /**
     * Serves the owning stdio connection until shutdown, EOF, or transport failure.
     *
     * @param arguments unused process arguments
     */
    public static void main(String[] arguments) {
        AuthoringServiceBuildInfo build = AuthoringServiceBuildInfo.current();
        EditorProjectLoader loader =
                new EditorProjectLoader(build.engineVersion(), AuthoringServiceMain.class.getClassLoader());
        AuthoringProjectService service = new AuthoringProjectService(loader);
        AuthoringProtocolServer server = new AuthoringProtocolServer(
                service,
                build.serviceVersion(),
                build.engineVersion(),
                UUID.randomUUID().toString());
        try {
            server.run(new FileInputStream(FileDescriptor.in), new FileOutputStream(FileDescriptor.out));
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Authoring protocol transport failed", exception);
            throw new UncheckedIOException("Authoring protocol transport failed", exception);
        }
    }
}
