/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandLocations;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusBarAlignment;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class EditorExtensionHostTest {
    private static final ViewId VIEW_ID = new ViewId("io.github.glynch.test.view");
    private static final CommandId MESSAGE_COMMAND = new CommandId("io.github.glynch.test.message");
    private static final CommandId REVEAL_COMMAND = new CommandId("io.github.glynch.test.reveal");

    @Test
    void activatesCapabilitiesAndRemovesOwnedContributionsAtShutdown() {
        EditorExtensionHost host = host();
        List<List<EditorViewContribution>> viewSnapshots = new ArrayList<>();
        List<EditorMessage> messages = new ArrayList<>();
        List<ViewId> viewRequests = new ArrayList<>();
        List<List<EditorStatusItemSnapshot>> statusSnapshots = new ArrayList<>();
        AtomicReference<EditorStatusItem> status = new AtomicReference<>();
        AtomicReference<EditorDiagnosticCollection> diagnostics = new AtomicReference<>();
        host.observeViews(viewSnapshots::add);
        host.observeViewRequests(viewRequests::add);
        host.observeStatusItems(statusSnapshots::add);
        host.showMessagesWith(messages::add);

        host.activate(extension(status, diagnostics));
        host.execute(MESSAGE_COMMAND);
        host.execute(REVEAL_COMMAND);

        assertThat(viewSnapshots.getLast())
                .singleElement()
                .extracting(it -> it.view().id())
                .isEqualTo(VIEW_ID);
        assertThat(messages).containsExactly(new EditorMessage(EditorMessageSeverity.INFORMATION, "Hello"));
        assertThat(viewRequests).containsExactly(VIEW_ID);
        assertThat(statusSnapshots.getLast())
                .singleElement()
                .extracting(item -> item.state().text())
                .isEqualTo("Ready");

        host.close();
        host.close();

        EditorStatusItem registeredStatus = status.get();
        EditorStatusItemState changedStatus =
                new EditorStatusItemState("Changed", Optional.empty(), Optional.empty(), Optional.empty(), true);
        EditorDiagnosticCollection registeredDiagnostics = diagnostics.get();
        assertThat(viewSnapshots.getLast()).isEmpty();
        assertThat(statusSnapshots.getLast()).isEmpty();
        assertThatThrownBy(() -> registeredStatus.update(changedStatus))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("status item is closed");
        assertThatThrownBy(registeredDiagnostics::clear)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("diagnostic collection is closed");
        assertThatThrownBy(() -> host.execute(MESSAGE_COMMAND))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("extension host is closed");
    }

    @Test
    void rejectsDuplicateExtensionsAndRollsBackFailedActivation() {
        EditorExtensionHost host = host();
        EditorExtension first = extension(new AtomicReference<>(), new AtomicReference<>());
        host.activate(first);

        assertThatThrownBy(() -> host.activate(first))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already active");

        host.close();
        EditorExtensionHost failingHost = host();
        List<List<EditorViewContribution>> snapshots = new ArrayList<>();
        failingHost.observeViews(snapshots::add);
        EditorExtension failing = new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.failing";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                context.subscriptions()
                        .add(context.views()
                                .register(new EditorViewContribution(
                                        new TestView(), EditorViewContainers.PRIMARY_SIDEBAR, 1)));
                throw new IllegalStateException("activation failed");
            }
        };
        assertThatThrownBy(() -> failingHost.activate(failing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("activation failed");
        assertThat(snapshots.getLast()).isEmpty();
        failingHost.close();
    }

    @Test
    void ordersStatusItemsByRegionAndDescendingPriority() {
        EditorExtensionHost host = host();
        List<List<EditorStatusItemSnapshot>> snapshots = new ArrayList<>();
        host.observeStatusItems(snapshots::add);
        host.activate(new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.status-order";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                registerStatus(context, "right-low", StatusBarAlignment.RIGHT, 10);
                registerStatus(context, "left-low", StatusBarAlignment.LEFT, 10);
                registerStatus(context, "right-high", StatusBarAlignment.RIGHT, 20);
                registerStatus(context, "left-high", StatusBarAlignment.LEFT, 20);
            }
        });

        assertThat(snapshots.getLast())
                .extracting(item -> item.contribution().id().value())
                .containsExactly(
                        "io.github.glynch.test.left-high",
                        "io.github.glynch.test.left-low",
                        "io.github.glynch.test.right-high",
                        "io.github.glynch.test.right-low");
        host.close();
    }

    private static void registerStatus(
            EditorExtensionContext context, String name, StatusBarAlignment alignment, int priority) {
        context.subscriptions()
                .add(context.statusBar()
                        .create(new EditorStatusItemContribution(
                                new StatusItemId("io.github.glynch.test." + name), alignment, priority)));
    }

    private static EditorExtension extension(
            AtomicReference<EditorStatusItem> status, AtomicReference<EditorDiagnosticCollection> diagnostics) {
        return new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.extension";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                context.subscriptions()
                        .add(context.views()
                                .register(new EditorViewContribution(
                                        new TestView(), EditorViewContainers.PRIMARY_SIDEBAR, 5)));
                context.subscriptions()
                        .add(context.commands()
                                .register(
                                        new EditorCommandContribution(MESSAGE_COMMAND, "Message"),
                                        invocation -> invocation
                                                .window()
                                                .showMessage(new EditorMessage(
                                                        EditorMessageSeverity.INFORMATION, "Hello"))));
                context.subscriptions()
                        .add(context.commands()
                                .register(
                                        new EditorCommandContribution(REVEAL_COMMAND, "Reveal"),
                                        invocation -> invocation.window().showView(VIEW_ID)));
                context.subscriptions()
                        .add(context.commandPlacements()
                                .register(new EditorCommandPlacement(
                                        MESSAGE_COMMAND, EditorCommandLocations.FILE_MENU, 5)));
                EditorStatusItem item = context.statusBar()
                        .create(new EditorStatusItemContribution(
                                new StatusItemId("io.github.glynch.test.status"), StatusBarAlignment.LEFT, 10));
                item.update(new EditorStatusItemState(
                        "Ready", Optional.empty(), Optional.of("Test status"), Optional.of(MESSAGE_COMMAND), true));
                status.set(context.subscriptions().add(item));
                EditorDiagnosticCollection collection = context.diagnostics()
                        .createCollection(new DiagnosticCollectionId("io.github.glynch.test.diagnostics"));
                collection.replace(
                        URI.create("file:///test/Example.java"),
                        List.of(new EditorDiagnostic(
                                EditorDiagnosticSeverity.WARNING,
                                "test.warning",
                                "Test warning",
                                "Example",
                                Optional.empty(),
                                Map.of())));
                diagnostics.set(context.subscriptions().add(collection));
                assertThat(context.projects().current()).isEmpty();
            }
        };
    }

    private static EditorExtensionHost host() {
        return new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
    }

    private static final class TestView implements EditorView {
        @Override
        public ViewId id() {
            return VIEW_ID;
        }

        @Override
        public String title() {
            return "Test";
        }

        @Override
        public ViewKindId kind() {
            return new ViewKindId("io.github.glynch.test.kind");
        }
    }
}
