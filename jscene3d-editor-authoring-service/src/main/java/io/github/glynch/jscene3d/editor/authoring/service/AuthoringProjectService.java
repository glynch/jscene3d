/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenResult;
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
import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoadResult;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
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
import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyNumericBound;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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

    /** Stable rejection when an Inspector request no longer targets the current definition revision. */
    public static final String INSPECTOR_STALE = "authoring.inspector.stale";

    /** Stable rejection when an Inspector target does not match current Java-issued identity. */
    public static final String INSPECTOR_TARGET_INVALID = "authoring.inspector.targetInvalid";

    private static final MessageSource AUTHORING_MESSAGES = new ResourceBundleMessageSource(
            AuthoringText.class.getModule(), "io.github.glynch.jscene3d.editor.presentation.messages");

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
        if (session.revision() != validParams.expectedDefinitionRevision()) {
            return inspectorFailure(INSPECTOR_STALE);
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

    /** Maps and validates a target DTO back to the domain identity previously issued by Java. */
    private static InspectorTarget inspectorTarget(DefinitionSnapshot.SemanticTarget target) {
        InspectorTarget.Kind kind =
                switch (target.kind()) {
                    case "world" -> InspectorTarget.Kind.WORLD;
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
                        state.editable()),
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
            case InspectorValue.BooleanValue booleanValue ->
                new InspectorSnapshot.BooleanValue("boolean", booleanValue.value());
            case InspectorValue.NumberValue number ->
                new InspectorSnapshot.NumberValue("number", number.value().toPlainString());
            case InspectorValue.TextValue text -> new InspectorSnapshot.TextValue("text", text.value());
            case InspectorValue.ArrayValue array ->
                new InspectorSnapshot.ArrayValue(
                        "array",
                        array.values().stream()
                                .map(entry -> inspectorValue(entry, locale))
                                .toList());
            case InspectorValue.ObjectValue object -> {
                LinkedHashMap<String, InspectorSnapshot.Value> values = new LinkedHashMap<>();
                object.values().forEach((key, entry) -> values.put(key, inspectorValue(entry, locale)));
                yield new InspectorSnapshot.ObjectValue("object", values);
            }
            case InspectorValue.ReferenceValue reference ->
                new InspectorSnapshot.ReferenceValue(
                        "reference",
                        serialized(reference.reference().kind()),
                        reference.reference().locator(),
                        resolve(reference.label(), locale),
                        resolution(reference.resolution()),
                        reference.revealUri().map(Object::toString).orElse(null));
            case InspectorValue.EntityTargetValue entity ->
                new InspectorSnapshot.EntityTargetValue(
                        "entity-target",
                        entity.entity().toString(),
                        resolve(entity.label(), locale),
                        resolution(entity.resolution()),
                        entity.occurrence()
                                .map(AuthoringProjectService::occurrence)
                                .orElse(null));
            case InspectorValue.ComponentTargetValue component ->
                new InspectorSnapshot.ComponentTargetValue(
                        "component-target",
                        component.target().entity().toString(),
                        component.target().component().toString(),
                        resolve(component.entityLabel(), locale),
                        resolve(component.componentLabel(), locale),
                        component
                                .componentType()
                                .map(AuthoringProjectService::componentType)
                                .orElse(null),
                        resolution(component.resolution()),
                        component
                                .occurrence()
                                .map(AuthoringProjectService::occurrence)
                                .orElse(null));
        };
    }

    /** Maps one future mutation identity without exposing implementation classes. */
    private static InspectorSnapshot.MutationTarget mutationTarget(InspectorMutationTarget target) {
        return switch (target) {
            case InspectorMutationTarget.EntityEnabled entity ->
                new InspectorSnapshot.EntityEnabledMutation(
                        "entity-enabled",
                        occurrence(entity.occurrence()),
                        entity.entity().toString());
            case InspectorMutationTarget.ComponentProperty property ->
                new InspectorSnapshot.ComponentPropertyMutation(
                        "component-property",
                        occurrence(property.occurrence()),
                        property.entity().toString(),
                        property.component().toString(),
                        property.property().value());
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

    /** Creates an operation-level Inspector rejection without leaking a stale generation. */
    private static InspectorReadResult inspectorFailure(String failureCode) {
        return new InspectorReadResult(false, null, null, List.of(), failureCode);
    }

    /** Rejects operations after process-scoped ownership has ended. */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Authoring project service is closed");
        }
    }
}
