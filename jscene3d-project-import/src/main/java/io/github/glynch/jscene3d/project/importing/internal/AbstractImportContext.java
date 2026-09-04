/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.importing.ImportDiagnosticCode;
import io.github.glynch.jscene3d.project.importing.ImportExecution;
import io.github.glynch.jscene3d.project.importing.ImportProgress;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.importing.extension.ImportInspectionContext;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Shared dependency, diagnostic, progress, and cancellation state for one adapter invocation. */
public abstract class AbstractImportContext implements ImportInspectionContext {
    private final GameProject project;
    private final GameProject.AssetSource asset;
    private final ImportExecution execution;
    private final Map<Path, String> dependencies = new LinkedHashMap<>();
    private final Map<String, SourceItem> items = new LinkedHashMap<>();
    private final List<ProjectDiagnostic> diagnostics = new ArrayList<>();

    /**
     * Stores one adapter invocation context.
     *
     * @param project containing project
     * @param asset authoritative source asset
     * @param execution caller-owned execution policy
     */
    protected AbstractImportContext(GameProject project, GameProject.AssetSource asset, ImportExecution execution) {
        this.project = Objects.requireNonNull(project, "project");
        this.asset = Objects.requireNonNull(asset, "asset");
        this.execution = Objects.requireNonNull(execution, "execution");
    }

    @Override
    public final GameProject project() {
        return project;
    }

    @Override
    public final GameProject.AssetSource asset() {
        return asset;
    }

    @Override
    public final void dependency(Path path) {
        checkCancelled();
        Path resolved = resolveDependency(path);
        if (resolved == null || dependencies.containsKey(resolved)) {
            return;
        }
        try {
            dependencies.put(resolved, ImportHashes.file(resolved));
        } catch (IOException exception) {
            error(
                    ImportDiagnosticCode.DEPENDENCY_READ_FAILED,
                    resolved.toString(),
                    Map.of("technicalDetail", "dependency cannot be read: " + resolved));
        }
    }

    @Override
    public final void sourceItem(SourceItem item) {
        SourceItem validItem = Objects.requireNonNull(item, "item");
        if (items.putIfAbsent(validItem.identity(), validItem) != null) {
            throw new IllegalArgumentException("source item identity is duplicated: " + validItem.identity());
        }
    }

    @Override
    public final void warning(DiagnosticCode code, String location, Map<String, String> details) {
        diagnostics.add(new ProjectDiagnostic(
                ProjectDiagnostic.Severity.WARNING,
                Objects.requireNonNull(code, "code"),
                asset.path().toUri(),
                Objects.requireNonNull(location, "location"),
                Map.copyOf(details)));
    }

    @Override
    public final void error(DiagnosticCode code, String location, Map<String, String> details) {
        diagnostics.add(new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                Objects.requireNonNull(code, "code"),
                asset.path().toUri(),
                Objects.requireNonNull(location, "location"),
                Map.copyOf(details)));
    }

    @Override
    public final void progress(ImportProgress progress) {
        execution.progressReporter().report(Objects.requireNonNull(progress, "progress"));
    }

    @Override
    public final void checkCancelled() {
        execution.cancellation().checkCancelled();
    }

    /**
     * Returns recorded dependency fingerprints in registration order.
     *
     * @return immutable dependency fingerprints
     */
    public final Map<Path, String> dependencies() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(dependencies));
    }

    /**
     * Returns discovered source items in registration order.
     *
     * @return immutable discovered items
     */
    public final List<SourceItem> items() {
        return List.copyOf(items.values());
    }

    /**
     * Returns ordered adapter and orchestration diagnostics.
     *
     * @return immutable diagnostics
     */
    public final List<ProjectDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    /** Adds one orchestration diagnostic whose source may differ from the imported asset. */
    final void diagnostic(ProjectDiagnostic diagnostic) {
        diagnostics.add(Objects.requireNonNull(diagnostic, "diagnostic"));
    }

    /** Resolves and confines one project dependency, reporting invalid input. */
    private @Nullable Path resolveDependency(Path path) {
        Path supplied = Objects.requireNonNull(path, "path");
        Path resolved = supplied.isAbsolute()
                ? supplied.normalize()
                : project.root().resolve(supplied).normalize();
        if (!resolved.startsWith(project.root()) || !Files.isRegularFile(resolved)) {
            error(
                    ImportDiagnosticCode.DEPENDENCY_INVALID,
                    supplied.toString(),
                    Map.of("technicalDetail", "dependency must be a project file: " + supplied));
            return null;
        }
        try {
            Path real = resolved.toRealPath();
            if (!real.startsWith(project.root())) {
                error(
                        ImportDiagnosticCode.DEPENDENCY_ESCAPES_PROJECT,
                        supplied.toString(),
                        Map.of("technicalDetail", "dependency resolves outside the project"));
                return null;
            }
            return real;
        } catch (IOException exception) {
            error(
                    ImportDiagnosticCode.DEPENDENCY_READ_FAILED,
                    supplied.toString(),
                    Map.of("technicalDetail", "dependency cannot be resolved: " + supplied));
            return null;
        }
    }
}
