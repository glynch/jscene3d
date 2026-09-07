# JavaFX/OpenGL viewport prototype

> **THROWAWAY PROTOTYPE:** this code exists only on the
> `prototype/javafx-opengl-viewport` branch and is not a production editor.

Question: can an OpenGLFX `GLCanvas` host the actual JScene3D renderer inside a
JavaFX interface with usable resizing, high-DPI dimensions, focus, input, and
resource disposal?

Run the prototype from the repository root:

```shell
./mvnw -pl :jscene3d-javafx-opengl-viewport-prototype -am -Prun-javafx-opengl-prototype package
```

Build the self-contained macOS application image:

```shell
./mvnw -pl :jscene3d-javafx-opengl-viewport-prototype -am -Ppackage-javafx-opengl-prototype clean verify
```

The application is written to
`target/prototype-app-image/JScene3D JavaFX OpenGL Prototype.app` beneath this
prototype module. It contains a linked Java runtime and launches the prototype
as a named JPMS module; no separately installed JDK or Maven process is used.

The interface deliberately uses ordinary JavaFX controls around the viewport.
Inside the viewport, the JScene3D renderer draws three boxes. Verify that:

1. the boxes remain visible while resizing the window and moving it between
   displays with different scaling;
2. the logical and framebuffer dimensions reported in the status bar change
   appropriately;
3. clicking the viewport gives it keyboard focus;
4. Space pauses and resumes rotation, dragging rotates the scene, and Reset
   restores its orientation; and
5. closing the window logs one renderer-disposal message without a native
   crash.

The prototype intentionally introduces no supported editor or renderer API.
