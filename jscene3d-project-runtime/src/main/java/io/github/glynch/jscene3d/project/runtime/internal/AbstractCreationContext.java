/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.RuntimeAction;
import io.github.glynch.jscene3d.project.runtime.RuntimePayloadAction;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;
import io.github.glynch.jscene3d.project.runtime.extension.RuntimeCreationContext;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Shared immutable factory context and endpoint policy. */
abstract class AbstractCreationContext implements RuntimeCreationContext {
    private final GameProject project;
    private final SceneDefinition scene;
    private final SceneNodeDefinition nodeDefinition;
    private final Map<String, ProjectValue> properties;
    private final RegisteredTypeDescriptor descriptor;
    private final EndpointRouter router;
    private final BooleanSupplier enabled;

    AbstractCreationContext(
            GameProject project,
            SceneDefinition scene,
            SceneNodeDefinition nodeDefinition,
            Map<String, ProjectValue> properties,
            RegisteredTypeDescriptor descriptor,
            EndpointRouter router,
            BooleanSupplier enabled) {
        this.project = Objects.requireNonNull(project, "project");
        this.scene = Objects.requireNonNull(scene, "scene");
        this.nodeDefinition = Objects.requireNonNull(nodeDefinition, "nodeDefinition");
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.router = Objects.requireNonNull(router, "router");
        this.enabled = Objects.requireNonNull(enabled, "enabled");
    }

    @Override
    public final GameProject project() {
        return project;
    }

    @Override
    public final SceneDefinition scene() {
        return scene;
    }

    @Override
    public final SceneNodeDefinition nodeDefinition() {
        return nodeDefinition;
    }

    @Override
    public final Map<String, ProjectValue> properties() {
        return properties;
    }

    @Override
    public final RuntimeSignal signal(String id) {
        return router.signal(nodeDefinition.id(), requireEndpoint(descriptor.signals(), id, "signal"), enabled);
    }

    @Override
    public final void action(String id, RuntimeAction action) {
        router.action(
                nodeDefinition.id(),
                requireEndpoint(descriptor.actions(), id, "action"),
                enabled,
                Objects.requireNonNull(action, "action"));
    }

    @Override
    public final void action(String id, RuntimePayloadAction action) {
        router.action(
                nodeDefinition.id(),
                requireEndpoint(descriptor.actions(), id, "action"),
                enabled,
                Objects.requireNonNull(action, "action"));
    }

    /** Returns one endpoint declared by the current type. */
    private static EndpointDescriptor requireEndpoint(
            Map<String, EndpointDescriptor> endpoints, String id, String kind) {
        String validId = Preconditions.requireNonBlank(id, "id");
        EndpointDescriptor endpoint = endpoints.get(validId);
        if (endpoint == null) {
            throw new IllegalArgumentException(kind + " is not declared: " + validId);
        }
        return endpoint;
    }
}
