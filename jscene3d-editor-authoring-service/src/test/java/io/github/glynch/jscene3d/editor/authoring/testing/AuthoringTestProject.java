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
                  "runtime":{"applicationExtension":"example.authoring-test","entryScene":"worlds/main.world.json"},
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
        writeFile(root, "worlds/main.world.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/world-definition-1.json",
                  "assetId":"e890c4c3-fb32-49d8-88b8-4e04e7a29656",
                  "assetType":"world-definition",
                  "formatVersion":1,
                  "name":"Opening World",
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
                  "runtime":{"applicationExtension":"example.authoring-test","entryScene":"worlds/main.world.json"},
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

    /** Writes one UTF-8 fixture file below the project root. */
    private static void writeFile(Path root, String relativePath, String content) throws IOException {
        Path path = root.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
