# Long-term physics and game direction

This document records JScene3D's intended direction beyond the current graphics
library. It is not a version 0.1 commitment or a fixed release schedule.

## Artifact direction

`jscene3d-physics` will provide an original, pure-Java, renderer-independent
physics engine. Its public model will center on a Physics World, Rigid Bodies,
Collision Shapes, and explicit Fixed Updates. It will not directly mutate
`Object3D` or treat rendered geometry as an implicit collision shape.

`jscene3d-game` will provide the higher-level 3D game runtime. It will coordinate
application lifecycle, fixed and rendered updates, game states, input mapping,
asset management, animation, physics, and rendering. Physics Bindings in this
layer will transfer simulated transforms to scene objects and interpolate them
for smooth presentation. It will remain genre independent and will not contain
the rules, world model, content formats, or assets of any particular game.

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

- A fixed-timestep Physics World
- Static Colliders and Kinematic Bodies
- Box, sphere, and capsule Collision Shapes
- Broad-phase spatial queries and narrow-phase collision detection
- Ray, overlap, and shape-sweep queries
- Gravity, floor detection, wall sliding, and bounded step traversal
- Trigger volumes
- Collision events
- Debug visualization through JScene3D line rendering

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
