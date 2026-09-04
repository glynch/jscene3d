/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.manifest.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectHashes.isSha256;
import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isProjectId;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.ProjectPathResolver;
import io.github.glynch.jscene3d.project.internal.SemanticVersion;
import io.github.glynch.jscene3d.project.internal.SemanticVersionRequirement;
import io.github.glynch.jscene3d.project.internal.ValidationContext;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Converts nullable JSON values into one validated immutable public descriptor. */
public final class ManifestValidator {
    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_URI = "https://jscene3d.org/schemas/project-1.json";
    private static final String LOCAL_SCHEMA_REFERENCE = "schema/project-1.schema.json";

    private final Path root;
    private final SemanticVersion engineVersion;
    private final String engineVersionText;
    private final DiagnosticCollector diagnostics;
    private final ProjectPathResolver paths;
    private final ValidationContext fields;

    /** Stores one validation context. */
    private ManifestValidator(Path root, Path source, SemanticVersion engineVersion, String engineVersionText) {
        this.root = root;
        this.engineVersion = engineVersion;
        this.engineVersionText = engineVersionText;
        diagnostics = new DiagnosticCollector(source);
        paths = new ProjectPathResolver(root, diagnostics, "project");
        fields = new ValidationContext(diagnostics, "project");
    }

    /** Validates one raw manifest and returns its complete loading result.
     *
     * @param raw nullable deserialization model
     * @param root canonical project directory
     * @param source canonical manifest path
     * @param engineVersion parsed running engine version
     * @param engineVersionText running engine version as supplied by the caller
     * @return validated project or ordered diagnostics
     */
    public static ValidationResult validate(
            RawManifest raw, Path root, Path source, SemanticVersion engineVersion, String engineVersionText) {
        ManifestValidator validator = new ManifestValidator(root, source, engineVersion, engineVersionText);
        Optional<GameProject> project = validator.validate(raw);
        return new ValidationResult(project, validator.diagnostics.diagnostics());
    }

    /** Runs validation in stable manifest order. */
    private Optional<GameProject> validate(RawManifest raw) {
        validateSchema(raw.schema(), raw.schemaVersion());
        GameProject.Identity identity = validateIdentity(raw.identity());
        List<GameProject.Author> authors = validateAuthors(raw.authors());
        GameProject.Links links = validateLinks(raw.links());
        GameProject.Legal legal = validateLegal(raw.legal());
        GameProject.EngineCompatibility engine = validateEngine(raw.engine());
        GameProject.RuntimeConfiguration runtime = validateRuntime(raw.runtime());
        List<GameProject.ExtensionRequirement> extensions = validateExtensions(raw.extensions());
        List<GameProject.AssetSource> assets = validateAssets(raw.assets());
        List<Path> imports = validatePathList(raw.imports(), "/imports");
        List<Path> exportPresets = validatePathList(raw.exportPresets(), "/exportPresets");
        GameProject.Catalog catalog = validateCatalog(raw.catalog());
        validateApplicationExtension(runtime.applicationExtension(), extensions);
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        GameProject.Metadata metadata = new GameProject.Metadata(identity, authors, links, legal, catalog);
        GameProject.ProjectFiles files = new GameProject.ProjectFiles(assets, imports, exportPresets);
        return Optional.of(new GameProject(root, metadata, engine, runtime, extensions, files));
    }

    /** Validates the authoritative integer schema version and optional schema URI. */
    private void validateSchema(@Nullable String schema, int schemaVersion) {
        if (schemaVersion != SCHEMA_VERSION) {
            diagnostics.error(
                    "project.schema.unsupported",
                    "schemaVersion must be " + SCHEMA_VERSION + ": " + schemaVersion,
                    "/schemaVersion");
        }
        if (schema != null && !SCHEMA_URI.equals(schema) && !LOCAL_SCHEMA_REFERENCE.equals(schema)) {
            diagnostics.warning(
                    "project.schema.uri",
                    "$schema does not identify the bundled Project Manifest version 1 schema",
                    "/$schema");
        }
    }

    /** Validates stable identity and project-browser metadata. */
    private GameProject.Identity validateIdentity(RawManifest.@Nullable Identity raw) {
        if (raw == null) {
            diagnostics.error("project.field.required", "identity is required", "/identity");
            raw = new RawManifest.Identity(null, null, null, null, null, null, null);
        }
        String id = fields.requiredText(raw.id(), "/identity/id");
        if (!id.isEmpty() && !isProjectId(id)) {
            diagnostics.error(
                    "project.identity.id", "identity.id must be a lowercase reverse-domain identifier", "/identity/id");
        }
        String name = fields.requiredText(raw.name(), "/identity/name");
        String version = fields.requiredText(raw.version(), "/identity/version");
        validateSemanticVersion(version, "/identity/version");
        Optional<LocalDate> created = optionalDate(raw.created(), "/identity/created");
        Optional<LocalDate> released = optionalDate(raw.released(), "/identity/released");
        Optional<String> description = fields.optionalText(raw.description(), "/identity/description");
        Optional<Path> icon = paths.resolveOptional(raw.icon(), "/identity/icon", true);
        return identityOrPlaceholder(id, name, version, created, released, description, icon);
    }

