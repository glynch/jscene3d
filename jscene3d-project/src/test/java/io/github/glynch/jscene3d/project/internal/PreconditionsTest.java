/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies shared project-model preconditions and immutable-copy policies. */
final class PreconditionsTest {
    /** Validates textual identifiers through their shared syntax policies. */
    @Test
    void validatesTextAndIdentifiers() {
        assertThat(Preconditions.requireNonBlank("value", "value")).isEqualTo("value");
        assertThat(Preconditions.requireProjectId("example.game", "id")).isEqualTo("example.game");
        assertThat(Preconditions.requireLocalId("player-start", "id")).isEqualTo("player-start");
        assertThat(Preconditions.requireRegisteredTypeId("example.game/player-3d", "type"))
                .isEqualTo("example.game/player-3d");
        assertThatThrownBy(() -> Preconditions.requireNonBlank(" ", "value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireProjectId("Example", "id"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireLocalId("player start", "id"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireRegisteredTypeId("player", "type"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Validates portable locators and semantic-version values. */
    @Test
    void validatesStructuredText() {
        assertThat(Preconditions.requirePortableLocator("textures/wall.png", "locator"))
                .isEqualTo("textures/wall.png");
        assertThat(Preconditions.requireSemanticVersion("1.2.3", "version")).isEqualTo("1.2.3");
        assertThat(Preconditions.requireSemanticVersionRequirement(">=1.0.0 <2.0.0", "requirement"))
                .isEqualTo(">=1.0.0 <2.0.0");
        assertThatThrownBy(() -> Preconditions.requirePortableLocator("../wall.png", "locator"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireSemanticVersion("1", "version"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireSemanticVersionRequirement("latest", "requirement"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Validates optional URI and digest values. */
    @Test
    void validatesOptionalScalars() {
        String uppercaseDigest = "A".repeat(64);
        Optional<String> normalized = Preconditions.requireOptionalSha256(Optional.of(uppercaseDigest), "sha256");
        Optional<String> blank = Optional.of(" ");
        URI relative = URI.create("relative");
        Optional<String> invalidDigest = Optional.of("invalid");

        assertThat(Preconditions.requireOptionalNonBlank(Optional.of("value"), "value"))
                .contains("value");
        assertThat(Preconditions.requireAbsoluteUri(URI.create("https://example.com"), "uri"))
                .isEqualTo(URI.create("https://example.com"));
        assertThat(normalized).contains("a".repeat(64));
        assertThatThrownBy(() -> Preconditions.requireOptionalNonBlank(blank, "value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireAbsoluteUri(relative, "uri"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireOptionalSha256(invalidDigest, "sha256"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Creates immutable validated copies of lists and project-value maps. */
    @Test
    void copiesValidatedCollections() {
        List<String> mutableStrings = new ArrayList<>(List.of("one"));
        Map<String, ProjectValue> mutableValues = new LinkedHashMap<>();
        mutableValues.put("enabled", new ProjectValue.BooleanValue(true));

        List<String> strings = Preconditions.immutableNonBlankStrings(mutableStrings, "strings");
        Map<String, ProjectValue> values = Preconditions.immutableProjectValues(mutableValues, "values");
        mutableStrings.clear();
        mutableValues.clear();

        assertThat(strings).containsExactly("one");
        assertThat(values).containsKey("enabled");
        assertThatThrownBy(strings::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(values::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Creates an immutable ordered index and rejects duplicate derived keys. */
    @Test
    void indexesUniqueValues() {
        List<NamedValue> duplicateValues = List.of(new NamedValue("one"), new NamedValue("one"));
        Map<String, NamedValue> values = Preconditions.immutableUniqueIndex(
                List.of(new NamedValue("one"), new NamedValue("two")), NamedValue::name, "values");

        assertThat(values)
                .containsExactly(Map.entry("one", new NamedValue("one")), Map.entry("two", new NamedValue("two")));
        assertThatThrownBy(() -> Preconditions.immutableUniqueIndex(duplicateValues, NamedValue::name, "values"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Escapes JSON Pointer segments through the shared representation policy. */
    @Test
    void escapesJsonPointerSegments() {
        assertThat(JsonPointers.escapeSegment("material~/name")).isEqualTo("material~0~1name");
    }

    /** Named value used to exercise generic indexing. */
    private record NamedValue(String name) {}
}
