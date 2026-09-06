# Doom WAD import under the entity-component architecture

This document defines how Doom-compatible WAD content enters the accepted
[entity-component world architecture](../design/entity-component-world-architecture.md).
Doomed Corridors is the first demanding consumer, using a pinned Freedoom Phase
2 WAD and beginning with `MAP01`.

## Ownership

- `jscene3d-project` owns safe project metadata, asset identity, typed
  references, definitions, validation, migrations, and diagnostics.
- `jscene3d-project-import` owns deterministic import orchestration, generated
  asset publication, provenance, and disposable cache policy.
- `jscene3d-wad` owns generic WAD validation, archive layering, and bounded
  access to ordered opaque lumps. It does not interpret Doom maps.
- `jscene3d-doom` owns reusable classic Doom map discovery, decoding, and
  validation over `jscene3d-wad`.
- Doomed Corridors owns actor rules, weapons, inventory, combat, campaign state,
  linedef and sector behavior, presentation choices, and Freedoom attribution.

No WAD, sector, weapon, enemy, or Doom-specific type belongs in the generic
entity, world, rendering, game, or physics modules.

## Project representation

The Doomed Corridors `ProjectManifest` identifies its startup
`WorldDefinition`, asset roots, game module, and import configuration. The
startup world composes ordinary entities and `EntityDefinition` placements; it
does not use an imperative parallel world-construction model.

The pinned WAD is a source `Asset` with stable identity, project-relative path,
digest, release information, license, notices, and credits. It is never
downloaded at runtime or mutated by an importer.

The import recipe selects `MAP01` and publishes typed generated assets. The
exact publication can evolve by vertical slice, but it must converge on the
canonical model:

- a generated read-only `EntityDefinition` for the finite map hierarchy when
  entity composition is appropriate;
- immutable runtime-resource assets for geometry, materials, textures, audio,
  and collision shapes;
- decoded Doom map or metadata assets where Doomed Corridors behavior still
  requires domain information;
- provenance mapping each generated asset back to its WAD, lump, map element,
  import settings, and importer version.

The authored startup `WorldDefinition` places the generated map definition and
adds the player, cameras, HUD, game behavior, and other authored definitions.
Generated import output remains read-only. Game-specific behavior is composed
around it rather than written into the disposable cache.

## Import and cache invariants

Source assets are authoritative. Generated output is reproducible from source
bytes, import settings, and importer version. Deleting the cache must never
delete project-authored information.

Every generated asset has a stable `AssetId` and explicit provenance. Source
indices, names, and hashes may provide identity evidence, but the importer must
not advertise an unstable locator as permanent identity or silently fuzzy
remap an uncertain target. An unresolved required target produces a diagnostic
and prevents play/export until explicitly repaired.

Import inspection and validation are headless. They do not create a window,
initialize rendering/audio, construct a `World`, or execute Doomed Corridors
behavior.

## Collision and things

Map rendering and map collision are independently generated. Rendered walls
and floors do not become collision merely because geometry exists.

Static collision is represented through collision-body components with one or
more explicitly referenced shape components. Pickups, exits, teleport regions,
and other non-blocking interactions use sensor/area components and typed
overlap signals. Doomed Corridors behavior decides what those signals mean.

Map things that need identity, components, independent state, or lifecycle are
instantiated as entities. High-volume geometry and internal map data may remain
batched runtime resources when independent entity semantics provide no value.

## Compatibility target

The initial target is the pinned Freedoom Phase 2 `freedoom2.wad`, progressing
from `MAP01` toward all 32 vanilla Doom II maps. Boom, MBF, Hexen-format, UDMF,
and GZDoom extensions are outside the initial target.

Multiplayer, save games, generalized mod discovery, and exact software-renderer
reproduction do not block this import path.

## Delivery sequence

1. Prove the canonical definitions, component descriptors, world composition,
   lifecycle, spawning, and collision signal path in Beacon Garden.
2. Retain the existing validated WAD archive and Doom map decoding modules.
3. Adapt import publication to stable assets, generated entity definitions,
   immutable runtime resources, and the new asset catalog.
4. Build a Doomed Corridors `WorldDefinition` that places generated `MAP01`
   content through the same composer used by Beacon Garden.
5. Migrate the player and one complete combat loop to entities and components.
6. Add pickups, doors, switches, lifts, teleports, exits, sector effects, and
   further actors as independently verifiable slices.
7. Progressively cover the pinned campaign without introducing a second world
   or entity model.
