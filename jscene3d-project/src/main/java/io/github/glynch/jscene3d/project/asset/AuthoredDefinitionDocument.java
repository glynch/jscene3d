/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.internal.AtomicProjectFileWriter;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.internal.ProjectJsonTreeWriter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueJsonWriter;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One source-preserving authored world or entity definition.
 *
 * <p>The trusted source, validated JSON tree, immutable domain projection, and persisted-source SHA-256 baseline are
 * established together and cannot drift independently. Candidate property patches deep-copy the accepted tree and
 * become documents only after a normal mixed-source graph load and component validation succeeds. The tree is never
 * exposed. Serialization preserves semantic members, object insertion order, array order, and exact decimal values;
 * whitespace and numeric lexical spelling are normalized.
 */
public final class AuthoredDefinitionDocument {
    private final AssetCatalog assets;
    private final DefinitionResolver definitions;
    private final RegisteredTypeCatalog types;
    private final AssetMetadata metadata;
    private final ObjectNode tree;
    private final Content content;
    private final SourceFingerprint sourceFingerprint;

    /** Stores one already validated, internally owned tree/domain pair. */
    private AuthoredDefinitionDocument(
            AssetCatalog assets,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            AssetMetadata metadata,
            ObjectNode tree,
            Content content,
            SourceFingerprint sourceFingerprint) {
        this.assets = Objects.requireNonNull(assets, "assets");
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.types = Objects.requireNonNull(types, "types");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.tree = Objects.requireNonNull(tree, "tree");
        this.content = Objects.requireNonNull(content, "content");
        this.sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        if (!metadata.id().equals(content.id()) || metadata.kind() != content.kind()) {
            throw new IllegalArgumentException("metadata and definition content must agree");
        }
    }

    /**
     * Loads one authored source from exact bytes into a coherent tree/domain document.
     *
     * <p>The catalog supplies trusted source identity. The supplied resolver is used so mixed authored/generated
     * transitive references are validated exactly as they are during ordinary project loading.
     *
     * @param assets authoritative authored asset catalog
     * @param definitions authoritative mixed-source definition resolver
     * @param types resolved safe component metadata
     * @param id authored world or entity identity
     * @return retained document or ordered structured diagnostics
     */
    public static LoadResult load(
            AssetCatalog assets, DefinitionResolver definitions, RegisteredTypeCatalog types, AssetId id) {
        AssetCatalog validAssets = Objects.requireNonNull(assets, "assets");
        DefinitionResolver validDefinitions = Objects.requireNonNull(definitions, "definitions");
        RegisteredTypeCatalog validTypes = Objects.requireNonNull(types, "types");
        AssetId validId = Objects.requireNonNull(id, "id");
        Optional<AssetMetadata> metadata = validAssets.find(validId);
        if (metadata.isEmpty()) {
            return failure(diagnostic(
                    validAssets.root(),
                    AssetDiagnosticCode.REFERENCE_MISSING,
                    "authored definition is not present in the catalog: " + validId));
        }
        AssetMetadata source = metadata.orElseThrow();
        SourceRead read = readSource(validAssets.root(), source.path());
        if (read.status() != SourceStatus.MATCH) {
            return failure(sourceDiagnostic(source.path(), read));
        }
        return loadBytes(
                validAssets,
                validDefinitions,
                validTypes,
                source,
                read.content().orElseThrow());
    }

    /** Loads already confined exact bytes through the normal tree and definition validation path. */
    private static LoadResult loadBytes(
            AssetCatalog assets,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            AssetMetadata metadata,
            byte[] bytes) {
        ObjectNode tree;
        try {
            JsonNode parsed = ProjectJsonReader.strict().readTree(new ByteArrayInputStream(bytes));
            if (!(parsed instanceof ObjectNode object)) {
                return failure(diagnostic(
                        metadata.path(),
                        AssetDiagnosticCode.JSON_INVALID,
                        "authored definition root must be an object"));
            }
            tree = object;
        } catch (JsonProcessingException exception) {
            return failure(
                    diagnostic(metadata.path(), AssetDiagnosticCode.JSON_INVALID, exception.getOriginalMessage()));
        } catch (IOException exception) {
            return failure(diagnostic(metadata.path(), AssetDiagnosticCode.FILE_READ_FAILED, exception.toString()));
        }
        DefinitionLoadResult<?> loaded = loadCandidate(definitions, types, metadata, bytes);
        if (!loaded.isValid()) {
            return new LoadResult(Optional.empty(), loaded.diagnostics());
        }
        Content content = content(metadata.kind(), loaded.definition().orElseThrow());
        AuthoredDefinitionDocument document = new AuthoredDefinitionDocument(
                assets, definitions, types, metadata, tree, content, SourceFingerprint.sha256(bytes));
        return new LoadResult(Optional.of(document), loaded.diagnostics());
    }

