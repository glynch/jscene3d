/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.game.GameLoopSettings;
import io.github.glynch.jscene3d.game.GameRuntime;
import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises project opening through safe metadata and service-loaded executable contributions. */
final class ProjectRuntimeLoaderTest {
    private static final String PROJECT = """
            {
              "$schema": "https://jscene3d.org/schemas/project-1.json",
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.runtime-test-project",
                "name": "Runtime Test Project",
                "version": "1.0.0"
              },
              "engine": {
                "requires": ">=0.1.0-SNAPSHOT <0.2.0"
              },
              "runtime": {
                "applicationExtension": "io.github.glynch.runtime-test",
                "entryScene": "application/main.scene.json"
              },
              "extensions": [
                {
                  "id": "io.github.glynch.runtime-test",
                  "requires": "1.0.0"
                }
              ]
            }
            """;
    private static final String RESOURCE_TYPE_PREFIX = "io.github.glynch.runtime-test/";
    private static final String SCENE = """
            {
              "$schema": "https://jscene3d.org/schemas/scene-1.json",
              "schemaVersion": 1,
              "id": "main",
              "root": {
                "id": "root",
                "type": "io.github.glynch.runtime-test/group-3d",
                "typeVersion": 1,
                "children": [
                  {
                    "id": "timer",
                    "type": "io.github.glynch.runtime-test/timer",
                    "typeVersion": 1
                  },
                  {
                    "id": "indicator",
                    "type": "io.github.glynch.runtime-test/indicator-3d",
                    "typeVersion": 1,
                    "properties": {
                      "active": true
                    },
                    "controller": {
                      "type": "io.github.glynch.runtime-test/toggle-controller",
                      "typeVersion": 1
                    }
                  }
                ]
              },
              "connections": [
                {
                  "from": {
                    "node": "timer",
                    "signal": "timeout"
                  },
                  "to": {
                    "node": "indicator",
                    "action": "toggle"
                  }
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private GameProject loadedProject;

    /** Creates a fresh valid project and resets service-provider observations. */
    @BeforeEach
    void createProject() throws IOException {
        TestRuntimeState.reset();
        write(ProjectLoader.MANIFEST_NAME, PROJECT);
        write("application/main.scene.json", SCENE);
        loadedProject = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
    }

    /** Opens, runs, routes, and closes a complete headless scene through the public seam. */
    @Test
    void composesExecutableSceneAndIntegratesWithGameRuntime() {
        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.isOpen()).isTrue();
        ProjectRuntime application = result.runtime().orElseThrow();
        assertRuntimeTree(application);
        TestRuntimeExtension.IndicatorObject indicator = (TestRuntimeExtension.IndicatorObject)
                application.findNode("indicator").orElseThrow().object();
        assertThat(indicator.isActive()).isTrue();

        GameLoopSettings settings = GameLoopSettings.builder()
                .fixedStep(Duration.ofMillis(10L))
                .maximumFrameTime(Duration.ofMillis(40L))
                .maximumFixedUpdates(4)
                .build();
        try (GameRuntime runtime = new GameRuntime(application, settings)) {
            runtime.start();
            runtime.advance(Duration.ofMillis(10L), ActionSnapshot.empty());
            runtime.render();
            assertThat(indicator.isActive()).isFalse();
        }

        assertThat(TestRuntimeState.EVENTS)
                .containsExactly(
                        "start:root",
                        "start:timer",
                        "start:indicator",
                        "start:controller",
                        "fixed:0",
                        "toggle:false",
                        "frame:1",
                        "render:1",
                        "close:controller",
                        "close:indicator",
                        "close:timer",
                        "close:root");
    }

    /** Returns structured validation errors before any executable factory is invoked. */
    @Test
    void rejectsUnknownRegisteredSceneTypeBeforeComposition() throws IOException {
        write("application/main.scene.json", SCENE.replace("group-3d", "missing-3d"));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.isOpen()).isFalse();
        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("scene.catalog.type.missing");
        assertThat(TestRuntimeState.EVENTS).isEmpty();
    }

    /** Requires the manifest's application extension to have an executable provider. */
    @Test
    void reportsMissingApplicationRuntimeProvider() {
        ProjectRuntimeLoadResult result = new ProjectRuntimeLoader("0.1.0-SNAPSHOT")
                .load(loadedProject, Thread.currentThread().getContextClassLoader());

        assertThat(result.isOpen()).isFalse();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("runtime.extension.application.missing");
    }

    /** Converts invalid trusted registration into a structured loading failure. */
    @Test
    void reportsFactoryRegisteredAgainstWrongDescriptorScope() {
        ProjectRuntimeLoadResult result = loadWithExtensions(new WrongScopeExtension());

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("runtime.extension.registration");
    }

    /** Reports a used type that its executable extension failed to bind. */
    @Test
    void reportsMissingSceneNodeFactory() {
        ProjectRuntimeLoadResult result = loadWithExtensions(new EmptyRuntimeExtension());

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("runtime.factory.scene-node.missing");
    }

    /** Diagnoses project-system data explicitly until that runtime slice is implemented. */
    @Test
    void reportsUnsupportedProjectSystems() throws IOException {
        write("game/systems.json", "{}");
        write(
                ProjectLoader.MANIFEST_NAME,
                PROJECT.replace(
                        "\"entryScene\": \"application/main.scene.json\"",
                        "\"entryScene\": \"application/main.scene.json\",\n"
                                + "    \"projectSystems\": \"game/systems.json\""));
        reloadProject();

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("runtime.project-systems.unsupported");
    }

    /** Does not silently flatten nested scene instances before instance semantics exist. */
    @Test
    void reportsUnsupportedNestedSceneInstance() throws IOException {
        write("application/nested.scene.json", "{}");
        write("application/main.scene.json", """
                {
                  "$schema": "https://jscene3d.org/schemas/scene-1.json",
                  "schemaVersion": 1,
                  "id": "main",
                  "root": {
                    "id": "nested",
                    "instance": "nested.scene.json"
                  }
                }
                """);

        ProjectRuntimeLoadResult result = loadWithExtensions(new EmptyRuntimeExtension());

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .contains("runtime.scene-instance.unsupported");
    }

    /** Shares one canonical resource value across nodes and closes it once after scene objects. */
    @Test
    void sharesAndOwnsResolvedProjectResources() throws IOException {
        write("resources/dependency.resource.json", resource("shared-data", "Dependency", null));
        write(
                "resources/shared.resource.json",
                resource("shared-data", "Shared value", "resources/dependency.resource.json"));
        write("application/main.scene.json", resourceScene("resources/shared.resource.json", true));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.diagnostics()).isEmpty();
        ProjectRuntime runtime = result.runtime().orElseThrow();
        TestRuntimeExtension.ResourceConsumerObject first = consumer(runtime, "first");
        TestRuntimeExtension.ResourceConsumerObject second = consumer(runtime, "second");
        assertThat(first.resource()).isSameAs(second.resource());
        assertThat(first.resource().label()).isEqualTo("Shared value");
        TestRuntimeExtension.SharedData dependency =
                first.resource().dependency().orElseThrow();
        assertThat(dependency.label()).isEqualTo("Dependency");

        runtime.close();
        runtime.close();

        assertThat(first.resource().closeCount()).isOne();
        assertThat(dependency.closeCount()).isOne();
        assertThat(TestRuntimeState.EVENTS)
                .endsWith(
                        "close:second",
                        "close:first",
                        "close:root",
                        "close-resource:Shared value",
                        "close-resource:Dependency");
    }

    /** Reports a complete project-relative dependency cycle as a terminal resource diagnostic. */
    @Test
    void reportsResourceDependencyCycle() throws IOException {
        write("resources/first.resource.json", resource("shared-data", "First", "resources/second.resource.json"));
        write("resources/second.resource.json", resource("shared-data", "Second", "resources/first.resource.json"));
        write("application/main.scene.json", resourceScene("resources/first.resource.json", false));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("runtime.resource.cycle");
            assertThat(diagnostic.message())
                    .contains("resources/first.resource.json -> resources/second.resource.json"
                            + " -> resources/first.resource.json");
        });
    }

    /** Preserves native resource loading diagnostics without invoking a factory. */
    @Test
    void reportsInvalidResourceDocument() throws IOException {
        String invalid = resource("shared-data", "Invalid", null).replace("\"typeVersion\": 1", "\"typeVersion\": 0");
        write("resources/invalid.resource.json", invalid);
        write("application/main.scene.json", resourceScene("resources/invalid.resource.json", false));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("resource.type.version");
    }

    /** Rejects a registered type whose descriptor does not have resource scope. */
    @Test
    void reportsResourceDescriptorWithWrongScope() throws IOException {
        write("resources/wrong-scope.resource.json", resource("group-3d", "Wrong scope", null));
        write("application/main.scene.json", resourceScene("resources/wrong-scope.resource.json", false));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("resource.catalog.type.scope");
    }

    /** Reports a resource descriptor that its executable extension did not bind. */
    @Test
    void reportsMissingResourceFactory() throws IOException {
        write("resources/unbound.resource.json", resource("unbound-data", "Unbound", null));
        write("application/main.scene.json", resourceScene("resources/unbound.resource.json", false));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("runtime.factory.resource.missing");
    }

    /** Verifies the requested Java value type at the resource-consumer boundary. */
    @Test
    void reportsResourceRuntimeValueTypeMismatch() throws IOException {
        write("resources/text.resource.json", resource("text-data", "Text", null));
        write("application/main.scene.json", resourceScene("resources/text.resource.json", false));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("runtime.resource.value.type");
    }

    /** Reports portable reference namespaces whose runtime adapter is not implemented yet. */
    @Test
    void reportsUnsupportedRuntimeResourceNamespace() throws IOException {
        write("application/main.scene.json", resourceSceneReference("import:test/output", false));

        ProjectRuntimeLoadResult result = loadWithTestExtension();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .containsExactly("runtime.resource.kind.unsupported");
    }

    /** Verifies tree identity, authored order, parent views, and descriptor defaults. */
    private static void assertRuntimeTree(ProjectRuntime runtime) {
        assertThat(runtime.project()).isNotNull();
        assertThat(runtime.scene().id()).isEqualTo("main");
        assertThat(runtime.root().definition().id()).isEqualTo("root");
        assertThat(runtime.root().parent()).isEmpty();
        assertThat(runtime.root().children())
                .extracting(node -> node.definition().id())
                .containsExactly("timer", "indicator");
        assertThat(runtime.findNode("missing")).isEmpty();
        assertThat(runtime.findNode("indicator").orElseThrow().controller()).isPresent();
        assertThat(TestRuntimeState.rootLabel).isEqualTo("Default root");
        assertThat(TestRuntimeState.timerParent).isEqualTo("root");
    }

    /** Returns one resource-consuming runtime node object. */
    private static TestRuntimeExtension.ResourceConsumerObject consumer(ProjectRuntime runtime, String nodeId) {
        return (TestRuntimeExtension.ResourceConsumerObject)
                runtime.findNode(nodeId).orElseThrow().object();
    }

    /** Creates one typed resource document with an optional dependency. */
    private static String resource(String type, String label, @Nullable String dependency) {
        String dependencyProperty =
                dependency == null ? "" : ",\n    \"dependency\": {\"$ref\": \"project:" + dependency + "\"}";
        return String.format(Locale.ROOT, """
                {
                  "schemaVersion": 1,
                  "type": "%s",
                  "typeVersion": 1,
                  "properties": {
                    "label": "%s"%s
                  }
                }
                """, RESOURCE_TYPE_PREFIX + type, label, dependencyProperty);
    }

    /** Creates a scene containing one or two nodes referencing the same resource path. */
    private static String resourceScene(String resourcePath, boolean includeSecond) {
        return resourceSceneReference("project:" + resourcePath, includeSecond);
    }

    /** Creates a scene containing one or two nodes using one complete resource reference. */
    private static String resourceSceneReference(String reference, boolean includeSecond) {
        String second = includeSecond ? String.format(Locale.ROOT, """
                        ,
                                {
                                  "id": "second",
                                  "type": "io.github.glynch.runtime-test/resource-consumer-3d",
                                  "typeVersion": 1,
                                  "properties": {
                                    "resource": {"$ref": "%s"}
                                  }
                                }
                        """, reference) : "";
        return String.format(Locale.ROOT, """
                {
                  "schemaVersion": 1,
                  "id": "resource-scene",
                  "root": {
                    "id": "root",
                    "type": "io.github.glynch.runtime-test/group-3d",
                    "typeVersion": 1,
                    "children": [
                      {
                        "id": "first",
                        "type": "io.github.glynch.runtime-test/resource-consumer-3d",
                        "typeVersion": 1,
                        "properties": {
                          "resource": {"$ref": "%s"}
                        }
                      }%s
                    ]
                  }
                }
                """, reference, second);
    }

    /** Resolves safe metadata independently, then supplies one trusted implementation explicitly. */
    private ProjectRuntimeLoadResult loadWithTestExtension() {
        ExtensionCatalogLoadResult catalog = loadCatalog();
        assertThat(catalog.diagnostics()).isEmpty();
        return new ProjectRuntimeLoader("0.1.0-SNAPSHOT")
                .load(loadedProject, catalog.catalog(), List.of(new TestRuntimeExtension()));
    }

    /** Loads with explicit executable providers against the test descriptor catalog. */
    private ProjectRuntimeLoadResult loadWithExtensions(ProjectRuntimeExtension... extensions) {
        RegisteredTypeCatalog catalog = loadCatalog().catalog();
        return new ProjectRuntimeLoader("0.1.0-SNAPSHOT").load(loadedProject, catalog, List.of(extensions));
    }

    /** Loads the safe test descriptor through the production catalog boundary. */
    private ExtensionCatalogLoadResult loadCatalog() {
        return new ExtensionCatalogLoader("0.1.0-SNAPSHOT")
                .load(loadedProject, Thread.currentThread().getContextClassLoader());
    }

    /** Reloads the current manifest after a test modifies it. */
    private void reloadProject() {
        loadedProject = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
    }

    /** Writes one UTF-8 test project file. */
    private void write(String relativePath, String content) throws IOException {
        Path target = temporaryDirectory.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    /** Application provider intentionally contributing no factories. */
    private static final class EmptyRuntimeExtension implements ProjectRuntimeExtension {
        @Override
        public String id() {
            return "io.github.glynch.runtime-test";
        }

        @Override
        public void register(ProjectRuntimeRegistry registry) {
            assertThat(registry).isNotNull();
        }
    }

    /** Provider intentionally binding a scene type through the controller registration method. */
    private static final class WrongScopeExtension implements ProjectRuntimeExtension {
        @Override
        public String id() {
            return "io.github.glynch.runtime-test";
        }

        @Override
        public void register(ProjectRuntimeRegistry registry) {
            RegisteredType group = new RegisteredType("io.github.glynch.runtime-test/group-3d", 1);
            registry.registerNodeController(group, ignored -> new NoOpRuntimeObject());
        }
    }

    /** Minimal object used only by a deliberately invalid registration. */
    private static final class NoOpRuntimeObject implements ProjectRuntimeObject {
        @Override
        public void start() {
            TestRuntimeState.EVENTS.add("unexpected-start");
        }

        @Override
        public void close() {
            TestRuntimeState.EVENTS.add("unexpected-close");
        }
    }
}
