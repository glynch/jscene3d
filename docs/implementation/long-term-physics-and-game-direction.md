# Long-term physics and game direction

This document records JScene3D's intended direction beyond the current graphics
library. It is not a version 0.1 commitment or a fixed release schedule.

## Artifact direction

`jscene3d-physics` will provide an original, pure-Java, renderer-independent
physics engine. Its public model includes `PhysicsWorld`, `StaticBody`,
`KinematicBody`, `Collider`, `CollisionSensor`, Collision Shapes, explicit Fixed
Updates, and a reusable `CharacterController`. Dynamic `RigidBody` simulation
will extend that model in a later slice. Physics does not directly mutate
`Object3D` or treat rendered geometry as an implicit collision shape.

`jscene3d-game` provides the higher-level 3D game runtime. Its initial slice
coordinates application lifecycle, fixed and rendered updates, semantic input
mapping, and interpolated Physics Bindings. Later slices may add game states and
asset lifetime management when their concrete requirements are defined. It
remains genre independent and does not contain the rules, world model, content
formats, or assets of any particular game.

Each playable title will be a separate Game Application artifact that depends on
the reusable Game Engine and Physics Engine. The first planned title is a
Doom-style FPS, but neither `jscene3d-game` nor `jscene3d-physics` will contain
Doom-specific concepts. The application artifact will own its level model,
combat rules, enemies, weapons, pickups, doors, presentation, and third-party
content attribution.

The initial artifact split does not prevent later integrations with established
physics engines. Such integrations must preserve the same separation between
simulation, scene representation, and rendering.

## Initial physics capability profile

The first useful physics slice will establish general collision and kinematic
capabilities that can support many game genres while enabling the first Game
Application:

- Caller-driven fixed updates with explicit kinematic moves
- Static Bodies and Kinematic Bodies that own one or more Colliders
- Box, sphere, and capsule Collision Shapes
- Broad-phase spatial queries and narrow-phase collision detection
- Ray, overlap, and shape-sweep queries
- Gravity, floor detection, wall sliding, and bounded step traversal
- Non-blocking Collision Sensors with deterministic overlap events
- Collision contacts and overlap events that identify the Collider and its
  owning collision object
- Debug visualization through JScene3D line rendering

Visual physics examples live in the unpublished `jscene3d-physics-examples`
artifact. They use the shared `jscene3d-example-framework` browser without
introducing renderer or GUI dependencies into `jscene3d-physics`. Rendering and
asset-loading examples remain isolated in `jscene3d-examples`.

Game-runtime integration examples live in the unpublished
`jscene3d-game-examples` artifact and use their own shared-framework browser.
The first-person sandbox proves semantic input, deterministic character motion,
and interpolated presentation without placing application rules in a reusable
artifact.

Dynamic Rigid Bodies, forces, mass, friction, restitution, contact resolution,
constraints, continuous collision detection, sleeping, and more advanced
solvers remain part of the general Physics Engine direction. They follow the
first collision and kinematic slice rather than blocking the first playable
application.

## Near-term sequencing

The next reusable foundation is captured in
`doom-style-game-foundation.md`. Weighted animation blending and cross-fading
already provide transition support for future Game Applications. Root motion
and animation events remain later graphics-library capabilities and should be
added only with focused runtime requirements and examples.
