# Entity-component world architecture

Status: current implemented architecture

JScene3D represents authored game structure as immutable world and entity
definitions, then composes those definitions into live hierarchical entities.
Components supply spatial state, rendering, physics, audio, input, animation,
and application behavior without requiring game-specific `Entity` subclasses.

This document is the authoritative design for that authored and runtime model.
For a shorter introduction, see
[Project and game fundamentals](../manual/project-fundamentals.md).

## Architectural rules

The model is built around several deliberate distinctions:

- A renderer `Scene` is not a project `World`.
- A renderer `Object3D` is not a live `Entity`.
- `WorldDefinition` and `EntityDefinition` are immutable authored data;
  `World` and `Entity` are live runtime state.
- The entity hierarchy owns identity, lifetime, and structural ownership.
- Components compose behavior instead of creating an inheritance hierarchy of
  entity types.
- Safe descriptors define serialized component contracts. Trusted runtime
  extensions provide their executable implementations separately.
- Components depend on semantic capabilities and stable targets, not concrete
  sibling implementation classes or hierarchy searches.
- Spatial, rendering, physics, audio, and other backend state remain behind
  world-scoped module interfaces.
- Rendering and collision are represented independently, even when generated
  from the same source content.
- Loading and authoring project data do not imply executing a project.

These rules let the headless project model, runtime host, editor, importers,
and exported applications share one definition format without sharing one
execution environment.

## Models and boundaries

The main flow is:

```text
.j3d descriptor
authored definitions ─┐
published definitions ├─ DefinitionResolver
published resources ──┘
         ↓
registered safe descriptors + trusted runtime extensions
         ↓
WorldComposer
         ↓
inactive World and Entity graph
         ↓ activate
live lifecycle, scheduling, signals, mutation, and spawning
```

The layers have different responsibilities:

- `jscene3d-project` owns project discovery, descriptors, authored definition
  data, extension metadata, validation, stable identities, and diagnostics.
- `jscene3d-project-import` owns deterministic import inspection, preparation,
  publication, and read-only access to generated artifacts.
- `jscene3d-project-runtime` owns trusted definition composition, live worlds,
  component factories, lifecycle, scheduling, signals, resources, and
  structural mutation.
- Focused modules such as `jscene3d-project-3d` and
  `jscene3d-project-physics` provide descriptors, runtime component factories,
  and world-module adapters for their domains.
- `jscene3d-game` adds genre-independent game-loop, input, application, and
  presentation facilities over the project runtime.
- Application extensions own title-specific rules and components.

The low-level renderer and physics modules remain usable without the project
model. Their objects are backend implementation details when used by a
composed project world.

## Authored project model

### Project descriptor

A JScene3D project has one project-named `.j3d` descriptor in its root. During
the compatibility period, `jscene3d.json` is accepted only when no `.j3d`
descriptor exists. `ProjectLoader` validates the selected descriptor into a
`GameProject`, which retains both the normalized project root and the actual
descriptor path.

`GameProject` contains project identity and attribution, engine compatibility,
runtime entry points, extension requirements, source assets, import-definition
paths, export presets, and launch presentation. Its
`RuntimeConfiguration` selects the application extension, gameplay entry
world, optional startup world, optional project systems, and optional input
map. The field names `entryScene` and `startupScene` remain in that API, but
the referenced assets used by the current runtime host are `WorldDefinition`
documents.

Loading a `GameProject` is structural and safe. It does not load runtime
extensions, execute importers, compose a world, or initialize rendering,
physics, or audio.

### Definition assets

`AssetCatalog` recursively discovers authored `*.world.json` and
`*.entity.json` documents in deterministic project-relative path order. Nested
project roots are boundaries. Each definition has a project-wide `AssetId`,
and complete content remains unloaded until requested through a
`DefinitionResolver`.

A `WorldDefinition` contains:

- its stable `AssetId` and display name;
- root `EntityEntry` values in authored order; and
- signal-to-action connections within the world definition.

