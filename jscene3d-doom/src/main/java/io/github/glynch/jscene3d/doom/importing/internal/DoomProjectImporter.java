/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnostic;
import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnosticCode;
import io.github.glynch.jscene3d.doom.geometry.DoomBlockingLineGeometryBuilder;
import io.github.glynch.jscene3d.doom.geometry.DoomGeometryBuildResult;
import io.github.glynch.jscene3d.doom.geometry.DoomGeometryDiagnostic;
import io.github.glynch.jscene3d.doom.geometry.DoomMeshData;
import io.github.glynch.jscene3d.doom.geometry.DoomStaticGeometry;
import io.github.glynch.jscene3d.doom.geometry.DoomStaticGeometryBuilder;
import io.github.glynch.jscene3d.doom.geometry.DoomSurface;
import io.github.glynch.jscene3d.doom.geometry.DoomUnits;
import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.doom.map.DoomMapDecodeResult;
import io.github.glynch.jscene3d.doom.map.DoomMapDecoder;
import io.github.glynch.jscene3d.doom.material.DoomMapMaterials;
import io.github.glynch.jscene3d.doom.material.DoomMaterial;
import io.github.glynch.jscene3d.doom.material.DoomMaterialDiagnostic;
import io.github.glynch.jscene3d.doom.material.DoomMaterialImportResult;
import io.github.glynch.jscene3d.doom.material.DoomMaterialImporter;
import io.github.glynch.jscene3d.doom.material.RgbaImage;
import io.github.glynch.jscene3d.doom.runtime.DoomCollisionCategories;
import io.github.glynch.jscene3d.doom.runtime.DoomDoorDescriptors;
import io.github.glynch.jscene3d.doom.runtime.DoomFloorDescriptors;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.importing.extension.ImportInspectionContext;
import io.github.glynch.jscene3d.project.importing.extension.ImportPreparationContext;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;
import io.github.glynch.jscene3d.project.physics3d.Physics3dDescriptors;
import io.github.glynch.jscene3d.project.physics3d.Physics3dResourceWriter;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.spatial3d.resource.Spatial3dResourceWriter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import io.github.glynch.jscene3d.wad.WadArchive;
import io.github.glynch.jscene3d.wad.WadDiagnostic;
import io.github.glynch.jscene3d.wad.WadLoadResult;
import io.github.glynch.jscene3d.wad.WadLoader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Publishes selected Doom maps as generated project-native entity definitions and spatial resources. */
final class DoomProjectImporter implements ProjectImporter {
    private static final float MINIMUM_COLLISION_NORMAL_LENGTH_SQUARED = 1.0E-12F;
    private static final String ITEM_KIND = "io.github.glynch.jscene3d.doom/map-presentation";
    private static final String MESH_MEDIA_TYPE = "application/vnd.jscene3d.mesh-v1";
    private static final String TEXTURE_MEDIA_TYPE = "application/vnd.jscene3d.rgba8-v1";
    private static final String COLLISION_MEDIA_TYPE = "application/vnd.jscene3d.triangle-mesh-collision-v1";
    private static final float TRIGGER_DEPTH = DoomUnits.toWorld(2.0F);

    @Override
    public void inspect(ImportInspectionContext context) {
        loadAndDescribe(context);
    }

    @Override
    public void prepare(ImportPreparationContext context) throws IOException {
        Optional<WadArchive> loaded = loadAndDescribe(context);
        if (loaded.isEmpty()) {
            return;
        }
        WadArchive archive = loaded.orElseThrow();
        Set<String> selection = Set.copyOf(context.definition().selection());
        for (String mapName : new DoomMapDecoder().discover(archive)) {
            String identity = mapIdentity(mapName);
            if (selection.contains(identity)) {
                prepareMap(context, archive, mapName, identity);
            }
        }
    }

    /** Loads the source and declares one selectable generated presentation per classic map. */
    private static Optional<WadArchive> loadAndDescribe(ImportInspectionContext context) {
        context.checkCancelled();
        WadLoadResult result = context.asset().sha256().isPresent()
                ? WadLoader.load(
                        context.asset().path(), context.asset().sha256().orElseThrow())
                : WadLoader.load(context.asset().path());
        if (!result.isValid()) {
            result.diagnostics().forEach(diagnostic -> report(context, diagnostic));
            return Optional.empty();
        }
        WadArchive archive = result.archive().orElseThrow();
        for (String mapName : new DoomMapDecoder().discover(archive)) {
            context.sourceItem(new SourceItem(
                    mapIdentity(mapName),
                    ITEM_KIND,
                    mapName,
                    true,
                    Map.of("map", new ProjectValue.TextValue(mapName)),
                    List.of()));
        }
        return Optional.of(archive);
    }

    /** Decodes and publishes one selected map as a closed graph of generic project artifacts. */
    private static void prepareMap(ImportPreparationContext context, WadArchive archive, String mapName, String prefix)
            throws IOException {
        Optional<DoomMap> decodedMap = decodeMap(context, archive, mapName);
        if (decodedMap.isEmpty()) {
            return;
        }
        DoomMap map = decodedMap.orElseThrow();
        Optional<DoomMapMaterials> importedMaterials = importMaterials(context, archive, map);
        if (importedMaterials.isEmpty()) {
            return;
        }
        DoomMapMaterials materials = importedMaterials.orElseThrow();
        Optional<DoomStaticGeometry> builtGeometry = buildGeometry(context, map, materials);
        if (builtGeometry.isEmpty()) {
            return;
        }
        DoomStaticGeometry geometry = builtGeometry.orElseThrow();
        publishMap(context, prefix, map, geometry, materials);
    }

    /** Requires one valid decoded map after forwarding source diagnostics. */
    private static Optional<DoomMap> decodeMap(ImportPreparationContext context, WadArchive archive, String mapName) {
        DoomMapDecodeResult result = new DoomMapDecoder().decode(archive, mapName);
        result.diagnostics().forEach(diagnostic -> report(context, diagnostic));
        if (!result.isValid()) {
            return Optional.empty();
        }
        return result.map();
    }

    /** Requires the complete material set referenced by one map. */
    private static Optional<DoomMapMaterials> importMaterials(
            ImportPreparationContext context, WadArchive archive, DoomMap map) {
        DoomMaterialImportResult result = new DoomMaterialImporter().importMap(archive, map);
        if (!result.isValid()) {
            result.diagnostics().forEach(diagnostic -> report(context, diagnostic));
            return Optional.empty();
        }
        return result.materials();
    }

    /** Requires complete renderer-independent surface geometry and forwards every warning. */
    private static Optional<DoomStaticGeometry> buildGeometry(
            ImportPreparationContext context, DoomMap map, DoomMapMaterials materials) {
        DoomGeometryBuildResult result = new DoomStaticGeometryBuilder().build(map, materials);
        result.diagnostics().forEach(diagnostic -> report(context, diagnostic));
        return result.geometry();
    }

