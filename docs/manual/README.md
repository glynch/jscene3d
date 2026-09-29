# JScene3D manual

This manual teaches JScene3D from its smallest rendering concepts through its
project, runtime, and editor architecture. It complements the root README,
which focuses on building and running the repository, and the architecture
decision records, which explain why individual design choices were made.

## Start here

1. [Rendering Fundamentals](fundamentals.md) builds and animates a cube using
   the renderer-level scene model.
2. [Project and Game Fundamentals](project-fundamentals.md) introduces authored
   projects, worlds, entities, components, and the live runtime model.
3. [Editor Fundamentals](editor-fundamentals.md) explains how the Code OSS
   editor presents and safely edits that authored project model.

```text
renderer-level scene model
        ↓
authored project and runtime model
        ↓
Code OSS editor and authoring workflow
```

Each layer builds on the concepts before it while retaining a distinct
responsibility.

## Reference material

- [Entity-component world architecture](../design/entity-component-world-architecture.md)
- [JScene3D Editor architecture](../design/editor-architecture.md)
- [Architecture decisions](../adr/)
- [Repository examples](../../jscene3d-examples/src/main/java/io/github/glynch/jscene3d/examples/)
