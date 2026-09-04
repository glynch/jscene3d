/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.importing.ImportCancelledException;
import io.github.glynch.jscene3d.project.importing.ImportExecution;
import io.github.glynch.jscene3d.project.importing.ImportPhase;
import io.github.glynch.jscene3d.project.importing.ImportPreview;
import io.github.glynch.jscene3d.project.importing.ImportProgress;
import io.github.glynch.jscene3d.project.importing.ImportState;
import io.github.glynch.jscene3d.project.importing.ImportStatus;
import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import io.github.glynch.jscene3d.project.importing.PreparedImport;
import io.github.glynch.jscene3d.project.importing.SourceInspection;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.importing.SourceItemRelation;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Deep implementation of deterministic inspection, preparation, status, and artifact access. */
public final class ImportCoordinator {
    private static final String EMPTY_FINGERPRINT = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private final GameProject project;
    private final CacheStore cache;
    private final ImporterBindings bindings;
    private final CacheIndexCodec codec;

    /**
     * Creates one project-scoped coordinator.
     *
     * @param project containing project
     * @param cache cache storage policy
     * @param bindings trusted importer implementations
     */
    public ImportCoordinator(GameProject project, CacheStore cache, ImporterBindings bindings) {
        this.project = Objects.requireNonNull(project, "project");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        codec = cache.codec();
    }

    /**
     * Inspects one source with its registered format adapter.
     *
     * @param assetId project asset identity
     * @param importerId registered importer identity
     * @param execution caller-owned execution policy
     * @return completed source inspection
     */
    public SourceInspection inspect(String assetId, String importerId, ImportExecution execution) {
        GameProject.AssetSource asset = requireAsset(assetId);
        ImporterBinding binding = requireBinding(importerId);
        execution.cancellation().checkCancelled();
        execution
                .progressReporter()
                .report(ImportProgress.phase(ImportPhase.INSPECTING, "Inspecting source asset " + asset.id()));
        InspectionContext context = new InspectionContext(project, asset, execution);
        invokeInspection(binding, context);
        String sourceFingerprint = fingerprintSource(asset, context);
        return new SourceInspection(
                binding.type(), sourceFingerprint, context.dependencies(), context.diagnostics(), context.items());
    }

    /**
     * Evaluates one import definition against its active published generation.
     *
     * @param definition import definition to evaluate
     * @return current import status
     */
    public ImportStatus status(ImportDefinition definition) {
        ImportDefinition validDefinition = requireDefinition(definition);
        Optional<CacheStore.ActiveGeneration> active;
        try {
            active = cache.active(validDefinition.id());
        } catch (IOException | RuntimeException exception) {
            return blocked(validDefinition, Optional.empty(), "import.cache.read", failureMessage(exception));
        }
        Optional<String> activeFingerprint =
                active.map(generation -> generation.index().fingerprint());
        Optional<ImporterBinding> binding = bindings.find(validDefinition.importer());
        if (binding.isEmpty()) {
            return blocked(
                    validDefinition,
                    activeFingerprint,
                    "import.importer.missing",
                    "importer implementation is unavailable: " + validDefinition.importer());
        }
        if (active.isEmpty()) {
            return sourceAvailable(validDefinition)
                    ? new ImportStatus(ImportState.MISSING, Optional.empty(), List.of())
                    : blocked(
                            validDefinition,
                            Optional.empty(),
                            "import.source.missing",
                            "source asset is unavailable: "
                                    + validDefinition.asset().path());
        }
        try {
            boolean current = isCurrent(
                    validDefinition, binding.orElseThrow(), active.orElseThrow().index());
            return new ImportStatus(current ? ImportState.CURRENT : ImportState.STALE, activeFingerprint, List.of());
        } catch (IOException exception) {
            return blocked(validDefinition, activeFingerprint, "import.status.read", failureMessage(exception));
        }
    }

