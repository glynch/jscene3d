/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for reusable project resources. */
public enum ResourceDiagnosticCode implements DiagnosticCode {
    /** The registered-type catalog is invalid. */
    CATALOG_INVALID("resource.catalog", "The registered-type catalog is invalid"),
    /** A required resource property is missing. */
    PROPERTY_REQUIRED("resource.catalog.property.required", "A required resource property is missing"),
    /** A resource property is undeclared. */
    PROPERTY_UNKNOWN("resource.catalog.property.unknown", "A resource property is not declared"),
    /** A resource property value is invalid. */
    PROPERTY_VALUE_INVALID("resource.catalog.property.value", "A resource property value is invalid"),
    /** A registered resource type is missing. */
    TYPE_MISSING("resource.catalog.type.missing", "The registered resource type was not found"),
    /** A registered type has the wrong scope. */
    TYPE_SCOPE_INVALID("resource.catalog.type.scope", "The registered type is not a resource type"),
    /** A resource file is missing. */
    FILE_MISSING("resource.file.missing", "The resource does not exist or is not a regular file"),
    /** A resource file cannot be read. */
    FILE_READ_FAILED("resource.file.read", "The resource could not be read"),
    /** A required resource field is missing. */
    FIELD_REQUIRED("resource.field.required", "A required resource value is missing"),
    /** An optional resource field is blank. */
    FIELD_BLANK("resource.field.blank", "An optional resource value must not be blank"),
    /** A resource identifier field is invalid. */
    FIELD_IDENTIFIER_INVALID("resource.field.identifier", "A resource value must be a portable lowercase identifier"),
    /** A resource type field is invalid. */
    FIELD_TYPE_INVALID("resource.field.type", "A resource value must be a registered-type identifier"),
    /** A resource document is invalid JSON. */
    JSON_INVALID("resource.json", "The resource is not valid JScene3D Resource JSON"),
    /** A resource path escapes the project directory. */
    PATH_ESCAPES_PROJECT("resource.path.escape", "The resource resolves outside the project directory"),
    /** A resource path is absolute. */
    PATH_ABSOLUTE("resource.path.absolute", "A resource path must be relative"),
    /** A resource path is invalid. */
    PATH_INVALID("resource.path.invalid", "A resource path is invalid"),
    /** A referenced resource path is missing. */
    PATH_MISSING("resource.path.missing", "A referenced resource path does not exist"),
    /** A resource path is not portable. */
    PATH_PORTABILITY_INVALID("resource.path.portable", "A resource path must use forward slashes"),
    /** A resource path cannot be resolved. */
    PATH_READ_FAILED("resource.path.read", "A resource path could not be resolved"),
    /** A resource reference object is invalid. */
    REFERENCE_OBJECT_INVALID("resource.reference.object", "A resource reference must be a valid reference object"),
    /** A resource-reference scheme is unsupported. */
    REFERENCE_SCHEME_UNSUPPORTED("resource.reference.scheme", "A resource reference scheme is unsupported"),
    /** A resource-reference locator is blank. */
    REFERENCE_LOCATOR_BLANK("resource.reference.locator", "A resource reference locator must not be blank"),
    /** An asset reference is invalid. */
    REFERENCE_ASSET_INVALID("resource.reference.asset", "An asset reference is invalid"),
    /** An asset reference names an undeclared asset. */
    REFERENCE_ASSET_MISSING("resource.reference.asset.missing", "An asset reference identifies an undeclared asset"),
    /** An imported-resource reference is invalid. */
    REFERENCE_IMPORT_INVALID("resource.reference.import", "An imported-resource reference is invalid"),
    /** The resource schema is unsupported. */
    SCHEMA_UNSUPPORTED("resource.schema.unsupported", "The resource schema version is unsupported"),
    /** The resource schema URI is invalid. */
    SCHEMA_URI_INVALID("resource.schema.uri", "The resource schema URI is invalid"),
    /** A resource type identifier is invalid. */
    TYPE_ID_INVALID("resource.type.identifier", "The resource type identifier is invalid"),
    /** A resource type version is invalid. */
    TYPE_VERSION_INVALID("resource.type.version", "The resource type version is invalid"),
    /** Resource properties are not an object. */
    VALUE_NOT_OBJECT("resource.value.object", "The resource properties must be an object");

    private final String value;
    private final String message;

    ResourceDiagnosticCode(String value, String message) {
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
