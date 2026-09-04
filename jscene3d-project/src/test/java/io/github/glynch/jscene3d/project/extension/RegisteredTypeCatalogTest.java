/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.scene.ControllerDefinition;
import io.github.glynch.jscene3d.project.scene.SceneConnection;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exercises catalog-aware validation over structurally valid scenes. */
final class RegisteredTypeCatalogTest {
    private static final Path SOURCE = Path.of("/project/scenes/main.scene.json");
    private static final RegisteredType NODE_TYPE = new RegisteredType("example.game/actor-3d", 1);
    private static final RegisteredType CONTROLLER_TYPE = new RegisteredType("example.game/controller", 1);
    private static final RegisteredType EVENT_TYPE = new RegisteredType("example.game/event", 1);

    /** Accepts declared properties and exactly compatible signal-to-action payloads. */
    @Test
    void validatesCompatibleScene() {
        SceneNodeDefinition receiver = node(
                "receiver",
                Map.of("label", new ProjectValue.TextValue("Receiver")),
                Optional.of(new ControllerDefinition(CONTROLLER_TYPE, Map.of())));
        SceneDefinition scene = scene(
                node(
                        "sender",
                        Map.of("label", new ProjectValue.TextValue("Sender")),
                        Optional.empty(),
                        List.of(receiver)),
                List.of(connection("sender", "selected", "receiver", "select")));

        assertThat(catalog().validate(scene)).isEmpty();
    }

    /** Reports unknown, missing, and structurally invalid authored properties together. */
    @Test
    void reportsPropertyErrors() {
        SceneNodeDefinition root = node(
                "root",
                Map.of(
                        "speed", new ProjectValue.TextValue("fast"),
                        "mystery", new ProjectValue.BooleanValue(true)),
                Optional.empty());

        List<ProjectDiagnostic> diagnostics = catalog().validate(scene(root, List.of()));

        assertThat(diagnostics)
                .extracting(ProjectDiagnostic::code)
                .containsExactlyInAnyOrder(
                        "scene.catalog.property.value",
                        "scene.catalog.property.unknown",
                        "scene.catalog.property.required");
        assertThat(diagnostics)
                .extracting(ProjectDiagnostic::location)
                .contains("/root/properties/speed", "/root/properties/mystery", "/root/properties");
    }

    /** Reports unresolved registered types and types used in the wrong scene scope. */
    @Test
    void reportsTypeResolutionErrors() {
        SceneNodeDefinition missing = typedNode("missing", new RegisteredType("example.game/missing", 1));
        SceneNodeDefinition wrongScope = typedNode("event", EVENT_TYPE, List.of(missing));

        List<ProjectDiagnostic> diagnostics = catalog().validate(scene(wrongScope, List.of()));

        assertThat(diagnostics)
                .extracting(ProjectDiagnostic::code)
                .containsExactly("scene.catalog.type.scope", "scene.catalog.type.missing");
    }

    /** Reports missing endpoints and incompatible payload contracts. */
    @Test
    void reportsConnectionErrors() {
        SceneNodeDefinition receiver = node(
                "receiver",
                Map.of("label", new ProjectValue.TextValue("Receiver")),
                Optional.of(new ControllerDefinition(CONTROLLER_TYPE, Map.of())));
        SceneNodeDefinition root = node(
                "sender", Map.of("label", new ProjectValue.TextValue("Sender")), Optional.empty(), List.of(receiver));
        List<SceneConnection> connections = List.of(
                connection("sender", "unknown", "receiver", "select"),
                connection("sender", "selected", "receiver", "unknown"),
                connection("sender", "ready", "receiver", "select"));

        List<ProjectDiagnostic> diagnostics = catalog().validate(scene(root, connections));

        assertThat(diagnostics)
                .extracting(ProjectDiagnostic::code)
                .containsExactly(
                        "scene.catalog.signal.missing",
                        "scene.catalog.action.missing",
                        "scene.catalog.connection.payload");
    }

    /** Rejects an endpoint name declared by both a node and its controller. */
    @Test
    void reportsAmbiguousEndpoint() {
        SceneNodeDefinition root = node(
                "root",
                Map.of("label", new ProjectValue.TextValue("Root")),
                Optional.of(new ControllerDefinition(CONTROLLER_TYPE, Map.of())));

        List<ProjectDiagnostic> diagnostics =
                catalog().validate(scene(root, List.of(connection("root", "selected", "root", "select"))));

        assertThat(diagnostics).extracting(ProjectDiagnostic::code).containsExactly("scene.catalog.signal.ambiguous");
    }

