# JScene3D module overview

The root `jscene3d-parent` POM supplies shared build and dependency management
and aggregates the 33 modules below. This page is a navigation aid: it states
where responsibilities live without repeating the detailed project, editor, or
content-integration designs.

For the main conceptual layers, start with [Rendering fundamentals](../manual/fundamentals.md),
[Project and game fundamentals](../manual/project-fundamentals.md), and
[Editor fundamentals](../manual/editor-fundamentals.md).

## Core and rendering

- `jscene3d-core` defines the renderer-independent scene graph, cameras,
  geometry, materials, textures, animation, lights, fog, helpers, raycasting,
  math, and telemetry APIs.
- `jscene3d-lwjgl` provides the OpenGL renderer, GLFW window and input platform,
  renderer-owned surfaces, camera controls, and LWJGL-backed image loading.
- `jscene3d-gui` adds optional themed controls, monitors, text, and overlay
  drawing on top of the LWJGL renderer.

These low-level modules remain usable without the project model. The manual's
[Rendering fundamentals](../manual/fundamentals.md) introduces that direct API.

## Projects and authoring

- `jscene3d-configuration` defines toolkit-independent setting identities,
  types, constraints, scopes, choices, and an immutable definition registry.
- `jscene3d-i18n` defines toolkit-independent localized-message lookup, with a
  Java resource-bundle implementation.
- `jscene3d-project` owns versioned project manifests and settings, assets,
  import definitions, entity and Scene definitions, safe extension metadata,
  built-in descriptor metadata, validation, stable identities, and diagnostics.
- `jscene3d-project-3d` supplies authorable spatial and presentation descriptors,
  their runtime components and resource codecs, and the world-scoped adapter
  from entity ownership to an internal `Object3D` hierarchy.
- `jscene3d-project-physics` supplies authorable 3D collision descriptors,
  shape resources, runtime static and character bodies, sensors, overlap
  delivery, queries, and the world-scoped physics adapter.

The boundary between authored descriptors, trusted runtime implementations,
and live worlds is documented in the
[entity-component world architecture](entity-component-world-architecture.md).

## Runtime and game facilities

- `jscene3d-project-runtime` resolves and composes definitions into live worlds,
  loads trusted component extensions, and owns lifecycle, scheduling, signals,
  resources, and structural mutation.
- `jscene3d-physics` is the renderer-independent collision library: it owns
  collision objects and shapes, filtering, spatial queries, kinematic movement,
  character control, sensors, overlap transitions, and debug snapshots.
- `jscene3d-audio` provides OpenAL-backed buffered playback, Ogg Vorbis
  decoding, spatial sources and attenuation, listener control, and volume
  categories.
- `jscene3d-game` layers fixed-timestep world driving, semantic input,
  character-movement coordination, application control, audio, and screen
  presentation facilities over the project runtime.
- `jscene3d-project-desktop` is the standard native desktop host for composed
  projects, including launch configuration, window and input ownership, frame
  driving, presentation, audio-listener synchronization, and startup UI.
- `jscene3d-project-export` assembles relocatable application directories from
  project data, completed import publications, and resolved runtime JARs. It
  also drives the current macOS-specific `jpackage` application-image and
  disk-image packaging through explicit plans and tool adapters.

These modules implement the runtime side of the model described in
[Project and game fundamentals](../manual/project-fundamentals.md). The low-level
physics and audio libraries can also be used independently of authored projects.

## Import and content support

- `jscene3d-project-import` owns deterministic source inspection, preparation,
  generated-artifact publication, provenance, cache status, and read-only
  access to published artifacts. Format-specific providers plug into it.
- `jscene3d-gltf` loads glTF 2.0 and GLB scenes into JScene3D resources and also
  registers a project-import provider that publishes glTF-derived project
  content.
- `jscene3d-wad` validates IWAD and PWAD containers and provides provenance,
  bounded opaque-lump access, and explicit archive layering without interpreting
  game content.
- `jscene3d-wad-import` adapts WAD archives and selected opaque lumps to the
  generic project-import lifecycle.
- `jscene3d-doom` discovers and decodes classic Doom maps, materials, and
  renderer-independent geometry; publishes project-native generated content;
  and provides the Doom runtime components used by that content.

The WAD/Doom boundaries and publication rules are documented in
[WAD and Doom integration](wad-doom-integration.md).

## Editor integration

- `jscene3d-editor-authoring` owns headless project discovery and loading,
  authoring sessions and working copies, hierarchy and Inspector projections,
  semantic mutations, persistence, and editor diagnostics.
- `jscene3d-editor-authoring-service` exposes that Java-owned authoring model to
  editor clients through a persistent process and a versioned JSON protocol
  carried over byte-accurate `Content-Length`-framed standard I/O.
- `jscene3d-editor-authoring-runtime` assembles the service's Maven-resolved
  runtime closure and descriptor-only extension metadata into the installed
  source-development runtime consumed by the editor launcher.
- `jscene3d-iosurface-macos` contains the reusable macOS IOSurface
  render-surface adapter and native Mach transfer bridge. Its native bridge is
  published as a platform artifact rather than consumed from a source checkout.
- `jscene3d-editor-renderer` is the product process host for one native editor
  renderer session. It owns the versioned renderer protocol, lifecycle, and
  IOSurface-backed validation scene while remaining separate from authoring and
  future project/world hosting.
- `jscene3d-editor-renderer-runtime` assembles the renderer host, its runtime
  module closure, macOS ARM64 LWJGL libraries, and the IOSurface JNI bridge into
  an independently consumable runtime archive. The active Code OSS extension
  does not yet mount the viewport.

The process boundaries, safe authoring rules, and current viewport status are
documented in the [JScene3D Editor architecture](editor-architecture.md).

## Examples and development support

- `jscene3d-documentation` is a build-only POM that installs and runs the pinned
  Markdown lint toolchain; the documentation itself remains under `docs/`.
- `jscene3d-example-framework` provides shared native example hosting, browser
  lifecycle, catalogs, resource access, renderer-setting isolation, diagnostics,
  and thumbnail capture.
- `jscene3d-examples` contains the main rendering and asset-loading example
  browser, focused feature examples, the Solar System Viewer, and its packaged
  desktop distribution configuration.
- `jscene3d-physics-examples` contains the visual character-controller and
  movement-diagnostics examples.
- `jscene3d-game-examples` contains first- and third-person sandboxes that
  exercise game timing, semantic input, physics, rendering, and presentation.
- `jscene3d-project-examples` contains headless and graphical examples for
  definition contracts, world composition, 3D presentation and collision,
  spawning, telemetry, and application-directory export.
- `jscene3d-audio-examples` contains browser-hosted positional-audio and volume-
  mixing examples.
- `jscene3d-wad-examples` contains headless WAD inspection and generic project-
  import examples.

Example applications demonstrate module seams; they are not additional runtime
layers or alternative implementations of the engine.
