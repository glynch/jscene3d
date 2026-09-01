# Separate physics simulation from game integration

JScene3D will pursue an original, pure-Java physics implementation as its
primary direction while leaving room for optional third-party adapters. The
renderer-independent `jscene3d-physics` artifact will own simulation concepts;
it will not mutate `Object3D` instances or depend on render-frame timing.

The optional `jscene3d-game` artifact will coordinate the eventual game runtime
and own Physics Bindings between Rigid Bodies and scene objects. This boundary
keeps physics independently testable, permits fixed simulation updates with
interpolated rendering, and prevents scene-graph and rendering concerns from
becoming part of the physics model.

The first physics capability profile will be deliberately bounded rather than
attempting immediate parity with mature general-purpose engines. It will begin
with static and dynamic bodies, sphere and box shapes, gravity and forces,
broad-phase and narrow-phase collision detection, contact resolution, friction,
restitution, collision events, and line-based debug visualization.
