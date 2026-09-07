/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import com.huskerdev.grapl.gl.GLProfile;
import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.canvas.events.GLDisposeEvent;
import com.huskerdev.openglfx.canvas.events.GLRenderEvent;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
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

/**
 * Throwaway application proving whether OpenGLFX can host the actual JScene3D renderer in JavaFX.
 *
 * <p>Question: can the bridge preserve rendering, resize, DPI, focus, input, and disposal while
 * ordinary JavaFX controls surround the viewport?
 */
public final class JavaFxOpenGlViewportPrototype extends Application {
    private static final System.Logger LOGGER = System.getLogger(JavaFxOpenGlViewportPrototype.class.getName());

    private final PrototypeInteraction interaction = new PrototypeInteraction();

    private @Nullable GLCanvas canvas;
    private @Nullable ViewportController controller;
    private double previousPointerX;
    private double previousPointerY;
    private boolean disposalRequested;

    /** Starts JavaFX through its ordinary application launcher. */
    public static void main(String[] arguments) {
        launch(arguments);
    }

    /** Constructs the prototype shell and installs the OpenGLFX viewport. */
    @Override
    public void start(Stage stage) {
        Label status = new Label("Waiting for the first OpenGL frame");
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(6.0, 10.0, 6.0, 10.0));

        GLCanvas viewportCanvas = createCanvas();
        ViewportController viewportController =
                new ViewportController(viewportCanvas, interaction, status, () -> completeDisposal(stage));
        canvas = viewportCanvas;
        controller = viewportController;
        installViewportEvents(viewportCanvas, viewportController);

        BorderPane root = new BorderPane();
        root.setTop(createToolbar(viewportCanvas));
        root.setLeft(createHierarchy());
        root.setCenter(createViewportPane(viewportCanvas));
        root.setRight(createInspector());
        root.setBottom(status);
        root.setStyle("-fx-base: #252a30; -fx-background-color: #1b2026;");

        stage.setTitle("JScene3D JavaFX/OpenGL Prototype — THROWAWAY");
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

    /** Creates a core-profile OpenGLFX canvas configured for the renderer's OpenGL baseline. */
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

