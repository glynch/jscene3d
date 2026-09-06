/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

/** Stable endpoint address used by the scene-definition runtime. */
record SceneEndpointAddress(String nodeId, String endpointId) implements EndpointAddress {
    /** Validates the two legacy endpoint identity dimensions. */
    SceneEndpointAddress {
        Preconditions.requireNonBlank(nodeId, "nodeId");
        Preconditions.requireNonBlank(endpointId, "endpointId");
    }
}
