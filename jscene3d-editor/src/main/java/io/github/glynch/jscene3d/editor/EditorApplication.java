/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.grapl.gl.GLProfile;
import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jspecify.annotations.Nullable;

/** Production JavaFX shell for the JScene3D visual editor. */
public final class EditorApplication extends Application {
    private final ViewportInteraction interaction;

    private @Nullable GLCanvas canvas;
    private double previousPointerX;
    private double previousPointerY;
    private boolean disposalRequested;

    /** Creates an application instance whose stage is initialized later by JavaFX. */
    public EditorApplication() {
        interaction = new ViewportInteraction();
    }

    /** Constructs the editor shell and installs its OpenGLFX viewport. */
    @Override
    public void start(Stage stage) {
        Label status = createStatus();
        GLCanvas viewportCanvas = createCanvas();
        ViewportController controller =
                new ViewportController(viewportCanvas, interaction, status, () -> completeDisposal(stage));
        canvas = viewportCanvas;
        installViewportEvents(viewportCanvas, controller);

        BorderPane root = new BorderPane();
        root.setTop(createToolbar(viewportCanvas, controller));
        root.setLeft(createNavigation());
        root.setCenter(createViewportPane(viewportCanvas));
        root.setRight(createInspector());
        root.setBottom(status);
        root.setStyle("-fx-base: #252a30; -fx-background-color: #1b2026;");

        stage.setTitle("JScene3D Editor");
        stage.setScene(new Scene(root, 1280.0, 780.0));
        stage.setMinWidth(800.0);
        stage.setMinHeight(520.0);
        stage.setOnCloseRequest(event -> {
            event.consume();
            disposeCanvas();
        });
        stage.show();
        viewportCanvas.requestFocus();
        scheduleAutomaticClose();
    }

    /** Releases the OpenGLFX peer if JavaFX stops without an ordinary close request. */
    @Override
    public void stop() {
        disposeCanvas();
    }

    /** Creates the status line shown below the viewport. */
    private static Label createStatus() {
        Label status = new Label("Waiting for the first OpenGL frame");
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
                .setFlipY(true)
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

    /** Installs rendering, focus, keyboard, and pointer callbacks on the viewport. */
    private void installViewportEvents(GLCanvas viewportCanvas, ViewportController controller) {
        viewportCanvas.addOnRenderEvent(controller::render);
        viewportCanvas.addOnDisposeEvent(ignored -> controller.dispose());
        viewportCanvas.focusedProperty().addListener((ignored, oldValue, focused) -> controller.setFocused(focused));
        viewportCanvas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                interaction.setSpinning(!interaction.isSpinning());
                controller.synchronizeAnimationControl();
                event.consume();
            }
        });
        viewportCanvas.setOnMousePressed(event -> {
            viewportCanvas.requestFocus();
            previousPointerX = event.getX();
            previousPointerY = event.getY();
        });
        viewportCanvas.setOnMouseDragged(event -> {
            interaction.drag((float) (event.getX() - previousPointerX), (float) (event.getY() - previousPointerY));
            previousPointerX = event.getX();
            previousPointerY = event.getY();
        });
    }

    /** Creates ordinary JavaFX toolbar controls that drive the temporary preview. */
    private ToolBar createToolbar(GLCanvas viewportCanvas, ViewportController controller) {
        Button reset = new Button("Reset orientation");
        reset.setOnAction(ignored -> {
            interaction.requestReset();
            viewportCanvas.requestFocus();
        });
        CheckBox animate = new CheckBox("Animate preview");
        animate.setSelected(interaction.isSpinning());
        animate.selectedProperty().addListener((ignored, oldValue, selected) -> interaction.setSpinning(selected));
        Slider speed = new Slider(0.0, 2.0, interaction.rotationSpeed());
        speed.setPrefWidth(160.0);
        speed.valueProperty()
                .addListener((ignored, oldValue, value) -> interaction.setRotationSpeed(value.floatValue()));
        controller.setAnimationControl(animate);
        return new ToolBar(reset, new Separator(), animate, new Label("Rotation speed"), speed);
    }

    /** Creates placeholder hierarchy and asset-browser regions for later editor slices. */
    private static SplitPane createNavigation() {
        SplitPane navigation = new SplitPane(createHierarchy(), createAssetBrowser());
        navigation.setOrientation(Orientation.VERTICAL);
        navigation.setDividerPositions(0.58);
        navigation.setPrefWidth(250.0);
        return navigation;
    }

    /** Creates a hierarchy that explicitly represents only the temporary preview scene. */
    private static VBox createHierarchy() {
        TreeItem<String> preview = new TreeItem<>("Preview scene");
        preview.getChildren().add(new TreeItem<>("Three-box model"));
        preview.getChildren().add(new TreeItem<>("Perspective camera"));
        preview.setExpanded(true);
        TreeView<String> hierarchy = new TreeView<>(preview);
        VBox.setVgrow(hierarchy, Priority.ALWAYS);
        return new VBox(6.0, new Label("Hierarchy"), hierarchy);
    }

    /** Creates an asset-browser placeholder without inventing an editor document model. */
    private static VBox createAssetBrowser() {
        ListView<String> assets = new ListView<>();
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
                new Label("Viewport controls\nSpace: pause or resume\nDrag: rotate preview"));
        inspector.setPadding(new Insets(12.0));
        inspector.setPrefWidth(250.0);
        VBox.setVgrow(inspector, Priority.ALWAYS);
        return inspector;
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
        PauseTransition closeDelay = new PauseTransition(Duration.seconds(seconds));
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
        currentCanvas.dispose();
    }

    /** Closes JavaFX only after the render thread releases the renderer and surface. */
    private void completeDisposal(Stage stage) {
        canvas = null;
        stage.setOnCloseRequest(null);
        stage.close();
        Platform.exit();
    }
}
