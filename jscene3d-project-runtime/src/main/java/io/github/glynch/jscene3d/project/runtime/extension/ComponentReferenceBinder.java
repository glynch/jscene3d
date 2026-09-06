/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

/**
 * Optional binding capability implemented by a runtime component with authored entity or component targets.
 *
 * <p>The world invokes this once after every component factory has completed and before any semantic lifecycle
 * callback. Implementations resolve their declared target properties through the supplied resolver and store the
 * resulting direct references. They must not retain the resolver beyond the invocation.
 */
@FunctionalInterface
public interface ComponentReferenceBinder {
    /**
     * Resolves and stores this component's authored references.
     *
     * @param references resolver scoped to this component's effective authored properties
     */
    void bindReferences(ComponentReferenceResolver references);
}
