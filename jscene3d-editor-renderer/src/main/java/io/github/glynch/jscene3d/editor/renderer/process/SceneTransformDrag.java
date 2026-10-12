/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Handle;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.RotationOrder;
import java.math.BigDecimal;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Immutable interaction context for one transform drag. */
final class SceneTransformDrag {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float UNCHANGED_EPSILON = 0.000001f;

    private final String editId;
    private final SceneTransformSelection selection;
    private final SceneTransformSettings settings;
    private final Handle handle;
    private final SceneTransformGizmoState.Point start;
    private final SceneTransformGizmoState state;
    private final @Nullable SceneTransformPlaneDrag plane;

    SceneTransformDrag(
            String editId,
            SceneTransformSelection selection,
            SceneTransformSettings settings,
            Handle handle,
            SceneTransformGizmoState.Point start,
            SceneTransformGizmoState state,
            @Nullable SceneTransformPlaneDrag plane) {
        this.editId = editId;
        this.selection = selection;
        this.settings = settings;
        this.handle = handle;
        this.start = start;
        this.state = state;
        this.plane = plane;
    }

    String editId() {
        return editId;
    }

    Handle handle() {
        return handle;
    }

    SceneTransformSelection selection() {
        return selection;
    }

    Vector3 initialValue() {
        return selection.transformValue(settings.tool());
    }

    String property() {
        return switch (settings.tool()) {
            case MOVE -> "position";
            case ROTATE -> "orientation";
            case SCALE -> "scale";
            case SELECT -> throw new IllegalStateException("Select has no transform property");
        };
    }

    Optional<Vector3> update(float x, float y) {
        float deltaX = x - start.x();
        float deltaY = y - start.y();
        float scalar = state.scalar(handle, deltaX, deltaY);
        return switch (settings.tool()) {
            case MOVE -> moveDelta(x, y, deltaX, deltaY).map(this::moved);
            case ROTATE -> Optional.of(rotated(scalar));
            case SCALE -> Optional.of(scaled(scalar));
            case SELECT -> throw new IllegalStateException("Select has no transform value");
        };
    }

    void apply(Vector3 value) {
        Object3D object = selection.object();
        switch (settings.tool()) {
            case MOVE -> object.setPosition(value(value.x()), value(value.y()), value(value.z()));
            case ROTATE ->
                object.setRotationFromEuler(
                        value(value.x()) * DEGREES_TO_RADIANS,
                        value(value.y()) * DEGREES_TO_RADIANS,
                        value(value.z()) * DEGREES_TO_RADIANS,
                        RotationOrder.XYZ);
            case SCALE -> object.setScale(value(value.x()), value(value.y()), value(value.z()));
            case SELECT -> throw new IllegalStateException("Select has no transform preview");
        }
    }

    Vector3 currentValue() {
        Object3D object = selection.object();
        return switch (settings.tool()) {
            case MOVE ->
                vector(
                        object.position().x(),
                        object.position().y(),
                        object.position().z());
            case ROTATE -> {
                Vector3f euler =
                        object.quaternion().getEulerAnglesXYZ(new Vector3f()).mul(1.0f / DEGREES_TO_RADIANS);
                yield vector(euler.x, euler.y, euler.z);
            }
            case SCALE ->
                vector(object.scale().x(), object.scale().y(), object.scale().z());
            case SELECT -> throw new IllegalStateException("Select has no transform value");
        };
    }

    private Optional<Vector3f> moveDelta(float x, float y, float deltaX, float deltaY) {
        return plane == null ? Optional.of(state.worldDelta(handle, deltaX, deltaY)) : plane.delta(x, y);
    }

    private Vector3 moved(Vector3f delta) {
        Object3D parent = selection.object().parent();
        if (parent != null) {
            parent.matrixWorld().invert(new Matrix4f()).transformDirection(delta);
        }
        Vector3 initial = selection.transform().position();
        return vector(
                movedComponent(value(initial.x()), delta.x),
                movedComponent(value(initial.y()), delta.y),
                movedComponent(value(initial.z()), delta.z));
    }

    private float movedComponent(float initial, float delta) {
        return Math.abs(delta) <= UNCHANGED_EPSILON ? initial : snap(initial + delta, settings.translationSnap());
    }

    private Vector3 rotated(float scalar) {
        Vector3 initial = selection.transform().orientationDegrees();
        float angle = scalar * 90.0f;
        Axis axis = handle.axes().getFirst();
        Quaternionf local = new Quaternionf()
                .rotationXYZ(
                        value(initial.x()) * DEGREES_TO_RADIANS,
                        value(initial.y()) * DEGREES_TO_RADIANS,
                        value(initial.z()) * DEGREES_TO_RADIANS);
        if (settings.space() == SceneTransformSpace.LOCAL) {
            local.rotateAxis(angle * DEGREES_TO_RADIANS, axis.vector());
        } else {
            Object3D parent = selection.object().parent();
            Quaternionf parentWorld = parent == null ? new Quaternionf() : parent.worldQuaternion(new Quaternionf());
            Quaternionf world = new Quaternionf(parentWorld).mul(local);
            Quaternionf delta = new Quaternionf().rotationAxis(angle * DEGREES_TO_RADIANS, axis.vector());
            local = parentWorld.invert().mul(delta.mul(world));
        }
        Vector3f euler = local.getEulerAnglesXYZ(new Vector3f()).mul(1.0f / DEGREES_TO_RADIANS);
        return vector(
                snap(euler.x, settings.rotationSnap()),
                snap(euler.y, settings.rotationSnap()),
                snap(euler.z, settings.rotationSnap()));
    }

    private Vector3 scaled(float scalar) {
        Vector3 initial = selection.transform().scale();
        float factor = Math.max(0.001f, 1.0f + scalar);
        boolean uniform = handle == Handle.UNIFORM;
        return vector(
                scaledComponent(initial.x(), Axis.X, factor, uniform),
                scaledComponent(initial.y(), Axis.Y, factor, uniform),
                scaledComponent(initial.z(), Axis.Z, factor, uniform));
    }

    private float scaledComponent(BigDecimal initial, Axis axis, float factor, boolean uniform) {
        float value = value(initial);
        return uniform || handle.axes().contains(axis) ? snap(value * factor, settings.scaleSnap()) : value;
    }

    private float snap(float value, float increment) {
        return settings.snapping() ? Math.round(value / increment) * increment : value;
    }

    private static float value(BigDecimal value) {
        return value.floatValue();
    }

    private static Vector3 vector(float x, float y, float z) {
        return new Vector3(decimal(x), decimal(y), decimal(z));
    }

    private static BigDecimal decimal(float value) {
        return new BigDecimal(Float.toString(value)).stripTrailingZeros();
    }
}
