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

The root build provisions its pinned Markdown toolchain and validates every
checked-in Markdown document alongside the Java formatting and static-analysis
checks. A global Node.js or `markdownlint-cli2` installation is not required.

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

Open the separate visual physics suite with:

```shell
./tools/scripts/run-example.sh PhysicsExampleBrowser
```

Open the game-runtime suite with:

```shell
./tools/scripts/run-example.sh GameExampleBrowser
```

Open the audio suite with:

```shell
./tools/scripts/run-example.sh AudioExampleBrowser
```

All four browsers use `jscene3d-example-framework`, which keeps one native window
and renderer alive while examples are selected. Their left sidebars provide
captured thumbnails, category and tag search, scrolling, and persistent
selection; the right content area hosts the fully interactive example.
Switching cards closes the previous example's resources before creating the
replacement. Examples continue to run as independent applications through the
same lifecycle. Use Up and Down to move through filtered results, Page Up and
Page Down to move by a visible page, and Home or End to reach either boundary.
Clicking the rendered example returns keyboard control to that example;
clicking the gallery or search field returns it to the browser. For example,
launch the Solar System Viewer directly with:

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
Pass one or more catalogue IDs to capture only those examples, for example
`./tools/scripts/capture-example-thumbnails.sh shadows` or
`./tools/scripts/capture-example-thumbnails.sh shadows basic-triangle`. The
capture command compiles incrementally; use the normal verification command
when a clean build is required.

Select another suite with `--suite`. For example, refresh only the kinematic
movement thumbnail with
`./tools/scripts/capture-example-thumbnails.sh --suite physics
kinematic-movement`.

Use `--suite game` for game-runtime examples, such as
`./tools/scripts/capture-example-thumbnails.sh --suite game
first-person-sandbox third-person-sandbox`.

Use `--suite audio` for audio examples, such as
`ALSOFT_DRIVERS=null ./tools/scripts/capture-example-thumbnails.sh --suite
audio positional-audio`. The null driver is useful for silent automated
captures; omit it when interactively listening to an example.

