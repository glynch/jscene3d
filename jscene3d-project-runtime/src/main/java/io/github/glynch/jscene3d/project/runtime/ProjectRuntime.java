/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.game.GameApplication;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import java.util.Optional;

/** Fully composed project application ready for ownership by a game runtime. */
public interface ProjectRuntime extends GameApplication {
    /**
     * Returns the loaded project.
     *
     * @return validated project manifest
     */
    GameProject project();

    /**
     * Returns the instantiated entry-scene definition.
     *
     * @return validated entry scene
     */
    SceneDefinition scene();

    /**
     * Returns the entry scene's runtime root.
     *
     * @return root runtime node
     */
    RuntimeNode root();

    /**
     * Finds a runtime node by its stable scene-wide identifier.
     *
     * @param id authored node identifier
     * @return matching runtime node, or empty when absent
     */
    Optional<RuntimeNode> findNode(String id);
}
