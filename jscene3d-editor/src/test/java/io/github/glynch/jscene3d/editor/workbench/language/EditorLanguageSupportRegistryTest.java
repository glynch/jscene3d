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
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

final class EditorLanguageSupportRegistryTest {
    private static final EditorLanguageSupport JAVA_SUPPORT = ignored -> () -> {};

    @Test
    void resolvesRegisteredSupportAndRemovesItIdempotently() {
        try (EditorLanguageSupportRegistry registry = new EditorLanguageSupportRegistry(new MutableProjects());
                var registration = registry.register(contribution("java", EditorLanguages.JAVA), JAVA_SUPPORT)) {
            assertThat(registry.resolve(EditorLanguages.JAVA)).containsSame(JAVA_SUPPORT);

            registration.close();
            registration.close();

            assertThat(registry.resolve(EditorLanguages.JAVA)).isEmpty();
        }
    }

    @Test
    void rejectsDuplicateIdentitiesAndLanguageClaims() {
        try (EditorLanguageSupportRegistry registry = new EditorLanguageSupportRegistry(new MutableProjects());
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
        EditorLanguageSupportRegistry registry = new EditorLanguageSupportRegistry(new MutableProjects());
        registry.register(contribution("java", EditorLanguages.JAVA), JAVA_SUPPORT);
        registry.close();

        assertThat(registry.resolve(EditorLanguages.JAVA)).isEmpty();
    }

    @Test
    void ownsExactlyOneSessionForTheCurrentProject() {
        MutableProjects projects = new MutableProjects();
        AtomicInteger opens = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        EditorLanguageSupport support = ignored -> {
            opens.incrementAndGet();
            return closes::incrementAndGet;
        };

        try (EditorLanguageSupportRegistry registry = new EditorLanguageSupportRegistry(projects);
                var registration = registry.register(contribution("java", EditorLanguages.JAVA), support)) {
            EditorProject project = new EditorProject(
                    "project",
                    "Project",
                    Path.of(System.getProperty("java.io.tmpdir"), "project").toUri());
            projects.publish(Optional.of(project));
            projects.publish(Optional.of(project));

            assertThat(opens).hasValue(1);
            assertThat(closes).hasValue(0);

            projects.publish(Optional.empty());
            assertThat(closes).hasValue(1);
        }
    }

    private static EditorLanguageSupportContribution contribution(String name, EditorLanguageId language) {
        return new EditorLanguageSupportContribution(
                new EditorLanguageSupportId("io.github.glynch.test." + name), Set.of(language));
    }

    private static EditorLanguageProjectSession session() {
        return () -> {};
    }

    private static final class MutableProjects implements EditorProjects {
        private final List<Consumer<Optional<EditorProject>>> listeners = new ArrayList<>();
        private Optional<EditorProject> current = Optional.empty();

        @Override
        public Optional<EditorProject> current() {
            return current;
        }

        @Override
        public EditorRegistration observe(Consumer<Optional<EditorProject>> listener) {
            listeners.add(listener);
            listener.accept(current);
            return () -> listeners.remove(listener);
        }

        private void publish(Optional<EditorProject> project) {
            current = project;
            List.copyOf(listeners).forEach(listener -> listener.accept(project));
        }
    }
}
