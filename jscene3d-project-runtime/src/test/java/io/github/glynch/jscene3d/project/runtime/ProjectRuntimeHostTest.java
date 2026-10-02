/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.extension.ApplicationRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises manifest-driven project hosting and application-provider discovery. */
final class ProjectRuntimeHostTest {
    private static final String APPLICATION_ID = "io.github.glynch.host-test";
    private static final ComponentType COMPONENT_TYPE = ComponentType.of(APPLICATION_ID + "/behavior", 1);
    private static final ComponentId COMPONENT_ID = ComponentId.from("b54e497f-49f4-4678-8fb3-e14f5749f3b9");
    private static final String MAIN_SCENE_ID = "2f26576c-570d-4338-bc30-52bc41def3a5";
    private static final String EXPLICIT_SCENE_ID = "8a0187d4-87cd-4b86-9fd7-f8fedbfc3153";
    private static final String ENTITY_DEFINITION_ID = "3b6ab6ad-6cff-4f52-a849-16ae8a4e91c6";

    @TempDir
    private Path temporaryDirectory;

    /** Discovers the manifest-selected provider, composes its component, and runs preparation before activation. */
    @Test
    void loadsProjectThroughDiscoveredApplicationExtension() throws IOException {
        Path projectRoot = writeProject(MAIN_SCENE_ID, "worlds/main.scene.json");
        try (URLClassLoader loader = runtimeClassLoader()) {
            HostedProject hosted = host(loader, environment(new RecordingApplicationExtension(), null))
                    .load(projectRoot);
            RecordingComponent component = hosted.world()
                    .roots()
                    .getFirst()
                    .component(COMPONENT_ID, RecordingComponent.class)
                    .orElseThrow();

            assertThat(hosted.project().identity().id()).isEqualTo(APPLICATION_ID);
            assertThat(hosted.assets().assets()).hasSize(1);
            assertThat(hosted.world().isActive()).isFalse();
            assertThat(component.isPrepared()).isTrue();

            hosted.close();
            hosted.close();
            assertThat(hosted.world().isClosed()).isTrue();
        }
    }

    /** Reports stable truthful phases around the existing synchronous host boundary. */
    @Test
    void reportsProjectLoadPhases() throws IOException {
        Path projectRoot = writeProject(MAIN_SCENE_ID, "worlds/main.scene.json");
        List<ProjectLoadProgressReporter.Phase> phases = new ArrayList<>();
        try (URLClassLoader loader = runtimeClassLoader();
                HostedProject ignored = runtimeHost(loader, environment(new RecordingApplicationExtension(), null))
                        .load(projectRoot, phases::add)) {
            assertThat(phases).containsExactly(ProjectLoadProgressReporter.Phase.values());
        }
    }

    /** Resolves the Main Scene by AssetId even when its path hint is stale. */
    @Test
    void resolvesMainSceneByAssetIdRatherThanPathHint() throws IOException {
        Path projectRoot = writeProject(MAIN_SCENE_ID, "worlds/renamed-from.scene.json");
        try (URLClassLoader loader = runtimeClassLoader();
                HostedProject hosted = runtimeHost(loader, environment(new RecordingApplicationExtension(), null))
                        .load(projectRoot)) {
            assertThat(hosted.world().definition().name()).isEqualTo("Main");
        }
    }

    /** Loads an explicitly selected Scene independently of Main Scene and exposes playtest parameters. */
    @Test
    void loadsExplicitLaunchRequest() throws IOException {
        Path projectRoot = writeProject(MAIN_SCENE_ID, "worlds/main.scene.json");
        Files.writeString(
                projectRoot.resolve("worlds/menu.scene.json"),
                worldDefinition(EXPLICIT_SCENE_ID, "Menu"),
                StandardCharsets.UTF_8);
        ProjectLaunchRequest request = ProjectLaunchRequest.playtest(
                "moving-floor-34",
                AssetRef.to(AssetId.from(EXPLICIT_SCENE_ID), "worlds/menu.scene.json"),
                Map.of("example.invulnerable", new ProjectValue.BooleanValue(true)));

        try (URLClassLoader loader = runtimeClassLoader();
                HostedProject hosted = runtimeHost(loader, environment(new RecordingApplicationExtension(), null))
                        .load(projectRoot, request)) {
            assertThat(hosted.world().definition().name()).isEqualTo("Menu");
            assertThat(hosted.project().runtime().mainScene())
                    .get()
                    .extracting(reference -> reference.id().toString())
                    .isEqualTo(MAIN_SCENE_ID);
            assertThat(hosted.launchRequest()).isSameAs(request);
            assertThat(hosted.launchRequest().parameter("example.invulnerable"))
                    .contains(new ProjectValue.BooleanValue(true));
        }
    }

