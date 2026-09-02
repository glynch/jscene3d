/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Resolves example resources to ordinary files, materializing archive entries when necessary. */
public final class BundledResources {
    private static final Map<String, Path> MATERIALIZED_RESOURCES = new HashMap<>();

    /** Prevents instantiation of this resource utility. */
    private BundledResources() {
        throw new AssertionError("BundledResources cannot be instantiated");
    }

    /**
     * Resolves one required resource to its build-time file path.
     *
     * @param resource resource resolved by its owning example module
     * @param resourceName absolute or owner-relative resource name
     * <p>File-backed development resources are returned directly. Resources inside a packaged
     * JAR are copied once to temporary storage and reused for the remainder of the process.
     *
     * @return resolved ordinary file path
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalStateException if the resource cannot be represented or materialized
     */
    public static Path path(URL resource, String resourceName) {
        URL validResource = Objects.requireNonNull(resource, resourceName);
        String validName = Objects.requireNonNull(resourceName, "resourceName");
        if (!"file".equalsIgnoreCase(validResource.getProtocol())) {
            return materialize(validResource, validName);
        }
        try {
            return Path.of(validResource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid bundled resource URI: " + validName, exception);
        }
    }

    /** Copies one archive-backed resource to an ordinary temporary file at most once. */
    private static Path materialize(URL resource, String resourceName) {
        String key = resource.toExternalForm();
        synchronized (MATERIALIZED_RESOURCES) {
            Path existing = MATERIALIZED_RESOURCES.get(key);
            if (existing != null) {
                return existing;
            }
            Path materialized = copyToTemporaryFile(resource, resourceName);
            MATERIALIZED_RESOURCES.put(key, materialized);
            return materialized;
        }
    }

    /** Copies one resource while retaining its filename for extension-sensitive loaders. */
    private static Path copyToTemporaryFile(URL resource, String resourceName) {
        String fileName = resourceFileName(resourceName);
        try {
            Path directory = Files.createTempDirectory("jscene3d-examples-");
            Path destination = directory.resolve(fileName);
            directory.toFile().deleteOnExit();
            destination.toFile().deleteOnExit();
            try (InputStream input = resource.openStream()) {
                Files.copy(input, destination);
            }
            return destination;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to materialize bundled resource: " + resourceName, exception);
        }
    }

    /** Returns the trailing resource name without interpreting it as a platform path. */
    private static String resourceFileName(String resourceName) {
        int separator = resourceName.lastIndexOf('/');
        String fileName = resourceName.substring(separator + 1);
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("Resource name must identify a file: " + resourceName);
        }
        return fileName;
    }
}
