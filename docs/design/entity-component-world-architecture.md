# Entity-component world architecture

Status: accepted baseline for the first Beacon Garden implementation

This document defines JScene3D's project-facing authoring and runtime model. It
records the decisions made during the September 2026 architecture review.
Names explicitly described as provisional remain open to refinement; the
semantics are the implementation baseline.

The design is deliberately no larger than the first useful implementation. It
must support Beacon Garden now and remain suitable for Doomed Corridors and
other 2D, 3D, and UI-based games. Features that are not required to establish
that foundation are listed as deferred rather than designed speculatively.

## Design references and constraints

Godot and Unity are references, not specifications. Godot demonstrates the
value of reusable hierarchical scenes, explicit physics objects with multiple
shapes, and signals. Unity demonstrates the value of composing an object from
independent components. JScene3D combines the useful parts without preserving
either product's public model one-for-one.

The current private codebase and provisional project formats are not
compatibility constraints. This is the point at which to choose the coherent
model and replace code that assumes a different one.

The architecture must make these cases natural:

- author a unique entity directly in a world;
- place the same reusable definition many times with independent state;
- spawn a prepared definition during gameplay;
- attach a weapon to an exported skeleton attachment point;
- import a glTF scene without creating a second public model hierarchy;
- author rendering and collision independently;
- give one collision object several independently transformed shapes;
- inspect live runtime-created entities without saving them as authored
  placements;
- represent a bullet as an entity while allowing a particle system to manage
  thousands of non-entity particle records internally;
- represent Doomed Corridors' player, projectiles, pickups, doors, enemies,
  level collision, and imported presentation using the same concepts.

## Architectural summary

JScene3D uses a hierarchical entity-component object architecture. It is not a
data-oriented ECS contract.

- `Entity` supplies runtime identity, ownership, enabled state, and component
  composition.
- Components supply spatial, rendering, physics, audio, animation, and game
  behavior capabilities.
- The entity hierarchy defines ownership and lifecycle. Compatible spatial
  relationships normally follow it, but spatial attachment is a separate
  relationship when required.
- `EntityDefinition` is an immutable, reusable, single-root authored asset.
- `WorldDefinition` is an authored asset containing world settings and any
  number of root entity records or definition placements.
- `World` is the runtime composition root. It owns root entities and runtime
  modules such as physics, audio, rendering, scheduling, and resources. It is
  not an entity and is not a JVM-global singleton.
- Definitions are inert. Placing or spawning a definition creates live
  entities; importing or loading a definition does not.
- Static placement and dynamic spawning share one validation, composition,
  resource, and lifecycle implementation.
- The runtime may compile the model into dense or backend-specific structures,
  but those structures are not part of the authoring interface.

The canonical public terms are `Entity`, `Component`, `EntityDefinition`,
`WorldDefinition`, `World`, `Asset`, and `Runtime Resource`. Avoid using `Node`,
`SceneDefinition`, `Prefab`, `Controller`, or `ModelInstance3d` as synonyms.
The existing renderer-level `Scene` and `Object3D` remain lower-level graphics
concepts rather than the game-authoring model.

## Authored asset model

### Asset

`Asset` is the umbrella term for independently stored and addressable project
content. Asset kinds initially include:

- `ProjectManifest`;
- `WorldDefinition`;
- `EntityDefinition`;
- meshes, materials, textures, animation clips, audio, and collision-shape
  assets;
- source assets and import recipes where the existing import architecture
  requires them.

All asset kinds use the same typed persistent reference model. Code may express
this as `AssetRef<T>`; the Java name is less important than the invariant that
the expected asset kind is known and validated.

An asset is not the same thing as its loaded runtime value. `Runtime Resource`
is the loaded, shareable representation used by a `World` or its modules.

### ProjectManifest

The project manifest is descriptive and operational project metadata. It is
not instantiated as a runtime entity. Initially it contains:

- project identity and schema version;
- the startup `WorldDefinition` reference;
- enabled engine modules and game-module entry points;
- asset roots and build/import configuration;
- project-wide display and runtime defaults;
- descriptive, legal, and compatibility metadata already required by the
  project loader.

World and entity content are referenced assets rather than embedded in the
manifest. Loading untrusted manifest metadata remains separate from executing
game code or importers.

### WorldDefinition

A `WorldDefinition` describes one runtime `World`. It contains world settings
and zero or more root entries. A root entry is either:

- an entity authored locally in that world; or
- a placement of an `EntityDefinition`.

Multiple root entities are valid. The editor must not invent a visible root
entity merely to hold them. If a game needs a global behavior object, it authors
an ordinary root entity with the relevant components.

### EntityDefinition

An `EntityDefinition` is an immutable, reusable, single-root entity hierarchy.
It may contain:

- entities authored locally in the definition;
- placements of other `EntityDefinition` assets;
- component configurations;
- internal signal/action connections;
- a deliberately exported public contract.

There is no separate prefab format. Levels and reusable objects have different
envelopes because a world has world settings and multiple roots, but all entity
trees use the same entity/component representation.

Structural definition inclusion must be acyclic. Behavioral asset references
may be cyclic because they do not expand the hierarchy: a gun may reference a
bullet definition, and an enemy may reference its own definition as something
it can spawn.

### Local entity or reusable definition

A unique light, camera, trigger, or piece of level behavior can be authored
directly in a `WorldDefinition`. Requiring a separate asset for every entity
would create noise. When a local subtree becomes reusable, the editor may offer
`Extract as Entity Definition`.

The same rule applies inside an `EntityDefinition`: unique children may be
inline, while reusable children are referenced placements.

## Entity model

There is one runtime `Entity` type. There are no `Entity3d`, `Entity2d`, or UI
runtime subclasses.

An entity intrinsically owns only:

- runtime identity;
- an ownership parent or `World` root membership;
- ordered owned children;
- local and effective enabled/lifecycle state;
- component identities and ownership;
- association with exactly one `World`.

