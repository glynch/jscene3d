/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class IndexBufferTest {
    @Test
    void copiesConstructionAndSnapshotArrays() {
        int[] source = {0, 1, 2};
        IndexBuffer index = IndexBuffer.of(source, BufferUsage.STREAM);
        source[0] = 9;

        int[] snapshot = index.toArray();
        snapshot[1] = 9;

        assertThat(index.count()).isEqualTo(3);
        assertThat(index.usage()).isEqualTo(BufferUsage.STREAM);
        assertThat(index.value(0)).isZero();
        assertThat(index.value(1)).isEqualTo(1);
    }

    @Test
    void versionsSingleAndScopedChangesAndExpiresEditor() {
        IndexBuffer index = IndexBuffer.of(new int[] {0, 1, 2});
        AtomicReference<IndexBuffer.Editor> retainedEditor = new AtomicReference<>();

        index.set(0, 2);
        index.edit(editor -> {
            retainedEditor.set(editor);
            editor.set(1, 2);
            editor.set(2, 0);
        });

        assertThat(index.toArray()).containsExactly(2, 2, 0);
        assertThat(index.version()).isEqualTo(2L);
        IndexBuffer.Editor expiredEditor = Objects.requireNonNull(retainedEditor.get());
        assertThatIllegalStateException().isThrownBy(() -> expiredEditor.set(0, 0));
    }

    @Test
    void retainsChangesAndVersionsWhenEditCallbackThrows() {
        IndexBuffer index = IndexBuffer.of(new int[] {0, 1, 2});

        assertThatIllegalStateException()
                .isThrownBy(() -> index.edit(editor -> {
                    editor.set(0, 2);
                    throw new IllegalStateException("failure from callback");
                }));

        assertThat(index.value(0)).isEqualTo(2);
        assertThat(index.version()).isEqualTo(1L);
    }

    @Test
    void rejectsInvalidConstructionAndMutation() {
        int[] negativeData = {0, -1, 2};
        IndexBuffer index = IndexBuffer.of(new int[] {0, 1, 2});

        assertThatIllegalArgumentException().isThrownBy(() -> IndexBuffer.of(negativeData));
        assertThatIllegalArgumentException().isThrownBy(() -> index.set(0, -1));
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> IndexBuffer.of(null));
        assertThatNullPointerException().isThrownBy(() -> IndexBuffer.of(new int[0], null));
        IndexBuffer index = IndexBuffer.of(new int[0]);
        assertThatNullPointerException().isThrownBy(() -> index.edit(null));
    }
}
