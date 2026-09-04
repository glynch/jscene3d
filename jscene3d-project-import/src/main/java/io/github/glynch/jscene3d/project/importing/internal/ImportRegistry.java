/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportRegistry;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;
import java.util.Objects;

/** Scope-checking registry exposed to one import extension during contribution. */
public final class ImportRegistry implements ProjectImportRegistry {
    private final String extensionId;
    private final RegisteredTypeCatalog catalog;
    private final ImporterBindings bindings;
    private boolean acceptingRegistrations = true;

    /**
     * Creates one extension registration scope.
     *
     * @param extensionId owning extension identifier
     * @param catalog validated safe descriptor catalog
     * @param bindings destination executable index
     */
    public ImportRegistry(String extensionId, RegisteredTypeCatalog catalog, ImporterBindings bindings) {
        this.extensionId = Preconditions.requireNonBlank(extensionId, "extensionId");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
    }

    @Override
    public void registerImporter(RegisteredType type, ProjectImporter importer) {
        RegisteredType validType = requireImporterScope(type);
        bindings.add(validType, Objects.requireNonNull(importer, "importer"));
    }

    /** Prevents a retained registry from accepting contributions after registration returns. */
    public void closeRegistration() {
        acceptingRegistrations = false;
    }

    /** Requires an owned descriptor-declared importer type. */
    private RegisteredType requireImporterScope(RegisteredType type) {
        if (!acceptingRegistrations) {
            throw new IllegalStateException("import registration has already closed");
        }
        RegisteredType validType = Objects.requireNonNull(type, "type");
        if (!validType.id().startsWith(extensionId + '/')) {
            throw new IllegalArgumentException(
                    "importer type does not belong to extension " + extensionId + ": " + validType);
        }
        RegisteredTypeDescriptor descriptor = catalog.find(validType)
                .orElseThrow(() -> new IllegalArgumentException("importer type has no descriptor: " + validType));
        if (descriptor.scope() != RegisteredTypeScope.IMPORTER) {
            throw new IllegalArgumentException(
                    "registered type has scope " + descriptor.scope() + " instead of IMPORTER: " + validType);
        }
        return validType;
    }
}
