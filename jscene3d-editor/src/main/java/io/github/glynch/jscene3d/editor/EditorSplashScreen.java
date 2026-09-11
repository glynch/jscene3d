/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** JScene3D-branded startup and project-loading view shown above the editor shell. */
public final class EditorSplashScreen extends StackPane implements EditorProjectLoadProgress {
    private static final javafx.util.Duration FADE_DURATION = javafx.util.Duration.millis(180.0);
    private static final String ARTWORK_RESOURCE = "splash/viewport-emergence-background.png";
    private static final String MARK_RESOURCE = "splash/jscene3d-mark.png";

    private final EditorSplashTiming timing;
    private final Label projectName = new Label();
    private final Label projectPath = new Label();
    private final Label phase = new Label();
    private final Label percentage = new Label();
    private final ProgressBar progress = new ProgressBar();
    private final VBox projectDetails = new VBox(4.0, projectName, projectPath);
    private final Region projectDivider = createDivider();
    private final HBox progressLine = new HBox(12.0, progress, percentage);

    private @Nullable FadeTransition fade;
    private @Nullable PauseTransition dismissalDelay;
    private long displayedAtNanos = -1L;

    /** Creates the full-frame product splash with live project and loading information. */
    EditorSplashScreen(String engineVersion, EditorSplashTiming timing) {
        this.timing = Objects.requireNonNull(timing, "timing");
        getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH);

