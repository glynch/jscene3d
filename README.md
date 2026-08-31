# JScene3D

JScene3D is a Java 21 scene-graph and rendering library inspired by Three.js.

## Requirements

- A Java 21 or newer JDK.
- macOS ARM64 is currently the sole Verified Platform.

The checked-in Maven Wrapper downloads the required Maven version. No system
Maven installation is required.

## Verify

Run the complete ordinary, headless verification lifecycle from a clean state:

```shell
./mvnw clean verify
```

OpenGL integration tests are isolated behind a separate profile:

```shell
./mvnw clean verify -Prender-integration
```

That profile creates hidden native windows and OpenGL contexts. On macOS it
must run in a session with access to the WindowServer.

To manually verify a visible, resizable blue window, run:

```shell
./mvnw clean verify -pl jscene3d-lwjgl -am -Prun-window-smoke
```

Close the window normally or press Escape to finish the build. This is an
internal development smoke test; applications do not receive raw OpenGL access.

## Run examples

Build and run the visible basic-triangle example with:

```shell
./mvnw clean verify -pl jscene3d-examples -am -Prun-example \
    -Djscene3d.exampleMainClass=io.github.glynch.jscene3d.examples.BasicTriangleExample
```

`TransformsExample`, `HierarchyExample`, `CamerasExample`,
`BufferGeometryExample`, `TexturedCubeExample`, `TransparencyExample`, and
`OrbitControlsExample` are also available using the same command. In
`OrbitControlsExample`, drag with the left mouse button to
orbit; drag with the right mouse button or Shift-left to pan; and use the middle
mouse button or scroll wheel to dolly. Arrow keys pan, while Shift-arrow rotates.
The example also demonstrates the optional themed control panel and FPS monitor
from `jscene3d-gui`; interacting with the panel does not move the camera.

On macOS, the OS-activated Maven profile launches the new JVM with
`-XstartOnFirstThread`. Other platforms use the same command without that JVM
option. The examples artifact is never deployed.

## Transparency

Setting a material's opacity does not implicitly enable blending; call
`setTransparent(true)` when the material should enter the transparent render
list. Transparent objects are sorted back-to-front by their object origins in
camera space, with scene traversal order providing a stable tie-breaker.

This object-level sort cannot correctly resolve intersecting transparent meshes
or triangles that overlap within one transparent mesh. Applications commonly
disable depth writes for blended surfaces with `setDepthWriteEnabled(false)`,
as demonstrated by `TransparencyExample`; the choice remains explicit because
some effects require depth writes.

## Project structure

- `jscene3d-core`: renderer-independent scene, camera, geometry, material, and
  texture descriptions.
- `jscene3d-lwjgl`: the OpenGL renderer, GLFW platform integration, controls,
  and the renderer-owned safe overlay canvas.
- `jscene3d-gui`: optional themed controls and monitors with bundled TrueType
  text rendering.
- `jscene3d-examples`: unpublished runnable examples depending on the public
  artifacts.

See `THREEJS_JAVA_ARCHITECTURE_BLUEPRINT.md`, `CODING_STANDARDS.md`, and
`CONTEXT.md` for the accepted version 0.1 design and terminology.
