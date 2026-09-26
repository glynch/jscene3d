/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation.internal;

import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.internal.JsonPointers;
import io.github.glynch.jscene3d.project.validation.PropertyTargetLookup;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Complete ordered implementation hidden behind {@code PropertySetValidator}. */
public final class PropertySetValidation {
    /** Prevents construction of this stateless implementation. */
    private PropertySetValidation() {
        throw new AssertionError("PropertySetValidation cannot be instantiated");
    }

    /**
     * Validates one complete registered-type property set.
     *
     * @param authored authored property values in document order
     * @param owner exact registered type which owns the properties
     * @param source source document containing the property set
     * @param location JSON pointer for the property set
     * @return ordered validation diagnostics
     */
    public static List<ProjectDiagnostic> validateRegistered(
            Map<String, ProjectValue> authored, RegisteredTypeDescriptor owner, URI source, String location) {
        Objects.requireNonNull(owner, "owner");
        return validateCore(
                immutableStrings(authored), owner.properties(), owner.type().toString(), source, location, null, null);
    }

    /**
     * Validates one complete component property set and its contextual targets.
     *
     * @param authored authored property values in document order
     * @param owner exact component type which owns the properties
     * @param source source document containing the property set
     * @param location JSON pointer for the property set
     * @param targets target identities visible from the containing definition
     * @return ordered validation diagnostics
     */
    public static List<ProjectDiagnostic> validateComponent(
            Map<PropertyId, ProjectValue> authored,
            ComponentTypeDescriptor owner,
            URI source,
            String location,
            PropertyTargetLookup targets) {
        Objects.requireNonNull(owner, "owner");
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        Objects.requireNonNull(authored, "authored")
                .forEach((id, value) -> values.put(
                        Objects.requireNonNull(id, "authored key").value(),
                        Objects.requireNonNull(value, "authored value")));
        Map<String, PropertyDescriptor> descriptors = new LinkedHashMap<>();
        owner.properties().forEach((id, descriptor) -> descriptors.put(id.value(), descriptor));
        return validateCore(
                Collections.unmodifiableMap(values),
                Collections.unmodifiableMap(descriptors),
                owner.type().toString(),
                source,
                location,
                Objects.requireNonNull(targets, "targets"),
                owner);
    }

