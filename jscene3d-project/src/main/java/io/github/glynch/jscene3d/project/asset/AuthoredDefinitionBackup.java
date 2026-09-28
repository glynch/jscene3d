/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePortableLocator;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireProjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.internal.ProjectJsonTreeWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic editor-recovery state for one source-preserving authored definition.
 *
 * <p>The backup records stable project, definition, kind, source, and persisted-source fingerprint identities beside
 * the current retained authored tree. It contains no live-session revision or Java implementation object. Decoding is
 * strict, and restoration revalidates the retained tree through the target document's normal authoritative loader.
 */
public final class AuthoredDefinitionBackup {
    /** Current deterministic backup-envelope version. */
    public static final int FORMAT_VERSION = 1;

    private static final String FORMAT = "jscene3d-authored-definition-backup";
    private static final Set<String> ROOT_FIELDS = Set.of("format", "version", "projectId", "definition");
    private static final Set<String> DEFINITION_FIELDS =
            Set.of("assetId", "kind", "source", "persistedSourceSha256", "content");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private final String projectId;
    private final AssetId definition;
    private final AssetKind kind;
    private final String source;
    private final String persistedSourceSha256;
    private final ObjectNode content;

    /** Stores one already validated recovery envelope and owns a detached content tree. */
    private AuthoredDefinitionBackup(
            String projectId,
            AssetId definition,
            AssetKind kind,
            String source,
            String persistedSourceSha256,
            ObjectNode content) {
        this.projectId = requireProjectId(projectId, "projectId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.source = requirePortableLocator(source, "source");
        this.persistedSourceSha256 = requireSha256(persistedSourceSha256);
        this.content = Objects.requireNonNull(content, "content").deepCopy();
    }

    /**
     * Captures current source-preserving authored state without reading or writing the physical source.
     *
     * @param projectId stable owning project identity
     * @param document current authoritative authored document
     * @return immutable recovery state
     */
    public static AuthoredDefinitionBackup create(String projectId, AuthoredDefinitionDocument document) {
        AuthoredDefinitionDocument current = Objects.requireNonNull(document, "document");
        return new AuthoredDefinitionBackup(
                projectId,
                current.id(),
                current.kind(),
                current.backupSourceIdentity(),
                current.sourceFingerprint().hexadecimal(),
                current.backupTree());
    }

    /**
     * Strictly decodes one untrusted recovery envelope.
     *
     * @param encoded complete UTF-8 backup JSON
     * @return decoded recovery state or a bounded format rejection
     */
    public static DecodeResult decode(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        JsonNode parsed;
        try {
            parsed = ProjectJsonReader.strict().readTree(new ByteArrayInputStream(encoded));
        } catch (IOException exception) {
            return new DecodeResult.Rejected(DecodeFailure.MALFORMED);
        }
        if (!(parsed instanceof ObjectNode root)) {
            return new DecodeResult.Rejected(DecodeFailure.MALFORMED);
        }
        if (!hasExactly(root, ROOT_FIELDS)) {
            return new DecodeResult.Rejected(DecodeFailure.INVALID_STRUCTURE);
        }
        JsonNode format = root.get("format");
        JsonNode version = root.get("version");
        if (format == null
                || !format.isTextual()
                || version == null
                || !version.isIntegralNumber()
                || !version.canConvertToInt()) {
            return new DecodeResult.Rejected(DecodeFailure.INVALID_STRUCTURE);
        }
        if (!FORMAT.equals(format.textValue()) || version.intValue() != FORMAT_VERSION) {
            return new DecodeResult.Rejected(DecodeFailure.UNSUPPORTED_FORMAT);
        }
        return decodeDefinition(root);
    }

    /**
     * Serializes this recovery state deterministically as human-readable UTF-8 JSON with one trailing newline.
     *
     * @return detached encoded backup bytes
     */
    public byte[] encode() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("format", FORMAT);
        root.put("version", FORMAT_VERSION);
        root.put("projectId", projectId);
        ObjectNode authored = root.putObject("definition");
        authored.put("assetId", definition.toString());
        authored.put("kind", kind.serializedName());
        authored.put("source", source);
        authored.put("persistedSourceSha256", persistedSourceSha256);
        authored.set("content", content.deepCopy());
        try {
            return ProjectJsonTreeWriter.serialize(root);
        } catch (IOException exception) {
            throw new IllegalStateException("authored definition backup could not be serialized", exception);
        }
    }

