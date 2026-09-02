/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.materials.BasicMaterial;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class BillboardTest {
    @Test
    void ownsAUnitQuadAndRetainsASharedMaterial() {
        try (BasicMaterial material = new BasicMaterial();
                Billboard billboard = new Billboard(material)) {
            assertThat(billboard.material()).isSameAs(material);
            assertThat(billboard.geometry().drawRangeCount()).isEqualTo(6);
            assertThat(billboard.alignment()).isEqualTo(BillboardAlignment.SPHERICAL);
            assertThat(billboard.anchor()).isEqualTo(new Vector2f(0.5f));
            assertThat(billboard.scale()).isEqualTo(new Vector3f(1.0f));
        }
    }

    @Test
    void changesAlignmentAnchorAndSharedMaterial() {
        try (BasicMaterial firstMaterial = new BasicMaterial();
                BasicMaterial secondMaterial = new BasicMaterial();
                Billboard billboard = new Billboard(firstMaterial)) {
            billboard.setAlignment(BillboardAlignment.CYLINDRICAL);
            billboard.setAnchor(new Vector2f(0.5f, 0.0f));
            billboard.setMaterial(secondMaterial);

            assertThat(billboard.alignment()).isEqualTo(BillboardAlignment.CYLINDRICAL);
            assertThat(billboard.anchor()).isEqualTo(new Vector2f(0.5f, 0.0f));
            assertThat(billboard.material()).isSameAs(secondMaterial);
        }
    }

    @Test
    void closesOnlyItsGeneratedGeometry() {
        try (BasicMaterial material = new BasicMaterial()) {
            Billboard billboard = new Billboard(material);

            billboard.close();
            billboard.close();

            assertThat(billboard.isClosed()).isTrue();
            assertThat(material.isClosed()).isFalse();
            assertThatIllegalStateException().isThrownBy(billboard::geometry);
            assertThatIllegalStateException().isThrownBy(billboard::material);
        }
    }

    @Test
    void rejectsClosedResourcesAndInvalidState() {
        BasicMaterial closedMaterial = new BasicMaterial();
        closedMaterial.close();
        assertThatIllegalArgumentException().isThrownBy(() -> new Billboard(closedMaterial));

        try (BasicMaterial material = new BasicMaterial();
                Billboard billboard = new Billboard(material)) {
            assertThatIllegalArgumentException().isThrownBy(() -> billboard.setAnchor(Float.NaN, 0.5f));
            billboard.close();
            assertThatIllegalStateException().isThrownBy(() -> billboard.setAlignment(BillboardAlignment.CYLINDRICAL));
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullValues() {
        try (BasicMaterial material = new BasicMaterial();
                Billboard billboard = new Billboard(material)) {
            assertThatNullPointerException().isThrownBy(() -> new Billboard(null));
            assertThatNullPointerException().isThrownBy(() -> billboard.setMaterial(null));
            assertThatNullPointerException().isThrownBy(() -> billboard.setAlignment(null));
            assertThatNullPointerException().isThrownBy(() -> billboard.setAnchor(null));
        }
    }
}
