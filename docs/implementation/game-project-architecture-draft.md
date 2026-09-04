# Game project, scene, extension, and import architecture

Status: active discussion draft. This document is intentionally non-normative.
It records a proposed direction and the decisions accepted during the design
grill before any of the described interfaces or formats are implemented. The
agreed-decision section is updated throughout the grill; later exploratory
sections must be reconciled with it before implementation. It does not supersede
[`game-projects-and-wad-import.md`](game-projects-and-wad-import.md) or approve
implementation work.

## Purpose

JScene3D needs a project model that supports both code-first developers and a
future visual editor. The same project must be loadable by a standalone game,
headless tools, automated tests, and the editor. Doomed Corridors is the first
demanding consumer, while glTF and Doom-compatible WAD files provide concrete
import cases.

JScene3D should support declarative scenes, reusable scene instances, shared
resources, semantic input actions, and custom code. It should improve
discoverability by keeping project-authored composition, configuration, and
connections in inspectable data wherever practical.

## Core principle

Every project-authored decision required to reproduce a game should be stored
in project data or referenced source assets. The future editor must read and
write that same data rather than maintain a parallel editor-only model.

This principle does not mean that arbitrary Java algorithms must be converted
into visual graphs. Java remains a first-class way to implement behavior. The
project records that a registered Java-backed type is used and records its
editable configuration. The extension that owns that type supplies its
implementation, validation schema, defaults, and editor metadata.

Consequently:

- project structure and configuration are reproducible by the editor;
- Java algorithms remain ordinary source code;
- a project can use Java code attached to a scene node;
- a project can also use Java code with project-wide or import-time scope;
- headless and graphical callers observe the same project definitions;
- running or editing a project never depends on reverse-engineering Java code.

## Agreed design decisions

This section is the incremental decision record for the design grill. It is
authoritative within this draft when an older exploratory passage below still
describes an unresolved alternative. The rest of the document will be
reconciled as the grill progresses rather than relying on memory at its end.

### Delivery sequencing decisions

- The architecture is developed incrementally and does not need every long-term
  engine or editor question settled before coding resumes.
- The next implementation milestone is the generic JScene3D project framework,
  implemented and proven inside the JScene3D repository before Doomed Corridors
  is migrated to it.
- The framework proof uses small synthetic project fixtures and an engine-owned
  runnable example. It must demonstrate safe project and scene loading,
  extension descriptors, type registration, resource resolution, runtime
  composition, controller or system lifecycle, connections, and the generic
  launcher without depending on Doom code.
- The first runnable acceptance project contains a `Group3d` root, `Camera3d`,
  a supported `Light3d` family, and `MeshInstance3d`. A repeating `Timer` signal
  invokes a declared action on an engine-owned example controller so the proof
  exercises descriptor discovery, controller lifecycle, stable endpoint
  resolution, and visible state change rather than merely loading static JSON.
- Automated tests load and exercise the same acceptance project headlessly. A
  manual smoke test launches it through `project.json` with the generic launcher
  and confirms the camera, lit mesh, and timer-driven change are visible.
- Existing engine modules such as `jscene3d-core`, `jscene3d-lwjgl`,
  `jscene3d-game`, `jscene3d-physics`, and `jscene3d-audio` are changed where the
  framework proof demonstrates a missing generic capability. The design must
  not pre-emptively add speculative features or move Doom-specific code into
  those modules.
- Only after the engine-owned framework proof passes does Doomed Corridors adopt
  the new Project Manifest, application extension, entry scene, registered
  types, and generic launcher.
- The first Doomed Corridors milestone is behavioral parity with the current
  MAP01 launcher. Ammunition drops, doors, switches, and other new interactions
  follow parity rather than serving as the framework bootstrap.
- Only decisions that affect that milestone are immediate implementation gates.
  Property API names, complete node catalogs, advanced animation, alternate
  renderer capabilities, and other unneeded branches may remain parked.
- First-class world-2d support remains an architectural requirement. A later 2d
  platformer project will validate the paired 2d APIs and editor workflow, but
  that project is not a prerequisite for the next Doomed Corridors slice.
- Near-term work must avoid assumptions that make the later 2d implementation
  unnecessarily incompatible, while also avoiding speculative 2d code that the
  current game does not exercise.
- Coordinated migration work uses a `feature/project-runtime` branch in both
  JScene3D and Doomed Corridors. Each repository's `main` branch remains the
  stable baseline for the currently playable game.
- The current repositories are private pre-release code. The feature branches
  do not preserve provisional schemas, interfaces, or launch paths merely for
  backward compatibility. Existing implementations are reused only where they
  fit the agreed architecture.
- The `main` branches and their tests remain available as a behavioral reference
  while the feature branches replace the old composition path.
- Integration proceeds in dependency order: the required JScene3D foundation
  is integrated before the corresponding Doomed Corridors migration.

### Project composition and extension loading decisions

- All Java contributions use one extension model. There is no privileged
  `GameProvider` abstraction with a separate registration mechanism.
- A project designates exactly one extension as its application extension, but
  that extension implements the same contracts as every other extension.
- Project data and the entry scene own composition. The application extension
  must not imperatively assemble relationships that should be visible in the
  editor.
- Maven is the initial extension-resolution authority. After the user trusts a
  project, the editor invokes the project's Maven Wrapper in a child JVM to
  resolve dependencies and discover extension descriptors.
- Version 0.1 does not require a separate extension marketplace or installed
  extension catalog.
- Before trust, the editor may parse and display bounded declarative metadata,
  including schema version, identity, descriptive metadata, authors, dates,
  links, legal information, engine requirements, extension requirements, entry
  references, source assets, and resource catalogs.
- Before trust, project-relative paths are checked for containment and only a
  bounded local raster project icon may be decoded. Maven, Java code, importers,
  shaders, scene instantiation, play, and export remain disabled.
- A malformed manifest remains inspectable through a raw view with structured
  diagnostics.
- The Project Manifest uses explicit typed references for its entry scene,
  project systems, input map, import recipes, and export presets. It does not
  contain one generic `runtime.configuration` property.
- The current provisional Project Manifest is replaced in place. The finished
  format remains schema version 1; no parallel version 2 loader or migration
  path is required for private pre-release data.

### Project module seam decisions

- `jscene3d-project` is deepened as the renderer-independent project-data
  module. It owns safe manifest and scene loading, resource references, type
  descriptors, structural validation, migrations, and diagnostics.
- `jscene3d-project` must remain usable in restricted mode without graphics,
  audio, import execution, Maven resolution, or extension instantiation.
- A new `jscene3d-project-runtime` module owns trusted executable composition.
  It resolves registered types, instantiates nodes and controllers, shares
  resources, connects signals and actions, orders project-system lifecycles,
  and integrates with game, rendering, physics, and audio capabilities.
- The safe loader and executable runtime each expose a small interface that
  hides parsing, resolution, registration, factory ordering, connection setup,
  and lifecycle details from Doomed Corridors, the editor, headless tools, and
  exported launchers.
- The first executable kernel exposes `ProjectRuntimeLoader.load(...)`. It
  returns a `ProjectRuntimeLoadResult` containing either a fully composed
  `ProjectRuntime` or ordered structured diagnostics. The runtime implements
  the existing `GameApplication` contract and is therefore driven by the
  existing `GameRuntime` rather than by a second loop.
- Trusted extension implementations are discovered with Java's service-loader
  mechanism or supplied explicitly by an embedding launcher. They register
  scene-node and controller factories against exact serialized
  `RegisteredType` identities. Project documents and safe descriptors never
  contain Java implementation class names.
- Factory contexts expose the project, scene, owning node, effective
  properties, parent or controlled node, and bounded signal/action
  capabilities. Effective properties apply descriptor defaults first and
  authored values second.
- Composition creates node and controller objects in scene preorder, resolves
  declared connections after every factory has returned, starts objects in
  creation order, and closes them in reverse order. Fixed, frame, and render
  callbacks are separate opt-in interfaces; disabled subtrees do not receive
  those callbacks or signal/action dispatch.
- Runtime signals dispatch synchronously in scene-connection declaration order.
  Payload-bearing endpoints use an exact registered payload identity and do
  not perform runtime conversion.
- Native project resources and their runtime ownership are part of the first
  executable kernel. The first built-in rendering adapter is now implemented;
  nested-scene expansion and project-system definitions remain subsequent
  runtime slices. Encountering a configured but unsupported feature produces a
  terminal diagnostic rather than silently ignoring project data.
- Annotation-processing ownership may be separated later if build-time
  dependency direction requires it; that does not change the runtime seam.

### Built-in declarative 3d rendering slice

- `jscene3d-project-runtime-lwjgl` owns the adapter from portable registered
  types to JScene3D scene-graph objects. The renderer-independent
  `jscene3d-project-runtime` module does not depend on LWJGL or construct render
  objects.
- An embedding host creates `JScene3dRuntimeExtension` with its live `Window`
  and `Renderer`, then supplies it to `ProjectRuntimeLoader`. Safe descriptor
  discovery remains class-loader based; only the executable adapter receives
  live host services.
- The first built-in scene-node types are `group-3d`, `mesh-instance-3d`,
  `perspective-camera-3d`, and `ambient-light-3d`. The first native resource
  types are `box-geometry-3d` and `lambert-material-3d`.
- The extension descriptor defines stable properties, defaults, display names,
  sections, units, ranges, and reference constraints. These properties are the
  future inspector contract as well as the runtime input; the Java adapter does
  not contain an example-specific scene layout.
- `perspective-camera-3d` version 1 uses `position`, `scale`, and `target` for
  orientation. It does not expose an Euler rotation that would be silently
  superseded by `target`. A later orientation mode can add alternative rotation
  authoring without making two properties compete.
- Effective scene-node enablement controls JScene3D visibility. Generated
  geometries and materials are shared by canonical project reference and are
  closed once by the project runtime after scene objects detach.
- `jscene3d-project-examples/src/main/project` is the first graphical proof. Its
  Project Manifest, scene, box geometry, and Lambert material are independent
  project documents. The Java example class supplies the native host and drives
  the existing `GameRuntime`; it does not construct the camera, light, mesh,
  geometry, material, or transforms.

### Native resource version 1 and runtime ownership

- A native resource document contains `schemaVersion`, an exact registered
  resource `type` and `typeVersion`, and an ordered `properties` object. Its
  absolute logical URI is its runtime identity. Project files use canonical
  `file:` URIs; imported resources use stable `import:` URIs.
- `ResourceLoader` performs strict, headless JSON loading, validates reference
  syntax and file containment, and returns either an immutable
  `ResourceDefinition` or ordered structured diagnostics. It does not execute
  extension code.
- `RegisteredTypeCatalog` validates the resource's authored properties against
  the descriptor for its exact resource type, including defaults, required
  values, and reference-kind constraints.
- A trusted extension registers a `ResourceFactory` for each executable
  resource type. The factory receives the owning project, immutable resource
  definition, effective properties, and a bounded resource-lookup capability.
- Version 1 runtime lookup resolves `project:` and `import:` references. It
  loads dependencies recursively, preserves sharing through a logical-URI
  cache, reports dependency cycles as terminal diagnostics, and verifies the
  Java value type requested by its consumer.
- Imported-resource resolution is an explicit host capability. The host passes
  an `ImportedArtifactLookup` to `ProjectRuntimeLoader`; `ImportManager`
  implements that narrow interface. The resolver indexes manifest-declared
  import definitions, opens the requested artifact without seeing its physical
  cache path, requires `RESOURCE` artifact metadata, parses the standard native
  resource document, and verifies that the document type matches the published
  artifact type.
- Generated resource diagnostics and factory contexts retain the logical
  `import:<definition>/<output>` URI. Multiple consumers of that URI share one
  parsed runtime value and open the artifact only once. Artifact read handles
  and their streams are closed immediately after parsing; runtime resource
  values follow the normal project-runtime ownership rules.
- A resource factory returns a newly owned runtime value. Values implementing
  `AutoCloseable` are closed once in reverse creation order after scene nodes
  and controllers. Scene objects may retain resolved values but do not own or
  close them.
- `asset:` remains a valid portable reference namespace for source and import
  definitions. Direct runtime materialization of authoritative source assets is
  not implemented; runtime consumers use native resources produced by an
  importer. An `import:` reference without a host-supplied artifact lookup
  produces a specific terminal diagnostic rather than accessing a cache path.

### Scene composition and identity decisions

- One reusable scene-definition format represents complete screens, worlds,
  actors, menus, HUD fragments, and other reusable compositions. Version 0.1
  does not introduce a separate prefab format.
- Every scene has one logical root and an ordered scene tree.
- The universal scene-node model contains stable identity, registered type,
  optional display name, parent and ordered children, enabled state, lifecycle,
  an optional Java controller, and declared connection endpoints.
- Spatial transforms are node properties shown as an Inspector section. A
  transform is never represented as a child node.
- A node may have at most one registered Java controller in version 0.1. This is
  analogous to attaching one script to a node, but the controller type and its
  authored properties are registered and inspectable.
- Reusable functional objects such as timers, sensors, audio sources, and
  animators may be child nodes. Project-wide Java code is a project system, not
  a controller attached to a dummy root.
- Resource types are not scene nodes. Meshes, textures, materials, animation
  clips, audio clips, collision shapes, and similar immutable definitions are
  reusable resources referenced by nodes.
- Authored nodes have persistent scene-local identifiers that are separate from
  editable display names. Renaming, reordering, or moving a node preserves its
  identifier.
- The editor generates opaque UUID-like identifiers by default. Hand-authored
  readable identifiers are permitted. Duplicating a subtree generates new
  identifiers and rewrites references internal to that duplicate.
