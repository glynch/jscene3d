/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static javafx.util.Duration.seconds;

import com.huskerdev.grapl.gl.GLProfile;
import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.telemetry.Telemetry;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;

/** Production JavaFX shell for the JScene3D visual editor. */
public final class EditorApplication extends Application {
    private static final System.Logger LOGGER = System.getLogger(EditorApplication.class.getName());

    private final Telemetry telemetry;
    private final EditorProjectLoader projectLoader;
    private final ExecutorService projectLoadingExecutor;

    private @Nullable GLCanvas canvas;
    private @Nullable ViewportController viewportController;
    private @Nullable EditorSplashScreen splashScreen;
    private @Nullable EditorWorkspace workspace;
    private boolean disposalRequested;

    /** Creates an application instance whose stage is initialized later by JavaFX. */
    public EditorApplication() {
        telemetry = Telemetry.recording(new EditorTelemetryLogger());
        projectLoader = new EditorProjectLoader(
                EditorBuildInfo.engineVersion(),
                EditorApplication.class.getClassLoader(),
                EditorExtensionPath.configured());
        projectLoadingExecutor = Executors.newSingleThreadExecutor();
    }

    /** Constructs the editor shell and installs its OpenGLFX viewport. */
    @Override
    public void start(Stage stage) {
        boolean startupProjectRequested = !getParameters().getUnnamed().isEmpty();
        EditorSplashTiming splashTiming =
                EditorSplashTiming.fromNamedArguments(getParameters().getNamed());
        EditorSplashScreen loadingScreen = new EditorSplashScreen(EditorBuildInfo.engineVersion(), splashTiming);
        GLCanvas viewportCanvas = createCanvas();
        EditorWorkspace editorWorkspace = new EditorWorkspace(viewportCanvas, () -> chooseProject(stage));
        ViewportController controller = new ViewportController(
                viewportCanvas,
                editorWorkspace.viewportStatus(),
                () -> finishStartupSplash(loadingScreen, startupProjectRequested),
                () -> completeDisposal(stage));
        canvas = viewportCanvas;
        viewportController = controller;
        splashScreen = loadingScreen;
        workspace = editorWorkspace;
        installViewportEvents(viewportCanvas, controller);

        StackPane root = new StackPane(editorWorkspace, loadingScreen);
        root.getStyleClass().add("editor-root");
        loadingScreen.phaseStarted(EditorLoadingPhase.PREPARING_VIEWPORT);

        stage.setTitle("JScene3D Editor");
        stage.setScene(createScene(root));
        stage.setMinWidth(800.0);
        stage.setMinHeight(520.0);
        stage.setOnCloseRequest(event -> {
            event.consume();
            disposeCanvas();
        });
        stage.show();
        Platform.runLater(editorWorkspace::applyInitialDividerPositions);
        loadingScreen.markDisplayed();
        loadCommandLineProject(stage);
        viewportCanvas.requestFocus();
        scheduleAutomaticClose();
    }

    /** Releases the OpenGLFX peer if JavaFX stops without an ordinary close request. */
    @Override
    public void stop() {
        projectLoadingExecutor.shutdownNow();
        disposeCanvas();
    }

    /** Creates the editor scene and installs its packaged visual theme. */
    private static Scene createScene(StackPane root) {
        Scene scene = new Scene(root, 1280.0, 780.0);
        EditorTheme.install(scene);
        return scene;
    }

    /** Creates a core-profile OpenGLFX canvas configured for the renderer baseline. */
    private static GLCanvas createCanvas() {
        GLCanvas.Builder.ContextDescription.New context = new GLCanvas.Builder.ContextDescription.New()
                .setProfile(GLProfile.CORE)
                .setMajorVersion(3)
                .setMinorVersion(3);
        GLCanvas result = new GLCanvas.Builder()
                .setExecutor(LWJGLExecutor.LWJGL_MODULE)
                .setContextDescription(context)
                .setFlipY(false)
                .setMSAA(0)
                .setSwapBuffers(2)
                .setFps(60.0)
                .build();
        result.setMinSize(1.0, 1.0);
        result.setPrefSize(800.0, 600.0);
        result.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        result.setFocusTraversable(true);
        return result;
    }