    /**
     * Returns the stable owning project identifier.
     *
     * @return project identity
     */
    public String projectId() {
        return projectId;
    }

    /**
     * Returns the stable authored-definition identity.
     *
     * @return definition identity
     */
    public AssetId definition() {
        return definition;
    }

    /**
     * Returns the authored structural kind.
     *
     * @return world or entity definition kind
     */
    public AssetKind kind() {
        return kind;
    }

    /**
     * Returns the portable project-relative trusted source identity.
     *
     * @return relative authored source locator
     */
    public String source() {
        return source;
    }

    /**
     * Returns the exact persisted-source SHA-256 baseline captured by this backup.
     *
     * @return lowercase hexadecimal SHA-256 value
     */
    public String persistedSourceSha256() {
        return persistedSourceSha256;
    }

    /**
     * Validates this recovery state against a newly loaded authoritative document.
     *
     * <p>The target's current persisted-source fingerprint must equal the backup baseline. The restored candidate
     * retains that target baseline rather than treating backup bytes as physical persisted content.
     *
     * @param expectedProjectId stable identity of the newly loaded project
     * @param target newly loaded target authored document
     * @return restored candidate, identity conflict, source conflict, or validation rejection
     */
    public RestoreResult restore(String expectedProjectId, AuthoredDefinitionDocument target) {
        String validProjectId = requireProjectId(expectedProjectId, "expectedProjectId");
        AuthoredDefinitionDocument validTarget = Objects.requireNonNull(target, "target");
        Optional<IdentityMismatch> mismatch = identityMismatch(validProjectId, validTarget);
        if (mismatch.isPresent()) {
            return new RestoreResult.IdentityRejected(mismatch.orElseThrow());
        }
        if (!persistedSourceSha256.equals(validTarget.sourceFingerprint().hexadecimal())) {
            return new RestoreResult.SourceConflict();
        }
        return switch (validTarget.restoreBackupTree(content)) {
            case AuthoredDefinitionDocument.CandidateResult.Accepted accepted ->
                new RestoreResult.Restored(accepted.document());
            case AuthoredDefinitionDocument.CandidateResult.Rejected rejected ->
                new RestoreResult.ValidationRejected(rejected.diagnostics());
            case AuthoredDefinitionDocument.CandidateResult.InvalidTarget ignored ->
                throw new IllegalStateException("complete backup tree produced a property-target rejection");
            case AuthoredDefinitionDocument.CandidateResult.InvalidEntityTarget ignored ->
                throw new IllegalStateException("complete backup tree produced an entity-target rejection");
        };
    }

    /** Decodes validated identity and content members from one structurally complete root. */
    private static DecodeResult decodeDefinition(ObjectNode root) {
        JsonNode projectId = root.get("projectId");
        JsonNode definition = root.get("definition");
        if (!isNonBlankText(projectId)
                || !(definition instanceof ObjectNode authored)
                || !hasExactly(authored, DEFINITION_FIELDS)) {
            return new DecodeResult.Rejected(DecodeFailure.INVALID_STRUCTURE);
        }
        try {
            return decoded(projectId.textValue(), authored);
        } catch (IllegalArgumentException exception) {
            return new DecodeResult.Rejected(DecodeFailure.INVALID_STRUCTURE);
        }
    }

    /** Decodes one definition object whose required member set has already been checked. */
    private static DecodeResult decoded(String projectId, ObjectNode authored) {
        JsonNode assetId = authored.get("assetId");
        JsonNode kind = authored.get("kind");
        JsonNode source = authored.get("source");
        JsonNode fingerprint = authored.get("persistedSourceSha256");
        JsonNode content = authored.get("content");
        if (!isNonBlankText(assetId)
                || !isNonBlankText(kind)
                || !isNonBlankText(source)
                || !isNonBlankText(fingerprint)
                || !(content instanceof ObjectNode object)) {
            return new DecodeResult.Rejected(DecodeFailure.INVALID_STRUCTURE);
        }
        Optional<AssetKind> decodedKind = AssetKind.fromSerializedName(kind.textValue());
        if (decodedKind.isEmpty()) {
            return new DecodeResult.Rejected(DecodeFailure.INVALID_STRUCTURE);
        }
        AuthoredDefinitionBackup backup = new AuthoredDefinitionBackup(
                projectId,
                AssetId.from(assetId.textValue()),
                decodedKind.orElseThrow(),
                source.textValue(),
                fingerprint.textValue(),
                object);
        return new DecodeResult.Decoded(backup);
    }

