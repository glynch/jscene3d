/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EditorDiagnosticContractTest {
    @Test
    void representsLspCompatibleTextRanges() {
        EditorTextPosition start = new EditorTextPosition(4, 2);
        EditorTextPosition end = new EditorTextPosition(4, 8);
        EditorTextRange range = new EditorTextRange(start, end);

        assertThat(start.compareTo(end)).isNegative();
        assertThat(end.compareTo(start)).isPositive();
        assertThat(start.compareTo(new EditorTextPosition(4, 2))).isZero();
        assertThat(new EditorTextPosition(3, 20).compareTo(start)).isNegative();
        assertThat(range.start()).isEqualTo(start);
        assertThat(range.end()).isEqualTo(end);
    }

    @Test
    void copiesDiagnosticDetails() {
        Map<String, String> mutableDetails = new LinkedHashMap<>();
        mutableDetails.put("source", "jdt.ls");
        EditorDiagnostic diagnostic = new EditorDiagnostic(
                EditorDiagnosticSeverity.ERROR,
                "java.compiler.undefinedMethod",
                "The method is undefined",
                "Example.java",
                Optional.of(new EditorTextRange(new EditorTextPosition(2, 4), new EditorTextPosition(2, 10))),
                mutableDetails);

        mutableDetails.clear();

        assertThat(diagnostic.details()).containsExactlyEntriesOf(Map.of("source", "jdt.ls"));
        assertThat(diagnostic.range()).isPresent();
        assertThat(EditorDiagnosticSeverity.values())
                .containsExactly(
                        EditorDiagnosticSeverity.ERROR,
                        EditorDiagnosticSeverity.WARNING,
                        EditorDiagnosticSeverity.INFORMATION,
                        EditorDiagnosticSeverity.HINT);
    }

    @Test
    void preservesPortableDiagnosticCollectionIdentity() {
        DiagnosticCollectionId id = new DiagnosticCollectionId("io.github.glynch.jscene3d.editor.java-diagnostics");

        assertThat(id).hasToString(id.value());
    }

    @Test
    void rejectsInvalidPositionsAndRanges() {
        assertThatThrownBy(() -> new EditorTextPosition(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("line and character must not be negative");
        assertThatThrownBy(() -> new EditorTextPosition(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("line and character must not be negative");
        EditorTextPosition start = new EditorTextPosition(2, 0);
        EditorTextPosition end = new EditorTextPosition(1, 5);
        assertThatThrownBy(() -> new EditorTextRange(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("start must not follow end");
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void validatesDiagnostics() {
        Optional<EditorTextRange> emptyRange = Optional.empty();
        Map<String, String> emptyDetails = Map.of();

        assertThatNullPointerException()
                .isThrownBy(() -> new EditorDiagnostic(null, "code", "message", "", emptyRange, emptyDetails))
                .withMessage("severity");
        assertThatThrownBy(() -> new EditorDiagnostic(
                        EditorDiagnosticSeverity.ERROR, " ", "message", "", emptyRange, emptyDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
        assertThatThrownBy(() ->
                        new EditorDiagnostic(EditorDiagnosticSeverity.ERROR, "code", " ", "", emptyRange, emptyDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorDiagnostic(
                        EditorDiagnosticSeverity.ERROR, "code", "message", null, emptyRange, emptyDetails))
                .withMessage("location");
        assertThatNullPointerException()
                .isThrownBy(() ->
                        new EditorDiagnostic(EditorDiagnosticSeverity.ERROR, "code", "message", "", null, emptyDetails))
                .withMessage("range");
        assertThatNullPointerException()
                .isThrownBy(() ->
                        new EditorDiagnostic(EditorDiagnosticSeverity.ERROR, "code", "message", "", emptyRange, null))
                .withMessage("details");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorTextRange(null, new EditorTextPosition(0, 0)))
                .withMessage("start");
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate null verifies public boundary validation.
    void rejectsMalformedDiagnosticCollectionIdentity() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DiagnosticCollectionId(null))
                .withMessage("value");
        assertThatThrownBy(() -> new DiagnosticCollectionId("local"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be a lowercase dotted namespaced identity");
    }
}
