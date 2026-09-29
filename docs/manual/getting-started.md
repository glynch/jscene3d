# Getting started

This guide covers the repository-level build and native smoke-test workflow.
For a first rendering program, continue with
[Rendering fundamentals](fundamentals.md). To explore complete runnable code,
see [Examples](examples.md).

## Requirements

- A Java 21 or newer JDK.
- A shell capable of running the checked-in `mvnw` script.
- A graphical desktop session for tests or examples that create native windows.

macOS ARM64 is currently the sole Verified Platform. The build selects LWJGL
native libraries for macOS ARM64 and x64, Linux x64, and Windows x64, but those
additional targets are not currently maintained to the same verified standard.

## Use the Maven Wrapper

Run build commands from the repository root with the checked-in Maven Wrapper:

```shell
./mvnw --version
```

The wrapper downloads the Maven version selected by the repository, so a
separate system Maven installation is not required. It still uses the active
JDK, which should report Java 21 or newer.

## Ordinary verification

Run the complete ordinary, headless acceptance lifecycle from a clean state:

```shell
./mvnw clean verify
```

This builds the Maven reactor, runs its tests, Javadocs, formatting and static
analysis, coverage gates, and Markdown validation. The build provisions its
pinned Markdown toolchain; a global Node.js or `markdownlint-cli2`
installation is not required.

Use module-focused Maven commands while iterating. Reserve the root command for
an acceptance check because it cleans and verifies the complete reactor.

## Render-integration verification

OpenGL integration tests are isolated from the ordinary headless build:

```shell
./mvnw clean verify -Prender-integration
```

This profile creates hidden native windows and OpenGL contexts and enforces the
render-integration coverage gate. On macOS, run it from a logged-in graphical
session with access to the WindowServer. It is not suitable for a strictly
headless host without an appropriate display environment.

## Native window smoke test

To exercise the public desktop window lifecycle manually, run:

```shell
./mvnw clean verify -pl jscene3d-lwjgl -am -Prun-window-smoke
```

The build opens a visible, resizable 960 by 540 window titled
`JScene3D Window Smoke Test` and clears it dark blue. Resize it, then close it
normally or press Escape. The Maven command finishes only after the window has
closed.

This is a development smoke test for `Window`; application code does not
receive raw OpenGL access. The renderer-facing lifecycle is introduced in
[Rendering fundamentals](fundamentals.md).

## Next steps

- Follow [Rendering fundamentals](fundamentals.md) to build a rotating cube.
- Browse the runnable [examples](examples.md).
- Learn the authored project layer in
  [Project and game fundamentals](project-fundamentals.md).
- Learn the Code OSS authoring workflow in
  [Editor fundamentals](editor-fundamentals.md).
