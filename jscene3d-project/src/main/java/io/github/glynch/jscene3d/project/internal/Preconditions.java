/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Shared implementation-only precondition checks for project model values. */
public final class Preconditions {
    /** Prevents instantiation of this validation container. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Requires a non-blank string.
     *
     * @param value value to validate
     * @param name argument name used in failures
     * @return validated string
     */
    public static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }

    /**
     * Requires an optional string to be non-blank when present.
     *
     * @param value optional string to validate
     * @param name argument name used in failures
     * @return validated optional string
     */
    public static Optional<String> requireOptionalNonBlank(Optional<String> value, String name) {
        Optional<String> validValue = Objects.requireNonNull(value, name);
        validValue.ifPresent(text -> requireNonBlank(text, name));
        return validValue;
    }

    /**
     * Requires a positive integer.
     *
     * @param value value to validate
     * @param name argument name used in failures
     * @return validated value
     */
    public static int requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
        return value;
    }

    /**
     * Requires a lowercase dotted project or extension identifier.
     *
     * @param value identifier to validate
     * @param name argument name used in failures
     * @return validated identifier
     */
    public static String requireProjectId(String value, String name) {
        String identifier = Objects.requireNonNull(value, name);
        if (!ProjectIdentifiers.isProjectId(identifier)) {
            throw new IllegalArgumentException(name + " must be a lowercase reverse-domain identifier: " + value);
        }
        return identifier;
    }

    /**
     * Requires a portable lowercase local identifier.
     *
     * @param value identifier to validate
     * @param name argument name used in failures
     * @return validated identifier
     */
    public static String requireLocalId(String value, String name) {
        String identifier = Objects.requireNonNull(value, name);
        if (!ProjectIdentifiers.isLocalId(identifier)) {
            throw new IllegalArgumentException(name + " must be a portable lowercase identifier: " + value);
        }
        return identifier;
    }

    /**
     * Requires an extension-qualified registered type identifier.
     *
     * @param value identifier to validate
     * @param name argument name used in failures
     * @return validated identifier
     */
    public static String requireRegisteredTypeId(String value, String name) {
        String identifier = Objects.requireNonNull(value, name);
        if (!ProjectIdentifiers.isRegisteredTypeId(identifier)) {
            throw new IllegalArgumentException(
                    name + " must be an extension-qualified registered type identifier: " + value);
        }
        return identifier;
    }

    /**
     * Requires a forward-slash relative locator without traversal segments.
     *
     * @param value locator to validate
     * @param name argument name used in failures
     * @return validated locator
     */
    public static String requirePortableLocator(String value, String name) {
        String locator = requireNonBlank(value, name);
        if (!ProjectIdentifiers.isPortableLocator(locator)) {
            throw new IllegalArgumentException(name + " must be a portable relative locator: " + value);
        }
        return locator;
    }

    /**
     * Requires a semantic version.
     *
     * @param value version to validate
     * @param name argument name used in failures
     * @return validated version text
     */
    public static String requireSemanticVersion(String value, String name) {
        String version = requireNonBlank(value, name);
        if (SemanticVersion.parse(version).isEmpty()) {
            throw new IllegalArgumentException(name + " must be a semantic version: " + value);
        }
        return version;
    }

    /**
     * Requires a semantic-version requirement expression.
     *
     * @param value requirement to validate
     * @param name argument name used in failures
     * @return validated requirement text
     */
    public static String requireSemanticVersionRequirement(String value, String name) {
        String requirement = requireNonBlank(value, name);
        if (SemanticVersionRequirement.parse(requirement).isEmpty()) {
            throw new IllegalArgumentException(name + " must be a semantic-version requirement: " + value);
        }
        return requirement;
    }

    /**
     * Requires an absolute URI.
     *
     * @param value URI to validate
     * @param name argument name used in failures
     * @return validated URI
     */
    public static URI requireAbsoluteUri(URI value, String name) {
        URI uri = Objects.requireNonNull(value, name);
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException(name + " must be an absolute URI: " + value);
        }
        return uri;
    }

    /**
     * Requires an optional URI to be absolute when present.
     *
     * @param value optional URI to validate
     * @param name argument name used in failures
     * @return validated optional URI
     */
    public static Optional<URI> requireOptionalAbsoluteUri(Optional<URI> value, String name) {
        Optional<URI> validValue = Objects.requireNonNull(value, name);
        validValue.ifPresent(uri -> requireAbsoluteUri(uri, name));
        return validValue;
    }

    /**
     * Returns an immutable copy containing non-blank strings.
     *
     * @param values strings to validate and copy
     * @param name argument name used in failures
     * @return immutable validated strings
     */
    public static List<String> immutableNonBlankStrings(List<String> values, String name) {
        Objects.requireNonNull(values, name);
        List<String> copied = new ArrayList<>(values.size());
        for (String value : values) {
            copied.add(requireNonBlank(value, name + " entry"));
        }
        return List.copyOf(copied);
    }

    /**
     * Returns an immutable ordered copy of project values.
     *
     * @param values project values to validate and copy
     * @param name argument name used in failures
     * @return immutable validated values
     */
    public static Map<String, ProjectValue> immutableProjectValues(Map<String, ProjectValue> values, String name) {
        Objects.requireNonNull(values, name);
        Map<String, ProjectValue> copied = new LinkedHashMap<>();
        values.forEach((key, value) ->
                copied.put(Objects.requireNonNull(key, name + " key"), Objects.requireNonNull(value, name + " value")));
        return Collections.unmodifiableMap(copied);
    }

    /**
     * Returns an immutable ordered index with unique keys derived from its values.
     *
     * @param <T> indexed value type
     * @param values values to validate and index
     * @param key function deriving each value's key
     * @param name argument name used in failures
     * @return immutable ordered index
     */
    public static <T> Map<String, T> immutableUniqueIndex(List<T> values, Function<T, String> key, String name) {
        Objects.requireNonNull(values, name);
        Function<T, String> validKey = Objects.requireNonNull(key, "key");
        Map<String, T> copied = new LinkedHashMap<>();
        for (T value : values) {
            T validValue = Objects.requireNonNull(value, name + " entry");
            String valueKey = requireNonBlank(validKey.apply(validValue), name + " key");
            if (copied.putIfAbsent(valueKey, validValue) != null) {
                throw new IllegalArgumentException(name + " contains a duplicate id: " + valueKey);
            }
        }
        return Collections.unmodifiableMap(copied);
    }

    /**
     * Requires an optional lowercase SHA-256 digest.
     *
     * @param value optional digest to validate
     * @param name argument name used in failures
     * @return normalized optional digest
     */
    public static Optional<String> requireOptionalSha256(Optional<String> value, String name) {
        Optional<String> digest = Objects.requireNonNull(value, name);
        if (digest.isEmpty()) {
            return digest;
        }
        String text = digest.orElseThrow();
        if (!ProjectHashes.isSha256(text)) {
            throw new IllegalArgumentException(name + " must contain exactly 64 hexadecimal digits");
        }
        return Optional.of(text.toLowerCase(Locale.ROOT));
    }
}