An `EntityDefinition` contains:

- its stable `AssetId` and display name;
- exactly one locally authored root entity;
- an `EntityContract` describing its deliberately exported surface; and
- signal-to-action connections private to the definition.

Both types are immutable. Loading them validates the complete transitive
placement graph and, when supplied a `RegisteredTypeCatalog`, validates their
component types, properties, capabilities, endpoints, and contracts.

### Local entities and placements

`EntityEntry` is a sealed authored choice between `LocalEntity` and
`EntityPlacement`.

A `LocalEntity` declares:

- an `EntityId` stable within its containing definition asset;
- an optional display name;
- its initial local enabled state;
- identified component definitions; and
- owned child entries in authored order.

An `EntityPlacement` references an `EntityDefinition`, supplies exported
arguments, and declares the instance root's local identity, optional name, and
enabled state. Composition expands the referenced definition directly into
the placement root. It does not add a synthetic wrapper entity.

Definition reuse is composition, not inheritance. There are no base or derived
definitions and no implicit override of private internals. A reusable
definition exposes only the members declared by its `EntityContract`.

### Exported definition contracts

An `EntityContract` can declare:

- ordinary parameters targeting private component properties or nested
  placement arguments;
- resource bindings targeting resource-valued properties;
- signals and actions backed by private or nested endpoints;
- semantic capabilities; and
- spatial attachment points.

Required parameters and resource bindings must be supplied, and supplied
values must match the declared structural kind. A placement can connect to an
exported signal or action without addressing the placed definition's private
component identities. Contract targets are validated against the complete
definition graph.

The current runtime applies parameter and resource-binding overrides through
definition-instance scopes and resolves exported signal/action endpoints during
composition. Capability and attachment declarations remain safe contract
metadata for validation and tooling; they do not grant arbitrary access to a
placed definition's private hierarchy.

## Identity and references

The identity scopes are intentionally distinct:

- `AssetId` identifies a world or entity definition across the project.
- `EntityId` identifies an authored entry within its containing asset.
- `ComponentId` identifies a component within its entity.
- `RuntimeEntityId` identifies one live instance within one `World`.

Placed and spawned definitions receive separate definition-instance scopes.
The same authored `EntityId` can therefore be instantiated repeatedly without
colliding at runtime. A live `Entity` retains provenance through
`authoredAsset()`, `authoredId()`, `instantiationKind()`, and, for placement or
spawn roots, `instantiatedDefinition()`.

Authored entity, component, property, endpoint, resource, and spatial targets
use stable identities. They do not resolve by display name, hierarchy path,
implementation class, nearest ancestor, or global search. Target-valued
properties retain the definition-instance scope in which they were authored,
including when passed through a public definition contract.

## Component model

### Authored component definitions

A `ComponentDefinition` is immutable configuration containing:

- a stable `ComponentId`;
- a namespaced `ComponentTypeId`;
- a positive configuration-schema version; and
- ordered `ProjectValue` properties.

Component values are data. They do not embed implementation class names or
constructors.

### Safe component descriptors

A `ComponentTypeDescriptor` is authoritative inert metadata for one exact
component type and version. It declares:

- presentation metadata;
- properties, defaults, value kinds, references, and constraints;
- signals and actions with optional registered payload types;
- provided and required `CapabilityId` values;
- named spatial attachments;
- single or multiple per-entity multiplicity;
- conflicting component types;
- a primary `ComponentSpatialDomain`; and
- lifecycle and deterministic update participation.

Validation rejects unknown exact component versions, invalid properties,
duplicate single-instance types, declared conflicts, ambiguous spatial
authority, and unsatisfied capabilities. Each required capability must have
exactly one provider on the same entity. A component cannot both provide and
require the same capability.

Capabilities express semantic sibling dependencies. At runtime,
`Entity.capability` selects a provider from the safe descriptor declarations
and only then checks the caller's expected Java representation. It does not
search parents, children, unrelated entities, or implementation types.