    /** Rejects invalid manifest data while retaining the loader's structured diagnostics. */
    @Test
    void reportsManifestDiagnostics() throws IOException {
        Path projectRoot = temporaryDirectory.resolve("invalid-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("jscene3d.json"), "{}\n", StandardCharsets.UTF_8);
        ProjectHost runtimeHost = host(getClass().getClassLoader(), emptyEnvironment());

        assertThatExceptionOfType(ProjectHostException.class)
                .isThrownBy(() -> runtimeHost.load(projectRoot))
                .satisfies(failure -> {
                    assertThat(failure).hasMessageContaining("project manifest loading failed");
                    assertThat(failure.diagnostics()).isNotEmpty();
                });
    }

    /** Closes environment adapters which a failed composition never takes into world ownership. */
    @Test
    void closesWorldModulesAfterMissingRuntimeProvider() throws IOException {
        Path projectRoot = writeProject(MAIN_SCENE_ID, "worlds/main.scene.json");
        RecordingWorldModule module = new RecordingWorldModule();
        ProjectRuntimeEnvironment environment = environment(null, module);
        try (URLClassLoader loader = runtimeClassLoader()) {
            ProjectHost runtimeHost = host(loader, environment);

            assertThatExceptionOfType(ProjectHostException.class)
                    .isThrownBy(() -> runtimeHost.load(projectRoot))
                    .satisfies(failure -> assertThat(failure).hasMessageContaining("Scene composition failed"));
        }

        assertThat(module.isClosed()).isTrue();
    }

    /** Closes the composed world when manifest-selected application preparation fails. */
    @Test
    void closesWorldAfterApplicationPreparationFailure() throws IOException {
        Path projectRoot = writeProject(MAIN_SCENE_ID, "worlds/main.scene.json");
        RecordingWorldModule module = new RecordingWorldModule();
        try (URLClassLoader loader = runtimeClassLoader()) {
            ProjectRuntimeEnvironment environment = environment(new FailingApplicationExtension(), module);
            ProjectHost runtimeHost = host(loader, environment);

            assertThatThrownBy(() -> runtimeHost.load(projectRoot))
                    .isInstanceOf(ProjectHostException.class)
                    .hasMessageContaining("application runtime preparation failed")
                    .hasCauseInstanceOf(IllegalStateException.class);
        }

        assertThat(module.isClosed()).isTrue();
    }

    /** Rejects a Main Scene identity which does not identify a catalogued definition. */
    @Test
    void rejectsUncataloguedMainScene() throws IOException {
        Path projectRoot = writeProject("00000000-0000-4000-8000-000000000001", "worlds/missing.scene.json");
        try (URLClassLoader loader = runtimeClassLoader()) {
            ProjectHost runtimeHost = host(loader, emptyEnvironment());

            assertThatExceptionOfType(ProjectHostException.class)
                    .isThrownBy(() -> runtimeHost.load(projectRoot))
                    .satisfies(failure -> {
                        assertThat(failure).hasMessageContaining("selected Scene is absent from the asset catalog");
                        assertThat(failure.diagnostics())
                                .singleElement()
                                .satisfies(diagnostic -> assertThat(
                                                diagnostic.code().code())
                                        .isEqualTo("project.runtime.scene.missing"));
                    });
        }
    }

    /** Rejects a Main Scene identity which resolves to a non-Scene asset. */
    @Test
    void rejectsWrongKindMainScene() throws IOException {
        Path projectRoot = writeProject(ENTITY_DEFINITION_ID, "entities/not-a-scene.entity.json");
        Files.createDirectories(projectRoot.resolve("entities"));
        Files.writeString(
                projectRoot.resolve("entities/not-a-scene.entity.json"),
                String.format(Locale.ROOT, """
                        {
                          "$schema": "https://jscene3d.org/schemas/entity-definition-1.json",
                          "assetId": "%s",
                          "assetType": "entity-definition",
                          "formatVersion": 1,
                          "name": "Not a Scene"
                        }
                        """, ENTITY_DEFINITION_ID),
                StandardCharsets.UTF_8);
        try (URLClassLoader loader = runtimeClassLoader()) {
            assertThatExceptionOfType(ProjectHostException.class)
                    .isThrownBy(() -> runtimeHost(loader, emptyEnvironment()).load(projectRoot))
                    .satisfies(failure -> assertThat(failure.diagnostics())
                            .singleElement()
                            .satisfies(diagnostic ->
                                    assertThat(diagnostic.code().code()).isEqualTo("project.runtime.scene.kind")));
        }
    }

    /** Rejects Run Project authoritatively when the project has no configured Main Scene. */
    @Test
    void rejectsRunProjectWithoutMainScene() throws IOException {
        Path projectRoot = writeProject(null, null);
        try (URLClassLoader loader = runtimeClassLoader()) {
            assertThatExceptionOfType(ProjectHostException.class)
                    .isThrownBy(() -> runtimeHost(loader, emptyEnvironment()).load(projectRoot))
                    .satisfies(failure -> {
                        assertThat(failure).hasMessageContaining("Run Project requires a configured Main Scene");
                        assertThat(failure.diagnostics())
                                .singleElement()
                                .satisfies(diagnostic -> assertThat(
                                                diagnostic.code().code())
                                        .isEqualTo("project.runtime.main-scene.missing"));
                    });
        }
    }

    /** Runs an explicit Scene even when the project has no configured Main Scene. */
    @Test
    void runsExplicitSceneWithoutMainScene() throws IOException {
        Path projectRoot = writeProject(null, null);
        ProjectLaunchRequest request = ProjectLaunchRequest.scene(AssetRef.to(AssetId.from(MAIN_SCENE_ID)));
        try (URLClassLoader loader = runtimeClassLoader();
                HostedProject hosted = runtimeHost(loader, environment(new RecordingApplicationExtension(), null))
                        .load(projectRoot, request)) {
            assertThat(hosted.world().definition().name()).isEqualTo("Main");
            assertThat(hosted.project().runtime().mainScene()).isEmpty();
        }
    }

    /** Validates required host construction arguments immediately. */
    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void rejectsNullHostArguments() {
        ClassLoader loader = getClass().getClassLoader();
        ProjectRuntimeEnvironment environment = emptyEnvironment();

        assertThatThrownBy(() -> new ProjectRuntimeHost(null, loader, environment))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("engineVersion");
        assertThatThrownBy(() -> new ProjectRuntimeHost("0.1.0", null, environment))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("classLoader");
        assertThatThrownBy(() -> new ProjectRuntimeHost("0.1.0", loader, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("environment");
    }

    /** Creates a generic host at the stable interface used by editor preview and exports. */
    private static ProjectHost host(ClassLoader loader, ProjectRuntimeEnvironment environment) {
        return runtimeHost(loader, environment);
    }

    /** Creates the concrete reusable host for tests exercising Scene selection. */
    private static ProjectRuntimeHost runtimeHost(ClassLoader loader, ProjectRuntimeEnvironment environment) {
        return new ProjectRuntimeHost("0.1.0-SNAPSHOT", loader, environment);
    }

    /** Creates an environment without built-ins, modules, or usable resource loading. */
    private static ProjectRuntimeEnvironment emptyEnvironment() {
        return environment(null, null);
    }

    /** Creates a minimal environment optionally supplying one close-observable module. */
    private static ProjectRuntimeEnvironment environment(
            @Nullable ComponentRuntimeExtension extension, @Nullable RecordingWorldModule module) {
        return new ProjectRuntimeEnvironment() {
            @Override
            public List<ExtensionDescriptor> descriptors() {
                return List.of();
            }

            @Override
            public List<ComponentRuntimeExtension> runtimeExtensions() {
                return extension == null ? List.of() : List.of(extension);
            }

            @Override
            public List<WorldModuleBinding<?>> createWorldModules(Optional<InputMapDefinition> inputMap) {
                return module == null ? List.of() : List.of(WorldModuleBinding.of(TestWorldModule.class, module));
            }

            @Override
            public ProjectContent loadContent(GameProject project, RegisteredTypeCatalog types, AssetCatalog authored) {
                RuntimeResourceProvider resources = new RuntimeResourceProvider() {
                    @Override
                    public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
                        throw new IllegalStateException("test project declares no runtime resources");
                    }
                };
                return new ProjectContent(authored, resources);
            }
        };
    }

    /** Writes a complete minimal project with an optional Main Scene reference. */
    private Path writeProject(@Nullable String mainSceneId, @Nullable String pathHint) throws IOException {
        Path projectRoot = temporaryDirectory.resolve("project");
        Files.createDirectories(projectRoot.resolve("worlds"));
        Files.writeString(
                projectRoot.resolve("jscene3d.json"), manifest(mainSceneId, pathHint), StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("worlds/main.scene.json"), worldDefinition(), StandardCharsets.UTF_8);
        return projectRoot;
    }

    /** Creates an isolated class loader containing descriptor and optional service-provider metadata. */
    private URLClassLoader runtimeClassLoader() throws IOException {
        Path classPath = temporaryDirectory.resolve("runtime-classpath");
        Path metadata = classPath.resolve("META-INF/jscene3d");
        Files.createDirectories(metadata);
        Files.writeString(metadata.resolve("extension.json"), extensionDescriptor(), StandardCharsets.UTF_8);
        return new URLClassLoader(
                new URL[] {classPath.toUri().toURL()}, getClass().getClassLoader());
    }

    /** Returns the test project manifest with an optional Main Scene reference. */
    private static String manifest(@Nullable String mainSceneId, @Nullable String pathHint) {
        String mainSceneProperty = mainSceneId == null
                ? ""
                : String.format(
                        Locale.ROOT,
                        ",%n        \"mainScene\": {\"assetId\": \"%s\", \"pathHint\": \"%s\"}",
                        mainSceneId,
                        pathHint);
        return String.format(Locale.ROOT, """
                {
                  "$schema": "https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion": 1,
                  "identity": {
                    "id": "%s",
                    "name": "Host Test",
                    "version": "0.1.0"
                  },
                  "engine": {
                    "requires": ">=0.1.0-SNAPSHOT <0.2.0",
                    "authoredWith": "0.1.0-SNAPSHOT"
                  },
                  "runtime": {
                    "applicationExtension": "%s"%s
                  },
                  "extensions": [
                    {"id": "%s", "requires": ">=0.1.0 <0.2.0"}
                  ],
                  "catalog": {
                    "genres": [],
                    "tags": [],
                    "players": {"minimum": 1, "maximum": 1}
                  }
                }
                """, APPLICATION_ID, APPLICATION_ID, mainSceneProperty, APPLICATION_ID);
    }

    /** Returns safe metadata for the test application component. */
    private static String extensionDescriptor() {
        return String.format(Locale.ROOT, """
                {
                  "$schema": "https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion": 1,
                  "id": "%s",
                  "version": "0.1.0",
                  "engineRequires": ">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName": "Host Test",
                  "types": [],
                  "components": [
                    {
                      "id": "%s/behavior",
                      "typeVersion": 1,
                      "displayName": "Behavior"
                    }
                  ]
                }
                """, APPLICATION_ID, APPLICATION_ID);
    }

    /** Returns one Scene containing the application component. */
    private static String worldDefinition() {
        return worldDefinition(MAIN_SCENE_ID, "Main");
    }

    /** Returns one Scene containing the application component. */
    private static String worldDefinition(String assetId, String name) {
        return String.format(Locale.ROOT, """
                {
                  "$schema": "https://jscene3d.org/schemas/scene-definition-1.json",
                  "assetId": "%s",
                  "assetType": "scene-definition",
                  "formatVersion": 1,
                  "name": "%s",
                  "connections": [],
                  "roots": [
                    {
                      "entryType": "local",
                      "entityId": "31068f35-754a-4f31-b1ef-e7650b285f8a",
                      "name": "Root",
                      "enabled": true,
                      "components": [
                        {
                          "componentId": "%s",
                          "type": "%s/behavior",
                          "typeVersion": 1,
                          "properties": {}
                        }
                      ],
                      "children": []
                    }
                  ]
                }
                """, assetId, name, COMPONENT_ID, APPLICATION_ID);
    }

    /** Application provider used to prove ServiceLoader discovery and preparation. */
    public static final class RecordingApplicationExtension implements ApplicationRuntimeExtension {
        /** Creates the service provider. */
        public RecordingApplicationExtension() {
            // Public construction is required by ServiceLoader.
        }

        @Override
        public String id() {
            return APPLICATION_ID;
        }

        @Override
        public void register(ComponentFactoryRegistry registry) {
            registry.register(COMPONENT_TYPE, ignored -> new RecordingComponent());
        }

        @Override
        public void prepare(HostedProject project) {
            RecordingComponent component = project.world()
                    .roots()
                    .getFirst()
                    .component(COMPONENT_ID, RecordingComponent.class)
                    .orElseThrow();
            component.prepare();
        }
    }

    /** Application provider whose preparation proves host rollback after composition. */
    public static final class FailingApplicationExtension implements ApplicationRuntimeExtension {
        /** Creates the service provider. */
        public FailingApplicationExtension() {
            // Public construction is required by ServiceLoader.
        }

        @Override
        public String id() {
            return APPLICATION_ID;
        }

        @Override
        public void register(ComponentFactoryRegistry registry) {
            registry.register(COMPONENT_TYPE, ignored -> new RecordingComponent());
        }

        @Override
        public void prepare(HostedProject project) {
            throw new IllegalStateException("deliberate preparation failure");
        }
    }

    /** Close-observable test world-module interface. */
    private interface TestWorldModule extends WorldModule {}

    /** Close-observable test adapter. */
    private static final class RecordingWorldModule implements TestWorldModule {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }

        /** Returns whether host or world ownership performed cleanup. */
        boolean isClosed() {
            return closed;
        }
    }

    /** Mutable test component recording application preparation. */
    public static final class RecordingComponent {
        private boolean prepared;

        /** Records successful application preparation. */
        void prepare() {
            prepared = true;
        }

        /** Returns whether application preparation occurred. */
        boolean isPrepared() {
            return prepared;
        }
    }
}
