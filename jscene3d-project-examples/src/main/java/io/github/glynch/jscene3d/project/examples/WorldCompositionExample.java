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
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
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
    private static final PropertyId LABEL = new PropertyId("label");
    private static final PropertyId LABEL_TARGET = new PropertyId("label-target");
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
            for (Entity root : world.roots()) {
                LabelComponent label =
                        root.component(LABEL_COMPONENT, LabelComponent.class).orElseThrow();
                LabelLinkComponent link = root.component(LABEL_LINK_COMPONENT, LabelLinkComponent.class)
                        .orElseThrow();
                LOGGER.info(() -> root.id() + " " + root.name().orElse("unnamed") + " = " + label.value()
                        + ", bound label = " + link.label().value());
            }
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
                .lifecycle(Set.of(
                        ComponentLifecycle.CREATED,
                        ComponentLifecycle.ACTIVATED,
                        ComponentLifecycle.DEACTIVATED,
                        ComponentLifecycle.DESTROYED))
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
    private static final class LabelRuntimeExtension implements ProjectRuntimeExtension {
        @Override
        public String id() {
            return EXTENSION_ID;
        }

        @Override
        public void register(ProjectRuntimeRegistry registry) {
            registry.registerComponent(LABEL_TYPE, context -> {
                ProjectValue value = Objects.requireNonNull(context.properties().get(LABEL), "label");
                return new LabelComponent(((ProjectValue.TextValue) value).value());
            });
            registry.registerComponent(LABEL_LINK_TYPE, context -> new LabelLinkComponent());
        }
    }

    /** Immutable component value independently created for each placed entity. */
    private record LabelComponent(String value) implements ComponentLifecycleCallbacks {
        @Override
        public void onCreated() {
            LOGGER.info(() -> "Created " + value);
        }

        @Override
        public void onActivated() {
            LOGGER.info(() -> "Activated " + value);
        }

        @Override
        public void onDeactivated() {
            LOGGER.info(() -> "Deactivated " + value);
        }

        @Override
        public void onDestroyed() {
            LOGGER.info(() -> "Destroyed " + value);
        }
    }

    /** Component proving that stable authored targets bind independently inside repeated definition placements. */
    private static final class LabelLinkComponent implements ComponentReferenceBinder {
        private Optional<LabelComponent> label = Optional.empty();

        @Override
        public void bindReferences(ComponentReferenceResolver references) {
            label = Optional.of(references.component(LABEL_TARGET, LabelComponent.class));
        }

        /** Returns the direct component reference established before world activation. */
        private LabelComponent label() {
            return label.orElseThrow(() -> new IllegalStateException("label reference is not bound"));
        }
    }
}