### Descriptor and implementation separation

An `ExtensionDescriptor` supplies safe metadata for registered types,
components, settings, presentation, version, and engine compatibility. It is
inert: it contains no implementation class names and does not load extension
code. Multiple resolved descriptors form a deterministic
`RegisteredTypeCatalog`.

A trusted `ComponentRuntimeExtension` with the same stable extension identity
registers executable `ComponentFactory` implementations. A factory receives a
bounded `ComponentFactoryContext` containing the owner, inactive world,
owner-scoped spawn target, authored definition, exact descriptor, effective
properties, and prepared resource access. Runtime component values are
ordinary non-null Java objects and need not extend an engine component base
class.

This division is fundamental:

```text
ExtensionDescriptor
  safe data for loading, validation, and authoring

ComponentRuntimeExtension
  trusted executable factories used only during runtime composition
```

## Transactional world composition

`WorldComposer` is the single composition boundary. It receives:

- a `DefinitionResolver` for authored and generated definitions;
- the selected `WorldDefinition` reference or a validated in-memory revision;
- a `RegisteredTypeCatalog`;
- trusted `ComponentRuntimeExtension` implementations;
- host-supplied `WorldModuleBinding` values; and
- a host-owned `RuntimeResourceProvider`.

After graph validation, the runtime-free composition planner in
`jscene3d-project` expands authored and generated definitions into an immutable
`CompositionPlan`. The plan assigns stable occurrence identities, preserves
authored scopes and provenance, and establishes effective Component properties,
Resource references, and scoped connections. It contains no live runtime,
renderer, or executable extension objects. Runtime composition consumes this
plan rather than independently interpreting the definition graph.

Composition performs these operations as one transaction:

1. Load and validate the root world and transitive entity-definition graph.
2. Safely plan local entries, definition placements, effective properties, and
   scoped connections without loading runtime implementations.
3. Allocate the complete live entity graph from the plan before invoking a
   component factory.
4. Resolve the planned exact descriptors to trusted runtime factories.
5. Construct every runtime component.
6. Bind authored entity and component references after all factories finish.
7. Bind every descriptor-declared signal and action implementation.
8. Resolve planned authored connections to exact live endpoint addresses.
9. Publish a complete inactive `World` only if every step succeeds.

Failure returns ordered `ProjectDiagnostic` values and releases constructed
component values and resource leases. No partial world is published. A failed
composition does not transfer ownership of host-supplied world modules; a
successful composition does.

The resulting world is deliberately inactive. Construction establishes a
complete graph, but semantic lifecycle and signal dispatch begin only when the
host calls `World.activate()`.

## Live world and entity APIs

`World` is the runtime ownership root for one composed definition. It exposes:

- the source `WorldDefinition`;
- deterministic roots and lookup by `RuntimeEntityId`;
- exact-interface world-module lookup;
- inactive definition preparation for later spawning;
- owner-scoped spawn targets;
- activation and active/closed state;
- fixed and frame advancement; and
- enable, disable, destroy, and terminal close operations.

The interface is confined to one caller-owned logical simulation thread. It is
not a concurrent entity store and does not create its own execution thread.

`Entity` is a read-only live view. It exposes authored and runtime identity,
instantiation provenance, local and effective enabled state, destruction state,
ownership parent and children, identified component access, local capability
access, and its owning `World`. Mutation occurs through bounded world or
component interfaces rather than arbitrary setters on the entity graph.

## Ownership and spatial state

The entity hierarchy defines structural ownership and cleanup order. A child
cannot outlive its owner. Local enablement combines with ancestor enablement to
produce effective enablement. The current runtime supports composition,
owner-scoped child spawning, enablement changes, and subtree destruction; it
does not expose a general reparenting operation.

Spatial state is optional. An entity without a spatial component is still a
valid entity. Descriptor validation permits at most one component with a
non-`NONE` primary spatial domain on an entity.

