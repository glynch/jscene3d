# JScene3D Project Context

This document provides consolidated architectural and implementation context for coding agents working on the JScene3D repository.

It is intended to reduce repeated repository discovery and provide a reliable map of the project's major architectural decisions, module boundaries, implementation direction, terminology, and constraints.

This document is **not** a replacement for the repository's ADRs, design documents, implementation plans, tests, or source code. Before implementing a feature, read the relevant detailed documents identified here and inspect the current implementation.

## Documentation Authority and Interpretation

JScene3D documentation has been written throughout the evolution of the project. Some documents describe durable architecture, some describe planned work, some track implementation, and some older descriptions have been overtaken by subsequent development.

A coding agent must therefore not treat every statement in every document as equally authoritative about the current implementation.

Use the following precedence when interpreting documentation:

1. Durable architectural decisions recorded in ADRs constrain new work unless they have been explicitly superseded.
2. Newer implementation documents are generally the best documentary source for what has been implemented or is actively being implemented.
3. Newer design documents describe accepted architectural or product direction where implementation is incomplete.
4. The README remains useful for broad capability, usage, build, and module information, but may lag the current editor implementation.
5. Current source code and tests must be inspected before assuming that a planned design has already been implemented.

Use these distinctions when reasoning about project documentation:

- **Architectural invariant**: an accepted rule or boundary that new implementation should preserve.
- **Implemented/current**: functionality known to exist in the current codebase.
- **In progress**: accepted functionality whose implementation is actively underway.
- **Accepted/planned**: agreed direction that is not necessarily implemented.
- **Deferred**: deliberately postponed functionality that should not be implemented incidentally.
- **Possibly stale**: an older description that may have been overtaken by later design or implementation.

When two documents appear to disagree, do not silently choose whichever interpretation is convenient. Prefer the newer and more specific source, determine whether an ADR establishes a durable constraint, and inspect the current implementation.

A newer implementation document does not automatically invalidate an older ADR. It must actually revise the architectural decision.

The README should not be used as the sole authority for the current state of the editor. In particular, older descriptions of the editor as primarily a read-only project loader and preview no longer describe the complete current implementation.

## Project Overview

JScene3D is a modular Java 21 3D engine and application platform.

Its major capabilities and subsystems include:

- renderer-independent scene and graphics APIs;
- an LWJGL/OpenGL desktop rendering backend;
- glTF asset loading;
- skeletal and morph-target animation;
- renderer-independent physics;
- audio;
- descriptor-authored entity-component worlds;
- project asset, import, and publication infrastructure;
- application export and desktop hosting;
- genre-independent game-runtime facilities;
- a native JavaFX visual editor;
- Monaco-based source editing;
- Java language tooling integration;
- an embedded editor viewport using the real JScene3D renderer through OpenGLFX.

Three.js is an important conceptual and example reference for JScene3D, but JScene3D is **not a Java port of Three.js and does not attempt API compatibility with Three.js**.

Three.js concepts, terminology, and defaults are retained where they translate naturally and remain useful. JScene3D deliberately diverges where Java can provide stronger guarantees or where the engine architecture requires different ownership and lifecycle semantics.

Java type safety, explicit ownership, controlled mutation, deterministic lifecycle behavior, and clear module boundaries take precedence over JavaScript compatibility.

Godot and Unity are also useful design references for higher-level authoring concepts, particularly reusable hierarchical content and component composition. They are references rather than specifications. JScene3D does not reproduce either engine's public object model.

The project deliberately separates several layers that must not be collapsed into one another:

- renderer scene representation;
- authored project representation;
- runtime entity-component worlds;
- physics simulation;
- game-runtime coordination;
- editor presentation;
- imported source content;
- generated runtime resources.

The same canonical project and runtime architecture is intended to support different applications rather than introducing application-specific engine models.

Beacon Garden is used as an early architecture-validation application for the generic entity-component foundation.

Doomed Corridors is a more demanding validation target covering imported Doom-compatible maps, player behavior, combat, projectiles, pickups, doors, HUD presentation, audio, and other game systems.

Neither application is permitted to introduce a second entity, world, physics, or project model merely because its requirements are more complex.

## Core Architectural Principles

Several principles recur throughout JScene3D and should be treated as default constraints when designing or reviewing implementation work.

### Controlled Public State Mutation

Library-owned mutable state should not normally be exposed directly to callers.

Public APIs favor:

- immutable values;
- read-only views;
- explicit mutation operations;
- validated construction;
- controlled bulk-edit operations where performance requires them.

This allows the owning object to preserve:

- validation;
- dirty tracking;
- versioning;
- derived-state maintenance;
- resource invalidation;
- lifecycle invariants.

JOML is intentionally part of the public API, but library-owned JOML values are exposed through JOML's read-only interfaces rather than mutable implementations.

Stable snapshots are explicit copies.

Geometry follows the same philosophy. Public geometry APIs must not expose mutable backing arrays or mutable NIO buffers merely for convenience. Construction defensively copies caller-owned primitive arrays into library-owned storage.

Where high-performance mutation is necessary, the API should provide controlled scalar or scoped bulk mutation while preserving validation and version tracking.

### Automatic Derived-State Maintenance

Ordinary callers should not have to remember synchronization operations after modifying valid state.

Transforms automatically maintain their derived matrices using dirty tracking and versioning.

JScene3D deliberately does not expose Three.js-style synchronization APIs such as:

- `updateMatrix()`;
- `updateMatrixWorld(boolean)`;
- `matrixAutoUpdate`.

Camera projection matrices are likewise maintained automatically after validated camera-property changes.

The general rule is that if JScene3D owns a derived invariant, JScene3D should maintain it rather than requiring callers to remember an unrelated update call.

### Explicit Resource Ownership

Ownership and lifetime should be understandable from the API.

Application-facing resources such as:

- `BufferGeometry`;
- `Texture`;
- `Material`;

are application-owned, shareable resource descriptions.

Their public `close()` operation is:

- terminal;
- idempotent.

Closing a shared resource ends its application-visible lifetime. Removing one mesh or scene object does not implicitly close resources that may be shared elsewhere.

Renderer/context-specific GPU realizations are owned separately by the renderer associated with that OpenGL context.

A renderer may therefore realize the same application resource independently from another renderer without transferring ownership of the resource description.

### Deterministic Lifecycle and Rollback

Complex runtime construction should not expose partially valid state.

World composition, component construction, resource acquisition, reference binding, and lifecycle activation use explicit ordering and rollback rules.

A failed composition must not publish a partially constructed World.

Resources and component values acquired during failed construction are released according to deterministic ownership rules.

Lifecycle behavior should remain observable and testable rather than depending on garbage collection or incidental collection order.

### Semantic Models Remain Independent of Backends

JScene3D deliberately separates semantic models from implementation technologies.

Examples include:

- physics is renderer-independent;
- the game-facing `World` and `Entity` model is not the renderer's `Scene` and `Object3D` model;
- the editor extension API does not expose JavaFX controls;
- language-support APIs do not expose LSP4J or JDT LS implementation types;
- build callers request semantic build intentions rather than constructing Maven command lines;
- settings contributors declare settings rather than constructing controls or serializers;
- importers publish canonical project assets rather than creating a parallel runtime object model.

When adding functionality, preserve these boundaries rather than allowing a convenient backend implementation to become the public abstraction.

### Deep Modules Over Distributed Coordination

Important subsystems should hide coordination complexity behind small interfaces.

Callers should not have to coordinate:

- parsing;
- dependency loading;
- process management;
- lifecycle ordering;
- revision tracking;
- backend handles;
- rollback;
- persistence scheduling;
- protocol details.

Examples of intended deep-module boundaries include:

- world composition;
- project builds;
- settings;
- workspace-state persistence;
- LSP integration;
- runtime-resource acquisition.

### Evidence Before Abstraction

JScene3D does not add APIs simply because another engine or framework provides them.

Features should normally be introduced when a concrete requirement establishes:

- semantics;
- ownership;
- lifecycle;
- failure behavior;
- extension boundaries;
- verification requirements.

This principle has deliberately deferred features such as:

- raw external OpenGL interoperation;
- speculative geometry-streaming APIs;
- definition inheritance and prefab variants;
- a public data-oriented ECS query model;
- arbitrary live editing and hot reload;
- scripting languages;
- multiplayer replication;
- generalized extension installation;
- broad platform promises without qualification.

Deferred functionality is not necessarily prohibited. It should deepen the established architecture when required rather than creating a parallel system by default.

### Stable Identity Over Incidental Location

Persistent identity must not depend on:

- editable display names;
- hierarchy positions;
- relative hierarchy paths;
- filesystem locations.

Assets, entities, components, properties, endpoints, and runtime instances use deliberately scoped stable identities.

Rename, reorder, reparent, or file relocation must not silently change semantic identity.

### Explicit Dependencies Over Implicit Discovery

Dependencies should be declared or explicitly targeted.

Avoid patterns such as:

- nearest ancestor of a particular type;
- global service lookup;
- implicit world singleton;
- arbitrary class-based component search;
- hidden fallback to a plausible object elsewhere in the hierarchy.

When a component requires a capability or specific participant, that dependency should be represented explicitly and validated before activation.

## Public API and Data Conventions

JScene3D uses several project-wide conventions that should remain consistent across new APIs.

### Angle Units

All public angles use **radians**.

This includes camera field of view and other values where external libraries or engines may conventionally use degrees.

Do not introduce ambiguous angle methods.

If a future convenience API accepts degrees, the degree unit must be explicit in its naming or type.

### Color-Space Convention

JScene3D uses **linear-sRGB as its working color space**.

Color input encoding is explicit.

Typical base-color image data is interpreted using sRGB conversion, while data textures remain linear.

Alpha is treated independently from RGB transfer encoding.

Renderer output conversion occurs at the appropriate output stage rather than allowing display-encoded values to be treated as linear lighting values internally.

### Closeable Resource Semantics

Application-facing closeable resource descriptions use terminal closure.

The first successful `close()` ends the resource's public lifetime.

Subsequent `close()` calls are harmless.

Operations that require a live resource fail clearly after closure.

Thread-affine renderer or window resources still obey the lifecycle/thread contract of their owning backend even when the public close semantics are idempotent.

### Authored Data Versus Runtime State

Authored project data and mutable runtime state are different concepts.

Authored data should remain:

- deterministic;
- serializable;
- inspectable without running application behavior;
- independent of live Java implementation objects.

Runtime state may include:

- transforms;
- visibility;
- animation playback;
- skeleton pose;
- physics state;
- behavior state;
- subscriptions;
- dynamically spawned entities.

Mutable runtime state must not accidentally mutate shared authored assets or shared immutable runtime resources.

### Canonical Serialization

Canonical authored project data uses deterministic UTF-8 JSON for structured content.

Large payloads such as meshes, textures, audio, and similar binary data remain separate binary resources.

Java object serialization is not an authored persistence mechanism.

Serialized component data must not contain Java implementation class names.

Every persistent format should carry explicit schema/version information and support deterministic validation and migration rules.

Loading an older format may migrate it in memory, but loading must not silently rewrite source files.

Unsupported newer schema versions fail with a clear diagnostic.

### Structured Diagnostics

Expected project/content failures should normally become structured diagnostic data rather than generic exceptions.

Examples include:

- unsupported schema versions;
- unresolved references;
- missing component types;
- invalid authored properties;
- conflicting capabilities;
- invalid imported targets.

Programmer-contract violations may remain exceptions.

Diagnostics should preserve enough stable identity and source information for the editor to associate them with relevant resources and authored objects.

## Rendering and Graphics Architecture

JScene3D separates renderer-independent graphics concepts from OpenGL-specific realization and platform hosting.

### Renderer Ownership of OpenGL State

A Renderer exclusively owns the OpenGL state of its associated context during its lifetime.

Applications must not freely interleave arbitrary LWJGL/OpenGL calls with JScene3D rendering and expect the renderer to preserve or reconstruct unknown external state.

The renderer does not attempt to snapshot and restore arbitrary OpenGL state around every operation.

`ShaderMaterial` is the supported customization boundary for application-defined shader behavior.

This ownership rule protects renderer invariants and prevents external state changes from invalidating assumptions made by rendering code.

### Independent Renderer Contexts

JScene3D supports multiple independent Window/Renderer pairs within one JVM.

The initial model assumes:

- each window owns an independent OpenGL context;
- one Renderer owns each context;
- contexts do not share OpenGL objects;
- rendering occurs sequentially on the designated render thread;
- applications do not manually switch contexts behind the renderer.

An application-owned resource description may be used by multiple Renderers.

Each Renderer lazily creates and owns its own context-specific GPU realization of that resource.

Closing one Renderer or Window must not invalidate another independent Renderer using the same application-level resource descriptions.

Concurrent rendering from multiple render threads and explicit OpenGL object sharing between contexts are not part of the initial public contract.

### RenderSurface Host Boundary

`RenderSurface` is the renderer-host abstraction that allows the same renderer to operate in different presentation environments.

The GLFW desktop integration and JavaFX/OpenGLFX editor integration use separate adapters at this seam.

The host owns:

- window or control lifetime;
- OpenGL context lifetime;
- frame scheduling;
- context activation;
- presentation;
- final surface destruction.

The Renderer owns:

- rendering;
- renderer-managed OpenGL state;
- context-specific GPU resource realizations.

The host supplies the active presentation framebuffer and one consistent surface size.

Renderer internals must not assume that rendering always targets framebuffer zero.

This boundary is particularly important to the editor because the JavaFX workbench embeds the actual JScene3D renderer rather than maintaining a second WebGL, WebGPU, or Three.js implementation.

### Renderer-Managed Transform Uniforms

Custom shaders may use five reserved transform uniforms:

- `modelMatrix`;
- `viewMatrix`;
- `projectionMatrix`;
- `modelViewMatrix`;
- `normalMatrix`.

When an active shader declares one of these uniforms with the expected type, the Renderer supplies its value automatically.

A reserved name declared with an incompatible type is invalid.

Other uniforms remain application supplied.

The automatic uniform contract gives custom shaders access to renderer-maintained transforms without exposing renderer internals or requiring application code to duplicate matrix synchronization.

### Texture and Image Ownership

Image decoding through STB remains an LWJGL implementation detail.

Decoded image pixels are copied into core-owned Java storage before the native STB allocation is released.

Core texture/image objects therefore do not expose or depend on STB-managed native lifetime.

Textures generate mipmaps by default and use an appropriate mipmapped minification strategy.

Applications may explicitly disable mipmaps where required, such as pixel-art or specialized memory-sensitive cases.

Invalid sampler and mipmap combinations should fail rather than being silently substituted with different behavior.

### Rendering Color Pipeline

JScene3D performs lighting calculations in its linear-sRGB working space.

Input encoding and output conversion remain explicit parts of the rendering pipeline.

Environment lighting and tone mapping build on this same color-space contract rather than creating a separate color interpretation.

### HDR Environment Lighting

Environment lighting is represented independently from whether the environment is visibly rendered as a background.

A Scene may therefore use an environment for image-based lighting without displaying it, or use appropriate independent environment/background configuration.

