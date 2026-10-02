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
    private static final String RUNTIME_ARTIFACT_PREFIX = "--runtime-artifact=";

    private final List<Path> installedExtensionMetadata;
    private final List<Path> runtimeArtifacts;

    /** Stores an ordered snapshot of installed descriptor-only extension artifacts. */
    private AuthoringServiceConfiguration(List<Path> installedExtensionMetadata, List<Path> runtimeArtifacts) {
        this.installedExtensionMetadata = List.copyOf(installedExtensionMetadata);
        this.runtimeArtifacts = List.copyOf(runtimeArtifacts);
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
        List<Path> runtimeArtifacts = new ArrayList<>();
        for (String argument : arguments) {
            String candidate = Objects.requireNonNull(argument, "argument");
            if (candidate.startsWith(EXTENSION_METADATA_PREFIX)) {
                installedExtensionMetadata.add(
                        argumentPath(candidate, EXTENSION_METADATA_PREFIX, "extension metadata"));
            } else if (candidate.startsWith(RUNTIME_ARTIFACT_PREFIX)) {
                runtimeArtifacts.add(argumentPath(candidate, RUNTIME_ARTIFACT_PREFIX, "runtime artifact"));
            } else {
                throw new IllegalArgumentException("Unsupported authoring service argument: " + candidate);
            }
        }
        return new AuthoringServiceConfiguration(installedExtensionMetadata, runtimeArtifacts);
    }

    /** Returns installed descriptor-only extension artifacts in configured order. */
    List<Path> installedExtensionMetadata() {
        return installedExtensionMetadata;
    }

    /** Returns renderer-only runtime artifacts in configured order. */
    List<Path> runtimeArtifacts() {
        return runtimeArtifacts;
    }

    private static Path argumentPath(String argument, String prefix, String description) {
        String configuredPath = argument.substring(prefix.length());
        if (configuredPath.isBlank()) {
            throw new IllegalArgumentException("Authoring service " + description + " path must not be blank");
        }
        return Path.of(configuredPath).toAbsolutePath().normalize();
    }
}