    /** Owns the complete non-bypassable phase sequence. */
    private static List<ProjectDiagnostic> validateCore(
            Map<String, ProjectValue> authored,
            Map<String, PropertyDescriptor> descriptors,
            String ownerId,
            URI source,
            String location,
            @Nullable PropertyTargetLookup targets,
            @Nullable ComponentTypeDescriptor componentOwner) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
        ValidationDiagnostics diagnostics = new ValidationDiagnostics(source);
        Set<String> structurallyValid = new LinkedHashSet<>();
        boolean allKnownValuesStructurallyValid =
                validateAuthored(authored, descriptors, ownerId, location, diagnostics, structurallyValid);
        if (allKnownValuesStructurallyValid && componentOwner != null) {
            diagnostics.addAll(BuiltInPropertyRules.validateComponent(
                    componentOwner, effective(authored, descriptors), source, location));
        }
        if (targets != null) {
            validateTargets(authored, structurallyValid, targets, location, diagnostics);
        }
        validateRequired(authored, descriptors, location, diagnostics);
        return diagnostics.values();
    }

    /** Validates authored entries in document order. */
    private static boolean validateAuthored(
            Map<String, ProjectValue> authored,
            Map<String, PropertyDescriptor> descriptors,
            String ownerId,
            String location,
            ValidationDiagnostics diagnostics,
            Set<String> structurallyValid) {
        boolean valid = true;
        for (Map.Entry<String, ProjectValue> entry : authored.entrySet()) {
            String propertyLocation = propertyLocation(location, entry.getKey());
            PropertyDescriptor descriptor = descriptors.get(entry.getKey());
            if (descriptor == null) {
                diagnostics.error(
                        PropertyValidationDiagnosticCode.UNKNOWN,
                        propertyLocation,
                        "property is not declared by its exact owner type",
                        entry.getKey(),
                        ownerId);
                continue;
            }
            if (PropertyValueValidation.validate(
                    entry.getKey(), entry.getValue(), descriptor, propertyLocation, diagnostics)) {
                structurallyValid.add(entry.getKey());
            } else {
                valid = false;
            }
        }
        return valid;
    }

    /** Appends missing required properties in descriptor declaration order. */
    private static void validateRequired(
            Map<String, ProjectValue> authored,
            Map<String, PropertyDescriptor> descriptors,
            String location,
            ValidationDiagnostics diagnostics) {
        for (PropertyDescriptor descriptor : descriptors.values()) {
            if (descriptor.isRequired() && !authored.containsKey(descriptor.id())) {
                diagnostics.error(
                        PropertyValidationDiagnosticCode.REQUIRED,
                        propertyLocation(location, descriptor.id()),
                        "required property is absent",
                        descriptor.id());
            }
        }
    }

    /** Builds effective values for whole-owner rules without writing defaults into authored state. */
    private static Map<String, ProjectValue> effective(
            Map<String, ProjectValue> authored, Map<String, PropertyDescriptor> descriptors) {
        Map<String, ProjectValue> result = new LinkedHashMap<>();
        for (PropertyDescriptor descriptor : descriptors.values()) {
            descriptor.defaultValue().ifPresent(value -> result.put(descriptor.id(), value));
        }
        result.putAll(authored);
        return Collections.unmodifiableMap(result);
    }

    /** Applies contextual target lookup after core and owner-type validation. */
    private static void validateTargets(
            Map<String, ProjectValue> authored,
            Set<String> structurallyValid,
            PropertyTargetLookup targets,
            String location,
            ValidationDiagnostics diagnostics) {
        for (Map.Entry<String, ProjectValue> entry : authored.entrySet()) {
            if (structurallyValid.contains(entry.getKey())) {
                validateTargetValue(
                        entry.getKey(),
                        entry.getValue(),
                        targets,
                        propertyLocation(location, entry.getKey()),
                        diagnostics);
            }
        }
    }

    /** Recursively validates targets while retaining exact nested JSON locations. */
    private static void validateTargetValue(
            String propertyId,
            ProjectValue value,
            PropertyTargetLookup targets,
            String location,
            ValidationDiagnostics diagnostics) {
        switch (value) {
            case ProjectValue.EntityTargetValue target -> {
                if (!targets.containsEntity(target.entity())) {
                    invalidTarget(propertyId, target.entity().toString(), location, diagnostics);
                }
            }
            case ProjectValue.ComponentTargetValue target ->
                validateComponentTarget(propertyId, target.target(), targets, location, diagnostics);
            case ProjectValue.ArrayValue array -> {
                for (int index = 0; index < array.values().size(); index++) {
                    validateTargetValue(
                            propertyId, array.values().get(index), targets, location + "/" + index, diagnostics);
                }
            }
            case ProjectValue.ObjectValue object ->
                object.values()
                        .forEach((key, nested) -> validateTargetValue(
                                propertyId,
                                nested,
                                targets,
                                location + "/" + JsonPointers.escapeSegment(key),
                                diagnostics));
            default -> {
                // Other portable values contain no definition-local target identity.
            }
        }
    }

    /** Validates one component target through the definition-local lookup. */
    private static void validateComponentTarget(
            String propertyId,
            ComponentTarget target,
            PropertyTargetLookup targets,
            String location,
            ValidationDiagnostics diagnostics) {
        if (!targets.containsComponent(target)) {
            invalidTarget(propertyId, target.toString(), location, diagnostics);
        }
    }

    /** Adds one structured contextual target error. */
    private static void invalidTarget(
            String propertyId, String target, String location, ValidationDiagnostics diagnostics) {
        diagnostics.error(
                PropertyValidationDiagnosticCode.TARGET,
                location,
                "target does not resolve in the current authored definition scope",
                propertyId,
                target);
    }

    /** Copies a caller map while retaining iteration order and rejecting null entries. */
    private static Map<String, ProjectValue> immutableStrings(Map<String, ProjectValue> authored) {
        Map<String, ProjectValue> result = new LinkedHashMap<>();
        Objects.requireNonNull(authored, "authored")
                .forEach((key, value) -> result.put(
                        Objects.requireNonNull(key, "authored key"), Objects.requireNonNull(value, "authored value")));
        return Collections.unmodifiableMap(result);
    }

    /** Returns a child JSON Pointer for one property identity. */
    private static String propertyLocation(String location, String propertyId) {
        return location + "/" + JsonPointers.escapeSegment(propertyId);
    }
}
