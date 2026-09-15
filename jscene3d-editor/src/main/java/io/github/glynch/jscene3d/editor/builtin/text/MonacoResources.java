/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jspecify.annotations.Nullable;

/** Stages the pinned Monaco WebJar as ordinary files that JavaFX WebKit can load. */
final class MonacoResources {
    static final String VERSION = configuredVersion();
    private static final String CONFIGURATION = "monaco.properties";
    private static final String EDITOR_PAGE = "monaco-editor.html";
    private static final String WEBJAR_ROOT = "META-INF/resources/webjars/monaco-editor/" + VERSION + "/min/vs/";
    private static @Nullable Distribution distribution;

    private MonacoResources() {}

    static URL loader() {
        return distribution().loader();
    }

    static URL editorPage() {
        return distribution().editorPage();
    }

    static String baseUrl() {
        return distribution().root().toUri().toString();
    }

    static synchronized void close() {
        Distribution current = distribution;
        distribution = null;
        if (current != null) {
            closeWorkspace(current.workspace());
        }
    }

    private static synchronized Distribution distribution() {
        Distribution current = distribution;
        if (current == null) {
            current = prepareDistribution();
            distribution = current;
        }
        return current;
    }

    private static Distribution prepareDistribution() {
        URL packagedLoader = MonacoResources.class.getClassLoader().getResource(WEBJAR_ROOT + "loader.js");
        if (packagedLoader == null) {
            throw new IllegalStateException("Bundled Monaco loader was not found");
        }
        if (!"jar".equals(packagedLoader.getProtocol())) {
            throw new IllegalStateException("Bundled Monaco distribution is not packaged in a WebJar");
        }
        TemporaryWorkspace workspace = null;
        try {
            workspace = TemporaryWorkspace.create("jscene3d-monaco-" + VERSION + "-");
            Path root = workspace.root();
            extractWebJar(packagedLoader, root);
            copyEditorPage(root);
            return new Distribution(
                    workspace,
                    root.resolve("loader.js").toUri().toURL(),
                    root.resolve(EDITOR_PAGE).toUri().toURL());
        } catch (IOException exception) {
            closeWorkspace(workspace);
            throw new IllegalStateException("Bundled Monaco distribution could not be prepared", exception);
        }
    }

    private static void extractWebJar(URL packagedLoader, Path root) throws IOException {
        JarURLConnection connection = (JarURLConnection) packagedLoader.openConnection();
        connection.setUseCaches(false);
        try (JarFile archive = connection.getJarFile()) {
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(WEBJAR_ROOT)) {
                    continue;
                }
                Path destination = root.resolve(entry.getName().substring(WEBJAR_ROOT.length()))
                        .normalize();
                if (!destination.startsWith(root)) {
                    throw new IOException("Invalid Monaco WebJar entry: " + entry.getName());
                }
                Files.createDirectories(destination.getParent());
                try (InputStream input = archive.getInputStream(entry)) {
                    Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void copyEditorPage(Path root) throws IOException {
        try (InputStream input = MonacoResources.class.getResourceAsStream(EDITOR_PAGE)) {
            if (input == null) {
                throw new IOException("Monaco editor page was not found");
            }
            Files.copy(input, root.resolve(EDITOR_PAGE), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String configuredVersion() {
        Properties properties = new Properties();
        try (InputStream input = MonacoResources.class.getResourceAsStream(CONFIGURATION)) {
            if (input == null) {
                throw new IllegalStateException("Monaco resource configuration was not found");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Monaco resource configuration could not be read", exception);
        }
        return properties.getProperty("version");
    }

    private static void closeWorkspace(@Nullable TemporaryWorkspace workspace) {
        if (workspace == null) {
            return;
        }
        try {
            workspace.close();
        } catch (UncheckedIOException ignored) {
            // Best-effort cleanup of a process-private temporary directory.
        }
    }

    private record Distribution(TemporaryWorkspace workspace, URL loader, URL editorPage) {
        private Path root() {
            return workspace.root();
        }
    }
}