An entity does not contain rendering, physics, audio, animation, or update
behavior. It does not recursively update or render its children. Runtime
modules compile and schedule the components that participate in those
activities.

An editor may offer `Entity3d`, `Entity2d`, and UI creation presets. These are
conveniences that create an ordinary entity and add `Transform3d`,
`Transform2d`, or `UiLayout`; they are not serialized subtypes.

### Spatial domain

Primary spatial authority is declared by the component descriptor's spatial
domain and exposed to sibling components through capability:

- `Transform3d` provides `Spatial3d`;
- `Transform2d` provides `Spatial2d`;
- `UiLayout` provides the UI layout capability;
- an entity with none of these is non-spatial.

An entity may have at most one primary spatial-domain component. Crossing
between domains uses an owned child entity and an explicit bridge or
projection. A 3D renderer or collider requires `Spatial3d`; a 2D renderer or
collider requires `Spatial2d`; a UI renderer requires the UI layout capability.
Renderers, colliders, and other spatial consumers require the capability but do
not themselves claim the primary spatial domain.

Ownership may contain mixed domains. Spatial inheritance applies only between
compatible domains.

### Entity granularity

Use a component when a capability shares the entity's identity, transform, and
lifetime. Use an owned child entity when something needs independent identity,
transform, lifecycle, components, attachment, or reuse.

A runtime-created bullet is normally an entity. It does not appear as a placed
entity in the authored level, but it does appear in the live hierarchy for its
lifetime. The bullet definition remains visible and editable as a project
asset.

Particles within a `ParticleSystem` need not each be entities. A particular
particle may be modeled as an entity when independent collision, behavior,
selection, or identity justifies that cost. The public model does not require
high-volume internal simulation records to be entities.

## Component model

A persistent component record contains:

- a stable local `ComponentId`;
- a stable namespaced component-type ID;
- a component configuration-schema version;
- typed authored property values.

The registered `ComponentTypeDescriptor` is authoritative for:

- type identity and schema version;
- property schemas, defaults, constraints, and editor metadata;
- provided and required capabilities;
- allowed multiplicity and conflicts;
- signals and actions;
- supported spatial domain;
- lifecycle and scheduling participation;
- the runtime construction seam.

Annotations or generated metadata may help define descriptors, but they must
not create a second semantic model. Serialized data never contains a Java
implementation class name.

Runtime components are ordinary Java objects and may contain behavior. There
is no privileged controller or script slot. A game may add several behavioral
components to the same entity when their descriptors allow it.

Component descriptors are safe extension metadata. They are declared in the
extension descriptor's `components` array and can therefore be discovered and
validated without loading executable extension code. For example:

```json
{
  "id": "example.game/mover",
  "typeVersion": 1,
  "displayName": "Mover",
  "properties": [
    {
      "id": "speed",
      "valueKind": "number",
      "required": true,
      "displayName": "Speed"
    }
  ],
  "signals": [
    { "id": "moved", "displayName": "Moved" }
  ],
  "actions": [
    { "id": "stop", "displayName": "Stop" }
  ],
  "requiredCapabilities": ["example.game/transform"],
  "multiplicity": "single",
  "conflicts": ["example.game/teleporter"],
  "spatialDomain": "none",
  "lifecycle": ["created", "destroyed"],
  "updatePhases": ["after-physics"]
}
```

`RegisteredTypeCatalog` indexes these descriptors by the exact pair of
`ComponentTypeId` and configuration-schema version. Catalog-aware entity and
world loading checks authored properties, multiplicity, conflicts, sibling
capability providers, local contract targets, and local signal/action payloads.
The structural loading overloads remain useful for an editor that must preserve
and display definitions whose extensions are unavailable; play and export use
catalog-aware loading and reject unresolved component types.

Component dependencies use declared capabilities or explicit stable component
identity. General `getComponent(Class<?>)` lookup and nearest-ancestor searches
are not the dependency model. A missing, conflicting, or ambiguous required
dependency is a validation failure before activation.

Runtime behavior may also query a capability on one exact live entity, for
example when a collision identifies the entity whose player-resource capability
should receive a pickup. The component descriptor remains the authority for
which component provides that capability; the query never searches ancestors,
descendants, or the wider world. Authored relationships which already know a
specific participant continue to use stable entity or component identity.

Required external contract values must be supplied. Optional values are valid
only when the descriptor explicitly defines meaningful behavior for absence;
the runtime never searches the hierarchy or world for a plausible replacement.

### Construction and dependency injection

Trusted engine and game modules register descriptors and runtime factories
before a `World` is composed. Construction passes through one replaceable seam,
provisionally called `ComponentFactory`:

1. the runtime allocates the complete inactive entity graph;
2. component configurations are validated and runtime components are created;
3. stable entity, component, asset, and public-contract references are bound;
4. declared capability dependencies are checked;
5. lifecycle creation begins only after the graph is complete.

A future dependency-injection framework can provide an adapter at this
construction seam. It may inject world module interfaces and game-owned
dependencies, but it must not become part of serialized data or the public
engine model. JScene3D does not initially depend on Spring or any other DI
container.

Executable code is registered separately by the trusted runtime extension:

```java
private static final ComponentType MOVER =
        ComponentType.of("example.game/mover", 1);

@Override
public void register(ComponentFactoryRegistry registry) {
    registry.register(MOVER, context ->
            new Mover(context.properties()));
}
```

Registration must match an exact descriptor owned by that extension. Duplicate,
foreign, undeclared, and late registrations fail immediately. A missing factory
for a validated component is a structured composition failure. The
`ComponentFactoryContext` exposes the immutable authored definition, its exact
descriptor, effective property values, and construction-time runtime-resource
resolution; it does not expose mutable composer internals. Descriptor discovery
and validation therefore do not depend on executable class loading, and
serialized files never name the factory or implementation class.

Components receive their owning `World` or a bounded context explicitly. They
do not discover a static global world, create backend implementations, or use
an implicit service locator.

