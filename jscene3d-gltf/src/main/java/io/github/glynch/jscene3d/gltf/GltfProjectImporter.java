/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.importing.extension.ImportInspectionContext;
import io.github.glynch.jscene3d.project.importing.extension.ImportPreparationContext;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.spatial3d.resource.Spatial3dResourceWriter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Publishes selected static glTF scenes as project-native definitions and referenced resources. */
final class GltfProjectImporter implements ProjectImporter {
    private static final String SCENE_ITEM_KIND = "io.github.glynch.jscene3d.gltf/scene";
    private static final String MESH_MEDIA_TYPE = "application/vnd.jscene3d.mesh-v1";

    @Override
    public void inspect(ImportInspectionContext context) throws IOException {
        context.checkCancelled();
        GltfProjectInspection inspection =
                GltfConverter.inspectProject(context.asset().path());
        inspection.dependencies().forEach(context::dependency);
        for (int index = 0; index < inspection.sceneNames().size(); index++) {
            context.sourceItem(new SourceItem(
                    sceneIdentity(index),
                    SCENE_ITEM_KIND,
                    inspection.sceneNames().get(index),
                    true,
                    Map.of("scene-index", number(index)),
                    List.of()));
        }
    }

    @Override
    public void prepare(ImportPreparationContext context) throws IOException {
        inspect(context);
        for (String selected : context.definition().selection()) {
            context.checkCancelled();
            int sceneIndex = parseSceneIdentity(selected);
            try (GltfProjectContent content =
                    GltfConverter.loadProject(context.asset().path(), sceneIndex)) {
                publishScene(context, sceneIndex, content);
            }
        }
    }