    /**
     * Returns the stable authored asset identity.
     *
     * @return authored definition identity
     */
    public AssetId id() {
        return metadata.id();
    }

    /**
     * Returns the authored structural definition kind.
     *
     * @return world or entity definition kind
     */
    public AssetKind kind() {
        return metadata.kind();
    }

    /**
     * Returns the trusted normalized absolute authored source.
     *
     * <p>Future normal persistence must use this retained identity, never a client-provided destination.
     *
     * @return trusted authored source path
     */
    public Path source() {
        return metadata.path();
    }

    /**
     * Returns the immutable domain projection of the same accepted state as the retained tree.
     *
     * @return authored world or entity content
     */
    public Content content() {
        return content;
    }

    /**
     * Returns the exact-byte fingerprint of the persisted baseline from which this document was loaded.
     *
     * <p>Candidate documents retain this baseline until a later successful save or reload establishes another one.
     *
     * @return persisted source baseline
     */
    public SourceFingerprint sourceFingerprint() {
        return sourceFingerprint;
    }

    /**
     * Returns whether another document represents the same authored tree state.
     *
     * <p>This comparison deliberately excludes object identity, persisted-source fingerprint, and serialized
     * formatting. It is the semantic/tree equality used by authored working copies to derive dirty state.
     *
     * @param other document to compare
     * @return whether both documents contain the same authored definition state
     */
    public boolean hasSameAuthoredState(AuthoredDefinitionDocument other) {
        AuthoredDefinitionDocument validOther = Objects.requireNonNull(other, "other");
        return id().equals(validOther.id()) && kind() == validOther.kind() && tree.equals(validOther.tree);
    }

    /**
     * Returns this authored state associated with another document's persisted-source baseline.
     *
     * <p>The supplied document must identify the same retained source. This operation performs no I/O and does not
     * claim that either document was saved; a future persistence transaction supplies the successfully reloaded or
     * otherwise baselined document.
     *
     * @param persistedBaseline document carrying the established persisted-source fingerprint
     * @return this authored state carrying the supplied persisted-source baseline
     */
    public AuthoredDefinitionDocument withPersistedSourceBaseline(AuthoredDefinitionDocument persistedBaseline) {
        AuthoredDefinitionDocument baseline = Objects.requireNonNull(persistedBaseline, "persistedBaseline");
        if (!id().equals(baseline.id()) || kind() != baseline.kind() || !source().equals(baseline.source())) {
            throw new IllegalArgumentException("persisted baseline must identify the same authored source");
        }
        return new AuthoredDefinitionDocument(
                assets, definitions, types, metadata, tree, content, baseline.sourceFingerprint);
    }

    /**
     * Serializes the retained tree with deterministic pretty printing and one trailing newline.
     *
     * @return detached complete UTF-8 authored JSON
     */
    public byte[] serialize() {
        return serializeTree(tree);
    }

    /**
     * Compares the exact current source bytes with the retained persisted baseline after rechecking confinement.
     *
     * @return match, external content change, or bounded source lifecycle failure
     */
    public SourceStatus verifySource() {
        SourceRead read = readSource(assets.root(), metadata.path());
        if (read.status() != SourceStatus.MATCH) {
            return read.status();
        }
        SourceFingerprint current = SourceFingerprint.sha256(read.content().orElseThrow());
        return sourceFingerprint.equals(current) ? SourceStatus.MATCH : SourceStatus.CONTENT_CHANGED;
    }

