/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Stages the pinned Monaco WebJar as ordinary files that JavaFX WebKit can load. */
final class MonacoResources {
    static final String VERSION = configuredVersion();
    private static final String CONFIGURATION = "monaco.properties";
    private static final String EDITOR_PAGE = "monaco-editor.html";
    private static final String WEBJAR_ROOT = "META-INF/resources/webjars/monaco-editor/" + VERSION + "/min/vs/";
    private static final Distribution DISTRIBUTION = prepareDistribution();

    private MonacoResources() {}

    static URL loader() {
        return DISTRIBUTION.loader();
    }

    static URL editorPage() {
        return DISTRIBUTION.editorPage();
    }

    static String baseUrl() {
        return DISTRIBUTION.root().toUri().toString();
    }

    private static Distribution prepareDistribution() {
        URL packagedLoader = MonacoResources.class.getClassLoader().getResource(WEBJAR_ROOT + "loader.js");
        if (packagedLoader == null) {
            throw new IllegalStateException("Bundled Monaco loader was not found");
        }
        if (!"jar".equals(packagedLoader.getProtocol())) {
            throw new IllegalStateException("Bundled Monaco distribution is not packaged in a WebJar");
        }
        Path root = null;
        try {
            root = Files.createTempDirectory("jscene3d-monaco-" + VERSION + "-");
            extractWebJar(packagedLoader, root);
            copyEditorPage(root);
            registerCleanup(root);
            return new Distribution(
                    root,
                    root.resolve("loader.js").toUri().toURL(),
                    root.resolve(EDITOR_PAGE).toUri().toURL());
        } catch (IOException exception) {
            deleteTree(root);
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

    private static void registerCleanup(Path root) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteTree(root), "jscene3d-monaco-cleanup"));
    }

    private static void deleteTree(Path root) {
        if (root == null) {
            return;
        }
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                    if (failure != null) {
                        throw failure;
                    }
                    Files.deleteIfExists(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup of a process-private temporary directory.
        }
    }

    private record Distribution(Path root, URL loader, URL editorPage) {}
}
