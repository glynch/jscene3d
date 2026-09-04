/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for extension descriptors and discovery. */
public enum ExtensionDiagnosticCode implements DiagnosticCode {
    /** An extension capability is duplicated. */
    CAPABILITY_DUPLICATE("extension.capability.duplicate", "An extension capability is duplicated"),
    /** A capability identifier is invalid. */
    CAPABILITY_ID_INVALID("extension.capability.id", "An extension capability identifier is invalid"),
    /** Extension descriptors cannot be enumerated. */
    DISCOVERY_READ_FAILED("extension.discovery.read", "Extension descriptors could not be enumerated"),
    /** An extension descriptor is duplicated. */
    DESCRIPTOR_DUPLICATE("extension.duplicate", "Multiple descriptors declare the same extension"),
    /** An editor contribution is not an object. */
    EDITOR_NOT_OBJECT("extension.editor.object", "The editor contribution must be an object"),
    /** An endpoint is duplicated. */
    ENDPOINT_DUPLICATE("extension.endpoint.duplicate", "An extension endpoint is duplicated"),
    /** An endpoint payload type is invalid. */
    ENDPOINT_PAYLOAD_INVALID("extension.endpoint.payload", "An endpoint payload type is invalid"),
    /** The extension is incompatible with the engine. */
    ENGINE_INCOMPATIBLE(
            "extension.engine.incompatible", "The running engine version is incompatible with the extension"),
    /** An extension engine requirement is invalid. */
    ENGINE_REQUIREMENT_INVALID("extension.engine.requirement", "The extension engine requirement is invalid"),
    /** A required extension field is missing. */
    FIELD_REQUIRED("extension.field.required", "A required extension value is missing"),
    /** An optional extension field is blank. */
    FIELD_BLANK("extension.field.blank", "An optional extension value must not be blank"),
    /** An extension identifier field is invalid. */
    FIELD_IDENTIFIER_INVALID(
            "extension.field.identifier", "An extension value must be a portable lowercase identifier"),
    /** An extension type field is invalid. */
    FIELD_TYPE_INVALID("extension.field.type", "An extension value must be a registered-type identifier"),
    /** An extension identifier is invalid. */
    ID_INVALID("extension.id", "The extension identifier is invalid"),
    /** An extension descriptor is invalid JSON. */
    JSON_INVALID("extension.json", "The extension descriptor is not valid JSON"),
    /** A declared extension is missing. */
    MISSING("extension.missing", "A declared extension was not discovered"),
    /** A property default is invalid. */
    PROPERTY_DEFAULT_INVALID("extension.property.default", "A property default value is invalid"),
    /** A registered property is duplicated. */
    PROPERTY_DUPLICATE("extension.property.duplicate", "A registered property is duplicated"),
    /** A registered property kind is invalid. */
    PROPERTY_KIND_INVALID("extension.property.kind", "A registered property kind is invalid"),
    /** A property reference kind is duplicated. */
    PROPERTY_REFERENCE_DUPLICATE("extension.property.reference-duplicate", "A property reference kind is duplicated"),
    /** A property reference kind is invalid. */
    PROPERTY_REFERENCE_KIND_INVALID("extension.property.reference-kind", "A property reference kind is invalid"),
    /** A required property lacks a default. */
    PROPERTY_REQUIRED_DEFAULT("extension.property.required-default", "A required property must declare a default"),
    /** An extension descriptor cannot be read. */
    READ_FAILED("extension.read", "The extension descriptor could not be read"),
    /** An extension resource URI is invalid. */
    RESOURCE_URI_INVALID("extension.resource.uri", "The extension descriptor resource URI is invalid"),
    /** The extension schema is unsupported. */
    SCHEMA_UNSUPPORTED("extension.schema.unsupported", "The extension schema version is unsupported"),
    /** The extension schema URI is invalid. */
    SCHEMA_URI_INVALID("extension.schema.uri", "The extension schema URI is invalid"),
    /** A registered extension type is duplicated. */
    TYPE_DUPLICATE("extension.type.duplicate", "A registered extension type is duplicated"),
    /** A registered extension type identifier is invalid. */
    TYPE_ID_INVALID("extension.type.id", "A registered extension type identifier is invalid"),
    /** A registered extension type scope is invalid. */
    TYPE_SCOPE_INVALID("extension.type.scope", "A registered extension type scope is invalid"),
    /** A registered extension type version is invalid. */
    TYPE_VERSION_INVALID("extension.type.version", "A registered extension type version is invalid"),
    /** An extension version is invalid. */
    VERSION_INVALID("extension.version", "The extension version is invalid"),
    /** A discovered extension version is incompatible. */
    VERSION_INCOMPATIBLE("extension.version.incompatible", "The discovered extension version is incompatible");

    private final String value;
    private final String message;

    ExtensionDiagnosticCode(String value, String message) {
        this.value = value;
        this.message = message;
    }

    @Override
    public String code() {
        return value;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
