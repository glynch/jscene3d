/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Serialized artifact kinds produced by project importers. */
public enum ImportArtifactKind {
    /** Complete native scene definition. */
    SCENE,
    /** Typed native project-resource definition. */
    RESOURCE,
    /** Opaque content referenced by a scene or resource. */
    PAYLOAD
}
