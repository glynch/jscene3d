/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.manifest;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableNonBlankStrings;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireLocalId;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireOptionalAbsoluteUri;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireOptionalNonBlank;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireOptionalSha256;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireProjectId;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireRegisteredTypeId;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireSemanticVersion;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireSemanticVersionRequirement;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.immutableNormalizedAbsolutePaths;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireOptionalNormalizedAbsolute;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, validated descriptor for one JScene3D game project. */
public final class GameProject {
    private final Path root;
    private final Metadata metadata;
    private final EngineCompatibility engine;
    private final RuntimeConfiguration runtime;
    private final List<ExtensionRequirement> extensions;
    private final ProjectFiles files;

    /**
     * Creates a validated immutable project descriptor.
     *
     * @param root normalized absolute project directory
     * @param metadata identity and project-browser metadata
     * @param engine engine compatibility
     * @param runtime application startup configuration
     * @param extensions extension requirements in declaration order
     * @param files project content references
     */
    public GameProject(
            Path root,
            Metadata metadata,
            EngineCompatibility engine,
            RuntimeConfiguration runtime,
            List<ExtensionRequirement> extensions,
            ProjectFiles files) {
        this.root = requireNormalizedAbsolute(root, "root");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.extensions = List.copyOf(extensions);
        this.files = Objects.requireNonNull(files, "files");
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
     * Returns application startup configuration.
     *
     * @return runtime configuration
     */
    public RuntimeConfiguration runtime() {
        return runtime;
    }

    /**
     * Returns required project extensions in declaration order.
     *
     * @return immutable extension requirements
     */
    public List<ExtensionRequirement> extensions() {
        return extensions;
    }

    /**
     * Returns authoritative source assets in manifest order.
     *
     * @return immutable source assets
     */
    public List<AssetSource> assets() {
        return files.assets();
    }

    /**
     * Returns import definitions in declaration order.
     *
     * @return immutable normalized absolute import-definition paths
     */
    public List<Path> imports() {
        return files.imports();
    }

    /**
     * Returns export presets in declaration order.
     *
     * @return immutable normalized absolute export-preset paths
     */
    public List<Path> exportPresets() {
        return files.exportPresets();
    }

    /**
     * Returns project content references as one cohesive value.
     *
     * @return immutable project file references
     */
    public ProjectFiles files() {
        return files;
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
            id = requireProjectId(id, "id");
            name = requireNonBlank(name, "name");
            version = requireSemanticVersion(version, "version");
            Objects.requireNonNull(created, "created");
            Objects.requireNonNull(released, "released");
            description = requireOptionalNonBlank(description, "description");
            Objects.requireNonNull(icon, "icon").ifPresent(path -> requireNormalizedAbsolute(path, "icon"));
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
            name = requireNonBlank(name, "name");
            roles = immutableNonBlankStrings(roles, "roles");
            url = requireOptionalAbsoluteUri(url, "url");
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
            homepage = requireOptionalAbsoluteUri(homepage, "homepage");
            source = requireOptionalAbsoluteUri(source, "source");
            issues = requireOptionalAbsoluteUri(issues, "issues");
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
            expression = requireNonBlank(expression, "expression");
            Objects.requireNonNull(file, "file").ifPresent(path -> requireNormalizedAbsolute(path, "file"));
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
                    .ifPresent(path -> requireNormalizedAbsolute(path, "thirdPartyNotices"));
            Objects.requireNonNull(credits, "credits").ifPresent(path -> requireNormalizedAbsolute(path, "credits"));
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
            requirement = requireSemanticVersionRequirement(requirement, "requirement");
            Optional<String> validAuthoredWith = Objects.requireNonNull(authoredWith, "authoredWith");
            validAuthoredWith.ifPresent(value -> requireSemanticVersion(value, "authoredWith"));
            authoredWith = validAuthoredWith;
        }
    }

    /** Runtime entry points used by preview, play, and export workflows. */
    public static final class RuntimeConfiguration {
        private final String applicationExtension;
        private final Path entryScene;
        private final Optional<Path> projectSystems;
        private final Optional<Path> inputMap;

