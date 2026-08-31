# Three.js-Inspired High-Level 3D Graphics Library for Java

## Architecture and Implementation Blueprint

**Status:** Accepted version 0.1 architecture
**Purpose:** Starting point for design refinement and implementation planning
**Primary implementation language:** Java
**Initial graphics backend:** OpenGL 3.3 Core through LWJGL 3
**Math library:** JOML

---

## 1. Executive Summary

This project will create a high-level, scene-graph-based 3D graphics library for
Java with a developer experience inspired by Three.js. The library should allow
application developers to describe a scene using objects, cameras, geometry,
materials, and textures, then render that scene without managing OpenGL state
directly.

The target experience is conceptually similar to:

```java
try (Window window = Window.create(1280, 720, "Example");
     Renderer renderer = Renderer.create(window, RendererOptions.defaults());
     BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
     BasicMaterial material = new BasicMaterial(
         Color.srgb(0.2f, 0.6f, 1.0f)
     )) {

    Scene scene = new Scene();
    PerspectiveCamera camera = new PerspectiveCamera(
        (float) Math.toRadians(60.0),
        window.framebufferAspectRatio(),
        0.1f,
        1000.0f
    );

    Mesh cube = new Mesh(geometry, material);

    scene.add(cube);
    camera.setPosition(0.0f, 0.0f, 5.0f);
    window.show();

    while (!window.shouldClose()) {
        Window.pollEvents();
        cube.rotateY(0.01f);
        renderer.render(scene, camera);
        window.swapBuffers();
    }
}
```

The core architectural rule is:

> Public scene objects describe what should be rendered. Renderer-internal
> objects own how those descriptions are represented and executed on the GPU.

This separation keeps the public model understandable, makes scene-graph logic
testable without a GPU, prevents OpenGL resource identifiers from leaking into
application code, and leaves room for a future renderer without forcing a
premature multi-backend abstraction.

The first objective is not literal Three.js feature parity. The first objective
is a coherent, well-tested vertical slice with a stable programming model:

- Create a window and OpenGL context.
- Construct and modify a hierarchical scene.
- Create cameras, geometry, materials, and meshes.
- Render opaque and transparent meshes correctly.
- Upload, reuse, update, and release GPU resources safely.
- Diagnose common misuse with useful errors.
- Run useful tests without requiring every test to open a native window.

---

## 2. Goals

### 2.1 Primary goals

1. Provide a high-level, object-oriented scene construction model for Java.
2. Hide routine OpenGL operations behind a compact renderer interface.
3. Support hierarchical transforms through a conventional scene graph.
4. Use JOML for allocation-conscious vector, quaternion, and matrix operations.
5. Use LWJGL 3 for GLFW, OpenGL bindings, native memory utilities, and platform
   integration.
6. Establish deterministic ownership and cleanup rules for native and GPU
   resources.
7. Keep the scene model independent from OpenGL resource identifiers and context
   ownership.
8. Make core behavior testable without a real GPU.
9. Provide predictable diagnostics for shader, geometry, context, and lifecycle
   errors.
10. Preserve a path toward additional renderers without implementing them
    prematurely.

### 2.2 Developer-experience goals

The normal user should need to understand:

- Scene hierarchy.
- Local and world transforms.
- Cameras.
- BufferGeometry attributes.
- Materials and textures.
- The per-frame update and render loop.
- Explicit cleanup at application shutdown.

The normal user should not need to understand:

- VAO, VBO, or EBO allocation.
- OpenGL binding order.
- Shader compilation and program linking for built-in materials.
- Uniform location caching.
- Texture unit allocation.
- Render-list sorting.
- Redundant state-change elimination.
- Native buffer allocation details.
- Platform-specific native dependency classifiers.

### 2.3 Quality goals

- No avoidable allocation in the steady-state render path.
- Deterministic cleanup of renderer, window, and context resources.
- Reproducible example applications.
- Unit tests for scene and math behavior.
- Integration tests for critical OpenGL behavior.
- Clear exception messages that include the failing object or shader context.
- Public documentation that states invariants, thread expectations, lifecycle,
  and mutation rules.

---

## 3. Non-Goals for the Initial Release

The initial release should not attempt to include all of the following:

- A visual scene editor.
- A game engine or entity-component system.
- Physics, audio, networking, scripting, or gameplay frameworks.
- Literal source compatibility with JavaScript Three.js.
- Every geometry generator provided by Three.js.
- Every Three.js material type.
- Physically based rendering in version 0.1.
- Shadows in the first vertical slice.
- Skeletal animation in the first vertical slice.
- Morph targets in the first vertical slice.
- glTF or GLB loading in version 0.1.
- Post-processing in the first vertical slice.
- Virtual reality or augmented reality support.
- Vulkan, Metal, Direct3D, or WebGPU implementations in the first release.
- A public backend abstraction designed around hypothetical future
  implementations.
- Automatic thread safety for scene mutation.
- Hidden, nondeterministic reliance on garbage collection for GPU cleanup.

These features may be valid later. Excluding them initially protects the
coherence of the foundation.

---

## 4. Feasibility and Platform Position

The architecture is technically feasible with the proposed stack.

LWJGL 3 provides direct Java bindings to GLFW and OpenGL. GLFW provides
cross-platform window creation, input callbacks, and OpenGL context management.
JOML provides the matrices, vectors, and quaternions required by scene traversal
and rendering.

OpenGL 3.3 Core is a practical initial baseline because it supports:

- Vertex array objects.
- Vertex buffer and index buffer objects.
- Programmable vertex and fragment shaders.
- Instanced drawing for later milestones.
- Framebuffer objects for later render-target work.
- Broad support on Windows, Linux, and macOS.

Important macOS constraints must be treated as architectural inputs:

- OpenGL is deprecated by Apple.
- Modern OpenGL contexts on macOS are core profile and forward-compatible.
- macOS OpenGL support does not advance beyond OpenGL 4.1.
- LWJGL/GLFW applications on macOS generally need JVM startup on the first
  thread.
- Logical window size and framebuffer pixel size differ on Retina displays.

The initial implementation should therefore use OpenGL without exposing OpenGL
assumptions throughout the public scene model. This does not require a public
`RenderBackend` interface. It requires disciplined package ownership and an
internal seam between scene descriptions and renderer-specific resources.

---

## 5. Architectural Vocabulary

The following terms are used consistently throughout this blueprint.

### Component

A component is a logical class, package, or subsystem with an interface and an
implementation.

### Artifact

An artifact is a separately built and published dependency.

### JPMS module

A JPMS module is a Java Platform Module System unit declared by
`module-info.java`. The unqualified term "module" is avoided because it is
otherwise ambiguous.

### Interface

An interface is everything a caller must understand to use a component correctly.
It includes method signatures, invariants, ordering constraints, lifecycle,
error behavior, thread expectations, and performance characteristics.

### Seam

A seam is a location where behavior can change without changing callers at that
location. The public renderer interface is one seam. Renderer-internal GPU
resource stores are internal seams.

### Adapter

An adapter is a concrete implementation placed at a seam. An OpenGL renderer
would become an adapter only if another renderer actually exists behind the same
interface. Until then, it is simply the renderer implementation.

### Deep component

A deep component provides substantial behavior behind a small interface. The
renderer should be a deep component: callers provide a scene and camera, while
traversal, sorting, shader resolution, GPU resource management, and draw
submission remain internal.

---

## 6. Core Architectural Principles

### 6.1 Descriptions are separate from GPU realizations

Public objects such as `BufferGeometry`, `Material`, and `Texture` describe renderable
data. They do not store context-specific OpenGL handles.

The renderer owns corresponding internal resources:

```text
Public BufferGeometry                 Renderer-internal BufferGeometry Resource
---------------                 -----------------------------------
attributes                      VAO identifier
indices                         VBO identifiers
bounds                          EBO identifier
version                         uploaded versions
usage hints                     attribute layouts
                                owning context
```

This rule enables:

- Headless testing of scene data.
- Multiple renderers or contexts later.
- Context recreation.
- Lazy GPU upload.
- Centralized cleanup.
- Renderer diagnostics.

### 6.2 Public interfaces do not expose OpenGL

Public core packages must not expose:

- OpenGL integer constants.
- OpenGL object identifiers.
- LWJGL OpenGL classes.
- Native pointer values.
- Context-specific capabilities.

Renderer and platform packages may expose carefully selected platform behavior
where necessary, but scene, camera, geometry, and material packages must remain
free of OpenGL details.

### 6.3 Favor vertical slices

Each milestone should end in visible, testable behavior. Avoid implementing a
large taxonomy of unused abstract base classes before one mesh can pass through
the entire system.

### 6.4 Introduce seams only where behavior varies

Do not create public interfaces such as `GraphicsBackend`, `ShaderCompiler`, or
`GpuBufferFactory` merely because they might vary someday. One implementation
behind an elaborate seam adds interface cost without current leverage.

Internal renderer classes may still be separated for locality and testing.

### 6.5 Keep hot paths allocation-conscious

Per-frame traversal and draw submission should reuse:

- JOML destination objects.
- Render-list storage.
- Temporary matrices.
- Uniform staging buffers.
- Native memory stacks where scope is clear.

Allocation-free operation is a steady-state objective, not a reason to make all
public interfaces obscure.

### 6.6 Fail early during development

Development builds should prefer clear validation over silent corruption:

- Reject scene-graph cycles.
- Reject attributes with inconsistent vertex counts.
- Reject invalid index ranges where practical.
- Include shader compilation logs in exceptions.
- Detect renderer use on the wrong thread.
- Detect use after close.
- Validate that a compatible OpenGL context is current.

Validation may become configurable when measurement shows that it affects
production performance.

---

## 7. High-Level Architecture

```text
Application
   |
   +-- creates and mutates --------------------------------------+
   |                                                            |
   v                                                            v
Window / Input                                             Scene Graph
   |                                                     Scene, Object3D
   |                                                     Camera, Mesh
   |                                                     BufferGeometry
   |                                                     Material, Texture
   |                                                            |
   +-- current context + framebuffer size                       |
                         |                                      |
                         v                                      v
                    Renderer.render(scene, camera)
                                  |
               +------------------+------------------+
               |                  |                  |
         Scene traversal     Render-list build   Resource resolve
               |                  |                  |
               +------------------+------------------+
                                  |
                        Program/state resolution
                                  |
                           OpenGL draw submission
                                  |
               +------------------+------------------+
               |                  |                  |
          BufferGeometry store      Texture store      Program cache
               |                  |                  |
               +------------------+------------------+
                                  |
                             LWJGL OpenGL
```

The public interface ends at `Renderer.render(scene, camera)` and explicit
lifecycle operations. Everything below that seam is renderer implementation.

---

## 8. Recommended Component Map

JScene3D uses the base package `io.github.glynch.jscene3d`. An illustrative
package layout follows; the child packages communicate ownership and dependency
direction rather than requiring one class per line immediately.

```text
io.github.glynch.jscene3d
├── math
│   ├── Color
│   ├── BoundingBox
│   ├── BoundingSphere
│   └── Angles
├── objects
│   ├── Object3D
│   ├── Group
│   ├── Mesh
│   ├── Line
│   └── LineSegments
├── scenes
│   └── Scene
├── cameras
│   ├── Camera
│   ├── PerspectiveCamera
│   └── OrthographicCamera
├── geometries
│   ├── BufferGeometry
│   ├── BufferAttribute
│   ├── IndexBuffer
│   └── generators
├── materials
│   ├── Material
│   ├── BasicMaterial
│   ├── LambertMaterial
│   ├── LineBasicMaterial
│   ├── NormalMaterial
│   ├── PhongMaterial
│   ├── ShaderMaterial
│   ├── MaterialSide
│   └── DepthFunction
├── lights
│   ├── Light
│   ├── AmbientLight
│   ├── DirectionalLight
│   ├── HemisphereLight
│   ├── PointLight
│   └── SpotLight
├── textures
│   ├── Texture
│   ├── TextureFilter
│   └── TextureWrap
├── helpers
│   ├── AxesHelper
│   ├── GridHelper
│   └── BoxHelper
├── raycasting
│   ├── Raycaster
│   └── RaycastHit
├── render
│   ├── Renderer
│   ├── RendererOptions
│   ├── RendererInfo
│   ├── RenderStatistics
│   ├── ResourceStatistics
│   └── internal
│       ├── RenderList
│       ├── RenderItem
│       ├── Frustum
│       ├── programs
│       └── resources
├── platform
│   ├── Window
│   ├── WindowOptions
│   ├── InputState
│   ├── Key
│   └── MouseButton
├── controls
│   └── OrbitControls
├── loaders
│   └── TextureLoader
├── gui
│   ├── ControlPanel
│   ├── FpsMonitor
│   └── GuiTheme
└── examples
    ├── BasicTriangleExample
    ├── HierarchyExample
    ├── TexturedCubeExample
    ├── TextureTransformsExample
    ├── ObjectSelectionExample
    └── TransparencyExample
```

