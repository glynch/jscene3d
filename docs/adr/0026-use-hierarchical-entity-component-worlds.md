# Use hierarchical entity-component worlds

JScene3D will represent authored game objects as reusable `EntityDefinition`
assets and live objects as component-composed `Entity` instances in a `World`.
The entity hierarchy owns identity and lifetime, while typed components provide
spatial, rendering, physics, audio, animation, and game behavior. This combines
the useful composition and hierarchy properties demonstrated by Unity and
Godot without adopting typed-node inheritance, a flat data-oriented ECS, or a
second public model-instance hierarchy. It also gives static placement and
runtime spawning one validated transactional composition path. The complete
initial contract is defined in
[`entity-component-world-architecture.md`](../design/entity-component-world-architecture.md).
