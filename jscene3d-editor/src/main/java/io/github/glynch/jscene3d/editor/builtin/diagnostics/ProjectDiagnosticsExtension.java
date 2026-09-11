/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Built-in extension publishing project-loading diagnostics through the editor API. */
public final class ProjectDiagnosticsExtension implements EditorExtension {
    private static final String TECHNICAL_DETAIL = "technicalDetail";
    private static final DiagnosticCollectionId COLLECTION_ID =
            new DiagnosticCollectionId("io.github.glynch.jscene3d.editor.project-diagnostics");

    private @Nullable EditorDiagnosticCollection collection;

    /** Creates the built-in project-diagnostics publisher. */
    public ProjectDiagnosticsExtension() {
        super();
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.project-diagnostics";
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                "Project Diagnostics",
                "Publishes project-loading diagnostics to the editor workbench.",
                "JScene3D",
                Optional.empty(),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        collection = editor.subscriptions().add(editor.diagnostics().createCollection(COLLECTION_ID));
    }

    /**
     * Replaces every project diagnostic currently published to the workbench.
     *
     * @param diagnostics complete project diagnostic set
     */
    public void showDiagnostics(List<ProjectDiagnostic> diagnostics) {
        Map<URI, List<EditorDiagnostic>> grouped = new LinkedHashMap<>();
        Objects.requireNonNull(diagnostics, "diagnostics")
                .forEach(diagnostic -> grouped.computeIfAbsent(diagnostic.source(), ignored -> new ArrayList<>())
                        .add(convert(diagnostic)));
        requireCollection().replaceAll(grouped);
    }

    private EditorDiagnosticCollection requireCollection() {
        EditorDiagnosticCollection current = collection;
        if (current == null) {
            throw new IllegalStateException("project diagnostics extension has not been activated");
        }
        return current;
    }

    private static EditorDiagnostic convert(ProjectDiagnostic diagnostic) {
        ProjectDiagnostic source = Objects.requireNonNull(diagnostic, "diagnostic");
        Map<String, String> details = new LinkedHashMap<>(source.details());
        String message = details.getOrDefault(TECHNICAL_DETAIL, source.message());
        details.remove(TECHNICAL_DETAIL);
        EditorDiagnosticSeverity severity =
                switch (source.severity()) {
                    case ERROR -> EditorDiagnosticSeverity.ERROR;
                    case WARNING -> EditorDiagnosticSeverity.WARNING;
                };
        return new EditorDiagnostic(
                severity, source.code().code(), message, source.location(), Optional.empty(), details);
    }
}
