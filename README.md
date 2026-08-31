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

Run the interactive Solar System Viewer with:

```shell
./tools/scripts/run-example.sh SolarSystemViewer
```

The viewer demonstrates the complete version 0.1 rendering path with a textured
Sun, all eight planets, Earth's Moon, Saturn's transparent rings, a star field,
Lambert lighting, scene hierarchy, orbit controls, four-times requested MSAA,
an interactive control panel, and an optional FPS monitor. Its distances, sizes,
and orbital speeds are intentionally stylized for visibility rather than being
an astronomical simulation. The bundled textures are distributed under CC BY
4.0; see their
[attribution and checksums](jscene3d-examples/src/main/resources/io/github/glynch/jscene3d/examples/solar-system/ATTRIBUTION.md).

List the available examples with `./tools/scripts/run-example.sh --list`. The
runner incrementally compiles the required artifacts and launches the selected
example without running tests or the full verification lifecycle.

`BasicTriangleExample`, `TransformsExample`, `HierarchyExample`,
`CamerasExample`, `BufferGeometryExample`, `TexturedCubeExample`,
`TransparencyExample`, `ShaderMaterialExample`, `LightingExample`,
`LineRenderingExample`, `HelpersExample`, `BoxHelperExample`, and
`OrbitControlsExample` are also
available using the same command. `LineRenderingExample` demonstrates a
connected `Line`, indexed `LineSegments`, vertex colors, transforms, and orbit
controls. In
`OrbitControlsExample`, drag with the left mouse button to orbit; drag with the
right mouse button or Shift-left to pan; and use the middle mouse button or
scroll wheel to dolly. Arrow keys pan, while Shift-arrow rotates. The example
also demonstrates the optional themed control panel and FPS monitor from
`jscene3d-gui`; interacting with the panel does not move the camera.

On macOS, the OS-activated Maven profile launches the new JVM with
`-XstartOnFirstThread`. Other platforms use the same runner without that JVM
option. The examples artifact is never deployed. Run `./mvnw clean verify`
separately when the project needs complete verification.

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

Every `Object3D` has an explicit render order, with lower values rendered first
within its opaque or transparent list. Render order does not bypass depth
testing or move objects between those lists. Materials default to
`DepthFunction.LESS_OR_EQUAL`, allowing a deliberately later coplanar object to
replace equal-depth fragments while remaining occluded by closer geometry.

## Custom shaders

`ShaderMaterial` accepts immutable OpenGL 3.3 Core vertex and fragment source,
optional preprocessor definitions, and declared standard attribute requirements.
Runtime values use typed `setUniform` overloads; JOML vectors and matrices are
copied, while textures remain shared and application-owned.

The renderer supplies any active `modelMatrix`, `viewMatrix`,
`projectionMatrix`, `modelViewMatrix`, and `normalMatrix` uniforms. Version 0.1
supports scalar, boolean, vector, matrix, `Color`, and two-dimensional `Texture`
uniforms. Uniform and attribute arrays, custom vertex attributes, uniform
blocks, and raw OpenGL access are not supported.

## Lighting

`LambertMaterial` provides diffuse lighting from visible `AmbientLight` and
`PointLight` scene nodes. Lit geometry must provide normals; color maps also
require texture coordinates. A Lambert surface renders black when the scene has
no lights because the renderer does not supply a hidden default light.

Point lights use their inherited world position. Their non-negative `decay`
controls distance falloff, while a positive `distance` adds a smooth cutoff;
zero distance leaves their range unlimited. Version 0.1 supports at most
`Renderer.MAX_POINT_LIGHTS` visible point lights per scene and reports an error
instead of silently dropping excess lights. Shadows are not yet supported.

## Lines

`Line` draws one connected strip through successive geometry elements, while
`LineSegments` draws independent pairs. Both support indexed and non-indexed
`BufferGeometry`, draw ranges, vertex colors, transforms, visibility, frustum
culling, transparency, and depth state through `LineBasicMaterial`. An odd
`LineSegments` draw-range count fails clearly because it leaves an unpaired
element. Portable line width is fixed to one framebuffer pixel.

`AxesHelper` supplies red, green, and blue positive X, Y, and Z axes.
`GridHelper` supplies a configurable XZ reference grid with optional distinct
center-line and grid colors. `BoxHelper` supplies world-axis-aligned wireframe
bounds around visible mesh and line geometry in a target subtree; call
`update()` after changing the target. All three are ordinary `LineSegments`
scene objects and own their generated geometry and material, so close the helper
itself when it is no longer needed. Their generated resources cannot be
replaced.
`HelpersExample` gives the axes a higher render order than the grid so their
coplanar X and Z segments remain distinct without altering either helper's
position or disabling depth testing.

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
