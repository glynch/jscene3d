# JScene3D

A public, general-purpose Java library for describing and rendering interactive 3D scenes through a high-level programming model.

The public Java namespace is `io.github.glynch.jscene3d`. The published Maven
coordinates are `io.github.glynch:jscene3d-core`,
`io.github.glynch:jscene3d-lwjgl`, and the optional
`io.github.glynch:jscene3d-gui`; the unpublished examples reactor artifact is
`jscene3d-examples`. The first post-0.1 loader is published separately as
`io.github.glynch:jscene3d-gltf`.

## Language

**JScene3D**:
The project and library name. `J` identifies Java, `Scene` identifies its
scene-oriented programming model, and `3D` identifies the domain.
_Avoid_: ThreeJava, LumaScene, VelaScene, Jovian3D

**Three.js-Inspired**:
Uses Three.js as a conceptual and behavioral reference while preserving Java type safety and deterministic lifecycle management. It does not promise source compatibility, complete feature parity, or synchronized releases.
_Avoid_: Three.js port, Three.js-compatible

**Library**:
The reusable public product consumed by Java application developers, rather than an educational exercise or an application-specific rendering component.
_Avoid_: Demo, teaching project, application engine

**Feature Example**:
An independently runnable, source-visible program centered on one user-facing capability or a small related group of capabilities. A user-facing feature is not release-complete until its Feature Example runs.
_Avoid_: Test, tutorial

**Integration Example**:
An independently runnable, source-visible program that validates several capabilities working together as a coherent application. It does not absorb every newly added feature.
_Avoid_: Feature Example, test

**Solar System Viewer**:
The first Integration Example, used to validate the initial scene hierarchy and rendering experience as an integrated whole.

**Resource Description**:
A renderer-independent `BufferGeometry`, `Texture`, or `Material` value that may be shared by meshes and realized by more than one renderer.
_Avoid_: GPU resource, OpenGL object

**GPU Realization**:
The context-bound GPU state that one renderer derives from a Resource Description and owns exclusively.
_Avoid_: Resource Description

**Closed Resource**:
A Resource Description whose application-owned lifetime has ended permanently. It cannot be rendered or reactivated.
Repeated closure is a no-op, and cleanup occurs at most once.
_Avoid_: Released resource, disposed generation

**Reparenting**:
Moving an `Object3D` from its current parent to a new parent while preserving its local transform; its world transform may consequently change.
_Avoid_: Attaching

**Detachment**:
Removing an `Object3D` from its parent while preserving its local transform; its world transform may consequently change.
_Avoid_: Removal from parent, unparenting

**Children**:
The ordered `Object3D` nodes directly parented by another `Object3D`.
_Avoid_: Descendants

**Orientation**:
An `Object3D` rotation represented authoritatively by a quaternion. Euler angles are accepted only as inputs to explicit rotation operations in version 0.1.
_Avoid_: Mutable Euler state

**Coordinate Frame**:
The right-handed 3D space in which positive Y is up and cameras look down their local negative Z axis.
_Avoid_: Left-handed coordinates, Z-up coordinates

**Local Transform**:
An `Object3D` transform relative to its parent, with translation, quaternion orientation, and scale composed as `T × R × S`.
_Avoid_: Object transform

**World Transform**:
An `Object3D` transform in the Coordinate Frame, formed as its parent's World Transform multiplied by its Local Transform.
_Avoid_: Global transform

**Automatic Transform Maintenance**:
The guarantee that supported transform mutations mark derived state dirty and
that public matrix queries, world-transform queries, and rendering make the
required local and ancestor transforms current automatically. Version 0.1 has
no public manual matrix-update method or automatic-update switch.
_Avoid_: Manual matrix synchronization, `matrixAutoUpdate`

**Look At**:
A camera orientation operation that aims the camera's local negative Z axis at a target in the Coordinate Frame.
_Avoid_: Generic object aiming

**Camera**:
An `Object3D` viewpoint combining a World Transform with a projection of the Scene.

**Perspective Camera**:
A Camera whose projection makes more distant objects appear smaller.

**Orthographic Camera**:
A Camera whose projection preserves an object's apparent size regardless of its distance.