For the implemented 3D domain, `Transform3d` owns mutable local position,
normalized orientation, scale, and derived local and world matrices. A direct
ownership parent's compatible transform supplies spatial inheritance. A
non-spatial or differently spatial parent starts a new 3D spatial root.

`Spatial3dWorldModule` realizes transforms and transform-attached cameras,
lights, meshes, and billboards. Its adapter may use renderer `Object3D` values
internally, but those objects do not become project entities and are not
exposed as the public world hierarchy.

Each live spatial entity has one effective transform authority. Static bodies
and sensors consume entity transform state. Explicitly moved character bodies
request motion through physics. A simulation-owned body can write its world
pose through the transform's designated world-pose seam. Presentation reads
the resulting spatial state rather than creating a competing entity transform.

## World modules and resources

A `WorldModule` is a world-scoped adapter for a focused runtime facility such
as spatial presentation, physics, input, audio, or screen presentation. The
host supplies each module through a `WorldModuleBinding` keyed by an exact
stable interface. Lookup does not search implementation classes, parent
interfaces, or global state.

A successfully composed world owns the bound adapters and closes them in
reverse binding order after components and resource leases are released. If
composition fails before ownership transfer, the host remains responsible for
closing them.

`RuntimeResourceProvider` is the host seam for acquiring shared immutable
runtime resources. The provider remains caller-owned. Each acquisition returns
a distinct `RuntimeResourceLease`; the world attributes leases to their owning
entities, shares underlying values where the provider permits, and releases a
lease when its final owning entity is destroyed or when the world closes.
Components receive the resource value and must not close shared values
directly.

## Lifecycle

Component descriptors, not Java method presence alone, authorize lifecycle
participation. A runtime value whose descriptor declares lifecycle callbacks
implements `ComponentLifecycleCallbacks`.

The semantic events are:

- `CREATED`: once after the complete graph has been constructed and bound;
- `ACTIVATED`: whenever the owning entity becomes effectively enabled in an
  active world;
- `DEACTIVATED`: whenever it ceases to be effectively enabled; and
- `DESTROYED`: once before permanent release.

Initial activation creates every component owner-first, then activates
components whose entities are effectively enabled. A failure compensates
completed work in reverse order, closes component values and retained
resources, closes owned modules, and leaves the world terminally closed.

Later enablement activates owner before descendant. Disablement, destruction,
and closure deactivate and destroy in reverse ownership/construction order.
Component values implementing `AutoCloseable` are closed after their semantic
lifecycle ends. Cleanup continues after an individual failure while preserving
the first failure and suppressing later ones.

`World.close()` is idempotent and terminal. It stops endpoint dispatch,
releases lifecycle participation and component values, closes resource leases,
and finally closes owned world modules.

## Scheduling and time

`World.advanceFixed(Duration)` and `World.advanceFrame(Duration, float)` are
synchronous and non-reentrant. The world owns fixed tick numbering and
accumulated simulation time. The host owns the outer loop and supplies the
fixed-step duration, accepted frame duration, and interpolation fraction.

Descriptors can declare exactly three component-visible phases:

- `BEFORE_PHYSICS` is the component-visible fixed-update phase for reading
  input and requesting movement or state changes.
- `AFTER_PHYSICS` is the component-visible fixed-update phase after physics
  state and signals are current.
- `FRAME_UPDATE` is the component-visible frame-update phase for presentation
  behavior using completed simulation time and interpolation.

One fixed advance runs `BEFORE_PHYSICS`, then an engine-owned physics seam, then
`AFTER_PHYSICS`. The physics seam is not a component-visible phase; bound
`PhysicsStepWorldModule` instances advance there in host binding order and
deliver physics signals. `FRAME_UPDATE` runs in a separate frame advance.

Only effectively enabled entities participate. Schedules are compiled from
exact descriptor declarations and ordered deterministically, not from runtime
registration timing.

Structural requests made by a callback commit at the next phase boundary.
Requests made while the world is idle commit before the requesting operation
returns. A callback cannot recursively advance or close the world.