`BasicTriangleExample`, `TransformsExample`, `HierarchyExample`,
`CamerasExample`, `BufferGeometryExample`, `TexturedCubeExample`,
`TextureTransformsExample`, `TransparencyExample`, `BillboardExample`,
`AnimatedBillboardExample`, `ShaderMaterialExample`, `LightingExample`,
`LineRenderingExample`, `HelpersExample`, `BoxHelperExample`,
`GeneratedGeometriesExample`, `MaterialsExample`, `StandardMaterialExample`,
`KeyframeAnimationExample`, `AnimationBlendingExample`, `SoldierAnimationBlendingExample`,
`GltfAnimationExample`, `GltfMorphAnimationExample`, `GltfLoadingExample`,
`SpotAndHemisphereLightsExample`, `ShadowsExample`, `FogExample`, `OrbitControlsExample`,
`ObjectSelectionExample`, `InstancingExample`, `InstanceAttributesExample`, and
`InstancedMorphTargetsExample` are also
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
    AnimationMixer mixer = new AnimationMixer();
    loaded.animations().forEach(clip -> mixer.action(clip).play());
    // Render or process the scene while the loaded owner remains open.
    // Advance the mixer explicitly once per frame with mixer.update(elapsedSeconds).
}
```

The current capability profile supports selected-scene hierarchies, TRS
and decomposable matrix transforms, triangle primitives, indices, positions,
normals, two texture-coordinate sets, RGB/RGBA vertex colours,
metallic-roughness materials, PNG/JPEG images, alpha modes, double-sided
materials, core sampler state, skeletal skinning, relative position and normal
morph targets, Draco-compressed mesh primitives, and translation, rotation,
scale, and morph-weight animation channels using step, linear, or cubic-spline
interpolation. Imported textures retain glTF's top-left texture-coordinate
convention rather than rewriting geometry UVs. Unsupported required extensions,
embedded cameras, texture-coordinate sets beyond one, and non-triangle
primitives fail with a source-aware diagnostic.
JglTF performs container and reference parsing internally; no JglTF type is
part of the public JScene3D API.

### Package the example browser

Build the standalone macOS ARM64 example-browser distribution with:

```shell
./mvnw clean verify -pl jscene3d-examples -am -Pexample-distribution-macos-arm64
```

The resulting distribution is
`jscene3d-examples/target/jscene3d-examples-0.1.0-SNAPSHOT-macos-arm64.zip`.
It contains a native `JScene3D Examples.app` application image with the examples,
assets, runtime dependencies, macOS ARM64 LWJGL native libraries, and a trimmed
Java runtime. Extract it and open the application:

```shell
unzip jscene3d-examples/target/jscene3d-examples-0.1.0-SNAPSHOT-macos-arm64.zip
open "JScene3D Examples.app"
```

The native launcher uses the included runtime and supplies
`-XstartOnFirstThread` automatically. The platform profile is structured so
future macOS x64, Windows x64, and Linux x64 distributions can provide their
own native libraries and launcher settings while sharing the shaded example
application. The macOS bundle uses a separate positive `1.0.0` launcher build
version because `jpackage` does not accept the project's pre-1.0 version as a
macOS application version; the JScene3D artifact version remains
`0.1.0-SNAPSHOT`.

## Declarative projects

The project modules load versioned manifests, extension descriptors, scenes,
resources, and import definitions without embedding Java implementation class
names in project data. Trusted runtime extensions bind registered types to Java
factories. Project resources and imported resources share the same native
resource format and runtime ownership model; imported references are resolved
by logical `import:` URI through a host-supplied artifact lookup, never through
a physical cache path.

Run the headless end-to-end example that imports a text source as a typed native
resource and resolves it from a declarative scene with:

```shell
./mvnw -pl jscene3d-project-examples -am -Prun-import-example compile
```

## WAD archives

The optional `jscene3d-wad` artifact validates IWAD and PWAD containers without
interpreting Doom maps, assets, or gameplay. Directory ordering and duplicate
lump names are preserved, and every archive records its source path, size, and
SHA-256 provenance:

```java
WadLoadResult result = WadLoader.load(Path.of("content.wad"));
WadArchive archive = result.archive().orElseThrow();
WadLump lump = archive.lastLumpNamed("NAME").orElseThrow();
byte[] content = archive.readAllBytes(lump, 1024 * 1024);
```

Large content can be consumed through `archive.openStream(lump)`, which returns an
independent caller-owned stream bounded to the validated lump range. Explicit
low-to-high precedence composition is available through `WadArchiveLayers`;
the library never discovers layers or assigns meaning to lump names itself.
WAD failures use the feature-owned `WadDiagnosticCode` enum. Its stable codes
act as localization keys, its default messages provide English fallbacks, and
variable failure values remain available as structured diagnostic details.

The optional `jscene3d-wad-import` artifact exposes WAD content through the
generic project import system. Its service-discovered
`io.github.glynch.jscene3d.wad/archive` importer reports one selectable archive
item and one selectable item per ordered lump. Selecting `archive` imports every
lump; selecting lump identities imports only those opaque payloads. Every import
also produces an `archive/index` JSON artifact containing portable source
provenance, complete directory order, duplicate-preserving names, and the
artifact identity of each imported lump. The adapter assigns no Doom-specific
meaning to lump names or content.

Run the self-contained archive and layering example with:

```shell
./mvnw -pl jscene3d-wad-examples -am -Prun-wad-example compile
```

Run the self-contained project-import example with:

```shell
./mvnw -pl jscene3d-wad-examples -am -Prun-wad-import-example compile
```

## Doom content

The optional `jscene3d-doom` artifact interprets classic Doom map data over the
generic WAD API. `DoomMapDecoder` discovers conventional `MAP##` and `E#M#`
markers and decodes things, vertices, linedefs, sidedefs, sectors, segs,
subsectors, BSP nodes, reject tables, and blockmaps into an immutable model.
Unsupported or malformed data is reported through typed `DoomDiagnosticCode`
values with stable localization keys, English fallback messages, and structured
details.

Its service-discovered `io.github.glynch.jscene3d.doom/maps` project importer
exposes the maps in a WAD as selectable source items. Each selected map produces
a deterministic, pretty-printed resource of type
`io.github.glynch.jscene3d.doom/map`. This first content slice preserves the
classic map records and source provenance; geometry, materials, sprites, audio,
and gameplay interpretation remain separate later concerns.