Recommended dependency direction:

```text
math <- geometries + materials + textures
objects + geometries + materials <- cameras + lights + scenes + helpers
cameras + geometries + lights + materials + objects + scenes + textures <- render
LWJGL OpenGL <- render implementation
LWJGL GLFW <- platform implementation
```

The core scene model must not depend on the renderer implementation.

Initial published artifact boundary:

```text
io.github.glynch:jscene3d-core  io.github.glynch:jscene3d-lwjgl  io.github.glynch:jscene3d-gui
----------------------------    --------------------------------  -------------------------------
scene graph                     OpenGL renderer                   control panels
cameras                         GLFW window/input                 diagnostics
geometry/materials              perspective OrbitControls        TrueType UI
textures and JOML-facing APIs   safe logical overlay canvas
```

The `jscene3d-lwjgl` artifact depends on `jscene3d-core`. The optional
`jscene3d-gui` artifact depends on `jscene3d-lwjgl`. Renderer owns the OpenGL
implementation behind a generic logical overlay interface; GUI code never
receives OpenGL handles or state.

The Maven group is `io.github.glynch`. Examples use the reactor artifact name
`jscene3d-examples`, depend on the three published artifacts, and are not published.
GLFW remains in `jscene3d-lwjgl` until a real alternative host requires a
separate integration.

Each published artifact is a genuine JPMS module and also supports ordinary
classpath consumption:

```text
Maven artifact    JPMS module
----------------  ----------------------------------
jscene3d-core     io.github.glynch.jscene3d.core
jscene3d-lwjgl    io.github.glynch.jscene3d.lwjgl
jscene3d-gui      io.github.glynch.jscene3d.gui
```

Each module exports only intentional caller packages. Packages do not split
across artifacts, and external consumer fixtures verify both module-path and
classpath use during ordinary verification.

The first post-0.1 loader adds a third published artifact:

```text
io.github.glynch:jscene3d-gltf
└── depends on io.github.glynch:jscene3d-core
```

`jscene3d-gltf` performs CPU-side parsing and mapping without depending on
`jscene3d-lwjgl`, GLFW, or an active OpenGL context. Format-specific dependencies
and later optional decoders remain on the loader side of this boundary.

### 8.1 Asset-import boundaries and format roadmap

Asset Import is distinct from native JScene3D Scene Persistence. Import maps an
external interchange format into the public JScene3D model; persistence will
eventually preserve JScene3D-specific state such as custom materials, resource
sharing, and library-specific object types.

glTF 2.0 is the primary portable runtime scene and asset format. The first
loader supports both `.gltf` documents with external resources and self-contained
`.glb` containers. Format support is described by an explicit, versioned
capability profile rather than an unqualified claim of complete glTF support.
Unsupported required extensions and unsupported representations fail with a
diagnostic identifying the feature and source location; they are never silently
discarded when that would change the represented scene.

The longer-term import tiers are:

1. glTF 2.0 and GLB as the first and primary loader.
2. OBJ with MTL for legacy static-model interchange.
3. STL for CAD and printing workflows, and PLY for scanned meshes, point data,
   and vertex-color workflows, when demonstrated demand justifies them.
4. FBX and COLLADA through conversion to glTF by default, with an optional
   Assimp-backed compatibility artifact only if direct-import demand justifies
   native dependencies and explicitly documented fidelity limits.
5. USD and USDZ as a separate specialist integration if professional scene
   composition or DCC-pipeline requirements emerge.

Format loaders do not become transitive dependencies of `jscene3d-core` or
`jscene3d-lwjgl`. glTF support lives in the optional `jscene3d-gltf` artifact;
later loaders receive their own artifact when their dependency or platform costs
justify one. A versioned native JScene3D persistence format is deferred until
the public scene, material, and resource models are stable enough to preserve
durably; Java built-in object serialization is not its fallback.

---

## 9. Coordinate and Math Conventions

These conventions must be decided before implementing cameras, helpers, loaders,
or culling.

### 9.1 Recommended conventions

- Right-handed coordinate system.
- Positive Y is up.
- Cameras look down local negative Z.
- Local transforms are composed exactly as `T × R × S`. With column vectors, a
  point is scaled first, then rotated, then translated.
- A child world transform is exactly `parentWorld × childLocal`.
- Every public angle is expressed in radians, including camera field of view.
- Examples use JOML's float angle constants and `org.joml.Math.toRadians(float)`;
  the library does not initially duplicate them in an angle utility.
- Quaternions are the sole authoritative orientation representation.
- Version 0.1 accepts Euler angles through explicit convenience operations but
  does not expose synchronized mutable Euler state.
- Matrices follow JOML/OpenGL-compatible conventions.
- Clip-space depth follows the initial OpenGL convention.

### 9.2 Required tests

Tests must establish the conventions with concrete examples:

- An untranslated object has an identity local and world matrix.
- A child at local `(1, 0, 0)` under a parent translated to `(5, 0, 0)` has
  world position `(6, 0, 0)`.
- A child inherits parent rotation.
- A child inherits non-uniform parent scale according to matrix multiplication
  rules.
- The default perspective camera sees an object located along negative Z.
- `lookAt` produces the documented forward direction.
- Projection and view matrices map representative points to expected normalized
  device coordinates.

### 9.3 Floating-point policy

- Use single-precision floats in rendering and scene transforms initially.
- Use epsilon comparisons in tests.
- Do not use exact matrix equality for calculated transforms.
- Document behavior for extremely large world coordinates as out of scope
  initially.
- Consider a floating-origin strategy later rather than prematurely switching
  the entire scene graph to doubles.

---

## 10. Scene Graph Design

### 10.1 `Object3D` responsibilities

`Object3D` is the base component for positioned scene nodes. It owns:

- Stable identity.
- Optional developer-assigned name.
- Parent reference.
- Ordered child collection.
- Local position.
- Local orientation.
- Local scale.
- Local matrix.
- World matrix.
- Visibility.
- Frustum-culling eligibility.
- Explicit render order within the opaque or transparent render list.
- Optional layer mask.
- Local-matrix update behavior.
- World-matrix update behavior.

It does not own:

- OpenGL resources.
- Render programs.
- Vertex attributes.
- Window state.
- Application-loop timing.

### 10.2 Initial public shape

The initial interface follows the project's Java naming and controlled-mutation
standards:

```java
public class Object3D {
    public Object3D parent();
    public List<Object3D> children();

    public Vector3fc position();
    public Quaternionfc quaternion();
    public Vector3fc scale();

    public void setPosition(float x, float y, float z);
    public void setPosition(Vector3fc position);
    public void setQuaternion(Quaternionfc quaternion);
    public void setScale(float x, float y, float z);
    public void setScale(Vector3fc scale);
    public void rotateX(float angle);
    public void rotateY(float angle);
    public void rotateZ(float angle);
    public void setRotationFromEuler(
        float x, float y, float z, RotationOrder order
    );

    public Matrix4fc matrix();
    public Matrix4fc matrixWorld();

    public boolean isVisible();
    public void setVisible(boolean visible);
    public boolean isFrustumCullingEnabled();
    public void setFrustumCullingEnabled(boolean enabled);
    public int renderOrder();
    public void setRenderOrder(int renderOrder);

    public boolean add(Object3D child);
    public boolean remove(Object3D child);
    public boolean detach();
    public void clear();

    public void traverse(Consumer<Object3D> visitor);
    public void traverseVisible(Consumer<Object3D> visitor);

    public Vector3f worldPosition(Vector3f destination);
    public Quaternionf worldQuaternion(Quaternionf destination);
    public Vector3f worldScale(Vector3f destination);
}
```

`children()` returns a stable, unmodifiable live view in insertion order. Scene
hierarchy mutations must go through `add`, `remove`, or `clear` so that parent
invariants cannot be bypassed. Callers that require a snapshot can explicitly
use `List.copyOf(object.children())`.

Mutation methods do not return their receiver merely to enable chaining.
Methods return a value only when it communicates a meaningful result; for
example, `add` and `remove` report whether the hierarchy changed. Builders are
reserved for construction with genuinely numerous optional settings.

### 10.3 Transform mutation strategy

There are two plausible strategies.

#### Rejected strategy: mutable JOML transforms with per-frame local recomposition

```java
mesh.position().set(1.0f, 2.0f, 3.0f);
mesh.quaternion().rotateY(angle);
```

At traversal time, nodes with automatic matrix updates recompute their local
matrices. This preserves a Three.js-like experience and avoids the need to wrap
every JOML mutation.

Advantages:

- Familiar, compact syntax.
- Direct use of JOML.
- No allocation.
- No custom mutable-vector wrapper.
- Mutations cannot silently bypass dirty marking because recomposition is
  automatic.

Costs:

- Local matrices are recomposed even when transforms are unchanged.
- Direct mutation must remain on the scene thread.

#### Selected strategy: controlled mutation with dirty versions

```java
mesh.setPosition(1.0f, 2.0f, 3.0f);
mesh.rotateY(angle);
```

Public accessors return zero-copy live views through JOML read-only interfaces.
Mutation methods validate input, mark derived state dirty, and preserve
component invariants. Common mutations provide scalar overloads, avoiding
temporary values, and read-only JOML-value overloads, copying values callers
already possess. Explicit snapshot operations copy into caller-owned
destinations. This creates a broader interface and makes mutable JOML chaining
less natural, but ensures supported updates cannot bypass the owning component.

#### Recommendation

Use controlled mutation and dirty versions initially. Recompose a local matrix
only after its position, orientation, or scale changes, then propagate world
matrix invalidation through the hierarchy. Large geometry payloads require a
separate explicit bulk-edit contract rather than per-value setters or silently
exposed mutable storage.

Transform-derived state is maintained automatically. `matrix()` ensures that
the local matrix is current; `matrixWorld()` and the world-transform query
methods update any required ancestor and local state before returning; and
`Renderer.render(...)` updates required world transforms during traversal.
Version 0.1 exposes neither manual matrix-update methods nor a
`matrixAutoUpdate` switch. Callers therefore never need to synchronize matrices
after using a supported transform mutator.

### 10.4 Hierarchy invariants

The following are mandatory:

- A node cannot be its own child.
- Adding an ancestor as a child is rejected because it creates a cycle.
- A node has at most one parent.
- Adding a node to a new parent first validates the operation, then detaches it
  from its old parent and preserves its local transform. Its world transform may
  consequently change.
- Adding the same child to the same parent is an idempotent no-op: it neither
  duplicates nor reorders the child.
- Removing a node updates both sides of the relationship.
- Calling `detach()` removes a node from its parent and reports whether the
  hierarchy changed. Its local transform is preserved, so its world transform
  may change.
- Child ordering is stable.
- Traversal behavior under concurrent structural mutation is explicitly
  unsupported or precisely defined.

Cycles are rejected before any hierarchy mutation occurs. Structural mutation
during traversal is unsupported and detected in development where practical. A
separate world-transform-preserving `attach` operation may be added when a real
use case requires it.

### 10.5 World-matrix propagation

Recommended traversal behavior:

1. Recompose the node's local matrix when automatic updates are enabled.
2. If the node has no parent, copy its local matrix to its world matrix.
3. Otherwise multiply the parent world matrix by the local matrix.
4. Recurse into children.

This is independent of node visibility. Invisible nodes may still need correct
world matrices for later queries. The render traversal may skip invisible
subtrees after matrices have been updated according to the documented policy.

