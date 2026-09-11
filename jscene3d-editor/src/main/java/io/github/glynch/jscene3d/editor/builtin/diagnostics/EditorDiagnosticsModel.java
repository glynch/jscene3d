/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Projects extension-published diagnostics into a deterministic filterable browser. */
public final class EditorDiagnosticsModel {
    private final List<Runnable> observers;
    private final EnumSet<EditorDiagnosticSeverity> visibleSeverities;
    private List<EditorDiagnosticSnapshot> diagnostics = List.of();
    private String query = "";

    /** Creates an empty model with every severity visible. */
    public EditorDiagnosticsModel() {
        observers = new ArrayList<>();
        visibleSeverities = EnumSet.allOf(EditorDiagnosticSeverity.class);
    }

    /**
     * Replaces the complete ordered diagnostic set.
     *
     * @param replacement complete ordered diagnostic set
     */
    public void showDiagnostics(List<EditorDiagnosticSnapshot> replacement) {
        diagnostics = List.copyOf(Objects.requireNonNull(replacement, "replacement"));
        notifyObservers();
    }

    /**
     * Applies a case-insensitive filter across all author-facing diagnostic information.
     *
     * @param text filter text
     */
    public void filter(String text) {
        query = Objects.requireNonNull(text, "text").strip().toLowerCase(Locale.ROOT);
        notifyObservers();
    }

    /**
     * Includes or excludes one severity without discarding the underlying diagnostics.
     *
     * @param severity severity whose visibility changes
     * @param visible whether the severity is visible
     */
    public void showSeverity(EditorDiagnosticSeverity severity, boolean visible) {
        EditorDiagnosticSeverity validSeverity = Objects.requireNonNull(severity, "severity");
        if (visible) {
            visibleSeverities.add(validSeverity);
        } else {
            visibleSeverities.remove(validSeverity);
        }
        notifyObservers();
    }

    /**
     * Observes model changes until the returned registration is closed.
     *
     * @param observer synchronous model-change observer
     * @return removable observer registration
     */
    public EditorRegistration observe(Runnable observer) {
        Runnable listener = Objects.requireNonNull(observer, "observer");
        observers.add(listener);
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                observers.remove(listener);
            }
        };
    }

    /**
     * Returns one immutable snapshot for the Diagnostics view.
     *
     * @return immutable view snapshot
     */
    public View view() {
        Map<URI, List<Item>> grouped = new LinkedHashMap<>();
        diagnostics.stream()
                .filter(snapshot ->
                        visibleSeverities.contains(snapshot.diagnostic().severity()))
                .filter(this::matchesQuery)
                .map(EditorDiagnosticsModel::project)
                .forEach(item -> grouped.computeIfAbsent(item.source(), ignored -> new ArrayList<>())
                        .add(item));
        List<Group> groups = grouped.entrySet().stream()
                .map(entry -> group(entry.getKey(), entry.getValue()))
                .toList();
        long visible = groups.stream().mapToLong(group -> group.items().size()).sum();
        return new View(
                count(EditorDiagnosticSeverity.ERROR),
                count(EditorDiagnosticSeverity.WARNING),
                count(EditorDiagnosticSeverity.INFORMATION),
                count(EditorDiagnosticSeverity.HINT),
                visible,
                groups);
    }

    private void notifyObservers() {
        List.copyOf(observers).forEach(Runnable::run);
    }

    private long count(EditorDiagnosticSeverity severity) {
        return diagnostics.stream()
                .filter(snapshot -> snapshot.diagnostic().severity() == severity)
                .count();
    }

    private boolean matchesQuery(EditorDiagnosticSnapshot snapshot) {
        if (query.isEmpty()) {
            return true;
        }
        EditorDiagnostic diagnostic = snapshot.diagnostic();
        String searchable = String.join(
                        " ",
                        diagnostic.code(),
                        diagnostic.message(),
                        snapshot.source().toString(),
                        diagnostic.location(),
                        String.join(" ", diagnostic.details().values()))
                .toLowerCase(Locale.ROOT);
        return searchable.contains(query);
    }

    private static Item project(EditorDiagnosticSnapshot snapshot) {
        List<Detail> details = snapshot.diagnostic().details().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Detail(entry.getKey(), entry.getValue()))
                .toList();
        return new Item(snapshot, details);
    }

    private static Group group(URI source, List<Item> items) {
        return new Group(source, sourceLabel(source), List.copyOf(items));
    }

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

    /**
     * Complete and filtered counts plus visible source groups.
     *
     * @param errors complete error count
     * @param warnings complete warning count
     * @param information complete information count
     * @param hints complete hint count
     * @param visible visible diagnostic count after filtering
     * @param groups visible diagnostics grouped by source
     */
    public record View(long errors, long warnings, long information, long hints, long visible, List<Group> groups) {
        /** Copies the visible group list. */
        public View {
            groups = List.copyOf(groups);
        }

        /**
         * Returns the complete unfiltered diagnostic count.
         *
         * @return complete unfiltered diagnostic count
         */
        public long total() {
            return errors + warnings + information + hints;
        }
    }

    /**
     * Diagnostics sharing one authoritative source URI.
     *
     * @param source authoritative source URI
     * @param label concise source label
     * @param items diagnostics belonging to the source
     */
    public record Group(URI source, String label, List<Item> items) {
        /** Copies one group's ordered diagnostic items. */
        public Group {
            items = List.copyOf(items);
        }

        /**
         * Counts items of one severity in this visible group.
         *
         * @param severity severity to count
         * @return number of matching visible items
         */
        public long count(EditorDiagnosticSeverity severity) {
            return items.stream()
                    .filter(item -> item.diagnostic().severity() == severity)
                    .count();
        }
    }

    /**
     * One concise diagnostic row with expandable structured details.
     *
     * @param snapshot published diagnostic snapshot
     * @param details ordered structured detail rows
     */
    public record Item(EditorDiagnosticSnapshot snapshot, List<Detail> details) {
        /** Copies one diagnostic's detail rows. */
        public Item {
            details = List.copyOf(details);
        }

        /**
         * Returns the diagnostic source.
         *
         * @return authoritative diagnostic source URI
         */
        public URI source() {
            return snapshot.source();
        }

        /**
         * Returns the diagnostic occurrence.
         *
         * @return diagnostic occurrence
         */
        public EditorDiagnostic diagnostic() {
            return snapshot.diagnostic();
        }

        /**
         * Returns one self-contained line suitable for copying outside the editor.
         *
         * @return self-contained diagnostic line
         */
        public String copyText() {
            String location =
                    diagnostic().location().isEmpty() ? "" : " " + diagnostic().location();
            return diagnostic().message() + " [" + diagnostic().code() + "] " + source() + location;
        }
    }

    /**
     * One labelled value shown below an expanded diagnostic row.
     *
     * @param label detail label
     * @param value detail value
     */
    public record Detail(String label, String value) {}
}