The same extension provides the runtime factory for that native resource type.
When a scene resolves an imported map, the factory reconstructs the immutable
renderer-independent `DoomMap`; consuming applications do not parse generated
JSON or depend on cache paths.

Run the self-contained Doom project-import example with:

```shell
./mvnw -pl jscene3d-doom-examples -am -Prun-doom-import-example compile
```

## Animation

The renderer-independent `io.github.glynch.jscene3d.animation` package provides
immutable `AnimationClip` objects, typed position, rotation, and scale tracks,
and caller-driven `AnimationMixer` playback. Keyframe arrays are copied during
construction. Track bindings retain their target `Object3D` and apply values
through controlled transform setters; they do not expose mutable JOML state.

`AnimationAction` supports explicit play, pause, stop, reset, seeking, positive
or negative time scales, contribution weights, linear fades, and once, repeat,
or ping-pong looping. Applications advance a mixer with their frame delta; the
animation package creates neither threads nor a hidden clock. The mixer resolves
concurrent tracks once per controlled property, completes partial weights with
the captured base pose, normalizes overweight blends, and aligns equivalent
quaternion signs before normalization. `AnimationMixer.crossFade` performs an
explicit source-to-destination transition and deactivates the source on
completion.

`KeyframeAnimationExample` compares step, linear, and cubic-spline interpolation
with live playback controls. `GltfAnimationExample` imports and synchronously
plays the nine transform clips in Khronos's CC0 Interpolation Test asset.
`GltfMorphAnimationExample` loads Khronos's Morph Stress Test and plays its
eight-target morph-weight animation while exposing the live target influences.
`SkeletalAnimationExample` constructs and animates a two-joint skinned mesh,
`AnimationBlendingExample` cross-fades the Khronos Fox model between idle,
walking, and running clips. `SoldierAnimationBlendingExample` follows the
Three.js skeletal-animation-blending reference with explicit activation,
single-step, cross-fade, blend-weight, and playback-speed controls. The
Soldier resource retains its Mixamo usage terms and attribution rather than
being relicensed under JScene3D. `LittlestTokyoExample` loads Glen Fox's
Draco-compressed, skeletally animated Littlest Tokyo scene.

Sprite animation uses the same caller-driven timing model without forcing
two-dimensional artwork through the general keyframe mixer. `SpriteAnimation`
stores named, timed `SpriteFrame` values and loop behavior, while
`SpriteAnimationSet` provides an immutable reusable collection. Each frame
selects a normalized `TextureRegion` from an atlas. `TextureRegion.fromPixels`
accepts the top-row-first coordinates used by image editors and future atlas
import tools.

`AnimatedBillboard` owns only its independent playback state. Multiple objects
can therefore share one atlas, material, and animation set while using
different animations, frame positions, and playback speeds. Applications
advance each object explicitly, can seek by frame and within-frame progress,
and can observe animation changes, frame changes, loops, and completion.
Animation names such as idle, attack, or fly remain application-defined rather
than engine-defined.

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

## Billboards

`Billboard` is an unlit rectangular scene object rendered through a shared
`BasicMaterial`. `BillboardAlignment.SPHERICAL` follows both camera yaw and
pitch, while `BillboardAlignment.CYLINDRICAL` turns only around the world-up
axis so character and vegetation sprites remain upright. The inherited X and Y
scale define world-space width and height. A configurable local anchor selects
which point remains at the object's world position, such as `(0.5, 0)` for a
sprite standing on the ground.

