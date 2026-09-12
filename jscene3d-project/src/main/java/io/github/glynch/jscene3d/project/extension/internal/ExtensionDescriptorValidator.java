/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isProjectId;
import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isRegisteredTypeId;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.configuration.SettingChoice;
import io.github.glynch.jscene3d.configuration.SettingConstraints;
import io.github.glynch.jscene3d.configuration.SettingDefinition;
import io.github.glynch.jscene3d.configuration.SettingKey;
import io.github.glynch.jscene3d.configuration.SettingPathKind;
import io.github.glynch.jscene3d.configuration.SettingScope;
import io.github.glynch.jscene3d.configuration.SettingValueType;
import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentMultiplicity;
import io.github.glynch.jscene3d.project.component.ComponentSpatialDomain;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDiagnosticCode;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.FieldDiagnosticCodes;
import io.github.glynch.jscene3d.project.internal.SemanticVersion;
import io.github.glynch.jscene3d.project.internal.SemanticVersionRequirement;
import io.github.glynch.jscene3d.project.internal.ValidationContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Converts nullable extension JSON into safe immutable descriptor metadata. */
public final class ExtensionDescriptorValidator {
    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_URI = "https://jscene3d.org/schemas/extension-1.json";

    private final SemanticVersion engineVersion;
    private final DiagnosticCollector diagnostics;
    private final ValidationContext fields;
    private final ProjectValueDecoder values;

    /** Stores one validation context. */
    private ExtensionDescriptorValidator(URI source, SemanticVersion engineVersion) {
        this.engineVersion = engineVersion;
        diagnostics = new DiagnosticCollector(source);
        fields = new ValidationContext(
                diagnostics,
                new FieldDiagnosticCodes(
                        ExtensionDiagnosticCode.FIELD_REQUIRED,
                        ExtensionDiagnosticCode.FIELD_BLANK,
                        ExtensionDiagnosticCode.FIELD_IDENTIFIER_INVALID,
                        ExtensionDiagnosticCode.FIELD_TYPE_INVALID));
        values = ProjectValueDecoder.plain();
    }

    /**
     * Validates one raw extension descriptor.
     *
     * @param raw nullable deserialization model
     * @param source absolute descriptor resource URI
     * @param engineVersion running engine version
     * @return validated descriptor and ordered diagnostics
     */
    public static ValidationResult validate(RawExtensionDescriptor raw, URI source, SemanticVersion engineVersion) {
        ExtensionDescriptorValidator validator = new ExtensionDescriptorValidator(source, engineVersion);
        Optional<ExtensionDescriptor> descriptor = validator.validate(raw);
        return new ValidationResult(descriptor, validator.diagnostics.diagnostics());
    }

    /** Runs validation in stable descriptor order. */
    private Optional<ExtensionDescriptor> validate(RawExtensionDescriptor raw) {
        validateSchema(raw.schema(), raw.schemaVersion());
        String id = fields.requiredText(raw.id(), "/id");
        if (!id.isEmpty() && !isProjectId(id)) {
            diagnostics.error(
                    ExtensionDiagnosticCode.ID_INVALID, "id must be a lowercase reverse-domain identifier", "/id");
        }
        String version = fields.requiredText(raw.version(), "/version");
        if (!version.isEmpty() && SemanticVersion.parse(version).isEmpty()) {
            diagnostics.error(
                    ExtensionDiagnosticCode.VERSION_INVALID, "version must be a semantic version", "/version");
        }
        String engineRequires = fields.requiredText(raw.engineRequires(), "/engineRequires");
        validateEngineRequirement(engineRequires);
        String displayName = fields.requiredText(raw.displayName(), "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), "/description");
        String safeId = isProjectId(id) ? id : "invalid.extension";
        List<RegisteredTypeDescriptor> types = validateTypes(raw.types(), safeId);
        List<ComponentTypeDescriptor> components = validateComponents(raw.components(), safeId);
        List<SettingDefinition<?>> settings = validateSettings(raw.settings(), safeId, displayName);
        validateCrossTypeIdentities(types, components);
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        DescriptorPresentation presentation = presentation(displayName, description);
        return Optional.of(
                new ExtensionDescriptor(id, version, engineRequires, presentation, types, components, settings));
    }

    /** Validates declarative settings without executing extension code. */
    private List<SettingDefinition<?>> validateSettings(
            @Nullable List<RawExtensionDescriptor.@Nullable Setting> rawSettings,
            String extensionId,
            String extensionDisplayName) {
        if (rawSettings == null) {
            return List.of();
        }
        List<SettingDefinition<?>> settings = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < rawSettings.size(); index++) {
            String location = "/settings/" + index;
            Optional<SettingDefinition<?>> definition =
                    validateSetting(rawSettings.get(index), extensionId, extensionDisplayName, location);
            if (definition.isPresent()
                    && !unique.add(definition.orElseThrow().key().value())) {
                diagnostics.error(
                        ExtensionDiagnosticCode.SETTING_DUPLICATE,
                        "setting is duplicated: "
                                + definition.orElseThrow().key().value(),
                        location + "/key");
            } else {
                definition.ifPresent(settings::add);
            }
        }
        return List.copyOf(settings);
    }