    /**
     * Persists the retained source-preserving tree after exact-byte conflict and confinement verification.
     *
     * <p>The existing atomic project writer replaces the already resolved in-project physical source. Success returns
     * a coherent copy carrying the SHA-256 fingerprint of the exact bytes supplied to that writer. This document is
     * never mutated, including when verification or writing fails.
     *
     * @return saved document, source conflict/lifecycle failure, or write failure
     */
    public SaveResult save() {
        SourceRead read = readSource(assets.root(), metadata.path());
        if (read.status() != SourceStatus.MATCH) {
            return new SaveResult.SourceFailure(read.status(), sourceDiagnostic(metadata.path(), read));
        }
        SourceFingerprint physicalFingerprint =
                SourceFingerprint.sha256(read.content().orElseThrow());
        if (!sourceFingerprint.equals(physicalFingerprint)) {
            SourceRead changed = new SourceRead(
                    SourceStatus.CONTENT_CHANGED,
                    Optional.empty(),
                    read.resolvedSource(),
                    "source content differs from the persisted baseline");
            return new SaveResult.SourceFailure(
                    SourceStatus.CONTENT_CHANGED, sourceDiagnostic(metadata.path(), changed));
        }
        byte[] serialized = serialize();
        try {
            AtomicProjectFileWriter.write(read.resolvedSource().orElseThrow(), serialized);
        } catch (IOException | SecurityException exception) {
            return new SaveResult.WriteFailure(
                    diagnostic(metadata.path(), AssetDiagnosticCode.FILE_WRITE_FAILED, exception.toString()));
        }
        AuthoredDefinitionDocument saved = new AuthoredDefinitionDocument(
                assets, definitions, types, metadata, tree, content, SourceFingerprint.sha256(serialized));
        return new SaveResult.Saved(saved);
    }

    /**
     * Reloads the current physical authored source and validates it as a complete replacement document.
     *
     * <p>Unlike save, reload deliberately accepts exact-byte external changes. Source lifecycle failures and invalid
     * documents are returned without changing this document.
     *
     * @return loaded document, source lifecycle failure, or structured validation rejection
     */
    public ReloadResult reload() {
        SourceRead read = readSource(assets.root(), metadata.path());
        if (read.status() != SourceStatus.MATCH) {
            return new ReloadResult.SourceFailure(read.status(), sourceDiagnostic(metadata.path(), read));
        }
        LoadResult loaded =
                loadBytes(assets, definitions, types, metadata, read.content().orElseThrow());
        return loaded.document()
                .<ReloadResult>map(ReloadResult.Loaded::new)
                .orElseGet(() -> new ReloadResult.Rejected(loaded.diagnostics()));
    }

    /**
     * Creates a validated candidate with one component property added or replaced.
     *
     * <p>The accepted document is never mutated. A semantically equal existing value is an accepted no-op.
     *
     * @param target Java-resolved local component-property identity
     * @param value complete structural replacement value
     * @return accepted copied document, structured validation rejection, or invalid target
     */
    public CandidateResult set(ComponentPropertyTarget target, ProjectValue value) {
        ComponentPropertyTarget validTarget = Objects.requireNonNull(target, "target");
        JsonNode encoded = ProjectValueJsonWriter.toJsonNode(Objects.requireNonNull(value, "value"));
        ObjectNode candidateTree = tree.deepCopy();
        Optional<ObjectNode> properties = properties(candidateTree, validTarget);
        if (properties.isEmpty()) {
            return new CandidateResult.InvalidTarget(validTarget, InvalidTargetReason.NOT_LOCAL_COMPONENT);
        }
        ObjectNode values = properties.orElseThrow();
        JsonNode current = values.get(validTarget.property().toString());
        if (encoded.equals(current)) {
            return new CandidateResult.Accepted(this, false);
        }
        values.set(validTarget.property().toString(), encoded);
        return validateCandidate(candidateTree);
    }

    /**
     * Creates a validated candidate with one authored component property removed.
     *
     * <p>An absent member is an accepted no-op. Descriptor defaults are never copied into authored JSON. Required
     * property removal is rejected through the normal complete definition validation path.
     *
     * @param target Java-resolved local component-property identity
     * @return accepted copied document, structured validation rejection, or invalid target
     */
    public CandidateResult remove(ComponentPropertyTarget target) {
        ComponentPropertyTarget validTarget = Objects.requireNonNull(target, "target");
        ObjectNode candidateTree = tree.deepCopy();
        Optional<ObjectNode> properties = properties(candidateTree, validTarget);
        if (properties.isEmpty()) {
            return new CandidateResult.InvalidTarget(validTarget, InvalidTargetReason.NOT_LOCAL_COMPONENT);
        }
        JsonNode removed =
                properties.orElseThrow().remove(validTarget.property().toString());
        if (removed == null) {
            return new CandidateResult.Accepted(this, false);
        }
        return validateCandidate(candidateTree);
    }

