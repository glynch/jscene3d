# Doom-compatible game foundation

This document records the reusable foundation for JScene3D's first Game
Application. The earlier proposal for a small original Doom-style level without
WAD compatibility has been superseded by the project and import direction in
[`game-projects-and-wad-import.md`](game-projects-and-wad-import.md). Doomed
Corridors now intends to load a pinned Freedoom Phase 2 WAD and progressively
implement vanilla Doom II data and gameplay semantics.

## Artifact boundaries

The implementation will be divided by responsibility:

- `jscene3d-physics` is an original, pure-Java, renderer-independent Physics
  Engine. It contains no scene objects, rendering behavior, game rules, WAD
  concepts, or Doom-specific collision rules.
- `jscene3d-game` is a reusable, genre-independent Game Engine. It coordinates
  lifecycle, fixed and rendered updates, input actions, assets, game states,
  physics bindings, animation, audio, and rendering without defining a
  particular game's world or rules.
- `jscene3d-project` is a headless, genre-independent project definition and
  loading artifact. It exposes validated metadata and runtime configuration to
  standalone launchers, tools, and the future editor without executing a game.
- A separately named Game Application artifact owns the first playable title.
  It contains the level representation, player rules, weapons, enemies,
  pickups, doors, combat, HUD composition, selected third-party content, and
  application packaging.

Generic capabilities will not be promoted out of the Game Application merely
because another game might hypothetically use them. Promotion requires a clear,
genre-independent contract and a demonstrated reuse case.

## Freedoom content strategy

[Freedoom](https://freedoom.github.io/) supplies original levels, artwork,
sounds, and music for Doom-compatible engines. It is content rather than an
engine. The first Game Application will reuse a selected, pinned set of Freedoom
assets under the project's
[3-clause BSD license](https://github.com/freedoom/freedoom/blob/master/COPYING.adoc),
retain its copyright notice and disclaimer, and show appropriate attribution.
Assets will be stored or converted into formats consumed directly by JScene3D;
they will not be downloaded at runtime.

The pinned `freedoom2.wad` will remain the authoritative source for its levels
and content. A headless reader will validate and expose WAD lumps before later
import slices convert maps and resources into engine-native runtime
representations. Doom-specific import and compatibility behavior initially
belongs in Doomed Corridors, not in the Physics Engine or Game Engine. A WAD
importer can move to a separate optional artifact once another consumer proves
that reusable seam.

## Existing rendering foundation

JScene3D already provides the rendering capabilities required by the initial
game: textured and generated geometry, alpha masking, transparency, fog,
lighting, instancing, custom per-instance attributes, custom shaders,
raycasting, overlays, GUI controls, glTF loading, animation, and line-based
debug visualization. The first game does not require PBR materials,
environment lighting, skeletal animation, post-processing, or shadows, even
though several are already supported.

## Reusable foundation status

### Platform input

The LWJGL platform layer now provides a captured cursor mode, optional raw mouse
motion when GLFW supports it, observable focus state, reliable input and cursor
restoration on focus loss, and reusable pointer-lock controls.

### Physics Engine

The first `jscene3d-physics` slice provides fixed updates, static collision,
kinematic bodies, box, sphere, and capsule shapes, broad- and narrow-phase
collision, ray and overlap queries, shape sweeps, gravity, floor detection, wall
sliding, bounded step traversal, collision sensors, overlap events, debug lines,
and a reusable `CharacterController` proven by the physics obstacle course and
game sandbox.

### Game Engine

The initial `jscene3d-game` slice now provides a lifecycle with separate fixed
and rendered updates, semantic input action mapping, bounded catch-up behavior,
camera-relative character movement, and Physics Bindings with render
interpolation. First- and third-person sandboxes prove that the character
movement interface remains independent of camera policy. Game-state
transitions, asset lifetime management, sprite-frame animation, and
camera-facing billboard batches remain later reusable slices. Static spherical
and upright cylindrical billboards are now part of the rendering foundation;
the game module does not own their camera-facing transform policy. The artifact
must not define sectors, weapons, enemies, damage rules, or a Doom level format.

### Audio

A recognizable action game requires positional effects, non-positional user
interface effects, music, camera-listener updates, and separate master, music,
and effects volumes. The optional `jscene3d-audio` artifact now provides this
foundation through buffered Ogg Vorbis clips and an OpenAL implementation. Its
public interface exposes application audio concepts rather than native handles.
A multi-backend adapter hierarchy will not be added until a second backend
creates a real integration seam.

## First Game Application slices

The first headless milestone will contain:

- A versioned JScene3D Project Manifest
- A validated Doomed Corridors project descriptor
- A pinned and attributed Freedoom Phase 2 WAD source
- Headless WAD inspection and map/resource enumeration

The first visual milestone will import and render Freedoom Phase 2 `MAP01`.
Collision, thing spawning, combat, Doom actor behavior, sector actions, HUD, and
campaign progression then proceed as independently verifiable vertical slices.
The initial compatibility target excludes Boom, MBF, Hexen-format, UDMF, and
GZDoom extensions. Multiplayer, save games, scripting, generalized mod support,
and exact software-renderer reproduction also remain later work.

## Delivery sequence

The first six reusable foundation slices are complete:

1. Cursor capture, raw mouse support, focus state, and pointer-lock controls.
2. The collision-query foundation in `jscene3d-physics`.
3. Explicit kinematic movement, collision-sensor overlap transitions, and
   renderer-independent physics debug snapshots, proven by an interactive
   obstacle-course example in the separate `jscene3d-physics-examples` suite.
4. A game lifecycle, semantic input actions, and interpolated Physics Bindings,
   proven by an interactive first-person sandbox in the separate
   `jscene3d-game-examples` suite.
5. Reusable named sprite-atlas animations, independent animated-billboard
   playback, and observable frame, loop, and completion events.
6. Buffered Ogg Vorbis playback, positional effects, listener updates, and
   independent master, music, and effects volumes in `jscene3d-audio`, proven
   by the separate `jscene3d-audio-examples` suite using CC0 assets.

The remaining sequence is:

1. Establish Project Manifest version 1 and prove it with the separate Game
   Application.
2. Read and inspect the pinned Freedoom Phase 2 WAD headlessly.
3. Import and render `MAP01`, then add collision and thing spawning.
4. Build one complete combat loop before expanding Doom compatibility.
5. Progressively support the complete pinned Freedoom campaign without
   broadening reusable artifacts with title-specific concepts.
