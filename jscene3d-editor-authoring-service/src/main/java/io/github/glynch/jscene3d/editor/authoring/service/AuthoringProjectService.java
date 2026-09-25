/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionSnapshot;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectCloseResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectDiagnosticDto;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectSummary;
import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoadResult;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.project.session.DefinitionRetentionResult;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.project.session.EditorRetainedDefinition;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Owns zero or one retained authoring project session for a persistent service process. */
public final class AuthoringProjectService implements AutoCloseable {
    /** Stable rejection when an open request would silently replace an active project. */
    public static final String PROJECT_ALREADY_OPEN = "authoring.project.alreadyOpen";

    /** Stable rejection when replacement is requested without an active project. */
    public static final String PROJECT_NOT_OPEN = "authoring.project.notOpen";

    /** Stable rejection when replacement no longer targets the active project generation. */
    public static final String PROJECT_GENERATION_CONFLICT = "authoring.project.generationConflict";

    /** Stable failure when a requested structural definition cannot be resolved. */
    public static final String DEFINITION_UNAVAILABLE = "authoring.definition.unavailable";

    private final EditorProjectLoader loader;
    private final ProjectDiagnosticMessageResolver diagnosticMessages;

    private @Nullable EditorProjectSession activeSession;
    private long nextProjectGeneration = 1;
    private long activeProjectGeneration;
    private boolean closed;

    /**
     * Creates a service backed by the established safe editor project loader.
     *
     * @param loader headless authoring project loader
     */
    public AuthoringProjectService(EditorProjectLoader loader) {
        this(loader, new ProjectDiagnosticMessageResolver());
    }

