/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension.project;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Owns the current-project lifecycle shared by the workbench and built-in extensions. */
public final class EditorProjectContext implements EditorProjects {
    private final List<Consumer<Optional<EditorProject>>> projectListeners;
    private final List<Consumer<Optional<EditorHierarchyNode>>> hierarchyListeners;
    private final List<Runnable> assetListeners;
    private Optional<EditorProject> current;
    private Optional<EditorHierarchyNode> hierarchy;
    private List<ProjectAsset> assets;

    /** Creates an initially empty project lifecycle. */
    public EditorProjectContext() {
        projectListeners = new ArrayList<>();
        hierarchyListeners = new ArrayList<>();
        assetListeners = new ArrayList<>();
        current = Optional.empty();
        hierarchy = Optional.empty();
        assets = List.of();
    }

    @Override
    public Optional<EditorProject> current() {
        return current;
    }

    @Override
    public EditorRegistration observe(Consumer<Optional<EditorProject>> listener) {
        Consumer<Optional<EditorProject>> observer = Objects.requireNonNull(listener, "listener");
        projectListeners.add(observer);
        observer.accept(current);
        return once(() -> projectListeners.remove(observer));
    }

    /**
     * Returns the editor-internal Hierarchy projection for the current project.
     *
     * @return current hierarchy, or empty while no project is open
     */
    public Optional<EditorHierarchyNode> hierarchy() {
        return hierarchy;
    }

    /**
     * Observes the editor-internal Hierarchy projection without adding it to the public extension interface.
     *
     * @param listener synchronous hierarchy listener
     * @return removable listener registration
     */
    public EditorRegistration observeHierarchy(Consumer<Optional<EditorHierarchyNode>> listener) {
        Consumer<Optional<EditorHierarchyNode>> observer = Objects.requireNonNull(listener, "listener");
        hierarchyListeners.add(observer);
        observer.accept(hierarchy);
        return once(() -> hierarchyListeners.remove(observer));
    }

    /**
     * Returns the editor-internal asset projection for the current project.
     *
     * @return immutable asset projection
     */
    public List<ProjectAsset> assets() {
        return assets;
    }

    /**
     * Observes complete changes to the editor-internal asset projection.
     *
     * @param listener synchronous asset listener
     * @return removable listener registration
     */
    public EditorRegistration observeAssets(Runnable listener) {
        Runnable observer = Objects.requireNonNull(listener, "listener");
        assetListeners.add(observer);
        observer.run();
        return once(() -> assetListeners.remove(observer));
    }

    /**
     * Publishes one completely loaded project to every workbench observer.
     *
     * @param project opened project identity
     * @param hierarchyRoot editor-internal hierarchy projection
     * @param projectAssets editor-internal asset projection
     */
    public void showProject(
            EditorProject project, EditorHierarchyNode hierarchyRoot, List<ProjectAsset> projectAssets) {
        EditorProject opened = Objects.requireNonNull(project, "project");
        EditorHierarchyNode root = Objects.requireNonNull(hierarchyRoot, "hierarchyRoot");
        current = Optional.of(opened);
        hierarchy = Optional.of(root);
        assets = List.copyOf(projectAssets);
        notifyProjectListeners();
        notifyHierarchyListeners();
        notifyAssetListeners();
    }

    /** Clears project-owned state before another project is loaded or after a failure. */
    public void clear() {
        if (current.isEmpty() && hierarchy.isEmpty() && assets.isEmpty()) {
            return;
        }
        current = Optional.empty();
        hierarchy = Optional.empty();
        assets = List.of();
        notifyProjectListeners();
        notifyHierarchyListeners();
        notifyAssetListeners();
    }

    private void notifyProjectListeners() {
        List.copyOf(projectListeners).forEach(listener -> listener.accept(current));
    }

    private void notifyHierarchyListeners() {
        List.copyOf(hierarchyListeners).forEach(listener -> listener.accept(hierarchy));
    }

    private void notifyAssetListeners() {
        List.copyOf(assetListeners).forEach(Runnable::run);
    }

    private static EditorRegistration once(Runnable removal) {
        return new EditorRegistration() {
            private boolean closed;

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    removal.run();
                }
            }
        };
    }
}
