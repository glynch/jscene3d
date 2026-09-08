/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        assertThat(session.assets())
                .extracting(EditorAssetItem::label)
                .containsExactly("Reusable Beacon", "Test World");
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
                    .returns(EditorHierarchyNode.Kind.LOCAL_ENTITY, EditorHierarchyNode::kind)
                    .satisfies(child -> assertThat(child.entityId())
                            .hasValueSatisfying(id -> assertThat(id.toString()).isEqualTo(CHILD_ID)));
        });
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

    /** Creates the complete valid source project used by the read-only loading test. */
    private void writeProject() throws IOException {
        write("project.json", """
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

    /** Creates the editor loader under test with this module's resource class loader. */
    private static EditorProjectLoader loader() {
        return new EditorProjectLoader("0.1.0-SNAPSHOT", EditorProjectLoaderTest.class.getClassLoader());
    }
}