## Signals, actions, and connections

A component descriptor declares named signal outputs and action inputs, each
with either no payload or one exact registered payload type. Components that
participate implement `ComponentEndpointBinder` to obtain `RuntimeSignal`
handles and register `RuntimeAction` or `RuntimePayloadAction` callbacks.

`SignalConnection` joins a stable `EndpointTarget` signal to a stable action
target. Targets can address a local component endpoint or an endpoint exported
by a definition placement. Definition-instance scoping prevents one placement
from accidentally binding another placement's private endpoint.

Composition requires every descriptor-declared endpoint implementation and
resolves every connection before publishing the world. Runtime dispatch is
synchronous and follows authored connection order. Payload identities must
match exactly. Disabled sources do not emit, and disabled actions do not
execute.

There is no implicit global event bus. Structural mutation requested during
nested signal dispatch remains deferred until the outermost dispatch finishes.

## Structural mutation and spawning

The live structural API is deliberately bounded:

- `enable` and `disable` change local enablement while preserving inherited
  effective state;
- `destroy` permanently removes one entity and its owned subtree; and
- `SpawnTarget` can add only direct children beneath its fixed owner.

Destroy requests make a subtree stop participating immediately. Commit then
deactivates and destroys its components, releases entity-owned resource leases,
removes endpoint routes and schedule entries, and removes the subtree from
world lookup and ownership traversal.

Runtime spawning has a preparation boundary. While a composed world is still
inactive, `World.prepare` resolves and validates an `EntityDefinition` and its
transitive graph, invokes factory preparation hooks, and retains declared
resources. Exported resource bindings are fixed for the preparation; ordinary
parameters remain instance-specific.

An active world uses an owner-scoped `SpawnTarget` to request an instance of a
`PreparedEntityDefinition`. An idle request commits synchronously. A request
from an update or signal callback waits for the next structural boundary and
cannot participate in the phase that requested it. A successful transaction
constructs, binds, activates, and then publishes the complete instance. A
failed or cancelled `SpawnOperation` exposes no partial entity.

## Rendering and collision

Rendering and collision are separate component and resource concerns. A mesh
renderer references renderable geometry and material resources. A collision
body references explicit collision-shape components or resources. Visible
geometry does not become collision merely because it exists, and a collision
shape does not imply a visible mesh.

`Physics3dWorldModule` registers descriptor-backed static bodies, non-blocking
sensors, and explicitly moved character bodies against an entity's
authoritative `Transform3d` and explicitly referenced shapes. Physics detects
contacts and overlaps; application components decide their game meaning
through typed signals and actions.

This separation also applies to generated content. An importer can derive both
render and collision artifacts from one source while publishing and referencing
them independently.

## Authored and generated definitions

`DefinitionResolver` is the common loading seam for authored and generated
definitions. An `AssetCatalog` provides authored sources. Import publication
can add generated entity definitions with authoritative `AssetId` values and
generated resources with ordinary `ResourceReference` values.

Generated definitions are immutable, read-only project content. They use the
same `EntityDefinition`, component descriptor, validation, placement, and
composition path as authored definitions. Application-specific behavior can be
composed around generated content through authored wrappers instead of being
written into a disposable import cache.

Normal runtime loading consumes complete published import generations through
`PublishedProjectContent` or `PublishedRuntimeResources`. It does not discover
or execute import providers and does not read the original import source.
Further detail is in
[WAD and Doom integration](wad-doom-integration.md), whose publication rules
are generic despite that document's source formats.

## Project hosting

`ProjectRuntimeHost` is the generic manifest-driven runtime host. A load:

1. locates and validates the project descriptor;
2. resolves safe extension descriptors and host-provided built-ins;
3. scans authored definitions;
4. loads an optional input map;
5. discovers trusted runtime extensions;
6. asks the `ProjectRuntimeEnvironment` for authored/generated content and
   fresh world modules;
