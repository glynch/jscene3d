/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionBackupResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionInteractiveEditParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionMutationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOperationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOperationResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionRestoreParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorReadResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorSnapshot;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectCloseResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectDiagnosticDto;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewSnapshotDto;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchSpecification;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoadResult;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.project.session.AuthoringBackupResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringInteractivePhase;
import io.github.glynch.jscene3d.editor.project.session.AuthoringMutation;
import io.github.glynch.jscene3d.editor.project.session.AuthoringMutationResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringPersistenceResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringRestoreResult;
import io.github.glynch.jscene3d.editor.project.session.DefinitionRetentionResult;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.project.session.EditorRetainedDefinition;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProjection;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjectionResult;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.Base64;
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

    /** Stable failure when a requested viewport Scene is unavailable in the active project. */
    public static final String VIEWPORT_SCENE_UNAVAILABLE = "authoring.viewport.sceneUnavailable";

    /** Stable failure when a requested structural definition cannot be resolved. */
    public static final String DEFINITION_UNAVAILABLE = "authoring.definition.unavailable";

    /** Stable rejection when an Inspector request no longer targets the current definition revision. */
    public static final String INSPECTOR_STALE = "authoring.inspector.stale";

    /** Stable rejection when an Inspector target does not match current Java-issued identity. */
    public static final String INSPECTOR_TARGET_INVALID = "authoring.inspector.targetInvalid";

    private final EditorProjectLoader loader;
    private final String engineVersion;
    private final List<Path> runtimeArtifacts;
    private final DiagnosticProtocolMapper diagnosticProtocol;
    private final ProjectProtocolMapper projectProtocol;
    private final DefinitionProtocolMapper definitionProtocol;
    private final InspectorProtocolMapper inspectorProtocol;
    private final SceneViewProtocolMapper sceneViewProtocol;
    private final AuthoringOperationProtocolMapper operationProtocol;

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
        this(loader, "0.1.0-SNAPSHOT", List.of(), new ProjectDiagnosticMessageResolver());
    }

    /**
     * Creates a service with the running engine version and renderer-only runtime artifacts.
     *
     * @param loader headless authoring project loader
     * @param engineVersion running JScene3D engine version
     * @param runtimeArtifacts artifacts available only to isolated renderer processes
     */
    public AuthoringProjectService(EditorProjectLoader loader, String engineVersion, List<Path> runtimeArtifacts) {
        this(loader, engineVersion, runtimeArtifacts, new ProjectDiagnosticMessageResolver());
    }

    /** Creates a service with an explicit diagnostic presentation resolver. */
    AuthoringProjectService(EditorProjectLoader loader, ProjectDiagnosticMessageResolver diagnosticMessages) {
        this(loader, "0.1.0-SNAPSHOT", List.of(), diagnosticMessages);
    }

    /** Creates a service with explicit launch and diagnostic collaborators. */
    AuthoringProjectService(
            EditorProjectLoader loader,
            String engineVersion,
            List<Path> runtimeArtifacts,
            ProjectDiagnosticMessageResolver diagnosticMessages) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.engineVersion = requireNonBlank(engineVersion, "engineVersion");
        this.runtimeArtifacts = runtimeArtifacts.stream()
                .map(path -> Objects.requireNonNull(path, "runtimeArtifact")
                        .toAbsolutePath()
                        .normalize())
                .toList();
        this.diagnosticProtocol = new DiagnosticProtocolMapper(diagnosticMessages);
        this.projectProtocol = new ProjectProtocolMapper();
        this.definitionProtocol = new DefinitionProtocolMapper();
        this.inspectorProtocol = new InspectorProtocolMapper(definitionProtocol);
        this.sceneViewProtocol = new SceneViewProtocolMapper(this.engineVersion);
        this.operationProtocol = new AuthoringOperationProtocolMapper(diagnosticProtocol);
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
        List<ProjectDiagnosticDto> diagnostics = diagnosticProtocol.map(loadResult.diagnostics(), locale);
        if (loadResult.session().isEmpty()) {
            return new ProjectOpenResult(false, null, null, diagnostics, null);
        }

        EditorProjectSession session = loadResult.session().orElseThrow();
        long projectGeneration = nextProjectGeneration++;
        activeSession = session;
        activeProjectGeneration = projectGeneration;
        return new ProjectOpenResult(true, projectGeneration, projectProtocol.summary(session), diagnostics, null);
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
        List<ProjectDiagnosticDto> diagnostics = diagnosticProtocol.map(loadResult.diagnostics(), locale);
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
                ProjectReplaceResult.REPLACED,
                replacementGeneration,
                projectProtocol.summary(candidate),
                diagnostics,
                null);
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
     * Prepares a renderer launch for an authoritative Scene in the active project.
     *
     * <p>The first product viewport renders the Main Scene. Requiring the caller to echo its Java-issued
     * {@link AssetId} prevents display labels or paths from becoming identity at the editor boundary.
     *
     * @param params expected generation and Scene identity
     * @return generation-scoped launch specification or stable rejection
     */
    public ViewportLaunchResult prepareViewportLaunch(ViewportLaunchParams params) {
        return prepareViewportLaunch(params, Locale.ENGLISH);
    }

    /**
     * Prepares a renderer launch using the initialized client locale for diagnostics.
     *
     * @param params expected generation and Scene identity
     * @param locale locale used to render launch diagnostics
     * @return generation-scoped launch specification or stable rejection
     */
    public synchronized ViewportLaunchResult prepareViewportLaunch(ViewportLaunchParams params, Locale locale) {
        ensureOpen();
        ViewportLaunchParams validParams = Objects.requireNonNull(params, "params");
        Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = activeSession;
        if (session == null) {
            return viewportFailure(PROJECT_NOT_OPEN);
        }
        if (activeProjectGeneration != validParams.expectedProjectGeneration()) {
            return viewportFailure(PROJECT_GENERATION_CONFLICT);
        }

        AssetId requestedScene;
        try {
            requestedScene = AssetId.from(validParams.sceneAssetId());
        } catch (IllegalArgumentException exception) {
            return viewportFailure(VIEWPORT_SCENE_UNAVAILABLE);
        }
        Optional<AssetMetadata> metadata = session.authoredAssets().find(requestedScene);
        if (metadata.isEmpty() || metadata.orElseThrow().kind() != AssetKind.SCENE_DEFINITION) {
            return viewportFailure(VIEWPORT_SCENE_UNAVAILABLE);
        }
        DefinitionLoadResult<SceneDefinition> loaded =
                session.definitions().loadScene(AssetRef.to(requestedScene), session.types());
        if (loaded.definition().isEmpty()) {
            return new ViewportLaunchResult(
                    false, null, diagnosticProtocol.map(loaded.diagnostics(), locale), VIEWPORT_SCENE_UNAVAILABLE);
        }
        SceneDefinition scene = loaded.definition().orElseThrow();

        GameProject project = session.project();
        Path publishedContentRoot = EditorProjectLoader.resolvePublishedContentRoot(project.root());
        ViewportLaunchSpecification launch = new ViewportLaunchSpecification(
                activeProjectGeneration,
                project.identity().id(),
                project.identity().name(),
                project.root().toString(),
                publishedContentRoot.toString(),
                engineVersion,
                scene.id().toString(),
                scene.name(),
                runtimeArtifacts.stream().map(Path::toString).toList());
        return new ViewportLaunchResult(true, launch, diagnosticProtocol.map(session.diagnostics(), locale), null);
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
        List<ProjectDiagnosticDto> mappedDiagnostics = diagnosticProtocol.map(result.diagnostics(), locale);
        if (result.definition().isEmpty()) {
            return new DefinitionOpenResult(false, null, null, mappedDiagnostics, DEFINITION_UNAVAILABLE);
        }
        EditorRetainedDefinition definition = result.definition().orElseThrow();
        return new DefinitionOpenResult(
                true, activeProjectGeneration, definitionProtocol.snapshot(definition), mappedDiagnostics, null);
    }

    /**
     * Applies one exact typed SET or property REMOVE through the authoritative working copy.
     *
     * @param params mutation identity, target, operation, and candidate value
     * @param locale initialized client display locale
     * @return authoritative operation state and localized validation diagnostics
     */
    public synchronized DefinitionOperationResult mutateDefinition(DefinitionMutationParams params, Locale locale) {
        ensureOpen();
        DefinitionMutationParams validParams = Objects.requireNonNull(params, "params");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = operationSession(validParams.expectedProjectGeneration());
        if (session == null) {
            return operationFailure(validParams.assetId(), validParams.expectedDefinitionRevision());
        }
        AssetId definition = AssetId.from(validParams.assetId());
        InspectorMutationTarget target = operationProtocol.target(validParams.target());
        AuthoringMutation mutation;
        try {
            mutation = "remove".equals(validParams.operation())
                    ? new AuthoringMutation.Remove()
                    : new AuthoringMutation.Set(
                            operationProtocol.value(Objects.requireNonNull(validParams.value(), "value")));
        } catch (NumberFormatException exception) {
            return operationProtocol.invalidNumericLiteral(
                    session,
                    definition,
                    target,
                    Objects.requireNonNull(validParams.value(), "value"),
                    validParams.expectedDefinitionRevision(),
                    validLocale);
        }
        AuthoringMutationResult result =
                session.mutate(definition, target, mutation, validParams.expectedDefinitionRevision());
        return operationProtocol.result(result, validLocale);
    }

    /**
     * Applies one ordered phase of an interactive component-property edit.
     *
     * @param params transaction identity, target, phase, and optional preview candidate
     * @param locale initialized client display locale
     * @return authoritative operation state and localized validation diagnostics
     */
    public synchronized DefinitionOperationResult interactDefinition(
            DefinitionInteractiveEditParams params, Locale locale) {
        ensureOpen();
        DefinitionInteractiveEditParams validParams = Objects.requireNonNull(params, "params");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = operationSession(validParams.expectedProjectGeneration());
        if (session == null) {
            return operationFailure(validParams.assetId(), validParams.expectedDefinitionRevision());
        }
        AssetId definition = AssetId.from(validParams.assetId());
        InspectorMutationTarget target = operationProtocol.target(validParams.target());
        if (!(target instanceof InspectorMutationTarget.ComponentProperty property)) {
            throw new IllegalArgumentException("interactive edit requires a component property target");
        }
        Optional<ProjectValue> value;
        try {
            value = validParams.value() == null
                    ? Optional.empty()
                    : Optional.of(operationProtocol.value(validParams.value()));
        } catch (NumberFormatException exception) {
            return operationProtocol.invalidNumericLiteral(
                    session,
                    definition,
                    target,
                    Objects.requireNonNull(validParams.value(), "value"),
                    validParams.expectedDefinitionRevision(),
                    validLocale);
        }
        AuthoringMutationResult result = session.interact(
                definition,
                property,
                AuthoringInteractivePhase.valueOf(validParams.phase().toUpperCase(Locale.ROOT)),
                validParams.editId(),
                value,
                validParams.expectedDefinitionRevision());
        return operationProtocol.result(result, validLocale);
    }

    /**
     * Invokes Java-owned undo for one authored definition.
     *
     * @param params definition identity and expected revision
     * @param locale initialized client display locale
     * @return authoritative operation state and localized diagnostics
     */
    public synchronized DefinitionOperationResult undoDefinition(DefinitionOperationParams params, Locale locale) {
        return historyOperation(params, locale, true);
    }

    /**
     * Invokes Java-owned redo for one authored definition.
     *
     * @param params definition identity and expected revision
     * @param locale initialized client display locale
     * @return authoritative operation state and localized diagnostics
     */
    public synchronized DefinitionOperationResult redoDefinition(DefinitionOperationParams params, Locale locale) {
        return historyOperation(params, locale, false);
    }

    /**
     * Invokes B2 source-preserving save for one authored definition.
     *
     * @param params definition identity and expected revision
     * @param locale initialized client display locale
     * @return authoritative operation state and localized diagnostics
     */
    public synchronized DefinitionOperationResult saveDefinition(DefinitionOperationParams params, Locale locale) {
        return persistenceOperation(params, locale, true);
    }

    /**
     * Invokes B2 authoritative revert for one authored definition.
     *
     * @param params definition identity and expected revision
     * @param locale initialized client display locale
     * @return authoritative operation state and localized diagnostics
     */
    public synchronized DefinitionOperationResult revertDefinition(DefinitionOperationParams params, Locale locale) {
        return persistenceOperation(params, locale, false);
    }

    /**
     * Captures the exact B3 recovery representation without interpreting it in the client.
     *
     * @param params definition identity and expected revision
     * @return authoritative operation state and opaque recovery representation
     */
    public synchronized DefinitionBackupResult backupDefinition(DefinitionOperationParams params) {
        ensureOpen();
        DefinitionOperationParams validParams = Objects.requireNonNull(params, "params");
        EditorProjectSession session = operationSession(validParams.expectedProjectGeneration());
        if (session == null) {
            return new DefinitionBackupResult(
                    validParams.assetId(),
                    "invalid-target",
                    validParams.expectedDefinitionRevision(),
                    false,
                    false,
                    false,
                    null);
        }
        AuthoringBackupResult result =
                session.backupDefinition(AssetId.from(validParams.assetId()), validParams.expectedDefinitionRevision());
        return operationProtocol.backup(result);
    }

    /**
     * Restores B3 recovery bytes into the newly loaded authoritative working copy.
     *
     * @param params definition identity, expected revision, and opaque recovery representation
     * @param locale initialized client display locale
     * @return authoritative operation state and localized diagnostics
     */
    public synchronized DefinitionOperationResult restoreDefinition(DefinitionRestoreParams params, Locale locale) {
        ensureOpen();
        DefinitionRestoreParams validParams = Objects.requireNonNull(params, "params");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = operationSession(validParams.expectedProjectGeneration());
        if (session == null) {
            return operationFailure(validParams.assetId(), validParams.expectedDefinitionRevision());
        }
        byte[] backup = Base64.getDecoder().decode(validParams.backup());
        AuthoringRestoreResult result = session.restoreDefinition(
                AssetId.from(validParams.assetId()), validParams.expectedDefinitionRevision(), backup);
        return operationProtocol.result(result, validLocale);
    }

    /**
     * Reads one complete generation- and revision-checked Inspector snapshot.
     *
     * @param params optimistic concurrency context and Java-issued target
     * @param locale initialized client display locale
     * @return exact success or deterministic rejection result
     */
    public synchronized InspectorReadResult readInspector(InspectorReadParams params, Locale locale) {
        ensureOpen();
        InspectorReadParams validParams = Objects.requireNonNull(params, "params");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = activeSession;
        if (session == null) {
            return inspectorFailure(PROJECT_NOT_OPEN);
        }
        if (activeProjectGeneration != validParams.expectedProjectGeneration()) {
            return inspectorFailure(PROJECT_GENERATION_CONFLICT);
        }
        try {
            InspectorProjection projection = session.inspect(
                    definitionProtocol.inspectorTarget(validParams.target()), validParams.expectedDefinitionRevision());
            InspectorSnapshot snapshot =
                    inspectorProtocol.snapshot(validParams.expectedDefinitionRevision(), projection, validLocale);
            return new InspectorReadResult(true, activeProjectGeneration, snapshot, List.of(), null);
        } catch (IllegalStateException exception) {
            return inspectorFailure(INSPECTOR_STALE);
        } catch (IllegalArgumentException exception) {
            return inspectorFailure(INSPECTOR_TARGET_INVALID);
        }
    }

    /**
     * Requests the current editor-safe Scene View projection within an active project generation.
     *
     * <p>The returned inner result remains tied to the requested Scene asset and its current working-copy revision.
     * The protocol transport adds the connection-generation envelope around this project-scoped result.
     *
     * @param expectedProjectGeneration active project generation observed by the caller
     * @param scene retained Scene asset identity
     * @param expectedDefinitionRevision Scene revision observed by the caller
     * @return project-generation-scoped projection or stable ownership rejection
     */
    public synchronized SceneViewServiceResult projectSceneView(
            long expectedProjectGeneration, AssetId scene, long expectedDefinitionRevision) {
        ensureOpen();
        AssetId validScene = Objects.requireNonNull(scene, "scene");
        EditorProjectSession session = activeSession;
        if (session == null) {
            return sceneViewFailure(PROJECT_NOT_OPEN);
        }
        if (activeProjectGeneration != expectedProjectGeneration) {
            return sceneViewFailure(PROJECT_GENERATION_CONFLICT);
        }
        SceneViewProjectionResult projection = session.projectSceneView(validScene, expectedDefinitionRevision);
        return new SceneViewServiceResult(true, activeProjectGeneration, projection, null);
    }

    /**
     * Reads one complete safe Scene View projection and its product-owned renderer launch context.
     *
     * <p>The response contains only data projected by the headless authoring model. In particular, it contains no
     * title runtime artifact list and grants no authority to execute project implementation code.
     *
     * @param params active project identity, Scene identity, and expected working-copy revision
     * @param locale initialized client display locale
     * @return exact projection outcome and safe renderer inputs
     */
    public synchronized SceneViewReadResult readSceneView(SceneViewReadParams params, Locale locale) {
        ensureOpen();
        SceneViewReadParams validParams = Objects.requireNonNull(params, "params");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = activeSession;
        if (session == null) {
            return sceneViewReadFailure(validParams, PROJECT_NOT_OPEN);
        }
        if (activeProjectGeneration != validParams.expectedProjectGeneration()) {
            return sceneViewReadFailure(validParams, PROJECT_GENERATION_CONFLICT);
        }

        AssetId scene;
        try {
            scene = AssetId.from(validParams.sceneAssetId());
        } catch (IllegalArgumentException exception) {
            return unavailableSceneView(validParams);
        }
        SceneViewProjectionResult projection =
                session.projectSceneView(scene, validParams.expectedDefinitionRevision());
        SceneViewSnapshotDto snapshot =
                projection.snapshot().map(sceneViewProtocol::snapshot).orElse(null);
        SceneViewReadResult.LaunchSpecification launch = projection
                .snapshot()
                .map(ignored -> sceneViewProtocol.launch(session, scene))
                .orElse(null);
        return new SceneViewReadResult(
                true,
                activeProjectGeneration,
                validParams.sceneAssetId(),
                validParams.expectedDefinitionRevision(),
                AuthoringOperationProtocolMapper.outcome(projection.outcome()),
                projection.currentRevision().isPresent()
                        ? projection.currentRevision().orElseThrow()
                        : null,
                snapshot,
                launch,
                diagnosticProtocol.map(projection.diagnostics(), validLocale),
                null);
    }

    /**
     * Returns the retained session for process orchestration and focused verification.
     *
     * @return active authoring session, when open
     */
    synchronized Optional<EditorProjectSession> activeSession() {
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

    /** Applies one history operation after generation validation. */
    private DefinitionOperationResult historyOperation(DefinitionOperationParams params, Locale locale, boolean undo) {
        ensureOpen();
        DefinitionOperationParams validParams = Objects.requireNonNull(params, "params");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = operationSession(validParams.expectedProjectGeneration());
        if (session == null) {
            return operationFailure(validParams.assetId(), validParams.expectedDefinitionRevision());
        }
        AssetId definition = AssetId.from(validParams.assetId());
        AuthoringMutationResult result = undo
                ? session.undo(definition, validParams.expectedDefinitionRevision())
                : session.redo(definition, validParams.expectedDefinitionRevision());
        return operationProtocol.result(result, validLocale);
    }

    /** Applies one persistence operation after generation validation. */
    private DefinitionOperationResult persistenceOperation(
            DefinitionOperationParams params, Locale locale, boolean save) {
        ensureOpen();
        DefinitionOperationParams validParams = Objects.requireNonNull(params, "params");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        EditorProjectSession session = operationSession(validParams.expectedProjectGeneration());
        if (session == null) {
            return operationFailure(validParams.assetId(), validParams.expectedDefinitionRevision());
        }
        AssetId definition = AssetId.from(validParams.assetId());
        AuthoringPersistenceResult result = save
                ? session.saveDefinition(definition, validParams.expectedDefinitionRevision())
                : session.revertDefinition(definition, validParams.expectedDefinitionRevision());
        return operationProtocol.result(result, validLocale);
    }

    /** Returns the current session only when the caller's project generation remains current. */
    private @Nullable EditorProjectSession operationSession(long expectedProjectGeneration) {
        return activeSession != null && activeProjectGeneration == expectedProjectGeneration ? activeSession : null;
    }

    /** Creates a deterministic operation rejection when project identity is no longer current. */
    private static DefinitionOperationResult operationFailure(String assetId, long expectedRevision) {
        return new DefinitionOperationResult(
                assetId, "project-generation-conflict", expectedRevision, false, false, false, List.of());
    }

    /** Creates an operation-level replacement conflict without loading a candidate. */
    private static ProjectReplaceResult conflict(String failureCode) {
        return new ProjectReplaceResult(ProjectReplaceResult.CONFLICT, null, null, List.of(), failureCode);
    }

    /** Creates an operation-level definition failure without leaking a stale generation. */
    private static DefinitionOpenResult definitionFailure(String failureCode) {
        return new DefinitionOpenResult(false, null, null, List.of(), failureCode);
    }

    /** Creates an operation-level Inspector rejection without leaking a stale generation. */
    private static InspectorReadResult inspectorFailure(String failureCode) {
        return new InspectorReadResult(false, null, null, List.of(), failureCode);
    }

    /** Creates a project-ownership rejection without leaking a stale generation. */
    private static SceneViewServiceResult sceneViewFailure(String failureCode) {
        return new SceneViewServiceResult(false, null, null, failureCode);
    }

    /** Creates a protocol-level Scene View ownership rejection without leaking a stale generation. */
    private static SceneViewReadResult sceneViewReadFailure(SceneViewReadParams params, String failureCode) {
        return new SceneViewReadResult(
                false,
                null,
                params.sceneAssetId(),
                params.expectedDefinitionRevision(),
                null,
                null,
                null,
                null,
                List.of(),
                failureCode);
    }

    /** Creates an accepted unavailable outcome for a malformed or unknown Scene identity. */
    private SceneViewReadResult unavailableSceneView(SceneViewReadParams params) {
        return new SceneViewReadResult(
                true,
                activeProjectGeneration,
                params.sceneAssetId(),
                params.expectedDefinitionRevision(),
                "scene-unavailable",
                null,
                null,
                null,
                List.of(),
                null);
    }

    /** Creates an operation-level viewport rejection without leaking stale launch inputs. */
    private static ViewportLaunchResult viewportFailure(String failureCode) {
        return new ViewportLaunchResult(false, null, List.of(), failureCode);
    }

    private static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }

    /** Rejects operations after process-scoped ownership has ended. */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Authoring project service is closed");
        }
    }
}
