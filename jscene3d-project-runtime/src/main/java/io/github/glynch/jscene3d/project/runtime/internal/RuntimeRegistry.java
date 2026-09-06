/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import java.util.Objects;

/** Scope-checking registry exposed to one extension during contribution. */
public final class RuntimeRegistry implements ComponentFactoryRegistry {
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
    public void register(ComponentType type, ComponentFactory<?> factory) {
        requireRegistrationOpen();
        ComponentType validType = Objects.requireNonNull(type, "type");
        requireOwned(validType.id().value(), validType);
        if (catalog.findComponent(validType).isEmpty()) {
            throw new IllegalArgumentException("runtime component has no descriptor: " + validType);
        }
        bindings.addComponent(validType, Objects.requireNonNull(factory, "factory"));
    }

    /** Prevents a retained registry from being mutated after contribution returns. */
    public void closeRegistration() {
        acceptingRegistrations = false;
    }

    /** Requires that the extension is still allowed to contribute factories. */
    private void requireRegistrationOpen() {
        if (!acceptingRegistrations) {
            throw new IllegalStateException("runtime registration has already closed");
        }
    }

    /** Requires a contributed type identity to belong to this extension. */
    private void requireOwned(String id, Object type) {
        if (!id.startsWith(extensionId + '/')) {
            throw new IllegalArgumentException(
                    "runtime type does not belong to extension " + extensionId + ": " + type);
        }
    }
}