- Connections and animation tracks address stable identifiers rather than node
  names or list positions. Identifiers remain hidden during ordinary editing.
- Imported-node identifiers are deterministic combinations of the import
  declaration identity and a source structural locator. Examples include
  `import:freedoom-map01/sector/12` and source mesh indices in a glTF import.
- Content hashes are not identities and version 0.1 does not perform fuzzy
  matching when a source structure changes. An authored overlay whose target
  disappears produces an explicit diagnostic.
- Scene-instance overrides are limited initially to basic instance properties
  and explicitly exported scene parameters or controller project properties.
  Arbitrary editing of internal child structure is deferred.
- A reusable scene owns its internal connections. Its parent may connect only
  to explicitly exposed properties, signals, and actions on the scene instance,
  not directly to private internal children.

### Registered type and property decisions

- Serialized type identifiers are always fully qualified as
  `extension-identity/local-type`. Project data never contains Java class names
  or unqualified type names.
- Extensions package generated, versioned descriptors as JAR resources. The
  editor can inspect these descriptors without instantiating runtime classes or
  running static initializers.
- A descriptor records type identity and scope, property metadata and defaults,
  signals, actions, required capabilities, schema and migration information,
  and editor presentation metadata.
- Annotation processing is the normal way to generate descriptors and binding
  code. An explicit descriptor is available as an advanced escape hatch.
- Each registered type has an independent integer `typeVersion`, separate from
  the project schema version and extension version.
- Type migrations are deterministic, data-only, and headless. The editor
  applies them in memory, previews their effect, and writes them only through an
  explicit save. Unsupported values remain preserved as unresolved data with
  diagnostics.
- A controller's authoring properties should be declared from Java and exposed
  in the Inspector through generated metadata, serving the same purpose as
  Godot's exported script properties without copying its syntax.
- Saved base values and effective runtime values are distinct. Runtime drivers,
  including animation, temporarily affect the effective value without
  overwriting the authored base value.
- Property metadata must distinguish at least authoring editability, runtime
  drivability, keyframe support, and unsaved live read-only output. These are
  independent capabilities rather than one `editable` or `readonly` flag.
- The annotation and enum names for those property capabilities are parked.
  Names such as `animatable`, `RuntimeAccess`, and `ProjectOutput` are examples,
  not accepted API terminology.

### Signals, events, and actions decisions

- A direct node signal represents a relationship from one specific scene
  emitter to one or more explicitly connected targets.
- A project event channel is a declared, typed project resource for
  cross-scene or system-wide notifications. It is not a global static event bus
  or a service locator.
- A targeted action or command is distinct from an event. An event announces
  something that happened; an action asks a known target to do something.
- Direct node signals are delivered synchronously on the game-loop thread.
- Project event channels use queued FIFO delivery at a defined event phase.
  Background threads enqueue work and never dispatch directly into scene code.
- Dispatch uses a subscription snapshot and detects runaway event cycles. There
  is no per-connection delivery mode in version 0.1.
- Version 0.1 supports exact payload matching, no-payload connections, and a
  receiver explicitly ignoring a payload. It does not perform implicit
  conversion, bound-argument insertion, filtering, mapping, or expressions.
- A visible adapter controller or node performs any required conversion.
- Connections are stored in one scene-owned connection list. The Inspector may
  present incoming and outgoing views without changing that ownership model.
- Java implementations may use private listeners for implementation details.
  Relationships that a developer should be able to reconnect in the editor
  belong in project data.

### Project-system decisions

- Project systems are developer-written and may be game-specific. A
  `DoomCombatSystem` is valid; JScene3D does not pretend combat rules are a
  generic engine concern.
- A project system has one instance per running session and may survive scene
  transitions. It is comparable to an explicit project-scoped service, not a
  process-global singleton.
- The base lifecycle is `start` and `close`. Fixed-step and frame-step work use
  opt-in participant interfaces rather than forcing every system to implement
  empty callbacks. Systems do not receive renderer callbacks.
- Systems declare whether they run while paused.
- System definitions declare dependencies by project-system instance identity.
  Startup uses a deterministic topological order, declaration order breaks
  otherwise equal ties, and closure runs in reverse order. Missing dependencies
  and cycles are errors.
- A dependency establishes lifecycle order but does not grant access to another
  system's concrete object. Systems communicate through typed events, actions,
  and deliberately narrow service contracts.
- Systems explicitly declare required generic JScene3D capabilities. The
  runtime supplies narrow dependencies rather than a universal engine object.

### Import and WAD decisions

- The shared import API standardizes orchestration rather than forcing every
  source format into one universal content model.
- The common workflow is inspect source, present selection and settings,
  validate the request, produce typed artifacts through a controlled output
  sink, and report provenance and diagnostics.
- Importers declare identity, version, supported source types, settings schema,
  safe source access, progress and cancellation behavior, deterministic output
  identities, and dependency information. An importer does not choose cache
  paths or mutate authored files.
- Generated output combines a lightweight serialized manifest with type-specific
  JSON or binary payloads. The manifest records logical identifiers, types and
  versions, dependencies, payload locations and encodings, fingerprints,
  provenance, and diagnostics.
- Runtime representations are immutable and may be loaded lazily. Import output
  is not limited to either direct Java objects or all-JSON serialization.
- Large imports provide a lightweight browse index. The editor uses virtualized
  trees, paging, lazy previews, and imported/read-only status rather than
  loading the complete graph.
- The source and generated output remain read-only. Authored changes are stored
  separately and reapplied so the effective scene is the original import plus
  an authored overlay.
- A version 0.1 overlay may edit allowed properties, disable imported nodes,
  attach a project controller, add authored child nodes or scene instances,
  create connections, and change display names or tags. It does not delete,
  reparent, or rewrite generated source data.
- Generic WAD archive access belongs in an optional `jscene3d-wad` module.
  Doom maps, patches, palettes, textures, flats, sprites, and DMX sound
  decoding belong in an optional `jscene3d-doom` module.
- Actor-to-sprite selection, combat presentation, rules, inventory, campaign,
  and other game semantics remain in Doomed Corridors.
- Standard editor and SDK distributions include WAD archive and Doom-content
  support out of the box, but core, game, and physics modules do not depend on
  Doom concepts.
- A WAD import declaration contains an explicit ordered archive stack with base
  and patch roles. Later archives have higher priority according to Doom
  semantics, output retains source provenance, and changes to order or
  fingerprints invalidate affected output. Version 0.1 does not scan arbitrary
  directories for patches.

### Editor, trust, and local-state decisions

- The desktop editor is a separate `jscene3d-editor` application written in
  Java. The existing `jscene3d-gui` module remains an in-game UI and overlay
  library rather than becoming the desktop shell.
- SWT is the leading desktop-toolkit choice, subject to a focused spike proving
  menus, trees, an Inspector, a resizable OpenGL canvas, input, DPI behavior,
  and cleanup on macOS followed by Windows and Linux.
- Version 0.1 does not require Eclipse RCP. Small JFace components may be
  considered later where they materially reduce editor code.
- The renderer gains a context or render-surface seam. Standalone games retain
  GLFW while the editor uses SWT's OpenGL canvas.
- Project trust is explicit and applies to a canonical project directory.
  Trust data is local to the machine and is never committed with a cloned
  project.
- Restricted mode permits safe metadata, structural data, raw properties,
  connection inspection, and non-executable editing. Trusted mode enables
  Maven, extension code, importers, shaders, play, and export.
- Child processes provide crash isolation but are not described as a security
  sandbox. New executable dependencies produce a renewed warning.
- When an extension is missing, the editor preserves unresolved nodes and data,
  shows structural and raw views with diagnostics, and permits safe file and
  structural operations. It does not guess a specialized Inspector or discard
  unknown values.
- Missing required runtime types block affected play or export. A missing
  editor-only extension need not block the game.
- Editor-local state is stored in the operating system's application-data area,
  keyed by stable project identity and canonical path. It includes window and
  panel state, open documents, selection, tree expansion, editor cameras,
  recent files, trust, and local toolchain or destination overrides.
- Team-visible settings are explicit project resources. Generated caches are
  separate from both project data and local editor preferences.
- Round trips are semantically lossless rather than byte-for-byte identical.
  Unknown valid subtrees, extension metadata, connections, values, and ordering
  are preserved. Unsupported newer schemas open read-only or raw and are never
  silently rewritten to an older form. Saves are atomic.
- The edit viewport uses built-in capabilities and descriptors without running
  project controllers. It can show meshes, instances, sprites, cameras, lights,
  environments, collision and sensor debug shapes, audio icons and ranges, UI,
  and descriptor-supplied placeholders.

### Play, build, and export decisions

- Play Current Scene and Play Project use the same application extension,
  systems, input, resources, event processing, physics, audio, and other
  services. They differ only in the selected entry scene.
- A reusable scene may declare an explicit preview or test scene in project
  data. The editor does not invent a camera, player, light, or hidden test
  harness when required content is missing.
- Version 0.1 play runs in a separate GLFW child process and game window. The
  editor and child exchange a small versioned control protocol for lifecycle
  state and structured failures. The editor does not infer state from console
  text.
- Embedded play, runtime inspection, and hot synchronization are later
  capabilities.
- The Project Manifest selects a build adapter. The initial Maven adapter uses
  the project Maven Wrapper and fixed operations for describing, compiling,
  discovering, playing, and preparing export. It does not search for a main
  class, assume one POM layout, execute arbitrary shell text, or parse logs as a
  protocol.
- Version 0.1 uses a generic JScene3D launcher and `jpackage`-style
  self-contained application images with a bundled Java runtime.
- True ahead-of-time native executables for macOS, Windows, and Linux, with no
  installed or bundled JVM, are a long-term architectural requirement rather
  than a synonym for `jpackage`.
- Extensions used by a true-native export are known at build time and must
  provide required native-image metadata, including resource, reflection,
  foreign-function, and native-library requirements.
- Export content is computed from a transitive resource graph beginning with
  the manifest, entry scene, systems, input map, import recipes, and export
  preset. Globs may add deliberately unreferenced content but are not the
  primary packaging model.
- The first certified target is `macos-aarch64`, followed by
  `windows-x86_64` and `linux-x86_64`. Additional architecture triples follow
  demonstrated demand. Each target uses matching native dependencies and real
  target smoke tests.

### Rendering and resource decisions

- Shared resource definitions are immutable. Per-node playback, body state,
  health, and material overrides are separate runtime state. “Make Unique”
  creates a new project resource instead of mutating shared data accidentally.
- Standard material resources expose portable properties such as color,
  textures, normals, emission, metallic and roughness values, opacity, alpha
  policy, culling, depth, sampling, and UV settings where supported.
- Custom shaders declare their renderer and source language explicitly. Initial
  raw GLSL resources target OpenGL and are not claimed to be portable to every
  future renderer. Export validates target compatibility and may later select
  declared variants or fallbacks.
- Instanced meshes support authored, imported, and deterministic generated
  instance sources. Authored instances have persistent resource-local IDs;
  imported instances use source identities; generated instances use stable
  generator keys when editing requires identity.
- One batched mesh instance cannot independently own children, a controller,
  signals, audio, or physics. Such an instance must be promoted to an ordinary
  scene node.

### Dimensional model and naming decisions

- World 2d is a first-class engine and editor concern, not a synonym for UI. A
  later platformer proof will validate scene, rendering, input, physics,
  animation, sensor, and editor architecture in two dimensions; it does not
  block the next Doomed Corridors implementation slice.
- Public Java types and authoring-model types use a lowercase `d` suffix, such
  as `Camera2d`, `Camera3d`, `Object3d`, and `Sensor3d`. Serialized local IDs use
  forms such as `camera-2d` and `sensor-3d`.
- Concepts shared by both dimensions use the same base name with only the
  suffix changed. A different name is used only when the concept itself is
  dimension-specific.
- The author-facing term is `Sensor2d` or `Sensor3d`, not `Trigger`. A trigger is
  one possible game use of a sensor.
- A scene tree may contain world-2d, world-3d, and screen-space UI branches.
  Transform inheritance occurs only through compatible dimensional ancestry;
  projection between spaces is explicit.
- `SceneNode` is the universal structural model but is not exposed as parallel
  `Node2d` and `Node3d` authoring types. Transform-only grouping uses `Group2d`
  or `Group3d`; dimension-neutral organization uses `Group`.
- World 2d uses positive X to the right, positive Y downward, and positive
  visible rotation clockwise. One 2d world unit is one logical pixel, independent
  of physical display pixels or DPI. Camera zoom controls display mapping.
- World 3d remains right-handed with positive Y upward and negative Z forward.
  One world unit is one metre.
- Three-dimensional transforms serialize normalized quaternions. The Inspector
  presents Euler angles in degrees. Authored angle properties also use degrees
  and runtime implementation may convert to radians.
- Screen-space UI is a separate capability family. It uses layout, anchors,
  containers, focus, clipping, and responsive sizing rather than world-2d
  transforms and cameras.
- Visible world-2d nodes have a named render layer and integer order. Project
  data orders layers; scene-tree order is the deterministic final tie-breaker.
  A `Group2d` may opt into Y sorting, cameras select visible layers, and UI uses
  its own stacking rules.

### Physics decisions

- Collision placement is represented by child `CollisionShape2d` or
  `CollisionShape3d` nodes with transforms and references to reusable immutable
  shape resources. Shapes are visible only through editor or debug overlays.
- The completed physics architecture must support static, kinematic, character,
  dynamic rigid-body, and sensor semantics in both dimensions. Dynamic
  rigid-body physics is an engine requirement, but completing both solver
  integrations does not block the first Doomed Corridors authoring slice.
