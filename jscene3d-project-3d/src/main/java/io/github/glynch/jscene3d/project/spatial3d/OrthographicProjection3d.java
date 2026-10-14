/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.cameras.OrthographicCamera;

/**
 * Complete validated authored orthographic projection state.
 *
 * <p>Validation follows {@link OrthographicCamera}: horizontal and vertical bounds are strictly ordered, the near
 * clipping distance is non-negative, the far distance is greater than the near distance, and zoom is positive.
 *
 * @param left finite left projection boundary
 * @param right finite right projection boundary greater than {@code left}
 * @param top finite top projection boundary greater than {@code bottom}
 * @param bottom finite bottom projection boundary
 * @param near finite non-negative near clipping distance
 * @param far finite far clipping distance greater than {@code near}
 * @param zoom finite positive projection magnification
 */
public record OrthographicProjection3d(
        float left, float right, float top, float bottom, float near, float far, float zoom) {
    /**
     * Validates complete projection state against the engine camera contract.
     *
     * @throws IllegalArgumentException if any value is invalid or a required relationship is not satisfied
     */
    public OrthographicProjection3d {
        OrthographicCamera camera = new OrthographicCamera(left, right, top, bottom, near, far);
        camera.setZoom(zoom);
    }
}
