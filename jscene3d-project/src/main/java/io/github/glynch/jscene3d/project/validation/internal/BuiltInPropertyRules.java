/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation.internal;

import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.standard.game3d.internal.StandardGame3dPropertyRules;
import io.github.glynch.jscene3d.project.standard.physics3d.internal.StandardPhysics3dPropertyRules;
import io.github.glynch.jscene3d.project.standard.presentation.internal.StandardPresentationPropertyRules;
import io.github.glynch.jscene3d.project.standard.spatial3d.internal.StandardSpatial3dPropertyRules;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Deterministic internal catalog of type-owned rules for built-in exact component types. */
final class BuiltInPropertyRules {
    /** Prevents construction of this closed built-in catalog. */
    private BuiltInPropertyRules() {
        throw new AssertionError("BuiltInPropertyRules cannot be instantiated");
    }

    /** Applies built-in owner-type rules in stable feature order. */
    static List<ProjectDiagnostic> validateComponent(
            ComponentTypeDescriptor owner, Map<String, ProjectValue> effective, URI source, String location) {
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(StandardSpatial3dPropertyRules.validate(owner.type(), effective, source, location));
        diagnostics.addAll(StandardPhysics3dPropertyRules.validate(owner.type(), effective, source, location));
        diagnostics.addAll(StandardGame3dPropertyRules.validate(owner.type(), effective, source, location));
        diagnostics.addAll(StandardPresentationPropertyRules.validate(owner.type(), effective, source, location));
        return List.copyOf(diagnostics);
    }
}
