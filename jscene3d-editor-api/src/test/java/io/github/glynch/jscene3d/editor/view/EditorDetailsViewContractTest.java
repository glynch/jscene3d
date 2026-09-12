/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

final class EditorDetailsViewContractTest {
    @Test
    void detailsViewUsesTheStandardKindAndCopiesNestedState() {
        EditorDetails.Property property = new EditorDetails.Property(
                "enabled",
                "Enabled",
                "boolean",
                "true",
                EditorDetails.ValueOrigin.AUTHORED,
                false,
                Optional.empty(),
                Map.of(),
                Optional.empty());
        EditorDetails details = new EditorDetails(
                "Player",
                "Local entity",
                "worlds/map01.world.json",
                "player",
                List.of(new EditorIcon(EditorIcons.READ_ONLY, "Read-only")),
                List.of(new EditorDetails.Section("Entity", Optional.empty(), true, List.of(property))));
        EditorDetailsView view = new TestDetailsView(details);

        assertThat(view.kind()).isEqualTo(EditorDetailsView.DETAILS_VIEW_KIND);
        assertThat(view.dataProvider().details()).contains(details);
        assertThat(details.decorations())
                .singleElement()
                .extracting(EditorIcon::id)
                .isEqualTo(EditorIcons.READ_ONLY);
        assertThat(details.sections())
                .singleElement()
                .extracting(EditorDetails.Section::title)
                .isEqualTo("Entity");
    }

    private record TestDetailsView(EditorDetails details) implements EditorDetailsView {
        @Override
        public ViewId id() {
            return new ViewId("io.github.glynch.test.details");
        }

        @Override
        public String title() {
            return "Details";
        }

        @Override
        public EditorDetailsDataProvider dataProvider() {
            return new EditorDetailsDataProvider() {
                @Override
                public Optional<EditorDetails> details() {
                    return Optional.of(TestDetailsView.this.details);
                }

                @Override
                public EditorRegistration observe(Consumer<Optional<EditorDetails>> listener) {
                    listener.accept(details());
                    return () -> {};
                }
            };
        }
    }
}