    /** Returns the first stable identity mismatch, if any. */
    private Optional<IdentityMismatch> identityMismatch(String expectedProjectId, AuthoredDefinitionDocument target) {
        if (!projectId.equals(expectedProjectId)) {
            return Optional.of(IdentityMismatch.PROJECT);
        }
        if (!definition.equals(target.id())) {
            return Optional.of(IdentityMismatch.DEFINITION);
        }
        if (kind != target.kind()) {
            return Optional.of(IdentityMismatch.KIND);
        }
        if (!source.equals(target.backupSourceIdentity())) {
            return Optional.of(IdentityMismatch.SOURCE);
        }
        return Optional.empty();
    }

    /** Returns whether an object has exactly the required member names. */
    private static boolean hasExactly(ObjectNode object, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        object.fieldNames().forEachRemaining(actual::add);
        return actual.equals(expected);
    }

    /** Returns whether a node is a non-blank JSON string. */
    private static boolean isNonBlankText(JsonNode node) {
        return node != null && node.isTextual() && !node.textValue().isBlank();
    }

    /** Validates one serialized persisted-source fingerprint. */
    private static String requireSha256(String value) {
        String fingerprint = Objects.requireNonNull(value, "persistedSourceSha256");
        if (!SHA_256.matcher(fingerprint).matches()) {
            throw new IllegalArgumentException("persistedSourceSha256 must be a lowercase SHA-256 value");
        }
        return fingerprint;
    }

    /** Closed result of decoding untrusted backup bytes. */
    public sealed interface DecodeResult permits DecodeResult.Decoded, DecodeResult.Rejected {
        /** Successfully decoded immutable recovery state.
         *
         * @param backup decoded recovery state
         */
        record Decoded(AuthoredDefinitionBackup backup) implements DecodeResult {
            /** Validates the decoded backup. */
            public Decoded {
                Objects.requireNonNull(backup, "backup");
            }
        }

        /** Rejected recovery bytes.
         *
         * @param failure bounded decoding failure
         */
        record Rejected(DecodeFailure failure) implements DecodeResult {
            /** Validates the decoding failure. */
            public Rejected {
                Objects.requireNonNull(failure, "failure");
            }
        }
    }

    /** Stable reasons that untrusted recovery bytes cannot be decoded. */
    public enum DecodeFailure {
        /** Bytes are not one complete strict JSON object. */
        MALFORMED,
        /** The explicit format marker or version is unsupported. */
        UNSUPPORTED_FORMAT,
        /** Required structure or identity values are missing or invalid. */
        INVALID_STRUCTURE
    }

    /** Stable backup-to-target identity dimensions. */
    public enum IdentityMismatch {
        /** Stable project identity differs. */
        PROJECT,
        /** Authored asset identity differs. */
        DEFINITION,
        /** Authored structural kind differs. */
        KIND,
        /** Portable trusted source identity differs. */
        SOURCE
    }

    /** Closed result of validating backup recovery against a newly loaded document. */
    public sealed interface RestoreResult
            permits RestoreResult.Restored,
                    RestoreResult.IdentityRejected,
                    RestoreResult.SourceConflict,
                    RestoreResult.ValidationRejected {
        /** Successfully validated current authored recovery state.
         *
         * @param document restored current document carrying the target's persisted-source baseline
         */
        record Restored(AuthoredDefinitionDocument document) implements RestoreResult {
            /** Validates the restored document. */
            public Restored {
                Objects.requireNonNull(document, "document");
            }
        }

        /** Stable identity mismatch between backup and target.
         *
         * @param mismatch mismatching identity dimension
         */
        record IdentityRejected(IdentityMismatch mismatch) implements RestoreResult {
            /** Validates the identity rejection. */
            public IdentityRejected {
                Objects.requireNonNull(mismatch, "mismatch");
            }
        }

        /** Persisted physical-source baseline differs from the captured backup baseline. */
        record SourceConflict() implements RestoreResult {}

        /** Backup content failed normal authoritative definition validation.
         *
         * @param diagnostics immutable ordered validation diagnostics
         */
        record ValidationRejected(List<ProjectDiagnostic> diagnostics) implements RestoreResult {
            /** Copies non-empty validation diagnostics. */
            public ValidationRejected {
                diagnostics = List.copyOf(diagnostics);
                if (diagnostics.isEmpty()) {
                    throw new IllegalArgumentException("backup validation rejection requires diagnostics");
                }
            }
        }
    }
}