    /** Installs rendering and focus callbacks on the viewport. */
    private void installViewportEvents(GLCanvas viewportCanvas, ViewportController controller) {
        viewportCanvas.addOnRenderEvent(controller::render);
        viewportCanvas.addOnDisposeEvent(ignored -> controller.dispose());
        viewportCanvas.focusedProperty().addListener((ignored, oldValue, focused) -> controller.setFocused(focused));
        viewportCanvas.setOnMousePressed(ignored -> viewportCanvas.requestFocus());
    }

    /** Opens the directory chooser and loads the selected project without starting it. */
    private void chooseProject(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open JScene3D Project");
        @Nullable File directory = chooser.showDialog(stage);
        if (directory != null) {
            openProject(stage, directory.toPath());
        }
    }

    /** Loads the optional first unnamed command-line argument as a project directory. */
    private void loadCommandLineProject(Stage stage) {
        if (!getParameters().getUnnamed().isEmpty()) {
            openProject(stage, Path.of(getParameters().getUnnamed().getFirst()));
        }
    }

    /** Starts loading one directory without blocking JavaFX rendering or splash progress. */
    private void openProject(Stage stage, Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        EditorProjectOpenTrace trace = new EditorProjectOpenTrace(telemetry, normalized);
        requireWorkspace().beginOpening(normalized);
        EditorSplashScreen loadingScreen = requireSplashScreen();
        loadingScreen.showProject(normalized);
        EditorProjectLoadTask task = new EditorProjectLoadTask(projectLoader, trace, normalized, loadingScreen);
        task.setOnSucceeded(ignored -> applyLoadedProject(stage, trace, task.getValue()));
        task.setOnFailed(ignored -> handleProjectLoadFailure(trace, task.getException()));
        projectLoadingExecutor.execute(task);
    }

    /** Applies background-loaded editor state and queues its preview on the OpenGL thread. */
    private void applyLoadedProject(Stage stage, EditorProjectOpenTrace trace, EditorProjectLoadResult result) {
        EditorWorkspace editorWorkspace = requireWorkspace();
        editorWorkspace.showDiagnostics(result.diagnostics());
        if (result.session().isEmpty()) {
            trace.fail("project loading did not create an editor session");
            editorWorkspace.clearProject();
            requireViewportController().clearProject();
            editorWorkspace.setProjectStatus("Open failed — see Diagnostics");
            editorWorkspace.openDiagnostics();
            requireSplashScreen().finish();
            return;
        }
        EditorProjectSession session = result.session().orElseThrow();
        editorWorkspace.showProject(session);
        stage.setTitle(session.project().identity().name() + " — JScene3D Editor");
        EditorSplashScreen loadingScreen = requireSplashScreen();
        loadingScreen.projectIdentified(session.project().identity().name());
        loadingScreen.phaseStarted(EditorLoadingPhase.PREPARING_PREVIEW);
        requireViewportController().showProject(session, trace, completion -> {
            applyPreviewDiagnostics(result.diagnostics(), session, completion);
            loadingScreen.finish();
        });
        editorWorkspace.setProjectStatus(
                "Preparing " + session.project().identity().name() + "…");
    }

    /** Restores the editor after an unexpected background-loading failure. */
    private void handleProjectLoadFailure(EditorProjectOpenTrace trace, Throwable failure) {
        trace.fail(failure);
        EditorWorkspace editorWorkspace = requireWorkspace();
        editorWorkspace.clearProject();
        requireViewportController().clearProject();
        editorWorkspace.setProjectStatus("Open failed — see log");
        LOGGER.log(System.Logger.Level.ERROR, "Editor project loading failed", failure);
        requireSplashScreen().finish();
    }

