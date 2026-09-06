/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises deterministic stable-ID discovery, definition persistence, and graph validation. */
final class AssetCatalogTest {
    private static final AssetId BEACON_ASSET = AssetId.from("4c189475-9845-4810-b7f9-af744d3cc726");
    private static final AssetId GARDEN_ASSET = AssetId.from("8d95e0d5-7cac-42a6-bceb-931437834532");
    private static final AssetId SECOND_ASSET = AssetId.from("6ce65e51-122a-4018-ae90-23a154e35c9a");
    private static final AssetId MISSING_ASSET = AssetId.from("ed20a5a2-f237-43f0-960b-cd93d02b7a8a");
    private static final EntityId BEACON_ROOT = EntityId.from("853f50a0-17dc-46ac-9f04-f772e54c44b2");
    private static final EntityId BEACON_PLACEMENT = EntityId.from("722c16bf-922b-41a5-9499-b23b68d8ab8e");
    private static final EntityId SECOND_PLACEMENT = EntityId.from("4b2fefc2-4d1e-441d-814d-fe94b5b23f09");
    private static final EntityId THIRD_PLACEMENT = EntityId.from("8faf21ab-2b6c-4399-809b-52355ccb55d7");
    private static final ComponentId TRANSFORM = ComponentId.from("b991ca3e-66bb-4ef0-a682-74773bbef0d0");

    @TempDir
    private Path temporaryDirectory;

    /** Resolves a stable placement after its target file moves without editing the world. */
    @Test
    void resolvesStableReferenceAfterDefinitionMoves() throws IOException {
        EntityDefinition beacon = beaconDefinition();
        WorldDefinition garden = gardenWorld("entities/beacon.entity.json");
        Path originalPath = temporaryDirectory.resolve("entities/beacon.entity.json");
        Path movedPath = temporaryDirectory.resolve("archive/beacon.entity.json");
        DefinitionWriter.write(originalPath, beacon);
        DefinitionWriter.write(temporaryDirectory.resolve("worlds/garden.world.json"), garden);

        AssetCatalog initial = scanValidCatalog();
        assertThat(initial.assets()).extracting(AssetMetadata::id).containsExactly(BEACON_ASSET, GARDEN_ASSET);
        assertThat(initial.loadWorld(AssetRef.to(GARDEN_ASSET)).definition()).contains(garden);

        Files.createDirectories(movedPath.getParent());
        Files.move(originalPath, movedPath);
        AssetCatalog moved = scanValidCatalog();

        assertThat(moved.find(BEACON_ASSET))
                .get()
                .extracting(AssetMetadata::path)
                .isEqualTo(movedPath.toRealPath());
        assertThat(moved.loadWorld(AssetRef.to(GARDEN_ASSET)).definition()).contains(garden);
    }

    /** Produces byte-stable JSON that loads back into equal immutable definitions. */
    @Test
    void writesDeterministicRoundTrips() throws IOException {
        EntityDefinition definition = beaconDefinition();
        Path target = temporaryDirectory.resolve("beacon.entity.json");
        DefinitionWriter.write(target, definition);
        String first = Files.readString(target, StandardCharsets.UTF_8);

        DefinitionWriter.write(target, definition);
        String second = Files.readString(target, StandardCharsets.UTF_8);
        AssetCatalog catalog = scanValidCatalog();

        assertThat(second).isEqualTo(first).endsWith("\n");
        assertThat(first)
                .containsSubsequence(
                        "\"$schema\"",
                        "\"assetId\"",
                        "\"assetType\"",
                        "\"formatVersion\"",
                        "\"name\"",
                        "\"contract\"",
                        "\"connections\"",
                        "\"root\"")
                .contains(
                        "\"nothing\" : null",
                        "\"enabled\" : true",
                        "\"parameters\"",
                        "\"signals\"",
                        "\"actions\"",
                        "\"capabilities\"",
                        "\"attachments\"",
                        "\"resourceBindings\"");
        assertThat(catalog.loadEntity(AssetRef.to(BEACON_ASSET)).definition()).contains(definition);
    }