### 10.6 `Scene`

`Scene` is a specialized root `Object3D`. Initially it owns an optional solid
background color. When present, that color overrides the renderer's default
clear color for the render call; when absent, the renderer default applies.
Environment settings and fog may be added later.

Do not place renderer state, GLFW state, or a list duplicated from the hierarchy
into `Scene`.

### 10.7 `Group`

`Group` is a semantic `Object3D` with no rendering behavior. It improves
readability without adding renderer complexity.

---

## 11. Camera Design

### 11.1 Base `Camera`

`Camera` extends `Object3D` and owns:

- Projection matrix.
- Inverse projection matrix if needed.
- View matrix, derived from the inverse world matrix.
- Projection version.

The renderer updates or obtains the camera view matrix after world transforms
are current.

`Camera.lookAt(...)` aims the camera's local negative Z axis at a world-space
target. Version 0.1 does not expose generic `Object3D.lookAt(...)`; a later
general-object operation must identify the local axis to orient toward the
target explicitly.

### 11.2 `PerspectiveCamera`

Required properties:

- Vertical field of view.
- Aspect ratio.
- Near clipping plane.
- Far clipping plane.

Required validation:

- Every value is finite.
- Field of view satisfies `0 < fov < π`.
- Aspect ratio is positive.
- Clipping planes satisfy `0 < near < far`.

The constructor validates the complete initial projection. Controlled setters
include `setFieldOfView(radians)`, `setAspectRatio(aspect)`, and the atomic
`setClippingPlanes(near, far)`. Setters mark the projection dirty; the projection
matrix is recomputed lazily when accessed or rendered. Callers never invoke a
manual projection-update method.

### 11.3 `OrthographicCamera`

Required properties:

- Left plane.
- Right plane.
- Top plane.
- Bottom plane.
- Near plane.
- Far plane.
- Positive zoom factor.

Every value must be finite. Bounds satisfy `left < right` and `bottom < top`;
clipping planes satisfy `0 <= near < far`. Infinite far planes, reversed depth,
inverted bounds, and oblique projections are unsupported in version 0.1.
Invalid constructor and setter arguments throw `IllegalArgumentException` that
names the offending values and required relationship.

The constructor validates the complete initial projection. Controlled setters
include atomic `setBounds(left, right, top, bottom)` and
`setClippingPlanes(near, far)` operations, plus `setZoom(zoom)`. Zoom preserves
the center of the configured bounds. Projection matrices follow the same
automatic lazy-update rule as `PerspectiveCamera`.

Version 0.1 includes only `PerspectiveCamera` and `OrthographicCamera`.
`StereoCamera` and other specialized camera compositions are added with the
later feature and focused example that justify them.

### 11.4 Resize behavior

Window resize and camera projection are separate concerns. The renderer updates
the viewport from framebuffer dimensions. The application updates camera aspect
ratio according to its presentation policy.

A convenience helper may connect these later, but automatic camera mutation
inside `Renderer.render` should be avoided because:

- Multiple cameras may exist.
- Split-screen rendering may use multiple viewports.
- Applications may use fixed-aspect rendering or letterboxing.

---

## 12. BufferGeometry Design

### 12.1 Public geometry is CPU-side descriptive data

`BufferGeometry` contains:

- Named vertex attributes.
- An optional index buffer.
- Draw range.
- Primitive topology.
- Bounding box and bounding sphere.
- Version information.
- Optional usage hints.
- A disposed state or disposal generation.

It does not contain:

- VAO identifiers.
- VBO identifiers.
- EBO identifiers.
- OpenGL target constants.
- Attribute locations tied to one shader.

### 12.2 `BufferAttribute`

A buffer attribute needs at least:

- A semantic or name such as `position`, `normal`, `uv`, or `color`.
- Backing primitive data.
- Item size, such as three values per position.
- Element count.
- Scalar type.
- Normalized flag where relevant.
- Usage hint: static, dynamic, or stream.
- Update version.
- Optional partial update range later.

Initial scalar support is deliberately narrow:

- Float attributes.
- Non-negative 32-bit integer indices, uploaded as unsigned GPU indices.

Additional integer and packed formats should be added when a real feature
requires them.

NIO buffers and mutable backing arrays are not part of the public interface in
version 0.1. Creation accepts ordinary `float[]` attribute data and `int[]`
index data and defensively copies it once. Explicit `toArray()`-style snapshot
operations may copy data back out when requested. The renderer uses a private
zero-copy view of library-owned storage and does not copy unchanged attributes
each frame.

### 12.3 BufferGeometry invariants

- A position attribute is required for mesh drawing.
- Item size is positive.
- Backing length is divisible by item size.
- All per-vertex attributes used by a material have compatible element counts.
- Indices refer to valid vertices.
- Draw ranges stay within the available index or vertex range.
- Attribute versions increase when upload-visible data changes.

### 12.4 Mutation and versioning

The renderer needs to distinguish unchanged data from data requiring re-upload.

Common changes use controlled setters:

```java
positions.setXYZ(vertexIndex, x, y, z);
```

Bulk changes use a scoped callback:

```java
positions.edit(editor -> {
    editor.setXYZ(0, x1, y1, z1);
    editor.setXYZ(1, x2, y2, z2);
});
```

The editor writes directly to library-owned storage; it does not make a working
copy. It validates indices and component counts, becomes unusable when the
callback returns, and records one attribute-version change for the batch. If a
callback throws after making changes, those changes remain, the version and
derived bounds are still invalidated, and the original exception is rethrown.
Single-value setters update the version immediately. Users never call
`markNeedsUpdate()` themselves.

This interface keeps validation and renderer synchronization under library
control. A lower-level streaming path may be added later only when a measured
workload demonstrates that this interface is inadequate.

### 12.5 Bounds

BufferGeometry should support:

- `computeBoundingBox()`.
- `computeBoundingSphere()`.
- Explicitly supplied bounds for procedurally streamed geometry.
- Invalidating computed bounds when position data changes.

Frustum culling should depend primarily on the bounding sphere initially.
Bounding boxes remain useful for tooling and raycasting broad-phase rejection.

### 12.6 Raycasting

Version 0.1 includes renderer-independent CPU raycasting for visible triangle
meshes. `Raycaster` accepts an explicit normalized world ray or derives one
from perspective and orthographic camera coordinates. It traverses hierarchies
iteratively and returns immutable nearest-first `RaycastHit` values containing
the mesh, distance, world point, face index, and optional interpolated texture
coordinate.

Mesh intersection respects world transforms, reflected winding, material side
and visibility, index buffers, and draw ranges. Bounding spheres and available
bounding boxes reject misses before triangle testing. Line picking remains out
of scope until its world- or screen-space distance tolerance is defined.

### 12.7 Primitive topology

Start with triangles. Add lines and points only when their object and material
behavior is defined. Avoid exposing every OpenGL topology merely because the
binding makes it easy.

### 12.8 BufferGeometry generators

Generators such as box, plane, sphere, and ring should create ordinary
`BufferGeometry` values. They do not need a deep inheritance hierarchy.

Recommended early generators:

- Triangle fixture.
- Plane.
- Box.
- UV sphere.
- Ring.

Each generator must test:

- Vertex and index counts.
- Attribute lengths.
- Winding order.
- Normal direction.
- UV range.
- Bounding volumes.

---

## 13. Material Design

### 13.1 Material role

A material describes how a surface should be rendered. It combines:

- Shader-visible properties.
- Program-selection characteristics.
- Fixed-function render state.

It should not simply contain paths to GLSL files.

### 13.2 Base material properties

Likely initial properties include:

- Visibility.
- Opacity.
- Transparent flag.
- Side: front, back, or double.
- Depth test enabled.
- Depth write enabled.
- Blending mode.
- Wireframe later if supported coherently.
- Material version.

Defaults must be explicit and documented.

### 13.3 `BasicMaterial`

The initial built-in material should be unlit and support:

- Base color.
- Optional vertex colors.
- Optional 2D color texture.
- Opacity.
- Side selection.
- Transparency.

This produces useful results without requiring a light system or physically
based shader model.

### 13.4 `ShaderMaterial`

`ShaderMaterial` is the escape hatch for custom rendering. It should include:

- Vertex shader source.
- Fragment shader source.
- Declared uniforms.
- Optional preprocessor definitions.
- Required attribute expectations.
- Render state inherited from `Material`.

The first design should deliberately constrain supported uniform types. For
example:

- `float`, `int`, and `boolean`.
- Vector2, Vector3, Vector4.
- Matrix3 and Matrix4.
- Color.
- Texture.

Avoid a reflection-heavy universal object mapper until concrete use cases
justify it.

### 13.5 `LambertMaterial` and initial lights

Version 0.1 also includes a narrow diffuse lighting path needed by the Solar
System Viewer. `LambertMaterial` supports base color, optional vertex colors,
an optional color map, and inherited material render state. It requires
geometry normals and responds to visible `AmbientLight`, `PointLight`,
`DirectionalLight`, `SpotLight`, and `HemisphereLight` scene nodes. Lights are
sealed `Object3D` subclasses with linear-sRGB color and a non-negative practical
intensity multiplier.

Point lights inherit their world position and expose non-negative distance and
decay controls. Directional lights derive their direction from their inherited
world position and a copied world-space target point. Spotlights similarly aim
at a copied target and add a bounded cone angle, penumbra, distance, and decay.
Hemisphere lights blend sky and ground colors according to the surface normal
and the light's world-space direction. The renderer supports eight visible
lights of each positional or directional type, aggregates ambient
contributions, and fails rather than silently dropping excess lights. This
initial path excludes shadows, calibrated physical units, normal maps,
environment lighting, and PBR behavior.

### 13.6 `LineBasicMaterial`

`LineBasicMaterial` provides unlit base color, optional vertex-color
multiplication, and inherited opacity, transparency, and depth state. Line
primitives have no face orientation, so inherited material-side selection is
ignored. The portable raster width remains one framebuffer pixel.

All materials expose a `DepthFunction`, defaulting to `LESS_OR_EQUAL`. This
allows deliberately ordered coplanar objects to replace equal-depth fragments
without bypassing occlusion by genuinely closer geometry.

### 13.7 `NormalMaterial` and `PhongMaterial`

`NormalMaterial` is an unlit diagnostic material that maps transformed
view-space normals to RGB. It deliberately exposes no inactive normal-map,
bump-map, displacement, or wireframe properties.

`PhongMaterial` adds Blinn-Phong specular highlights to the existing lit path.
It supports base color, optional vertex colors, an optional transformed color
map, emissive color and intensity, specular color, shininess, and inherited
material state. It responds to the same ambient, point, directional, spot, and
hemisphere lights as `LambertMaterial`. Common transform and light-uniform
staging is shared by the Lambert and Phong renderer programs.

### 13.8 Shader variants

Even a basic material can produce variants:

- Textured or untextured.
- Vertex colors enabled or disabled.
- Alpha testing later.
- Skinning later.
- Instancing later.

The renderer should calculate a stable program key from structural
characteristics, not from ordinary per-object values such as color.

Example conceptual key:

```text
material-family=basic
has-color-map=true
has-vertex-color=false
double-sided=false
skinning=false
instancing=false
```

Color changes update uniforms. They do not compile a new program.

### 13.7 Shader source ownership

Built-in shaders should be library resources controlled and versioned with the
renderer implementation. Custom shader strings belong to `ShaderMaterial`.

Shader preprocessing should initially be small and deterministic. A complex
include and macro language is a feature of its own and should not emerge
accidentally.

---

## 14. Texture Design

### 14.1 Public texture description

A texture describes:

- Image data or an image source.
- Width and height.
- Pixel format.
- Color-space interpretation.
- Minification and magnification filters.
- Horizontal and vertical wrap modes.
- Texture-coordinate offset, repeat, rotation, and rotation center.
- Mipmap policy.
- Flip policy if needed.
- Upload version.
- Disposal state or generation.

It does not contain an OpenGL texture identifier.

### 14.2 Color-space policy

JScene3D's internal working color space is linear sRGB. Public color creation
identifies the input encoding explicitly:

```java
Color.srgb(1.0f, 0.5f, 0.0f);
Color.srgb(0xff8000);
Color.linear(red, green, blue);
```

`Color` is an immutable value. There is no ambiguous `Color.rgb(...)` factory.
Alpha is represented separately and remains linear. Version 0.1 supplies the
small, universally recognizable constants `BLACK`, `WHITE`, `RED`, `GREEN`,
`BLUE`, `YELLOW`, `CYAN`, `MAGENTA`, and `GRAY`; `GRAY` corresponds to sRGB
`#808080`. Additional constants require demonstrated recurring use rather than
growing into a second named-color catalog. There is no `TRANSPARENT` constant
because transparency is not part of `Color`.

Textures distinguish:

- Base-color images, which default to sRGB and use an sRGB texture format so
  sampling converts them to the linear working space.
- Data textures, which default to linear and receive no color conversion.

The renderer requests an sRGB-capable default framebuffer and enables linear to
sRGB output conversion when available. Window or renderer diagnostics expose
whether that capability is actually active. Unsupported sRGB framebuffer
conversion produces an actionable compatibility warning rather than silently
claiming color-correct output.

### 14.3 Image loading

Version 0.1 provides `TextureLoader` in `jscene3d-lwjgl`:

```java
Texture earth = TextureLoader.load(Path.of("earth.png"));
```

It officially supports PNG and JPEG and uses LWJGL STB internally to decode to
RGBA8. The loader copies the decoded pixels once into core-owned Java storage,
then frees the STB native allocation before returning. No STB type or native
buffer appears in the public interface. The `Texture` retains its CPU pixels
until terminal close so more than one renderer can realize it independently.

Invalid or unsupported images fail with the source path and STB diagnostic.
Other formats that STB happens to decode are not part of the 0.1 compatibility
promise until JScene3D tests and documents them. A lower-level ownership-transfer
path is added only if measurement shows that the one-time copy is material.

### 14.4 Upload behavior

The renderer uploads a texture lazily on first use. Image, sampler, and
texture-coordinate transform changes have independent versions so each renderer
performs only the relevant work. Pixel changes re-upload the image and regenerate
mipmaps, sampler changes reapply OpenGL texture parameters, and transform changes
do neither.

Version 0.1 accepts one base image and does not accept caller-supplied mip
levels. `MipmapMode.GENERATE` is the default: each renderer generates the full
mipmap chain when it first realizes the texture and regenerates it once after a
pixel-data version change. The default minification filter is trilinear
filtering (`LINEAR_MIPMAP_LINEAR`), and the default magnification filter is
`LINEAR`. OpenGL 3.3 non-power-of-two textures follow the same behavior.

`MipmapMode.NONE` supports pixel art, interface artwork, and other cases that do
not want mipmaps. A texture with that mode must use a non-mipmap minification
filter. Texture construction or configuration commit rejects incompatible
filter and mipmap combinations with `IllegalArgumentException`; the renderer
never silently substitutes a different filter.

### 14.5 Texture-coordinate transforms

`Texture` owns finite offset, repeat, rotation, and rotation-center values. Its
Java interface provides scalar setters and `Vector2fc` copy-in overloads rather
than exposing directly mutable properties. Rotation is expressed in radians.
Vector and matrix reads copy into caller-owned JOML storage.

The texture caches a homogeneous three-by-three transform whenever one of these
values changes. `BasicMaterial` and `LambertMaterial` upload that matrix and
apply it before the renderer's image-orientation conversion. Geometry UV
attributes and `RaycastHit` texture coordinates remain unchanged. A transform
edit therefore has constant CPU cost and one small uniform upload per textured
draw, with no image upload, mipmap regeneration, or sampler update.

Repeats outside the unit interval produce tiling only when the corresponding
wrap mode is `REPEAT` or `MIRRORED_REPEAT`. `ShaderMaterial` remains an explicit
escape hatch and must apply any desired texture transform itself.

---

## 15. Mesh Design

`Mesh` extends `Object3D` and binds:

- One buffer geometry.
- One material initially.

Conceptual interface:

```java
public final class Mesh extends Object3D {
    public Mesh(BufferGeometry geometry, Material material);
    public BufferGeometry geometry();
    public void setGeometry(BufferGeometry geometry);
    public Material material();
    public void setMaterial(Material material);
}
```

Multiple materials and geometry groups should wait until the single-material
path is stable. They affect render-list construction, draw ranges, material
indexing, and sorting.

The mesh owns references to geometry and material, not their exclusive
lifecycle. Multiple meshes may share them.

`Line` and `LineSegments` follow the same shared-resource and lifecycle model.
`Line` interprets successive geometry elements as one connected strip;
`LineSegments` interprets successive pairs as independent segments and rejects
odd draw-range counts. Both retain `LineBasicMaterial`, support indexed and
non-indexed geometry, and participate in transforms, visibility, frustum
culling, opaque or transparent ordering, and depth state.

`AxesHelper`, `GridHelper`, and `BoxHelper` are renderer-independent
`LineSegments` scene
objects. `AxesHelper` generates red, green, and blue positive coordinate axes;
`GridHelper` generates a configurable XZ reference grid; `BoxHelper` generates
world-axis-aligned bounds around visible renderable geometry in a target
subtree and refreshes explicitly after target changes. Unlike ordinary lines,
each helper owns and closes its generated geometry and material. Replacing
either resource is unsupported because doing so would make lifecycle ownership
ambiguous.

---

## 16. Renderer Public Interface

### 16.1 Desired depth

The renderer should expose a compact interface while hiding significant
implementation behavior.

Conceptual public shape:

```java
public final class Renderer implements AutoCloseable {
    public static Renderer create(Window window, RendererOptions options);

    public void render(Scene scene, Camera camera);
    public void setViewport(int x, int y, int width, int height);
    public void setClearColor(Color color, float alpha);
    public RendererInfo info();
    public boolean isClosed();

    @Override
    public void close();
}
```

### 16.2 Renderer invariants

- The renderer is associated with one OpenGL context.
- Exactly one renderer may own a JScene3D-created context.
- A renderer cannot move to another context.
- Rendering occurs on the context-owning thread.
- Context-dependent renderer operations ensure that the associated window's
  context is current; callers do not switch contexts manually.
- A closed renderer cannot render.
- Scene mutation during `render` is unsupported unless performed by documented
  callbacks.
- Renderer-owned OpenGL resources are released when the renderer closes.
- The renderer exclusively owns the OpenGL state of its context for its entire
  lifetime.
- Applications must not issue direct LWJGL or OpenGL calls against a
  JScene3D-managed context. Doing so results in unspecified behavior because it
  may invalidate renderer state caches.
- The renderer neither preserves nor restores arbitrary external OpenGL state.
- `ShaderMaterial` is the supported version 0.1 customization boundary. A state
  invalidation or raw-interop interface may be added later only for a concrete
  use case.

### 16.3 Renderer options

Initial options may include:

- Debug validation enabled.
- Shader error checking enabled.
- Automatic clear enabled.
- Clear color and alpha.
- Frustum culling enabled.
- Object sorting enabled.
- Vertical synchronization belongs to the window, not renderer.

Avoid an oversized options object with unimplemented flags.

### 16.4 Renderer information

`RendererInfo` is a stable container that separates per-frame rendering
measurements from persistent renderer-owned resource counts:

```java
RendererInfo info = renderer.info();
RenderStatistics statistics = info.statistics();
ResourceStatistics resources = info.resources();
```

`RenderStatistics` exposes:

- Frame number.
- Draw calls.
- Rendered triangles.
- Rendered line segments.
- Visible mesh count.
- Culled mesh count.
- Visible line-object count.
- Culled line-object count.
- Buffer upload count and bytes uploaded for the frame.
- Texture upload count and bytes uploaded for the frame.

`ResourceStatistics` exposes:

- Active geometry resources.
- Active texture resources.
- Compiled program count.

All three objects are stable read-only live views. This separation prevents
resource lifetime information from being confused with counters reset at the
start of each rendered frame. The diagnostics are important for both users and
automated performance tests.

---

## 17. Render Pipeline

The initial render call should execute a deterministic sequence.

### 17.1 Validate invocation

- Verify renderer is open.
- Verify current thread.
- Verify context availability where practical.
- Verify non-null scene and camera.
- Verify camera projection state.

### 17.2 Synchronize framebuffer and viewport state

- Obtain framebuffer pixel dimensions from the window or explicit render target.
- Apply the current viewport.
- Avoid using logical window dimensions on high-density displays.

### 17.3 Clear requested buffers

- Clear color according to renderer or scene settings.
- Clear depth.
- Clear stencil only if the renderer requested and created it.

### 17.4 Update transforms

- Update the scene's world matrices from the root.
- Update the camera world matrix.
- Calculate the view matrix as the inverse camera world matrix.
- Ensure the projection matrix is current.

### 17.5 Build the view frustum

- Multiply projection and view matrices into a view-projection matrix.
- Extract or update frustum planes.
- Reuse storage rather than allocating per frame.

### 17.6 Traverse the scene

For each node:

- Skip invisible subtrees according to visibility semantics.
- Apply camera layer filtering if layers are supported.
- Identify renderable meshes and line objects.
- Validate geometry, topology, and material state.
- Transform geometry bounds to world space.
- Apply frustum culling unless disabled.
- Create or reuse a render item.

### 17.7 Partition render items

Maintain separate reusable lists for:

- Opaque objects.
- Transparent objects.

Opaque objects are generally sorted to reduce program, material, geometry, and
state changes while preserving explicit render-order constraints.

Transparent objects are generally sorted back-to-front. The limitations of
object-level transparency sorting must be documented.

### 17.8 Resolve programs and GPU resources

For each render item:

- Resolve the shader variant.
- Compile and link the program if absent.
- Resolve geometry GPU state.
- Upload new or changed attributes.
- Resolve textures.
- Upload new or changed texture data.
- Resolve uniform locations and binding metadata.

### 17.9 Bind frame, camera, object, and material state

Suggested conceptual categories:

```text
Frame uniforms
  time later
  framebuffer size later

Camera uniforms
  view matrix
  projection matrix
  view-projection matrix if used
  camera world position if used

Object uniforms
  model matrix
  model-view matrix if used
  normal matrix if used

Material uniforms
  color
  opacity
  texture samplers
  custom uniforms
```

Do not calculate matrices that a particular program does not use unless the
simplicity is justified and measured.

### 17.10 Apply render state

The state cache should minimize redundant calls for:

- Active program.
- Vertex array.
- Blend enabled and blend function.
- Depth test and function.
- Depth write mask.
- Face culling and front-face convention.
- Active texture unit and bound texture.

### 17.11 Submit draw calls

- Use indexed drawing when an index buffer exists.
- Use non-indexed drawing otherwise.
- Respect draw ranges.
- Update renderer statistics.

### 17.12 Process deferred disposal

Delete resources only while the correct OpenGL context is current and on its
owning thread. Disposal requests from scene objects may need to be queued until
this point.

### 17.13 Reset frame-local storage

Reuse render-list and scratch storage for the next frame without retaining
references that prevent intended cleanup.

---

## 18. Renderer-Internal Resource Management

### 18.1 BufferGeometry store

The geometry store maps public geometry identity to context-specific GPU state.

It tracks:

- VAO.
- Attribute VBOs.
- Optional index EBO.
- Uploaded attribute versions.
- Uploaded index version.
- Vertex layout for each program or a compatible layout strategy.
- Last-used frame for diagnostics or later eviction.
- Disposal generation.

### 18.2 Texture store

The texture store tracks:

- OpenGL texture object.
- Uploaded image version.
- Uploaded parameter version.
- Mipmap state.
- Pixel format decisions.
- Last-used frame.
- Disposal generation.

### 18.3 Program cache

The program cache tracks:

- Stable program key.
- Compiled vertex and fragment shaders during linking.
- Linked program.
- Attribute locations.
- Uniform locations and expected types.
- Compile and link diagnostics.
- Last-used frame.

Individual shader objects should normally be deleted after successful linking if
they are no longer needed.

### 18.4 Context ownership

