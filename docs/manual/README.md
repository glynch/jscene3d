# JScene3D manual

This manual teaches JScene3D from its smallest rendering concepts through its
project and game architecture. It complements the root README, which focuses
on building and running the repository, and the architecture decision records,
which explain why individual design choices were made.

## Start here

1. [Rendering fundamentals](fundamentals.md) builds and animates a cube using
   the direct scene-graph API.
2. [Project and game fundamentals](project-fundamentals.md) explains how
   authored projects, worlds, entities, components, and runtime extensions sit
   above that rendering API.

The two layers are deliberately separate. A renderer `Scene` contains the
objects required to draw a frame. A project `World` contains live game entities
and behavior and may use a renderer scene as one part of its presentation.

## Reference material

- [Entity-component world architecture](../design/entity-component-world-architecture.md)
- [Architecture decisions](../adr/)
- [Repository examples](../../jscene3d-examples/src/main/java/io/github/glynch/jscene3d/examples/)
