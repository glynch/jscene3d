/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.language.EditorLanguageProjectSession;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupport;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportContribution;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportId;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class EditorLanguageSupportRegistryTest {
    private static final EditorLanguageSupport JAVA_SUPPORT = ignored -> () -> {};

    @Test
    void resolvesRegisteredSupportAndRemovesItIdempotently() {
        try (EditorLanguageSupportRegistry registry = new EditorLanguageSupportRegistry();
                var registration = registry.register(contribution("java", EditorLanguages.JAVA), JAVA_SUPPORT)) {
            assertThat(registry.resolve(EditorLanguages.JAVA)).containsSame(JAVA_SUPPORT);

            registration.close();
            registration.close();

            assertThat(registry.resolve(EditorLanguages.JAVA)).isEmpty();
        }
    }

    @Test
    void rejectsDuplicateIdentitiesAndLanguageClaims() {
        try (EditorLanguageSupportRegistry registry = new EditorLanguageSupportRegistry();
                var registration = registry.register(contribution("java", EditorLanguages.JAVA), JAVA_SUPPORT)) {
            assertThatIllegalArgumentException()
                    .isThrownBy(
                            () -> registry.register(contribution("java", EditorLanguages.XML), ignored -> session()))
                    .withMessageContaining("identity is already registered");
            assertThatIllegalArgumentException()
                    .isThrownBy(
                            () -> registry.register(contribution("other", EditorLanguages.JAVA), ignored -> session()))
                    .withMessageContaining("already registered for: java");
        }
    }

    @Test
    void closeClearsEveryLanguageClaim() {
        EditorLanguageSupportRegistry registry = new EditorLanguageSupportRegistry();
        registry.register(contribution("java", EditorLanguages.JAVA), JAVA_SUPPORT);
        registry.close();

        assertThat(registry.resolve(EditorLanguages.JAVA)).isEmpty();
    }

    private static EditorLanguageSupportContribution contribution(String name, EditorLanguageId language) {
        return new EditorLanguageSupportContribution(
                new EditorLanguageSupportId("io.github.glynch.test." + name), Set.of(language));
    }

    private static EditorLanguageProjectSession session() {
        return () -> {};
    }
}
