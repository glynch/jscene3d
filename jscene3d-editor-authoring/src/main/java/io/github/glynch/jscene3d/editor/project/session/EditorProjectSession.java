/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.configuration.definition.SettingKey;
import io.github.glynch.jscene3d.editor.command.EditorUndoRedoEntry;
import io.github.glynch.jscene3d.editor.configuration.EditorConfigurationChange;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjector;
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
public final class EditorProjectSession implements AutoCloseable {
    private final GameProject project;
    private final ProjectConfiguration configuration;
    private final AssetCatalog authoredAssets;
    private final RegisteredTypeCatalog types;
    private final ProjectContent content;
    private final List<ProjectAsset> assets;
    private final EditorHierarchyProjector hierarchyProjector;
    private final EditorWorkingCopyRegistry workingCopies = new EditorWorkingCopyRegistry();
    private final EditorWorldWorkingCopy startupWorld;
    private final EditorEventSource<EditorHierarchyNode> hierarchyChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorConfigurationChange> configurationChanges = new EditorEventSource<>();
    private final EditorRegistration workingCopyRegistration;
    private final EditorRegistration contentRegistration;
    private final EditorRegistration saveRegistration;

    private EditorHierarchyNode hierarchy;
    private boolean closed;

    /**
     * Stores the validated project data needed by the first authoring views.
     *
     * @param source loaded project inputs
     * @param assets validated project assets
     * @param hierarchyProjector projector used to rebuild the Hierarchy after authored edits
     */
    public EditorProjectSession(Source source, List<ProjectAsset> assets, EditorHierarchyProjector hierarchyProjector) {
        Source validSource = Objects.requireNonNull(source, "source");
        project = validSource.project();
        configuration = validSource.configuration();
        authoredAssets = validSource.authoredAssets();
        types = validSource.types();
        content = validSource.content();
        this.assets = List.copyOf(assets);
        this.hierarchyProjector = Objects.requireNonNull(hierarchyProjector, "hierarchyProjector");
        startupWorld = new EditorWorldWorkingCopy(validSource.startupWorldSource(), validSource.startupWorld());
        workingCopyRegistration = workingCopies.register(startupWorld);
        contentRegistration = startupWorld.onDidChangeContent().subscribe(ignored -> refreshProjection());
        saveRegistration = startupWorld.onDidSave().subscribe(ignored -> refreshProjection());
        hierarchy = projectHierarchy();
    }

    /**
     * Returns the validated project descriptor.
     *
     * @return validated project descriptor
     */
    public GameProject project() {
        ensureOpen();
        return project;
    }

    /**
     * Returns portable settings applied to this project session.
     *
     * @return validated portable project settings
     */
    public ProjectSettings settings() {
        ensureOpen();
        return configuration.document();
    }

    /**
     * Returns the effective project configuration and its declarative setting registry.
     *
     * @return live project configuration
     */
    public ProjectConfiguration configuration() {
        ensureOpen();
        return configuration;
    }

    /**
     * Returns the typed event fired after a project setting is persisted.
     *
     * @return configuration-change event
     */
    public EditorEvent<EditorConfigurationChange> onDidChangeConfiguration() {
        ensureOpen();
        return configurationChanges;
    }

    /**
     * Atomically persists one declared project setting and publishes its new effective value.
     *
     * @param <T> setting value type
     * @param key declared setting key
     * @param value new setting value
     * @throws IOException when the updated settings document cannot be persisted
     */
    public <T> void updateSetting(SettingKey<T> key, T value) throws IOException {
        ensureOpen();
        configuration.update(key, value);
        configurationChanges.emit(new EditorConfigurationChange(key, true));
    }

    /**
     * Removes one project override and publishes the declaration's effective default.
     *
     * @param key declared setting key
     * @throws IOException when the updated settings document cannot be persisted
     */
    public void resetSetting(SettingKey<?> key) throws IOException {
        ensureOpen();
        configuration.reset(key);
        configurationChanges.emit(new EditorConfigurationChange(key, false));
    }

    /**
     * Returns authored definition metadata.
     *
     * @return authored asset catalog
     */
    public AssetCatalog authoredAssets() {
        ensureOpen();
        return authoredAssets;
    }

    /**
     * Returns safe component and resource metadata.
     *
     * @return registered type catalog
     */
    public RegisteredTypeCatalog types() {
        ensureOpen();
        return types;
    }

    /**
     * Returns the combined authored and generated definition resolver.
     *
     * @return combined definition resolver
     */
    public DefinitionResolver definitions() {
        ensureOpen();
        return content.definitions();
    }

