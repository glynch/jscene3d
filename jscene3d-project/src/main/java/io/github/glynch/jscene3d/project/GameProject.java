/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, validated descriptor for one JScene3D game project. */
public final class GameProject {
    private final Path root;
    private final Metadata metadata;
    private final EngineCompatibility engine;
    private final RuntimeConfiguration runtime;
    private final List<AssetSource> assets;

    /**
     * Creates a validated immutable project descriptor.
     *
     * @param root normalized absolute project directory
     * @param metadata identity and project-browser metadata
     * @param engine engine compatibility
     * @param runtime Game Provider and startup configuration
     * @param assets authoritative source assets
     */
    public GameProject(
            Path root,
            Metadata metadata,
            EngineCompatibility engine,
            RuntimeConfiguration runtime,
            List<AssetSource> assets) {
        this.root = requireAbsolute(root, "root");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.assets = List.copyOf(assets);
    }

    /**
     * Returns the normalized absolute project directory.
     *
     * @return project root
     */
    public Path root() {
        return root;
    }

    /**
     * Returns stable project identity and display metadata.
     *
     * @return project identity
     */
    public Identity identity() {
        return metadata.identity();
    }

    /**
     * Returns principal project authors in manifest order.
     *
     * @return immutable authors
     */
    public List<Author> authors() {
        return metadata.authors();
    }

    /**
     * Returns optional public project links.
     *
     * @return project links
     */
    public Links links() {
        return metadata.links();
    }

    /**
     * Returns project-level legal-document references.
     *
     * @return legal information
     */
    public Legal legal() {
        return metadata.legal();
    }

    /**
     * Returns declared engine compatibility.
     *
     * @return engine compatibility
     */
    public EngineCompatibility engine() {
        return engine;
    }

    /**
     * Returns game-provider and startup configuration.
     *
     * @return runtime configuration
     */
    public RuntimeConfiguration runtime() {
        return runtime;
    }

    /**
     * Returns authoritative source assets in manifest order.
     *
     * @return immutable source assets
     */
    public List<AssetSource> assets() {
        return assets;
    }

    /**
     * Returns optional project-browser catalog metadata.
     *
     * @return catalog metadata
     */
    public Catalog catalog() {
        return metadata.catalog();
    }

    /**
     * Returns identity and project-browser metadata as one cohesive value.
     *
     * @return immutable project metadata
     */
    public Metadata metadata() {
        return metadata;
    }

    /** Identity, attribution, links, legal references, and catalog information.
     *
     * @param identity stable project identity
     * @param authors principal authors in display order
     * @param links public project links
     * @param legal legal-document references
     * @param catalog project-browser catalog metadata
     */
    public record Metadata(Identity identity, List<Author> authors, Links links, Legal legal, Catalog catalog) {
        /** Validates and copies project metadata. */
        public Metadata {
            Objects.requireNonNull(identity, "identity");
            authors = List.copyOf(authors);
            Objects.requireNonNull(links, "links");
            Objects.requireNonNull(legal, "legal");
            Objects.requireNonNull(catalog, "catalog");
        }
    }

    /** Stable project identity and project-browser metadata.
     *
     * @param id permanent reverse-domain project identifier
     * @param name human-readable project name
     * @param version project release version independent of the manifest schema
     * @param created optional original creation date
     * @param released optional date of this project release
     * @param description optional project description
     * @param icon optional normalized absolute icon path
     */
    public record Identity(
            String id,
            String name,
            String version,
            Optional<LocalDate> created,
            Optional<LocalDate> released,
            Optional<String> description,
            Optional<Path> icon) {
        /** Validates immutable identity values. */
        public Identity {
            requireText(id, "id");
            requireText(name, "name");
            requireText(version, "version");
            Objects.requireNonNull(created, "created");
            Objects.requireNonNull(released, "released");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(icon, "icon").ifPresent(path -> requireAbsolute(path, "icon"));
        }
    }

    /** Principal author displayed by project browsers and credits interfaces.
     *
     * @param name author or organization name
     * @param roles immutable descriptive roles
     * @param url optional public author URL
     */
    public record Author(String name, List<String> roles, Optional<URI> url) {
        /** Validates and copies author values. */
        public Author {
            requireText(name, "name");
            roles = copyTextList(roles, "roles");
            Objects.requireNonNull(url, "url");
        }
    }

    /** Public project links.
     *
     * @param homepage optional project homepage
     * @param source optional source repository
     * @param issues optional issue tracker
     */
    public record Links(Optional<URI> homepage, Optional<URI> source, Optional<URI> issues) {
        /** Validates optional links. */
        public Links {
            Objects.requireNonNull(homepage, "homepage");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(issues, "issues");
        }

