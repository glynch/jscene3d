/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies reusable output-channel state and observation. */
final class EditorOutputChannelTest {
    @Test
    void publishesCompleteSnapshotsForReplacementAppendAndClear() {
        List<String> snapshots = new ArrayList<>();
        try (EditorOutputChannel channel = channel();
                EditorRegistration ignored = channel.observe(snapshots::add)) {
            channel.replace("build started\n");
            channel.append("build completed\n");
            channel.clear();
        }

        assertThat(snapshots).containsExactly("", "build started\n", "build started\nbuild completed\n", "");
    }

    @Test
    void exposesItsStableViewIdentity() {
        try (EditorOutputChannel channel = channel()) {
            assertThat(channel.id()).isEqualTo(new ViewId("test.output"));
            assertThat(channel.title()).isEqualTo("Output");
            assertThat(channel.kind())
                    .isEqualTo(new ViewKindId("io.github.glynch.jscene3d.editor.view.output-channel"));
        }
    }

    @Test
    void observationCanBeRemovedWithoutClosingTheChannel() {
        List<String> snapshots = new ArrayList<>();
        try (EditorOutputChannel channel = channel()) {
            EditorRegistration observation = channel.observe(snapshots::add);
            observation.close();

            channel.replace("not observed");

            assertThat(channel.content()).isEqualTo("not observed");
            assertThat(snapshots).containsExactly("");
        }
    }

    @Test
    void rejectsMutationAndObservationAfterClose() {
        EditorOutputChannel channel = channel();
        channel.close();
        channel.close();

        assertThatThrownBy(() -> channel.replace("closed")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> channel.append("closed")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> channel.observe(ignored -> {})).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesItsIdentityAndTitle() {
        ViewId id = new ViewId("test.output");

        assertThatThrownBy(() -> new EditorOutputChannel(null, "Output")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EditorOutputChannel(id, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EditorOutputChannel(id, " ")).isInstanceOf(IllegalArgumentException.class);
    }

    private static EditorOutputChannel channel() {
        return new EditorOutputChannel(new ViewId("test.output"), "Output");
    }
}
