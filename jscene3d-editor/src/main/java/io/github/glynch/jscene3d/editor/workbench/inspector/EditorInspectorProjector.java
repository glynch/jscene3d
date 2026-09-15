/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorComponentSectionProjector.componentSections;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProperties.authoredProperty;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProperties.booleanProperty;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProperties.numberProperty;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProperties.textProperty;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorViewFactory.Editability.READ_ONLY;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorViewFactory.editability;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorViewFactory.section;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorViewFactory.selection;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorViewFactory.view;

import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKindId;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Projects validated project data into immutable, non-executable Inspector data. */
public final class EditorInspectorProjector {
    private EditorInspectorProjector() {}

    /**
     * Projects the opened world root.
     *
     * @param world world definition
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @return immutable world selection
     */
    public static EditorSelection world(WorldDefinition world, Path source, Path projectRoot) {
        List<EditorDetails.Property> properties = List.of(
                textProperty("asset-id", "Asset ID", world.id().toString()),
                numberProperty("root-count", "Root entities", world.roots().size()),
                numberProperty(
                        "connection-count", "Connections", world.connections().size()));
        EditorDetails details = view(
                world.name(),
                "World definition",
                source,
                projectRoot,
                world.id().toString(),
                READ_ONLY,
                List.of(section("World", properties)));
        return selection(EditorSelectionKinds.WORLD, source, world.id().toString(), details);
    }

    /**
     * Projects one local or generated entity and its descriptor-backed components.
     *
     * @param entity entity to project
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @param types registered component types
     * @param generated whether the entity was generated
     * @return immutable entity selection
     */
    public static EditorSelection entity(
            LocalEntity entity, Path source, Path projectRoot, RegisteredTypeCatalog types, boolean generated) {
        return entity(entity, source, projectRoot, types, generated, Optional.empty(), Optional.empty());
    }

    /**
     * Projects one local or generated entity with an optional enabled-state edit command.
     *
     * @param entity entity to project
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @param types registered component types
     * @param generated whether the entity was generated
     * @param enabledEditor optional enabled-state editor
     * @return immutable entity selection
     */
    public static EditorSelection entity(
            LocalEntity entity,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated,
            Optional<Consumer<Boolean>> enabledEditor) {
        return entity(entity, source, projectRoot, types, generated, enabledEditor, Optional.empty());
    }

    /**
     * Projects one local or generated entity with optional authored entity/component edit commands.
     *
     * @param entity entity to project
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @param types registered component types
     * @param generated whether the entity was generated
     * @param enabledEditor optional enabled-state editor
     * @param componentEditor optional component-property editor
     * @return immutable entity selection
     */
    public static EditorSelection entity(
            LocalEntity entity,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated,
            Optional<Consumer<Boolean>> enabledEditor,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        String title = entity.name().orElse("Unnamed entity");
        List<EditorDetails.Section> sections = new ArrayList<>();
        sections.add(section(
                "Entity",
                List.of(
                        booleanProperty("enabled", "Enabled", entity.isEnabled(), enabledEditor),
                        numberProperty(
                                "child-count", "Children", entity.children().size()))));
        sections.addAll(componentSections(entity.id(), entity.components(), types, componentEditor));
        EditorDetails details = view(
                title,
                generated ? "Generated entity" : "Local entity",
                source,
                projectRoot,
                entity.id().toString(),
                editability(generated, enabledEditor.isPresent()),
                sections);
        EditorSelectionKindId kind =
                generated ? EditorSelectionKinds.GENERATED_ENTITY : EditorSelectionKinds.LOCAL_ENTITY;
        return selection(kind, source, entity.id().toString(), details);
    }

    /**
     * Projects an authored reusable-definition placement and its realized root components.
     *
     * @param placement placement to project
     * @param definition resolved reusable definition, when available
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @param types registered component types
     * @param generated whether the placement was generated
     * @return immutable placement selection
     */
    public static EditorSelection placement(
            EntityPlacement placement,
            Optional<EntityDefinition> definition,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated) {
        return placement(placement, definition, source, projectRoot, types, generated, Optional.empty());
    }