**Angle**:
A rotation or angular extent expressed in radians throughout the public interface, including camera field of view.
_Avoid_: Implicit degrees

**Background**:
The optional solid color belonging to a `Scene` and rendered behind its contents. When absent, the renderer's configured default clear color applies.
_Avoid_: Environment

**Material**:
A renderer-independent, shareable description of surface appearance and render
state. Closing a Material ends its application-owned lifetime without closing
any Mesh that refers to it.
_Avoid_: Shader program, OpenGL state object

**Basic Material**:
An unlit Material whose base color may optionally be multiplied by per-vertex
colors.
_Avoid_: Default material

**Mesh**:
An `Object3D` that binds one shared BufferGeometry to one shared Material for
triangle rendering. A Mesh does not own either Resource Description's lifetime.
_Avoid_: BufferGeometry, model

**Shader Material**:
A material whose application supplies vertex and fragment shader source plus
explicit custom uniform values. JScene3D supplies its Automatic Transform
Uniforms when the shader declares them.

**Automatic Transform Uniform**:
One of the renderer-owned `ShaderMaterial` matrices named `modelMatrix`,
`viewMatrix`, `projectionMatrix`, `modelViewMatrix`, or `normalMatrix`. A shader
may omit an unused one but cannot override the value of one it declares.
_Avoid_: Built-in uniform, application uniform

**Buffer Attribute**:
Library-owned, typed vertex data grouped into fixed-size items such as 3-value
positions or 2-value texture coordinates. Construction arrays are copied once;
mutable arrays and NIO buffers are not exposed publicly in version 0.1.
_Avoid_: Public backing buffer

**Attribute Edit**:
A bounded callback that directly changes a Buffer Attribute and automatically
records one version change for the batch. Its editor cannot be used after the
callback returns.
_Avoid_: Manual needs-update flag, buffer mapping

**Index Buffer**:
Library-owned non-negative vertex indices used for indexed triangle drawing.
Construction arrays are copied, and controlled edits remain valid for every
BufferGeometry sharing the Index Buffer.
_Avoid_: Element buffer object, public index array

**Draw Range**:
The validated contiguous portion of a BufferGeometry's indices or vertices selected
for drawing. In the absence of an explicit Draw Range, all available elements
are selected.
_Avoid_: Unchecked offset and count

**Asset Import**:
Loading an external interchange asset and mapping its supported content into
JScene3D scenes, objects, and Resource Descriptions. glTF 2.0 and GLB are the
primary formats.
_Avoid_: Scene Persistence

**glTF Support Profile**:
The explicit, versioned list of glTF core capabilities and extensions that a
JScene3D loader represents correctly. Unsupported required capabilities fail
diagnostically instead of being silently discarded. The current profile covers
correctly rendered scenes plus translation, rotation, and scale animation using
step, linear, and cubic-spline interpolation; skinning and morph targets remain
outside that profile.
_Avoid_: Full glTF support

**Animation Clip**:
An immutable named collection of typed keyframe tracks sharing a playback
timeline. A clip retains its target scene objects through its tracks but owns no
clock or thread.
_Avoid_: Animation sequence

**Animation Action**:
Mutable playback state for one Animation Clip, including local time, time scale,
loop mode, and running or paused state. An Animation Mixer owns one stable action
for each clip identity.
_Avoid_: Animator

**Animation Mixer**:
A caller-driven owner that advances Animation Actions using an explicit elapsed
time. Until weighted blending is supported, concurrent actions must target
distinct object properties.
_Avoid_: Animation thread

**Physics World**:
A renderer-independent simulation containing Rigid Bodies and advancing them
through explicit Fixed Updates.
_Avoid_: Scene, render world

**Rigid Body**:
A simulated body's motion state, mass properties, forces, and Collision Shape.
It does not own or directly mutate a scene object.
_Avoid_: Mesh, Object3D

**Collision Shape**:
A physics-specific description used for collision detection independently of
rendered geometry.
_Avoid_: BufferGeometry, visible mesh

**Fixed Update**:
One deterministic-duration advancement of a Physics World, independent of
render-frame timing.
_Avoid_: Render frame, variable update

