/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

final class EditorCollectionViewContractTest {
    @Test
    void suppliesSemanticCollectionDataThroughTheStandardCollectionKind() {
        TestProvider provider = new TestProvider();
        EditorCollectionView<String> view = new TestView(provider);

        EditorCollectionSnapshot<String> snapshot =
                view.dataProvider().snapshot().toCompletableFuture().join();

        assertThat(view.kind()).isEqualTo(EditorCollectionView.COLLECTION_VIEW_KIND);
        assertThat(view.selectionModel()).isEmpty();
        assertThat(view.categories())
                .containsExactly(new EditorCollectionCategory("worlds", "Worlds", Optional.of("W")));
        assertThat(view.allItemsLabel()).isEqualTo("All Items");
        assertThat(view.rootIcon()).isEmpty();
        assertThat(view.searchPlaceholder()).isEqualTo("Search items…");
        assertThat(snapshot.rootLabel()).isEqualTo("Test Project");
        assertThat(snapshot.elements()).containsExactly("MAP01");
        assertThat(snapshot.searchable()).isTrue();
        assertThat(view.dataProvider().item("MAP01").categoryId()).contains("worlds");
    }

    @Test
    void publishesCompleteCollectionInvalidationWithoutToolkitEvents() {
        TestProvider provider = new TestProvider();
        int[] invalidations = {0};
        EditorRegistration registration = provider.observeChanges(() -> invalidations[0]++);

        provider.invalidate();
        registration.close();
        provider.invalidate();

        assertThat(invalidations[0]).isEqualTo(1);
    }

    private record TestView(EditorCollectionDataProvider<String> dataProvider) implements EditorCollectionView<String> {
        @Override
        public ViewId id() {
            return new ViewId("io.github.glynch.test.collection");
        }

        @Override
        public String title() {
            return "Test Collection";
        }

        @Override
        public List<EditorCollectionCategory> categories() {
            return List.of(new EditorCollectionCategory("worlds", "Worlds", Optional.of("W")));
        }
    }

    private static final class TestProvider implements EditorCollectionDataProvider<String> {
        private Runnable listener = () -> {};

        @Override
        public CompletionStage<EditorCollectionSnapshot<String>> snapshot() {
            return CompletableFuture.completedFuture(
                    new EditorCollectionSnapshot<>("Test Project", List.of("MAP01"), true, "No matching assets"));
        }

        @Override
        public EditorCollectionItem item(String element) {
            return new EditorCollectionItem(
                    element,
                    Optional.of("World definition"),
                    Optional.of("worlds/map01.world.json"),
                    Optional.empty(),
                    Optional.of("W"),
                    Optional.of("worlds"),
                    Optional.of("R/O"),
                    Optional.empty(),
                    Optional.of("world"));
        }

        @Override
        public EditorRegistration observeChanges(Runnable newListener) {
            listener = newListener;
            return () -> listener = () -> {};
        }

        private void invalidate() {
            listener.run();
        }
    }
}