The first implemented composition boundary is `WorldComposer.compose(...)`.
It loads and validates the complete authored definition graph, allocates every
live entity before invoking a component factory, applies descriptor defaults
and instance-contract arguments, and returns either a complete inactive
`World` or ordered structured diagnostics. A failed composition publishes no
world and releases already-created `AutoCloseable` component values in reverse
construction order. The caller explicitly calls `World.activate()` after
composition. A successful world owns its component values and the
runtime-resource leases acquired while constructing them. It releases component
values before their resource leases. The caller retains ownership of the
`RuntimeResourceProvider`; only its returned leases transfer to the world.

`RuntimeResourceProvider` is the host seam for acquiring shared immutable
runtime values. Each acquisition returns an independent `RuntimeResourceLease`;
the provider may retain one underlying value across leases and worlds. Within a
world, the first request for a `ResourceReference` acquires one lease and later
requests receive the identical value after type validation. The world attributes
use to owning entities, releases a lease when its final owning entity is
destroyed, and otherwise releases leases in reverse acquisition order during
world cleanup. Components receive values rather than lease handles and therefore
cannot accidentally release resources still used elsewhere.

Composition constructs every component value before binding authored entity or
component targets. A component type with `entity_target` or `component_target`
properties returns a value implementing `ComponentReferenceBinder`. Its single
binding callback receives a short-lived `ComponentReferenceResolver`; resolving
by property identity produces a direct live `Entity` or component reference.
Every binding succeeds before the inactive world is published. Binding failure
therefore uses the same complete reverse-order rollback as factory failure and
no lifecycle callback has yet run.

After reference binding, each component type declaring signals or actions binds
them through `ComponentEndpointBinder`. The short-lived `ComponentEndpoints`
context supplies persistent signal handles and accepts synchronous action
callbacks. Descriptor metadata is authoritative: every declared endpoint must
be implemented exactly once, its direction and payload presence must match, and
arbitrary implementation failure rolls back the whole composition. The composer
then resolves internal connections and definition-contract exports to the exact
live component endpoints in each definition-instance scope.

The implemented boundary includes initial lifecycle activation,
descriptor-compiled fixed and frame schedules, synchronous runtime signal
dispatch, safe enablement and destruction commits, exact host-supplied
world-module lookup, and world-owned runtime-resource leases. Scheduled
callbacks and signal dispatch are enabled only after successful world activation
and stop before world cleanup. The first 3D adapter slice adds descriptor-backed
`Transform3d`, perspective-camera, directional-light, and mesh-renderer
components. It maintains automatic compatible direct-parent world transforms,
selects one explicitly authored primary camera, resolves shared mesh and material
resources through world-owned leases, and submits the resulting presentation
through `Spatial3dWorldModule`. Its internal `Scene`, `Object3D`, camera, light,
and mesh objects remain adapter details. Spawning, physics, audio, and input
adapters remain subsequent slices on top of the same composed entity graph.

Project import artifacts distinguish generated `EntityDefinition` documents
from typed runtime-resource documents and opaque payloads. Generated definitions
use the same canonical serializer as authored definitions and can be written
directly into importer-owned staging output without exposing cache paths.

## Definition placement and composition

Loading or importing an `EntityDefinition` does not create live entities. The
editor can open a definition as its own editable document, or as a read-only
generated document for imported content. It enters another authored hierarchy
only when the developer explicitly places it.

When a definition is placed or spawned, its root becomes the instance entity
directly. The runtime does not add an artificial wrapper:

```text
Bullet
├── Mesh
└── Collision
```

The placement supplies its stable instance identity, display name, enabled
state, initial root transform where applicable, and exported parameter values.
A spatial definition must have the appropriate primary transform on its root
to receive spatial placement.

Nested definitions preserve logical instance identity, public contract, state,
and lifecycle even if the runtime flattens their representation internally.
Definition-instance scope is runtime bookkeeping, not a second public object in
the live hierarchy.

Each live entity receives a fresh world-local `RuntimeEntityId`. Its authored
asset and entity identities remain available separately for inspection and
diagnostics. Repeated placements therefore retain the same definition-local
IDs internally while owning distinct live entities and component values. A
placed root reports the containing asset and placement ID publicly; descendants
report the reusable definition asset and their definition-local IDs.

The live entity also reports whether it came from a local entity declaration,
an authored definition placement, or a runtime spawn. Placement and spawn roots
report the exact definition asset they instantiate. Children declared inside
that definition retain local-entity provenance beneath the instance root. This
metadata makes the existing read-only World hierarchy the live inspection
model; the runtime does not construct a parallel diagnostic entity graph.

### No authored definition inheritance

The initial model has no base definitions, derived definitions, prefab
variants, or multi-level override chains. The editor may duplicate a definition
as a starting-point convenience. The duplicate is independent from that point
on.

A duplicated asset receives a new `AssetId`. Its local entity and component IDs
may remain unchanged because they are scoped by the new asset identity, which
also preserves its internal references. Heavy referenced assets remain shared
until deliberately duplicated.

### Definition encapsulation

A containing world or definition may configure a placed definition only
through its public contract and standard placement state. It cannot edit the
placed definition's private internal entities or attach arbitrary components to
them.

The public contract may export:

- typed parameters;
- typed signals;
- typed actions;
- declared capabilities;
- named spatial attachment points;
- explicitly supported resource bindings such as material slots.

An exported attachment point is a stable public name backed by a private
internal spatial target. For example, a character may export `right-hand`
without exposing its skeleton structure. A parent can attach a weapon to that
point and remain insulated from internal reorganization.

Additional behavior, physics, audio, or child structure is added by creating
an authored wrapper definition whose local root or children compose the placed
definition. This is ordinary composition, not inheritance or arbitrary
refinement.

## Identity and references

Identity is never derived from an editable display name, hierarchy position,
or filesystem location.