**Physics Binding**:
A game-layer association that synchronizes a Rigid Body with a scene object and
may interpolate between Fixed Updates for presentation.
_Avoid_: Physics-owned Object3D

**Game Engine**:
The optional higher-level runtime that coordinates application lifecycle, game
states, input, assets, physics, animation, and rendering through JScene3D.
_Avoid_: Renderer, Physics World

**Scene Persistence**:
Future saving and restoration of JScene3D-specific scene state, identities, and
resource sharing through a versioned native format. It is separate from Asset
Import.
_Avoid_: Asset Import, Java object serialization

**Verified Platform**:
An operating-system and CPU combination on which the complete current test suite
has run successfully on maintained hardware. During private foundation work,
this is macOS ARM64 on the maintainer's M1 MacBook Pro.

**Provisional Platform**:
A portability target that is not advertised as supported until its automated
native-resolution, deterministic rendering, pixel-readback, and cleanup checks
pass.
_Avoid_: Supported Platform

**Supported Platform**:
A Provisional Platform that has passed the documented qualification checks for
the release being published.
_Avoid_: Platform with available native binaries

**Public Preview Gate**:
The point at which the interactive Solar System Viewer demonstrates the useful
public programming path on the Verified Platform. Crossing it makes the
repository public and enables free cross-platform qualification in GitHub
Actions while 0.1 remains under development.

**Multisample Anti-Aliasing (MSAA)**:
Optional smoothing provided by a window's default framebuffer. Version 0.1
disables it by default, permits an application to request a preferred sample
count at window creation, and reports the actual platform-supplied count.
_Avoid_: Render-target MSAA

**Color**:
An immutable three-channel value stored in JScene3D's linear-sRGB working space.
Factories identify whether caller input is sRGB-encoded or already linear;
alpha is represented separately. Version 0.1 includes a small set of familiar
primary, secondary, black, white, and gray constants.
_Avoid_: Ambiguous RGB value

**Base-Color Texture**:
A texture containing display-oriented surface color. It defaults to sRGB input
and is converted to the linear working space during sampling.

**Data Texture**:
A texture whose channels represent non-color data. It defaults to linear input
and receives no color-space conversion.

**Mipmap Mode**:
A Texture policy choosing either renderer-generated mipmap levels or no mipmaps.
Version 0.1 defaults to generation with trilinear minification, accepts only a
base image, and rejects filters incompatible with the selected mode.
_Avoid_: Silently normalized texture filter

**Texture Loader**:
The `jscene3d-lwjgl` component that loads supported disk images into core-owned
Texture pixel storage. Version 0.1 supports PNG and JPEG without exposing STB or
native-memory ownership to callers.

**Renderer State Ownership**:
The renderer's exclusive control of all OpenGL state in its context throughout
its lifetime. Version 0.1 does not support direct application OpenGL calls on a
JScene3D-managed context and does not preserve or restore external OpenGL state;
`ShaderMaterial` is its supported rendering-customization boundary.
_Avoid_: Shared OpenGL state, implicit raw OpenGL interoperability

**Window-Renderer Pair**:
A JScene3D Window with its independent OpenGL context and the sole Renderer that
owns that context. Version 0.1 permits multiple pairs sequentially on one render
thread; Resource Descriptions used by multiple pairs receive independent GPU
Realizations. Context sharing, renderer migration, and concurrent render
threads are excluded.
_Avoid_: Shared context, interchangeable renderer context

**Event Poll**:
One process-wide call that dispatches pending platform events for every open
JScene3D Window and updates each window's state. It returns no event collection.
_Avoid_: Per-window polling, event-list return value

**Input State**:
A stable, read-only per-window view of held keyboard and mouse state, current
pointer position, and the press, release, pointer movement, and scroll changes
accumulated during the latest Event Poll. Reading it does not consume changes.
_Avoid_: Native callback, allocated event queue

**Orbit Controls**:
A window-input component that keeps an unparented Perspective or Orthographic
Camera aimed at a configurable World Transform target. Mouse and keyboard input
support orbiting, panning, perspective dolly, and orthographic zoom, with
optional damping and automatic rotation. Version 0.1 excludes touch input,
remappable bindings, zoom-to-cursor behavior, and parented cameras.
_Avoid_: Camera ownership, native input callback
