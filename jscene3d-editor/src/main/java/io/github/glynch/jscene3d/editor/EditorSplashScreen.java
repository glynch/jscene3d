/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import org.jspecify.annotations.Nullable;

/** JScene3D-branded startup and project-loading view shown above the editor shell. */
final class EditorSplashScreen extends StackPane implements EditorProjectLoadProgress {
    private static final javafx.util.Duration FADE_DURATION = javafx.util.Duration.millis(180.0);

    private final EditorSplashTiming timing;
    private final Label projectName = new Label();
    private final Label projectPath = new Label();
    private final Label phase = new Label();
    private final ProgressBar progress = new ProgressBar();
    private final VBox projectDetails = new VBox(4.0, projectName, projectPath);

    private @Nullable FadeTransition fade;
    private @Nullable PauseTransition dismissalDelay;
    private long displayedAtNanos = -1L;

    /** Creates the product splash with editor branding and the embedded engine version. */
    EditorSplashScreen(String engineVersion, EditorSplashTiming timing) {
        this.timing = Objects.requireNonNull(timing, "timing");
        getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH);
        setAlignment(Pos.CENTER);

        VBox card = new VBox(
                24.0, createBrand(), createRule(), projectDetails, createProgress(), createVersion(engineVersion));
        card.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_CARD);
        card.setMaxWidth(680.0);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        getChildren().add(card);

        projectName.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PROJECT_NAME);
        projectPath.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PROJECT_PATH);
        projectPath.setWrapText(true);
        projectDetails.setVisible(false);
        projectDetails.setManaged(false);
        showStartup();
    }

    /** Restores the initial product-startup presentation. */
    void showStartup() {
        showOverlay(false);
        projectDetails.setVisible(false);
        projectDetails.setManaged(false);
        showPhase(EditorLoadingPhase.STARTING_EDITOR);
    }

    /** Shows project context before its authored name is available from the manifest. */
    void showProject(Path directory) {
        Path normalized =
                Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        showOverlay(!isVisible());
        projectDetails.setVisible(true);
        projectDetails.setManaged(true);
        Path fileName = normalized.getFileName();
        projectName.setText(fileName == null ? normalized.toString() : fileName.toString());
        projectPath.setText(normalized.toString());
        showPhase(EditorLoadingPhase.READING_MANIFEST);
    }

    /** Starts minimum-visibility timing once the containing editor stage has been shown. */
    void markDisplayed() {
        displayedAtNanos = System.nanoTime();
    }

    /** Replaces the directory-derived name once the project manifest supplies its identity. */
    @Override
    public void projectIdentified(String authoredProjectName) {
        projectName.setText(Objects.requireNonNull(authoredProjectName, "authoredProjectName"));
    }

    /** Displays one actual phase and its cumulative phase progress. */
    @Override
    public void phaseStarted(EditorLoadingPhase loadingPhase) {
        showPhase(loadingPhase);
    }

    /** Completes progress and dismisses the overlay after its configured minimum visibility. */
    void finish() {
        showPhase(EditorLoadingPhase.READY);
        stopDismissal();
        Duration visibleFor = displayedAtNanos < 0L
                ? Duration.ZERO
                : Duration.ofNanos(Math.max(0L, System.nanoTime() - displayedAtNanos));
        Duration remaining = timing.remainingAfter(visibleFor);
        if (remaining.isZero()) {
            startFade();
            return;
        }
        dismissalDelay = new PauseTransition(javafx.util.Duration.millis(remaining.toNanos() / 1_000_000.0));
        dismissalDelay.setOnFinished(ignored -> {
            dismissalDelay = null;
            startFade();
        });
        dismissalDelay.play();
    }

    /** Starts the short visual transition that finally removes the overlay. */
    private void startFade() {
        fade = new FadeTransition(FADE_DURATION, this);
        fade.setFromValue(getOpacity());
        fade.setToValue(0.0);
        fade.setOnFinished(ignored -> {
            fade = null;
            setVisible(false);
            setManaged(false);
            setMouseTransparent(true);
        });
        fade.play();
    }

    /** Creates the cube mark and two-part JScene3D product wordmark. */
    private static HBox createBrand() {
        HBox wordmark = new HBox(
                0.0,
                styledLabel("JSCENE", EditorStyleClasses.EDITOR_SPLASH_BRAND),
                styledLabel("3D", EditorStyleClasses.EDITOR_SPLASH_BRAND_ACCENT));
        wordmark.setAlignment(Pos.CENTER_LEFT);
        VBox title = new VBox(0.0, wordmark, styledLabel("EDITOR", EditorStyleClasses.EDITOR_SPLASH_PRODUCT));
        title.setAlignment(Pos.CENTER_LEFT);
        HBox brand = new HBox(22.0, createCubeMark(), title);
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    /** Creates a compact isometric cube mark without requiring toolkit-specific image assets. */
    private static Pane createCubeMark() {
        Polygon top = polygon(EditorStyleClasses.EDITOR_SPLASH_MARK_TOP, 46.0, 0.0, 88.0, 22.0, 46.0, 44.0, 4.0, 22.0);
        Polygon left =
                polygon(EditorStyleClasses.EDITOR_SPLASH_MARK_LEFT, 4.0, 22.0, 46.0, 44.0, 46.0, 88.0, 4.0, 66.0);
        Polygon right =
                polygon(EditorStyleClasses.EDITOR_SPLASH_MARK_RIGHT, 46.0, 44.0, 88.0, 22.0, 88.0, 66.0, 46.0, 88.0);
        Pane mark = new Pane(top, left, right);
        mark.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_MARK);
        mark.setMinSize(92.0, 92.0);
        mark.setPrefSize(92.0, 92.0);
        mark.setMaxSize(92.0, 92.0);
        return mark;
    }

    /** Creates one filled polygon used by the product cube mark. */
    private static Polygon polygon(String styleClass, double... coordinates) {
        Polygon polygon = new Polygon(coordinates);
        polygon.getStyleClass().add(styleClass);
        return polygon;
    }

    /** Creates the restrained accent rule beneath the product identity. */
    private static Region createRule() {
        Region rule = new Region();
        rule.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_RULE);
        rule.setMinHeight(2.0);
        rule.setPrefHeight(2.0);
        rule.setMaxHeight(2.0);
        return rule;
    }

    /** Creates the phase label and progress indicator. */
    private VBox createProgress() {
        phase.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PHASE);
        progress.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PROGRESS);
        progress.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(progress, Priority.NEVER);
        return new VBox(10.0, phase, progress);
    }

    /** Creates the version line displayed at the bottom of the splash card. */
    private static Label createVersion(String engineVersion) {
        Label version = new Label("JScene3D " + Objects.requireNonNull(engineVersion, "engineVersion"));
        version.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_VERSION);
        return version;
    }

    /** Creates one label and assigns its splash-specific style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** Makes the overlay modal and optionally starts a new minimum-visibility interval. */
    private void showOverlay(boolean restartMinimumVisibility) {
        stopDismissal();
        if (restartMinimumVisibility) {
            markDisplayed();
        }
        setManaged(true);
        setVisible(true);
        setMouseTransparent(false);
        setOpacity(1.0);
    }

    /** Applies one phase without changing project context or overlay visibility. */
    private void showPhase(EditorLoadingPhase loadingPhase) {
        EditorLoadingPhase validPhase = Objects.requireNonNull(loadingPhase, "loadingPhase");
        phase.setText(validPhase.description());
        progress.setProgress(validPhase.progress());
    }

    /** Stops and discards an in-progress fade before the overlay is reused. */
    private void stopFade() {
        if (fade != null) {
            fade.stop();
            fade = null;
        }
    }

    /** Cancels every pending or active dismissal before the overlay is reused. */
    private void stopDismissal() {
        if (dismissalDelay != null) {
            dismissalDelay.stop();
            dismissalDelay = null;
        }
        stopFade();
    }
}
