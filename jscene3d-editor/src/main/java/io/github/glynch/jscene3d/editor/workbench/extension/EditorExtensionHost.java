/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.CommandLocationId;
import io.github.glynch.jscene3d.editor.command.EditorCommand;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistration;
import io.github.glynch.jscene3d.editor.configuration.EditorConfiguration;
import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.menu.EditorMenuContribution;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.workbench.appearance.EditorColorThemeRegistry;
import io.github.glynch.jscene3d.editor.workbench.appearance.InMemoryEditorAppearancePreferences;
import io.github.glynch.jscene3d.editor.workbench.command.EditorCommandMenuRegistry;
import io.github.glynch.jscene3d.editor.workbench.configuration.EditorConfigurationContext;
import io.github.glynch.jscene3d.editor.workbench.file.EditorFileTypeRegistry;
import io.github.glynch.jscene3d.editor.workbench.language.EditorLanguageSupportRegistry;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuCommandSnapshot;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuSnapshot;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopies;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Activates extensions and owns their toolkit-independent workbench contributions. */
public final class EditorExtensionHost implements AutoCloseable {
    private final Map<String, EditorExtensionSubscriptions> activeExtensions = new LinkedHashMap<>();
    private final EditorContributionRegistry contributions = new EditorContributionRegistry();
    private final EditorExtensionCatalog catalog = new EditorExtensionCatalog();
    private final EditorFileTypeRegistry fileTypes = new EditorFileTypeRegistry();
    private final EditorLanguageSupportRegistry languageSupports;
    private final EditorColorThemeRegistry colorThemes;
    private final EditorStatusItemRegistry statusItems = new EditorStatusItemRegistry();
    private final EditorDiagnosticRegistry diagnostics = new EditorDiagnosticRegistry();
    private final EditorWindowDispatcher window = new EditorWindowDispatcher(contributions);
    private final EditorCommandMenuRegistry commandMenus = new EditorCommandMenuRegistry(() -> window);
    private final EditorExtensionScope.Capabilities capabilities;
    private final EditorRegistration projectContextRegistration;
    private boolean closed;

    /**
     * Creates an empty host around the editor's current-project lifecycle.
     *
     * @param projects current-project context
     * @param selections editor selection service
     */
    public EditorExtensionHost(EditorProjectContext projects, EditorSelections selections) {
        this(
                projects,
                selections,
                new EditorConfigurationContext(),
                new EditorColorThemeRegistry(new InMemoryEditorAppearancePreferences()));
    }

    /**
     * Creates an empty host with the window's live effective configuration.
     *
     * @param projects current-project context
     * @param selections editor selection service
     * @param configuration effective editor configuration
     */
    public EditorExtensionHost(
            EditorProjectContext projects, EditorSelections selections, EditorConfiguration configuration) {
        this(
                projects,
                selections,
                configuration,
                new EditorColorThemeRegistry(new InMemoryEditorAppearancePreferences()));
    }

    /**
     * Creates an empty host with live project configuration and user appearance.
     *
     * @param projects current-project context
     * @param selections editor selection service
     * @param configuration effective editor configuration
     * @param colorThemes color-theme registry
     */
    public EditorExtensionHost(
            EditorProjectContext projects,
            EditorSelections selections,
            EditorConfiguration configuration,
            EditorColorThemeRegistry colorThemes) {
        EditorProjectContext projectContext = Objects.requireNonNull(projects, "projects");
        this.colorThemes = Objects.requireNonNull(colorThemes, "colorThemes");
        languageSupports = new EditorLanguageSupportRegistry(projectContext);
        capabilities = new EditorExtensionScope.Capabilities(
                contributions,
                contributions,
                catalog,
                commandMenus,
                commandMenus,
                commandMenus,
                Objects.requireNonNull(configuration, "configuration"),
                statusItems,
                window,
                diagnostics,
                fileTypes,
                languageSupports,
                colorThemes,
                projectContext,
                Objects.requireNonNull(selections, "selections"));
        projectContextRegistration = projectContext.observe(
                project -> contributions.setContext(EditorContextKeys.PROJECT_OPEN, project.isPresent()));
    }

