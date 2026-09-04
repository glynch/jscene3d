/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isLocalId;
import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isPortableLocator;
import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isRegisteredTypeId;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.ProjectPathResolver;
import io.github.glynch.jscene3d.project.internal.ValidationContext;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.scene.ControllerDefinition;
import io.github.glynch.jscene3d.project.scene.SceneConnection;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import java.nio.file.InvalidPathException;
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
        values = ProjectValueDecoder.withReferences(this::validateReferenceValue);
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
        if (schema != null && !isSupportedSchemaReference(schema)) {
            diagnostics.warning(
                    "scene.schema.uri", "$schema does not identify the bundled Scene version 1 schema", "/$schema");
        }
    }

    /** Recognizes the canonical URI or a project-local schema path relative to this scene. */
    private boolean isSupportedSchemaReference(String schema) {
        if (SCHEMA_URI.equals(schema) || LOCAL_SCHEMA_REFERENCE.equals(schema)) {
            return true;
        }
        if (schema.indexOf('\\') >= 0) {
            return false;
        }
        try {
            Path reference = Path.of(schema);
            Path sourceParent = source.getParent();
            return !reference.isAbsolute()
                    && sourceParent != null
                    && sourceParent
                            .resolve(reference)
                            .normalize()
                            .equals(project.root().resolve(LOCAL_SCHEMA_REFERENCE));
        } catch (InvalidPathException ignored) {
            return false;
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

    /** Converts the reserved single-property resource-reference object. */
    private ProjectValue.ReferenceValue validateReferenceValue(JsonNode raw, String location) {
        JsonNode referenceNode = raw.get("$ref");
        if (raw.size() != 1 || !referenceNode.isTextual()) {
            diagnostics.error(
                    "scene.reference.object",
                    "a resource reference must contain only one textual $ref property",
                    location);
            return new ProjectValue.ReferenceValue(ResourceReference.asset("invalid"));
        }
        return new ProjectValue.ReferenceValue(validateReference(referenceNode.textValue(), location + "/$ref"));
    }

    /** Validates a namespaced resource reference. */
    private ResourceReference validateReference(String value, String location) {
        for (ResourceReference.Kind kind : ResourceReference.Kind.values()) {
            if (value.startsWith(kind.prefix())) {
                return validateReference(kind, value.substring(kind.prefix().length()), location);
            }
        }
        diagnostics.error(
                "scene.reference.scheme", "resource reference must use project:, asset:, or import:", location);
        return ResourceReference.asset("invalid");
    }

    /** Validates one resource reference according to its namespace. */
    private ResourceReference validateReference(ResourceReference.Kind kind, String locator, String location) {
        if (locator.isBlank()) {
            diagnostics.error("scene.reference.locator", "resource reference locator must not be blank", location);
            return ResourceReference.asset("invalid");
        }
        return switch (kind) {
            case PROJECT -> validateProjectReference(locator, location);
            case ASSET -> validateAssetReference(locator, location);
            case IMPORT -> validateImportReference(locator, location);
        };
    }

    /** Validates a project-file reference. */
    private ResourceReference validateProjectReference(String locator, String location) {
        Optional<Path> path = paths.resolve(locator, location, true);
        if (path.isEmpty()) {
            return ResourceReference.project("invalid.resource", project.root().resolve("invalid.resource"));
        }
        return ResourceReference.project(locator, path.orElseThrow());
    }

    /** Validates a source-asset reference. */
    private ResourceReference validateAssetReference(String locator, String location) {
        if (!isLocalId(locator)) {
            diagnostics.error("scene.reference.asset", "asset reference must contain a local asset id", location);
            return ResourceReference.asset("invalid");
        } else if (project.assets().stream().noneMatch(asset -> asset.id().equals(locator))) {
            diagnostics.error("scene.reference.asset.missing", "asset is not declared: " + locator, location);
        }
        return ResourceReference.asset(locator);
    }

    /** Validates an imported-output reference. */
    private ResourceReference validateImportReference(String locator, String location) {
        int separator = locator.indexOf('/');
        boolean valid = separator > 0
                && separator < locator.length() - 1
                && isLocalId(locator.substring(0, separator))
                && isPortableLocator(locator.substring(separator + 1));
        if (!valid) {
            diagnostics.error(
                    "scene.reference.import",
                    "import reference must contain an import id and portable output locator",
                    location);
            return ResourceReference.imported("invalid/output");
        }
        return ResourceReference.imported(locator);
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