    /** Publishes shared textures and materials, material-batched meshes, then their generated definition. */
    private static void publishMap(
            ImportPreparationContext context,
            String prefix,
            DoomMap map,
            DoomStaticGeometry geometry,
            DoomMapMaterials sourceMaterials)
            throws IOException {
        List<DoomDoorPublication> doors = DoomDoorPublication.discover(map);
        Set<Integer> doorSectors =
                doors.stream().map(DoomDoorPublication::sectorIndex).collect(Collectors.toUnmodifiableSet());
        List<DoomFloorPublication> floors = DoomFloorPublication.discover(map);
        Set<Integer> floorSectors =
                floors.stream().map(DoomFloorPublication::sectorIndex).collect(Collectors.toUnmodifiableSet());
        List<RenderBatch> batches = renderBatches(geometry, doorSectors, floorSectors);
        List<DoorRenderBatches> doorBatches = doorRenderBatches(geometry, doors);
        List<FloorRenderBatches> floorBatches = floorRenderBatches(geometry, floors);
        Map<MaterialKey, DoomMaterial> materials =
                selectedMaterials(batches, doorBatches, floorBatches, sourceMaterials);
        for (Map.Entry<MaterialKey, DoomMaterial> entry : materials.entrySet()) {
            publishMaterial(context, prefix, entry.getKey(), entry.getValue());
        }

        List<String> references = new ArrayList<>();
        List<ComponentDefinition> components = new ArrayList<>();
        components.add(component(
                context.definition().id(), prefix + "/root/transform", Spatial3dDescriptors.transformType(), Map.of()));
        publishCollision(
                context, collisionIdentity(prefix), staticCollisionMesh(map, geometry, doorSectors, floorSectors));
        String collisionIdentity = collisionIdentity(prefix);
        references.add(collisionIdentity);
        ComponentId collisionShape = componentId(context.definition().id(), prefix + "/root/collision/static-shape");
        components.add(collisionShape(context.definition().id(), prefix, collisionShape));
        components.add(staticBody(context.definition().id(), prefix, collisionShape));
        for (int index = 0; index < batches.size(); index++) {
            RenderBatch batch = batches.get(index);
            String meshIdentity = meshIdentity(prefix, index);
            publishMesh(context, meshIdentity, batch.mesh());
            references.add(meshIdentity);
            components.add(meshRenderer(context.definition().id(), prefix, index, batch.material()));
        }
        List<EntityEntry> children = new ArrayList<>();
        List<SignalConnection> connections = new ArrayList<>();
        for (DoorRenderBatches door : doorBatches) {
            children.add(publishDoor(context, prefix, door, references, connections));
        }
        for (FloorRenderBatches floor : floorBatches) {
            children.add(publishFloor(context, prefix, floor, references));
        }
        publishFloorTriggers(context, prefix, map, floors, children, references, connections);
        materials.keySet().stream().map(key -> materialIdentity(prefix, key)).forEach(references::add);

        String definitionIdentity = prefix + "/definition";
        AssetId definitionId = assetId(context.definition().id(), definitionIdentity);
        LocalEntity root = new LocalEntity(
                entityId(context.definition().id(), prefix + "/root"),
                prefix.substring(prefix.lastIndexOf('/') + 1),
                true,
                components,
                children);
        EntityDefinition definition = new EntityDefinition(
                definitionId, root.name().orElseThrow(), EntityContract.empty(), connections, root);
        context.artifact(
                ImportArtifactDescriptor.entityDefinition(definitionIdentity, definitionId, references),
                output -> DefinitionWriter.write(output, definition));
    }

    /** Publishes the static map boundary as collision-specific data distinct from every rendered mesh. */
    private static void publishCollision(
            ImportPreparationContext context, String resourceIdentity, DoomMeshData collision) throws IOException {
        String payloadIdentity = resourceIdentity.replace("/resources/", "/payloads/") + ".mesh";
        context.artifact(
                ImportArtifactDescriptor.payload(payloadIdentity, COLLISION_MEDIA_TYPE),
                output -> Physics3dResourceWriter.writeTriangleMeshPayload(
                        output, collision.positions(), collision.indices()));
        ResourceReference payload = imported(context, payloadIdentity);
        context.artifact(
                ImportArtifactDescriptor.resource(
                        resourceIdentity, Physics3dDescriptors.triangleMeshResourceType(), List.of(payloadIdentity)),
                output -> Physics3dResourceWriter.writeTriangleMeshDefinition(output, payload));
    }

    /** Creates one shape component retaining the generated static collision resource. */
    private static ComponentDefinition collisionShape(String importId, String prefix, ComponentId shapeId) {
        return new ComponentDefinition(
                shapeId,
                Physics3dDescriptors.collisionShapeType().id(),
                Physics3dDescriptors.collisionShapeType().version(),
                Map.of(Physics3dDescriptors.shapeProperty(), reference(importId, collisionIdentity(prefix))));
    }

    /** Publishes one movable door's presentation, collision, and descriptor-selected behavior. */
    private static LocalEntity publishDoor(
            ImportPreparationContext context,
            String prefix,
            DoorRenderBatches door,
            List<String> references,
            List<SignalConnection> connections)
            throws IOException {
        DoomDoorPublication behavior = door.behavior();
        String locator = doorLocator(prefix, behavior.sectorIndex());
        EntityId entityId = entityId(context.definition().id(), locator);
        ComponentId transformId = componentId(context.definition().id(), locator + "/transform");
        List<ComponentDefinition> components = new ArrayList<>();
        components.add(new ComponentDefinition(
                transformId,
                Spatial3dDescriptors.transformType().id(),
                Spatial3dDescriptors.transformType().version(),
                Map.of(
                        Spatial3dDescriptors.positionProperty(),
                        new ProjectValue.ArrayValue(
                                List.of(number(0.0F), number(behavior.closedHeight()), number(0.0F))))));

        String collision = doorCollisionIdentity(prefix, behavior.sectorIndex());
        publishCollision(context, collision, door.collision());
        references.add(collision);
        ComponentId shapeId = componentId(context.definition().id(), locator + "/collision/shape");
        components.add(collisionShapeForResource(context.definition().id(), collision, shapeId));
        components.add(staticBody(context.definition().id(), locator, entityId, shapeId));

        for (int index = 0; index < door.batches().size(); index++) {
            RenderBatch batch = door.batches().get(index);
            String mesh = doorMeshIdentity(prefix, behavior.sectorIndex(), index);
            publishMesh(context, mesh, batch.mesh());
            references.add(mesh);
            components.add(meshRenderer(context.definition().id(), locator, mesh, index, prefix, batch.material()));
        }
        ComponentId behaviorId = componentId(context.definition().id(), locator + "/behavior");
        components.add(doorBehavior(entityId, transformId, behaviorId, behavior));
        String obstructionShape = doorObstructionShapeIdentity(prefix, behavior.sectorIndex());
        Bounds3d obstructionBounds = Bounds3d.of(door.collision());
        publishBoxCollision(context, obstructionShape, obstructionBounds);
        references.add(obstructionShape);
        LocalEntity obstruction = doorObstructionSensor(
                context.definition().id(),
                locator,
                obstructionShape,
                obstructionBounds,
                entityId,
                behaviorId,
                connections);
        return new LocalEntity(entityId, "Door " + behavior.sectorIndex(), true, components, List.of(obstruction));
    }