    /** Writes generated definitions without taking ownership of the importing subsystem's stream. */
    @Test
    void writesGeneratedDefinitionsToCallerOwnedStreams() throws IOException {
        ByteArrayOutputStream entityOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream worldOutput = new ByteArrayOutputStream();

        DefinitionWriter.write(entityOutput, beaconDefinition());
        DefinitionWriter.write(worldOutput, gardenWorld("entities/beacon.entity.json"));
        entityOutput.write('!');
        worldOutput.write('!');

        assertThat(entityOutput.toString(StandardCharsets.UTF_8))
                .contains("\"assetType\" : \"entity-definition\"")
                .endsWith("\n!");
        assertThat(worldOutput.toString(StandardCharsets.UTF_8))
                .contains("\"assetType\" : \"world-definition\"")
                .endsWith("\n!");
    }

    /** Resolves a generated definition placed by an authored world through one mixed-source graph. */
    @Test
    void resolvesGeneratedDefinitionFromAuthoredWorld() throws IOException {
        WorldDefinition world = new WorldDefinition(
                GARDEN_ASSET,
                "Generated world",
                List.of(new EntityPlacement(BEACON_PLACEMENT, true, AssetRef.to(BEACON_ASSET), Map.of())));
        EntityDefinition generatedDefinition = new EntityDefinition(
                BEACON_ASSET,
                "Generated entity",
                new LocalEntity(BEACON_ROOT, "Generated root", true, List.of(), List.of()));
        DefinitionWriter.write(temporaryDirectory.resolve("garden.world.json"), world);
        AssetCatalog authored = scanValidCatalog();
        ByteArrayOutputStream generated = new ByteArrayOutputStream();
        DefinitionWriter.write(generated, generatedDefinition);
        DefinitionResolver definitions = DefinitionResolvers.builder(authored)
                .addGeneratedEntity(
                        BEACON_ASSET,
                        URI.create("import:beacons/definitions/main"),
                        new ByteArrayInputStream(generated.toByteArray()))
                .build();

        DefinitionLoadResult<WorldDefinition> result =
                definitions.loadWorld(AssetRef.to(GARDEN_ASSET), RegisteredTypeCatalog.of(List.of()));

        assertThat(result.definition()).contains(world);
        assertThat(result.diagnostics()).isEmpty();
    }

