/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.runtime.RuntimeNode;

/** Construction context for one controller attached to an instantiated node. */
public interface NodeControllerContext extends RuntimeCreationContext {
    /**
     * Returns the node controlled by the new object.
     *
     * @return fully created owning runtime node
     */
    RuntimeNode node();
}