        Region artwork = new Region();
        artwork.setBackground(coveringBackground(ARTWORK_RESOURCE));
        artwork.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_ARTWORK);
        Region scrim = new Region();
        scrim.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_SCRIM);

        BorderPane composition = new BorderPane();
        composition.setTop(createBrand());
        composition.setBottom(createInformationBar(engineVersion));
        composition.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_CONTENT);
        getChildren().setAll(artwork, scrim, composition);

        configureLiveInformation();
        showStartup();
    }

    /** Restores the initial product-startup presentation. */
    void showStartup() {
        showOverlay(false);
        showProjectDetails(false);
        showPhase(EditorLoadingPhase.STARTING_EDITOR);
    }

    /**
     * Shows project context before its authored name is available from the manifest.
     *
     * @param directory project directory being opened
     */
    public void showProject(Path directory) {
        Path normalized =
                Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        showOverlay(!isVisible());
        showProjectDetails(true);
        Path fileName = normalized.getFileName();
        setProjectName(fileName == null ? normalized.toString() : fileName.toString());
        setProjectPath(normalized.toString());
        showPhase(EditorLoadingPhase.READING_MANIFEST);
    }

    /** Starts minimum-visibility timing once the containing editor stage has been shown. */
    void markDisplayed() {
        displayedAtNanos = System.nanoTime();
    }

    /** Replaces the directory-derived name once the project manifest supplies its identity. */
    @Override
    public void projectIdentified(String authoredProjectName) {
        setProjectName(Objects.requireNonNull(authoredProjectName, "authoredProjectName"));
    }

    /** Displays one actual phase and its cumulative phase progress. */
    @Override
    public void phaseStarted(EditorLoadingPhase loadingPhase) {
        showPhase(loadingPhase);
    }

    /** Completes progress and dismisses the overlay after its configured minimum visibility. */
    public void finish() {
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

    /** Creates the original product mark and live JScene3D Editor wordmark. */
    private static HBox createBrand() {
        ImageView mark = new ImageView(loadImage(MARK_RESOURCE));
        mark.setFitWidth(72.0);
        mark.setFitHeight(72.0);
        mark.setPreserveRatio(true);
        mark.setAccessibleText("JScene3D");
        mark.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_MARK);

        HBox wordmark = new HBox(
                12.0,
                styledLabel("JScene3D", EditorStyleClasses.EDITOR_SPLASH_BRAND),
                styledLabel("Editor", EditorStyleClasses.EDITOR_SPLASH_BRAND_ACCENT));
        wordmark.setAlignment(Pos.CENTER_LEFT);
        wordmark.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_WORDMARK);

        HBox brand = new HBox(22.0, mark, wordmark);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        brand.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_IDENTITY);
        BorderPane.setMargin(brand, new Insets(46.0, 48.0, 0.0, 48.0));
        return brand;
    }

    /** Creates the full-width lower region containing only live JavaFX information. */
    private HBox createInformationBar(String engineVersion) {
        VBox loading = createProgress();
        HBox.setHgrow(loading, Priority.ALWAYS);
        Label version = createVersion(engineVersion);
        HBox information = new HBox(30.0, projectDetails, projectDivider, loading, createDivider(), version);
        information.setAlignment(Pos.CENTER_LEFT);
        information.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_INFORMATION);
        return information;
    }

    /** Configures truncation, accessibility, and sizing for data that changes during loading. */
    private void configureLiveInformation() {
        projectName.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PROJECT_NAME);
        projectName.setTextOverrun(OverrunStyle.ELLIPSIS);
        projectName.setMaxWidth(Double.MAX_VALUE);
        projectPath.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PROJECT_PATH);
        projectPath.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        projectPath.setMaxWidth(Double.MAX_VALUE);
        projectDetails.setMinWidth(180.0);
        projectDetails.setPrefWidth(360.0);
        projectDetails.setMaxWidth(420.0);
        projectDetails.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PROJECT);

        phase.setTextOverrun(OverrunStyle.ELLIPSIS);
        phase.setMaxWidth(Double.MAX_VALUE);
        progress.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progress, Priority.ALWAYS);
        progressLine.setAlignment(Pos.CENTER_LEFT);
    }

    /** Creates the phase label, percentage, and progress indicator. */
    private VBox createProgress() {
        phase.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PHASE);
        progress.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PROGRESS);
        percentage.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_PERCENTAGE);
        VBox loading = new VBox(9.0, phase, progressLine);
        loading.setMinWidth(220.0);
        loading.setMaxWidth(Double.MAX_VALUE);
        loading.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_LOADING);
        return loading;
    }

    /** Creates the version line displayed in the lower information region. */
    private static Label createVersion(String engineVersion) {
        Label version = new Label("JScene3D " + Objects.requireNonNull(engineVersion, "engineVersion"));
        version.setMinWidth(150.0);
        version.setAlignment(Pos.CENTER_RIGHT);
        version.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_VERSION);
        return version;
    }

    /** Creates one structural separator used inside the lower information region. */
    private static Region createDivider() {
        Region divider = new Region();
        divider.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_DIVIDER);
        return divider;
    }

    /** Creates one label and assigns its splash-specific style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** Loads one required packaged image and fails startup clearly if packaging is incomplete. */
    private static Image loadImage(String resourceName) {
        URL resource = EditorSplashScreen.class.getResource(resourceName);
        if (resource == null) {
            throw new IllegalStateException("splash image resource was not found: " + resourceName);
        }
        return new Image(resource.toExternalForm(), false);
    }

    /** Creates a background that fills the frame while retaining the artwork's aspect ratio. */
    private static Background coveringBackground(String resourceName) {
        BackgroundSize cover = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true);
        BackgroundImage image = new BackgroundImage(
                loadImage(resourceName),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                cover);
        return new Background(image);
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
        percentage.setText(Math.round(validPhase.progress() * 100.0) + "%");
    }

    /** Applies a project name while preserving the full value for pointer and assistive access. */
    private void setProjectName(String name) {
        projectName.setText(name);
        projectName.setTooltip(new Tooltip(name));
        projectName.setAccessibleText(name);
    }

    /** Applies a project path while preserving the full value for pointer and assistive access. */
    private void setProjectPath(String path) {
        projectPath.setText(path);
        projectPath.setTooltip(new Tooltip(path));
        projectPath.setAccessibleText(path);
    }

    /** Shows or removes the complete project-information column and its separator. */
    private void showProjectDetails(boolean show) {
        projectDetails.setManaged(show);
        projectDetails.setVisible(show);
        projectDivider.setManaged(show);
        projectDivider.setVisible(show);
    }

    /** Starts the short visual transition that finally removes the overlay. */
    private void startFade() {
        fade = new FadeTransition(FADE_DURATION, this);
        fade.setFromValue(getOpacity());
        fade.setToValue(0.0);
        fade.setOnFinished(ignored -> {
            fade = null;
            dismissImmediately();
        });
        fade.play();
    }

    /** Removes the modal overlay immediately after an explicit failure action or completed fade. */
    private void dismissImmediately() {
        setVisible(false);
        setManaged(false);
        setMouseTransparent(true);
        setOpacity(0.0);
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
