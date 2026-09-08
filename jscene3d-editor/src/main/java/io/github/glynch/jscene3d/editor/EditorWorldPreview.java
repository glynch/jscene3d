/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import io.github.glynch.jscene3d.render.Renderer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Owns an active spatial-only realization of an authored startup world for the editor viewport. */
final class EditorWorldPreview implements AutoCloseable {
    private final World world;
    private final Spatial3dWorldModule spatial;

    /** Retains the composed world which owns the supplied spatial adapter. */
    private EditorWorldPreview(World world, Spatial3dWorldModule spatial) {
        this.world = world;
        this.spatial = spatial;
    }

    /** Composes real presentation components and inert substitutes for every non-presentation extension. */
    static EditorWorldPreviewLoadResult compose(EditorProjectSession session) {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        List<ComponentRuntimeExtension> extensions = previewExtensions(session);
        WorldCompositionResult composition = WorldComposer.compose(
                session.content().definitions(),
                AssetRef.<WorldDefinition>to(session.startupWorld().id()),
                session.types(),
                extensions,
                List.of(WorldModuleBinding.of(Spatial3dWorldModule.class, spatial)),
                session.content().resources());
        if (!composition.isComposed()) {
            closeAfterFailure(spatial);
            return new EditorWorldPreviewLoadResult(Optional.empty(), composition.diagnostics());
        }
        World world = composition.world().orElseThrow();
        try {
            world.activate();
            if (!spatial.isReadyToRender()) {
                world.close();
                ProjectDiagnostic diagnostic = error(
                        session,
                        EditorDiagnosticCode.PREVIEW_CAMERA_MISSING,
                        "startup world has no enabled primary PerspectiveCamera3d");
                return new EditorWorldPreviewLoadResult(Optional.empty(), List.of(diagnostic));
            }
            return new EditorWorldPreviewLoadResult(
                    Optional.of(new EditorWorldPreview(world, spatial)), composition.diagnostics());
        } catch (RuntimeException failure) {
            closeAfterFailure(world);
            ProjectDiagnostic diagnostic =
                    error(session, EditorDiagnosticCode.PREVIEW_COMPOSITION_FAILED, failure.toString());
            return new EditorWorldPreviewLoadResult(Optional.empty(), List.of(diagnostic));
        }
    }

    /** Renders the authored scene through its active primary camera. */
    void render(Renderer renderer, float viewportAspectRatio) {
        spatial.render(renderer, viewportAspectRatio);
    }

    /** Returns whether spatial presentation is active and ready for renderer submission. */
    boolean isReady() {
        return world.isActive() && spatial.isReadyToRender();
    }

    /** Closes the world, its spatial adapter, component values, and acquired resource leases. */
    @Override
    public void close() {
        world.close();
    }

    /** Selects the real spatial provider and descriptor-backed inert providers for all other extensions. */
    private static List<ComponentRuntimeExtension> previewExtensions(EditorProjectSession session) {
        List<ComponentRuntimeExtension> result = new ArrayList<>();
        for (ExtensionDescriptor descriptor : session.types().extensions()) {
            if (descriptor.id().equals(Spatial3dDescriptors.extensionId())) {
                result.add(new Spatial3dRuntimeExtension());
            } else {
                result.add(new InertComponentRuntimeExtension(descriptor));
            }
        }
        return List.copyOf(result);
    }

    /** Creates one editor-owned preview diagnostic at the startup-world source. */
    private static ProjectDiagnostic error(EditorProjectSession session, EditorDiagnosticCode code, String detail) {
        URI source = session.project()
                .root()
                .resolve(session.project().runtime().entryScene())
                .toUri();
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, "", Map.of("technicalDetail", detail));
    }

    /** Releases a module which composition did not take ownership of. */
    private static void closeAfterFailure(Spatial3dWorldModule spatial) {
        try {
            spatial.close();
        } catch (RuntimeException ignored) {
            // The composition diagnostics remain the actionable primary failure.
        }
    }

    /** Completes cleanup after activation or readiness fails. */
    private static void closeAfterFailure(World world) {
        if (!world.isClosed()) {
            try {
                world.close();
            } catch (RuntimeException ignored) {
                // The activation or readiness failure remains the actionable primary failure.
            }
        }
    }
}