- The public JScene3D physics model is backend-independent and delegates
  production simulation to mature solvers rather than growing the existing
  static and kinematic collision routines into an ad hoc solver.
- Version 0.1 ships one certified 2d backend and one certified 3d backend while
  preserving an extension seam for later alternatives.
- Physics project data covers collision layers and masks, materials, queries,
  contact events, gravity, forces, impulses, damping, sleeping, continuous
  collision detection, and an initial bounded joint or constraint set.
- Each running scene session supplies default `PhysicsWorld2d` and
  `PhysicsWorld3d` instances configured by explicit project resources. Instanced
  scenes inherit their host world. An isolated world requires an explicit
  boundary and is an advanced use case.

### Animation decisions

- Animation uses a dimension-neutral `Animator` scene node. It can target
  declared properties on world-2d, world-3d, UI, material, sprite, light, and
  audio nodes through stable node and property identifiers.
- Selecting an `Animator` opens an integrated lower editor panel while the
  scene tree, viewport, and Inspector remain visible. The panel provides clip
  management, tracks, a timeline, keyframes, transport controls, snapping,
  looping, interpolation, and key editing.
- Animatable Inspector properties display a key control. Moving the playhead,
  changing a property in the Inspector or viewport, and adding a key creates or
  updates the corresponding typed track.
- This adopts the useful property-track workflow demonstrated by Godot without
  copying node paths, arbitrary Java method-call tracks, or a magic animation
  name for reset behavior.
- Animation tracks use stable node IDs and registered property IDs. They may
  use discrete or supported interpolated values and an explicit baseline pose.
  Scrubbing overlays values without overwriting saved scene properties.
- Arbitrary Java methods are not animation targets. Declared actions, signals,
  or events provide inspectable integration with project code.
- Clips are scene-owned resources by default. The editor can extract a clip as
  a shared resource. Initially, a shared clip targets instances of the same
  scene definition; cross-structure reuse requires an explicit binding or
  retargeting map.
- `Sprite2d` and `Sprite3d` work with the general `Animator`. A “Create sprite
  frame animation” editor operation may generate the appropriate clip and
  tracks without introducing a separate frame-only animation system.
- Version 0.1 includes an optional project-authored animation state machine with
  a graph editor in the Animation panel. A project defines its own state names,
  parameters, triggers, transitions, exit rules, priorities, and crossfades.
- Java game code owns decisions such as movement, attacks, pain, and death. It
  updates declared animation parameters or triggers; the generic animation
  state machine selects presentation clips. Direct clip playback remains
  available when no state machine is needed.

## Goals

- Keep the Project Manifest small, stable, safe to inspect, and independent of
  graphics, audio, importing, and extension execution.
- Define a native, versioned scene format capable of describing a complete game
  composition rather than only a render hierarchy.
- Reuse scenes through scene instantiation instead of introducing a separate
  prefab format prematurely.
- Support shared native resources without duplicating them at every use site.
- Support one optional node-scoped controller and project-wide Java systems
  explicitly.
- Let Java extensions register types through stable identifiers rather than
  serializing implementation class names.
- Make registered types discoverable and configurable by the future editor.
- Preserve serialized project-authored event connections where those
  connections are part of the game design.
- Make project layout directory-agnostic while offering optional scaffold
  conventions.
- Establish one import orchestration model for source formats such as glTF and
  Doom-compatible WADs.
- Distinguish an editor's in-process 2d or 3d edit viewport from playable preview,
  running the current scene, and running the complete project.
- Define reproducible export presets that package project content, Java code,
  dependencies, a Java runtime, and platform-native libraries.
- Establish a deliberately bounded version 0.1 catalog of world-2d, world-3d,
  UI, resource, and runtime capabilities without making the scene schema depend
  on one renderer implementation.
- Keep imported sources authoritative, imports deterministic, and derived
  output disposable.
- Allow project-authored content to extend imported content without being
  destroyed by reimport.
- Provide supported WAD capabilities without putting Doom concepts in
  `jscene3d-game` or `jscene3d-physics`.
- Keep expected loading and import failures as structured diagnostics suitable
  for command-line tools and editor navigation.

## Non-goals

- Require `main/`, `game/`, `scenes/`, `assets/`, or any other directory names.
- Make all Java implementation details editable as data.
- Infer editor controls by reflecting over arbitrary Java classes.
- Introduce a visual programming language in the first version.
- Make Doom rules, actors, sectors, weapons, or linedef semantics part of the
  genre-independent Game Engine or Physics Engine.
- Guarantee that every WAD contains Doom-compatible data. A WAD is first an
  archive of named lumps; interpretation is supplied separately.
- Download and execute untrusted extensions merely to display basic Project
  Manifest metadata.
- Define final filenames or Java type signatures in this discussion draft.

## Terminology proposed by this draft

### Project Manifest

The root `project.json` containing safe descriptive metadata and references to
the resources needed to open or run the project. It locates content; it does not
contain the complete game.

### Project resource

A versioned, project-authored document or directly usable asset identified by a
stable project-relative reference. Examples include scenes, input maps, themes,
combat rules, and imported-source declarations.

### Source asset

An authoritative external asset such as a WAD, glTF file, image, or audio file.
An importer reads it without modifying it.

### Imported resource

A deterministic runtime or editor representation derived from a source asset,
import settings, importer identity, and importer version. It retains provenance
and may be regenerated.

### Edit viewport

An editor-owned rendering surface used to inspect and manipulate authored scene
data. It may use an editor camera, selection overlays, transform gizmos, and
preview lighting that are not part of the saved game scene. It is not a running
game.

### Play current scene

A playable runtime session whose entry point is the scene currently being
edited. This is a fast test path and may need a small editor-supplied harness
for project systems normally created by the application entry scene.

### Play project

A playable runtime session that starts from the Project Manifest's configured
entry scene and uses normal project startup semantics.

### Export preset

A versioned project resource describing one distribution target and the
content, Java runtime, dependencies, native libraries, launcher configuration,
and packaging options included in that target. Credentials and machine-local
signing state are not part of the shared preset.

### Scene definition

A project resource that describes an instantiable hierarchy of typed entries,
their properties, child relationships, resource references, and declared event
connections. A scene definition can represent a whole application screen, a
gameplay world, a user interface, or one reusable actor.

### Scene instance

An entry that instantiates another scene definition. This provides prefab-like
reuse without a second, nearly identical format.

### Registered type

A stable type identifier contributed by JScene3D or a Java extension. Its
descriptor defines where the type may be used, its editable properties,
validation rules, defaults, events, actions, and editor presentation.

### Node controller

An optional registered Java-backed type attached to one scene node. Its
lifetime is associated with that node instance. Version 0.1 permits at most one
controller per node; reusable functional composition uses child nodes.

### Project system definition

A registered Java-backed type instantiated once for a running project or game
session rather than attached artificially to a scene entry. Examples include
combat coordination, campaign progression, save management, or a project-wide
inventory.

### Java extension

A packaged Java contribution that registers node controllers, project systems,
resource types, importers, or editor descriptors. A project designates one
extension as its application extension, but it uses the same contracts as
other extensions.

### Adapter

A concrete implementation at a format or integration seam. The glTF and WAD
import adapters are concrete examples at the source-import seam.

## Directory independence

Neither schemas nor runtime code should infer meaning from directory names. All
required locations are explicit references starting at `project.json`.

These projects should be equivalent:

```text
project.json
main.scene.json
player.scene.json
```

```text
project.json
application/main.scene.json
actors/player.scene.json
```

An optional scaffold may suggest a layout such as the following because
conventions improve navigation, but the layout is not part of the project
interface:

```text
project.json
scenes/
actors/
ui/
input/
imports/
assets/
```

Renaming or reorganizing these directories should require updating references,
not changing Java code.

## Project Manifest direction

Project Manifest schema version 1 separates descriptive metadata from
`schemaVersion`, engine compatibility, runtime selection, and source assets. It
remains safe to load without executing extensions or importing assets. The
current provisional version 1 structure may be replaced freely before release;
private pre-release files receive no compatibility promise.

The current startup pair of source asset and target was useful for proving WAD
loading, but it should evolve toward an engine-native entry scene. Source asset
and target selection belong in an import declaration rather than serving as the
permanent application entry point.

A possible final version 1 shape is:

```json
{
  "$schema": "schema/project-1.schema.json",
  "schemaVersion": 1,
  "identity": {
    "id": "io.github.glynch.doomed-corridors",
    "name": "Doomed Corridors",
    "version": "0.1.0-SNAPSHOT",
    "icon": "branding/icon.png"
  },
  "engine": {
    "requires": ">=0.1.0 <0.2.0"
  },
  "runtime": {
    "applicationExtension": "io.github.glynch.doomed-corridors",
    "entryScene": "application/main.scene.json",
    "projectSystems": "game/systems.json",
    "inputMap": "input/default.json"
  },
  "extensions": [
    {
      "id": "io.github.glynch.doomed-corridors",
      "requires": ">=0.1.0 <0.2.0"
    },
    {
      "id": "io.github.glynch.jscene3d.doom",
      "requires": ">=0.1.0 <0.2.0"
    }
  ],
  "assets": [
    {
      "id": "freedoom",
      "type": "io.github.glynch.jscene3d.doom/wad",
      "path": "assets/freedoom2.wad",
      "sha256": "..."
    }
  ],
  "imports": ["imports/freedoom-map01.import.json"],
  "exportPresets": ["exports/desktop.json"]
}
```

This is the settled Project Manifest version 1 direction for the first
framework increment. The application extension must also occur in
`extensions`. Extension requirements use semantic-version comparisons. Asset
types use an extension-qualified registered type identifier. `assets`,
`imports`, `exportPresets`, `projectSystems`, and `inputMap` are optional; this
allows a generated engine example to start from a scene without pretending it
has external source assets. All file paths use forward-slash project-relative
syntax on disk and become normalized paths confined to the project root only
after safe loading.

## Native scene model

### One reusable scene concept

The initial design should use one scene-definition format for both top-level
scenes and reusable fragments. A player, door, HUD, menu, and complete game can
all be scenes at different scales. An explicit scene instance provides reuse.

Introducing both `scene` and `prefab` formats would duplicate identity,
properties, resource references, nesting, validation, overrides, and event
connections before a demonstrated semantic difference exists.

### Broader than a render tree

A complete game composition contains more than rendered spatial objects. It may
also contain collision, audio, user-interface elements, timers, animators, node
controllers, and references to project systems. Therefore the serialized scene
model uses one universal structural node rather than assuming every entry is a
render object.

Registered node capabilities create the appropriate world-2d, world-3d, UI,
audio, or physics runtime objects. All capabilities share one logical scene
root, while transform inheritance remains dimension-specific.

### Stable identities

Every addressable scene entry needs a stable identifier independent of its
display name and list position. Importers also need deterministic identifiers
derived from source identities, such as a WAD map name plus thing, linedef, or
sector index.

Names may change without breaking connections. Reordering children must not
change identity. Randomly regenerated identifiers must not make deterministic
imports appear modified on every run.

### Illustrative scene document

```json
{
  "$schema": "../schema/scene-1.schema.json",
  "schemaVersion": 1,
  "id": "main",
  "root": {
    "id": "application",
    "type": "io.github.glynch.jscene3d/group-3d",
    "typeVersion": 1,
    "children": [
      {
        "id": "world",
        "instance": "../maps/map01.scene.json"
      },
      {
        "id": "player",
        "instance": "../actors/player.scene.json"
      },
      {
        "id": "hud",
        "instance": "../ui/hud.scene.json"
      }
    ]
  },
  "connections": [
    {
      "from": {
        "node": "combat",
        "signal": "player-health-changed"
      },
      "to": {
        "node": "hud",
        "action": "set-health"
      }
    }
  ]
}
```

Scene version 1 settles stable node identifiers, registered typed nodes, nested
scene instances, optional controllers, ordered children, editable properties,
bounded instance overrides, and scene-level signal-to-action connections.
Registered node and controller references contain an extension-qualified
`type` plus a positive `typeVersion`. A node declares exactly one of a typed
source or an `instance`. The first loader verifies node identity, endpoint
existence, path safety, and reference syntax; descriptor-aware payload and
endpoint compatibility belongs to the registered-type catalog increment.

### Instance overrides

Scene instances need bounded overrides so a reusable definition can be
configured at each use site. Overrides should address stable property paths
declared editable by the registered type.

```json
{
  "id": "guard-west",
  "instance": "../actors/zombieman.scene.json",
  "overrides": {
    "position": [-128, 0, 256],
    "sight-range": 1536
  }
}
```

Arbitrary structural patching should not be included initially. It complicates
validation, inheritance, reimporting, and editor presentation. Structural
variants can be represented as another scene that instantiates and composes
smaller scenes until a real use case proves that structural overrides are
necessary.

### Resources and sharing

Scenes should reference resources by stable logical reference. Loading the same
resource reference twice should preserve sharing where the resource's ownership
and mutability interface permits it.

A native resource has its own registered resource type and portable property
values:

```json
{
  "$schema": "../schema/resource-1.schema.json",
  "schemaVersion": 1,
  "type": "io.github.glynch.doomed-corridors/actor-animation-set",
  "typeVersion": 1,
  "properties": {
    "source": {
      "$ref": "asset:freedoom"
    },
    "idle": {
      "$ref": "import:freedoom-sprites/zombieman/idle"
    }
  }
}
```

The resource file's canonical project-relative path provides identity. Its
registered type provides semantics and factory selection; a Java class name is
never persisted in the document.