    /** Publishes one independently movable floor's presentation, collision, and source-derived behavior. */
    private static LocalEntity publishFloor(
            ImportPreparationContext context, String prefix, FloorRenderBatches floor, List<String> references)
            throws IOException {
        DoomFloorPublication behavior = floor.behavior();
        String locator = floorLocator(prefix, behavior.sectorIndex());
        EntityId entityId = entityId(context.definition().id(), locator);
        ComponentId transformId = componentId(context.definition().id(), locator + "/transform");
        List<ComponentDefinition> components = new ArrayList<>();
        components.add(new ComponentDefinition(
                transformId,
                Spatial3dDescriptors.transformType().id(),
                Spatial3dDescriptors.transformType().version(),
                Map.of(
                        Spatial3dDescriptors.positionProperty(),
                        new ProjectValue.ArrayValue(
                                List.of(number(0.0F), number(behavior.raisedHeight()), number(0.0F))))));

        String collision = floorCollisionIdentity(prefix, behavior.sectorIndex());
        publishCollision(context, collision, floor.collision());
        references.add(collision);
        ComponentId shapeId = componentId(context.definition().id(), locator + "/collision/shape");
        components.add(collisionShapeForResource(context.definition().id(), collision, shapeId));
        components.add(staticBody(context.definition().id(), locator, entityId, shapeId));

        for (int index = 0; index < floor.batches().size(); index++) {
            RenderBatch batch = floor.batches().get(index);
            String mesh = floorMeshIdentity(prefix, behavior.sectorIndex(), index);
            publishMesh(context, mesh, batch.mesh());
            references.add(mesh);
            components.add(meshRenderer(context.definition().id(), locator, mesh, index, prefix, batch.material()));
        }
        ComponentId behaviorId = componentId(context.definition().id(), locator + "/behavior");
        components.add(floorBehavior(entityId, transformId, behaviorId, behavior));
        return new LocalEntity(entityId, "Moving floor " + behavior.sectorIndex(), true, components, List.of());
    }

    /** Publishes each distinct source trigger once and connects it to every floor sharing its sector tag. */
    private static void publishFloorTriggers(
            ImportPreparationContext context,
            String prefix,
            DoomMap map,
            List<DoomFloorPublication> floors,
            List<EntityEntry> children,
            List<String> references,
            List<SignalConnection> connections)
            throws IOException {
        Map<Integer, List<DoomFloorPublication>> floorsByTrigger = new LinkedHashMap<>();
        for (DoomFloorPublication floor : floors) {
            for (int linedefIndex : floor.triggerLinedefs()) {
                floorsByTrigger
                        .computeIfAbsent(linedefIndex, ignored -> new ArrayList<>())
                        .add(floor);
            }
        }
        for (Map.Entry<Integer, List<DoomFloorPublication>> entry : floorsByTrigger.entrySet()) {
            children.add(publishFloorTrigger(
                    context, prefix, map, entry.getKey(), entry.getValue(), references, connections));
        }
    }

    /** Creates one oriented player-only overlap volume centered on the source walk-over linedef. */
    private static LocalEntity publishFloorTrigger(
            ImportPreparationContext context,
            String prefix,
            DoomMap map,
            int linedefIndex,
            List<DoomFloorPublication> floors,
            List<String> references,
            List<SignalConnection> connections)
            throws IOException {
        TriggerVolume volume = TriggerVolume.of(map, linedefIndex);
        String locator = floorTriggerLocator(prefix, linedefIndex);
        EntityId triggerEntity = entityId(context.definition().id(), locator);
        ComponentId shapeId = componentId(context.definition().id(), locator + "/shape");
        ComponentId sensorId = componentId(context.definition().id(), locator + "/sensor");
        String shapeIdentity = floorTriggerShapeIdentity(prefix, linedefIndex);
        context.artifact(
                ImportArtifactDescriptor.resource(shapeIdentity, Physics3dDescriptors.boxResourceType(), List.of()),
                output -> Physics3dResourceWriter.writeBox(output, volume.width(), volume.height(), TRIGGER_DEPTH));
        references.add(shapeIdentity);

        ComponentDefinition transform = component(
                context.definition().id(),
                locator + "/transform",
                Spatial3dDescriptors.transformType(),
                Map.of(
                        Spatial3dDescriptors.positionProperty(), volume.position(),
                        Spatial3dDescriptors.orientationProperty(), volume.orientation()));
        ComponentDefinition shape = collisionShapeForResource(
                context.definition().id(),
                shapeIdentity,
                shapeId,
                Map.of(
                        Physics3dDescriptors.categoryBitsProperty(),
                                number(DoomCollisionCategories.PLAYER_TRIGGER_SENSOR),
                        Physics3dDescriptors.maskBitsProperty(), number(DoomCollisionCategories.PLAYER)));
        ComponentDefinition sensor = collisionObject(
                context.definition().id(),
                locator + "/sensor",
                Physics3dDescriptors.collisionSensorType(),
                triggerEntity,
                shapeId);
        for (DoomFloorPublication floor : floors) {
            String floorLocator = floorLocator(prefix, floor.sectorIndex());
            connections.add(new SignalConnection(
                    EndpointTarget.component(triggerEntity, sensorId, Physics3dDescriptors.overlapEnteredSignal()),
                    EndpointTarget.component(
                            entityId(context.definition().id(), floorLocator),
                            componentId(context.definition().id(), floorLocator + "/behavior"),
                            DoomFloorDescriptors.TRIGGER_ENTERED_ACTION)));
        }
        return new LocalEntity(
                triggerEntity, "Walk-over trigger " + linedefIndex, true, List.of(transform, shape, sensor), List.of());
    }

