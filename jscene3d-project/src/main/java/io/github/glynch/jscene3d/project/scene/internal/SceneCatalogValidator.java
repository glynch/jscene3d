/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.extension.internal.RegisteredPropertyValidator;
import io.github.glynch.jscene3d.project.internal.PropertyDiagnosticCodes;
import io.github.glynch.jscene3d.project.scene.ControllerDefinition;
import io.github.glynch.jscene3d.project.scene.SceneConnection;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneDiagnosticCode;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Performs registered-type and endpoint validation over a structurally loaded scene. */
public final class SceneCatalogValidator {
    private final SceneDefinition scene;
    private final RegisteredTypeCatalog catalog;
    private final URI source;
    private final List<ProjectDiagnostic> diagnostics = new ArrayList<>();
    private final Map<String, NodeTypes> nodeTypes = new LinkedHashMap<>();

    /** Stores one catalog-validation context. */
    private SceneCatalogValidator(SceneDefinition scene, RegisteredTypeCatalog catalog) {
        this.scene = scene;
        this.catalog = catalog;
        source = scene.source().toUri();
    }

    /**
     * Returns ordered catalog-aware diagnostics for one scene.
     *
     * @param scene structurally loaded scene
     * @param catalog resolved registered-type catalog
     * @return immutable ordered diagnostics
     */
    public static List<ProjectDiagnostic> validate(SceneDefinition scene, RegisteredTypeCatalog catalog) {
        SceneCatalogValidator validator = new SceneCatalogValidator(scene, catalog);
        validator.validateNode(scene.root(), "/root");
        validator.validateConnections();
        return List.copyOf(validator.diagnostics);
    }

    /** Validates one node and records its resolved node and controller descriptors. */
    private void validateNode(SceneNodeDefinition node, String location) {
        Optional<RegisteredTypeDescriptor> nodeType = Optional.empty();
        if (node.source() instanceof SceneNodeDefinition.TypedNode typed) {
            nodeType = resolve(typed.type(), RegisteredTypeScope.SCENE_NODE, location + "/type");
            nodeType.ifPresent(
                    descriptor -> validateProperties(typed.properties(), descriptor, location + "/properties"));
        }
        Optional<RegisteredTypeDescriptor> controllerType =
                node.controller().flatMap(controller -> validateController(controller, location + "/controller"));
        nodeTypes.put(
                node.id(),
                new NodeTypes(nodeType, controllerType, node.source() instanceof SceneNodeDefinition.SceneInstance));
        List<SceneNodeDefinition> children = node.children();
        for (int index = 0; index < children.size(); index++) {
            validateNode(children.get(index), location + "/children/" + index);
        }
    }

    /** Resolves and validates one node controller. */
    private Optional<RegisteredTypeDescriptor> validateController(ControllerDefinition controller, String location) {
        Optional<RegisteredTypeDescriptor> descriptor =
                resolve(controller.type(), RegisteredTypeScope.NODE_CONTROLLER, location + "/type");
        descriptor.ifPresent(value -> validateProperties(controller.properties(), value, location + "/properties"));
        return descriptor;
    }

    /** Resolves an exact registered type and checks its allowed scope. */
    private Optional<RegisteredTypeDescriptor> resolve(
            RegisteredType type, RegisteredTypeScope expectedScope, String location) {
        Optional<RegisteredTypeDescriptor> descriptor = catalog.find(type);
        if (descriptor.isEmpty()) {
            error(SceneDiagnosticCode.TYPE_MISSING, "registered type was not found: " + type, location);
        } else if (descriptor.orElseThrow().scope() != expectedScope) {
            error(
                    SceneDiagnosticCode.TYPE_SCOPE_INVALID,
                    "registered type " + type + " has scope "
                            + descriptor.orElseThrow().scope() + " but " + expectedScope + " is required",
                    location);
            return Optional.empty();
        }
        return descriptor;
    }

    /** Validates authored properties against one registered type descriptor. */
    private void validateProperties(
            Map<String, ProjectValue> authored, RegisteredTypeDescriptor type, String location) {
        diagnostics.addAll(RegisteredPropertyValidator.validate(
                authored,
                type,
                source,
                location,
                new PropertyDiagnosticCodes(
                        SceneDiagnosticCode.PROPERTY_REQUIRED,
                        SceneDiagnosticCode.PROPERTY_UNKNOWN,
                        SceneDiagnosticCode.PROPERTY_VALUE_INVALID)));
    }

