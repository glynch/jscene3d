/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.objects.Mesh;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MorphTargetKeyframeTrackTest {
    @Test
    void animatesAndBlendsCompleteInfluenceVectors() {
        try (BufferGeometry geometry = new BufferGeometry();
                BasicMaterial material = new BasicMaterial()) {
            geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(new float[9], 3));
            geometry.addMorphTarget(new MorphTarget("first", BufferAttribute.of(new float[9], 3), null));
            geometry.addMorphTarget(new MorphTarget("second", BufferAttribute.of(new float[9], 3), null));
            Mesh mesh = new Mesh(geometry, material);
            MorphTargetKeyframeTrack track = MorphTargetKeyframeTrack.influences(
                    mesh, new float[] {0.0f, 1.0f}, new float[] {0.0f, 1.0f, 1.0f, 0.0f}, Interpolation.LINEAR);
            AnimationAction action = new AnimationMixer().action(new AnimationClip("morph", List.of(track)));

            action.setTime(0.25f);

            assertThat(mesh.morphTargetInfluence(0)).isEqualTo(0.25f);
            assertThat(mesh.morphTargetInfluence(1)).isEqualTo(0.75f);
            assertThat(track.property()).isEqualTo(MorphProperty.INFLUENCES);
        }
    }
}
