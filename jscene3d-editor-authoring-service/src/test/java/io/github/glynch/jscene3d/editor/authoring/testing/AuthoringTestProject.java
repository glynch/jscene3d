/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.testing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Writes deterministic minimal authoring projects for service and process tests. */
public final class AuthoringTestProject {
    /** Current descriptor filename used by the primary fixture. */
    public static final String DESCRIPTOR = "small-authoring-project.j3d";

    /** Stable local entity identity used by working-copy lifecycle tests. */
    public static final String ENTITY_ID = "0b295328-b5a3-4f41-9f34-e9b4abc430a7";

    /** Stable Scene asset identity. */
    public static final String SCENE_ASSET_ID = "e890c4c3-fb32-49d8-88b8-4e04e7a29656";

    /** Stable reusable entity-definition identity for definition-opening tests. */
    public static final String DEFINITION_ASSET_ID = "4ccdb339-9c5b-47d3-9b18-9169be5e4936";

    /** Stable scalar-component identity used by mutation tests. */
    public static final String SCALAR_COMPONENT_ID = "635b219c-7604-4d6f-a021-664a7d609195";

    private AuthoringTestProject() {}

    /**
     * Writes one valid project using either the current or legacy descriptor name.
     *
     * @param root fixture project root
     * @param descriptorName descriptor filename
     * @throws IOException when fixture files cannot be written
     */
    public static void write(Path root, String descriptorName) throws IOException {
        write(root, descriptorName, "Small Authoring Project");
    }

