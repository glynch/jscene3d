# Long-term physics and game direction

This document records the intended direction beyond the current graphics
library. The accepted authoring and runtime model is defined by the
[entity-component world architecture](../design/entity-component-world-architecture.md).

## Physics module

`jscene3d-physics` remains an original, pure-Java, renderer-independent physics
module. Its public model includes a Physics World, collision objects with one
or more shapes, static bodies, explicitly moved character/kinematic bodies,
non-blocking sensors/areas, spatial queries, and eventually dynamic rigid-body
simulation.

Physics does not own `Entity`, mutate renderer `Object3D` instances, infer
collision from visible geometry, or decide game responses. Entity physics
components adapt stable entity/component identities, transforms, and authored
shape membership into physics objects owned by the world's physics module.

Each spatial entity has one declared transform authority:

- static bodies and sensors consume entity transform state;
- character/kinematic behavior requests movement through physics;
- rigid bodies produce authoritative simulation state;
- presentation may interpolate physics state for rendering.

This seam leaves room for optional third-party physics adapters without making
one backend's handles or lifecycle part of the entity model.

## Game and world runtime

`jscene3d-game` remains genre-independent. It provides host lifecycle, fixed and
frame timing, semantic input, and coordination needed to run a `World`. World
composition owns entity/component lifecycle, scheduling, signals, structural
mutation, resource leases, and module access.

The game layer contains no rules, content formats, or assets belonging to a
particular game. Each playable title is a separate Game Application whose
`ProjectManifest` selects its startup `WorldDefinition`, game module, assets,
and import configuration.

## Physics capability direction

The first useful profile includes:

- fixed-step simulation;
- static and character/kinematic collision bodies;
- collision objects containing multiple locally transformed shapes;
- box, sphere, capsule, and triangle-mesh shapes as supported;
- broad- and narrow-phase collision detection;
- ray, overlap, and shape-sweep queries;
- gravity, floor detection, wall sliding, and bounded step traversal;
- non-blocking sensors with typed enter, stay, and exit signals;
- contact/overlap results identifying precise shapes and owning objects;
- renderer-independent debug snapshots.

Dynamic rigid bodies, forces, mass properties, friction, restitution, contact
resolution, constraints, continuous collision detection, and sleeping extend
this model later. They do not block Beacon Garden's first architecture slice.

## Verification

Physics remains testable through its own interface without constructing a
renderer or game world. Entity/physics integration is tested separately through
world composition and component interfaces. Visual physics examples use host
adapters and debug rendering without adding rendering dependencies to the
physics module.

Beacon Garden first proves one explicit multi-shape sensor and typed overlap
signal. Doomed Corridors later validates generated static map collision,
character movement, projectiles, pickups, doors, lifts, and teleport regions
through the same model.

## Later capabilities

Weighted animation blending and cross-fading already provide transition support
for future game applications. Root motion, animation events, broader streaming,
save-state persistence, networking, and specialized high-volume simulation are
added only when concrete requirements define their interfaces.
