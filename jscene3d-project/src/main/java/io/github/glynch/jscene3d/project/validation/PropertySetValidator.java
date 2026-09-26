/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation;

import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.validation.internal.PropertySetValidation;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Authoritative project-domain validation facade for complete authored property sets.
 *
 * <p>Validation is deterministic, synchronous, side-effect free, and non-normalizing. The implementation always
 * applies core property-set, structural, descriptor, and built-in semantic rules before owner-type and contextual
 * rules. Callers cannot select a partial phase. Future extension rules may append diagnostics but cannot remove or
 * replace core diagnostics.
 */
public final class PropertySetValidator {
    /** Prevents construction of this stateless facade. */
    private PropertySetValidator() {
        throw new AssertionError("PropertySetValidator cannot be instantiated");
    }

    /**
     * Validates a complete resource, scene-node, system, or importer property set.
     *
     * @param authored complete authored property map in document order
     * @param owner exact resolved owner descriptor
     * @param source absolute source document URI
     * @param location JSON Pointer of the property object
     * @return immutable diagnostics in deterministic validation order
     */
    public static List<ProjectDiagnostic> validate(
            Map<String, ProjectValue> authored, RegisteredTypeDescriptor owner, URI source, String location) {
        return PropertySetValidation.validateRegistered(authored, owner, source, location);
    }

    /**
     * Validates a complete component property set, including definition-local targets.
     *
     * @param authored complete authored property map in document order
     * @param owner exact resolved component descriptor
     * @param source absolute source document URI
     * @param location JSON Pointer of the property object
     * @param targets definition-local read-only target lookup
     * @return immutable diagnostics in deterministic validation order
     */
    public static List<ProjectDiagnostic> validateComponent(
            Map<PropertyId, ProjectValue> authored,
            ComponentTypeDescriptor owner,
            URI source,
            String location,
            PropertyTargetLookup targets) {
        return PropertySetValidation.validateComponent(authored, owner, source, location, targets);
    }
}
