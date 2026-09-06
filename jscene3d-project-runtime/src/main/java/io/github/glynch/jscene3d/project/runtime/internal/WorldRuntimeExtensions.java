/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registers trusted world component factories in safe descriptor order. */
final class WorldRuntimeExtensions {
    /** Prevents construction of this stateless registration implementation. */
    private WorldRuntimeExtensions() {
        throw new AssertionError("WorldRuntimeExtensions cannot be instantiated");
    }

    /** Returns component factory bindings or throws the ordered structured registration diagnostics. */
    static FactoryBindings register(
            URI source, RegisteredTypeCatalog catalog, Collection<ComponentRuntimeExtension> extensions) {
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        Map<String, ComponentRuntimeExtension> providers = index(source, extensions, diagnostics);
        FactoryBindings bindings = new FactoryBindings();
        for (ExtensionDescriptor descriptor : catalog.extensions()) {
            ComponentRuntimeExtension extension = providers.get(descriptor.id());
            if (extension != null) {
                register(source, catalog, extension, bindings, diagnostics);
            }
        }
        if (!diagnostics.isEmpty()) {
            throw new RuntimeDiagnosticsException(diagnostics);
        }
        return bindings;
    }

    /** Indexes unique runtime providers and records invalid declarations. */
    private static Map<String, ComponentRuntimeExtension> index(
            URI source, Collection<ComponentRuntimeExtension> extensions, List<ProjectDiagnostic> diagnostics) {
        Map<String, ComponentRuntimeExtension> providers = new LinkedHashMap<>();
        for (ComponentRuntimeExtension extension : extensions) {
            try {
                ComponentRuntimeExtension validExtension = Objects.requireNonNull(extension, "extensions entry");
                String id = Preconditions.requireNonBlank(validExtension.id(), "runtime extension id");
                if (providers.putIfAbsent(id, validExtension) != null) {
                    diagnostics.add(error(
                            source,
                            RuntimeDiagnosticCode.EXTENSION_DUPLICATE,
                            "multiple runtime providers declare extension " + id));
                }
            } catch (RuntimeException exception) {
                diagnostics.add(error(
                        source,
                        RuntimeDiagnosticCode.EXTENSION_INVALID,
                        failureMessage("invalid runtime extension provider", exception)));
            }
        }
        return providers;
    }

    /** Invokes one declared provider in a contribution scope which always closes on return. */
    private static void register(
            URI source,
            RegisteredTypeCatalog catalog,
            ComponentRuntimeExtension extension,
            FactoryBindings bindings,
            List<ProjectDiagnostic> diagnostics) {
        RuntimeRegistry registry = new RuntimeRegistry(extension.id(), catalog, bindings);
        try {
            extension.register(registry);
        } catch (RuntimeException exception) {
            diagnostics.add(error(
                    source,
                    RuntimeDiagnosticCode.EXTENSION_REGISTRATION_FAILED,
                    failureMessage("runtime extension registration failed for " + extension.id(), exception)));
        } finally {
            registry.closeRegistration();
        }
    }

    /** Creates one structured registration diagnostic. */
    private static ProjectDiagnostic error(URI source, RuntimeDiagnosticCode code, String detail) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, "/extensions", Map.of("technicalDetail", detail));
    }

    /** Appends the available implementation detail to a stable diagnostic prefix. */
    private static String failureMessage(String prefix, Throwable failure) {
        String detail = failure.getMessage();
        return detail == null ? prefix + ": " + failure.getClass().getSimpleName() : prefix + ": " + detail;
    }
}