    /** Dismisses startup branding after the empty viewport presents when no project was requested. */
    private static void finishStartupSplash(EditorSplashScreen loadingScreen, boolean startupProjectRequested) {
        if (!startupProjectRequested) {
            loadingScreen.finish();
        }
    }

    /** Combines project-loading and viewport-composition diagnostics after render-thread preparation. */
    private void applyPreviewDiagnostics(
            List<ProjectDiagnostic> projectDiagnostics, EditorProjectSession session, EditorPreviewResult result) {
        List<ProjectDiagnostic> combined = new ArrayList<>(projectDiagnostics);
        combined.addAll(result.diagnostics());
        EditorWorkspace editorWorkspace = requireWorkspace();
        editorWorkspace.showDiagnostics(combined);
        boolean failed = result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (failed) {
            editorWorkspace.setProjectStatus(session.project().identity().name() + " · preview failed");
            editorWorkspace.openDiagnostics();
            return;
        }
        EditorProjectOpenDurations durations = result.durations();
        String firstFrame =
                durations.firstPresentation().map(EditorApplication::format).orElse("not presented");
        LOGGER.log(
                System.Logger.Level.INFO,
                "Opened " + session.project().identity().name() + " in "
                        + format(durations.total())
                        + " (project " + format(durations.projectLoad())
                        + ", preview " + format(durations.previewComposition())
                        + ", first frame " + firstFrame + ")");
        editorWorkspace.setProjectStatus(session.project().identity().name() + " · ready");
    }

    /** Formats a measured duration in milliseconds with useful sub-millisecond precision. */
    private static String format(Duration duration) {
        double milliseconds = duration.toNanos() / 1_000_000.0;
        return String.format(Locale.ROOT, "%.1f ms", milliseconds);
    }

    /** Returns the controller installed during JavaFX stage initialization. */
    private ViewportController requireViewportController() {
        ViewportController controller = viewportController;
        if (controller == null) {
            throw new IllegalStateException("editor viewport has not been initialized");
        }
        return controller;
    }

    /** Returns the splash view installed during JavaFX stage initialization. */
    private EditorSplashScreen requireSplashScreen() {
        EditorSplashScreen current = splashScreen;
        if (current == null) {
            throw new IllegalStateException("editor splash screen has not been initialized");
        }
        return current;
    }

    /** Returns the workspace installed during JavaFX stage initialization. */
    private EditorWorkspace requireWorkspace() {
        EditorWorkspace current = workspace;
        if (current == null) {
            throw new IllegalStateException("editor workspace has not been initialized");
        }
        return current;
    }

    /** Optionally closes automated smoke runs while leaving ordinary launches interactive. */
    private void scheduleAutomaticClose() {
        String commandLineSeconds = getParameters().getNamed().get("auto-close-seconds");
        int seconds = commandLineSeconds == null
                ? Integer.getInteger("jscene3d.editor.autoCloseSeconds", 0)
                : Integer.parseInt(commandLineSeconds);
        if (seconds <= 0) {
            return;
        }
        PauseTransition closeDelay = new PauseTransition(seconds(seconds));
        closeDelay.setOnFinished(ignored -> disposeCanvas());
        closeDelay.play();
    }

    /** Disposes the OpenGLFX canvas at most once while its JavaFX peer remains available. */
    private void disposeCanvas() {
        GLCanvas currentCanvas = canvas;
        if (currentCanvas == null || disposalRequested) {
            return;
        }
        disposalRequested = true;
        projectLoadingExecutor.shutdownNow();
        currentCanvas.dispose();
    }

    /** Closes JavaFX only after the render thread releases the renderer and surface. */
    private void completeDisposal(Stage stage) {
        canvas = null;
        viewportController = null;
        splashScreen = null;
        workspace = null;
        stage.setOnCloseRequest(null);
        stage.close();
        Platform.exit();
    }
}