Renderer-derived environment data may include:

- diffuse irradiance;
- GGX-prefiltered reflections;
- split-sum BRDF lookup data.

ACES filmic tone mapping is a renderer-level output operation over HDR rendering.

Tone mapping is not implicitly enabled merely because the renderer supports it. Existing applications should not pay for an additional HDR/tone-mapping path unless requested.

### Current Rendering Capability Baseline

The engine currently includes a substantial rendering feature set.

Relevant capabilities include:

- Basic materials;
- Lambert materials;
- Phong materials;
- Normal materials;
- metallic-roughness Standard materials;
- ambient lighting;
- point lighting;
- directional lighting;
- spot lighting;
- hemisphere lighting;
- directional shadows;
- spot shadows;
- point-light shadows;
- fog;
- transparency and explicit alpha modes;
- instancing;
- custom instance attributes;
- morph targets;
- skeletal rendering;
- animation;
- custom OpenGL 3.3 Core shaders;
- texture transforms;
- line rendering and helpers;
- billboards;
- animated billboards;
- triangle-mesh raycasting;
- HDR environments;
- image-based lighting;
- ACES filmic tone mapping.

This feature inventory should not be mistaken for the architecture itself.

New rendering work must continue to respect resource ownership, renderer/backend separation, color-space conventions, and automatic state maintenance.

## Asset Loading and glTF Integration

JScene3D treats external asset formats as inputs to its own renderer-independent models rather than allowing third-party parser models to become public engine APIs.

### glTF as the Primary Portable Format

glTF 2.0 is the primary portable runtime interchange format.

Both `.gltf` and `.glb` forms are supported.

The glTF loader is maintained as a renderer-independent artifact.

Loading glTF must not require:

- LWJGL;
- GLFW;
- OpenGL;
- an active graphics context.

This allows asset loading and validation to be exercised independently of native rendering.

### Internal Parser Boundary

JglTF is used internally for parsing and reference resolution.

JglTF types are implementation details.

They must not become part of JScene3D's public API or force callers to understand JglTF's internal object model.

The loader translates supported glTF concepts into JScene3D-owned values.

### Current glTF Capability

Current support extends significantly beyond a minimal static-mesh loader.

Supported areas include:

- selected-scene hierarchy loading;
- node TRS transforms;
- supported decomposable matrix transforms;
- triangle primitives;
- indexed geometry;
- positions;
- normals;
- multiple texture-coordinate sets where supported;
- RGB/RGBA vertex colors;
- metallic-roughness materials;
- PNG and JPEG image data;
- alpha modes;
- double-sided materials;
- sampler configuration;
- skeletal skinning;
- morph targets;
- Draco-compressed mesh primitives;
- transform animation;
- morph-weight animation.

Unsupported required features must fail with useful diagnostics rather than being silently approximated.

### Import Versus Runtime Representation

The external glTF hierarchy is not a second permanent game-authoring hierarchy.

Under the accepted entity-component architecture, imported glTF scenes project into canonical generated project content.

A typical projection is:

- glTF node -> Entity;
- local transform -> `Transform3d` component;
- mesh -> mesh-renderer component;
- skinned mesh -> skinned-mesh-renderer component;
- camera -> camera component;
- light -> light component;
- animation ownership -> animation component;
- payload data -> referenced immutable assets/runtime resources.

Imported cameras and lights remain ordinary authored/generated components. Importing them does not implicitly make one camera globally active or assign unrelated global behavior.

### Generated Entity Definitions

Selecting/importing a glTF scene may publish a generated read-only `EntityDefinition` plus referenced resources such as:

- meshes;
- materials;
- textures;
- skins;
- animation clips;
- other immutable runtime-resource assets.

There is no separate public `Model3d` plus `ModelInstance3d` hierarchy in the accepted architecture.

Generated definitions use the same canonical definition model used by authored content.

Generated internals remain read-only and disposable because they can be reproduced from their authoritative source and import configuration.

Gameplay behavior, collision, audio, additional children, and application-specific composition belong in authored content around the generated definition rather than being written into disposable generated output.

### Source Assets and Published Imports

Source assets are authoritative inputs.

Normal project runtime startup consumes completed published import generations rather than executing importers or rereading original source assets.

This keeps source inspection/import publication separate from runtime world composition.

Import output should carry enough provenance to explain:

- originating source asset;
- source element or locator;
- import settings;
- importer version;
- generated asset identity.

Reimport should prefer explicit stable source identifiers.

When only weaker evidence exists, remapping must be conservative.

An uncertain required target must not be silently fuzzy-matched to a different source element.

### Other Asset Formats

Support for additional formats is demand-driven.

A new external format should normally translate into JScene3D-owned canonical models rather than creating a parallel public hierarchy.

Conversion-first workflows are acceptable where they avoid unnecessary permanent engine complexity.

A future compact runtime package may be generated from authored assets, but derived runtime packaging must not become a second authoring format.

## Entity-Component World Architecture

The entity-component world architecture is the primary project-facing authoring and runtime model for higher-level JScene3D applications.

It is designed to support 2D, 3D, UI-based, editor-preview, and game use cases without creating separate world models for individual applications.

### Architectural Model

JScene3D uses a **hierarchical entity-component object architecture**.

It is explicitly not a public data-oriented ECS contract.

The canonical public terms are:

- `Entity`;
- `Component`;
- `EntityDefinition`;
- `WorldDefinition`;
- `World`;
- `Asset`;
- `Runtime Resource`.

Avoid using these terms as synonyms:

- `Node`;
- `SceneDefinition`;
- `Prefab`;
- `Controller`;
- `ModelInstance3d`.

The existing renderer-level `Scene` and `Object3D` remain lower-level graphics concepts.

They are not the project-facing game-authoring hierarchy.

### Runtime Entity

There is one runtime `Entity` type.

There are no separate `Entity3d`, `Entity2d`, or UI Entity subclasses.

An Entity intrinsically owns:

- runtime identity;
- ownership parent or World-root membership;
- ordered owned children;
- local enabled state;
- effective enabled/lifecycle state;
- component identity and ownership;
- association with exactly one World.

An Entity does not intrinsically implement:

- rendering;
- physics;
- audio;
- animation;
- update behavior.

It does not recursively render or update its children.

Runtime modules compile and schedule the components participating in those systems.

Editor creation presets such as a conceptual “3D Entity” may create an ordinary Entity plus an appropriate spatial component. Such presets are authoring conveniences, not serialized Entity subtypes.

### Entity Granularity

Use a Component when a capability shares the Entity's:

- identity;
- transform;
- lifetime.

Use an owned child Entity when something requires independent:

- identity;
- transform;
- lifecycle;
- component composition;
- attachment;
- reuse.

A runtime-created projectile is normally an Entity because it has meaningful independent identity and lifecycle.

Individual particles managed internally by a high-volume particle system do not need to become Entities unless independent behavior, selection, collision, or identity justifies that cost.

### Component Records

A persistent component record contains:

- stable local `ComponentId`;
- stable namespaced component-type identity;
- component configuration-schema version;
- typed authored property values.

The registered `ComponentTypeDescriptor` is authoritative for:

- component type identity;
- schema version;
- property schemas;
- defaults;
- constraints;
- editor metadata;
- provided capabilities;
- required capabilities;
- multiplicity;
- conflicts;
- signals;
- actions;
- spatial domain;
- lifecycle participation;
- scheduling participation;
- runtime construction seam.

Annotations or generated metadata may help produce descriptors, but they must not create a second semantic model.

Serialized component data never names its Java implementation class.

### Runtime Component Implementations

Runtime Components are ordinary Java objects and may contain behavior.

There is no privileged script, behavior, or controller slot.

An Entity may contain multiple behavioral Components when their descriptors permit it.

Trusted runtime extensions register runtime factories separately from safe descriptor discovery.

Factory registration must correspond to an exact descriptor owned by that extension.

Duplicate, foreign, undeclared, or late registrations fail rather than being resolved heuristically.

### Safe Descriptor Discovery

Component descriptors are safe extension metadata.

The editor and project infrastructure must be able to discover and validate descriptor metadata without executing arbitrary extension implementation code.

This supports workflows such as:

- project inspection;
- generated Inspector presentation;
- validation;
- missing-extension diagnostics;
- settings-like metadata presentation.

Executable runtime factories remain a separate trusted boundary.

### Component Dependencies

Component dependencies use:

- declared capabilities; or
- explicit stable component identity.

General `getComponent(Class<?>)` discovery is not the dependency model.

Nearest-ancestor searches are not the dependency model.

Missing, conflicting, or ambiguous required dependencies should be detected before activation.

Runtime behavior may query a capability on one exact live Entity when that Entity is already known, but such lookup does not search ancestors, descendants, or the wider World for a plausible replacement.

### Spatial Domains

Primary spatial authority is supplied by explicit Components.

Examples include:

- `Transform3d` providing `Spatial3d`;
- `Transform2d` providing `Spatial2d`;
- `UiLayout` providing UI layout capability.

An Entity may have at most one primary spatial-domain Component.

Spatial consumers such as renderers and colliders require the appropriate capability but do not themselves claim transform authority.

Ownership hierarchies may contain mixed spatial domains.

Spatial inheritance applies only between compatible domains.

Cross-domain relationships require explicit bridge/projection semantics rather than accidental transform inheritance.

### Transform Authority

Each spatial Entity has one effective transform authority.

Examples include:

- static objects consuming authored/runtime transform state;
- sensors consuming authored/runtime transform state;
- kinematic movement requesting motion through physics;
- rigid bodies eventually producing authoritative simulation state;
- rendering interpolating simulation state for presentation.

Conflicting transform writers are architectural errors and should be rejected through validation.

### Authored Asset Model

`Asset` is the umbrella term for independently stored/addressable project content.

Asset kinds include concepts such as:

- `ProjectManifest`;
- `WorldDefinition`;
- `EntityDefinition`;
- meshes;
- materials;
- textures;
- animation clips;
- audio;
- collision-shape assets;
- source assets;
- import recipes where required.

Persistent references are typed.

The expected asset kind should be known and validated rather than inferred after loading.

An authored Asset is distinct from its loaded Runtime Resource.

### Project Manifest

`ProjectManifest` contains descriptive and operational project metadata.

It is not instantiated as an Entity.

It may describe:

- project identity;
- schema version;
- startup WorldDefinition;
- enabled engine modules;
- application/game module entry point;
- asset roots;
- import/build configuration;
- project-wide runtime/display defaults;
- legal and compatibility metadata.

World and Entity content remain referenced Assets rather than being embedded directly into the manifest.

### World Definition

`WorldDefinition` describes one runtime World.

It contains world settings and zero or more root entries.

A root entry may be:

- an Entity authored locally in that WorldDefinition; or
- a placement of an EntityDefinition.

Multiple roots are valid.

The editor and runtime must not invent a visible root Entity merely to hold them.

### Entity Definition

`EntityDefinition` is an immutable, reusable, single-root Entity hierarchy.

It may contain:

- locally authored Entities;
- placements of other EntityDefinitions;
- Component configuration;
- internal signal/action connections;
- an explicitly exported public contract.

There is no separate prefab format.

Unique content may be authored directly in a World or definition. Reusable content can be extracted into an EntityDefinition when reuse is justified.

Structural definition inclusion must remain acyclic.

Behavioral references may legitimately form cycles because they do not recursively expand the authored hierarchy.

### Definition Placement

Loading or importing an EntityDefinition does not create live Entities.

A definition becomes live only when explicitly placed or spawned.

When placed, its root becomes the instance Entity directly.

Do not create an artificial runtime wrapper around a definition instance.

Nested definition instances retain logical instance scope and contract boundaries even if runtime implementation later flattens internal representation.

### No Definition Inheritance

The initial architecture deliberately has no:

- base definitions;
- derived definitions;
- prefab variants;
- multi-level override chains.

Duplicating a definition creates an independent asset.

When additional behavior or structure is required around a reusable definition, create ordinary authored composition rather than introducing inheritance implicitly.

### Definition Encapsulation

A containing World or EntityDefinition may configure a placed definition only through:

- standard placement state;
- the definition's exported public contract.

It may not arbitrarily modify private internal Entities or attach Components inside that private hierarchy.

The public contract may expose:

- typed parameters;
- typed signals;
- typed actions;
- capabilities;
- named spatial attachment points;
- deliberately supported resource bindings.

An exported attachment point provides a stable public name while hiding private implementation structure.

### Stable Identity

Identity must not derive from:

- display name;
- hierarchy position;
- filesystem path.

Every stored Asset has a stable opaque `AssetId`.

Entity IDs are stable within their authored Asset.

Component IDs are stable within their owning Entity.

Rename, reorder, reparent, and file relocation preserve identity.

Persistent addressing is conceptually based on:

`AssetId + EntityId + optional ComponentId + optional PropertyId`

Live addressing additionally includes definition-instance scope.

These identity kinds should remain distinct in Java types where practical.

### Runtime Resource Sharing

Heavy runtime resources are immutable and shareable.

Examples include:

- meshes;
- materials;
- textures;
- collision shapes;
- skeleton definitions;
- animation clips;
- audio payloads.

Mutable state belongs to live instances.

One live instance must not modify another by mutating a shared Runtime Resource.

`RuntimeResourceProvider` is the host seam for acquiring shared immutable runtime values.

Acquisitions return leases controlled by World lifetime.

Components receive resolved values rather than lease handles so they cannot accidentally release resources still required elsewhere.

## World Composition, Lifecycle, and Runtime Scheduling

World composition is a deep runtime boundary. Callers should not coordinate component construction, dependency resolution, resource acquisition, lifecycle ordering, signal binding, scheduling, or rollback themselves.

### Transactional World Composition

`WorldComposer.compose(...)` is the primary composition boundary.

The broad composition sequence is:

1. Load and validate the complete authored definition graph.
2. Allocate the complete inactive live Entity graph.
3. Validate Component configuration and construct runtime Component values.
4. Resolve Runtime Resources required during construction.
5. Bind stable Entity and Component references.
6. Validate declared capability dependencies.
7. Bind declared signals and actions.
8. Resolve authored signal/action connections.
9. Resolve exported definition contracts.
10. Publish a complete inactive World only after all required construction succeeds.

No semantic lifecycle callback should run while the graph is still partially constructed.

A failed composition publishes no World.

Already-created closeable Component values and acquired Runtime Resource leases are released according to deterministic reverse-order rollback rules.

A successfully composed World remains inactive until the caller explicitly invokes `World.activate()`.

### Component Construction Boundary

Trusted engine and application extensions register Component factories before composition.

The construction context may provide:

- immutable authored Component definition;
- exact ComponentTypeDescriptor;
- effective property values;
- construction-time Runtime Resource resolution;
- explicitly bound WorldModule dependencies.

It must not expose mutable WorldComposer internals.

A future dependency-injection framework may adapt to this construction seam, but dependency injection must not become part of serialized project data or require the engine to adopt a particular DI framework.

### Reference Binding

Authored Entity and Component references are resolved only after the complete live graph exists.

Components requiring authored references participate through the dedicated reference-binding seam.

