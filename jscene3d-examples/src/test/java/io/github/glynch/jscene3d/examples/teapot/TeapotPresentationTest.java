/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.teapot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.examples.teapot.TeapotPresentation.Shading;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Mesh;
import org.junit.jupiter.api.Test;

final class TeapotPresentationTest {
    @Test
    void rebuildsGeometryAndSwitchesPresentationModes() {
        try (TeapotPresentation presentation = new TeapotPresentation()) {
            Mesh mesh = (Mesh) presentation.root().children().get(0);
            LineSegments wireframe =
                    (LineSegments) presentation.root().children().get(1);
            int defaultElementCount = mesh.geometry().drawRangeCount();

            presentation.setTessellation(2);
            assertThat(mesh.geometry().drawRangeCount()).isLessThan(defaultElementCount);

            presentation.setShading(Shading.WIREFRAME);
            assertThat(mesh.isVisible()).isFalse();
            assertThat(wireframe.isVisible()).isTrue();

            presentation.setShading(Shading.FLAT);
            assertThat(mesh.isVisible()).isTrue();
            assertThat(wireframe.isVisible()).isFalse();
            assertThat(mesh.geometry().index()).isNull();

            presentation.setShading(Shading.SMOOTH);
            assertThat(mesh.geometry().index()).isNotNull();
        }
    }

    @Test
    void preservesAtLeastOneVisibleSection() {
        try (TeapotPresentation presentation = new TeapotPresentation()) {
            presentation.setIncludeLid(false);
            presentation.setIncludeBody(false);
            presentation.setIncludeBottom(false);

            assertThat(presentation.includesLid()).isFalse();
            assertThat(presentation.includesBody()).isFalse();
            assertThat(presentation.includesBottom()).isTrue();
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsInvalidSettings() {
        try (TeapotPresentation presentation = new TeapotPresentation()) {
            assertThatIllegalArgumentException().isThrownBy(() -> presentation.setTessellation(1));
            assertThatNullPointerException().isThrownBy(() -> presentation.setShading(null));
        }
    }
}
