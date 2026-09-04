/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.resource.ResourceLoadResult;
import io.github.glynch.jscene3d.project.resource.ResourceLoader;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactory;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Canonical-path resource cache, dependency resolver, and lifetime owner. */
final class ProjectResourceResolver {
    private final GameProject project;
    private final URI entrySceneSource;
    private final RegisteredTypeCatalog catalog;
    private final FactoryBindings factories;
    private final List<ProjectDiagnostic> runtimeDiagnostics;
    private final ResourceLoader loader = new ResourceLoader();
    private final Map<Path, Object> cache = new LinkedHashMap<>();
    private final List<Object> creationOrder = new ArrayList<>();
    private final List<Path> resolving = new ArrayList<>();
    private boolean closed;

    /** Creates an empty resolver for one project runtime. */
    ProjectResourceResolver(
            GameProject project,
            URI entrySceneSource,
            RegisteredTypeCatalog catalog,
            FactoryBindings factories,
            List<ProjectDiagnostic> runtimeDiagnostics) {
        this.project = Objects.requireNonNull(project, "project");
        this.entrySceneSource = Objects.requireNonNull(entrySceneSource, "entrySceneSource");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.factories = Objects.requireNonNull(factories, "factories");
        this.runtimeDiagnostics = Objects.requireNonNull(runtimeDiagnostics, "runtimeDiagnostics");
    }

    /** Resolves, caches, type-checks, and returns one project resource. */
    <T> T resolve(ResourceReference reference, Class<T> valueType) {
        requireOpen();
        ResourceReference validReference = Objects.requireNonNull(reference, "reference");
        Class<T> validValueType = Objects.requireNonNull(valueType, "valueType");
        if (validReference.kind() != ResourceReference.Kind.PROJECT) {
            throw failure(
                    currentSource(),
                    "runtime.resource.kind.unsupported",
                    "runtime resource resolution is not implemented for "
                            + validReference.kind().prefix(),
                    "");
        }
        ResourceLoadResult loadResult =
                loader.load(project, validReference.projectPath().orElseThrow());
        if (!loadResult.isValid()) {
            throw new RuntimeDiagnosticsException(loadResult.diagnostics());
        }
        ResourceDefinition definition = loadResult.resource().orElseThrow();
        Object value = cache.get(definition.source());
        if (value == null) {
            runtimeDiagnostics.addAll(loadResult.diagnostics());
            value = create(definition);
        }
        if (!validValueType.isInstance(value)) {
            throw failure(
                    definition.source().toUri(),
                    "runtime.resource.value.type",
                    "resource " + relative(definition.source()) + " produced "
                            + value.getClass().getName() + " but " + validValueType.getName() + " is required",
                    "");
        }
        return validValueType.cast(value);
    }

    /** Closes created resource values in reverse dependency order. */
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        for (int index = creationOrder.size() - 1; index >= 0; index--) {
            try {
                closeValue(creationOrder.get(index));
            } catch (RuntimeException closeFailure) {
                failure = combine(failure, closeFailure);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Closes one owned resource value when it exposes a close operation. */
    private static void closeValue(Object value) {
        if (!(value instanceof AutoCloseable closeable)) {
            return;
        }
        try {
            closeable.close();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("resource closure failed", exception);
        }
    }

    /** Retains the first closure failure and suppresses subsequent failures onto it. */
    private static RuntimeException combine(@Nullable RuntimeException first, RuntimeException subsequent) {
        if (first == null) {
            return subsequent;
        }
        first.addSuppressed(subsequent);
        return first;
    }

    /** Creates one uncached resource after structural and catalog validation. */
    private Object create(ResourceDefinition definition) {
        if (resolving.contains(definition.source())) {
            throw failure(
                    definition.source().toUri(),
                    "runtime.resource.cycle",
                    "resource dependency cycle: " + cycle(definition.source()),
                    "");
        }
        List<ProjectDiagnostic> catalogDiagnostics = catalog.validate(definition);
        if (hasErrors(catalogDiagnostics)) {
            throw new RuntimeDiagnosticsException(catalogDiagnostics);
        }
        runtimeDiagnostics.addAll(catalogDiagnostics);
        RegisteredTypeDescriptor descriptor = catalog.find(definition.type()).orElseThrow();
        ResourceFactory factory = requireFactory(definition);
        ResourceCreationContext context = new ResourceCreationContext(
                project, definition, EffectiveProperties.merge(descriptor, definition.properties()), this);
        resolving.add(definition.source());
        try {
            Object value = Objects.requireNonNull(factory.create(context), "resource factory result");
            cache.put(definition.source(), value);
            creationOrder.add(value);
            return value;
        } catch (RuntimeDiagnosticsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(
                    definition.source().toUri(),
                    "runtime.factory.resource.create",
                    failureMessage("resource factory failed for " + definition.type(), exception),
                    "");
        } finally {
            resolving.removeLast();
        }
    }

    /** Resolves a factory while retaining the resource document as diagnostic source. */
    private ResourceFactory requireFactory(ResourceDefinition definition) {
        try {
            return factories.requireResource(definition.type(), "/type");
        } catch (RuntimeCompositionException exception) {
            throw failure(
                    definition.source().toUri(),
                    exception.code(),
                    Objects.requireNonNullElse(exception.getMessage(), exception.code()),
                    exception.location());
        }
    }

    /** Returns the source currently requesting a nested resource. */
    private URI currentSource() {
        return resolving.isEmpty() ? entrySceneSource : resolving.getLast().toUri();
    }

    /** Formats one cycle using project-relative resource identities. */
    private String cycle(Path repeated) {
        int start = resolving.indexOf(repeated);
        List<String> path = new ArrayList<>();
        for (int index = start; index < resolving.size(); index++) {
            path.add(relative(resolving.get(index)));
        }
        path.add(relative(repeated));
        return String.join(" -> ", path);
    }

    /** Returns a stable project-relative resource path for diagnostics. */
    private String relative(Path resource) {
        return project.root().relativize(resource).toString().replace('\\', '/');
    }

    /** Requires a resolver whose owned values remain available. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("project resource resolver is closed");
        }
    }

    /** Returns whether structured diagnostics contain a terminal error. */
    private static boolean hasErrors(List<ProjectDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }

    /** Creates one structured runtime failure. */
    private static RuntimeDiagnosticsException failure(URI source, String code, String message, String location) {
        ProjectDiagnostic diagnostic =
                new ProjectDiagnostic(ProjectDiagnostic.Severity.ERROR, code, message, source, location);
        return new RuntimeDiagnosticsException(List.of(diagnostic));
    }

    /** Adds implementation detail to a stable failure prefix. */
    private static String failureMessage(String prefix, RuntimeException failure) {
        String detail = failure.getMessage();
        return detail == null ? prefix + ": " + failure.getClass().getSimpleName() : prefix + ": " + detail;
    }
}
