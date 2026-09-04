/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isRegisteredTypeId;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.ProjectPathResolver;
import io.github.glynch.jscene3d.project.internal.ProjectReferenceDecoder;
import io.github.glynch.jscene3d.project.internal.ProjectSchemaReferences;
import io.github.glynch.jscene3d.project.internal.ValidationContext;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.scene.ControllerDefinition;
import io.github.glynch.jscene3d.project.scene.SceneConnection;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Converts nullable scene JSON into a validated immutable public definition. */
public final class SceneValidator {
    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_URI = "https://jscene3d.org/schemas/scene-1.json";
    private static final String LOCAL_SCHEMA_REFERENCE = "schema/scene-1.schema.json";

    private final GameProject project;
    private final Path source;
    private final DiagnosticCollector diagnostics;
    private final ProjectPathResolver paths;
    private final ValidationContext fields;
    private final ProjectValueDecoder values;
    private final Set<String> nodeIds = new HashSet<>();

    /** Stores one validation context. */
    private SceneValidator(GameProject project, Path source) {
        this.project = project;
        this.source = source;
        diagnostics = new DiagnosticCollector(source);
        paths = new ProjectPathResolver(project.root(), diagnostics, "scene");
        fields = new ValidationContext(diagnostics, "scene");
        ProjectReferenceDecoder references = new ProjectReferenceDecoder(project, paths, diagnostics, "scene");
        values = ProjectValueDecoder.withReferences(references::decode);
    }

    /**
     * Validates one raw scene and returns its complete loading result.
     *
     * @param raw nullable deserialization model
     * @param project containing validated project
     * @param source normalized absolute scene path
     * @return validated scene or ordered diagnostics
     */
    public static ValidationResult validate(RawScene raw, GameProject project, Path source) {
        SceneValidator validator = new SceneValidator(project, source);
        Optional<SceneDefinition> scene = validator.validate(raw);
        return new ValidationResult(scene, validator.diagnostics.diagnostics());
    }

    /** Runs validation in stable scene order. */
    private Optional<SceneDefinition> validate(RawScene raw) {
        validateSchema(raw.schema(), raw.schemaVersion());
        String id = fields.requiredLocalId(raw.id(), "/id");
        SceneNodeDefinition root = validateNode(raw.root(), "/root");
        List<SceneConnection> connections = validateConnections(raw.connections());
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(new SceneDefinition(source, id, root, connections));
    }

    /** Validates the authoritative integer schema version and optional schema URI. */
    private void validateSchema(@Nullable String schema, int schemaVersion) {
        if (schemaVersion != SCHEMA_VERSION) {
            diagnostics.error(
                    "scene.schema.unsupported",
                    "schemaVersion must be " + SCHEMA_VERSION + ": " + schemaVersion,
                    "/schemaVersion");
        }
        if (schema != null
                && !ProjectSchemaReferences.matches(
                        project.root(), source, schema, SCHEMA_URI, LOCAL_SCHEMA_REFERENCE)) {
            diagnostics.warning(
                    "scene.schema.uri", "$schema does not identify the bundled Scene version 1 schema", "/$schema");
        }
    }

    /** Validates one node and its descendants. */
    private SceneNodeDefinition validateNode(RawScene.@Nullable Node raw, String location) {
        if (raw == null) {
            diagnostics.error("scene.field.required", "scene node is required", location);
            return placeholderNode();
        }
        String id = fields.requiredLocalId(raw.id(), location + "/id");
        String safeId = id.isEmpty() ? "invalid-" + nodeIds.size() : id;
        if (!id.isEmpty() && !nodeIds.add(id)) {
            diagnostics.error("scene.node.duplicate", "node id is duplicated: " + id, location + "/id");
        }
        Optional<String> name = fields.optionalText(raw.name(), location + "/name");
        SceneNodeDefinition.Source sourceDefinition = validateNodeSource(raw, location);
        Optional<ControllerDefinition> controller = validateController(raw.controller(), location + "/controller");
        List<SceneNodeDefinition> children = validateChildren(raw.children(), location + "/children");
        return new SceneNodeDefinition(
                safeId, name, raw.enabled() == null || raw.enabled(), sourceDefinition, controller, children);
    }

    /** Validates either a registered node type or a nested scene instance. */
    private SceneNodeDefinition.Source validateNodeSource(RawScene.Node raw, String location) {
        boolean hasType = raw.type() != null || raw.typeVersion() != null || raw.properties() != null;
        boolean hasInstance = raw.instance() != null || raw.overrides() != null;
        if (hasType == hasInstance) {
            diagnostics.error(
                    "scene.node.source",
                    "a node must declare exactly one of a typed source or a scene instance",
                    location);
            return new SceneNodeDefinition.TypedNode(new RegisteredType("invalid.extension/invalid", 1), Map.of());
        }
        if (hasInstance) {
            return validateSceneInstance(raw, location);
        }
        RegisteredType type = validateRegisteredType(raw.type(), raw.typeVersion(), location);
        Map<String, ProjectValue> properties = validateValueMap(raw.properties(), location + "/properties");
        return new SceneNodeDefinition.TypedNode(type, properties);
    }

