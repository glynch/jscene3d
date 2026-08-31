/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

final class LineTest {
    @Test
    void retainsAndReplacesSharedResourcesForBothLineKinds() {
        try (BufferGeometry firstGeometry = geometry();
                BufferGeometry secondGeometry = geometry();
                LineBasicMaterial firstMaterial = new LineBasicMaterial(Color.RED);
                LineBasicMaterial secondMaterial = new LineBasicMaterial(Color.BLUE)) {
            Line line = new Line(firstGeometry, firstMaterial);
            LineSegments segments = new LineSegments(firstGeometry, firstMaterial);

            line.setGeometry(secondGeometry);
            line.setMaterial(secondMaterial);

            assertThat(line.geometry()).isSameAs(secondGeometry);
            assertThat(line.material()).isSameAs(secondMaterial);
            assertThat(segments.geometry()).isSameAs(firstGeometry);
            assertThat(segments.material()).isSameAs(firstMaterial);
            assertThat(line.parent()).isNull();
        }
    }

    @Test
    void rejectsClosedResourcesAndReportsClosureAfterBinding() {
        BufferGeometry geometry = geometry();
        LineBasicMaterial material = new LineBasicMaterial();
        try {
            Line line = new Line(geometry, material);
            BufferGeometry closedGeometry = geometry();
            closedGeometry.close();
            LineBasicMaterial closedMaterial = new LineBasicMaterial();
            closedMaterial.close();

            assertThatIllegalArgumentException().isThrownBy(() -> new Line(closedGeometry, material));
            assertThatIllegalArgumentException().isThrownBy(() -> new LineSegments(geometry, closedMaterial));
            assertThatIllegalArgumentException().isThrownBy(() -> line.setGeometry(closedGeometry));
            assertThatIllegalArgumentException().isThrownBy(() -> line.setMaterial(closedMaterial));

            geometry.close();
            assertThatIllegalStateException().isThrownBy(line::geometry);
            material.close();
            assertThatIllegalStateException().isThrownBy(line::material);
        } finally {
            geometry.close();
            material.close();
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsNullResources() {
        try (BufferGeometry geometry = geometry();
                LineBasicMaterial material = new LineBasicMaterial()) {
            assertThatNullPointerException().isThrownBy(() -> new Line(null, material));
            assertThatNullPointerException().isThrownBy(() -> new Line(geometry, null));
            assertThatNullPointerException().isThrownBy(() -> new LineSegments(null, material));
            assertThatNullPointerException().isThrownBy(() -> new LineSegments(geometry, null));
            Line line = new Line(geometry, material);
            assertThatNullPointerException().isThrownBy(() -> line.setGeometry(null));
            assertThatNullPointerException().isThrownBy(() -> line.setMaterial(null));
        }
    }

    /** Creates two-position geometry suitable for either line kind. */
    private static BufferGeometry geometry() {
        return BufferGeometry.builder()
                .positions(-1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
                .build();
    }
}
