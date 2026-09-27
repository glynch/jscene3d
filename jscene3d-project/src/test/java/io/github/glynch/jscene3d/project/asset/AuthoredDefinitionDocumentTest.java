/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static org.assertj.core.api.Assertions.assertThat;

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

/** Exercises source-preserving authored world and entity documents. */
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
              "$schema": "https://jscene3d.org/schemas/world-definition-1.json",
              "assetId": "4b067393-2ee9-4345-babc-ea1614fb95c8",
              "assetType": "world-definition",
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
        worldSource = projectRoot.resolve("worlds/map01.world.json");
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

        assertThat(world.content()).isInstanceOf(AuthoredDefinitionDocument.Content.World.class);
        assertThat(entity.content()).isInstanceOf(AuthoredDefinitionDocument.Content.Entity.class);
        assertThat(world.source()).isEqualTo(worldSource.toRealPath());
        assertThat(entity.source()).isEqualTo(entitySource.toRealPath());
        assertThat(world.sourceFingerprint()).isEqualTo(SourceFingerprint.sha256(bytes(worldSource)));
        assertThat(entity.sourceFingerprint()).isEqualTo(SourceFingerprint.sha256(bytes(entitySource)));
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
                WORLD_SOURCE.replace("\"world-definition\"", "\"entity-definition\""),
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
        } catch (UnsupportedOperationException exception) {
            Assumptions.abort("symbolic links are not supported");
        } finally {
            Files.deleteIfExists(worldSource);
            Files.deleteIfExists(outside);
        }
    }

    private AuthoredDefinitionDocument load(AssetId id) {
        AuthoredDefinitionDocument.LoadResult result = AuthoredDefinitionDocument.load(assets, assets, types, id);
        assertThat(result.diagnostics()).isEmpty();
        return result.document().orElseThrow();
    }

    private static AuthoredDefinitionDocument accepted(AuthoredDefinitionDocument.CandidateResult result) {
        assertThat(result).isInstanceOf(AuthoredDefinitionDocument.CandidateResult.Accepted.class);
        return ((AuthoredDefinitionDocument.CandidateResult.Accepted) result).document();
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
