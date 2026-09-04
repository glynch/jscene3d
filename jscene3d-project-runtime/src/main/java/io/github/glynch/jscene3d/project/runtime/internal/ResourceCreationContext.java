/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactoryContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Runtime-owned context for one resource-factory invocation. */
final class ResourceCreationContext implements ResourceFactoryContext {
    private final GameProject project;
    private final ResourceDefinition definition;
    private final Map<String, ProjectValue> properties;
    private final ProjectResourceResolver resources;

    /** Creates one immutable resource-factory context. */
    ResourceCreationContext(
            GameProject project,
            ResourceDefinition definition,
            Map<String, ProjectValue> properties,
            ProjectResourceResolver resources) {
        this.project = Objects.requireNonNull(project, "project");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    @Override
    public GameProject project() {
        return project;
    }

    @Override
    public ResourceDefinition definition() {
        return definition;
    }

    @Override
    public Map<String, ProjectValue> properties() {
        return properties;
    }

    @Override
    public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
        return resources.resolve(reference, valueType);
    }
}
