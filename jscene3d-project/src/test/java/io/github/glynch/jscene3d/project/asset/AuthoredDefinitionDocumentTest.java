/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises source-preserving authored Scene and entity documents. */
final class AuthoredDefinitionDocumentTest {
    private static final AssetId WORLD_ID = AssetId.from("4b067393-2ee9-4345-babc-ea1614fb95c8");
    private static final AssetId ENTITY_DEFINITION_ID = AssetId.from("15a64477-b57f-3ae3-bf65-33cd6baab7b6");
    private static final EntityId ENTITY_ID = EntityId.from("103f3852-1667-4ca9-99d5-ae364754fa53");
    private static final ComponentId COMPONENT_ID = ComponentId.from("64a72fdf-c947-49e8-b592-6f3bd6db263a");
    private static final PropertyId REQUIRED_NUMBER = new PropertyId("required-number");
    private static final PropertyId OPTIONAL_TEXT = new PropertyId("optional-text");
    private static final PropertyId NESTED = new PropertyId("nested");
    private static final AuthoredDefinitionDocument.ComponentPropertyTarget OPTIONAL_TEXT_TARGET =
            new AuthoredDefinitionDocument.ComponentPropertyTarget(ENTITY_ID, COMPONENT_ID, OPTIONAL_TEXT);
    private static final String EXACT_NUMBER = "1234567890.123456789012345678901234567890";
    private static final String WORLD_SOURCE = """
            {
              "$schema": "https://jscene3d.org/schemas/scene-definition-1.json",
              "assetId": "4b067393-2ee9-4345-babc-ea1614fb95c8",
              "assetType": "scene-definition",
              "formatVersion": 1,
              "name": "Preserved World",
              "connections": [],
              "roots": [
                {
                  "entryType": "local",
                  "entityId": "103f3852-1667-4ca9-99d5-ae364754fa53",
                  "name": "Player",
                  "enabled": true,
                  "components": [
                    {
                      "componentId": "64a72fdf-c947-49e8-b592-6f3bd6db263a",
                      "type": "example.test/source-preservation",
                      "typeVersion": 1,
                      "properties": {
                        "required-number": 1234567890.123456789012345678901234567890,
                        "optional-text": "before",
                        "nested": {
                          "zeta": [true, null, {"$ref": "asset:model"}],
                          "alpha": {"$target": {"entityId": "103f3852-1667-4ca9-99d5-ae364754fa53"}},
                          "tiny": 0.000000000000000000000000000001
                        }
                      }
                    }
                  ],
                  "children": []
                },
                {
                  "entryType": "local",
                  "entityId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                  "name": "Unrelated",
                  "enabled": true,
                  "components": [
                    {
                      "componentId": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                      "type": "example.test/source-preservation",
                      "typeVersion": 1,
                      "properties": {"required-number": 1}
                    }
                  ],
                  "children": []
                }
              ]
            }
            """;
    private static final String ENTITY_SOURCE = """
            {
              "$schema": "https://jscene3d.org/schemas/entity-definition-1.json",
              "assetId": "15a64477-b57f-3ae3-bf65-33cd6baab7b6",
              "assetType": "entity-definition",
              "formatVersion": 1,
              "name": "Preserved Entity",
              "contract": {
                "parameters": [],
                "signals": [],
                "actions": [],
                "capabilities": [],
                "attachments": [],
                "resourceBindings": []
              },
              "connections": [],
              "root": {
                "entryType": "local",
                "entityId": "103f3852-1667-4ca9-99d5-ae364754fa53",
                "name": "Reusable Player",
                "enabled": true,
                "components": [
                  {
                    "componentId": "64a72fdf-c947-49e8-b592-6f3bd6db263a",
                    "type": "example.test/source-preservation",
                    "typeVersion": 1,
                    "properties": {
                      "required-number": 1234567890.123456789012345678901234567890,
                      "optional-text": "before",
                      "nested": {"first": 1, "second": [2, 3]}
                    }
                  }
                ],
                "children": []
              }
            }
            """;

    @TempDir
    private Path projectRoot;

    @TempDir
    private Path externalDirectory;

    private Path worldSource;
    private Path entitySource;
    private AssetCatalog assets;
    private RegisteredTypeCatalog types;

    /** Creates representative authored sources and their safe descriptor catalog. */
    @BeforeEach
    void setUp() throws IOException {
        worldSource = projectRoot.resolve("worlds/map01.scene.json");
        entitySource = projectRoot.resolve("entities/player.entity.json");
        Files.createDirectories(worldSource.getParent());
        Files.createDirectories(entitySource.getParent());
        Files.writeString(worldSource, WORLD_SOURCE, StandardCharsets.UTF_8);
        Files.writeString(entitySource, ENTITY_SOURCE, StandardCharsets.UTF_8);
        assets = AssetCatalog.scan(projectRoot).catalog().orElseThrow();
        types = registeredTypes();
    }

    /** Retains one validated tree and matching domain projection for each authored definition kind. */
    @Test
    void loadsWorldAndEntityFromTheirExactSourceBytes() throws IOException {
        AuthoredDefinitionDocument world = load(WORLD_ID);
        AuthoredDefinitionDocument entity = load(ENTITY_DEFINITION_ID);

        assertThat(world.content()).isInstanceOf(AuthoredDefinitionDocument.Content.Scene.class);
        assertThat(entity.content()).isInstanceOf(AuthoredDefinitionDocument.Content.Entity.class);
        assertThat(world.source()).isEqualTo(worldSource.toRealPath());
        assertThat(entity.source()).isEqualTo(entitySource.toRealPath());
        assertThat(world.sourceFingerprint()).isEqualTo(SourceFingerprint.sha256(bytes(worldSource)));
        assertThat(entity.sourceFingerprint()).isEqualTo(SourceFingerprint.sha256(bytes(entitySource)));
    }

