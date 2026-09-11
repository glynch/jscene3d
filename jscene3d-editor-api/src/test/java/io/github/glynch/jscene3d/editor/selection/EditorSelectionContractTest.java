/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.view.EditorDetails;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EditorSelectionContractTest {
    @Test
    void selectionCarriesStableSemanticIdentityAndOptionalDetails() {
        EditorDetails details =
                new EditorDetails("Player", "Local entity", "world.json", "player", List.of(), List.of());

        EditorSelection selection = new EditorSelection(EditorSelectionKinds.LOCAL_ENTITY, "world:player", details);

        assertThat(selection.kind()).isEqualTo(EditorSelectionKinds.LOCAL_ENTITY);
        assertThat(selection.identity()).isEqualTo("world:player");
        assertThat(selection.details()).contains(details);
        assertThat(new EditorSelection(EditorSelectionKinds.ASSET, "asset:test", Optional.empty()).details())
                .isEmpty();
    }

    @Test
    void identitiesMustBeNamespacedAndSelectionsMustNotBeBlank() {
        assertThatThrownBy(() -> new EditorSelectionKindId("asset")).isInstanceOf(IllegalArgumentException.class);
        Optional<EditorDetails> noDetails = Optional.empty();
        assertThatThrownBy(() -> new EditorSelection(EditorSelectionKinds.ASSET, " ", noDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