Reference resolution should produce direct live Entity or Component references from stable authored identities.

Binding failure is a composition failure.

No lifecycle callback has run at this point, so failed binding can use the same complete rollback path as failed construction.

### Endpoint Binding

Components declaring signals or actions bind those endpoints through the descriptor-backed endpoint seam.

Descriptor metadata is authoritative.

Every required endpoint implementation must:

- correspond to a declared endpoint;
- use the correct direction;
- use the declared payload semantics;
- be bound exactly as required by the descriptor.

Arbitrary undeclared runtime endpoints must not become a second hidden Component interface.

### World Activation

Composition and activation are separate operations.

A successfully composed World is complete but inactive.

`World.activate()` begins semantic lifecycle participation.

Initial creation and activation ordering follows ownership and declared Component dependencies rather than incidental collection order.

A partially activated definition instance must not become observable as a valid World state.

### Entity Lifecycle States

The effective runtime lifecycle has three primary Entity states:

- **Active**;
- **Disabled**;
- **Destroyed**.

A disabled Entity remains live and retains its state but does not participate in active behavior.

Destroying an Entity permanently removes it and its owned subtree according to World mutation rules.

Disabling an owner effectively disables descendants without rewriting each descendant's independently authored/local enabled flag.

Re-enabling the owner therefore does not incorrectly enable a child that was independently disabled.

### Component Lifecycle Callbacks

Descriptor metadata determines which lifecycle events a Component participates in.

The semantic lifecycle points are:

1. **Created**;
2. **Activated**;
3. **Deactivated**;
4. **Destroyed**.

Implementing a Java callback interface does not itself opt a Component into lifecycle events.

The Component descriptor remains authoritative.

Creation and activation proceed owner before owned children.

Deactivation and destruction proceed children before owner.

Declared Component dependencies determine required ordering within one Entity.

Components without an explicit ordering dependency must not rely on incidental authored or collection order.

### Structural Mutation

Live structural changes go through the owning World.

Entities do not expose arbitrary mutable child or Component collections as the public runtime mutation interface.

The implemented mutation direction includes operations such as:

- enable;
- disable;
- destroy.

Requests made while the World is idle may commit synchronously because execution is already between component-visible phases.

Requests made during Component callbacks are deferred and committed at a safe structural boundary.

A destroy request makes the target effectively inactive immediately so it receives no new scheduled callbacks or signal delivery while waiting for physical removal.

Permanent removal and cleanup occur at the next structural commit.

Repeated destruction is harmless where specified, while operations against already destroyed or invalid targets fail clearly.

### Runtime Spawning

Runtime spawning follows the same EntityDefinition composition architecture as static authored placement.

Do not create a second lightweight object-construction model for spawned content.

Spawning is transactional.

Gameplay-critical definitions should normally be prepared before they are needed.

Spawning a prepared definition performs in-memory composition and commits the new instance at a safe structural boundary.

A spawn requested during one Component-visible phase cannot participate retroactively in that same phase.

After commit, it may participate in a later valid phase.

Blocking asset I/O must not occur on the simulation thread.

An immediate spawn request for unprepared content should fail explicitly rather than unexpectedly blocking simulation.

A separate asynchronous prepare-and-spawn workflow may support non-immediate use cases.

### Simulation Thread Model

JScene3D guarantees deterministic execution ordering within its defined runtime model.

It does not currently promise bit-for-bit deterministic simulation across every platform.

One logical simulation thread invokes arbitrary game Component behavior.

Physics, rendering, audio, loading, or other modules may use worker threads internally, but worker threads must not directly invoke arbitrary Entity Components.

Worker results return to the owning World and are delivered at defined phases.

### Runtime Update Cadences

The initial runtime exposes two Component-visible update cadences:

- fixed simulation updates;
- per-frame presentation updates.

Component descriptors explicitly declare participation.

Hierarchy position and serialized Component order do not determine scheduling.

### Engine Phase Vocabulary

The initial closed phase vocabulary is:

1. **Input acquisition**;
2. **Before physics**;
3. **Physics**;
4. **After physics**;
5. **Frame update**;
6. **Render preparation and submission**.

The engine may subdivide phases internally when required.

Extensions do not add arbitrary new global phases.

Animation, transform propagation, and backend coordination are scheduled internally where required by their ownership and transform-authority rules.

### Fixed and Frame Advancement

The World owns simulation tick and accumulated simulation time.

Fixed advancement runs declared fixed-step behavior around the engine-owned physics phase.

Frame advancement runs presentation-oriented behavior with the interpolation state produced by completed fixed simulation.

Rendering may interpolate previous and current simulation state.

Presentation interpolation must not mutate authoritative simulation state.

### Scheduling Determinism

Schedules are compiled from descriptor metadata and stable authored/runtime identities.

Execution order must not accidentally depend on:

- serialized Component collection order;
- sibling insertion order where no semantic dependency exists;
- runtime-extension registration order;
- hash-map iteration order.

Effectively disabled Entities are skipped.

Component callbacks are synchronous and non-reentrant unless a future design explicitly changes that contract.

### Signals and Actions

Signals announce typed events.

Actions request typed behavior from known targets.

They are not an untyped global event bus.

Signal delivery is synchronous and deterministic on the simulation thread during the appropriate phase.

Signal connections use a stable snapshot for dispatch so structural mutation requested by listeners does not unpredictably alter the currently executing connection traversal.

Connections inside authored content use stable identities.

Connections crossing a reusable definition boundary use explicitly exported signals and actions.

An unconnected signal is valid.

A required target or required contract value must not be silently absent.

### Runtime Failure Behavior

A failure during Component update is different from a failure during initial composition.

Composition is transactional.

Ordinary runtime Component updates are not globally transactional.

Structural mutation already accepted during a Component-visible phase may still commit at the defined structural boundary even if later Component code fails in that phase.

Runtime failures should preserve precise context such as:

- phase;
- live Entity;
- authored Component identity;
- lifecycle event where relevant.

When lifecycle or cleanup failure leaves the World unable to guarantee reliable state, closing the World is preferable to continuing with an unknown partially valid runtime.

## Doom-Compatible Content and Doomed Corridors Boundaries

Doom-compatible content is an important validation target for JScene3D, but Doom-specific concepts must not define the generic engine architecture.

### Module Ownership

Responsibilities are deliberately separated.

`jscene3d-project` owns generic project concerns such as:

- project metadata;
- asset identity;
- typed references;
- definitions;
- validation;
- schema migration;
- diagnostics.

`jscene3d-project-import` owns generic import concerns such as:

- deterministic import orchestration;
- generated asset publication;
- provenance;
- disposable cache policy.

`jscene3d-wad` owns generic WAD concerns such as:

- IWAD/PWAD validation;
- archive structure;
- ordered lump access;
- archive layering;
- bounded opaque data access.

`jscene3d-wad` does not interpret Doom maps or gameplay.

`jscene3d-doom` owns reusable classic Doom-format interpretation such as:

- map discovery;
- map decoding;
- Doom-format validation.

Doomed Corridors owns application-specific concerns such as:

- actors;
- player behavior;
- weapons;
- inventory;
- combat;
- campaign progression;
- doors and lifts;
- switches;
- sector behavior;
- HUD behavior;
- presentation choices;
- content selection;
- Freedoom attribution and title-specific legal material.

### No Doom Concepts in Generic Engine Modules

Do not place concepts such as:

- WAD;
- sector;
- linedef;
- Doom weapon;
- Doom enemy;
- Doom actor;

into generic:

- Entity architecture;
- World architecture;
- rendering;
- physics;
- game runtime.

Generic functionality moves into reusable engine modules only when it has a clear application-independent contract.

### WAD as Authoritative Source Content

Doomed Corridors uses a pinned Freedoom Phase 2 WAD as source content.

The source WAD is authoritative.

It is not downloaded dynamically at runtime.

It is not mutated by importers.

Project metadata should retain appropriate provenance, release, digest, licensing, and attribution information.

### Doom Import Publication

Doom import translates source content into canonical JScene3D project/runtime concepts.

Generated publication may include:

- read-only generated EntityDefinitions;
- immutable geometry resources;
- materials;
- textures;
- audio resources;
- collision-shape resources;
- decoded Doom-domain metadata where application behavior still requires it;
- provenance records.

The exact publication shape may evolve through vertical slices, but it must converge on the canonical Entity/Asset/Runtime Resource architecture.

### Generated Map Definition

Where hierarchical Entity composition is useful, imported map structure is represented as a generated read-only EntityDefinition.

The authored startup WorldDefinition places that generated map definition alongside authored content such as:

- player;
- cameras;
- HUD;
- application behavior;
- other reusable definitions.

Application behavior is composed around generated content rather than written into disposable generated import output.

### Import Cache Invariants

Generated output is reproducible from:

- authoritative source bytes;
- import settings;
- importer version.

Deleting generated cache data must never delete authored project information.

Generated assets require stable identity and explicit provenance.

An importer must not advertise an unstable locator as permanent identity.

When a required imported target can no longer be resolved confidently, the system should produce a diagnostic and require explicit repair rather than silently choosing a fuzzy match.

### Headless Import

Import inspection and validation are headless operations.

They must not require:

- creating a window;
- initializing OpenGL;
- initializing audio;
- constructing a runtime World;
- executing Doomed Corridors behavior.

This keeps content processing independent from runtime hosting.

### Doom Rendering and Collision

Map rendering and map collision are independently generated.

Rendered walls, floors, or other surfaces do not become collision merely because geometry exists.

Static collision is represented through explicit physics Components and collision shapes.

Non-blocking interactions such as:

- pickups;
- exits;
- teleport regions;
- trigger areas;

use appropriate sensor/area concepts and typed overlap signals.

Doomed Corridors behavior determines the gameplay meaning of those signals.

### Doom Things and Entity Granularity

A Doom map thing that requires independent:

- identity;
- Components;
- mutable state;
- lifecycle;

is naturally represented as an Entity.

High-volume geometry or internal map representation may remain batched data or Runtime Resources when independent Entity semantics provide no benefit.

### Initial Compatibility Target

The initial content target is the pinned Freedoom Phase 2 `freedoom2.wad`.

Development progresses from `MAP01` toward the vanilla Doom II map set.

The initial target does not require compatibility with:

- Boom;
- MBF;
- Hexen-format maps;
- UDMF;
- GZDoom-specific extensions.

Multiplayer, save games, generalized mod discovery, and exact reproduction of the original software renderer are also outside the initial requirement.

### Migration Strategy

Beacon Garden establishes the generic Entity/World architecture before Doomed Corridors drives the more demanding migration.

The Doomed Corridors migration should proceed through independently verifiable slices such as:

1. canonical ProjectManifest and asset catalog;
2. generated MAP01 publication;
3. rendering generated map content through Entity rendering Components;
4. explicit generated collision;
5. player composition;
6. prepared projectile spawning and one complete combat loop;
7. enemies and pickups;
8. doors, switches, lifts, teleports and exits;
9. sector effects;
10. HUD and campaign progression;
11. progressive coverage of the pinned Freedoom campaign.

The first target is behavioral parity with the existing playable MAP01 path.

New Doom-specific features should follow that parity rather than distorting the generic architecture during migration.

## Physics and Game Runtime Architecture

Physics, world composition, game-runtime coordination, and application-specific game behavior are deliberately separate responsibilities.

### Renderer-Independent Physics

`jscene3d-physics` is an original pure-Java renderer-independent physics module.

Its public model must not depend on:

- OpenGL;
- LWJGL rendering objects;
- JavaFX;
- game-specific Entity types;
- Doom-specific concepts.

Physics does not own the JScene3D Entity hierarchy.

It does not mutate renderer `Object3D` instances.

It does not infer collision automatically from visible rendering geometry.

It does not decide what a collision means to gameplay.

### Physics World Responsibility

The physics subsystem owns concepts such as:

- collision objects;
- colliders/shapes;
- collision filtering;
- spatial queries;
- simulation state;
- contact and overlap detection;
- kinematic movement;
- sensors;
- debug geometry snapshots.

Entity/World integration adapts authored Component state into physics objects through explicit module boundaries.

### Independent Rendering and Collision

Visible geometry does not automatically become collision geometry.

Collision geometry does not need to be visible.

Rendering and collision are authored independently.

This distinction is particularly important for:

- imported level geometry;
- simplified collision meshes;
- invisible triggers;
- sensors;
- gameplay volumes;
- generated Doom map content.

Do not infer physics configuration from the presence of a renderer Component.

### Collision Objects and Shapes

A collision-object Component represents a body or sensor and may own multiple independently transformed collision shapes.

A shape has stable identity and may carry:

- immutable collision-shape resource reference;
- local transform;
- filtering information;
- physics material information where supported.

Shape membership uses stable Entity/Component identities rather than relative hierarchy paths.

The editor may provide convenient commands that visually make shape composition easy, but those commands must produce the canonical explicit model.

### Physics Object Kinds

Physics object kinds are capabilities rather than Entity subclasses.

The architecture supports concepts such as:

- static bodies;
- explicitly moved kinematic/character bodies;
- non-blocking sensors/areas;
- dynamic rigid bodies when implemented.

An object may use multiple shapes regardless of its body kind where the physics capability supports that arrangement.

### Transform Authority and Physics

Each spatial Entity has one effective transform authority.

Typical authority rules are:

- static bodies consume Entity transform state;
- sensors consume Entity transform state;
- kinematic/character movement requests movement through physics;
- dynamic rigid bodies eventually produce authoritative simulation state;
- rendering may interpolate physics state for presentation.

Physics and rendering must not simultaneously believe they independently own the authoritative transform.

### Initial Physics Capability Direction

The useful physics baseline includes or is intended to include:

- fixed-step simulation;
- static collision;
- character/kinematic collision;
- collision objects with multiple local shapes;
- box shapes;
- sphere shapes;
- capsule shapes;
- triangle-mesh shapes where supported;
- broad-phase collision detection;
- narrow-phase collision detection;
- ray queries;
- overlap queries;
- shape-sweep queries;
- gravity;
- floor detection;
- wall sliding;
- bounded step traversal;
- non-blocking sensors;
- enter/stay/exit overlap information;
- renderer-independent debug snapshots.

Dynamic rigid-body features extend this model later.

Examples of later capabilities include:

- forces;
- mass properties;
- friction;
- restitution;
- general contact solving;
- constraints;
- continuous collision detection;
- sleeping.

These later features must extend the existing physics/world boundary rather than replacing it with a different runtime model.

### Renderer-Independent Debugging

Physics debugging exposes renderer-independent world-space geometry.

Applications or editor tools may render that geometry using JScene3D line facilities.

The physics module must not gain a rendering dependency merely to provide visualization.

### Genre-Independent Game Runtime

`jscene3d-game` remains a reusable genre-independent game-runtime module.

It provides general facilities such as:

- host/runtime coordination;
- fixed and frame timing;
- semantic input;
- world advancement;
- reusable game/physics coordination.

It must not contain rules, content formats, or assets belonging to one particular game.

### Application-Specific Behavior

Each playable title is a separate Game Application.

Application-specific concerns include:

- game rules;
- actors;
- weapons;
- inventory;
- combat;
- campaign state;
- title-specific HUD behavior;
- title-specific content interpretation;
- title-specific assets.

A capability should move into a reusable engine module only after it has a genuinely game-independent contract and demonstrated reuse.

### Semantic Input

The game runtime uses semantic input actions rather than requiring gameplay Components to depend directly on raw GLFW key codes.

Input mapping translates platform input into authored or configured actions.

Game behavior consumes those semantic actions through the runtime interfaces.

This keeps application behavior independent from one particular desktop input backend.

### World Frame Driving

World advancement separates fixed simulation from rendered-frame presentation.

The frame driver is responsible for concerns such as:

- fixed-step accumulation;
- bounded catch-up;
- long-frame clamping;
- action transition buffering;
- interpolation fraction.

This timing infrastructure coordinates a World rather than introducing a parallel application lifecycle.

### Project Desktop Hosting

The standard desktop project host loads and runs a manifest-selected World using the generic project/runtime architecture.

The host coordinates:

- project manifest loading;
- runtime extension discovery;
- project content;
- input;
- rendering;
- physics;
- frame driving;
- application lifecycle.

Packaged applications use the generic launcher/host model rather than generating a title-specific Java bootstrap that manually constructs the startup World.

### Application Lifecycle

Application-level transitions are distinct from Entity lifecycle.

A project may use authored Worlds for concepts such as:

- startup presentation;
- menu;
- gameplay.

The host owns transitions between hosted Worlds and application-level state.

Application-specific menu presentation and behavior remain authored application concerns rather than hard-coded desktop-host behavior.

## Editor Architecture and Current Implementation

The JScene3D editor is a native JavaFX desktop application built around the same project, runtime, rendering, diagnostic, and extension concepts used elsewhere in the engine.

The editor must not become a parallel model of the project merely because it presents that project graphically.

### Native JavaFX Workbench

JavaFX owns the native editor shell and ordinary desktop controls.

This includes workbench concepts such as:

- application menus;
- Activity Bar;
- primary and secondary sidebars;
- editor tabs;
- Inspector;
- Project views;
- Diagnostics;
- output/build presentation;
- status bar;
- modal dialogs;
- settings presentation.

JavaFX is an editor implementation technology.

It must not leak into toolkit-independent extension contracts or project/runtime models.

### Embedded JScene3D Viewport

The central scene viewport uses OpenGLFX to host the actual JScene3D OpenGL renderer.

The editor does not maintain a second rendering implementation using:

- Three.js;
- WebGL;
- WebGPU;
- another scene graph.

The JavaFX/OpenGLFX host uses the same `RenderSurface` renderer boundary used by other presentation hosts.

The renderer owns rendering and GPU realizations.

The editor host owns:

- JavaFX control lifetime;
- OpenGLFX context lifetime;
- frame scheduling;
- presentation;
- workbench integration.

This allows the editor preview to remain representative of actual JScene3D rendering behavior.

### Preview Versus Running Application

The editor's scene preview is not automatically a running Game Application.

Preview and Game execution are separate concepts.

The ordinary viewport should identify itself as a Preview rather than implying that application behavior is executing.

A future Game or Play mode must be explicit.

Do not execute arbitrary application behavior merely to display an authored World in the editor.

### Safe Project Inspection

The editor is designed to inspect project structure using safe project data such as:

- ProjectManifest;
- asset catalogs;
- EntityDefinitions;
- WorldDefinitions;
- safe Component descriptors;
- published import generations;
- structured diagnostics.

Project inspection should not require loading arbitrary application implementation classes.

Descriptor metadata is the basis for generic Inspector and validation behavior.

### Editor Projection Principle

The visual editor is a projection of the same canonical definitions and descriptors used by:

- JSON;
- project validation;
- runtime composition;
- Java builders where provided.

It is not a second authoring model.

For example:

- the asset browser displays EntityDefinitions without instantiating them;
- opening a definition displays its authored hierarchy;
- generated imported definitions remain read-only;
- placing a definition creates a placement;
- the Inspector exposes authored state and exported contracts;
- runtime-created Entities appear only in live runtime inspection;
- runtime inspection does not automatically write live state into authored content.

### Current Editor State

The current implementation has progressed significantly beyond older README descriptions of the editor as only a read-only project loader and preview.

Current editor screenshots and newer implementation material show a workbench containing:

- native application chrome and menus;
- narrow Activity Bar;
- Scene/Explorer-style navigation;
- Hierarchy presentation;
- Workspace Explorer;
- bundled Extensions view;
- central editor-tab area;
- Monaco source editing;
- source-editor minimap;
- Inspector;
- Project panel;
- Diagnostics panel;
- Build output/presentation;
- status-bar contributions;
- Java/language-support status;
- build status;
- real JScene3D scene preview;
- current MAP01 preview;
- production editor splash.

When source code and older README text disagree about editor capability, inspect the current implementation rather than assuming the README remains complete.

### Current Visual Direction

The accepted editor visual language is quiet and technical.

Primary characteristics include:

- near-black neutral surfaces;
- restrained indigo/violet accent;
- thin structural dividers;
- compact control density;
- small surface-value differences rather than large shadows;
- rendered project content as the strongest visual element.

Indigo is used primarily for concepts such as:

- selection;
- focus;
- active tools;
- product identity.

Red, green, and blue remain available for conventional spatial-axis semantics and should not become general interface accents.

### Custom JavaFX Theme

JScene3D deliberately maintains its own JavaFX visual theme.

An AtlantaFX experiment established that third-party theming was technically possible, but its visual character and density did not match the desired product direction.

Do not replace the custom theme merely to reduce CSS implementation work.

Reusable visual behavior should use:

- semantic looked-up colors;
- shared style classes;
- reusable cell factories;
- consistent interaction-state styling.

Avoid one-off inline styling.

### Workbench Layout

The workbench is organized around standard editor regions rather than one monolithic JavaFX layout.

Conceptually these include:

- Activity Bar;
- Primary Side Bar;
- central editor area;
- Secondary Side Bar;
- lower Panel;
- status bar.

Views have declared default locations, while workbench layout determines current placement where movement is permitted.

The central editor area is a generic tab surface.

Preview, source files, Settings, Welcome content, extension metadata, and other editors own their tab lifecycle through appropriate editor abstractions rather than being hard-coded into one central controller.

### Activity Bar

The Activity Bar provides navigation between primary activities that are not normally visible simultaneously.

An Activity contribution includes semantic information such as:

- stable identity;
- localized title;
- semantic icon;
- order;
- associated primary-sidebar views.

Activity-owned views remain pinned to their declared primary-sidebar activity.

Selecting the currently active Activity may collapse or restore the Primary Side Bar.

Bundled activities should use the same contribution contracts intended for future editor extensions.

### Extensions View

The bundled Extensions view currently represents activated/bundled extension metadata through the editor's extension infrastructure.

It must not imply that a complete public extension marketplace already exists.

Capabilities such as:

- installing arbitrary packages;
- community registry discovery;
- dependency resolution;
- package trust;
- signatures;
- permissions;
- updates;
- removal;

require a deliberate extension-distribution and security architecture before being exposed as functional controls.

### Inspector Presentation

The Inspector projects authored semantic state.

It should present concepts such as:

- selected item name;
- authored kind;
- source;
- generated/read-only state;
- Component descriptor display names;
- authored property values;
- defaults;
- constraints;
- references.

Read-only content should remain visually legible rather than appearing disabled.

The Inspector must not expose arbitrary Java implementation classes merely because a runtime factory eventually implements a Component type.

### Diagnostics Presentation

Diagnostics are structured project/editor data, not merely text log output.

A diagnostic may carry:

- severity;
- stable code;
- source;
- resource/location;
- message;
- structured detail.

The Diagnostics panel should aggregate and present those values consistently regardless of whether the diagnostic originated from:

- project validation;
- build;
- Java language support;
- import;
- editor infrastructure.

Subsystems should not create unrelated parallel “problems” models where the shared diagnostic model already applies.

### Status Presentation

Persistent status-bar information should remain concise.

Long technical detail belongs in:

- Diagnostics;
- Output;
- logs;
- expandable detail surfaces.

Status contributions should expose meaningful state rather than lengthy implementation telemetry.

## Editor Extension and Workbench Architecture

The editor extension architecture separates semantic contributions from JavaFX presentation.

Extensions describe what they contribute.

The workbench decides how those contributions are rendered using the active desktop toolkit and theme.

### Toolkit-Independent Extension Contracts

Public editor extension contracts must not expose:

- JavaFX controls;
- JavaFX events;
- WebKit implementation objects;
- Monaco implementation objects;
- LSP4J objects;
- JDT LS implementation types.

Extension-facing values should describe editor semantics.

JavaFX adapters translate those values into native presentation.

### Built-In Features Use Extension Contracts

Bundled editor functionality should use the same contribution mechanisms intended for installed extensions where those mechanisms apply.

Built-in functionality must not receive a parallel privileged API merely because it ships with the editor.

This principle is important because it validates extension contracts against real first-party use before exposing them to third parties.

Examples include bundled:

- activities;
- views;
- commands;
- status contributions;
- extension metadata;
- language support.

### Stable Contribution Identity

Contributions use stable identities.

Identity should remain separate from localized presentation text.

Stable IDs are used for concepts such as:

- commands;
- views;
- activities;
- settings;
- languages;
- extension ownership;
- workbench placement.

Localized titles must not become lookup keys.

### View Contributions

A contributed View describes semantic content and behavior without constructing its JavaFX container.

The workbench owns:

- current region placement;
- JavaFX container;
- styling;
- focus integration;
- visibility;
- layout behavior.

Views may expose toolkit-independent models suitable for concepts such as:

- trees;
- collections;
- detail presentation;
- actions;
- selection.

Do not force every extension to implement its own JavaFX tree/list boilerplate.

### Workbench Placement

A View contribution declares its default workbench container.

The workbench separately owns the current placement.

Movable ordinary views may be placed in appropriate regions such as:

- Primary Side Bar;
- Secondary Side Bar;
- lower Panel.

Activity-owned views remain pinned to their declared Activity location.

The workbench layout model should remain toolkit-independent even though JavaFX renders the current layout.

### Layout Customization

One editor-owned layout customization surface controls workbench placement and region visibility.

Individual Views should not each invent context-menu behavior for moving themselves.

The layout model may include concepts such as:

- region visibility;
- primary-sidebar side;
- movable View placement;
- restoration of defaults.

Session-only movement can validate interaction before durable workspace-state persistence is added.

### Selection Model

Selection is an editor semantic concept rather than a JavaFX-control implementation detail.

Hierarchy, Project views, Inspector, commands, and other surfaces may need to observe or publish semantic selection.

A selection should retain stable identity and enough semantic context to support:

- inspection;
- commands;
- diagnostics;
- navigation.

Commands that originate from a specific item surface should still receive that exact item as their argument rather than depending on whichever global selection happens to exist at execution time.

### Semantic Icons

Extensions and editor modules should refer to semantic icon identities rather than embedding JavaFX image construction throughout feature code.

The workbench/theme infrastructure resolves semantic icon identity into presentation.

This allows future icon-theme support without changing every contribution interface.

Color-theme contributions and icon-theme contributions remain separate concepts.

### Status Contributions

Status items are contributions to the workbench rather than arbitrary JavaFX nodes inserted by subsystems.

A status contribution should expose semantic state and actions while allowing the workbench to own:

- placement;
- styling;
- interaction presentation.

Examples include:

- Java language-server state;
- project-build state;
- diagnostic state.

### Commands as Shared Operations

Extensions register an operation once under a stable command identity.

Different presentation surfaces invoke that same command.

A command should not have separate implementations for:

- menu;
- context menu;
- toolbar;
- keybinding;
- command palette.

This avoids behavior divergence between surfaces.

### Declarative Menu Placement

Menu contributions describe placement.

They do not construct JavaFX `Menu` or `MenuItem` objects.

Placement metadata may include concepts such as:

- target menu/location;
- grouping;
- ordering;
- contextual availability.

The JavaFX adapter renders the resulting menu structure.

### Modal Interaction

Extensions should request semantic modal interactions rather than constructing arbitrary JavaFX dialog controls through the public extension API.

The workbench owns native dialog presentation.

This preserves:

- toolkit independence;
- consistent visual design;
- accessibility;
- window ownership;
- localization.

### Extension Lifecycle Boundaries

Safe extension metadata and executable extension behavior are different trust boundaries.

The editor should be capable of inspecting appropriate declarative metadata without executing extension implementation code.

Installing arbitrary external extension packages introduces additional concerns such as:

- package provenance;
- signatures;
- trust;
- permissions;
- dependency resolution;
- updates;
- executable language servers.

Those concerns require their own architecture and must not be bypassed by accepting arbitrary executable paths from project files.

### Localization

User-visible extension/editor text should resolve through the shared localization/message abstraction.

Stable IDs remain language-neutral.

Do not embed operating-system-specific strings or user-visible command labels inside deep command behavior when shared localization/platform abstractions already exist.

## Editor Working Copies and Editable Resources

Editable editor content is modeled through resource working copies rather than accumulating mutable resource state inside `EditorProjectSession` or individual JavaFX controls.

This architecture is intended to support multiple editable resource types consistently.

### Working Copy Responsibility

An `EditorWorkingCopy` represents the editor's current editable state for one logical resource.

Its responsibilities include concepts such as:

- current content;
- saved baseline;
- dirty state;
- save;
- revert;
- content-change notification;
- dirty-state notification;
- save notification.

The working copy is authoritative for unsaved editor content.

A source editor, graphical editor, language server, settings editor, or other subsystem must not independently maintain a conflicting unsaved version of the same resource.

### Working Copy Identity

Working-copy identity combines the canonical resource identity with a stable working-copy type.

This allows the editor to distinguish different logical editing models where necessary while still avoiding duplicate mutable state for the same editor resource.

Identity should not depend on:

- tab title;
- display label;
- current selection;
- temporary JavaFX node.

### Saved Baseline and Dirty State

Dirty state is derived from the relationship between the current working-copy state and its saved baseline.

The working copy owns this relationship.

Individual editor tabs should not invent independent dirty flags.

The workbench may aggregate dirty state, but the resource working copy remains authoritative for whether that resource has unsaved changes.

### Working Copy Registry

`EditorWorkingCopyRegistry` owns registration and lookup of working copies.

It also derives aggregate workspace dirty state.

The registry does not become the persistence implementation for every resource.

Resource-specific working copies remain responsible for their own content model and saving through the appropriate underlying resource seam.

### EditorProjectSession Boundary

`EditorProjectSession` coordinates project-level services.

It must not become a catch-all mutable store containing the current editable content of every resource.

Moving editable state into resource working copies prevents the project session from accumulating unrelated responsibilities as the editor gains:

- world editing;
- settings editing;
- Java editing;
- other text resources;
- additional project asset editors.

### Shared Save Behavior

Global editor commands such as Save or Save All operate through registered working copies.

They should not contain resource-type-specific persistence logic.

