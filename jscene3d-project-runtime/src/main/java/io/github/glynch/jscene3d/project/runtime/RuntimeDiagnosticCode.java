/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for project runtime composition. */
public enum RuntimeDiagnosticCode implements DiagnosticCode {
    /** Runtime composition failed unexpectedly. */
    COMPOSITION_FAILED("runtime.composition", "Runtime composition failed"),
    /** A runtime extension is duplicated. */
    EXTENSION_DUPLICATE("runtime.extension.duplicate", "A runtime extension is duplicated"),
    /** A runtime extension is invalid. */
    EXTENSION_INVALID("runtime.extension.invalid", "A runtime extension is invalid"),
    /** Runtime-extension registration failed. */
    EXTENSION_REGISTRATION_FAILED("runtime.extension.registration", "Runtime extension registration failed"),
    /** A component factory is missing. */
    COMPONENT_FACTORY_MISSING("runtime.factory.component.missing", "A component factory is missing"),
    /** A runtime factory failed to create an object. */
    FACTORY_CREATE_FAILED("runtime.factory.create", "A runtime factory failed to create an object"),
    /** A runtime component does not implement its descriptor-declared lifecycle callbacks. */
    COMPONENT_LIFECYCLE_UNSUPPORTED(
            "runtime.component.lifecycle.unsupported", "A runtime component does not support its declared lifecycle"),
    /** A runtime component does not implement its descriptor-declared update callbacks. */
    COMPONENT_UPDATE_UNSUPPORTED(
            "runtime.component.update.unsupported", "A runtime component does not support its declared update phases"),
    /** A runtime component cannot bind descriptor-declared signals or actions. */
    COMPONENT_ENDPOINT_BINDING_UNSUPPORTED(
            "runtime.component.endpoint.unsupported", "A runtime component does not support its declared endpoints"),
    /** A component endpoint-binding callback failed. */
    COMPONENT_ENDPOINT_BINDING_FAILED(
            "runtime.component.endpoint.binding", "A runtime component failed to bind its declared endpoints"),
    /** A declared signal or action has no runtime implementation. */
    COMPONENT_ENDPOINT_UNIMPLEMENTED(
            "runtime.component.endpoint.unimplemented", "A declared component endpoint is not implemented"),
    /** An authored signal or action is absent from its live definition instance. */
    COMPONENT_ENDPOINT_MISSING(
            "runtime.component.endpoint.missing", "An authored component endpoint target is missing"),
    /** A runtime component cannot bind descriptor-declared target properties. */
    COMPONENT_REFERENCE_BINDING_UNSUPPORTED(
            "runtime.component.reference.unsupported", "A runtime component does not support authored references"),
    /** A component reference-binding callback failed. */
    COMPONENT_REFERENCE_BINDING_FAILED(
            "runtime.component.reference.binding", "A runtime component failed to bind authored references"),
    /** An authored entity or component target is absent from its live definition instance. */
    COMPONENT_REFERENCE_MISSING(
            "runtime.component.reference.missing", "An authored component reference target is missing"),
    /** An authored component target has an incompatible runtime representation. */
    COMPONENT_REFERENCE_TYPE_INVALID(
            "runtime.component.reference.type", "An authored component reference has an incompatible runtime type"),
    /** A registered runtime type is missing. */
    TYPE_MISSING("runtime.type.missing", "A registered runtime type is missing"),
    /** A runtime resource could not be acquired with its required type. */
    RESOURCE_ACQUISITION_FAILED("runtime.resource.acquisition", "A runtime resource could not be acquired"),
    /** A required host-supplied world module is missing. */
    WORLD_MODULE_MISSING("runtime.world-module.missing", "A required world module is missing"),
    /** A world-module interface or adapter is bound more than once. */
    WORLD_MODULE_DUPLICATE("runtime.world-module.duplicate", "A world module binding is duplicated");

    private final String value;
    private final String message;

    RuntimeDiagnosticCode(String value, String message) {
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
