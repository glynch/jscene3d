/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One independently browsable collection of examples and captured thumbnails.
 *
 * @param windowTitle title assigned to the native browser window
 * @param brandName suite name displayed by the gallery
 * @param resourceAnchor class whose module owns the suite resources
 * @param thumbnailDirectory absolute classpath directory containing the suite thumbnails
 * @param definitions examples exposed by the suite in gallery order
 */
public record ExampleSuite(
        String windowTitle,
        String brandName,
        Class<?> resourceAnchor,
        String thumbnailDirectory,
        List<ExampleDefinition> definitions) {
    /** Validates suite metadata, copies definitions, and rejects ambiguous identifiers. */
    public ExampleSuite {
        Objects.requireNonNull(windowTitle, "windowTitle");
        Objects.requireNonNull(brandName, "brandName");
        Objects.requireNonNull(resourceAnchor, "resourceAnchor");
        Objects.requireNonNull(thumbnailDirectory, "thumbnailDirectory");
        definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
        if (windowTitle.isBlank()) {
            throw new IllegalArgumentException("windowTitle must not be blank");
        }
        if (brandName.isBlank()) {
            throw new IllegalArgumentException("brandName must not be blank");
        }
        if (!thumbnailDirectory.startsWith("/") || thumbnailDirectory.endsWith("/")) {
            throw new IllegalArgumentException(
                    "thumbnailDirectory must be an absolute resource directory without a trailing slash");
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("definitions must not be empty");
        }
        requireUniqueIds(definitions);
    }

    /**
     * Returns the absolute classpath name of one definition's required PNG thumbnail.
     *
     * @param definition definition belonging to this suite
     * @return absolute classpath resource name for the definition's thumbnail
     */
    public String thumbnailResource(ExampleDefinition definition) {
        ExampleDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        if (!definitions.contains(validDefinition)) {
            throw new IllegalArgumentException("Definition does not belong to this suite: " + validDefinition.id());
        }
        return thumbnailDirectory + "/" + validDefinition.id() + ".png";
    }

    /** Rejects duplicate stable identifiers before browser selection becomes ambiguous. */
    private static void requireUniqueIds(List<ExampleDefinition> definitions) {
        Set<String> ids = new HashSet<>();
        for (ExampleDefinition definition : definitions) {
            if (!ids.add(definition.id())) {
                throw new IllegalArgumentException("Duplicate example identifier: " + definition.id());
            }
        }
    }
}
