/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class BufferAttributeTest {
    @Test
    void copiesConstructionAndSnapshotArrays() {
        float[] source = {1.0f, 2.0f, 3.0f, 4.0f};
        BufferAttribute attribute = BufferAttribute.of(source, 2, BufferUsage.DYNAMIC);
        source[0] = 99.0f;

        float[] snapshot = attribute.toArray();
        snapshot[1] = 99.0f;

        assertThat(attribute.itemSize()).isEqualTo(2);
        assertThat(attribute.count()).isEqualTo(2);
        assertThat(attribute.usage()).isEqualTo(BufferUsage.DYNAMIC);
        assertThat(attribute.value(0, 0)).isEqualTo(1.0f);
        assertThat(attribute.value(0, 1)).isEqualTo(2.0f);
    }

    @Test
    void copiesIntoReusableCallerStorage() {
        BufferAttribute attribute = BufferAttribute.of(new float[] {1.0f, 2.0f, 3.0f}, 3);
        float[] destination = new float[3];

        attribute.copyTo(destination);

        assertThat(destination).containsExactly(1.0f, 2.0f, 3.0f);
        assertThatIllegalArgumentException().isThrownBy(() -> attribute.copyTo(new float[2]));
    }

    @Test
    void versionsActualScalarAndConvenienceChanges() {
        BufferAttribute attribute = BufferAttribute.of(new float[8], 4);

        attribute.set(0, 0, 1.0f);
        attribute.setXY(0, 2.0f, 3.0f);
        attribute.setXYZ(1, 4.0f, 5.0f, 6.0f);
        attribute.setXYZW(1, 4.0f, 5.0f, 6.0f, 7.0f);

        assertThat(attribute.version()).isEqualTo(4L);
        assertThat(attribute.toArray()).containsExactly(2.0f, 3.0f, 0.0f, 0.0f, 4.0f, 5.0f, 6.0f, 7.0f);

        attribute.setXYZW(1, 4.0f, 5.0f, 6.0f, 7.0f);
        assertThat(attribute.version()).isEqualTo(4L);
    }

    @Test
    void recordsOneVersionForScopedBatchAndExpiresEditor() {
        BufferAttribute attribute = BufferAttribute.of(new float[6], 3);
        AtomicReference<BufferAttribute.Editor> retainedEditor = new AtomicReference<>();

        attribute.edit(editor -> {
            retainedEditor.set(editor);
            editor.setXYZ(0, 1.0f, 2.0f, 3.0f);
            editor.setXYZ(1, 4.0f, 5.0f, 6.0f);
        });

        assertThat(attribute.version()).isEqualTo(1L);
        BufferAttribute.Editor expiredEditor = Objects.requireNonNull(retainedEditor.get());
        assertThatIllegalStateException().isThrownBy(() -> expiredEditor.setX(0, 9.0f));
    }

    @Test
    void retainsChangesAndVersionsWhenEditCallbackThrows() {
        BufferAttribute attribute = BufferAttribute.of(new float[3], 3);

        assertThatIllegalStateException()
                .isThrownBy(() -> attribute.edit(editor -> {
                    editor.setXYZ(0, 1.0f, 2.0f, 3.0f);
                    throw new IllegalStateException("failure from callback");
                }));

        assertThat(attribute.toArray()).containsExactly(1.0f, 2.0f, 3.0f);
        assertThat(attribute.version()).isEqualTo(1L);
    }

    @Test
    void rejectsInvalidConstructionAndMutation() {
        float[] indivisibleData = {1.0f, 2.0f, 3.0f};
        float[] nonFiniteData = {Float.NaN};
        BufferAttribute attribute = BufferAttribute.of(new float[3], 3);

        assertThatIllegalArgumentException().isThrownBy(() -> BufferAttribute.of(indivisibleData, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> BufferAttribute.of(nonFiniteData, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> attribute.setX(0, Float.POSITIVE_INFINITY));
        assertThatIndexOutOfBoundsException().isThrownBy(() -> attribute.setXYZW(0, 1.0f, 2.0f, 3.0f, 4.0f));
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> BufferAttribute.of(null, 3));
        assertThatNullPointerException().isThrownBy(() -> BufferAttribute.of(new float[3], 3, null));
        BufferAttribute attribute = BufferAttribute.of(new float[3], 3);
        assertThatNullPointerException().isThrownBy(() -> attribute.edit(null));
        assertThatNullPointerException().isThrownBy(() -> attribute.copyTo(null));
    }
}
