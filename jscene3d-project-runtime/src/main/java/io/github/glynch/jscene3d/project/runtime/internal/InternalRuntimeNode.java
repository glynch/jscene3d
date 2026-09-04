/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;
import io.github.glynch.jscene3d.project.runtime.RuntimeNode;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Runtime-owned mutable assembly of an externally read-only node. */
final class InternalRuntimeNode implements RuntimeNode {
    private final SceneNodeDefinition definition;
    private final boolean enabled;
    private final ProjectRuntimeObject object;
    private final Optional<RuntimeNode> parent;
    private final List<RuntimeNode> children = new ArrayList<>();
    private Optional<ProjectRuntimeObject> controller = Optional.empty();
    private boolean complete;

    /** Creates an incomplete runtime node before its controller and children. */
    InternalRuntimeNode(
            SceneNodeDefinition definition,
            boolean enabled,
            ProjectRuntimeObject object,
            Optional<RuntimeNode> parent) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.enabled = enabled;
        this.object = Objects.requireNonNull(object, "object");
        this.parent = Objects.requireNonNull(parent, "parent");
    }

    @Override
    public SceneNodeDefinition definition() {
        return definition;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ProjectRuntimeObject object() {
        return object;
    }

    @Override
    public Optional<ProjectRuntimeObject> controller() {
        return controller;
    }

    @Override
    public Optional<RuntimeNode> parent() {
        return parent;
    }

    @Override
    public List<RuntimeNode> children() {
        return List.copyOf(children);
    }

    /** Assigns the optional controller during composition. */
    void setController(ProjectRuntimeObject value) {
        requireIncomplete();
        controller = Optional.of(Objects.requireNonNull(value, "value"));
    }

    /** Adds one child during composition. */
    void addChild(RuntimeNode child) {
        requireIncomplete();
        children.add(Objects.requireNonNull(child, "child"));
    }

    /** Prevents any further structural mutation. */
    void complete() {
        requireIncomplete();
        complete = true;
    }

    /** Requires the runtime-owned assembly phase. */
    private void requireIncomplete() {
        if (complete) {
            throw new IllegalStateException("runtime node composition is complete");
        }
    }
}