    /** Installs rendering, focus, keyboard, and pointer callbacks on the viewport node. */
    private void installViewportEvents(GLCanvas viewportCanvas, ViewportController viewportController) {
        viewportCanvas.addOnRenderEvent(viewportController::render);
        viewportCanvas.addOnDisposeEvent(viewportController::dispose);
        viewportCanvas
                .focusedProperty()
                .addListener((ignored, oldValue, focused) -> viewportController.setFocused(focused));
        viewportCanvas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                interaction.setSpinning(!interaction.isSpinning());
                viewportController.synchronizeAnimationControl();
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

    /** Creates ordinary JavaFX toolbar controls that drive the viewport state. */
    private ToolBar createToolbar(GLCanvas viewportCanvas) {
        Button reset = new Button("Reset orientation");
        reset.setOnAction(ignored -> {
            interaction.requestReset();
            viewportCanvas.requestFocus();
        });
        CheckBox animate = new CheckBox("Animate");
        animate.setSelected(interaction.isSpinning());
        animate.selectedProperty().addListener((ignored, oldValue, selected) -> interaction.setSpinning(selected));
        Slider speed = new Slider(0.0, 2.0, interaction.rotationSpeed());
        speed.setPrefWidth(160.0);
        speed.valueProperty()
                .addListener((ignored, oldValue, value) -> interaction.setRotationSpeed(value.floatValue()));
        controllerOrThrow().setAnimationControl(animate);
        return new ToolBar(reset, new Separator(), animate, new Label("Rotation speed"), speed);
    }

    /** Creates a representative JavaFX hierarchy control beside the rendered viewport. */
    private static TreeView<String> createHierarchy() {
        TreeItem<String> world = new TreeItem<>("Prototype World");
        TreeItem<String> model = new TreeItem<>("Asymmetric Model");
        model.getChildren().add(new TreeItem<>("Green Base"));
        model.getChildren().add(new TreeItem<>("Cyan Beacon"));
        model.getChildren().add(new TreeItem<>("Orange Marker"));
        world.getChildren().add(model);
        world.getChildren().add(new TreeItem<>("Perspective Camera"));
        world.setExpanded(true);
        model.setExpanded(true);
        TreeView<String> hierarchy = new TreeView<>(world);
        hierarchy.setPrefWidth(230.0);
        return hierarchy;
    }

    /** Wraps the OpenGLFX node in an ordinary resizable JavaFX layout pane. */
    private static StackPane createViewportPane(GLCanvas viewportCanvas) {
        StackPane viewport = new StackPane(viewportCanvas);
        viewport.setPadding(new Insets(1.0));
        viewport.setStyle("-fx-background-color: #59636e;");
        return viewport;
    }

    /** Creates a representative property inspector entirely from JavaFX controls. */
    private static VBox createInspector() {
        Label heading = new Label("Viewport inspector");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox inspector = new VBox(
                10.0,
                heading,
                new Separator(),
                new Label("Renderer\nJScene3D Renderer"),
                new Label("Bridge\nOpenGLFX GLCanvas"),
                new Label("Context\nOpenGL 3.3 Core"),
                new Label("Input\nSpace: pause/resume\nDrag: rotate\nClick: focus"));
        inspector.setPadding(new Insets(12.0));
        inspector.setPrefWidth(250.0);
        VBox.setVgrow(inspector, Priority.ALWAYS);
        return inspector;
    }

    /** Optionally closes automated smoke runs while leaving ordinary launches interactive. */
    private void scheduleAutomaticClose() {
        int seconds = Integer.getInteger("jscene3d.prototype.autoCloseSeconds", 0);
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

    /** Closes JavaFX only after the render thread has released the renderer and its context. */
    private void completeDisposal(Stage stage) {
        canvas = null;
        stage.setOnCloseRequest(null);
        stage.close();
        Platform.exit();
    }

    /** Returns the controller after startup has created it. */
    private ViewportController controllerOrThrow() {
        return Objects.requireNonNull(controller, "Viewport controller has not been created");
    }

    /** Coordinates OpenGLFX callbacks, JScene3D lifecycle, and visible prototype diagnostics. */
    private static final class ViewportController {
        private final GLCanvas canvas;
        private final PrototypeInteraction interaction;
        private final Label status;
        private final Runnable disposalComplete;
        private final OpenGlFxRendererContext context = new OpenGlFxRendererContext();

        private @Nullable PrototypeViewport viewport;
        private @Nullable CheckBox animationControl;
        private boolean focused;
        private boolean disposed;

        /** Stores the JavaFX controls and mutable state used by rendering callbacks. */
        private ViewportController(
                GLCanvas canvas, PrototypeInteraction interaction, Label status, Runnable disposalComplete) {
            this.canvas = canvas;
            this.interaction = interaction;
            this.status = status;
            this.disposalComplete = disposalComplete;
        }

        /** Renders one OpenGLFX frame through the actual JScene3D renderer. */
        private void render(GLRenderEvent event) {
            try {
                context.beginFrame(canvas, event);
                PrototypeViewport currentViewport = viewport;
                if (currentViewport == null) {
                    currentViewport = new PrototypeViewport(context);
                    viewport = currentViewport;
                }
                float elapsedSeconds = (float) Math.clamp(event.delta, 0.0, 0.1);
                currentViewport.render(elapsedSeconds, event.width, event.height, interaction.consume());
                if (currentViewport.frameCount() % 30L == 0L) {
                    updateStatus(event, currentViewport.frameCount());
                }
            } catch (RuntimeException exception) {
                canvas.setFps(0.0);
                LOGGER.log(System.Logger.Level.ERROR, "JavaFX/OpenGL prototype rendering failed", exception);
                updateStatus("Rendering failed: " + exception.getMessage());
            }
        }

        /** Releases renderer resources from OpenGLFX's context-owning disposal callback. */
        private void dispose(GLDisposeEvent ignored) {
            if (disposed) {
                return;
            }
            PrototypeViewport currentViewport = viewport;
            if (currentViewport != null) {
                currentViewport.close();
            }
            disposed = true;
            LOGGER.log(System.Logger.Level.INFO, "Disposed JavaFX/OpenGL viewport and JScene3D renderer");
            Platform.runLater(disposalComplete);
        }

        /** Stores the toolbar animation control after the surrounding shell creates it. */
        private void setAnimationControl(CheckBox animationControl) {
            this.animationControl = animationControl;
        }

        /** Reflects a Space-key toggle in the ordinary JavaFX toolbar control. */
        private void synchronizeAnimationControl() {
            CheckBox currentControl = animationControl;
            if (currentControl != null) {
                currentControl.setSelected(interaction.isSpinning());
            }
        }

        /** Records JavaFX focus changes for the next visible status update. */
        private void setFocused(boolean focused) {
            this.focused = focused;
        }

        /** Formats physical, logical, frame-rate, focus, and frame-count diagnostics. */
        private void updateStatus(GLRenderEvent event, long frameCount) {
            String text = "JScene3D frames: "
                    + frameCount
                    + "   JavaFX: "
                    + Math.round(canvas.getWidth())
                    + "×"
                    + Math.round(canvas.getHeight())
                    + " logical   OpenGL: "
                    + event.width
                    + "×"
                    + event.height
                    + " pixels   FPS: "
                    + event.fps
                    + "   Focus: "
                    + (focused ? "viewport" : "other control");
            updateStatus(text);
        }

        /** Applies a status update on the JavaFX Application Thread. */
        private void updateStatus(String text) {
            if (Platform.isFxApplicationThread()) {
                status.setText(text);
            } else {
                Platform.runLater(() -> status.setText(text));
            }
        }
    }
}
