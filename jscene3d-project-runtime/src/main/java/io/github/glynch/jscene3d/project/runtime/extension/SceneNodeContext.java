/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.runtime.RuntimeNode;
import java.util.Optional;

/** Construction context for one registered scene-node type. */
public interface SceneNodeContext extends RuntimeCreationContext {
    /**
     * Returns the runtime parent, or empty while creating the root.
     *
     * @return parent runtime node, or empty for the root
     */
    Optional<RuntimeNode> parent();
}
