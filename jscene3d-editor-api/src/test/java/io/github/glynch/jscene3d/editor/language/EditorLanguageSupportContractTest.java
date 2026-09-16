/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class EditorLanguageSupportContractTest {
    @Test
    void contributionCopiesItsNonEmptyLanguageSet() {
        Set<EditorLanguageId> languages = new HashSet<>();
        languages.add(EditorLanguages.JAVA);

        EditorLanguageSupportContribution contribution = new EditorLanguageSupportContribution(
                new EditorLanguageSupportId("io.github.glynch.test.java"), languages);
        languages.clear();

        assertThat(contribution.languages()).containsExactly(EditorLanguages.JAVA);
    }

    @Test
    void contributionRejectsMissingOrEmptyLanguages() {
        EditorLanguageSupportId id = new EditorLanguageSupportId("io.github.glynch.test.java");

        assertThatNullPointerException().isThrownBy(() -> new EditorLanguageSupportContribution(id, null));
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorLanguageSupportContribution(id, setContainingNull()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorLanguageSupportContribution(id, Set.of()))
                .withMessage("languages must not be empty");
    }

    @Test
    void supportIdentityRequiresANamespacedValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorLanguageSupportId("java"))
                .withMessageContaining("namespaced identity");
    }

    @Test
    void projectSessionDefaultsToEmptyCompletionResult() {
        EditorCompletionRequest request = new EditorCompletionRequest(
                URI.create("file:///workspace/Example.java"),
                7,
                new EditorTextPosition(2, 4),
                EditorCompletionTrigger.manual());

        try (EditorLanguageProjectSession session = new DefaultProjectSession()) {
            EditorCompletionResult result =
                    session.completion(request).toCompletableFuture().join();

            assertThat(result.version()).isEqualTo(7);
            assertThat(result.items()).isEmpty();
            assertThat(result.incomplete()).isFalse();
        }
    }

    @Test
    void projectSessionRejectsNullCompletionRequest() {
        try (EditorLanguageProjectSession session = new DefaultProjectSession()) {
            assertThatNullPointerException().isThrownBy(() -> session.completion(null));
        }
    }

    private static Set<EditorLanguageId> setContainingNull() {
        Set<EditorLanguageId> languages = new HashSet<>();
        languages.add(null);
        return languages;
    }

    private static final class DefaultProjectSession implements EditorLanguageProjectSession {
        @Override
        public void close() {
            // No resources are owned by this test session.
        }
    }
}