Every stored asset has a stable opaque `AssetId`. Moving or renaming its file
does not change that identity. The project asset catalog maps IDs to current
locations. A serialized reference contains the ID and may include a
human-readable path/name hint used only for diagnostics.

`EntityId` values are stable and unique within their containing authored asset.
`ComponentId` values are stable and unique within their owning entity entry.
Rename, reorder, and reparent operations preserve them. A persistent target is
conceptually:

```text
AssetId + local EntityId + optional ComponentId + optional PropertyId
```

A live target additionally includes definition-instance scope. Public Java
types should prevent accidental interchange of these identity kinds.

Component properties persist entity and component targets as distinct portable
value kinds. Their canonical JSON representation is:

```json
{
  "body": {
    "$target": {
      "entityId": "a stable entity UUID",
      "componentId": "a stable component UUID"
    }
  }
}
```

Omitting `componentId` produces an `entity_target`; including it produces a
`component_target`. The target is interpreted in the authored scope containing
the value, not in the eventual hierarchy position. When a target value passes
through a placed definition's public parameter, composition preserves its
original containing instance scope. This permits an explicit public dependency
without making a repeated definition bind back into the wrong placement.

An outer asset may target a placement entity itself, but it cannot name a
private component inside that placement. Such a dependency must be deliberately
exported by the reusable definition's public contract. Target validation and
runtime lookup both enforce that seam.

Animation tracks, component dependencies, collision-shape membership, signal
connections, and action targets use these stable identities. They never use
paths such as `../body` or "nearest ancestor of type X".

A raw filesystem copy that duplicates an `AssetId` is an asset-catalog error.
The editor must ask the user to resolve the duplicate rather than silently
selecting one file.

## Ownership, attachment, and reparenting

The ownership hierarchy controls serialization and lifetime. Destroying an
owner destroys its owned descendants unless an explicit transfer has already
moved one elsewhere.

For the common case, a compatible child spatial component uses its owning
parent as its spatial parent. An explicit stable attachment target is used when
ownership and spatial parentage differ, including skeleton joints, exported
attachment points, world-space attachments, and cross-domain bridges.

Ownership transfer and spatial attachment are distinct runtime operations,
although the editor may offer a combined command for the common case.

Any operation that changes the spatial parent must choose a transform policy:

- `KEEP_WORLD` recalculates local state so the entity remains visually fixed;
- `KEEP_LOCAL` preserves local state and moves it into the new parent's space.

Runtime interfaces require the policy explicitly. Ordinary editor hierarchy
drag-and-drop defaults to `KEEP_WORLD`; explicit attach-to-mount operations
default to `KEEP_LOCAL`.

Each spatial entity has one effective transform authority. Static objects and
sensors consume authored/runtime transform state; kinematic movement uses the
physics movement interface; rigid bodies produce authoritative simulation
state; presentation may interpolate that state. Conflicting transform writers
are rejected by descriptor validation.

## World and runtime modules

`World` is the runtime composition root and lifetime owner for:

- root entities;
- entity/component composition and mutation;
- scheduling and clocks;
- physics;
- rendering coordination;
- audio;
- input snapshots;
- runtime-resource loading and sharing;
- diagnostics and external-event ingestion.

It is not an entity. It is also not a JVM-global singleton: a game world,
editor preview, thumbnail renderer, and test world may coexist.

The owning world is explicitly available to runtime components through their
construction or lifecycle context. A host binds each world-scoped facility to
an exact stable Java interface extending `WorldModule`; concrete adapter classes
are never lookup identities. `world.findModule(interfaceType)` represents an
optional dependency and `world.requireModule(interfaceType)` represents a
required dependency. A required lookup made by a component factory fails as a
structured composition diagnostic at that component's authored location when
the host omitted the binding. Lookup neither searches parent interfaces nor
falls back to static or process-global state.

Passing a binding into composition does not itself transfer ownership. When
composition succeeds, the published world owns every bound adapter and closes
them in reverse binding order after releasing all entity component values. When
composition fails, no world is published and the host retains ownership of all
supplied adapters. Module lookup is unavailable after world closure. Separate
worlds have separate bindings, allowing a game world, editor preview, thumbnail
renderer, and tests to coexist without shared mutable world state.

Stable module interfaces allow useful commands and queries without exposing
simulation stepping, internal collections, backend replacement, or
unrestricted entity mutation.

The modules are designed as deep modules: callers do not coordinate parsing,
dependency loading, lifecycle ordering, schedule mutation, backend handles, or
rollback themselves.

## Lifecycle

The public lifecycle has three effective entity states:

- **Active**: participates in behavior, physics, audio, rendering, and signals.
- **Disabled**: remains live and retains state but does not participate.
- **Destroyed**: permanently removed at the next safe structural commit.

Disabling a parent effectively disables its descendants. Each descendant keeps
its own enabled flag, so re-enabling the parent does not enable a child that was
independently disabled.

Components opt into these semantic lifecycle points through safe descriptor
metadata. A runtime value whose descriptor declares any lifecycle event
implements `ComponentLifecycleCallbacks`. The descriptor remains authoritative:
the world invokes only declared events even when the Java value overrides other
callback methods.

1. **Created** once, after the full graph exists and references are resolved.
2. **Activated** whenever the component becomes effectively active.
3. **Deactivated** whenever it ceases to be effectively active.
4. **Destroyed** once, when permanently removed.

Creation and activation proceed owner before owned children. Deactivation and
destruction proceed children before owner. Declared component dependencies
determine ordering within an entity. Components without an ordering dependency
must not rely on incidental authored or collection order.

Activation is transactional. Failure while validating, acquiring resources,
constructing components, binding references, or registering modules rolls back
the complete instance. Constructed component values close before acquired
resource leases, and leases release in reverse acquisition order. A partially
active definition instance is never observable.