    /** Creates one generated child sensor which follows the door and reports character-only overlaps. */
    private static LocalEntity doorObstructionSensor(
            String importId,
            String doorLocator,
            String shapeIdentity,
            Bounds3d bounds,
            EntityId doorEntity,
            ComponentId behaviorId,
            List<SignalConnection> connections) {
        String locator = doorLocator + "/obstruction";
        EntityId sensorEntity = entityId(importId, locator);
        ComponentId shapeId = componentId(importId, locator + "/shape");
        ComponentId sensorId = componentId(importId, locator + "/sensor");
        ComponentDefinition transform = component(
                importId,
                locator + "/transform",
                Spatial3dDescriptors.transformType(),
                Map.of(
                        Spatial3dDescriptors.positionProperty(),
                        new ProjectValue.ArrayValue(List.of(
                                number(bounds.centerX()), number(bounds.centerY()), number(bounds.centerZ())))));
        ComponentDefinition shape = collisionShapeForResource(
                importId,
                shapeIdentity,
                shapeId,
                Map.of(
                        Physics3dDescriptors.categoryBitsProperty(),
                                number(DoomCollisionCategories.DOOR_OBSTRUCTION_SENSOR),
                        Physics3dDescriptors.maskBitsProperty(), number(DoomCollisionCategories.DOOR_OBSTRUCTIONS)));
        ComponentDefinition sensor = collisionObject(
                importId, locator + "/sensor", Physics3dDescriptors.collisionSensorType(), sensorEntity, shapeId);
        connections.add(new SignalConnection(
                EndpointTarget.component(sensorEntity, sensorId, Physics3dDescriptors.overlapEnteredSignal()),
                EndpointTarget.component(doorEntity, behaviorId, DoomDoorDescriptors.OBSTRUCTION_ENTERED_ACTION)));
        connections.add(new SignalConnection(
                EndpointTarget.component(sensorEntity, sensorId, Physics3dDescriptors.overlapExitedSignal()),
                EndpointTarget.component(doorEntity, behaviorId, DoomDoorDescriptors.OBSTRUCTION_EXITED_ACTION)));
        return new LocalEntity(
                sensorEntity, "Door obstruction sensor", true, List.of(transform, shape, sensor), List.of());
    }

    /** Publishes one convex sensor box from the exact local bounds of a movable door collision mesh. */
    private static void publishBoxCollision(ImportPreparationContext context, String identity, Bounds3d bounds)
            throws IOException {
        context.artifact(
                ImportArtifactDescriptor.resource(identity, Physics3dDescriptors.boxResourceType(), List.of()),
                output -> Physics3dResourceWriter.writeBox(output, bounds.width(), bounds.height(), bounds.depth()));
    }

    /** Creates one collision shape retaining an explicitly named generated resource. */
    private static ComponentDefinition collisionShapeForResource(
            String importId, String collisionIdentity, ComponentId shapeId) {
        return collisionShapeForResource(importId, collisionIdentity, shapeId, Map.of());
    }

    /** Creates one collision shape with explicitly authored filtering additions. */
    private static ComponentDefinition collisionShapeForResource(
            String importId, String collisionIdentity, ComponentId shapeId, Map<PropertyId, ProjectValue> additions) {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(Physics3dDescriptors.shapeProperty(), reference(importId, collisionIdentity));
        properties.putAll(additions);
        return new ComponentDefinition(
                shapeId,
                Physics3dDescriptors.collisionShapeType().id(),
                Physics3dDescriptors.collisionShapeType().version(),
                properties);
    }

    /** Creates one collision-object component with exact sibling shape membership. */
    private static ComponentDefinition collisionObject(
            String importId, String locator, ComponentType type, EntityId entityId, ComponentId shapeId) {
        ProjectValue.ComponentTargetValue target =
                new ProjectValue.ComponentTargetValue(new ComponentTarget(entityId, shapeId));
        return component(
                importId,
                locator,
                type,
                Map.of(Physics3dDescriptors.shapesProperty(), new ProjectValue.ArrayValue(List.of(target))));
    }

    /** Creates a static collision body on an explicitly selected generated entity. */
    private static ComponentDefinition staticBody(
            String importId, String locator, EntityId entityId, ComponentId shapeId) {
        ProjectValue.ComponentTargetValue target =
                new ProjectValue.ComponentTargetValue(new ComponentTarget(entityId, shapeId));
        return component(
                importId,
                locator + "/collision/body",
                Physics3dDescriptors.staticBodyType(),
                Map.of(Physics3dDescriptors.shapesProperty(), new ProjectValue.ArrayValue(List.of(target))));
    }