    /**
     * Creates a validated candidate with one authored entity or placement enabled state replaced.
     *
     * <p>This preserves the existing Java authoring behavior while routing it through the same source-preserving
     * candidate validation path as component-property mutations.
     *
     * @param entity source-local entity or placement identity
     * @param enabled replacement enabled state
     * @return accepted copied document, structured validation rejection, or invalid target
     */
    public CandidateResult setEntityEnabled(EntityId entity, boolean enabled) {
        EntityId validEntity = Objects.requireNonNull(entity, "entity");
        ObjectNode candidateTree = tree.deepCopy();
        Optional<ObjectNode> entry = entry(candidateTree, validEntity);
        if (entry.isEmpty()) {
            return new CandidateResult.InvalidEntityTarget(validEntity);
        }
        ObjectNode value = entry.orElseThrow();
        boolean current =
                value.path("enabled").isMissingNode() || value.path("enabled").booleanValue();
        if (current == enabled) {
            return new CandidateResult.Accepted(this, false);
        }
        value.put("enabled", enabled);
        return validateCandidate(candidateTree);
    }

    /** Serializes a detached tree through the one retained-document formatting policy. */
    static byte[] serializeTree(JsonNode tree) {
        try {
            return ProjectJsonTreeWriter.serialize(Objects.requireNonNull(tree, "tree"));
        } catch (IOException exception) {
            throw new IllegalStateException("retained authored JSON could not be serialized", exception);
        }
    }

    /** Reparses and fully validates a patched copied tree before constructing its coherent candidate. */
    private CandidateResult validateCandidate(ObjectNode candidateTree) {
        byte[] candidateBytes = serializeTree(candidateTree);
        DefinitionLoadResult<?> loaded = loadCandidate(definitions, types, metadata, candidateBytes);
        if (!loaded.isValid()) {
            return new CandidateResult.Rejected(loaded.diagnostics());
        }
        Content candidateContent = content(metadata.kind(), loaded.definition().orElseThrow());
        return new CandidateResult.Accepted(
                new AuthoredDefinitionDocument(
                        assets, definitions, types, metadata, candidateTree, candidateContent, sourceFingerprint),
                true);
    }

    /** Loads candidate bytes through the existing graph loader for the trusted kind. */
    private static DefinitionLoadResult<?> loadCandidate(
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            AssetMetadata metadata,
            byte[] candidateBytes) {
        return switch (metadata.kind()) {
            case ENTITY_DEFINITION -> definitions.loadAuthoredEntity(metadata, candidateBytes, types);
            case WORLD_DEFINITION -> definitions.loadAuthoredWorld(metadata, candidateBytes, types);
        };
    }

    /** Wraps a typed domain projection after a successful kind-specific load. */
    private static Content content(AssetKind kind, Object definition) {
        return switch (kind) {
            case ENTITY_DEFINITION -> new Content.Entity((EntityDefinition) definition);
            case WORLD_DEFINITION -> new Content.World((WorldDefinition) definition);
        };
    }

    /** Locates the properties object of one local entity component without traversing placements. */
    private Optional<ObjectNode> properties(ObjectNode candidate, ComponentPropertyTarget target) {
        return kind() == AssetKind.WORLD_DEFINITION
                ? propertiesInEntries(candidate.get("roots"), target)
                : propertiesInEntry(candidate.get("root"), target);
    }

    /** Finds one source-local entity or placement by stable identity. */
    private Optional<ObjectNode> entry(ObjectNode candidate, EntityId target) {
        return kind() == AssetKind.WORLD_DEFINITION
                ? entryInEntries(candidate.get("roots"), target)
                : entryInEntry(candidate.get("root"), target);
    }

