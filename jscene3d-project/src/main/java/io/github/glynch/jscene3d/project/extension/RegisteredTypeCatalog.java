/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.internal.SceneCatalogValidator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable lookup of safe metadata contributed by the project's resolved extensions. */
public final class RegisteredTypeCatalog {
    private final List<ExtensionDescriptor> extensions;
    private final Map<RegisteredType, RegisteredTypeDescriptor> types;

    /** Builds a catalog from validated descriptors. */
    RegisteredTypeCatalog(List<ExtensionDescriptor> extensions) {
        this.extensions = List.copyOf(extensions);
        Map<RegisteredType, RegisteredTypeDescriptor> indexed = new LinkedHashMap<>();
        for (ExtensionDescriptor extension : this.extensions) {
            for (RegisteredTypeDescriptor descriptor : extension.types()) {
                if (indexed.putIfAbsent(descriptor.type(), descriptor) != null) {
                    throw new IllegalArgumentException("registered type is duplicated: " + descriptor.type());
                }
            }
        }
        types = Collections.unmodifiableMap(indexed);
    }

    /**
     * Returns resolved extension descriptors in project declaration order.
     *
     * @return immutable extension descriptors
     */
    public List<ExtensionDescriptor> extensions() {
        return extensions;
    }

    /**
     * Returns registered type descriptors in deterministic extension and declaration order.
     *
     * @return immutable registered-type descriptors
     */
    public List<RegisteredTypeDescriptor> types() {
        return List.copyOf(types.values());
    }

    /**
     * Returns the exact registered type version when available.
     *
     * @param type registered type and version
     * @return matching descriptor when registered
     */
    public Optional<RegisteredTypeDescriptor> find(RegisteredType type) {
        return Optional.ofNullable(types.get(Objects.requireNonNull(type, "type")));
    }

    /**
     * Validates registered types, properties, and connection endpoints in one loaded scene.
     *
     * @param scene structurally valid scene
     * @return ordered catalog-aware diagnostics
     */
    public List<ProjectDiagnostic> validate(SceneDefinition scene) {
        return SceneCatalogValidator.validate(Objects.requireNonNull(scene, "scene"), this);
    }
}