The renderer resolves the camera-facing transform per camera without mutating
the scene hierarchy, so rendering the same scene through multiple cameras does
not change application state. A billboard owns its generated unit quad but
retains its caller-owned material and textures. It is deliberately distinct
from `Mesh`: billboards are not included in mesh raycasts or shadow-map caster
passes. `BillboardExample` compares bottom-anchored cylindrical character
cutouts with centred spherical markers. `AnimatedBillboardExample` demonstrates
named atlas animations, independent playback over shared resources, live
events, and inspector-style controls. These stable runtime objects are also the
data model a future GUI animation editor will inspect and modify; the GUI will
not be required to construct or run them from Java. Billboard batching remains
a separate later feature.

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
uniforms. A custom shader may explicitly opt into renderer-managed instancing,
consume the instance transform and optional instance color, and declare up to
four application-defined floating-point per-instance inputs. The renderer
uploads only changed attribute ranges and advances those inputs once per
instance. `InstanceAttributesExample` animates a grid from custom phase, scale,
and tint inputs in one draw call. Uniform arrays, custom per-vertex attributes,
uniform blocks, and raw OpenGL access are not supported.

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
mapping. `WaterBottleModelExample` exercises base-colour, metallic-roughness,
normal, occlusion, and emissive maps on another CC0 glTF asset.
`BoomBoxModelExample` presents metallic surfaces and a glowing emissive front
panel under the same HDR lighting pipeline. The browser
displays source and licence metadata for bundled
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
supported for directional, spot, and point lights. Shadow-map generation is
opt-in on each light and each casting mesh, while receiving is enabled
independently on each mesh. Directional lights expose orthographic shadow-camera
bounds; all shadow-capable lights expose map dimensions, camera range, depth
bias, and normal bias. Lambert, Phong, and Standard materials receive filtered
shadows. A draw supports at most `Renderer.MAX_TWO_DIMENSIONAL_SHADOW_MAPS`
combined directional and spot maps plus `Renderer.MAX_POINT_SHADOW_MAPS` point
maps; excess enabled maps fail clearly. `ShadowsExample` demonstrates all three
light types and exposes these settings through live controls.

## Fog

A scene can use either `LinearFog`, with explicit near and far distances, or
`ExponentialSquaredFog`, with a density that increases the fog continuously
with view-space distance. Fog affects the built-in mesh and line materials;
custom `ShaderMaterial` shaders remain responsible for their own fog logic.
Clearing a scene's fog disables the effect without changing its background.

`FogExample` compares both fog models across every built-in mesh material and
line-rendered grid geometry. Its controls change the fog model, colour, range,
and density live. `SoldierAnimationBlendingExample` uses linear fog with a
matching background to blend its large ground plane into the distance.

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
indexed or non-indexed draw ranges, morph-target influences, instanced
transforms, and optional texture coordinates. Mesh and per-instance morph-aware
bounds are cached and invalidated by the geometry, morph attributes, or
influences that affect them, providing exact broad-phase rejection before
individual triangles are tested.

Line picking is not included in version 0.1 because it requires an explicit
world- or screen-space distance tolerance rather than exact triangle
intersection behavior.

## Physics movement

`PhysicsWorld` owns collision objects. `StaticBody` represents immovable world
geometry, `KinematicBody` is moved explicitly by its caller, and
`CollisionSensor` reports non-blocking overlaps. Each object owns one or more
`Collider` instances with local transforms, so compound collision shapes move
as a unit. `PhysicsWorld.move(...)` resolves a registered kinematic body's
desired translation against solid world geometry. `CharacterController` builds
on that primitive for common game-character behavior: the caller supplies
planar velocity once per fixed update, while the controller owns gravity,
vertical velocity, grounded state, jumping, wall sliding, and step traversal.
Both APIs return immutable movement results containing contacts and
deterministic sensor enter, stay, and exit events. Collision filtering remains
mutual, and collision sensors never block movement.

`PhysicsWorld.debugSnapshot()` exposes renderer-independent world-space line
segments for box, sphere, and capsule colliders. Applications can render that
snapshot with JScene3D lines without introducing a rendering dependency into
`jscene3d-physics`. The separately compiled character-controller example in
`jscene3d-physics-examples` demonstrates the complete seam with a caller-owned
120 Hz update, WASD movement, gravity, wall sliding, step traversal, a collision
sensor, and live debug geometry.

## Game runtime

`GameRuntime` coordinates a caller-owned `GameApplication` without creating a
window, render thread, or physics world. It separates deterministic Fixed
Updates from rendered frame updates, clamps long frames, bounds catch-up work,
buffers action transitions until simulation consumes them, and reports an
interpolation factor for smooth presentation. `InputMap` translates physical
keyboard and mouse controls into named `InputAction` values while respecting
input captured by a host interface. `PhysicsBinding` is the explicit game-layer
adapter between renderer-independent collision objects and scene objects.
`CharacterMovementController` converts configurable semantic actions and a
caller-supplied view direction into normalized camera-relative movement, jump
requests, and fixed-step character physics without depending on a particular
camera implementation.

