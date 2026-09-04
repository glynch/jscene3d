/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for scene loading and validation. */
public enum SceneDiagnosticCode implements DiagnosticCode {
    /** The registered-type catalog is invalid. */
    CATALOG_INVALID("scene.catalog", "The registered-type catalog is invalid"),
    /** A required node property is missing. */
    PROPERTY_REQUIRED("scene.catalog.property.required", "A required scene-node property is missing"),
    /** A node property is undeclared. */
    PROPERTY_UNKNOWN("scene.catalog.property.unknown", "A scene-node property is not declared"),
    /** A node property value is invalid. */
    PROPERTY_VALUE_INVALID("scene.catalog.property.value", "A scene-node property value is invalid"),
    /** An action endpoint is ambiguous. */
    ACTION_AMBIGUOUS("scene.catalog.action.ambiguous", "The action endpoint is ambiguous"),
    /** An action endpoint is missing. */
    ACTION_MISSING("scene.catalog.action.missing", "The action endpoint was not found"),
    /** Connected endpoint payload types are incompatible. */
    CONNECTION_PAYLOAD_INCOMPATIBLE(
            "scene.catalog.connection.payload", "Signal and action payload types are incompatible"),
    /** A scene-instance endpoint is invalid. */
    INSTANCE_ENDPOINT_INVALID("scene.catalog.instance.endpoint", "The scene-instance endpoint is invalid"),
    /** A signal endpoint is ambiguous. */
    SIGNAL_AMBIGUOUS("scene.catalog.signal.ambiguous", "The signal endpoint is ambiguous"),
    /** A signal endpoint is missing. */
    SIGNAL_MISSING("scene.catalog.signal.missing", "The signal endpoint was not found"),
    /** A registered node type is missing. */
    TYPE_MISSING("scene.catalog.type.missing", "The registered scene-node type was not found"),
    /** A registered type has the wrong scope. */
    TYPE_SCOPE_INVALID("scene.catalog.type.scope", "The registered type is not a scene-node type"),
    /** A scene connection is duplicated. */
    CONNECTION_DUPLICATE("scene.connection.duplicate", "The scene connection is duplicated"),
    /** A connection references a missing node. */
    CONNECTION_NODE_MISSING("scene.connection.node", "The connection references an unknown node"),
    /** A required scene field is missing. */
    FIELD_REQUIRED("scene.field.required", "A required scene value is missing"),
    /** An optional scene field is blank. */
    FIELD_BLANK("scene.field.blank", "An optional scene value must not be blank"),
    /** A scene identifier field is invalid. */
    FIELD_IDENTIFIER_INVALID("scene.field.identifier", "A scene value must be a portable lowercase identifier"),
    /** A scene type field is invalid. */
    FIELD_TYPE_INVALID("scene.field.type", "A scene value must be a registered-type identifier"),
    /** A scene file is missing. */
    FILE_MISSING("scene.file.missing", "The scene does not exist or is not a regular file"),
    /** A scene file cannot be read. */
    FILE_READ_FAILED("scene.file.read", "The scene could not be read"),
    /** A scene document is invalid JSON. */
    JSON_INVALID("scene.json", "The scene is not valid JScene3D Scene JSON"),
    /** A node identifier is duplicated. */
    NODE_DUPLICATE("scene.node.duplicate", "A scene-node identifier is duplicated"),
    /** A scene instance contains incompatible fields. */
    INSTANCE_FIELDS_INVALID("scene.node.instance.fields", "A scene instance contains incompatible fields"),
    /** A node source is invalid. */
    NODE_SOURCE_INVALID("scene.node.source", "The scene-node source is invalid"),
    /** A scene path escapes the project directory. */
    PATH_ESCAPES_PROJECT("scene.path.escape", "The scene resolves outside the project directory"),
    /** A scene path is absolute. */
    PATH_ABSOLUTE("scene.path.absolute", "A scene path must be relative"),
    /** A scene path is invalid. */
    PATH_INVALID("scene.path.invalid", "A scene path is invalid"),
    /** A referenced scene path is missing. */
    PATH_MISSING("scene.path.missing", "A referenced scene path does not exist"),
    /** A scene path is not portable. */
    PATH_PORTABILITY_INVALID("scene.path.portable", "A scene path must use forward slashes"),
    /** A scene path cannot be resolved. */
    PATH_READ_FAILED("scene.path.read", "A scene path could not be resolved"),
    /** A resource reference object is invalid. */
    REFERENCE_OBJECT_INVALID("scene.reference.object", "A scene resource reference must be a valid reference object"),
    /** A resource-reference scheme is unsupported. */
    REFERENCE_SCHEME_UNSUPPORTED("scene.reference.scheme", "A scene resource reference scheme is unsupported"),
    /** A resource-reference locator is blank. */
    REFERENCE_LOCATOR_BLANK("scene.reference.locator", "A scene resource reference locator must not be blank"),
    /** An asset reference is invalid. */
    REFERENCE_ASSET_INVALID("scene.reference.asset", "A scene asset reference is invalid"),
    /** An asset reference names an undeclared asset. */
    REFERENCE_ASSET_MISSING("scene.reference.asset.missing", "A scene asset reference identifies an undeclared asset"),
    /** An imported-resource reference is invalid. */
    REFERENCE_IMPORT_INVALID("scene.reference.import", "A scene imported-resource reference is invalid"),
    /** The scene schema is unsupported. */
    SCHEMA_UNSUPPORTED("scene.schema.unsupported", "The scene schema version is unsupported"),
    /** The scene schema URI is invalid. */
    SCHEMA_URI_INVALID("scene.schema.uri", "The scene schema URI is invalid"),
    /** A node type identifier is invalid. */
    TYPE_ID_INVALID("scene.type.identifier", "The scene-node type identifier is invalid"),
    /** A node type version is invalid. */
    TYPE_VERSION_INVALID("scene.type.version", "The scene-node type version is invalid"),
    /** A scene value is not an object. */
    VALUE_NOT_OBJECT("scene.value.object", "The scene value must be an object");

    private final String value;
    private final String message;

    SceneDiagnosticCode(String value, String message) {
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