    /** Validates one nested scene source and its overrides. */
    private SceneNodeDefinition.SceneInstance validateSceneInstance(RawScene.Node raw, String location) {
        if (raw.type() != null || raw.typeVersion() != null || raw.properties() != null) {
            diagnostics.error(
                    "scene.node.instance.fields",
                    "a scene instance cannot also declare type, typeVersion, or properties",
                    location);
        }
        Optional<Path> scene = paths.resolveRequired(raw.instance(), location + "/instance", true);
        Map<String, ProjectValue> overrides = validateValueMap(raw.overrides(), location + "/overrides");
        return new SceneNodeDefinition.SceneInstance(
                scene.orElse(project.root().resolve("invalid.scene.json")), overrides);
    }

    /** Validates one optional project controller. */
    private Optional<ControllerDefinition> validateController(RawScene.@Nullable Controller raw, String location) {
        if (raw == null) {
            return Optional.empty();
        }
        RegisteredType type = validateRegisteredType(raw.type(), raw.typeVersion(), location);
        Map<String, ProjectValue> properties = validateValueMap(raw.properties(), location + "/properties");
        return Optional.of(new ControllerDefinition(type, properties));
    }

    /** Validates a registered type identifier and positive definition version. */
    private RegisteredType validateRegisteredType(
            @Nullable String rawId, @Nullable Integer rawVersion, String location) {
        String id = fields.requiredText(rawId, location + "/type");
        if (!id.isEmpty() && !isRegisteredTypeId(id)) {
            diagnostics.error(
                    "scene.type.identifier",
                    "type must contain an extension id and local type separated by one slash",
                    location + "/type");
        }
        int version = rawVersion == null ? 0 : rawVersion;
        if (version < 1) {
            diagnostics.error("scene.type.version", "typeVersion must be positive", location + "/typeVersion");
        }
        String safeId = isRegisteredTypeId(id) ? id : "invalid.extension/invalid";
        return new RegisteredType(safeId, Math.clamp(version, 1, Integer.MAX_VALUE));
    }

    /** Validates child nodes in scene-tree order. */
    private List<SceneNodeDefinition> validateChildren(
            @Nullable List<RawScene.@Nullable Node> rawChildren, String location) {
        if (rawChildren == null) {
            return List.of();
        }
        List<SceneNodeDefinition> children = new ArrayList<>();
        for (int index = 0; index < rawChildren.size(); index++) {
            children.add(validateNode(rawChildren.get(index), location + "/" + index));
        }
        return List.copyOf(children);
    }

    /** Validates scene-level signal-to-action connections. */
    private List<SceneConnection> validateConnections(@Nullable List<RawScene.@Nullable Connection> rawConnections) {
        if (rawConnections == null) {
            return List.of();
        }
        List<SceneConnection> connections = new ArrayList<>();
        Set<SceneConnection> unique = new HashSet<>();
        for (int index = 0; index < rawConnections.size(); index++) {
            String location = "/connections/" + index;
            Optional<SceneConnection> connection = validateConnection(rawConnections.get(index), location);
            if (connection.isPresent() && !unique.add(connection.orElseThrow())) {
                diagnostics.error("scene.connection.duplicate", "connection is duplicated", location);
            } else {
                connection.ifPresent(connections::add);
            }
        }
        return List.copyOf(connections);
    }

    /** Validates one connection and its referenced nodes. */
    private Optional<SceneConnection> validateConnection(RawScene.@Nullable Connection raw, String location) {
        if (raw == null || raw.from() == null || raw.to() == null) {
            diagnostics.error("scene.field.required", "connection endpoints are required", location);
            return Optional.empty();
        }
        String fromNode = fields.requiredLocalId(raw.from().node(), location + "/from/node");
        String signal = fields.requiredLocalId(raw.from().signal(), location + "/from/signal");
        String toNode = fields.requiredLocalId(raw.to().node(), location + "/to/node");
        String action = fields.requiredLocalId(raw.to().action(), location + "/to/action");
        validateEndpointNode(fromNode, location + "/from/node");
        validateEndpointNode(toNode, location + "/to/node");
        if (fromNode.isEmpty() || signal.isEmpty() || toNode.isEmpty() || action.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SceneConnection(
                new SceneConnection.SignalEndpoint(fromNode, signal),
                new SceneConnection.ActionEndpoint(toNode, action)));
    }

    /** Requires one connection endpoint to identify a node in this scene. */
    private void validateEndpointNode(String node, String location) {
        if (!node.isEmpty() && !nodeIds.contains(node)) {
            diagnostics.error("scene.connection.node", "connection references an unknown node: " + node, location);
        }
    }

    /** Converts a JSON object to an ordered map of portable values. */
    private Map<String, ProjectValue> validateValueMap(@Nullable JsonNode raw, String location) {
        if (raw == null) {
            return Map.of();
        }
        if (!raw.isObject()) {
            diagnostics.error("scene.value.object", "value must be an object", location);
            return Map.of();
        }
        return values.decodeObject(raw, location).values();
    }

    /** Creates a safe placeholder node while collecting a required-field error. */
    private SceneNodeDefinition placeholderNode() {
        String id = "invalid-node";
        return new SceneNodeDefinition(
                id,
                Optional.empty(),
                true,
                new SceneNodeDefinition.TypedNode(new RegisteredType("invalid.extension/invalid", 1), Map.of()),
                Optional.empty(),
                List.of());
    }

    /** Validated scene and ordered diagnostics returned to the public loader.
     *
     * @param scene validated scene when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<SceneDefinition> scene, List<ProjectDiagnostic> diagnostics) {}
}
