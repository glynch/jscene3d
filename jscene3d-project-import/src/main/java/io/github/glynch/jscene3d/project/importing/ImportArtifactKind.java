/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Serialized artifact kinds produced by project importers. */
public enum ImportArtifactKind {
    /** Complete generated entity definition. */
    ENTITY_DEFINITION,
    /** Typed native project-resource definition. */
    RESOURCE,
    /** Opaque content referenced by an entity definition or resource. */
    PAYLOAD
}
