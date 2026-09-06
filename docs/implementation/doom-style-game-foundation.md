# Doom-compatible game foundation

This document records the reusable engine foundation and migration direction
for Doomed Corridors. New game composition follows the accepted
[entity-component world architecture](../design/entity-component-world-architecture.md),
while WAD-specific publication is defined in
[`doom-wad-import.md`](doom-wad-import.md).

## Artifact responsibilities

- `jscene3d-core` remains the renderer-independent graphics foundation.
- `jscene3d-physics` remains a renderer-independent physics module containing
  collision objects, shapes, queries, and simulation rather than rendered or
  game-specific objects.
- `jscene3d-game` owns genre-independent host lifecycle, input, clocks, and
  coordination needed by a `World`.
- `jscene3d-project` owns the safe asset/definition model, loading, validation,
  migration, catalogs, and diagnostics.
- World composition owns live entities, components, module registration,
  scheduling, signals, resource leases, and transactional mutation.
- Doomed Corridors owns its level rules, actors, weapons, inventory, combat,
  campaign, HUD, presentation, content selection, and packaging.

Generic functionality moves into a reusable module only after it has a clear
game-independent interface and a demonstrated reuse case.

## Existing reusable capabilities

The lower-level libraries already contain capabilities that new entity
components may call through deliberately designed interfaces. Legacy project
runtime objects and participation interfaces are not retained or adapted.

### Rendering

JScene3D supports textured and generated geometry, lighting, fog, transparency,
instancing, custom shaders, ray casting, overlays, glTF loading, animation, and
line-based debug visualization. Renderer `Scene` and `Object3D` remain
lower-level implementation concepts; neither becomes the game-facing `World`
or `Entity` hierarchy.

### Input and timing

The LWJGL layer provides captured cursor mode, raw mouse motion where available,
focus state, and input restoration. The game layer provides fixed and rendered
updates, semantic input actions, bounded catch-up, and camera-relative movement.
These capabilities should be exposed through the `World` host and scheduling
interfaces rather than a parallel application runtime.

### Physics

The physics module provides static collision, kinematic bodies, primitive
shapes, broad- and narrow-phase queries, ray and overlap queries, shape sweeps,
gravity, floor detection, wall sliding, step traversal, collision sensors,
overlap events, and debug snapshots.

Entity physics components will adapt these objects into a `World`. Rendering
and collision remain independently authored. Each spatial entity has one
declared transform authority, and presentation interpolation reads physics
state without transferring physics ownership into the renderer.

### Audio

The audio module supports buffered Ogg Vorbis clips, positional effects,
non-positional interface effects, music, listener updates, and independent
master/music/effects gains through OpenAL. A world audio interface hides native
handles and backend lifetime from components.

## Freedoom content

Freedoom supplies content rather than engine behavior. Doomed Corridors uses a
pinned `freedoom2.wad` under its 3-clause BSD license, retaining the required
notices and attribution. The WAD remains authoritative and is not downloaded at
runtime.

Generic WAD access and Doom decoding remain separate from rendering, physics,
audio, and gameplay. Generated content enters a Doomed Corridors world as
assets, immutable runtime resources, entity definitions, and ordinary component
configuration.

## Migration sequence

Beacon Garden establishes the generic architecture before Doomed Corridors is
migrated. The Doomed Corridors sequence is then:

1. Adapt the project manifest and asset catalog to reference the startup
   `WorldDefinition` and WAD import recipe.
2. Publish `MAP01` through generated assets and a read-only map
   `EntityDefinition` where entity structure is appropriate.
3. Render the placed map through entity rendering components.
4. Register map collision through explicit collision components and shapes.
5. Compose the player from transform, character physics, input behavior,
   camera, weapon, and audio components/entities.
6. Prepare and spawn one projectile definition and complete one combat loop.
7. Add enemies, pickups, doors, switches, lifts, teleports, exits, sector
   effects, HUD, and campaign progression in independently testable slices.
8. Progressively cover the pinned Freedoom campaign.

The first migration target is behavioral parity with the existing playable
`MAP01` path. New Doom features follow parity rather than distorting the generic
architecture bootstrap.

## Deferred game scope

Boom, MBF, Hexen-format, UDMF, and GZDoom extensions are outside the first
compatibility target. Multiplayer, save games, scripting, generalized mod
support, and exact software-renderer reproduction also remain later work.

The engine may later add rigid-body features, broader resource streaming,
specialized high-volume simulation, and richer animation. None requires a
second entity, definition, world, or collision model.