    /** Creates the descriptor-authored door behavior targeting its generated transform. */
    private static ComponentDefinition doorBehavior(
            EntityId entityId, ComponentId transformId, ComponentId behaviorId, DoomDoorPublication behavior) {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                DoomDoorDescriptors.TRANSFORM_PROPERTY,
                new ProjectValue.ComponentTargetValue(new ComponentTarget(entityId, transformId)));
        properties.put(DoomDoorDescriptors.CLOSED_HEIGHT_PROPERTY, number(behavior.closedHeight()));
        properties.put(DoomDoorDescriptors.OPEN_HEIGHT_PROPERTY, number(behavior.openHeight()));
        properties.put(DoomDoorDescriptors.SPEED_PROPERTY, number(behavior.speed()));
        properties.put(DoomDoorDescriptors.HOLD_OPEN_SECONDS_PROPERTY, number(behavior.holdOpenSeconds()));
        properties.put(
                DoomDoorDescriptors.PROFILE_PROPERTY,
                new ProjectValue.TextValue(behavior.profile().name().toLowerCase(Locale.ROOT)));
        return new ComponentDefinition(
                behaviorId, DoomDoorDescriptors.DOOR_TYPE.id(), DoomDoorDescriptors.DOOR_TYPE.version(), properties);
    }

    /** Creates the descriptor-authored floor behavior targeting its generated transform. */
    private static ComponentDefinition floorBehavior(
            EntityId entityId, ComponentId transformId, ComponentId behaviorId, DoomFloorPublication behavior) {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(
                DoomFloorDescriptors.TRANSFORM_PROPERTY,
                new ProjectValue.ComponentTargetValue(new ComponentTarget(entityId, transformId)));
        properties.put(DoomFloorDescriptors.RAISED_HEIGHT_PROPERTY, number(behavior.raisedHeight()));
        properties.put(DoomFloorDescriptors.LOWERED_HEIGHT_PROPERTY, number(behavior.loweredHeight()));
        properties.put(DoomFloorDescriptors.SPEED_PROPERTY, number(behavior.speed()));
        properties.put(
                DoomFloorDescriptors.PROFILE_PROPERTY,
                new ProjectValue.TextValue(behavior.profile().name().toLowerCase(Locale.ROOT)));
        return new ComponentDefinition(
                behaviorId,
                DoomFloorDescriptors.FLOOR_TYPE.id(),
                DoomFloorDescriptors.FLOOR_TYPE.version(),
                properties);
    }

    /** Creates one static body with explicit sibling membership rather than hierarchy inference. */
    private static ComponentDefinition staticBody(String importId, String prefix, ComponentId shapeId) {
        ProjectValue.ComponentTargetValue target = new ProjectValue.ComponentTargetValue(
                new ComponentTarget(entityId(importId, prefix + "/root"), shapeId));
        Map<PropertyId, ProjectValue> properties =
                Map.of(Physics3dDescriptors.shapesProperty(), new ProjectValue.ArrayValue(List.of(target)));
        return component(
                importId, prefix + "/root/collision/static-body", Physics3dDescriptors.staticBodyType(), properties);
    }

    /** Publishes one shared image, texture resource, and unlit material resource. */
    private static void publishMaterial(
            ImportPreparationContext context, String prefix, MaterialKey key, DoomMaterial source) throws IOException {
        String textureIdentity = textureIdentity(prefix, key);
        String payloadIdentity = textureIdentity + ".rgba8";
        String materialIdentity = materialIdentity(prefix, key);
        RgbaImage image = source.image();
        try (Texture texture = createTexture(image)) {
            context.artifact(
                    ImportArtifactDescriptor.payload(payloadIdentity, TEXTURE_MEDIA_TYPE),
                    output -> Spatial3dResourceWriter.writeTexturePayload(output, texture));
            ResourceReference payload = imported(context, payloadIdentity);
            context.artifact(
                    ImportArtifactDescriptor.resource(
                            textureIdentity, Spatial3dDescriptors.textureResourceType(), List.of(payloadIdentity)),
                    output -> Spatial3dResourceWriter.writeTextureDefinition(output, texture, payload));
            try (BasicMaterial material = createMaterial(texture, image)) {
                ResourceReference colorMap = imported(context, textureIdentity);
                context.artifact(
                        ImportArtifactDescriptor.resource(
                                materialIdentity,
                                Spatial3dDescriptors.basicMaterialResourceType(),
                                List.of(textureIdentity)),
                        output -> Spatial3dResourceWriter.writeBasicMaterial(output, material, colorMap));
            }
        }
    }

    /** Publishes one mesh document and opaque binary payload. */
    private static void publishMesh(ImportPreparationContext context, String identity, DoomMeshData mesh)
            throws IOException {
        String payloadIdentity = identity.replace("/resources/meshes/", "/payloads/meshes/") + ".mesh";
        try (BufferGeometry geometry = bufferGeometry(mesh)) {
            context.artifact(
                    ImportArtifactDescriptor.payload(payloadIdentity, MESH_MEDIA_TYPE),
                    output -> Spatial3dResourceWriter.writeMeshPayload(output, geometry));
            ResourceReference payload = imported(context, payloadIdentity);
            context.artifact(
                    ImportArtifactDescriptor.resource(
                            identity, Spatial3dDescriptors.meshResourceType(), List.of(payloadIdentity)),
                    output -> Spatial3dResourceWriter.writeMeshDefinition(output, payload));
        }
    }

    /** Creates one mesh-renderer component referencing a batched mesh and shared material. */
    private static ComponentDefinition meshRenderer(String importId, String prefix, int index, MaterialKey material) {
        return meshRenderer(importId, prefix + "/root", meshIdentity(prefix, index), index, prefix, material);
    }

    /** Creates one renderer for an explicitly named mesh on the selected entity locator. */
    private static ComponentDefinition meshRenderer(
            String importId,
            String entityLocator,
            String meshIdentity,
            int index,
            String mapPrefix,
            MaterialKey material) {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(Spatial3dDescriptors.meshProperty(), reference(importId, meshIdentity));
        properties.put(
                Spatial3dDescriptors.materialProperty(), reference(importId, materialIdentity(mapPrefix, material)));
        return component(
                importId,
                entityLocator + "/mesh-renderers/" + formatted(index),
                Spatial3dDescriptors.meshRendererType(),
                properties);
    }

    /** Groups immutable static surfaces by material to avoid one draw resource per source surface. */
    private static List<RenderBatch> renderBatches(
            DoomStaticGeometry geometry, Set<Integer> doorSectors, Set<Integer> floorSectors) {
        Map<MaterialKey, List<DoomMeshData>> meshesByMaterial = new LinkedHashMap<>();
        for (DoomSurface surface : geometry.surfaces()) {
            if (surface.movingCeilingSector().stream().anyMatch(doorSectors::contains)) {
                continue;
            }
            if (surface.type() == DoomSurface.Type.FLOOR && floorSectors.contains(surface.sectorIndex())) {
                continue;
            }
            meshesByMaterial
                    .computeIfAbsent(materialKey(surface), ignored -> new ArrayList<>())
                    .add(surface.mesh());
        }
        return meshesByMaterial.entrySet().stream()
                .map(entry -> new RenderBatch(entry.getKey(), combine(entry.getValue())))
                .toList();
    }

    /** Groups each supported door's rebased surfaces independently from immutable map geometry. */
    private static List<DoorRenderBatches> doorRenderBatches(
            DoomStaticGeometry geometry, List<DoomDoorPublication> doors) {
        List<DoorRenderBatches> result = new ArrayList<>();
        for (DoomDoorPublication door : doors) {
            Map<MaterialKey, List<DoomMeshData>> meshesByMaterial = new LinkedHashMap<>();
            List<DoomMeshData> collision = new ArrayList<>();
            for (DoomSurface surface : geometry.surfaces()) {
                if (surface.movingCeilingSector().orElse(-1) != door.sectorIndex()) {
                    continue;
                }
                DoomMeshData rebased = rebaseHeight(surface.mesh(), door.closedHeight());
                meshesByMaterial
                        .computeIfAbsent(materialKey(surface), ignored -> new ArrayList<>())
                        .add(rebased);
                if (surface.type() != DoomSurface.Type.MASKED_MIDDLE_WALL) {
                    collision.add(rebased);
                }
            }
            if (collision.isEmpty()) {
                throw new IllegalArgumentException(
                        "manual door has no generated moving surfaces: " + door.sectorIndex());
            }
            List<RenderBatch> batches = meshesByMaterial.entrySet().stream()
                    .map(entry -> new RenderBatch(entry.getKey(), combine(entry.getValue())))
                    .toList();
            result.add(new DoorRenderBatches(door, batches, withoutDegenerateTriangles(combine(collision))));
        }
        return List.copyOf(result);
    }

    /** Groups each supported moving floor's plane independently from immutable map geometry. */
    private static List<FloorRenderBatches> floorRenderBatches(
            DoomStaticGeometry geometry, List<DoomFloorPublication> floors) {
        List<FloorRenderBatches> result = new ArrayList<>();
        for (DoomFloorPublication floor : floors) {
            Map<MaterialKey, List<DoomMeshData>> meshesByMaterial = new LinkedHashMap<>();
            List<DoomMeshData> collision = new ArrayList<>();
            for (DoomSurface surface : geometry.surfaces()) {
                if (surface.type() != DoomSurface.Type.FLOOR || surface.sectorIndex() != floor.sectorIndex()) {
                    continue;
                }
                DoomMeshData rebased = rebaseHeight(surface.mesh(), floor.raisedHeight());
                meshesByMaterial
                        .computeIfAbsent(materialKey(surface), ignored -> new ArrayList<>())
                        .add(rebased);
                collision.add(rebased);
            }
            if (collision.isEmpty()) {
                throw new IllegalArgumentException(
                        "moving floor has no generated floor surfaces: " + floor.sectorIndex());
            }
            List<RenderBatch> batches = meshesByMaterial.entrySet().stream()
                    .map(entry -> new RenderBatch(entry.getKey(), combine(entry.getValue())))
                    .toList();
            result.add(new FloorRenderBatches(floor, batches, withoutDegenerateTriangles(combine(collision))));
        }
        return List.copyOf(result);
    }

    /** Converts world-height vertices into coordinates local to the door's closed transform. */
    private static DoomMeshData rebaseHeight(DoomMeshData mesh, float closedHeight) {
        float[] positions = mesh.positions();
        for (int index = 1; index < positions.length; index += 3) {
            positions[index] -= closedHeight;
        }
        return new DoomMeshData(positions, mesh.normals(), mesh.textureCoordinates(), mesh.indices());
    }

    /** Combines physical surfaces and source-semantic blockers into independently published collision. */
    private static DoomMeshData staticCollisionMesh(
            DoomMap map, DoomStaticGeometry geometry, Set<Integer> doorSectors, Set<Integer> floorSectors) {
        List<DoomMeshData> collisionSurfaces = new ArrayList<>(geometry.surfaces().stream()
                .filter(surface -> surface.type() != DoomSurface.Type.MASKED_MIDDLE_WALL)
                .filter(surface -> surface.movingCeilingSector().stream().noneMatch(doorSectors::contains))
                .filter(surface ->
                        surface.type() != DoomSurface.Type.FLOOR || !floorSectors.contains(surface.sectorIndex()))
                .map(DoomSurface::mesh)
                .toList());
        collisionSurfaces.addAll(new DoomBlockingLineGeometryBuilder().build(map));
        if (collisionSurfaces.isEmpty()) {
            throw new IllegalArgumentException("Doom map has no static collision surfaces");
        }
        return withoutDegenerateTriangles(combine(collisionSurfaces));
    }

    /** Removes presentation triangles that do not define a stable collision plane. */
    private static DoomMeshData withoutDegenerateTriangles(DoomMeshData mesh) {
        float[] positions = mesh.positions();
        int[] sourceIndices = mesh.indices();
        int[] retainedIndices = new int[sourceIndices.length];
        int retainedCount = 0;
        for (int offset = 0; offset < sourceIndices.length; offset += 3) {
            if (definesCollisionPlane(positions, sourceIndices, offset)) {
                System.arraycopy(sourceIndices, offset, retainedIndices, retainedCount, 3);
                retainedCount += 3;
            }
        }
        if (retainedCount == 0) {
            throw new IllegalArgumentException("Doom map has no non-degenerate static collision triangles");
        }
        return new DoomMeshData(
                positions, mesh.normals(), mesh.textureCoordinates(), Arrays.copyOf(retainedIndices, retainedCount));
    }

    /** Returns whether one indexed triangle has enough area to define a collision plane. */
    private static boolean definesCollisionPlane(float[] positions, int[] indices, int offset) {
        int first = indices[offset] * 3;
        int second = indices[offset + 1] * 3;
        int third = indices[offset + 2] * 3;
        float firstEdgeX = positions[second] - positions[first];
        float firstEdgeY = positions[second + 1] - positions[first + 1];
        float firstEdgeZ = positions[second + 2] - positions[first + 2];
        float secondEdgeX = positions[third] - positions[first];
        float secondEdgeY = positions[third + 1] - positions[first + 1];
        float secondEdgeZ = positions[third + 2] - positions[first + 2];
        float normalX = firstEdgeY * secondEdgeZ - firstEdgeZ * secondEdgeY;
        float normalY = firstEdgeZ * secondEdgeX - firstEdgeX * secondEdgeZ;
        float normalZ = firstEdgeX * secondEdgeY - firstEdgeY * secondEdgeX;
        float normalLengthSquared = normalX * normalX + normalY * normalY + normalZ * normalZ;
        return normalLengthSquared >= MINIMUM_COLLISION_NORMAL_LENGTH_SQUARED;
    }

    /** Combines compatible indexed meshes while preserving their vertex and triangle order. */
    private static DoomMeshData combine(List<DoomMeshData> meshes) {
        int vertexCount = meshes.stream().mapToInt(DoomMeshData::vertexCount).reduce(0, Math::addExact);
        int indexCount = meshes.stream()
                .mapToInt(mesh -> Math.multiplyExact(mesh.triangleCount(), 3))
                .reduce(0, Math::addExact);
        float[] positions = new float[Math.multiplyExact(vertexCount, 3)];
        float[] normals = new float[positions.length];
        float[] textureCoordinates = new float[Math.multiplyExact(vertexCount, 2)];
        int[] indices = new int[indexCount];
        int vertexOffset = 0;
        int indexOffset = 0;
        for (DoomMeshData mesh : meshes) {
            copy(mesh.positions(), positions, Math.multiplyExact(vertexOffset, 3));
            copy(mesh.normals(), normals, Math.multiplyExact(vertexOffset, 3));
            copy(mesh.textureCoordinates(), textureCoordinates, Math.multiplyExact(vertexOffset, 2));
            int[] sourceIndices = mesh.indices();
            for (int index = 0; index < sourceIndices.length; index++) {
                indices[indexOffset + index] = Math.addExact(sourceIndices[index], vertexOffset);
            }
            vertexOffset = Math.addExact(vertexOffset, mesh.vertexCount());
            indexOffset = Math.addExact(indexOffset, sourceIndices.length);
        }
        return new DoomMeshData(positions, normals, textureCoordinates, indices);
    }

    /** Copies one primitive source array into its preallocated destination. */
    private static void copy(float[] source, float[] destination, int offset) {
        System.arraycopy(source, 0, destination, offset, source.length);
    }

    /** Collects only referenced materials while preserving first-surface order. */
    private static Map<MaterialKey, DoomMaterial> selectedMaterials(
            List<RenderBatch> batches,
            List<DoorRenderBatches> doors,
            List<FloorRenderBatches> floors,
            DoomMapMaterials sourceMaterials) {
        Map<MaterialKey, DoomMaterial> selected = new LinkedHashMap<>();
        for (RenderBatch batch : batches) {
            MaterialKey key = batch.material();
            selected.computeIfAbsent(key, ignored -> sourceMaterial(key, sourceMaterials));
        }
        doors.stream().flatMap(door -> door.batches().stream()).forEach(batch -> {
            MaterialKey key = batch.material();
            selected.computeIfAbsent(key, ignored -> sourceMaterial(key, sourceMaterials));
        });
        floors.stream().flatMap(floor -> floor.batches().stream()).forEach(batch -> {
            MaterialKey key = batch.material();
            selected.computeIfAbsent(key, ignored -> sourceMaterial(key, sourceMaterials));
        });
        return selected;
    }

    /** Resolves one material from the flat or wall-texture namespace. */
    private static DoomMaterial sourceMaterial(MaterialKey key, DoomMapMaterials materials) {
        Map<String, DoomMaterial> namespace =
                key.kind() == DoomMaterial.Kind.FLAT ? materials.flats() : materials.wallTextures();
        DoomMaterial material = namespace.get(key.name());
        if (material == null) {
            throw new IllegalArgumentException("missing presentation material: " + key.name());
        }
        return material;
    }

    /** Creates Doom's nearest-filtered repeating texture description. */
    private static Texture createTexture(RgbaImage image) {
        Texture texture = Texture.baseColor(image.width(), image.height(), image.pixels());
        texture.setCoordinateOrigin(TextureCoordinateOrigin.TOP_LEFT);
        texture.setHorizontalWrap(TextureWrap.REPEAT);
        texture.setVerticalWrap(TextureWrap.REPEAT);
        texture.setMinificationFilter(TextureFilter.NEAREST_MIPMAP_NEAREST);
        texture.setMagnificationFilter(TextureFilter.NEAREST);
        return texture;
    }

    /** Creates Doom's unlit map material and preserves masked source pixels. */
    private static BasicMaterial createMaterial(Texture texture, RgbaImage image) {
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(texture);
        if (hasTransparentPixel(image.pixels())) {
            material.setAlphaMode(AlphaMode.MASK);
            material.setAlphaCutoff(0.5F);
        }
        return material;
    }

    /** Converts immutable Doom triangle data to one owned project mesh input. */
    private static BufferGeometry bufferGeometry(DoomMeshData mesh) {
        return BufferGeometry.builder()
                .attribute(BufferGeometry.POSITION, BufferAttribute.of(mesh.positions(), 3))
                .normals(mesh.normals())
                .attribute(BufferGeometry.UV, BufferAttribute.of(mesh.textureCoordinates(), 2))
                .indices(mesh.indices())
                .build();
    }

    /** Creates one typed component with a deterministic source-derived identity. */
    private static ComponentDefinition component(
            String importId, String locator, ComponentType type, Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(
                new ComponentId(stableId(importId, locator)), type.id(), type.version(), properties);
    }

    /** Creates one deterministic source-derived component identity. */
    private static ComponentId componentId(String importId, String locator) {
        return new ComponentId(stableId(importId, locator));
    }

    /** Creates one imported resource property. */
    private static ProjectValue.ReferenceValue reference(String importId, String identity) {
        return new ProjectValue.ReferenceValue(ResourceReference.imported(importId + '/' + identity));
    }

    /** Creates one imported resource reference for the active recipe. */
    private static ResourceReference imported(ImportPreparationContext context, String identity) {
        return ResourceReference.imported(context.definition().id() + '/' + identity);
    }

    /** Forwards one WAD diagnostic without replacing its feature-owned identity. */
    private static void report(ImportInspectionContext context, WadDiagnostic diagnostic) {
        if (diagnostic.severity() == WadDiagnostic.Severity.ERROR) {
            context.error(diagnostic.code(), diagnostic.location(), diagnostic.details());
        } else {
            context.warning(diagnostic.code(), diagnostic.location(), diagnostic.details());
        }
    }

    /** Forwards one decoded-map diagnostic without replacing its feature-owned identity. */
    private static void report(ImportInspectionContext context, DoomDiagnostic diagnostic) {
        if (diagnostic.severity() == DoomDiagnostic.Severity.ERROR) {
            context.error(diagnostic.code(), diagnostic.location(), diagnostic.details());
        } else {
            context.warning(diagnostic.code(), diagnostic.location(), diagnostic.details());
        }
    }

    /** Reports one material diagnostic through the Doom import boundary. */
    private static void report(ImportInspectionContext context, DoomMaterialDiagnostic diagnostic) {
        Map<String, String> details = Map.of("sourceCode", diagnostic.code(), "message", diagnostic.message());
        if (diagnostic.severity() == DoomMaterialDiagnostic.Severity.ERROR) {
            context.error(DoomDiagnosticCode.MATERIAL_INVALID, diagnostic.location(), details);
        } else {
            context.warning(DoomDiagnosticCode.MATERIAL_INVALID, diagnostic.location(), details);
        }
    }

    /** Forwards one geometry diagnostic through its stable import code. */
    private static void report(ImportInspectionContext context, DoomGeometryDiagnostic diagnostic) {
        Map<String, String> details = Map.of(
                "sourceCode", diagnostic.code(),
                "message", diagnostic.message());
        if (diagnostic.severity() == DoomGeometryDiagnostic.Severity.ERROR) {
            context.error(DoomDiagnosticCode.GEOMETRY_INVALID, diagnostic.location(), details);
        } else {
            context.warning(DoomDiagnosticCode.GEOMETRY_INVALID, diagnostic.location(), details);
        }
    }

    /** Returns whether any source pixel requires alpha masking. */
    private static boolean hasTransparentPixel(byte[] pixels) {
        for (int alpha = 3; alpha < pixels.length; alpha += 4) {
            if (Byte.toUnsignedInt(pixels[alpha]) < 255) {
                return true;
            }
        }
        return false;
    }

    /** Returns the material namespace and normalized source name for one surface. */
    private static MaterialKey materialKey(DoomSurface surface) {
        DoomMaterial.Kind kind =
                switch (surface.type()) {
                    case FLOOR, CEILING -> DoomMaterial.Kind.FLAT;
                    case MIDDLE_WALL, UPPER_WALL, LOWER_WALL, MASKED_MIDDLE_WALL -> DoomMaterial.Kind.WALL_TEXTURE;
                };
        return new MaterialKey(kind, surface.materialName());
    }

    /** Returns the stable source-item prefix for one map. */
    private static String mapIdentity(String mapName) {
        return "maps/" + mapName;
    }

    /** Returns one deterministic surface-mesh identity. */
    private static String meshIdentity(String prefix, int index) {
        return prefix + "/resources/meshes/" + formatted(index);
    }

    /** Returns one deterministic shared-texture identity. */
    private static String textureIdentity(String prefix, MaterialKey key) {
        return prefix + "/resources/textures/" + key.path();
    }

    /** Returns one deterministic shared-material identity. */
    private static String materialIdentity(String prefix, MaterialKey key) {
        return prefix + "/resources/materials/" + key.path();
    }

    /** Returns the independent static collision-resource identity. */
    private static String collisionIdentity(String prefix) {
        return prefix + "/resources/collision/static";
    }

    /** Returns the stable generated locator for one source-sector door. */
    private static String doorLocator(String prefix, int sectorIndex) {
        return prefix + "/doors/" + formatted(sectorIndex);
    }

    /** Returns one moving door collision resource identity. */
    private static String doorCollisionIdentity(String prefix, int sectorIndex) {
        return prefix + "/resources/collision/doors/" + formatted(sectorIndex);
    }

    /** Returns one moving door's convex obstruction-sensor resource identity. */
    private static String doorObstructionShapeIdentity(String prefix, int sectorIndex) {
        return prefix + "/resources/collision/door-obstruction/" + formatted(sectorIndex);
    }

    /** Returns one moving door render-mesh identity. */
    private static String doorMeshIdentity(String prefix, int sectorIndex, int index) {
        return prefix + "/resources/doors/" + formatted(sectorIndex) + "/meshes/" + formatted(index);
    }

    /** Returns the stable generated locator for one source-sector moving floor. */
    private static String floorLocator(String prefix, int sectorIndex) {
        return prefix + "/floors/" + formatted(sectorIndex);
    }

    /** Returns one moving floor collision resource identity. */
    private static String floorCollisionIdentity(String prefix, int sectorIndex) {
        return prefix + "/resources/collision/floors/" + formatted(sectorIndex);
    }

    /** Returns one moving floor render-mesh identity. */
    private static String floorMeshIdentity(String prefix, int sectorIndex, int index) {
        return prefix + "/resources/floors/" + formatted(sectorIndex) + "/meshes/" + formatted(index);
    }

    /** Returns the stable generated locator for one source-linedef walk-over trigger. */
    private static String floorTriggerLocator(String prefix, int linedefIndex) {
        return prefix + "/floor-triggers/" + formatted(linedefIndex);
    }

    /** Returns one walk-over trigger box resource identity. */
    private static String floorTriggerShapeIdentity(String prefix, int linedefIndex) {
        return prefix + "/resources/collision/floor-triggers/" + formatted(linedefIndex);
    }

    /** Creates a portable finite numeric property. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }

    /** Formats stable surface indices for lexical and source ordering to agree. */
    private static String formatted(int index) {
        return String.format(Locale.ROOT, "%05d", index);
    }

    /** Creates one deterministic source-derived entity identity. */
    private static EntityId entityId(String importId, String locator) {
        return new EntityId(stableId(importId, locator));
    }

    /** Creates one deterministic source-derived definition identity. */
    private static AssetId assetId(String importId, String locator) {
        return new AssetId(stableId(importId, locator));
    }

    /** Uses standard name UUIDs so reimport preserves identity for unchanged source locators. */
    private static UUID stableId(String importId, String locator) {
        return UUID.nameUUIDFromBytes((importId + ':' + locator).getBytes(StandardCharsets.UTF_8));
    }

    /** Separates equal names in the flat and wall-texture namespaces. */
    private record MaterialKey(DoomMaterial.Kind kind, String name) {
        /** Returns one portable namespace-qualified identity suffix. */
        private String path() {
            return kind.name().toLowerCase(Locale.ROOT).replace('_', '-') + '/' + name.toLowerCase(Locale.ROOT);
        }
    }

    /** One renderer-ready static mesh and its shared material. */
    private record RenderBatch(MaterialKey material, DoomMeshData mesh) {}

    /** One generated door's material batches and combined collision, all rebased to its closed transform. */
    private record DoorRenderBatches(DoomDoorPublication behavior, List<RenderBatch> batches, DoomMeshData collision) {
        private DoorRenderBatches {
            batches = List.copyOf(batches);
        }
    }

    /** One generated moving floor's material batches and collision, all rebased to its raised transform. */
    private record FloorRenderBatches(
            DoomFloorPublication behavior, List<RenderBatch> batches, DoomMeshData collision) {
        private FloorRenderBatches {
            batches = List.copyOf(batches);
        }
    }

    /** Oriented box pose and positive extents derived from one source walk-over linedef. */
    private record TriggerVolume(
            ProjectValue.ArrayValue position, ProjectValue.ArrayValue orientation, float width, float height) {
        /** Computes an exact horizontal line span and the complete adjacent vertical opening. */
        private static TriggerVolume of(DoomMap map, int linedefIndex) {
            DoomMap.Linedef linedef = map.linedefs().get(linedefIndex);
            DoomMap.Vertex start = map.vertices().get(linedef.startVertex());
            DoomMap.Vertex end = map.vertices().get(linedef.endVertex());
            float startX = DoomUnits.toWorld(start.x());
            float startZ = DoomUnits.yToWorldZ(start.y());
            float endX = DoomUnits.toWorld(end.x());
            float endZ = DoomUnits.yToWorldZ(end.y());
            float deltaX = endX - startX;
            float deltaZ = endZ - startZ;
            float width = (float) Math.hypot(deltaX, deltaZ);
            if (width <= 0.0F) {
                throw new IllegalArgumentException("floor trigger linedef has zero length: " + linedefIndex);
            }

            List<Integer> adjacentSectors = new ArrayList<>();
            adjacentSectors.add(map.sidedefs().get(linedef.rightSidedef()).sector());
            if (linedef.leftSidedef() >= 0) {
                adjacentSectors.add(map.sidedefs().get(linedef.leftSidedef()).sector());
            }
            float bottom = adjacentSectors.stream()
                    .map(map.sectors()::get)
                    .map(DoomMap.Sector::floorHeight)
                    .map(DoomUnits::toWorld)
                    .min(Float::compare)
                    .orElseThrow();
            float top = adjacentSectors.stream()
                    .map(map.sectors()::get)
                    .map(DoomMap.Sector::ceilingHeight)
                    .map(DoomUnits::toWorld)
                    .max(Float::compare)
                    .orElseThrow();
            if (top <= bottom) {
                throw new IllegalArgumentException("floor trigger has no positive vertical span: " + linedefIndex);
            }
            float yaw = (float) Math.atan2(-deltaZ, deltaX);
            float halfYaw = yaw * 0.5F;
            return new TriggerVolume(
                    numbers(midpoint(startX, endX), midpoint(bottom, top), midpoint(startZ, endZ)),
                    numbers(0.0F, (float) Math.sin(halfYaw), 0.0F, (float) Math.cos(halfYaw)),
                    width,
                    top - bottom);
        }

        /** Avoids adding two potentially large finite coordinates before halving. */
        private static float midpoint(float first, float second) {
            return first + (second - first) * 0.5F;
        }

        /** Creates one portable finite-number array property. */
        private static ProjectValue.ArrayValue numbers(float... values) {
            List<ProjectValue> numbers = new ArrayList<>(values.length);
            for (float value : values) {
                numbers.add(number(value));
            }
            return new ProjectValue.ArrayValue(numbers);
        }
    }

    /** Positive local extents and centre derived from one non-empty collision mesh. */
    private record Bounds3d(float centerX, float centerY, float centerZ, float width, float height, float depth) {
        /** Computes a finite axis-aligned bound directly from the published door vertices. */
        private static Bounds3d of(DoomMeshData mesh) {
            float[] positions = mesh.positions();
            if (positions.length == 0) {
                throw new IllegalArgumentException("door collision mesh must contain positions");
            }
            float minimumX = positions[0];
            float maximumX = positions[0];
            float minimumY = positions[1];
            float maximumY = positions[1];
            float minimumZ = positions[2];
            float maximumZ = positions[2];
            for (int index = 3; index < positions.length; index += 3) {
                minimumX = Math.min(minimumX, positions[index]);
                maximumX = Math.max(maximumX, positions[index]);
                minimumY = Math.min(minimumY, positions[index + 1]);
                maximumY = Math.max(maximumY, positions[index + 1]);
                minimumZ = Math.min(minimumZ, positions[index + 2]);
                maximumZ = Math.max(maximumZ, positions[index + 2]);
            }
            return new Bounds3d(
                    midpoint(minimumX, maximumX),
                    midpoint(minimumY, maximumY),
                    midpoint(minimumZ, maximumZ),
                    maximumX - minimumX,
                    maximumY - minimumY,
                    maximumZ - minimumZ);
        }

        /** Avoids adding two potentially large finite coordinates before halving. */
        private static float midpoint(float minimum, float maximum) {
            return minimum + (maximum - minimum) * 0.5F;
        }
    }
}