    /**
     * Projects an authored placement with an optional enabled-state edit command.
     *
     * @param placement placement to project
     * @param definition resolved reusable definition, when available
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @param types registered component types
     * @param generated whether the placement was generated
     * @param enabledEditor optional enabled-state editor
     * @return immutable placement selection
     */
    public static EditorSelection placement(
            EntityPlacement placement,
            Optional<EntityDefinition> definition,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated,
            Optional<Consumer<Boolean>> enabledEditor) {
        String title = placement
                .name()
                .orElseGet(() -> definition.map(EntityDefinition::name).orElse("Unavailable definition"));
        List<EditorDetails.Property> placementProperties = new ArrayList<>();
        placementProperties.add(textProperty(
                "definition", "Definition", placement.definition().id().toString()));
        placementProperties.add(booleanProperty("enabled", "Enabled", placement.isEnabled(), enabledEditor));
        placement.arguments().forEach((id, value) -> placementProperties.add(authoredProperty(id, value)));
        List<EditorDetails.Section> sections = new ArrayList<>();
        sections.add(section("Placement", placementProperties));
        definition.ifPresent(
                value -> sections.addAll(componentSections(value.root().components(), types)));
        EditorDetails details = view(
                title,
                generated ? "Generated placement" : "Entity-definition placement",
                source,
                projectRoot,
                placement.id().toString(),
                editability(generated, enabledEditor.isPresent()),
                sections);
        return selection(EditorSelectionKinds.PLACEMENT, source, placement.id().toString(), details);
    }

    /**
     * Projects one reusable entity-definition asset.
     *
     * @param definition entity definition
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @param types registered component types
     * @return immutable asset selection
     */
    public static EditorSelection entityDefinition(
            EntityDefinition definition, Path source, Path projectRoot, RegisteredTypeCatalog types) {
        List<EditorDetails.Section> sections = new ArrayList<>();
        sections.add(section(
                "Definition",
                List.of(
                        textProperty("asset-id", "Asset ID", definition.id().toString()),
                        numberProperty(
                                "parameters",
                                "Parameters",
                                definition.contract().parameters().size()),
                        numberProperty(
                                "connections",
                                "Connections",
                                definition.connections().size()))));
        sections.addAll(componentSections(definition.root().components(), types));
        EditorDetails details = view(
                definition.name(),
                "Entity definition",
                source,
                projectRoot,
                definition.id().toString(),
                READ_ONLY,
                sections);
        return selection(EditorSelectionKinds.ASSET, source, definition.id().toString(), details);
    }

    /**
     * Projects one world-definition asset.
     *
     * @param world world definition
     * @param source source file
     * @param projectRoot project root used to relativize source paths
     * @return immutable asset selection
     */
    public static EditorSelection worldDefinition(WorldDefinition world, Path source, Path projectRoot) {
        EditorDetails details = view(
                world.name(),
                "World definition",
                source,
                projectRoot,
                world.id().toString(),
                READ_ONLY,
                List.of(section(
                        "Definition",
                        List.of(
                                textProperty("asset-id", "Asset ID", world.id().toString()),
                                numberProperty(
                                        "root-count",
                                        "Root entities",
                                        world.roots().size()),
                                numberProperty(
                                        "connection-count",
                                        "Connections",
                                        world.connections().size())))));
        return selection(EditorSelectionKinds.ASSET, source, world.id().toString(), details);
    }

    /**
     * Projects one authoritative source asset declared by the manifest.
     *
     * @param asset source asset
     * @param projectRoot project root used to relativize source paths
     * @return immutable asset selection
     */
    public static EditorSelection sourceAsset(GameProject.AssetSource asset, Path projectRoot) {
        List<EditorDetails.Property> properties = new ArrayList<>();
        properties.add(textProperty("identity", "Identity", asset.id()));
        properties.add(textProperty("type", "Type", asset.type()));
        asset.sha256().ifPresent(hash -> properties.add(textProperty("sha256", "SHA-256", hash)));
        EditorDetails details = view(
                asset.id(),
                "Source asset",
                asset.path(),
                projectRoot,
                asset.id(),
                READ_ONLY,
                List.of(section("Asset", properties)));
        return selection(EditorSelectionKinds.ASSET, asset.path(), asset.id(), details);
    }

    /**
     * Projects one deterministic source-import definition and its authored settings.
     *
     * @param definition source-import definition
     * @param projectRoot project root used to relativize source paths
     * @return immutable asset selection
     */
    public static EditorSelection importDefinition(ImportDefinition definition, Path projectRoot) {
        List<EditorDetails.Property> properties = new ArrayList<>();
        properties.add(textProperty("identity", "Identity", definition.id()));
        properties.add(textProperty("importer", "Importer", definition.importer()));
        properties.add(textProperty("asset", "Source asset", definition.asset().id()));
        properties.add(numberProperty(
                "selection-count", "Selected items", definition.selection().size()));
        definition.settings().forEach((id, value) -> properties.add(authoredProperty(new PropertyId(id), value)));
        EditorDetails details = view(
                definition.id(),
                "Import definition",
                definition.source(),
                projectRoot,
                definition.id(),
                READ_ONLY,
                List.of(section("Import", properties)));
        return selection(EditorSelectionKinds.ASSET, definition.source(), definition.id(), details);
    }
}