All renderer-internal OpenGL identifiers belong to the renderer/context that
created them. A public geometry can be rendered by multiple renderers, but each
renderer creates its own GPU realization.

### 18.5 Disposal model

The design must satisfy all of these cases:

- Renderer shutdown deletes all resources it created.
- An application can release a large geometry before renderer shutdown.
- Shared geometry is not deleted merely because one mesh is removed from a
  scene.
- Deletion runs with the correct context current.
- Use after public disposal produces a clear failure or documented reactivation
  behavior.
- Garbage collection is not the only cleanup mechanism.

`BufferGeometry`, `Texture`, and `Material` are application-owned Resource
Descriptions implementing `AutoCloseable`. Calling `close()` is terminal and
notifies every renderer that realized the description. Each renderer queues its
own context-bound GPU Realization for deletion on the context-owning thread.

All public `close()` operations are idempotent. The first call permanently ends
the lifetime and schedules or performs cleanup at most once; later calls are
no-ops. Each closeable type exposes `isClosed()`. Other operations fail with
`IllegalStateException` after closure. Thread-affinity requirements still apply
to the first `Renderer.close()` and `Window.close()` call.

Removing a mesh does not close its shared descriptions. Attempting to render a
closed description fails clearly. Closing a renderer deletes every remaining
realization regardless of description state. Version 0.1 does not expose a
per-renderer `release(resource)` operation.

---

## 19. Shader System

### 19.1 Initial GLSL target

Use a GLSL version compatible with OpenGL 3.3 Core. Built-in shaders must not
use deprecated fixed-function constructs.

### 19.2 Compilation behavior

Shader compilation must:

- Include stage name in errors.
- Include material or program-key context.
- Include the driver compilation log.
- Include numbered source or enough source context to locate failures.
- Delete partially created shader and program objects after failure.

### 19.3 Program linking behavior

Program linking must:

- Validate expected attributes and uniforms.
- Include the link log on failure.
- Avoid leaving partial OpenGL objects alive.
- Cache successful results by a stable structural key.

### 19.4 Built-in bindings

Built-in attribute names should be standardized:

```text
position
normal
uv
color
```

Built-in uniform names may be standardized internally without becoming public.
The five automatic transform uniforms below are the deliberate exception: their
names and meanings are part of the public `ShaderMaterial` contract.

### 19.5 Custom shader contract

JScene3D automatically supplies the following reserved uniforms when they are
active in a linked `ShaderMaterial` program:

```glsl
uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
uniform mat3 normalMatrix;
```

Their meanings are:

- `modelMatrix`: object-local coordinates to world coordinates.
- `viewMatrix`: world coordinates to camera-view coordinates.
- `projectionMatrix`: camera-view coordinates to clip coordinates.
- `modelViewMatrix`: `viewMatrix * modelMatrix`.
- `normalMatrix`: inverse transpose of the upper-left 3-by-3 portion of
  `modelViewMatrix`.

Applications may omit any unused automatic uniform. If declared, it must have
the exact type above; the application cannot override its value. All other
uniforms are application-declared and supplied explicitly. The renderer binds
only uniforms active in the linked program and fails fast with a descriptive
error when a reserved name has the wrong type.

---

## 20. Window and Display Lifecycle

### 20.1 Separation from renderer

Window management and rendering are related but distinct components.

The window owns:

- GLFW initialization participation.
- Native window handle.
- OpenGL context creation.
- Context activation.
- Visibility through `show()` and `hide()`.
- Explicit frame publication through `swapBuffers()`.
- Window-close state.
- Window and framebuffer sizes.
- Input callback registration and polling.
- Swap interval.

The renderer owns:

- OpenGL rendering state.
- GPU resource realization.
- Scene rendering.

### 20.2 GLFW global lifecycle

GLFW has process-wide lifecycle concerns. Multiple windows must not cause one
window close to terminate GLFW while another window remains active.

Version 0.1 supports multiple independent window-renderer pairs operated
sequentially on one render thread. Use a small internal runtime manager with
reference counting so closing one window does not disrupt another and GLFW
terminates only after the final window closes. This is an internal seam, not a
public singleton.

Each window owns an independent, unshared OpenGL context and exactly one
renderer may own that context. The same public Resource Description may be used
by multiple renderers, each of which creates and owns a separate GPU
Realization. Version 0.1 does not support shared OpenGL objects between
contexts, multiple renderers for one context, moving a renderer between
contexts, caller-created contexts, background upload contexts, or concurrent
render threads.

### 20.3 Window options and context creation

`WindowOptions` is a final, immutable value with value equality. Its builder
contains only the version 0.1 choices needed when creating a normal window:

- Logical size, defaulting to `1280` by `720`.
- Title, defaulting to `JScene3D`.
- Initial `VerticalSync`, defaulting to `ENABLED`.
- Preferred default-framebuffer MSAA samples, defaulting to `0`.

`WindowOptions.defaults()` returns the complete default value, while
`WindowOptions.builder()` creates a mutable builder whose `build()` method
returns an immutable value. Width and height must be positive. The title must
not be `null` or contain a null character, although it may be empty. The MSAA
sample count must be non-negative, and `VerticalSync` must not be `null`.
Invalid values fail when supplied rather than being deferred until native
window creation.

Windows are resizable in version 0.1 and always start hidden. Resizability and
initial visibility are therefore behavior guarantees rather than options. Raw
color, depth, or stencil bit counts, fullscreen modes, decorations, native
handles, and debug-context switches remain internal until a concrete example
requires a public choice.

Initial hints should request:

- OpenGL 3.3.
- Core profile.
- Forward-compatible behavior where required.
- Library-selected color, depth, and stencil properties.
- A preferred default-framebuffer MSAA sample count from
  `WindowOptions.Builder.preferredFramebufferSampleCount(int)`.
- Initial invisibility so context configuration can finish before the window is
  shown.

The MSAA request is non-negative and defaults to `0`, meaning disabled. The
Solar System Viewer requests `4`. GLFW treats the sample count as a soft
framebuffer hint, so JScene3D queries and exposes the actual count after context
creation rather than claiming the request was satisfied. A different actual
count is not a creation failure. Multisampled off-screen render targets are a
later, separate feature.

The low-frequency diagnostic `Window.framebufferSampleCount()` reports the
actual sample count of the created default framebuffer. Most applications do
not need to query it because the renderer manages multisampling internally.

After making the context current, initialize LWJGL OpenGL capabilities before
calling OpenGL functions. Applications explicitly call `Window.show()` once
they are ready for the native window to become visible. `Window.hide()` changes
visibility without changing the window or context lifetime.
`Window.setVerticalSync(VerticalSync)` may change the initial option later
without recreating the window.

The version 0.1 public contract is:

```java
public final class Window implements AutoCloseable {
    public static Window create(WindowOptions options);
    public static Window create(int width, int height, String title);
    public static void pollEvents();

    public void show();
    public void hide();
    public boolean isVisible();
    public String title();
    public void setTitle(String title);

    public int width();
    public int height();
    public int framebufferWidth();
    public int framebufferHeight();
    public float framebufferAspectRatio();
    public boolean framebufferSizeChanged();
    public int framebufferSampleCount();

    public InputState input();
    public VerticalSync verticalSync();
    public void setVerticalSync(VerticalSync verticalSync);

    public boolean shouldClose();
    public void requestClose();
    public void swapBuffers();
    public boolean isClosed();

    @Override
    public void close();
}
```

The three-argument `create` method applies all remaining defaults. The input
view is stable and read-only. Native instance operations execute only on the
creating thread, and `swapBuffers()` makes the associated context current
internally. Native handles and manual context activation are never public.

### 20.4 Framebuffer size

The window must expose framebuffer pixel dimensions separately from logical
window dimensions. Viewports use framebuffer dimensions. A minimized window
may have a zero-width or zero-height framebuffer; rendering is skipped in that
state, and `framebufferAspectRatio()` throws `IllegalStateException` when a
ratio cannot be computed.

### 20.5 Event polling and input state

`Window.pollEvents()` is a process-wide operation and returns `void`. It asks
GLFW to process pending events for every open JScene3D window; internal
callbacks then update the state owned by the corresponding window. Applications
inspect `window.input()` after polling rather than receiving an allocated event
collection.

`InputState` is a stable, read-only view that exposes:

- Whether a key or mouse button is currently held.
- Whether a key or mouse button was pressed during the latest poll.
- Whether a key or mouse button was released during the latest poll.
- Current pointer position in logical window coordinates.
- Pointer movement accumulated during the latest poll.
- Scroll movement accumulated during the latest poll.

At the start of each poll, transient pressed, released, pointer-delta,
scroll-delta, and window-change flags are cleared for every open window.
Callbacks populate those values while pending events are processed. Held input
state and current pointer position persist between polls. Key-repeat
notifications do not create additional press transitions; continuous behavior
uses held state.

Window-specific changes remain queryable from their `Window`, including
`shouldClose()` and `framebufferSizeChanged()`. These queries do not consume the
state, so more than one application component may inspect the same polling
cycle.

Version 0.1 does not expose a general event list, public native callbacks, text
input, file-drop events, or ordered event replay. Those interfaces should be
added only when a concrete example requires their distinct semantics.

### 20.6 Main-thread constraints

The library must document macOS first-thread requirements and provide build-tool
example configurations. Hiding the requirement completely may not be possible
because JVM launch options are selected before library code runs.

### 20.7 Cleanup

Window close should:

- Release callbacks.
- Destroy the native window.
- Decrement shared GLFW runtime ownership.
- Terminate GLFW only when appropriate.

Renderer resources should be closed before destroying the context that owns
them. `Window.close()` fails with `IllegalStateException` while its Renderer is
still open rather than destroying a context beneath live GPU resources. The
recommended application close order must be explicit. `close()` itself is
idempotent; after it succeeds, only `close()` and `isClosed()` remain valid.

`Window.swapBuffers()` publishes the completed back buffer and is deliberately
separate from `Window.show()`: visibility normally changes once, while buffers
are swapped once per displayed frame. `Renderer.render(...)` does not swap
buffers implicitly, allowing an application to perform multiple render passes
before publishing the frame.

---

## 21. Application Loop

The renderer should render one frame; it should not impose the application loop.

Recommended normal loop:

```java
while (!window.shouldClose()) {
    Window.pollEvents();

    double now = clock.nowSeconds();
    double delta = now - previous;
    previous = now;

    update(delta);
    renderer.render(scene, camera);
    window.swapBuffers();
}
```

An optional high-level `Application` component may later provide:

- Initialization hook.
- Fixed simulation step.
- Variable render interpolation.
- Resize hook.
- Input hook.
- Structured shutdown.

It should be built after the lower-level window and renderer lifecycle is
proven.

### 21.1 Orbit controls

Version 0.1 provides `OrbitControls` in the exported
`io.github.glynch.jscene3d.controls` package of `jscene3d-lwjgl`. Its dependency
on `Window` and `InputState` keeps it out of renderer-independent
`jscene3d-core`.

The control supports unparented perspective and orthographic cameras, a
configurable world-space target, mouse and keyboard orbit and pan, perspective
dolly, orthographic zoom, independent enable flags and speeds, distance, zoom,
polar, and azimuth limits, optional damping, optional automatic rotation,
screen-space panning, programmatic movement, and save/reset state. `update()`
reads input already accumulated by `Window.pollEvents()` and returns whether it
changed the camera. It registers no native callback and owns no closeable
resource.

Touch input, remappable bindings, zoom-to-cursor behavior, target-radius limits,
input events, and parented cameras are deferred until Feature Examples make
their interface and behavior requirements concrete.

---

## 22. Threading Model

### 22.1 Initial rule

Use a single render thread. The same thread should:

- Own the current OpenGL context.
- Construct or initialize the renderer.
- Call `render`.
- Process GPU resource disposal.
- Close the renderer.

### 22.2 Scene mutation

Scene objects are not automatically thread-safe. Applications must not mutate
scene data while it is being traversed or rendered.

### 22.3 Background work

Background threads may later perform CPU-only work such as:

- File reading.
- Image decoding.
- BufferGeometry generation.
- Asset parsing.

Transfer to render-visible state must occur through a documented handoff. OpenGL
upload remains on the render thread unless a deliberately designed
shared-context system is introduced.

