/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.RuntimeAction;
import io.github.glynch.jscene3d.project.runtime.RuntimePayloadAction;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;

/** Capabilities shared by scene-node and controller factories. */
public interface RuntimeCreationContext extends RuntimeResourceLookup {
    /**
     * Returns the project being composed.
     *
     * @return validated project manifest
     */
    GameProject project();

    /**
     * Returns the entry-scene definition.
     *
     * @return validated entry scene
     */
    SceneDefinition scene();

    /**
     * Returns the node definition owning this runtime object.
     *
     * @return validated owning-node definition
     */
    SceneNodeDefinition nodeDefinition();

    /**
     * Returns authored values merged over descriptor defaults.
     *
     * @return immutable effective properties in descriptor declaration order
     */
    Map<String, ProjectValue> properties();

    /**
     * Returns whether the owning node and every ancestor are enabled.
     *
     * @return effective enabled state during composition and dispatch
     */
    boolean isEnabled();

    /**
     * Returns an emitter for a signal declared by this registered type.
     *
     * @param id declared signal identifier
     * @return runtime-bound signal emitter
     */
    RuntimeSignal signal(String id);

    /**
     * Implements one declared action without a payload.
     *
     * @param id declared action identifier
     * @param action synchronous action implementation
     */
    void action(String id, RuntimeAction action);

    /**
     * Implements one declared action carrying a payload.
     *
     * @param id declared action identifier
     * @param action synchronous typed-payload implementation
     */
    void action(String id, RuntimePayloadAction action);
}
