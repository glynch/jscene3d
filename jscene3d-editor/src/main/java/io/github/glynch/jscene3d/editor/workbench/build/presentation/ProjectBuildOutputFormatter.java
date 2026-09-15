/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.presentation;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildCompletion;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import java.util.Locale;
import java.util.Objects;

/** Formats one terminal project build for the generic Build output channel. */
final class ProjectBuildOutputFormatter {
    private ProjectBuildOutputFormatter() {}

    static String format(ProjectBuildCompletion completion) {
        ProjectBuildCompletion event = Objects.requireNonNull(completion, "completion");
        return event.result().map(result -> format(event, result)).orElseGet(() -> formatFailure(event));
    }

    private static String format(ProjectBuildCompletion completion, ProjectBuildResult result) {
        StringBuilder output = new StringBuilder();
        output.append("Build revision ")
                .append(completion.request().revision())
                .append(" (")
                .append(completion.request().kind().name().toLowerCase(Locale.ROOT))
                .append(")\n");
        output.append("Command: ").append(String.join(" ", result.command())).append('\n');
        output.append("Outcome: ")
                .append(result.outcome().name().toLowerCase(Locale.ROOT))
                .append('\n');
        output.append("Duration: ").append(result.duration().toMillis()).append(" ms\n");
        appendSection(output, "Standard output", result.standardOutput());
        appendSection(output, "Standard error", result.standardError());
        return output.toString();
    }

    private static String formatFailure(ProjectBuildCompletion completion) {
        Throwable failure = completion.failure().orElseThrow();
        String detail = failure.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = failure.getClass().getName();
        }
        return "Build revision " + completion.request().revision() + " failed before execution\n\n" + detail + '\n';
    }

    private static void appendSection(StringBuilder output, String title, String content) {
        if (content.isEmpty()) {
            return;
        }
        output.append('\n').append(title).append(":\n").append(content);
        if (!content.endsWith("\n")) {
            output.append('\n');
        }
    }
}
