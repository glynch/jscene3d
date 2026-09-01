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

Open the searchable native example browser with:

```shell
./tools/scripts/run-example.sh ExampleBrowser
```

The browser keeps one native window and renderer alive while examples are
selected. Its left sidebar provides captured thumbnails, category and tag
search, scrolling, and persistent selection; the right content area hosts the
fully interactive example. Switching cards closes the previous example's GPU
resources before creating the replacement. The examples continue to run as
independent applications through the same lifecycle. Use Up and Down to move
through filtered results, Page Up and Page Down to move by a visible page, and
Home or End to reach either boundary. Clicking the rendered example returns
keyboard control to that example; clicking the gallery or search field returns
it to the browser. For example, launch the Solar System Viewer directly with:

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

Maintainers can regenerate every browser thumbnail from the examples' own
OpenGL framebuffers with `./tools/scripts/capture-example-thumbnails.sh`. The
capture process creates one hidden shared window, renders every catalogued
scene without its large control overlays, and replaces the checked-in PNGs.

`BasicTriangleExample`, `TransformsExample`, `HierarchyExample`,
`CamerasExample`, `BufferGeometryExample`, `TexturedCubeExample`,
`TextureTransformsExample`, `TransparencyExample`, `ShaderMaterialExample`, `LightingExample`,
`LineRenderingExample`, `HelpersExample`, `BoxHelperExample`,
`GeneratedGeometriesExample`, `MaterialsExample`, `StandardMaterialExample`,
`GltfLoadingExample`,
`SpotAndHemisphereLightsExample`, `OrbitControlsExample`, and
`ObjectSelectionExample` are also
available using the same command. `LineRenderingExample` demonstrates a
connected `Line`, indexed `LineSegments`, vertex colors, transforms, and orbit
controls. In
`OrbitControlsExample`, drag with the left mouse button to orbit; drag with the
right mouse button or Shift-left to pan; and use the middle mouse button or
scroll wheel to dolly. Arrow keys pan, while Shift-arrow rotates. The example
also demonstrates the optional themed control panel and FPS monitor from
`jscene3d-gui`; interacting with the panel does not move the camera.
`ObjectSelectionExample` casts a ray from each unclaimed primary-button press,
highlights the nearest intersected mesh, and reports the selection in a
read-only control-panel row.
`TextureTransformsExample` displays an asymmetric generated pattern and provides
live controls for offset, repeat, rotation, rotation center, and horizontal and
vertical wrapping.

## glTF loading

The optional `jscene3d-gltf` artifact loads glTF 2.0 JSON and binary GLB files
without requiring LWJGL, OpenGL, or a graphics context:

```java
try (LoadedGltf loaded = GltfLoader.load(Path.of("scene.glb"))) {
    Scene scene = loaded.scene();
    // Render or process the scene while the loaded owner remains open.
}
```

The initial capability profile supports static selected-scene hierarchies, TRS
and decomposable matrix transforms, triangle primitives, indices, positions,
normals, primary texture coordinates, RGB/RGBA vertex colours,
metallic-roughness materials, PNG/JPEG images, alpha modes, double-sided
materials, and core sampler state. Imported textures retain glTF's top-left
texture-coordinate convention rather than rewriting geometry UVs. Unsupported
required extensions, animation,
skinning, morph targets, embedded cameras, secondary texture-coordinate
selection, and non-triangle primitives fail with a source-aware diagnostic.
JglTF performs container and reference parsing internally; no JglTF type is
part of the public JScene3D API.

On macOS, the OS-activated Maven profile launches the new JVM with
`-XstartOnFirstThread`. Other platforms use the same runner without that JVM
option. The examples artifact is never deployed. Run `./mvnw clean verify`
separately when the project needs complete verification.

## Transparency

Setting a material's opacity does not implicitly enable blending. Select
`AlphaMode.BLEND` when the material should enter the transparent render list,
or `AlphaMode.MASK` with an alpha cutoff for binary cutout surfaces.
`setTransparent(true)` remains a convenience alias for blend mode. Transparent
objects are sorted back-to-front by their object origins in camera space, with
scene traversal order providing a stable tie-breaker.

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

## Texture transforms

`Texture` provides scalar and `Vector2fc` setters for texture-coordinate offset,
repeat, and rotation center, plus a radians-based rotation setter. Corresponding
copy-out methods never expose mutable internal vectors or matrices. The cached
homogeneous transform is applied automatically by `BasicMaterial` and
`LambertMaterial`, `PhongMaterial`, and every `StandardMaterial` texture role
without modifying geometry UV attributes. `TextureCoordinateOrigin` explicitly
selects bottom-left or top-left UV orientation; ordinary textures default to
bottom-left, while the glTF loader marks imported images as top-left.

Image, sampler, and transform changes have independent versions. Updating a
texture transform therefore uploads only a small matrix uniform during a draw;
it does not upload pixels, regenerate mipmaps, or reapply OpenGL sampler state.
Repeats outside the unit interval tile only with `TextureWrap.REPEAT` or
`TextureWrap.MIRRORED_REPEAT`. Custom `ShaderMaterial` programs remain
responsible for any texture-coordinate transforms they require.

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

