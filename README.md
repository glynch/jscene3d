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

## Run an example

Once a public feature example exists, build its dependencies and run it with:

```shell
./mvnw -pl jscene3d-examples -am verify -Prun-example \
    -Djscene3d.exampleMainClass=io.github.glynch.jscene3d.examples.ExampleName
```

On macOS, the OS-activated Maven profile launches the new JVM with
`-XstartOnFirstThread`. Other platforms use the same command without that JVM
option. The examples artifact is never deployed.

## Project structure

- `jscene3d-core`: renderer-independent scene, camera, geometry, material, and
  texture descriptions.
- `jscene3d-lwjgl`: the OpenGL renderer, GLFW platform integration, and STB
  texture loading.
- `jscene3d-examples`: unpublished runnable examples depending on both public
  artifacts.

See `THREEJS_JAVA_ARCHITECTURE_BLUEPRINT.md`, `CODING_STANDARDS.md`, and
`CONTEXT.md` for the accepted version 0.1 design and terminology.
