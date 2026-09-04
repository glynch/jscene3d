/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Mutable construction-time importer index that becomes read-only after registration. */
public final class ImporterBindings {
    private final Map<String, ImporterBinding> bindings = new LinkedHashMap<>();

    /** Creates an index whose registrations are populated by trusted extensions. */
    private ImporterBindings() {
        // Registration is deliberately a separate phase after construction.
    }

    /**
     * Creates an empty mutable binding index for one registration pass.
     *
     * @return empty binding index
     */
    public static ImporterBindings create() {
        return new ImporterBindings();
    }

    /**
     * Adds one unique importer identity.
     *
     * @param type exact registered importer type
     * @param importer executable importer implementation
     */
    public void add(RegisteredType type, ProjectImporter importer) {
        ImporterBinding binding = new ImporterBinding(type, importer);
        if (bindings.putIfAbsent(type.id(), binding) != null) {
            throw new IllegalArgumentException("importer implementation is duplicated: " + type.id());
        }
    }

    /**
     * Returns the importer registered under one descriptor identity.
     *
     * @param importerId registered importer identity
     * @return importer binding when registered
     */
    public Optional<ImporterBinding> find(String importerId) {
        return Optional.ofNullable(bindings.get(importerId));
    }
}
