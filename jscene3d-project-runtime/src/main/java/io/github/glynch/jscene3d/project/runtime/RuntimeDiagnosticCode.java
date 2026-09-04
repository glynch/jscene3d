/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostic codes and English fallbacks for project runtime composition. */
public enum RuntimeDiagnosticCode implements DiagnosticCode {
    /** A connected action lacks a runtime implementation. */
    ACTION_UNIMPLEMENTED("runtime.action.unimplemented", "The runtime action is not implemented"),
    /** Runtime composition failed unexpectedly. */
    COMPOSITION_FAILED("runtime.composition", "Runtime composition failed"),
    /** The application extension was not loaded. */
    APPLICATION_EXTENSION_MISSING("runtime.extension.application.missing", "The application extension was not loaded"),
    /** Runtime-extension discovery failed. */
    EXTENSION_DISCOVERY_FAILED("runtime.extension.discovery", "Runtime extensions could not be discovered"),
    /** A runtime extension is duplicated. */
    EXTENSION_DUPLICATE("runtime.extension.duplicate", "A runtime extension is duplicated"),
    /** A runtime extension is invalid. */
    EXTENSION_INVALID("runtime.extension.invalid", "A runtime extension is invalid"),
    /** Runtime-extension registration failed. */
    EXTENSION_REGISTRATION_FAILED("runtime.extension.registration", "Runtime extension registration failed"),
    /** A controller factory is missing. */
    CONTROLLER_FACTORY_MISSING("runtime.factory.controller.missing", "A controller factory is missing"),
    /** A runtime factory failed to create an object. */
    FACTORY_CREATE_FAILED("runtime.factory.create", "A runtime factory failed to create an object"),
    /** A resource factory failed to create a value. */
    RESOURCE_FACTORY_CREATE_FAILED("runtime.factory.resource.create", "A resource factory failed to create a value"),
    /** A resource factory is missing. */
    RESOURCE_FACTORY_MISSING("runtime.factory.resource.missing", "A resource factory is missing"),
    /** A scene-node factory is missing. */
    SCENE_NODE_FACTORY_MISSING("runtime.factory.scene-node.missing", "A scene-node factory is missing"),
    /** Project systems are unsupported. */
    PROJECT_SYSTEMS_UNSUPPORTED("runtime.project-systems.unsupported", "Project systems are not supported"),
    /** Project resources contain a dependency cycle. */
    RESOURCE_CYCLE("runtime.resource.cycle", "Project resources contain a dependency cycle"),
    /** A resource-reference kind is unsupported. */
    RESOURCE_KIND_UNSUPPORTED("runtime.resource.kind.unsupported", "The project resource kind is unsupported"),
    /** A resource value has an incompatible Java type. */
    RESOURCE_VALUE_TYPE_INVALID("runtime.resource.value.type", "The resource factory returned an incompatible value"),
    /** Scene instances are unsupported. */
    SCENE_INSTANCE_UNSUPPORTED("runtime.scene-instance.unsupported", "Scene instances are not supported"),
    /** A registered runtime type is missing. */
    TYPE_MISSING("runtime.type.missing", "A registered runtime type is missing");

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
