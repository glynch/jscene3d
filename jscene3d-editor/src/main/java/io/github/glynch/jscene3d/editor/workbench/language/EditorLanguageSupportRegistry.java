/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.language;

import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.language.EditorLanguageProjectSession;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupport;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportContribution;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportId;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupports;
import io.github.glynch.jscene3d.editor.language.EditorTextDocument;
import io.github.glynch.jscene3d.editor.language.EditorTextDocumentChange;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.workingcopy.EditorTextWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopies;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopyId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Resolves one deterministic language adapter for each contributed source language. */
public final class EditorLanguageSupportRegistry implements EditorLanguageSupports, AutoCloseable {
    private final Map<EditorLanguageSupportId, RegisteredSupport> contributions = new LinkedHashMap<>();
    private final Map<EditorLanguageId, RegisteredSupport> languages = new LinkedHashMap<>();
    private final Map<RegisteredSupport, EditorLanguageProjectSession> sessions = new LinkedHashMap<>();
    private final Map<EditorWorkingCopyId, EditorTextDocument> openDocuments = new LinkedHashMap<>();
    private final Map<EditorWorkingCopyId, EditorRegistration> textChangeRegistrations = new LinkedHashMap<>();
    private final EditorRegistration projectRegistration;
    private Optional<EditorProject> currentProject = Optional.empty();

    /**
     * Creates a registry coupled to the owning editor window's project lifecycle.
     *
     * @param projects active-project service
     */
    public EditorLanguageSupportRegistry(EditorProjects projects) {
        projectRegistration = Objects.requireNonNull(projects, "projects").observe(this::projectChanged);
    }

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
        currentProject.ifPresent(project -> openSession(registered, project));
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

    /**
     * Synchronizes registered text working copies with their active language-project sessions.
     *
     * @param workingCopies working-copy source
     * @return removable synchronization registration
     */
    public EditorRegistration synchronize(EditorWorkingCopies workingCopies) {
        EditorWorkingCopies copies = Objects.requireNonNull(workingCopies, "workingCopies");
        EditorRegistration opened = copies.onDidRegister().subscribe(this::documentOpened);
        EditorRegistration saved = copies.onDidSave().subscribe(this::documentSaved);
        EditorRegistration closed = copies.onDidUnregister().subscribe(this::documentClosed);
        AtomicBoolean active = new AtomicBoolean(true);
        return () -> {
            if (active.compareAndSet(true, false)) {
                closed.close();
                saved.close();
                opened.close();
                List.copyOf(openDocuments.values()).forEach(this::publishClose);
                textChangeRegistrations.values().forEach(EditorRegistration::close);
                textChangeRegistrations.clear();
                openDocuments.clear();
            }
        };
    }

    /** Removes every registered language adapter. */
    @Override
    public void close() {
        projectRegistration.close();
        closeSessions();
        textChangeRegistrations.values().forEach(EditorRegistration::close);
        textChangeRegistrations.clear();
        openDocuments.clear();
        contributions.clear();
        languages.clear();
    }

    private void remove(RegisteredSupport registered) {
        closeSession(registered);
        contributions.remove(registered.contribution().id(), registered);
        registered.contribution().languages().forEach(language -> languages.remove(language, registered));
    }

    private void projectChanged(Optional<EditorProject> project) {
        Optional<EditorProject> next = Objects.requireNonNull(project, "project");
        if (next.equals(currentProject)) {
            return;
        }
        closeSessions();
        currentProject = next;
        next.ifPresent(opened -> contributions.values().forEach(support -> openSession(support, opened)));
    }

    private void openSession(RegisteredSupport registered, EditorProject project) {
        EditorLanguageProjectSession session = registered.support().openProject(project);
        sessions.put(registered, Objects.requireNonNull(session, "language project session"));
        openDocuments.values().stream()
                .filter(document -> registered.contribution().languages().contains(document.language()))
                .forEach(session::didOpen);
    }

    private void closeSessions() {
        List.copyOf(sessions.values()).reversed().forEach(EditorLanguageProjectSession::close);
        sessions.clear();
    }

    private void closeSession(RegisteredSupport registered) {
        EditorLanguageProjectSession session = sessions.remove(registered);
        if (session != null) {
            session.close();
        }
    }

    private void documentOpened(EditorWorkingCopy workingCopy) {
        if (workingCopy instanceof EditorTextWorkingCopy text) {
            EditorTextDocument document = snapshot(text);
            openDocuments.put(text.id(), document);
            textChangeRegistrations.put(text.id(), text.onDidChangeText().subscribe(this::documentChanged));
            sessionFor(document.language()).ifPresent(session -> session.didOpen(document));
        }
    }

    private void documentChanged(EditorTextDocumentChange change) {
        EditorTextDocument document = change.document();
        openDocuments.entrySet().stream()
                .filter(entry -> entry.getValue().resource().equals(document.resource()))
                .findFirst()
                .ifPresent(entry -> openDocuments.put(entry.getKey(), document));
        sessionFor(document.language()).ifPresent(session -> session.didChange(change));
    }

    private void documentSaved(EditorWorkingCopy workingCopy) {
        if (workingCopy instanceof EditorTextWorkingCopy text) {
            EditorTextDocument document = snapshot(text);
            openDocuments.put(text.id(), document);
            sessionFor(document.language()).ifPresent(session -> session.didSave(document));
        }
    }

    private void documentClosed(EditorWorkingCopy workingCopy) {
        EditorRegistration registration = textChangeRegistrations.remove(workingCopy.id());
        if (registration != null) {
            registration.close();
        }
        EditorTextDocument document = openDocuments.remove(workingCopy.id());
        if (document != null) {
            publishClose(document);
        }
    }

    private void publishClose(EditorTextDocument document) {
        sessionFor(document.language()).ifPresent(session -> session.didClose(document));
    }

    private Optional<EditorLanguageProjectSession> sessionFor(EditorLanguageId language) {
        RegisteredSupport registered = languages.get(language);
        return registered == null ? Optional.empty() : Optional.ofNullable(sessions.get(registered));
    }

    private static EditorTextDocument snapshot(EditorTextWorkingCopy workingCopy) {
        return new EditorTextDocument(
                workingCopy.id().resource(), workingCopy.language(), workingCopy.version(), workingCopy.content());
    }

    private record RegisteredSupport(EditorLanguageSupportContribution contribution, EditorLanguageSupport support) {}
}
