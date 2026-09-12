/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static javafx.util.Duration.seconds;

import com.huskerdev.grapl.gl.GLProfile;
import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import io.github.glynch.jscene3d.editor.builtin.diagnostics.DiagnosticsExtension;
import io.github.glynch.jscene3d.editor.builtin.diagnostics.ProjectDiagnosticsExtension;
import io.github.glynch.jscene3d.editor.builtin.extensions.ExtensionsExtension;
import io.github.glynch.jscene3d.editor.builtin.hierarchy.HierarchyExtension;
import io.github.glynch.jscene3d.editor.builtin.inspector.InspectorExtension;
import io.github.glynch.jscene3d.editor.builtin.project.ProjectExtension;
import io.github.glynch.jscene3d.editor.builtin.status.SelectionStatusExtension;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.opening.EditorProjectOpener;
import io.github.glynch.jscene3d.editor.project.opening.EditorProjectPublication;
import io.github.glynch.jscene3d.editor.workbench.configuration.EditorConfigurationContext;
import io.github.glynch.jscene3d.editor.workbench.dialog.JavaFxModalDialogs;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.telemetry.Telemetry;
import java.io.File;
import java.nio.file.Path;
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
    private final Telemetry telemetry;
    private final EditorProjectLoader projectLoader;
    private final EditorProjectContext projectContext;
    private final EditorSelectionContext selectionContext;
    private final EditorConfigurationContext configurationContext;
    private final EditorExtensionHost extensionHost;

    private @Nullable GLCanvas canvas;
    private @Nullable EditorWorkspace workspace;
    private @Nullable EditorProjectOpener projectOpener;
    private boolean disposalRequested;

    /** Creates an application instance whose stage is initialized later by JavaFX. */
    public EditorApplication() {
        telemetry = Telemetry.recording(new EditorTelemetryLogger());
        projectLoader = new EditorProjectLoader(
                EditorBuildInfo.engineVersion(),
                EditorApplication.class.getClassLoader(),
                EditorExtensionPath.configured());
        projectContext = new EditorProjectContext();
        selectionContext = new EditorSelectionContext();
        configurationContext = new EditorConfigurationContext();
        extensionHost = new EditorExtensionHost(projectContext, selectionContext, configurationContext);
    }

    /** Constructs the editor shell and installs its OpenGLFX viewport. */
    @Override
    public void start(Stage stage) {
        EditorBuildInfo buildInfo = EditorBuildInfo.current();
        boolean startupProjectRequested = !getParameters().getUnnamed().isEmpty();
        EditorSplashTiming splashTiming =
                EditorSplashTiming.fromNamedArguments(getParameters().getNamed());
        EditorSplashScreen loadingScreen = new EditorSplashScreen(buildInfo.version(), splashTiming);
        GLCanvas viewportCanvas = createCanvas();
        JavaFxModalDialogs dialogs = new JavaFxModalDialogs(stage);
        extensionHost.showDialogsWith(dialogs::show);
        EditorWorkspace editorWorkspace = new EditorWorkspace(
                viewportCanvas,
                () -> chooseProject(stage),
                this::requestClose,
                selectionContext,
                extensionHost,
                buildInfo);
        extensionHost.showMessagesWith(editorWorkspace::showMessage);
        ProjectDiagnosticsExtension projectDiagnostics = new ProjectDiagnosticsExtension();
        extensionHost.activate(projectDiagnostics);
        extensionHost.activate(new HierarchyExtension(projectContext));
        extensionHost.activate(new ExtensionsExtension());
        extensionHost.activate(new ProjectExtension(projectContext));
        extensionHost.activate(new InspectorExtension());
        extensionHost.activate(new SelectionStatusExtension());
        extensionHost.activate(new DiagnosticsExtension(extensionHost));
        ViewportController controller = new ViewportController(
                viewportCanvas,
                editorWorkspace::setViewportStatus,
                () -> finishStartupSplash(loadingScreen, startupProjectRequested),
                () -> completeDisposal(stage));
        canvas = viewportCanvas;
        workspace = editorWorkspace;
        projectOpener = new EditorProjectOpener(
                telemetry,
                projectLoader,
                new EditorProjectPublication(projectContext, projectDiagnostics::showDiagnostics, configurationContext),
                editorWorkspace,
                controller,
                loadingScreen,
                stage::setTitle);
        installViewportEvents(viewportCanvas, controller);

        StackPane root = new StackPane(editorWorkspace, loadingScreen);
        root.getStyleClass().add(EditorStyleClasses.EDITOR_ROOT);
        loadingScreen.phaseStarted(EditorLoadingPhase.PREPARING_VIEWPORT);

        stage.setTitle("JScene3D Editor");
        stage.setScene(createScene(root));
        stage.setMinWidth(800.0);
        stage.setMinHeight(520.0);
        stage.setOnCloseRequest(event -> {
            event.consume();
            requestClose();
        });
        stage.show();
        Platform.runLater(editorWorkspace::applyInitialDividerPositions);
        loadingScreen.markDisplayed();
        loadCommandLineProject();
        viewportCanvas.requestFocus();
        scheduleAutomaticClose();
    }

    /** Releases the OpenGLFX peer if JavaFX stops without an ordinary close request. */
    @Override
    public void stop() {
        closeProjectOpener();
        closeWorkspace();
        extensionHost.close();
        configurationContext.close();
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
            requireProjectOpener().openProject(directory.toPath());
        }
    }

    /** Loads the optional first unnamed command-line argument as a project directory. */
    private void loadCommandLineProject() {
        if (!getParameters().getUnnamed().isEmpty()) {
            requireProjectOpener()
                    .openProject(Path.of(getParameters().getUnnamed().getFirst()));
        }
    }

    /** Dismisses startup branding after the empty viewport presents when no project was requested. */
    private static void finishStartupSplash(EditorSplashScreen loadingScreen, boolean startupProjectRequested) {
        if (!startupProjectRequested) {
            loadingScreen.finish();
        }
    }

    /** Returns the project opener installed during JavaFX stage initialization. */
    private EditorProjectOpener requireProjectOpener() {
        EditorProjectOpener current = projectOpener;
        if (current == null) {
            throw new IllegalStateException("editor project opener has not been initialized");
        }
        return current;
    }

    /** Stops project loading after the project opener has been installed. */
    private void closeProjectOpener() {
        EditorProjectOpener current = projectOpener;
        if (current != null) {
            current.close();
        }
    }

    /** Detaches workbench observers before bundled extensions are deactivated. */
    private void closeWorkspace() {
        EditorWorkspace current = workspace;
        workspace = null;
        if (current != null) {
            current.close();
        }
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
        closeProjectOpener();
        currentCanvas.dispose();
    }

    /** Requests a close through the dirty-resource guard before renderer disposal begins. */
    private void requestClose() {
        EditorWorkspace currentWorkspace = workspace;
        if (currentWorkspace == null || currentWorkspace.prepareToClose()) {
            disposeCanvas();
        }
    }

    /** Closes JavaFX only after the render thread releases the renderer and surface. */
    private void completeDisposal(Stage stage) {
        canvas = null;
        closeWorkspace();
        projectOpener = null;
        stage.setOnCloseRequest(null);
        stage.close();
        Platform.exit();
    }
}
