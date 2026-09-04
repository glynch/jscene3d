/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import io.github.glynch.jscene3d.project.importing.ImportExecution;
import io.github.glynch.jscene3d.project.importing.ImportPhase;
import io.github.glynch.jscene3d.project.importing.ImportPreview;
import io.github.glynch.jscene3d.project.importing.ImportProgress;
import io.github.glynch.jscene3d.project.importing.ImportPublicationException;
import io.github.glynch.jscene3d.project.importing.PreparedImport;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Owned staging transaction implementing revalidation and atomic publication. */
public final class InternalPreparedImport implements PreparedImport {
    private final ImportDefinition definition;
    private final Path projectRoot;
    private final ImporterBinding binding;
    private final ImportExecution execution;
    private final CacheStore cache;
    private final TemporaryWorkspace workspace;
    private final ImportPreview preview;
    private final Optional<CachedImportIndex> index;
    private boolean committed;
    private boolean closed;

    /**
     * Creates one fully prepared transaction.
     *
     * @param context completed preparation context and its staging workspace
     * @param binding exact importer binding
     * @param execution caller-owned execution policy
     * @param cache physical cache policy
     * @param preview immutable preparation preview
     * @param index publishable index exactly when the preview is valid
     */
    public InternalPreparedImport(
            PreparationContext context,
            ImporterBinding binding,
            ImportExecution execution,
            CacheStore cache,
            ImportPreview preview,
            Optional<CachedImportIndex> index) {
        PreparationContext validContext = Objects.requireNonNull(context, "context");
        this.definition = validContext.definition();
        this.projectRoot = validContext.project().root();
        this.binding = Objects.requireNonNull(binding, "binding");
        this.execution = Objects.requireNonNull(execution, "execution");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.workspace = validContext.workspace();
        this.preview = Objects.requireNonNull(preview, "preview");
        this.index = Objects.requireNonNull(index, "index");
        if (preview.isValid() != index.isPresent()) {
            throw new IllegalArgumentException("a valid preview requires a publishable cache index");
        }
    }

    @Override
    public ImportPreview preview() {
        requireOpen();
        return preview;
    }

    @Override
    public void commit() {
        requireOpen();
        if (committed) {
            throw new IllegalStateException("Prepared import is already committed");
        }
        if (!preview.isValid()) {
            throw new IllegalStateException("Invalid prepared import cannot be committed");
        }
        execution.cancellation().checkCancelled();
        CachedImportIndex publishable = index.orElseThrow();
        requireCurrentInputs(publishable);
        execution
                .progressReporter()
                .report(ImportProgress.phase(ImportPhase.COMMITTING, "Publishing import " + definition.id()));
        cache.publish(definition.id(), publishable, workspace);
        committed = true;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        workspace.close();
    }

    /** Requires source and dependency bytes to match the prepared fingerprint. */
    private void requireCurrentInputs(CachedImportIndex prepared) {
        try {
            String definitionFingerprint = ImportHashes.definition(definition, binding.type());
            String sourceFingerprint = ImportHashes.file(definition.asset().path());
            Map<Path, String> dependencies = currentDependencies(prepared);
            String fingerprint =
                    ImportHashes.complete(definitionFingerprint, sourceFingerprint, projectRoot, dependencies);
            if (!prepared.definitionFingerprint().equals(definitionFingerprint)
                    || !prepared.sourceFingerprint().equals(sourceFingerprint)
                    || !prepared.fingerprint().equals(fingerprint)) {
                throw new ImportPublicationException("Import inputs changed after preparation: " + definition.id());
            }
        } catch (IOException exception) {
            throw new ImportPublicationException("Unable to revalidate import inputs: " + definition.id(), exception);
        }
    }

    /** Recomputes prepared dependency fingerprints from project-relative index paths. */
    private Map<Path, String> currentDependencies(CachedImportIndex prepared) throws IOException {
        Map<Path, String> current = new LinkedHashMap<>();
        for (Map.Entry<String, String> dependency : prepared.dependencies().entrySet()) {
            Path path = projectRoot.resolve(dependency.getKey()).normalize();
            current.put(path, ImportHashes.file(path));
        }
        return current;
    }

    /** Requires this transaction to remain open. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Prepared import is closed");
        }
    }
}
