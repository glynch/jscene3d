/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import io.github.glynch.jscene3d.render.internal.ShadowFrame;
import java.util.List;

/** Complete one-pixel fallback maps and fixed-unit binding for built-in shadow samplers. */
public final class DefaultShadowMaps implements AutoCloseable {
    /** First texture unit reserved for two-dimensional shadow maps. */
    public static final int TWO_DIMENSIONAL_TEXTURE_UNIT = 8;

    /** First texture unit reserved for point-light cube shadow maps. */
    public static final int POINT_TEXTURE_UNIT = 12;

    /** Total fragment texture units required by standard materials with shadows. */
    public static final int REQUIRED_TEXTURE_UNITS = 16;

    private final ShadowMapResource twoDimensional;
    private final ShadowMapResource cube;

    /** Creates and clears complete fallback maps with depth one. */
    public DefaultShadowMaps() {
        twoDimensional = new ShadowMapResource(false);
        twoDimensional.realize(1, 1);
        twoDimensional.bindForWriting(0);
        glClear(GL_DEPTH_BUFFER_BIT);
        cube = new ShadowMapResource(true);
        cube.realize(1, 1);
        for (int face = 0; face < 6; face++) {
            cube.bindForWriting(face);
            glClear(GL_DEPTH_BUFFER_BIT);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Binds every fixed sampler unit to its active frame map or a complete fallback.
     *
     * @param frame completed shadow frame to bind
     */
    public void bind(ShadowFrame frame) {
        List<ShadowFrame.TwoDimensionalShadow> twoDimensionalShadows = frame.twoDimensionalShadows();
        for (int index = 0; index < ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS; index++) {
            glActiveTexture(GL_TEXTURE0 + TWO_DIMENSIONAL_TEXTURE_UNIT + index);
            if (index < twoDimensionalShadows.size()) {
                twoDimensionalShadows.get(index).resource().bindTexture();
            } else {
                twoDimensional.bindTexture();
            }
        }
        List<ShadowFrame.PointShadow> pointShadows = frame.pointShadows();
        for (int index = 0; index < ShadowFrame.MAX_POINT_SHADOWS; index++) {
            glActiveTexture(GL_TEXTURE0 + POINT_TEXTURE_UNIT + index);
            if (index < pointShadows.size()) {
                pointShadows.get(index).resource().bindTexture();
            } else {
                cube.bindTexture();
            }
        }
    }

    /** Releases both complete fallback maps. */
    @Override
    public void close() {
        twoDimensional.close();
        cube.close();
    }
}
