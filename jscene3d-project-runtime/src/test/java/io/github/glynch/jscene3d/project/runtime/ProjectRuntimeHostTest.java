/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.runtime.extension.ApplicationRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises manifest-driven project hosting and application-provider discovery. */
final class ProjectRuntimeHostTest {
    private static final String APPLICATION_ID = "io.github.glynch.host-test";
    private static final ComponentType COMPONENT_TYPE = ComponentType.of(APPLICATION_ID + "/behavior", 1);
    private static final ComponentId COMPONENT_ID = ComponentId.from("b54e497f-49f4-4678-8fb3-e14f5749f3b9");
    private static final String WORLD_ASSET_ID = "2f26576c-570d-4338-bc30-52bc41def3a5";

    @TempDir
    private Path temporaryDirectory;

    /** Discovers the manifest-selected provider, composes its component, and runs preparation before activation. */
    @Test
    void loadsProjectThroughDiscoveredApplicationExtension() throws IOException {
        Path projectRoot = writeProject("worlds/main.world.json");
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

    /** Rejects invalid manifest data while retaining the loader's structured diagnostics. */
    @Test
    void reportsManifestDiagnostics() throws IOException {
        Path projectRoot = temporaryDirectory.resolve("invalid-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("project.json"), "{}\n", StandardCharsets.UTF_8);
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
        Path projectRoot = writeProject("worlds/main.world.json");
        RecordingWorldModule module = new RecordingWorldModule();
        ProjectRuntimeEnvironment environment = environment(null, module);
        try (URLClassLoader loader = runtimeClassLoader()) {
            ProjectHost runtimeHost = host(loader, environment);

            assertThatThrownBy(() -> runtimeHost.load(projectRoot))
                    .isInstanceOf(ProjectHostException.class)
                    .hasMessageContaining("startup world composition failed");
        }

        assertThat(module.isClosed()).isTrue();
    }

    /** Closes the composed world when manifest-selected application preparation fails. */
    @Test
    void closesWorldAfterApplicationPreparationFailure() throws IOException {
        Path projectRoot = writeProject("worlds/main.world.json");
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

    /** Rejects an entry path which does not identify a catalogued definition. */
    @Test
    void rejectsUncataloguedStartupAsset() throws IOException {
        Path projectRoot = writeProject("worlds/missing.world.json");
        try (URLClassLoader loader = runtimeClassLoader()) {
            ProjectHost runtimeHost = host(loader, emptyEnvironment());

            assertThatThrownBy(() -> runtimeHost.load(projectRoot))
                    .isInstanceOf(ProjectHostException.class)
                    .hasMessageContaining("startup world is absent from the asset catalog");
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
            public List<WorldModuleBinding<?>> createWorldModules() {
                return module == null ? List.of() : List.of(WorldModuleBinding.of(TestWorldModule.class, module));
            }

            @Override
            public RuntimeResourceProvider resources() {
                return new RuntimeResourceProvider() {
                    @Override
                    public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
                        throw new IllegalStateException("test project declares no runtime resources");
                    }
                };
            }
        };
    }

    /** Writes a complete minimal project whose extension metadata is supplied by the test class loader. */
    private Path writeProject(String entryScene) throws IOException {
        Path projectRoot = temporaryDirectory.resolve("project");
        Files.createDirectories(projectRoot.resolve("worlds"));
        Files.writeString(projectRoot.resolve("project.json"), manifest(entryScene), StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("worlds/main.world.json"), worldDefinition(), StandardCharsets.UTF_8);
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

    /** Returns the test project manifest. */
    private static String manifest(String entryScene) {
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
                    "applicationExtension": "%s",
                    "entryScene": "%s"
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
                """, APPLICATION_ID, APPLICATION_ID, entryScene, APPLICATION_ID);
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

    /** Returns one world containing the application component. */
    private static String worldDefinition() {
        return String.format(Locale.ROOT, """
                {
                  "$schema": "https://jscene3d.org/schemas/world-definition-1.json",
                  "assetId": "%s",
                  "assetType": "world-definition",
                  "formatVersion": 1,
                  "name": "Main",
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
                """, WORLD_ASSET_ID, COMPONENT_ID, APPLICATION_ID);
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
