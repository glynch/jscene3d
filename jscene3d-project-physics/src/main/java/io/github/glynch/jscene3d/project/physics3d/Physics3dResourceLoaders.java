/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.ResourceContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Supplies runtime loaders corresponding exactly to {@link Physics3dDescriptors}. */
public final class Physics3dResourceLoaders {
    private static final List<RuntimeResourceLoader<?>> ALL =
            List.of(new BoxLoader(), new SphereLoader(), new CapsuleLoader(), new TriangleMeshLoader());

    /** Prevents construction of this stable loader collection. */
    private Physics3dResourceLoaders() {
        throw new AssertionError("Physics3dResourceLoaders cannot be instantiated");
    }

    /**
     * Returns every standard collision resource loader.
     *
     * @return immutable list of all version-one collision resource loaders
     */
    public static List<RuntimeResourceLoader<?>> all() {
        return ALL;
    }

    /** Reconstructs one box resource from validated portable properties. */
    private static final class BoxLoader implements RuntimeResourceLoader<BoxCollisionShape3dResource> {
        @Override
        public RegisteredType type() {
            return Physics3dDescriptors.boxResourceType();
        }

        @Override
        public Class<BoxCollisionShape3dResource> valueType() {
            return BoxCollisionShape3dResource.class;
        }

        @Override
        public BoxCollisionShape3dResource load(ResourceDefinition definition, ResourceContent content) {
            Map<String, ProjectValue> properties = definition.properties();
            return new BoxCollisionShape3dResource(
                    number(properties, "width"), number(properties, "height"), number(properties, "depth"));
        }
    }

    /** Reconstructs one sphere resource from validated portable properties. */
    private static final class SphereLoader implements RuntimeResourceLoader<SphereCollisionShape3dResource> {
        @Override
        public RegisteredType type() {
            return Physics3dDescriptors.sphereResourceType();
        }

        @Override
        public Class<SphereCollisionShape3dResource> valueType() {
            return SphereCollisionShape3dResource.class;
        }

        @Override
        public SphereCollisionShape3dResource load(ResourceDefinition definition, ResourceContent content) {
            return new SphereCollisionShape3dResource(number(definition.properties(), "radius"));
        }
    }

    /** Reconstructs one capsule resource from validated portable properties. */
    private static final class CapsuleLoader implements RuntimeResourceLoader<CapsuleCollisionShape3dResource> {
        @Override
        public RegisteredType type() {
            return Physics3dDescriptors.capsuleResourceType();
        }

        @Override
        public Class<CapsuleCollisionShape3dResource> valueType() {
            return CapsuleCollisionShape3dResource.class;
        }

        @Override
        public CapsuleCollisionShape3dResource load(ResourceDefinition definition, ResourceContent content) {
            Map<String, ProjectValue> properties = definition.properties();
            return new CapsuleCollisionShape3dResource(
                    number(properties, "radius"), nonNegativeNumber(properties, "segment-length"));
        }

        /** Reads one required non-negative finite float property. */
        private static float nonNegativeNumber(Map<String, ProjectValue> properties, String name) {
            ProjectValue value = properties.get(name);
            if (!(value instanceof ProjectValue.NumberValue number)) {
                throw new IllegalArgumentException("collision resource property must be a number: " + name);
            }
            return CollisionPreconditions.requireNonNegative(number.value().floatValue(), name);
        }
    }

    /** Reconstructs one immutable static triangle mesh from its independently published payload. */
    private static final class TriangleMeshLoader
            implements RuntimeResourceLoader<TriangleMeshCollisionShape3dResource> {
        @Override
        public RegisteredType type() {
            return Physics3dDescriptors.triangleMeshResourceType();
        }

        @Override
        public Class<TriangleMeshCollisionShape3dResource> valueType() {
            return TriangleMeshCollisionShape3dResource.class;
        }

        @Override
        public TriangleMeshCollisionShape3dResource load(ResourceDefinition definition, ResourceContent content)
                throws IOException {
            ProjectValue payload = definition.properties().get("payload");
            if (!(payload instanceof ProjectValue.ReferenceValue reference)) {
                throw new IllegalArgumentException("triangle-mesh collision resource payload must be a reference");
            }
            try (InputStream input = content.openPayload(reference.reference())) {
                return Physics3dResourceCodec.readTriangleMesh(input);
            }
        }
    }

    /** Reads one required positive finite float property. */
    private static float number(Map<String, ProjectValue> properties, String name) {
        ProjectValue value = properties.get(name);
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException("collision resource property must be a number: " + name);
        }
        return CollisionPreconditions.requirePositive(number.value().floatValue(), name);
    }
}