    /**
     * Activates one extension exactly once for this host's lifetime.
     *
     * @param extension extension to activate
     */
    public void activate(EditorExtension extension) {
        requireOpen();
        EditorExtension candidate = Objects.requireNonNull(extension, "extension");
        String id = requireExtensionId(candidate);
        EditorExtensionDescriptor descriptor = requireMatchingDescriptor(candidate, id);
        EditorExtensionSubscriptions subscriptions = new EditorExtensionSubscriptions();
        EditorExtensionContext extensionContext = new EditorExtensionScope(capabilities, subscriptions);
        try {
            candidate.activate(extensionContext);
            activeExtensions.put(id, subscriptions);
            catalog.add(descriptor);
        } catch (RuntimeException failure) {
            subscriptions.close();
            throw failure;
        }
    }

    /**
     * Observes ordered Activity Bar contributions and immediately publishes the current snapshot.
     *
     * @param observer activity contribution observer
     * @return registration that removes the observer
     */
    public EditorRegistration observeActivities(Consumer<List<EditorActivityContribution>> observer) {
        requireOpen();
        return contributions.observeActivities(observer);
    }

    /**
     * Observes ordered view contributions and immediately publishes the current snapshot.
     *
     * @param observer view contribution observer
     * @return registration that removes the observer
     */
    public EditorRegistration observeViews(Consumer<List<EditorViewContribution>> observer) {
        requireOpen();
        return contributions.observeViews(observer);
    }

    /**
     * Observes requests to reveal registered views.
     *
     * @param observer view-request observer
     * @return registration that removes the observer
     */
    public EditorRegistration observeViewRequests(Consumer<ViewId> observer) {
        requireOpen();
        return window.observeViewRequests(observer);
    }

    /**
     * Observes requests to preview or open workspace files.
     *
     * @param observer file-request observer
     * @return registration that removes the observer
     */
    public EditorRegistration observeFileRequests(Consumer<EditorFileOpenRequest> observer) {
        requireOpen();
        return window.observeFileRequests(observer);
    }

    /**
     * Resolves the contributed type for one workspace file.
     *
     * @param resource workspace file resource
     * @return resolved file type, when registered
     */
    public Optional<EditorFileType> resolveFileType(URI resource) {
        requireOpen();
        return fileTypes.resolve(Objects.requireNonNull(resource, "resource"));
    }

    /**
     * Returns the active appearance and color-theme registry owned by this editor window.
     *
     * @return window color-theme registry
     */
    public EditorColorThemeRegistry colorThemes() {
        requireOpen();
        return colorThemes;
    }

    /**
     * Observes ordered status items and immediately publishes the current snapshot.
     *
     * @param observer status-item observer
     * @return registration that removes the observer
     */
    public EditorRegistration observeStatusItems(Consumer<List<EditorStatusItemSnapshot>> observer) {
        requireOpen();
        return statusItems.observe(observer);
    }

    /**
     * Observes combined diagnostics and immediately publishes the current snapshot.
     *
     * @param observer diagnostic observer
     * @return registration that removes the observer
     */
    public EditorRegistration observeDiagnostics(Consumer<List<EditorDiagnosticSnapshot>> observer) {
        requireOpen();
        return diagnostics.observe(observer);
    }

    /**
     * Observes diagnostics for one exact resource and immediately publishes its current snapshot.
     *
     * @param resource exact resource URI
     * @param observer diagnostic snapshot consumer
     * @return removable observer registration
     */
    public EditorRegistration observeDiagnostics(URI resource, Consumer<List<EditorDiagnosticSnapshot>> observer) {
        requireOpen();
        URI source = Objects.requireNonNull(resource, "resource");
        Consumer<List<EditorDiagnosticSnapshot>> listener = Objects.requireNonNull(observer, "observer");
        return diagnostics.observe(snapshot -> listener.accept(
                snapshot.stream().filter(item -> item.source().equals(source)).toList()));
    }

    /**
     * Connects one workbench working-copy set to registered language support.
     *
     * @param workingCopies working copies to synchronize
     * @return removable synchronization registration
     */
    public EditorRegistration synchronizeLanguages(EditorWorkingCopies workingCopies) {
        requireOpen();
        return languageSupports.synchronize(workingCopies);
    }

    /**
     * Routes extension window messages to the workbench presentation.
     *
     * @param sink message presentation sink
     */
    public void showMessagesWith(Consumer<EditorMessage> sink) {
        requireOpen();
        window.showMessagesWith(sink);
    }

    /**
     * Routes extension modal dialogs to the workbench's platform adapter.
     *
     * @param sink modal-dialog presentation sink
     */
    public void showDialogsWith(Function<EditorDialog, Optional<EditorDialogButtonId>> sink) {
        requireOpen();
        window.showDialogsWith(sink);
    }