    /** Applies envelope validation to generated content and reports its logical import source. */
    @Test
    void rejectsGeneratedDefinitionWithMismatchedIdentity() throws IOException {
        AssetCatalog authored = scanValidCatalog();
        ByteArrayOutputStream generated = new ByteArrayOutputStream();
        DefinitionWriter.write(generated, beaconDefinition());
        byte[] mismatched = generated
                .toString(StandardCharsets.UTF_8)
                .replace(BEACON_ASSET.toString(), SECOND_ASSET.toString())
                .getBytes(StandardCharsets.UTF_8);
        URI source = URI.create("import:beacons/definitions/main");
        DefinitionResolver definitions = DefinitionResolvers.builder(authored)
                .addGeneratedEntity(BEACON_ASSET, source, new ByteArrayInputStream(mismatched))
                .build();

        DefinitionLoadResult<EntityDefinition> result =
                definitions.loadEntity(AssetRef.to(BEACON_ASSET), RegisteredTypeCatalog.of(List.of()));

        assertThat(result.definition()).isEmpty();
        assertThat(result.source()).isEqualTo(source);
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.source()).isEqualTo(source);
            assertThat(diagnostic.code().code()).isEqualTo("asset.catalog.stale");
        });
    }

    /** Rejects duplicate identities across otherwise independently valid asset files. */
    @Test
    void rejectsDuplicateAssetIds() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("first.entity.json"), beaconDefinition());
        DefinitionWriter.write(temporaryDirectory.resolve("nested/second.entity.json"), beaconDefinition());

        AssetCatalogLoadResult result = AssetCatalog.scan(temporaryDirectory);

        assertThat(result.isValid()).isFalse();
        assertThat(result.catalog()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("asset.id.duplicate");
    }

    /** Reports unsupported envelopes and filename-kind conflicts during header scanning. */
    @Test
    void rejectsInvalidCatalogHeaders() throws IOException {
        write("unsupported.entity.json", """
                {"assetId":"4c189475-9845-4810-b7f9-af744d3cc726",
                 "assetType":"entity-definition","formatVersion":2}
                """);
        write("wrong.entity.json", """
                {"assetId":"6ce65e51-122a-4018-ae90-23a154e35c9a",
                 "assetType":"world-definition","formatVersion":1}
                """);
        write("broken.world.json", "{");
        write("null.world.json", "null");

        AssetCatalogLoadResult result = AssetCatalog.scan(temporaryDirectory);

        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("asset.json", "asset.json", "asset.format.unsupported", "asset.kind.suffix");
    }

    /** Keeps unrelated JSON out of the catalog and diagnoses invalid project roots. */
    @Test
    void scansOnlyDefinitionFilesBelowAValidRoot() throws IOException {
        write("notes.json", "not JSON because this file is unrelated");
        DefinitionWriter.write(temporaryDirectory.resolve("beacon.entity.json"), beaconDefinition());

        AssetCatalog catalog = scanValidCatalog();
        AssetCatalogLoadResult missing = AssetCatalog.scan(temporaryDirectory.resolve("missing"));

        assertThat(catalog.assets())
                .singleElement()
                .extracting(AssetMetadata::kind)
                .isEqualTo(AssetKind.ENTITY_DEFINITION);
        assertThat(AssetKind.fromSerializedName("entity-definition")).contains(AssetKind.ENTITY_DEFINITION);
        assertThat(AssetKind.fromSerializedName("unknown")).isEmpty();
        assertThat(missing.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo("asset.root");
    }

    /** Diagnoses unresolved and wrong-kind typed references without consulting their path hints. */
    @Test
    void rejectsMissingAndWrongKindReferences() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("garden.world.json"), gardenWorld("ignored.entity.json"));
        AssetCatalog catalog = scanValidCatalog();

        DefinitionLoadResult<EntityDefinition> wrongKind =
                catalog.loadEntity(AssetRef.to(GARDEN_ASSET, "somewhere/else.entity.json"));
        DefinitionLoadResult<EntityDefinition> missing = catalog.loadEntity(AssetRef.to(MISSING_ASSET));
        DefinitionLoadResult<WorldDefinition> invalidWorld = catalog.loadWorld(AssetRef.to(GARDEN_ASSET));

        assertThat(wrongKind.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo("asset.reference.kind");
        assertThat(missing.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo("asset.reference.missing");
        assertThat(invalidWorld.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo("asset.reference.missing");
    }

    /** Detects reusable-definition inclusion cycles by stable identity. */
    @Test
    void detectsDefinitionCycles() throws IOException {
        DefinitionWriter.write(
                temporaryDirectory.resolve("first.entity.json"),
                definitionPlacing(BEACON_ASSET, SECOND_ASSET, "Second"));
        DefinitionWriter.write(
                temporaryDirectory.resolve("second.entity.json"),
                definitionPlacing(SECOND_ASSET, BEACON_ASSET, "First"));
        AssetCatalog catalog = scanValidCatalog();

        DefinitionLoadResult<EntityDefinition> result = catalog.loadEntity(AssetRef.to(BEACON_ASSET));

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code().code()).isEqualTo("asset.definition.cycle");
            assertThat(diagnostic.details().get("technicalDetail"))
                    .contains(BEACON_ASSET.toString(), SECOND_ASSET.toString());
        });
    }

    /** Validates every placement argument against the referenced definition's exported contract. */
    @Test
    void rejectsInvalidPlacementArguments() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("projectile.entity.json"), requiredSpeedDefinition());
        List<EntityPlacement> placements = List.of(
                new EntityPlacement(BEACON_PLACEMENT, true, AssetRef.to(BEACON_ASSET), Map.of()),
                new EntityPlacement(
                        SECOND_PLACEMENT,
                        true,
                        AssetRef.to(BEACON_ASSET),
                        Map.of(property("unknown"), new ProjectValue.NumberValue(BigDecimal.ONE))),
                new EntityPlacement(
                        THIRD_PLACEMENT,
                        true,
                        AssetRef.to(BEACON_ASSET),
                        Map.of(property("speed"), new ProjectValue.TextValue("fast"))));
        DefinitionWriter.write(
                temporaryDirectory.resolve("garden.world.json"),
                new WorldDefinition(GARDEN_ASSET, "Garden", placements));
        AssetCatalog catalog = scanValidCatalog();

        DefinitionLoadResult<WorldDefinition> result = catalog.loadWorld(AssetRef.to(GARDEN_ASSET));

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains(
                        "asset.contract.argument.required",
                        "asset.contract.argument.unknown",
                        "asset.contract.argument.type");
    }

    /** Allows a containing definition contract to supply a required nested placement argument. */
    @Test
    void acceptsRequiredArgumentReexportedByContainingDefinition() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("projectile.entity.json"), requiredSpeedDefinition());
        EntityPlacement nested = new EntityPlacement(BEACON_PLACEMENT, true, AssetRef.to(BEACON_ASSET), Map.of());
        PropertyId speed = property("speed");
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        speed,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.REQUIRED,
                        PropertyTarget.placement(BEACON_PLACEMENT, speed))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        EntityDefinition launcher = new EntityDefinition(
                SECOND_ASSET,
                "Launcher",
                contract,
                List.of(),
                new LocalEntity(BEACON_ROOT, true, List.of(), List.of(nested)));
        DefinitionWriter.write(temporaryDirectory.resolve("launcher.entity.json"), launcher);
        AssetCatalog catalog = scanValidCatalog();

        assertThat(catalog.loadEntity(AssetRef.to(SECOND_ASSET)).definition()).contains(launcher);
    }

    /** Requires placed connection endpoints to exist and declare compatible payloads. */
    @Test
    void rejectsInvalidPlacedEndpointConnections() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("source.entity.json"), endpointDefinition());
        EntityPlacement placement = new EntityPlacement(BEACON_PLACEMENT, true, AssetRef.to(BEACON_ASSET), Map.of());
        EndpointTarget emitted = EndpointTarget.placement(BEACON_PLACEMENT, new EndpointId("emitted"));
        List<SignalConnection> connections = List.of(
                new SignalConnection(emitted, EndpointTarget.placement(BEACON_PLACEMENT, new EndpointId("accept"))),
                new SignalConnection(emitted, EndpointTarget.placement(BEACON_PLACEMENT, new EndpointId("missing"))));
        DefinitionWriter.write(
                temporaryDirectory.resolve("garden.world.json"),
                new WorldDefinition(GARDEN_ASSET, "Garden", connections, List.of(placement)));
        AssetCatalog catalog = scanValidCatalog();

        DefinitionLoadResult<WorldDefinition> result = catalog.loadWorld(AssetRef.to(GARDEN_ASSET));

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("asset.contract.payload", "asset.contract.member.missing");
    }

    /** Collects structural identity and entry diagnostics from a complete definition read. */
    @Test
    void rejectsInvalidDefinitionStructure() throws IOException {
        write("invalid.entity.json", """
                {
                  "assetId":"4c189475-9845-4810-b7f9-af744d3cc726",
                  "assetType":"entity-definition",
                  "formatVersion":1,
                  "name":"Invalid",
                  "root":{
                    "entryType":"local",
                    "entityId":"853f50a0-17dc-46ac-9f04-f772e54c44b2",
                    "components":[
                      {"componentId":"b991ca3e-66bb-4ef0-a682-74773bbef0d0",
                       "type":"invalid","typeVersion":0},
                      {"componentId":"b991ca3e-66bb-4ef0-a682-74773bbef0d0",
                       "type":"io.github.glynch.jscene3d/transform-3d","typeVersion":1}
                    ],
                    "children":[
                      {"entryType":"placement",
                       "entityId":"853f50a0-17dc-46ac-9f04-f772e54c44b2",
                       "definition":{"assetId":"not-a-uuid","pathHint":"../outside"}}
                    ]
                  }
                }
                """);
        AssetCatalog catalog = scanValidCatalog();

        DefinitionLoadResult<EntityDefinition> result = catalog.loadEntity(AssetRef.to(BEACON_ASSET));

        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains(
                        "asset.component.type",
                        "asset.component.version",
                        "asset.component.id.duplicate",
                        "asset.entity.id.duplicate",
                        "asset.id",
                        "asset.reference.hint");
    }

    /** Collects malformed public-contract diagnostics without aborting at the first declaration. */
    @Test
    void rejectsMalformedContractDeclarations() throws IOException {
        write("invalid-contract.entity.json", """
                {
                  "assetId":"4c189475-9845-4810-b7f9-af744d3cc726",
                  "assetType":"entity-definition",
                  "formatVersion":1,
                  "name":"Invalid contract",
                  "contract":{
                    "parameters":[null,{"id":"not valid","valueKind":"unknown","target":null}],
                    "signals":[null,{"id":"bad/id","payload":{"id":"bad","version":0},"target":null}],
                    "actions":[null],
                    "capabilities":[null,"bad"],
                    "attachments":[null,{"id":"bad id","target":null}],
                    "resourceBindings":[null,{"id":"bad id","acceptedKinds":["import","IMPORT","unknown",null],"target":null}]
                  },
                  "connections":[null,{"signal":null,"action":null}],
                  "root":{
                    "entryType":"local",
                    "entityId":"853f50a0-17dc-46ac-9f04-f772e54c44b2",
                    "components":[],
                    "children":[]
                  }
                }
                """);
        AssetCatalog catalog = scanValidCatalog();

        DefinitionLoadResult<EntityDefinition> result = catalog.loadEntity(AssetRef.to(BEACON_ASSET));

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains(
                        "asset.field.required",
                        "asset.property.id",
                        "asset.endpoint.id",
                        "asset.attachment.id",
                        "asset.capability.id",
                        "asset.contract.argument.type",
                        "asset.value.reference",
                        "asset.component.type",
                        "asset.component.version");
    }

    /** Rejects strict JSON unknown fields and detects files changed after catalog scanning. */
    @Test
    void rejectsUnknownAndChangedDefinitionHeaders() throws IOException {
        Path target = temporaryDirectory.resolve("beacon.entity.json");
        DefinitionWriter.write(target, beaconDefinition());
        AssetCatalog catalog = scanValidCatalog();
        String valid = Files.readString(target, StandardCharsets.UTF_8);
        Files.writeString(target, valid.replace("\"name\"", "\"mystery\" : true,\n  \"name\""));

        assertThat(catalog.loadEntity(AssetRef.to(BEACON_ASSET)).diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo("asset.json");

        DefinitionWriter.write(
                target,
                new EntityDefinition(SECOND_ASSET, "Changed", beaconDefinition().root()));
        assertThat(catalog.loadEntity(AssetRef.to(BEACON_ASSET)).diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("asset.catalog.stale");
    }

    /** Publishes both version-one schemas for editors and external validation tools. */
    @Test
    void bundlesDefinitionSchemas() throws IOException {
        assertSchema("entity-definition-1.schema.json", "Entity Definition", "entity-definition");
        assertSchema("world-definition-1.schema.json", "World Definition", "world-definition");
    }

    /** Creates one entity definition containing representative portable property values. */
    private static EntityDefinition beaconDefinition() {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(property("nothing"), ProjectValue.NullValue.INSTANCE);
        properties.put(property("visible"), new ProjectValue.BooleanValue(true));
        properties.put(property("intensity"), new ProjectValue.NumberValue(new BigDecimal("2.50")));
        properties.put(property("label"), new ProjectValue.TextValue("Beacon"));
        properties.put(
                property("position"),
                new ProjectValue.ArrayValue(List.of(
                        new ProjectValue.NumberValue(BigDecimal.ZERO), new ProjectValue.NumberValue(BigDecimal.ONE))));
        properties.put(
                property("metadata"),
                new ProjectValue.ObjectValue(Map.of("mode", new ProjectValue.TextValue("pulse"))));
        properties.put(
                property("mesh"), new ProjectValue.ReferenceValue(ResourceReference.imported("garden/mesh/beacon")));
        ComponentDefinition transform = new ComponentDefinition(
                TRANSFORM, new ComponentTypeId("io.github.glynch.jscene3d/transform-3d"), 1, properties);
        EndpointId activated = new EndpointId("activated");
        EndpointId deactivate = new EndpointId("deactivate");
        RegisteredType pulse = new RegisteredType("io.github.glynch.beacon/pulse", 1);
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        property("color"),
                        ProjectValueKind.TEXT,
                        EntityContract.Requirement.OPTIONAL,
                        PropertyTarget.component(BEACON_ROOT, TRANSFORM, property("color")))),
                List.of(new EntityContract.Signal(
                        activated, pulse, EndpointTarget.component(BEACON_ROOT, TRANSFORM, activated))),
                List.of(new EntityContract.Action(
                        deactivate, pulse, EndpointTarget.component(BEACON_ROOT, TRANSFORM, deactivate))),
                List.of(new CapabilityId("io.github.glynch.beacon/illuminates")),
                List.of(new EntityContract.Attachment(
                        new AttachmentPointId("light-origin"), SpatialTarget.component(BEACON_ROOT, TRANSFORM))),
                List.of(new EntityContract.ResourceBinding(
                        property("mesh"),
                        EntityContract.Requirement.OPTIONAL,
                        Set.of(ResourceReference.Kind.IMPORT),
                        PropertyTarget.component(BEACON_ROOT, TRANSFORM, property("mesh")))));
        return new EntityDefinition(
                BEACON_ASSET,
                "Beacon",
                contract,
                List.of(new SignalConnection(
                        EndpointTarget.component(BEACON_ROOT, TRANSFORM, activated),
                        EndpointTarget.component(BEACON_ROOT, TRANSFORM, deactivate))),
                new LocalEntity(BEACON_ROOT, "Beacon", true, List.of(transform), List.of()));
    }

    /** Creates one world placing the reusable beacon through a deliberately replaceable path hint. */
    private static WorldDefinition gardenWorld(String pathHint) {
        EntityPlacement placement = new EntityPlacement(
                BEACON_PLACEMENT,
                "Beacon A",
                true,
                AssetRef.to(BEACON_ASSET, pathHint),
                Map.of(property("color"), new ProjectValue.TextValue("green")));
        return new WorldDefinition(GARDEN_ASSET, "Garden", List.of(placement));
    }

    /** Creates one definition whose local root places another definition. */
    private static EntityDefinition definitionPlacing(AssetId id, AssetId target, String name) {
        EntityPlacement placement = new EntityPlacement(BEACON_PLACEMENT, true, AssetRef.to(target), Map.of());
        LocalEntity root = new LocalEntity(BEACON_ROOT, name, true, List.of(), List.of(placement));
        return new EntityDefinition(id, name, root);
    }

    /** Creates a definition requiring one numeric placement argument. */
    private static EntityDefinition requiredSpeedDefinition() {
        PropertyId speed = property("speed");
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        speed,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.REQUIRED,
                        PropertyTarget.component(BEACON_ROOT, TRANSFORM, speed))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        ComponentDefinition component = new ComponentDefinition(
                TRANSFORM, new ComponentTypeId("io.github.glynch.jscene3d/transform-3d"), 1, Map.of());
        return new EntityDefinition(
                BEACON_ASSET,
                "Projectile",
                contract,
                List.of(),
                new LocalEntity(BEACON_ROOT, true, List.of(component), List.of()));
    }

    /** Creates a definition with deliberately incompatible signal and action payloads. */
    private static EntityDefinition endpointDefinition() {
        EndpointId emitted = new EndpointId("emitted");
        EndpointId accept = new EndpointId("accept");
        EntityContract contract = new EntityContract(
                List.of(),
                List.of(new EntityContract.Signal(
                        emitted,
                        new RegisteredType("io.github.glynch.game/emitted", 1),
                        EndpointTarget.component(BEACON_ROOT, TRANSFORM, emitted))),
                List.of(new EntityContract.Action(
                        accept,
                        new RegisteredType("io.github.glynch.game/accepted", 1),
                        EndpointTarget.component(BEACON_ROOT, TRANSFORM, accept))),
                List.of(),
                List.of(),
                List.of());
        ComponentDefinition component = new ComponentDefinition(
                TRANSFORM, new ComponentTypeId("io.github.glynch.jscene3d/transform-3d"), 1, Map.of());
        return new EntityDefinition(
                BEACON_ASSET,
                "Source",
                contract,
                List.of(),
                new LocalEntity(BEACON_ROOT, true, List.of(component), List.of()));
    }

    /** Creates one stable local property identity. */
    private static PropertyId property(String value) {
        return new PropertyId(value);
    }

    /** Scans the temporary project and requires success. */
    private AssetCatalog scanValidCatalog() {
        AssetCatalogLoadResult result = AssetCatalog.scan(temporaryDirectory);
        assertThat(result.diagnostics()).isEmpty();
        return result.catalog().orElseThrow();
    }

    /** Writes one raw test file relative to the project root. */
    private void write(String relativePath, String content) throws IOException {
        Path target = temporaryDirectory.resolve(relativePath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    /** Verifies one bundled schema's stable identity and kind. */
    private void assertSchema(String name, String title, String kind) throws IOException {
        String resource = "/META-INF/jscene3d/project/" + name;
        try (var input = getClass().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(schema).contains(title, "\"const\": \"" + kind + "\"");
        }
    }
}