### 22.4 Why not make everything thread-safe

Automatic synchronization throughout the scene graph would:

- Increase interface complexity.
- Add render-path overhead.
- Make mutation ordering harder to reason about.
- Still not solve OpenGL context affinity.

Explicit ownership is preferable initially.

---

## 23. Error Handling and Diagnostics

### 23.1 Exception categories

Potential library-specific exceptions include:

- Window initialization failure.
- OpenGL context creation failure.
- Unsupported capability failure.
- Shader compilation failure.
- Program link failure.
- Invalid geometry failure.
- Resource lifecycle failure.
- Wrong-thread renderer access.

Avoid creating a large exception hierarchy before callers need distinct recovery
behavior.

### 23.2 Error-message requirements

Errors should answer:

- What operation failed?
- Which object, material, shader, or resource was involved?
- What did the underlying platform report?
- What can the caller do next?

### 23.3 Debug labels

Public resources should support optional names or labels. Renderer diagnostics
can include them without forcing users to interpret generated numeric
identities.

### 23.4 OpenGL diagnostics

Where supported, debug callbacks may be used in development. The design must not
assume debug contexts are available on every platform, particularly macOS.

### 23.5 Validation policy

Version 0.1 keeps contract validation mandatory. Null, lifecycle, thread,
hierarchy, scalar, attribute-shape, draw-range, and index-bound checks cannot be
disabled. Whole-buffer checks run when data is created or committed after an
edit, and version tracking prevents unchanged data from being rescanned during
rendering. OpenGL debug callbacks and unusually expensive diagnostic detail may
remain renderer options, but a general validation-off mode is added only if a
benchmark demonstrates material cost that cannot be moved out of the hot path.

---

## 24. Resource and Memory Policy

### 24.1 Java heap allocations

Allowed and expected during:

- Scene construction.
- Asset loading.
- BufferGeometry generation.
- First use of a shader variant.
- First GPU realization of a resource.

Avoided during a stable repeated render loop:

- One object per render item per frame.
- New matrices per mesh.
- New collections per traversal.
- Repeated string construction for program keys.
- Repeated uniform lookup.

### 24.2 Native memory

Use scoped native allocations where data is consumed immediately. Persistent
native allocations require an explicit owner and cleanup path.

Do not return a view backed by a popped memory stack.

### 24.3 GPU memory

Track enough information to report active geometry and texture resources.
Renderer close must delete all objects it created.

### 24.4 Finalizers and cleaners

Do not depend on finalizers. A `Cleaner` may emit leak diagnostics or act as an
emergency safety net, but it should not be the normal mechanism for deleting
context-bound OpenGL resources.

---

## 25. Render Ordering and Transparency

### 25.1 Opaque sorting

The opaque sort key includes:

1. Explicit render order.
2. Program key or program identity.
3. Material identity.
4. BufferGeometry identity.
5. Stable object identity.

Explicit render order is compared first. The remaining batching keys may be
profiled, but their final traversal-order tie-breaker remains deterministic.

### 25.2 Transparent sorting

The transparent sort key includes:

1. Explicit render order.
2. Camera-space depth, back-to-front.
3. Stable object identity as a tie-breaker.

Object-level sorting does not solve intersecting or internally self-overlapping
transparent geometry. Document this limitation.

### 25.3 Stable ordering

Equal sort keys must not cause random frame-to-frame reordering. Stable identity
or insertion order can act as a final tie-breaker.

---

## 26. Frustum Culling

Initial frustum culling should:

- Use geometry bounding spheres.
- Transform the sphere into world space.
- Account conservatively for object scale.
- Be individually disableable on objects that cannot provide reliable bounds.
- Record culled counts in renderer information.

Renderable objects enable culling by default through the Java-style
`isFrustumCullingEnabled()` and `setFrustumCullingEnabled(boolean)` operations
inherited from `Object3D`. The setting applies only to the object itself; it does
not hide or disable culling for descendants. Non-renderable groups retain the
setting for interface consistency and future renderable `Object3D` subtypes,
but the renderer does not test groups against the frustum.

Non-uniform scale can conservatively use the maximum axis scale for the sphere
radius.

Required tests include:

- Object fully inside.
- Object fully outside each frustum plane.
- Object intersecting a plane.
- Translated object.
- Uniformly scaled object.
- Non-uniformly scaled object.
- Culling disabled.

---

## 27. Build and Dependency Strategy

### 27.1 Build-tool requirements

The project uses Maven through a checked-in Maven Wrapper and contains no Gradle
build. The wrapper pins a stable Maven 3.9 release until Maven 4 reaches general
availability and a deliberate migration is justified. The Maven reactor must
support:

- A checked-in wrapper.
- Java toolchains or an explicit JDK baseline.
- JUnit tests.
- OS- and architecture-specific LWJGL native dependencies.
- Example execution tasks with macOS JVM arguments.
- Source and documentation artifacts if published.
- Reproducible dependency versions.

The mandatory ordinary verification command is always:

```shell
./mvnw clean verify
```

The `clean` phase is not optional: a successful verification must not depend on
stale compiled classes, generated sources, resources, reports, or test output.
Every reliably machine-checkable rule in `CODING_STANDARDS.md` runs within this
lifecycle. CI checks source formatting and never rewrites a contributor's
branch.

### 27.2 Suggested dependency set

Initial dependencies should remain small:

- LWJGL core.
- LWJGL GLFW.
- LWJGL OpenGL.
- LWJGL STB.
- JOML.
- JUnit Jupiter.
- A lightweight assertion library only if it materially improves tests.

### 27.3 Native classifiers and support qualification

During private foundation development, macOS ARM64 on the maintainer's M1
MacBook Pro is the only Verified Platform. The initial Provisional Platforms
are:

- Windows x86-64.
- Linux x86-64 under both X11 and Wayland.
- macOS x86-64.
- macOS ARM64.

Windows ARM64 and Linux ARM64 remain experimental, and 32-bit platforms are
unsupported.

The repository remains private during foundation work and consumes no GitHub
Actions minutes. Once the Public Preview Gate is reached, the repository becomes
public and the standard hosted-runner matrix is enabled. A Provisional Platform
becomes a Supported Platform only after its native dependencies resolve and a
repeatable test creates an OpenGL context, renders deterministic pixels, reads
them back successfully, and closes all tracked resources. Compilation alone is
not qualification. The final 0.1 release notes list only platforms that passed
this gate.

### 27.4 Java baseline

Java 21 is the minimum runtime and compilation target. Continuous integration
tests both Java 21 and Java 25. Foreign-memory features are not necessary because
LWJGL already provides the native interface, so the implementation does not
require Java 25-only APIs without a demonstrated need. Raising the minimum is
reserved for a major-version compatibility boundary.

The implementation should not drift to a newer Java feature without updating the
recorded baseline.

### 27.5 Publication and versioning

The first public artifact release is `0.1.0` and publishes
`io.github.glynch:jscene3d-core`, `io.github.glynch:jscene3d-lwjgl`, and the
optional `io.github.glynch:jscene3d-gui` to Maven Central through the Central
Publisher Portal. All published JScene3D artifacts use one lockstep version.

JScene3D follows Semantic Versioning with a stricter pre-1.0 policy:

- `0.1.x` patch releases contain only backward-compatible bug and security
  fixes.
- `0.2.0`, `0.3.0`, and later pre-1.0 minor releases may add features and may
  contain explicitly approved breaking changes with migration notes and prior
  deprecation where practical.
- `1.0.0` begins the normal stable public-interface compatibility promise.

Revapi prevents accidental source or binary incompatibility in patch releases.
Published versions are immutable. Release bundles include source and Javadoc
JARs, required POM metadata, checksums, and GPG signatures. Pre-1.0 deployments
require manual approval in the Central Portal rather than automatic publishing,
and each release receives an immutable signed tag such as `v0.1.0`.

The release command is:

```shell
./mvnw clean deploy -Prelease
```

### 27.6 License

JScene3D is licensed under the Apache License, Version 2.0, with SPDX identifier
`Apache-2.0`. The repository includes the official license text, published POMs
declare it, original source files use the project's short SPDX header, and
release verification checks the declared project license.

---

## 28. Testing Strategy

### 28.1 Test pyramid

#### Pure unit tests

No GLFW or OpenGL context required:

- Scene hierarchy invariants.
- Transform composition.
- Camera projection validation.
- Bounds calculation.
- Frustum tests.
- BufferGeometry attribute validation.
- Program-key generation.
- Render-list ordering.
- Disposal state transitions.

#### Renderer-internal tests with fakes

Use internal seams only where they provide real test leverage:

- Render-list construction from a scene.
- State-transition decisions.
- Resource-version comparison.
- Uniform binding plans.

Do not mirror every OpenGL function in a giant public fake merely to count
calls. Prefer extracting deterministic planning logic and testing its returned
results.

#### OpenGL integration tests

With a real context:

- Context initialization.
- Shader compilation and linking.
- Attribute upload.
- Indexed and non-indexed drawing.
- Texture upload.
- Resource deletion.
- Resize and viewport behavior.
- Basic framebuffer readback.

#### Visual regression tests

Render small deterministic scenes and read pixels or save artifacts. Exact byte
equality may differ across drivers, so use carefully selected assertions:

- Known center pixel color.
- Expected non-background region.
- Per-channel tolerance.
- Simple geometry without unstable antialiasing at edges.

### 28.2 Test fixtures

Create reusable fixtures for:

- A single triangle.
- An indexed quad.
- Parent-child transforms.
- Opaque overlap.
- Transparent overlap.
- Textured quad.
- Invalid shaders.
- Invalid geometry.

### 28.3 Leak checks

At renderer close, tests should assert that tracked resource stores are empty
and deletion paths executed. Native leak detection options should be enabled in
selected test runs if LWJGL supports them in the chosen configuration.

### 28.4 Cross-platform verification

Separate pure tests from context-dependent tests so contributors can run most
tests anywhere. During private development, run the complete suite locally on
macOS ARM64. After the Public Preview Gate, run compilation, unit tests, native
resolution, and context-dependent smoke tests on each Provisional Platform.
Only a platform with a passing deterministic render and cleanup test may be
claimed as supported in a release.

---

## 29. Performance Strategy

### 29.1 Measure before adding complexity

Do not introduce dirty graphs, object pools, custom collections, or
multithreaded submission based only on intuition. Establish benchmarks and
renderer statistics first.

### 29.2 Suggested benchmarks

- World-matrix update for a deep hierarchy.
- World-matrix update for a wide hierarchy.
- Render-list construction for many meshes.
- Opaque sorting.
- Transparent sorting.
- Program-key calculation.
- BufferGeometry upload of static and dynamic buffers.
- Repeated rendering with no resource changes.

### 29.3 Steady-state checks

For a fixed scene after warm-up:

- No shader compilation occurs.
- No geometry upload occurs.
- No texture upload occurs.
- Render-list capacity remains stable.
- Per-frame allocation approaches zero.
- Draw counts remain deterministic.

### 29.4 Optimization order

Prefer optimization in this order:

1. Eliminate unintended repeated GPU uploads.
2. Eliminate repeated shader compilation.
3. Reduce redundant OpenGL state changes.
4. Improve culling.
5. Reduce render-list allocation.
6. Measure transform update cost.
7. Consider batching and instancing when real workloads justify them.

---

## 30. Initial User-Facing Example

The following is an interface target, not committed implementation code:

```java
public final class RotatingCube {
    public static void main(String[] args) {
        WindowOptions windowOptions = WindowOptions.builder()
            .size(1280, 720)
            .title("Rotating Cube")
            .verticalSync(VerticalSync.ENABLED)
            .preferredFramebufferSampleCount(4)
            .build();

        try (
            Window window = Window.create(windowOptions);
            Renderer renderer = Renderer.create(
                window,
                RendererOptions.defaults()
            );
            BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
            BasicMaterial material = new BasicMaterial(
                Color.srgb(0.2f, 0.7f, 1.0f)
            )
        ) {

            Scene scene = new Scene();
            scene.setBackground(Color.srgb(0.02f, 0.02f, 0.04f));

            PerspectiveCamera camera = new PerspectiveCamera(
                (float) Math.toRadians(60.0),
                window.framebufferAspectRatio(),
                0.1f,
                100.0f
            );
            camera.setPosition(0.0f, 0.0f, 4.0f);

            Mesh cube = new Mesh(geometry, material);
            scene.add(cube);
            window.show();

            while (!window.shouldClose()) {
                Window.pollEvents();

                if (window.framebufferSizeChanged()) {
                    camera.setAspectRatio(window.framebufferAspectRatio());
                    renderer.setViewport(0, 0,
                        window.framebufferWidth(),
                        window.framebufferHeight());
                }

                cube.rotateY(0.01f);
                cube.rotateX(0.005f);

                renderer.render(scene, camera);
                window.swapBuffers();
            }
        }
    }
}
```

This example is useful as a design test. If the real interface requires users to
touch OpenGL, upload buffers manually, locate uniforms, or manage matrix
multiplication, the high-level component is too shallow.

---

## 31. Implementation Milestones

Calendar estimates are intentionally omitted. Actual duration depends heavily on
prior OpenGL knowledge, desired production hardening, platform matrix,
automation, and AI-agent throughput. Each milestone instead defines dependencies
and observable completion criteria.

### Milestone 0: Repository and build foundation

#### Repository foundation deliverables

- Build wrapper.
- Explicit Java baseline.
- LWJGL and JOML dependencies.
- Native dependency selection for development platforms.
- JUnit configuration.
- Formatting and static-analysis decision.
- Basic package structure.
- Continuous integration skeleton if desired.

#### Repository foundation acceptance criteria

- A clean checkout builds using only the wrapper and documented JDK.
- Tests execute even when no graphical context is available.
- Dependency versions are centralized.
- macOS example launch configuration can apply first-thread startup
  requirements.

### Milestone 1: Window and OpenGL context

#### Window lifecycle deliverables

- GLFW runtime lifecycle.
- Native window creation.
- OpenGL 3.3 Core context request.
- Current-context activation.
- LWJGL capability initialization.
- Framebuffer-size tracking.
- Explicit `show()` and `hide()` visibility control, with new windows hidden by
  default.
- Swap interval configuration.
- Explicit `swapBuffers()` frame publication.
- Process-wide event polling through `Window.pollEvents()`.
- Deterministic close.

#### Window lifecycle visible proof

- An example opens a window, clears it to a configured color, responds to
  resize, and exits cleanly.

#### Window lifecycle acceptance criteria

- Repeated create/close cycles work in tests where supported.
- Initialization failures include GLFW error details.
- Viewport follows framebuffer pixels, not only logical size.
- No renderer abstraction is required yet.

### Milestone 2: Scene graph and transforms

#### Scene graph deliverables

- `Object3D`.
- `Group`.
- `Scene`.
- Parent/child management.
- Position, quaternion, scale.
- Local and world matrix updates.
- Traversal.
- World-space queries.
- `lookAt` semantics.

#### Scene graph visible proof

- A non-rendering example or test constructs a multi-level hierarchy and
  prints/verifies expected world positions.

#### Scene graph acceptance criteria

- Cycle prevention is tested.
- Reparenting behavior is tested.
- Translation, rotation, and scale inheritance are tested.
- Traversal order is deterministic.
- Core tests require no GLFW or OpenGL.

### Milestone 3: Cameras

#### Camera deliverables

- Base camera state.
- Perspective camera.
- Orthographic camera.
- View matrix calculation.
- Projection validation.
- Resize example integration.

#### Camera acceptance criteria

- Known world points map to expected normalized device coordinates.
- Invalid projection parameters fail clearly.
- Camera parent transforms work.

### Milestone 4: BufferGeometry and material descriptions

#### BufferGeometry and material deliverables

- Float buffer attributes.
- Optional unsigned index data.
- BufferGeometry validation.
- BufferGeometry versions.
- Bounds.
- Basic material.
- Mesh.

#### BufferGeometry and material acceptance criteria

- BufferGeometry can be created and validated without a context.
- Shared geometry and material references are supported.
- Attribute mutation and update-version behavior are tested.
- No public object contains an OpenGL identifier.

### Milestone 5: Minimal static renderer

#### Static renderer deliverables

- Renderer construction and close.
- Built-in vertex and fragment shaders.
- Program compilation and linking.
- BufferGeometry GPU realization.
- VAO/VBO/EBO creation.
- Model, view, and projection uniform binding.
- Indexed and non-indexed draw paths.
- Clear and viewport behavior.
- Renderer statistics.

#### Static renderer visible proof

- Render a colored triangle through `Scene`, `Mesh`, `BufferGeometry`,
  `BasicMaterial`, `Camera`, and `Renderer.render`.

#### Static renderer acceptance criteria

- No example code calls OpenGL directly.
- Shader failures return useful diagnostics.
- A second frame with no changes performs no buffer upload or shader compile.
- Renderer close releases tracked objects.

### Milestone 6: Multiple objects and hierarchy rendering

#### Scene rendering deliverables

- Full scene traversal.
- Reusable render lists.
- Multiple meshes.
- Opaque sorting.
- Visibility inheritance.
- BufferGeometry and material sharing.
- Frustum culling.

#### Scene rendering visible proof

- Render multiple transformed cubes, including children inheriting parent
  movement.

#### Scene rendering acceptance criteria

- Hidden objects are not drawn.
- Culled objects are reflected in statistics.
- Shared geometry uploads once per renderer.
- Steady-state render-list operation avoids per-item allocation.

### Milestone 7: Textures and transparency

#### Texture and transparency deliverables

- Image loading.
- Texture description.
- Texture GPU realization.
- Sampler state.
- Texture-coordinate transforms.
- Basic color-space handling.
- Transparent render list.
- Blend and depth-write behavior.

#### Texture and transparency visible proof

- Render a textured cube, an interactively transformed texture, and a transparent
  object with deterministic ordering.

#### Texture and transparency acceptance criteria

- Shared textures upload once per renderer.
- Transform changes do not re-upload image data or reapply sampler state.
- Texture close/release behavior is verified.
- Unsupported formats fail clearly.
- Transparent limitations are documented.

### Milestone 8: Public 0.1 hardening

#### Public Preview Gate

The repository remains private during current 0.1 development. Repository
visibility is an owner-controlled release decision; completing a feature or
example never changes it automatically. A cross-platform GitHub Actions matrix
can be enabled when the repository becomes public and CI capacity is available.

#### Public 0.1 hardening deliverables

- Interactive Solar System Viewer Integration Example.
- Box, plane, sphere, and ring generators.
- Diffuse Lambert and specular Phong materials with all version 0.1 lights.
- `ShaderMaterial` escape hatch.
- Error and lifecycle documentation.
- Supported-platform documentation.
- Runnable examples.
- Unit and integration test separation.
- Performance baseline.
- Resource leak checks.
- Published artifact metadata if distribution is intended.

#### Public 0.1 hardening acceptance criteria

- A new user can build and run an example from a clean checkout.
- The public interface does not expose OpenGL state.
- Scene tests run headlessly.
- Renderer integration tests cover resource creation and deletion.
- The steady-state example performs no unintended uploads or shader
  compilations.
- All supported platforms have recorded verification.

---

## 32. Post-0.1 Feature Sequence

The first major post-0.1 feature block is correctly rendered static glTF 2.0
and GLB loading. It builds on the initial ambient and point lights, introduces
metallic-roughness PBR material behavior, then loads static scene hierarchy,
triangle meshes, textures, core PBR materials, and perspective or orthographic
cameras. It excludes animation, skinning, morph targets, lights stored in the
asset, and compression extensions until their corresponding runtime features
and focused examples exist.

After that block, a plausible sequence is:

1. Animation clips and mixers.
2. Skinning.
3. Morph targets.
4. Render targets and framebuffer management.
5. Shadow maps.
6. Instanced meshes.
7. Environment maps and image-based lighting.
8. Additional glTF extensions and optional compression integrations.
9. Additional material models when an example requires them.
10. Post-processing graph.
11. Additional renderer only if product requirements justify it.

The sequence should be adjusted by real user needs. For example, a
data-visualization library may prioritize lines, points, labels, and picking
ahead of shadows and PBR.

---

## 33. Scope-Based Effort Model

Instead of assigning a calendar duration before implementation velocity is
known, estimate the project from feature multipliers.

### 33.1 Low-to-moderate complexity foundation

- Build and native dependency wiring.
- One GLFW window and context.
- Scene graph.
- Perspective and orthographic cameras.
- Float geometry attributes.
- One built-in unlit material.
- Static opaque drawing.

### 33.2 Complexity multipliers

Each of the following expands multiple components rather than adding one isolated
class:

#### Multiple material models

Impacts shader variants, program keys, uniforms, documentation, examples, and
rendering tests.

#### Lighting

Impacts scene traversal, light collection, shader variants, normal matrices,
uniform packing, and material behavior.

#### Shadows

Introduces extra render passes, shadow cameras, render targets, depth materials,
culling differences, bias configuration, and extensive visual edge cases.

#### glTF

Introduces asset graphs, binary buffers, accessors, images, samplers, PBR
semantics, coordinate conventions, animations, skinning, morphing, and
compatibility tests.

#### Transparency

Introduces render-list partitioning, sorting policy, blend state, depth-write
decisions, and unavoidable documented limitations.

#### Post-processing

Introduces render targets, fullscreen passes, attachment formats, resizing,
ping-pong resources, and pass ordering.

#### Additional renderer

Forces previously internal OpenGL assumptions to become explicit cross-renderer
semantics. This is when a genuine renderer seam should be designed.

#### Broad platform support

Multiplies native packaging, CI, context behavior, shader-driver differences,
and support documentation.

### 33.3 Estimation protocol

After each milestone:

1. Record tasks completed.
2. Record agent/human iteration count.
3. Record tests and defects discovered.
4. Measure throughput using completed acceptance criteria, not lines of code.
5. Estimate only the next one or two milestones from observed throughput.
6. Revisit feature scope before extending the public interface.

This produces a more credible project forecast than extrapolating from generic
engine-development history.

---

## 34. Key Risks and Mitigations

### Risk: public model becomes a thin wrapper over OpenGL

**Symptoms:** users select GL constants, bind buffers, or manage program
state.
**Mitigation:** enforce the description-versus-realization rule and keep
renderer implementation packages internal.

### Risk: premature feature parity

**Symptoms:** many half-implemented materials, loaders, and helpers before one
path is robust.
**Mitigation:** complete vertical milestones with acceptance tests before adding
breadth.

### Risk: shader-variant explosion

**Symptoms:** compile delays, unstable keys, duplicate programs, hard-to-test
combinations.
**Mitigation:** begin with a minimal feature set, use structural keys, and
measure cache behavior.

### Risk: lifecycle leaks

**Symptoms:** GPU memory grows when scenes change; deletion occurs after context
destruction.
**Mitigation:** explicit renderer ownership, disposal queue, renderer
statistics, and integration tests.

### Risk: scene transforms become difficult to mutate safely

**Symptoms:** direct JOML mutation bypasses dirty flags.
**Mitigation:** initially recompose automatic local matrices during traversal;
optimize only after measurement.

### Risk: macOS behavior diverges

**Symptoms:** context failure, blank output, incorrect Retina viewport,
first-thread startup errors.
**Mitigation:** dedicated macOS launch configuration and verification from the
first window milestone.

### Risk: tests depend on a physical GPU

**Symptoms:** most logic cannot run in CI or fails differently across drivers.
**Mitigation:** keep scene and render-planning logic CPU-testable; use a smaller
integration suite for actual OpenGL.

### Risk: abstraction for future backends slows the first renderer

**Symptoms:** generic resource interfaces mirror OpenGL while no second
implementation exists.
**Mitigation:** keep the public model backend-neutral but leave renderer
internals concrete until another implementation is real.

### Risk: custom shaders undermine built-in invariants

**Symptoms:** missing attributes, conflicting uniform names, undefined render
state.
**Mitigation:** document a narrow `ShaderMaterial` contract and validate active
bindings after link.

### Risk: automatic cleanup is misunderstood

