/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isLocalId;
import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isPortableLocator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Shared decoder and validator for resource-reference values in project documents. */
public final class ProjectReferenceDecoder {
    private final GameProject project;
    private final ProjectPathResolver paths;
    private final DiagnosticCollector diagnostics;
    private final String diagnosticPrefix;

    /**
     * Creates a decoder for one project document family.
     *
     * @param project containing validated project
     * @param paths project-confined path resolver
     * @param diagnostics destination for validation diagnostics
     * @param diagnosticPrefix diagnostic namespace such as {@code scene} or {@code resource}
     */
    public ProjectReferenceDecoder(
            GameProject project, ProjectPathResolver paths, DiagnosticCollector diagnostics, String diagnosticPrefix) {
        this.project = Objects.requireNonNull(project, "project");
        this.paths = Objects.requireNonNull(paths, "paths");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.diagnosticPrefix = Preconditions.requireNonBlank(diagnosticPrefix, "diagnosticPrefix");
    }

    /**
     * Converts a reserved single-property reference object.
     *
     * @param raw object containing the authored reference
     * @param location JSON Pointer location
     * @return validated reference value or a safe placeholder after an error
     */
    public ProjectValue.ReferenceValue decode(JsonNode raw, String location) {
        JsonNode referenceNode = raw.get("$ref");
        if (raw.size() != 1 || referenceNode == null || !referenceNode.isTextual()) {
            diagnostics.error(
                    code("reference.object"),
                    "a resource reference must contain only one textual $ref property",
                    location);
            return new ProjectValue.ReferenceValue(ResourceReference.asset("invalid"));
        }
        return new ProjectValue.ReferenceValue(validate(referenceNode.textValue(), location + "/$ref"));
    }

    /** Validates a namespaced reference. */
    private ResourceReference validate(String value, String location) {
        for (ResourceReference.Kind kind : ResourceReference.Kind.values()) {
            if (value.startsWith(kind.prefix())) {
                return validate(kind, value.substring(kind.prefix().length()), location);
            }
        }
        diagnostics.error(
                code("reference.scheme"), "resource reference must use project:, asset:, or import:", location);
        return ResourceReference.asset("invalid");
    }

    /** Validates one reference namespace. */
    private ResourceReference validate(ResourceReference.Kind kind, String locator, String location) {
        if (locator.isBlank()) {
            diagnostics.error(code("reference.locator"), "resource reference locator must not be blank", location);
            return ResourceReference.asset("invalid");
        }
        return switch (kind) {
            case PROJECT -> validateProject(locator, location);
            case ASSET -> validateAsset(locator, location);
            case IMPORT -> validateImport(locator, location);
        };
    }

    /** Validates one project-file reference. */
    private ResourceReference validateProject(String locator, String location) {
        Optional<Path> path = paths.resolve(locator, location, true);
        if (path.isEmpty()) {
            return ResourceReference.project("invalid.resource", project.root().resolve("invalid.resource"));
        }
        return ResourceReference.project(locator, path.orElseThrow());
    }

    /** Validates one declared source-asset reference. */
    private ResourceReference validateAsset(String locator, String location) {
        if (!isLocalId(locator)) {
            diagnostics.error(code("reference.asset"), "asset reference must contain a local asset id", location);
            return ResourceReference.asset("invalid");
        }
        if (project.assets().stream().noneMatch(asset -> asset.id().equals(locator))) {
            diagnostics.error(code("reference.asset.missing"), "asset is not declared: " + locator, location);
        }
        return ResourceReference.asset(locator);
    }

    /** Validates one imported-output reference. */
    private ResourceReference validateImport(String locator, String location) {
        int separator = locator.indexOf('/');
        boolean valid = separator > 0
                && separator < locator.length() - 1
                && isLocalId(locator.substring(0, separator))
                && isPortableLocator(locator.substring(separator + 1));
        if (!valid) {
            diagnostics.error(
                    code("reference.import"),
                    "import reference must contain an import id and portable output locator",
                    location);
            return ResourceReference.imported("invalid/output");
        }
        return ResourceReference.imported(locator);
    }

    /** Builds one document-family diagnostic code. */
    private String code(String suffix) {
        return diagnosticPrefix + '.' + suffix;
    }
}