    /** Validates every signal-to-action connection. */
    private void validateConnections() {
        List<SceneConnection> connections = scene.connections();
        for (int index = 0; index < connections.size(); index++) {
            validateConnection(connections.get(index), "/connections/" + index);
        }
    }

    /** Resolves both endpoints and checks exact payload compatibility. */
    private void validateConnection(SceneConnection connection, String location) {
        Optional<EndpointDescriptor> signal = resolveSignal(connection.from(), location + "/from");
        Optional<EndpointDescriptor> action = resolveAction(connection.to(), location + "/to");
        if (signal.isPresent()
                && action.isPresent()
                && !signal.orElseThrow().payload().equals(action.orElseThrow().payload())) {
            error(
                    SceneDiagnosticCode.CONNECTION_PAYLOAD_INCOMPATIBLE,
                    "signal and action payload types are incompatible",
                    location);
        }
    }

    /** Resolves one signal on a node or its controller. */
    private Optional<EndpointDescriptor> resolveSignal(SceneConnection.SignalEndpoint endpoint, String location) {
        return resolveEndpoint(endpoint.node(), endpoint.signal(), true, location);
    }

    /** Resolves one action on a node or its controller. */
    private Optional<EndpointDescriptor> resolveAction(SceneConnection.ActionEndpoint endpoint, String location) {
        return resolveEndpoint(endpoint.node(), endpoint.action(), false, location);
    }

    /** Resolves one endpoint, rejecting missing or ambiguous declarations. */
    private Optional<EndpointDescriptor> resolveEndpoint(
            String nodeId, String endpointId, boolean signal, String location) {
        NodeTypes types = nodeTypes.get(nodeId);
        if (types == null) {
            return Optional.empty();
        }
        if (types.sceneInstance()) {
            warning(
                    SceneDiagnosticCode.INSTANCE_ENDPOINT_INVALID,
                    "nested-scene endpoint validation is deferred until scene-instance resolution",
                    location);
            return Optional.empty();
        }
        List<EndpointDescriptor> matches = endpointMatches(types, endpointId, signal);
        if (matches.isEmpty()) {
            error(
                    signal ? SceneDiagnosticCode.SIGNAL_MISSING : SceneDiagnosticCode.ACTION_MISSING,
                    (signal ? "signal" : "action") + " is not declared: " + endpointId,
                    location);
            return Optional.empty();
        }
        if (matches.size() > 1) {
            error(
                    signal ? SceneDiagnosticCode.SIGNAL_AMBIGUOUS : SceneDiagnosticCode.ACTION_AMBIGUOUS,
                    (signal ? "signal" : "action") + " is declared by both the node and controller: " + endpointId,
                    location);
            return Optional.empty();
        }
        return Optional.of(matches.getFirst());
    }

    /** Finds matching endpoints on the node and controller descriptors. */
    private static List<EndpointDescriptor> endpointMatches(NodeTypes types, String endpointId, boolean signal) {
        List<EndpointDescriptor> matches = new ArrayList<>();
        types.node().flatMap(type -> endpoint(type, endpointId, signal)).ifPresent(matches::add);
        types.controller().flatMap(type -> endpoint(type, endpointId, signal)).ifPresent(matches::add);
        return matches;
    }

    /** Finds one signal or action in a registered type descriptor. */
    private static Optional<EndpointDescriptor> endpoint(
            RegisteredTypeDescriptor type, String endpointId, boolean signal) {
        Map<String, EndpointDescriptor> endpoints = signal ? type.signals() : type.actions();
        return Optional.ofNullable(endpoints.get(endpointId));
    }

    /** Adds a catalog validation error. */
    private void error(DiagnosticCode code, String technicalDetail, String location) {
        diagnostics.add(new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, location, Map.of("technicalDetail", technicalDetail)));
    }

    /** Adds a catalog validation warning. */
    private void warning(DiagnosticCode code, String technicalDetail, String location) {
        diagnostics.add(new ProjectDiagnostic(
                ProjectDiagnostic.Severity.WARNING,
                code,
                source,
                location,
                Map.of("technicalDetail", technicalDetail)));
    }

    /** Resolved type metadata for one scene node. */
    private record NodeTypes(
            Optional<RegisteredTypeDescriptor> node,
            Optional<RegisteredTypeDescriptor> controller,
            boolean sceneInstance) {}
}
