/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactLookup;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntime;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.RuntimeNode;
import io.github.glynch.jscene3d.project.runtime.extension.NodeControllerFactory;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeFactory;
import io.github.glynch.jscene3d.project.scene.ControllerDefinition;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Instantiates one validated scene through trusted registered factories. */
public final class ProjectRuntimeComposer {
    private final GameProject project;
    private final SceneDefinition scene;
    private final RegisteredTypeCatalog catalog;
    private final FactoryBindings factories;
    private final ProjectResourceResolver resources;
    private final EndpointRouter router = new EndpointRouter();
    private final RuntimeCreationServices creationServices;
    private final List<LifecycleEntry> lifecycle = new ArrayList<>();
    private final Map<String, RuntimeNode> nodes = new LinkedHashMap<>();

    /**
     * Creates a composer for one validated scene and catalog.
     *
     * @param project validated project manifest
     * @param scene validated entry scene
     * @param catalog validated registered-type catalog
     * @param factories trusted factory index
     * @param importedArtifacts optional host lookup for published imported artifacts
     * @param diagnostics destination for non-terminal runtime diagnostics
     */
    public ProjectRuntimeComposer(
            GameProject project,
            SceneDefinition scene,
            RegisteredTypeCatalog catalog,
            FactoryBindings factories,
            Optional<ImportedArtifactLookup> importedArtifacts,
            List<ProjectDiagnostic> diagnostics) {
        this.project = Objects.requireNonNull(project, "project");
        this.scene = Objects.requireNonNull(scene, "scene");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.factories = Objects.requireNonNull(factories, "factories");
        ImportedResourceLoader importedResources = new ImportedResourceLoader(project, importedArtifacts, diagnostics);
        resources = new ProjectResourceResolver(
                project, scene.source().toUri(), catalog, factories, importedResources, diagnostics);
        creationServices = new RuntimeCreationServices(project, scene, router, resources);
    }

    /**
     * Creates every runtime object, then resolves authored connections.
     *
     * @return fully composed runtime awaiting startup
     */
    public ProjectRuntime compose() {
        try {
            InternalRuntimeNode root = createNode(scene.root(), Optional.empty(), true, "/root");
            router.connect(scene.connections());
            return new InternalProjectRuntime(project, scene, root, lifecycle, nodes, router, resources);
        } catch (RuntimeException exception) {
            closeCreated(exception);
            throw exception;
        }
    }

    /** Recursively instantiates one typed node, controller, and authored children. */
    private InternalRuntimeNode createNode(
            SceneNodeDefinition definition, Optional<RuntimeNode> parent, boolean parentEnabled, String location) {
        if (!(definition.source() instanceof SceneNodeDefinition.TypedNode typedNode)) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.SCENE_INSTANCE_UNSUPPORTED,
                    "nested scene instances are not implemented by this runtime slice",
                    location + "/instance");
        }
        boolean enabled = parentEnabled && definition.enabled();
        RegisteredTypeDescriptor descriptor = requireDescriptor(typedNode.type(), location + "/type");
        SceneNodeFactory factory = factories.requireSceneNode(typedNode.type(), location + "/type");
        Map<String, ProjectValue> properties = EffectiveProperties.merge(descriptor, typedNode.properties());
        ProjectRuntimeObject object =
                createNodeObject(factory, definition, properties, descriptor, parent, enabled, location);
        InternalRuntimeNode runtimeNode = new InternalRuntimeNode(definition, enabled, object, parent);
        lifecycle.add(new LifecycleEntry(object, enabled));
        nodes.put(definition.id(), runtimeNode);
        createController(runtimeNode, enabled, location);
        List<SceneNodeDefinition> children = definition.children();
        for (int index = 0; index < children.size(); index++) {
            RuntimeNode child =
                    createNode(children.get(index), Optional.of(runtimeNode), enabled, location + "/children/" + index);
            runtimeNode.addChild(child);
        }
        runtimeNode.complete();
        return runtimeNode;
    }

    /** Creates one optional controller immediately after its owning node. */
    private void createController(InternalRuntimeNode node, boolean enabled, String location) {
        Optional<ControllerDefinition> optionalDefinition = node.definition().controller();
        if (optionalDefinition.isEmpty()) {
            return;
        }
        ControllerDefinition definition = optionalDefinition.orElseThrow();
        RegisteredTypeDescriptor descriptor = requireDescriptor(definition.type(), location + "/controller/type");
        NodeControllerFactory factory = factories.requireController(definition.type(), location + "/controller/type");
        Map<String, ProjectValue> properties = EffectiveProperties.merge(descriptor, definition.properties());
        ControllerCreationContext context =
                new ControllerCreationContext(creationServices, node, properties, descriptor, node::isEnabled);
        ProjectRuntimeObject controller;
        try {
            controller = Objects.requireNonNull(factory.create(context), "controller factory result");
        } catch (RuntimeException exception) {
            throw factoryFailure(definition.type(), node.definition().id(), exception, location + "/controller");
        }
        node.setController(controller);
        lifecycle.add(new LifecycleEntry(controller, enabled));
    }

    /** Invokes one scene-node factory with its bounded construction context. */
    private ProjectRuntimeObject createNodeObject(
            SceneNodeFactory factory,
            SceneNodeDefinition definition,
            Map<String, ProjectValue> properties,
            RegisteredTypeDescriptor descriptor,
            Optional<RuntimeNode> parent,
            boolean enabled,
            String location) {
        SceneNodeCreationContext context = new SceneNodeCreationContext(
                creationServices, definition, properties, descriptor, () -> enabled, parent);
        try {
            return Objects.requireNonNull(factory.create(context), "scene-node factory result");
        } catch (RuntimeException exception) {
            throw factoryFailure(descriptor.type(), definition.id(), exception, location);
        }
    }

    /** Returns the exact descriptor required by a scene that already passed catalog validation. */
    private RegisteredTypeDescriptor requireDescriptor(RegisteredType type, String location) {
        return catalog.find(type)
                .orElseThrow(() -> new RuntimeCompositionException(
                        RuntimeDiagnosticCode.TYPE_MISSING,
                        "registered type is absent from the runtime catalog: " + type,
                        location));
    }

    /** Creates one diagnostic-ready factory failure without leaking implementation internals. */
    private static RuntimeException factoryFailure(
            RegisteredType type, String nodeId, RuntimeException exception, String location) {
        if (exception instanceof RuntimeDiagnosticsException diagnosticsException) {
            return diagnosticsException;
        }
        String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.FACTORY_CREATE_FAILED,
                "factory for " + type + " failed at node " + nodeId + ": " + detail,
                location);
    }

    /** Releases partially created objects in reverse ownership order after composition fails. */
    private void closeCreated(RuntimeException failure) {
        router.deactivate();
        for (int index = lifecycle.size() - 1; index >= 0; index--) {
            try {
                lifecycle.get(index).object().close();
            } catch (RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
        try {
            resources.close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
