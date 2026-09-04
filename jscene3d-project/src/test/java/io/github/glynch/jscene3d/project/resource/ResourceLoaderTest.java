/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises safe native-resource loading without executing runtime factories. */
final class ResourceLoaderTest {
    private static final String PROJECT_MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "example.resource-test",
                "name": "Resource Test",
                "version": "1.0.0"
              },
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "example.resource-test",
                "entryScene": "main.scene.json"
              },
              "extensions": [
                {"id": "example.resource-test", "requires": "1.0.0"}
              ],
              "assets": [
                {
                  "id": "source-data",
                  "type": "example.resource-test/source-data",
                  "path": "assets/source.dat"
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private GameProject loadedProject;

    /** Creates one valid project for each resource test. */
    @BeforeEach
    void createProject() throws IOException {
        write("assets/source.dat", "source");
        write(ProjectLoader.MANIFEST_NAME, PROJECT_MANIFEST);
        loadedProject = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
    }

    /** Loads typed properties and every portable reference namespace. */
    @Test
    void loadsCompleteResourceDefinition() throws IOException {
        write("resources/dependency.resource.json", "{}");
        write("resources/material.resource.json", """
                {
                  "$schema": "../schema/resource-1.schema.json",
                  "schemaVersion": 1,
                  "type": "example.resource-test/material",
                  "typeVersion": 2,
                  "properties": {
                    "label": "Steel",
                    "roughness": 0.75,
                    "dependency": {"$ref": "project:resources/dependency.resource.json"},
                    "source": {"$ref": "asset:source-data"},
                    "imported": {"$ref": "import:materials/output/main"}
                  }
                }
                """);

        ResourceLoadResult result =
                new ResourceLoader().load(loadedProject, Path.of("resources/material.resource.json"));

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.isValid()).isTrue();
        ResourceDefinition resource = result.resource().orElseThrow();
        assertThat(resource.source())
                .isEqualTo(temporaryDirectory
                        .resolve("resources/material.resource.json")
                        .toRealPath());
        assertThat(resource.type()).isEqualTo(new RegisteredType("example.resource-test/material", 2));
        assertThat(resource.properties()).containsEntry("label", new ProjectValue.TextValue("Steel"));
        assertThat(resource.properties())
                .containsEntry("roughness", new ProjectValue.NumberValue(new BigDecimal("0.75")));
        assertThat(reference(resource, "dependency").kind()).isEqualTo(ResourceReference.Kind.PROJECT);
        assertThat(reference(resource, "source")).isEqualTo(ResourceReference.asset("source-data"));
        assertThat(reference(resource, "imported")).isEqualTo(ResourceReference.imported("materials/output/main"));
    }

    /** Collects independent schema, type, property, and reference errors. */
    @Test
    void collectsSemanticResourceErrors() throws IOException {
        write("resources/invalid.resource.json", """
                {
                  "$schema": "wrong.json",
                  "schemaVersion": 2,
                  "type": "bad",
                  "typeVersion": 0,
                  "properties": {
                    "source": {"$ref": "asset:missing"},
                    "broken": {"$ref": 42, "other": true}
                  }
                }
                """);

        ResourceLoadResult result =
                new ResourceLoader().load(loadedProject, Path.of("resources/invalid.resource.json"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.resource()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(ProjectDiagnostic::code)
                .containsExactly(
                        "resource.schema.unsupported",
                        "resource.schema.uri",
                        "resource.type.identifier",
                        "resource.type.version",
                        "resource.reference.asset.missing",
                        "resource.reference.object");
    }

    /** Rejects unknown document fields through strict JSON parsing. */
    @Test
    void rejectsUnknownResourceField() throws IOException {
        write("resources/unknown.resource.json", """
                {
                  "schemaVersion": 1,
                  "type": "example.resource-test/data",
                  "typeVersion": 1,
                  "unknown": true
                }
                """);

        ResourceLoadResult result =
                new ResourceLoader().load(loadedProject, Path.of("resources/unknown.resource.json"));

        assertThat(result.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo("resource.json");
    }

    /** Confines resource loading to regular files within the project root. */
    @Test
    void rejectsMissingAndEscapingResourcePaths() {
        ResourceLoader loader = new ResourceLoader();

        ResourceLoadResult missing = loader.load(loadedProject, Path.of("resources/missing.resource.json"));
        ResourceLoadResult escaping = loader.load(loadedProject, Path.of("../outside.resource.json"));

        assertThat(missing.diagnostics()).extracting(ProjectDiagnostic::code).containsExactly("resource.file.missing");
        assertThat(escaping.diagnostics()).extracting(ProjectDiagnostic::code).containsExactly("resource.path.escape");
    }

    /** Publishes the Resource version-one schema for editors and validation tools. */
    @Test
    void bundlesVersionOneResourceSchema() throws IOException {
        String resourcePath = "/META-INF/jscene3d/project/resource-1.schema.json";

        try (var input = getClass().getResourceAsStream(resourcePath)) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("\"$id\": \"https://jscene3d.org/schemas/resource-1.json\"")
                    .contains("\"typeVersion\"")
                    .contains("\"properties\"");
        }
    }

    /** Returns one decoded reference property. */
    private static ResourceReference reference(ResourceDefinition resource, String property) {
        ProjectValue value = resource.properties().get(property);
        return ((ProjectValue.ReferenceValue) Objects.requireNonNull(value)).reference();
    }

    /** Writes one UTF-8 project test file. */
    private void write(String relativePath, String content) throws IOException {
        Path target = temporaryDirectory.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
}
