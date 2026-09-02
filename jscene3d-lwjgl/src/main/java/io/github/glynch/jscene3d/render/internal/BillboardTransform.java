/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.objects.Billboard;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/** Reusable camera-dependent billboard matrix resolver. */
final class BillboardTransform {
    private static final float MINIMUM_DIRECTION_LENGTH_SQUARED = 1.0E-8F;

    private final Vector3f position = new Vector3f();
    private final Vector3f scale = new Vector3f();
    private final Vector3f cameraPosition = new Vector3f();
    private final Vector3f forward = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Quaternionf cameraOrientation = new Quaternionf();

    /** Resolves one billboard's submitted world matrix without modifying scene state. */
    Matrix4f resolve(Billboard billboard, Matrix4fc cameraWorldMatrix, Matrix4f destination) {
        Matrix4fc sourceWorldMatrix = billboard.matrixWorld();
        sourceWorldMatrix.getTranslation(position);
        sourceWorldMatrix.getScale(scale);
        if (sourceWorldMatrix.determinant3x3() < 0.0f) {
            scale.x = -scale.x;
        }
        if (billboard.alignment() == BillboardAlignment.SPHERICAL) {
            resolveSpherical(cameraWorldMatrix, destination);
        } else {
            resolveCylindrical(cameraWorldMatrix, destination);
        }
        applyAnchor(billboard.anchor(), destination);
        return destination;
    }

    /** Copies camera orientation while preserving billboard world position and scale. */
    private void resolveSpherical(Matrix4fc cameraWorldMatrix, Matrix4f destination) {
        cameraWorldMatrix.getUnnormalizedRotation(cameraOrientation).normalize();
        destination.translationRotateScale(position, cameraOrientation, scale);
    }

    /** Builds an upright basis whose positive Z axis faces the camera around world positive Y. */
    private void resolveCylindrical(Matrix4fc cameraWorldMatrix, Matrix4f destination) {
        cameraWorldMatrix.getTranslation(cameraPosition);
        forward.set(cameraPosition.x - position.x, 0.0f, cameraPosition.z - position.z);
        if (forward.lengthSquared() <= MINIMUM_DIRECTION_LENGTH_SQUARED) {
            cameraWorldMatrix.positiveZ(forward);
            forward.y = 0.0f;
        }
        if (forward.lengthSquared() <= MINIMUM_DIRECTION_LENGTH_SQUARED) {
            forward.set(0.0f, 0.0f, 1.0f);
        } else {
            forward.normalize();
        }
        right.set(forward.z, 0.0f, -forward.x);

        destination.zero();
        destination.m00(right.x * scale.x).m01(0.0f).m02(right.z * scale.x);
        destination.m10(0.0f).m11(scale.y).m12(0.0f);
        destination.m20(forward.x * scale.z).m21(0.0f).m22(forward.z * scale.z);
        destination.m30(position.x).m31(position.y).m32(position.z).m33(1.0f);
    }

    /** Moves the unit quad so the selected local anchor remains at the object world position. */
    private static void applyAnchor(Vector2fc anchor, Matrix4f destination) {
        destination.translate(0.5f - anchor.x(), 0.5f - anchor.y(), 0.0f);
    }
}