Save All should save the applicable dirty working copies through their normal save operations.

This gives future resource types consistent participation without continually enlarging the File menu implementation.

### Close Guards

Closing:

- an editor tab;
- a project;
- the editor window;
- the application;

must respect dirty working-copy state.

Dirty-resource prompting and save/discard/cancel behavior should be centralized rather than independently reimplemented by every editor type.

Native window close and explicit Quit should pass through the same dirty-resource guard.

### Source Editor Integration

A source editor displays and edits the working copy.

The JavaScript/Monaco editor is not the persistence authority.

Changes flow from Monaco into the editor-owned working-copy model.

Saving flows through the working copy and underlying filesystem/resource infrastructure.

This becomes especially important when language-server synchronization is active.

### Language Server Integration

The working copy is authoritative for unsaved source content.

Language-server document synchronization mirrors that state.

A language server must not overwrite an open working copy by writing directly to disk behind it.

Server-requested edits must pass through editor-owned edit application and working-copy infrastructure.

### Settings Integration

Graphical settings editing and direct JSON settings editing must use the same underlying working copy.

A graphical settings control must not write around an open dirty JSON editor.

Likewise, direct JSON editing must not create a second settings state unrelated to the generated graphical editor.

### Future Resource Types

The working-copy architecture is intended to scale to resources such as:

- authored Worlds;
- EntityDefinitions;
- project settings;
- user settings;
- Java source;
- other text files;
- future editable project assets.

Resource-specific implementations may differ, but they should participate in the same editor concepts:

- identity;
- dirty state;
- save;
- close guard;
- tab lifecycle;
- external-change handling.

### Recovery Is Separate

Unsaved working-copy recovery is not ordinary workspace-layout persistence.

A future recovery system may persist unsaved content for crash recovery, but it requires:

- explicit document versions;
- recovery ownership;
- user-visible restore/discard behavior.

Restoring an editor tab must never silently replace current saved content with an old unsaved buffer merely because that buffer appeared in workspace-state storage.

## Editor Settings, Preferences, and Workspace State

The editor persists several kinds of information with different ownership, portability, lifetime, and failure semantics.

These categories must remain distinct even when multiple categories happen to use JSON storage.

### Persistent State Classification

The accepted classification is:

- **Project setting**: intentional project configuration shared by collaborators.
- **User setting**: intentional developer preference that applies across workspaces.
- **Workspace preference**: intentional developer preference scoped to one workspace.
- **Workspace state**: automatically captured workbench restoration information.
- **Session state**: transient state belonging to one running editor process.
- **Recovery data**: unsaved content retained specifically for failure recovery.

Do not merge these categories merely because they are serializable.

They differ in:

- ownership;
- portability;
- source-control expectations;
- privacy;
- merge behavior;
- restoration semantics;
- failure handling.

### Project Settings

Project settings are portable authored configuration.

They belong in:

`<project>/.jscene3d/settings.json`

Project settings may include concepts such as:

- asset roots;
- extension configuration;
- project exclusions;
- project-specific editor/engine behavior where explicitly supported.

Project settings may be committed to source control.

They must not contain machine-specific paths, private workbench layout, or incidental UI restoration data.

Unknown extension-owned settings should remain preserved when their contributor is temporarily unavailable.

### User Settings

User settings represent intentional developer choices that apply across workspaces.

Examples include:

- selected color theme;
- preferred editor font family;
- editor font size;
- minimap visibility.

User settings belong in platform-appropriate JScene3D application configuration rather than inside a project.

Opening a different project must not unexpectedly change inherently user-scoped preferences such as the selected theme.

Existing Java Preferences values used by older implementation should be treated as transitional storage and migrated or consumed as a one-time fallback when the common settings system replaces them.

### Workspace Preferences

A Workspace preference is an intentional developer choice applying to one project/workspace without modifying that project.

**Build Automatically** is the primary current example.

Two developers may choose different automatic-build behavior for the same repository.

One developer may also choose different behavior for different projects.

Such preferences therefore must not be written into:

- `jscene3d.json`;
- `.jscene3d/settings.json`;
- another repository file.

Workspace preferences are still settings rather than automatically captured restoration state.

Where appropriate, the generated settings experience may eventually expose a Workspace scope using the same settings module.

### Effective Setting Resolution

A `SettingDefinition` declares the scopes in which it is valid.

Where a setting supports multiple scopes, effective-value resolution should be explicit.

The accepted general direction is:

`Workspace override -> Project override -> User value -> declared default`

Only scopes actually supported by that definition participate.

Some settings are inherently user-specific and must not accept Project or Workspace overrides.

The selected application theme is an example.

### Declarative Setting Definitions

Core modules and extensions contribute declarative `SettingDefinition` values.

They do not contribute:

- JavaFX controls;
- settings-document parsers;
- persistence implementations;
- independent search entries.

A SettingDefinition supplies the semantic information required by the settings infrastructure.

The generated editor can then provide consistent:

- controls;
- validation;
- diagnostics;
- persistence;
- search;
- effective-value resolution.

Safe extension setting declarations should be discoverable without executing extension implementation code.

### Generated Settings Experience

The accepted direction is one searchable settings experience rather than unrelated settings systems.

Primary scopes initially include:

- User;
- Project.

A Workspace scope may follow for appropriate private per-workspace preferences.

Appearance is a category of User settings rather than a separate settings architecture.

The settings UI should support search over useful metadata such as:

- display name;
- stable setting key;
- description;
- category;
- contributing owner/extension.

### Direct JSON Editing

The graphical settings editor and direct JSON editing are two presentations of the same underlying configuration.

An **Open Settings (JSON)** operation opens the relevant settings document through the ordinary source-editor infrastructure.

The settings module remains authoritative for:

- validation;
- effective values;
- persistence;
- change events;
- diagnostics.

Direct JSON editing uses the resource working-copy architecture.

A graphical settings edit must not overwrite unresolved dirty JSON edits behind the user's back.

Invalid JSON or invalid known values should produce diagnostics while the last valid active configuration remains effective.

Unknown keys remain preserved where the settings contract requires forward/extension compatibility.

### Workspace State

Workspace state is private automatically captured information used to restore the editor to a useful shape.

Examples may include:

- open editor identities;
- editor order;
- selected editor;
- preview/pinned state;
- editor groups;
- split proportions;
- Primary Side Bar visibility;
- Secondary Side Bar visibility;
- Panel visibility;
- workbench region sizes;
- active View within each region;
- Explorer expansion;
- Explorer selection;
- editor cursor/selection/scroll positions;
- window bounds;
- maximized/full-screen state.

Workspace state is descriptive rather than authoritative.

Missing resources, unavailable extensions, removed Views, changed monitor configurations, and newer schemas must not prevent the workspace from opening.

Invalid restoration entries should be ignored safely and diagnosed where useful.

### Workspace State Must Not Contain Live Processes

Workspace state must not attempt to persist and resurrect transient runtime infrastructure.

Do not persist as active state:

- build processes;
- JDT LS processes;
- progress operations;
- menus/dialogs;
- keyboard focus;
- current language-server diagnostic publications.

Those are Session state.

They are recreated or recomputed during a new editor process.

### Workspace State Must Not Contain Unsaved Content

Restoring an open document is different from restoring unsaved document content.

Workspace state may remember that a resource was open.

The saved resource content is then read normally.

Unsaved working-copy content belongs to a separate future recovery system.

### Workspace State Storage

Workspace state belongs in private application state for the current operating-system user.

It must not be written into the project repository merely for convenience.

Platform-directory selection belongs behind the appropriate application-directory abstraction.

Individual Views and editor modules must not independently:

- choose state files;
- parse the root workspace-state document;
- schedule their own writes.

A deep workspace-state module owns versioning, snapshots, namespacing, migration, atomic persistence, and lifecycle ordering.

### Workspace Identity

One shared module owns workspace identity normalization and lookup.

Other modules must not independently invent path hashes or alternative workspace identities.

The initial implementation may derive an opaque identity from normalized project location, but the design should allow later replacement if stable project identities need to survive project moves.

### Restoration Failure Policy

Restoration is best effort.

A malformed or obsolete state entry must not prevent a valid project from opening.

Window bounds should be clamped to available displays.

Unavailable contributed Views or editors should be skipped rather than treated as fatal project errors.

### Initial Workspace Restoration Scope

The first useful restoration slice should prioritize stable high-value state such as:

1. window bounds/maximized state;
2. workbench region visibility and split positions;
3. active Activity Bar and Panel Views;
4. open editors, order, preview/pinned state and selected editor;
5. Explorer expansion state.

More detailed state such as cursor positions, multiple editor groups, extension-specific state, retained output, and crash recovery can follow after the core persistence seam is proven.

## Editor Commands, Menus, Keybindings, and Source Navigation

Editor operations follow a command-first architecture.

A semantic operation is registered once and reused by every presentation surface.

### Stable Command Identity

Commands have stable language-neutral identities.

A command implementation owns the semantic operation.

Presentation surfaces invoke that implementation through its stable command ID.

Do not implement separate behavior for:

- application menus;
- context menus;
- toolbars;
- command palette;
- keyboard shortcuts;
- status actions.

This prevents behavior from diverging between surfaces.

### Exact Command Arguments

Item-oriented command invocation supplies the exact semantic object that initiated the command.

Examples include:

- Workspace Explorer entry;
- editor tab;
- authored Entity;
- Component instance;
- text selection.

A command must not infer its target from mutable global selection when the invoking surface already knows the exact target.

For example, a tab context-menu Close operation acts on the tab whose context menu was opened, not whichever tab becomes globally selected before execution.

### Command Availability

Command availability is contextual.

A command may be:

- absent;
- visible but disabled;
- enabled.

Availability may depend on:

- semantic target kind;
- editor state;
- working-copy state;
- language capability;
- build state;
- platform capability.

Presentation surfaces observe command state rather than maintaining independent enabled-state logic.

### Declarative Command Placement

Commands are placed into workbench surfaces declaratively.

Placement metadata describes concepts such as:

- target location;
- grouping;
- ordering;
- contextual conditions.

JavaFX renders those declarations.

JavaFX controls do not become the ownership location for command behavior.

### Core Command Surfaces

Useful command groups include project build operations, Workspace Explorer operations, editor-tab operations, source-editor operations, and language-contributed operations.

These groups describe user-facing organization rather than separate command systems.

### Command Palette

The command palette is a generic searchable presentation of commands explicitly placed into the palette.

It does not implement operations itself.

It does not contain special knowledge of:

- Java;
- JDT LS;
- Maven;
- project formats;
- JavaFX implementation classes.

Search may use:

- localized command title;
- category;
- contributed keywords;
- stable command ID.

Context and command state determine whether a result is available.

The conventional platform Command/Control+Shift+P interaction is expected, but it should use the same shared keybinding architecture as other shortcuts.

### Keybinding Architecture

Keybindings invoke stable command IDs.

They must not reference JavaFX handlers directly.

The representation should be toolkit-independent.

It must support a platform-neutral primary modifier so the same declaration can map naturally to:

- Command on macOS;
- Control on Windows/Linux.

Built-in defaults, extension defaults, and user overrides are distinct concepts.

User overrides take precedence over defaults.

Conflicting active bindings should produce an actionable diagnostic rather than arbitrarily invoking one command.

Context conditions prevent shortcuts from activating in inappropriate workbench regions.

### Platform-Specific Presentation

Platform-specific behavior should use shared platform abstractions.

Do not embed strings such as macOS-specific Finder labels directly in deep command implementations.

User-visible command titles, confirmations, and errors resolve through the shared message/localization infrastructure.

### Workspace Explorer Commands

Expected Workspace Explorer command direction includes operations such as:

- Open;
- Open With;
- Copy Path;
- Copy Relative Path;
- Reveal in platform file manager;
- Rename;
- Delete;
- New File;
- New Folder.

Filesystem operations must resolve and validate targets before mutation.

Destructive operations require appropriate confirmation and should use recoverable platform behavior where practical.

### Editor Tab Commands

Tab-oriented operations include concepts such as:

- Pin;
- Unpin;
- Close;
- Close Others;
- Close to the Right;
- Close All;
- Copy Path;
- Reveal in Explorer.

Dirty-resource handling continues through the shared working-copy and close-guard infrastructure.

### Source Editor Commands

Generic source-editor operations include:

- Undo;
- Redo;
- Cut;
- Copy;
- Paste;
- Select All.

Language-aware operations are contributed when supported by the active language capability.

Examples include:

- Go to Definition;
- Find References;
- Rename Symbol;
- Format Document;
- Quick Fix.

The source editor supplies document/cursor/range/language context.

The language adapter performs semantic language work.

### Authored Entity-to-Source Navigation

JScene3D does not model application behavior by attaching one Java script class directly to each Entity.

An Entity may contain multiple behavioral Components identified by stable Component type IDs.

Serialized project content must not acquire Java implementation class names merely to support editor navigation.

Entity/component source navigation therefore starts from a semantic authored target.

The intended flow is:

`Hierarchy or Inspector target -> source-reference provider -> language-neutral source target -> navigation adapter -> ordinary editor tab`

### Source Reference Provider

The source-reference provider maps semantic project targets to source targets.

Hierarchy and Inspector code must not:

- scan Java source roots itself;
- inspect runtime implementation classes;
- understand LSP;
- maintain a second Java index.

A source target may identify:

- resource URI;
- source range;
- symbol where known.

The exact source-association mechanism remains a feature-specific design concern.

### Java Source Resolution

Java LSP should become authoritative for Java semantic resolution where it has sufficient information.

Entity-to-source association and LSP symbol navigation solve different problems.

The source-reference provider answers:

> Which source is associated with this authored Component or Entity concept?

The language server answers questions such as:

> Where is this Java symbol defined?

Both ultimately use the same editor navigation/opening infrastructure.

### Source Opening

Source navigation uses the normal editor-file path.

It must preserve normal behavior for:

- editor identity;
- preview tabs;
- pinned tabs;
- working copies;
- dirty state.

Language navigation must not create a parallel tab/document lifecycle.

## Workspace Explorer Architecture

The Workspace Explorer presents filesystem content beneath the open project root while hiding generated, private, and editor-owned content that is normally noise.

The Explorer is a filesystem projection, not the project Asset browser.

### Visibility Concepts

Explorer visibility distinguishes:

- **default exclusion**;
- **project inclusion**;
- **project exclusion**;
- **safety rule**.

A default exclusion is supplied by the editor or another trusted subsystem and hides a path unless an allowed Project inclusion overrides it.

A Project inclusion makes normally excluded content visible.

It is an exception list, not a whitelist.

A Project exclusion explicitly hides matching project content.

A safety rule prevents traversal regardless of Project settings.

### Safety Rules

Filesystem safety takes precedence over user visibility configuration.

Paths outside the normalized project root must not be exposed through ordinary Explorer traversal.

Symbolic-link traversal is excluded by the accepted initial safety direction.

Project settings must not be able to override a safety boundary.

### Visibility Resolution

The accepted resolution order is:

1. reject a path violating a safety rule;
2. hide a path matching a Project exclusion;
3. show a path matching a Project inclusion;
4. hide a path matching a default exclusion;
5. show the path.

