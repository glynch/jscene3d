/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.hierarchy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies semantic hierarchy projection and occurrence-safe identity. */
final class EditorHierarchyProjectorTest {
    private static final AssetId WORLD_ID = AssetId.from("3b406aba-26fb-4681-9abe-7a952c321f9a");
    private static final AssetId DEFINITION_ID = AssetId.from("4ccdb339-9c5b-47d3-9b18-9169be5e4936");
    private static final EntityId LOCAL_ID = EntityId.from("0b295328-b5a3-4f41-9f34-e9b4abc430a7");
    private static final EntityId GENERATED_ID = EntityId.from("7be5cdf0-1bc7-481c-8880-3d627e14d52d");
    private static final EntityId FIRST_PLACEMENT = EntityId.from("9e7c5a08-006a-4434-a0a5-05a6f430a87b");
    private static final EntityId SECOND_PLACEMENT = EntityId.from("24456d3c-0361-48bd-b7d1-38f2d2675061");

    @TempDir
    private Path projectRoot;

    /** Projects ordered world, local, placement, and generated entries with correct state. */
    @Test
    void projectsOrderedHierarchyAndAuthoringState() throws Exception {
        AssetCatalog catalog = catalog(definition());
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        EditorHierarchyProjector projector = projector(catalog, diagnostics);
        WorldDefinition world = world();

        EditorHierarchyNode root = projector.project(world, Set.of(LOCAL_ID));

        assertThat(root.kind()).isEqualTo(EditorHierarchyNode.Kind.WORLD);
        assertThat(root.children())
                .extracting(EditorHierarchyNode::kind)
                .containsExactly(
                        EditorHierarchyNode.Kind.LOCAL_ENTITY,
                        EditorHierarchyNode.Kind.PLACEMENT,
                        EditorHierarchyNode.Kind.PLACEMENT);
        assertThat(root.children().getFirst())
                .returns(true, EditorHierarchyNode::isModified)
                .returns(true, EditorHierarchyNode::isEditable);
        EditorHierarchyNode generated = root.children().get(1).children().getFirst();
        assertThat(generated)
                .returns(EditorHierarchyNode.Kind.GENERATED_ENTITY, EditorHierarchyNode::kind)
                .returns(false, EditorHierarchyNode::isEditable)
                .returns(false, EditorHierarchyNode::isModified);
        assertThat(diagnostics).isEmpty();
    }

    /** Distinguishes repeated definition descendants by their authored placement path and keeps identities stable. */
    @Test
    void createsStableDistinctOccurrenceIdentitiesForRepeatedPlacements() throws Exception {
        AssetCatalog catalog = catalog(definition());
        EditorHierarchyProjector projector = projector(catalog, new ArrayList<>());

        EditorHierarchyNode firstProjection = projector.project(world(), Set.of());
        EditorHierarchyNode secondProjection = projector.project(world(), Set.of());
        HierarchyOccurrenceId first =
                firstProjection.children().get(1).children().getFirst().occurrence();
        HierarchyOccurrenceId second =
                firstProjection.children().get(2).children().getFirst().occurrence();

        assertThat(first).isNotEqualTo(second);
        assertThat(first.entityPath()).containsExactly(FIRST_PLACEMENT, GENERATED_ID);
        assertThat(second.entityPath()).containsExactly(SECOND_PLACEMENT, GENERATED_ID);
        assertThat(secondProjection.children().get(1).children().getFirst().occurrence())
                .isEqualTo(first);
    }

    /** Stops a cyclic definition graph and retains the authored placement as an unavailable leaf. */
    @Test
    void handlesDefinitionCycles() throws Exception {
        EntityPlacement cycle = new EntityPlacement(GENERATED_ID, true, AssetRef.to(DEFINITION_ID), Map.of());
        EntityDefinition definition = new EntityDefinition(
                DEFINITION_ID, "Cyclic", new LocalEntity(LOCAL_ID, "Root", true, List.of(), List.of(cycle)));
        AssetCatalog catalog = catalog(definition);
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        EditorHierarchyProjector projector = projector(catalog, diagnostics);

        EditorHierarchyNode root = projector.project(
                new WorldDefinition(
                        WORLD_ID,
                        "World",
                        List.of(new EntityPlacement(FIRST_PLACEMENT, true, AssetRef.to(DEFINITION_ID), Map.of()))),
                Set.of());

        assertThat(root.children())
                .singleElement()
                .satisfies(node -> assertThat(node.children()).isEmpty());
        assertThat(diagnostics).isNotEmpty();
    }

    private AssetCatalog catalog(EntityDefinition definition) throws Exception {
        DefinitionWriter.write(projectRoot.resolve("shared.entity.json"), definition);
        return AssetCatalog.scan(projectRoot).catalog().orElseThrow();
    }

    private EditorHierarchyProjector projector(AssetCatalog catalog, List<ProjectDiagnostic> diagnostics) {
        return new EditorHierarchyProjector(
                projectRoot.resolve("world.world.json"),
                projectRoot,
                catalog,
                catalog,
                RegisteredTypeCatalog.of(List.of()),
                diagnostics);
    }

    private static EntityDefinition definition() {
        LocalEntity child = new LocalEntity(GENERATED_ID, "Generated child", true, List.of(), List.of());
        LocalEntity root = new LocalEntity(LOCAL_ID, "Definition root", true, List.of(), List.of(child));
        return new EntityDefinition(DEFINITION_ID, "Reusable", root);
    }

    private static WorldDefinition world() {
        LocalEntity local = new LocalEntity(LOCAL_ID, "Local", true, List.of(), List.of());
        EntityPlacement first =
                new EntityPlacement(FIRST_PLACEMENT, "First", true, AssetRef.to(DEFINITION_ID), Map.of());
        EntityPlacement second =
                new EntityPlacement(SECOND_PLACEMENT, "Second", false, AssetRef.to(DEFINITION_ID), Map.of());
        return new WorldDefinition(WORLD_ID, "World", List.of(local, first, second));
    }
}
