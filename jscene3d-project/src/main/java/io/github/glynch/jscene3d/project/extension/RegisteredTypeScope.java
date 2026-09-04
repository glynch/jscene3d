/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

/** Supported authoring and runtime scopes for registered types. */
public enum RegisteredTypeScope {
    /** Type instantiated as a scene-tree node. */
    SCENE_NODE,
    /** Java controller attached to one scene node. */
    NODE_CONTROLLER,
    /** Project-wide system instantiated once per runtime session. */
    PROJECT_SYSTEM,
    /** Reusable typed project resource. */
    RESOURCE,
    /** Source-asset importer. */
    IMPORTER
}
