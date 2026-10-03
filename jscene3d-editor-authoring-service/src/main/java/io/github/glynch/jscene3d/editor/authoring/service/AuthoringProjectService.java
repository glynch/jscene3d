/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionBackupResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionMutationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOperationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOperationResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionRestoreParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionSnapshot;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorReadResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorSnapshot;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectCloseResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectDiagnosticDto;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectSummary;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewSnapshotDto;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchSpecification;
import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoadResult;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.project.session.AuthoringBackupResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringMutation;
import io.github.glynch.jscene3d.editor.project.session.AuthoringMutationResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringPersistenceResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringRestoreResult;
import io.github.glynch.jscene3d.editor.project.session.DefinitionRetentionResult;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.project.session.EditorRetainedDefinition;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProjection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProperty;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorSection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorValue;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjectionResult;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyNumericBound;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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

    private static final MessageSource AUTHORING_MESSAGES = new ResourceBundleMessageSource(
            AuthoringText.class.getModule(), "io.github.glynch.jscene3d.editor.presentation.messages");

    private final EditorProjectLoader loader;
    private final ProjectDiagnosticMessageResolver diagnosticMessages;
    private final String engineVersion;
    private final List<Path> runtimeArtifacts;

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
                    false, null, diagnostics(loaded.diagnostics(), locale), VIEWPORT_SCENE_UNAVAILABLE);
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
        return new ViewportLaunchResult(true, launch, diagnostics(session.diagnostics(), locale), null);
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
     * Applies one exact scalar SET or property REMOVE through the authoritative working copy.
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
        InspectorMutationTarget target = mutationTarget(validParams.target());
        AuthoringMutation mutation;
        try {
            mutation = "remove".equals(validParams.operation())
                    ? new AuthoringMutation.Remove()
                    : new AuthoringMutation.Set(projectValue(Objects.requireNonNull(validParams.value(), "value")));
        } catch (NumberFormatException exception) {
            return invalidNumericLiteral(session, definition, target, validParams, validLocale);
        }
        AuthoringMutationResult result =
                session.mutate(definition, target, mutation, validParams.expectedDefinitionRevision());
        return operationResult(result, validLocale);
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
        return new DefinitionBackupResult(
                result.definition().toString(),
                serialized(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                result.backup()
                        .map(backup -> Base64.getEncoder().encodeToString(backup.encode()))
                        .orElse(null));
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
        return operationResult(result, validLocale);
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
            InspectorProjection projection =
                    session.inspect(inspectorTarget(validParams.target()), validParams.expectedDefinitionRevision());
            InspectorSnapshot snapshot =
                    inspectorSnapshot(validParams.expectedDefinitionRevision(), projection, validLocale);
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
        SceneViewSnapshotDto snapshot = projection
                .snapshot()
                .map(AuthoringProjectService::sceneViewSnapshot)
                .orElse(null);
        SceneViewReadResult.LaunchSpecification launch = projection
                .snapshot()
                .map(ignored -> sceneViewLaunch(session, scene))
                .orElse(null);
        return new SceneViewReadResult(
                true,
                activeProjectGeneration,
                validParams.sceneAssetId(),
                validParams.expectedDefinitionRevision(),
                serialized(projection.outcome()),
                projection.currentRevision().isPresent()
                        ? projection.currentRevision().orElseThrow()
                        : null,
                snapshot,
                launch,
                diagnostics(projection.diagnostics(), validLocale),
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

    /** Maps the retained domain session to the deliberately narrow first-milestone summary. */
    private static ProjectSummary summary(EditorProjectSession session) {
        GameProject project = session.project();
        ProjectSummary.SceneSummary mainScene = project.runtime()
                .mainScene()
                .flatMap(reference -> session.definitions()
                        .loadScene(reference, session.types())
                        .definition())
                .map(scene -> new ProjectSummary.SceneSummary(scene.id().toString(), scene.name()))
                .orElse(null);
        return new ProjectSummary(
                project.identity().id(),
                project.identity().name(),
                project.identity().version(),
                project.root().toString(),
                project.descriptor().toString(),
                mainScene,
                new ProjectSummary.AssetCounts(
                        session.authoredAssets().assets().size(),
                        session.assets().size()),
                catalog(session, mainScene));
    }

    /** Projects only Java-classified structural definitions into the semantic Project catalog. */
    private static ProjectSummary.ProjectCatalog catalog(
            EditorProjectSession session, ProjectSummary.@Nullable SceneSummary mainScene) {
        Map<String, ProjectAsset> projections = session.assets().stream()
                .filter(asset -> asset.kind() == ProjectAsset.Kind.SCENE_DEFINITION
                        || asset.kind() == ProjectAsset.Kind.ENTITY_DEFINITION)
                .collect(Collectors.toUnmodifiableMap(ProjectAsset::identity, asset -> asset));
        List<ProjectSummary.CatalogEntry> scenes = session.authoredAssets().assets().stream()
                .filter(asset -> asset.kind() == AssetKind.SCENE_DEFINITION)
                .map(asset -> catalogEntry(
                        asset,
                        projections,
                        mainScene != null && mainScene.id().equals(asset.id().toString())))
                .toList();
        List<ProjectSummary.CatalogEntry> entityDefinitions = session.authoredAssets().assets().stream()
                .filter(asset -> asset.kind() == AssetKind.ENTITY_DEFINITION)
                .map(asset -> catalogEntry(asset, projections, false))
                .toList();
        return new ProjectSummary.ProjectCatalog(scenes, entityDefinitions);
    }

    /** Maps one AssetCatalog-derived authored definition without inferring semantics from its path. */
    private static ProjectSummary.CatalogEntry catalogEntry(
            AssetMetadata metadata, Map<String, ProjectAsset> projections, boolean mainScene) {
        ProjectAsset projection =
                Objects.requireNonNull(projections.get(metadata.id().toString()), "definition projection");
        return new ProjectSummary.CatalogEntry(
                metadata.id().toString(),
                projection.label(),
                metadata.path().toUri().toString(),
                "authored",
                true,
                mainScene);
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

    /** Builds the fixed safe-renderer launch context without exposing runtime implementation artifacts. */
    private SceneViewReadResult.LaunchSpecification sceneViewLaunch(EditorProjectSession session, AssetId scene) {
        GameProject project = session.project();
        Path publishedContentRoot = EditorProjectLoader.resolvePublishedContentRoot(project.root());
        String sceneName = session.assets().stream()
                .filter(asset -> scene.toString().equals(asset.identity()))
                .map(ProjectAsset::label)
                .findFirst()
                .orElse(scene.toString());
        return new SceneViewReadResult.LaunchSpecification(
                project.identity().id(),
                project.identity().name(),
                project.root().toString(),
                publishedContentRoot.toString(),
                engineVersion,
                sceneName);
    }

    /** Maps a runtime-free domain snapshot to the explicit protocol representation. */
    private static SceneViewSnapshotDto sceneViewSnapshot(SceneViewSnapshot snapshot) {
        return new SceneViewSnapshotDto(
                snapshot.scene().toString(),
                snapshot.revision(),
                snapshot.occurrences().stream()
                        .map(AuthoringProjectService::sceneViewOccurrence)
                        .toList());
    }

    /** Maps one expanded visual occurrence. */
    private static SceneViewSnapshotDto.VisualOccurrence sceneViewOccurrence(
            SceneViewSnapshot.VisualOccurrence occurrence) {
        return new SceneViewSnapshotDto.VisualOccurrence(
                sceneViewOccurrenceId(occurrence.occurrence()),
                occurrence
                        .parent()
                        .map(AuthoringProjectService::sceneViewOccurrenceId)
                        .orElse(null),
                occurrence.authoredAsset().toString(),
                occurrence.authoredSource().toString(),
                occurrence.authoredEntity().toString(),
                occurrence.name().orElse(null),
                occurrence.enabled(),
                occurrence
                        .transform()
                        .map(AuthoringProjectService::sceneViewTransform)
                        .orElse(null),
                occurrence.meshes().stream()
                        .map(AuthoringProjectService::sceneViewMesh)
                        .toList(),
                occurrence
                        .directionalLight()
                        .map(AuthoringProjectService::sceneViewDirectionalLight)
                        .orElse(null));
    }

    /** Maps one stable expanded occurrence identity. */
    private static SceneViewSnapshotDto.Occurrence sceneViewOccurrenceId(CompositionOccurrenceId occurrence) {
        return new SceneViewSnapshotDto.Occurrence(
                occurrence.rootDefinition().toString(),
                occurrence.entityPath().stream().map(Object::toString).toList());
    }

    /** Maps one stable visual-component identity. */
    private static SceneViewSnapshotDto.ComponentIdentity sceneViewComponentIdentity(
            SceneViewSnapshot.ComponentIdentity identity) {
        return new SceneViewSnapshotDto.ComponentIdentity(
                sceneViewOccurrenceId(identity.occurrence()),
                new SceneViewSnapshotDto.Scope(
                        identity.scope().definition().toString(),
                        sceneViewOccurrenceId(identity.scope().anchor())),
                identity.authoredEntity().toString(),
                identity.component().toString());
    }

    /** Maps an exact decimal vector without binary floating-point conversion. */
    private static SceneViewSnapshotDto.Vector3 sceneViewVector(SceneViewSnapshot.Vector3 vector) {
        return new SceneViewSnapshotDto.Vector3(
                vector.x().toPlainString(),
                vector.y().toPlainString(),
                vector.z().toPlainString());
    }

    /** Maps one built-in transform projection. */
    private static SceneViewSnapshotDto.Transform3d sceneViewTransform(SceneViewSnapshot.Transform3d transform) {
        return new SceneViewSnapshotDto.Transform3d(
                sceneViewComponentIdentity(transform.identity()),
                sceneViewVector(transform.position()),
                sceneViewVector(transform.orientationDegrees()),
                sceneViewVector(transform.scale()));
    }

    /** Maps one unrealized renderer-resource reference. */
    private static SceneViewSnapshotDto.ResourceReference sceneViewResource(ResourceReference reference) {
        return new SceneViewSnapshotDto.ResourceReference(
                serialized(reference.kind()),
                reference.locator(),
                reference.projectPath().map(Path::toString).orElse(null));
    }

    /** Maps one built-in mesh projection. */
    private static SceneViewSnapshotDto.MeshRenderer3d sceneViewMesh(SceneViewSnapshot.MeshRenderer3d mesh) {
        return new SceneViewSnapshotDto.MeshRenderer3d(
                sceneViewComponentIdentity(mesh.identity()),
                sceneViewResource(mesh.mesh()),
                sceneViewResource(mesh.material()),
                mesh.visible());
    }

    /** Maps one built-in directional-light projection. */
    private static SceneViewSnapshotDto.DirectionalLight3d sceneViewDirectionalLight(
            SceneViewSnapshot.DirectionalLight3d light) {
        return new SceneViewSnapshotDto.DirectionalLight3d(
                sceneViewComponentIdentity(light.identity()),
                sceneViewVector(light.color()),
                light.intensity().toPlainString(),
                sceneViewVector(light.target()));
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

    /** Maps and validates a target DTO back to the domain identity previously issued by Java. */
    private static InspectorTarget inspectorTarget(DefinitionSnapshot.SemanticTarget target) {
        InspectorTarget.Kind kind =
                switch (target.kind()) {
                    case "scene" -> InspectorTarget.Kind.SCENE;
                    case "local-entity" -> InspectorTarget.Kind.LOCAL_ENTITY;
                    case "generated-entity" -> InspectorTarget.Kind.GENERATED_ENTITY;
                    case "placement" -> InspectorTarget.Kind.PLACEMENT;
                    case "asset" -> InspectorTarget.Kind.ASSET;
                    default ->
                        throw new IllegalArgumentException("Unsupported Inspector target kind: " + target.kind());
                };
        Optional<HierarchyOccurrenceId> occurrence = Optional.ofNullable(target.occurrence())
                .map(value -> new HierarchyOccurrenceId(
                        AssetId.from(value.definitionAssetId()),
                        value.entityPath().stream().map(EntityId::from).toList()));
        return new InspectorTarget(kind, URI.create(target.source()), target.identity(), occurrence);
    }

    /** Maps one mutation target that was previously issued in an Inspector snapshot. */
    private static InspectorMutationTarget mutationTarget(DefinitionMutationParams.MutationTarget target) {
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(
                AssetId.from(target.occurrence().definitionAssetId()),
                target.occurrence().entityPath().stream().map(EntityId::from).toList());
        EntityId entity = EntityId.from(target.entityId());
        return switch (target.kind()) {
            case "entity-enabled" -> new InspectorMutationTarget.EntityEnabled(occurrence, entity);
            case "component-property" ->
                new InspectorMutationTarget.ComponentProperty(
                        occurrence,
                        entity,
                        ComponentId.from(Objects.requireNonNull(target.componentId(), "componentId")),
                        new PropertyId(Objects.requireNonNull(target.propertyId(), "propertyId")));
            default -> throw new IllegalArgumentException("Unsupported mutation target kind: " + target.kind());
        };
    }

    /** Maps an exact supported scalar transport candidate without binary floating-point conversion. */
    private static ProjectValue projectValue(DefinitionMutationParams.CandidateValue value) {
        return switch (value.kind()) {
            case "boolean" -> new ProjectValue.BooleanValue(Objects.requireNonNull(value.value(), "value"));
            case "integer", "number" ->
                new ProjectValue.NumberValue(new BigDecimal(Objects.requireNonNull(value.literal(), "literal")));
            case "text" -> new ProjectValue.TextValue(Objects.requireNonNull(value.literal(), "literal"));
            default -> throw new IllegalArgumentException("Unsupported mutation candidate kind: " + value.kind());
        };
    }

    /** Returns a structured authoritative rejection for malformed exact numeric transport text. */
    private DefinitionOperationResult invalidNumericLiteral(
            EditorProjectSession session,
            AssetId definition,
            InspectorMutationTarget target,
            DefinitionMutationParams params,
            Locale locale) {
        DefinitionMutationParams.CandidateValue candidate = Objects.requireNonNull(params.value(), "value");
        URI source = session.retainDefinition(definition)
                .definition()
                .map(EditorRetainedDefinition::source)
                .orElseGet(() -> session.project().root().toUri());
        String property = target
                        instanceof
                        InspectorMutationTarget.ComponentProperty(
                                var occurrence,
                                var entity,
                                var component,
                                PropertyId propertyId)
                ? propertyId.toString()
                : "enabled";
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                PropertyValidationDiagnosticCode.KIND,
                source,
                "",
                List.of(property, candidate.kind().toUpperCase(Locale.ROOT), "invalid numeric literal"),
                Map.of("candidateKind", candidate.kind(), "technicalDetail", "numeric literal could not be parsed"));
        AuthoringMutationResult result =
                session.rejectMutation(definition, target, params.expectedDefinitionRevision(), List.of(diagnostic));
        return operationResult(result, locale);
    }

    /** Maps a domain projection to the explicit locale-resolved Inspector wire snapshot. */
    private static InspectorSnapshot inspectorSnapshot(long revision, InspectorProjection projection, Locale locale) {
        return new InspectorSnapshot(
                revision,
                target(projection.target()),
                resolve(projection.title(), locale),
                projection.definitionOrigin().name().toLowerCase(Locale.ROOT),
                projection.provenance().name().toLowerCase(Locale.ROOT),
                projection.editable(),
                projection.sections().stream()
                        .map(section -> inspectorGroup(section, locale))
                        .toList());
    }

    /** Maps one selectable Inspector group. */
    private static InspectorSnapshot.TargetGroup inspectorGroup(InspectorSection section, Locale locale) {
        return new InspectorSnapshot.TargetGroup(
                section.identity(),
                section.kind().name().toLowerCase(Locale.ROOT),
                resolve(section.label(), locale),
                section.description().map(value -> resolve(value, locale)).orElse(null),
                section.componentId().map(Object::toString).orElse(null),
                section.componentType()
                        .map(AuthoringProjectService::componentType)
                        .orElse(null),
                section.metadataAvailable() ? "available" : "unavailable",
                section.editable(),
                section.properties().stream()
                        .map(property -> inspectorProperty(property, locale))
                        .toList());
    }

    /** Maps one Inspector property without exposing raw descriptor metadata. */
    private static InspectorSnapshot.Property inspectorProperty(InspectorProperty property, Locale locale) {
        InspectorProperty.Presentation presentation = property.presentation();
        InspectorProperty.State state = property.state();
        return new InspectorSnapshot.Property(
                property.identity(),
                resolve(presentation.label(), locale),
                presentation.description().map(value -> resolve(value, locale)).orElse(null),
                serialized(presentation.valueKind()),
                presentation.required(),
                new InspectorSnapshot.Constraints(
                        presentation
                                .constraints()
                                .elementKind()
                                .map(AuthoringProjectService::serialized)
                                .orElse(null),
                        presentation.constraints().exactElementCount().orElse(null),
                        presentation.constraints().acceptedReferenceKinds().stream()
                                .map(AuthoringProjectService::serialized)
                                .sorted()
                                .toList(),
                        new InspectorSnapshot.EditorSemantics(
                                presentation.constraints().editor().semantic().serializedName(),
                                presentation
                                        .constraints()
                                        .editor()
                                        .minimum()
                                        .map(AuthoringProjectService::numericBound)
                                        .orElse(null),
                                presentation
                                        .constraints()
                                        .editor()
                                        .maximum()
                                        .map(AuthoringProjectService::numericBound)
                                        .orElse(null))),
                new InspectorSnapshot.PropertyState(
                        state.authoredValue()
                                .map(value -> inspectorValue(value, locale))
                                .orElse(null),
                        state.defaultValue()
                                .map(value -> inspectorValue(value, locale))
                                .orElse(null),
                        state.effectiveValue()
                                .map(value -> inspectorValue(value, locale))
                                .orElse(null),
                        state.origin().name().toLowerCase(Locale.ROOT),
                        state.validity().name().toLowerCase(Locale.ROOT).replace('_', '-'),
                        state.editable(),
                        state.modified()),
                property.mutationTarget()
                        .map(AuthoringProjectService::mutationTarget)
                        .orElse(null));
    }

    /** Maps one exact numeric bound without converting it to binary floating point. */
    private static InspectorSnapshot.NumericBound numericBound(PropertyNumericBound bound) {
        return new InspectorSnapshot.NumericBound(bound.value().toPlainString(), bound.isInclusive());
    }

    /** Recursively maps one semantic Inspector value to explicit tagged wire data. */
    private static InspectorSnapshot.Value inspectorValue(InspectorValue value, Locale locale) {
        return switch (value) {
            case InspectorValue.NullValue ignored -> new InspectorSnapshot.NullValue("null");
            case InspectorValue.BooleanValue(boolean booleanValue) ->
                new InspectorSnapshot.BooleanValue("boolean", booleanValue);
            case InspectorValue.NumberValue(BigDecimal number) ->
                new InspectorSnapshot.NumberValue("number", number.toPlainString());
            case InspectorValue.TextValue(String text) -> new InspectorSnapshot.TextValue("text", text);
            case InspectorValue.ArrayValue(List<InspectorValue> entries) ->
                new InspectorSnapshot.ArrayValue(
                        "array",
                        entries.stream()
                                .map(entry -> inspectorValue(entry, locale))
                                .toList());
            case InspectorValue.ObjectValue(Map<String, InspectorValue> entries) -> {
                LinkedHashMap<String, InspectorSnapshot.Value> values = new LinkedHashMap<>();
                entries.forEach((key, entry) -> values.put(key, inspectorValue(entry, locale)));
                yield new InspectorSnapshot.ObjectValue("object", values);
            }
            case InspectorValue.ReferenceValue(
                    ResourceReference reference,
                    AuthoringText label,
                    InspectorValue.Resolution referenceResolution,
                    Optional<URI> revealUri) ->
                new InspectorSnapshot.ReferenceValue(
                        "reference",
                        serialized(reference.kind()),
                        reference.locator(),
                        resolve(label, locale),
                        resolution(referenceResolution),
                        revealUri.map(Object::toString).orElse(null));
            case InspectorValue.EntityTargetValue(
                    EntityId entity,
                    AuthoringText label,
                    InspectorValue.Resolution targetResolution,
                    Optional<HierarchyOccurrenceId> targetOccurrence) ->
                new InspectorSnapshot.EntityTargetValue(
                        "entity-target",
                        entity.toString(),
                        resolve(label, locale),
                        resolution(targetResolution),
                        targetOccurrence
                                .map(AuthoringProjectService::occurrence)
                                .orElse(null));
            case InspectorValue.ComponentTargetValue(
                    ComponentTarget target,
                    AuthoringText entityLabel,
                    AuthoringText componentLabel,
                    Optional<ComponentType> targetType,
                    InspectorValue.Resolution targetResolution,
                    Optional<HierarchyOccurrenceId> targetOccurrence) ->
                new InspectorSnapshot.ComponentTargetValue(
                        "component-target",
                        target.entity().toString(),
                        target.component().toString(),
                        resolve(entityLabel, locale),
                        resolve(componentLabel, locale),
                        targetType.map(AuthoringProjectService::componentType).orElse(null),
                        resolution(targetResolution),
                        targetOccurrence
                                .map(AuthoringProjectService::occurrence)
                                .orElse(null));
        };
    }

    /** Maps one future mutation identity without exposing implementation classes. */
    private static InspectorSnapshot.MutationTarget mutationTarget(InspectorMutationTarget target) {
        return switch (target) {
            case InspectorMutationTarget.EntityEnabled(HierarchyOccurrenceId occurrence, EntityId entity) ->
                new InspectorSnapshot.EntityEnabledMutation(
                        "entity-enabled", occurrence(occurrence), entity.toString());
            case InspectorMutationTarget.ComponentProperty(
                    HierarchyOccurrenceId occurrence,
                    EntityId entity,
                    ComponentId component,
                    PropertyId property) ->
                new InspectorSnapshot.ComponentPropertyMutation(
                        "component-property",
                        occurrence(occurrence),
                        entity.toString(),
                        component.toString(),
                        property.value());
        };
    }

    /** Maps one exact component type. */
    private static InspectorSnapshot.ComponentTypeDto componentType(ComponentType type) {
        return new InspectorSnapshot.ComponentTypeDto(type.id().value(), type.version());
    }

    /** Resolves Java-owned semantic text while preserving authored literals. */
    private static String resolve(AuthoringText text, Locale locale) {
        return text.resolve(AUTHORING_MESSAGES, locale);
    }

    /** Serializes structural value kinds independently of Java enum spelling. */
    private static String serialized(ProjectValueKind kind) {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** Serializes reference namespaces independently of Java enum spelling. */
    private static String serialized(ResourceReference.Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    /** Serializes semantic resolution independently of Java enum spelling. */
    private static String resolution(InspectorValue.Resolution resolution) {
        return resolution.name().toLowerCase(Locale.ROOT);
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
        return operationResult(result, validLocale);
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
        return operationResult(result, validLocale);
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

    /** Maps an authoritative mutation/history result to the wire contract. */
    private DefinitionOperationResult operationResult(AuthoringMutationResult result, Locale locale) {
        return new DefinitionOperationResult(
                result.definition().toString(),
                serialized(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                diagnostics(result.diagnostics(), locale));
    }

    /** Maps an authoritative persistence result to the wire contract. */
    private DefinitionOperationResult operationResult(AuthoringPersistenceResult result, Locale locale) {
        return new DefinitionOperationResult(
                result.definition().toString(),
                serialized(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                diagnostics(result.diagnostics(), locale));
    }

    /** Maps an authoritative recovery result to the wire contract. */
    private DefinitionOperationResult operationResult(AuthoringRestoreResult result, Locale locale) {
        return new DefinitionOperationResult(
                result.definition().toString(),
                serialized(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                diagnostics(result.diagnostics(), locale));
    }

    /** Serializes closed Java enum outcomes independently of enum spelling. */
    private static String serialized(Enum<?> outcome) {
        return outcome.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** Preserves authored literals and structured Java-owned semantic text. */
    private static DefinitionSnapshot.AuthoringTextDto text(AuthoringText value) {
        return switch (value) {
            case AuthoringText.Literal(String literal) ->
                new DefinitionSnapshot.AuthoringTextDto("literal", literal, null, List.of());
            case AuthoringText.Message(String code, String defaultMessage, List<Object> arguments) ->
                new DefinitionSnapshot.AuthoringTextDto(
                        "message",
                        defaultMessage,
                        code,
                        arguments.stream().map(Object::toString).toList());
        };
    }

    /** Serializes hierarchy kinds independently of Java enum spelling. */
    private static String serialized(EditorHierarchyNode.Kind kind) {
        return switch (kind) {
            case SCENE -> "scene";
            case LOCAL_ENTITY -> "local-entity";
            case PLACEMENT -> "placement";
            case GENERATED_ENTITY -> "generated-entity";
        };
    }

    /** Serializes Inspector target kinds independently of Java enum spelling. */
    private static String serialized(InspectorTarget.Kind kind) {
        return switch (kind) {
            case SCENE -> "scene";
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
