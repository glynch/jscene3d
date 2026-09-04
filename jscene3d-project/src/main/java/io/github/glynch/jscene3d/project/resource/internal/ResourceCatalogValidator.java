/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.extension.internal.RegisteredPropertyValidator;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import java.util.List;
import java.util.Optional;

/** Performs registered-type validation over one structurally loaded resource. */
public final class ResourceCatalogValidator {
    /** Prevents instantiation of this shared validation policy. */
    private ResourceCatalogValidator() {
        throw new AssertionError("ResourceCatalogValidator cannot be instantiated");
    }

    /**
     * Returns ordered catalog-aware diagnostics for one resource.
     *
     * @param resource structurally loaded resource
     * @param catalog resolved registered-type catalog
     * @return immutable ordered diagnostics
     */
    public static List<ProjectDiagnostic> validate(ResourceDefinition resource, RegisteredTypeCatalog catalog) {
        DiagnosticCollector diagnostics = new DiagnosticCollector(resource.source());
        Optional<RegisteredTypeDescriptor> descriptor = catalog.find(resource.type());
        if (descriptor.isEmpty()) {
            diagnostics.error(
                    "resource.catalog.type.missing", "registered type was not found: " + resource.type(), "/type");
        } else if (descriptor.orElseThrow().scope() != RegisteredTypeScope.RESOURCE) {
            diagnostics.error(
                    "resource.catalog.type.scope",
                    "registered type " + resource.type() + " has scope "
                            + descriptor.orElseThrow().scope() + " but " + RegisteredTypeScope.RESOURCE
                            + " is required",
                    "/type");
        } else {
            diagnostics.addAll(RegisteredPropertyValidator.validate(
                    resource.properties(),
                    descriptor.orElseThrow(),
                    resource.source().toUri(),
                    "/properties",
                    "resource.catalog"));
        }
        return diagnostics.diagnostics();
    }
}
