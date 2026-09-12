/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.workbench.configuration.EditorConfigurationContext;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxEditorArea;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

/** Exercises splash visibility against the real JavaFX animation lifecycle. */
final class EditorSplashScreenTest {
    /** Starts minimum visibility when the splash can actually be seen, not while its stage is hidden. */
    @Test
    void startsMinimumVisibilityWhenStageIsShown() throws InterruptedException {
        AtomicBoolean visibleAfterObservation = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);

        Platform.startup(() -> runVisibilityScenario(visibleAfterObservation, failure, completed));

        assertThat(completed.await(6, SECONDS)).isTrue();
        assertThat(failure.get()).isNull();
        assertThat(visibleAfterObservation).isTrue();
    }

    /** Runs the staged visibility scenario entirely on the JavaFX application thread. */
    private static void runVisibilityScenario(
            AtomicBoolean visibleAfterObservation, AtomicReference<Throwable> failure, CountDownLatch completed) {
        Platform.setImplicitExit(false);
        try {
            EditorSplashScreen splash = new EditorSplashScreen("test", new EditorSplashTiming(Duration.ofSeconds(3)));
            Stage stage = new Stage();
            Scene scene = new Scene(splash, 640.0, 400.0);
            EditorTheme.install(scene);
            stage.setScene(scene);
            PauseTransition beforeStageIsShown = new PauseTransition(javafx.util.Duration.seconds(3.1));
            beforeStageIsShown.setOnFinished(
                    ignored -> showAndObserve(stage, splash, visibleAfterObservation, failure, completed));
            beforeStageIsShown.play();
        } catch (RuntimeException exception) {
            failure.set(exception);
            completed.countDown();
        }
    }

    /** Shows the stage, requests dismissal, and observes whether the splash was held visibly. */
    private static void showAndObserve(
            Stage stage,
            EditorSplashScreen splash,
            AtomicBoolean visibleAfterObservation,
            AtomicReference<Throwable> failure,
            CountDownLatch completed) {
        stage.show();
        splash.markDisplayed();
        splash.finish();
        PauseTransition observation = new PauseTransition(javafx.util.Duration.millis(500.0));
        observation.setOnFinished(ignored -> {
            try {
                visibleAfterObservation.set(splash.isVisible());
                assertProductionPresentation(splash);
            } catch (RuntimeException | AssertionError exception) {
                failure.set(exception);
            } finally {
                completed.countDown();
                Platform.runLater(() -> {
                    stage.close();
                    Platform.exit();
                });
            }
        });
        observation.play();
    }

    /** Checks packaged artwork and live loading data. */
    private static void assertProductionPresentation(EditorSplashScreen splash) {
        Region artwork = (Region) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_ARTWORK));
        assertThat(artwork.getBackground().getImages()).hasSize(1);
        ImageView mark = (ImageView) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_MARK));
        assertThat(mark.getImage().isError()).isFalse();
        assertThat(mark.isPreserveRatio()).isTrue();
        assertThat(mark.getAccessibleText()).isEqualTo("JScene3D");
        assertThat(mark.isFocusTraversable()).isFalse();

        Path project = Path.of("/example/a-very-long-project-directory/Doomed Corridors");
        splash.showProject(project);
        splash.phaseStarted(EditorLoadingPhase.PREPARING_PREVIEW);
        Label projectName = (Label) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_PROJECT_NAME));
        Label projectPath = (Label) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_PROJECT_PATH));
        Label percentage = (Label) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_PERCENTAGE));
        assertThat(projectName.getText()).isEqualTo("Doomed Corridors");
        assertThat(projectPath.getTooltip().getText()).isEqualTo(project.toString());
        assertThat(percentage.getText()).isEqualTo("96%");
        assertEditorAreaStartupPresentation();
    }

    /** Verifies the mutually exclusive no-project and project startup presentations. */
    private static void assertEditorAreaStartupPresentation() {
        EditorProjectContext projects = new EditorProjectContext();
        EditorSelectionContext selections = new EditorSelectionContext();
        EditorConfigurationContext configuration = new EditorConfigurationContext();
        AtomicBoolean openProjectRequested = new AtomicBoolean();
        StackPane previewContent = new StackPane();
        try (EditorExtensionHost extensions = new EditorExtensionHost(projects, selections, configuration);
                EditorWorkbenchLayout layout = new EditorWorkbenchLayout(extensions);
                JavaFxEditorArea editorArea = new JavaFxEditorArea(
                        extensions,
                        layout,
                        JavaFxIconRenderer.builtIn(),
                        new SimpleStringProperty("Doomed Corridors Preview"),
                        new SimpleBooleanProperty(false),
                        previewContent,
                        () -> openProjectRequested.set(true))) {
            assertThat(editorArea.node().getTabs())
                    .singleElement()
                    .extracting(Tab::getContent)
                    .isSameAs(previewContent);

            editorArea.showEmptyWorkspace();
            assertThat(editorArea.node().getTabs())
                    .singleElement()
                    .extracting(Tab::getText)
                    .isEqualTo("Welcome");
            Tab welcome = editorArea.node().getTabs().getFirst();
            Button openProject =
                    (Button) findByStyleClass(welcome.getContent(), EditorStyleClasses.EDITOR_WELCOME_OPEN_PROJECT)
                            .orElseThrow();
            openProject.fire();
            assertThat(openProjectRequested).isTrue();

            editorArea.showProjectPreview();
            assertThat(editorArea.node().getTabs()).hasSize(2);
            assertThat(editorArea.node().getSelectionModel().getSelectedItem()).isNotSameAs(welcome);

            editorArea.showEmptyWorkspace();
            assertThat(editorArea.node().getTabs())
                    .singleElement()
                    .extracting(Tab::getText)
                    .isEqualTo("Welcome");
        } finally {
            configuration.close();
        }
    }

    private static Optional<Node> findByStyleClass(Node node, String styleClass) {
        if (node.getStyleClass().contains(styleClass)) {
            return Optional.of(node);
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream()
                    .map(child -> findByStyleClass(child, styleClass))
                    .flatMap(Optional::stream)
                    .findFirst();
        }
        return Optional.empty();
    }

    private static String selector(String styleClass) {
        return "." + styleClass;
    }
}