`WorldComposer.compose(...)` publishes the complete graph in an inactive state
without invoking semantic callbacks. `World.activate()` delivers creation to
every component and activation to components on initially enabled entities.
The active world accepts explicit enable, disable, and destroy commands. It
propagates effective enablement without changing independently authored child
flags and performs permanent subtree cleanup child first. A lifecycle callback
failure closes the world after completing cleanup and identifies the event,
entity, and component with `WorldLifecycleException`. Spawned-instance lifecycle
remains a later slice.

## Scheduling, time, and concurrency

JScene3D guarantees deterministic ordering, not bit-for-bit cross-platform
simulation. Exact replay or lockstep networking would require additional
numeric and physics constraints.

One logical simulation thread invokes game component behavior. Rendering,
physics, audio, loading, and other modules may use workers internally, but they
must not invoke arbitrary entity components from those workers. Worker results
are queued into the owning world and delivered at a defined phase, optionally
carrying timestamps or simulation-step numbers.

The first runtime has two component-visible cadences:

- fixed simulation updates;
- per-frame presentation updates.

A component declares its participation through its descriptor; hierarchy and
serialized component order do not select execution order.

The initial closed phase vocabulary is:

1. **Input acquisition**: the host publishes a stable input snapshot and
   accepts queued external results.
2. **Before physics**: fixed-step game behavior reads input and requests motion,
   forces, or other simulation changes.
3. **Physics**: the world physics module advances. This phase is engine-owned.
4. **After physics**: physics results are synchronized and contact/overlap
   signals are delivered; fixed-step game behavior may respond.
5. **Frame update**: presentation-only behavior runs once per rendered frame
   with the fixed-step interpolation fraction.
6. **Render preparation and submission**: transforms and render state are made
   current and submitted. This is engine-owned.

The engine may internally subdivide a phase without expanding the public
vocabulary. Extensions may participate in supported phases but may not add new
global phases. Animation and transform propagation are scheduled internally at
the points required by their declared transform authority.

Rendering may interpolate previous and current simulation states but must not
alter authoritative simulation state.

The implemented scheduler exposes `World.advanceFixed(step)` and
`World.advanceFrame(elapsed, interpolation)`. The world owns the zero-based
fixed tick and accumulated simulation time. A fixed advance runs every declared
`before-physics` callback, reserves the engine-owned physics seam, and then runs
every declared `after-physics` callback. A frame advance runs declared
`frame-update` callbacks using the completed simulation time.

Each completed component-visible phase is also a structural commit point. An
accepted mutation request still commits when later component code fails that
phase; component updates are not transactional. Fixed tick and simulation time
advance only after both fixed component phases and their commits succeed.

Any component descriptor declaring an update phase requires its runtime value
to implement `ComponentUpdateCallbacks`; implementing that Java interface does
not itself place a component in a schedule. Phase schedules are compiled once
at composition and ordered by exact component type, stable authored entity
address, and component identity rather than serialized component, sibling, or
runtime-extension registration order. Effectively disabled entities are skipped.
Update callbacks are synchronous, non-reentrant, and report implementation
failure with the precise phase, live entity, and authored component identity.

## Signals and actions

A typed signal announces that something happened. A typed action requests that
a known target do something. Neither is an untyped global event bus.

Signals are delivered synchronously and deterministically on the simulation
thread during their owning phase, using a stable connection snapshot.
Structural mutation requested by a listener is deferred. Signal payloads use
exact registered identities; the runtime does not perform implicit payload
conversion.

Connections within one authored asset address stable entity and component IDs.
Connections across a placed definition's seam use its exported signals and
actions. A signal may legitimately have no listeners; a required target or
contract reference may not be silently absent.

The first implemented endpoint seam compiles authored connections during world
composition, after all component values and stable references exist but before
the inactive world is published. Components obtain world-owned `RuntimeSignal`
handles and register action callbacks through their descriptor-backed endpoint
binder. Emission requires an active world, verifies the exact registered payload
identity, ignores disabled sources, skips disabled actions, and dispatches to a
snapshot of listeners in authored order. An unconnected signal is valid.

## Structural mutation and spawning

All structural changes are requested through the owning world. Direct list
mutation on an entity is not the public runtime interface.

The implemented live-mutation seam is `World.enable(entity)`,
`World.disable(entity)`, and `World.destroy(entity)`. Calls made while the world
is idle commit synchronously because the caller is already between phases.
Calls from update callbacks are coalesced and commit after the current
component-visible phase. Enable and disable preserve each descendant's local
flag while recalculating effective participation. Destruction becomes
effectively inactive when requested, then deactivates, destroys, closes, and
removes the complete owned subtree at commit. Retained entity references expose
stable identity and destroyed state but no longer expose children or components.

Destroyed entities are removed from live lookup, ownership traversal, update
schedules, and endpoint routes. Repeating destruction is harmless; enablement
commands against pending or destroyed entities fail. A lifecycle or cleanup
failure completes as much cleanup as possible and closes the world rather than
publishing unreliable partially active state.

Mutations commit between engine-defined phases:

- a spawned entity cannot participate in the phase that requested it;
- once committed, it may participate in a later phase of the same simulation
  step;
- a destroy request marks the entity pending destruction and effectively
  inactive immediately;
- pending entities receive no new scheduled callbacks or signals;
- physical removal and cleanup occur at the next structural commit.

Spawning is transactional. An entity may spawn an owned child directly. Code
that needs a different owner must receive an explicit scoped spawn target; it
must not gain an unrestricted global mutation escape hatch.

The asynchronous result/handle is provisionally named `SpawnOperation` because
the previously suggested `SpawnTicket` was rejected. The name remains open,
but the semantics are fixed: it reports preparation, activation, cancellation,
and structured failure without exposing a partially constructed entity.

### Preparation and loading

Spawning never performs blocking asset I/O on the simulation thread.

- Gameplay-critical definitions such as bullets, enemies, and effects are
  prepared before use.
- Spawning a prepared definition performs in-memory composition and activates
  it at a safe commit point.
