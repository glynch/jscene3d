/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.manifest;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for project manifests and references. */
public enum ProjectDiagnosticCode implements DiagnosticCode {
    /** An asset identifier is duplicated. */
    ASSET_DUPLICATE("project.asset.duplicate", "An asset identifier is duplicated"),
    /** An asset fingerprint is invalid. */
    ASSET_FINGERPRINT_INVALID("project.asset.sha256", "An asset SHA-256 fingerprint is invalid"),
    /** The supported player count is invalid. */
    CATALOG_PLAYERS_INVALID("project.catalog.players", "The supported player count is invalid"),
    /** The project directory is missing. */
    DIRECTORY_MISSING("project.directory.missing", "The project directory does not exist or is not a directory"),
    /** The project directory cannot be resolved. */
    DIRECTORY_READ_FAILED("project.directory.read", "The project directory could not be resolved"),
    /** The engine version is incompatible. */
    ENGINE_INCOMPATIBLE("project.engine.incompatible", "The running engine version is incompatible with the project"),
    /** The engine requirement is invalid. */
    ENGINE_REQUIREMENT_INVALID("project.engine.requirement", "The engine version requirement is invalid"),
    /** A required extension is duplicated. */
    EXTENSION_DUPLICATE("project.extension.duplicate", "A required extension is duplicated"),
    /** A required extension identifier is invalid. */
    EXTENSION_ID_INVALID("project.extension.id", "A required extension identifier is invalid"),
    /** An extension requirement is invalid. */
    EXTENSION_REQUIREMENT_INVALID("project.extension.requirement", "An extension version requirement is invalid"),
    /** A project date is invalid. */
    FIELD_DATE_INVALID("project.field.date", "A project date is invalid"),
    /** A metadata field is duplicated. */
    FIELD_DUPLICATE("project.field.duplicate", "A project metadata field is duplicated"),
    /** A required field is missing. */
    FIELD_REQUIRED("project.field.required", "A non-blank project value is required"),
    /** An optional field is blank. */
    FIELD_BLANK("project.field.blank", "An optional project value must not be blank"),
    /** A field identifier is invalid. */
    FIELD_IDENTIFIER_INVALID("project.field.identifier", "A project value must be a portable lowercase identifier"),
    /** A registered-type field is invalid. */
    FIELD_TYPE_INVALID("project.field.type", "A project value must be a registered-type identifier"),
    /** A URI field is invalid. */
    FIELD_URI_INVALID("project.field.uri", "A project URI is invalid"),
    /** A version field is invalid. */
    FIELD_VERSION_INVALID("project.field.version", "The project version is invalid"),
    /** The project identity is invalid. */
    IDENTITY_ID_INVALID("project.identity.id", "The project identifier is invalid"),
    /** A project value is invalid JSON. */
    JSON_INVALID("project.json", "The project value is not valid JSON"),
    /** The manifest escapes the project directory. */
    MANIFEST_ESCAPES_PROJECT("project.manifest.escape", "The project manifest resolves outside the project directory"),
    /** The manifest is invalid JSON. */
    MANIFEST_JSON_INVALID("project.manifest.json", "The project manifest is not valid Project Manifest JSON"),
    /** The manifest is missing. */
    MANIFEST_MISSING("project.manifest.missing", "The project manifest does not exist or is not a regular file"),
    /** The manifest cannot be read. */
    MANIFEST_READ_FAILED("project.manifest.read", "The project manifest could not be read"),
    /** A project path is duplicated. */
    PATH_DUPLICATE("project.path.duplicate", "A project path is used more than once"),
    /** The application extension identifier is invalid. */
    RUNTIME_EXTENSION_INVALID("project.runtime.extension", "The runtime extension identifier is invalid"),
    /** The application extension is undeclared. */
    RUNTIME_EXTENSION_MISSING("project.runtime.extension.missing", "The runtime extension is not declared"),
    /** The manifest schema is unsupported. */
    SCHEMA_UNSUPPORTED("project.schema.unsupported", "The project schema version is unsupported"),
    /** The manifest schema URI is invalid. */
    SCHEMA_URI_INVALID("project.schema.uri", "The project schema URI is invalid"),
    /** An asset reference is invalid. */
    REFERENCE_ASSET_INVALID("reference.asset", "The asset reference is invalid"),
    /** An asset reference names an undeclared asset. */
    REFERENCE_ASSET_MISSING("reference.asset.missing", "The referenced asset is not declared"),
    /** An imported-resource reference is invalid. */
    REFERENCE_IMPORT_INVALID("reference.import", "The imported-resource reference is invalid"),
    /** A reference locator is blank. */
    REFERENCE_LOCATOR_BLANK("reference.locator", "The resource reference locator must not be blank"),
    /** A resource reference has an invalid representation. */
    REFERENCE_OBJECT_INVALID("reference.object", "The resource reference must be a string or object"),
    /** A resource-reference scheme is unsupported. */
    REFERENCE_SCHEME_UNSUPPORTED("reference.scheme", "The resource reference scheme is unsupported"),
    /** A resource reference is invalid. */
    REFERENCE_RESOURCE_INVALID("invalid.resource", "The resource reference is invalid"),
    /** A scene reference is invalid. */
    SCENE_JSON_INVALID("invalid.scene.json", "The scene value is invalid"),
    /** A project path is absolute. */
    PATH_ABSOLUTE("project.path.absolute", "A project path must be relative"),
    /** A project path escapes the project directory. */
    PATH_ESCAPES_PROJECT("project.path.escape", "A project path escapes the project directory"),
    /** A project path is invalid. */
    PATH_INVALID("project.path.invalid", "A project path is invalid"),
    /** A referenced project path is missing. */
    PATH_MISSING("project.path.missing", "A referenced project path does not exist"),
    /** A project path is not portable. */
    PATH_PORTABILITY_INVALID("project.path.portable", "A project path must use forward slashes"),
    /** A project path symlink escapes the project directory. */
    PATH_SYMLINK_ESCAPES_PROJECT("project.path.symlink", "A project path resolves outside the project directory"),
    /** A project path cannot be resolved. */
    PATH_UNREADABLE("project.path.read", "A project path could not be resolved");

    private final String value;
    private final String message;

    ProjectDiagnosticCode(String value, String message) {
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