    /**
     * Prepares one complete candidate generation without publishing it.
     *
     * @param definition import definition to prepare
     * @param execution caller-owned execution policy
     * @return owned prepared import transaction
     */
    public PreparedImport prepare(ImportDefinition definition, ImportExecution execution) {
        ImportDefinition validDefinition = requireDefinition(definition);
        ImporterBinding binding = requireBinding(validDefinition.importer());
        execution.cancellation().checkCancelled();
        execution
                .progressReporter()
                .report(ImportProgress.phase(ImportPhase.PREPARING, "Preparing import " + validDefinition.id()));
        TemporaryWorkspace workspace = cache.createStagingWorkspace(validDefinition.id());
        try {
            return prepareInWorkspace(validDefinition, binding, execution, workspace);
        } catch (RuntimeException exception) {
            workspace.close();
            throw exception;
        }
    }

    /**
     * Opens an artifact from the last successfully published generation, including when stale.
     *
     * @param definition owning import definition
     * @param identity importer-local artifact identity
     * @return owned artifact handle when present
     */
    public Optional<ImportedArtifact> openArtifact(ImportDefinition definition, String identity) {
        ImportDefinition validDefinition = requireDefinition(definition);
        String validIdentity = Preconditions.requirePortableIdentity(identity, "identity");
        try {
            Optional<CacheStore.ActiveGeneration> active = cache.active(validDefinition.id());
            if (active.isEmpty()) {
                return Optional.empty();
            }
            CacheStore.ActiveGeneration generation = active.orElseThrow();
            Optional<CachedArtifact> artifact = generation.index().artifacts().stream()
                    .filter(candidate -> candidate.identity().equals(validIdentity))
                    .findFirst();
            if (artifact.isEmpty()) {
                return Optional.empty();
            }
            CachedArtifact cachedArtifact = artifact.orElseThrow();
            return Optional.of(new InternalImportedArtifact(
                    codec.metadata(cachedArtifact), cache.artifactPath(generation, cachedArtifact)));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to open imported artifact " + validIdentity, exception);
        }
    }

    /** Invokes adapter inspection and converts operational failures to diagnostics. */
    private static void invokeInspection(ImporterBinding binding, InspectionContext context) {
        try {
            binding.importer().inspect(context);
        } catch (IOException | RuntimeException exception) {
            context.error("import.inspect.failed", "source inspection failed: " + failureMessage(exception), "");
        }
    }

    /** Completes preparation, graph validation, fingerprinting, and cache-index writing. */
    private PreparedImport prepareInWorkspace(
            ImportDefinition definition,
            ImporterBinding binding,
            ImportExecution execution,
            TemporaryWorkspace workspace) {
        PreparationContext context = new PreparationContext(project, definition, execution, workspace);
        String sourceFingerprint = fingerprintSource(definition.asset(), context);
        String definitionFingerprint = ImportHashes.definition(definition, binding.type());
        invokePreparation(binding, context);
        validateSelection(context);
        validateArtifactGraph(context);
        execution.cancellation().checkCancelled();
        execution
                .progressReporter()
                .report(ImportProgress.phase(ImportPhase.VALIDATING, "Validating import " + definition.id()));
        ImportFingerprints fingerprints = new ImportFingerprints(
                definitionFingerprint,
                sourceFingerprint,
                ImportHashes.complete(
                        definitionFingerprint, sourceFingerprint, project.root(), context.dependencies()));
        List<ImportedArtifactMetadata> artifacts =
                context.artifacts().stream().map(StagedArtifact::metadata).toList();
        long estimatedSize =
                artifacts.stream().mapToLong(ImportedArtifactMetadata::size).sum();
        ImportPreview initialPreview =
                new ImportPreview(fingerprints.complete(), context.diagnostics(), artifacts, estimatedSize);
        if (!initialPreview.isValid()) {
            return new InternalPreparedImport(context, binding, execution, cache, initialPreview, Optional.empty());
        }
        return createPublishable(definition, binding, execution, workspace, context, fingerprints, initialPreview);
    }