    /** Avoids constructing an invalid public value while diagnostics contain errors. */
    private GameProject.Identity identityOrPlaceholder(
            String id,
            String name,
            String version,
            Optional<LocalDate> created,
            Optional<LocalDate> released,
            Optional<String> description,
            Optional<Path> icon) {
        if (!isProjectId(id) || name.isEmpty() || SemanticVersion.parse(version).isEmpty()) {
            return new GameProject.Identity(
                    "invalid.project", "Invalid project", "0.0.0", created, released, description, icon);
        }
        return new GameProject.Identity(id, name, version, created, released, description, icon);
    }

    /** Validates principal authors. */
    private List<GameProject.Author> validateAuthors(@Nullable List<RawManifest.@Nullable Author> rawAuthors) {
        if (rawAuthors == null) {
            return List.of();
        }
        List<GameProject.Author> authors = new ArrayList<>();
        for (int index = 0; index < rawAuthors.size(); index++) {
            String location = "/authors/" + index;
            RawManifest.@Nullable Author raw = rawAuthors.get(index);
            if (raw == null) {
                diagnostics.error("project.field.required", "author must be an object", location);
                continue;
            }
            String name = fields.requiredText(raw.name(), location + "/name");
            List<String> roles = textList(raw.roles(), location + "/roles");
            Optional<URI> url = optionalUri(raw.url(), location + "/url");
            if (!name.isEmpty()) {
                authors.add(new GameProject.Author(name, roles, url));
            }
        }
        return List.copyOf(authors);
    }

    /** Validates optional public project links. */
    private GameProject.Links validateLinks(RawManifest.@Nullable Links raw) {
        if (raw == null) {
            return GameProject.Links.empty();
        }
        return new GameProject.Links(
                optionalUri(raw.homepage(), "/links/homepage"),
                optionalUri(raw.source(), "/links/source"),
                optionalUri(raw.issues(), "/links/issues"));
    }

    /** Validates optional legal-document references. */
    private GameProject.Legal validateLegal(RawManifest.@Nullable Legal raw) {
        if (raw == null) {
            return GameProject.Legal.empty();
        }
        Optional<GameProject.ProjectLicense> license = validateLicense(raw.projectLicense());
        Optional<Path> notices = paths.resolveOptional(raw.thirdPartyNotices(), "/legal/thirdPartyNotices", true);
        Optional<Path> credits = paths.resolveOptional(raw.credits(), "/legal/credits", true);
        return new GameProject.Legal(license, notices, credits);
    }

    /** Validates one optional project license. */
    private Optional<GameProject.ProjectLicense> validateLicense(RawManifest.@Nullable ProjectLicense raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String expression = fields.requiredText(raw.expression(), "/legal/projectLicense/expression");
        Optional<Path> file = paths.resolveOptional(raw.file(), "/legal/projectLicense/file", true);
        return expression.isEmpty() ? Optional.empty() : Optional.of(new GameProject.ProjectLicense(expression, file));
    }

    /** Validates syntax and current compatibility for the engine requirement. */
    private GameProject.EngineCompatibility validateEngine(RawManifest.@Nullable Engine raw) {
        if (raw == null) {
            diagnostics.error("project.field.required", "engine is required", "/engine");
            raw = new RawManifest.Engine(null, null);
        }
        String requirementText = fields.requiredText(raw.requires(), "/engine/requires");
        Optional<SemanticVersionRequirement> requirement = SemanticVersionRequirement.parse(requirementText);
        if (!requirementText.isEmpty() && requirement.isEmpty()) {
            diagnostics.error(
                    "project.engine.requirement",
                    "engine.requires must contain semantic-version comparisons",
                    "/engine/requires");
        } else if (requirement.isPresent() && !requirement.orElseThrow().includes(engineVersion)) {
            diagnostics.error(
                    "project.engine.incompatible",
                    "project requires " + requirementText + " but the current engine is " + engineVersionText,
                    "/engine/requires");
        }
        Optional<String> authoredWith = fields.optionalText(raw.authoredWith(), "/engine/authoredWith");
        authoredWith.ifPresent(value -> validateSemanticVersion(value, "/engine/authoredWith"));
        String safeRequirement = requirement.isPresent() ? requirementText : "0.0.0";
        Optional<String> safeAuthoredWith =
                authoredWith.filter(value -> SemanticVersion.parse(value).isPresent());
        return new GameProject.EngineCompatibility(safeRequirement, safeAuthoredWith);
    }