Therefore:

- Project exclusions override Project inclusions;
- Project inclusions may override ordinary default exclusions;
- neither can override safety rules.

### Default Exclusions

Default exclusions include ordinary development noise such as:

- `.git` directories;
- `target` directories;
- `.DS_Store`;
- `.jscene3d/cache`.

Resolved project cache locations may also contribute exclusions.

Build adapters may contribute additional generated-output exclusions.

These are ordinary defaults rather than hard safety boundaries and may therefore be revealable through explicit Project inclusion where the design permits it.

### Workspace Explorer Settings Contract

Explorer inclusion/exclusion configuration belongs in the Project settings system.

The intended settings are conceptually equivalent to:

- `jscene3d.explorer.inclusions`;
- `jscene3d.explorer.exclusions`.

The settings system owns:

- declaration;
- loading;
- validation;
- diagnostics;
- persistence.

The Explorer receives typed resolved values.

It does not parse `.jscene3d/settings.json` itself.

### Glob Semantics

Explorer patterns use normalized workspace-relative paths with `/` separators across platforms.

The accepted pattern language is deliberately small.

It includes concepts such as:

- `*` within one path segment;
- `?` for one character within a segment;
- `**` across path segments;
- exact relative paths.

Absolute patterns, empty patterns, and patterns escaping the workspace through `..` are invalid.

When a pattern matches a directory, its decision applies to that subtree.

### Declarative String-List Settings

Explorer patterns require list-valued settings.

If the common settings infrastructure does not yet support a string-list value family, that capability should be added to the settings model.

Do not bypass the declarative settings architecture by teaching Explorer code to parse arbitrary JSON arrays itself.

### Immutable Visibility Policy

The Explorer should operate from one resolved immutable visibility-policy snapshot combining:

- safety rules;
- editor defaults;
- build-adapter defaults;
- Project inclusions;
- Project exclusions.

When relevant Project settings change, rebuild the policy and refresh the Explorer.

Reopening the complete project should not be necessary merely to apply a visibility-setting change.

### Explorer Versus Project Browser

Do not confuse filesystem navigation with the JScene3D Project/Asset browser.

The Workspace Explorer answers:

> What files exist beneath this project root?

The Project browser answers questions about JScene3D concepts such as:

- Worlds;
- Entity Definitions;
- Source Assets;
- Imports.

Both may ultimately open resources through shared editor infrastructure, but they represent different semantic projections.

## Java Language Server Architecture

Java language support is intended to provide genuine IDE behavior while preserving a language-neutral editor architecture.

The accepted direction uses Eclipse JDT Language Server for Java and LSP4J for the Java-side Language Server Protocol implementation.

Exact lower-level interfaces may evolve during implementation, but the architectural boundaries are established.

### Language Support Architecture

The conceptual flow is:

`Monaco source editor -> JavaFX/Monaco language adapter -> language-neutral editor interface -> LSP module using LSP4J -> JDT Language Server child process`

Each layer has a distinct responsibility.

Monaco owns source presentation and interaction.

The JavaFX/Monaco adapter bridges the WebView boundary.

The language-neutral editor interface exposes editor concepts.

The LSP module owns protocol and process behavior.

The Java support module configures and packages JDT LS.

### Language-Neutral Public Seam

The external editor-facing language interface uses concepts such as:

- language identity;
- documents;
- positions;
- ranges;
- edits;
- diagnostics;
- completion;
- navigation targets;
- language capabilities.

It must not expose:

- JSON-RPC messages;
- LSP4J classes;
- JDT LS implementation classes;
- JavaFX controls;
- WebKit objects;
- Monaco JavaScript objects.

This seam is intended to support future non-Java language extensions without requiring the workbench to understand each language server.

### LSP Module Responsibility

The reusable LSP module is a deep module.

It hides concerns such as:

- server-process startup;
- standard-input/output transport;
- LSP4J lifecycle;
- initialization;
- negotiated capabilities;
- request dispatch;
- cancellation;
- stale-result handling;
- document versions;
- document synchronization;
- protocol conversion;
- server notifications;
- shutdown;
- bounded failure recovery;
- protocol-level logging.

JavaFX presentation and Java-specific JDT configuration do not belong in this module.

### Java Support Responsibility

Bundled Java support owns Java/JDT-specific concerns such as:

- JDT LS distribution discovery;
- JDT LS launch configuration;
- Java-project applicability;
- Maven-aware initialization;
- Java-specific configuration;
- Java status;
- Java diagnostic ownership;
- Java language commands;
- JDT LS packaging metadata;
- third-party notices.

The workbench and Monaco host must not know which concrete Java language server implementation is used.

### JDT LS Process Model

JDT LS runs as a child process rather than being embedded into the editor implementation.

Communication uses standard input/output through LSP4J.

The editor launches the server directly rather than introducing an unnecessary shell process.

Standard output carries protocol traffic.

Standard error is consumed independently for logging so neither stream can block the child process.

### Language Session Scope

The initial architecture uses one Java language-server session per open project.

It does not create one JDT LS process per editor tab.

All applicable Java source editors in the same project share that project language session.

Project close shuts down the associated language-server session.

### Background Startup

Language support starts after the project session becomes available.

It must not keep the editor splash open or prevent the primary workbench from becoming usable.

Language initialization may continue in the background.

The workbench exposes concise lifecycle state such as:

- Starting;
- Importing project;
- Ready;
- Error.

### JDT LS Distribution

Java support is intended to work out of the box.

A released editor therefore contains a pinned, checksum-verified JDT LS distribution rather than requiring developers to install a separate `jdtls` executable.

Packaged JDT LS files are immutable.

Configuration/workspace files that JDT LS must modify are staged in editor-managed cache/state locations.

Required licenses and notices must ship with the distribution.

### Editor Runtime Versus Project JDK

The Java runtime used to launch JDT LS and the JDK used by the developer's project are separate concerns.

The packaged editor may run JDT LS using its bundled Java runtime.

Future project-JDK configuration may select a different JDK for project compilation or analysis.

Do not couple the editor's own runtime identity to the project's configured Java toolchain.

### Document Synchronization

The editor working copy is authoritative for an open document.

The language-server session mirrors that state.

The normal synchronization lifecycle is:

- `didOpen` with current content/version;
- ordered incremental `didChange`;
- `didSave` after successful working-copy save;
- `didClose` after the final editor for the document closes.

Monaco changes use zero-based UTF-16 positions, which aligns with the editor diagnostic position convention.

### Document Versions

Language requests are associated with document versions.

A result that cannot safely be applied to the current document version must be:

- discarded;
- rejected;
- re-requested where appropriate.

Do not apply stale language results blindly to a newer working copy.

Closing a document or cancelling the corresponding Monaco operation should cancel outstanding LSP requests where the protocol permits it.

### Monaco Provider Bridge

Monaco remains responsible for source-editor interaction.

Language features use Monaco provider APIs for capabilities such as:

- completion;
- hover;
- signature help;
- definitions;
- references;
- formatting;
- code actions;
- rename.

Requests crossing the JavaFX WebView boundary are asynchronous.

The expected pattern is:

1. Monaco creates a request identity and Promise.
2. The Java bridge starts the corresponding language request.
3. The LSP/language layer completes asynchronously.
4. Java resolves or rejects the matching Promise on the JavaFX thread.
5. Monaco cancellation cancels the corresponding Java request where possible.

No language request may block the JavaFX Application Thread.

### Bridge Values

Values crossing the WebView boundary are editor-owned representations.

Do not expose raw LSP JSON as the editor's public bridge contract merely because it would reduce conversion code.

Feature-specific converters should remain small and independently testable.

The bridge should not accumulate every language feature into one enormous Java class or JavaScript file.

### Completion Architecture

Completion is part of the first useful Java vertical slice.

Conceptually:

`Monaco completion request -> JavaFX bridge -> language-neutral completion request -> LSP conversion -> JDT LS -> LSP result -> editor completion values -> Monaco completion result`

Completion implementation should preserve:

- asynchronous execution;
- document version;
- cancellation;
- position conversion;
- explicit result conversion;
- stale-result handling.

Monaco-specific completion objects do not belong in the LSP module.

LSP4J completion objects do not belong in the Monaco-facing public editor seam.

### Diagnostics

JDT LS diagnostics are translated into the shared editor diagnostic model.

Java support owns one replaceable diagnostic publication rather than creating a parallel Java-specific Problems model.

The Diagnostics panel consumes those ordinary editor diagnostics.

Open Monaco editors observe the same diagnostic state for their resource and render appropriate markers/squiggles.

Diagnostic replacement must be version-aware.

Restarting or closing Java support clears stale diagnostics owned by that session.

### Server-Requested Edits

JDT LS may request edits affecting one or more resources.

Those edits must pass through editor-owned edit infrastructure.

The edit application layer is responsible for concerns such as:

- target URI validation;
- expected document versions;
- updating open working copies;
- preserving dirty state;
- normal Save behavior;
- filesystem create/rename/delete operations;
- atomic failure where required.

A language server must not write directly behind an open working copy.

Early implementation may reject unsupported multi-resource edit kinds.

It must not partially apply a multi-resource edit and leave the project in an unknown state.

### Cross-File Navigation

Cross-file navigation opens destination resources through the existing editor-file infrastructure.

Language support does not create an independent source-tab lifecycle.

Navigation uses normal:

- tab identity;
- preview/pinning;
- working-copy handling;
- range reveal.

### Language Server Failure

Unexpected JDT LS termination produces visible language status and appropriate diagnostics/messages.

Any automatic restart policy must be bounded.

A broken language server must not enter an invisible infinite restart loop.

Normal project close requests LSP shutdown followed by exit.

A bounded timeout may terminate an unresponsive process after graceful shutdown fails.

### Separation from Project Builds

JDT LS project understanding is not the project build coordinator.

Language analysis and runnable build freshness are separate responsibilities.

Saving Maven/build configuration may eventually cause language-project refresh, but the LSP session does not become responsible for determining whether the project has a current successful runnable build.

### Initial Java Language Slice

The first useful Java vertical slice establishes:

- language-neutral seam;
- LSP lifecycle;
- bundled JDT LS staging/launch;
- non-blocking Maven-project initialization;
- incremental Java document synchronization;
- Java diagnostics;
- Monaco diagnostic markers;
- Monaco completion;
- Java lifecycle status;
- clean project shutdown.

This slice proves the complete path from Monaco through LSP4J to a real Java project.

### Later Java Language Slices

Navigation/information capabilities follow with features such as:

- hover;
- signature help;
- Go to Definition;
- modified-click navigation;
- references;
- implementations;
- document symbols.

Source transformations follow with capabilities such as:

- document formatting;
- selection formatting;
- quick fixes;
- source actions;
- rename;
- safe multi-resource edits;
- Maven project refresh.

Richer intelligence may later include:

- semantic tokens;
- inlay hints;
- code lenses;
- call hierarchy;
- type hierarchy.

Exact feature order may change when implementation dependencies become clearer.

### Language Support Testing

The language-neutral interface is the primary test seam.

Tests should use fake/controllable language implementations where possible.

Normal unit tests must not download or start JDT LS.

Dedicated integration or packaging tests exercise the pinned real distribution against a controlled Maven workspace.

Failure tests should cover relevant cases such as:

- malformed protocol behavior;
- early process exit;
- initialization timeout;
- stale versions;
- cancellation;
- unsupported server requests;
- shutdown timeout.

Native qualification should verify at least startup responsiveness, diagnostics, completion, process cleanup, and packaged JDT LS discovery.

## Project Build Architecture

Project builds are a separate editor subsystem from language-server analysis.

The accepted build architecture is actively being implemented.

Its purpose is to provide reliable development builds without blocking the JavaFX workbench or coupling future runtime execution directly to Maven.

### Build Versus Language Analysis

JDT LS may understand and analyze a Maven project, but that does not make JDT LS the editor's authoritative project build system.

Project builds establish whether a particular saved project revision has a successful development output suitable for later execution.

Language diagnostics and build diagnostics remain distinct diagnostic sources even when they report related compilation problems.

### Automatic Build Behavior

Automatic building is enabled by default for a newly opened workspace unless the developer has an existing Workspace preference.

Automatic builds trigger after successful saves of build-relevant resources.

They do not trigger after every editor keystroke.

Unsaved working copies remain unsaved and must not be presented as though they were part of a completed build.

### Build-Relevant Resources

The active build adapter determines which saved resources affect build output.

For Maven projects this may include:

- Java source;
- `module-info.java`;
- `pom.xml`;
- Maven wrapper files;
- Maven configuration;
- generated-source inputs;
- processed resources;
- other build-declared inputs.

The generic build coordinator must not hard-code `src/main/java` as the complete build input set.

Saving unrelated documentation or private editor state should not trigger a project build.

### Save Coalescing

One logical Save All operation may save several build-relevant resources.

Automatic build requests use a short coalescing period so that one logical save operation does not start several redundant builds.

Changes saved while a build is already queued or running are also coalesced.

At most one build process runs for a project at a time.

### Saved Project Revisions

Every build request captures the saved-project revision it is building.

A later save may make an in-progress build obsolete.

The obsolete build may still finish and report its result, but it cannot make the newest saved project revision `CURRENT`.

When automatic building is enabled, changes arriving during a build produce exactly one follow-up request for the newest saved revision.

Additional saves update that pending revision rather than starting additional processes.

### Build State Model

The accepted project build states are:

- `UNKNOWN`;
- `CURRENT`;
- `STALE`;
- `QUEUED`;
- `BUILDING`;
- `FAILED`.

`UNKNOWN` means no result has yet established freshness for the current session.

`CURRENT` means the latest saved build-relevant revision has succeeded.

`STALE` means newer saved build-relevant input exists.

`QUEUED` means a build has been requested but has not started.

`BUILDING` means the adapter is executing the build.

`FAILED` means the latest completed relevant saved revision failed.

Cancellation is an outcome rather than a persistent freshness state.

### Unsaved Changes and Build Freshness

Saved build freshness and unsaved editor state are independent.

A project may simultaneously have:

- a `CURRENT` successful build for the saved revision;
- dirty build-relevant working copies containing newer unsaved changes.

This distinction is required for future behavior such as Play Saved Version.

### Semantic Build Intents

The generic build interface defines semantic build intentions rather than Maven commands.

Initial intents are:

- incremental development build;
- clean development build.

Automatic build and **Build Project** use the incremental intent.

**Rebuild Project** uses the clean intent.

Callers do not construct Maven goals.

### Build Adapter

A build adapter owns build-system-specific behavior.

Responsibilities include:

- applicability;
- relevant-input classification;
- invocation;
- cancellation;
- diagnostic translation;
- successful build-snapshot construction.

Maven is the first bundled adapter.

A future second build system should use the same coordinator seam without requiring changes to:

- menus;
- status presentation;
- Play coordination;
- generic build state.

### Maven Adapter

The Maven adapter prefers the project's Maven Wrapper.

It owns decisions such as:

- wrapper selection;
- reactor root;
- goals;
- JDK;
- environment;
- output locations.