    /** Writes the generated index and converts an index failure into an invalid preview. */
    private PreparedImport createPublishable(
            ImportDefinition definition,
            ImporterBinding binding,
            ImportExecution execution,
            TemporaryWorkspace workspace,
            PreparationContext context,
            ImportFingerprints fingerprints,
            ImportPreview preview) {
        CachedImportIndex index = codec.fromPreparation(context, binding.type(), fingerprints);
        try {
            cache.writeIndex(workspace.root(), index);
            return new InternalPreparedImport(context, binding, execution, cache, preview, Optional.of(index));
        } catch (IOException exception) {
            List<ProjectDiagnostic> diagnostics = new ArrayList<>(preview.diagnostics());
            diagnostics.add(error(
                    definition,
                    "import.cache.write",
                    "prepared cache index cannot be written: " + failureMessage(exception)));
            ImportPreview invalid = new ImportPreview(
                    fingerprints.complete(), diagnostics, preview.artifacts(), preview.estimatedSize());
            return new InternalPreparedImport(context, binding, execution, cache, invalid, Optional.empty());
        }
    }

    /** Invokes one preparation adapter while preserving cancellation as a distinct outcome. */
    private static void invokePreparation(ImporterBinding binding, PreparationContext context) {
        try {
            binding.importer().prepare(context);
        } catch (ImportCancelledException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            context.error("import.prepare.failed", "source import failed: " + failureMessage(exception), "");
        }
    }

    /** Reports artifact references that do not resolve within the prepared output set. */
    private static void validateArtifactGraph(PreparationContext context) {
        Map<String, StagedArtifact> artifacts = new LinkedHashMap<>();
        context.artifacts()
                .forEach(artifact -> artifacts.put(artifact.metadata().identity(), artifact));
        for (StagedArtifact artifact : artifacts.values()) {
            for (String reference : artifact.metadata().descriptor().references()) {
                if (!artifacts.containsKey(reference)) {
                    context.error(
                            "import.artifact.reference",
                            "artifact " + artifact.metadata().identity() + " references missing output " + reference,
                            artifact.metadata().identity());
                }
            }
        }
    }

    /** Validates authored roots and item settings against the source graph discovered during preparation. */
    private static void validateSelection(PreparationContext context) {
        Map<String, SourceItem> items = new LinkedHashMap<>();
        context.items().forEach(item -> items.put(item.identity(), item));
        validateRelations(context, items);
        Set<String> reachable = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        for (int index = 0; index < context.definition().selection().size(); index++) {
            String identity = context.definition().selection().get(index);
            SourceItem item = items.get(identity);
            String location = "/selection/" + index;
            if (item == null) {
                context.definitionError(
                        "import.selection.missing", "selected source item no longer exists: " + identity, location);
            } else if (!item.isSelectable()) {
                context.definitionError(
                        "import.selection.not-selectable", "source item cannot be selected: " + identity, location);
            } else {
                pending.add(identity);
            }
        }
        collectReachable(items, pending, reachable);
        context.definition().itemSettings().keySet().forEach(identity -> {
            String location = "/itemSettings/" + identity.replace("~", "~0").replace("/", "~1");
            if (!items.containsKey(identity)) {
                context.definitionError(
                        "import.item-settings.missing",
                        "configured source item no longer exists: " + identity,
                        location);
            } else if (!reachable.contains(identity)) {
                context.definitionWarning(
                        "import.item-settings.unused",
                        "source item is outside the selected dependency closure: " + identity,
                        location);
            }
        });
    }

    /** Reports relationships whose target identities are absent from the discovered graph. */
    private static void validateRelations(PreparationContext context, Map<String, SourceItem> items) {
        for (SourceItem item : items.values()) {
            for (SourceItemRelation relation : item.relations()) {
                if (!items.containsKey(relation.targetIdentity())) {
                    context.error(
                            "import.source-item.reference",
                            "source item " + item.identity() + " references missing item " + relation.targetIdentity(),
                            item.identity());
                }
            }
        }
    }

    /** Computes the transitive relationship closure from valid selected roots. */
    private static void collectReachable(Map<String, SourceItem> items, Deque<String> pending, Set<String> reachable) {
        while (!pending.isEmpty()) {
            String identity = pending.removeFirst();
            if (!reachable.add(identity)) {
                continue;
            }
            SourceItem item = items.get(identity);
            if (item != null) {
                item.relations().stream()
                        .map(SourceItemRelation::targetIdentity)
                        .filter(items::containsKey)
                        .forEach(pending::addLast);
            }
        }
    }

