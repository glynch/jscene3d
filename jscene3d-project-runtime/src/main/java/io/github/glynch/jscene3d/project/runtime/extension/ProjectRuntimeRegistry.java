/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.extension.RegisteredType;

/** Construction-time registry binding safe type descriptors to trusted factories. */
public interface ProjectRuntimeRegistry {
    /**
     * Registers the construction adapter for one component type.
     *
     * @param type exact descriptor-declared component type
     * @param factory trusted component factory
     */
    void registerComponent(ComponentType type, ComponentFactory<?> factory);

    /**
     * Registers the factory for one scene-node type.
     *
     * @param type exact descriptor-declared type identity
     * @param factory trusted runtime factory
     */
    void registerSceneNode(RegisteredType type, SceneNodeFactory factory);

    /**
     * Registers the factory for one node-controller type.
     *
     * @param type exact descriptor-declared type identity
     * @param factory trusted runtime factory
     */
    void registerNodeController(RegisteredType type, NodeControllerFactory factory);

    /**
     * Registers the factory for one reusable resource type.
     *
     * @param type exact descriptor-declared type identity
     * @param factory trusted runtime factory
     */
    void registerResource(RegisteredType type, ResourceFactory factory);
}
