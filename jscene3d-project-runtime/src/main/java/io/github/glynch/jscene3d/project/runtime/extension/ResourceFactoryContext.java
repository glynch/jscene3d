/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;

/** Bounded context supplied while creating one shared runtime resource. */
public interface ResourceFactoryContext extends RuntimeResourceLookup {
    /**
     * Returns the project owning the resource.
     *
     * @return validated project manifest
     */
    GameProject project();

    /**
     * Returns the structurally and catalog-validated resource definition.
     *
     * @return validated resource definition
     */
    ResourceDefinition definition();

    /**
     * Returns descriptor defaults merged with authored values.
     *
     * @return immutable effective properties in descriptor declaration order
     */
    Map<String, ProjectValue> properties();
}