Other editor modules must not infer Maven output by assuming `target/classes`.

The ordinary development build does not clean.

Maven and configured plug-ins determine what work can be reused.

Do not introduce a Maven daemon or alternative fast compiler without measuring actual project build performance and establishing a concrete need.

### Deep Build Module

The project-build subsystem should hide:

- saved-revision tracking;
- freshness state;
- automatic-build policy;
- request coalescing;
- build serialization;
- adapter selection;
- process lifetime;
- cancellation;
- progress;
- diagnostic publication;
- output capture;
- successful build snapshots.

Callers request builds and observe immutable state/results.

They do not coordinate processes or parse tool output themselves.

### Successful Build Snapshot

Only a successful result for the newest saved revision replaces the current successful build snapshot.

A failed, cancelled, or obsolete result must not silently make older output current.

The snapshot carries build-system-neutral information required by future runtime launching.

It must not expose a Maven process or require runtime code to infer launch information from conventional Maven directory names.

### Build Diagnostics

Each build owns a replaceable diagnostic publication.

Structured source errors/warnings use the shared Diagnostics infrastructure.

A diagnostic that cannot be attached to a precise source range may attach to the project or build descriptor.

Build diagnostics identify their source independently from JDT LS diagnostics.

### Revision-Aware Inline Diagnostics

Inline build diagnostics should be shown only when the diagnostic's saved revision still corresponds to the document being displayed.

A build diagnostic for an older saved revision must not place a misleading marker onto a newer unsaved working copy.

### Build Output

Complete build information belongs in a Build output channel.

Useful retained information includes:

- standard output;
- standard error;
- command context;
- duration;
- exit outcome.

If a generic Output infrastructure is required, introduce the minimum reusable output-channel seam rather than creating a Maven-only text control.

Language support and runtime processes may later contribute their own output channels through the same infrastructure.

### Build Status

Persistent status presentation should remain concise.

Examples include:

- Build required;
- Build queued;
- Building;
- Build succeeded;
- Build failed.

Do not invent a percentage when the build adapter cannot provide meaningful progress.

A failed build status may navigate to Diagnostics.

Complete technical details remain available through Build output.

### Build Cancellation

Cancellation asks the adapter to terminate the complete build process tree within a bounded interval.

Failure to terminate must be reported visibly.

Closing a project cancels or terminates its active build before releasing project build state.

Callbacks from a closed project must not update a subsequently opened project.

### Build Failure

Build failure does not close the project or prevent editing.

The latest applicable failure replaces prior build diagnostics and preserves complete output.

Old successful output may remain on disk, but it is not silently treated as current.

### Build Automatically Preference

**Build Automatically** is a Workspace preference.

It is private to the developer/workspace and does not modify project files.

Disabling automatic build does not implicitly cancel an already running build.

It prevents later automatic requests.

Manual Build Project remains available.

### Project Menu Build Commands

The accepted Project menu direction includes:

- Build Project;
- Rebuild Project;
- Cancel Build;
- Show Build Output;
- Build Automatically.

These operations use stable command identities.

The menu does not own build behavior.

Future command palette, status actions and keybindings invoke the same commands.

### Future Play Contract

Play is deliberately separate from the initial build implementation.

Future ordinary Play asks the build coordinator for a current successful snapshot.

If the saved project is stale, Play starts or joins the required build even when automatic building is disabled.

Play begins only after the requested saved revision succeeds.

When build-relevant working copies are dirty, the accepted interaction distinguishes:

- Save All and Play;
- Play Saved Version;
- Cancel.

Ordinary Play must not silently fall back to an older successful build.

### Build Testing

Coordinator tests should use a controllable adapter and clock.

Important behaviors include:

- build after relevant saves only;
- Save All coalescing;
- one active build;
- exactly one follow-up build after changes during execution;
- stale/obsolete result handling;
- failure handling;
- cancellation;
- preference changes;
- project-close isolation;
- successful snapshot selection.

Maven integration tests use controlled temporary projects and their wrapper.

Unit tests must not implicitly download Maven distributions or depend on a developer's local Maven repository state beyond explicitly designed test infrastructure.

## Project Packaging, Modules, and Distribution

JScene3D uses explicit module and packaging boundaries so renderer-independent functionality, optional integrations, editor tooling, examples, and application distribution do not collapse into one artifact.

### Renderer-Independent Core Boundary

The original publication boundary separates a renderer-independent core from the LWJGL desktop renderer and platform integration.

Core concepts must remain usable without requiring:

- LWJGL;
- GLFW;
- OpenGL;
- a graphics context.

Optional subsystems should preserve the same dependency discipline where their responsibilities permit it.

### Published Module Responsibility Map

Important module responsibilities include:

- `jscene3d-core`: renderer-independent scene, camera, geometry, material, texture, animation-supporting primitives, and raycasting APIs;
- `jscene3d-lwjgl`: OpenGL rendering, GLFW platform integration, controls, and renderer-owned native presentation facilities;
- `jscene3d-gui`: optional themed controls and monitors used by examples and suitable host applications;
- `jscene3d-gltf`: renderer-independent glTF 2.0 and GLB loading;
- `jscene3d-physics`: renderer-independent collision objects, colliders, filtering, queries, kinematic movement, sensors, and debug snapshots;
- `jscene3d-project`: project manifests, Assets, WorldDefinitions, EntityDefinitions, Component descriptors, migrations, catalogs, validation, and structured diagnostics;
- `jscene3d-project-import`: deterministic import inspection, preparation, generated-asset publication, provenance, and disposable cache management;
- `jscene3d-project-runtime`: transactional World composition through descriptor-backed Component factories;
- `jscene3d-project-3d`: descriptor-backed 3D Components and adapters between Entity ownership and renderer-level spatial presentation;
- `jscene3d-project-physics`: descriptor-backed authored physics Components and their integration with the renderer-independent physics module;
- `jscene3d-project-desktop`: standard native project host providing windowing, input, frame driving, rendering, and project execution;
- `jscene3d-project-export`: build-tool-independent application-directory assembly and native packaging support;
- `jscene3d-editor-api`: toolkit-independent editor extension contracts;
- `jscene3d-editor`: native JavaFX workbench and editor presentation;
- `jscene3d-wad`: generic WAD validation, provenance, bounded lump access, and layering;
- `jscene3d-wad-import`: generic WAD project-import adapter;
- `jscene3d-doom`: reusable Doom map discovery, decoding, validation, and project import;
- `jscene3d-game`: genre-independent World timing, semantic input, and game/physics coordination;
- `jscene3d-audio`: OpenAL-backed audio concepts and implementation.

Additional editor modules may own focused capabilities such as language-server or bundled Java support according to the accepted editor architecture.

Do not move functionality between modules merely to reduce the number of classes in one package. Dependency direction and responsibility should justify module boundaries.

### Java Platform Module System

Published JScene3D artifacts are genuine JPMS modules.

Packages must not be split across published artifacts.

Module exports should be deliberate.

Internal implementation packages should remain unexported unless callers genuinely require them as part of the supported API.

Published artifacts should also remain usable from the classpath where that compatibility is part of the release contract.

### Package Organization Within Modules

A Maven module is not itself sufficient organization for a substantial subsystem.

Implementation classes should be grouped into cohesive responsibility packages.

Do not accumulate unrelated types in a module's root package merely because they belong to the same artifact.

For example, a substantial subsystem may separate responsibilities such as:

- small public interface and immutable values;
- coordination;
- process execution;
- diagnostics/output translation;
- backend-specific adaptation;
- toolkit-specific presentation.

Tests should mirror the responsibility boundaries they exercise.

Do not create speculative empty packages before implementation establishes the actual responsibilities.

### Example and Verification Artifacts

Example artifacts demonstrate public engine capabilities without becoming part of the reusable engine API.

Examples are organized into focused suites such as:

- rendering and asset-loading examples;
- physics examples;
- game-runtime examples;
- audio examples;
- project/world-composition examples;
- WAD examples.

Reusable example-browser infrastructure remains separate from published caller-facing engine functionality.

Examples should exercise the same public APIs expected of external consumers rather than relying on privileged internal shortcuts.

### Game Application Separation

Playable Game Applications remain separate from the reusable engine.

Application-specific code, content, packaging configuration, and gameplay rules must not be placed into generic engine modules merely because the application currently provides the strongest use case.

Doomed Corridors and Beacon Garden are architecture consumers and validation targets rather than hidden parts of the engine.

### Canonical Application Directory

Application export first assembles one relocatable target-platform application directory.

This directory is the canonical assembled export.

It contains the runtime material required by the application, including concepts such as:

- relative launchers;
- application JARs;
- required JScene3D/runtime JARs;
- target-platform native dependencies;
- authored runtime project data;
- completed published import content.

The application-directory exporter owns the rules determining which project/runtime content belongs in that assembled application.

Build tools and native packagers must not independently reproduce those selection rules.

### Export Input Boundaries

Application export consumes concepts such as:

- authored project data;
- completed import publications;
- caller-resolved runtime artifacts;
- target output location.

Editable source inputs used only to generate published imports should not automatically become runtime application content.

Runtime dependency discovery follows authored runtime references and completed publication state rather than requiring each application build to maintain an unrelated manual copy list.

### Native Packaging

Native application bundles and installers consume the canonical assembled application directory.

They add only the platform-specific concerns required for distribution, such as:

- runtime image;
- native launcher;
- application metadata;
- icon;
- signing;
- distribution container.

They do not independently reconstruct the project's runtime contents.

The initial platform progression is centered on the verified macOS ARM64 path.

Other desktop platforms follow the same composition rule when they receive equivalent implementation and qualification.

### macOS Distribution

The macOS packaging path may wrap the canonical application directory using `jpackage` and produce a native application image and DMG.

Project-owned application presentation such as an `.icns` icon or supported DMG artwork remains packaging input rather than requiring a project-specific Java bootstrap.

Packaging details must not change the authored World or application runtime architecture.

### No Executable-JAR Application Model

A conventional executable or self-extracting JAR is not the supported Game Application export model.

JScene3D applications require reliable target-platform native-library and runtime setup before application code begins.

The exporter must not work around that requirement by constructing a Java bootstrap that extracts native libraries and restarts the JVM.

A ZIP or similar archive may distribute the already assembled application directory without changing its runtime model.

### Generic Desktop Launcher

Packaged applications use the generic `DesktopProjectLauncher` and project-host architecture.

The generated launcher supplies runtime information such as:

- engine version;
- packaged project root;
- published-content root.

It does not generate an application-specific Java class that manually reconstructs the startup World.

The ProjectManifest and runtime-extension architecture remain authoritative for application composition.

### Maven Central Publication

Caller-facing published artifacts use the `io.github.glynch` Maven coordinates and one lockstep project version.

JScene3D begins its public versioning at `0.1.0`.

During pre-1.0 development, explicitly approved breaking changes are reserved for minor releases, with migration information and prior deprecation where practical.

Version `1.0.0` begins the normal stable semantic-versioning compatibility promise.

### Lockstep Artifact Versions

Published JScene3D artifacts share one coherent release version.

An unchanged artifact may therefore be republished when another peer artifact changes.

This intentionally avoids requiring early users to reason about a compatibility matrix between independently versioned engine artifacts.

### Release Artifacts and Supply Chain

Published releases include the metadata and supporting artifacts required by Maven Central and the project's release policy, including:

- source JARs;
- Javadoc JARs;
- required POM metadata;
- checksums;
- signatures;
- supply-chain reporting where required.

Published versions are immutable.

Release tags and approval procedures should preserve a traceable relationship between repository state and published artifacts.

### Project License

JScene3D source and published artifacts use the Apache License 2.0.

Published metadata and source headers should consistently identify the project license.

Third-party assets retain their own licenses and attribution requirements rather than being silently relicensed under Apache 2.0.

Bundled examples, models, textures, audio, JDT LS distributions, and other third-party material must retain the appropriate notices and provenance required by their licenses.

## Testing, Verification, and Development Expectations

JScene3D treats verification as part of feature implementation rather than a separate cleanup activity performed after architecture and code are complete.

Tests should exercise the same meaningful interfaces used by production code wherever practical.

### Primary Verification Lifecycle

The ordinary repository verification lifecycle begins with:

`./mvnw clean verify`

A change should not be considered complete merely because the affected module compiles.

Run the verification appropriate to the subsystem that changed.

Some features additionally require:

- focused integration tests;
- native rendering tests;
- packaged-application tests;
- editor smoke tests;
- manual qualification.

### Unit Test Boundaries

Unit tests should exercise semantic module interfaces without unnecessarily starting expensive external infrastructure.

Examples include:

- World composition tests using controlled Runtime Resource and WorldModule implementations;
- build-coordinator tests using controllable build adapters;
- language orchestration tests using fake language adapters or controlled LSP endpoints;
- editor projection tests using toolkit-independent models;
- settings tests using controlled persistence adapters;
- workspace-state tests using deterministic snapshots/storage;
- physics tests without constructing a renderer.

A deep module should normally expose a test seam that allows its coordination behavior to be verified without reproducing its internal implementation in the test.

### Integration Tests

Integration tests are appropriate when the behavior being verified depends on a real integration boundary.

Examples include:

- Maven adapter behavior against a temporary Maven project;
- JDT LS integration against a controlled Java workspace;
- OpenGL rendering behavior;
- packaged resource discovery;
- glTF loading across real supported files;
- project import/publication;
- application-directory export.

Do not convert every unit test into an integration test merely because the production implementation ultimately invokes an external process or native backend.

### External Tool Downloads

Normal unit tests must not unexpectedly download external tools or distributions.

Examples include:

- Maven distributions;
- JDT LS;
- native tools;
- external model/assets.

Where a real external distribution is required for integration qualification, use a deliberate integration or packaging profile with controlled inputs.

Tests must not silently depend on arbitrary developer-machine configuration when a reproducible fixture can be used.

### Temporary Project Fixtures

Build, language-server, import, and project tests should prefer controlled temporary project fixtures.

Fixtures should establish the minimum project shape required by the behavior under test.

They should not depend on the complete JScene3D repository checkout unless the test specifically verifies reactor behavior.

### Renderer-Independent Testing

Renderer-independent modules should remain testable without creating:

- a window;
- an OpenGL context;
- a JavaFX Application;
- a complete Game Application.

Examples include:

- physics;
- project validation;
- authored definition loading;
- import inspection;
- Component descriptor validation;
- World composition where backend modules are replaced with test implementations.

If ordinary logic can only be tested by launching the complete editor, examine whether backend/toolkit concerns have leaked across an architectural boundary.

### Native Rendering Qualification

Some rendering behavior cannot be established through headless tests alone.

Native qualification may be required for:

- OpenGL context behavior;
- framebuffer handling;
- OpenGLFX integration;
- renderer cleanup;
- high-DPI surface sizing;
- shader compilation;
- native resource lifecycle;
- packaged native dependencies.

The verified macOS ARM64 path is the primary initial qualification environment.

Do not claim equivalent Windows or Linux support merely because platform-neutral unit tests pass.

### Editor Verification