    /** Validates one extension-owned setting declaration. */
    private Optional<SettingDefinition<?>> validateSetting(
            RawExtensionDescriptor.@Nullable Setting raw,
            String extensionId,
            String extensionDisplayName,
            String location) {
        if (raw == null) {
            diagnostics.error(ExtensionDiagnosticCode.FIELD_REQUIRED, "setting must be an object", location);
            return Optional.empty();
        }
        String key = fields.requiredText(raw.key(), location + "/key");
        SettingValueType type = settingType(raw.type(), location + "/type");
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        String category = fields.requiredText(raw.category(), location + "/category");
        SettingScope scope = settingScope(raw.scope(), location + "/scope");
        int order = raw.order() == null ? 0 : raw.order();
        Optional<Object> defaultValue = settingDefault(raw.defaultValue(), type, location + "/defaultValue");
        SettingConstraints constraints = settingConstraints(raw, type, location);
        if (key.isEmpty() || displayName.isEmpty() || category.isEmpty() || defaultValue.isEmpty()) {
            return Optional.empty();
        }
        try {
            SettingMetadata metadata = new SettingMetadata(
                    extensionId,
                    extensionDisplayName,
                    key,
                    displayName,
                    description,
                    category,
                    scope,
                    order,
                    constraints);
            return Optional.of(settingDefinition(metadata, type, defaultValue.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            ExtensionDiagnosticCode code =
                    exception.getMessage() != null && exception.getMessage().contains("setting key")
                            ? ExtensionDiagnosticCode.SETTING_KEY_INVALID
                            : ExtensionDiagnosticCode.SETTING_CONSTRAINT_INVALID;
            diagnostics.error(
                    code, Objects.requireNonNullElse(exception.getMessage(), code.defaultMessage()), location);
            return Optional.empty();
        }
    }

    /** Parses one supported setting value family. */
    private SettingValueType settingType(@Nullable String value, String location) {
        String text = fields.requiredText(value, location);
        try {
            return SettingValueType.parse(text);
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(
                    ExtensionDiagnosticCode.SETTING_TYPE_INVALID,
                    "type must be boolean, integer, number, string, path, or enum",
                    location);
            return SettingValueType.STRING;
        }
    }

    /** Parses one supported setting persistence scope. */
    private SettingScope settingScope(@Nullable String value, String location) {
        String text = fields.requiredText(value, location);
        if ("project".equals(text)) {
            return SettingScope.PROJECT;
        }
        diagnostics.error(ExtensionDiagnosticCode.SETTING_SCOPE_INVALID, "scope must currently be project", location);
        return SettingScope.PROJECT;
    }

    /** Converts a JSON default into the declared setting value family. */
    private Optional<Object> settingDefault(@Nullable JsonNode value, SettingValueType type, String location) {
        if (value == null || value.isNull()) {
            diagnostics.error(ExtensionDiagnosticCode.FIELD_REQUIRED, "defaultValue is required", location);
            return Optional.empty();
        }
        Object raw =
                switch (type) {
                    case BOOLEAN -> value.isBoolean() ? value.booleanValue() : null;
                    case INTEGER -> value.isIntegralNumber() ? value.bigIntegerValue() : null;
                    case NUMBER -> value.isNumber() ? value.decimalValue() : null;
                    case STRING, PATH, ENUM -> value.isTextual() ? value.textValue() : null;
                };
        if (raw == null) {
            diagnostics.error(
                    ExtensionDiagnosticCode.SETTING_DEFAULT_INVALID,
                    "defaultValue does not match the declared setting type",
                    location);
            return Optional.empty();
        }
        try {
            return Optional.of(type.convert(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.error(
                    ExtensionDiagnosticCode.SETTING_DEFAULT_INVALID,
                    Objects.requireNonNullElse(exception.getMessage(), "setting default is invalid"),
                    location);
            return Optional.empty();
        }
    }

    /** Builds typed constraints and reports malformed choice and path metadata. */
    private SettingConstraints settingConstraints(
            RawExtensionDescriptor.Setting raw, SettingValueType type, String location) {
        List<SettingChoice> choices = settingChoices(raw.choices(), location + "/choices");
        Optional<SettingPathKind> pathKind = settingPathKind(raw.pathKind(), location + "/pathKind");
        boolean projectRelative = raw.projectRelative() != null && raw.projectRelative();
        try {
            return new SettingConstraints(
                    Optional.ofNullable(raw.minimum()),
                    Optional.ofNullable(raw.maximum()),
                    Optional.ofNullable(raw.step()),
                    choices,
                    pathKind,
                    projectRelative);
        } catch (IllegalArgumentException exception) {
            diagnostics.error(
                    ExtensionDiagnosticCode.SETTING_CONSTRAINT_INVALID,
                    Objects.requireNonNullElse(exception.getMessage(), "setting constraints are invalid"),
                    location);
            return type == SettingValueType.ENUM
                    ? SettingConstraints.choices(List.of(new SettingChoice("invalid", "Invalid")))
                    : SettingConstraints.NONE;
        }
    }

    /** Parses choice metadata for an enumerated setting. */
    private List<SettingChoice> settingChoices(
            @Nullable List<RawExtensionDescriptor.@Nullable Choice> rawChoices, String location) {
        if (rawChoices == null) {
            return List.of();
        }
        List<SettingChoice> choices = new ArrayList<>();
        for (int index = 0; index < rawChoices.size(); index++) {
            RawExtensionDescriptor.Choice raw = rawChoices.get(index);
            String itemLocation = location + "/" + index;
            if (raw == null) {
                diagnostics.error(ExtensionDiagnosticCode.FIELD_REQUIRED, "choice must be an object", itemLocation);
                continue;
            }
            String value = fields.requiredText(raw.value(), itemLocation + "/value");
            String label = fields.requiredText(raw.label(), itemLocation + "/label");
            if (!value.isEmpty() && !label.isEmpty()) {
                choices.add(new SettingChoice(value, label));
            }
        }
        return List.copyOf(choices);
    }

    /** Parses an optional path-target presentation hint. */
    private Optional<SettingPathKind> settingPathKind(@Nullable String value, String location) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SettingPathKind.valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(
                    ExtensionDiagnosticCode.SETTING_CONSTRAINT_INVALID, "pathKind must be file or directory", location);
            return Optional.empty();
        }
    }

    /** Constructs a setting definition using the Java type assigned to its value family. */
    private static SettingDefinition<?> settingDefinition(
            SettingMetadata metadata, SettingValueType type, Object defaultValue) {
        return typedSettingDefinition(metadata, type, type.valueClass(), defaultValue);
    }

    private static <T> SettingDefinition<T> typedSettingDefinition(
            SettingMetadata metadata, SettingValueType type, Class<T> valueClass, Object defaultValue) {
        return new SettingDefinition<>(
                metadata.extensionId(),
                metadata.extensionDisplayName(),
                new SettingKey<>(metadata.key(), valueClass),
                type,
                valueClass.cast(defaultValue),
                metadata.displayName(),
                metadata.description(),
                metadata.category(),
                metadata.scope(),
                metadata.order(),
                metadata.constraints());
    }

    private record SettingMetadata(
            String extensionId,
            String extensionDisplayName,
            String key,
            String displayName,
            Optional<String> description,
            String category,
            SettingScope scope,
            int order,
            SettingConstraints constraints) {}

    /** Validates the authoritative schema version and optional canonical URI. */
    private void validateSchema(@Nullable String schema, int schemaVersion) {
        if (schemaVersion != SCHEMA_VERSION) {
            diagnostics.error(
                    ExtensionDiagnosticCode.SCHEMA_UNSUPPORTED,
                    "schemaVersion must be " + SCHEMA_VERSION + ": " + schemaVersion,
                    "/schemaVersion");
        }
        if (schema != null && !SCHEMA_URI.equals(schema)) {
            diagnostics.warning(
                    ExtensionDiagnosticCode.SCHEMA_URI_INVALID,
                    "$schema does not identify the bundled Extension Descriptor version 1 schema",
                    "/$schema");
        }
    }

    /** Validates the extension's engine requirement and current compatibility. */
    private void validateEngineRequirement(String requirementText) {
        Optional<SemanticVersionRequirement> requirement = SemanticVersionRequirement.parse(requirementText);
        if (!requirementText.isEmpty() && requirement.isEmpty()) {
            diagnostics.error(
                    ExtensionDiagnosticCode.ENGINE_REQUIREMENT_INVALID,
                    "engineRequires must contain semantic-version comparisons",
                    "/engineRequires");
        } else if (requirement.isPresent() && !requirement.orElseThrow().includes(engineVersion)) {
            diagnostics.error(
                    ExtensionDiagnosticCode.ENGINE_INCOMPATIBLE,
                    "extension requires " + requirementText + " but the current engine is incompatible",
                    "/engineRequires");
        }
    }

    /** Validates registered types and removes invalid placeholders from the public result. */
    private List<RegisteredTypeDescriptor> validateTypes(
            @Nullable List<RawExtensionDescriptor.@Nullable Type> rawTypes, String extensionId) {
        if (rawTypes == null) {
            return List.of();
        }
        List<RegisteredTypeDescriptor> types = new ArrayList<>();
        Set<RegisteredType> unique = new HashSet<>();
        for (int index = 0; index < rawTypes.size(); index++) {
            String location = "/types/" + index;
            Optional<RegisteredTypeDescriptor> descriptor = validateType(rawTypes.get(index), extensionId, location);
            if (descriptor.isPresent() && !unique.add(descriptor.orElseThrow().type())) {
                diagnostics.error(
                        ExtensionDiagnosticCode.TYPE_DUPLICATE,
                        "registered type is duplicated: "
                                + descriptor.orElseThrow().type(),
                        location);
            } else {
                descriptor.ifPresent(types::add);
            }
        }
        return List.copyOf(types);
    }

    /** Validates component types and removes invalid placeholders from the public result. */
    private List<ComponentTypeDescriptor> validateComponents(
            @Nullable List<RawExtensionDescriptor.@Nullable Component> rawComponents, String extensionId) {
        if (rawComponents == null) {
            return List.of();
        }
        List<ComponentTypeDescriptor> components = new ArrayList<>();
        Set<ComponentType> unique = new HashSet<>();
        for (int index = 0; index < rawComponents.size(); index++) {
            String location = "/components/" + index;
            Optional<ComponentTypeDescriptor> descriptor =
                    validateComponent(rawComponents.get(index), extensionId, location);
            if (descriptor.isPresent() && !unique.add(descriptor.orElseThrow().type())) {
                diagnostics.error(
                        ExtensionDiagnosticCode.TYPE_DUPLICATE,
                        "component type is duplicated: "
                                + descriptor.orElseThrow().type(),
                        location);
            } else {
                descriptor.ifPresent(components::add);
            }
        }
        return List.copyOf(components);
    }

    /** Validates one component type and its authoring and execution declarations. */
    private Optional<ComponentTypeDescriptor> validateComponent(
            RawExtensionDescriptor.@Nullable Component raw, String extensionId, String location) {
        if (raw == null) {
            diagnostics.error(ExtensionDiagnosticCode.FIELD_REQUIRED, "component must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredText(raw.id(), location + "/id");
        boolean validId = isRegisteredTypeId(id) && id.startsWith(extensionId + '/');
        if (!id.isEmpty() && !validId) {
            diagnostics.error(
                    ExtensionDiagnosticCode.TYPE_ID_INVALID,
                    "component id must be qualified by its owning extension: " + extensionId,
                    location + "/id");
        }
        int version = positiveVersion(raw.typeVersion(), location + "/typeVersion");
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        ComponentTypeDescriptor.Builder builder = ComponentTypeDescriptor.builder(
                        new ComponentType(
                                new ComponentTypeId(validId ? id : "invalid.extension/invalid"), Math.max(version, 1)),
                        presentation(displayName.isEmpty() ? "Invalid component" : displayName, description))
                .properties(validateProperties(raw.properties(), location + "/properties"))
                .signals(validateEndpoints(raw.signals(), location + "/signals"))
                .actions(validateEndpoints(raw.actions(), location + "/actions"))
                .providedCapabilities(capabilitySet(
                        raw.providedCapabilities(), location + "/providedCapabilities", "provided capability"))
                .requiredCapabilities(capabilitySet(
                        raw.requiredCapabilities(), location + "/requiredCapabilities", "required capability"))
                .attachments(attachmentSet(raw.attachments(), location + "/attachments"))
                .multiplicity(multiplicity(raw.multiplicity(), location + "/multiplicity"))
                .conflicts(componentTypeSet(raw.conflicts(), location + "/conflicts"))
                .spatialDomain(spatialDomain(raw.spatialDomain(), location + "/spatialDomain"))
                .lifecycle(lifecycleSet(raw.lifecycle(), location + "/lifecycle"))
                .updatePhases(updatePhaseSet(raw.updatePhases(), location + "/updatePhases"));
        if (!validId || version < 1 || displayName.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(builder.build());
        } catch (IllegalArgumentException exception) {
            diagnostics.error(
                    ExtensionDiagnosticCode.COMPONENT_DESCRIPTOR_INVALID,
                    Objects.requireNonNullElse(exception.getMessage(), "component descriptor is invalid"),
                    location);
            return Optional.empty();
        }
    }

    /** Reports a versioned identity declared as both a component and another extension type. */
    private void validateCrossTypeIdentities(
            List<RegisteredTypeDescriptor> types, List<ComponentTypeDescriptor> components) {
        Set<RegisteredType> identities = new HashSet<>();
        types.stream().map(RegisteredTypeDescriptor::type).forEach(identities::add);
        for (int index = 0; index < components.size(); index++) {
            ComponentType component = components.get(index).type();
            RegisteredType identity = new RegisteredType(component.id().value(), component.version());
            if (identities.contains(identity)) {
                diagnostics.error(
                        ExtensionDiagnosticCode.TYPE_DUPLICATE,
                        "type identity is used by both a component and another type: " + component,
                        "/components/" + index + "/id");
            }
        }
    }

    /** Validates one registered type. */
    private Optional<RegisteredTypeDescriptor> validateType(
            RawExtensionDescriptor.@Nullable Type raw, String extensionId, String location) {
        if (raw == null) {
            diagnostics.error(ExtensionDiagnosticCode.FIELD_REQUIRED, "registered type must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredText(raw.id(), location + "/id");
        boolean validId = isRegisteredTypeId(id) && id.startsWith(extensionId + '/');
        if (!id.isEmpty() && !validId) {
            diagnostics.error(
                    ExtensionDiagnosticCode.TYPE_ID_INVALID,
                    "type id must be qualified by its owning extension: " + extensionId,
                    location + "/id");
        }
        int version = positiveVersion(raw.typeVersion(), location + "/typeVersion");
        RegisteredTypeScope scope = scope(raw.scope(), location + "/scope");
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        List<PropertyDescriptor> properties = validateProperties(raw.properties(), location + "/properties");
        List<EndpointDescriptor> signals = validateEndpoints(raw.signals(), location + "/signals");
        List<EndpointDescriptor> actions = validateEndpoints(raw.actions(), location + "/actions");
        List<String> capabilities = registeredTypeList(raw.requiredCapabilities(), location + "/requiredCapabilities");
        if (!validId || version < 1 || displayName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RegisteredTypeDescriptor(
                new RegisteredType(id, version),
                scope,
                presentation(displayName, description),
                properties,
                signals,
                actions,
                capabilities));
    }

    /** Validates property descriptors. */
    private List<PropertyDescriptor> validateProperties(
            @Nullable List<RawExtensionDescriptor.@Nullable Property> rawProperties, String location) {
        if (rawProperties == null) {
            return List.of();
        }
        List<PropertyDescriptor> properties = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < rawProperties.size(); index++) {
            Optional<PropertyDescriptor> property = validateProperty(rawProperties.get(index), location + "/" + index);
            if (property.isPresent() && !unique.add(property.orElseThrow().id())) {
                diagnostics.error(
                        ExtensionDiagnosticCode.PROPERTY_DUPLICATE,
                        "property is duplicated: " + property.orElseThrow().id(),
                        location + "/" + index + "/id");
            } else {
                property.ifPresent(properties::add);
            }
        }
        return List.copyOf(properties);
    }

    /** Validates one property descriptor. */
    private Optional<PropertyDescriptor> validateProperty(
            RawExtensionDescriptor.@Nullable Property raw, String location) {
        if (raw == null) {
            diagnostics.error(ExtensionDiagnosticCode.FIELD_REQUIRED, "property must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredLocalId(raw.id(), location + "/id");
        ProjectValueKind valueKind = valueKind(raw.valueKind(), location + "/valueKind");
        Optional<ProjectValueKind> elementKind = elementKind(raw.elementKind(), valueKind, location + "/elementKind");
        boolean required = raw.required() != null && raw.required();
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        Map<String, ProjectValue> editor = objectValues(raw.editor(), location + "/editor");
        Set<ResourceReference.Kind> acceptedReferences =
                referenceKinds(raw.acceptedReferences(), valueKind, location + "/acceptedReferences");
        Optional<ProjectValue> defaultValue = optionalValue(raw.defaultValue(), location + "/defaultValue");
        if (defaultValue.isPresent()
                && !accepts(valueKind, elementKind, acceptedReferences, defaultValue.orElseThrow())) {
            diagnostics.error(
                    ExtensionDiagnosticCode.PROPERTY_DEFAULT_INVALID,
                    "defaultValue does not satisfy valueKind and acceptedReferences",
                    location + "/defaultValue");
            defaultValue = Optional.empty();
        }
        if (required && defaultValue.isPresent()) {
            diagnostics.error(
                    ExtensionDiagnosticCode.PROPERTY_REQUIRED_DEFAULT,
                    "a required property cannot also declare a defaultValue",
                    location);
        }
        if (id.isEmpty() || displayName.isEmpty()) {
            return Optional.empty();
        }
        DescriptorPresentation metadata = presentation(displayName, description);
        if (elementKind.isPresent()) {
            return Optional.of(arrayProperty(id, elementKind.orElseThrow(), required, defaultValue, metadata, editor));
        }
        if (required) {
            return Optional.of(PropertyDescriptor.required(id, valueKind, metadata, editor, acceptedReferences));
        }
        if (defaultValue.isPresent()) {
            return Optional.of(PropertyDescriptor.optionalWithDefault(
                    id, valueKind, defaultValue.orElseThrow(), metadata, editor, acceptedReferences));
        }
        return Optional.of(PropertyDescriptor.optional(id, valueKind, metadata, editor, acceptedReferences));
    }

    /** Creates the declared homogeneous array variant after common validation. */
    private static PropertyDescriptor arrayProperty(
            String id,
            ProjectValueKind elementKind,
            boolean required,
            Optional<ProjectValue> defaultValue,
            DescriptorPresentation presentation,
            Map<String, ProjectValue> editor) {
        if (required) {
            return PropertyDescriptor.requiredArray(id, elementKind, presentation, editor);
        }
        if (defaultValue.isPresent()) {
            ProjectValue.ArrayValue array = (ProjectValue.ArrayValue) defaultValue.orElseThrow();
            return PropertyDescriptor.optionalArrayWithDefault(id, elementKind, array, presentation, editor);
        }
        return PropertyDescriptor.optionalArray(id, elementKind, presentation, editor);
    }

    /** Validates signal or action descriptors. */
    private List<EndpointDescriptor> validateEndpoints(
            @Nullable List<RawExtensionDescriptor.@Nullable Endpoint> rawEndpoints, String location) {
        if (rawEndpoints == null) {
            return List.of();
        }
        List<EndpointDescriptor> endpoints = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < rawEndpoints.size(); index++) {
            Optional<EndpointDescriptor> endpoint = validateEndpoint(rawEndpoints.get(index), location + "/" + index);
            if (endpoint.isPresent() && !unique.add(endpoint.orElseThrow().id())) {
                diagnostics.error(
                        ExtensionDiagnosticCode.ENDPOINT_DUPLICATE,
                        "endpoint is duplicated: " + endpoint.orElseThrow().id(),
                        location + "/" + index + "/id");
            } else {
                endpoint.ifPresent(endpoints::add);
            }
        }
        return List.copyOf(endpoints);
    }

    /** Validates one signal or action descriptor. */
    private Optional<EndpointDescriptor> validateEndpoint(
            RawExtensionDescriptor.@Nullable Endpoint raw, String location) {
        if (raw == null) {
            diagnostics.error(ExtensionDiagnosticCode.FIELD_REQUIRED, "endpoint must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredLocalId(raw.id(), location + "/id");
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        Optional<RegisteredType> payload = registeredType(raw.payload(), location + "/payload");
        if (id.isEmpty() || displayName.isEmpty()) {
            return Optional.empty();
        }
        DescriptorPresentation metadata = presentation(displayName, description);
        return Optional.of(
                payload.isPresent()
                        ? EndpointDescriptor.withPayload(id, payload.orElseThrow(), metadata)
                        : EndpointDescriptor.withoutPayload(id, metadata));
    }

    /** Validates one optional registered payload type. */
    private Optional<RegisteredType> registeredType(
            RawExtensionDescriptor.@Nullable RegisteredType raw, String location) {
        if (raw == null) {
            return Optional.empty();
        }
        String id = fields.requiredText(raw.type(), location + "/type");
        if (!id.isEmpty() && !isRegisteredTypeId(id)) {
            diagnostics.error(
                    ExtensionDiagnosticCode.ENDPOINT_PAYLOAD_INVALID,
                    "payload type must be extension-qualified",
                    location + "/type");
        }
        int version = positiveVersion(raw.typeVersion(), location + "/typeVersion");
        return isRegisteredTypeId(id) && version > 0 ? Optional.of(new RegisteredType(id, version)) : Optional.empty();
    }

    /** Validates one positive type version. */
    private int positiveVersion(@Nullable Integer version, String location) {
        int value = version == null ? 0 : version;
        if (value < 1) {
            diagnostics.error(ExtensionDiagnosticCode.TYPE_VERSION_INVALID, "typeVersion must be positive", location);
        }
        return value;
    }

    /** Parses one registered type scope. */
    private RegisteredTypeScope scope(@Nullable String value, String location) {
        String text = fields.requiredText(value, location);
        try {
            return RegisteredTypeScope.valueOf(text.replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(
                    ExtensionDiagnosticCode.TYPE_SCOPE_INVALID,
                    "scope must be scene-node, node-controller, project-system, resource, or importer",
                    location);
            return RegisteredTypeScope.RESOURCE;
        }
    }

    /** Parses one portable project-value kind. */
    private ProjectValueKind valueKind(@Nullable String value, String location) {
        String text = fields.requiredText(value, location);
        try {
            return ProjectValueKind.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(
                    ExtensionDiagnosticCode.PROPERTY_KIND_INVALID,
                    "valueKind must be null, boolean, number, text, array, object, reference, entity_target, or component_target",
                    location);
            return ProjectValueKind.NULL;
        }
    }

    /** Parses an optional homogeneous array-element kind. */
    private Optional<ProjectValueKind> elementKind(
            @Nullable String value, ProjectValueKind valueKind, String location) {
        if (value == null) {
            return Optional.empty();
        }
        ProjectValueKind parsed;
        try {
            parsed = ProjectValueKind.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(
                    ExtensionDiagnosticCode.PROPERTY_KIND_INVALID,
                    "elementKind must be null, boolean, number, text, array, object, reference, entity_target, or component_target",
                    location);
            return Optional.empty();
        }
        if (valueKind != ProjectValueKind.ARRAY) {
            diagnostics.error(
                    ExtensionDiagnosticCode.PROPERTY_KIND_INVALID, "elementKind requires valueKind array", location);
            return Optional.empty();
        }
        return Optional.of(parsed);
    }

    /** Parses optional reference namespace constraints. */
    private Set<ResourceReference.Kind> referenceKinds(
            @Nullable List<@Nullable String> values, ProjectValueKind valueKind, String location) {
        if (values == null) {
            return Set.of();
        }
        Set<ResourceReference.Kind> result = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String value = fields.requiredText(values.get(index), location + "/" + index);
            try {
                ResourceReference.Kind kind = ResourceReference.Kind.valueOf(value.toUpperCase(Locale.ROOT));
                if (!result.add(kind)) {
                    diagnostics.error(
                            ExtensionDiagnosticCode.PROPERTY_REFERENCE_DUPLICATE,
                            "accepted reference namespace is duplicated: " + value,
                            location + "/" + index);
                }
            } catch (IllegalArgumentException ignored) {
                diagnostics.error(
                        ExtensionDiagnosticCode.PROPERTY_REFERENCE_KIND_INVALID,
                        "accepted reference namespace must be project, asset, or import",
                        location + "/" + index);
            }
        }
        if (valueKind != ProjectValueKind.REFERENCE && !result.isEmpty()) {
            diagnostics.error(
                    ExtensionDiagnosticCode.PROPERTY_REFERENCE_KIND_INVALID,
                    "acceptedReferences require valueKind reference",
                    location);
            return Set.of();
        }
        return Collections.unmodifiableSet(result);
    }

    /** Validates an ordered list of extension-qualified type identifiers. */
    private List<String> registeredTypeList(@Nullable List<@Nullable String> values, String location) {
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String value = fields.requiredText(values.get(index), location + "/" + index);
            if (!value.isEmpty() && !isRegisteredTypeId(value)) {
                diagnostics.error(
                        ExtensionDiagnosticCode.CAPABILITY_ID_INVALID,
                        "capability must be an extension-qualified identifier",
                        location + "/" + index);
            } else if (!value.isEmpty() && !unique.add(value)) {
                diagnostics.error(
                        ExtensionDiagnosticCode.CAPABILITY_DUPLICATE,
                        "required capability is duplicated: " + value,
                        location + "/" + index);
            } else if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    /** Parses an optional component multiplicity, defaulting to one instance per entity. */
    private ComponentMultiplicity multiplicity(@Nullable String value, String location) {
        if (value == null) {
            return ComponentMultiplicity.SINGLE;
        }
        return enumValue(
                value,
                ComponentMultiplicity.class,
                ComponentMultiplicity.SINGLE,
                ExtensionDiagnosticCode.COMPONENT_MULTIPLICITY_INVALID,
                "multiplicity must be single or multiple",
                location);
    }

    /** Parses an optional component spatial domain, defaulting to non-spatial. */
    private ComponentSpatialDomain spatialDomain(@Nullable String value, String location) {
        if (value == null) {
            return ComponentSpatialDomain.NONE;
        }
        return enumValue(
                value,
                ComponentSpatialDomain.class,
                ComponentSpatialDomain.NONE,
                ExtensionDiagnosticCode.COMPONENT_SPATIAL_DOMAIN_INVALID,
                "spatialDomain must be none, three-dimensional, two-dimensional, or user-interface",
                location);
    }

    /** Parses unique component lifecycle declarations. */
    private Set<ComponentLifecycle> lifecycleSet(@Nullable List<@Nullable String> values, String location) {
        return enumSet(
                values,
                ComponentLifecycle.class,
                ExtensionDiagnosticCode.COMPONENT_LIFECYCLE_INVALID,
                "lifecycle value must be created, activated, deactivated, or destroyed",
                location);
    }

    /** Parses unique component update-phase declarations. */
    private Set<ComponentUpdatePhase> updatePhaseSet(@Nullable List<@Nullable String> values, String location) {
        return enumSet(
                values,
                ComponentUpdatePhase.class,
                ExtensionDiagnosticCode.COMPONENT_UPDATE_PHASE_INVALID,
                "update phase must be before-physics, after-physics, or frame-update",
                location);
    }

    /** Parses unique extension-qualified capability identities. */
    private Set<CapabilityId> capabilitySet(
            @Nullable List<@Nullable String> values, String location, String description) {
        if (values == null) {
            return Set.of();
        }
        Set<CapabilityId> result = new LinkedHashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemLocation = location + "/" + index;
            String value = fields.requiredText(values.get(index), itemLocation);
            if (!value.isEmpty() && !isRegisteredTypeId(value)) {
                diagnostics.error(
                        ExtensionDiagnosticCode.CAPABILITY_ID_INVALID,
                        description + " must be an extension-qualified identifier",
                        itemLocation);
            } else if (!value.isEmpty() && !result.add(new CapabilityId(value))) {
                diagnostics.error(
                        ExtensionDiagnosticCode.CAPABILITY_DUPLICATE,
                        description + " is duplicated: " + value,
                        itemLocation);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Parses unique local attachment identities. */
    private Set<AttachmentPointId> attachmentSet(@Nullable List<@Nullable String> values, String location) {
        if (values == null) {
            return Set.of();
        }
        Set<AttachmentPointId> result = new LinkedHashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemLocation = location + "/" + index;
            String value = fields.requiredLocalId(values.get(index), itemLocation);
            if (!value.isEmpty() && !result.add(new AttachmentPointId(value))) {
                diagnostics.error(
                        ExtensionDiagnosticCode.COMPONENT_ATTACHMENT_DUPLICATE,
                        "component attachment is duplicated: " + value,
                        itemLocation);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Parses unique extension-qualified conflicting component types. */
    private Set<ComponentTypeId> componentTypeSet(@Nullable List<@Nullable String> values, String location) {
        if (values == null) {
            return Set.of();
        }
        Set<ComponentTypeId> result = new LinkedHashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemLocation = location + "/" + index;
            String value = fields.requiredText(values.get(index), itemLocation);
            if (!value.isEmpty() && !isRegisteredTypeId(value)) {
                diagnostics.error(
                        ExtensionDiagnosticCode.COMPONENT_CONFLICT_INVALID,
                        "conflicting component must be an extension-qualified identifier",
                        itemLocation);
            } else if (!value.isEmpty() && !result.add(new ComponentTypeId(value))) {
                diagnostics.error(
                        ExtensionDiagnosticCode.COMPONENT_CONFLICT_DUPLICATE,
                        "conflicting component is duplicated: " + value,
                        itemLocation);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Parses one serialized enum value and reports a focused diagnostic on failure. */
    private <E extends Enum<E>> E enumValue(
            String value, Class<E> type, E fallback, ExtensionDiagnosticCode code, String message, String location) {
        try {
            return Enum.valueOf(type, value.replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(code, message, location);
            return fallback;
        }
    }

    /** Parses a unique set of serialized enum values. */
    private <E extends Enum<E>> Set<E> enumSet(
            @Nullable List<@Nullable String> values,
            Class<E> type,
            ExtensionDiagnosticCode code,
            String message,
            String location) {
        if (values == null) {
            return Set.of();
        }
        Set<E> result = new LinkedHashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemLocation = location + "/" + index;
            String value = fields.requiredText(values.get(index), itemLocation);
            if (value.isEmpty()) {
                continue;
            }
            try {
                E parsed = Enum.valueOf(type, value.replace('-', '_').toUpperCase(Locale.ROOT));
                if (!result.add(parsed)) {
                    diagnostics.error(code, "value is duplicated: " + value, itemLocation);
                }
            } catch (IllegalArgumentException ignored) {
                diagnostics.error(code, message, itemLocation);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Returns whether a default value satisfies property constraints. */
    private static boolean accepts(
            ProjectValueKind kind,
            Optional<ProjectValueKind> elementKind,
            Set<ResourceReference.Kind> referenceKinds,
            ProjectValue value) {
        if (ProjectValueKind.of(value) != kind) {
            return false;
        }
        if (value instanceof ProjectValue.ReferenceValue reference) {
            return referenceKinds.isEmpty()
                    || referenceKinds.contains(reference.reference().kind());
        }
        return !(value instanceof ProjectValue.ArrayValue array)
                || elementKind.isEmpty()
                || array.values().stream()
                        .allMatch(element -> ProjectValueKind.of(element) == elementKind.orElseThrow());
    }

    /** Converts an optional raw JSON value without interpreting resource references. */
    private Optional<ProjectValue> optionalValue(@Nullable JsonNode raw, String location) {
        return raw == null ? Optional.empty() : Optional.of(values.decode(raw, location));
    }

    /** Converts an optional JSON object to generic editor metadata. */
    private Map<String, ProjectValue> objectValues(@Nullable JsonNode raw, String location) {
        if (raw == null) {
            return Map.of();
        }
        if (!raw.isObject()) {
            diagnostics.error(ExtensionDiagnosticCode.EDITOR_NOT_OBJECT, "editor metadata must be an object", location);
            return Map.of();
        }
        return values.decodeObject(raw, location).values();
    }

    /** Builds presentation metadata from already validated values. */
    private static DescriptorPresentation presentation(String displayName, Optional<String> description) {
        return description.isPresent()
                ? DescriptorPresentation.described(displayName, description.orElseThrow())
                : DescriptorPresentation.named(displayName);
    }

    /** Validated extension descriptor and ordered diagnostics.
     *
     * @param descriptor validated descriptor when no errors occurred
     * @param diagnostics immutable ordered diagnostics
     */
    public record ValidationResult(Optional<ExtensionDescriptor> descriptor, List<ProjectDiagnostic> diagnostics) {}
}