The separate `jscene3d-game-examples` artifact demonstrates these seams with
first- and third-person sandboxes. The first-person example uses W/S or Up/Down
to move, A/D to strafe, Left/Right or captured mouse movement to turn, and Space
to jump. The third-person example uses W A S D or the arrow keys for
camera-relative movement, mouse dragging to orbit, scrolling to zoom, and Space
to jump. Both sandboxes use the same reusable movement controller and contain a
low step, solid obstacles, and enclosing walls for exercising stepping, jumping,
collision, and sliding. Run them in the game example browser or directly with:

```shell
./tools/scripts/run-example.sh FirstPersonSandboxExample
./tools/scripts/run-example.sh ThirdPersonSandboxExample
```

## Audio

The optional `jscene3d-audio` artifact provides buffered clips from Ogg Vorbis
resources or in-memory signed 16-bit PCM, independent playback sources,
three-dimensional distance attenuation,
camera-listener updates, and separate master, music, and effects gains through
an OpenAL implementation. The public interface remains focused on application
audio concepts; OpenAL handles and decoder details stay encapsulated.

The separate audio suite demonstrates a mono source orbiting the listener and
independent stereo music and interface-effect mixing. Run either example
directly with:

```shell
./tools/scripts/run-example.sh PositionalAudioExample
./tools/scripts/run-example.sh AudioMixingExample
```

All bundled example sounds and music are CC0 assets from Kenney. Their source,
license notices, and exact selected filenames are recorded beside the assets in
`jscene3d-audio-examples`.

## Project structure

- `jscene3d-core`: renderer-independent scene, camera, geometry, material,
  texture, and raycasting APIs.
- `jscene3d-physics`: renderer-independent collision objects and colliders,
  collision filtering, spatial queries, explicit kinematic movement, sensor
  overlap transitions, and debug snapshots.
- `jscene3d-lwjgl`: the OpenGL renderer, GLFW platform integration, controls,
  and the renderer-owned safe overlay canvas.
- `jscene3d-gui`: optional themed controls and monitors with bundled TrueType
  text rendering.
- `jscene3d-gltf`: optional, renderer-independent glTF 2.0 and GLB loading.
- `jscene3d-project`: versioned project manifests, extension descriptors,
  scenes, resources, import definitions, and structured validation diagnostics.
- `jscene3d-project-import`: deterministic import inspection, preparation,
  cache publication, and logical artifact access.
- `jscene3d-project-runtime`: trusted scene composition and shared runtime
  resource resolution for project and imported resources.
- `jscene3d-project-runtime-lwjgl`: built-in declarative 3d runtime types backed
  by the LWJGL renderer.
- `jscene3d-wad`: optional, renderer-independent WAD validation, provenance,
  bounded lump access, and explicit archive layering.
- `jscene3d-wad-import`: optional project-import adapter exposing WAD archives
  and opaque lumps as selectable source items and cached artifacts.
- `jscene3d-doom`: optional classic Doom map discovery, decoding, validation,
  and project import over the generic WAD capability.
- `jscene3d-game`: optional, genre-independent application lifecycle, Fixed
  Updates, semantic input actions, and interpolated Physics Bindings.
- `jscene3d-audio`: optional OpenAL-backed clips, playback sources, positional
  attenuation, listener control, and volume categories.
- `jscene3d-example-framework`: unpublished reusable native hosting, browsing,
  lifecycle, catalog, and thumbnail-capture support.
- `jscene3d-examples`: unpublished rendering and asset-loading examples.
- `jscene3d-physics-examples`: unpublished visual physics examples.
- `jscene3d-game-examples`: unpublished examples that integrate the Game
  Engine, Physics Engine, renderer, and shared browser framework.
- `jscene3d-audio-examples`: unpublished positional-audio and mixing examples
  using attributed CC0 sounds and music.
- `jscene3d-wad-examples`: unpublished headless archive, layering, and project
  import examples.
- `jscene3d-doom-examples`: unpublished headless Doom decoding and project
  import examples.

See `THREEJS_JAVA_ARCHITECTURE_BLUEPRINT.md`, `CODING_STANDARDS.md`, and
`CONTEXT.md` for the accepted version 0.1 design and terminology.
