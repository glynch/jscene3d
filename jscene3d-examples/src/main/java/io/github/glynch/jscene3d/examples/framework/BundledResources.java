/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves file-backed resources bundled with the executable examples. */
public final class BundledResources {
    /** Prevents instantiation of this resource utility. */
    private BundledResources() {
        throw new AssertionError("BundledResources cannot be instantiated");
    }

    /**
     * Resolves one required resource to its build-time file path.
     *
     * @param owner class whose loader resolves the resource
     * @param resourceName absolute or owner-relative resource name
     * @return resolved file path
     * @throws NullPointerException if either argument is {@code null}, or the resource is absent
     * @throws IllegalStateException if the resource URI cannot be represented as a file path
     */
    public static Path path(Class<?> owner, String resourceName) {
        Class<?> validOwner = Objects.requireNonNull(owner, "owner");
        String validName = Objects.requireNonNull(resourceName, "resourceName");
        URL resource = Objects.requireNonNull(validOwner.getResource(validName), validName);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid bundled resource URI: " + validName, exception);
        }
    }
}