    /** Returns whether the active index still represents every current input. */
    private boolean isCurrent(ImportDefinition definition, ImporterBinding binding, CachedImportIndex index)
            throws IOException {
        String definitionFingerprint = ImportHashes.definition(definition, binding.type());
        String sourceFingerprint = ImportHashes.file(definition.asset().path());
        Map<Path, String> dependencies = currentDependencies(index);
        String complete = ImportHashes.complete(definitionFingerprint, sourceFingerprint, project.root(), dependencies);
        return index.importId().equals(definition.id())
                && index.importerId().equals(binding.type().id())
                && index.importerVersion() == binding.type().version()
                && index.definitionFingerprint().equals(definitionFingerprint)
                && index.sourceFingerprint().equals(sourceFingerprint)
                && index.dependencies().equals(relativeDependencies(dependencies))
                && index.fingerprint().equals(complete);
    }

    /** Recomputes dependency hashes recorded by one published index. */
    private Map<Path, String> currentDependencies(CachedImportIndex index) throws IOException {
        Map<Path, String> current = new LinkedHashMap<>();
        for (String relativePath : index.dependencies().keySet()) {
            Path path = project.root().resolve(relativePath).normalize();
            if (!path.startsWith(project.root()) || !Files.isRegularFile(path)) {
                throw new IOException("Import dependency is unavailable: " + relativePath);
            }
            current.put(path.toRealPath(), ImportHashes.file(path));
        }
        return current;
    }

    /** Converts absolute dependency paths to deterministic project-relative keys. */
    private Map<String, String> relativeDependencies(Map<Path, String> dependencies) {
        Map<String, String> relative = new LinkedHashMap<>();
        dependencies.forEach((path, hash) ->
                relative.put(project.root().relativize(path).toString().replace('\\', '/'), hash));
        return relative;
    }

    /** Fingerprints the source, reporting unavailable input through the adapter context. */
    private static String fingerprintSource(GameProject.AssetSource asset, AbstractImportContext context) {
        try {
            return ImportHashes.file(asset.path());
        } catch (IOException exception) {
            context.error("import.source.read", "source asset cannot be read: " + asset.path(), "");
            return EMPTY_FINGERPRINT;
        }
    }

    /** Requires a project-declared source asset. */
    private GameProject.AssetSource requireAsset(String assetId) {
        String validId = Preconditions.requireNonBlank(assetId, "assetId");
        return project.assets().stream()
                .filter(asset -> asset.id().equals(validId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("project asset does not exist: " + validId));
    }

    /** Requires one registered executable importer. */
    private ImporterBinding requireBinding(String importerId) {
        String validId = Preconditions.requireRegisteredTypeId(importerId, "importerId");
        return bindings.find(validId)
                .orElseThrow(() -> new IllegalArgumentException("importer implementation is unavailable: " + validId));
    }

    /** Requires a definition belonging to this coordinator's project. */
    private ImportDefinition requireDefinition(ImportDefinition definition) {
        ImportDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        if (!validDefinition.source().startsWith(project.root())
                || project.assets().stream().noneMatch(asset -> asset.equals(validDefinition.asset()))) {
            throw new IllegalArgumentException("import definition does not belong to this project");
        }
        return validDefinition;
    }

    /** Returns whether the authoritative source is currently readable. */
    private static boolean sourceAvailable(ImportDefinition definition) {
        return Files.isRegularFile(definition.asset().path());
    }

    /** Creates a blocked status carrying one actionable diagnostic. */
    private static ImportStatus blocked(
            ImportDefinition definition, Optional<String> fingerprint, String code, String message) {
        return new ImportStatus(ImportState.BLOCKED, fingerprint, List.of(error(definition, code, message)));
    }

    /** Creates one import-definition diagnostic. */
    private static ProjectDiagnostic error(ImportDefinition definition, String code, String message) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                code,
                message,
                definition.source().toUri(),
                "");
    }

    /** Returns a stable useful failure message. */
    private static String failureMessage(Throwable failure) {
        String detail = failure.getMessage();
        return detail == null ? failure.getClass().getSimpleName() : detail;
    }
}
