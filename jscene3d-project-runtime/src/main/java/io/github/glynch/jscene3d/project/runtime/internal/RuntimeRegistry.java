/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.runtime.extension.NodeControllerFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactory;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeFactory;
import java.util.Objects;

/** Scope-checking registry exposed to one extension during contribution. */
public final class RuntimeRegistry implements ProjectRuntimeRegistry {
    private final String extensionId;
    private final RegisteredTypeCatalog catalog;
    private final FactoryBindings bindings;
    private boolean acceptingRegistrations = true;

    /**
     * Creates one contribution scope.
     *
     * @param extensionId owning extension identifier
     * @param catalog validated registered-type catalog
     * @param bindings destination factory index
     */
    public RuntimeRegistry(String extensionId, RegisteredTypeCatalog catalog, FactoryBindings bindings) {
        this.extensionId = Preconditions.requireNonBlank(extensionId, "extensionId");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
    }

    @Override
    public void registerSceneNode(RegisteredType type, SceneNodeFactory factory) {
        requireScope(type, RegisteredTypeScope.SCENE_NODE);
        bindings.addSceneNode(type, Objects.requireNonNull(factory, "factory"));
    }

    @Override
    public void registerNodeController(RegisteredType type, NodeControllerFactory factory) {
        requireScope(type, RegisteredTypeScope.NODE_CONTROLLER);
        bindings.addController(type, Objects.requireNonNull(factory, "factory"));
    }

    @Override
    public void registerResource(RegisteredType type, ResourceFactory factory) {
        requireScope(type, RegisteredTypeScope.RESOURCE);
        bindings.addResource(type, Objects.requireNonNull(factory, "factory"));
    }

    /** Prevents a retained registry from being mutated after contribution returns. */
    public void closeRegistration() {
        acceptingRegistrations = false;
    }

    /** Requires an owned catalog type with the registration's exact scope. */
    private void requireScope(RegisteredType type, RegisteredTypeScope expectedScope) {
        if (!acceptingRegistrations) {
            throw new IllegalStateException("runtime registration has already closed");
        }
        RegisteredType validType = Objects.requireNonNull(type, "type");
        if (!validType.id().startsWith(extensionId + '/')) {
            throw new IllegalArgumentException(
                    "runtime type does not belong to extension " + extensionId + ": " + type);
        }
        RegisteredTypeDescriptor descriptor = catalog.find(validType)
                .orElseThrow(() -> new IllegalArgumentException("runtime type has no descriptor: " + validType));
        if (descriptor.scope() != expectedScope) {
            throw new IllegalArgumentException(
                    "runtime type has scope " + descriptor.scope() + " instead of " + expectedScope + ": " + validType);
        }
    }
}
