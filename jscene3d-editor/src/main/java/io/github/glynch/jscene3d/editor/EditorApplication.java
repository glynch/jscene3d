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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;

/** Production JavaFX shell for the JScene3D visual editor. */
public final class EditorApplication extends Application {
    private static final System.Logger LOGGER = System.getLogger(EditorApplication.class.getName());

    private final Telemetry telemetry;
    private final EditorProjectLoader projectLoader;
    private final ExecutorService projectLoadingExecutor;
    private final TreeView<EditorHierarchyNode> hierarchy;
    private final ListView<EditorAssetItem> assets;
    private final ListView<ProjectDiagnostic> diagnostics;

    private @Nullable GLCanvas canvas;
    private @Nullable ViewportController viewportController;
    private @Nullable EditorSplashScreen splashScreen;
    private boolean disposalRequested;

    /** Creates an application instance whose stage is initialized later by JavaFX. */
    public EditorApplication() {
        telemetry = Telemetry.recording(new EditorTelemetryLogger());
        projectLoader = new EditorProjectLoader(
                EditorBuildInfo.engineVersion(),
                EditorApplication.class.getClassLoader(),
                EditorExtensionPath.configured());
        projectLoadingExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform()
                .daemon()
                .name("jscene3d-editor-project-loader")
                .factory());
        hierarchy = new TreeView<>();
        assets = new ListView<>();
        diagnostics = new ListView<>();
    }

    /** Constructs the editor shell and installs its OpenGLFX viewport. */
    @Override
    public void start(Stage stage) {
        boolean startupProjectRequested = !getParameters().getUnnamed().isEmpty();
        Label projectStatus = createStatus("No project opened");
        Label viewportStatus = createStatus("Waiting for the first OpenGL frame");
        EditorSplashTiming splashTiming =
                EditorSplashTiming.fromNamedArguments(getParameters().getNamed());
        EditorSplashScreen loadingScreen = new EditorSplashScreen(EditorBuildInfo.engineVersion(), splashTiming);
        GLCanvas viewportCanvas = createCanvas();
        ViewportController controller = new ViewportController(
                viewportCanvas,
                viewportStatus,
                () -> finishStartupSplash(loadingScreen, startupProjectRequested),
                () -> completeDisposal(stage));
        canvas = viewportCanvas;
        viewportController = controller;
        splashScreen = loadingScreen;
        installViewportEvents(viewportCanvas, controller);

        BorderPane editor = new BorderPane();
        editor.setTop(createTopControls(stage, projectStatus));
        editor.setLeft(createNavigation());
        editor.setCenter(createViewportPane(viewportCanvas));
        editor.setRight(createInspector());
        editor.setBottom(createDiagnosticsPane(projectStatus, viewportStatus));
        editor.setStyle("-fx-background-color: #1b2026;");
        StackPane root = new StackPane(editor, loadingScreen);
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
        loadingScreen.markDisplayed();
        loadCommandLineProject(stage, projectStatus);
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
        scene.getStylesheets()
                .add(Objects.requireNonNull(EditorApplication.class.getResource("editor.css"), "editor.css")
                        .toExternalForm());
        return scene;
    }

    /** Creates the status line shown below the viewport. */
    private static Label createStatus(String initialText) {
        Label status = new Label(initialText);
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(6.0, 10.0, 6.0, 10.0));
        return status;
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

    /** Creates the application menu and project toolbar. */
    private VBox createTopControls(Stage stage, Label status) {
        MenuItem openProject = new MenuItem("Open Project…");
        openProject.setOnAction(ignored -> chooseProject(stage, status));
        Menu file = new Menu("File");
        file.getItems().add(openProject);

        Button openButton = new Button("Open Project…");
        openButton.setOnAction(ignored -> chooseProject(stage, status));
        ToolBar toolbar = new ToolBar(openButton);
        return new VBox(new MenuBar(file), toolbar);
    }

    /** Creates hierarchy and asset-browser regions backed by the opened project session. */
    private SplitPane createNavigation() {
        SplitPane navigation = new SplitPane(createHierarchy(), createAssetBrowser());
        navigation.setOrientation(Orientation.VERTICAL);
        navigation.setDividerPositions(0.58);
        navigation.setPrefWidth(250.0);
        return navigation;
    }

    /** Creates the initially empty authored hierarchy view. */
    private VBox createHierarchy() {
        VBox.setVgrow(hierarchy, Priority.ALWAYS);
        return new VBox(6.0, new Label("Hierarchy"), hierarchy);
    }

    /** Creates the initially empty asset browser. */
    private VBox createAssetBrowser() {
        assets.setPlaceholder(new Label("Open a project to browse its assets"));
        VBox.setVgrow(assets, Priority.ALWAYS);
        return new VBox(6.0, new Label("Assets"), assets);
    }

    /** Wraps the OpenGLFX node in an ordinary resizable JavaFX layout pane. */
    private static StackPane createViewportPane(GLCanvas viewportCanvas) {
        StackPane viewport = new StackPane(viewportCanvas);
        viewport.setPadding(new Insets(1.0));
        viewport.setStyle("-fx-background-color: #59636e;");
        return viewport;
    }

    /** Creates the placeholder inspector for a future editor selection model. */
    private static VBox createInspector() {
        Label heading = new Label("Inspector");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox inspector = new VBox(
                10.0,
                heading,
                new Separator(),
                new Label("Nothing selected"),
                new Label("Select an authored entity or asset to inspect it."));
        inspector.setPadding(new Insets(12.0));
        inspector.setPrefWidth(250.0);
        VBox.setVgrow(inspector, Priority.ALWAYS);
        return inspector;
    }

    /** Creates a compact status and structured-diagnostics region. */
    private VBox createDiagnosticsPane(Label projectStatus, Label viewportStatus) {
        diagnostics.setPlaceholder(new Label("No project diagnostics"));
        diagnostics.setCellFactory(ignored -> new DiagnosticCell());
        diagnostics.setPrefHeight(105.0);
        diagnostics.setMinHeight(72.0);
        VBox pane = new VBox(4.0, projectStatus, viewportStatus, new Label("Diagnostics"), diagnostics);
        pane.setPadding(new Insets(0.0, 8.0, 8.0, 8.0));
        return pane;
    }

    /** Opens the directory chooser and loads the selected project without starting it. */
    private void chooseProject(Stage stage, Label status) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open JScene3D Project");
        @Nullable File directory = chooser.showDialog(stage);
        if (directory != null) {
            openProject(stage, status, directory.toPath());
        }
    }

    /** Loads the optional first unnamed command-line argument as a project directory. */
    private void loadCommandLineProject(Stage stage, Label status) {
        if (!getParameters().getUnnamed().isEmpty()) {
            openProject(stage, status, Path.of(getParameters().getUnnamed().getFirst()));
        }
    }

    /** Starts loading one directory without blocking JavaFX rendering or splash progress. */
    private void openProject(Stage stage, Label status, Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        EditorProjectOpenTrace trace = new EditorProjectOpenTrace(telemetry, normalized);
        status.setText("Opening " + normalized);
        EditorSplashScreen loadingScreen = requireSplashScreen();
        loadingScreen.showProject(normalized);
        EditorProjectLoadTask task = new EditorProjectLoadTask(projectLoader, trace, normalized, loadingScreen);
        task.setOnSucceeded(ignored -> applyLoadedProject(stage, status, trace, task.getValue()));
        task.setOnFailed(ignored -> handleProjectLoadFailure(status, trace, task.getException()));
        projectLoadingExecutor.execute(task);
    }

    /** Applies background-loaded editor state and queues its preview on the OpenGL thread. */
    private void applyLoadedProject(
            Stage stage, Label status, EditorProjectOpenTrace trace, EditorProjectLoadResult result) {
        diagnostics.getItems().setAll(result.diagnostics());
        if (result.session().isEmpty()) {
            EditorProjectOpenDurations durations = trace.fail("project loading did not create an editor session");
            hierarchy.setRoot(null);
            assets.getItems().clear();
            requireViewportController().clearProject();
            status.setText("Project could not be opened after " + format(durations.total()) + " — see diagnostics");
            requireSplashScreen().finish();
            return;
        }
        EditorProjectSession session = result.session().orElseThrow();
        TreeItem<EditorHierarchyNode> root = createTreeItem(session.hierarchy());
        root.setExpanded(true);
        hierarchy.setRoot(root);
        assets.getItems().setAll(session.assets());
        stage.setTitle(session.project().identity().name() + " — JScene3D Editor");
        EditorSplashScreen loadingScreen = requireSplashScreen();
        loadingScreen.projectIdentified(session.project().identity().name());
        loadingScreen.phaseStarted(EditorLoadingPhase.PREPARING_PREVIEW);
        requireViewportController().showProject(session, trace, completion -> {
            applyPreviewDiagnostics(result.diagnostics(), session, completion, status);
            loadingScreen.finish();
        });
        status.setText(
                "Preparing viewport preview for " + session.project().identity().name());
    }

    /** Restores the editor after an unexpected background-loading failure. */
    private void handleProjectLoadFailure(Label status, EditorProjectOpenTrace trace, Throwable failure) {
        trace.fail(failure);
        hierarchy.setRoot(null);
        assets.getItems().clear();
        requireViewportController().clearProject();
        status.setText("Project could not be opened — " + Objects.requireNonNullElse(failure.getMessage(), failure));
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
            List<ProjectDiagnostic> projectDiagnostics,
            EditorProjectSession session,
            EditorPreviewResult result,
            Label status) {
        List<ProjectDiagnostic> combined = new ArrayList<>(projectDiagnostics);
        combined.addAll(result.diagnostics());
        diagnostics.getItems().setAll(combined);
        boolean failed = result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (failed) {
            status.setText("Project opened in " + format(result.durations().total())
                    + ", but its viewport preview could not be composed — see diagnostics");
            return;
        }
        long errors = combined.stream()
                .filter(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR)
                .count();
        EditorProjectOpenDurations durations = result.durations();
        String firstFrame =
                durations.firstPresentation().map(EditorApplication::format).orElse("not presented");
        status.setText("Opened " + session.project().identity().name() + " in " + format(durations.total())
                + " (project " + format(durations.projectLoad())
                + ", preview " + format(durations.previewComposition())
                + ", first frame " + firstFrame + ") — "
                + session.hierarchy().children().size() + " root entities, "
                + session.assets().size() + " assets, " + errors + " errors");
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

    /** Converts one immutable hierarchy projection into JavaFX tree items. */
    private static TreeItem<EditorHierarchyNode> createTreeItem(EditorHierarchyNode node) {
        TreeItem<EditorHierarchyNode> item = new TreeItem<>(node);
        item.getChildren()
                .setAll(node.children().stream()
                        .map(EditorApplication::createTreeItem)
                        .toList());
        return item;
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
        stage.setOnCloseRequest(null);
        stage.close();
        Platform.exit();
    }

    /** Formats structured project diagnostics without discarding their stable code or source. */
    private static final class DiagnosticCell extends ListCell<ProjectDiagnostic> {
        @Override
        protected void updateItem(@Nullable ProjectDiagnostic diagnostic, boolean empty) {
            super.updateItem(diagnostic, empty);
            if (empty || diagnostic == null) {
                setText(null);
                setStyle("");
                return;
            }
            String location = diagnostic.location().isEmpty() ? "" : diagnostic.location();
            String detail = diagnostic.details().getOrDefault("technicalDetail", diagnostic.message());
            setText(diagnostic.severity() + "  " + diagnostic.code().code() + "  " + detail + "  —  "
                    + diagnostic.source() + location);
            setStyle(
                    diagnostic.severity() == ProjectDiagnostic.Severity.ERROR
                            ? "-fx-text-fill: #ff8a80;"
                            : "-fx-text-fill: #ffd180;");
        }
    }
}