**Symptoms:** users assume removing a mesh frees shared resources or rely on GC
timing.
**Mitigation:** document ownership with examples and expose renderer resource
counts.

---

## 35. Architectural Decision Register

Each decision should eventually become a short architecture decision record with
context, choice, consequences, and alternatives.

### Decisions proposed by this blueprint

1. Use LWJGL 3 for GLFW and OpenGL bindings.
2. Use JOML for math.
3. Target OpenGL 3.3 Core initially.
4. Use a right-handed, Y-up coordinate system.
5. Keep scene descriptions independent of OpenGL identifiers.
6. Let the renderer own context-specific GPU resources.
7. Keep window lifecycle separate from frame rendering.
8. Use a single render thread initially.
9. Use controlled transform mutation with read-only accessors and dirty
   versions.
10. Begin with an unlit `BasicMaterial`.
11. Add `ShaderMaterial` as a constrained escape hatch.
12. Use lazy GPU realization and version-based updates.
13. Use explicit deterministic renderer close.
14. Partition opaque and transparent render lists.
15. Test scene behavior without a context.
16. Avoid a public multi-backend interface until a second renderer exists.
17. Publish separate core and LWJGL artifacts, with examples kept unpublished.
18. Make Resource Description `close()` terminal and let renderers queue deletion
    of their own GPU Realizations.
19. Expose JOML read-only interfaces and controlled mutation overloads rather
    than library-specific math wrappers.
20. Use meaningful mutation return values rather than universal fluent chaining
    or duplicate chaining aliases.
21. Name self-removal from the scene graph `detach()` and preserve the local
    transform.
22. Express every public angle, including camera field of view, in radians.
23. Limit `lookAt` to cameras in version 0.1, where it aims local negative Z at
    the target.
24. Provide perspective and orthographic cameras in version 0.1 with validated
    setters and automatic lazy projection updates.
25. Fail fast on non-finite or relationally invalid camera projection values.
26. Require Java 21 or later and test both Java 21 and Java 25.
27. Build exclusively with Maven through a checked-in Maven Wrapper.
28. Name the library JScene3D, use the base package
    `io.github.glynch.jscene3d`, and publish `jscene3d-core` and
    `jscene3d-lwjgl` under the Maven group `io.github.glynch`.
29. Automatically supply the five Three.js-style transform uniforms
    `modelMatrix`, `viewMatrix`, `projectionMatrix`, `modelViewMatrix`, and
    `normalMatrix` to custom shaders under a strict reserved-name contract.
30. Keep NIO buffers and mutable arrays out of the public geometry interface in
    version 0.1; copy construction arrays once and provide controlled scalar and
    zero-copy batch mutation with automatic version tracking.
31. Use glTF 2.0 and GLB as the primary asset-import formats under an explicit
    capability profile; keep legacy and specialist loaders optional, and treat
    native JScene3D Scene Persistence as a separate future concern.
32. Keep glTF loading out of version 0.1 and make correctly rendered static
    glTF/GLB loading, together with its minimum PBR and lighting prerequisites,
    the first major post-0.1 feature block.
33. Publish glTF support post-0.1 as the renderer-independent artifact
    `io.github.glynch:jscene3d-gltf`, depending on `jscene3d-core` without
    requiring `jscene3d-lwjgl` or an active graphics context.
34. Keep the repository private and macOS ARM64 as the sole Verified Platform
    during foundation development; make it public and enable the provisional
    desktop CI matrix when the interactive Solar System Viewer reaches its
    Public Preview Gate, promoting only platforms with passing rendering tests.
35. Include optional default-framebuffer MSAA in version 0.1, disabled by
    default; let the Solar System Viewer request four samples and expose the
    actual sample count supplied by the platform.
36. Publish `jscene3d-core`, `jscene3d-lwjgl`, and optional `jscene3d-gui` as
    the JPMS modules `io.github.glynch.jscene3d.core`,
    `io.github.glynch.jscene3d.lwjgl`, and `io.github.glynch.jscene3d.gui`, with
    intentional exports and both module-path and classpath consumer tests. Keep
    renderer-owned OpenGL resources behind a generic safe overlay interface.
37. Make every public `close()` idempotent while keeping closure terminal;
    expose `isClosed()`, perform cleanup at most once, and reject other
    operations after closure.
38. Publish version `0.1.0` to Maven Central, version all JScene3D artifacts in
    lockstep, preserve compatibility in `0.x` patch releases, reserve approved
    breaking changes for minor releases, and manually approve signed immutable
    releases through the Central Portal.
39. License JScene3D under the Apache License, Version 2.0.
40. Use linear sRGB as the internal working color space, require explicit sRGB
    or linear `Color` factories, decode base-color textures through sRGB formats,
    and request sRGB conversion for default-framebuffer output.
41. Load PNG and JPEG textures in `jscene3d-lwjgl` through STB, copy decoded
    RGBA8 pixels once into core-owned storage, free native decoding memory before
    return, and expose no native buffers publicly.
42. Give each renderer exclusive ownership of its context's OpenGL state for its
    lifetime; direct OpenGL calls against that context are unsupported, and the
    renderer does not preserve or restore external state.
43. Maintain local and world matrices automatically through controlled
    transform mutation and dirty versions; expose no manual matrix-update method
    or automatic-update switch in version 0.1.
44. Generate mipmaps per renderer by default and use trilinear minification;
    permit an explicit no-mipmap mode, accept no caller-supplied mip levels in
    version 0.1, and fail fast on incompatible sampler configuration.
45. Support multiple independent window-renderer pairs sequentially on one
    render thread, with one renderer per JScene3D-created unshared context and
    independent GPU realizations; exclude context sharing and concurrent render
    threads from version 0.1.
46. Provide perspective and orthographic `OrbitControls` in `jscene3d-lwjgl`
    with desktop mouse and keyboard navigation, optional damping and automatic
    rotation, explicit targets and limits, and no separate callback or lifecycle
    ownership; defer touch, remapping, zoom-to-cursor behavior, input events, and
    parented cameras.

### Decisions still required

No unresolved architecture decision currently blocks version 0.1
implementation. Release-readiness checks may still expose concrete decisions
that must be resolved before publication.

---

## 36. Suggested AI-Agent Implementation Protocol

This project is well suited to incremental agent implementation if the agent is
constrained by explicit decisions and acceptance tests.

For each milestone, the agent should:

1. Read this blueprint and any accepted architecture decision records.
2. Inspect the current repository before proposing changes.
3. State the exact vertical behavior being added.
4. Identify affected public interfaces and invariants.
5. Add or update tests before or alongside implementation.
6. Implement the smallest complete behavior that reaches the visible proof.
7. Run pure tests.
8. Run context-dependent tests where the environment permits.
9. Run the milestone example.
10. Inspect resource statistics and error paths.
11. Report deviations from the blueprint rather than silently inventing new
    conventions.
12. Record decisions that will constrain later features.

### Agent guardrails

The agent should not:

- Add public abstractions solely for speculative future backends.
- Expose OpenGL identifiers in public scene types.
- Add a new material family without defining shader and state consequences.
- Depend on finalization for renderer cleanup.
- Claim cross-platform support without verification.
- optimize transform or render-list code without a benchmark or demonstrated
  allocation issue.
- add hidden global state when explicit ownership is possible.
- combine unrelated feature milestones in one change.
- silently change coordinate, matrix, winding, or color-space conventions.

### Preferred task granularity

A good implementation task has:

- One visible behavior.
- One primary component.
- Explicit invariants.
- Focused tests.
- A verification command.
- A clear completion condition.

Example tasks:

- Add cycle-safe `Object3D.add` and tests.
- Add parent-to-child world transform propagation and tests.
- Create a GLFW 3.3 Core window and clear example.
- Compile a built-in shader program with numbered-source diagnostics.
- Upload a float position attribute exactly once when unchanged.
- Delete all geometry resources during renderer close.

Poorly scoped task:

- Implement the renderer.

---

## 37. Definition of Done for Version 0.1

Version 0.1 is complete when all of the following are true:

### Public interface completion

- A user can construct scenes, groups, meshes, two camera types, geometry, basic
  materials, shader materials, and textures.
- A user can render with `renderer.render(scene, camera)` without direct OpenGL
  calls.
- A user can inspect perspective and orthographic scenes interactively with
  orbit, pan, dolly, and zoom controls driven by a window's polled input state.
- Lifecycle and thread rules are documented.
- Public core types do not expose OpenGL identifiers.

### Correctness

- Hierarchical transforms behave according to documented conventions.
- Camera projection tests cover representative points.
- Indexed and non-indexed geometry render correctly.
- Visibility and frustum culling work.
- Opaque and transparent lists use documented ordering.
- Texture color-space behavior is defined and tested at a basic level.

### Resource behavior

- Unchanged resources are not re-uploaded each frame.
- Shared resources are realized once per renderer.
- Released resources are deleted with the correct context.
- Renderer close deletes all remaining GPU resources.
- Resource statistics are observable.

### Developer experience

- A clean checkout builds with the wrapper.
- Examples run with documented commands.
- Shader errors contain actionable diagnostics.
- Invalid geometry fails clearly.
- Resize behavior is demonstrated.

### Verification

- Pure tests run without a graphics context.
- OpenGL integration tests cover shader, buffer, texture, draw, and deletion
  paths.
- A rotating textured cube example runs on every claimed supported platform.
- A hierarchy example visibly demonstrates inherited transforms.
- A transparency example documents the sorting limitations.

---

## 38. Resolved Foundation Questions

The version 0.1 foundation review established that JScene3D is a public,
general-purpose Java library inspired by Three.js but governed by Java idioms
and deterministic lifecycle safety. It targets Java 21 and OpenGL 3.3 Core,
publishes separate core, LWJGL, and optional GUI JPMS artifacts through Maven
Central, and uses Apache-2.0.

Version 0.1 includes custom GLSL through `ShaderMaterial`, Basic, Lambert,
Normal, Phong, and line materials, ambient, point, directional, spot, and
hemisphere lights, PNG and JPEG texture loading,
genuine module-path and classpath use, multiple independent
Window-Renderer Pairs on one render thread, and explicit idempotent terminal
closure. It excludes raw OpenGL interoperability and shared contexts. The
interactive Solar System Viewer is the first Integration Example and the Public
Preview Gate. Platform qualification follows the private-to-public repository
policy documented above.

---

## 39. Completed Version 0.1 Design Review

The foundation review approved:

1. Coordinate and matrix conventions.
2. Public transform mutation style.
3. BufferGeometry attribute representation.
4. GPU resource ownership and disposal.
5. Window/renderer lifecycle ordering.
6. Initial material features.
7. Threading rule.
8. Initial supported platform matrix.
9. Build tool and Java baseline.
10. Version 0.1 exclusions.

These decisions are stable enough for the first implementation vertical slice.
New questions should be resolved only when implementation or example-writing
exposes a concrete missing contract; post-0.1 feature design belongs in a later
review session.

---

## 40. References

- [LWJGL getting-started guide](https://www.lwjgl.org/guide)
- [LWJGL project](https://www.lwjgl.org/)
- [JOML project and allocation-conscious design](https://github.com/JOML-CI/JOML)
- [GLFW OpenGL and platform compatibility](https://www.glfw.org/docs/latest/compat_guide.html)
- [GLFW window and context guide](https://www.glfw.org/docs/latest/window.html)
- [Three.js `Object3D` documentation](https://threejs.org/docs/pages/Object3D.html)
- [Three.js `WebGLRenderer` documentation](https://threejs.org/docs/pages/WebGLRenderer.html)

---

## 41. Closing Recommendation

Build the library around one complete, high-quality path:

```text
Window
  -> Scene graph
  -> Camera
  -> Mesh
  -> BufferGeometry + BasicMaterial
  -> Renderer.render
  -> deterministic GPU cleanup
```

Make that path easy to use, easy to test, and easy to diagnose. Add breadth only
after this path proves that the scene model and renderer ownership rules work
under real usage.

The most consequential early rule is worth repeating:

> BufferGeometry, materials, and textures describe rendering. The renderer
> owns their context-specific GPU realization.

That rule provides the strongest foundation for a high-level Java graphics
library that remains understandable as it grows.
