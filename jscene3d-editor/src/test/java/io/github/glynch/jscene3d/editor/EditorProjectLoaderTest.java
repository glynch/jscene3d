/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopyId;
import io.github.glynch.jscene3d.telemetry.Telemetry;
import io.github.glynch.jscene3d.telemetry.TelemetryMeasurement;
import io.github.glynch.jscene3d.telemetry.TelemetryOperation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the headless project-to-editor-session seam with real project files. */
final class EditorProjectLoaderTest {
    private static final String ENTITY_ASSET_ID = "4c189475-9845-4810-b7f9-af744d3cc726";
    private static final String PLACEMENT_ID = "853f50a0-17dc-46ac-9f04-f772e54c44b2";
    private static final String CHILD_ID = "b991ca3e-66bb-4ef0-a682-74773bbef0d0";

    @TempDir
    private Path temporaryDirectory;

    /** Loads project metadata, assets, and a placement hierarchy without resolving application classes. */
    @Test
    void opensProjectAsReadOnlyEditorSession() throws IOException {
        writeProject();

        EditorProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.session())
                .withFailMessage(() -> result.diagnostics().toString())
                .isPresent();
        assertThat(result.diagnostics()).isEmpty();
        EditorProjectSession session = result.session().orElseThrow();
        assertThat(session.project().identity().name()).isEqualTo("Editor Test");
        assertThat(session.types().extensions())
                .extracting(extension -> extension.id())
                .contains("io.github.glynch.jscene3d.game3d", "io.github.glynch.jscene3d.presentation");
        assertThat(session.assets()).extracting(ProjectAsset::label).containsExactly("Reusable Beacon", "Test World");
        assertThat(session.hierarchy().label()).isEqualTo("Test World");
        assertThat(session.hierarchy().children()).singleElement().satisfies(placement -> {
            assertThat(placement.kind()).isEqualTo(EditorHierarchyNode.Kind.PLACEMENT);
            assertThat(placement.label()).isEqualTo("Beacon A");
            assertThat(placement.entityId())
                    .hasValueSatisfying(id -> assertThat(id.toString()).isEqualTo(PLACEMENT_ID));
            assertThat(placement.definitionId())
                    .hasValueSatisfying(id -> assertThat(id.toString()).isEqualTo(ENTITY_ASSET_ID));
            assertThat(placement.children())
                    .singleElement()
                    .returns("Lamp", EditorHierarchyNode::label)
                    .returns(EditorHierarchyNode.Kind.GENERATED_ENTITY, EditorHierarchyNode::kind)
                    .satisfies(child -> assertThat(child.selection().details().orElseThrow())
                            .returns("Lamp", EditorDetails::title)
                            .satisfies(details -> assertThat(details.decorations())
                                    .singleElement()
                                    .satisfies(icon -> {
                                        assertThat(icon.id()).isEqualTo(EditorIcons.READ_ONLY);
                                        assertThat(icon.tooltip()).contains("Generated content");
                                    }))
                            .satisfies(inspection -> assertThat(inspection.sections())
                                    .extracting(EditorDetails.Section::title)
                                    .contains("Transform 3D", "Perspective Camera 3D")))
                    .satisfies(child -> assertThat(child.entityId())
                            .hasValueSatisfying(id -> assertThat(id.toString()).isEqualTo(CHILD_ID)));
        });
        assertThat(session.assets())
                .extracting(item -> item.selection().details().orElseThrow().kind())
                .containsExactly("Entity definition", "World definition");
    }

    /** Preserves manifest diagnostics when a directory cannot become an editor session. */
    @Test
    void reportsInvalidProjectWithoutCreatingSession() {
        EditorProjectLoadResult result = loader().load(temporaryDirectory.resolve("missing"));

        assertThat(result.session()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .returns(
                        "project.directory.missing",
                        diagnostic -> diagnostic.code().code());
    }

    /** Records each material loading phase beneath the caller-owned operation. */
    @Test
    void recordsProjectLoadingPhases() throws IOException {
        writeProject();
        List<TelemetryMeasurement> measurements = new ArrayList<>();
        Telemetry telemetry = Telemetry.recording(measurements::add);

        try (TelemetryOperation operation = telemetry.begin("test.project.load", Map.of())) {
            EditorProjectLoadResult result = loader().load(temporaryDirectory, operation);
            assertThat(result.session()).isPresent();
        }

        assertThat(measurements)
                .extracting(TelemetryMeasurement::name)
                .containsExactly(
                        "project.manifest.load",
                        "project.asset-catalog.scan",
                        "project.extensions.load",
                        "project.import-definitions.load",
                        "project.published-content.load",
                        "project.assets.validate",
                        "project.startup-world.load",
                        "project.hierarchy.project",
                        "test.project.load");
    }

    /** Reports the authored identity and every visible loading phase in execution order. */
    @Test
    void reportsVisibleProjectLoadingProgress() throws IOException {
        writeProject();
        RecordingProgress progress = new RecordingProgress();

        try (TelemetryOperation operation = Telemetry.disabled().begin("test.project.load", Map.of())) {
            EditorProjectLoadResult result = loader().load(temporaryDirectory, operation, progress);
            assertThat(result.session()).isPresent();
        }

        assertThat(progress.projectNames).containsExactly("Editor Test");
        assertThat(progress.phases)
                .containsExactly(
                        EditorLoadingPhase.READING_MANIFEST,
                        EditorLoadingPhase.SCANNING_ASSETS,
                        EditorLoadingPhase.LOADING_EXTENSIONS,
                        EditorLoadingPhase.READING_IMPORTS,
                        EditorLoadingPhase.LOADING_PUBLISHED_CONTENT,
                        EditorLoadingPhase.VALIDATING_ASSETS,
                        EditorLoadingPhase.LOADING_STARTUP_WORLD,
                        EditorLoadingPhase.BUILDING_HIERARCHY);
    }

    /** Reads the engine version filtered into the built editor artifact. */
    @Test
    void readsEmbeddedEngineVersion() {
        assertThat(EditorBuildInfo.engineVersion()).isEqualTo("0.1.0-SNAPSHOT");
    }

    /** Composes spatial components while the deliberately missing application provider remains unloaded. */
    @Test
    void composesEditorSafeSpatialPreview() throws IOException {
        writeProject();
        EditorProjectSession session =
                loader().load(temporaryDirectory).session().orElseThrow();

        EditorWorldPreviewLoadResult result = EditorWorldPreview.compose(session);

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.preview()).isPresent();
        try (EditorWorldPreview preview = result.preview().orElseThrow()) {
            assertThat(preview.isReady()).isTrue();
        }
    }

    /** Discovers installed extension descriptors without adding their implementation classes to the editor module. */
    @Test
    void discoversInstalledExtensionMetadata() throws IOException {
        writeProject();
        write("jscene3d.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion":1,
                  "identity":{"id":"example.editor-test","name":"Editor Test","version":"1.0.0"},
                  "engine":{"requires":">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime":{"applicationExtension":"example.editor-test","entryScene":"worlds/test.world.json"},
                  "extensions":[
                    {"id":"example.editor-test","requires":">=1.0.0 <2.0.0"},
                    {"id":"example.installed","requires":">=1.0.0 <2.0.0"}
                  ]
                }
                """);
        Path installed = temporaryDirectory.resolve("installed-extension");
        writeInstalledDescriptor(installed);
        EditorProjectLoader projectLoader = new EditorProjectLoader(
                "0.1.0-SNAPSHOT", EditorProjectLoaderTest.class.getClassLoader(), List.of(installed));

        EditorProjectLoadResult result = projectLoader.load(temporaryDirectory);

        assertThat(result.session())
                .withFailMessage(() -> result.diagnostics().toString())
                .isPresent();
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.session().orElseThrow().types().extensions())
                .extracting(extension -> extension.id())
                .contains("example.installed");
    }

    /** Checks a valid project through the editor's non-graphical load and preview command. */
    @Test
    void checksProjectWithoutOpeningGraphicalEditor() throws IOException {
        writeProject();
        EditorProjectLoader projectLoader = loader();

        assertThatCode(() -> EditorProjectCheck.check(projectLoader, temporaryDirectory))
                .doesNotThrowAnyException();
    }

    /** Rejects a project which cannot produce an editor session. */
    @Test
    void rejectsInvalidProjectCheck() {
        Path missing = temporaryDirectory.resolve("missing");
        EditorProjectLoader projectLoader = loader();

        assertThatThrownBy(() -> EditorProjectCheck.check(projectLoader, missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("project load failed");
    }

    /** Parses the dedicated headless-check argument without consuming ordinary project arguments. */
    @Test
    void recognizesProjectCheckArgument() {
        assertThat(EditorProjectCheck.requestedProject(
                        new String[] {temporaryDirectory.toString(), "--check-project=project"}))
                .contains(Path.of("project").toAbsolutePath().normalize());
        assertThat(EditorProjectCheck.requestedProject(new String[] {temporaryDirectory.toString()}))
                .isEmpty();
        String[] emptyProjectArgument = {"--check-project="};
        assertThatThrownBy(() -> EditorProjectCheck.requestedProject(emptyProjectArgument))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a project directory");
    }

    /** Prefers the editor-owned cache when it contains published imports. */
    @Test
    void resolvesEditorProjectCache() throws IOException {
        Path projectCache = temporaryDirectory.resolve(".jscene3d/cache");
        Files.createDirectories(projectCache.resolve("imports"));
        Files.createDirectories(temporaryDirectory.resolve(".jscene3d/published/imports"));

        assertThat(EditorProjectLoader.resolvePublishedContentRoot(temporaryDirectory))
                .isEqualTo(projectCache);
    }

    /** Prefers a portable project-configured cache over the conventional editor cache. */
    @Test
    void resolvesConfiguredEditorProjectCache() throws IOException {
        write(".jscene3d/settings.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/project-settings-1.json",
                  "schemaVersion":1,
                  "cache":{"location":".cache/editor"}
                }
                """);
        Path configuredCache = temporaryDirectory.resolve(".cache/editor");
        Files.createDirectories(configuredCache.resolve("imports"));
        Files.createDirectories(temporaryDirectory.resolve(".jscene3d/cache/imports"));

        assertThat(EditorProjectLoader.resolvePublishedContentRoot(temporaryDirectory))
                .isEqualTo(configuredCache);
    }

    /** Rejects invalid shared settings instead of silently using a machine-dependent path. */
    @Test
    void rejectsInvalidProjectSettings() throws IOException {
        writeProject();
        write(".jscene3d/settings.json", """
                {"schemaVersion":1,"cache":{"location":"../outside"}}
                """);

        EditorProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.session()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("project.settings.invalid");
    }

    /** Edits, undoes, redoes, and atomically persists an authored enabled property. */
    @Test
    void editsAndSavesAuthoredEnabledProperty() throws IOException {
        writeProject();
        EditorProjectSession session =
                loader().load(temporaryDirectory).session().orElseThrow();
        EditorHierarchyNode placement = session.hierarchy().children().getFirst();
        EditorDetails.Property enabled =
                placement.selection().details().orElseThrow().sections().getFirst().properties().stream()
                        .filter(property -> property.identity().equals("enabled"))
                        .findFirst()
                        .orElseThrow();

        assertThat(enabled.editor()).isPresent();
        enabled.editor().orElseThrow().setValue("false");

        assertThat(session.isDirty()).isTrue();
        assertThat(session.canUndo()).isTrue();
        assertThat(session.canRedo()).isFalse();
        assertThat(session.hierarchy().children().getFirst().isEnabled()).isFalse();
        assertThat(session.hierarchy().children().getFirst().isModified()).isTrue();

        session.undo();
        assertThat(session.isDirty()).isFalse();
        assertThat(session.hierarchy().children().getFirst().isEnabled()).isTrue();
        assertThat(session.hierarchy().children().getFirst().isModified()).isFalse();
        assertThat(session.canRedo()).isTrue();

        session.redo();
        assertThat(session.hierarchy().children().getFirst().isModified()).isTrue();
        session.save();

        assertThat(session.isDirty()).isFalse();
        assertThat(session.hierarchy().children().getFirst().isModified()).isFalse();
        assertThat(Files.readString(temporaryDirectory.resolve("worlds/test.world.json")))
                .contains("\"enabled\" : false");
        assertThat(loader().load(temporaryDirectory)
                        .session()
                        .orElseThrow()
                        .hierarchy()
                        .children()
                        .getFirst()
                        .isEnabled())
                .isFalse();
    }

    /** Publishes typed working-copy state and tags undo history with its affected resource. */
    @Test
    void publishesWorldWorkingCopyLifecycle() throws IOException {
        writeProject();
        EditorProjectSession session =
                loader().load(temporaryDirectory).session().orElseThrow();
        AtomicInteger hierarchyChanges = new AtomicInteger();
        AtomicInteger contentChanges = new AtomicInteger();
        AtomicInteger dirtyChanges = new AtomicInteger();
        AtomicInteger saves = new AtomicInteger();
        session.onDidChangeHierarchy().subscribe(ignored -> hierarchyChanges.incrementAndGet());
        session.workingCopies().onDidChangeContent().subscribe(ignored -> contentChanges.incrementAndGet());
        session.workingCopies().onDidChangeDirty().subscribe(ignored -> dirtyChanges.incrementAndGet());
        session.workingCopies().onDidSave().subscribe(ignored -> saves.incrementAndGet());
        EditorWorkingCopyId worldId = new EditorWorkingCopyId(
                temporaryDirectory
                        .resolve("worlds/test.world.json")
                        .toRealPath()
                        .toUri(),
                "world");
        EditorWorkingCopy worldWorkingCopy =
                session.workingCopies().find(worldId).orElseThrow();

        EditorDetails.Property enabled = session
                .hierarchy()
                .children()
                .getFirst()
                .selection()
                .details()
                .orElseThrow()
                .sections()
                .getFirst()
                .properties()
                .stream()
                .filter(property -> property.identity().equals("enabled"))
                .findFirst()
                .orElseThrow();
        enabled.editor().orElseThrow().setValue("false");

        assertThat(worldWorkingCopy.isDirty()).isTrue();
        assertThat(session.workingCopies().dirtyWorkingCopies()).containsExactly(worldWorkingCopy);
        assertThat(session.workingCopies().dirtyCount()).isEqualTo(1);
        assertThat(session.undoEntry()).hasValueSatisfying(entry -> {
            assertThat(entry.label()).isEqualTo("Set entity enabled");
            assertThat(entry.workingCopyId()).isEqualTo(worldId);
        });

        session.undo();
        session.redo();
        session.save();

        assertThat(session.workingCopies().hasDirty()).isFalse();
        assertThat(hierarchyChanges).hasValue(4);
        assertThat(contentChanges).hasValue(3);
        assertThat(dirtyChanges).hasValue(4);
        assertThat(saves).hasValue(1);
    }

    /** Resolves the portable import snapshot supplied by a Project Workspace Archive. */
    @Test
    void resolvesPortablePublishedImports() throws IOException {
        Path publishedContent = temporaryDirectory.resolve(".jscene3d/published");
        Files.createDirectories(publishedContent.resolve("imports"));

        assertThat(EditorProjectLoader.resolvePublishedContentRoot(temporaryDirectory))
                .isEqualTo(publishedContent);
    }

    /** Retains compatibility with Maven workspaces until editor-owned caches replace them. */
    @Test
    void resolvesLegacyMavenImportCache() {
        assertThat(EditorProjectLoader.resolvePublishedContentRoot(temporaryDirectory))
                .isEqualTo(temporaryDirectory.resolve("target/import-cache"));
    }

    /** Creates the complete valid source project used by the read-only loading test. */
    private void writeProject() throws IOException {
        write("jscene3d.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion":1,
                  "identity":{"id":"example.editor-test","name":"Editor Test","version":"1.0.0"},
                  "engine":{"requires":">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime":{"applicationExtension":"example.editor-test","entryScene":"worlds/test.world.json"},
                  "extensions":[{"id":"example.editor-test","requires":">=1.0.0 <2.0.0"}]
                }
                """);
        write("src/main/resources/META-INF/jscene3d/extension.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.editor-test",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Editor Test",
                  "types":[],
                  "components":[{
                    "id":"example.editor-test/inert-behavior",
                    "typeVersion":1,
                    "displayName":"Inert Behavior",
                    "properties":[],
                    "actions":[{
                      "id":"invoke",
                      "displayName":"Invoke"
                    }],
                    "updatePhases":["before-physics"]
                  }]
                }
                """);
        write(
                "src/main/resources/META-INF/services/"
                        + "io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension",
                "class.which.must.not.be.loaded.MissingRuntimeExtension\n");
        write("entities/beacon.entity.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/entity-definition-1.json",
                  "assetId":"4c189475-9845-4810-b7f9-af744d3cc726",
                  "assetType":"entity-definition",
                  "formatVersion":1,
                  "name":"Reusable Beacon",
                  "contract":{
                    "parameters":[],
                    "signals":[],
                    "actions":[],
                    "capabilities":[],
                    "attachments":[],
                    "resourceBindings":[]
                  },
                  "connections":[],
                  "root":{
                    "entryType":"local",
                    "entityId":"441eea33-ec8d-4405-aa9b-4bbd28cc7942",
                    "name":"Beacon",
                    "enabled":true,
                    "components":[{
                      "componentId":"6947ae19-3787-44a4-8a87-149022182770",
                      "type":"example.editor-test/inert-behavior",
                      "typeVersion":1,
                      "properties":{}
                    }],
                    "children":[{
                      "entryType":"local",
                      "entityId":"b991ca3e-66bb-4ef0-a682-74773bbef0d0",
                      "name":"Lamp",
                      "enabled":true,
                      "components":[{
                        "componentId":"d53eb01b-ea50-43fd-b012-c6df2277872a",
                        "type":"io.github.glynch.jscene3d.spatial3d/transform-3d",
                        "typeVersion":1,
                        "properties":{}
                      },{
                        "componentId":"9ab42328-8935-4d3d-850f-3872b624b90e",
                        "type":"io.github.glynch.jscene3d.spatial3d/perspective-camera-3d",
                        "typeVersion":1,
                        "properties":{"primary":true}
                      }],
                      "children":[]
                    }]
                  }
                }
                """);
        write("worlds/test.world.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/world-definition-1.json",
                  "assetId":"89508a65-a28a-4650-90a6-8042b936ca16",
                  "assetType":"world-definition",
                  "formatVersion":1,
                  "name":"Test World",
                  "connections":[],
                  "roots":[{
                    "entryType":"placement",
                    "entityId":"853f50a0-17dc-46ac-9f04-f772e54c44b2",
                    "name":"Beacon A",
                    "enabled":true,
                    "definition":{
                      "assetId":"4c189475-9845-4810-b7f9-af744d3cc726",
                      "pathHint":"entities/beacon.entity.json"
                    },
                    "arguments":{}
                  }]
                }
                """);
    }

    /** Writes one UTF-8 test project file below the temporary project root. */
    private void write(String relativePath, String content) throws IOException {
        Path path = temporaryDirectory.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /** Writes one descriptor into a stand-alone installed-extension metadata root. */
    private static void writeInstalledDescriptor(Path root) throws IOException {
        Path descriptor = root.resolve("META-INF/jscene3d/extension.json");
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.installed",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Installed Test Extension",
                  "types":[],
                  "components":[]
                }
                """, StandardCharsets.UTF_8);
    }

    /** Creates the editor loader under test with this module's resource class loader. */
    private static EditorProjectLoader loader() {
        return new EditorProjectLoader("0.1.0-SNAPSHOT", EditorProjectLoaderTest.class.getClassLoader());
    }

    /** Records background-safe progress callbacks for assertions. */
    private static final class RecordingProgress implements EditorProjectLoadProgress {
        private final List<String> projectNames = new ArrayList<>();
        private final List<EditorLoadingPhase> phases = new ArrayList<>();

        @Override
        public void projectIdentified(String projectName) {
            projectNames.add(projectName);
        }

        @Override
        public void phaseStarted(EditorLoadingPhase phase) {
            phases.add(phase);
        }
    }
}
