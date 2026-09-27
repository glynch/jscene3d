/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjector;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProjection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProperty;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorValue;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionDocument;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.asset.DefinitionResolvers;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies per-definition authored working-copy behavior through the permanent session facade. */
final class EditorProjectSessionTest {
    private static final AssetId WORLD_ID = AssetId.from("3b406aba-26fb-4681-9abe-7a952c321f9a");
    private static final AssetId ENTITY_DEFINITION_ID = AssetId.from("4ccdb339-9c5b-47d3-9b18-9169be5e4936");
    private static final AssetId GENERATED_DEFINITION_ID = AssetId.from("11111111-1111-4111-8111-111111111111");
    private static final EntityId WORLD_ENTITY_ID = EntityId.from("0b295328-b5a3-4f41-9f34-e9b4abc430a7");
    private static final EntityId DEFINITION_ENTITY_ID = EntityId.from("7be5cdf0-1bc7-481c-8880-3d627e14d52d");
    private static final ComponentId WORLD_COMPONENT_ID = ComponentId.from("3e940be7-e58d-4f3a-8b5e-e61c99c00904");
    private static final ComponentId DEFINITION_COMPONENT_ID = ComponentId.from("8f9c514c-eb42-4800-a9a6-012347ff7104");
    private static final PropertyId SPEED = new PropertyId("speed");
    private static final PropertyId BOOST = new PropertyId("boost");

    @TempDir
    private Path projectRoot;

    /** Retains one reusable authoritative working copy for each authored structural definition. */
    @Test
    void retainsEditableWorldAndEntityWorkingCopies() throws Exception {
        try (EditorProjectSession session = session()) {
            EditorRetainedDefinition world = retain(session, WORLD_ID);
            EditorRetainedDefinition entity = retain(session, ENTITY_DEFINITION_ID);
            AuthoredDefinitionWorkingCopy worldCopy =
                    session.retainedAuthoredWorkingCopy(WORLD_ID).orElseThrow();

            assertAuthoredInitialState(world, AssetKind.WORLD_DEFINITION);
            assertAuthoredInitialState(entity, AssetKind.ENTITY_DEFINITION);
            assertThat(entity.hierarchy().roots()).singleElement().returns(true, EditorHierarchyNode::isEditable);
            assertThat(session.definitionState(WORLD_ID)).get().returns(false, AuthoringDefinitionState::dirty);
            assertThat(session.definitionState(ENTITY_DEFINITION_ID))
                    .get()
                    .returns(false, AuthoringDefinitionState::dirty);
            session.retainDefinition(WORLD_ID);
            assertThat(session.retainedAuthoredWorkingCopy(WORLD_ID)).containsSame(worldCopy);
            assertThat(session.retainedDefinitionIds()).containsExactly(WORLD_ID, ENTITY_DEFINITION_ID);
        }
    }

