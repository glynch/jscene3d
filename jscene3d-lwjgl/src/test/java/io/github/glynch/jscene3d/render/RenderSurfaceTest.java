/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

final class RenderSurfaceTest {
    @Test
    void releasesTransferredAccessWithoutObscuringConstructionFailure() {
        RuntimeException activationFailure = new IllegalStateException("activation failed");
        RuntimeException releaseFailure = new IllegalStateException("release failed");
        FailingRenderSurface surface = new FailingRenderSurface(activationFailure, releaseFailure);

        Throwable thrown = catchThrowable(() -> Renderer.create(surface));

        assertThat(thrown).isSameAs(activationFailure);
        assertThat(thrown.getSuppressed()).containsExactly(releaseFailure);
        assertThat(surface.releaseCount()).isOne();
    }

    /** Surface that fails before renderer construction reaches OpenGL. */
    private static final class FailingRenderSurface implements RenderSurface {
        private final RuntimeException activationFailure;
        private final RuntimeException releaseFailure;

        private int releaseCount;

        /** Stores the failures returned by the two exercised lifecycle operations. */
        private FailingRenderSurface(RuntimeException activationFailure, RuntimeException releaseFailure) {
            this.activationFailure = activationFailure;
            this.releaseFailure = releaseFailure;
        }

        @Override
        public void activate() {
            throw activationFailure;
        }

        @Override
        public RenderSurfaceSize size() {
            throw new AssertionError("Renderer must not query size after activation fails");
        }

        @Override
        public void release() {
            releaseCount++;
            throw releaseFailure;
        }

        /** Returns the number of release attempts. */
        private int releaseCount() {
            return releaseCount;
        }
    }
}
