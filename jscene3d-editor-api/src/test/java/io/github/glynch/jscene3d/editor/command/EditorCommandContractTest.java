/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

final class EditorCommandContractTest {
    @Test
    void keepsExecutionIndependentFromVisualPlacement() {
        List<EditorMessage> messages = new ArrayList<>();
        EditorWindow window = new RecordingWindow(messages);
        EditorCommandContext context = () -> window;
        EditorCommand command = invocation ->
                invocation.window().showMessage(new EditorMessage(EditorMessageSeverity.INFORMATION, "Hello World!"));
        CommandId id = new CommandId("com.example.hello-world.say-hello");
        EditorCommandContribution contribution = new EditorCommandContribution(id, "Say Hello");
        EditorCommandPlacement placement = new EditorCommandPlacement(id, EditorCommandLocations.FILE_MENU, 10);

        command.execute(context);

        assertThat(contribution.id()).isEqualTo(id);
        assertThat(contribution.title()).isEqualTo("Say Hello");
        assertThat(placement.command()).isEqualTo(id);
        assertThat(placement.group()).isEqualTo("default");
        assertThat(placement.order()).isEqualTo(10);
        assertThat(messages).singleElement().extracting(EditorMessage::text).isEqualTo("Hello World!");
    }

    @Test
    void preservesPortableCommandAndLocationIdentities() {
        CommandId command = new CommandId("io.github.glynch.jscene3d.project.open");
        CommandLocationId location = EditorCommandLocations.VIEWPORT_TOOLBAR;

        assertThat(command).hasToString(command.value());
        assertThat(location).hasToString(location.value());
    }

    @Test
    void derivesStableViewSpecificLocations() {
        ViewId view = new ViewId("com.example.extension.custom-tree");

        assertThat(EditorCommandLocations.viewTitle(view).value())
                .isEqualTo("com.example.extension.custom-tree.command-location.title");
        assertThat(EditorCommandLocations.viewItemContext(view).value())
                .isEqualTo("com.example.extension.custom-tree.command-location.item-context");
    }

    @Test
    void rejectsMalformedCommandAndLocationIdentities() {
        assertRejected(CommandId::new);
        assertRejected(CommandLocationId::new);
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void validatesCommandMetadataAndPlacement() {
        CommandId command = new CommandId("io.github.glynch.test.command");
        CommandLocationId location = new CommandLocationId("io.github.glynch.test.location");

        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCommandContribution(null, "Test"))
                .withMessage("id");
        assertThatThrownBy(() -> new EditorCommandContribution(command, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCommandPlacement(null, location, 0))
                .withMessage("command");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCommandPlacement(command, null, 0))
                .withMessage("location");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCommandPlacement(command, location, null, 0))
                .withMessage("group");
        assertThatThrownBy(() -> new EditorCommandPlacement(command, location, " ", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("group must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> EditorCommandLocations.viewTitle(null))
                .withMessage("view");
    }

    @SuppressWarnings("NullAway") // Deliberate null verifies public boundary validation.
    private static <T> void assertRejected(Function<String, T> constructor) {
        assertThatNullPointerException()
                .isThrownBy(() -> constructor.apply(null))
                .withMessage("value");
        assertThatThrownBy(() -> constructor.apply("not-namespaced"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be a lowercase dotted namespaced identity");
    }

    private record RecordingWindow(List<EditorMessage> messages) implements EditorWindow {
        @Override
        public void showMessage(EditorMessage message) {
            messages.add(message);
        }

        @Override
        public void showView(ViewId view) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EditorDialogButtonId> showDialog(EditorDialog dialog) {
            throw new UnsupportedOperationException();
        }
    }
}
