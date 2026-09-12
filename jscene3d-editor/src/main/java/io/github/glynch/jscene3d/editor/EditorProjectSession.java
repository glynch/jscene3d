/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.configuration.SettingKey;
import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.command.EditorUndoRedoEntry;
import io.github.glynch.jscene3d.editor.configuration.EditorConfigurationChange;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorEventSource;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorWorkingCopyRegistry;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorWorldWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopies;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopy;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.settings.ProjectSettings;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates loaded project services and resource working copies for one editor window.
 *
 * <p>Editable content, saved baselines, and undo history belong to individual working copies.
 */
public final class EditorProjectSession {
    private final GameProject project;
    private final ProjectConfiguration configuration;
    private final AssetCatalog authoredAssets;
    private final RegisteredTypeCatalog types;
    private final ProjectContent content;
    private final List<ProjectAsset> assets;
    private final EditorHierarchyProjection hierarchyProjection;
    private final EditorWorkingCopyRegistry workingCopies = new EditorWorkingCopyRegistry();
    private final EditorWorldWorkingCopy startupWorld;
    private final EditorEventSource<EditorHierarchyNode> hierarchyChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorConfigurationChange> configurationChanges = new EditorEventSource<>();

    private EditorHierarchyNode hierarchy;

    /** Stores the validated project data needed by the first authoring views. */
    EditorProjectSession(Source source, List<ProjectAsset> assets, EditorHierarchyProjection hierarchyProjection) {
        Source validSource = Objects.requireNonNull(source, "source");
        project = validSource.project();
        configuration = validSource.configuration();
        authoredAssets = validSource.authoredAssets();
        types = validSource.types();
        content = validSource.content();
        this.assets = List.copyOf(assets);
        this.hierarchyProjection = Objects.requireNonNull(hierarchyProjection, "hierarchyProjection");
        startupWorld = new EditorWorldWorkingCopy(validSource.startupWorldSource(), validSource.startupWorld());
        workingCopies.register(startupWorld);
        startupWorld.onDidChangeContent().subscribe(ignored -> refreshProjection());
        startupWorld.onDidSave().subscribe(ignored -> refreshProjection());
        hierarchy = projectHierarchy();
    }

    /**
     * Returns the validated project descriptor.
     *
     * @return validated project descriptor
     */
    public GameProject project() {
        return project;
    }

    /**
     * Returns portable settings applied to this project session.
     *
     * @return validated portable project settings
     */
    public ProjectSettings settings() {
        return configuration.document();
    }

    /**
     * Returns the effective project configuration and its declarative setting registry.
     *
     * @return live project configuration
     */
    public ProjectConfiguration configuration() {
        return configuration;
    }

    /** Returns the typed event fired after a project setting is persisted. */
    public EditorEvent<EditorConfigurationChange> onDidChangeConfiguration() {
        return configurationChanges;
    }

    /** Atomically persists one declared project setting and publishes its new effective value. */
    public <T> void updateSetting(SettingKey<T> key, T value) throws IOException {
        configuration.update(key, value);
        configurationChanges.emit(new EditorConfigurationChange(key, true));
    }

    /** Removes one project override and publishes the declaration's effective default. */
    public void resetSetting(SettingKey<?> key) throws IOException {
        configuration.reset(key);
        configurationChanges.emit(new EditorConfigurationChange(key, false));
    }

    /** Returns authored definition metadata. */
    AssetCatalog authoredAssets() {
        return authoredAssets;
    }

    /** Returns safe component and resource metadata. */
    RegisteredTypeCatalog types() {
        return types;
    }

    /** Returns the combined authored and generated definition resolver. */
    DefinitionResolver definitions() {
        return content.definitions();
    }

    /** Returns combined definitions and lazily loaded spatial resources for preview composition. */
    ProjectContent content() {
        return content;
    }

    /** Returns the current startup-world working-copy content. */
    WorldDefinition startupWorld() {
        return startupWorld.current();
    }

    /**
     * Returns all registered resource working copies.
     *
     * @return read-only working-copy registry
     */
    public EditorWorkingCopies workingCopies() {
        return workingCopies;
    }

    /**
     * Returns the working copy presented by the permanent scene-preview tab.
     *
     * @return startup-world working copy
     */
    public EditorWorkingCopy startupWorldWorkingCopy() {
        return startupWorld;
    }

    /**
     * Returns whether at least one project resource has unsaved changes.
     *
     * @return aggregate project dirty state
     */
    public boolean isDirty() {
        return workingCopies.hasDirty();
    }

    /**
     * Returns whether an earlier in-memory revision can be restored.
     *
     * @return whether undo is available
     */
    public boolean canUndo() {
        return undoEntry().isPresent();
    }

    /**
     * Returns whether a previously undone in-memory revision can be restored.
     *
     * @return whether redo is available
     */
    public boolean canRedo() {
        return redoEntry().isPresent();
    }

    /**
     * Returns the next resource-aware undo entry.
     *
     * @return next undo entry, if available
     */
    public Optional<EditorUndoRedoEntry> undoEntry() {
        return startupWorld.undoEntry();
    }

    /**
     * Returns the next resource-aware redo entry.
     *
     * @return next redo entry, if available
     */
    public Optional<EditorUndoRedoEntry> redoEntry() {
        return startupWorld.redoEntry();
    }

    /** Restores the immediately preceding document revision. */
    public void undo() {
        startupWorld.undo();
    }

    /** Reapplies the immediately following document revision. */
    public void redo() {
        startupWorld.redo();
    }

    /**
     * Atomically saves the current startup-world revision to its authored source.
     *
     * @throws IOException when the complete replacement cannot be persisted atomically
     */
    public void save() throws IOException {
        startupWorld.save();
    }

    /**
     * Returns the typed event fired after the hierarchy projection changes.
     *
     * @return hierarchy-change event
     */
    public EditorEvent<EditorHierarchyNode> onDidChangeHierarchy() {
        return hierarchyChanges;
    }

    /**
     * Returns the hierarchy projection for the configured startup world.
     *
     * @return startup-world hierarchy projection
     */
    public EditorHierarchyNode hierarchy() {
        return hierarchy;
    }

    /**
     * Returns asset-browser items in deterministic order.
     *
     * @return immutable asset-browser items
     */
    public List<ProjectAsset> assets() {
        return assets;
    }

    private EditorHierarchyNode projectHierarchy() {
        return hierarchyProjection.project(
                startupWorld.current(), startupWorld.modifiedEntityIds(), startupWorld::setEntityEnabled);
    }

    private void refreshProjection() {
        hierarchy = projectHierarchy();
        hierarchyChanges.emit(hierarchy);
    }

    /** Groups the immutable loaded inputs from which an editor project session is opened. */
    record Source(
            GameProject project,
            ProjectConfiguration configuration,
            AssetCatalog authoredAssets,
            RegisteredTypeCatalog types,
            ProjectContent content,
            WorldDefinition startupWorld,
            Path startupWorldSource) {
        /** Validates the complete loaded source. */
        Source {
            Objects.requireNonNull(project, "project");
            Objects.requireNonNull(configuration, "configuration");
            Objects.requireNonNull(authoredAssets, "authoredAssets");
            Objects.requireNonNull(types, "types");
            Objects.requireNonNull(content, "content");
            Objects.requireNonNull(startupWorld, "startupWorld");
            Objects.requireNonNull(startupWorldSource, "startupWorldSource");
        }
    }
}
