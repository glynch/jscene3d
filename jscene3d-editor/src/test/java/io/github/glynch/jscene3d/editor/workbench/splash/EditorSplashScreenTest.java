/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.splash;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.builtin.text.JavaFxMonacoEditorProbe;
import io.github.glynch.jscene3d.editor.project.loading.EditorLoadingPhase;
import io.github.glynch.jscene3d.editor.workbench.appearance.EditorTheme;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxCollectionAccessibilityAssertions;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises splash visibility against the real JavaFX animation lifecycle. */
final class EditorSplashScreenTest {
    /** Starts minimum visibility when the splash can actually be seen, not while its stage is hidden. */
    @Test
    void startsMinimumVisibilityWhenStageIsShown(@TempDir Path temporaryDirectory) throws InterruptedException {
        AtomicBoolean visibleAfterObservation = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Path monacoProbeSource = temporaryDirectory.resolve("MonacoProbe.java");

        Platform.startup(() -> runVisibilityScenario(monacoProbeSource, visibleAfterObservation, failure, completed));

        assertThat(completed.await(12, SECONDS)).isTrue();
        assertThat(failure.get()).isNull();
        assertThat(visibleAfterObservation).isTrue();
    }

    /** Runs the staged visibility scenario entirely on the JavaFX application thread. */
    private static void runVisibilityScenario(
            Path monacoProbeSource,
            AtomicBoolean visibleAfterObservation,
            AtomicReference<Throwable> failure,
            CountDownLatch completed) {
        Platform.setImplicitExit(false);
        try {
            EditorSplashScreen splash = new EditorSplashScreen("test", new EditorSplashTiming(Duration.ofSeconds(3)));
            Stage stage = new Stage();
            Scene scene = new Scene(splash, 640.0, 400.0);
            EditorTheme.install(scene);
            stage.setScene(scene);
            PauseTransition beforeStageIsShown = new PauseTransition(javafx.util.Duration.seconds(3.1));
            beforeStageIsShown.setOnFinished(ignored ->
                    showAndObserve(stage, splash, monacoProbeSource, visibleAfterObservation, failure, completed));
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
            Path monacoProbeSource,
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
                finishJavaFxScenario(stage, completed);
                return;
            }
            JavaFxMonacoEditorProbe.verify(stage, monacoProbeSource, result -> {
                result.ifPresent(failure::set);
                finishJavaFxScenario(stage, completed);
            });
        });
        observation.play();
    }

    private static void finishJavaFxScenario(Stage stage, CountDownLatch completed) {
        completed.countDown();
        Platform.runLater(() -> {
            stage.close();
            Platform.exit();
        });
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
        JavaFxCollectionAccessibilityAssertions.assertAccessibleProjectCategories();

        Path project = Path.of("/example/a-very-long-project-directory/Doomed Corridors");
        splash.opening(project);
        splash.phaseStarted(EditorLoadingPhase.PREPARING_PREVIEW);
        Label projectName = (Label) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_PROJECT_NAME));
        Label projectPath = (Label) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_PROJECT_PATH));
        Label percentage = (Label) splash.lookup(selector(EditorStyleClasses.EDITOR_SPLASH_PERCENTAGE));
        assertThat(projectName.getText()).isEqualTo("Doomed Corridors");
        assertThat(projectPath.getTooltip().getText()).isEqualTo(project.toString());
        assertThat(percentage.getText()).isEqualTo("96%");
    }

    private static String selector(String styleClass) {
        return "." + styleClass;
    }
}
