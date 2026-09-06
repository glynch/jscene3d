/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;

/**
 * Resolves target-valued component properties within their original definition-instance scopes.
 *
 * <p>The resolver is valid only during one {@link ComponentReferenceBinder#bindReferences} invocation. Resolution
 * uses stable authored identities and preserves the source scope of values supplied through public definition
 * contracts; it never searches by hierarchy path, name, implementation type, or nearest ancestor.
 */
public interface ComponentReferenceResolver {
    /**
     * Resolves one required {@code entity_target} property.
     *
     * @param property target-valued property identity
     * @return live entity in the target value's original definition-instance scope
     * @throws IllegalStateException if the resolver is used outside its binding invocation
     */
    Entity entity(PropertyId property);

    /**
     * Resolves one required {@code component_target} property and verifies its runtime representation.
     *
     * @param <T> expected runtime component type
     * @param property target-valued property identity
     * @param valueType expected Java representation
     * @return live component value in the target value's original definition-instance scope
     * @throws IllegalStateException if the resolver is used outside its binding invocation
     */
    <T> T component(PropertyId property, Class<T> valueType);
}
