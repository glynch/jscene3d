/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Shared implementation-only precondition checks for project importing. */
public final class Preconditions {
    /** Prevents instantiation of this validation container. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Requires a non-blank string.
     *
     * @param value value to validate
     * @param name parameter name for failures
     * @return validated value
     */
    public static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }

    /**
     * Requires an optional non-blank string.
     *
     * @param value optional value to validate
     * @param name parameter name for failures
     * @return validated optional
     */
    public static Optional<String> requireOptionalNonBlank(Optional<String> value, String name) {
        Optional<String> validValue = Objects.requireNonNull(value, name);
        validValue.ifPresent(text -> requireNonBlank(text, name));
        return validValue;
    }

    /**
     * Requires a portable relative identity.
     *
     * @param value value to validate
     * @param name parameter name for failures
     * @return validated identity
     */
    public static String requirePortableIdentity(String value, String name) {
        String identity = requireNonBlank(value, name);
        if (identity.indexOf('\\') >= 0 || identity.startsWith("/") || identity.endsWith("/")) {
            throw new IllegalArgumentException(name + " must be a portable relative identity: " + value);
        }
        for (String segment : identity.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException(name + " must be a portable relative identity: " + value);
            }
        }
        return identity;
    }

    /**
     * Requires an optional portable relative identity.
     *
     * @param value optional value to validate
     * @param name parameter name for failures
     * @return validated optional identity
     */
    public static Optional<String> requireOptionalPortableIdentity(Optional<String> value, String name) {
        Optional<String> validValue = Objects.requireNonNull(value, name);
        validValue.ifPresent(identity -> requirePortableIdentity(identity, name));
        return validValue;
    }

    /**
     * Requires an extension-qualified registered type identifier.
     *
     * @param value value to validate
     * @param name parameter name for failures
     * @return validated type identifier
     */
    public static String requireRegisteredTypeId(String value, String name) {
        String type = requireNonBlank(value, name);
        int slash = type.indexOf('/');
        if (slash < 3 || slash != type.lastIndexOf('/') || slash == type.length() - 1) {
            throw new IllegalArgumentException(name + " must be an extension-qualified type: " + value);
        }
        return type;
    }

    /**
     * Requires a lowercase SHA-256 fingerprint.
     *
     * @param value value to validate
     * @param name parameter name for failures
     * @return validated fingerprint
     */
    public static String requireSha256(String value, String name) {
        String fingerprint = requireNonBlank(value, name);
        if (fingerprint.length() != 64) {
            throw new IllegalArgumentException(name + " must contain 64 hexadecimal digits");
        }
        for (int index = 0; index < fingerprint.length(); index++) {
            char character = fingerprint.charAt(index);
            if (!(character >= '0' && character <= '9') && !(character >= 'a' && character <= 'f')) {
                throw new IllegalArgumentException(name + " must contain lowercase hexadecimal digits");
            }
        }
        return fingerprint;
    }

    /**
     * Requires an optional lowercase SHA-256 fingerprint.
     *
     * @param value optional value to validate
     * @param name parameter name for failures
     * @return validated optional fingerprint
     */
    public static Optional<String> requireOptionalSha256(Optional<String> value, String name) {
        Optional<String> validValue = Objects.requireNonNull(value, name);
        validValue.ifPresent(fingerprint -> requireSha256(fingerprint, name));
        return validValue;
    }

    /**
     * Copies unique portable identities in declaration order.
     *
     * @param values identities to copy
     * @param name parameter name for failures
     * @return immutable validated identities
     */
    public static List<String> copyPortableIdentities(List<String> values, String name) {
        Objects.requireNonNull(values, name);
        List<String> copied = new ArrayList<>(values.size());
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String identity = requirePortableIdentity(value, name + " entry");
            if (!unique.add(identity)) {
                throw new IllegalArgumentException(name + " contains a duplicate identity: " + identity);
            }
            copied.add(identity);
        }
        return List.copyOf(copied);
    }

    /**
     * Copies project values while preserving declaration order.
     *
     * @param values values to copy
     * @param name parameter name for failures
     * @return immutable validated values
     */
    public static Map<String, ProjectValue> copyProjectValues(Map<String, ProjectValue> values, String name) {
        Objects.requireNonNull(values, name);
        Map<String, ProjectValue> copied = new LinkedHashMap<>();
        values.forEach((key, value) ->
                copied.put(requireNonBlank(key, name + " key"), Objects.requireNonNull(value, name + " value")));
        return Collections.unmodifiableMap(copied);
    }

    /**
     * Copies normalized absolute paths and lowercase fingerprints.
     *
     * @param values fingerprints to copy
     * @param name parameter name for failures
     * @return immutable validated fingerprints
     */
    public static Map<Path, String> copyFingerprints(Map<Path, String> values, String name) {
        Objects.requireNonNull(values, name);
        Map<Path, String> copied = new LinkedHashMap<>();
        values.forEach((path, fingerprint) -> {
            Path validPath = Objects.requireNonNull(path, name + " path");
            if (!validPath.isAbsolute() || !validPath.equals(validPath.normalize())) {
                throw new IllegalArgumentException(name + " paths must be normalized and absolute: " + validPath);
            }
            copied.put(validPath, requireSha256(fingerprint.toLowerCase(Locale.ROOT), name + " fingerprint"));
        });
        return Collections.unmodifiableMap(copied);
    }

    /**
     * Copies source items while requiring unique identities.
     *
     * @param items source items to copy
     * @return immutable validated items
     */
    public static List<SourceItem> copyUniqueSourceItems(List<SourceItem> items) {
        Objects.requireNonNull(items, "items");
        LinkedHashSet<String> identities = new LinkedHashSet<>();
        for (SourceItem item : items) {
            SourceItem validItem = Objects.requireNonNull(item, "items entry");
            if (!identities.add(validItem.identity())) {
                throw new IllegalArgumentException("source item identity is duplicated: " + validItem.identity());
            }
        }
        return List.copyOf(items);
    }

    /**
     * Copies artifact metadata while requiring unique identities.
     *
     * @param artifacts artifact metadata to copy
     * @return immutable validated artifacts
     */
    public static List<ImportedArtifactMetadata> copyUniqueArtifactMetadata(List<ImportedArtifactMetadata> artifacts) {
        Objects.requireNonNull(artifacts, "artifacts");
        LinkedHashSet<String> identities = new LinkedHashSet<>();
        for (ImportedArtifactMetadata artifact : artifacts) {
            ImportedArtifactMetadata validArtifact = Objects.requireNonNull(artifact, "artifacts entry");
            if (!identities.add(validArtifact.identity())) {
                throw new IllegalArgumentException("artifact identity is duplicated: " + validArtifact.identity());
            }
        }
        return List.copyOf(artifacts);
    }
}
