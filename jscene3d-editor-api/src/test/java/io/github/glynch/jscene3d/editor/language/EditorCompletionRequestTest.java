/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import java.net.URI;
import org.junit.jupiter.api.Test;

class EditorCompletionRequestTest {

    private static final URI RESOURCE = URI.create("file:///workspace/src/main/java/example/Example.java");
    private static final EditorTextPosition POSITION = new EditorTextPosition(3, 12);
    private static final EditorCompletionTrigger TRIGGER = EditorCompletionTrigger.manual();

    @Test
    void exposesRequestProperties() {
        EditorCompletionRequest request = new EditorCompletionRequest(RESOURCE, 7, POSITION, TRIGGER);

        assertThat(request.resource()).isEqualTo(RESOURCE);
        assertThat(request.version()).isEqualTo(7);
        assertThat(request.position()).isEqualTo(POSITION);
        assertThat(request.trigger()).isEqualTo(TRIGGER);
    }

    @Test
    void rejectsNullResource() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCompletionRequest(null, 1, POSITION, TRIGGER))
                .withMessage("resource");
    }

    @Test
    void rejectsZeroVersion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorCompletionRequest(RESOURCE, 0, POSITION, TRIGGER))
                .withMessage("version must be positive");
    }

    @Test
    void rejectsNegativeVersion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorCompletionRequest(RESOURCE, -1, POSITION, TRIGGER))
                .withMessage("version must be positive");
    }

    @Test
    void rejectsNullPosition() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCompletionRequest(RESOURCE, 1, null, TRIGGER))
                .withMessage("position");
    }

    @Test
    void rejectsNullTrigger() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCompletionRequest(RESOURCE, 1, POSITION, null))
                .withMessage("trigger");
    }
}
