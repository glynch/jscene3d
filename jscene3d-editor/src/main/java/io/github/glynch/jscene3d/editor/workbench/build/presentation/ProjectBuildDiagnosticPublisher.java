/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.presentation;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildCompletion;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildDiagnostic;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Publishes only diagnostics which still describe the newest saved project revision. */
final class ProjectBuildDiagnosticPublisher {
    private final EditorDiagnosticCollection collection;

    ProjectBuildDiagnosticPublisher(EditorDiagnosticCollection collection) {
        this.collection = Objects.requireNonNull(collection, "collection");
    }

    void publish(Path projectRoot, ProjectBuildCompletion completion, long currentSavedRevision) {
        ProjectBuildCompletion event = Objects.requireNonNull(completion, "completion");
        if (event.request().revision() != currentSavedRevision) {
            collection.clear();
            return;
        }
        Optional<ProjectBuildResult> result = event.result();
        if (result.isPresent() && result.orElseThrow().outcome() == ProjectBuildOutcome.CANCELLED) {
            collection.clear();
            return;
        }

        Map<URI, List<EditorDiagnostic>> grouped = new LinkedHashMap<>();
        result.map(ProjectBuildResult::diagnostics).orElse(List.of()).forEach(item -> add(grouped, item));
        if (grouped.isEmpty()
                && result.isPresent()
                && result.orElseThrow().outcome() == ProjectBuildOutcome.SUCCEEDED) {
            collection.clear();
            return;
        }
        if (grouped.isEmpty()) {
            URI descriptor = Objects.requireNonNull(projectRoot, "projectRoot")
                    .resolve("pom.xml")
                    .toAbsolutePath()
                    .normalize()
                    .toUri();
            grouped.put(descriptor, List.of(fallback(event)));
        }
        collection.replaceAll(grouped);
    }

    void clear() {
        collection.clear();
    }

    private static void add(Map<URI, List<EditorDiagnostic>> grouped, ProjectBuildDiagnostic item) {
        grouped.computeIfAbsent(item.source(), ignored -> new ArrayList<>()).add(item.diagnostic());
    }

    private static EditorDiagnostic fallback(ProjectBuildCompletion completion) {
        Optional<ProjectBuildResult> result = completion.result();
        String message = result.map(ProjectBuildDiagnosticPublisher::failureMessage)
                .orElseGet(() -> throwableMessage(completion.failure().orElseThrow()));
        Map<String, String> details =
                result.map(ProjectBuildDiagnosticPublisher::commandDetail).orElse(Map.of());
        return new EditorDiagnostic(
                EditorDiagnosticSeverity.ERROR,
                "project.build.failed",
                "Project Build",
                message,
                "",
                Optional.empty(),
                details);
    }

    private static String failureMessage(ProjectBuildResult result) {
        return result.standardError()
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElse("Project build failed; open Build output for details");
    }

    private static String throwableMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getName() : message;
    }

    private static Map<String, String> commandDetail(ProjectBuildResult result) {
        return Map.of("Command", String.join(" ", result.command()));
    }
}