Editor features should separate semantic/headless verification from native JavaFX interaction verification.

Where possible, test:

- selection;
- projections;
- command state;
- workbench placement;
- diagnostic aggregation;
- settings resolution;
- build coordination;
- language orchestration;

through toolkit-independent interfaces.

Use native JavaFX tests and manual editor qualification for behavior that genuinely depends on:

- focus;
- keyboard events;
- layout;
- scaling;
- accessibility;
- WebView;
- OpenGLFX;
- native window lifecycle.

### Visual Qualification

Visual changes should be evaluated as a composed product rather than only as isolated CSS rules.

Relevant editor states include:

- empty editor;
- project loading;
- opened project;
- warning state;
- failure state;
- source editing;
- scene preview.

Qualification should consider:

- focus visibility;
- mouse behavior;
- text truncation;
- high-DPI scaling;
- viewport resizing;
- split-pane behavior;
- long project/resource names;
- empty states;
- loading states;
- error presentation.

Reference screenshots may be useful for review, but screenshots do not replace behavioral tests.

### Accessibility

Editor implementation should preserve keyboard accessibility and visible focus.

Controls must not rely on color alone to communicate:

- focus;
- selection;
- severity;
- generated/read-only state.

Known accessibility limitations should be recorded explicitly rather than hidden through styling.

Platform-specific accessibility behavior may require native qualification.

### World Architecture Verification

World-composition tests should cover architectural invariants such as:

- complete graph allocation before activation;
- descriptor validation;
- exact Component factory registration;
- capability dependency validation;
- reference binding;
- endpoint binding;
- rollback;
- lifecycle order;
- scheduling order;
- enable/disable propagation;
- destruction;
- Runtime Resource lease lifetime;
- WorldModule lookup;
- structural commit boundaries.

Failure tests are particularly important because transactional composition is a core architectural guarantee.

### Physics Verification

Physics remains independently testable through its own API.

Physics tests should not require constructing renderer objects.

Entity/physics integration is tested separately through World composition and Component/module interfaces.

Visual physics examples may render debug snapshots without making rendering part of the physics module.

### Build Verification

Build coordinator tests should verify behaviors such as:

- automatic requests only after relevant saves;
- Save All coalescing;
- one active build;
- one follow-up request after saves during a build;
- saved-revision tracking;
- stale result rejection;
- failed result handling;
- cancellation;
- automatic-build preference changes;
- project-close isolation;
- successful snapshot selection.

Maven integration tests should verify actual adapter behavior separately.

### Language Server Verification

Language tests should cover concepts such as:

- initialization;
- document synchronization;
- completion;
- diagnostics;
- cancellation;
- stale document versions;
- shutdown;
- malformed or unexpected protocol behavior;
- process termination;
- initialization timeout.

Normal unit tests should not start the bundled real JDT LS distribution.

Real JDT LS qualification belongs in focused integration/packaging verification.

### Packaging Verification

Application export and native packaging should be tested from their public build boundaries.

Verify that packaged execution does not accidentally depend on:

- source-checkout paths;
- developer-local native-library locations;
- undeclared Maven reactor state;
- writable packaged resources.

Application-directory export should be verified independently from each native wrapper format.

### External Consumer Verification

Published APIs should be tested from the perspective of an external consumer where practical.

A repository-internal module may accidentally compile because it can see implementation details unavailable to real users.

Consumer verification helps detect:

- missing exports;
- JPMS errors;
- accidental transitive assumptions;
- missing runtime dependencies;
- packaging mistakes.

### Small Behavioral Commits

Implementation should proceed through behaviorally coherent commits.

Each commit should:

- have one understandable purpose;
- include appropriate tests;
- leave the repository in a valid state;
- avoid mixing unrelated refactoring with feature behavior where practical.

Large features should be delivered as independently verifiable vertical or architectural slices.

### Avoid Speculative Refactoring

Do not perform broad cleanup merely because code was encountered during feature work.

Refactor when it:

- enables the required feature;
- protects an architectural boundary;
- removes demonstrated duplication;
- reduces an established maintenance problem.

Do not create abstractions for hypothetical future use without a concrete requirement.

### Static Analysis and Code Quality

Static-analysis findings should be addressed without weakening architecture or readability merely to silence a rule.

Prefer correcting the underlying issue.

Where a rule genuinely conflicts with a deliberate design, document or configure the exception appropriately rather than distorting the implementation.

Tests, compiler warnings, static analysis, and native qualification provide complementary evidence. None individually proves overall correctness.

### Completion Reporting

When reporting implementation completion, state what was actually verified.

Do not claim:

- tests passed when they were not run;
- a file was modified without verifying the resulting state;
- native behavior was validated using only headless tests;
- planned functionality is implemented because its architecture document exists.

A coding agent should distinguish clearly between:

- implemented;
- compiled;
- unit tested;
- integration tested;
- manually/native qualified.

## Current Implementation and Planned Work Status

JScene3D contains a mixture of mature engine functionality, recently implemented editor infrastructure, active implementation work, and accepted future design.

A coding agent must distinguish these states before modifying the repository.

### Established Engine Foundation

The renderer-independent graphics foundation and LWJGL rendering path are established parts of the project.

Existing capabilities include substantial support for:

- scene graphs;
- geometry;
- materials;
- textures;
- cameras;
- lighting;
- shadows;
- fog;
- transparency;
- instancing;
- animation;
- glTF;
- custom shaders;
- raycasting;
- HDR environment lighting;
- tone mapping.

Do not treat these as merely planned because older ADRs describe their original introduction.

### Established Resource and Transform Rules

The following are durable implemented architectural directions:

- controlled public mutation;
- read-only JOML exposure;
- automatic transform maintenance;
- automatic camera projection maintenance;
- application-owned resource descriptions;
- renderer-owned GPU realizations;
- terminal idempotent resource closure;
- renderer ownership of OpenGL state;
- radians for public angles;
- linear-sRGB working color space.

New code should preserve these rules unless an explicit architectural revision is being made.

### Entity-Component Architecture Status

The Entity/Component/World architecture is an accepted implementation baseline rather than speculative brainstorming.

Implemented boundaries described by the architecture include substantial infrastructure around:

- immutable authored definitions;
- descriptor catalogs;
- validation;
- Component factory registration;
- `WorldComposer`;
- inactive World publication;
- explicit activation;
- reference binding;
- endpoint binding;
- lifecycle;
- scheduling;
- signals/actions;
- safe enable/disable/destroy mutation;
- WorldModule lookup;
- Runtime Resource leases;
- initial 3D adapter Components.

However, the same architecture document also deliberately describes later slices.

Do not assume every capability discussed there already exists.

Examples of later/deferred areas include:

- full spawning/preparation workflow;
- broader physics adapters;
- audio/input adapters;
- dynamic rigid bodies;
- complete 2D/UI catalog;
- arbitrary live editing;
- runtime persistence;
- networking;
- scripting.

Inspect current source for the exact implementation boundary before extending it.

### Physics Status

Renderer-independent physics already contains meaningful reusable capability.

The long-term physics design extends that foundation.

Do not assume that every future rigid-body feature described in direction documents is currently implemented.

Static/kinematic collision, queries, sensors, movement, and related established capabilities should be distinguished from later dynamic rigid-body/contact-solving work.

### Doom Migration Status

Generic WAD and Doom map infrastructure already exists.

The accepted Entity/World architecture defines how Doomed Corridors should progressively migrate onto canonical generated assets, EntityDefinitions, Components, physics, and World composition.

The migration plan is directional.

Do not assume the complete Freedoom campaign or every described gameplay system has already been migrated to the new architecture.

### Editor Visual Redesign Status

The editor visual redesign is substantially implemented.

Current editor screenshots confirm a workbench considerably beyond the original redesign mockups.

Implemented/current visible areas include:

- near-black/indigo visual system;
- Activity Bar;
- primary navigation;
- Workspace Explorer;
- Hierarchy-related presentation;
- Extensions view;
- central tabbed editor area;
- Monaco source editor;
- Inspector;
- lower Project/Diagnostics/Build surfaces;
- status bar;
- real scene preview;
- production splash.

The visual-design documents remain important for semantic and styling constraints even where their implementation plans describe work that has since been completed.

### Editor Working Copy Status

The resource working-copy architecture is an accepted editor foundation and should be treated as the canonical direction for editable resources.

Do not reintroduce resource-specific dirty/save state into unrelated controllers merely because a new editor feature needs editable content.

Inspect the current implementation to determine which resource types have already migrated to working copies.

### Editor Command Infrastructure Status

Stable commands and declarative placement already provide foundation for current editor interaction.

Some broader command surfaces remain staged.

The command palette, complete keybinding customization, and full command catalogue should not be assumed complete solely because their architecture is accepted.

New command behavior should nevertheless use the existing stable command model rather than introducing temporary JavaFX-only handlers that will later need replacement.

### Editor Settings Status

The broader unified settings experience is accepted/planned.

Some existing appearance and project-setting functionality predates that unified design.

Do not assume that:

- all User settings;
- Workspace settings;
- generated settings search;
- direct JSON editing;
- complete settings migration;

are already implemented.

When implementing settings work, inspect both current settings infrastructure and the newer accepted design.

### Workspace State Persistence Status

The classification and architecture for User settings, Workspace preferences, Workspace state, Session state, and Recovery data are accepted.

The broader workspace-state persistence implementation is deferred.

Do not mistake the detailed restoration design for an already implemented persistence subsystem.

### Workspace Explorer Settings Status

Workspace Explorer currently has established filesystem behavior and default safety/exclusion rules.

Settings-backed Project inclusion/exclusion behavior is planned.

Do not assume that the documented glob configuration is already active without checking current implementation.

### Java Language Server Status

Java language-server architecture is accepted and is an active high-priority editor direction.

The design deliberately stages implementation.

The current repository may contain portions of:

- LSP lifecycle;
- JDT LS process management;
- Java project support;
- diagnostics;
- Monaco bridge infrastructure;
- completion;
- status integration.

The exact current slice must be determined from source and tests.

Do not infer implementation merely from the design document's complete roadmap.

In particular, later capabilities such as:

- hover;
- signature help;
- references;
- implementations;
- rename;
- formatting;
- code actions;
- semantic tokens;
- inlay hints;

may remain unimplemented even though their intended architecture is documented.

### Project Build Status

Project build direction is accepted and implementation is in progress.

The architecture is therefore a strong constraint for current build work, but source inspection remains necessary to determine which slices are complete.

Do not implement future Play behavior as part of build work unless explicitly requested.

The build design intentionally establishes a clean future Play seam first.

### Workspace Restoration Status

Detailed restoration behavior is accepted direction, not proof of implementation.

Features such as:

- restored open editors;
- divider positions;
- active Views;
- Explorer expansion;
- cursor positions;
- recovery data;

must be checked individually before being described as current functionality.

### Extension Marketplace Status

The editor has bundled extension infrastructure and an Extensions view.

This does not mean JScene3D currently has a complete third-party extension marketplace.

Package installation, registry discovery, trust, signatures, permissions, dependency resolution, update, disable, and removal require additional architecture.

Do not add placeholder controls implying unsupported functionality.

### Platform Support Status

macOS ARM64 is the primary verified platform.

Windows and Linux should not be described as equivalently supported merely because code is intended to be portable.

Platform promotion requires appropriate rendering, cleanup, scaling, focus, packaging, and native qualification.

### README Status Caveat

The README is valuable for:

- broad feature inventory;
- usage examples;
- build commands;
- module overview;
- public API examples.

It may lag newer editor architecture and implementation.

When determining whether an editor capability exists, prefer current source, tests, recent implementation documents, and current editor behavior over an older README statement.

### Status Discipline for New Work

Before implementing a requested feature, explicitly determine which of these applies:

- extending established implementation;
- completing an in-progress slice;
- implementing accepted but deferred design;
- revisiting an architectural invariant.

Do not accidentally implement a deferred feature while solving a smaller task.

Do not redesign an established subsystem merely because a planning document contains broader future possibilities.

## Coding-Agent Operating Guide and Source Document Map

This section defines how a coding agent should use this context when performing repository work.

The objective is to reduce unnecessary repository exploration without replacing feature-specific investigation.

### Required Workflow Before Editing

Before changing code:

1. Read this project-context document.
2. Identify the subsystem affected by the request.
3. Locate the relevant ADRs, design documents, and implementation plans.
4. Read the feature-specific documents before proposing architecture.
5. Inspect the current source implementation.
6. Inspect existing tests around the affected seam.
7. Determine what is implemented versus planned.
8. Identify the existing architectural seam to extend.
9. Implement the smallest behaviorally coherent change.
10. Add or update focused tests.
11. Run the relevant verification.
12. Inspect the resulting working tree.
13. Report exactly what changed and what verification actually ran.

Do not begin by recursively reading large unrelated portions of the repository when this context and feature documents already identify the relevant subsystem.

### Architecture Before Convenience

When implementation appears easier by bypassing an established abstraction, assume the abstraction is intentional until the relevant design proves otherwise.

Examples of boundaries that should not be bypassed casually include:

- renderer-independent core versus LWJGL;
- resource descriptions versus renderer GPU realizations;
- authored definitions versus runtime Entities;
- Entity/World versus Scene/Object3D;
- WorldModule interfaces versus backend implementations;
- working copies versus direct resource writes;
- language-neutral editor APIs versus LSP4J;
- command model versus JavaFX event handlers;
- settings module versus direct JSON parsing;
- build coordinator versus direct Maven invocation;
- generated imports versus source assets.

### Feature Scope Discipline

Implement the requested feature, not every adjacent future feature described in the design.

Architecture documents often describe later slices to ensure the current seam can support them.

That does not make those later slices part of the current task.

Avoid speculative implementation of:

- future extension marketplace behavior;
- generalized hot reload;
- dynamic rigid bodies;
- definition inheritance;
- scripting;
- multiplayer;
- complete workspace recovery;
- future language features;
- Play/Debug infrastructure;

unless the task explicitly requires them.

### Existing Code Is Evidence

Documentation may lag implementation.

If current source has clearly progressed beyond an older implementation plan, do not rewrite working newer architecture back toward the older document.

Conversely, existing code is not automatically the desired architecture when a newer accepted design explicitly records its replacement.

Use chronology, document status, tests, and current implementation together.

### Avoid Inventing Missing Contracts

If a design deliberately leaves an exact interface open until implementation, do not pretend the document already specifies that interface.

Inspect existing code and derive the smallest contract that satisfies:

- accepted semantics;
- dependency direction;
- testability;
- current feature requirements.

Do not freeze speculative abstractions simply to make the implementation appear complete.

### Preserve Terminology

Use canonical project terminology consistently.

Important examples include:

- `Entity`, not Node;
- `EntityDefinition`, not Prefab;
- `WorldDefinition`, not SceneDefinition;
- `World`, not renderer Scene;
- `Runtime Resource` where the loaded shareable resource distinction matters;
- Component descriptors rather than implementation-class metadata;
- Preview when the editor is not executing the Game Application.

Do not import terminology from Unity, Godot, VS Code, or Three.js when JScene3D has deliberately chosen a different concept.