    /** Validates application startup and optional project-level definitions. */
    private GameProject.RuntimeConfiguration validateRuntime(RawManifest.@Nullable RuntimeConfiguration raw) {
        if (raw == null) {
            diagnostics.error("project.field.required", "runtime is required", "/runtime");
            raw = new RawManifest.RuntimeConfiguration(null, null, null, null);
        }
        String extension = fields.requiredText(raw.applicationExtension(), "/runtime/applicationExtension");
        if (!extension.isEmpty() && !isProjectId(extension)) {
            diagnostics.error(
                    "project.runtime.extension",
                    "runtime.applicationExtension must be a lowercase reverse-domain identifier",
                    "/runtime/applicationExtension");
        }
        Optional<Path> entryScene = paths.resolveRequired(raw.entryScene(), "/runtime/entryScene", true);
        Optional<Path> projectSystems = paths.resolveOptional(raw.projectSystems(), "/runtime/projectSystems", true);
        Optional<Path> inputMap = paths.resolveOptional(raw.inputMap(), "/runtime/inputMap", true);
        String safeExtension = isProjectId(extension) ? extension : "invalid.extension";
        Path safeEntryScene = entryScene.orElse(root.resolve("invalid.scene.json"));
        return new GameProject.RuntimeConfiguration(safeExtension, safeEntryScene, projectSystems, inputMap);
    }

    /** Validates extension identifiers, version requirements, and uniqueness. */
    private List<GameProject.ExtensionRequirement> validateExtensions(
            @Nullable List<RawManifest.@Nullable ExtensionRequirement> rawExtensions) {
        if (rawExtensions == null || rawExtensions.isEmpty()) {
            diagnostics.error("project.field.required", "at least one extension is required", "/extensions");
            return List.of();
        }
        List<GameProject.ExtensionRequirement> extensions = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        for (int index = 0; index < rawExtensions.size(); index++) {
            validateExtension(rawExtensions.get(index), index, identifiers).ifPresent(extensions::add);
        }
        return List.copyOf(extensions);
    }