        /**
         * Returns empty project links.
         *
         * @return links with no declared values
         */
        public static Links empty() {
            return new Links(Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    /** Project license expression and optional license document.
     *
     * @param expression SPDX project-license expression
     * @param file optional normalized absolute license-document path
     */
    public record ProjectLicense(String expression, Optional<Path> file) {
        /** Validates license values. */
        public ProjectLicense {
            requireText(expression, "expression");
            Objects.requireNonNull(file, "file").ifPresent(path -> requireAbsolute(path, "file"));
        }
    }

    /** Project-level legal references separate from per-asset provenance.
     *
     * @param projectLicense optional project source license
     * @param thirdPartyNotices optional normalized absolute third-party-notices path
     * @param credits optional normalized absolute credits path
     */
    public record Legal(
            Optional<ProjectLicense> projectLicense, Optional<Path> thirdPartyNotices, Optional<Path> credits) {
        /** Validates legal references. */
        public Legal {
            Objects.requireNonNull(projectLicense, "projectLicense");
            Objects.requireNonNull(thirdPartyNotices, "thirdPartyNotices")
                    .ifPresent(path -> requireAbsolute(path, "thirdPartyNotices"));
            Objects.requireNonNull(credits, "credits").ifPresent(path -> requireAbsolute(path, "credits"));
        }

        /**
         * Returns empty legal information.
         *
         * @return legal information with no declared documents
         */
        public static Legal empty() {
            return new Legal(Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    /** JScene3D engine compatibility declared by the project.
     *
     * @param requirement version-requirement expression
     * @param authoredWith optional engine version last used to author the project
     */
    public record EngineCompatibility(String requirement, Optional<String> authoredWith) {
        /** Validates engine compatibility values. */
        public EngineCompatibility {
            requireText(requirement, "requirement");
            Objects.requireNonNull(authoredWith, "authoredWith").ifPresent(value -> requireText(value, "authoredWith"));
        }
    }

    /** Generic startup target within one source asset.
     *
     * @param asset source-asset identifier
     * @param target importer-specific target identifier
     */
    public record StartupTarget(String asset, String target) {
        /** Validates startup identifiers. */
        public StartupTarget {
            requireText(asset, "asset");
            requireText(target, "target");
        }
    }

    /** Game Provider and startup configuration.
     *
     * @param gameProvider stable Game Provider identifier
     * @param startup initial source asset and target
     * @param inputMap optional normalized absolute input-map path
     */
    public record RuntimeConfiguration(String gameProvider, StartupTarget startup, Optional<Path> inputMap) {
        /** Validates runtime configuration. */
        public RuntimeConfiguration {
            requireText(gameProvider, "gameProvider");
            Objects.requireNonNull(startup, "startup");
            Objects.requireNonNull(inputMap, "inputMap").ifPresent(path -> requireAbsolute(path, "inputMap"));
        }
    }

    /** Authoritative external or project-authored source asset.
     *
     * @param id stable project-local asset identifier
     * @param type importer type identifier
     * @param path normalized absolute source path
     * @param sha256 optional lowercase SHA-256 digest
     */
    public record AssetSource(String id, String type, Path path, Optional<String> sha256) {
        /** Validates source-asset values. */
        public AssetSource {
            requireText(id, "id");
            requireText(type, "type");
            requireAbsolute(path, "path");
            Objects.requireNonNull(sha256, "sha256");
        }
    }

    /** Supported player-count range.
     *
     * @param minimum positive minimum player count
     * @param maximum player count not below the minimum
     */
    public record PlayerRange(int minimum, int maximum) {
        /** Validates the player range. */
        public PlayerRange {
            if (minimum < 1 || maximum < minimum) {
                throw new IllegalArgumentException(
                        "player range must satisfy 1 <= minimum <= maximum: " + minimum + ".." + maximum);
            }
        }
    }

    /** Optional metadata used for project discovery and catalog presentation.
     *
     * @param genres immutable project genres
     * @param tags immutable discovery tags
     * @param players optional supported player-count range
     * @param contentWarnings immutable content warnings
     */
    public record Catalog(
            List<String> genres, List<String> tags, Optional<PlayerRange> players, List<String> contentWarnings) {
        /** Validates and copies catalog values. */
        public Catalog {
            genres = copyTextList(genres, "genres");
            tags = copyTextList(tags, "tags");
            Objects.requireNonNull(players, "players");
            contentWarnings = copyTextList(contentWarnings, "contentWarnings");
        }

        /**
         * Returns empty catalog metadata.
         *
         * @return catalog with no declared values
         */
        public static Catalog empty() {
            return new Catalog(List.of(), List.of(), Optional.empty(), List.of());
        }
    }

    /** Requires a non-blank public string value. */
    private static String requireText(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }

    /** Copies and validates an immutable string list. */
    private static List<String> copyTextList(List<String> values, String name) {
        Objects.requireNonNull(values, name);
        List<String> copied = new ArrayList<>(values.size());
        for (String value : values) {
            copied.add(requireText(value, name + " entry"));
        }
        return List.copyOf(copied);
    }

    /** Requires a normalized absolute path. */
    private static Path requireAbsolute(Path path, String name) {
        Path validPath = Objects.requireNonNull(path, name);
        if (!validPath.isAbsolute() || !validPath.equals(validPath.normalize())) {
            throw new IllegalArgumentException(name + " must be a normalized absolute path: " + validPath);
        }
        return validPath;
    }
}
