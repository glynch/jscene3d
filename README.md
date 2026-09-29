# JScene3D

JScene3D is a modular 3D/game engine and authoring platform for Java 21. It
combines a renderer-independent scene graph, an OpenGL desktop renderer,
reusable game-runtime facilities, descriptor-authored projects, and a Code
OSS-based JScene3D Editor.

The project is intended for Java applications that need both a programmable
engine and structured, toolable game content without moving project semantics
or rendering into a browser-only stack.

> **Work in progress:** JScene3D is under active development and has not
> reached version 1.0. APIs, project formats, editor behavior, and module
> boundaries may change without backward compatibility before 1.0. **Platform
> status:** macOS ARM64 is currently the only verified platform.

## What JScene3D includes

JScene3D is developed as three related areas:

- **Engine** — a Java scene graph, OpenGL renderer, assets and animation,
  physics, input, audio, and application-runtime facilities.
- **Project and authoring platform** — authored worlds, hierarchical entities
  and components, extension descriptors, imports, validation, and safe
  source-preserving document editing.
- **JScene3D Editor** — a Code OSS-based workbench that presents Java-owned
  project and authoring state through familiar IDE surfaces.

The repository is a multi-module Maven project. See the
[module overview](docs/design/module-overview.md) for the modules grouped by
responsibility.

## Capabilities

### Rendering and assets

- Renderer-independent scenes and object hierarchies with an OpenGL desktop
  backend.
- Built-in materials, custom shaders, lighting, shadows, fog, HDR environment
  lighting, and tone mapping.
- glTF and GLB loading, textures and texture transforms, skeletal and keyframe
  animation, blending, morph targets, and instancing.
- Lines, helpers, generated geometry, sprites, billboards, and raycasting.

See [Rendering](docs/manual/rendering.md) and
[Assets and animation](docs/manual/assets-and-animation.md).

### Runtime

- Collision and physics integration, character movement, sensors, and debug
  geometry.
- Fixed-step and frame-update scheduling, semantic input, and reusable game
  runtime components.
- Positional audio, music and effects mixing, desktop project hosting, and
  application export.

See [Application runtime](docs/manual/application-runtime.md).

### Projects and authoring

- `WorldDefinition` and `EntityDefinition` content composed from hierarchical
  entities and components.
- Inert extension descriptors that describe title-specific types without
  loading arbitrary runtime implementations into the editor.
- Authored definitions alongside read-only generated and imported content.
- Java-owned validation, diagnostics, revisions, and source-preserving working
  copies.

See [Project and game fundamentals](docs/manual/project-fundamentals.md).

## JScene3D Editor

The JScene3D Editor uses Code OSS for the mature workbench while Java remains
authoritative for project discovery, definitions, validation, working copies,
mutations, persistence, and recovery. The frontend communicates with a
persistent Java authoring service instead of reimplementing JScene3D semantics
in TypeScript.

Current editor surfaces include **JScene3D Project**, **Hierarchy**,
**Inspector**, **Problems**, and authored-definition editors. Authored
definitions use source-preserving Java-owned working copies; generated
definitions are read-only. Simple Inspector property editing is currently
being integrated and refined, while unsupported projected values remain
read-only.

The native rendering direction uses the real Java renderer:

```text
JScene3D / LWJGL / OpenGL
        ↓
native shared surface
        ↓
Electron / Chromium
```

The standalone Electron/LWJGL proof of concept established this shared
rendering path, and the Code OSS proof of concept established that it can work
inside Code OSS. The active editor extension does **not** yet mount the native
viewport as a user-facing surface.

Read [Editor fundamentals](docs/manual/editor-fundamentals.md) for the authoring
workflow and [JScene3D Editor architecture](docs/design/editor-architecture.md)
for the technical boundaries and rendering integration.

## Quick start

Requirements:

- Java 21 or newer.
- The checked-in Maven Wrapper; a system Maven installation is not required.

Build and verify the repository:

```shell
./mvnw clean verify
```

Launch the searchable rendering and asset example browser:

```shell
./tools/scripts/run-example.sh ExampleBrowser
```

List every supported example entry point:

```shell
./tools/scripts/run-example.sh --list
```

See [Getting started](docs/manual/getting-started.md) for setup, verification,
and native smoke-test guidance.

## Examples

The repository includes native example browsers for rendering and asset
loading, physics, game-runtime facilities, and audio. Representative examples
cover the Solar System Viewer, lighting and shadows, glTF models, skeletal and
keyframe animation, character physics, semantic game input, and positional
audio.

Examples can run through a searchable browser or as focused standalone
programs. See [Examples](docs/manual/examples.md) for the available suites,
launch commands, interaction, and thumbnail maintenance.

## Project model

JScene3D keeps the renderer and project models distinct:

```text
Scene / Object3D
    renderer-level model

World / Entity / Component
    live project and game state

WorldDefinition / EntityDefinition
    authored or generated data
```

Definitions describe content; composition turns them into live worlds and
entities. Components compose behavior without requiring a title-specific
entity subclass hierarchy.

Start with [Project and game fundamentals](docs/manual/project-fundamentals.md),
then see the
[entity-component world architecture](docs/design/entity-component-world-architecture.md)
for the complete design.

## WAD and Doom integration

JScene3D includes generic WAD archive support plus classic Doom content
discovery, decoding, and project importing. Imported maps, definitions, and
resources enter the normal JScene3D project model as published generated
content rather than creating a separate runtime architecture.

See [WAD and Doom integration](docs/design/wad-doom-integration.md) for module
responsibilities, validation, provenance, publication, and runtime boundaries.

## Documentation

### Start here

- [Manual](docs/manual/README.md)
- [Getting started](docs/manual/getting-started.md)
- [Rendering fundamentals](docs/manual/fundamentals.md)
- [Project and game fundamentals](docs/manual/project-fundamentals.md)
- [Editor fundamentals](docs/manual/editor-fundamentals.md)

### Reference

- [Rendering](docs/manual/rendering.md)
- [Assets and animation](docs/manual/assets-and-animation.md)
- [Application runtime](docs/manual/application-runtime.md)
- [Examples](docs/manual/examples.md)

### Architecture

- [Entity-component world architecture](docs/design/entity-component-world-architecture.md)
- [JScene3D Editor architecture](docs/design/editor-architecture.md)
- [Module overview](docs/design/module-overview.md)
- [WAD and Doom integration](docs/design/wad-doom-integration.md)
- [Architecture decision records](docs/adr/)

## License

JScene3D is licensed under the [Apache License 2.0](LICENSE). Third-party
components and bundled assets are documented in
[Third-party notices](THIRD_PARTY_NOTICES.md).
