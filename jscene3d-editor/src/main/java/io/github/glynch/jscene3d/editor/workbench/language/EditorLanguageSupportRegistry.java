/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.language;

import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupport;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportContribution;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportId;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupports;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Resolves one deterministic language adapter for each contributed source language. */
public final class EditorLanguageSupportRegistry implements EditorLanguageSupports, AutoCloseable {
    private final Map<EditorLanguageSupportId, RegisteredSupport> contributions = new LinkedHashMap<>();
    private final Map<EditorLanguageId, RegisteredSupport> languages = new LinkedHashMap<>();

    @Override
    public EditorRegistration register(EditorLanguageSupportContribution contribution, EditorLanguageSupport support) {
        EditorLanguageSupportContribution candidate = Objects.requireNonNull(contribution, "contribution");
        EditorLanguageSupport implementation = Objects.requireNonNull(support, "support");
        if (contributions.containsKey(candidate.id())) {
            throw new IllegalArgumentException("language-support identity is already registered: " + candidate.id());
        }
        candidate.languages().forEach(language -> {
            if (languages.containsKey(language)) {
                throw new IllegalArgumentException("language support is already registered for: " + language.value());
            }
        });

        RegisteredSupport registered = new RegisteredSupport(candidate, implementation);
        contributions.put(candidate.id(), registered);
        candidate.languages().forEach(language -> languages.put(language, registered));
        AtomicBoolean active = new AtomicBoolean(true);
        return () -> {
            if (active.compareAndSet(true, false)) {
                remove(registered);
            }
        };
    }

    /** Returns the registered adapter for one exact language identity. */
    public Optional<EditorLanguageSupport> resolve(EditorLanguageId language) {
        RegisteredSupport registered = languages.get(Objects.requireNonNull(language, "language"));
        return registered == null ? Optional.empty() : Optional.of(registered.support());
    }

    /** Removes every registered language adapter. */
    @Override
    public void close() {
        contributions.clear();
        languages.clear();
    }

    private void remove(RegisteredSupport registered) {
        contributions.remove(registered.contribution().id(), registered);
        registered.contribution().languages().forEach(language -> languages.remove(language, registered));
    }

    private record RegisteredSupport(EditorLanguageSupportContribution contribution, EditorLanguageSupport support) {}
}