`LambertMaterial` provides diffuse lighting, while `PhongMaterial` adds
Blinn-Phong specular highlights plus independent emissive color and intensity.
`StandardMaterial` provides metallic-roughness physically based direct lighting,
with base-color, metallic-roughness, normal, occlusion, and emissive texture
roles. These lit materials respond to visible `AmbientLight`, `PointLight`,
`DirectionalLight`, `SpotLight`, and `HemisphereLight` scene nodes, support base
color and vertex colors, and require geometry normals. Every built-in texture
role applies its texture's coordinate transform. A non-emissive lit surface
renders black when the scene has no lights because the renderer does not supply
a hidden default light. `NormalMaterial` is an unlit diagnostic material that
maps transformed view-space normals to RGB and therefore also requires geometry
normals.

`MaterialsExample` compares Basic, Lambert, Normal, and Phong spheres side by
side. Its control panel changes Phong shininess and emissive intensity live.
`StandardMaterialExample` presents a metalness-by-roughness grid under direct
lighting. `EnvironmentLightingExample` presents the same material dimensions
under HDR image-based lighting, while `AvocadoModelExample` combines the glTF
loader, a realistic CC0 model, environment lighting, and ACES filmic tone
mapping. The browser displays source and licence metadata for bundled
third-party assets; complete notices are collected in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
`SpotAndHemisphereLightsExample` demonstrates the two directional-area light
models with live controls for intensity, range, cone angle, penumbra, and decay.

Point lights use their inherited world position. Their non-negative `decay`
controls distance falloff, while a positive `distance` adds a smooth cutoff;
zero distance leaves their range unlimited. Version 0.1 supports at most
`Renderer.MAX_POINT_LIGHTS` visible point lights per scene and reports an error
instead of silently dropping excess lights. Directional lights use their world
position and a copied world-space target point to establish parallel incoming
illumination; their distance from the target does not affect intensity. Version
0.1 supports at most `Renderer.MAX_DIRECTIONAL_LIGHTS` visible directional
lights per scene. Spotlights illuminate toward a copied world-space target and
provide distance, decay, cone-angle, and penumbra controls. Hemisphere lights
blend their sky and ground colors using the surface normal and the light's
world-space direction. The corresponding limits are
`Renderer.MAX_SPOT_LIGHTS` and `Renderer.MAX_HEMISPHERE_LIGHTS`. Shadows are
not yet supported.

## Environment lighting

`EnvironmentMapLoader` decodes Radiance HDR equirectangular images into
application-owned `EnvironmentMap` values. Assign a map to a scene with
`setEnvironment` to illuminate `StandardMaterial` objects, and independently
use `setBackgroundEnvironment` when the same or another map should be visible.
Scene-level intensity and rotation apply to all environment-lit materials;
each `StandardMaterial` also has its own environment-intensity multiplier.

The renderer derives and caches diffuse irradiance, GGX-prefiltered reflection
levels, and a split-sum BRDF lookup. Enable high-dynamic-range output explicitly
with `renderer.setToneMapping(ToneMapping.ACES_FILMIC)` and adjust exposure with
`setExposure`. Tone mapping defaults to `NONE`, preserving the established
linear-sRGB rendering path for applications that do not request HDR output.
Close an `EnvironmentMap` only after every scene using it has stopped
rendering.

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

`CircleGeometry`, `CylinderGeometry`, `ConeGeometry`, and `TorusGeometry`
create indexed positions, normals, texture coordinates, and bounds. Their
common overloads supply practical segment defaults, while configurable
overloads support tessellation, angular extents, and open-ended cylinders and
cones. `GeneratedGeometriesExample` displays all four with Lambert lighting and
orbit controls.

## Raycasting

`Raycaster` creates normalized world-space rays explicitly or from perspective
and orthographic camera coordinates. It intersects visible triangle meshes on
the CPU, returning immutable `RaycastHit` values ordered nearest-first. Mesh
queries respect hierarchy transforms, material visibility and side selection,
indexed or non-indexed draw ranges, and optional texture coordinates. Cached
bounding spheres and supplied bounding boxes provide broad-phase rejection
before individual triangles are tested.

Line picking is not included in version 0.1 because it requires an explicit
world- or screen-space distance tolerance rather than exact triangle
intersection behavior.

## Project structure

- `jscene3d-core`: renderer-independent scene, camera, geometry, material,
  texture, and raycasting APIs.
- `jscene3d-lwjgl`: the OpenGL renderer, GLFW platform integration, controls,
  and the renderer-owned safe overlay canvas.
- `jscene3d-gui`: optional themed controls and monitors with bundled TrueType
  text rendering.
- `jscene3d-gltf`: optional, renderer-independent glTF 2.0 and GLB loading.
- `jscene3d-examples`: unpublished runnable examples depending on the public
  artifacts.

See `THREEJS_JAVA_ARCHITECTURE_BLUEPRINT.md`, `CODING_STANDARDS.md`, and
`CONTEXT.md` for the accepted version 0.1 design and terminology.