    /** Applies SET, reports same-value no-op, and returns stale or validation outcomes atomically. */
    @Test
    void appliesSetAndReturnsStructuredNonChangingOutcomes() throws Exception {
        try (EditorProjectSession session = session()) {
            retain(session, WORLD_ID);
            InspectorMutationTarget.ComponentProperty target = worldProperty(session, SPEED);
            List<AuthoringDefinitionChange> changes = definitionChanges(session);

            AuthoringMutationResult changed = set(session, WORLD_ID, target, number("2"), 0L);
            AuthoringMutationResult noOp = set(session, WORLD_ID, target, number("2"), 1L);
            AuthoringMutationResult stale = set(session, WORLD_ID, target, number("3"), 0L);
            AuthoringMutationResult rejected = set(session, WORLD_ID, target, new ProjectValue.TextValue("fast"), 1L);

            assertOutcome(changed, AuthoringMutationResult.Outcome.ACCEPTED, 1L, true, true, false);
            assertOutcome(noOp, AuthoringMutationResult.Outcome.NO_OP, 1L, true, true, false);
            assertOutcome(stale, AuthoringMutationResult.Outcome.STALE_REVISION, 1L, true, true, false);
            assertOutcome(rejected, AuthoringMutationResult.Outcome.VALIDATION_REJECTED, 1L, true, true, false);
            assertThat(rejected.diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .containsExactly("property.kind");
            assertThat(changes)
                    .containsExactly(change(WORLD_ID, 1L, true, true, false, AuthoringDefinitionChange.Kind.SET));
        }
    }

    /** Implements REMOVE for optional properties while preserving required-property validation. */
    @Test
    void removesOptionalPropertyAndRejectsRequiredPropertyRemoval() throws Exception {
        try (EditorProjectSession session = session()) {
            retain(session, WORLD_ID);
            InspectorMutationTarget.ComponentProperty boost = worldProperty(session, BOOST);
            InspectorMutationTarget.ComponentProperty speed = worldProperty(session, SPEED);

            AuthoringMutationResult add = set(session, WORLD_ID, boost, number("4"), 0L);
            AuthoringMutationResult removed = session.mutate(WORLD_ID, boost, new AuthoringMutation.Remove(), 1L);
            AuthoringMutationResult absent = session.mutate(WORLD_ID, boost, new AuthoringMutation.Remove(), 2L);
            AuthoringMutationResult required = session.mutate(WORLD_ID, speed, new AuthoringMutation.Remove(), 2L);

            assertOutcome(add, AuthoringMutationResult.Outcome.ACCEPTED, 1L, true, true, false);
            assertOutcome(removed, AuthoringMutationResult.Outcome.ACCEPTED, 2L, false, true, false);
            assertOutcome(absent, AuthoringMutationResult.Outcome.NO_OP, 2L, false, true, false);
            assertOutcome(required, AuthoringMutationResult.Outcome.VALIDATION_REJECTED, 2L, false, true, false);
            assertThat(required.diagnostics()).isNotEmpty();
        }
    }

    /** Keeps revisions monotonic through the required A-B-C undo/undo/redo sequence. */
    @Test
    void maintainsMonotonicRevisionAndCoherentHistoryNotifications() throws Exception {
        try (EditorProjectSession session = session()) {
            retain(session, WORLD_ID);
            InspectorMutationTarget.ComponentProperty target = worldProperty(session, SPEED);
            List<AuthoringDefinitionChange> changes = definitionChanges(session);

            set(session, WORLD_ID, target, number("2"), 0L);
            set(session, WORLD_ID, target, number("3"), 1L);
            session.undo(WORLD_ID, 2L);
            session.undo(WORLD_ID, 3L);
            AuthoringMutationResult redone = session.redo(WORLD_ID, 4L);

            assertOutcome(redone, AuthoringMutationResult.Outcome.ACCEPTED, 5L, true, true, true);
            assertThat(changes).extracting(AuthoringDefinitionChange::revision).containsExactly(1L, 2L, 3L, 4L, 5L);
            assertThat(changes)
                    .extracting(AuthoringDefinitionChange::kind)
                    .containsExactly(
                            AuthoringDefinitionChange.Kind.SET,
                            AuthoringDefinitionChange.Kind.SET,
                            AuthoringDefinitionChange.Kind.UNDO,
                            AuthoringDefinitionChange.Kind.UNDO,
                            AuthoringDefinitionChange.Kind.REDO);
        }
    }

    /** Tracks a saved middle state without clearing history or changing content revision. */
    @Test
    void marksPersistedStateAndTracksDirtyAcrossHistory() throws Exception {
        try (EditorProjectSession session = session()) {
            retain(session, WORLD_ID);
            InspectorMutationTarget.ComponentProperty target = worldProperty(session, SPEED);
            set(session, WORLD_ID, target, number("2"), 0L);
            AuthoredDefinitionDocument stateB =
                    session.retainedAuthoredDocument(WORLD_ID).orElseThrow();
            AuthoringMutationResult persisted = session.markPersisted(WORLD_ID, stateB, 1L);
            set(session, WORLD_ID, target, number("3"), 1L);

            assertOutcome(persisted, AuthoringMutationResult.Outcome.ACCEPTED, 1L, false, true, false);
            assertThat(session.undo(WORLD_ID, 2L)).returns(false, AuthoringMutationResult::dirty);
            assertThat(session.undo(WORLD_ID, 3L)).returns(true, AuthoringMutationResult::dirty);
            assertThat(session.redo(WORLD_ID, 4L)).returns(false, AuthoringMutationResult::dirty);
            assertThat(session.markPersisted(WORLD_ID, stateB, 5L).outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.NO_OP);
        }
    }

    /** Retains persisted state even when its old redo transaction becomes unreachable. */
    @Test
    void keepsPersistedBaselineAfterRedoBranchIsDiscarded() throws Exception {
        try (EditorProjectSession session = session()) {
            retain(session, WORLD_ID);
            InspectorMutationTarget.ComponentProperty target = worldProperty(session, SPEED);
            set(session, WORLD_ID, target, number("2"), 0L);
            set(session, WORLD_ID, target, number("3"), 1L);
            AuthoredDefinitionDocument stateC =
                    session.retainedAuthoredDocument(WORLD_ID).orElseThrow();
            session.markPersisted(WORLD_ID, stateC, 2L);
            session.undo(WORLD_ID, 2L);
            set(session, WORLD_ID, target, number("4"), 3L);
            session.undo(WORLD_ID, 4L);
            AuthoringMutationResult returnsToC = set(session, WORLD_ID, target, number("3"), 5L);

            assertOutcome(returnsToC, AuthoringMutationResult.Outcome.ACCEPTED, 6L, false, true, false);
            assertThat(session.redo(WORLD_ID, 6L).outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.REDO_UNAVAILABLE);
        }
    }

    /** Gives authored entity definitions the same mutation, revision, projection, and history behavior as worlds. */
    @Test
    void mutatesAndProjectsAuthoredEntityDefinitionWorkingCopy() throws Exception {
        try (EditorProjectSession session = session()) {
            EditorRetainedDefinition retained = retain(session, ENTITY_DEFINITION_ID);
            InspectorMutationTarget.ComponentProperty target = new InspectorMutationTarget.ComponentProperty(
                    retained.hierarchy().roots().getFirst().occurrence(),
                    DEFINITION_ENTITY_ID,
                    DEFINITION_COMPONENT_ID,
                    SPEED);

            AuthoringMutationResult changed = set(session, ENTITY_DEFINITION_ID, target, number("5"), 0L);
            EditorRetainedDefinition current = retain(session, ENTITY_DEFINITION_ID);
            InspectorProjection inspection =
                    session.inspect(current.hierarchy().roots().getFirst().inspectorTarget(), current.revision());

            assertOutcome(changed, AuthoringMutationResult.Outcome.ACCEPTED, 1L, true, true, false);
            assertThat(property(inspection, SPEED).state().authoredValue())
                    .contains(new InspectorValue.NumberValue(new BigDecimal("5")));
            session.undo(ENTITY_DEFINITION_ID, 1L);
            EditorRetainedDefinition restored = retain(session, ENTITY_DEFINITION_ID);
            InspectorProjection restoredInspection =
                    session.inspect(restored.hierarchy().roots().getFirst().inspectorTarget(), restored.revision());
            assertThat(property(restoredInspection, SPEED).state().authoredValue())
                    .contains(new InspectorValue.NumberValue(BigDecimal.ONE));
        }
    }

    /** Keeps generated definitions immutable through mutation and history session entry points. */
    @Test
    void rejectsGeneratedDefinitionOperationsAsNonEditable() throws Exception {
        URI source = URI.create("jscene3d-import:/models/generated.entity.json");
        try (EditorProjectSession session = sessionWithGeneratedDefinition(source)) {
            EditorRetainedDefinition generated = retain(session, GENERATED_DEFINITION_ID);
            EditorHierarchyNode root = generated.hierarchy().roots().getFirst();
            InspectorMutationTarget.ComponentProperty target = new InspectorMutationTarget.ComponentProperty(
                    root.occurrence(), root.entityId().orElseThrow(), WORLD_COMPONENT_ID, SPEED);

            assertThat(set(session, GENERATED_DEFINITION_ID, target, number("2"), 0L)
                            .outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.NON_EDITABLE);
            assertThat(session.mutate(GENERATED_DEFINITION_ID, target, new AuthoringMutation.Remove(), 0L)
                            .outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.NON_EDITABLE);
            assertThat(session.undo(GENERATED_DEFINITION_ID, 0L).outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.NON_EDITABLE);
            assertThat(session.redo(GENERATED_DEFINITION_ID, 0L).outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.NON_EDITABLE);
            assertThat(session.definitionState(GENERATED_DEFINITION_ID)).isEmpty();
        }
    }

    /** Returns bounded invalid/history outcomes without changing state or publishing notifications. */
    @Test
    void rejectsInvalidTargetsAndUnavailableHistoryWithoutSideEffects() throws Exception {
        try (EditorProjectSession session = session()) {
            retain(session, WORLD_ID);
            List<AuthoringDefinitionChange> changes = definitionChanges(session);
            HierarchyOccurrenceId document = new HierarchyOccurrenceId(WORLD_ID, List.of());
            InspectorMutationTarget.EntityEnabled invalid =
                    new InspectorMutationTarget.EntityEnabled(document, WORLD_ENTITY_ID);

            assertThat(session.mutate(
                                    WORLD_ID,
                                    invalid,
                                    new AuthoringMutation.Set(new ProjectValue.BooleanValue(false)),
                                    0L)
                            .outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.INVALID_TARGET);
            assertThat(session.undo(WORLD_ID, 0L).outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.UNDO_UNAVAILABLE);
            assertThat(session.redo(WORLD_ID, 0L).outcome())
                    .isEqualTo(AuthoringMutationResult.Outcome.REDO_UNAVAILABLE);
            assertThat(changes).isEmpty();
            assertThat(session.isDirty()).isFalse();
        }
    }

    /** Disposes every retained working copy and rejects all later session operations. */
    @Test
    void closesDefinitionWorkingCopiesWithTheirProjectSession() throws Exception {
        EditorProjectSession session = session();
        retain(session, WORLD_ID);
        AuthoredDefinitionWorkingCopy workingCopy =
                session.retainedAuthoredWorkingCopy(WORLD_ID).orElseThrow();
        InspectorMutationTarget.ComponentProperty target = worldProperty(session, SPEED);
        ProjectValue replacement = number("2");

        session.close();
        session.close();

        assertThat(session.isClosed()).isTrue();
        assertThatThrownBy(() -> set(session, WORLD_ID, target, replacement, 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("session is closed");
        assertThatThrownBy(workingCopy::state)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("working copy is closed");
    }

    /** Resolves only current retained Java-issued Inspector targets at the expected per-definition revision. */
    @Test
    void inspectsRetainedOccurrenceAtPerDefinitionRevision() throws Exception {
        try (EditorProjectSession session = session()) {
            EditorRetainedDefinition retained = retain(session, WORLD_ID);
            InspectorTarget target = retained.hierarchy().roots().getFirst().inspectorTarget();
            InspectorProjection projection = session.inspect(target, 0L);

            assertThat(projection)
                    .returns(target, InspectorProjection::target)
                    .returns(InspectorProjection.DefinitionOrigin.AUTHORED, InspectorProjection::definitionOrigin)
                    .returns(true, InspectorProjection::editable);
            assertThatThrownBy(() -> session.inspect(target, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("stale authoring revision");
            InspectorTarget mismatch =
                    new InspectorTarget(target.kind(), target.source(), "different-entity", target.occurrence());
            assertThatThrownBy(() -> session.inspect(mismatch, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not match");
        }
    }

    /** Returns resolver diagnostics without retaining an unknown structural identity. */
    @Test
    void rejectsUnknownDefinitionIdentity() throws Exception {
        try (EditorProjectSession session = session()) {
            DefinitionRetentionResult result = session.retainDefinition(GENERATED_DEFINITION_ID);

            assertThat(result.definition()).isEmpty();
            assertThat(result.diagnostics()).isNotEmpty();
            assertThat(session.retainedDefinitionIds()).isEmpty();
        }
    }

    /** Returns one retained definition or fails the test with its diagnostics. */
    private static EditorRetainedDefinition retain(EditorProjectSession session, AssetId id) {
        return session.retainDefinition(id).definition().orElseThrow();
    }

    /** Asserts the common initial contract for authored world and entity definitions. */
    private static void assertAuthoredInitialState(EditorRetainedDefinition retained, AssetKind kind) {
        assertThat(retained)
                .returns(kind, EditorRetainedDefinition::kind)
                .returns(EditorRetainedDefinition.Origin.AUTHORED, EditorRetainedDefinition::origin)
                .returns(true, EditorRetainedDefinition::editable)
                .returns(0L, EditorRetainedDefinition::revision);
    }

    /** Subscribes to the one coherent authored-definition state stream. */
    private static List<AuthoringDefinitionChange> definitionChanges(EditorProjectSession session) {
        List<AuthoringDefinitionChange> changes = new ArrayList<>();
        session.onDidChangeDefinition().subscribe(changes::add);
        return changes;
    }

    /** Creates the current startup-world component-property target. */
    private static InspectorMutationTarget.ComponentProperty worldProperty(
            EditorProjectSession session, PropertyId property) {
        return new InspectorMutationTarget.ComponentProperty(
                session.hierarchy().roots().getFirst().occurrence(), WORLD_ENTITY_ID, WORLD_COMPONENT_ID, property);
    }

    /** Applies one SET through the public session entry point. */
    private static AuthoringMutationResult set(
            EditorProjectSession session,
            AssetId definition,
            InspectorMutationTarget target,
            ProjectValue value,
            long expectedRevision) {
        return session.mutate(definition, target, new AuthoringMutation.Set(value), expectedRevision);
    }

    /** Asserts one complete structured operation result. */
    private static void assertOutcome(
            AuthoringMutationResult result,
            AuthoringMutationResult.Outcome outcome,
            long revision,
            boolean dirty,
            boolean canUndo,
            boolean canRedo) {
        assertThat(result)
                .returns(outcome, AuthoringMutationResult::outcome)
                .returns(revision, AuthoringMutationResult::revision)
                .returns(dirty, AuthoringMutationResult::dirty)
                .returns(canUndo, AuthoringMutationResult::canUndo)
                .returns(canRedo, AuthoringMutationResult::canRedo);
    }

    /** Creates one expected authored-definition notification. */
    private static AuthoringDefinitionChange change(
            AssetId id,
            long revision,
            boolean dirty,
            boolean canUndo,
            boolean canRedo,
            AuthoringDefinitionChange.Kind kind) {
        return new AuthoringDefinitionChange(id, revision, dirty, canUndo, canRedo, kind);
    }

    /** Finds one projected Inspector property. */
    private static InspectorProperty property(InspectorProjection projection, PropertyId property) {
        return projection.sections().stream()
                .flatMap(section -> section.properties().stream())
                .filter(candidate -> candidate.identity().equals(property.toString()))
                .findFirst()
                .orElseThrow();
    }

    private EditorProjectSession session() throws IOException {
        writeProject();
        return new EditorProjectLoader("0.1.0-SNAPSHOT", getClass().getClassLoader())
                .load(projectRoot)
                .session()
                .orElseThrow();
    }

    private EditorProjectSession sessionWithGeneratedDefinition(URI source) throws Exception {
        EditorProjectSession base = session();
        ByteArrayOutputStream content = generatedDefinition();
        DefinitionResolver definitions = DefinitionResolvers.builder(base.authoredAssets())
                .addGeneratedEntity(GENERATED_DEFINITION_ID, source, new ByteArrayInputStream(content.toByteArray()))
                .build();
        EditorProjectSession.Source sessionSource = source(base, definitions);
        List<ProjectAsset> assets = base.assets();
        RegisteredTypeCatalog types = base.types();
        base.close();
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        return new EditorProjectSession(
                sessionSource, assets, new EditorHierarchyProjector(definitions, types, diagnostics), diagnostics);
    }

    /** Serializes one generated entity fixture. */
    private static ByteArrayOutputStream generatedDefinition() throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        DefinitionWriter.write(
                content,
                new EntityDefinition(
                        GENERATED_DEFINITION_ID,
                        "Generated",
                        new LocalEntity(
                                EntityId.from("22222222-2222-4222-8222-222222222222"),
                                "Generated Root",
                                true,
                                List.of(),
                                List.of())));
        return content;
    }

    /** Captures source inputs before the base fixture session is closed. */
    private static EditorProjectSession.Source source(EditorProjectSession base, DefinitionResolver definitions) {
        GameProject project = base.project();
        ProjectConfiguration configuration = base.configuration();
        AssetCatalog authored = base.authoredAssets();
        RegisteredTypeCatalog types = base.types();
        WorldDefinition world = base.startupWorld();
        Path worldSource = base.startupWorldSource();
        return new EditorProjectSession.Source(
                project, configuration, authored, types, definitions, world, worldSource);
    }

    private void writeProject() throws IOException {
        writeDescriptor();
        writeExtension();
        writeWorld();
        writeEntity();
    }

    private void writeDescriptor() throws IOException {
        write("authoring-session.j3d", """
                {
                  "$schema":"https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion":1,
                  "identity":{"id":"example.session-test","name":"Session Test","version":"1.0.0"},
                  "engine":{"requires":">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime":{"applicationExtension":"example.session-test","entryScene":"worlds/main.world.json"},
                  "extensions":[{"id":"example.session-test","requires":">=1.0.0 <2.0.0"}]
                }
                """);
    }

    private void writeExtension() throws IOException {
        write("src/main/resources/META-INF/jscene3d/extension.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.session-test",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Session Test",
                  "types":[],
                  "components":[{
                    "id":"example.session-test/mover",
                    "typeVersion":1,
                    "displayName":"Mover",
                    "properties":[
                      {"id":"speed","valueKind":"number","required":true,"displayName":"Speed"},
                      {"id":"boost","valueKind":"number","required":false,"displayName":"Boost"}
                    ]
                  }]
                }
                """);
    }

    private void writeWorld() throws IOException {
        write("worlds/main.world.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/world-definition-1.json",
                  "assetId":"3b406aba-26fb-4681-9abe-7a952c321f9a",
                  "assetType":"world-definition",
                  "formatVersion":1,
                  "name":"Opening World",
                  "connections":[],
                  "roots":[{
                    "entryType":"local",
                    "entityId":"0b295328-b5a3-4f41-9f34-e9b4abc430a7",
                    "name":"Player",
                    "enabled":true,
                    "components":[{
                      "componentId":"3e940be7-e58d-4f3a-8b5e-e61c99c00904",
                      "type":"example.session-test/mover",
                      "typeVersion":1,
                      "properties":{"speed":1}
                    }],
                    "children":[]
                  }]
                }
                """);
    }

    private void writeEntity() throws IOException {
        write("entities/reusable.entity.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/entity-definition-1.json",
                  "assetId":"4ccdb339-9c5b-47d3-9b18-9169be5e4936",
                  "assetType":"entity-definition",
                  "formatVersion":1,
                  "name":"Reusable",
                  "contract":{"parameters":[],"signals":[],"actions":[],"capabilities":[],"attachments":[],"resourceBindings":[]},
                  "connections":[],
                  "root":{
                    "entryType":"local",
                    "entityId":"7be5cdf0-1bc7-481c-8880-3d627e14d52d",
                    "name":"Reusable Root",
                    "enabled":true,
                    "components":[{
                      "componentId":"8f9c514c-eb42-4800-a9a6-012347ff7104",
                      "type":"example.session-test/mover",
                      "typeVersion":1,
                      "properties":{"speed":1}
                    }],
                    "children":[]
                  }
                }
                """);
    }

    private void write(String relativePath, String content) throws IOException {
        Path target = projectRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    /** Creates one exact decimal project number. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }
}
