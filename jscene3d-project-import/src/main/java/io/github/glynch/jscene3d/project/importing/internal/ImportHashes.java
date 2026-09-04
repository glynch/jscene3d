/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic SHA-256 policy for source files, configuration, and complete imports. */
public final class ImportHashes {
    /** Prevents instantiation of this hashing policy. */
    private ImportHashes() {
        throw new AssertionError("ImportHashes cannot be instantiated");
    }

    /**
     * Returns the lowercase SHA-256 fingerprint of one regular file.
     *
     * @param path file to hash
     * @return lowercase SHA-256 fingerprint
     * @throws IOException when the file cannot be read
     */
    public static String file(Path path) throws IOException {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[8192];
        try (InputStream input = Files.newInputStream(path)) {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Returns a deterministic fingerprint for authored configuration and importer version.
     *
     * @param definition authored import definition
     * @param importer exact importer type
     * @return deterministic definition fingerprint
     */
    public static String definition(ImportDefinition definition, RegisteredType importer) {
        MessageDigest digest = sha256();
        update(digest, definition.id());
        update(digest, definition.asset().id());
        update(digest, definition.asset().type());
        update(digest, importer.id());
        update(digest, Integer.toString(importer.version()));
        updateStrings(digest, definition.selection());
        updateValues(digest, definition.settings());
        definition.itemSettings().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    update(digest, entry.getKey());
                    updateValues(digest, entry.getValue());
                });
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Returns the complete fingerprint of configuration, source, and dependencies.
     *
     * @param definitionFingerprint authored definition fingerprint
     * @param sourceFingerprint authoritative source fingerprint
     * @param projectRoot containing project root
     * @param dependencies dependency fingerprints
     * @return deterministic complete fingerprint
     */
    public static String complete(
            String definitionFingerprint, String sourceFingerprint, Path projectRoot, Map<Path, String> dependencies) {
        MessageDigest digest = sha256();
        update(digest, definitionFingerprint);
        update(digest, sourceFingerprint);
        dependencies.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            update(digest, projectRoot.relativize(entry.getKey()).toString().replace('\\', '/'));
            update(digest, entry.getValue());
        });
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Adds ordered strings with unambiguous length prefixes. */
    private static void updateStrings(MessageDigest digest, List<String> values) {
        update(digest, Integer.toString(values.size()));
        values.forEach(value -> update(digest, value));
    }

    /** Adds a property map in key order so JSON member order is not significant. */
    private static void updateValues(MessageDigest digest, Map<String, ProjectValue> values) {
        update(digest, Integer.toString(values.size()));
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            update(digest, entry.getKey());
            updateValue(digest, entry.getValue());
        });
    }

    /** Adds one portable project value with an explicit kind marker. */
    private static void updateValue(MessageDigest digest, ProjectValue value) {
        Objects.requireNonNull(value, "value");
        switch (value) {
            case ProjectValue.NullValue ignored -> update(digest, "null");
            case ProjectValue.BooleanValue booleanValue -> update(digest, Boolean.toString(booleanValue.value()));
            case ProjectValue.NumberValue numberValue -> updateNumber(digest, numberValue.value());
            case ProjectValue.TextValue textValue -> updateText(digest, textValue.value());
            case ProjectValue.ArrayValue arrayValue -> updateArray(digest, arrayValue.values());
            case ProjectValue.ObjectValue objectValue -> {
                update(digest, "object");
                updateValues(digest, objectValue.values());
            }
            case ProjectValue.ReferenceValue referenceValue -> {
                update(digest, "reference");
                update(digest, referenceValue.reference().toString());
            }
        }
    }

    /** Adds one normalized arbitrary-precision number. */
    private static void updateNumber(MessageDigest digest, BigDecimal value) {
        update(digest, "number");
        update(digest, value.stripTrailingZeros().toPlainString());
    }

    /** Adds one text value with its kind marker. */
    private static void updateText(MessageDigest digest, String value) {
        update(digest, "text");
        update(digest, value);
    }

    /** Adds one ordered array. */
    private static void updateArray(MessageDigest digest, List<ProjectValue> values) {
        update(digest, "array");
        update(digest, Integer.toString(values.size()));
        values.forEach(value -> updateValue(digest, value));
    }

    /** Adds one UTF-8 value with an unambiguous byte length. */
    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    /** Creates a required JDK SHA-256 digest. */
    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("Java runtime does not provide SHA-256", exception);
        }
    }
}