    /**
     * Writes one valid project with a selectable author-facing name.
     *
     * @param root fixture project root
     * @param descriptorName descriptor filename
     * @param projectName author-facing project name
     * @throws IOException when fixture files cannot be written
     */
    public static void write(Path root, String descriptorName, String projectName) throws IOException {
        writeFile(root, descriptorName, String.format(Locale.ROOT, """
                {
                  "$schema":"https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion":1,
                  "identity":{"id":"example.authoring-test","name":"%s","version":"1.2.3"},
                  "engine":{"requires":">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime":{"applicationExtension":"example.authoring-test","mainScene":{"assetId":"e890c4c3-fb32-49d8-88b8-4e04e7a29656","pathHint":"worlds/main.scene.json"}},
                  "extensions":[{"id":"example.authoring-test","requires":">=1.0.0 <2.0.0"}]
                }
                """, projectName));
        writeFile(root, "src/main/resources/META-INF/jscene3d/extension.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.authoring-test",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Authoring Test",
                  "types":[],
                  "components":[]
                }
                """);
        writeFile(root, "worlds/main.scene.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/scene-definition-1.json",
                  "assetId":"e890c4c3-fb32-49d8-88b8-4e04e7a29656",
                  "assetType":"scene-definition",
                  "formatVersion":1,
                  "name":"Opening Scene",
                  "connections":[],
                  "roots":[{
                    "entryType":"local",
                    "entityId":"0b295328-b5a3-4f41-9f34-e9b4abc430a7",
                    "name":"Player",
                    "enabled":true,
                    "components":[],
                    "children":[]
                  }]
                }
                """);
    }

    /**
     * Writes a valid project which additionally requires one installed extension descriptor.
     *
     * @param root fixture project root
     * @param descriptorName descriptor filename
     * @param extensionId required installed extension identity
     * @throws IOException when fixture files cannot be written
     */
    public static void writeRequiringInstalledExtension(Path root, String descriptorName, String extensionId)
            throws IOException {
        write(root, descriptorName);
        writeFile(root, descriptorName, String.format(Locale.ROOT, """
                {
                  "$schema":"https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion":1,
                  "identity":{"id":"example.authoring-test","name":"Small Authoring Project","version":"1.2.3"},
                  "engine":{"requires":">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime":{"applicationExtension":"example.authoring-test","mainScene":{"assetId":"e890c4c3-fb32-49d8-88b8-4e04e7a29656","pathHint":"worlds/main.scene.json"}},
                  "extensions":[
                    {"id":"example.authoring-test","requires":">=1.0.0 <2.0.0"},
                    {"id":"%s","requires":">=1.0.0 <2.0.0"}
                  ]
                }
                """, extensionId));
    }

    /**
     * Writes descriptor-only installed extension metadata with an unusable runtime provider declaration.
     *
     * @param root installed metadata root
     * @param extensionId installed extension identity
     * @throws IOException when fixture files cannot be written
     */
    public static void writeInstalledExtensionMetadata(Path root, String extensionId) throws IOException {
        writeFile(root, "META-INF/jscene3d/extension.json", String.format(Locale.ROOT, """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"%s",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Installed Authoring Test",
                  "types":[],
                  "components":[]
                }
                """, extensionId));
        writeFile(
                root,
                "META-INF/services/io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension",
                "class.which.must.not.be.loaded.MissingRuntimeExtension\n");
    }

    /** Writes one additional authored reusable entity definition. */
    public static void writeEntityDefinition(Path root) throws IOException {
        writeFile(root, "entities/reusable.entity.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/entity-definition-1.json",
                  "assetId":"4ccdb339-9c5b-47d3-9b18-9169be5e4936",
                  "assetType":"entity-definition",
                  "formatVersion":1,
                  "name":"Reusable",
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
                    "entityId":"7be5cdf0-1bc7-481c-8880-3d627e14d52d",
                    "name":"Reusable Root",
                    "enabled":true,
                    "components":[],
                    "children":[]
                  }
                }
                """);
    }

    /**
     * Writes a project whose component descriptor declares one Euler rotation property.
     *
     * @param root fixture project root
     * @throws IOException when fixture files cannot be written
     */
    public static void writeEulerRotationProject(Path root) throws IOException {
        write(root, DESCRIPTOR);
        writeFile(root, "src/main/resources/META-INF/jscene3d/extension.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.authoring-test",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Authoring Test",
                  "types":[],
                  "components":[{
                    "id":"example.authoring-test/rotation",
                    "typeVersion":1,
                    "displayName":"Rotation",
                    "properties":[{
                      "id":"rotation",
                      "valueKind":"array",
                      "elementKind":"number",
                      "exactElementCount":3,
                      "defaultValue":[0,90.0000000000000000001,-2.5],
                      "displayName":"Rotation",
                      "editor":{"semantic":"euler-rotation"}
                    }]
                  }]
                }
                """);
        writeFile(root, "worlds/main.scene.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/scene-definition-1.json",
                  "assetId":"e890c4c3-fb32-49d8-88b8-4e04e7a29656",
                  "assetType":"scene-definition",
                  "formatVersion":1,
                  "name":"Opening Scene",
                  "connections":[],
                  "roots":[{
                    "entryType":"local",
                    "entityId":"0b295328-b5a3-4f41-9f34-e9b4abc430a7",
                    "name":"Player",
                    "enabled":true,
                    "components":[{
                      "componentId":"635b219c-7604-4d6f-a021-664a7d609195",
                      "type":"example.authoring-test/rotation",
                      "typeVersion":1,
                      "properties":{}
                    }],
                    "children":[]
                  }]
                }
                """);
    }

    /** Writes a Scene containing every built-in visual projection supported by the safe Scene View. */
    public static void writeSceneViewProject(Path root) throws IOException {
        write(root, DESCRIPTOR);
        writeFile(root, "worlds/main.scene.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/scene-definition-1.json",
                  "assetId":"e890c4c3-fb32-49d8-88b8-4e04e7a29656",
                  "assetType":"scene-definition",
                  "formatVersion":1,
                  "name":"Opening Scene",
                  "connections":[],
                  "roots":[{
                    "entryType":"local",
                    "entityId":"0b295328-b5a3-4f41-9f34-e9b4abc430a7",
                    "name":"Visual Root",
                    "enabled":true,
                    "components":[{
                      "componentId":"11111111-1111-4111-8111-111111111111",
                      "type":"io.github.glynch.jscene3d.spatial3d/transform-3d",
                      "typeVersion":1,
                      "properties":{
                        "position":[1.25,2,3],
                        "orientation":[10,20,30],
                        "scale":[2,3,4]
                      }
                    },{
                      "componentId":"22222222-2222-4222-8222-222222222222",
                      "type":"io.github.glynch.jscene3d.spatial3d/mesh-renderer-3d",
                      "typeVersion":1,
                      "properties":{
                        "mesh":{"$ref":"asset:test-mesh"},
                        "material":{"$ref":"asset:test-material"},
                        "visible":true
                      }
                    }],
                    "children":[{
                      "entryType":"local",
                      "entityId":"33333333-3333-4333-8333-333333333333",
                      "enabled":false,
                      "components":[{
                        "componentId":"55555555-5555-4555-8555-555555555555",
                        "type":"io.github.glynch.jscene3d.spatial3d/transform-3d",
                        "typeVersion":1,
                        "properties":{}
                      },{
                        "componentId":"44444444-4444-4444-8444-444444444444",
                        "type":"io.github.glynch.jscene3d.spatial3d/directional-light-3d",
                        "typeVersion":1,
                        "properties":{
                          "color":[0.25,0.5,0.75],
                          "intensity":3.5,
                          "target":[7,8,9]
                        }
                      }],
                      "children":[]
                    }]
                  }]
                }
                """);
    }

    /**
     * Writes a project containing each scalar property supported by the first editable Inspector slice.
     *
     * @param root fixture project root
     * @throws IOException when fixture files cannot be written
     */
    public static void writeScalarPropertyProject(Path root) throws IOException {
        write(root, DESCRIPTOR);
        writeFile(root, "src/main/resources/META-INF/jscene3d/extension.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.authoring-test",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Authoring Test",
                  "types":[],
                  "components":[{
                    "id":"example.authoring-test/scalars",
                    "typeVersion":1,
                    "displayName":"Scalars",
                    "properties":[
                      {"id":"visible","valueKind":"boolean","required":true,"displayName":"Visible"},
                      {"id":"count","valueKind":"number","required":true,"displayName":"Count","editor":{"semantic":"integer","minimum":0}},
                      {"id":"precision","valueKind":"number","required":true,"displayName":"Precision"},
                      {"id":"title","valueKind":"text","required":true,"displayName":"Title"}
                    ]
                  }]
                }
                """);
        writeFile(root, "worlds/main.scene.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/scene-definition-1.json",
                  "assetId":"e890c4c3-fb32-49d8-88b8-4e04e7a29656",
                  "assetType":"scene-definition",
                  "formatVersion":1,
                  "name":"Opening Scene",
                  "connections":[],
                  "roots":[{
                    "entryType":"local",
                    "entityId":"0b295328-b5a3-4f41-9f34-e9b4abc430a7",
                    "name":"Player",
                    "enabled":true,
                    "components":[{
                      "componentId":"635b219c-7604-4d6f-a021-664a7d609195",
                      "type":"example.authoring-test/scalars",
                      "typeVersion":1,
                      "properties":{"visible":true,"count":1,"precision":1.25,"title":"Player"}
                    }],
                    "children":[]
                  }]
                }
                """);
    }

    /** Writes one UTF-8 fixture file below the project root. */
    private static void writeFile(Path root, String relativePath, String content) throws IOException {
        Path path = root.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