- A separate asynchronous convenience operation may prepare and then spawn
  unprepared content for loading screens or non-immediate use.
- An immediate spawn request for unprepared content fails explicitly.

Preparing an `EntityDefinition` resolves and validates its transitive
structural and runtime-resource dependencies. Worlds retain shared immutable
runtime resources while any active or prepared content leases them.

## Collision architecture

Rendering and collision are independently authored. A visible mesh does not
silently become collision geometry, and a collision object need not be
visible.

A collision-object component represents a body or sensor and owns membership
of one or more shapes. Each shape has:

- a stable `ComponentId`;
- an immutable collision-shape asset reference;
- its own local transform;
- filtering and material properties where supported.

The collision object identifies its member shapes using stable entity and
component identities, not relative hierarchy paths. The editor may hide this
wiring by offering shape-child creation commands.

Physics object kinds are capabilities rather than entity subclasses. The
initial family includes static bodies, explicitly moved character/kinematic
bodies, dynamic rigid bodies when supported, and non-blocking sensors/areas.
A body may have multiple shapes regardless of its kind.

The physics module detects contacts and overlaps and emits typed signals that
identify both collision objects and precise shapes. Game behavior decides what
the event means: damage, pickup, door activation, sound, scoring, or no action.
The physics module does not infer gameplay response from rendering or entity
names.

## Runtime resources and mutable state

Heavy runtime resources are immutable and shareable. These include meshes,
materials, textures, collision shapes, skin/skeleton definitions, animation
clips, and audio payloads.

All mutable state belongs to the live instance, including transforms,
visibility, material parameters, morph weights, skeleton pose, animation
playback, physics state, behavior state, signal subscriptions, and dynamically
spawned children. One live instance must never mutate another by changing a
shared runtime resource.

An editor `Make Unique` operation creates a new authored asset; it does not make
a shared runtime resource conditionally mutable.

Animation tracks target persistent entity, component, and property identities.
Targets are rebound within each live definition instance. Clips do not retain
live Java object references.

## glTF import and generated definitions

Selecting a glTF scene publishes a generated, read-only `EntityDefinition`
plus referenced mesh, material, texture, skin, animation, and other assets.
There is no public `Model3d` resource plus `ModelInstance3d` entity/component in
the initial architecture.

The import projection is direct:

```text
glTF node                 entity
local transform           Transform3d component
mesh                      MeshRenderer3d component
skinned mesh              SkinnedMeshRenderer3d component
camera                    Camera3d component
light                     Light3d component
animation playback owner  animation component
payload data              referenced immutable assets
```

Imported cameras and lights are ordinary components. They do not automatically
become the active camera or global light.

Generated internals are read-only and disposable. Import settings control
scene selection, supported feature inclusion, resource mapping, exported
attachment points, and explicitly supported material bindings. Gameplay,
collision, audio, additional children, and behavior belong in an authored
wrapper definition.

Source inspection and import publication are editor or project-build
operations. Normal `ProjectHost` startup consumes only complete published
generations through `ProjectContent`; it neither executes an importer nor reads
the original source asset. Export packaging includes the selected published
definitions and resources, not the source-import workflow that produced them.

Reimport prefers explicit source identifiers. Otherwise it uses stored source
evidence and deterministic locators conservatively. It does not silently fuzzy
match an uncertain target. Unresolved required targets prevent play/export and
produce diagnostics that allow explicit remapping or source repair.

The runtime may privately compile or flatten an imported hierarchy. A public
model hierarchy should be introduced only if measured performance and real
authoring cases demonstrate that the normal entity/component model is
insufficient.

## Project hosting and exported games

Editor play and exported games enter the engine through the same `ProjectHost`
contract. The host, rather than application source code, loads `project.json`,
discovers the runtime extensions present in the application, scans authored
assets, selects the optional startup scene or the entry scene, composes that
world, and returns a `HostedProject`. Applications therefore do not generate or
maintain a project-specific world loader.

Project composition reports stable coarse-grained milestones through an
optional `ProjectLoadProgress` callback. This is progress from completed or
starting host phases, not elapsed-time animation or estimated asset counts.
Callbacks run on the loading thread and must return promptly, allowing a desktop
host to keep its native window responsive while preserving the thread affinity
required by OpenGL and audio realization.

An application supplies behavior through a `ComponentRuntimeExtension`
provider. A provider which also implements `ApplicationRuntimeExtension` may
perform application-level preparation after composition. Discovery only makes
a provider available; the application extension named by the project manifest
controls which provider participates as the application entry point. Component
participation remains controlled by authored component type declarations.

The desktop host owns application lifecycle transitions requested through
`ApplicationControl`. A distinct startup scene may implement a main menu. New
Game transactionally loads a fresh entry world before releasing prior state,
Show Menu retains and stops advancing gameplay, Resume returns to that exact
world, and Quit ends the host loop. Menu rendering and command selection remain
authored application behavior; the desktop module only owns world lifetime and
transition semantics.

Optional launch presentation is manifest data rather than a generated Java
bootstrap. The desktop host opens the native window before world composition,
presents a project-owned background, title, optional studio logo, middleware
badges, and real load progress for the authored minimum duration, and displays it
only once per process launch. Later world replacement retains the menu beneath a
compact progress surface. A startup failure remains visible in the native window
instead of being reduced to a terminal exception alone.

The host executable selects a `ProjectRuntimeEnvironment`. The environment
supplies the engine capabilities included in that build: built-in component
descriptors and factories, fresh world subsystem adapters, and project-scoped
content loading. After the manifest, safe type catalog, and authored asset
catalog are available, it returns one `ProjectContent` containing the definition
resolver and immutable-resource provider used by composition. That resolver may
combine authored definitions with generated import publications without making
the generic host depend on a particular importer. Editor preview can provide a
preview environment while a desktop export can provide rendering, input,
audio, physics, and imported-content adapters without changing application code
or authored data.

