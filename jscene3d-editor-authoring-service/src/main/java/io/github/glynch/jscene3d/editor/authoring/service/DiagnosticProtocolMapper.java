/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectDiagnosticDto;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Maps ordered Java diagnostics to their locale-resolved wire representation. */
final class DiagnosticProtocolMapper {
    private final ProjectDiagnosticMessageResolver messages;

    DiagnosticProtocolMapper(ProjectDiagnosticMessageResolver messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    ProjectDiagnosticDto map(ProjectDiagnostic diagnostic, Locale locale) {
        return new ProjectDiagnosticDto(
                diagnostic.severity().name().toLowerCase(Locale.ROOT),
                diagnostic.code().code(),
                messages.resolve(diagnostic, locale),
                diagnostic.source().toString(),
                diagnostic.location(),
                diagnostic.details());
    }

    List<ProjectDiagnosticDto> map(List<ProjectDiagnostic> diagnostics, Locale locale) {
        Objects.requireNonNull(locale, "locale");
        return diagnostics.stream().map(diagnostic -> map(diagnostic, locale)).toList();
    }
}