    /** Validates one extension declaration. */
    private Optional<GameProject.ExtensionRequirement> validateExtension(
            RawManifest.@Nullable ExtensionRequirement raw, int index, Set<String> identifiers) {
        String location = "/extensions/" + index;
        if (raw == null) {
            diagnostics.error("project.field.required", "extension must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredText(raw.id(), location + "/id");
        if (!id.isEmpty() && !isProjectId(id)) {
            diagnostics.error(
                    "project.extension.id",
                    "extension id must be a lowercase reverse-domain identifier",
                    location + "/id");
        }
        if (!id.isEmpty() && !identifiers.add(id)) {
            diagnostics.error("project.extension.duplicate", "extension id is duplicated: " + id, location + "/id");
        }
        String requirement = fields.requiredText(raw.requires(), location + "/requires");
        boolean validRequirement = SemanticVersionRequirement.parse(requirement).isPresent();
        if (!requirement.isEmpty() && !validRequirement) {
            diagnostics.error(
                    "project.extension.requirement",
                    "extension requires must contain semantic-version comparisons",
                    location + "/requires");
        }
        if (!isProjectId(id) || !validRequirement) {
            return Optional.empty();
        }
        return Optional.of(new GameProject.ExtensionRequirement(id, requirement));
    }

    /** Validates source-asset identity, path containment, and optional digest. */
    private List<GameProject.AssetSource> validateAssets(@Nullable List<RawManifest.@Nullable Asset> rawAssets) {
        if (rawAssets == null) {
            return List.of();
        }
        List<GameProject.AssetSource> assets = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        for (int index = 0; index < rawAssets.size(); index++) {
            String location = "/assets/" + index;
            RawManifest.@Nullable Asset raw = rawAssets.get(index);
            if (raw == null) {
                diagnostics.error("project.field.required", "asset source must be an object", location);
                continue;
            }
            String id = fields.requiredLocalId(raw.id(), location + "/id");
            if (!id.isEmpty() && !identifiers.add(id)) {
                diagnostics.error("project.asset.duplicate", "asset source id is duplicated: " + id, location + "/id");
            }
            String type = fields.requiredRegisteredTypeId(raw.type(), location + "/type");
            Optional<Path> path = paths.resolveRequired(raw.path(), location + "/path", true);
            Optional<String> digest = validateDigest(raw.sha256(), location + "/sha256");
            if (!id.isEmpty() && !type.isEmpty() && path.isPresent()) {
                assets.add(new GameProject.AssetSource(id, type, path.orElseThrow(), digest));
            }
        }
        return List.copyOf(assets);
    }

    /** Validates optional discovery metadata. */
    private GameProject.Catalog validateCatalog(RawManifest.@Nullable Catalog raw) {
        if (raw == null) {
            return GameProject.Catalog.empty();
        }
        List<String> genres = textList(raw.genres(), "/catalog/genres");
        List<String> tags = textList(raw.tags(), "/catalog/tags");
        Optional<GameProject.PlayerRange> players = validatePlayers(raw.players());
        List<String> warnings = textList(raw.contentWarnings(), "/catalog/contentWarnings");
        return new GameProject.Catalog(genres, tags, players, warnings);
    }

    /** Validates an optional supported player-count range. */
    private Optional<GameProject.PlayerRange> validatePlayers(RawManifest.@Nullable Players raw) {
        if (raw == null) {
            return Optional.empty();
        }
        if (raw.minimum() < 1 || raw.maximum() < raw.minimum()) {
            diagnostics.error(
                    "project.catalog.players", "players must satisfy 1 <= minimum <= maximum", "/catalog/players");
            return Optional.empty();
        }
        return Optional.of(new GameProject.PlayerRange(raw.minimum(), raw.maximum()));
    }

    /** Requires the application extension to be declared by the project. */
    private void validateApplicationExtension(
            String applicationExtension, List<GameProject.ExtensionRequirement> extensions) {
        boolean present =
                extensions.stream().anyMatch(extension -> extension.id().equals(applicationExtension));
        if (!present) {
            diagnostics.error(
                    "project.runtime.extension.missing",
                    "application extension is not declared: " + applicationExtension,
                    "/runtime/applicationExtension");
        }
    }

    /** Validates one optional list of unique project-relative paths. */
    private List<Path> validatePathList(@Nullable List<@Nullable String> values, String location) {
        if (values == null) {
            return List.of();
        }
        List<Path> resolvedPaths = new ArrayList<>();
        Set<Path> unique = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemLocation = location + "/" + index;
            Optional<Path> path = paths.resolveRequired(values.get(index), itemLocation, true);
            if (path.isPresent() && !unique.add(path.orElseThrow())) {
                diagnostics.error("project.path.duplicate", "project path is duplicated", itemLocation);
            } else {
                path.ifPresent(resolvedPaths::add);
            }
        }
        return List.copyOf(resolvedPaths);
    }

    /** Validates one optional SHA-256 digest and normalizes it to lowercase. */
    private Optional<String> validateDigest(@Nullable String value, String location) {
        Optional<String> digest = fields.optionalText(value, location);
        if (digest.isPresent() && !isSha256(digest.orElseThrow())) {
            diagnostics.error("project.asset.sha256", "sha256 must contain exactly 64 hexadecimal digits", location);
            return Optional.empty();
        }
        return digest.map(text -> text.toLowerCase(Locale.ROOT));
    }

    /** Parses one optional ISO date. */
    private Optional<LocalDate> optionalDate(@Nullable String value, String location) {
        Optional<String> text = fields.optionalText(value, location);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(text.orElseThrow()));
        } catch (DateTimeParseException ignored) {
            diagnostics.error("project.field.date", "value must be an ISO-8601 calendar date", location);
            return Optional.empty();
        }
    }

    /** Parses one optional absolute URI. */
    private Optional<URI> optionalUri(@Nullable String value, String location) {
        Optional<String> text = fields.optionalText(value, location);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(text.orElseThrow());
            if (!uri.isAbsolute()) {
                diagnostics.error("project.field.uri", "URI must be absolute", location);
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (URISyntaxException ignored) {
            diagnostics.error("project.field.uri", "value must be a valid absolute URI", location);
            return Optional.empty();
        }
    }

    /** Validates a semantic version when a value is present. */
    private void validateSemanticVersion(String value, String location) {
        if (!value.isEmpty() && SemanticVersion.parse(value).isEmpty()) {
            diagnostics.error("project.field.version", "value must be a semantic version", location);
        }
    }

    /** Validates and copies an optional unique string list. */
    private List<String> textList(@Nullable List<@Nullable String> values, String location) {
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemLocation = location + "/" + index;
            String value = fields.requiredText(values.get(index), itemLocation);
            if (!value.isEmpty() && !unique.add(value)) {
                diagnostics.error("project.field.duplicate", "list value is duplicated: " + value, itemLocation);
            } else if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    /** Validated project and ordered diagnostics returned to the public loader.
     *
     * @param project validated project when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<GameProject> project, List<ProjectDiagnostic> diagnostics) {}
}
