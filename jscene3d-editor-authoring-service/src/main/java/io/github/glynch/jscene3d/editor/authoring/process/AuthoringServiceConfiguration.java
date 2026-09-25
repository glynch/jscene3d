/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.process;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable process configuration parsed from authoring-service command-line arguments. */
final class AuthoringServiceConfiguration {
    private static final String EXTENSION_METADATA_PREFIX = "--extension-metadata=";

    private final List<Path> installedExtensionMetadata;

    /** Stores an ordered snapshot of installed descriptor-only extension artifacts. */
    private AuthoringServiceConfiguration(List<Path> installedExtensionMetadata) {
        this.installedExtensionMetadata = List.copyOf(installedExtensionMetadata);
    }

    /**
     * Parses zero or more repeatable installed-extension metadata arguments.
     *
     * @param arguments process arguments
     * @return validated authoring-service configuration
     */
    static AuthoringServiceConfiguration from(String[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        List<Path> installedExtensionMetadata = new ArrayList<>();
        for (String argument : arguments) {
            String candidate = Objects.requireNonNull(argument, "argument");
            if (!candidate.startsWith(EXTENSION_METADATA_PREFIX)) {
                throw new IllegalArgumentException("Unsupported authoring service argument: " + candidate);
            }
            String configuredPath = candidate.substring(EXTENSION_METADATA_PREFIX.length());
            if (configuredPath.isBlank()) {
                throw new IllegalArgumentException("Authoring service extension metadata path must not be blank");
            }
            installedExtensionMetadata.add(
                    Path.of(configuredPath).toAbsolutePath().normalize());
        }
        return new AuthoringServiceConfiguration(installedExtensionMetadata);
    }

    /** Returns installed descriptor-only extension artifacts in configured order. */
    List<Path> installedExtensionMetadata() {
        return installedExtensionMetadata;
    }
}