    /** Searches one authored entry array recursively in source order. */
    private static Optional<ObjectNode> entryInEntries(JsonNode entries, EntityId target) {
        if (entries == null || !entries.isArray()) {
            return Optional.empty();
        }
        for (JsonNode entry : entries) {
            Optional<ObjectNode> found = entryInEntry(entry, target);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Searches one authored entry and locally authored descendants. */
    private static Optional<ObjectNode> entryInEntry(JsonNode entry, EntityId target) {
        if (!(entry instanceof ObjectNode object)) {
            return Optional.empty();
        }
        if (target.toString().equals(object.path("entityId").textValue())) {
            return Optional.of(object);
        }
        return "local".equals(object.path("entryType").textValue())
                ? entryInEntries(object.get("children"), target)
                : Optional.empty();
    }

    /** Searches one authored entry array recursively in source order. */
    private static Optional<ObjectNode> propertiesInEntries(JsonNode entries, ComponentPropertyTarget target) {
        if (entries == null || !entries.isArray()) {
            return Optional.empty();
        }
        for (JsonNode entry : entries) {
            Optional<ObjectNode> found = propertiesInEntry(entry, target);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Searches one local entry and its locally authored descendants. */
    private static Optional<ObjectNode> propertiesInEntry(JsonNode entry, ComponentPropertyTarget target) {
        if (!(entry instanceof ObjectNode object)
                || !"local".equals(object.path("entryType").textValue())) {
            return Optional.empty();
        }
        if (target.entity().toString().equals(object.path("entityId").textValue())) {
            return componentProperties(object.path("components"), target.component());
        }
        return propertiesInEntries(object.get("children"), target);
    }

    /** Finds one component and creates its optional properties object only after identity resolution. */
    private static Optional<ObjectNode> componentProperties(JsonNode components, ComponentId target) {
        if (components == null || !components.isArray()) {
            return Optional.empty();
        }
        for (JsonNode component : components) {
            if (component instanceof ObjectNode object
                    && target.toString().equals(object.path("componentId").textValue())) {
                JsonNode properties = object.get("properties");
                if (properties == null || properties.isNull()) {
                    ObjectNode created = object.objectNode();
                    object.set("properties", created);
                    return Optional.of(created);
                }
                return properties instanceof ObjectNode values ? Optional.of(values) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Reads exact bytes only after resolving the source and project root through real-path confinement. */
    private static SourceRead readSource(Path projectRoot, Path source) {
        try {
            Path realRoot = projectRoot.toRealPath();
            Path realSource = source.toRealPath();
            if (!realSource.startsWith(realRoot)) {
                return new SourceRead(
                        SourceStatus.SOURCE_OUTSIDE_PROJECT,
                        Optional.empty(),
                        Optional.of(realSource),
                        "source escapes project");
            }
            if (!Files.isRegularFile(realSource) || !Files.isReadable(realSource)) {
                return new SourceRead(
                        SourceStatus.SOURCE_INACCESSIBLE,
                        Optional.empty(),
                        Optional.of(realSource),
                        "source is not readable");
            }
            return new SourceRead(
                    SourceStatus.MATCH, Optional.of(Files.readAllBytes(realSource)), Optional.of(realSource), "");
        } catch (NoSuchFileException exception) {
            return new SourceRead(
                    SourceStatus.SOURCE_MISSING, Optional.empty(), Optional.empty(), exception.toString());
        } catch (IOException | SecurityException exception) {
            return new SourceRead(
                    SourceStatus.SOURCE_INACCESSIBLE, Optional.empty(), Optional.empty(), exception.toString());
        }
    }

    /** Creates one structured source diagnostic. */
    private static ProjectDiagnostic sourceDiagnostic(Path source, SourceRead read) {
        AssetDiagnosticCode code =
                switch (read.status()) {
                    case CONTENT_CHANGED -> AssetDiagnosticCode.CATALOG_STALE;
                    case SOURCE_OUTSIDE_PROJECT -> AssetDiagnosticCode.PATH_ESCAPES_ROOT;
                    case SOURCE_MISSING, SOURCE_INACCESSIBLE -> AssetDiagnosticCode.FILE_READ_FAILED;
                    case MATCH -> throw new IllegalArgumentException("matching source has no diagnostic");
                };
        return diagnostic(source, code, read.detail());
    }

    /** Creates one structured load diagnostic with a bounded technical detail. */
    private static ProjectDiagnostic diagnostic(Path source, AssetDiagnosticCode code, String detail) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                code,
                source.toAbsolutePath().normalize().toUri(),
                "",
                Map.of("technicalDetail", Objects.requireNonNullElse(detail, "")));
    }

    /** Creates one failed load result. */
    private static LoadResult failure(ProjectDiagnostic diagnostic) {
        return new LoadResult(Optional.empty(), List.of(diagnostic));
    }

    /** Exact semantic identity of one property on a component authored in this source.
     *
     * @param entity source-local entity identity
     * @param component source-local component identity
     * @param property descriptor property identity
     */
    public record ComponentPropertyTarget(EntityId entity, ComponentId component, PropertyId property) {
        /** Validates the complete Java-resolved identity. */
        public ComponentPropertyTarget {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(component, "component");
            Objects.requireNonNull(property, "property");
        }
    }

    /** Outcome of loading one authored source.
     *
     * @param document coherent retained document when loading succeeded
     * @param diagnostics immutable ordered loading diagnostics
     */
    public record LoadResult(Optional<AuthoredDefinitionDocument> document, List<ProjectDiagnostic> diagnostics) {
        /** Copies the complete load outcome. */
        public LoadResult {
            Objects.requireNonNull(document, "document");
            diagnostics = List.copyOf(diagnostics);
            boolean hasErrors = diagnostics.stream()
                    .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
            if (document.isPresent() == hasErrors) {
                throw new IllegalArgumentException(
                        "a document must be present exactly when diagnostics have no errors");
            }
        }
    }

    /** Closed outcome of patching a copied source tree and reparsing it authoritatively. */
    public sealed interface CandidateResult
            permits CandidateResult.Accepted,
                    CandidateResult.Rejected,
                    CandidateResult.InvalidTarget,
                    CandidateResult.InvalidEntityTarget {
        /** Accepted coherent candidate document.
         *
         * @param document validated candidate or the original document for a no-op
         * @param changed whether serialized semantic source state changed
         */
        record Accepted(AuthoredDefinitionDocument document, boolean changed) implements CandidateResult {
            /** Validates accepted candidate state. */
            public Accepted {
                Objects.requireNonNull(document, "document");
            }
        }

        /** Candidate rejected by normal structured definition validation.
         *
         * @param diagnostics immutable ordered validation diagnostics
         */
        record Rejected(List<ProjectDiagnostic> diagnostics) implements CandidateResult {
            /** Copies non-empty rejection diagnostics. */
            public Rejected {
                diagnostics = List.copyOf(diagnostics);
                if (diagnostics.isEmpty()) {
                    throw new IllegalArgumentException("rejected candidate requires diagnostics");
                }
            }
        }

        /** Candidate target that does not identify editable local source content.
         *
         * @param target requested semantic target
         * @param reason stable invalid-target reason
         */
        record InvalidTarget(ComponentPropertyTarget target, InvalidTargetReason reason) implements CandidateResult {
            /** Validates invalid target detail. */
            public InvalidTarget {
                Objects.requireNonNull(target, "target");
                Objects.requireNonNull(reason, "reason");
            }
        }

        /** Entity or placement identity that is not present in this authored source.
         *
         * @param entity requested source-local identity
         */
        record InvalidEntityTarget(EntityId entity) implements CandidateResult {
            /** Validates the invalid entity identity. */
            public InvalidEntityTarget {
                Objects.requireNonNull(entity, "entity");
            }
        }
    }

    /** Closed outcome of atomically persisting one retained authored document. */
    public sealed interface SaveResult permits SaveResult.Saved, SaveResult.SourceFailure, SaveResult.WriteFailure {
        /** Successful exact-byte persistence.
         *
         * @param document coherent authored state carrying the new persisted fingerprint
         */
        record Saved(AuthoredDefinitionDocument document) implements SaveResult {
            /** Validates the saved document. */
            public Saved {
                Objects.requireNonNull(document, "document");
            }
        }

        /** Source verification rejected persistence before any write.
         *
         * @param status stable source conflict or lifecycle status
         * @param diagnostic structured source diagnostic
         */
        record SourceFailure(SourceStatus status, ProjectDiagnostic diagnostic) implements SaveResult {
            /** Validates a non-matching source failure. */
            public SourceFailure {
                Objects.requireNonNull(status, "status");
                Objects.requireNonNull(diagnostic, "diagnostic");
                if (status == SourceStatus.MATCH) {
                    throw new IllegalArgumentException("source failure cannot report a match");
                }
            }
        }

        /** Atomic writer failure after successful source verification.
         *
         * @param diagnostic structured write diagnostic
         */
        record WriteFailure(ProjectDiagnostic diagnostic) implements SaveResult {
            /** Validates the write diagnostic. */
            public WriteFailure {
                Objects.requireNonNull(diagnostic, "diagnostic");
            }
        }
    }

    /** Closed outcome of reloading and validating the current physical authored source. */
    public sealed interface ReloadResult
            permits ReloadResult.Loaded, ReloadResult.SourceFailure, ReloadResult.Rejected {
        /** Successfully loaded replacement document.
         *
         * @param document coherent validated disk state
         */
        record Loaded(AuthoredDefinitionDocument document) implements ReloadResult {
            /** Validates the loaded document. */
            public Loaded {
                Objects.requireNonNull(document, "document");
            }
        }

        /** Physical source lifecycle failure.
         *
         * @param status stable source lifecycle status
         * @param diagnostic structured source diagnostic
         */
        record SourceFailure(SourceStatus status, ProjectDiagnostic diagnostic) implements ReloadResult {
            /** Validates a source lifecycle failure. */
            public SourceFailure {
                Objects.requireNonNull(status, "status");
                Objects.requireNonNull(diagnostic, "diagnostic");
                if (status == SourceStatus.MATCH || status == SourceStatus.CONTENT_CHANGED) {
                    throw new IllegalArgumentException("reload source failure must be a lifecycle failure");
                }
            }
        }

        /** Physical source parsed or validated unsuccessfully.
         *
         * @param diagnostics immutable ordered validation diagnostics
         */
        record Rejected(List<ProjectDiagnostic> diagnostics) implements ReloadResult {
            /** Copies non-empty rejection diagnostics. */
            public Rejected {
                diagnostics = List.copyOf(diagnostics);
                if (diagnostics.isEmpty()) {
                    throw new IllegalArgumentException("reload rejection requires diagnostics");
                }
            }
        }
    }

    /** Stable reason that a semantic property target cannot be patched in this authored source. */
    public enum InvalidTargetReason {
        /** The entity is a placement, is absent, or does not own the requested local component. */
        NOT_LOCAL_COMPONENT
    }

    /** Result of rechecking the trusted source before future persistence. */
    public enum SourceStatus {
        /** Exact source bytes still match the persisted baseline. */
        MATCH,
        /** Exact source bytes differ from the persisted baseline. */
        CONTENT_CHANGED,
        /** The retained source no longer exists. */
        SOURCE_MISSING,
        /** The retained source cannot be resolved or read. */
        SOURCE_INACCESSIBLE,
        /** The retained source now resolves outside its retained project root. */
        SOURCE_OUTSIDE_PROJECT
    }

    /** Closed immutable domain projection retained beside the accepted source tree. */
    public sealed interface Content permits Content.World, Content.Entity {
        /**
         * Returns the authoritative asset identity.
         *
         * @return definition identity
         */
        AssetId id();

        /**
         * Returns the structural definition kind.
         *
         * @return world or entity kind
         */
        AssetKind kind();

        /** Authored world domain projection.
         *
         * @param definition validated immutable world
         */
        record World(WorldDefinition definition) implements Content {
            /** Validates world content. */
            public World {
                Objects.requireNonNull(definition, "definition");
            }

            @Override
            public AssetId id() {
                return definition.id();
            }

            @Override
            public AssetKind kind() {
                return AssetKind.WORLD_DEFINITION;
            }
        }

        /** Authored reusable entity domain projection.
         *
         * @param definition validated immutable entity definition
         */
        record Entity(EntityDefinition definition) implements Content {
            /** Validates entity content. */
            public Entity {
                Objects.requireNonNull(definition, "definition");
            }

            @Override
            public AssetId id() {
                return definition.id();
            }

            @Override
            public AssetKind kind() {
                return AssetKind.ENTITY_DEFINITION;
            }
        }
    }

    /** Internal bounded result of reading one trusted source. */
    private static final class SourceRead {
        private final SourceStatus status;
        private final Optional<byte[]> content;
        private final Optional<Path> resolvedSource;
        private final String detail;

        /** Stores defensively copied source bytes when reading succeeded. */
        private SourceRead(
                SourceStatus status, Optional<byte[]> content, Optional<Path> resolvedSource, String detail) {
            this.status = Objects.requireNonNull(status, "status");
            this.content = content.map(byte[]::clone);
            this.resolvedSource = Objects.requireNonNull(resolvedSource, "resolvedSource");
            this.detail = Objects.requireNonNull(detail, "detail");
        }

        /** Returns the bounded source state. */
        private SourceStatus status() {
            return status;
        }

        /** Returns a defensive copy of successfully read source bytes. */
        private Optional<byte[]> content() {
            return content.map(byte[]::clone);
        }

        /** Returns the real source resolved during confinement verification. */
        private Optional<Path> resolvedSource() {
            return resolvedSource;
        }

        /** Returns bounded technical detail for a source diagnostic. */
        private String detail() {
            return detail;
        }
    }
}