An exported game will package the generic launcher and host, its selected
runtime environment and engine modules, application runtime-extension
providers, and the project assets. Its generated platform launcher supplies the
engine version, packaged project root, and published-content root to the generic
`DesktopProjectLauncher`; it does not generate a Java class which knows the
structure of the startup world. Packaging and platform launcher generation are
derived build concerns and do not introduce another authoring format.

The application-directory exporter accepts authored project data, completed import
publications, caller-resolved runtime JARs, and an output directory through one
build-tool-independent request. It owns runtime project-file selection,
exclusion of imported raw sources and incomplete publication state,
application-directory layout, relative launcher generation, and staged replacement.
Maven or a future editor resolves the runtime artifact set but does not
reproduce those export rules. Native bundles and archives will wrap this same
application directory rather than assembling games independently.

Runtime project-file selection includes the startup and entry scenes, launch
presentation, declared runtime resources, and project-file references nested in
resource properties. Editable source inputs used only to produce published
imports remain excluded. Export dependency discovery therefore follows authored
resource references instead of requiring every game build to maintain a
parallel copy list.

The application directory is the canonical assembled export and the first
supported output format. It contains relative launchers, application and engine
JARs, target-platform native dependencies, authored runtime project data, and
published imported content. The initial directory uses a compatible installed
Java runtime; it is relocatable on its target platform but is not claimed to be
a single-file or cross-platform executable.

Platform packaging consumes that assembled directory and adds only the runtime,
native launcher, metadata, signing, and distribution container required by the
target. The implementation order is the application directory on the current
host, a macOS application bundle over that directory, and then a DMG containing
the application bundle. Windows and Linux bundles and installers follow the
same composition rule on their respective build hosts. Mobile targets require
their own platform hosts and build pipelines; they do not reuse a desktop
launcher unchanged.

A conventional executable JAR is not an export target because the game needs
target-specific native libraries and reliable native-library discovery before
the application starts. The exporter will not create a self-extracting JAR or
restart Java through an application bootstrapper. A ZIP or similar archive may
distribute the assembled application directory without changing its runtime
model.

## Serialization and schema evolution

The canonical authoring format is deterministic UTF-8 JSON.

- Small structured assets use JSON.
- Large mesh, texture, audio, and similar payloads remain separate binaries.
- A compact runtime package may be generated later, but it is derived output,
  not a second authoring format.
- Java serialization and persisted implementation class names are forbidden.
- Known fields are validated strictly. Explicit namespaced extension objects
  are the escape hatch for extension-owned data.

Every asset envelope records at least its `AssetId`, asset kind, and
asset-format version. Every component record contains its component-type ID and
configuration-schema version.

Older formats may migrate deterministically in memory. Loading never silently
rewrites source files; the editor reports the migration and persists it only
through explicit save or migration. A newer unsupported version fails with a
clear diagnostic.

If a component type is unavailable, the editor preserves its complete
serialized record as opaque data, displays the missing type ID, and permits
unrelated inspection. Required unavailable components prevent play/export.
They are never discarded merely because their descriptor is missing.

The version-one entity-definition shape is:

```json
{
  "$schema": "https://jscene3d.org/schemas/entity-definition-1.json",
  "assetId": "4c189475-9845-4810-b7f9-af744d3cc726",
  "assetType": "entity-definition",
  "formatVersion": 1,
  "name": "Bullet",
  "contract": {
    "parameters": [],
    "signals": [],
    "actions": [],
    "capabilities": [],
    "attachments": [],
    "resourceBindings": []
  },
  "connections": [],
  "root": {
    "entryType": "local",
    "entityId": "853f50a0-17dc-46ac-9f04-f772e54c44b2",
    "name": "Bullet",
    "enabled": true,
    "components": [
      {
        "componentId": "b991ca3e-66bb-4ef0-a682-74773bbef0d0",
        "type": "io.github.glynch.jscene3d/transform-3d",
        "typeVersion": 1,
        "properties": {
          "translation": [0.0, 0.0, 0.0]
        }
      }
    ],
    "children": []
  }
}
```

Contract targets and connections store stable entity, component, property,
endpoint, and attachment identities. Omitting `componentId` addresses the
public contract of a nested definition placement; it never exposes that
definition's private components.

## Validation and diagnostics

Validation occurs before activation and reports ordered structured diagnostics.
At minimum it detects:

- duplicate asset IDs;
- unsupported asset or component schema versions;
- unknown required asset/component types;
- missing or wrong-kind asset references;
- structural definition cycles;
- duplicate local entity/component IDs;
- unresolved required contract values;
- illegal component multiplicity or capability conflicts;
- missing or ambiguous capability dependencies;
- incompatible spatial domains;
- multiple transform authorities;
- invalid signal/action payload or endpoint types;
- invalid collision-shape membership;
- orphaned imported targets.

Expected project/content failures are diagnostic data, not generic exceptions.
Programmer-contract violations may remain exceptions.

## Editor projection

The visual editor is a projection of the same definitions and descriptors used
by JSON, Java builders, validation, and runtime composition.

- The asset browser shows definitions without instantiating them.
- Opening a definition shows its authored hierarchy. Generated imported
  definitions are read-only.
- Dragging a definition into a world or another definition creates a placement.
- A placed definition appears as its root entity, with no wrapper node.
- The inspector exposes placement state and the definition's public contract,
  not private internals.
- Descriptor metadata supplies component property editors, constraints,
  signals, actions, and defaults.
- Runtime-created entities appear only in the live hierarchy while they exist.
- Live inspection never automatically writes runtime entities back into the
  authored world.
- Stable IDs are normally hidden but available for diagnostics and advanced
  wiring.

Java builders and annotations may provide additional authoring front ends, but
they must produce the same canonical immutable definitions. Arbitrary Java
construction is not a second runtime composition model.

## Visual editor host

