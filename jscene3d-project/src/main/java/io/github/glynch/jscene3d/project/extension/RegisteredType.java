/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePositive;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireRegisteredTypeId;

/** Versioned type supplied by the engine or a project extension.
 *
 * @param id extension-qualified type identifier
 * @param version positive definition version
 */
public record RegisteredType(String id, int version) {
    /** Validates one registered type reference. */
    public RegisteredType {
        id = requireRegisteredTypeId(id, "id");
        version = requirePositive(version, "version");
    }
}