    /** Creates a service with an explicit diagnostic presentation resolver. */
    AuthoringProjectService(EditorProjectLoader loader, ProjectDiagnosticMessageResolver diagnosticMessages) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.diagnosticMessages = Objects.requireNonNull(diagnosticMessages, "diagnosticMessages");
    }

    /**
     * Opens a project with the stable English presentation used by direct Java consumers.
     *
     * @param params generic project-root or descriptor selection
     * @return domain outcome with summary and structured diagnostics
     */
    public synchronized ProjectOpenResult openProject(ProjectOpenParams params) {
        return openProject(params, Locale.ENGLISH);
    }

    /**
     * Opens and retains one project, rejecting replacement until an explicit close.
     *
     * @param params generic project-root or descriptor selection
     * @param locale initialized client display locale
     * @return domain outcome with summary and structured diagnostics
     */
    public synchronized ProjectOpenResult openProject(ProjectOpenParams params, Locale locale) {
        ensureOpen();
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(locale, "locale");
        if (activeSession != null) {
            return new ProjectOpenResult(false, null, null, List.of(), PROJECT_ALREADY_OPEN);
        }

        EditorProjectLoadResult loadResult = loader.load(Path.of(params.path()));
        List<ProjectDiagnosticDto> diagnostics = diagnostics(loadResult.diagnostics(), locale);
        if (loadResult.session().isEmpty()) {
            return new ProjectOpenResult(false, null, null, diagnostics, null);
        }

        EditorProjectSession session = loadResult.session().orElseThrow();
        long projectGeneration = nextProjectGeneration++;
        activeSession = session;
        activeProjectGeneration = projectGeneration;
        return new ProjectOpenResult(true, projectGeneration, summary(session), diagnostics, null);
    }

    /**
     * Replaces a project with the stable English presentation used by direct Java consumers.
     *
     * @param params expected active generation and candidate project selection
     * @return explicit replacement, candidate-rejection, or generation-conflict outcome
     */
    public ProjectReplaceResult replaceProject(ProjectReplaceParams params) {
        return replaceProject(params, Locale.ENGLISH);
    }

    /**
     * Loads a candidate while retaining the current project, then conditionally installs it.
     *
     * <p>Candidate loading uses the ordinary editor project loader. A rejected candidate leaves the
     * active session, its generation, and all authored state untouched. A successful candidate is
     * installed under a new generation before the replaced session is disposed without saving.
     *
     * @param params expected active generation and candidate project selection
     * @param locale initialized client display locale
     * @return explicit replacement, candidate-rejection, or generation-conflict outcome
     */
    public ProjectReplaceResult replaceProject(ProjectReplaceParams params, Locale locale) {
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(locale, "locale");
        EditorProjectSession expectedSession;
        synchronized (this) {
            ensureOpen();
            expectedSession = activeSession;
            if (expectedSession == null) {
                return conflict(PROJECT_NOT_OPEN);
            }
            if (activeProjectGeneration != params.expectedProjectGeneration()) {
                return conflict(PROJECT_GENERATION_CONFLICT);
            }
        }

        EditorProjectLoadResult loadResult = loader.load(Path.of(params.path()));
        List<ProjectDiagnosticDto> diagnostics = diagnostics(loadResult.diagnostics(), locale);
        if (loadResult.session().isEmpty()) {
            return new ProjectReplaceResult(ProjectReplaceResult.CANDIDATE_REJECTED, null, null, diagnostics, null);
        }

        EditorProjectSession candidate = loadResult.session().orElseThrow();
        long replacementGeneration;
        synchronized (this) {
            if (closed) {
                candidate.close();
                ensureOpen();
            }
            if (activeSession != expectedSession || activeProjectGeneration != params.expectedProjectGeneration()) {
                candidate.close();
                return conflict(PROJECT_GENERATION_CONFLICT);
            }
            replacementGeneration = nextProjectGeneration++;
            activeSession = candidate;
            activeProjectGeneration = replacementGeneration;
        }

        expectedSession.close();
        return new ProjectReplaceResult(
                ProjectReplaceResult.REPLACED, replacementGeneration, summary(candidate), diagnostics, null);
    }

    /**
     * Closes the active session without saving and leaves the process available for another project.
     *
     * @return whether a project was closed and which generation was invalidated
     */
    public synchronized ProjectCloseResult closeProject() {
        ensureOpen();
        EditorProjectSession session = activeSession;
        if (session == null) {
            return new ProjectCloseResult(false, null);
        }
        long invalidatedGeneration = activeProjectGeneration;
        activeSession = null;
        activeProjectGeneration = 0;
        session.close();
        return new ProjectCloseResult(true, invalidatedGeneration);
    }

    /**
     * Resolves, retains, and snapshots one structural definition in the active project generation.
     *
     * @param params expected project generation and authoritative asset identity
     * @return retained definition and complete hierarchy snapshot
     */
    public DefinitionOpenResult openDefinition(DefinitionOpenParams params) {
        return openDefinition(params, Locale.ENGLISH);
    }

    /**
     * Resolves, retains, and snapshots one structural definition using the initialized client locale.
     *
     * @param params expected project generation and authoritative asset identity
     * @param locale initialized client display locale
     * @return retained definition and complete hierarchy snapshot
     */
    public synchronized DefinitionOpenResult openDefinition(DefinitionOpenParams params, Locale locale) {
        ensureOpen();
        DefinitionOpenParams validParams = Objects.requireNonNull(params, "params");
        Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = activeSession;
        if (session == null) {
            return definitionFailure(PROJECT_NOT_OPEN);
        }
        if (activeProjectGeneration != validParams.expectedProjectGeneration()) {
            return definitionFailure(PROJECT_GENERATION_CONFLICT);
        }

        DefinitionRetentionResult result = session.retainDefinition(AssetId.from(validParams.assetId()));
        List<ProjectDiagnosticDto> mappedDiagnostics = diagnostics(result.diagnostics(), locale);
        if (result.definition().isEmpty()) {
            return new DefinitionOpenResult(false, null, null, mappedDiagnostics, DEFINITION_UNAVAILABLE);
        }
        EditorRetainedDefinition definition = result.definition().orElseThrow();
        return new DefinitionOpenResult(true, activeProjectGeneration, snapshot(definition), mappedDiagnostics, null);
    }

    /**
     * Returns the retained session for process orchestration and focused verification.
     *
     * @return active authoring session, when open
     */
    public synchronized Optional<EditorProjectSession> activeSession() {
        ensureOpen();
        return Optional.ofNullable(activeSession);
    }

    /**
     * Returns whether process-scoped service ownership has ended.
     *
     * @return whether the service is closed
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /** Closes any retained session without saving; repeated closure has no additional effect. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        EditorProjectSession session = activeSession;
        activeSession = null;
        activeProjectGeneration = 0;
        closed = true;
        if (session != null) {
            session.close();
        }
    }

    /** Maps the retained domain session to the deliberately narrow first-milestone summary. */
    private static ProjectSummary summary(EditorProjectSession session) {
        GameProject project = session.project();
        WorldDefinition world = session.startupWorld();
        return new ProjectSummary(
                project.identity().id(),
                project.identity().name(),
                project.identity().version(),
                project.root().toString(),
                project.descriptor().toString(),
                new ProjectSummary.WorldSummary(world.id().toString(), world.name()),
                new ProjectSummary.AssetCounts(
                        session.authoredAssets().assets().size(),
                        session.assets().size()));
    }

    /** Maps one domain diagnostic without adding serialization concerns to the domain type. */
    private ProjectDiagnosticDto diagnostic(ProjectDiagnostic diagnostic, Locale locale) {
        return new ProjectDiagnosticDto(
                diagnostic.severity().name().toLowerCase(Locale.ROOT),
                diagnostic.code().code(),
                diagnosticMessages.resolve(diagnostic, locale),
                diagnostic.source().toString(),
                diagnostic.location(),
                diagnostic.details());
    }

    /** Maps ordered domain diagnostics to their stable wire representation. */
    private List<ProjectDiagnosticDto> diagnostics(List<ProjectDiagnostic> diagnostics, Locale locale) {
        return diagnostics.stream()
                .map(diagnostic -> diagnostic(diagnostic, locale))
                .toList();
    }

    /** Maps one retained domain definition to its explicit versioned wire snapshot. */
    private static DefinitionSnapshot snapshot(EditorRetainedDefinition definition) {
        DefinitionSnapshot.DefinitionContext context = new DefinitionSnapshot.DefinitionContext(
                definition.id().toString(),
                definition.kind().serializedName(),
                definition.origin().name().toLowerCase(Locale.ROOT),
                definition.editable(),
                definition.source().toString(),
                text(definition.hierarchy().context().label()));
        return new DefinitionSnapshot(
                definition.revision(),
                context,
                definition.hierarchy().roots().stream()
                        .map(AuthoringProjectService::hierarchyNode)
                        .toList());
    }

    /** Recursively maps one semantic hierarchy occurrence. */
    private static DefinitionSnapshot.HierarchyNode hierarchyNode(EditorHierarchyNode node) {
        return new DefinitionSnapshot.HierarchyNode(
                occurrence(node.occurrence()),
                serialized(node.kind()),
                node.entityId().map(Object::toString).orElse(null),
                node.definitionId().map(Object::toString).orElse(null),
                text(node.label()),
                node.isEnabled(),
                node.isModified(),
                node.isEditable(),
                target(node.inspectorTarget()),
                node.children().stream()
                        .map(AuthoringProjectService::hierarchyNode)
                        .toList());
    }

    /** Maps one future-Inspector semantic target without UI selection state. */
    private static DefinitionSnapshot.SemanticTarget target(InspectorTarget target) {
        return new DefinitionSnapshot.SemanticTarget(
                serialized(target.kind()),
                target.source().toString(),
                target.identity(),
                target.occurrence().map(AuthoringProjectService::occurrence).orElse(null));
    }

    /** Maps occurrence identity without labels, paths, or array indexes. */
    private static DefinitionSnapshot.Occurrence occurrence(HierarchyOccurrenceId occurrence) {
        return new DefinitionSnapshot.Occurrence(
                occurrence.definition().toString(),
                occurrence.entityPath().stream().map(Object::toString).toList());
    }

    /** Preserves authored literals and structured Java-owned semantic text. */
    private static DefinitionSnapshot.AuthoringTextDto text(AuthoringText value) {
        return switch (value) {
            case AuthoringText.Literal literal ->
                new DefinitionSnapshot.AuthoringTextDto("literal", literal.value(), null, List.of());
            case AuthoringText.Message message ->
                new DefinitionSnapshot.AuthoringTextDto(
                        "message",
                        message.defaultMessage(),
                        message.code(),
                        message.arguments().stream().map(Object::toString).toList());
        };
    }

    /** Serializes hierarchy kinds independently of Java enum spelling. */
    private static String serialized(EditorHierarchyNode.Kind kind) {
        return switch (kind) {
            case WORLD -> "world";
            case LOCAL_ENTITY -> "local-entity";
            case PLACEMENT -> "placement";
            case GENERATED_ENTITY -> "generated-entity";
        };
    }

    /** Serializes Inspector target kinds independently of Java enum spelling. */
    private static String serialized(InspectorTarget.Kind kind) {
        return switch (kind) {
            case WORLD -> "world";
            case LOCAL_ENTITY -> "local-entity";
            case GENERATED_ENTITY -> "generated-entity";
            case PLACEMENT -> "placement";
            case ASSET -> "asset";
        };
    }

    /** Creates an operation-level replacement conflict without loading a candidate. */
    private static ProjectReplaceResult conflict(String failureCode) {
        return new ProjectReplaceResult(ProjectReplaceResult.CONFLICT, null, null, List.of(), failureCode);
    }

    /** Creates an operation-level definition failure without leaking a stale generation. */
    private static DefinitionOpenResult definitionFailure(String failureCode) {
        return new DefinitionOpenResult(false, null, null, List.of(), failureCode);
    }

    /** Rejects operations after process-scoped ownership has ended. */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Authoring project service is closed");
        }
    }
}