        /**
         * Creates validated runtime configuration.
         *
         * @param applicationExtension extension that creates project-specific runtime objects
         * @param entryScene normalized absolute entry-scene path
         * @param projectSystems optional normalized absolute project-systems path
         * @param inputMap optional normalized absolute input-map path
         */
        public RuntimeConfiguration(
                String applicationExtension, Path entryScene, Optional<Path> projectSystems, Optional<Path> inputMap) {
            this.applicationExtension = requireProjectId(applicationExtension, "applicationExtension");
            this.entryScene = requireNormalizedAbsolute(entryScene, "entryScene");
            this.projectSystems = requireOptionalNormalizedAbsolute(projectSystems, "projectSystems");
            this.inputMap = requireOptionalNormalizedAbsolute(inputMap, "inputMap");
        }

        /**
         * Returns the project-specific application extension identifier.
         *
         * @return application extension identifier
         */
        public String applicationExtension() {
            return applicationExtension;
        }

        /**
         * Returns the initial scene used for preview, play, and export.
         *
         * @return normalized absolute entry-scene path
         */
        public Path entryScene() {
            return entryScene;
        }

        /**
         * Returns the optional project-systems definition.
         *
         * @return normalized absolute project-systems path when configured
         */
        public Optional<Path> projectSystems() {
            return projectSystems;
        }

        /**
         * Returns the optional input-map definition.
         *
         * @return normalized absolute input-map path when configured
         */
        public Optional<Path> inputMap() {
            return inputMap;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof RuntimeConfiguration configuration
                    && applicationExtension.equals(configuration.applicationExtension)
                    && entryScene.equals(configuration.entryScene)
                    && projectSystems.equals(configuration.projectSystems)
                    && inputMap.equals(configuration.inputMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(applicationExtension, entryScene, projectSystems, inputMap);
        }

        @Override
        public String toString() {
            return "RuntimeConfiguration[applicationExtension=" + applicationExtension + ", entryScene=" + entryScene
                    + ", projectSystems=" + projectSystems + ", inputMap=" + inputMap + ']';
        }
    }

    /** Required project extension.
     *
     * @param id stable reverse-domain extension identifier
     * @param requirement semantic-version requirement
     */
    public record ExtensionRequirement(String id, String requirement) {
        /** Validates extension requirement values. */
        public ExtensionRequirement {
            id = requireProjectId(id, "id");
            requirement = requireSemanticVersionRequirement(requirement, "requirement");
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
            id = requireLocalId(id, "id");
            type = requireRegisteredTypeId(type, "type");
            requireNormalizedAbsolute(path, "path");
            sha256 = requireOptionalSha256(sha256, "sha256");
        }
    }

    /** Project files referenced directly by the manifest. */
    public static final class ProjectFiles {
        private final List<AssetSource> assets;
        private final List<Path> imports;
        private final List<Path> exportPresets;

        /**
         * Creates validated immutable project file references.
         *
         * @param assets authoritative source assets
         * @param imports normalized absolute import-definition paths
         * @param exportPresets normalized absolute export-preset paths
         */
        public ProjectFiles(List<AssetSource> assets, List<Path> imports, List<Path> exportPresets) {
            this.assets = List.copyOf(assets);
            this.imports = immutableNormalizedAbsolutePaths(imports, "imports");
            this.exportPresets = immutableNormalizedAbsolutePaths(exportPresets, "exportPresets");
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
         * Returns import definitions in manifest order.
         *
         * @return immutable normalized absolute import paths
         */
        public List<Path> imports() {
            return imports;
        }

        /**
         * Returns export presets in manifest order.
         *
         * @return immutable normalized absolute export-preset paths
         */
        public List<Path> exportPresets() {
            return exportPresets;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof ProjectFiles projectFiles
                    && assets.equals(projectFiles.assets)
                    && imports.equals(projectFiles.imports)
                    && exportPresets.equals(projectFiles.exportPresets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(assets, imports, exportPresets);
        }

        @Override
        public String toString() {
            return "ProjectFiles[assets=" + assets + ", imports=" + imports + ", exportPresets=" + exportPresets + ']';
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
            genres = immutableNonBlankStrings(genres, "genres");
            tags = immutableNonBlankStrings(tags, "tags");
            Objects.requireNonNull(players, "players");
            contentWarnings = immutableNonBlankStrings(contentWarnings, "contentWarnings");
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
}
