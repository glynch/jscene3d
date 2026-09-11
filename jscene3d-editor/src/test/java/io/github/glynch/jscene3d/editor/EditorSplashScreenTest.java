/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
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
                    ignored -> showAndObserve(stage, splash, visibleAfterObservation, completed));
            beforeStageIsShown.play();
        } catch (RuntimeException exception) {
            failure.set(exception);
            completed.countDown();
        }
    }

    /** Shows the stage, requests dismissal, and observes whether the splash was held visibly. */
    private static void showAndObserve(
            Stage stage, EditorSplashScreen splash, AtomicBoolean visibleAfterObservation, CountDownLatch completed) {
        stage.show();
        splash.markDisplayed();
        splash.finish();
        PauseTransition observation = new PauseTransition(javafx.util.Duration.millis(500.0));
        observation.setOnFinished(ignored -> {
            visibleAfterObservation.set(splash.isVisible());
            completed.countDown();
            Platform.runLater(() -> {
                stage.close();
                Platform.exit();
            });
        });
        observation.play();
    }
}
