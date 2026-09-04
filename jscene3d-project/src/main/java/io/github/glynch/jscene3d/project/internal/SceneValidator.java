/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.ControllerDefinition;
import io.github.glynch.jscene3d.project.GameProject;
import io.github.glynch.jscene3d.project.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.ProjectValue;
import io.github.glynch.jscene3d.project.RegisteredType;
import io.github.glynch.jscene3d.project.ResourceReference;
import io.github.glynch.jscene3d.project.SceneConnection;
import io.github.glynch.jscene3d.project.SceneDefinition;
import io.github.glynch.jscene3d.project.SceneNodeDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final Set<String> nodeIds = new HashSet<>();

    /** Stores one validation context. */
    private SceneValidator(GameProject project, Path source) {
        this.project = project;
        this.source = source;
        diagnostics = new DiagnosticCollector(source);
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
        String id = requiredLocalId(raw.id(), "/id");
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
        String id = requiredLocalId(raw.id(), location + "/id");
        String safeId = id.isEmpty() ? "invalid-" + nodeIds.size() : id;
        if (!id.isEmpty() && !nodeIds.add(id)) {
            diagnostics.error("scene.node.duplicate", "node id is duplicated: " + id, location + "/id");
        }
        Optional<String> name = optionalText(raw.name(), location + "/name");
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
        Optional<Path> scene = requiredPath(raw.instance(), location + "/instance", true);
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
        String id = requiredText(rawId, location + "/type");
        if (!id.isEmpty() && !isRegisteredType(id)) {
            diagnostics.error(
                    "scene.type.identifier",
                    "type must contain an extension id and local type separated by one slash",
                    location + "/type");
        }
        int version = rawVersion == null ? 0 : rawVersion;
        if (version < 1) {
            diagnostics.error("scene.type.version", "typeVersion must be positive", location + "/typeVersion");
        }
        String safeId = id.isEmpty() ? "invalid.extension/invalid" : id;
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
        String fromNode = requiredLocalId(raw.from().node(), location + "/from/node");
        String signal = requiredLocalId(raw.from().signal(), location + "/from/signal");
        String toNode = requiredLocalId(raw.to().node(), location + "/to/node");
        String action = requiredLocalId(raw.to().action(), location + "/to/action");
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
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        raw.properties()
                .forEach(entry -> values.put(
                        entry.getKey(),
                        validateValue(entry.getValue(), location + "/" + escapePointer(entry.getKey()))));
        return Collections.unmodifiableMap(values);
    }

    /** Converts one JSON value without exposing Jackson through the public interface. */
    private ProjectValue validateValue(JsonNode raw, String location) {
        if (raw.isNull()) {
            return ProjectValue.NullValue.INSTANCE;
        }
        if (raw.isBoolean()) {
            return new ProjectValue.BooleanValue(raw.booleanValue());
        }
        if (raw.isNumber()) {
            return new ProjectValue.NumberValue(raw.decimalValue());
        }
        if (raw.isTextual()) {
            return new ProjectValue.TextValue(raw.textValue());
        }
        if (raw.isArray()) {
            return validateArray(raw, location);
        }
        if (raw.has("$ref")) {
            return validateReferenceValue(raw, location);
        }
        return validateObject(raw, location);
    }

    /** Converts one JSON array. */
    private ProjectValue.ArrayValue validateArray(JsonNode raw, String location) {
        List<ProjectValue> values = new ArrayList<>();
        for (int index = 0; index < raw.size(); index++) {
            values.add(validateValue(raw.get(index), location + "/" + index));
        }
        return new ProjectValue.ArrayValue(values);
    }

    /** Converts one ordinary JSON object. */
    private ProjectValue.ObjectValue validateObject(JsonNode raw, String location) {
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        raw.properties()
                .forEach(entry -> values.put(
                        entry.getKey(),
                        validateValue(entry.getValue(), location + "/" + escapePointer(entry.getKey()))));
        return new ProjectValue.ObjectValue(values);
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
        Optional<Path> path = resolvePath(locator, location, true);
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

    /** Requires a non-blank string, returning a placeholder while collecting errors. */
    private String requiredText(@Nullable String value, String location) {
        if (value == null || value.isBlank()) {
            diagnostics.error("scene.field.required", "a non-blank value is required", location);
            return "";
        }
        return value.strip();
    }

    /** Requires a portable lowercase identifier. */
    private String requiredLocalId(@Nullable String value, String location) {
        String identifier = requiredText(value, location);
        if (!identifier.isEmpty() && !isLocalId(identifier)) {
            diagnostics.error("scene.field.identifier", "value must be a portable lowercase identifier", location);
        }
        return identifier;
    }

    /** Returns an optional non-blank string. */
    private Optional<String> optionalText(@Nullable String value, String location) {
        if (value == null) {
            return Optional.empty();
        }
        if (value.isBlank()) {
            diagnostics.error("scene.field.blank", "optional values must not be blank", location);
            return Optional.empty();
        }
        return Optional.of(value.strip());
    }

    /** Resolves a required project-relative path. */
    private Optional<Path> requiredPath(@Nullable String value, String location, boolean warnIfMissing) {
        String text = requiredText(value, location);
        return text.isEmpty() ? Optional.empty() : resolvePath(text, location, warnIfMissing);
    }

    /** Confines one portable scene path to the project root. */
    private Optional<Path> resolvePath(String value, String location, boolean warnIfMissing) {
        if (value.indexOf('\\') >= 0) {
            diagnostics.error("scene.path.portable", "project paths must use forward slashes", location);
            return Optional.empty();
        }
        try {
            Path relative = Path.of(value);
            if (relative.isAbsolute()) {
                diagnostics.error("scene.path.absolute", "project path must be relative", location);
                return Optional.empty();
            }
            Path resolved = project.root().resolve(relative).normalize();
            if (!resolved.startsWith(project.root())) {
                diagnostics.error("scene.path.escape", "project path escapes the project directory", location);
                return Optional.empty();
            }
            if (!validateExistingPath(resolved, location)) {
                return Optional.empty();
            }
            if (warnIfMissing && Files.notExists(resolved)) {
                diagnostics.warning("scene.path.missing", "referenced project path does not exist: " + value, location);
            }
            return Optional.of(resolved);
        } catch (InvalidPathException ignored) {
            diagnostics.error("scene.path.invalid", "project path is invalid", location);
            return Optional.empty();
        }
    }

    /** Rejects an existing symlink target outside the real project directory. */
    private boolean validateExistingPath(Path resolved, String location) {
        if (Files.notExists(resolved)) {
            return true;
        }
        try {
            if (!resolved.toRealPath().startsWith(project.root().toRealPath())) {
                diagnostics.error("scene.path.escape", "project path resolves outside the project directory", location);
                return false;
            }
            return true;
        } catch (IOException exception) {
            diagnostics.error(
                    "scene.path.read", "project path cannot be resolved: " + exception.getMessage(), location);
            return false;
        }
    }

    /** Recognizes an extension-qualified registered type identifier. */
    private static boolean isRegisteredType(String value) {
        int separator = value.indexOf('/');
        return separator > 0
                && separator == value.lastIndexOf('/')
                && separator < value.length() - 1
                && isProjectId(value.substring(0, separator))
                && isLocalId(value.substring(separator + 1));
    }

    /** Recognizes a lowercase dotted identifier without regex backtracking. */
    private static boolean isProjectId(String value) {
        if (value.isEmpty() || !isAsciiLowercase(value.charAt(0))) {
            return false;
        }
        int segmentStart = 0;
        boolean foundDot = false;
        for (int index = 0; index <= value.length(); index++) {
            if (index == value.length() || value.charAt(index) == '.') {
                if (!isProjectIdSegment(value, segmentStart, index)) {
                    return false;
                }
                foundDot |= index < value.length();
                segmentStart = index + 1;
            }
        }
        return foundDot;
    }

    /** Recognizes one dot-delimited project identifier segment. */
    private static boolean isProjectIdSegment(String value, int start, int end) {
        if (start >= end || !isAsciiAlphaNumeric(value.charAt(start)) || !isAsciiAlphaNumeric(value.charAt(end - 1))) {
            return false;
        }
        for (int index = start + 1; index < end - 1; index++) {
            char character = value.charAt(index);
            if (!isAsciiAlphaNumeric(character) && character != '-') {
                return false;
            }
        }
        return true;
    }

    /** Recognizes a portable lowercase identifier. */
    private static boolean isLocalId(String value) {
        if (value.isEmpty()
                || !isAsciiAlphaNumeric(value.charAt(0))
                || !isAsciiAlphaNumeric(value.charAt(value.length() - 1))) {
            return false;
        }
        for (int index = 1; index < value.length() - 1; index++) {
            char character = value.charAt(index);
            if (!isAsciiAlphaNumeric(character) && character != '-') {
                return false;
            }
        }
        return true;
    }

    /** Recognizes one portable import-output locator. */
    private static boolean isPortableLocator(String value) {
        if (value.isBlank() || value.indexOf('\\') >= 0 || value.startsWith("/") || value.endsWith("/")) {
            return false;
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether a character is an ASCII lowercase letter. */
    private static boolean isAsciiLowercase(char character) {
        return character >= 'a' && character <= 'z';
    }

    /** Returns whether a character is an ASCII decimal digit. */
    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }

    /** Returns whether a character is an ASCII lowercase letter or decimal digit. */
    private static boolean isAsciiAlphaNumeric(char character) {
        return isAsciiLowercase(character) || isAsciiDigit(character);
    }

    /** Escapes one JSON Pointer path segment. */
    private static String escapePointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    /** Validated scene and ordered diagnostics returned to the public loader.
     *
     * @param scene validated scene when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<SceneDefinition> scene, List<ProjectDiagnostic> diagnostics) {}
}
