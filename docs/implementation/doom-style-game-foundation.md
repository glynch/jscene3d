# Doom-style game foundation

This document defines the minimum reusable foundation for JScene3D's first Game
Application. "Doom-style" describes the intended first-person action and does
not name the eventual game or imply compatibility with Doom data or behavior.

## Artifact boundaries

The implementation will be divided by responsibility:

- `jscene3d-physics` is an original, pure-Java, renderer-independent Physics
  Engine. It contains no scene objects, rendering behavior, game rules, WAD
  concepts, or Doom-specific collision rules.
- `jscene3d-game` is a reusable, genre-independent Game Engine. It coordinates
  lifecycle, fixed and rendered updates, input actions, assets, game states,
  physics bindings, animation, audio, and rendering without defining a
  particular game's world or rules.
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

The first game will use an original compact level rather than load Freedoom WAD
levels. Direct WAD support would additionally require palette and colormap
decoding, texture patch composition, flats, sprites, map lumps, sectors,
linedefs, BSP nodes, blockmaps, actions, and compatibility behavior. If pursued
later, that work belongs in a separate optional WAD artifact or in the Game
Application, not in the Physics Engine or Game Engine.

## Existing rendering foundation

JScene3D already provides the rendering capabilities required by the initial
game: textured and generated geometry, alpha masking, transparency, fog,
lighting, instancing, custom per-instance attributes, custom shaders,
raycasting, overlays, GUI controls, glTF loading, animation, and line-based
debug visualization. The first game does not require PBR materials,
environment lighting, skeletal animation, post-processing, or shadows, even
though several are already supported.

## Remaining reusable foundation

### Platform input

The LWJGL platform layer needs a captured or disabled cursor mode, optional raw
mouse motion when GLFW supports it, observable focus state, and reliable input
and cursor restoration on focus loss. A small pointer-lock control should prove
yaw and pitch independently of any game.

### Physics Engine

The first `jscene3d-physics` slice needs fixed updates, static collision,
kinematic bodies, box, sphere, and capsule shapes, broad- and narrow-phase
collision, ray and overlap queries, shape sweeps, gravity, floor detection, wall
sliding, bounded step traversal, triggers, collision events, and debug lines.
These are general simulation capabilities; the player controller that combines
them into a particular movement model belongs in the Game Application or in a
general Game Engine controller only after its contract is proven reusable.

### Game Engine

The first `jscene3d-game` slice needs a lifecycle with separate fixed and
rendered updates, input action mapping, game-state transitions, asset lifetime
management, Physics Bindings with render interpolation, and reusable support
for sprite-frame animation and camera-facing billboard batches. It must not
define sectors, weapons, enemies, damage rules, or a Doom level format.

### Audio

A recognizable action game requires positional effects, non-positional user
interface effects, music, camera-listener updates, and separate master, music,
and effects volumes. This should be an optional concrete JScene3D audio artifact
backed initially by OpenAL. A multi-backend adapter hierarchy will not be added
until a second backend creates a real integration seam.

## First Game Application slice

The first playable milestone will contain:

- One original room-and-corridor level
- Selected and attributed Freedoom textures, sprites, sounds, and music
- Captured first-person camera movement and collision
- One billboard enemy with a small state machine
- One hitscan weapon
- Health and ammunition pickups
- One operable door and one exit trigger
- Damage, death, restart, and level completion
- A minimal health, ammunition, and weapon HUD

It will not initially include WAD compatibility, a BSP or portal renderer,
multiplayer, save games, scripting, mod support, jumping, crouching, navigation
meshes, dynamic rigid-body props, multiple weapons, or complex enemy AI.

## Delivery sequence

The first three reusable foundation slices are complete:

1. Cursor capture, raw mouse support, focus state, and pointer-lock controls.
2. The collision-query foundation in `jscene3d-physics`.
3. Explicit kinematic movement, trigger transitions, and renderer-independent
   physics debug snapshots, proven by an interactive obstacle-course example
   in the separate `jscene3d-physics-examples` suite.

The remaining sequence is:

1. Implement the lifecycle, input actions, and Physics Bindings in
   `jscene3d-game`.
2. Add reusable billboard animation and the initial audio artifact.
3. Build a one-room combat prototype in the separate Game Application.
4. Expand the prototype into the first compact level without broadening the
   reusable artifacts with title-specific concepts.