7. composes the selected world; and
8. invokes `prepare` only on the manifest-selected
   `ApplicationRuntimeExtension`, if it implements that optional entry point.

The standard load selects the optional startup world before the gameplay entry
world. `loadEntry` selects the gameplay entry world. A `ProjectLaunchRequest`
can instead select a project-relative world and portable application-defined
parameters, including a named playtest request.

Success returns a `HostedProject` containing the validated `GameProject`, the
authored asset catalog, the launch request, and an owned inactive `World`.
Application preparation must not activate it. The runner or host decides when
to activate and advance the world. Closing the `HostedProject` closes its
world.

The selected application extension is the title entry point; merely appearing
on the class path or implementing `ApplicationRuntimeExtension` does not cause
application participation.

## Editor and authoring boundary

The current editor architecture is:

```text
Code OSS frontend
        ↓ framed authoring protocol
Java authoring service
        ↓
headless editor-authoring and project model
```

The authoring service loads the `.j3d` descriptor, project settings, safe
extension descriptors, authored definitions, import definitions, and already
published generated definitions. It does not load runtime extensions, execute
arbitrary application code, run import providers, or construct an active
`World`.

`EditorProjectLoader` creates an `EditorProjectSession` that owns project
identity, diagnostics, definition retention, hierarchy and Inspector
projections, and authoritative authored working copies. Authored definitions
can be changed through source-preserving, validated semantic operations;
generated definitions remain read-only. The Code OSS frontend presents those
projections and delegates project semantics to Java.

The detailed document lifecycle and frontend/session identity rules are
defined in [JScene3D Editor architecture](editor-architecture.md). They are not
part of runtime world composition.

## Validation and diagnostics

Project loading and definition composition report structured
`ProjectDiagnostic` values with stable codes, severity, source URI, JSON
location, and technical details. Validation is layered:

- descriptor and path validation establishes a safe project boundary;
- structural definition loading validates schema, identities, references, and
  transitive placement graphs;
- descriptor-aware validation checks exact component versions, properties,
  capabilities, multiplicity, conflicts, spatial authority, endpoints, and
  contracts; and
- runtime composition checks executable factory registration, construction,
  reference binding, endpoint implementation, modules, and resources.

Operational failures are converted to structured diagnostics at the boundary
where possible. Invalid authored or generated content does not produce a
partially live world.

## Architectural invariants

The following invariants define the current model:

1. Authored definitions never become mutable live state.
2. Every live entity belongs to exactly one world and has one ownership parent
   or is a world root.
3. Placement and spawn roots are the instantiated definition roots, not wrapper
   entities.
4. Component descriptors are the authority for validation, lifecycle,
   scheduling, endpoints, and capabilities.
5. Runtime extensions implement descriptors but do not redefine their safe
   contract.
6. Required capabilities resolve to exactly one provider on the same entity.
7. At most one component supplies an entity's primary spatial domain.
8. Composition is transactional and publishes only a complete inactive world.
9. Signals dispatch synchronously through explicit authored connections.
10. Structural mutation commits only at controlled world boundaries.
11. Runtime resources are retained through world-owned leases.
12. Rendering, collision, authoring, importing, and application execution keep
    explicit ownership boundaries.

## Related decisions and documentation

- [ADR 0024: Separate physics from game integration](../adr/0024-separate-physics-from-game-integration.md)
- [ADR 0025: Separate game applications from the game engine](../adr/0025-separate-game-applications-from-the-game-engine.md)
- [ADR 0026: Use hierarchical entity-component worlds](../adr/0026-use-hierarchical-entity-component-worlds.md)
- [ADR 0030: Model editable content as resource working copies](../adr/0030-model-editable-content-as-resource-working-copies.md)
- [Project and game fundamentals](../manual/project-fundamentals.md)
- [Editor fundamentals](../manual/editor-fundamentals.md)
- [JScene3D Editor architecture](editor-architecture.md)
- [WAD and Doom integration](wad-doom-integration.md)
