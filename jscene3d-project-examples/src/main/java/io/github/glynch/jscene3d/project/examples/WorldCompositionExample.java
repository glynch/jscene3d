/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.FrameUpdateContext;
import io.github.glynch.jscene3d.project.runtime.RuntimePayload;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpoints;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/** Loads an authored world and transactionally composes two independent definition placements. */
public final class WorldCompositionExample {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.world-example";
    private static final AssetId WORLD_ID = AssetId.from("0fbb5faf-b309-4684-a24c-8dd2295bb483");
    private static final ComponentId LABEL_COMPONENT = ComponentId.from("14b0558a-1f72-4773-a03c-af7001323f92");
    private static final ComponentId LABEL_LINK_COMPONENT = ComponentId.from("34d922c8-dd18-4d4c-8fd4-2a0daaa1cd08");
    private static final ComponentType LABEL_TYPE = ComponentType.of(EXTENSION_ID + "/label", 1);
    private static final ComponentType LABEL_LINK_TYPE = ComponentType.of(EXTENSION_ID + "/label-link", 1);
    private static final RegisteredType LABEL_UPDATE_TYPE = new RegisteredType(EXTENSION_ID + "/label-update", 1);
    private static final PropertyId LABEL = new PropertyId("label");
    private static final PropertyId LABEL_TARGET = new PropertyId("label-target");
    private static final EndpointId LABEL_UPDATE_SIGNAL = new EndpointId("label-update-requested");
    private static final EndpointId LABEL_UPDATE_ACTION = new EndpointId("update-label");
    private static final Logger LOGGER = Logger.getLogger(WorldCompositionExample.class.getName());
    private static final RuntimeResourceLookup NO_RESOURCES = new RuntimeResourceLookup() {
        @Override
        public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
            throw new IllegalArgumentException("the world-composition example has no resources");
        }
    };

    /** Prevents instantiation of this application entry point. */
    private WorldCompositionExample() {
        throw new AssertionError("WorldCompositionExample cannot be instantiated");
    }

    /**
     * Composes the example world in the supplied asset directory.
     *
     * @param arguments one world asset-directory path
     */
    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected one world asset-directory path");
        }
        Path assetDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        AssetCatalog assets = AssetCatalog.scan(assetDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(descriptor()));
        WorldCompositionResult result = WorldComposer.compose(
                assets,
                AssetRef.<WorldDefinition>to(WORLD_ID),
                types,
                List.of(new LabelRuntimeExtension()),
                NO_RESOURCES);
        try (World world = result.world()
                .orElseThrow(() -> new IllegalStateException("world composition failed: " + result.diagnostics()))) {
            world.activate();
            world.advanceFixed(Duration.ofMillis(16L));
            world.advanceFrame(Duration.ofMillis(16L), 0.0F);
            for (Entity root : world.roots()) {
                LabelComponent label =
                        root.component(LABEL_COMPONENT, LabelComponent.class).orElseThrow();
                LabelLinkComponent link = root.component(LABEL_LINK_COMPONENT, LabelLinkComponent.class)
                        .orElseThrow();
                LOGGER.info(() -> root.id() + " " + root.name().orElse("unnamed") + " = " + label.value()
                        + ", bound label = " + link.label().value());
            }
            Entity mutableRoot = world.roots().getLast();
            world.disable(mutableRoot);
            LOGGER.info(() -> mutableRoot.name().orElse("unnamed") + " enabled = " + mutableRoot.isEnabled());
            world.enable(mutableRoot);
            world.destroy(mutableRoot);
            LOGGER.info(() -> mutableRoot.name().orElse("unnamed") + " destroyed = " + mutableRoot.isDestroyed()
                    + ", remaining roots = " + world.roots().size());
        }
    }

    /** Creates the safe descriptor catalog entry used to validate the authored component. */
    private static ExtensionDescriptor descriptor() {
        PropertyDescriptor label = PropertyDescriptor.optionalWithDefault(
                LABEL.value(),
                ProjectValueKind.TEXT,
                new ProjectValue.TextValue("Unlabelled"),
                DescriptorPresentation.named("Label"),
                Map.of(),
                Set.of());
        ComponentTypeDescriptor labelType = ComponentTypeDescriptor.builder(
                        LABEL_TYPE, DescriptorPresentation.named("Label"))
                .properties(List.of(label))
                .actions(List.of(EndpointDescriptor.withPayload(
                        LABEL_UPDATE_ACTION.value(), LABEL_UPDATE_TYPE, DescriptorPresentation.named("Update label"))))
                .lifecycle(Set.of(
                        ComponentLifecycle.CREATED,
                        ComponentLifecycle.ACTIVATED,
                        ComponentLifecycle.DEACTIVATED,
                        ComponentLifecycle.DESTROYED))
                .updatePhases(Set.of(ComponentUpdatePhase.FRAME_UPDATE))
                .build();
        PropertyDescriptor target = PropertyDescriptor.required(
                LABEL_TARGET.value(),
                ProjectValueKind.COMPONENT_TARGET,
                DescriptorPresentation.named("Label target"),
                Map.of(),
                Set.of());
        ComponentTypeDescriptor linkType = ComponentTypeDescriptor.builder(
                        LABEL_LINK_TYPE, DescriptorPresentation.named("Label link"))
                .properties(List.of(target))
                .signals(List.of(EndpointDescriptor.withPayload(
                        LABEL_UPDATE_SIGNAL.value(),
                        LABEL_UPDATE_TYPE,
                        DescriptorPresentation.named("Label update requested"))))
                .updatePhases(Set.of(ComponentUpdatePhase.AFTER_PHYSICS))
                .build();
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("World example"),
                List.of(),
                List.of(labelType, linkType));
    }

    /** Supplies the executable factory independently of the safe descriptor. */
    private static final class LabelRuntimeExtension implements ComponentRuntimeExtension {
        @Override
        public String id() {
            return EXTENSION_ID;
        }

        @Override
        public void register(ComponentFactoryRegistry registry) {
            registry.register(LABEL_TYPE, context -> {
                ProjectValue value = Objects.requireNonNull(context.properties().get(LABEL), "label");
                return new LabelComponent(((ProjectValue.TextValue) value).value());
            });
            registry.register(LABEL_LINK_TYPE, context -> new LabelLinkComponent());
        }
    }

    /** Mutable label independently created for each placed entity and changed through its declared action. */
    private static final class LabelComponent
            implements ComponentLifecycleCallbacks, ComponentEndpointBinder, ComponentUpdateCallbacks {
        private String value;

        /** Stores the initial effective authored label. */
        private LabelComponent(String value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            endpoints.action(LABEL_UPDATE_ACTION, payload -> value = (String) payload.value());
        }

        /** Returns the current label. */
        private String value() {
            return value;
        }

        @Override
        public void onCreated() {
            LOGGER.info(() -> "Created " + value());
        }

        @Override
        public void onActivated() {
            LOGGER.info(() -> "Activated " + value());
        }

        @Override
        public void onDeactivated() {
            LOGGER.info(() -> "Deactivated " + value());
        }

        @Override
        public void onDestroyed() {
            LOGGER.info(() -> "Destroyed " + value());
        }

        @Override
        public void onFrameUpdate(FrameUpdateContext update) {
            LOGGER.info(() -> "Presented " + value() + " at simulation time " + update.simulationTime());
        }
    }

    /** Component proving that stable authored targets bind independently inside repeated definition placements. */
    private static final class LabelLinkComponent
            implements ComponentReferenceBinder, ComponentEndpointBinder, ComponentUpdateCallbacks {
        private Optional<LabelComponent> label = Optional.empty();
        private Optional<RuntimeSignal> updateLabel = Optional.empty();

        @Override
        public void bindReferences(ComponentReferenceResolver references) {
            label = Optional.of(references.component(LABEL_TARGET, LabelComponent.class));
        }

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            updateLabel = Optional.of(endpoints.signal(LABEL_UPDATE_SIGNAL));
        }

        /** Requests a label change through the authored signal/action connection. */
        private void updateLabel(String value) {
            RuntimePayload payload = new RuntimePayload(LABEL_UPDATE_TYPE, value);
            updateLabel
                    .orElseThrow(() -> new IllegalStateException("label update signal is not bound"))
                    .emit(payload);
        }

        /** Returns the direct component reference established before world activation. */
        private LabelComponent label() {
            return label.orElseThrow(() -> new IllegalStateException("label reference is not bound"));
        }

        @Override
        public void onAfterPhysics(FixedUpdateContext update) {
            updateLabel(label().value() + " signalled");
        }
    }
}
