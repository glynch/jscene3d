/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises resource-only extension discovery and catalog construction. */
final class ExtensionCatalogLoaderTest {
    private static final String VALID_DESCRIPTOR = """
            {
              "$schema": "https://jscene3d.org/schemas/extension-1.json",
              "schemaVersion": 1,
              "id": "example.game",
              "version": "1.2.0",
              "engineRequires": ">=0.1.0-SNAPSHOT <0.2.0",
              "displayName": "Example Game",
              "description": "Test extension metadata.",
              "types": [
                {
                  "id": "example.game/group-3d",
                  "typeVersion": 1,
                  "scope": "scene-node",
                  "displayName": "Group 3d",
                  "properties": [
                    {
                      "id": "visible",
                      "valueKind": "boolean",
                      "defaultValue": true,
                      "displayName": "Visible",
                      "editor": {"group": "Rendering"}
                    },
                    {
                      "id": "mesh",
                      "valueKind": "reference",
                      "displayName": "Mesh",
                      "acceptedReferences": ["project", "import"]
                    }
                  ],
                  "signals": [
                    {
                      "id": "selected",
                      "payload": {"type": "example.game/selection", "typeVersion": 1},
                      "displayName": "Selected"
                    }
                  ],
                  "actions": [
                    {"id": "show", "displayName": "Show"}
                  ],
                  "requiredCapabilities": ["org.jscene3d.render/mesh-3d"]
                },
                {
                  "id": "example.game/selection",
                  "typeVersion": 1,
                  "scope": "resource",
                  "displayName": "Selection"
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    /** Discovers descriptor resources without asking the supplied loader for implementation classes. */
    @Test
    void discoversDescriptorWithoutLoadingExtensionClasses() throws IOException {
        Path classPathRoot = descriptorRoot("classpath", VALID_DESCRIPTOR);

        try (TrackingClassLoader classLoader = classLoader(classPathRoot)) {
            ExtensionCatalogLoadResult result = new ExtensionCatalogLoader("0.1.0-SNAPSHOT")
                    .load(project("example.game", ">=1.0.0 <2.0.0"), classLoader);

            assertThat(result.isComplete()).isTrue();
            assertThat(result.diagnostics()).isEmpty();
            assertThat(result.catalog().extensions()).singleElement().satisfies(extension -> {
                assertThat(extension.id()).isEqualTo("example.game");
                assertThat(extension.version()).isEqualTo("1.2.0");
                assertThat(extension.presentation().description()).contains("Test extension metadata.");
            });
            assertGroupType(result.catalog());
            assertThat(classLoader.extensionClassLoads()).isZero();
        }
    }

    /** Reports a manifest error when a declared extension has no descriptor resource. */
    @Test
    void reportsMissingDeclaredExtension() throws IOException {
        try (TrackingClassLoader classLoader = classLoader(temporaryDirectory.resolve("empty"))) {
            ExtensionCatalogLoadResult result =
                    new ExtensionCatalogLoader("0.1.0").load(project("example.missing", "1.0.0"), classLoader);

            assertThat(result.isComplete()).isFalse();
            assertThat(result.catalog().extensions()).isEmpty();
            assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
                assertThat(diagnostic.code().code()).isEqualTo("extension.missing");
                assertThat(diagnostic.location()).isEqualTo("/extensions/0");
                assertThat(diagnostic.source())
                        .isEqualTo(temporaryDirectory.resolve("project.json").toUri());
            });
        }
    }

    /** Keeps the first sorted descriptor while reporting duplicate extension identities. */
    @Test
    void reportsDuplicateDescriptorIdentityDeterministically() throws IOException {
        Path first = descriptorRoot("a-first", VALID_DESCRIPTOR);
        Path second = descriptorRoot("z-second", VALID_DESCRIPTOR.replace("Example Game", "Later Copy"));

        try (TrackingClassLoader classLoader = classLoader(second, first)) {
            ExtensionCatalogLoadResult result =
                    new ExtensionCatalogLoader("0.1.0").load(project("example.game", "1.2.0"), classLoader);

            assertThat(result.isComplete()).isFalse();
            assertThat(result.catalog().extensions())
                    .singleElement()
                    .extracting(extension -> extension.presentation().displayName())
                    .isEqualTo("Example Game");
            assertThat(result.diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .containsExactly("extension.duplicate");
        }
    }

    /** Ignores duplicate enumeration of the same physical descriptor resource. */
    @Test
    void deduplicatesRepeatedDescriptorUrl() throws IOException {
        Path classPathRoot = descriptorRoot("repeated", VALID_DESCRIPTOR);
        URL descriptorUrl = classPathRoot
                .resolve(ExtensionCatalogLoader.DESCRIPTOR_RESOURCE)
                .toUri()
                .toURL();
        ClassLoader classLoader = new RepeatingResourceClassLoader(descriptorUrl);

        ExtensionCatalogLoadResult result =
                new ExtensionCatalogLoader("0.1.0").load(project("example.game", "1.2.0"), classLoader);

        assertThat(result.isComplete()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.catalog().extensions())
                .singleElement()
                .extracting(ExtensionDescriptor::id)
                .isEqualTo("example.game");
    }

    /** Rejects malformed descriptor JSON and then reports the unresolved declaration. */
    @Test
    void reportsMalformedDescriptorJson() throws IOException {
        Path classPathRoot = descriptorRoot("broken", "{\"schemaVersion\": 1,");

        try (TrackingClassLoader classLoader = classLoader(classPathRoot)) {
            ExtensionCatalogLoadResult result =
                    new ExtensionCatalogLoader("0.1.0").load(project("example.game", "1.2.0"), classLoader);

            assertThat(result.catalog().extensions()).isEmpty();
            assertThat(result.diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .containsExactly("extension.json", "extension.missing");
        }
    }

    /** Rejects descriptors incompatible with the engine before catalog selection. */
    @Test
    void reportsEngineIncompatibility() throws IOException {
        String incompatible = VALID_DESCRIPTOR.replace(">=0.1.0-SNAPSHOT <0.2.0", ">=2.0.0");
        Path classPathRoot = descriptorRoot("incompatible", incompatible);

        try (TrackingClassLoader classLoader = classLoader(classPathRoot)) {
            ExtensionCatalogLoadResult result =
                    new ExtensionCatalogLoader("0.1.0").load(project("example.game", "1.2.0"), classLoader);

            assertThat(result.catalog().extensions()).isEmpty();
            assertThat(result.diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .containsExactly("extension.engine.incompatible", "extension.missing");
        }
    }

    /** Rejects a discovered version outside the project's declared requirement. */
    @Test
    void reportsProjectRequirementMismatch() throws IOException {
        Path classPathRoot = descriptorRoot("classpath", VALID_DESCRIPTOR);

        try (TrackingClassLoader classLoader = classLoader(classPathRoot)) {
            ExtensionCatalogLoadResult result =
                    new ExtensionCatalogLoader("0.1.0").load(project("example.game", ">=2.0.0"), classLoader);

            assertThat(result.catalog().extensions()).isEmpty();
            assertThat(result.diagnostics())
                    .singleElement()
                    .extracting(diagnostic -> diagnostic.code().code())
                    .isEqualTo("extension.version.incompatible");
        }
    }

    /** Collects independent descriptor errors without exposing a partially valid descriptor. */
    @Test
    void collectsDescriptorSemanticErrors() throws IOException {
        Path classPathRoot = descriptorRoot("invalid", """
                {
                  "$schema": "https://example.com/wrong.json",
                  "schemaVersion": 1,
                  "id": "example.game",
                  "version": "1.0.0",
                  "engineRequires": ">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName": "Example Game",
                  "types": [
                    null,
                    {
                      "id": "example.game/actor-3d",
                      "typeVersion": 1,
                      "scope": "scene-node",
                      "displayName": "Actor 3d",
                      "description": " ",
                      "properties": [
                        null,
                        {
                          "id": "Bad Id",
                          "valueKind": "mystery",
                          "displayName": " ",
                          "editor": [],
                          "acceptedReferences": ["project"]
                        },
                        {
                          "id": "health",
                          "valueKind": "number",
                          "required": true,
                          "defaultValue": 100,
                          "displayName": "Health"
                        },
                        {"id": "health", "valueKind": "number", "displayName": "Health"}
                      ],
                      "signals": [
                        null,
                        {"id": "ready", "displayName": "Ready"},
                        {"id": "ready", "displayName": "Ready again"}
                      ],
                      "actions": [
                        {
                          "id": "apply",
                          "displayName": "Apply",
                          "payload": {"type": "bad", "typeVersion": 0}
                        }
                      ],
                      "requiredCapabilities": [
                        "bad",
                        "example.game/capability",
                        "example.game/capability"
                      ]
                    },
                    {
                      "id": "example.game/actor-3d",
                      "typeVersion": 1,
                      "scope": "scene-node",
                      "displayName": "Duplicate Actor"
                    }
                  ]
                }
                """);

        try (TrackingClassLoader classLoader = classLoader(classPathRoot)) {
            ExtensionCatalogLoadResult result =
                    new ExtensionCatalogLoader("0.1.0-SNAPSHOT").load(project("example.game", "1.0.0"), classLoader);

            assertThat(result.catalog().extensions()).isEmpty();
            assertThat(result.diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .contains(
                            "extension.schema.uri",
                            "extension.field.required",
                            "extension.field.blank",
                            "extension.property.kind",
                            "extension.editor.object",
                            "extension.property.reference-kind",
                            "extension.property.required-default",
                            "extension.property.duplicate",
                            "extension.endpoint.duplicate",
                            "extension.endpoint.payload",
                            "extension.type.version",
                            "extension.capability.id",
                            "extension.capability.duplicate",
                            "extension.type.duplicate",
                            "extension.missing");
        }
    }

    /** Converts class-path enumeration failures into structured diagnostics. */
    @Test
    void reportsDescriptorEnumerationFailure() {
        ExtensionCatalogLoadResult result = new ExtensionCatalogLoader("0.1.0")
                .load(project("example.game", "1.0.0"), new FailingResourceClassLoader());

        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("extension.discovery.read", "extension.missing");
    }

    /** Treats an invalid configured engine version as a caller error. */
    @Test
    void rejectsInvalidEngineVersion() {
        assertThatThrownBy(() -> new ExtensionCatalogLoader("latest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("engineVersion must be a semantic version");
    }

    /** Publishes the descriptor schema as a JPMS Module resource. */
    @Test
    void bundlesVersionOneDescriptorSchema() throws IOException {
        String resource = "/META-INF/jscene3d/project/extension-1.schema.json";

        try (var input = getClass().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("\"$id\": \"https://jscene3d.org/schemas/extension-1.json\"")
                    .contains("\"node-controller\"")
                    .contains("\"acceptedReferences\"");
        }
    }

    /** Verifies representative type, property, endpoint, and capability metadata. */
    private static void assertGroupType(RegisteredTypeCatalog catalog) {
        RegisteredTypeDescriptor type =
                catalog.find(new RegisteredType("example.game/group-3d", 1)).orElseThrow();
        PropertyDescriptor visible = Objects.requireNonNull(type.properties().get("visible"));
        EndpointDescriptor selected = Objects.requireNonNull(type.signals().get("selected"));

        assertThat(type.scope()).isEqualTo(RegisteredTypeScope.SCENE_NODE);
        assertThat(type.requiredCapabilities()).containsExactly("org.jscene3d.render/mesh-3d");
        assertThat(visible.defaultValue()).contains(new ProjectValue.BooleanValue(true));
        assertThat(visible.editorMetadata()).containsEntry("group", new ProjectValue.TextValue("Rendering"));
        assertThat(selected.payload()).contains(new RegisteredType("example.game/selection", 1));
        assertThat(type.actions()).containsKey("show");
    }

    /** Creates a minimal valid project declaring one extension requirement. */
    private GameProject project(String extensionId, String requirement) {
        Path root = temporaryDirectory.toAbsolutePath().normalize();
        GameProject.Identity identity = new GameProject.Identity(
                "example.project",
                "Example Project",
                "1.0.0",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        GameProject.Metadata metadata = new GameProject.Metadata(
                identity, List.of(), GameProject.Links.empty(), GameProject.Legal.empty(), GameProject.Catalog.empty());
        return new GameProject(
                root,
                metadata,
                new GameProject.EngineCompatibility(">=0.1.0 <0.2.0", Optional.empty()),
                new GameProject.RuntimeConfiguration(
                        extensionId, root.resolve("main.scene.json"), Optional.empty(), Optional.empty()),
                List.of(new GameProject.ExtensionRequirement(extensionId, requirement)),
                new GameProject.ProjectFiles(List.of(), List.of(), List.of()));
    }

    /** Writes one descriptor at the fixed discovery path under a class-path root. */
    private Path descriptorRoot(String name, String descriptor) throws IOException {
        Path root = temporaryDirectory.resolve(name);
        Path resource = root.resolve(ExtensionCatalogLoader.DESCRIPTOR_RESOURCE);
        Files.createDirectories(resource.getParent());
        Files.writeString(resource, descriptor, StandardCharsets.UTF_8);
        return root;
    }

    /** Creates an isolated class loader over the supplied resource roots. */
    private static TrackingClassLoader classLoader(Path... roots) throws IOException {
        URL[] urls = new URL[roots.length];
        for (int index = 0; index < roots.length; index++) {
            urls[index] = roots[index].toUri().toURL();
        }
        return new TrackingClassLoader(urls);
    }

    /** Class loader that records attempts to load project extension implementation classes. */
    private static final class TrackingClassLoader extends URLClassLoader {
        private int extensionClassLoads;

        /** Creates an isolated descriptor class loader. */
        private TrackingClassLoader(URL[] urls) {
            super(urls, null);
        }

        /** Records implementation-class requests while retaining normal URL loading behavior. */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("example.")) {
                extensionClassLoads++;
            }
            return super.loadClass(name, resolve);
        }

        /** Returns how many extension implementation classes were requested. */
        private int extensionClassLoads() {
            return extensionClassLoads;
        }
    }

    /** Class loader that simulates an unreadable dependency class path. */
    private static final class FailingResourceClassLoader extends ClassLoader {
        /** Fails fixed-resource enumeration. */
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            throw new IOException("simulated failure");
        }
    }

    /** Class loader that returns one physical descriptor URL more than once. */
    private static final class RepeatingResourceClassLoader extends ClassLoader {
        private final URL descriptorUrl;

        /** Creates a loader for the repeated descriptor URL. */
        private RepeatingResourceClassLoader(URL descriptorUrl) {
            super(null);
            this.descriptorUrl = descriptorUrl;
        }

        /** Repeats the fixed descriptor resource while leaving all other resources absent. */
        @Override
        public Enumeration<URL> getResources(String name) {
            if (ExtensionCatalogLoader.DESCRIPTOR_RESOURCE.equals(name)) {
                return Collections.enumeration(List.of(descriptorUrl, descriptorUrl));
            }
            return Collections.emptyEnumeration();
        }
    }
}
