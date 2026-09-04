/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.extension.NodeControllerFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactory;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable factory index used only while runtime extensions register. */
public final class FactoryBindings {
    private final Map<RegisteredType, SceneNodeFactory> sceneNodes;
    private final Map<RegisteredType, NodeControllerFactory> controllers;
    private final Map<RegisteredType, ResourceFactory> resources;

    /** Creates an empty composition-time factory index. */
    public FactoryBindings() {
        sceneNodes = new LinkedHashMap<>();
        controllers = new LinkedHashMap<>();
        resources = new LinkedHashMap<>();
    }

    /**
     * Adds one uniquely registered scene-node factory.
     *
     * @param type exact registered type identity
     * @param factory trusted runtime factory
     */
    public void addSceneNode(RegisteredType type, SceneNodeFactory factory) {
        if (sceneNodes.putIfAbsent(type, factory) != null) {
            throw new IllegalArgumentException("scene-node factory is already registered: " + type);
        }
    }

    /**
     * Adds one uniquely registered controller factory.
     *
     * @param type exact registered type identity
     * @param factory trusted runtime factory
     */
    public void addController(RegisteredType type, NodeControllerFactory factory) {
        if (controllers.putIfAbsent(type, factory) != null) {
            throw new IllegalArgumentException("node-controller factory is already registered: " + type);
        }
    }

    /**
     * Adds one uniquely registered resource factory.
     *
     * @param type exact registered type identity
     * @param factory trusted runtime factory
     */
    public void addResource(RegisteredType type, ResourceFactory factory) {
        if (resources.putIfAbsent(type, factory) != null) {
            throw new IllegalArgumentException("resource factory is already registered: " + type);
        }
    }

    /**
     * Returns the required scene-node factory.
     *
     * @param type exact registered type identity
     * @param location diagnostic location used when absent
     * @return registered scene-node factory
     */
    public SceneNodeFactory requireSceneNode(RegisteredType type, String location) {
        SceneNodeFactory factory = sceneNodes.get(type);
        if (factory == null) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.SCENE_NODE_FACTORY_MISSING,
                    "no runtime factory is registered for " + type,
                    location);
        }
        return factory;
    }

    /**
     * Returns the required node-controller factory.
     *
     * @param type exact registered type identity
     * @param location diagnostic location used when absent
     * @return registered controller factory
     */
    public NodeControllerFactory requireController(RegisteredType type, String location) {
        NodeControllerFactory factory = controllers.get(type);
        if (factory == null) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.CONTROLLER_FACTORY_MISSING,
                    "no runtime factory is registered for " + type,
                    location);
        }
        return factory;
    }

    /**
     * Returns the required resource factory.
     *
     * @param type exact registered type identity
     * @param location diagnostic location used when absent
     * @return registered resource factory
     */
    public ResourceFactory requireResource(RegisteredType type, String location) {
        ResourceFactory factory = resources.get(type);
        if (factory == null) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.RESOURCE_FACTORY_MISSING,
                    "no runtime factory is registered for " + type,
                    location);
        }
        return factory;
    }
}
