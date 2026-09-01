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
for smooth presentation.

The initial artifact split does not prevent later integrations with established
physics engines. Such integrations must preserve the same separation between
simulation, scene representation, and rendering.

## Initial physics capability profile

The first useful physics slice should include:

- A fixed-timestep Physics World
- Static and dynamic Rigid Bodies
- Sphere and box Collision Shapes
- Gravity, forces, mass, velocity, friction, and restitution
- Broad-phase and narrow-phase collision detection
- Contact generation and resolution
- Collision events
- Debug visualization through JScene3D line rendering

Kinematic bodies, additional shapes, constraints, continuous collision
detection, sleeping, and more advanced solvers can be considered after the
initial profile is correct and well tested.

## Near-term sequencing

Animation blending and cross-fading remain the next graphics-library feature.
Their design should leave room for later root motion and animation events needed
by the Game Engine without adding those capabilities prematurely.