    /** Defers nested-scene endpoint checks until instances have been resolved. */
    @Test
    void defersSceneInstanceEndpointValidation() {
        SceneNodeDefinition instance = new SceneNodeDefinition(
                "nested",
                Optional.empty(),
                true,
                new SceneNodeDefinition.SceneInstance(Path.of("/project/scenes/nested.scene.json"), Map.of()),
                Optional.empty(),
                List.of());

        List<ProjectDiagnostic> diagnostics =
                catalog().validate(scene(instance, List.of(connection("nested", "ready", "nested", "reset"))));

        assertThat(diagnostics).hasSize(2).allSatisfy(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(ProjectDiagnostic.Severity.WARNING);
            assertThat(diagnostic.code()).isEqualTo("scene.catalog.instance.endpoint");
        });
    }

    /** Creates the registered-type catalog used by scene-validation tests. */
    private static RegisteredTypeCatalog catalog() {
        ExtensionDescriptor extension = new ExtensionDescriptor(
                "example.game",
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Example Game"),
                List.of(nodeDescriptor(), controllerDescriptor(), eventDescriptor()));
        return new RegisteredTypeCatalog(List.of(extension));
    }

    /** Creates the scene-node descriptor used by tests. */
    private static RegisteredTypeDescriptor nodeDescriptor() {
        return new RegisteredTypeDescriptor(
                NODE_TYPE,
                RegisteredTypeScope.SCENE_NODE,
                DescriptorPresentation.named("Actor 3d"),
                List.of(
                        PropertyDescriptor.required(
                                "label",
                                ProjectValueKind.TEXT,
                                DescriptorPresentation.named("Label"),
                                Map.of(),
                                Set.of()),
                        PropertyDescriptor.optionalWithDefault(
                                "speed",
                                ProjectValueKind.NUMBER,
                                new ProjectValue.NumberValue(BigDecimal.ONE),
                                DescriptorPresentation.named("Speed"),
                                Map.of(),
                                Set.of())),
                List.of(
                        EndpointDescriptor.withPayload(
                                "selected", EVENT_TYPE, DescriptorPresentation.named("Selected")),
                        EndpointDescriptor.withoutPayload("ready", DescriptorPresentation.named("Ready"))),
                List.of(EndpointDescriptor.withoutPayload("reset", DescriptorPresentation.named("Reset"))),
                List.of());
    }

    /** Creates the controller descriptor used by tests. */
    private static RegisteredTypeDescriptor controllerDescriptor() {
        return new RegisteredTypeDescriptor(
                CONTROLLER_TYPE,
                RegisteredTypeScope.NODE_CONTROLLER,
                DescriptorPresentation.named("Controller"),
                List.of(),
                List.of(EndpointDescriptor.withPayload(
                        "selected", EVENT_TYPE, DescriptorPresentation.named("Selected"))),
                List.of(EndpointDescriptor.withPayload("select", EVENT_TYPE, DescriptorPresentation.named("Select"))),
                List.of());
    }

    /** Creates the payload-resource descriptor used by endpoint contracts. */
    private static RegisteredTypeDescriptor eventDescriptor() {
        return new RegisteredTypeDescriptor(
                EVENT_TYPE,
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.named("Event"),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    /** Creates one typed node without children. */
    private static SceneNodeDefinition node(
            String id, Map<String, ProjectValue> properties, Optional<ControllerDefinition> controller) {
        return node(id, properties, controller, List.of());
    }

    /** Creates one typed node with children. */
    private static SceneNodeDefinition node(
            String id,
            Map<String, ProjectValue> properties,
            Optional<ControllerDefinition> controller,
            List<SceneNodeDefinition> children) {
        return new SceneNodeDefinition(
                id,
                Optional.empty(),
                true,
                new SceneNodeDefinition.TypedNode(NODE_TYPE, properties),
                controller,
                children);
    }

    /** Creates one node using an arbitrary registered type. */
    private static SceneNodeDefinition typedNode(String id, RegisteredType type) {
        return typedNode(id, type, List.of());
    }

    /** Creates one node using an arbitrary registered type and children. */
    private static SceneNodeDefinition typedNode(String id, RegisteredType type, List<SceneNodeDefinition> children) {
        return new SceneNodeDefinition(
                id,
                Optional.empty(),
                true,
                new SceneNodeDefinition.TypedNode(type, Map.of()),
                Optional.empty(),
                children);
    }

    /** Creates a scene definition over one root. */
    private static SceneDefinition scene(SceneNodeDefinition root, List<SceneConnection> connections) {
        return new SceneDefinition(SOURCE, "main", root, connections);
    }

    /** Creates one declarative signal-to-action connection. */
    private static SceneConnection connection(String fromNode, String signal, String toNode, String action) {
        return new SceneConnection(
                new SceneConnection.SignalEndpoint(fromNode, signal),
                new SceneConnection.ActionEndpoint(toNode, action));
    }
}