    /** Publishes all referenced resources before the generated definition that names them. */
    private static void publishScene(ImportPreparationContext context, int sceneIndex, GltfProjectContent content)
            throws IOException {
        String prefix = sceneIdentity(sceneIndex);
        Map<String, GltfProjectContent.Primitive> meshes = new LinkedHashMap<>();
        Map<String, GltfProjectContent.Primitive> materials = new LinkedHashMap<>();
        collectResources(content.roots(), prefix, meshes, materials);
        for (Map.Entry<String, GltfProjectContent.Primitive> entry : meshes.entrySet()) {
            publishMesh(context, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, GltfProjectContent.Primitive> entry : materials.entrySet()) {
            context.artifact(
                    ImportArtifactDescriptor.resource(
                            entry.getKey(), Spatial3dDescriptors.materialResourceType(), List.of()),
                    output -> Spatial3dResourceWriter.writeMaterial(
                            output, entry.getValue().material()));
        }
        List<String> references = new ArrayList<>(meshes.keySet());
        references.addAll(materials.keySet());
        AssetId definitionId = assetId(context.definition().id(), prefix + "/definition");
        EntityDefinition definition = new EntityDefinition(
                definitionId, content.name(), sceneRoot(context.definition().id(), prefix, content));
        context.artifact(
                ImportArtifactDescriptor.entityDefinition(prefix + "/definition", definitionId, references),
                output -> DefinitionWriter.write(output, definition));
    }

    /** Publishes one mesh document and its opaque payload as independently addressable artifacts. */
    private static void publishMesh(
            ImportPreparationContext context, String resourceIdentity, GltfProjectContent.Primitive primitive)
            throws IOException {
        String payloadIdentity = resourceIdentity.replace("/resources/", "/payloads/") + ".mesh";
        context.artifact(
                ImportArtifactDescriptor.payload(payloadIdentity, MESH_MEDIA_TYPE),
                output -> Spatial3dResourceWriter.writeMeshPayload(output, primitive.geometry()));
        ResourceReference payloadReference =
                ResourceReference.imported(context.definition().id() + '/' + payloadIdentity);
        context.artifact(
                ImportArtifactDescriptor.resource(
                        resourceIdentity, Spatial3dDescriptors.meshResourceType(), List.of(payloadIdentity)),
                output -> Spatial3dResourceWriter.writeMeshDefinition(output, payloadReference));
    }

    /** Collects shared resources by deterministic source locator. */
    private static void collectResources(
            List<GltfProjectContent.Node> nodes,
            String prefix,
            Map<String, GltfProjectContent.Primitive> meshes,
            Map<String, GltfProjectContent.Primitive> materials) {
        for (GltfProjectContent.Node node : nodes) {
            for (GltfProjectContent.Primitive primitive : node.primitives()) {
                meshes.putIfAbsent(meshIdentity(prefix, primitive), primitive);
                materials.putIfAbsent(materialIdentity(prefix, primitive), primitive);
            }
            collectResources(node.children(), prefix, meshes, materials);
        }
    }

    /** Creates one synthetic definition root so every selected glTF scene remains single-rooted. */
    private static LocalEntity sceneRoot(String importId, String prefix, GltfProjectContent content) {
        List<EntityEntry> children = content.roots().stream()
                .map(node -> (EntityEntry) entity(importId, prefix, node))
                .toList();
        return new LocalEntity(
                entityId(importId, prefix + "/root"),
                content.name(),
                true,
                List.of(transform(
                        importId,
                        prefix + "/root/transform",
                        new GltfProjectContent.Transform(new Vector3f(), new Quaternionf(), new Vector3f(1.0F)))),
                children);
    }

    /** Converts one prepared source node into a project-native authored entity. */
    private static LocalEntity entity(String importId, String prefix, GltfProjectContent.Node node) {
        String locator = prefix + "/nodes/" + formatted(node.sourceIndex());
        List<ComponentDefinition> components = new ArrayList<>();
        components.add(transform(importId, locator + "/transform", node.transform()));
        for (GltfProjectContent.Primitive primitive : node.primitives()) {
            Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
            properties.put(Spatial3dDescriptors.meshProperty(), reference(importId, meshIdentity(prefix, primitive)));
            properties.put(
                    Spatial3dDescriptors.materialProperty(), reference(importId, materialIdentity(prefix, primitive)));
            components.add(component(
                    importId,
                    locator + "/mesh-renderers/" + formatted(primitive.meshIndex()) + '-'
                            + formatted(primitive.primitiveIndex()),
                    Spatial3dDescriptors.meshRendererType(),
                    properties));
        }
        List<EntityEntry> children = node.children().stream()
                .map(child -> (EntityEntry) entity(importId, prefix, child))
                .toList();
        return new LocalEntity(entityId(importId, locator), node.name(), true, components, children);
    }

    /** Creates one transform component from decomposed local source values. */
    private static ComponentDefinition transform(
            String importId, String locator, GltfProjectContent.Transform transform) {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(Spatial3dDescriptors.positionProperty(), vector(transform.position()));
        properties.put(Spatial3dDescriptors.orientationProperty(), quaternion(transform.orientation()));
        properties.put(Spatial3dDescriptors.scaleProperty(), vector(transform.scale()));
        return component(importId, locator, Spatial3dDescriptors.transformType(), properties);
    }

    /** Creates one typed component with a deterministic source-derived identity. */
    private static ComponentDefinition component(
            String importId, String locator, ComponentType type, Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(
                new ComponentId(stableId(importId, locator)), type.id(), type.version(), properties);
    }

    /** Creates one imported-resource property. */
    private static ProjectValue.ReferenceValue reference(String importId, String identity) {
        return new ProjectValue.ReferenceValue(ResourceReference.imported(importId + '/' + identity));
    }

    /** Creates one portable three-component vector. */
    private static ProjectValue.ArrayValue vector(Vector3fc vector) {
        return new ProjectValue.ArrayValue(List.of(number(vector.x()), number(vector.y()), number(vector.z())));
    }

    /** Creates one portable four-component quaternion. */
    private static ProjectValue.ArrayValue quaternion(Quaternionfc value) {
        return new ProjectValue.ArrayValue(
                List.of(number(value.x()), number(value.y()), number(value.z()), number(value.w())));
    }

    /** Creates one portable decimal value. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
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

    /** Returns one stable selectable scene identity. */
    private static String sceneIdentity(int index) {
        return "scenes/" + formatted(index);
    }

    /** Parses one selection after requiring the exact inspected identity shape. */
    private static int parseSceneIdentity(String identity) {
        if (!identity.startsWith("scenes/") || identity.length() != 11) {
            throw new IllegalArgumentException("unsupported glTF source selection: " + identity);
        }
        try {
            return Integer.parseInt(identity.substring(7));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("unsupported glTF source selection: " + identity, failure);
        }
    }

    /** Formats stable source indices for lexical and source ordering to agree. */
    private static String formatted(int index) {
        return String.format(Locale.ROOT, "%04d", index);
    }

    /** Returns one deterministic mesh-resource locator. */
    private static String meshIdentity(String prefix, GltfProjectContent.Primitive primitive) {
        return prefix + "/resources/meshes/" + formatted(primitive.meshIndex()) + "/primitives/"
                + formatted(primitive.primitiveIndex());
    }

    /** Returns one deterministic shared-material locator. */
    private static String materialIdentity(String prefix, GltfProjectContent.Primitive primitive) {
        String local = primitive.materialIndex() < 0 ? "default" : formatted(primitive.materialIndex());
        return prefix + "/resources/materials/" + local;
    }
}
