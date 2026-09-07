/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import java.util.Objects;

/** Project-scoped definition and immutable-resource content used for one world composition.
 *
 * @param definitions authored and generated definition resolver
 * @param resources immutable runtime-resource provider
 */
public record ProjectContent(DefinitionResolver definitions, RuntimeResourceProvider resources) {
    /** Validates both project-content facilities. */
    public ProjectContent {
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(resources, "resources");
    }
}