    /**
     * Returns combined definitions and lazily loaded spatial resources for preview composition.
     *
     * @return loaded project content
     */
    public ProjectContent content() {
        ensureOpen();
        return content;
    }

    /**
     * Returns the current startup-world working-copy content.
     *
     * @return current startup-world definition
     */
    public WorldDefinition startupWorld() {
        ensureOpen();
        return startupWorld.current();
    }

    /**
     * Returns all registered resource working copies.
     *
     * @return read-only working-copy registry
     */
    public EditorWorkingCopies workingCopies() {
        ensureOpen();
        return workingCopies;
    }

    /**
     * Returns the working copy presented by the permanent scene-preview tab.
     *
     * @return startup-world working copy
     */
    public EditorWorkingCopy startupWorldWorkingCopy() {
        ensureOpen();
        return startupWorld;
    }

    /**
     * Returns whether at least one project resource has unsaved changes.
     *
     * @return aggregate project dirty state
     */
    public boolean isDirty() {
        ensureOpen();
        return workingCopies.hasDirty();
    }

    /**
     * Returns whether an earlier in-memory revision can be restored.
     *
     * @return whether undo is available
     */
    public boolean canUndo() {
        ensureOpen();
        return undoEntry().isPresent();
    }

    /**
     * Returns whether a previously undone in-memory revision can be restored.
     *
     * @return whether redo is available
     */
    public boolean canRedo() {
        ensureOpen();
        return redoEntry().isPresent();
    }

    /**
     * Returns the next resource-aware undo entry.
     *
     * @return next undo entry, if available
     */
    public Optional<EditorUndoRedoEntry> undoEntry() {
        ensureOpen();
        return startupWorld.undoEntry();
    }

    /**
     * Returns the next resource-aware redo entry.
     *
     * @return next redo entry, if available
     */
    public Optional<EditorUndoRedoEntry> redoEntry() {
        ensureOpen();
        return startupWorld.redoEntry();
    }

    /** Restores the immediately preceding document revision. */
    public void undo() {
        ensureOpen();
        startupWorld.undo();
    }

    /** Reapplies the immediately following document revision. */
    public void redo() {
        ensureOpen();
        startupWorld.redo();
    }

    /**
     * Atomically saves every dirty registered resource to its authored source.
     *
     * @throws IOException when the complete replacement cannot be persisted atomically
     */
    public void save() throws IOException {
        ensureOpen();
        for (EditorWorkingCopy workingCopy : workingCopies.dirtyWorkingCopies()) {
            workingCopy.save();
        }
    }

    /**
     * Returns the typed event fired after the hierarchy projection changes.
     *
     * @return hierarchy-change event
     */
    public EditorEvent<EditorHierarchyNode> onDidChangeHierarchy() {
        ensureOpen();
        return hierarchyChanges;
    }

    /**
     * Returns the hierarchy projection for the configured startup world.
     *
     * @return startup-world hierarchy projection
     */
    public EditorHierarchyNode hierarchy() {
        ensureOpen();
        return hierarchy;
    }

    /**
     * Returns asset-browser items in deterministic order.
     *
     * @return immutable asset-browser items
     */
    public List<ProjectAsset> assets() {
        ensureOpen();
        return assets;
    }

    /**
     * Returns whether this authoring session has released its owned subscriptions and working copies.
     *
     * @return whether the session is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /** Releases project-scoped subscriptions without saving dirty working copies. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        saveRegistration.close();
        contentRegistration.close();
        workingCopyRegistration.close();
        workingCopies.close();
    }

    private EditorHierarchyNode projectHierarchy() {
        return hierarchyProjector.project(
                startupWorld.current(),
                startupWorld.modifiedEntityIds(),
                startupWorld::setEntityEnabled,
                startupWorld::setComponentProperty);
    }

    private void refreshProjection() {
        ensureOpen();
        hierarchy = projectHierarchy();
        hierarchyChanges.emit(hierarchy);
    }

    /** Rejects use after ownership has ended. */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Editor project session is closed");
        }
    }

    /**
     * Groups the immutable loaded inputs from which an editor project session is opened.
     *
     * @param project validated project descriptor
     * @param configuration effective project configuration
     * @param authoredAssets authored asset catalog
     * @param types registered project type catalog
     * @param content resolved project content
     * @param startupWorld loaded startup-world definition
     * @param startupWorldSource authored startup-world source path
     */
    public record Source(
            GameProject project,
            ProjectConfiguration configuration,
            AssetCatalog authoredAssets,
            RegisteredTypeCatalog types,
            ProjectContent content,
            WorldDefinition startupWorld,
            Path startupWorldSource) {
        /** Validates the complete loaded source. */
        public Source {
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
