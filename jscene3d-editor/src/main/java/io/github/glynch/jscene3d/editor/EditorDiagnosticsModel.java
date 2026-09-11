/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Projects structured project diagnostics into a deterministic filterable browser. */
final class EditorDiagnosticsModel {
    private static final String TECHNICAL_DETAIL = "technicalDetail";

    private List<ProjectDiagnostic> diagnostics = List.of();
    private String query = "";
    private final EnumSet<ProjectDiagnostic.Severity> visibleSeverities =
            EnumSet.allOf(ProjectDiagnostic.Severity.class);

    /** Replaces the complete ordered diagnostic set. */
    void showDiagnostics(List<ProjectDiagnostic> projectDiagnostics) {
        diagnostics = List.copyOf(Objects.requireNonNull(projectDiagnostics, "projectDiagnostics"));
    }

    /** Applies a case-insensitive filter across all author-facing diagnostic information. */
    void filter(String text) {
        query = Objects.requireNonNull(text, "text").strip().toLowerCase(Locale.ROOT);
    }

    /** Includes or excludes one severity without discarding the underlying diagnostics. */
    void showSeverity(ProjectDiagnostic.Severity severity, boolean visible) {
        ProjectDiagnostic.Severity validSeverity = Objects.requireNonNull(severity, "severity");
        if (visible) {
            visibleSeverities.add(validSeverity);
        } else {
            visibleSeverities.remove(validSeverity);
        }
    }

    /** Returns one immutable snapshot for the Diagnostics drawer. */
    View view() {
        long errors = count(ProjectDiagnostic.Severity.ERROR);
        long warnings = count(ProjectDiagnostic.Severity.WARNING);
        Map<URI, List<Item>> grouped = new LinkedHashMap<>();
        diagnostics.stream()
                .filter(diagnostic -> visibleSeverities.contains(diagnostic.severity()))
                .filter(this::matchesQuery)
                .map(EditorDiagnosticsModel::project)
                .forEach(item -> grouped.computeIfAbsent(item.diagnostic().source(), ignored -> new ArrayList<>())
                        .add(item));
        List<Group> groups = grouped.entrySet().stream()
                .map(entry -> group(entry.getKey(), entry.getValue()))
                .toList();
        long visible = groups.stream().mapToLong(group -> group.items().size()).sum();
        return new View(errors, warnings, visible, groups);
    }

    /** Counts all diagnostics of one severity before presentation filtering. */
    private long count(ProjectDiagnostic.Severity severity) {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == severity)
                .count();
    }

    /** Tests one diagnostic against the normalized text filter. */
    private boolean matchesQuery(ProjectDiagnostic diagnostic) {
        if (query.isEmpty()) {
            return true;
        }
        String searchable = String.join(
                        " ",
                        diagnostic.code().code(),
                        diagnostic.message(),
                        diagnostic.source().toString(),
                        diagnostic.location(),
                        String.join(" ", diagnostic.details().values()))
                .toLowerCase(Locale.ROOT);
        return searchable.contains(query);
    }

    /** Projects one diagnostic without losing the source value used for navigation. */
    private static Item project(ProjectDiagnostic diagnostic) {
        String summary = diagnostic.details().getOrDefault(TECHNICAL_DETAIL, diagnostic.message());
        List<Detail> details = diagnostic.details().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(TECHNICAL_DETAIL))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Detail(entry.getKey(), entry.getValue()))
                .toList();
        return new Item(diagnostic, summary, details);
    }

    /** Creates one source group while retaining diagnostic arrival order. */
    private static Group group(URI source, List<Item> items) {
        long errors = items.stream()
                .filter(item -> item.diagnostic().severity() == ProjectDiagnostic.Severity.ERROR)
                .count();
        long warnings = items.size() - errors;
        return new Group(source, sourceLabel(source), errors, warnings, List.copyOf(items));
    }

    /** Returns a compact source label while retaining the complete URI in the group. */
    private static String sourceLabel(URI source) {
        if ("file".equalsIgnoreCase(source.getScheme())) {
            try {
                Path fileName = Path.of(source).getFileName();
                if (fileName != null) {
                    return fileName.toString();
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to URI-based labelling for unusual file URIs.
            }
        }
        String candidate = Objects.requireNonNullElse(source.getPath(), source.getSchemeSpecificPart());
        int separator = candidate.lastIndexOf('/');
        String label = separator >= 0 ? candidate.substring(separator + 1) : candidate;
        return label.isBlank() ? source.toString() : label;
    }

    /** Complete and filtered count plus visible source groups. */
    record View(long errors, long warnings, long visible, List<Group> groups) {
        /** Copies the visible group list. */
        View {
            groups = List.copyOf(groups);
        }

        /** Returns the complete unfiltered diagnostic count. */
        long total() {
            return errors + warnings;
        }
    }

    /** Diagnostics sharing one authoritative source URI. */
    record Group(URI source, String label, long errors, long warnings, List<Item> items) {
        /** Copies one group's ordered diagnostic items. */
        Group {
            items = List.copyOf(items);
        }
    }

    /** One concise diagnostic row with expandable structured details. */
    record Item(ProjectDiagnostic diagnostic, String summary, List<Detail> details) {
        /** Copies one diagnostic's detail rows. */
        Item {
            details = List.copyOf(details);
        }

        /** Returns one self-contained line suitable for copying outside the editor. */
        String copyText() {
            String location = diagnostic.location().isEmpty() ? "" : " " + diagnostic.location();
            return summary + " [" + diagnostic.code().code() + "] " + diagnostic.source() + location;
        }
    }

    /** One labelled value shown below an expanded diagnostic row. */
    record Detail(String label, String value) {}
}
