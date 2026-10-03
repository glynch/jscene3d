/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionMutationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOperationResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionSnapshot;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorReadResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorSnapshot;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectCloseResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectSummary;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewSnapshotDto;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchSpecification;
import io.github.glynch.jscene3d.editor.authoring.testing.AuthoringTestProject;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.project.session.AuthoringMutation;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjectionResult;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises retained-session ownership through the real safe authoring loader. */
final class AuthoringProjectServiceTest {
    @TempDir
    private Path temporaryDirectory;

    private final AuthoringProjectService service = new AuthoringProjectService(
            new EditorProjectLoader("0.1.0-SNAPSHOT", AuthoringProjectServiceTest.class.getClassLoader()));

    /** Releases retained sessions even after assertion failures. */
    @AfterEach
    void closeService() {
        service.close();
    }

    /** Opens a current descriptor, retains its session, and returns the narrow project summary. */
    @Test
    void opensJ3dProjectAndReturnsSummary() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);

        ProjectOpenResult result = service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        ProjectSummary summary = Objects.requireNonNull(result.project());

        assertThat(result.opened()).isTrue();
        assertThat(result.projectGeneration()).isEqualTo(1L);
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.failureCode()).isNull();
        assertThat(summary)
                .returns("example.authoring-test", project -> project.id())
                .returns("Small Authoring Project", project -> project.name())
                .returns("1.2.3", project -> project.version())
                .returns(temporaryDirectory.toRealPath().toString(), project -> project.root())
                .returns(
                        temporaryDirectory
                                .toRealPath()
                                .resolve(AuthoringTestProject.DESCRIPTOR)
                                .toString(),
                        project -> project.descriptor())
                .satisfies(project -> {
                    ProjectSummary.SceneSummary mainScene = Objects.requireNonNull(project.mainScene());
                    assertThat(mainScene.id()).isEqualTo("e890c4c3-fb32-49d8-88b8-4e04e7a29656");
                    assertThat(mainScene.name()).isEqualTo("Opening Scene");
                    assertThat(project.assetCounts().authored()).isEqualTo(1);
                    assertThat(project.assetCounts().projected()).isEqualTo(1);
                    assertThat(project.catalog().scenes())
                            .singleElement()
                            .returns(AuthoringTestProject.SCENE_ASSET_ID, ProjectSummary.CatalogEntry::id)
                            .returns("Opening Scene", ProjectSummary.CatalogEntry::name)
                            .returns("authored", ProjectSummary.CatalogEntry::origin)
                            .returns(true, ProjectSummary.CatalogEntry::editable)
                            .returns(true, ProjectSummary.CatalogEntry::mainScene)
                            .satisfies(entry -> assertThat(entry.source()).endsWith("/worlds/main.scene.json"));
                    assertThat(project.catalog().entityDefinitions()).isEmpty();
                });
        assertThat(service.activeSession()).isPresent();
    }

    /** Scopes Java-side Scene View requests to project, Scene, and retained definition revision. */
    @Test
    void scopesSceneViewProjectionToCurrentAuthoringIdentity() throws IOException {
        AssetId scene = AssetId.from(AuthoringTestProject.SCENE_ASSET_ID);
        SceneViewServiceResult unopened = service.projectSceneView(1L, scene, 0L);
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

        SceneViewServiceResult wrongGeneration = service.projectSceneView(2L, scene, 0L);
        SceneViewServiceResult unretained = service.projectSceneView(1L, scene, 0L);
        service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.SCENE_ASSET_ID));
        SceneViewServiceResult projected = service.projectSceneView(1L, scene, 0L);
        SceneViewReadResult wireProjection = service.readSceneView(
                new SceneViewReadParams(1L, AuthoringTestProject.SCENE_ASSET_ID, 0L), Locale.ENGLISH);

        assertThat(unopened)
                .returns(false, SceneViewServiceResult::accepted)
                .returns(AuthoringProjectService.PROJECT_NOT_OPEN, SceneViewServiceResult::failureCode);
        assertThat(wrongGeneration)
                .returns(false, SceneViewServiceResult::accepted)
                .returns(AuthoringProjectService.PROJECT_GENERATION_CONFLICT, SceneViewServiceResult::failureCode);
        assertThat(unretained)
                .returns(true, SceneViewServiceResult::accepted)
                .returns(1L, SceneViewServiceResult::projectGeneration)
                .extracting(SceneViewServiceResult::projection)
                .isNotNull()
                .extracting(SceneViewProjectionResult::outcome)
                .isEqualTo(SceneViewProjectionResult.Outcome.SCENE_UNAVAILABLE);
        assertThat(projected)
                .returns(true, SceneViewServiceResult::accepted)
                .returns(1L, SceneViewServiceResult::projectGeneration)
                .extracting(SceneViewServiceResult::projection)
                .isNotNull()
                .satisfies(result -> {
                    assertThat(result.outcome()).isEqualTo(SceneViewProjectionResult.Outcome.PROJECTED);
                    assertThat(result.scene()).isEqualTo(scene);
                    assertThat(result.currentRevision()).hasValue(0L);
                    assertThat(result.snapshot()).get().returns(scene, snapshot -> snapshot.scene());
                });
        assertThat(wireProjection)
                .returns(true, SceneViewReadResult::accepted)
                .returns("projected", SceneViewReadResult::outcome)
                .returns(0L, SceneViewReadResult::currentRevision)
                .satisfies(result -> {
                    assertThat(result.snapshot()).isNotNull();
                    assertThat(result.launch())
                            .isNotNull()
                            .returns("example.authoring-test", SceneViewReadResult.LaunchSpecification::projectId)
                            .returns("Opening Scene", SceneViewReadResult.LaunchSpecification::sceneName);
                });
    }

    /** Returns exact protocol outcomes for absent, stale, malformed, and stale-revision Scene View requests. */
    @Test
    void rejectsInvalidSceneViewProtocolIdentity() throws IOException {
        SceneViewReadResult unopened = service.readSceneView(
                new SceneViewReadParams(1L, AuthoringTestProject.SCENE_ASSET_ID, 0L), Locale.ENGLISH);
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

        SceneViewReadResult wrongGeneration = service.readSceneView(
                new SceneViewReadParams(2L, AuthoringTestProject.SCENE_ASSET_ID, 0L), Locale.ENGLISH);
        SceneViewReadResult malformed =
                service.readSceneView(new SceneViewReadParams(1L, "not-an-id", 0L), Locale.ENGLISH);
        service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.SCENE_ASSET_ID));
        SceneViewReadResult staleRevision = service.readSceneView(
                new SceneViewReadParams(1L, AuthoringTestProject.SCENE_ASSET_ID, 1L), Locale.ENGLISH);

        assertThat(unopened)
                .returns(false, SceneViewReadResult::accepted)
                .returns(AuthoringProjectService.PROJECT_NOT_OPEN, SceneViewReadResult::failureCode);
        assertThat(wrongGeneration)
                .returns(false, SceneViewReadResult::accepted)
                .returns(AuthoringProjectService.PROJECT_GENERATION_CONFLICT, SceneViewReadResult::failureCode);
        assertThat(malformed)
                .returns(true, SceneViewReadResult::accepted)
                .returns("scene-unavailable", SceneViewReadResult::outcome)
                .returns(null, SceneViewReadResult::snapshot)
                .returns(null, SceneViewReadResult::launch);
        assertThat(staleRevision)
                .returns(true, SceneViewReadResult::accepted)
                .returns("stale-revision", SceneViewReadResult::outcome)
                .returns(0L, SceneViewReadResult::currentRevision)
                .returns(null, SceneViewReadResult::snapshot)
                .returns(null, SceneViewReadResult::launch);
    }

    /** Serializes every supported built-in Scene View projection without runtime implementation inputs. */
    @Test
    void serializesCompleteSceneViewProjection() throws IOException {
        AuthoringTestProject.writeSceneViewProject(temporaryDirectory);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        DefinitionOpenResult opened =
                service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.SCENE_ASSET_ID));

        SceneViewReadResult result = service.readSceneView(
                new SceneViewReadParams(1L, AuthoringTestProject.SCENE_ASSET_ID, 0L), Locale.ENGLISH);
        SceneViewSnapshotDto snapshot = Objects.requireNonNull(result.snapshot());

        assertThat(opened.opened()).withFailMessage(opened::toString).isTrue();
        assertThat(result)
                .returns(true, SceneViewReadResult::accepted)
                .returns("projected", SceneViewReadResult::outcome)
                .returns(0L, SceneViewReadResult::currentRevision);
        assertThat(snapshot.occurrences()).hasSize(2);
        assertRootSceneViewProjection(snapshot.occurrences().getFirst());
        assertChildSceneViewProjection(snapshot.occurrences().get(1));
        assertThat(result.launch())
                .isNotNull()
                .returns("example.authoring-test", SceneViewReadResult.LaunchSpecification::projectId)
                .returns("Opening Scene", SceneViewReadResult.LaunchSpecification::sceneName);
    }

    /** Verifies the root transform, mesh resources, and stable component scope. */
    private static void assertRootSceneViewProjection(SceneViewSnapshotDto.VisualOccurrence root) {
        assertThat(root.parent()).isNull();
        assertThat(root.name()).isEqualTo("Visual Root");
        assertThat(root.transform()).isNotNull().satisfies(transform -> {
            assertThat(transform.position().x()).isEqualTo("1.25");
            assertThat(transform.orientationDegrees().z()).isEqualTo("30");
            assertThat(transform.scale().y()).isEqualTo("3");
            assertThat(transform.identity().scope().definitionAssetId()).isEqualTo(AuthoringTestProject.SCENE_ASSET_ID);
        });
        assertThat(root.meshes()).singleElement().satisfies(mesh -> {
            assertThat(mesh.visible()).isTrue();
            assertThat(mesh.mesh())
                    .returns("asset", SceneViewSnapshotDto.ResourceReference::kind)
                    .returns("test-mesh", SceneViewSnapshotDto.ResourceReference::locator)
                    .returns(null, SceneViewSnapshotDto.ResourceReference::projectPath);
            assertThat(mesh.material())
                    .returns("asset", SceneViewSnapshotDto.ResourceReference::kind)
                    .returns("test-material", SceneViewSnapshotDto.ResourceReference::locator)
                    .returns(null, SceneViewSnapshotDto.ResourceReference::projectPath);
        });
        assertThat(root.directionalLight()).isNull();
    }

    /** Verifies parent identity, disabled state, and exact directional-light values. */
    private static void assertChildSceneViewProjection(SceneViewSnapshotDto.VisualOccurrence child) {
        assertThat(child.parent()).isNotNull();
        assertThat(child.name()).isNull();
        assertThat(child.enabled()).isFalse();
        assertThat(child.transform()).isNotNull();
        assertThat(child.meshes()).isEmpty();
        assertThat(child.directionalLight()).isNotNull().satisfies(light -> {
            assertThat(light.color().y()).isEqualTo("0.5");
            assertThat(light.intensity()).isEqualTo("3.5");
            assertThat(light.target().z()).isEqualTo("9");
        });
    }

    /** Groups definitions semantically and retains Main Scene identity after physical relocation. */
    @Test
    void catalogsDefinitionsIndependentlyOfPhysicalDirectoryLayout() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        AuthoringTestProject.writeEntityDefinition(temporaryDirectory);
        Path relocatedScene = temporaryDirectory.resolve("unconventional/content/opening.scene.json");
        Path relocatedEntity = temporaryDirectory.resolve("misc/catalog/player.entity.json");
        Files.createDirectories(relocatedScene.getParent());
        Files.createDirectories(relocatedEntity.getParent());
        Files.move(temporaryDirectory.resolve("worlds/main.scene.json"), relocatedScene);
        Files.move(temporaryDirectory.resolve("entities/reusable.entity.json"), relocatedEntity);

        ProjectSummary summary =
                Objects.requireNonNull(service.openProject(new ProjectOpenParams(temporaryDirectory.toString()))
                        .project());

        assertThat(summary.catalog().scenes())
                .singleElement()
                .returns(AuthoringTestProject.SCENE_ASSET_ID, ProjectSummary.CatalogEntry::id)
                .returns(true, ProjectSummary.CatalogEntry::mainScene)
                .satisfies(entry -> assertThat(entry.source()).endsWith("/unconventional/content/opening.scene.json"));
        assertThat(summary.catalog().entityDefinitions())
                .singleElement()
                .returns(AuthoringTestProject.DEFINITION_ASSET_ID, ProjectSummary.CatalogEntry::id)
                .returns(false, ProjectSummary.CatalogEntry::mainScene)
                .satisfies(entry -> assertThat(entry.source()).endsWith("/misc/catalog/player.entity.json"));
    }

    /** Opens a valid Project with no Scenes, no Main Scene, and no Entity definitions. */
    @Test
    void returnsEmptySemanticGroupsForProjectWithoutDefinitions() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        Files.delete(temporaryDirectory.resolve("worlds/main.scene.json"));
        Path descriptor = temporaryDirectory.resolve(AuthoringTestProject.DESCRIPTOR);
        String source = Files.readString(descriptor);
        Files.writeString(
                descriptor,
                source.replace(
                        "\"runtime\":{\"applicationExtension\":\"example.authoring-test\",\"mainScene\":{\"assetId\":\"e890c4c3-fb32-49d8-88b8-4e04e7a29656\",\"pathHint\":\"worlds/main.scene.json\"}}",
                        "\"runtime\":{\"applicationExtension\":\"example.authoring-test\"}"));

        ProjectOpenResult result = service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        ProjectSummary summary = Objects.requireNonNull(result.project());

        assertThat(result.opened()).isTrue();
        assertThat(summary.mainScene()).isNull();
        assertThat(summary.catalog().scenes()).isEmpty();
        assertThat(summary.catalog().entityDefinitions()).isEmpty();
    }

    /** Prepares only the generation-scoped Main Scene with renderer-only runtime inputs. */
    @Test
    void preparesAuthoritativeMainSceneViewportLaunch() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        Path runtimeArtifact = temporaryDirectory.resolve("runtime/application.jar");
        AuthoringProjectService viewportService = new AuthoringProjectService(
                new EditorProjectLoader("0.1.0-SNAPSHOT", AuthoringProjectServiceTest.class.getClassLoader()),
                "0.1.0-SNAPSHOT",
                List.of(runtimeArtifact));
        try (viewportService) {
            viewportService.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

            ViewportLaunchResult result = viewportService.prepareViewportLaunch(
                    new ViewportLaunchParams(1L, AuthoringTestProject.SCENE_ASSET_ID));
            ViewportLaunchSpecification launch = Objects.requireNonNull(result.launch());

            assertThat(result.prepared()).isTrue();
            assertThat(result.failureCode()).isNull();
            assertThat(launch)
                    .returns(1L, ViewportLaunchSpecification::projectGeneration)
                    .returns("example.authoring-test", ViewportLaunchSpecification::projectId)
                    .returns("Small Authoring Project", ViewportLaunchSpecification::projectName)
                    .returns("0.1.0-SNAPSHOT", ViewportLaunchSpecification::engineVersion)
                    .returns(AuthoringTestProject.SCENE_ASSET_ID, ViewportLaunchSpecification::sceneAssetId)
                    .returns("Opening Scene", ViewportLaunchSpecification::sceneName);
            assertThat(launch.projectRoot())
                    .isEqualTo(temporaryDirectory.toRealPath().toString());
            assertThat(launch.runtimeArtifacts())
                    .containsExactly(
                            runtimeArtifact.toAbsolutePath().normalize().toString());
        }
    }

    /** Rejects absent projects, stale generations, malformed IDs, and non-Main Scenes. */
    @Test
    void rejectsUnavailableViewportLaunchIdentity() throws IOException {
        ViewportLaunchResult absent =
                service.prepareViewportLaunch(new ViewportLaunchParams(1L, AuthoringTestProject.SCENE_ASSET_ID));
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

        ViewportLaunchResult stale =
                service.prepareViewportLaunch(new ViewportLaunchParams(2L, AuthoringTestProject.SCENE_ASSET_ID));
        ViewportLaunchResult malformed = service.prepareViewportLaunch(new ViewportLaunchParams(1L, "not-an-id"));
        ViewportLaunchResult different =
                service.prepareViewportLaunch(new ViewportLaunchParams(1L, AuthoringTestProject.DEFINITION_ASSET_ID));

        assertThat(absent.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_NOT_OPEN);
        assertThat(stale.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_GENERATION_CONFLICT);
        assertThat(malformed.failureCode()).isEqualTo(AuthoringProjectService.VIEWPORT_SCENE_UNAVAILABLE);
        assertThat(different.failureCode()).isEqualTo(AuthoringProjectService.VIEWPORT_SCENE_UNAVAILABLE);
    }

    /** Opens generic world and entity definitions only for the expected active project generation. */
    @Test
    void opensDefinitionsWithGenerationScopedIdentity() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        AuthoringTestProject.writeEntityDefinition(temporaryDirectory);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

        DefinitionOpenResult world =
                service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.SCENE_ASSET_ID));
        DefinitionOpenResult entity =
                service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.DEFINITION_ASSET_ID));
        DefinitionOpenResult stale =
                service.openDefinition(new DefinitionOpenParams(2L, AuthoringTestProject.SCENE_ASSET_ID));

        assertThat(world.opened()).isTrue();
        var worldSnapshot = Objects.requireNonNull(world.definition());
        assertThat(worldSnapshot.context())
                .returns("scene-definition", context -> context.kind())
                .returns("authored", context -> context.origin())
                .returns(true, context -> context.editable());
        assertThat(worldSnapshot.roots()).hasSize(1);
        assertThat(entity.opened()).isTrue();
        var entitySnapshot = Objects.requireNonNull(entity.definition());
        assertThat(entitySnapshot.context())
                .returns("entity-definition", DefinitionSnapshot.DefinitionContext::kind)
                .returns("authored", DefinitionSnapshot.DefinitionContext::origin)
                .returns(true, DefinitionSnapshot.DefinitionContext::editable);
        assertThat(entitySnapshot.roots())
                .singleElement()
                .satisfies(root -> assertThat(root.occurrence().entityPath()).hasSize(1));
        assertThat(service.activeSession().orElseThrow().retainedDefinitionIds())
                .hasSize(2);
        assertThat(stale.opened()).isFalse();
        assertThat(stale.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_GENERATION_CONFLICT);
    }

    /** Reads a complete current Inspector snapshot and rejects stale or mismatched identity. */
    @Test
    void readsInspectorWithGenerationRevisionAndTargetChecks() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        DefinitionOpenResult definition =
                service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.SCENE_ASSET_ID));
        DefinitionSnapshot retained = Objects.requireNonNull(definition.definition());
        DefinitionSnapshot.SemanticTarget target = retained.roots().getFirst().target();

        InspectorReadResult read =
                service.readInspector(new InspectorReadParams(1L, retained.revision(), target), Locale.FRENCH);
        InspectorReadResult staleGeneration =
                service.readInspector(new InspectorReadParams(2L, retained.revision(), target), Locale.ENGLISH);
        InspectorReadResult staleRevision =
                service.readInspector(new InspectorReadParams(1L, retained.revision() + 1, target), Locale.ENGLISH);
        InspectorReadResult mismatched = service.readInspector(
                new InspectorReadParams(
                        1L,
                        retained.revision(),
                        new DefinitionSnapshot.SemanticTarget(
                                target.kind(),
                                target.source(),
                                "different-identity",
                                Objects.requireNonNull(target.occurrence()))),
                Locale.ENGLISH);

        assertThat(read.read()).isTrue();
        assertThat(read.projectGeneration()).isEqualTo(1L);
        assertThat(Objects.requireNonNull(read.snapshot()))
                .returns(retained.revision(), snapshot -> snapshot.revision())
                .returns("Player", snapshot -> snapshot.title())
                .returns("authored", snapshot -> snapshot.definitionOrigin())
                .returns("local", snapshot -> snapshot.provenance());
        assertThat(Objects.requireNonNull(read.snapshot()).groups())
                .singleElement()
                .returns("Entité", group -> group.label());
        assertThat(staleGeneration.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_GENERATION_CONFLICT);
        assertThat(staleRevision.failureCode()).isEqualTo(AuthoringProjectService.INSPECTOR_STALE);
        assertThat(mismatched.failureCode()).isEqualTo(AuthoringProjectService.INSPECTOR_TARGET_INVALID);
    }

    /** Projects Euler semantics and exact degree components through the complete service boundary. */
    @Test
    void projectsEulerRotationInspectorSemantics() throws IOException {
        AuthoringTestProject.writeEulerRotationProject(temporaryDirectory);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        DefinitionOpenResult definition =
                service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.SCENE_ASSET_ID));
        DefinitionSnapshot retained = Objects.requireNonNull(definition.definition());

        InspectorReadResult read = service.readInspector(
                new InspectorReadParams(
                        1L, retained.revision(), retained.roots().getFirst().target()),
                Locale.ENGLISH);
        var property = Objects.requireNonNull(read.snapshot())
                .groups()
                .get(1)
                .properties()
                .getFirst();
        var value = (InspectorSnapshot.ArrayValue)
                Objects.requireNonNull(property.state().effectiveValue());

        assertThat(property.constraints().editor().semantic()).isEqualTo("euler-rotation");
        assertThat(property.constraints().elementKind()).isEqualTo("number");
        assertThat(property.constraints().exactElementCount()).isEqualTo(3);
        assertThat(value.values())
                .extracting(component -> ((InspectorSnapshot.NumberValue) component).decimal())
                .containsExactly("0", "90.0000000000000000001", "-2.5");
    }

    /** Parses every first-slice scalar candidate in Java while preserving exact decimal text. */
    @Test
    @SuppressWarnings("NullAway") // Candidate transport variants deliberately leave the alternate field null.
    void mutatesSupportedScalarCandidatesThroughServiceBoundary() throws IOException {
        AuthoringTestProject.writeScalarPropertyProject(temporaryDirectory);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        DefinitionSnapshot definition = Objects.requireNonNull(
                service.openDefinition(new DefinitionOpenParams(1L, AuthoringTestProject.SCENE_ASSET_ID))
                        .definition());
        InspectorSnapshot inspector = Objects.requireNonNull(service.readInspector(
                        new InspectorReadParams(
                                1L,
                                definition.revision(),
                                definition.roots().getFirst().target()),
                        Locale.ENGLISH)
                .snapshot());

        DefinitionOperationResult booleanResult =
                mutate(inspector, "visible", 0L, new DefinitionMutationParams.CandidateValue("boolean", false, null));
        DefinitionOperationResult integerResult = mutate(
                inspector,
                "count",
                1L,
                new DefinitionMutationParams.CandidateValue("integer", null, "12345678901234567890"));
        DefinitionOperationResult numberResult = mutate(
                inspector,
                "precision",
                2L,
                new DefinitionMutationParams.CandidateValue("number", null, "0.00000000000000000001"));
        DefinitionOperationResult textResult = mutate(
                inspector, "title", 3L, new DefinitionMutationParams.CandidateValue("text", null, "Exact title"));
        DefinitionOperationResult rejected =
                mutate(inspector, "count", 4L, new DefinitionMutationParams.CandidateValue("integer", null, "1.5"));
        DefinitionOperationResult malformed = mutate(
                inspector,
                "precision",
                4L,
                new DefinitionMutationParams.CandidateValue("number", null, "not-a-number"));

        assertThat(List.of(booleanResult, integerResult, numberResult, textResult))
                .extracting(DefinitionOperationResult::outcome)
                .containsExactly("accepted", "accepted", "accepted", "accepted");
        assertThat(textResult)
                .returns(4L, DefinitionOperationResult::revision)
                .returns(true, DefinitionOperationResult::dirty)
                .returns(true, DefinitionOperationResult::canUndo);
        assertThat(rejected)
                .returns("validation-rejected", DefinitionOperationResult::outcome)
                .returns(4L, DefinitionOperationResult::revision)
                .returns(true, DefinitionOperationResult::dirty)
                .satisfies(result -> assertThat(result.diagnostics())
                        .singleElement()
                        .returns("property.integer", diagnostic -> diagnostic.code()));
        assertThat(malformed)
                .returns("validation-rejected", DefinitionOperationResult::outcome)
                .returns(4L, DefinitionOperationResult::revision)
                .returns(true, DefinitionOperationResult::dirty)
                .satisfies(result -> assertThat(result.diagnostics())
                        .singleElement()
                        .returns("property.kind", diagnostic -> diagnostic.code()));
        String source = Files.readString(temporaryDirectory.resolve("worlds/main.scene.json"));
        assertThat(source).contains("\"precision\":1.25", "\"title\":\"Player\"");

        InspectorSnapshot refreshed = Objects.requireNonNull(service.readInspector(
                        new InspectorReadParams(
                                1L, 4L, definition.roots().getFirst().target()),
                        Locale.ENGLISH)
                .snapshot());
        assertThat(property(refreshed, "count").state().effectiveValue())
                .isEqualTo(new InspectorSnapshot.NumberValue("number", "12345678901234567890"));
        assertThat(property(refreshed, "count").state().modified()).isTrue();
        assertThat(property(refreshed, "precision").state().effectiveValue())
                .isEqualTo(new InspectorSnapshot.NumberValue("number", "0.00000000000000000001"));
        assertThat(property(refreshed, "precision").state().modified()).isTrue();
        assertThat(property(refreshed, "title").state().effectiveValue())
                .isEqualTo(new InspectorSnapshot.TextValue("text", "Exact title"));
        assertThat(property(refreshed, "title").state().modified()).isTrue();
    }

    /** Accepts the selected descriptor path without encoding its filename in the protocol. */
    @Test
    void opensSelectedDescriptorPath() throws IOException {
        String descriptorName = "custom-game-name.j3d";
        AuthoringTestProject.write(temporaryDirectory, descriptorName);

        ProjectOpenResult result = service.openProject(
                new ProjectOpenParams(temporaryDirectory.resolve(descriptorName).toString()));

        assertThat(result.opened()).isTrue();
        assertThat(Objects.requireNonNull(result.project()).descriptor()).endsWith(descriptorName);
    }

    /** Explicit close releases the session without terminating the process-scoped service. */
    @Test
    void closesActiveProject() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();

        ProjectCloseResult result = service.closeProject();

        assertThat(result.closed()).isTrue();
        assertThat(result.invalidatedProjectGeneration()).isEqualTo(1L);
        assertThat(retained.isClosed()).isTrue();
        assertThat(service.activeSession()).isEmpty();
        assertThat(service.isClosed()).isFalse();
    }

    /** Closing with no project is an idempotent no-op. */
    @Test
    void closesWhenNoProjectIsOpen() {
        ProjectCloseResult result = service.closeProject();

        assertThat(result.closed()).isFalse();
        assertThat(result.invalidatedProjectGeneration()).isNull();
    }

    /** A close permits another project with a strictly newer session generation. */
    @Test
    void opensClosesAndReopens() throws IOException {
        Path first = temporaryDirectory.resolve("first");
        Path second = temporaryDirectory.resolve("second");
        AuthoringTestProject.write(first, "first.j3d", "First Project");
        AuthoringTestProject.write(second, "second.j3d", "Second Project");

        ProjectOpenResult firstResult = service.openProject(new ProjectOpenParams(first.toString()));
        service.closeProject();
        ProjectOpenResult secondResult = service.openProject(new ProjectOpenParams(second.toString()));

        assertThat(firstResult.projectGeneration()).isEqualTo(1L);
        assertThat(secondResult.projectGeneration()).isEqualTo(2L);
        assertThat(Objects.requireNonNull(secondResult.project()).name()).isEqualTo("Second Project");
    }

    /** A second open is rejected rather than silently replacing the retained session. */
    @Test
    void rejectsSecondOpenWhileProjectIsActive() throws IOException {
        Path first = temporaryDirectory.resolve("first");
        Path second = temporaryDirectory.resolve("second");
        AuthoringTestProject.write(first, "first.j3d");
        AuthoringTestProject.write(second, "second.j3d");
        service.openProject(new ProjectOpenParams(first.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();

        ProjectOpenResult rejected = service.openProject(new ProjectOpenParams(second.toString()));

        assertThat(rejected.opened()).isFalse();
        assertThat(rejected.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_ALREADY_OPEN);
        assertThat(service.activeSession()).containsSame(retained);
        assertThat(retained.isClosed()).isFalse();
    }

    /** Installs a normally loaded candidate under a new generation before disposing the old session. */
    @Test
    void replacesActiveProject() throws IOException {
        Path first = temporaryDirectory.resolve("first");
        Path second = temporaryDirectory.resolve("second");
        AuthoringTestProject.write(first, "first.j3d", "First Project");
        AuthoringTestProject.write(second, "jscene3d.json", "Second Project");
        ProjectOpenResult opened = service.openProject(new ProjectOpenParams(first.toString()));
        EditorProjectSession replaced = service.activeSession().orElseThrow();

        ProjectReplaceResult result = service.replaceProject(
                new ProjectReplaceParams(Objects.requireNonNull(opened.projectGeneration()), second.toString()));
        EditorProjectSession replacement = service.activeSession().orElseThrow();

        assertThat(result.outcome()).isEqualTo(ProjectReplaceResult.REPLACED);
        assertThat(result.projectGeneration()).isEqualTo(2L);
        assertThat(Objects.requireNonNull(result.project()).name()).isEqualTo("Second Project");
        assertThat(result.diagnostics())
                .singleElement()
                .returns("project.descriptor.legacy", diagnostic -> diagnostic.code());
        assertThat(replaced.isClosed()).isTrue();
        assertThat(replacement).isNotSameAs(replaced);
        assertThat(replacement.project().identity().name()).isEqualTo("Second Project");
        assertThat(service.closeProject().invalidatedProjectGeneration()).isEqualTo(2L);
    }

    /** Rejects an invalid candidate without changing dirty working-copy or history state. */
    @Test
    void preservesDirtyActiveProjectWhenCandidateIsInvalid() throws IOException {
        Path active = temporaryDirectory.resolve("active");
        Path invalid = temporaryDirectory.resolve("invalid");
        AuthoringTestProject.write(active, "active.j3d", "Active Project");
        ProjectOpenResult opened = service.openProject(new ProjectOpenParams(active.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();
        InspectorMutationTarget.EntityEnabled target = enabledTarget(retained);
        AssetId world = AssetId.from(AuthoringTestProject.SCENE_ASSET_ID);
        retained.mutate(world, target, new AuthoringMutation.Set(new ProjectValue.BooleanValue(false)), 0L);
        retained.mutate(world, target, new AuthoringMutation.Set(new ProjectValue.BooleanValue(true)), 1L);
        retained.undo(world, 2L);
        long revision = retained.definitionState(world).orElseThrow().revision();

        ProjectReplaceResult result = service.replaceProject(
                new ProjectReplaceParams(Objects.requireNonNull(opened.projectGeneration()), invalid.toString()));

        assertThat(result.outcome()).isEqualTo(ProjectReplaceResult.CANDIDATE_REJECTED);
        assertThat(result.projectGeneration()).isNull();
        assertThat(result.diagnostics())
                .singleElement()
                .returns("project.directory.missing", diagnostic -> diagnostic.code());
        assertThat(service.activeSession()).containsSame(retained);
        assertThat(retained.definitionState(world)).get().returns(revision, state -> state.revision());
        assertThat(retained.isDirty()).isTrue();
        assertThat(retained.activeHierarchy())
                .get()
                .satisfies(hierarchy ->
                        assertThat(hierarchy.roots().getFirst().isEnabled()).isFalse());
        assertThat(retained.definitionState(world)).get().returns(true, state -> state.canUndo());
        assertThat(retained.definitionState(world)).get().returns(true, state -> state.canRedo());
        retained.redo(world, revision);
        assertThat(retained.activeHierarchy())
                .get()
                .satisfies(hierarchy ->
                        assertThat(hierarchy.roots().getFirst().isEnabled()).isTrue());
        assertThat(service.closeProject().invalidatedProjectGeneration()).isEqualTo(1L);
    }

    /** Dirty state does not block a valid candidate from replacing and disposing the active session. */
    @Test
    void replacesDirtyActiveProject() throws IOException {
        Path active = temporaryDirectory.resolve("active");
        Path replacement = temporaryDirectory.resolve("replacement");
        AuthoringTestProject.write(active, "active.j3d", "Active Project");
        AuthoringTestProject.write(replacement, "replacement.j3d", "Replacement Project");
        ProjectOpenResult opened = service.openProject(new ProjectOpenParams(active.toString()));
        EditorProjectSession replaced = service.activeSession().orElseThrow();
        InspectorMutationTarget.EntityEnabled target = enabledTarget(replaced);
        replaced.mutate(
                AssetId.from(AuthoringTestProject.SCENE_ASSET_ID),
                target,
                new AuthoringMutation.Set(new ProjectValue.BooleanValue(false)),
                0L);

        ProjectReplaceResult result = service.replaceProject(
                new ProjectReplaceParams(Objects.requireNonNull(opened.projectGeneration()), replacement.toString()));

        assertThat(result.outcome()).isEqualTo(ProjectReplaceResult.REPLACED);
        assertThat(result.projectGeneration()).isEqualTo(2L);
        assertThat(Objects.requireNonNull(result.project()).name()).isEqualTo("Replacement Project");
        assertThat(replaced.isClosed()).isTrue();
        assertThat(service.activeSession())
                .get()
                .extracting(session -> session.project().identity().name())
                .isEqualTo("Replacement Project");
    }

    /** Rejects stale ownership before attempting to interpret or load the candidate path. */
    @Test
    void rejectsStaleGenerationWithoutLoadingCandidate() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();

        ProjectReplaceResult result = service.replaceProject(new ProjectReplaceParams(2L, "\0not-loaded"));

        assertThat(result.outcome()).isEqualTo(ProjectReplaceResult.CONFLICT);
        assertThat(result.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_GENERATION_CONFLICT);
        assertThat(result.diagnostics()).isEmpty();
        assertThat(service.activeSession()).containsSame(retained);
        assertThat(retained.isClosed()).isFalse();
        assertThat(service.closeProject().invalidatedProjectGeneration()).isEqualTo(1L);
    }

    /** Replacement requires an active project and does not attempt candidate loading otherwise. */
    @Test
    void rejectsReplacementWithoutActiveProject() {
        ProjectReplaceResult result = service.replaceProject(new ProjectReplaceParams(1L, "\0not-loaded"));

        assertThat(result.outcome()).isEqualTo(ProjectReplaceResult.CONFLICT);
        assertThat(result.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_NOT_OPEN);
        assertThat(service.activeSession()).isEmpty();
    }

    /** An invalid project remains a domain result with structured diagnostics. */
    @Test
    void reportsInvalidProject() {
        ProjectOpenResult result =
                service.openProject(new ProjectOpenParams(temporaryDirectory.toString()), Locale.FRENCH);

        assertThat(result.opened()).isFalse();
        assertThat(result.project()).isNull();
        assertThat(result.diagnostics())
                .singleElement()
                .returns("error", diagnostic -> diagnostic.severity())
                .returns("project.descriptor.missing", diagnostic -> diagnostic.code())
                .returns(
                        "Le dossier du projet ne contient aucun descripteur de projet",
                        diagnostic -> diagnostic.message())
                .satisfies(diagnostic -> assertThat(diagnostic.source()).startsWith("file:"));
        assertThat(service.activeSession()).isEmpty();
    }

    /** Candidate rejection resolves diagnostics through the same requested-locale path as open. */
    @Test
    void localizesCandidateRejectionDiagnostics() throws IOException {
        Path active = temporaryDirectory.resolve("active");
        Path invalid = temporaryDirectory.resolve("invalid");
        AuthoringTestProject.write(active, "active.j3d");
        ProjectOpenResult opened = service.openProject(new ProjectOpenParams(active.toString()), Locale.FRENCH);

        ProjectReplaceResult result = service.replaceProject(
                new ProjectReplaceParams(Objects.requireNonNull(opened.projectGeneration()), invalid.toString()),
                Locale.FRENCH);

        assertThat(result.outcome()).isEqualTo(ProjectReplaceResult.CANDIDATE_REJECTED);
        assertThat(result.failureCode()).isNull();
        assertThat(result.diagnostics())
                .singleElement()
                .returns("error", diagnostic -> diagnostic.severity())
                .returns("project.directory.missing", diagnostic -> diagnostic.code())
                .returns(
                        "Le dossier du projet n’existe pas ou n’est pas un dossier", diagnostic -> diagnostic.message())
                .satisfies(diagnostic -> {
                    assertThat(diagnostic.source()).startsWith("file:");
                    assertThat(diagnostic.location()).isEmpty();
                    assertThat(diagnostic.details()).isNotNull();
                });
    }

    /** The legacy descriptor opens with its real path and deprecation diagnostic intact. */
    @Test
    void opensLegacyProjectWithDeprecationDiagnostic() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, "jscene3d.json");

        ProjectOpenResult result = service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

        assertThat(result.opened()).isTrue();
        assertThat(Objects.requireNonNull(result.project()).descriptor()).endsWith("jscene3d.json");
        assertThat(result.diagnostics())
                .singleElement()
                .returns("warning", diagnostic -> diagnostic.severity())
                .returns("project.descriptor.legacy", diagnostic -> diagnostic.code())
                .satisfies(diagnostic -> assertThat(diagnostic.source()).endsWith("jscene3d.json"));
    }

    /** Process-scoped close releases a retained session and rejects later operations. */
    @Test
    void cleansUpRetainedSessionOnServiceClose() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();

        service.close();

        assertThat(service.isClosed()).isTrue();
        assertThat(retained.isClosed()).isTrue();
        assertThatThrownBy(service::activeSession)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Creates an enabled-state mutation target for the fixture's local entity. */
    private static InspectorMutationTarget.EntityEnabled enabledTarget(EditorProjectSession session) {
        EditorHierarchyNode entity = session.retainDefinition(AssetId.from(AuthoringTestProject.SCENE_ASSET_ID))
                .definition()
                .orElseThrow()
                .hierarchy()
                .roots()
                .getFirst();
        return new InspectorMutationTarget.EntityEnabled(
                entity.occurrence(), EntityId.from(AuthoringTestProject.ENTITY_ID));
    }

    /** Mutates one scalar property using the Java-issued target from the initial Inspector snapshot. */
    @SuppressWarnings("NullAway") // The assertion fixture guarantees the projected mutation target is present.
    private DefinitionOperationResult mutate(
            InspectorSnapshot inspector,
            String propertyId,
            long revision,
            DefinitionMutationParams.CandidateValue value) {
        InspectorSnapshot.ComponentPropertyMutation target = (InspectorSnapshot.ComponentPropertyMutation)
                property(inspector, propertyId).mutationTarget();
        return service.mutateDefinition(
                new DefinitionMutationParams(
                        1L,
                        AuthoringTestProject.SCENE_ASSET_ID,
                        revision,
                        "set",
                        new DefinitionMutationParams.MutationTarget(
                                target.kind(),
                                target.occurrence(),
                                target.entityId(),
                                target.componentId(),
                                target.propertyId()),
                        value),
                Locale.ENGLISH);
    }

    /** Finds one scalar property in the fixture's registered component section. */
    private static InspectorSnapshot.Property property(InspectorSnapshot inspector, String propertyId) {
        return inspector.groups().stream()
                .flatMap(group -> group.properties().stream())
                .filter(property -> property.identity().equals(propertyId))
                .findFirst()
                .orElseThrow();
    }
}
