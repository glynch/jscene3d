/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.project.GameProject;
import io.github.glynch.jscene3d.project.ProjectDiagnostic;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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

    /** Stores one validation context. */
    private ManifestValidator(Path root, Path source, SemanticVersion engineVersion, String engineVersionText) {
        this.root = root;
        this.engineVersion = engineVersion;
        this.engineVersionText = engineVersionText;
        diagnostics = new DiagnosticCollector(source);
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
        List<GameProject.AssetSource> assets = validateAssets(raw.assets());
        GameProject.Catalog catalog = validateCatalog(raw.catalog());
        validateStartupAsset(runtime.startup(), assets);
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        GameProject.Metadata metadata = new GameProject.Metadata(identity, authors, links, legal, catalog);
        return Optional.of(new GameProject(root, metadata, engine, runtime, assets));
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
        String id = requiredText(raw.id(), "/identity/id");
        if (!id.isEmpty() && !isProjectId(id)) {
            diagnostics.error(
                    "project.identity.id", "identity.id must be a lowercase reverse-domain identifier", "/identity/id");
        }
        String name = requiredText(raw.name(), "/identity/name");
        String version = requiredText(raw.version(), "/identity/version");
        validateSemanticVersion(version, "/identity/version");
        Optional<LocalDate> created = optionalDate(raw.created(), "/identity/created");
        Optional<LocalDate> released = optionalDate(raw.released(), "/identity/released");
        Optional<String> description = optionalText(raw.description(), "/identity/description");
        Optional<Path> icon = optionalPath(raw.icon(), "/identity/icon", true);
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
        if (id.isEmpty() || name.isEmpty() || version.isEmpty()) {
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
            String name = requiredText(raw.name(), location + "/name");
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
        Optional<Path> notices = optionalPath(raw.thirdPartyNotices(), "/legal/thirdPartyNotices", true);
        Optional<Path> credits = optionalPath(raw.credits(), "/legal/credits", true);
        return new GameProject.Legal(license, notices, credits);
    }

    /** Validates one optional project license. */
    private Optional<GameProject.ProjectLicense> validateLicense(RawManifest.@Nullable ProjectLicense raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String expression = requiredText(raw.expression(), "/legal/projectLicense/expression");
        Optional<Path> file = optionalPath(raw.file(), "/legal/projectLicense/file", true);
        return expression.isEmpty() ? Optional.empty() : Optional.of(new GameProject.ProjectLicense(expression, file));
    }

    /** Validates syntax and current compatibility for the engine requirement. */
    private GameProject.EngineCompatibility validateEngine(RawManifest.@Nullable Engine raw) {
        if (raw == null) {
            diagnostics.error("project.field.required", "engine is required", "/engine");
            raw = new RawManifest.Engine(null, null);
        }
        String requirementText = requiredText(raw.requires(), "/engine/requires");
        Optional<EngineVersionRequirement> requirement = EngineVersionRequirement.parse(requirementText);
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
        Optional<String> authoredWith = optionalText(raw.authoredWith(), "/engine/authoredWith");
        authoredWith.ifPresent(value -> validateSemanticVersion(value, "/engine/authoredWith"));
        String safeRequirement = requirementText.isEmpty() ? "0.0.0" : requirementText;
        return new GameProject.EngineCompatibility(safeRequirement, authoredWith);
    }

    /** Validates the Game Provider, startup target, and optional input map. */
    private GameProject.RuntimeConfiguration validateRuntime(RawManifest.@Nullable RuntimeConfiguration raw) {
        if (raw == null) {
            diagnostics.error("project.field.required", "runtime is required", "/runtime");
            raw = new RawManifest.RuntimeConfiguration(null, null, null);
        }
        String provider = requiredText(raw.gameProvider(), "/runtime/gameProvider");
        if (!provider.isEmpty() && !isProjectId(provider)) {
            diagnostics.error(
                    "project.runtime.provider",
                    "runtime.gameProvider must be a lowercase reverse-domain identifier",
                    "/runtime/gameProvider");
        }
        GameProject.StartupTarget startup = validateStartup(raw.startup());
        Optional<Path> inputMap = optionalPath(raw.inputMap(), "/runtime/inputMap", true);
        String safeProvider = provider.isEmpty() ? "invalid.provider" : provider;
        return new GameProject.RuntimeConfiguration(safeProvider, startup, inputMap);
    }

    /** Validates the generic startup source and importer-specific target. */
    private GameProject.StartupTarget validateStartup(RawManifest.@Nullable Startup raw) {
        if (raw == null) {
            diagnostics.error("project.field.required", "runtime.startup is required", "/runtime/startup");
            raw = new RawManifest.Startup(null, null);
        }
        String asset = requiredLocalId(raw.asset(), "/runtime/startup/asset");
        String target = requiredText(raw.target(), "/runtime/startup/target");
        return new GameProject.StartupTarget(
                asset.isEmpty() ? "invalid" : asset, target.isEmpty() ? "invalid" : target);
    }

    /** Validates source-asset identity, path containment, and optional digest. */
    private List<GameProject.AssetSource> validateAssets(@Nullable List<RawManifest.@Nullable Asset> rawAssets) {
        if (rawAssets == null || rawAssets.isEmpty()) {
            diagnostics.error("project.field.required", "at least one asset source is required", "/assets");
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
            String id = requiredLocalId(raw.id(), location + "/id");
            if (!id.isEmpty() && !identifiers.add(id)) {
                diagnostics.error("project.asset.duplicate", "asset source id is duplicated: " + id, location + "/id");
            }
            String type = requiredLocalId(raw.type(), location + "/type");
            Optional<Path> path = requiredPath(raw.path(), location + "/path", true);
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

    /** Requires the startup source identifier to resolve inside the project descriptor. */
    private void validateStartupAsset(GameProject.StartupTarget startup, List<GameProject.AssetSource> assets) {
        boolean present = assets.stream().anyMatch(asset -> asset.id().equals(startup.asset()));
        if (!present) {
            diagnostics.error(
                    "project.startup.asset",
                    "startup asset does not identify a declared source: " + startup.asset(),
                    "/runtime/startup/asset");
        }
    }

    /** Validates one optional SHA-256 digest and normalizes it to lowercase. */
    private Optional<String> validateDigest(@Nullable String value, String location) {
        Optional<String> digest = optionalText(value, location);
        if (digest.isPresent() && !isSha256(digest.orElseThrow())) {
            diagnostics.error("project.asset.sha256", "sha256 must contain exactly 64 hexadecimal digits", location);
            return Optional.empty();
        }
        return digest.map(text -> text.toLowerCase(Locale.ROOT));
    }

    /** Requires a non-blank string, returning a placeholder while collecting errors. */
    private String requiredText(@Nullable String value, String location) {
        if (value == null || value.isBlank()) {
            diagnostics.error("project.field.required", "a non-blank value is required", location);
            return "";
        }
        return value.strip();
    }

    /** Requires a portable lowercase identifier. */
    private String requiredLocalId(@Nullable String value, String location) {
        String identifier = requiredText(value, location);
        if (!identifier.isEmpty() && !isLocalId(identifier)) {
            diagnostics.error("project.field.identifier", "value must be a portable lowercase identifier", location);
        }
        return identifier;
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
                || !isAsciiLowercase(value.charAt(0))
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

    /** Recognizes exactly 64 ASCII hexadecimal digits. */
    private static boolean isSha256(String value) {
        if (value.length() != 64) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!isAsciiDigit(character)
                    && (character < 'a' || character > 'f')
                    && (character < 'A' || character > 'F')) {
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

    /** Returns an optional non-blank string. */
    private Optional<String> optionalText(@Nullable String value, String location) {
        if (value == null) {
            return Optional.empty();
        }
        if (value.isBlank()) {
            diagnostics.error("project.field.blank", "optional values must not be blank", location);
            return Optional.empty();
        }
        return Optional.of(value.strip());
    }

    /** Parses one optional ISO date. */
    private Optional<LocalDate> optionalDate(@Nullable String value, String location) {
        Optional<String> text = optionalText(value, location);
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
        Optional<String> text = optionalText(value, location);
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
            String value = requiredText(values.get(index), itemLocation);
            if (!value.isEmpty() && !unique.add(value)) {
                diagnostics.error("project.field.duplicate", "list value is duplicated: " + value, itemLocation);
            } else if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    /** Resolves an optional project-relative path. */
    private Optional<Path> optionalPath(@Nullable String value, String location, boolean warnIfMissing) {
        Optional<String> text = optionalText(value, location);
        return text.isEmpty() ? Optional.empty() : resolvePath(text.orElseThrow(), location, warnIfMissing);
    }

    /** Resolves a required project-relative path. */
    private Optional<Path> requiredPath(@Nullable String value, String location, boolean warnIfMissing) {
        String text = requiredText(value, location);
        return text.isEmpty() ? Optional.empty() : resolvePath(text, location, warnIfMissing);
    }

    /** Confines one portable manifest path to the project root. */
    private Optional<Path> resolvePath(String value, String location, boolean warnIfMissing) {
        if (value.indexOf('\\') >= 0) {
            diagnostics.error("project.path.portable", "project paths must use forward slashes", location);
            return Optional.empty();
        }
        try {
            Path relative = Path.of(value);
            if (relative.isAbsolute()) {
                diagnostics.error("project.path.absolute", "project path must be relative", location);
                return Optional.empty();
            }
            Path resolved = root.resolve(relative).normalize();
            if (!resolved.startsWith(root)) {
                diagnostics.error("project.path.escape", "project path escapes the project directory", location);
                return Optional.empty();
            }
            if (!validateExistingPath(resolved, location)) {
                return Optional.empty();
            }
            if (warnIfMissing && Files.notExists(resolved)) {
                diagnostics.warning(
                        "project.path.missing", "referenced project path does not exist: " + value, location);
            }
            return Optional.of(resolved);
        } catch (InvalidPathException ignored) {
            diagnostics.error("project.path.invalid", "project path is invalid", location);
            return Optional.empty();
        }
    }

    /** Rejects an existing symlink target outside the real project directory. */
    private boolean validateExistingPath(Path resolved, String location) {
        if (Files.notExists(resolved)) {
            return true;
        }
        try {
            if (!resolved.toRealPath().startsWith(root.toRealPath())) {
                diagnostics.error(
                        "project.path.escape", "project path resolves outside the project directory", location);
                return false;
            }
            return true;
        } catch (IOException exception) {
            diagnostics.error(
                    "project.path.read", "project path cannot be resolved: " + exception.getMessage(), location);
            return false;
        }
    }

    /** Validated project and ordered diagnostics returned to the public loader.
     *
     * @param project validated project when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<GameProject> project, List<ProjectDiagnostic> diagnostics) {}
}