The visual editor is a native JavaFX application. JavaFX owns the editor shell,
including its menus, hierarchy, inspectors, asset browser, diagnostics, tabs,
and layout. Its central viewport uses the actual JScene3D OpenGL renderer through
OpenGLFX. The editor will not maintain a parallel WebGL, WebGPU, or Three.js
renderer merely to display project content.

The focused JavaFX/OpenGLFX prototype proved actual JScene3D rendering,
physical and logical resizing, JavaFX-managed input and focus, ordered resource
disposal, named-module execution, and a relocatable packaged macOS application.
Other desktop platforms remain subject to equivalent qualification.

`jscene3d-lwjgl` exposes `RenderSurface` as the renderer-host seam. Its adapter
activates the host-owned context and correct presentation framebuffer, returns
one consistent `RenderSurfaceSize`, and releases the renderer's exclusive access
without destroying the surface or context. JScene3D's GLFW integration and the
editor's OpenGLFX integration use separate adapters at that seam. The renderer
owns GPU realizations; the host owns the window or control, context lifetime,
frame scheduling, and final presentation. This contract also prevents renderer
internals from assuming that presentation always targets framebuffer zero.

The initial editor and preview runtime share one JVM and communicate through
ordinary Java contracts. HTTP, JSON RPC, or gRPC is not introduced between
them. A separately hosted preview process and an IPC protocol may be added later
if crash isolation or hot restart provides enough value to justify that extra
boundary. JavaFX and OpenGLFX are editor dependencies and are never
included in exported games unless a game explicitly uses them itself.

## Screen presentation components

Screen presentation uses the same entity and component model as the 3D world;
it does not introduce a special HUD node type or a second hierarchy. A
`screen-canvas` component registers one ordinary entity subtree as an overlay.
Descendant `screen-region` components resolve anchored rectangles, while content
components such as `screen-image` and `bitmap-number` draw descriptor-selected
overlay-image resources into those regions.

Game-specific HUD behavior remains a game component. It binds explicitly to
game-state capabilities and generic screen-content components by stable
component target, then updates their presentation-neutral values. Layout, glyph
resources, and initial values therefore remain editor-visible project data; the
game extension supplies only semantic binding that a generic editor cannot
infer.

Overlay images are screen-presentation resources, separate from 3D texture
resources. Import may share source pixels, but runtime resource types do not
expose renderer texture objects for screen drawing.

## Initial module seams

The first implementation should preserve these seams even if existing Maven
artifact names later change:

1. **Asset/definition module**: JSON loading, immutable definitions, typed IDs
   and references, schema migration, catalogs, validation, and diagnostics.
2. **Component registry module**: type descriptors, migrations, capability
   requirements, and runtime construction adapters.
3. **World composition module**: preparation, transactional instantiation,
   lifecycle, schedules, mutation commits, signals/actions, and resource
   leases.
4. **3D adapter module**: built-in 3D descriptors and bridges to rendering and
   physics without making either backend the entity model.
5. **Host adapter**: window/game-loop integration and explicit construction of
   a `World` from registered modules.

These are responsibility seams; final Maven artifact names are selected during
implementation rather than defining the architecture.

## First implementation acceptance cases

The engine foundation is ready for Beacon Garden when one project can:

1. load a `ProjectManifest` and startup `WorldDefinition` from JSON;
2. validate a world containing local entities and placed definitions;
3. construct a `World` with explicit rendering, physics, audio, resource, and
   scheduling module interfaces as required by the host;
4. instantiate entities with `Transform3d`, camera, light, mesh-renderer, and
   Java behavior components;
5. load one generated glTF `EntityDefinition` and render an instance;
6. prepare and spawn a reusable definition during a fixed update;
7. activate it atomically at a phase boundary and show it in the live
   hierarchy;
8. author a collision body or sensor with more than one shape;
9. deliver a typed collision/overlap signal to game behavior;
10. disable, re-enable, and destroy an owned subtree with the documented
    lifecycle order;
11. fail invalid content through structured diagnostics without exposing a
    partially constructed world.

Doomed Corridors is a design validation target, not part of this first delivery.
The same model must later express a generated/imported level, player and weapon
composition, bullet and enemy spawning, pickup sensors, doors and switches,
independent collision geometry, HUD/UI children, audio, and live entities at
the required scale. Nothing in the first slice may make those cases require a
second entity model.

## Explicitly deferred

The following do not block the first Beacon Garden implementation:

- choosing or building a dependency-injection framework;
- final replacement name for `SpawnOperation`;
- data-oriented ECS storage or public queries;
- exact cross-platform deterministic replay or lockstep networking;
- definition inheritance or variants;
- general imported-definition refinement;
- a public model/model-instance hierarchy;
- arbitrary live editing and hot reload;
- runtime persistence/save games;
- streaming worlds and world partitioning;
- multiplayer replication;
- scripting languages or visual scripting;
- a complete 2D/UI component catalog;
- specialized pooling or batched projectile representations;
- final editor interaction design beyond the projection rules above.

Deferred does not mean prohibited. A later feature must deepen the established
modules or justify revisiting an invariant; it should not create a parallel
authoring or runtime model by default.

## Related decisions and plans

The durable architectural choice is summarized by ADR 0026. The concrete Beacon
Garden delivery sequence lives in its own repository so engine design and
application implementation remain separate.

## Reference designs

- [Godot: Nodes and scenes](https://docs.godotengine.org/en/4.7/getting_started/step_by_step/nodes_and_scenes.html)
- [Godot: 3D](https://docs.godotengine.org/en/4.7/tutorials/3d/index.html)
- [Godot: Physics introduction](https://docs.godotengine.org/en/stable/tutorials/physics/physics_introduction.html)
- [Unity: GameObjects](https://docs.unity3d.com/6000.0/Documentation/Manual/GameObjects.html)
- [Unity: Prefabs](https://docs.unity3d.com/6000.0/Documentation/Manual/Prefabs.html)
- [Unity: Runtime instantiation](https://docs.unity3d.com/6000.0/Documentation/ScriptReference/Object.Instantiate.html)