    /** Round-trips deterministic world recovery state without losing retained extension values or source order. */
    @Test
    void backsUpAndRestoresSourcePreservingWorldState() throws IOException {
        AuthoredDefinitionDocument persisted = load(WORLD_ID);
        AuthoredDefinitionDocument current =
                accepted(persisted.set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("recovered")));
        AuthoredDefinitionBackup backup = AuthoredDefinitionBackup.create("example.backup-test", current);

        byte[] encoded = backup.encode();
        AuthoredDefinitionBackup decoded = decoded(encoded);
        AuthoredDefinitionDocument restored = restored(decoded.restore("example.backup-test", persisted));
        JsonNode restoredProperties = properties(restored);

        assertThat(decoded.encode()).isEqualTo(encoded);
        assertThat(decoded)
                .returns(WORLD_ID, AuthoredDefinitionBackup::definition)
                .returns(AssetKind.SCENE_DEFINITION, AuthoredDefinitionBackup::kind)
                .returns("worlds/map01.scene.json", AuthoredDefinitionBackup::source)
                .returns(persisted.sourceFingerprint().hexadecimal(), AuthoredDefinitionBackup::persistedSourceSha256);
        assertThat(restored.sourceFingerprint()).isEqualTo(persisted.sourceFingerprint());
        assertThat(restoredProperties.path("optional-text").textValue()).isEqualTo("recovered");
        assertThat(fieldNames(restoredProperties.path("nested"))).containsExactly("zeta", "alpha", "tiny");
        assertThat(restoredProperties
                        .path("nested")
                        .path("zeta")
                        .path(2)
                        .path("$ref")
                        .textValue())
                .isEqualTo("asset:model");
        assertThat(restoredProperties
                        .path("nested")
                        .path("alpha")
                        .path("$target")
                        .path("entityId")
                        .textValue())
                .isEqualTo(ENTITY_ID.toString());
        assertThat(restoredProperties.path("nested").path("tiny").decimalValue())
                .isEqualByComparingTo("0.000000000000000000000000000001");
        assertThat(bytes(worldSource)).isEqualTo(WORLD_SOURCE.getBytes(StandardCharsets.UTF_8));
    }

    /** Applies the same deterministic recovery envelope and exact-decimal semantics to entity definitions. */
    @Test
    void backsUpAndRestoresEntityStateWithExactDecimals() throws IOException {
        AuthoredDefinitionDocument persisted = load(ENTITY_DEFINITION_ID);
        AuthoredDefinitionDocument.ComponentPropertyTarget target =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(
                        ENTITY_ID, COMPONENT_ID, new PropertyId("optional-number"));
        AuthoredDefinitionDocument current =
                accepted(persisted.set(target, new ProjectValue.NumberValue(new BigDecimal(EXACT_NUMBER))));

        AuthoredDefinitionBackup backup = decoded(
                AuthoredDefinitionBackup.create("example.backup-test", current).encode());
        AuthoredDefinitionDocument restored = restored(backup.restore("example.backup-test", persisted));

        assertThat(backup.kind()).isEqualTo(AssetKind.ENTITY_DEFINITION);
        assertThat(property(restored, "optional-number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(StreamSupport.stream(
                                property(restored, "nested").path("second").spliterator(), false)
                        .map(JsonNode::intValue)
                        .toList())
                .containsExactly(2, 3);
        assertThat(bytes(entitySource)).isEqualTo(ENTITY_SOURCE.getBytes(StandardCharsets.UTF_8));
    }

    /** Rejects malformed, unsupported, duplicate-member, and structurally invalid recovery envelopes. */
    @Test
    void strictlyRejectsInvalidBackupEncoding() {
        byte[] valid = AuthoredDefinitionBackup.create("example.backup-test", load(WORLD_ID))
                .encode();
        byte[] unsupported = replace(valid, "\"version\" : 1", "\"version\" : 2");
        byte[] unknown = replace(valid, "\"projectId\"", "\"unexpected\"");
        byte[] duplicate = replace(
                valid,
                "\"projectId\" : \"example.backup-test\"",
                "\"projectId\" : \"example.backup-test\", \"projectId\" : \"example.backup-test\"");

        assertDecodeFailure("{".getBytes(StandardCharsets.UTF_8), AuthoredDefinitionBackup.DecodeFailure.MALFORMED);
        assertDecodeFailure(unsupported, AuthoredDefinitionBackup.DecodeFailure.UNSUPPORTED_FORMAT);
        assertDecodeFailure(unknown, AuthoredDefinitionBackup.DecodeFailure.INVALID_STRUCTURE);
        assertDecodeFailure(duplicate, AuthoredDefinitionBackup.DecodeFailure.MALFORMED);
    }

    /** Identifies each stable target mismatch and persisted-source conflict before validating backup content. */
    @Test
    void rejectsMismatchedBackupIdentityAndPersistedFingerprint() throws IOException {
        AuthoredDefinitionDocument target = load(WORLD_ID);
        AuthoredDefinitionBackup original = AuthoredDefinitionBackup.create("example.backup-test", target);
        AuthoredDefinitionBackup wrongDefinition = tampered(
                original, "\"assetId\" : \"" + WORLD_ID + "\"", "\"assetId\" : \"" + ENTITY_DEFINITION_ID + "\"");
        AuthoredDefinitionBackup wrongKind =
                tampered(original, "\"kind\" : \"scene-definition\"", "\"kind\" : \"entity-definition\"");
        AuthoredDefinitionBackup wrongSource = tampered(
                original, "\"source\" : \"worlds/map01.scene.json\"", "\"source\" : \"worlds/other.scene.json\"");
        AuthoredDefinitionBackup wrongFingerprint =
                tampered(original, original.persistedSourceSha256(), "0".repeat(64));

        assertIdentityMismatch(
                original.restore("different.project", target), AuthoredDefinitionBackup.IdentityMismatch.PROJECT);
        assertIdentityMismatch(
                wrongDefinition.restore("example.backup-test", target),
                AuthoredDefinitionBackup.IdentityMismatch.DEFINITION);
        assertIdentityMismatch(
                wrongKind.restore("example.backup-test", target), AuthoredDefinitionBackup.IdentityMismatch.KIND);
        assertIdentityMismatch(
                wrongSource.restore("example.backup-test", target), AuthoredDefinitionBackup.IdentityMismatch.SOURCE);
        assertThat(wrongFingerprint.restore("example.backup-test", target))
                .isInstanceOf(AuthoredDefinitionBackup.RestoreResult.SourceConflict.class);
    }

    /** Rejects semantically invalid recovery content through normal authoritative definition validation. */
    @Test
    void rejectsSemanticallyInvalidBackupContent() throws IOException {
        AuthoredDefinitionDocument target = load(WORLD_ID);
        AuthoredDefinitionBackup backup = AuthoredDefinitionBackup.create("example.backup-test", target);
        AuthoredDefinitionBackup invalid = tampered(
                backup,
                "\"required-number\" : 1234567890.123456789012345678901234567890",
                "\"required-number\" : \"invalid\"");

        AuthoredDefinitionBackup.RestoreResult result = invalid.restore("example.backup-test", target);

        assertThat(result).isInstanceOf(AuthoredDefinitionBackup.RestoreResult.ValidationRejected.class);
        assertThat(((AuthoredDefinitionBackup.RestoreResult.ValidationRejected) result).diagnostics())
                .isNotEmpty();
        assertThat(property(target, "required-number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(bytes(worldSource)).isEqualTo(WORLD_SOURCE.getBytes(StandardCharsets.UTF_8));
    }

    /** Replaces one world property on a copied tree and preserves the accepted document. */
    @Test
    void setsWorldPropertyOnValidatedCopy() throws IOException {
        AuthoredDefinitionDocument original = load(WORLD_ID);

        AuthoredDefinitionDocument candidate =
                accepted(original.set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));

        assertThat(property(original, "optional-text").textValue()).isEqualTo("before");
        assertThat(property(candidate, "optional-text").textValue()).isEqualTo("after");
        assertThat(candidate.content()).isNotEqualTo(original.content());
        assertThat(candidate.sourceFingerprint()).isEqualTo(original.sourceFingerprint());
        assertThat(bytes(worldSource)).isEqualTo(WORLD_SOURCE.getBytes(StandardCharsets.UTF_8));
    }

    /** Reports a semantically equal SET as a no-op without replacing the accepted document. */
    @Test
    void settingEqualValueIsNoOp() {
        AuthoredDefinitionDocument original = load(WORLD_ID);

        AuthoredDefinitionDocument.CandidateResult.Accepted result =
                (AuthoredDefinitionDocument.CandidateResult.Accepted)
                        original.set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("before"));

        assertThat(result.changed()).isFalse();
        assertThat(result.document()).isSameAs(original);
    }

    /** Adds one absent entity property using exact decimal ProjectValue encoding. */
    @Test
    void addsEntityPropertyAndPreservesExactDecimal() throws IOException {
        AuthoredDefinitionDocument original = load(ENTITY_DEFINITION_ID);
        ProjectValue.NumberValue exact = new ProjectValue.NumberValue(new BigDecimal(EXACT_NUMBER));
        AuthoredDefinitionDocument.ComponentPropertyTarget target =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(
                        ENTITY_ID, COMPONENT_ID, new PropertyId("optional-number"));

        AuthoredDefinitionDocument candidate = accepted(original.set(target, exact));

        assertThat(property(candidate, "optional-number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(candidate.serialize()).endsWith((byte) '\n');
        assertThat(ProjectJsonReader.strict().readTree(new ByteArrayInputStream(candidate.serialize())))
                .isNotNull();
    }

    /** Encodes every ProjectValue kind through the shared authoritative JSON mapping. */
    @Test
    void setsNestedValueContainingEveryStructuralKind() throws IOException {
        Map<String, ProjectValue> values = new LinkedHashMap<>();
        values.put("null", ProjectValue.NullValue.INSTANCE);
        values.put("boolean", new ProjectValue.BooleanValue(true));
        values.put("number", new ProjectValue.NumberValue(new BigDecimal(EXACT_NUMBER)));
        values.put("text", new ProjectValue.TextValue("value"));
        values.put("array", new ProjectValue.ArrayValue(List.of(new ProjectValue.TextValue("first"))));
        values.put("reference", new ProjectValue.ReferenceValue(ResourceReference.asset("model")));
        values.put("entity", new ProjectValue.EntityTargetValue(ENTITY_ID));
        values.put("component", new ProjectValue.ComponentTargetValue(new ComponentTarget(ENTITY_ID, COMPONENT_ID)));

        AuthoredDefinitionDocument candidate = accepted(load(WORLD_ID)
                .set(
                        new AuthoredDefinitionDocument.ComponentPropertyTarget(ENTITY_ID, COMPONENT_ID, NESTED),
                        new ProjectValue.ObjectValue(values)));
        JsonNode nested = property(candidate, "nested");

        assertThat(fieldNames(nested)).containsExactlyElementsOf(values.keySet());
        assertThat(nested.path("null").isNull()).isTrue();
        assertThat(nested.path("number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(nested.path("reference").path("$ref").textValue()).isEqualTo("asset:model");
        assertThat(nested.path("entity").path("$target").path("entityId").textValue())
                .isEqualTo(ENTITY_ID.toString());
        assertThat(nested.path("component").path("$target").path("componentId").textValue())
                .isEqualTo(COMPONENT_ID.toString());
    }

    /** Preserves nested extension values and declaration order around one replacement. */
    @Test
    void preservesUnrelatedMembersAndOrdering() throws IOException {
        AuthoredDefinitionDocument original = load(WORLD_ID);

        AuthoredDefinitionDocument candidate =
                accepted(original.set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));
        JsonNode tree = tree(candidate);

        assertThat(fieldNames(tree))
                .containsExactly("$schema", "assetId", "assetType", "formatVersion", "name", "connections", "roots");
        assertThat(fieldNames(properties(tree))).containsExactly("required-number", "optional-text", "nested");
        assertThat(fieldNames(property(tree, "nested"))).containsExactly("zeta", "alpha", "tiny");
        assertThat(property(tree, "required-number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(property(tree, "nested").path("zeta").path(2).path("$ref").textValue())
                .isEqualTo("asset:model");
        assertThat(property(tree, "nested").path("tiny").decimalValue())
                .isEqualByComparingTo("0.000000000000000000000000000001");
        assertThat(tree.path("roots").path(1).path("name").textValue()).isEqualTo("Unrelated");
    }

    /** Removes optional authored state without materializing a descriptor default. */
    @Test
    void removesOptionalPropertyFromWorldAndEntity() throws IOException {
        AuthoredDefinitionDocument world = accepted(load(WORLD_ID).remove(OPTIONAL_TEXT_TARGET));
        AuthoredDefinitionDocument entity = accepted(load(ENTITY_DEFINITION_ID).remove(OPTIONAL_TEXT_TARGET));

        assertThat(properties(tree(world)).has("optional-text")).isFalse();
        assertThat(properties(tree(entity)).has("optional-text")).isFalse();
        assertThat(fieldNames(properties(tree(world)))).containsExactly("required-number", "nested");
        assertThat(fieldNames(properties(tree(entity)))).containsExactly("required-number", "nested");
    }

    /** Removes an authored optional property that has no descriptor default. */
    @Test
    void removesOptionalPropertyWithoutDefault() throws IOException {
        AuthoredDefinitionDocument.ComponentPropertyTarget target =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(
                        ENTITY_ID, COMPONENT_ID, new PropertyId("optional-number"));
        AuthoredDefinitionDocument withOptional =
                accepted(load(ENTITY_DEFINITION_ID).set(target, new ProjectValue.NumberValue(BigDecimal.ONE)));

        AuthoredDefinitionDocument removed = accepted(withOptional.remove(target));

        assertThat(properties(tree(removed)).has("optional-number")).isFalse();
        assertThat(fieldNames(properties(tree(removed)))).containsExactly("required-number", "optional-text", "nested");
    }

    /** Reports absent-property removal as an accepted no-op without replacing the document. */
    @Test
    void removingAbsentPropertyIsNoOp() {
        AuthoredDefinitionDocument original = load(WORLD_ID);
        AuthoredDefinitionDocument.ComponentPropertyTarget missing =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(
                        ENTITY_ID, COMPONENT_ID, new PropertyId("optional-number"));

        AuthoredDefinitionDocument.CandidateResult.Accepted result =
                (AuthoredDefinitionDocument.CandidateResult.Accepted) original.remove(missing);

        assertThat(result.changed()).isFalse();
        assertThat(result.document()).isSameAs(original);
    }

    /** Rejects required-property removal atomically through normal definition validation. */
    @Test
    void rejectsRequiredPropertyRemovalWithoutChangingAcceptedState() throws IOException {
        AuthoredDefinitionDocument original = load(WORLD_ID);
        byte[] accepted = original.serialize();
        AuthoredDefinitionDocument.ComponentPropertyTarget target =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(ENTITY_ID, COMPONENT_ID, REQUIRED_NUMBER);

        AuthoredDefinitionDocument.CandidateResult result = original.remove(target);

        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.CandidateResult.Rejected.class);
        assertThat(((AuthoredDefinitionDocument.CandidateResult.Rejected) result).diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("property.required");
        assertThat(original.serialize()).isEqualTo(accepted);
        assertThat(original.verifySource()).isEqualTo(AuthoredDefinitionDocument.SourceStatus.MATCH);
    }

    /** Rejects a wrong structural value without leaking the candidate into accepted state. */
    @Test
    void rejectsWrongStructuralSetAtomically() throws IOException {
        AuthoredDefinitionDocument original = load(ENTITY_DEFINITION_ID);

        AuthoredDefinitionDocument.CandidateResult result = original.set(
                new AuthoredDefinitionDocument.ComponentPropertyTarget(ENTITY_ID, COMPONENT_ID, REQUIRED_NUMBER),
                new ProjectValue.TextValue("not a number"));

        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.CandidateResult.Rejected.class);
        assertThat(property(original, "required-number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(bytes(entitySource)).isEqualTo(ENTITY_SOURCE.getBytes(StandardCharsets.UTF_8));
    }

    /** Rejects a structurally valid value that violates authoritative numeric semantics. */
    @Test
    void rejectsSemanticValidationFailureAtomically() throws IOException {
        AuthoredDefinitionDocument original = load(WORLD_ID);

        AuthoredDefinitionDocument.CandidateResult result = original.set(
                new AuthoredDefinitionDocument.ComponentPropertyTarget(ENTITY_ID, COMPONENT_ID, REQUIRED_NUMBER),
                new ProjectValue.NumberValue(new BigDecimal("-0.000000000000000000000000000001")));

        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.CandidateResult.Rejected.class);
        assertThat(((AuthoredDefinitionDocument.CandidateResult.Rejected) result).diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("property.minimum");
        assertThat(property(original, "required-number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(bytes(worldSource)).isEqualTo(WORLD_SOURCE.getBytes(StandardCharsets.UTF_8));
    }

    /** Distinguishes a placement or unknown component from a validation rejection. */
    @Test
    void rejectsNonLocalOrUnknownPatchTarget() {
        AuthoredDefinitionDocument original = load(WORLD_ID);
        AuthoredDefinitionDocument.ComponentPropertyTarget target =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(
                        EntityId.from("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"), COMPONENT_ID, OPTIONAL_TEXT);

        AuthoredDefinitionDocument.CandidateResult result = original.set(target, new ProjectValue.TextValue("after"));

        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.CandidateResult.InvalidTarget.class);
    }

    /** Applies entity-enabled candidates to both authored definition kinds and reports no-op or invalid targets. */
    @Test
    void setsEntityEnabledAcrossWorldAndEntityDocuments() throws IOException {
        AuthoredDefinitionDocument world = load(WORLD_ID);
        AuthoredDefinitionDocument entity = load(ENTITY_DEFINITION_ID);

        AuthoredDefinitionDocument changedWorld = accepted(world.setEntityEnabled(ENTITY_ID, false));
        AuthoredDefinitionDocument changedEntity = accepted(entity.setEntityEnabled(ENTITY_ID, false));
        AuthoredDefinitionDocument.CandidateResult unchanged = changedWorld.setEntityEnabled(ENTITY_ID, false);
        AuthoredDefinitionDocument.CandidateResult missing =
                world.setEntityEnabled(EntityId.from("ffffffff-ffff-4fff-8fff-ffffffffffff"), false);

        assertThat(tree(changedWorld).path("roots").path(0).path("enabled").booleanValue())
                .isFalse();
        assertThat(tree(changedEntity).path("root").path("enabled").booleanValue())
                .isFalse();
        assertThat(((AuthoredDefinitionDocument.CandidateResult.Accepted) unchanged).changed())
                .isFalse();
        assertThat(missing).isInstanceOf(AuthoredDefinitionDocument.CandidateResult.InvalidEntityTarget.class);
    }

    /** Compares authored tree state independently of object identity and persisted-source fingerprint. */
    @Test
    void comparesSemanticAuthoredState() {
        AuthoredDefinitionDocument original = load(WORLD_ID);
        AuthoredDefinitionDocument changed =
                accepted(original.set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));
        AuthoredDefinitionDocument restored =
                accepted(changed.set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("before")));

        assertThat(original.hasSameAuthoredState(changed)).isFalse();
        assertThat(original.hasSameAuthoredState(restored)).isTrue();
        assertThat(original.hasSameAuthoredState(load(ENTITY_DEFINITION_ID))).isFalse();
    }

    /** Associates current authored state with only a compatible persisted-source baseline. */
    @Test
    void adoptsCompatiblePersistedSourceBaseline() {
        AuthoredDefinitionDocument original = load(WORLD_ID);
        AuthoredDefinitionDocument changed =
                accepted(original.set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));
        AuthoredDefinitionDocument other = load(ENTITY_DEFINITION_ID);

        AuthoredDefinitionDocument rebased = changed.withPersistedSourceBaseline(original);

        assertThat(rebased.hasSameAuthoredState(changed)).isTrue();
        assertThat(rebased.sourceFingerprint()).isEqualTo(original.sourceFingerprint());
        assertThatThrownBy(() -> changed.withPersistedSourceBaseline(other))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same authored source");
    }

    /** Uses exact source content rather than timestamps or file size for conflict detection. */
    @Test
    void detectsExactByteExternalChanges() throws IOException {
        AuthoredDefinitionDocument document = load(WORLD_ID);
        FileTime originalTimestamp = Files.getLastModifiedTime(worldSource);

        Files.setLastModifiedTime(worldSource, FileTime.fromMillis(originalTimestamp.toMillis() + 10_000));
        assertThat(document.verifySource()).isEqualTo(AuthoredDefinitionDocument.SourceStatus.MATCH);

        byte[] changed = bytes(worldSource);
        int offset = indexOf(changed, "Preserved World");
        changed[offset] = 'p';
        Files.write(worldSource, changed);
        Files.setLastModifiedTime(worldSource, originalTimestamp);

        assertThat(Files.size(worldSource)).isEqualTo(WORLD_SOURCE.getBytes(StandardCharsets.UTF_8).length);
        assertThat(Files.getLastModifiedTime(worldSource)).isEqualTo(originalTimestamp);
        assertThat(document.verifySource()).isEqualTo(AuthoredDefinitionDocument.SourceStatus.CONTENT_CHANGED);
        assertThat(document.serialize()).containsExactly(loadTreeBytes(WORLD_SOURCE));
    }

    /** Reports a missing persisted source without changing the retained document. */
    @Test
    void reportsMissingSource() throws IOException {
        AuthoredDefinitionDocument document = load(ENTITY_DEFINITION_ID);

        Files.delete(entitySource);

        assertThat(document.verifySource()).isEqualTo(AuthoredDefinitionDocument.SourceStatus.SOURCE_MISSING);
        assertThat(document.content()).isInstanceOf(AuthoredDefinitionDocument.Content.Entity.class);
    }

    /** Reports a source that no longer resolves to a regular readable file as inaccessible. */
    @Test
    void reportsInaccessibleSource() throws IOException {
        AuthoredDefinitionDocument document = load(WORLD_ID);
        Files.delete(worldSource);
        Files.createDirectory(worldSource);

        assertThat(document.verifySource()).isEqualTo(AuthoredDefinitionDocument.SourceStatus.SOURCE_INACCESSIBLE);
    }

    /** Rejects malformed source before it can become an authored document. */
    @Test
    void rejectsMalformedSource() throws IOException {
        Files.writeString(worldSource, "{", StandardCharsets.UTF_8);

        AuthoredDefinitionDocument.LoadResult result = AuthoredDefinitionDocument.load(assets, assets, types, WORLD_ID);

        assertThat(result.document()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("asset.json");
    }

    /** Rejects well-formed source whose authored envelope uses an unsupported schema version. */
    @Test
    void rejectsSchemaInvalidSource() throws IOException {
        Files.writeString(
                worldSource,
                WORLD_SOURCE.replace("\"formatVersion\": 1", "\"formatVersion\": 2"),
                StandardCharsets.UTF_8);

        AuthoredDefinitionDocument.LoadResult result = AuthoredDefinitionDocument.load(assets, assets, types, WORLD_ID);

        assertThat(result.document()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("asset.format.unsupported");
    }

    /** Rejects a source whose envelope kind no longer agrees with trusted catalog metadata. */
    @Test
    void rejectsDefinitionKindMismatch() throws IOException {
        Files.writeString(
                worldSource,
                WORLD_SOURCE.replace("\"scene-definition\"", "\"entity-definition\""),
                StandardCharsets.UTF_8);

        AuthoredDefinitionDocument.LoadResult result = AuthoredDefinitionDocument.load(assets, assets, types, WORLD_ID);

        assertThat(result.document()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("asset.kind");
    }

    /** Detects a retained source replaced by a symlink that resolves beyond the real project root. */
    @Test
    void rejectsSourceSymlinkReplacementOutsideProject() throws IOException {
        AuthoredDefinitionDocument document = load(WORLD_ID);
        Path outside = externalDirectory.resolve("outside-world.json");
        try {
            Files.writeString(outside, WORLD_SOURCE, StandardCharsets.UTF_8);
            Files.delete(worldSource);
            Files.createSymbolicLink(worldSource, outside);
            assertThat(document.verifySource())
                    .isEqualTo(AuthoredDefinitionDocument.SourceStatus.SOURCE_OUTSIDE_PROJECT);
            AuthoredDefinitionDocument.SaveResult save = document.save();
            assertThat(((AuthoredDefinitionDocument.SaveResult.SourceFailure) save).status())
                    .isEqualTo(AuthoredDefinitionDocument.SourceStatus.SOURCE_OUTSIDE_PROJECT);
        } catch (UnsupportedOperationException exception) {
            Assumptions.abort("symbolic links are not supported");
        } finally {
            Files.deleteIfExists(worldSource);
            Files.deleteIfExists(outside);
        }
    }

    /** Persists source-preserving world and entity trees and fingerprints the exact replacement bytes. */
    @Test
    void savesWorldAndEntityWithExactPersistedFingerprints() throws IOException {
        AuthoredDefinitionDocument changedWorld =
                accepted(load(WORLD_ID).set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));
        AuthoredDefinitionDocument changedEntity =
                accepted(load(ENTITY_DEFINITION_ID).set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));

        AuthoredDefinitionDocument savedWorld = saved(changedWorld.save());
        AuthoredDefinitionDocument savedEntity = saved(changedEntity.save());

        assertThat(bytes(worldSource)).isEqualTo(changedWorld.serialize());
        assertThat(bytes(entitySource)).isEqualTo(changedEntity.serialize());
        assertThat(savedWorld.sourceFingerprint()).isEqualTo(SourceFingerprint.sha256(bytes(worldSource)));
        assertThat(savedEntity.sourceFingerprint()).isEqualTo(SourceFingerprint.sha256(bytes(entitySource)));
        assertThat(property(savedWorld, "required-number").decimalValue()).isEqualByComparingTo(EXACT_NUMBER);
        assertThat(fieldNames(property(savedWorld, "nested"))).containsExactly("zeta", "alpha", "tiny");
        assertThat(property(savedWorld, "nested")
                        .path("zeta")
                        .path(2)
                        .path("$ref")
                        .textValue())
                .isEqualTo("asset:model");
        assertThat(property(savedWorld, "nested")
                        .path("alpha")
                        .path("$target")
                        .path("entityId")
                        .textValue())
                .isEqualTo(ENTITY_ID.toString());
        assertThat(property(savedEntity, "nested").path("second").path(1).decimalValue())
                .isEqualByComparingTo("3");
    }

    /** Rejects a same-size external change even when its original timestamp is restored. */
    @Test
    void rejectsConflictingSaveWithoutChangingDiskOrCandidate() throws IOException {
        AuthoredDefinitionDocument changed =
                accepted(load(WORLD_ID).set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));
        byte[] candidate = changed.serialize();
        FileTime originalTimestamp = Files.getLastModifiedTime(worldSource);
        byte[] external = bytes(worldSource);
        external[indexOf(external, "Preserved World")] = 'p';
        Files.write(worldSource, external);
        Files.setLastModifiedTime(worldSource, originalTimestamp);

        AuthoredDefinitionDocument.SaveResult result = changed.save();

        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.SaveResult.SourceFailure.class);
        assertThat(((AuthoredDefinitionDocument.SaveResult.SourceFailure) result).status())
                .isEqualTo(AuthoredDefinitionDocument.SourceStatus.CONTENT_CHANGED);
        assertThat(bytes(worldSource)).isEqualTo(external);
        assertThat(changed.serialize()).isEqualTo(candidate);
    }

    /** Reports bounded source lifecycle failures before save can invoke the atomic writer. */
    @Test
    void rejectsMissingAndInaccessibleSaveSources() throws IOException {
        AuthoredDefinitionDocument missing = load(WORLD_ID);
        AuthoredDefinitionDocument inaccessible = load(ENTITY_DEFINITION_ID);
        Files.delete(worldSource);
        Files.delete(entitySource);
        Files.createDirectory(entitySource);

        AuthoredDefinitionDocument.SaveResult missingResult = missing.save();
        AuthoredDefinitionDocument.SaveResult inaccessibleResult = inaccessible.save();

        assertThat(((AuthoredDefinitionDocument.SaveResult.SourceFailure) missingResult).status())
                .isEqualTo(AuthoredDefinitionDocument.SourceStatus.SOURCE_MISSING);
        assertThat(((AuthoredDefinitionDocument.SaveResult.SourceFailure) inaccessibleResult).status())
                .isEqualTo(AuthoredDefinitionDocument.SourceStatus.SOURCE_INACCESSIBLE);
    }

    /** Leaves disk and candidate state unchanged when the atomic writer cannot create its sibling temporary file. */
    @Test
    void reportsAtomicWriterFailureWithoutChangingAcceptedState() throws IOException {
        Path parent = worldSource.getParent();
        Assumptions.assumeTrue(Files.getFileStore(parent).supportsFileAttributeView("posix"));
        AuthoredDefinitionDocument changed =
                accepted(load(WORLD_ID).set(OPTIONAL_TEXT_TARGET, new ProjectValue.TextValue("after")));
        byte[] before = bytes(worldSource);
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(parent);
        try {
            Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("r-x------"));
            Assumptions.assumeFalse(Files.isWritable(parent));

            AuthoredDefinitionDocument.SaveResult result = changed.save();

            assertThat(result).isInstanceOf(AuthoredDefinitionDocument.SaveResult.WriteFailure.class);
            assertThat(((AuthoredDefinitionDocument.SaveResult.WriteFailure) result)
                            .diagnostic()
                            .code()
                            .code())
                    .isEqualTo("asset.file.write");
            assertThat(bytes(worldSource)).isEqualTo(before);
            assertThat(property(changed, "optional-text").textValue()).isEqualTo("after");
        } finally {
            Files.setPosixFilePermissions(parent, originalPermissions);
        }
    }

    /** Reloads externally changed content while rejecting malformed content atomically. */
    @Test
    void reloadsExternalSourceAndRejectsMalformedReplacement() throws IOException {
        AuthoredDefinitionDocument original = load(WORLD_ID);
        Files.writeString(worldSource, WORLD_SOURCE.replace("\"before\"", "\"external\""), StandardCharsets.UTF_8);

        AuthoredDefinitionDocument reloaded = loaded(original.reload());
        Files.writeString(worldSource, "{", StandardCharsets.UTF_8);
        AuthoredDefinitionDocument.ReloadResult malformed = reloaded.reload();

        assertThat(property(reloaded, "optional-text").textValue()).isEqualTo("external");
        assertThat(reloaded.sourceFingerprint()).isEqualTo(SourceFingerprint.sha256(bytesBeforeMalformed()));
        assertThat(malformed).isInstanceOf(AuthoredDefinitionDocument.ReloadResult.Rejected.class);
        assertThat(((AuthoredDefinitionDocument.ReloadResult.Rejected) malformed).diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("asset.json");
        assertThat(property(reloaded, "optional-text").textValue()).isEqualTo("external");
    }

    /** Rejects a physical source whose stable identity no longer matches the catalog identity. */
    @Test
    void reloadRejectsWrongDefinitionIdentity() throws IOException {
        AuthoredDefinitionDocument original = load(WORLD_ID);
        Files.writeString(
                worldSource,
                WORLD_SOURCE.replace(WORLD_ID.toString(), ENTITY_DEFINITION_ID.toString()),
                StandardCharsets.UTF_8);

        AuthoredDefinitionDocument.ReloadResult result = original.reload();

        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.ReloadResult.Rejected.class);
        assertThat(((AuthoredDefinitionDocument.ReloadResult.Rejected) result).diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("asset.catalog.stale");
        assertThat(original.id()).isEqualTo(WORLD_ID);
    }

    /** Returns the expected source lifecycle outcome when reload cannot access the retained source. */
    @Test
    void rejectsReloadOfMissingSource() throws IOException {
        AuthoredDefinitionDocument document = load(ENTITY_DEFINITION_ID);
        Files.delete(entitySource);

        AuthoredDefinitionDocument.ReloadResult result = document.reload();

        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.ReloadResult.SourceFailure.class);
        assertThat(((AuthoredDefinitionDocument.ReloadResult.SourceFailure) result).status())
                .isEqualTo(AuthoredDefinitionDocument.SourceStatus.SOURCE_MISSING);
    }

    private AuthoredDefinitionDocument load(AssetId id) {
        AuthoredDefinitionDocument.LoadResult result = AuthoredDefinitionDocument.load(assets, assets, types, id);
        assertThat(result.diagnostics()).isEmpty();
        return result.document().orElseThrow();
    }

    /** Returns one successfully decoded backup or fails with its bounded decoding result. */
    private static AuthoredDefinitionBackup decoded(byte[] encoded) {
        AuthoredDefinitionBackup.DecodeResult result = AuthoredDefinitionBackup.decode(encoded);
        assertThat(result).isInstanceOf(AuthoredDefinitionBackup.DecodeResult.Decoded.class);
        return ((AuthoredDefinitionBackup.DecodeResult.Decoded) result).backup();
    }

    /** Returns one successfully restored document or fails with its bounded recovery result. */
    private static AuthoredDefinitionDocument restored(AuthoredDefinitionBackup.RestoreResult result) {
        assertThat(result).isInstanceOf(AuthoredDefinitionBackup.RestoreResult.Restored.class);
        return ((AuthoredDefinitionBackup.RestoreResult.Restored) result).document();
    }

    /** Reencodes one backup after replacing an exact serialized fragment. */
    private static AuthoredDefinitionBackup tampered(AuthoredDefinitionBackup backup, String before, String after) {
        return decoded(replace(backup.encode(), before, after));
    }

    /** Replaces one required UTF-8 fragment in encoded test data. */
    private static byte[] replace(byte[] encoded, String before, String after) {
        String source = new String(encoded, StandardCharsets.UTF_8);
        assertThat(source).contains(before);
        return source.replace(before, after).getBytes(StandardCharsets.UTF_8);
    }

    /** Asserts one bounded backup decoding failure. */
    private static void assertDecodeFailure(byte[] encoded, AuthoredDefinitionBackup.DecodeFailure expected) {
        AuthoredDefinitionBackup.DecodeResult result = AuthoredDefinitionBackup.decode(encoded);
        assertThat(result).isInstanceOf(AuthoredDefinitionBackup.DecodeResult.Rejected.class);
        assertThat(((AuthoredDefinitionBackup.DecodeResult.Rejected) result).failure())
                .isEqualTo(expected);
    }

    /** Asserts one stable backup-to-target identity mismatch. */
    private static void assertIdentityMismatch(
            AuthoredDefinitionBackup.RestoreResult result, AuthoredDefinitionBackup.IdentityMismatch expected) {
        assertThat(result).isInstanceOf(AuthoredDefinitionBackup.RestoreResult.IdentityRejected.class);
        assertThat(((AuthoredDefinitionBackup.RestoreResult.IdentityRejected) result).mismatch())
                .isEqualTo(expected);
    }

    private static AuthoredDefinitionDocument accepted(AuthoredDefinitionDocument.CandidateResult result) {
        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.CandidateResult.Accepted.class);
        return ((AuthoredDefinitionDocument.CandidateResult.Accepted) result).document();
    }

    private static AuthoredDefinitionDocument saved(AuthoredDefinitionDocument.SaveResult result) {
        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.SaveResult.Saved.class);
        return ((AuthoredDefinitionDocument.SaveResult.Saved) result).document();
    }

    private static AuthoredDefinitionDocument loaded(AuthoredDefinitionDocument.ReloadResult result) {
        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.ReloadResult.Loaded.class);
        return ((AuthoredDefinitionDocument.ReloadResult.Loaded) result).document();
    }

    private byte[] bytesBeforeMalformed() {
        return WORLD_SOURCE.replace("\"before\"", "\"external\"").getBytes(StandardCharsets.UTF_8);
    }

    private static JsonNode tree(AuthoredDefinitionDocument document) throws IOException {
        return Objects.requireNonNull(
                ProjectJsonReader.strict().readTree(new ByteArrayInputStream(document.serialize())), "tree");
    }

    private static JsonNode properties(AuthoredDefinitionDocument document) throws IOException {
        return properties(tree(document));
    }

    private static JsonNode properties(JsonNode tree) {
        JsonNode root =
                tree.has("root") ? tree.path("root") : tree.path("roots").path(0);
        return root.path("components").path(0).path("properties");
    }

    private static JsonNode property(AuthoredDefinitionDocument document, String property) throws IOException {
        return properties(document).path(property);
    }

    private static JsonNode property(JsonNode tree, String property) {
        return properties(tree).path(property);
    }

    private static List<String> fieldNames(JsonNode node) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
                .toList();
    }

    private static byte[] bytes(Path source) {
        try {
            return Files.readAllBytes(source);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static int indexOf(byte[] content, String text) {
        String source = new String(content, StandardCharsets.UTF_8);
        int index = source.indexOf(text);
        assertThat(index).isGreaterThanOrEqualTo(0);
        return index;
    }

    private static byte[] loadTreeBytes(String source) throws IOException {
        JsonNode tree =
                ProjectJsonReader.strict().readTree(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
        return AuthoredDefinitionDocument.serializeTree(Objects.requireNonNull(tree, "tree"));
    }

    private static RegisteredTypeCatalog registeredTypes() {
        List<PropertyDescriptor> properties = List.of(
                PropertyDescriptor.required(
                        REQUIRED_NUMBER.toString(),
                        ProjectValueKind.NUMBER,
                        DescriptorPresentation.named("Required number"),
                        Map.of("minimum", new ProjectValue.NumberValue(BigDecimal.ZERO)),
                        Set.of()),
                PropertyDescriptor.optionalWithDefault(
                        OPTIONAL_TEXT.toString(),
                        ProjectValueKind.TEXT,
                        new ProjectValue.TextValue("default"),
                        DescriptorPresentation.named("Optional text"),
                        Map.of(),
                        Set.of()),
                PropertyDescriptor.optional(
                        "optional-number",
                        ProjectValueKind.NUMBER,
                        DescriptorPresentation.named("Optional number"),
                        Map.of(),
                        Set.of()),
                PropertyDescriptor.optional(
                        NESTED.toString(),
                        ProjectValueKind.OBJECT,
                        DescriptorPresentation.named("Nested"),
                        Map.of(),
                        Set.of()),
                PropertyDescriptor.optional(
                        "reference",
                        ProjectValueKind.REFERENCE,
                        DescriptorPresentation.named("Reference"),
                        Map.of(),
                        Set.of(ResourceReference.Kind.ASSET)));
        ComponentTypeDescriptor component = ComponentTypeDescriptor.builder(
                        ComponentType.of("example.test/source-preservation", 1),
                        DescriptorPresentation.named("Source preservation"))
                .properties(properties)
                .build();
        ExtensionDescriptor extension = new ExtensionDescriptor(
                "example.test",
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Example"),
                List.of(),
                List.of(component));
        return RegisteredTypeCatalog.of(List.of(extension));
    }
}
