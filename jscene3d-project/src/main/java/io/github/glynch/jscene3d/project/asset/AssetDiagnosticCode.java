/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes for authored asset discovery, loading, and reference resolution. */
public enum AssetDiagnosticCode implements DiagnosticCode {
    /** The project asset root is unavailable. */
    ROOT_INVALID("asset.root", "The project asset root is not a readable directory"),
    /** An asset file cannot be read. */
    FILE_READ_FAILED("asset.file.read", "The asset file could not be read"),
    /** A discovered asset resolves outside the project root. */
    PATH_ESCAPES_ROOT("asset.path.escape", "The asset resolves outside the project root"),
    /** An asset document is invalid JSON. */
    JSON_INVALID("asset.json", "The asset is not valid JScene3D asset JSON"),
    /** A required asset field is missing. */
    FIELD_REQUIRED("asset.field.required", "A required asset value is missing"),
    /** An optional asset field is blank. */
    FIELD_BLANK("asset.field.blank", "An optional asset value must not be blank"),
    /** A persistent identity is invalid. */
    ID_INVALID("asset.id", "An asset, entity, or component identity is invalid"),
    /** An authored asset kind is invalid. */
    KIND_INVALID("asset.kind", "The authored asset kind is invalid"),
    /** An authored asset kind conflicts with its filename. */
    KIND_SUFFIX_MISMATCH("asset.kind.suffix", "The authored asset kind conflicts with its filename"),
    /** An asset format version is unsupported. */
    FORMAT_UNSUPPORTED("asset.format.unsupported", "The asset format version is unsupported"),
    /** An asset identity appears in more than one file. */
    ID_DUPLICATE("asset.id.duplicate", "The asset identity is duplicated"),
    /** An asset reference cannot be resolved. */
    REFERENCE_MISSING("asset.reference.missing", "The referenced asset was not found"),
    /** An asset reference resolves to the wrong kind. */
    REFERENCE_KIND_INVALID("asset.reference.kind", "The referenced asset has the wrong kind"),
    /** An optional asset-reference path hint is invalid. */
    REFERENCE_HINT_INVALID("asset.reference.hint", "The asset reference path hint is invalid"),
    /** An asset changed after its catalog metadata was read. */
    CATALOG_STALE("asset.catalog.stale", "The asset no longer matches its catalog metadata"),
    /** Reusable entity definitions contain an inclusion cycle. */
    DEFINITION_CYCLE("asset.definition.cycle", "Entity definitions contain an inclusion cycle"),
    /** A definition entry combines incompatible fields. */
    ENTRY_INVALID("asset.entry", "An entity entry contains incompatible fields"),
    /** A definition value must be a JSON object. */
    VALUE_NOT_OBJECT("asset.value.object", "The definition value must be an object"),
    /** A reserved project-value reference object is invalid. */
    VALUE_REFERENCE_INVALID("asset.value.reference", "The definition value reference is invalid"),
    /** A component type identifier is invalid. */
    COMPONENT_TYPE_INVALID("asset.component.type", "The component type identifier is invalid"),
    /** A component type version is invalid. */
    COMPONENT_VERSION_INVALID("asset.component.version", "The component type version is invalid"),
    /** An authored component type is not registered. */
    COMPONENT_TYPE_MISSING("asset.component.catalog.type", "The component type is not registered"),
    /** A required component property is missing. */
    COMPONENT_PROPERTY_REQUIRED(
            "asset.component.catalog.property.required", "A required component property is missing"),
    /** An authored component property is not declared. */
    COMPONENT_PROPERTY_UNKNOWN("asset.component.catalog.property.unknown", "A component property is not declared"),
    /** An authored component property value violates its declaration. */
    COMPONENT_PROPERTY_VALUE_INVALID("asset.component.catalog.property.value", "A component property value is invalid"),
    /** A single-instance component type occurs more than once on an entity. */
    COMPONENT_MULTIPLICITY_INVALID(
            "asset.component.catalog.multiplicity", "A component type exceeds its allowed multiplicity"),
    /** Conflicting component types occur on the same entity. */
    COMPONENT_CONFLICT("asset.component.catalog.conflict", "Conflicting component types occur on one entity"),
    /** A required component capability has no provider. */
    COMPONENT_CAPABILITY_MISSING(
            "asset.component.catalog.capability.missing", "A required component capability has no provider"),
    /** A required component capability has more than one provider. */
    COMPONENT_CAPABILITY_AMBIGUOUS(
            "asset.component.catalog.capability.ambiguous", "A required component capability is ambiguous"),
    /** A component endpoint target is not declared by its component type. */
    COMPONENT_ENDPOINT_MISSING(
            "asset.component.catalog.endpoint.missing", "A component endpoint target is not declared"),
    /** A component property target is not declared by its component type. */
    COMPONENT_PROPERTY_MISSING(
            "asset.component.catalog.property.missing", "A component property target is not declared"),
    /** A component attachment target is not declared by its component type. */
    COMPONENT_ATTACHMENT_MISSING(
            "asset.component.catalog.attachment.missing", "A component attachment target is not declared"),
    /** A local entity identity is duplicated. */
    ENTITY_ID_DUPLICATE("asset.entity.id.duplicate", "An entity identity is duplicated within an asset"),
    /** A component identity is duplicated on one entity. */
    COMPONENT_ID_DUPLICATE("asset.component.id.duplicate", "A component identity is duplicated on an entity"),
    /** A property identity is invalid. */
    PROPERTY_ID_INVALID("asset.property.id", "A property identity is invalid"),
    /** An endpoint identity is invalid. */
    ENDPOINT_ID_INVALID("asset.endpoint.id", "An endpoint identity is invalid"),
    /** An attachment-point identity is invalid. */
    ATTACHMENT_ID_INVALID("asset.attachment.id", "An attachment-point identity is invalid"),
    /** A capability identity is invalid. */
    CAPABILITY_ID_INVALID("asset.capability.id", "A capability identity is invalid"),
    /** An exported contract declaration is duplicated. */
    CONTRACT_DUPLICATE("asset.contract.duplicate", "An exported contract declaration is duplicated"),
    /** An authored stable target does not exist or crosses a definition seam illegally. */
    TARGET_INVALID("asset.target", "An authored stable target is invalid"),
    /** A placement supplies an argument not declared by its target definition. */
    CONTRACT_ARGUMENT_UNKNOWN("asset.contract.argument.unknown", "A placement argument is not exported"),
    /** A placement omits a required exported argument. */
    CONTRACT_ARGUMENT_REQUIRED("asset.contract.argument.required", "A required placement argument is missing"),
    /** A placement argument does not satisfy its exported declaration. */
    CONTRACT_ARGUMENT_TYPE("asset.contract.argument.type", "A placement argument has the wrong type"),
    /** A placed definition does not export the referenced contract member. */
    CONTRACT_MEMBER_MISSING("asset.contract.member.missing", "A referenced contract member is not exported"),
    /** Connected or re-exported endpoints have incompatible payload declarations. */
    CONTRACT_PAYLOAD_INVALID("asset.contract.payload", "Endpoint payload declarations are incompatible"),
    /** The optional schema reference does not identify the expected bundled schema. */
    SCHEMA_URI_INVALID("asset.schema.uri", "The asset schema URI is invalid");

    private final String value;
    private final String message;

    /** Stores one stable code and English fallback. */
    AssetDiagnosticCode(String value, String message) {
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
