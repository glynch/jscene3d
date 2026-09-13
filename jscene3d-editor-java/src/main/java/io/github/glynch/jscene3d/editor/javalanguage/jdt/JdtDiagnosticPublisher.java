/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

/** Translates version-aware JDT diagnostics into the Java extension's editor collection. */
final class JdtDiagnosticPublisher {
    private static final System.Logger LOGGER = System.getLogger(JdtDiagnosticPublisher.class.getName());

    private final EditorDiagnosticCollection collection;
    private final Map<URI, Integer> openVersions = new LinkedHashMap<>();
    private final Set<URI> closedDocuments = new LinkedHashSet<>();

    JdtDiagnosticPublisher(EditorDiagnosticCollection collection) {
        this.collection = Objects.requireNonNull(collection, "collection");
    }

    synchronized void documentVersion(URI resource, int version) {
        URI source = Objects.requireNonNull(resource, "resource");
        closedDocuments.remove(source);
        openVersions.put(source, version);
    }

    synchronized void documentClosed(URI resource) {
        URI source = Objects.requireNonNull(resource, "resource");
        openVersions.remove(source);
        closedDocuments.add(source);
        collection.clear(source);
    }

    synchronized void publish(PublishDiagnosticsParams publication) {
        if (publication == null || publication.getUri() == null) {
            LOGGER.log(System.Logger.Level.WARNING, "JDT LS sent diagnostics without a source URI");
            return;
        }
        URI source;
        try {
            source = URI.create(publication.getUri());
        } catch (IllegalArgumentException failure) {
            LOGGER.log(System.Logger.Level.WARNING, "JDT LS sent an invalid diagnostic URI", failure);
            return;
        }
        Integer publishedVersion = publication.getVersion();
        if (closedDocuments.contains(source) && publishedVersion != null) {
            return;
        }
        Integer currentVersion = openVersions.get(source);
        if (publishedVersion != null && currentVersion != null && !publishedVersion.equals(currentVersion)) {
            return;
        }
        List<Diagnostic> published = Objects.requireNonNullElse(publication.getDiagnostics(), List.of());
        collection.replace(
                source,
                published.stream().map(JdtDiagnosticPublisher::translate).toList());
    }

    synchronized void clear() {
        openVersions.clear();
        closedDocuments.clear();
        collection.clear();
    }

    private static EditorDiagnostic translate(Diagnostic diagnostic) {
        Diagnostic source = Objects.requireNonNull(diagnostic, "diagnostic");
        Optional<EditorTextRange> range = Optional.ofNullable(source.getRange())
                .map(value -> new EditorTextRange(
                        new EditorTextPosition(
                                value.getStart().getLine(), value.getStart().getCharacter()),
                        new EditorTextPosition(
                                value.getEnd().getLine(), value.getEnd().getCharacter())));
        Map<String, String> details = new LinkedHashMap<>();
        if (source.getCodeDescription() != null && source.getCodeDescription().getHref() != null) {
            details.put("More information", source.getCodeDescription().getHref());
        }
        return new EditorDiagnostic(
                severity(source.getSeverity()),
                code(source),
                diagnosticSource(source),
                message(source),
                location(range),
                range,
                details);
    }

    private static EditorDiagnosticSeverity severity(DiagnosticSeverity severity) {
        if (severity == null) {
            return EditorDiagnosticSeverity.ERROR;
        }
        return switch (severity) {
            case Error -> EditorDiagnosticSeverity.ERROR;
            case Warning -> EditorDiagnosticSeverity.WARNING;
            case Information -> EditorDiagnosticSeverity.INFORMATION;
            case Hint -> EditorDiagnosticSeverity.HINT;
        };
    }

    private static String code(Diagnostic diagnostic) {
        if (diagnostic.getCode() == null) {
            return "java";
        }
        String value = diagnostic.getCode().isLeft()
                ? diagnostic.getCode().getLeft()
                : Integer.toString(diagnostic.getCode().getRight());
        return value == null || value.isBlank() ? "java" : value;
    }

    private static String message(Diagnostic diagnostic) {
        var published = diagnostic.getMessage();
        String message = null;
        if (published != null) {
            message = published.isLeft()
                    ? published.getLeft()
                    : published.getRight().getValue();
        }
        return message == null || message.isBlank() ? "Java language error" : message;
    }

    private static String diagnosticSource(Diagnostic diagnostic) {
        String source = diagnostic.getSource();
        return source == null ? "" : source.strip();
    }

    private static String location(Optional<EditorTextRange> range) {
        return range.map(value ->
                        (value.start().line() + 1) + ":" + (value.start().character() + 1))
                .orElse("");
    }
}
