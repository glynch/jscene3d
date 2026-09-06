/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimePayload;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpoints;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Standard sensor component binding backend transitions to descriptor-declared runtime signals. */
final class InternalCollisionSensor3d extends AbstractCollisionObject3d
        implements CollisionSensor3d, ComponentEndpointBinder, CollisionOverlapListener {
    private @Nullable RuntimeSignal entered;
    private @Nullable RuntimeSignal exited;

    /** Stores the world module dependencies used after reference and endpoint binding. */
    InternalCollisionSensor3d(
            Entity owner, ComponentId componentId, Spatial3dWorldModule spatial, Physics3dWorldModule physics) {
        super(owner, componentId, spatial, physics);
    }

    @Override
    public void bindEndpoints(ComponentEndpoints endpoints) {
        entered = endpoints.signal(Physics3dDescriptors.overlapEnteredSignal());
        exited = endpoints.signal(Physics3dDescriptors.overlapExitedSignal());
    }

    @Override
    public void onEntered(CollisionOverlap3d overlap) {
        requireSignal(entered, "overlap-entered").emit(payload(overlap));
    }

    @Override
    public void onExited(CollisionOverlap3d overlap) {
        requireSignal(exited, "overlap-exited").emit(payload(overlap));
    }

    @Override
    CollisionObject3dRegistration register(
            Physics3dWorldModule physics, Transform3d transform, List<CollisionShape3d> resolvedShapes) {
        return physics.registerSensor(this, transform, resolvedShapes, this);
    }

    /** Wraps one precise overlap with its descriptor-declared payload identity. */
    private static RuntimePayload payload(CollisionOverlap3d overlap) {
        return new RuntimePayload(Physics3dDescriptors.overlapPayloadType(), overlap);
    }

    /** Requires endpoint binding before the first world-owned physics step. */
    private static RuntimeSignal requireSignal(@Nullable RuntimeSignal signal, String name) {
        if (signal == null) {
            throw new IllegalStateException("collision sensor signal is not bound: " + name);
        }
        return signal;
    }
}
