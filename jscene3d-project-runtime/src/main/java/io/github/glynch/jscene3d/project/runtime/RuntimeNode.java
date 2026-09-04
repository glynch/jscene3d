/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import java.util.List;
import java.util.Optional;

/** Read-only view of one instantiated scene-tree node. */
public interface RuntimeNode {
    /**
     * Returns the authored node definition.
     *
     * @return validated authored definition
     */
    SceneNodeDefinition definition();

    /**
     * Returns whether this node and every ancestor are enabled.
     *
     * @return effective enabled state
     */
    boolean isEnabled();

    /**
     * Returns the object created by the node factory.
     *
     * @return owned runtime object
     */
    ProjectRuntimeObject object();

    /**
     * Returns the optional object created by the controller factory.
     *
     * @return attached controller object, or empty when none was authored
     */
    Optional<ProjectRuntimeObject> controller();

    /**
     * Returns the optional parent node.
     *
     * @return parent runtime node, or empty for the root
     */
    Optional<RuntimeNode> parent();

    /**
     * Returns child nodes in authored order.
     *
     * @return immutable ordered child nodes
     */
    List<RuntimeNode> children();
}
