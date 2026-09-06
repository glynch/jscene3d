/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Shared descriptor binding and lifecycle for one static body or sensor. */
abstract class AbstractCollisionObject3d
        implements CollisionObject3d, ComponentReferenceBinder, ComponentLifecycleCallbacks {
    private final Entity owner;
    private final ComponentId componentId;
    private final Spatial3dWorldModule spatial;
    private final Physics3dWorldModule physics;
    private List<CollisionShape3d> shapes = List.of();
    private @Nullable CollisionObject3dRegistration registration;
    private boolean closed;

    /** Stores construction dependencies without resolving authored shape targets early. */
    AbstractCollisionObject3d(
            Entity owner, ComponentId componentId, Spatial3dWorldModule spatial, Physics3dWorldModule physics) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.componentId = Objects.requireNonNull(componentId, "componentId");
        this.spatial = Objects.requireNonNull(spatial, "spatial");
        this.physics = Objects.requireNonNull(physics, "physics");
    }

    @Override
    public final Entity owner() {
        return owner;
    }

    @Override
    public final ComponentId componentId() {
        return componentId;
    }

    @Override
    public final List<CollisionShape3d> shapes() {
        return shapes;
    }

    @Override
    public final boolean isActive() {
        CollisionObject3dRegistration current = registration;
        return current != null && !current.isClosed() && current.isEnabled();
    }

    @Override
    public final boolean isClosed() {
        return closed;
    }

    @Override
    public final void bindReferences(ComponentReferenceResolver references) {
        if (registration != null) {
            throw new IllegalStateException("collision shape membership is already bound");
        }
        List<CollisionShape3d> resolved = Objects.requireNonNull(references, "references")
                .components(Physics3dDescriptors.shapesProperty(), CollisionShape3d.class);
        requireValidMembership(resolved);
        shapes = List.copyOf(resolved);
        Transform3d transform = spatial.requireTransform(owner);
        registration = register(physics, transform, shapes);
    }

    @Override
    public final void onActivated() {
        requireRegistration().setEnabled(true);
    }

    @Override
    public final void onDeactivated() {
        CollisionObject3dRegistration current = registration;
        if (current != null && !current.isClosed()) {
            current.setEnabled(false);
        }
    }

    @Override
    public final void onDestroyed() {
        close();
    }

    @Override
    public final void close() {
        if (closed) {
            return;
        }
        closed = true;
        CollisionObject3dRegistration current = registration;
        if (current != null) {
            current.close();
        }
    }

    /** Creates one initially disabled backend registration after every shape target resolves. */
    abstract CollisionObject3dRegistration register(
            Physics3dWorldModule physics, Transform3d transform, List<CollisionShape3d> resolvedShapes);

    /** Requires non-empty, unique sibling shape membership for the first collision profile. */
    private void requireValidMembership(List<CollisionShape3d> resolved) {
        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("a collision object requires at least one shape");
        }
        if (new HashSet<>(resolved).size() != resolved.size()) {
            throw new IllegalArgumentException("a collision object cannot reference the same shape more than once");
        }
        resolved.forEach(shape -> {
            if (shape.owner() != owner) {
                throw new IllegalArgumentException(
                        "collision shape must be a sibling component of its collision object: " + shape.componentId());
            }
            if (shape.isClosed()) {
                throw new IllegalArgumentException("collision shape is already closed: " + shape.componentId());
            }
        });
    }

    /** Returns the registration established during authored-reference binding. */
    private CollisionObject3dRegistration requireRegistration() {
        CollisionObject3dRegistration current = registration;
        if (current == null || current.isClosed()) {
            throw new IllegalStateException("collision object has no open backend registration");
        }
        return current;
    }
}
