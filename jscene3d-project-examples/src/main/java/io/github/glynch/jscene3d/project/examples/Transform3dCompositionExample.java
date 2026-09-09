/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.joml.Vector3f;

/** Composes descriptor-backed Transform3d components without exposing renderer scene objects. */
public final class Transform3dCompositionExample {
    private static final AssetId WORLD_ID = AssetId.from("9b76c125-3c5f-4a38-8571-199b79b3d77e");
    private static final ComponentId PARENT_TRANSFORM = ComponentId.from("70ef8d19-70c3-4268-93d7-fc9750d2d0dc");
    private static final ComponentId CHILD_TRANSFORM = ComponentId.from("f34c69a4-9d74-450a-ae13-9f4362b84f8b");
    private static final Logger LOGGER = Logger.getLogger(Transform3dCompositionExample.class.getName());

    private static final RuntimeResourceProvider NO_RESOURCES = new RuntimeResourceProvider() {
        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the example defines no runtime resources");
        }
    };

    /** Prevents instantiation of this application entry point. */
    private Transform3dCompositionExample() {
        throw new AssertionError("Transform3dCompositionExample cannot be instantiated");
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
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(Spatial3dDescriptors.extensionDescriptor()));
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        WorldCompositionResult result = WorldComposer.compose(
                assets,
                AssetRef.<WorldDefinition>to(WORLD_ID),
                types,
                List.of(new Spatial3dRuntimeExtension()),
                List.of(WorldModuleBinding.of(Spatial3dWorldModule.class, spatial)),
                NO_RESOURCES);
        try (World world = result.world()
                .orElseThrow(() -> new IllegalStateException("world composition failed: " + result.diagnostics()))) {
            world.activate();
            Entity parent = world.roots().getFirst();
            Entity child = parent.children().getFirst();
            Transform3d parentTransform =
                    parent.component(PARENT_TRANSFORM, Transform3d.class).orElseThrow();
            Transform3d childTransform =
                    child.component(CHILD_TRANSFORM, Transform3d.class).orElseThrow();

            LOGGER.info(() -> "Parent world position = " + worldPosition(parentTransform));
            LOGGER.info(() -> "Child world position = " + worldPosition(childTransform));
            parentTransform.setPosition(20.0F, 0.0F, 0.0F);
            LOGGER.info(() -> "Moved child world position = " + worldPosition(childTransform));
        }
        LOGGER.info(() -> "Spatial adapter closed = " + spatial.isClosed());
    }

    /** Formats one current world position deterministically for the example output. */
    private static String worldPosition(Transform3d transform) {
        Vector3f position = transform.worldMatrix().getTranslation(new Vector3f());
        return "[" + position.x + ", " + position.y + ", " + position.z + ']';
    }
}