```json
{
  "id": "zombieman-visual",
  "type": "io.github.glynch.doomed-corridors/animated-billboard-3d",
  "typeVersion": 1,
  "properties": {
    "animations": {
      "$ref": "project:actors/zombieman-animations.json"
    },
    "material": {
      "$ref": "import:freedoom-map01/materials/actor-sprite"
    },
    "source": {
      "$ref": "asset:freedoom"
    }
  }
}
```

Scene version 1 distinguishes three explicit resource namespaces:

- `project:` locates a project-relative file and is confined to the project
  root during loading;
- `asset:` names an authoritative source asset declared by the manifest;
- `import:` names an import identifier followed by a portable output locator.

A reference is represented by an object containing only `$ref`; ordinary text
therefore remains ordinary text. Runtime-only object references are deliberately
excluded from persisted property values.

## Dimensional scene and resource model

Godot is useful here as an established product reference, not as a format, API,
or node catalog to reproduce. Its stable documentation separates hierarchical
world objects with local transforms from reusable resource data, and separates
editor viewports from cameras and environments that ship with a game. It also
uses the same saved-scene concept for a whole level and an instanced reusable
object. These observations support, but do not by themselves define, the scene
and resource decisions in this draft. See Godot's
[introduction to 3D](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html),
[nodes and scenes](https://docs.godotengine.org/en/stable/getting_started/step_by_step/nodes_and_scenes.html),
and [Resource](https://docs.godotengine.org/en/stable/classes/class_resource.html)
documentation.

### Entries declare composition; resources hold reusable data

The JScene3D scene format should model visible composition through typed scene
nodes. A world-2d or world-3d node has a parent-relative transform and may own
children, but mesh data, materials, textures, animation clips, audio clips, and
collision shapes are reusable resources referenced by nodes. This prevents
every instance from duplicating large data and lets an importer emit one
resource that many authored nodes can use.

This separation is more important than matching another engine's type names.
For example, a mesh instance entry might reference a mesh resource and an
optional material override. It should not embed renderer buffers or require the
serialized type to be the concrete `jscene3d-core` `Mesh` class. A runtime
adapter resolves the registered scene type and resource types to the existing
JScene3D objects.

Resource sharing needs an explicit mutability contract. Immutable imported
meshes, textures, clips, and collision shapes are natural shared values.
Mutable per-instance animation state, playback position, physics pose, and
material parameter overrides are runtime instance state and must not mutate a
shared project resource accidentally.

### Baseline capability families across dimensions

The native model should have a place for the following capability families in
world 2d, world 3d, or UI where the concept genuinely exists. The last column is
a recommendation for project-format version 0.1, not a claim about another
engine's scope.

| Capability | Scene composition | Reusable or derived data | Version 0.1 scope |
| --- | --- | --- | --- |
| Scene hierarchy | `Group`, dimension-specific groups, concrete nodes, and scene instances | None beyond scene data | One logical tree supporting world 2d, world 3d, and UI |
| Mesh rendering | Dimension-specific mesh-instance and instanced-mesh nodes where supported | Geometry, material, texture, and instance transforms | Existing world-3d mesh capabilities plus a deliberate world-2d mesh contract |
| Sprites | `Sprite2d` or `Sprite3d` composed with an `Animator` when animated | Images, texture regions, and animation clips | Required in both dimensions |
| Camera and viewport | Dimension-specific cameras assigned to an output | Render-target and viewport settings | One primary output and active camera; sub-viewports later |
| Lighting and environment | Dimension-appropriate light nodes and explicit world environment references | Environment maps and environment settings | Existing world-3d lights; world-2d lighting must be designed with its renderer |
| Materials and shaders | Material references and bounded per-instance overrides | Standard and shader material resources | Existing materials and inspectable custom spatial shaders required |
| Animation | A dimension-neutral `Animator` targets registered properties | Transform, morph, skeletal, sprite, and general property clips | Timeline editing and a basic animation state machine required |
| Physics and collision | Static, kinematic, character, rigid-body, sensor, shape, and joint nodes in both dimensions | Primitive, imported, and generated shape resources | Full dynamic simulation through certified backends in both dimensions |
| Audio | Dimension-specific spatial sources and listeners plus non-spatial playback | Audio clips and playback configuration | Existing audio capabilities generalized across both dimensions |
| User interface | A separate UI hierarchy for menus and HUDs | Images, fonts, themes, and layout data | Small HUD and menu composition proof required |
| Tile maps | World-2d tile-map authoring and rendering | Tile sets, layers, collision, and source provenance | Required by the platformer proof; exact first schema remains to design |
| Navigation | Regions, links, agents, and obstacles connected to a navigation world | Navigation mesh or graph derived data | Long-term; custom game navigation remains possible |
| Particles | Emitter entries and collision or attraction entries | Emitter settings, materials, and particle shaders | Long-term |
| Visibility optimization | Per-instance ranges, hierarchy proxies, occluders, and renderer settings | LOD meshes and baked occlusion data | Long-term and renderer-capability driven |
| Level construction | Imported geometry, generated geometry, or specialized authoring entries | Meshes, collision, navigation, and source provenance | Import and generated geometry now; CSG, grids, and terrain later |

Godot's documentation illustrates why these families should not collapse into
one generic property bag. A `MeshInstance3D` references a mesh resource, while
`MultiMeshInstance3D` provides many instances of one mesh with important shared
material and group-culling tradeoffs. Standard materials cover common artist
needs, while shaders expose a different programming and parameter surface.
See [procedural geometry](https://docs.godotengine.org/en/stable/tutorials/3d/procedural_geometry/index.html),
[MultiMesh optimization](https://docs.godotengine.org/en/stable/tutorials/3d/using_multi_mesh_instance.html),
[StandardMaterial3D](https://docs.godotengine.org/en/stable/tutorials/3d/standard_material_3d.html),
and the [shading language](https://docs.godotengine.org/en/stable/tutorials/shaders/shader_reference/shading_language.html).

Registered type descriptors should preserve these meaningful distinctions
while hiding renderer construction details. A mesh instance descriptor can
require a mesh reference; a camera descriptor can expose projection and clip
settings; a light descriptor can expose color, intensity, range, and shadow
settings. Editor controls can then be specific and valid without knowing the
Java implementation class.

### Meshes and instancing

A mesh resource describes geometry; it is not a scene entry. Version 0.1 should
represent the capabilities already established by `BufferGeometry`: named
vertex attributes, optional indices, draw ranges, bounds, morph targets, and
one or more material-bearing surfaces once the project resource representation
is designed. Importers and Java extensions may produce mesh resources, but GPU
buffer identifiers and renderer allocation details are never serialized.

A mesh-instance entry gives one mesh a transform, visibility, material
assignments or overrides, shadow policy, and an explicit or derived bounds
policy. Many entries may reference the same mesh and materials. Mutable
per-instance state must not alter those shared resources unless the author has
explicitly chosen a shared edit.

An instanced-mesh entry is an optimization with a different interface, not
shorthand for thousands of fully independent scene entries. JScene3D's
existing `InstancedMesh` supports a fixed-capacity batch, an active instance
count, local transforms, optional colors, custom numeric attributes, morph
influences, and batch or per-instance bounds. Version 0.1 project data should
be able to reference authored, imported, or generated instance data that maps
to those supported capabilities.

All instances in a batch share geometry and material structure, render
callbacks occur for the batch, and transparent instances cannot be sorted as
independent objects. An instance that needs its own children, controller,
event connections, audio, physics body, or independently ordered transparent
rendering should normally be an ordinary scene instance instead. The editor
may offer a populate or conversion tool, but its result must be inspectable
instance data rather than hidden editor state. Godot's
[procedural geometry](https://docs.godotengine.org/en/stable/tutorials/3d/procedural_geometry/index.html)
and [MultiMesh documentation](https://docs.godotengine.org/en/stable/tutorials/3d/using_multi_mesh_instance.html)
illustrate the same resource, scene-instance, and batched-instance distinction.

### Materials and shaders

Version 0.1 should support the existing basic, normal, Lambert, Phong,
physically based standard, line, and custom shader material families through
registered resource descriptors. Shared render state includes visibility,
alpha mode and cutoff, face selection, depth testing, depth writing, and depth
comparison. A standard material exposes only the texture channels and numeric
properties its runtime implementation actually honors.

Custom shaders are already a JScene3D capability and should not be hidden from
the authoring model. A version 0.1 shader resource should declare:

- the source language and version, initially `glsl-330-core`;
- vertex and fragment stage sources or project-relative source references;
- required standard vertex attributes;
- whether renderer-managed instance transforms and colors are consumed;
- custom per-instance numeric attributes;
- typed uniforms, defaults, ranges, color-space meaning, and resource
  constraints;
- renderer-managed transform uniforms that project code cannot redefine;
- compatible renderer capabilities and structured compilation diagnostics.

The explicit language identifier matters. Raw GLSL 3.3 is an OpenGL renderer
resource, not automatically portable project data. A future renderer can
support another source variant, a common intermediate representation, or an
engine shading language without changing the scene entry that references the
material. The editor should be able to edit declared uniforms and show shader
compilation errors, but version 0.1 does not require a visual shader graph,
compute shaders, custom render passes, particle shaders, sky or fog shaders,
or a post-processing graph. Godot's separation of standard materials from
programmable shader types is a useful product reference; see
[StandardMaterial3D](https://docs.godotengine.org/en/stable/tutorials/3d/standard_material_3d.html)
and [Introduction to shaders](https://docs.godotengine.org/en/stable/tutorials/shaders/introduction_to_shaders.html).

### Cameras, viewports, environments, and editor aids

A saved game camera and an editor camera are different objects. The editor
needs its own freely navigable camera even when the scene has no game camera.
Likewise, editor preview lighting can make an unlit scene editable but must not
silently become runtime lighting. Godot explicitly keeps its preview sun and
environment out of a running or exported scene, which is the behavior JScene3D
should adopt. See the [Godot 3D workspace](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html#d-workspace)
and [preview environment and light](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html#preview-environment-and-light).

Version 0.1 should support one primary output viewport and a selected active
camera. The data model must not make that a permanent one-camera restriction.
Later sub-viewports are needed for picture-in-picture, security monitors,
mirrors, minimaps, and off-screen rendering. Godot's `Viewport` is a useful
reference because it owns a drawing surface, input region, world, cameras, and
audio listener rather than being only a rectangle. See the official
[Viewport reference](https://docs.godotengine.org/en/stable/classes/class_viewport.html).

Environment data should be an explicit resource selected by a world or camera,
not a collection of invisible renderer globals. The first resource may expose
only features JScene3D implements today. Background, ambient lighting,
environment maps, tone mapping, fog, and post-processing can evolve through a
versioned descriptor without changing scene hierarchy semantics. Godot uses an
environment resource in a similar role; see
[Environment and post-processing](https://docs.godotengine.org/en/stable/tutorials/3d/environment_and_post_processing.html).

### Animation, physics, and navigation are runtime systems

Scene entries declare animation players, bodies, shapes, navigation regions,
or agents, but the scene tree should not expose every internal mixer, broadphase
index, or pathfinding data structure. Runtime systems build optimized state
from the declared composition and referenced resources.

JScene3D already has transform, morph, skeletal, and sprite animation
capabilities. Version 0.1 should serialize clips, stable property targets,
playback selection, looping, interpolation, baseline values, and
project-authored animation events. It also includes a basic project-authored
animation state machine; advanced blend trees and multidimensional blend spaces
remain later capabilities. Godot's split between property animation and a
higher-level animation tree demonstrates this distinction; see
[AnimationTree](https://docs.godotengine.org/en/stable/tutorials/animation/animation_tree.html).

Physics serialization distinguishes a body node from child collision-shape
nodes, each of which references reusable shape data. Static, kinematic,
character, rigid-body, sensor, and joint semantics are explicit in both
dimensions. The editor may offer derived collision generation, but imported
render meshes must not automatically become expensive dynamic triangle
collision. Godot likewise recommends primitive shapes for dynamic bodies and
concave shapes for static level collision. See
[3D collision shapes](https://docs.godotengine.org/en/stable/tutorials/physics/collision_shapes_3d.html).

Navigation should be an optional world capability rather than assumed actor
behavior. A navigation resource may be imported or baked from geometry;
regions, links, and obstacles contribute to a navigation world; an agent asks
for a path; and project Java code still owns movement and game decisions.
Godot's navigation documentation makes the same crucial distinction that path
queries do not move the parent actor. See the
[3D navigation overview](https://docs.godotengine.org/en/stable/tutorials/navigation/navigation_introduction_3d.html).

Navigation is not required to model the first Doom map interactions. Doomed
Corridors can continue using map topology and game-specific movement while the
navigation resource and runtime seam are designed against another real use
case. This avoids making Doom sector semantics the general navigation model.

### Visibility, particles, and level-authoring tools

Automatic mesh LOD, hierarchical visibility ranges, and baked occlusion are
different optimization mechanisms and should remain renderer capabilities with
inspectable project settings, not implicit scene-loader behavior. Godot's
documentation treats them separately and describes different tradeoffs for
per-mesh LOD, grouped replacements, and CPU-tested occluders. See
[mesh LOD](https://docs.godotengine.org/en/stable/tutorials/3d/mesh_lod.html),
[visibility ranges](https://docs.godotengine.org/en/stable/tutorials/3d/visibility_ranges.html),
and [occlusion culling](https://docs.godotengine.org/en/stable/tutorials/3d/occlusion_culling.html).

Particles need registered emitter and resource descriptors plus renderer
support, simulation lifecycle, bounds, and editor preview controls. They should
be planned as a capability family but not delay the version 0.1 composition
model. Godot's 3D particle documentation distinguishes CPU and GPU simulation,
process materials or shaders, collision, attraction, trails, and subemitters;
that breadth argues against pretending particles are merely animated meshes.
See [3D particle systems](https://docs.godotengine.org/en/stable/tutorials/3d/particles/index.html).

Constructive solid geometry, tile grids, and terrain are authoring strategies,
not universal scene foundations. Godot describes CSG as a useful level
prototyping tool that can be converted to mesh and describes `GridMap` as a
three-dimensional tile map backed by a mesh library. The stable documentation
does not present one built-in general terrain workflow. JScene3D should
therefore let specialized extensions contribute these tools and bake or import
ordinary scene resources. See [CSG tools](https://docs.godotengine.org/en/stable/tutorials/3d/csg_tools.html)
and [GridMap](https://docs.godotengine.org/en/stable/classes/class_gridmap.html).

### Coordinate and unit contract

World 3d is right-handed, positive Y is up, negative Z is forward, and one unit
is one metre. World 2d uses positive X to the right, positive Y downward,
clockwise positive visible rotation, and one unit per logical pixel. UI also
uses logical pixels but has a separate layout coordinate system.

Three-dimensional transforms serialize normalized quaternions. The Inspector
edits Euler angles in degrees, and authored angle properties use degrees.
Import adapters normalize source formats deliberately and record provenance
rather than let presentation code guess. The editor grid, Inspector, physics,
audio attenuation, navigation, and runtime use these same contracts.

Godot's explicit Y-up, right-handed, metre-based convention shows the product
value of settling this early, but JScene3D must document its own existing
mathematical convention rather than adopt a different one by analogy. See
[Godot's coordinate system](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html#coordinate-system).

## Registered type catalog

The editor and runtime need a shared description of every type that can appear
in project data. A registered type descriptor should include at least:

- a globally stable identifier;
- a display name and description;
- its allowed scope, such as scene node, node controller, project system,
  resource, or importer;
- a versioned configuration schema;
- defaults;
- editor categories, ranges, units, and resource-reference constraints;
- declared events and actions with payload descriptions;
- runtime implementation ownership;
- compatibility and migration information.

JSON Schema can validate document structure and simple property constraints.
It should not be stretched to describe runtime factories, lifecycle rules, or
all cross-resource relationships. Those belong in the registered type
descriptor and semantic validation.

The editor should construct its property inspector from descriptors rather than
hard-code knowledge of Doomed Corridors. A Doom door may then appear in the
editor because an installed extension contributes its descriptor, not because
the editor itself understands sectors.

### Extension Descriptor version 1

An extension artifact publishes its safe, generated descriptor at
`META-INF/jscene3d/extension.json`. Descriptor discovery reads only these
resources. It must not load implementation classes, invoke static initializers,
or use service providers while the editor is inspecting a project.

The first descriptor schema is bundled as
`META-INF/jscene3d/project/extension-1.schema.json` and has the canonical URI
`https://jscene3d.org/schemas/extension-1.json`. A representative descriptor is:

```json
{
  "$schema": "https://jscene3d.org/schemas/extension-1.json",
  "schemaVersion": 1,
  "id": "io.github.glynch.example-game",
  "version": "1.0.0",
  "engineRequires": ">=0.1.0-SNAPSHOT <0.2.0",
  "displayName": "Example Game",
  "types": [
    {
      "id": "io.github.glynch.example-game/actor-3d",
      "typeVersion": 1,
      "scope": "scene-node",
      "displayName": "Actor 3d",
      "properties": [
        {
          "id": "mesh",
          "valueKind": "reference",
          "displayName": "Mesh",
          "acceptedReferences": ["project", "import"]
        },
        {
          "id": "visible",
          "valueKind": "boolean",
          "defaultValue": true,
          "displayName": "Visible",
          "editor": {"group": "Rendering"}
        }
      ],
      "signals": [
        {
          "id": "died",
          "payload": {
            "type": "io.github.glynch.example-game/actor-event",
            "typeVersion": 1
          },
          "displayName": "Died"
        }
      ],
      "actions": [
        {"id": "activate", "displayName": "Activate"}
      ],
      "requiredCapabilities": ["org.jscene3d.render/mesh-3d"]
    }
  ]
}
```

The descriptor contains no Java implementation class names. The resolved
catalog indexes the exact pair of stable type identifier and positive
`typeVersion`. It validates type scope, property names and value kinds,
required properties, reference namespaces, signal and action existence, and
exact endpoint payload compatibility before runtime instantiation. Project
extension requirements and descriptor engine requirements are independent
semantic-version checks.

Property `editor` data is deliberately generic metadata in version 1. Naming
and semantics for broader property capabilities remain undecided; the initial
schema does not encode an animation-specific policy by accident.

### Stable type identifiers, not Java class names

Project data should contain:

```json
{
  "type": "io.github.glynch.doomed-corridors/zombieman-controller"
}
```

It should not contain:

```json
{
  "class": "io.github.glynch.doomedcorridors.combat.ZombiemanBehavior"
}
```

The owning extension maps a stable type identifier to an implementation. This permits
implementation refactoring, JPMS encapsulation, validation before
instantiation, and useful diagnostics when an extension is missing.

## Java extension scopes

### Node controller extension scope

A node controller has one instance for one scene node. A node has zero or one
controller in version 0.1. The controller receives only the scoped runtime
facilities and references it declares. Typical examples include actor movement,
an interactive switch, a camera controller, or a door mover.

```json
{
  "id": "door-motion",
  "type": "io.github.glynch.jscene3d/group-3d",
  "controller": {
    "type": "io.github.glynch.doomed-corridors/sector-door-controller",
    "properties": {
      "sector": "import:freedoom-map01/sector/12",
      "speed": 2.0,
      "wait": "PT4S",
      "movement": "open-wait-close"
    }
  }
}
```

### Project system

A project system is created once for a game session and is not attached to a
dummy scene entry. It may coordinate many instantiated scenes or survive scene
changes according to its declared lifecycle.

```json
{
  "$schema": "../schema/runtime-1.schema.json",
  "schemaVersion": 1,
  "systems": [
    {
      "id": "combat",
      "type": "io.github.glynch.doomed-corridors/combat",
      "properties": {
        "rules": "../game/combat.json",
        "presentation": "../game/combat-presentation.json"
      }
    },
    {
      "id": "campaign",
      "type": "io.github.glynch.doomed-corridors/campaign",
      "properties": {
        "definition": "../game/campaign.json"
      }
    }
  ]
}
```

Project systems receive declared narrow capabilities rather than process-wide
mutable singletons. Their base lifecycle contains startup and closure. Fixed or
frame updates are opt-in participant interfaces, and systems do not receive
renderer callbacks.

Ordering should be derived from explicit dependencies or a small number of
well-defined phases. A manually ordered list whose correctness depends on
undocumented positions would become part of the interface and be difficult for
the editor to validate.

### Import extension

An import extension participates only in explicit import operations. It reads a
source asset and returns imported resources plus structured diagnostics and
provenance. It must not mutate the source or silently write project-authored
documents.

### Application extension

Every project designates one application extension. It uses the same extension
contracts as additional dependencies and does not gain a privileged imperative
assembly API. Project systems and scene controllers provide the required
lifecycles without an invisible root node.

### Editor extension

The initial editor should derive ordinary property controls from type
descriptors and schemas. Custom editor panels or visual tools may later be
contributed by trusted Java extensions, but custom editor execution is a
separate security and lifecycle problem. It must not be required merely to show
the Project Manifest or raw JSON configuration.

## Conceptual Java interfaces

The following sketches illustrate responsibility and depth. They are not
proposed source-ready signatures.

```java
public interface ProjectExtension {
    ExtensionDescriptor descriptor();

    void contribute(GameTypeCatalog catalog);
}
```

```java
public interface ProjectSystem extends AutoCloseable {
    void start(ProjectRuntimeContext context);
}
```

Separate opt-in interfaces participate in fixed or frame updates. Exact names
must be tested against combat, campaign progression, save management, and other
real systems before becoming public API.

The external runtime seam is:

```java
ProjectRuntimeLoadResult load(GameProject project, ClassLoader extensions);
```

An overload accepts an already resolved `RegisteredTypeCatalog` and trusted
runtime-extension instances for embedded launchers and deterministic tests.

The implementation should hide scene parsing, type resolution, dependency
ordering, resource sharing, instance overrides, semantic validation, and
diagnostic ordering. The returned runtime integrates with the existing
`GameApplication` and `GameRuntime` lifecycle rather than introducing a second
competing game loop.

## Event connections and imperative code

Connections authored as part of game composition should normally be serialized
so the editor can display and reproduce them. Examples include:

- a button action requesting a new game;
- a switch activating a door;
- player health changes updating the HUD;
- an actor's death spawning a configured drop;
- reaching an exit requesting a scene transition.

Java implementations may still create internal subscriptions that are
implementation details. The distinction is whether changing the relationship
is a project-authoring decision. If a developer should be able to reconnect it
in the editor, it belongs in project data.

Connections require declared, typed endpoints. A descriptor might declare:

```json
{
  "events": {
    "died": {
      "payload": "jscene3d/entity-reference"
    }
  },
  "actions": {
    "spawn": {
      "payload": "jscene3d/spawn-request"
    }
  }
}
```

An event descriptor must define its payload type, delivery cardinality, and the
lifecycle during which it can be emitted. An action descriptor must define its
accepted payload type, whether it requires a live target instance, and its
failure behavior. Scene validation resolves both endpoints and rejects an
incompatible payload before play. Dispatch order for multiple connections must
be deterministic. Removing a target during dispatch must have specified
snapshot or immediate-removal semantics rather than depend on collection
implementation details.

The recommended version 0.1 event contract is deliberately smaller than a
general message bus:

- registered scene nodes, node controllers, and project systems declare named
  signal or event outputs and action inputs in their descriptors;
- payload types use stable registered identifiers with schemas the editor and
  headless validator can inspect;
- a serialized connection targets an action identifier, never an arbitrary
  Java method name;
- emission and action delivery occur on the owning game-loop thread;
- connections from one event are delivered in their preserved declaration
  order using snapshot semantics, so removing a target does not corrupt the
  current dispatch;
- no-payload events and exact compatible payload forwarding are supported;
- payload conversion, filtering, aggregation, request-response, persistence,
  and cross-process delivery are deferred until real cases justify them;
- a target that disappears with its scene instance is disconnected as part of
  that instance's lifecycle, while an invalid authored target is a validation
  diagnostic.

This contract should support editor wiring for a switch activating a door, an
actor death requesting a drop, a timer expiring, and health changes updating a
HUD. It does not prevent a Java implementation from using ordinary Java
listeners internally when that relationship is not project-authored.

Events are notifications rather than mutable shared state. Request/response
operations and resource lookup should use explicit runtime interfaces instead
of pretending every interaction is an event. Godot's signals are a useful
reference for decoupling observers from emitters, but JScene3D still needs its
own payload, lifecycle, and diagnostic contracts. See
[Using signals](https://docs.godotengine.org/en/stable/getting_started/step_by_step/signals.html).

The first version should avoid a general expression language. Constants,
resource references, and compatible event payload forwarding may be enough for
initial doors, switches, drops, menus, and HUD updates. Transformation
expressions should be added only when concrete connections cannot otherwise be
represented cleanly.

## Input actions

Input maps are project resources, not hard-coded launcher behavior. Node
controllers and project systems consume semantic actions such as
`move-forward`, `turn-left`, or `fire`, while an input map binds keys, mouse
buttons, pointer motion, and controllers.

The existing `jscene3d-game` `InputMap` and `ActionSnapshot` provide the runtime
foundation. A native input-map resource should be a serializable description
that creates the same runtime model.

```json
{
  "$schema": "../schema/input-map-1.schema.json",
  "schemaVersion": 1,
  "actions": {
    "move-forward": [
      { "device": "keyboard", "key": "W" },
      { "device": "keyboard", "key": "UP" }
    ],
    "turn-left": [
      { "device": "keyboard", "key": "LEFT" }
    ],
    "fire": [
      { "device": "mouse", "button": "LEFT" },
      { "device": "keyboard", "key": "SPACE" }
    ]
  }
}
```

Defaults supplied by an extension may seed a new project, but once bindings are
part of a project they must be data the editor can inspect and change.

## Source import architecture

### Import orchestration seam

JScene3D already has more than one concrete source-format case: glTF and WAD.
That provides evidence for a shared import orchestration seam even though their
decoders and output capabilities differ.

Shared orchestration responsibilities include:

- source selection and project-relative path safety;
- importer selection by stable identifier;
- importer settings validation;
- source, dependency, settings, and importer-version fingerprints;
- structured diagnostics and progress;
- deterministic output identity;
- provenance;
- cache invalidation and regeneration;
- cancellation;
- presentation of import options to the editor.

The seam should not force every format into one lowest-common-denominator data
model. A glTF adapter and Doom WAD adapter may produce different registered
resource types while sharing orchestration, diagnostics, provenance, and cache
behavior.

### Prepared import transaction

Import preparation and publication use a settled prepare, preview, and commit
transaction. Preparation performs expensive source decoding exactly once and
writes candidate artifacts into an isolated `TemporaryWorkspace`. It returns an
owned `PreparedImport` whose immutable preview describes diagnostics, source
and dependency changes, stable output identities, provenance, output changes,
and estimated size.

```java
try (PreparedImport prepared = importManager.prepare(project, definition)) {
    ImportPreview preview = prepared.preview();
    if (preview.isValid()) {
        prepared.commit();
    }
}
```

The editor can present the preview and request confirmation. A headless build
can apply the same transaction immediately after checking its diagnostics.
`commit()` verifies that the prepared source fingerprint is still current and
then atomically publishes the staged artifacts to the disposable import cache.
Closing an uncommitted transaction deletes its staging workspace. A committed
transaction remains terminal and closing it releases any remaining staging
resources without deleting published artifacts.

The engine-owned import module controls source resolution, staging paths,
fingerprints, cache transactions, diagnostics, progress, cancellation, and
provenance. A format adapter receives an import context and definition and
returns named artifacts. It does not select cache paths, mutate authored
project files, or publish its own results. This keeps editor and command-line
behavior identical while allowing glTF and Doom adapters to produce
different registered resource types.

### Imported artifact kinds

Importers produce serialized project artifacts rather than live Java runtime
objects. Version 1 supports three artifact kinds:

- `SCENE` for a complete scene definition;
- `RESOURCE` for a typed project resource definition;
- `PAYLOAD` for opaque data referenced by a resource definition, such as image
  pixels, mesh buffers, audio data, or compiled shader data.

Every artifact has a deterministic importer-local output identity, an artifact
kind, a content fingerprint, and provenance identifying its source elements.
Resource artifacts also identify their registered resource type. Artifacts
declare references to other outputs so the orchestrator can validate the
resulting graph before publication.

The adapter chooses logical output identities and content. The import module
chooses physical staging and cache paths. This permits large payloads to be
written directly to staging instead of being retained as byte arrays, while
keeping physical machine paths out of the portable project model.

For example, one Doom import may publish:

```text
import:freedoom-map01/map
    kind: SCENE

import:freedoom-map01/textures/STARTAN3
    kind: RESOURCE
    type: io.github.glynch.jscene3d/texture-2d

import:freedoom-map01/payload/textures/STARTAN3
    kind: PAYLOAD
    mediaType: image/png
```

### Source inspection before import

Source inspection is a settled read-only operation performed before an import
definition is authored. It allows tools to discover selectable content and
source-dependent option values without creating cache entries or changing
project files.

```java
SourceInspection inspection =
        importManager.inspect(project, assetReference, importerId);
```

An inspection contains the importer identity and version, source and dependency
fingerprints, structured diagnostics, and discovered source items. Each source
item has a stable source-local identity, an adapter-qualified kind, a display
label, selectability metadata, properties, and relationships to other items.
Relationships form a graph rather than requiring a tree because formats such as
glTF permit meshes, materials, skins, and animations to be shared. An editor
may derive suitable tree presentations from that graph.

Static importer metadata describes available settings and their editor
presentation without reading a source. Inspection supplies dynamic values such
as the maps present in one WAD or the scenes and animations present in one glTF
document. A chosen source-item identity and settings become authored import
definition data. Headless import can skip inspection when that definition
already exists, although preparation still validates every selected identity
against the current source.

### Importer registration

Importer metadata is declared as a registered extension type with `IMPORTER`
scope. Tools can therefore discover, validate, and present an importer without
executing its Java implementation. Executable implementations use a separate
import-time extension seam:

```java
public interface ProjectImportExtension {
    String id();

    void register(ProjectImportRegistry registry);
}
```

`ProjectImportRegistry` verifies that each implementation corresponds to a
declared importer owned by the same extension, that its definition version is
compatible, and that no duplicate implementation exists. The registry rejects
runtime node, controller, system, and resource factories in importer slots.

JPMS provider discovery and explicitly supplied host extensions are both
supported, matching runtime extension composition. A single extension may
contribute runtime types, importers, or both while their discovery and
lifecycles remain independent. The editor loads executable import extensions
only when inspection or preparation is requested.

### Import selection and settings

An import definition stores `selection` as an ordered, duplicate-free list of
stable source-item identities returned by inspection. The same structure
supports one WAD map, one glTF scene, multiple animations, or any later source
format without placing importer-specific field names in the generic schema.

Importer-wide `settings` are portable project values validated against the
property descriptions on the registered importer type. Static defaults come
from the descriptor, while source-dependent choices come from inspection. An
authoring tool writes the effective values of all settings known when it
creates a definition so consequential choices remain visible in source
control. A definition created against an older importer may omit settings added
later and receives the newer descriptor's declared defaults for those settings.

```json
{
  "selection": ["maps/MAP01"],
  "settings": {
    "skill": "hurt-me-plenty",
    "includeMultiplayerThings": false
  }
}
```

### Per-source-item import settings

An import definition may configure individual source items through an optional
`itemSettings` object keyed by stable identities from source inspection. Root
selection and item configuration remain distinct: `selection` determines the
roots whose dependency closure is imported, while `itemSettings` configures any
selected or transitively reachable item.

```json
{
  "selection": ["scenes/0"],
  "settings": {
    "importAnimations": true
  },
  "itemSettings": {
    "meshes/Body": {
      "generateCollision": true,
      "materialMode": "preserve"
    },
    "animations/Walk": {
      "loop": true
    }
  }
}
```

Importer metadata declares item-setting property descriptions by
adapter-qualified source-item kind. Inspection associates each item with one
of those kinds, allowing the editor and headless validator to apply the same
property rules.

A setting keyed by an identity absent from the current source is an error
because authored configuration has become stale. An invalid property for the
item kind is also an error. Configuration for an existing item outside the
selected dependency closure produces a warning and has no effect. Items
without authored settings use the importer defaults declared for their kind.

### Illustrative import declaration

```json
{
  "$schema": "../schema/import-1.schema.json",
  "schemaVersion": 1,
  "id": "freedoom-map01",
  "source": "asset:freedoom",
  "importer": "io.github.glynch.jscene3d.doom/map",
  "selection": ["maps/MAP01"],
  "settings": {
    "skill": "hurt-me-plenty",
    "includeMultiplayerThings": false
  }
}
```

The imported outputs receive logical identities such as:

```text
import:freedoom-map01/map
import:freedoom-map01/sector/12
import:freedoom-map01/linedef/47
import:freedoom-map01/thing/86
```

This syntax is illustrative. The requirement is stable logical identity, not a
particular URI grammar.

### Authoritative source and disposable cache

An import result is determined by:

```text
source bytes
+ referenced source dependencies
+ import settings
+ importer identifier and version
= imported resources
```

Derived data belongs in a cache that can be deleted and rebuilt. Project files
must never rely on an undocumented machine-specific absolute path. Importing
must not rewrite or normalize the original WAD or glTF source.

### Cache location and atomic publication

The host supplies the resolved safe extension catalog and cache root when it
creates the import manager. The catalog lets executable importer registrations
be checked against descriptor ownership, scope, and version:

```java
ImportManager.create(project, registeredTypeCatalog, cacheRoot, importExtensions)
```

This keeps cache placement out of the portable project schema. The editor and
command-line tools should initially default to a hidden project-local cache at
`<project>/.jscene3d/cache/`, which is ignored by version control. A command-line
override permits externally persisted CI caches. Export uses an isolated build
cache or a previously populated cache whose generations have been validated.

The physical layout is engine-managed infrastructure rather than an authored
project convention and may evolve without changing project files. A useful
initial layout is:

```text
cache/
  imports/
    freedoom-map01/
      <complete-fingerprint>/
        artifact-index.json
        scenes/
        resources/
        payloads/
  staging/
```

The complete fingerprint covers the source bytes, referenced source
dependencies, selection, settings, per-item settings, importer identifier, and
importer version. Published generations are immutable.

Preparation writes a complete candidate generation beneath `staging/` on the
same file system as the published cache. Commit rechecks the source fingerprint,
validates the candidate, obtains a lock for the import definition, and publishes
the generation with an atomic move. A failed preparation or commit removes its
staging area and leaves the previous generation untouched. If the file system
cannot provide the required atomic publication guarantee, commit fails
explicitly instead of exposing a partially written generation.

Readers retain a generation while using it. Garbage collection may remove old
generations only after their readers release them. Logical `import:` references
resolve through the artifact index and never reveal cache paths. Export follows
the referenced artifact closure rather than copying the cache wholesale.

Secure staging beneath a caller-supplied cache root uses the shared
`TemporaryWorkspace.create(parent, prefix)` facility rather than duplicating
temporary-directory policy in import orchestration.

This follows a proven editor workflow. Godot keeps original source assets,
commits small per-asset import settings, writes converted results to a hidden
cache, detects source changes, and can regenerate the cache after deletion.
JScene3D should adopt those properties without copying the `.import` filename
or cache layout. See Godot's official
[import process](https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/import_process.html).

Complex three-dimensional sources need per-object import configuration and an
authored layer above imported content. Godot preserves the imported base and
supports advanced settings for meshes, materials, animation, physics, and
navigation, plus inherited scenes for durable authored changes. For JScene3D,
the corresponding recommendation is an import recipe plus deterministic
imported identities and authored composition or overlays. See
[Importing 3D scenes](https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/index.html)
and [Advanced import settings](https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/import_configuration.html).

### Import execution, progress, and cancellation

Import operations are synchronous. `ImportManager.inspect()`,
`ImportManager.prepare()`, and `PreparedImport.commit()` perform their work on
the calling thread and do not create implicit background threads. The caller
owns execution policy: the editor submits operations to its worker executor,
while command-line and build tools may invoke them directly. The editor also
owns transfer of progress and completion notifications onto its Java UI thread.

The context passed to an importer provides cooperative cancellation and
progress reporting. Importers check cancellation at I/O boundaries and at
bounded intervals during long decoding or conversion loops. Cancellation is a
distinct operation outcome rather than a validation error. It closes the
prepared import, removes its staging generation, and never replaces the last
published generation.

Progress is hierarchical and may identify phases such as inspecting, reading,
decoding, writing, validating, and committing. A progress update contains a
phase, a human-readable activity description, and optional completed and total
work when the importer can measure them. It may also identify the source item
being processed. Progress callbacks execute on the calling thread and must not
encode assumptions about an editor toolkit.

An import manager supports concurrent independent operations. Imports with
different identities may prepare and publish concurrently; the per-import lock
serializes publication for the same import identity. A `PreparedImport` is
single-use and is not thread-safe. These rules make concurrency explicit in the
module interface while keeping thread creation and UI scheduling outside the
import module.

### Import state and reimport policy

`ImportManager.status()` performs a synchronous, read-only evaluation of an
import definition and reports one of four states with structured diagnostics:

- `CURRENT` means a complete published generation matches the current source,
  dependencies, configuration, and importer version;
- `MISSING` means no complete generation exists;
- `STALE` means a previous complete generation exists but its complete
  fingerprint no longer matches;
- `BLOCKED` means the desired state cannot be evaluated or prepared, for
  example because the source, dependency, or importer is unavailable or the
  definition is invalid.

File watching and debounce policy belong to the host. An editor may watch known
source and dependency paths and request status checks, but the import module
does not create watcher threads. A watch notification is only a hint: status is
determined from validated fingerprints rather than assumed from the event.

Automatic reimport is an editor preference. It invokes the same `prepare()` and
`commit()` transaction as an explicit user request; there is no second import
path with weaker guarantees. The editor may continue rendering the last
successfully published generation while clearly marking it stale or displaying
the diagnostics from a failed reimport.

Play and export require every referenced import to be `CURRENT`. They do not
silently consume a stale generation. A failed reimport preserves its previous
generation for editor inspection but does not make that generation current.
Opening an untrusted project may inspect declarative metadata, but it never
loads or executes import extensions automatically.

### Authored composition over imported content

The imported MAP01 scene should not be the whole application scene. A
project-authored scene can instantiate the imported map and compose it with a
player, HUD, project systems, presentation, and additional authored content.

```json
{
  "id": "map01-gameplay",
  "root": {
    "id": "map01",
    "type": "jscene3d/game-root",
    "children": [
      {
        "id": "imported-world",
        "instance": "import:freedoom-map01/map"
      },
      {
        "id": "player",
        "instance": "../actors/player.scene.json"
      },
      {
        "id": "hud",
        "instance": "../ui/doom-hud.scene.json"
      }
    ]
  }
}
```

If project authors need to alter imported objects, overrides should live in an
authored overlay keyed by deterministic imported identity. The cache remains
regenerable, and reimport can diagnose an overlay whose source target no longer
exists.

## WAD support

### WAD archive capability

WAD archive support should be available as a reusable optional JScene3D
capability rather than remain private forever inside Doomed Corridors. Its
genre-neutral responsibilities are:

- validate a WAD header and directory;
- distinguish supported archive kinds such as IWAD and PWAD without assigning
  gameplay meaning;
- enumerate and locate named lumps while preserving order and duplicates;
- expose bounded raw lump data safely;
- support explicit archive layering where specified;
- produce structured diagnostics;
- retain source provenance;
- avoid graphics, audio, physics, and game-rule dependencies.

This is the `jscene3d-wad` artifact. Its public interface exposes validated
archive provenance, ordered opaque lump metadata, bounded caller-owned streams,
allocation-limited convenience reads, and explicit low-to-high precedence
archive layers. It does not invent a `WadReader` interface or multiple
interchangeable readers. Supporting WAD files does not itself require an
adapter hierarchy.

### Generic WAD project import

The separate `jscene3d-wad-import` adapter connects that archive capability to
the generic project import lifecycle. Its registered importer identity is
`io.github.glynch.jscene3d.wad/archive`; it is discoverable as both a JPMS
provider and a class-path service.

Inspection returns one selectable `archive` root with `contains` relationships
to every directory entry, plus one selectable item for each opaque lump. Lump
identities combine the zero-padded directory index with the hexadecimal WAD
name, for example `lumps/00000042/5448494E4753`. The index distinguishes
duplicate names, and including the name prevents a reordered archive from
silently resolving an authored selection to unrelated content.

Selecting the archive root imports all contained lumps. Selecting individual
lump items imports only those payloads. Each preparation also writes an
`archive/index` JSON artifact containing the archive kind, portable source
provenance, complete directory order, lump metadata, and the artifact identity
of each selected lump. Absolute source paths are deliberately excluded from
the artifact. This layer interprets neither Doom structures nor lump names.

### Doom content interpretation

The following capabilities are necessarily Doom-family-specific even though
they are reusable by more than one game:

- classic Doom map lump groups;
- vertices, linedefs, sidedefs, sectors, BSP nodes, subsectors, segs, reject
  tables, blockmaps, and things;
- palettes, colormaps, patches, composite wall textures, flats, and sprites;
- Doom sound and music encodings;
- map markers and Doom naming conventions;
- vanilla Doom and Doom II compatibility diagnostics.

These belong in the optional `jscene3d-doom` extension layered over the WAD
archive capability. Its first implemented slice discovers conventional
`MAP##` and `E#M#` markers, decodes every classic map record into an immutable
model, validates cross-record references and binary structures, and exposes a
service-discovered project importer. A selected map produces a deterministic,
pretty-printed resource of type `io.github.glynch.jscene3d.doom/map` with
portable WAD provenance. Material, image, audio, geometry, collision, and
gameplay conversion remain later slices. Doom concepts do not enter the Game
Engine or Physics Engine.

### Doom gameplay interpretation

Actor types, monster state machines, weapons, damage, ammunition, keys,
inventory, sector motion, switch behavior, exits, and campaign progression are
gameplay semantics. Doomed Corridors may implement them through registered
types and project data.

A later reusable Doom-runtime extension would be justified only if it has a
clear supported compatibility profile and consumers beyond one title. It
should not be created merely to move application code into the engine
repository.

### Meaning of out-of-the-box support

Out-of-the-box WAD support should mean that a standard JScene3D tool or editor
distribution can include the WAD archive and Doom extensions, recognize
supported WAD sources, inspect their contents, offer applicable importers, and
create native/imported resources without requiring each developer to write a
WAD parser.

It should not mean that every JScene3D application depends on Doom types, or
that loading an arbitrary WAD automatically produces a fully playable Doom
game.

## Modeling upcoming Doomed Corridors features

The next features should validate the architecture rather than bypass it.

### Enemy ammunition drops

The fact that a zombieman drops an ammunition clip is project-authored gameplay
configuration. It can be represented as a declared death connection or a drop
configuration consumed by a registered child node and controller.

```json
{
  "root": {
    "id": "zombieman",
    "type": "io.github.glynch.jscene3d/character-body-3d",
    "controller": {
      "type": "io.github.glynch.doomed-corridors/zombieman-controller"
    },
    "children": [
      {
        "id": "death-drop",
        "type": "io.github.glynch.jscene3d/group-3d",
        "controller": {
          "type": "io.github.glynch.doomed-corridors/drop-spawner-controller",
          "properties": {
            "scene": "../pickups/ammunition-clip.scene.json",
            "chance": 1.0
          }
        }
      }
    ]
  },
  "connections": [
    {
      "from": "node:zombieman/died",
      "to": "node:death-drop/spawn"
    }
  ]
}
```

The Java implementation performs spawning and owns runtime correctness. The
choice of dropped scene and chance remain visible and editable.

### Doors

A Doom door combines imported source identity, activation rules, sector motion,
timing, collision updates, sound, and presentation. The WAD adapter recognizes
the linedef special and produces deterministic imported metadata. Registered
Doomed Corridors controllers and project systems interpret that metadata.

Properties likely to be editable include activation mode, repeatability, key
requirement, speed, destination rule, wait duration, sound set, and target
sector. The runtime implementation owns interpolation, collision consistency,
and state transitions.

### Switches

A switch is a useful proof of serialized connections:

```json
{
  "from": "node:switch-47/activated",
  "to": "node:door-12/activate"
}
```

The WAD adapter can generate that relationship from tags and specials. An
author can inspect and, where allowed, override it in the editor. The switch's
visual state and sound are resources referenced by its registered controller.

### Lifts, teleports, exits, and sector effects

These should use the same registered-type, property, identity, and connection
mechanisms. If each feature requires a new hard-coded startup branch, the scene
and extension design has failed to provide sufficient leverage.

## Editor workflow

The editor should expose three deliberately different operations:

- **Edit scene** renders project data in an editor-owned 2d, 3d, or UI viewport
  with editor cameras, selection, transform gizmos, grids, debug overlays, and
  optional preview lighting. It must not start the game loop or execute project
  controllers merely to display standard registered types.
- **Play current scene** starts a playable session at the open scene. The
  application extension, project systems, input, events, physics, audio, and
  services are the same as Play Project; only the entry scene changes.
- **Play project** starts from the Project Manifest's entry scene exactly as a
  packaged application would.

Godot exposes an editor 3D workspace separately from running a scene, and its
playable embedded view still runs in another process so a game crash does not
crash the editor. It also offers pause, frame advance, runtime inspection,
camera override, and a clear warning that edits to runtime state are not saved.
These are strong product references for JScene3D. See
[Introduction to 3D](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html)
and [Game embedding](https://docs.godotengine.org/en/stable/tutorials/editor/game_embedding.html).

For JScene3D, edit-scene rendering should initially run in the editor process
using trusted engine adapters and descriptor-driven representations. Both play
operations should run the game in a child JVM. A separate game window is the
smallest first implementation; visually embedding that native window can come
later without moving gameplay into the editor JVM. Process isolation prevents a
project crash, `System.exit`, runaway game loop, or native renderer failure from
taking down the editor.

The editor launches the project's Maven Wrapper through a declared Maven build
adapter rather than assuming a globally installed Maven or one POM layout. The
project build owns Java compilation and dependency resolution. The version 0.1
child protocol reports lifecycle state and structured failures. Scene-tree
snapshots, runtime property inspection, embedded play, and hot synchronization
are later protocol capabilities and must not imply that runtime mutations are
saved.

The complete conceptual sequence is:

1. Load `project.json` safely through `jscene3d-project`.
2. Display safe identity, authorship, icon, compatibility, source assets, and
   extension diagnostics without executing extension code.
3. After the project is trusted, invoke the Maven build adapter in a child JVM
   and discover extension descriptors.
4. Validate referenced resources against structural schemas and registered
   semantic rules.
5. Inspect source assets and run explicit or automatic deterministic imports.
6. Present project and imported resources in one resource browser while keeping
   their provenance distinct.
7. Present scene hierarchy, scene instances, properties, and connections.
8. Save changes to the same versioned documents consumed by headless tools and
   standalone games.
9. Render standard scene entries in the edit viewport without starting a game
   session.
10. Launch current-scene or project play in a child JVM through the same
    project-opening and runtime interfaces as the packaged application.
11. Build exports from named, validated export presets.

Editor-only state such as panel layout, expanded tree nodes, recent selections,
editor cameras, trust, local toolchain overrides, and window position stays in
the operating system's application-data area outside shared project
definitions.

## Export and distribution

Export is not a copy of the source directory. It is a reproducible build that
walks the project's transitive resource graph, compiles Java code, resolves
runtime dependencies, chooses platform-native libraries, and produces a
launchable distribution for one declared target.

Godot separates named export presets from export templates and can emit either
a playable build or a content pack. It also distinguishes committed presets
from local credentials. The exact mechanism is different for Java, but the
product concepts transfer well. See [Exporting projects](https://docs.godotengine.org/en/stable/tutorials/export/exporting_projects.html)
and [PCK files](https://docs.godotengine.org/en/stable/tutorials/export/exporting_pcks.html).

An illustrative JScene3D export preset is:

```json
{
  "$schema": "../schema/export-preset-1.schema.json",
  "schemaVersion": 1,
  "id": "macos-arm64",
  "platform": "macos",
  "architecture": "aarch64",
  "format": "application-image",
  "content": {
    "additional": ["licenses/THIRD-PARTY.txt"]
  },
  "java": {
    "runtimeImage": "linked",
    "options": ["-Xms256m", "-Xmx2g"]
  }
}
```

The syntax and values are illustrative. Version 0.1 uses a generic JScene3D
launcher and the Project Manifest's application definition rather than a
project main class. Content selection follows typed references so a scene,
resource, imported output, source asset, provenance record, or license is not
accidentally omitted. Globs may deliberately add otherwise unreferenced content
but are not the primary closure mechanism.

Maven should remain the Java build authority. An export component can ask the
project build for compiled application artifacts and a resolved runtime
classpath or JPMS module path, stage the selected project resources, validate
native classifiers, and invoke JDK packaging tools. Oracle's `jlink` creates a
custom runtime image from a selected JPMS module closure, while `jpackage`
creates self-contained application images and native package formats and can
invoke `jlink`. See the JDK 21 [`jlink`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html)
and [`jpackage`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
manuals.

Exports are platform and architecture specific because the JDK packaging tools
produce packages for the host platform and LWJGL dependencies contain native
libraries for a target. Therefore macOS, Windows, and Linux outputs should be
built and smoke-tested on corresponding build hosts. The preset is portable;
the generated package is not.

Version 0.1 certifies a self-contained application image for
`macos-aarch64`, including a bundled Java runtime and deterministic dependency
report. `windows-x86_64` and `linux-x86_64` follow. Installers, signing,
notarization, content-only packs, and additional triples can follow.
Credentials, certificates, and passwords remain outside committed project
data.

True ahead-of-time native executables for macOS, Windows, and Linux, requiring
no installed or bundled JVM, are a long-term requirement. The generic launcher
and extensions must therefore remain compatible with build-time discovery,
closed-world resource metadata, foreign-function declarations, and target
native libraries instead of assuming dynamic runtime classpath discovery.

## Trust and extension execution

Project metadata loading and Java extension execution must remain separate.
Opening an unfamiliar project browser entry should not execute arbitrary code.

A possible trust sequence is:

```text
safe manifest load
-> dependency and compatibility inspection
-> explicit project trust
-> extension discovery and loading
-> full resource validation and import
-> edit or run
```

The security design distinguishes restricted and trusted modes. Trust applies
to a canonical project directory and is stored only in local editor state. In
trusted mode, Maven may download dependencies and extension code may execute in
child processes. Those processes provide failure isolation, not a security
sandbox. New executable dependencies require a renewed warning.

## Diagnostics

Project, scene, resource, extension, and import failures should use a common
diagnostic shape where practical:

- severity;
- feature-owned `DiagnosticCode` implemented by an enum;
- stable machine-readable code that also serves as a localization key;
- an English default message used as a fallback when no translation exists;
- source resource;
- machine-navigable location;
- immutable language-neutral details for variable values;
- related locations where a relationship spans documents;
- optional remediation metadata for the editor.

`DiagnosticCode` belongs to the core diagnostic interface. Each feature owns
its enum, such as `WadDiagnosticCode`, so adding or translating WAD diagnostics
does not modify a central catalogue. Diagnostic producers select codes and
retain variable values as structured details. They do not select a locale or
permanently interpolate those values into English text. A command-line tool or
editor resolves the stable code through its resource bundles and falls back to
`defaultMessage()` when no localized entry exists.

Expected invalid project data is diagnostic output, not an exception. Caller
contract violations and unexpected implementation failures remain exceptions.
Diagnostics should be deterministic so command-line output, tests, and editor
problem lists agree.

## Testing strategy

The project definitions and registered-type descriptors must be testable
headlessly. The principal test surface should cover observable behavior through
the loader, importer, scene-instantiation, and runtime interfaces rather than
implementation helpers.

Required design proofs include:

- Project Manifest loading does not initialize graphics, audio, imports, or
  extensions.
- A scene with nested scene instances loads with stable identities and shared
  resources.
- Missing types and extensions produce navigable diagnostics.
- Invalid properties, connections, and reference cycles fail deterministically.
- Scene instance overrides do not mutate the reusable source scene.
- glTF and WAD adapters use the same import orchestration interface.
- repeating an unchanged import produces equivalent identities and content.
- changing source bytes, settings, or importer version invalidates derived
  output.
- deleting imported cache data loses no project-authored information.
- an authored overlay survives reimport and diagnoses missing imported targets.
- a project system can be tested without a window or renderer where its
  behavior is headless.
- editor-produced project data loads through the same headless interface as
  hand-authored data.
- a standalone application and editor play mode instantiate the same project
  composition.
- edit-viewport rendering does not execute project systems or persist editor
  cameras and preview lighting into the game scene.
- current-scene play and project play select different entry points but use the
  same scene-instantiation and runtime components.
- a crashing child game process leaves the editor process running and reports a
  structured termination diagnostic.
- an export includes the transitive scene and resource closure, selected
  imported content, Java runtime dependencies, licenses, and correct native
  libraries for its declared target.
- a generated application image starts without a developer JDK or machine-local
  Maven repository.

## Version 0.1 authoring boundary

Version 0.1 should prove a complete vertical authoring path, not attempt to
serialize every existing engine class. The following catalog is concrete enough
to design schemas and editor inspectors while leaving final stable type
identifiers for the grill.

### Version 0.1 scene-node catalog

The names below describe capabilities rather than final serialized type
identifiers or a Java inheritance hierarchy.

| Node family | Version 0.1 responsibility |
| --- | --- |
| `Group` | Ordered dimension-neutral composition without a transform |
| `Group2d` and `Group3d` | Transform-only grouping in the corresponding world dimension |
| Scene instance | Another scene definition plus bounded property overrides and a stable instance identity |
| World-2d rendering | `Sprite2d`, `Camera2d`, and `TileMap2d`; exact mesh and light nodes remain to be settled during the grill |
| World-3d rendering | `MeshInstance3d`, `InstancedMesh3d`, `Sprite3d`, dimension-specific cameras, and the existing light families using lowercase `3d` suffixes |
| `Animator` | Clip library, playback, stable property targets, baseline pose, and an optional project-authored animation state machine |
| Bodies in both dimensions | `StaticBody2d` or `3d`, `KinematicBody2d` or `3d`, `CharacterBody2d` or `3d`, and `RigidBody2d` or `3d` |
| Collision in both dimensions | Child `CollisionShape2d` or `3d`, `Sensor2d` or `3d`, collision filters, physics materials, and a bounded initial joint set |
| Audio | Dimension-specific spatial listener and source nodes plus a dimension-neutral non-spatial player |
| `Timer` | Duration, one-shot or repeating mode, autoplay, pause behavior, and timeout signal |
| UI composition | `UiCanvas` and a minimal family of layout, image, text, and interactive controls separate from world 2d |

Tags are inspectable labels for lookup and categorization; they do not replace
stable identities or typed connections. A node may have one optional controller
and does not contain a list of behaviors. Collision placement uses transformable
child shape nodes that reference immutable shape resources, allowing compound
bodies and independently positioned sensor shapes without turning shapes into
rendered meshes.

A shader is a resource used by a material, not a scene entry. Raw shader source,
declared uniforms, uniform types, defaults, ranges, and resource constraints
belong to the shader descriptor. A mesh is also a resource rather than a scene
entry; a mesh resource contains surfaces and geometry, while material assignment
occurs by surface with a bounded entry-level override. This keeps shader
programming and mesh data reusable and prevents renderer buffers from leaking
into project documents.

### Version 0.1 resource catalog

- scene definition and input map;
- image, texture, and texture region;
- tile set, tile-map layers, and their collision metadata;
- mesh and instanced-transform data;
- standard material, shader material, and shader source;
- environment settings and environment map;
- property, transform, morph, skeletal, and sprite animation clips plus a basic
  animation-state resource;
- audio clip;
- font and a minimal user-interface theme and layout resource;
- supported 2d and 3d primitive collision shapes, physics materials, and
  deterministic imported or generated static collision geometry;
- project configuration resources registered by extensions;
- import recipes and logical references to imported outputs.

Font, minimal theme, and layout resources must be designed with the
user-interface component and are required for the version 0.1 menu and HUD
proof. A complete widget theme system is long-term scope; the initial resources
must not be improvised as image-specific properties.

### Version 0.1 runtime systems

- project loading, type registration, semantic validation, and migration;
- scene resolution, instantiation, overrides, lifecycle, and stable addressing;
- resource resolution, sharing, and closure;
- world-2d and world-3d rendering, camera selection, input, animation, and audio;
- certified 2d and 3d physics backends behind the common physics model;
- direct signals, project event routing, registered node controllers, and
  project systems;
- deterministic import and reimport orchestration;
- child-JVM current-scene and project play;
- host-platform application-image export.

Navigation, particle simulation, LOD authoring, hierarchical visibility,
occlusion baking, sub-viewports, CSG, world-3d grid editing, terrain tools,
advanced animation blend graphs, embedded play, runtime inspection, hot
synchronization, content packs, installers, and signing are long-term
capabilities. The scene and descriptor formats should leave room for registered
types in these families; version 0.1 does not need empty placeholders for each
one.

### Long-term scene-entry families

Long-term support should be prioritized by demonstrated game and editor needs,
not by an attempt to match another engine class for class. The architecture
should nevertheless accommodate these families without a new scene format:

| Family | Candidate entries or authoring capabilities |
| --- | --- |
| Rendering | Three-dimensional text, decals, line and debug geometry, reflection and light probes, fog volumes, particle emitters, and renderer-specific effects |
| Output and compositing | Sub-viewports, render targets, multiple windows, cameras per output, screen-space and world-space UI composition, and post-processing volumes |
| Spatial authoring | Markers and sockets, paths and curves, transform constraints, editor gizmos, and large-world partitioning |
| Animation | Advanced blend trees, multidimensional blend spaces, skeleton and skin controls, inverse kinematics, root motion, cross-structure retargeting, and ragdoll integration |
| Physics | Additional joints, shape casts, persistent ray queries, soft bodies, vehicles, backend-specific advanced capabilities, and richer debug visualization |
| Navigation | Navigation worlds, regions, agents, links, obstacles, layers, baked meshes, graph navigation, and editor debug views |
| Visibility and scale | Automatic mesh LOD, authored HLOD replacement groups, occluders, visibility ranges, streaming cells, and background loading |
| Level construction | CSG blockout, reusable grid or tile libraries, spline-generated geometry, terrain extensions, collision generation, and navigation baking |
| Audio | Area-based ambience, reverb zones, buses, effects, snapshots, streaming sources, and richer listener routing |
| User interface | Complete layout containers, controls, focus and navigation, themes, localization, accessibility metadata, animation, and multiple resolutions |
| Extension-defined | Trusted registered scene entries with descriptor-driven properties, events, actions, previews, gizmos, and optional custom editor tools |

Several items may first exist as editor tools that bake ordinary versioned
resources. CSG, grid painting, occluder generation, navigation baking, and LOD
generation do not all need permanent runtime scene-entry types. That distinction
keeps the runtime catalog smaller while preserving capable visual authoring.

## Proposed responsibility placement

The following is a first allocation for discussion:

| Responsibility | Proposed owner |
| --- | --- |
| Safe Project Manifest loading and metadata | `jscene3d-project` |
| Native scene and resource schemas | Deepened `jscene3d-project` |
| Scene instantiation and project runtime composition | New `jscene3d-project-runtime` |
| Existing fixed/rendered game loop | `jscene3d-game` |
| Registered type descriptors and safe catalog validation | `jscene3d-project` |
| Import orchestration, provenance, and cache policy | A reusable import artifact justified by glTF and WAD adapters |
| glTF format interpretation | `jscene3d-gltf` adapter |
| Generic WAD archive access | Optional `jscene3d-wad` artifact |
| Doom content decoding and import | Optional `jscene3d-doom` artifact |
| Doom gameplay semantics | Doomed Corridors application extension and project resources |
| General 2d and 3d physics model | `jscene3d-physics` with certified solver backends |
| Descriptor-driven 2d and 3d edit viewports and editor-only aids | Editor application using renderer adapters |
| Child-JVM current-scene and project play | Editor runtime-launch component over the project build and game runtime |
| Export preset validation and dependency closure | Reusable project-export component |
| Java runtime image and native application packaging | Platform build adapter using Maven and JDK packaging tools |
| Editor application | Separate Java `jscene3d-editor` application, with SWT subject to the agreed spike |

The native scene model, import orchestration, and editor are substantial enough
that artifact ownership should not be decided merely by placing new packages in
an existing artifact. Each proposed artifact must hide meaningful complexity
behind a small interface and avoid dependency cycles.

Within `jscene3d-project`, public APIs are grouped by responsibility rather than
collected in one root package:

| Package | Responsibility |
| --- | --- |
| `io.github.glynch.jscene3d.project.manifest` | Project Manifest model and loading |
| `io.github.glynch.jscene3d.project.scene` | Scene model and structural loading |
| `io.github.glynch.jscene3d.project.value` | Portable values and resource references |
| `io.github.glynch.jscene3d.project.extension` | Safe descriptors, discovery, catalog lookup, and catalog-aware validation |
| `io.github.glynch.jscene3d.project.diagnostic` | Structured diagnostics shared by public loading operations |

Each format family owns its raw Jackson model and validator in a corresponding
unexported `internal` package. The shared unexported
`io.github.glynch.jscene3d.project.internal` package is limited to genuinely
cross-cutting policies. `Preconditions` owns throwing public-model invariants;
`ValidationContext` owns diagnostic-producing authored-field validation;
`ProjectPathResolver` owns project-root confinement and symlink checks;
`ProjectJsonReader` owns strict Jackson configuration; and `JsonPointers` owns
JSON Pointer escaping. Portable JSON-to-`ProjectValue` conversion belongs to
the value family's internal decoder. This division gives each rule one
implementation while preserving the distinction between invalid Java API use
and invalid authored documents.

## Relationship to current Doomed Corridors code

Doomed Corridors already demonstrates useful separations:

- `project.json` provides validated project metadata and source asset identity;
- `actors.json`, `combat.json`, and `combat-presentation.json` move substantial
  authoring choices out of Java;
- WAD parsing, map decoding, material import, sprite import, and sound import are
  separated from rendering;
- headless map, geometry, collision, and combat models can be tested without a
  window;
- presentation adapts those models to JScene3D rendering and audio.

The principal architectural gap is runtime composition. Startup code currently
knows how to find and assemble the WAD map, imported resources, actor catalog,
combat rules, presentation rules, player, enemies, HUD, input, audio, and
renderer. The Project Manifest points directly at a WAD asset and map rather
than at an engine-native application scene.

The reusable `DoomMap`, `DoomMapDecodeResult`, and `DoomMapDecoder` in
`jscene3d-doom` supersede the corresponding Doomed Corridors classes. The game
copies remain temporarily so the existing application stays runnable until its
import path is migrated. Material, sprite, sound, presentation, and gameplay
classes are not superseded by this slice. They supply the concrete behavior
needed to design and test later registered types, import adapters, project
systems, and scene composition.

## Incremental design and implementation sequence

Implementation should begin only after this draft is challenged and the first
decisions are recorded.

1. Settle only the Project Manifest, scene, descriptor, extension, and runtime
   interfaces required by the first engine-owned framework proof.
2. Deepen `jscene3d-project` with safe manifest, scene, descriptor, resource
   reference, validation, and diagnostic data models.
3. Add extension descriptor discovery and a registered type catalog that can be
   tested without executing extension implementations.
4. Add `jscene3d-project-runtime` with scene and resource resolution, node and
   controller instantiation, project-system lifecycle, and connection setup.
5. Add the smallest built-in 3d node adapters and generic launcher needed by the
   proof, changing core, LWJGL, game, physics, or audio modules only at
   demonstrated generic seams.
6. Prove the framework headlessly and graphically with synthetic project
   fixtures and an engine-owned runnable example.
7. Design import orchestration using both the existing glTF loader and current
   WAD pipeline so the seam is based on two real adapters.
8. Separate generic WAD archive behavior from Doom content interpretation and
   decide which existing Doomed Corridors classes should migrate.
9. Give Doomed Corridors an application extension, entry scene, project systems,
   registered types, and generic-launcher path, reusing existing domain and
   presentation implementations only where they fit the new architecture.
10. Reproduce the current MAP01 rendering, movement, combat, enemies, HUD,
    audio, and pickups through the new runtime.
11. Represent zombieman ammunition drops through a registered child node,
    controller, scene-level connection, and project data.
12. Represent one complete door and switch interaction, including imported
    identity, motion, collision, audio, and state.
13. Add further Doom controllers and systems only through the proven
    definitions and runtime interfaces.
14. Prove descriptor-driven edit-viewport rendering and child-process play only
    after the runtime and game migration interfaces have survived real use.
15. Prove the first `macos-aarch64` application-image export from the same
    project definition and transitive resource closure.

Each migration slice should keep the standalone game runnable and provide a
manual smoke test in addition to automated verification.

## Alternatives rejected by this draft

### Prescribed directories

Requiring `main/`, `game/`, or another fixed layout would confuse organization
with semantics and make migration unnecessarily difficult. Explicit references
are sufficient.

### Java-only runtime assembly

Java-only composition is flexible but hides project structure from the editor,
makes common authoring changes require recompilation, and duplicates work when
an editor model is eventually added.

### Data-only games

Requiring every algorithm to be serialized would create a programming language
inside the project format and make advanced behavior harder to implement,
debug, test, and reuse. Java extensions remain first-class.

### Serialized Java class names

Class names expose implementation structure as persistent project interface,
weaken JPMS encapsulation, and make refactoring saved projects difficult.
Stable registered type identifiers provide a better seam.

### Separate scene and prefab formats

Both would initially need the same hierarchy, properties, instances,
connections, references, validation, and overrides. One reusable scene concept
has greater depth and less duplicated interface.

### Converting WAD content once and discarding provenance

This would prevent reliable reimport, obscure licensing and source identity,
and make it difficult to verify that a project corresponds to its pinned WAD.
The WAD remains authoritative.

### Editing generated import output directly

Reimport would overwrite authored work or make generated data no longer
reproducible. Authored composition and overlays must remain separate from the
disposable cache.

### Making Doom behavior generic by renaming it

A `SectorDoor` or Doom linedef special remains Doom-specific even if placed in
`jscene3d-game` under an abstract name. Reusability requires a demonstrated
genre-independent interface, not generic naming.

### Building custom editor panels for every type

This would make ordinary project editing depend on executable editor plugins.
Schemas and descriptors should handle ordinary property editing; custom panels
are reserved for interactions that genuinely need them.

## Remaining questions for the design grill

The incremental decision record resolves the original checklist below. The
remaining branches include:

- final names for project-property access, runtime drivability, keyframe
  support, and live read-only output;
- the exact version 0.1 node and resource catalog, including world-2d meshes,
  lights, tile maps, audio, and UI controls;
- viewport, output, camera, and explicit isolated-world ownership;
- selection of the certified 2d and 3d physics backends and the initial joint
  set;
- the world-2d renderer, material, shader, batching, and texture-atlas contract;
- remaining animation semantics, including cues, action tracks, interruption,
  transition priority, and blending;
- exact descriptor, resource-document, overlay, and import schemas beyond the
  settled first Project Manifest and Scene version 1 structures;
- module ownership, dependency direction, build-adapter protocol, and the first
  migration slices.