    /**
     * Observes ordered menus and their currently placed command state.
     *
     * @param observer menu observer
     * @return registration that removes the observer
     */
    public EditorRegistration observeMenus(Consumer<List<EditorMenuSnapshot>> observer) {
        requireOpen();
        return commandMenus.observe(observer);
    }

    /**
     * Returns commands currently available at an item-oriented command location.
     *
     * @param location command location
     * @param contextValue semantic context value, when present
     * @return available commands
     */
    public List<EditorMenuCommandSnapshot> commandsAt(CommandLocationId location, Optional<String> contextValue) {
        requireOpen();
        return commandMenus.commandsAt(location, contextValue);
    }

    /**
     * Invokes a registered command through the workbench action path.
     *
     * @param command command identity
     */
    public void execute(CommandId command) {
        requireOpen();
        commandMenus.execute(command);
    }

    /**
     * Invokes a registered command with the exact semantic item involved in the interaction.
     *
     * @param command command identity
     * @param argument semantic invocation argument
     */
    public void execute(CommandId command, Object argument) {
        requireOpen();
        commandMenus.execute(command, argument);
    }

    /**
     * Requests that the workbench reveal one registered view.
     *
     * @param view view identity
     */
    public void showView(ViewId view) {
        window.showView(view);
    }

    /**
     * Opens a file and reveals an exact diagnostic source range.
     *
     * @param resource source resource
     * @param range source range to select
     */
    public void revealFile(URI resource, EditorTextRange range) {
        requireOpen();
        window.revealFile(resource, range);
    }

    /**
     * Shows one extension or workbench message through the configured presentation.
     *
     * @param message message to show
     */
    public void showMessage(EditorMessage message) {
        window.showMessage(message);
    }

    /**
     * Shows one toolkit-independent application-modal dialog.
     *
     * @param dialog dialog to show
     * @return selected button, when the user made a selection
     */
    public Optional<EditorDialogButtonId> showDialog(EditorDialog dialog) {
        return window.showDialog(dialog);
    }

    /** Deactivates extensions in reverse order and removes every remaining contribution. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        projectContextRegistration.close();
        List.copyOf(activeExtensions.values()).reversed().forEach(EditorExtensionSubscriptions::close);
        activeExtensions.clear();
        catalog.close();
        contributions.close();
        commandMenus.close();
        statusItems.close();
        diagnostics.close();
        languageSupports.close();
        window.close();
    }

    /**
     * Registers one workbench or extension command.
     *
     * @param contribution command metadata
     * @param command command implementation
     * @return command state and lifecycle registration
     */
    public EditorCommandRegistration registerCommand(EditorCommandContribution contribution, EditorCommand command) {
        requireOpen();
        return commandMenus.registerCommand(contribution, command);
    }

    /**
     * Registers one visual placement for a registered workbench or extension command.
     *
     * @param placement command placement
     * @return placement lifecycle registration
     */
    public EditorRegistration registerCommandPlacement(EditorCommandPlacement placement) {
        requireOpen();
        return commandMenus.registerPlacement(placement);
    }

    /**
     * Registers one toolkit-independent top-level menu.
     *
     * @param contribution menu metadata
     * @return menu lifecycle registration
     */
    public EditorRegistration registerMenu(EditorMenuContribution contribution) {
        requireOpen();
        return commandMenus.registerMenu(contribution);
    }

    /**
     * Returns whether a view remains registered even when its context condition is currently false.
     *
     * @param view view identity
     * @return whether the view is registered
     */
    public boolean isViewRegistered(ViewId view) {
        requireOpen();
        return contributions.isViewRegistered(view);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("extension host is closed");
        }
    }

    private String requireExtensionId(EditorExtension extension) {
        String id = Objects.requireNonNull(extension.id(), "extension.id()");
        if (id.isBlank()) {
            throw new IllegalArgumentException("extension identity must not be blank");
        }
        if (activeExtensions.containsKey(id)) {
            throw new IllegalArgumentException("extension identity is already active: " + id);
        }
        return id;
    }

    private static EditorExtensionDescriptor requireMatchingDescriptor(EditorExtension extension, String id) {
        EditorExtensionDescriptor descriptor = Objects.requireNonNull(extension.descriptor(), "extension.descriptor()");
        if (!descriptor.id().equals(id)) {
            throw new IllegalArgumentException(
                    "extension descriptor identity does not match extension identity: " + id);
        }
        return descriptor;
    }
}
