# Game project, scene, extension, and import architecture

Status: first discussion draft. This document is intentionally non-normative.
It records a proposed direction for review and interrogation before any of the
described interfaces or formats are implemented. It does not supersede
[`game-projects-and-wad-import.md`](game-projects-and-wad-import.md) or approve
implementation work.

## Purpose

JScene3D needs a project model that supports both code-first developers and a
future visual editor. The same project must be loadable by a standalone game,
headless tools, automated tests, and the editor. Doomed Corridors is the first
demanding consumer, while glTF and Doom-compatible WAD files provide concrete
import cases.

JScene3D should support declarative scenes, reusable scene instances, shared
resources, semantic input actions, and custom code. It should improve
discoverability by keeping project-authored composition, configuration, and
connections in inspectable data wherever practical.

## Core principle

Every project-authored decision required to reproduce a game should be stored
in project data or referenced source assets. The future editor must read and
write that same data rather than maintain a parallel editor-only model.

This principle does not mean that arbitrary Java algorithms must be converted
into visual graphs. Java remains a first-class way to implement behavior. The
project records that a registered Java-backed type is used and records its
editable configuration. The provider of that type supplies its implementation,
validation schema, defaults, and editor metadata.

Consequently:

- project structure and configuration are reproducible by the editor;
- Java algorithms remain ordinary source code;
- a project can use Java code attached to a scene node;
- a project can also use Java code with project-wide or import-time scope;
- headless and graphical callers observe the same project definitions;
- running or editing a project never depends on reverse-engineering Java code.

## Goals

- Keep the Project Manifest small, stable, safe to inspect, and independent of
  graphics, audio, importing, and Game Provider execution.
- Define a native, versioned scene format capable of describing a complete game
  composition rather than only an `Object3D` hierarchy.
- Reuse scenes through scene instantiation instead of introducing a separate
  prefab format prematurely.
- Support shared native resources without duplicating them at every use site.
- Support node-scoped behavior and project-wide Java systems explicitly.
- Let Java providers register types through stable identifiers rather than
  serializing implementation class names.
- Make registered types discoverable and configurable by the future editor.
- Preserve serialized project-authored event connections where those
  connections are part of the game design.
- Make project layout directory-agnostic while offering optional scaffold
  conventions.
- Establish one import orchestration model for source formats such as glTF and
  Doom-compatible WADs.
- Distinguish an editor's in-process 3D edit viewport from playable preview,
  running the current scene, and running the complete project.
- Define reproducible export presets that package project content, Java code,
  dependencies, a Java runtime, and platform-native libraries.
- Establish a deliberately bounded version 0.1 catalog of 3D scene entries,
  resources, and runtime systems without making the scene schema depend on one
  renderer implementation.
- Keep imported sources authoritative, imports deterministic, and derived
  output disposable.
- Allow project-authored content to extend imported content without being
  destroyed by reimport.
- Provide supported WAD capabilities without putting Doom concepts in
  `jscene3d-game` or `jscene3d-physics`.
- Keep expected loading and import failures as structured diagnostics suitable
  for command-line tools and editor navigation.

## Non-goals

- Require `main/`, `game/`, `scenes/`, `assets/`, or any other directory names.
- Make all Java implementation details editable as data.
- Infer editor controls by reflecting over arbitrary Java classes.
- Introduce a visual programming language in the first version.
- Make Doom rules, actors, sectors, weapons, or linedef semantics part of the
  genre-independent Game Engine or Physics Engine.
- Guarantee that every WAD contains Doom-compatible data. A WAD is first an
  archive of named lumps; interpretation is supplied separately.
- Download and execute untrusted extensions merely to display basic Project
  Manifest metadata.
- Define final filenames or Java type signatures in this discussion draft.

## Terminology proposed by this draft

### Project Manifest

The root `project.json` containing safe descriptive metadata and references to
the resources needed to open or run the project. It locates content; it does not
contain the complete game.

### Project resource

A versioned, project-authored document or directly usable asset identified by a
stable project-relative reference. Examples include scenes, input maps, themes,
combat rules, and imported-source declarations.

### Source asset

An authoritative external asset such as a WAD, glTF file, image, or audio file.
An importer reads it without modifying it.

### Imported resource

A deterministic runtime or editor representation derived from a source asset,
import settings, importer identity, and importer version. It retains provenance
and may be regenerated.

### Edit viewport

An editor-owned rendering surface used to inspect and manipulate authored scene
data. It may use an editor camera, selection overlays, transform gizmos, and
preview lighting that are not part of the saved game scene. It is not a running
game.

### Play current scene

A playable runtime session whose entry point is the scene currently being
edited. This is a fast test path and may need a small editor-supplied harness
for project systems normally created by the application entry scene.

### Play project

A playable runtime session that starts from the Project Manifest's configured
entry scene and uses normal project startup semantics.

### Export preset

A versioned project resource describing one distribution target and the
content, Java runtime, dependencies, native libraries, launcher configuration,
and packaging options included in that target. Credentials and machine-local
signing state are not part of the shared preset.

### Scene definition

A project resource that describes an instantiable hierarchy of typed entries,
their properties, child relationships, resource references, and declared event
connections. A scene definition can represent a whole application screen, a
gameplay world, a user interface, or one reusable actor.

### Scene instance

An entry that instantiates another scene definition. This provides prefab-like
reuse without a second, nearly identical format.

### Registered type

A stable type identifier contributed by JScene3D or a Java extension. Its
descriptor defines where the type may be used, its editable properties,
validation rules, defaults, events, actions, and editor presentation.

### Behavior

A registered type applied to one scene entry or scene instance. Its lifetime is
associated with that instance.

### Project system definition

A registered Java-backed type instantiated once for a running project or game
session rather than attached artificially to a scene entry. Examples include
combat coordination, campaign progression, save management, or a project-wide
inventory.

### Java extension

A packaged Java contribution that registers behaviors, project systems,
resource types, importers, or editor descriptors. A Game Provider is the
project's principal Java extension and may depend on additional extensions.

### Adapter

A concrete implementation at a format or integration seam. The glTF and WAD
import adapters are concrete examples at the source-import seam.

## Directory independence

Neither schemas nor runtime code should infer meaning from directory names. All
required locations are explicit references starting at `project.json`.

These projects should be equivalent:

```text
project.json
main.scene.json
player.scene.json
```

```text
project.json
application/main.scene.json
actors/player.scene.json
```

An optional scaffold may suggest a layout such as the following because
conventions improve navigation, but the layout is not part of the project
interface:

```text
project.json
scenes/
actors/
ui/
input/
imports/
assets/
```

Renaming or reorganizing these directories should require updating references,
not changing Java code.

## Project Manifest direction

The existing Project Manifest version 1 correctly separates project metadata
from `schemaVersion`, engine compatibility, runtime selection, and source
assets. It should remain safe to load without executing extensions or importing
assets.

The current startup pair of source asset and target was useful for proving WAD
loading, but it should evolve toward an engine-native entry scene. Source asset
and target selection belong in an import declaration rather than serving as the
permanent application entry point.

A possible future shape is:

```json
{
  "$schema": "schema/project-2.schema.json",
  "schemaVersion": 2,
  "identity": {
    "id": "io.github.glynch.doomed-corridors",
    "name": "Doomed Corridors",
    "version": "0.1.0-SNAPSHOT",
    "icon": "branding/icon.png"
  },
  "engine": {
    "requires": ">=0.1.0 <0.2.0"
  },
  "runtime": {
    "gameProvider": "io.github.glynch.doomed-corridors",
    "entryScene": "application/main.scene.json",
    "configuration": "game/runtime.json",
    "inputMap": "input/default.json"
  },
  "extensions": [
    {
      "id": "io.github.glynch.jscene3d.doom-format",
      "requires": ">=0.1.0 <0.2.0"
    }
  ],
  "assets": [
    {
      "id": "freedoom",
      "type": "wad",
      "path": "assets/freedoom2.wad",
      "sha256": "..."
    }
  ],
  "imports": ["imports/freedoom-map01.import.json"]
}
```

This is an illustrative example rather than a proposed final schema. Questions
still to resolve include whether extension requirements belong in the Project
Manifest, an application package descriptor, or both, and whether a single Game
Provider remains sufficient.

## Native scene model

### One reusable scene concept

The initial design should use one scene-definition format for both top-level
scenes and reusable fragments. A player, door, HUD, menu, and complete game can
all be scenes at different scales. An explicit scene instance provides reuse.

Introducing both `scene` and `prefab` formats would duplicate identity,
properties, resource references, nesting, validation, overrides, and event
connections before a demonstrated semantic difference exists.

### Broader than an `Object3D` tree

A complete game composition contains more than rendered spatial objects. It may
also contain collision descriptions, audio emitters, user-interface elements,
timers, behaviors, and references to project systems. Therefore the serialized
scene model should not assume that every entry is an `Object3D`.

The scene loader can create `Object3D` instances for registered spatial types,
but the serialized hierarchy represents game composition. Whether spatial and
user-interface entries ultimately share one root abstraction is an open design
question and should be proven with both a 3D world and a menu/HUD composition.

### Stable identities

Every addressable scene entry needs a stable identifier independent of its
display name and list position. Importers also need deterministic identifiers
derived from source identities, such as a WAD map name plus thing, linedef, or
sector index.

Names may change without breaking connections. Reordering children must not
change identity. Randomly regenerated identifiers must not make deterministic
imports appear modified on every run.

### Illustrative scene document

```json
{
  "$schema": "../schema/scene-1.schema.json",
  "schemaVersion": 1,
  "id": "main",
  "root": {
    "id": "application",
    "type": "jscene3d/game-root",
    "children": [
      {
        "id": "world",
        "instance": "../maps/map01.scene.json"
      },
      {
        "id": "player",
        "instance": "../actors/player.scene.json"
      },
      {
        "id": "hud",
        "instance": "../ui/hud.scene.json"
      }
    ]
  },
  "connections": [
    {
      "from": "system:combat/player-health-changed",
      "to": "node:hud/set-health"
    }
  ]
}
```

This example intentionally shows concepts, not settled connection syntax. A
final design must define endpoint types, payload compatibility, multiplicity,
lifecycle, connection ordering, and diagnostic behavior.

### Instance overrides

Scene instances need bounded overrides so a reusable definition can be
configured at each use site. Overrides should address stable property paths
declared editable by the registered type.

```json
{
  "id": "guard-west",
  "instance": "../actors/zombieman.scene.json",
  "overrides": {
    "transform.position": [-128, 0, 256],
    "awareness.sightRange": 1536
  }
}
```

Arbitrary structural patching should not be included initially. It complicates
validation, inheritance, reimporting, and editor presentation. Structural
variants can be represented as another scene that instantiates and composes
smaller scenes until a real use case proves that structural overrides are
necessary.

### Resources and sharing

Scenes should reference resources by stable logical reference. Loading the same
resource reference twice should preserve sharing where the resource's ownership
and mutability interface permits it.

```json
{
  "id": "zombieman-visual",
  "type": "jscene3d/animated-billboard",
  "properties": {
    "animations": "resource:actors/zombieman-animations",
    "material": "resource:materials/actor-sprite"
  }
}
```

The final design must distinguish project-relative file references, declared
resource identifiers, imported-resource references, and runtime-only object
references. A URI-like syntax may make those categories explicit, but it should
not be adopted until resolution and error behavior are specified.

## Three-dimensional scene and resource model

Godot is useful here as an established product reference, not as a format or
API to reproduce. Its stable documentation separates a hierarchical 3D entry
with a local transform from reusable resource data, and separates the editor's
3D viewport from the camera and environment that ship with the game. It also
uses the same saved-scene concept for a whole level and an instanced reusable
object. These observations support, but do not by themselves settle, the scene
and resource decisions in this draft. See Godot's
[introduction to 3D](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html),
[nodes and scenes](https://docs.godotengine.org/en/stable/getting_started/step_by_step/nodes_and_scenes.html),
and [Resource](https://docs.godotengine.org/en/stable/classes/class_resource.html)
documentation.

### Entries declare composition; resources hold reusable data

The JScene3D scene format should model visible composition through typed scene
entries. A spatial entry has a parent-relative transform and may own children,
but mesh data, materials, textures, animation clips, audio clips, and collision
shapes should be reusable resources referenced by entries. This prevents every
instance from duplicating large data and lets an importer emit one resource
that many authored entries can use.

This separation is more important than matching another engine's type names.
For example, a mesh instance entry might reference a mesh resource and an
optional material override. It should not embed renderer buffers or require the
serialized type to be the concrete `jscene3d-core` `Mesh` class. A runtime
adapter resolves the registered scene type and resource types to the existing
JScene3D objects.

Resource sharing needs an explicit mutability contract. Immutable imported
meshes, textures, clips, and collision shapes are natural shared values.
Mutable per-instance animation state, playback position, physics pose, and
material parameter overrides are runtime instance state and must not mutate a
shared project resource accidentally.

### Baseline 3D capability families

The native model should have a place for the following capability families.
The last column is a recommendation for project-format version 0.1, not a
claim about Godot's scope.

| Capability | Scene composition | Reusable or derived data | Recommended version 0.1 scope |
| --- | --- | --- | --- |
| Spatial hierarchy | Transform-bearing entries and scene instances | None beyond scene data | Required |
| Mesh rendering | Mesh instance and instanced-mesh entries | Geometry, material, texture, and instance transforms | Mesh instance required; instanced mesh supported where the renderer already can |
| Sprite in 3D | Billboard and animated-billboard entries | Image regions and sprite animation sets | Required for Doomed Corridors |
| Camera and viewport | Perspective or orthographic camera entry assigned to an output | Render-target and viewport settings | One game viewport and active camera required; sub-viewports later |
| Lighting and environment | Ambient, hemisphere, directional, point, and spot light entries; scene environment reference | Environment maps and environment settings | Existing light types plus a minimal environment resource |
| Materials and shaders | Material references and bounded per-instance overrides | Standard and shader material resources | Existing materials and inspectable custom spatial shaders required |
| Animation | Animation player or behavior references clips and targets | Transform, morph, skeletal, and sprite clips | Existing transform, morph, skeletal, and sprite playback; advanced blend graphs later |
| Physics and collision | Static body, kinematic body, sensor, and character configuration | Primitive and generated collision shapes | Existing supported shapes and character motion required |
| Audio | Listener and spatial or non-spatial source entries | Audio clips and playback configuration | Existing audio capabilities required |
| User interface | Non-spatial hierarchy for menus and HUD | Images, fonts, themes, and layout data | Small HUD and menu composition proof required |
| Navigation | Regions, links, agents, and obstacles connected to a navigation world | Navigation mesh or graph derived data | Long-term; custom game navigation remains possible |
| Particles | Emitter entries and collision or attraction entries | Emitter settings, materials, and particle shaders | Long-term |
| Visibility optimization | Per-instance ranges, hierarchy proxies, occluders, and renderer settings | LOD meshes and baked occlusion data | Long-term and renderer-capability driven |
| Level construction | Imported geometry, generated geometry, or specialized authoring entries | Meshes, collision, navigation, and source provenance | Import and generated geometry now; CSG, grids, and terrain later |

Godot's documentation illustrates why these families should not collapse into
one generic property bag. A `MeshInstance3D` references a mesh resource, while
`MultiMeshInstance3D` provides many instances of one mesh with important shared
material and group-culling tradeoffs. Standard materials cover common artist
needs, while shaders expose a different programming and parameter surface.
See [procedural geometry](https://docs.godotengine.org/en/stable/tutorials/3d/procedural_geometry/index.html),
[MultiMesh optimization](https://docs.godotengine.org/en/stable/tutorials/3d/using_multi_mesh_instance.html),
[StandardMaterial3D](https://docs.godotengine.org/en/stable/tutorials/3d/standard_material_3d.html),
and the [shading language](https://docs.godotengine.org/en/stable/tutorials/shaders/shader_reference/shading_language.html).

Registered type descriptors should preserve these meaningful distinctions
while hiding renderer construction details. A mesh instance descriptor can
require a mesh reference; a camera descriptor can expose projection and clip
settings; a light descriptor can expose color, intensity, range, and shadow
settings. Editor controls can then be specific and valid without knowing the
Java implementation class.

### Meshes and instancing

A mesh resource describes geometry; it is not a scene entry. Version 0.1 should
represent the capabilities already established by `BufferGeometry`: named
vertex attributes, optional indices, draw ranges, bounds, morph targets, and
one or more material-bearing surfaces once the project resource representation
is designed. Importers and Java extensions may produce mesh resources, but GPU
buffer identifiers and renderer allocation details are never serialized.

A mesh-instance entry gives one mesh a transform, visibility, material
assignments or overrides, shadow policy, and an explicit or derived bounds
policy. Many entries may reference the same mesh and materials. Mutable
per-instance state must not alter those shared resources unless the author has
explicitly chosen a shared edit.

An instanced-mesh entry is an optimization with a different interface, not
shorthand for thousands of fully independent scene entries. JScene3D's
existing `InstancedMesh` supports a fixed-capacity batch, an active instance
count, local transforms, optional colors, custom numeric attributes, morph
influences, and batch or per-instance bounds. Version 0.1 project data should
be able to reference authored, imported, or generated instance data that maps
to those supported capabilities.

All instances in a batch share geometry and material structure, render
callbacks occur for the batch, and transparent instances cannot be sorted as
independent objects. An instance that needs its own children, behavior,
event connections, audio, physics body, or independently ordered transparent
rendering should normally be an ordinary scene instance instead. The editor
may offer a populate or conversion tool, but its result must be inspectable
instance data rather than hidden editor state. Godot's
[procedural geometry](https://docs.godotengine.org/en/stable/tutorials/3d/procedural_geometry/index.html)
and [MultiMesh documentation](https://docs.godotengine.org/en/stable/tutorials/3d/using_multi_mesh_instance.html)
illustrate the same resource, scene-instance, and batched-instance distinction.

### Materials and shaders

Version 0.1 should support the existing basic, normal, Lambert, Phong,
physically based standard, line, and custom shader material families through
registered resource descriptors. Shared render state includes visibility,
alpha mode and cutoff, face selection, depth testing, depth writing, and depth
comparison. A standard material exposes only the texture channels and numeric
properties its runtime implementation actually honors.

Custom shaders are already a JScene3D capability and should not be hidden from
the authoring model. A version 0.1 shader resource should declare:

- the source language and version, initially `glsl-330-core`;
- vertex and fragment stage sources or project-relative source references;
- required standard vertex attributes;
- whether renderer-managed instance transforms and colors are consumed;
- custom per-instance numeric attributes;
- typed uniforms, defaults, ranges, color-space meaning, and resource
  constraints;
- renderer-managed transform uniforms that project code cannot redefine;
- compatible renderer capabilities and structured compilation diagnostics.

The explicit language identifier matters. Raw GLSL 3.3 is an OpenGL renderer
resource, not automatically portable project data. A future renderer can
support another source variant, a common intermediate representation, or an
engine shading language without changing the scene entry that references the
material. The editor should be able to edit declared uniforms and show shader
compilation errors, but version 0.1 does not require a visual shader graph,
compute shaders, custom render passes, particle shaders, sky or fog shaders,
or a post-processing graph. Godot's separation of standard materials from
programmable shader types is a useful product reference; see
[StandardMaterial3D](https://docs.godotengine.org/en/stable/tutorials/3d/standard_material_3d.html)
and [Introduction to shaders](https://docs.godotengine.org/en/stable/tutorials/shaders/introduction_to_shaders.html).

### Cameras, viewports, environments, and editor aids

A saved game camera and an editor camera are different objects. The editor
needs its own freely navigable camera even when the scene has no game camera.
Likewise, editor preview lighting can make an unlit scene editable but must not
silently become runtime lighting. Godot explicitly keeps its preview sun and
environment out of a running or exported scene, which is the behavior JScene3D
should adopt. See the [Godot 3D workspace](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html#d-workspace)
and [preview environment and light](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html#preview-environment-and-light).

Version 0.1 should support one primary output viewport and a selected active
camera. The data model must not make that a permanent one-camera restriction.
Later sub-viewports are needed for picture-in-picture, security monitors,
mirrors, minimaps, and off-screen rendering. Godot's `Viewport` is a useful
reference because it owns a drawing surface, input region, world, cameras, and
audio listener rather than being only a rectangle. See the official
[Viewport reference](https://docs.godotengine.org/en/stable/classes/class_viewport.html).

Environment data should be an explicit resource selected by a world or camera,
not a collection of invisible renderer globals. The first resource may expose
only features JScene3D implements today. Background, ambient lighting,
environment maps, tone mapping, fog, and post-processing can evolve through a
versioned descriptor without changing scene hierarchy semantics. Godot uses an
environment resource in a similar role; see
[Environment and post-processing](https://docs.godotengine.org/en/stable/tutorials/3d/environment_and_post_processing.html).

### Animation, physics, and navigation are runtime systems

Scene entries declare animation players, bodies, shapes, navigation regions,
or agents, but the scene tree should not expose every internal mixer, broadphase
index, or pathfinding data structure. Runtime systems build optimized state
from the declared composition and referenced resources.

JScene3D already has transform, morph, skeletal, and sprite animation
capabilities. Version 0.1 should serialize clips, targets, playback selection,
looping, and project-authored animation events that those capabilities can
honor. A later animation graph can add state machines and blend spaces without
changing the identity of the animated entry. Godot's split between property
animation and a higher-level animation tree demonstrates this distinction; see
[AnimationTree](https://docs.godotengine.org/en/stable/tutorials/animation/animation_tree.html).

Physics serialization should distinguish a body from its reusable shape and
should make static, kinematic, character, and sensor semantics explicit. The
editor may offer derived collision generation, but imported render meshes must
not automatically become expensive dynamic triangle collision. Godot likewise
recommends primitive shapes for dynamic bodies and concave shapes for static
level collision. See [3D collision shapes](https://docs.godotengine.org/en/stable/tutorials/physics/collision_shapes_3d.html).

Navigation should be an optional world capability rather than assumed actor
behavior. A navigation resource may be imported or baked from geometry;
regions, links, and obstacles contribute to a navigation world; an agent asks
for a path; and project Java code still owns movement and game decisions.
Godot's navigation documentation makes the same crucial distinction that path
queries do not move the parent actor. See the
[3D navigation overview](https://docs.godotengine.org/en/stable/tutorials/navigation/navigation_introduction_3d.html).

Navigation is not required to model the first Doom map interactions. Doomed
Corridors can continue using map topology and game-specific movement while the
navigation resource and runtime seam are designed against another real use
case. This avoids making Doom sector semantics the general navigation model.

### Visibility, particles, and level-authoring tools

Automatic mesh LOD, hierarchical visibility ranges, and baked occlusion are
different optimization mechanisms and should remain renderer capabilities with
inspectable project settings, not implicit scene-loader behavior. Godot's
documentation treats them separately and describes different tradeoffs for
per-mesh LOD, grouped replacements, and CPU-tested occluders. See
[mesh LOD](https://docs.godotengine.org/en/stable/tutorials/3d/mesh_lod.html),
[visibility ranges](https://docs.godotengine.org/en/stable/tutorials/3d/visibility_ranges.html),
and [occlusion culling](https://docs.godotengine.org/en/stable/tutorials/3d/occlusion_culling.html).

Particles need registered emitter and resource descriptors plus renderer
support, simulation lifecycle, bounds, and editor preview controls. They should
be planned as a capability family but not delay the version 0.1 composition
model. Godot's 3D particle documentation distinguishes CPU and GPU simulation,
process materials or shaders, collision, attraction, trails, and subemitters;
that breadth argues against pretending particles are merely animated meshes.
See [3D particle systems](https://docs.godotengine.org/en/stable/tutorials/3d/particles/index.html).

Constructive solid geometry, tile grids, and terrain are authoring strategies,
not universal scene foundations. Godot describes CSG as a useful level
prototyping tool that can be converted to mesh and describes `GridMap` as a
three-dimensional tile map backed by a mesh library. The stable documentation
does not present one built-in general terrain workflow. JScene3D should
therefore let specialized extensions contribute these tools and bake or import
ordinary scene resources. See [CSG tools](https://docs.godotengine.org/en/stable/tutorials/3d/csg_tools.html)
and [GridMap](https://docs.godotengine.org/en/stable/classes/class_gridmap.html).

### Coordinate and unit contract

The scene format must state its coordinate handedness, up axis, forward axis,
angle units, distance units, and conversion rules. Import adapters should
normalize source formats deliberately and record provenance rather than let
each presentation path guess. The editor grid, transform inspector, physics,
audio attenuation, navigation, and runtime must all use the same contract.

Godot's explicit Y-up, right-handed, metre-based convention shows the product
value of settling this early, but JScene3D must document its own existing
mathematical convention rather than adopt a different one by analogy. See
[Godot's coordinate system](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html#coordinate-system).

## Registered type catalog

The editor and runtime need a shared description of every type that can appear
in project data. A registered type descriptor should include at least:

- a globally stable identifier;
- a display name and description;
- its allowed scope, such as scene entry, behavior, project system, resource,
  or importer;
- a versioned configuration schema;
- defaults;
- editor categories, ranges, units, and resource-reference constraints;
- declared events and actions with payload descriptions;
- runtime implementation ownership;
- compatibility and migration information.

JSON Schema can validate document structure and simple property constraints.
It should not be stretched to describe runtime factories, lifecycle rules, or
all cross-resource relationships. Those belong in the registered type
descriptor and semantic validation.

The editor should construct its property inspector from descriptors rather than
hard-code knowledge of Doomed Corridors. A Doom door may then appear in the
editor because an installed extension contributes its descriptor, not because
the editor itself understands sectors.

### Stable type identifiers, not Java class names

Project data should contain:

```json
{
  "type": "io.github.glynch.doomed-corridors/zombieman-behavior"
}
```

It should not contain:

```json
{
  "class": "io.github.glynch.doomedcorridors.combat.ZombiemanBehavior"
}
```

The provider maps a stable type identifier to an implementation. This permits
implementation refactoring, JPMS encapsulation, validation before
instantiation, and useful diagnostics when an extension is missing.

## Java extension scopes

### Scene behavior

A scene behavior has one instance for one scene entry or instantiated scene. It
receives only the scoped runtime facilities and references it declares. Typical
examples include actor movement, an interactive switch, a camera controller, or
a door mover.

```json
{
  "id": "door-motion",
  "type": "io.github.glynch.doomed-corridors/sector-door",
  "properties": {
    "sector": "import:freedoom-map01/sector/12",
    "speed": 2.0,
    "wait": "PT4S",
    "movement": "open-wait-close"
  }
}
```

### Project system

A project system is created once for a game session and is not attached to a
dummy scene entry. It may coordinate many instantiated scenes or survive scene
changes according to its declared lifecycle.

```json
{
  "$schema": "../schema/runtime-1.schema.json",
  "schemaVersion": 1,
  "systems": [
    {
      "id": "combat",
      "type": "io.github.glynch.doomed-corridors/combat",
      "properties": {
        "rules": "../game/combat.json",
        "presentation": "../game/combat-presentation.json"
      }
    },
    {
      "id": "campaign",
      "type": "io.github.glynch.doomed-corridors/campaign",
      "properties": {
        "definition": "../game/campaign.json"
      }
    }
  ]
}
```

Project systems should receive a project-scoped runtime context rather than
access process-wide mutable singletons. The lifecycle must define creation,
startup, fixed update, rendered update, scene-change notification, stopping,
and closure only where each phase is genuinely required. A single deep runtime
interface is preferable to exposing loader, registry, renderer, physics, audio,
and cache implementation details to every system.

Ordering should be derived from explicit dependencies or a small number of
well-defined phases. A manually ordered list whose correctness depends on
undocumented positions would become part of the interface and be difficult for
the editor to validate.

### Import extension

An import extension participates only in explicit import operations. It reads a
source asset and returns imported resources plus structured diagnostics and
provenance. It must not mutate the source or silently write project-authored
documents.

### Application lifecycle extension

Some applications may require Java code around project startup, scene
selection, or shutdown. This should be represented by an explicit provider or
project system lifecycle rather than requiring an invisible root scene node.
The design should avoid adding a separate lifecycle extension category if the
Game Provider and project systems already cover the demonstrated cases.

### Editor extension

The initial editor should derive ordinary property controls from type
descriptors and schemas. Custom editor panels or visual tools may later be
contributed by trusted Java extensions, but custom editor execution is a
separate security and lifecycle problem. It must not be required merely to show
the Project Manifest or raw JSON configuration.

## Conceptual Java interfaces

The following sketches illustrate responsibility and depth. They are not
proposed source-ready signatures.

```java
public interface GameExtension {
    ExtensionDescriptor descriptor();

    void contribute(GameTypeCatalog catalog);
}
```

```java
public interface ProjectSystem extends AutoCloseable {
    void start(ProjectRuntimeContext context);

    void fixedUpdate(FixedUpdate update);

    void frameUpdate(FrameUpdate update);

    void stop();
}
```

The second sketch may expose too much lifecycle surface. Before implementation,
it should be tested against at least combat, campaign progression, and save
management. Systems that do not need every callback should not be forced to
implement empty methods, and splitting every callback into a separate shallow
interface would also be undesirable.

The external runtime seam should remain small. Conceptually:

```java
ProjectRuntimeOpenResult open(GameProject project, ExtensionCatalog extensions);
```

The implementation should hide scene parsing, type resolution, dependency
ordering, resource sharing, instance overrides, semantic validation, and
diagnostic ordering. The returned runtime integrates with the existing
`GameApplication` and `GameRuntime` lifecycle rather than introducing a second
competing game loop.

## Event connections and imperative code

Connections authored as part of game composition should normally be serialized
so the editor can display and reproduce them. Examples include:

- a button action requesting a new game;
- a switch activating a door;
- player health changes updating the HUD;
- an actor's death spawning a configured drop;
- reaching an exit requesting a scene transition.

Java implementations may still create internal subscriptions that are
implementation details. The distinction is whether changing the relationship
is a project-authoring decision. If a developer should be able to reconnect it
in the editor, it belongs in project data.

Connections require declared, typed endpoints. A descriptor might declare:

```json
{
  "events": {
    "died": {
      "payload": "jscene3d/entity-reference"
    }
  },
  "actions": {
    "spawn": {
      "payload": "jscene3d/spawn-request"
    }
  }
}
```

An event descriptor must define its payload type, delivery cardinality, and the
lifecycle during which it can be emitted. An action descriptor must define its
accepted payload type, whether it requires a live target instance, and its
failure behavior. Scene validation resolves both endpoints and rejects an
incompatible payload before play. Dispatch order for multiple connections must
be deterministic. Removing a target during dispatch must have specified
snapshot or immediate-removal semantics rather than depend on collection
implementation details.

The recommended version 0.1 event contract is deliberately smaller than a
general message bus:

- registered scene entries, behaviors, and project systems declare named event
  outputs and action inputs in their descriptors;
- payload types use stable registered identifiers with schemas the editor and
  headless validator can inspect;
- a serialized connection targets an action identifier, never an arbitrary
  Java method name;
- emission and action delivery occur on the owning game-loop thread;
- connections from one event are delivered in their preserved declaration
  order using snapshot semantics, so removing a target does not corrupt the
  current dispatch;
- no-payload events and exact compatible payload forwarding are supported;
- payload conversion, filtering, aggregation, request-response, persistence,
  and cross-process delivery are deferred until real cases justify them;
- a target that disappears with its scene instance is disconnected as part of
  that instance's lifecycle, while an invalid authored target is a validation
  diagnostic.

This contract should support editor wiring for a switch activating a door, an
actor death requesting a drop, a timer expiring, and health changes updating a
HUD. It does not prevent a Java implementation from using ordinary Java
listeners internally when that relationship is not project-authored.

Events are notifications rather than mutable shared state. Request/response
operations and resource lookup should use explicit runtime interfaces instead
of pretending every interaction is an event. Godot's signals are a useful
reference for decoupling observers from emitters, but JScene3D still needs its
own payload, lifecycle, and diagnostic contracts. See
[Using signals](https://docs.godotengine.org/en/stable/getting_started/step_by_step/signals.html).

The first version should avoid a general expression language. Constants,
resource references, and compatible event payload forwarding may be enough for
initial doors, switches, drops, menus, and HUD updates. Transformation
expressions should be added only when concrete connections cannot otherwise be
represented cleanly.

## Input actions

Input maps are project resources, not hard-coded launcher behavior. Behaviors
consume semantic actions such as `move-forward`, `turn-left`, or `fire`, while
an input map binds keys, mouse buttons, pointer motion, and controllers.

The existing `jscene3d-game` `InputMap` and `ActionSnapshot` provide the runtime
foundation. A native input-map resource should be a serializable description
that creates the same runtime model.

```json
{
  "$schema": "../schema/input-map-1.schema.json",
  "schemaVersion": 1,
  "actions": {
    "move-forward": [
      { "device": "keyboard", "key": "W" },
      { "device": "keyboard", "key": "UP" }
    ],
    "turn-left": [
      { "device": "keyboard", "key": "LEFT" }
    ],
    "fire": [
      { "device": "mouse", "button": "LEFT" },
      { "device": "keyboard", "key": "SPACE" }
    ]
  }
}
```

Defaults supplied by a provider may seed a new project, but once bindings are
part of a project they must be data the editor can inspect and change.

## Source import architecture

### Import orchestration seam

JScene3D already has more than one concrete source-format case: glTF and WAD.
That provides evidence for a shared import orchestration seam even though their
decoders and output capabilities differ.

Shared orchestration responsibilities include:

- source selection and project-relative path safety;
- importer selection by stable identifier;
- importer settings validation;
- source, dependency, settings, and importer-version fingerprints;
- structured diagnostics and progress;
- deterministic output identity;
- provenance;
- cache invalidation and regeneration;
- cancellation;
- presentation of import options to the editor.

The seam should not force every format into one lowest-common-denominator data
model. A glTF adapter and Doom WAD adapter may produce different registered
resource types while sharing orchestration, diagnostics, provenance, and cache
behavior.

### Illustrative import declaration

```json
{
  "$schema": "../schema/import-1.schema.json",
  "schemaVersion": 1,
  "id": "freedoom-map01",
  "source": "asset:freedoom",
  "importer": "io.github.glynch.jscene3d.doom-format/map",
  "selection": {
    "map": "MAP01"
  },
  "settings": {
    "skill": "hurt-me-plenty",
    "includeMultiplayerThings": false
  }
}
```

The imported outputs receive logical identities such as:

```text
import:freedoom-map01/map
import:freedoom-map01/sector/12
import:freedoom-map01/linedef/47
import:freedoom-map01/thing/86
```

This syntax is illustrative. The requirement is stable logical identity, not a
particular URI grammar.

### Authoritative source and disposable cache

An import result is determined by:

```text
source bytes
+ referenced source dependencies
+ import settings
+ importer identifier and version
= imported resources
```

Derived data belongs in a cache that can be deleted and rebuilt. Project files
must never rely on an undocumented machine-specific absolute path. Importing
must not rewrite or normalize the original WAD or glTF source.

This follows a proven editor workflow. Godot keeps original source assets,
commits small per-asset import settings, writes converted results to a hidden
cache, detects source changes, and can regenerate the cache after deletion.
JScene3D should adopt those properties without copying the `.import` filename
or cache layout. See Godot's official
[import process](https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/import_process.html).

Complex three-dimensional sources need per-object import configuration and an
authored layer above imported content. Godot preserves the imported base and
supports advanced settings for meshes, materials, animation, physics, and
navigation, plus inherited scenes for durable authored changes. For JScene3D,
the corresponding recommendation is an import recipe plus deterministic
imported identities and authored composition or overlays. See
[Importing 3D scenes](https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/index.html)
and [Advanced import settings](https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/import_configuration.html).

### Authored composition over imported content

The imported MAP01 scene should not be the whole application scene. A
project-authored scene can instantiate the imported map and compose it with a
player, HUD, project systems, presentation, and additional authored content.

```json
{
  "id": "map01-gameplay",
  "root": {
    "id": "map01",
    "type": "jscene3d/game-root",
    "children": [
      {
        "id": "imported-world",
        "instance": "import:freedoom-map01/map"
      },
      {
        "id": "player",
        "instance": "../actors/player.scene.json"
      },
      {
        "id": "hud",
        "instance": "../ui/doom-hud.scene.json"
      }
    ]
  }
}
```

If project authors need to alter imported objects, overrides should live in an
authored overlay keyed by deterministic imported identity. The cache remains
regenerable, and reimport can diagnose an overlay whose source target no longer
exists.

## WAD support

### WAD archive capability

WAD archive support should be available as a reusable optional JScene3D
capability rather than remain private forever inside Doomed Corridors. Its
genre-neutral responsibilities are:

- validate a WAD header and directory;
- distinguish supported archive kinds such as IWAD and PWAD without assigning
  gameplay meaning;
- enumerate and locate named lumps while preserving order and duplicates;
- expose bounded raw lump data safely;
- support explicit archive layering where specified;
- produce structured diagnostics;
- retain source provenance;
- avoid graphics, audio, physics, and game-rule dependencies.

This can be a deep concrete library without inventing a `WadReader` interface
and multiple interchangeable readers. Supporting WAD files does not itself
require an adapter hierarchy.

An illustrative artifact name is `jscene3d-wad`. The name and publication plan
remain open decisions.

### Doom-format interpretation

The following capabilities are necessarily Doom-family-specific even though
they are reusable by more than one game:

- classic Doom map lump groups;
- vertices, linedefs, sidedefs, sectors, BSP nodes, subsectors, segs, reject
  tables, blockmaps, and things;
- palettes, colormaps, patches, composite wall textures, flats, and sprites;
- Doom sound and music encodings;
- map markers and Doom naming conventions;
- vanilla Doom and Doom II compatibility diagnostics.

These belong in a separate optional Doom-format extension layered over the WAD
archive capability. An illustrative artifact name is `jscene3d-doom-format`.
It should describe and import the format without adding Doom concepts to the
Game Engine or Physics Engine.

### Doom gameplay interpretation

Actor types, monster state machines, weapons, damage, ammunition, keys,
inventory, sector motion, switch behavior, exits, and campaign progression are
gameplay semantics. Doomed Corridors may implement them through registered
types and project data.

A later reusable Doom-runtime extension would be justified only if it has a
clear supported compatibility profile and consumers beyond one title. It
should not be created merely to move application code into the engine
repository.

### Meaning of out-of-the-box support

Out-of-the-box WAD support should mean that a standard JScene3D tool or editor
distribution can include the WAD archive and Doom-format extensions, recognize
supported WAD sources, inspect their contents, offer applicable importers, and
create native/imported resources without requiring each developer to write a
WAD parser.

It should not mean that every JScene3D application depends on Doom types, or
that loading an arbitrary WAD automatically produces a fully playable Doom
game.

## Modeling upcoming Doomed Corridors features

The next features should validate the architecture rather than bypass it.

### Enemy ammunition drops

The fact that a zombieman drops an ammunition clip is project-authored gameplay
configuration. It can be represented as a declared death connection or a drop
configuration consumed by a registered behavior.

```json
{
  "id": "zombieman",
  "type": "io.github.glynch.doomed-corridors/actor",
  "behaviors": [
    {
      "type": "io.github.glynch.doomed-corridors/drop-on-death",
      "properties": {
        "scene": "../pickups/ammunition-clip.scene.json",
        "chance": 1.0
      }
    }
  ]
}
```

The Java implementation performs spawning and owns runtime correctness. The
choice of dropped scene and chance remain visible and editable.

### Doors

A Doom door combines imported source identity, activation rules, sector motion,
timing, collision updates, sound, and presentation. The WAD adapter recognizes
the linedef special and produces deterministic imported metadata. Registered
Doomed Corridors behaviors interpret that metadata.

Properties likely to be editable include activation mode, repeatability, key
requirement, speed, destination rule, wait duration, sound set, and target
sector. The runtime implementation owns interpolation, collision consistency,
and state transitions.

### Switches

A switch is a useful proof of serialized connections:

```json
{
  "from": "node:switch-47/activated",
  "to": "node:door-12/activate"
}
```

The WAD adapter can generate that relationship from tags and specials. An
author can inspect and, where allowed, override it in the editor. The switch's
visual state and sound are resources referenced by its registered behavior.

### Lifts, teleports, exits, and sector effects

These should use the same registered-type, property, identity, and connection
mechanisms. If each feature requires a new hard-coded startup branch, the scene
and extension design has failed to provide sufficient leverage.

## Editor workflow

The editor should expose three deliberately different operations:

- **Edit scene** renders project data in an editor-owned 3D viewport with an
  editor camera, selection, transform gizmos, grid, debug overlays, and optional
  preview lighting. It must not start the game loop or execute arbitrary
  project Java code merely to display standard registered types.
- **Play current scene** starts a playable session at the open scene. The
  runtime composition interface must define which project systems and input
  configuration form its test harness.
- **Play project** starts from the Project Manifest's entry scene exactly as a
  packaged application would.

Godot exposes an editor 3D workspace separately from running a scene, and its
playable embedded view still runs in another process so a game crash does not
crash the editor. It also offers pause, frame advance, runtime inspection,
camera override, and a clear warning that edits to runtime state are not saved.
These are strong product references for JScene3D. See
[Introduction to 3D](https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html)
and [Game embedding](https://docs.godotengine.org/en/stable/tutorials/editor/game_embedding.html).

For JScene3D, edit-scene rendering should initially run in the editor process
using trusted engine adapters and descriptor-driven representations. Both play
operations should run the game in a child JVM. A separate game window is the
smallest first implementation; visually embedding that native window can come
later without moving gameplay into the editor JVM. Process isolation prevents a
project crash, `System.exit`, runaway game loop, or native renderer failure from
taking down the editor.

The editor should launch the project's Maven Wrapper rather than assume a
globally installed Maven. The project build owns Java compilation and runtime
dependency resolution. A small, versioned preview protocol may later support
ready/stopped/error state, structured diagnostics, pause, frame advance, scene
tree snapshots, selected-property inspection, and camera override. It must not
promise persistence of runtime mutations. Durable changes target the local
authored scene and are then synchronized or applied on the next run.

The complete conceptual sequence is:

1. Load `project.json` safely through `jscene3d-project`.
2. Display basic identity, icon, compatibility, source assets, and missing
   extension diagnostics without executing Game Provider code.
3. After the project is trusted, discover installed extensions and their type
   descriptors.
4. Validate referenced resources against structural schemas and registered
   semantic rules.
5. Inspect source assets and run explicit or automatic deterministic imports.
6. Present project and imported resources in one resource browser while keeping
   their provenance distinct.
7. Present scene hierarchy, scene instances, properties, and connections.
8. Save changes to the same versioned documents consumed by headless tools and
   standalone games.
9. Render standard scene entries in the edit viewport without starting a game
   session.
10. Launch current-scene or project play in a child JVM through the same
    project-opening and runtime interfaces as the packaged application.
11. Build exports from named, validated export presets.

Editor-only state such as panel layout, expanded tree nodes, recent selections,
local cache paths, and window position stays outside the shared project
definitions.

## Export and distribution

Export is not a copy of the source directory. It is a reproducible build that
selects project content, compiles Java code, resolves runtime dependencies,
chooses platform-native libraries, creates a Java runtime image, and produces a
launchable distribution for one target.

Godot separates named export presets from export templates and can emit either
a playable build or a content pack. It also distinguishes committed presets
from local credentials. The exact mechanism is different for Java, but the
product concepts transfer well. See [Exporting projects](https://docs.godotengine.org/en/stable/tutorials/export/exporting_projects.html)
and [PCK files](https://docs.godotengine.org/en/stable/tutorials/export/exporting_pcks.html).

An illustrative JScene3D export preset is:

```json
{
  "$schema": "../schema/export-preset-1.schema.json",
  "schemaVersion": 1,
  "id": "macos-arm64",
  "platform": "macos",
  "architecture": "aarch64",
  "format": "application-image",
  "mainClass": "io.github.glynch.doomedcorridors.DoomedCorridors",
  "content": {
    "include": ["project.json", "scenes/**", "game/**", "assets/**"],
    "exclude": ["target/**"]
  },
  "java": {
    "runtimeImage": "linked",
    "options": ["-Xms256m", "-Xmx2g"]
  }
}
```

The syntax and values are illustrative. The final preset must reference an
application entry definition instead of duplicating a Java main class if the
project runtime can supply one generic launcher. Content selection must be
dependency-aware so a referenced scene, resource, imported output, WAD, or
license file is not accidentally omitted by a glob.

Maven should remain the Java build authority. An export component can ask the
project build for compiled application artifacts and a resolved runtime
classpath or JPMS module path, stage the selected project resources, validate
native classifiers, and invoke JDK packaging tools. Oracle's `jlink` creates a
custom runtime image from a selected JPMS module closure, while `jpackage`
creates self-contained application images and native package formats and can
invoke `jlink`. See the JDK 21 [`jlink`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html)
and [`jpackage`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
manuals.

Exports are platform and architecture specific because the JDK packaging tools
produce packages for the host platform and LWJGL dependencies contain native
libraries for a target. Therefore macOS, Windows, and Linux outputs should be
built and smoke-tested on corresponding build hosts. The preset is portable;
the generated package is not.

Version 0.1 should support a runnable application image for the current host,
with a staged project-content directory and deterministic dependency report.
Installers, signing, notarization, icons for every platform, content-only packs,
patches, and cross-host CI matrices can follow. Credentials, certificates, and
passwords must remain outside committed project data.

## Trust and extension execution

Project metadata loading and Java extension execution must remain separate.
Opening an unfamiliar project browser entry should not execute arbitrary code.

A possible trust sequence is:

```text
safe manifest load
-> dependency and compatibility inspection
-> explicit project trust
-> extension discovery and loading
-> full resource validation and import
-> edit or run
```

The final security design must define extension provenance, installation,
version resolution, JPMS module-layer or classpath behavior, failure isolation, and
what the editor can still display when an extension is missing or untrusted.
Automatic Maven dependency download and arbitrary code execution are outside
the initial proposal.

## Diagnostics

Project, scene, resource, extension, and import failures should use a common
diagnostic shape where practical:

- severity;
- stable diagnostic code;
- human-readable message;
- source resource;
- machine-navigable location;
- related locations where a relationship spans documents;
- optional remediation metadata for the editor.

Expected invalid project data is diagnostic output, not an exception. Caller
contract violations and unexpected implementation failures remain exceptions.
Diagnostics should be deterministic so command-line output, tests, and editor
problem lists agree.

## Testing strategy

The project definitions and registered-type descriptors must be testable
headlessly. The principal test surface should cover observable behavior through
the loader, importer, scene-instantiation, and runtime interfaces rather than
implementation helpers.

Required design proofs include:

- Project Manifest loading does not initialize graphics, audio, imports, or
  extensions.
- A scene with nested scene instances loads with stable identities and shared
  resources.
- Missing types and extensions produce navigable diagnostics.
- Invalid properties, connections, and reference cycles fail deterministically.
- Scene instance overrides do not mutate the reusable source scene.
- glTF and WAD adapters use the same import orchestration interface.
- repeating an unchanged import produces equivalent identities and content.
- changing source bytes, settings, or importer version invalidates derived
  output.
- deleting imported cache data loses no project-authored information.
- an authored overlay survives reimport and diagnoses missing imported targets.
- a project system can be tested without a window or renderer where its
  behavior is headless.
- editor-produced project data loads through the same headless interface as
  hand-authored data.
- a standalone application and editor play mode instantiate the same project
  composition.
- edit-viewport rendering does not execute project systems or persist editor
  cameras and preview lighting into the game scene.
- current-scene play and project play select different entry points but use the
  same scene-instantiation and runtime components.
- a crashing child game process leaves the editor process running and reports a
  structured termination diagnostic.
- an export includes the transitive scene and resource closure, selected
  imported content, Java runtime dependencies, licenses, and correct native
  libraries for its declared target.
- a generated application image starts without a developer JDK or machine-local
  Maven repository.

## Version 0.1 authoring boundary

Version 0.1 should prove a complete vertical authoring path, not attempt to
serialize every existing engine class. The following catalog is concrete enough
to design schemas and editor inspectors while leaving final stable type
identifiers for the grill.

### Version 0.1 scene-entry catalog

The names below describe capabilities rather than final serialized type
identifiers or a Java inheritance hierarchy.

| Entry family | Version 0.1 responsibility |
| --- | --- |
| Logical group | Ordered non-spatial composition, name, tags, enabled state, behaviors, events, and actions |
| Spatial group | Parent-relative transform, visibility, ordered children, tags, and behaviors without a render resource |
| Scene instance | Another scene definition plus bounded property overrides and a stable instance identity |
| World | Scene environment, primary output, active camera selection, and world-level settings |
| Mesh instance | Shared mesh, per-surface materials or overrides, transform, visibility, shadow settings, and bounds policy |
| Instanced mesh | Shared mesh and materials plus active count, transforms, colors, custom numeric attributes, morph influences, and bounds supported by `InstancedMesh` |
| Billboard | Texture or region, size, alignment, alpha behavior, and transform |
| Animated billboard | Sprite animation set, selected animation, playback, looping, events, and billboard presentation |
| Perspective camera | Field of view, clipping, output selection, layer mask, and active priority |
| Orthographic camera | View size, clipping, output selection, layer mask, and active priority |
| Ambient or hemisphere light | Existing ambient contribution properties |
| Directional, point, or spot light | Existing color, intensity, range, cone, shadow, and transform properties applicable to the selected family |
| Animation player | Clip library, initial animation, playback settings, and stable target bindings for existing animation capabilities |
| Static body | One or more collision-shape references, transforms, material settings when supported, and collision filters |
| Kinematic body | Collision-shape references, collision filters, and project-controlled movement |
| Character body | Character-controller settings, collision filters, and semantic movement inputs without game-specific rules |
| Sensor | Shape references, collision filters, enabled state, and entered, stayed, and exited events |
| Audio listener | Listener transform and active priority |
| Audio source | Clip, spatial mode, attenuation, category, gain, looping, autoplay, and playback actions or events |
| Timer | Duration, one-shot or repeating mode, autoplay, pause behavior, and timeout event |
| Screen canvas and UI group | Ordered screen-space composition and a minimal anchored layout model |
| UI image | Texture or region, tint, opacity, fit mode, and anchors |
| UI text | Text, font reference, color, alignment, wrapping, and anchors sufficient for menus and HUDs |

Tags are inspectable labels for lookup and categorization; they do not replace
stable identities or typed event connections. Behaviors are attached using the
separate behavior mechanism and do not require a dummy behavior node. Collision
shapes remain resources referenced by bodies or sensors rather than visible
meshes or independent transform nodes. If compound-shape authoring later proves
that child shape entries provide better editing, that can be added without
changing the shape resource itself.

A shader is a resource used by a material, not a scene entry. Raw shader source,
declared uniforms, uniform types, defaults, ranges, and resource constraints
belong to the shader descriptor. A mesh is also a resource rather than a scene
entry; a mesh resource contains surfaces and geometry, while material assignment
occurs by surface with a bounded entry-level override. This keeps shader
programming and mesh data reusable and prevents renderer buffers from leaking
into project documents.

### Version 0.1 resource catalog

- scene definition and input map;
- image, texture, and texture region;
- mesh and instanced-transform data;
- standard material, shader material, and shader source;
- environment settings and environment map;
- transform, morph, skeletal, and sprite animation data already represented by
  JScene3D;
- audio clip;
- font and a minimal user-interface theme and layout resource;
- supported primitive collision shapes and deterministic imported or generated
  static collision geometry;
- project configuration resources registered by extensions;
- import recipes and logical references to imported outputs.

Font, minimal theme, and layout resources must be designed with the
user-interface component and are required for the version 0.1 menu and HUD
proof. A complete widget theme system is long-term scope; the initial resources
must not be improvised as image-specific properties.

### Version 0.1 runtime systems

- project loading, type registration, semantic validation, and migration;
- scene resolution, instantiation, overrides, lifecycle, and stable addressing;
- resource resolution, sharing, and closure;
- rendering, camera selection, input, physics, animation, and audio adapters over
  the existing engine components;
- project event routing and registered behaviors;
- deterministic import and reimport orchestration;
- child-JVM current-scene and project play;
- host-platform application-image export.

Navigation, particle simulation, LOD authoring, hierarchical visibility,
occlusion baking, sub-viewports, CSG, grid editing, terrain tools, advanced
animation graphs, embedded play, runtime inspection, hot synchronization,
content packs, installers, and signing are long-term capabilities. The scene and
descriptor formats should leave room for registered types in these families;
version 0.1 does not need empty placeholders for each one.

### Long-term scene-entry families

Long-term support should be prioritized by demonstrated game and editor needs,
not by an attempt to match another engine class for class. The architecture
should nevertheless accommodate these families without a new scene format:

| Family | Candidate entries or authoring capabilities |
| --- | --- |
| Rendering | Three-dimensional text, decals, line and debug geometry, reflection and light probes, fog volumes, particle emitters, and renderer-specific effects |
| Output and compositing | Sub-viewports, render targets, multiple windows, cameras per output, screen-space and world-space UI composition, and post-processing volumes |
| Spatial authoring | Markers and sockets, paths and curves, transform constraints, editor gizmos, and large-world partitioning |
| Animation | Blend trees, state machines, blend spaces, skeleton and skin controls, inverse kinematics, root motion, animation retargeting, and ragdoll integration |
| Physics | Dynamic rigid bodies, joints, shape casts, persistent ray queries, soft bodies, vehicles, richer physics materials, and debug visualization |
| Navigation | Navigation worlds, regions, agents, links, obstacles, layers, baked meshes, graph navigation, and editor debug views |
| Visibility and scale | Automatic mesh LOD, authored HLOD replacement groups, occluders, visibility ranges, streaming cells, and background loading |
| Level construction | CSG blockout, reusable grid or tile libraries, spline-generated geometry, terrain extensions, collision generation, and navigation baking |
| Audio | Area-based ambience, reverb zones, buses, effects, snapshots, streaming sources, and richer listener routing |
| User interface | Complete layout containers, controls, focus and navigation, themes, localization, accessibility metadata, animation, and multiple resolutions |
| Extension-defined | Trusted registered scene entries with descriptor-driven properties, events, actions, previews, gizmos, and optional custom editor tools |

Several items may first exist as editor tools that bake ordinary versioned
resources. CSG, grid painting, occluder generation, navigation baking, and LOD
generation do not all need permanent runtime scene-entry types. That distinction
keeps the runtime catalog smaller while preserving capable visual authoring.

## Proposed responsibility placement

The following is a first allocation for discussion:

| Responsibility | Proposed owner |
| --- | --- |
| Safe Project Manifest loading and metadata | `jscene3d-project` |
| Native scene and resource schemas | A new project/scene artifact or a carefully deepened `jscene3d-project` |
| Scene instantiation and project runtime composition | A genre-independent game-project runtime artifact or `jscene3d-game` only if the interface remains genre-independent and deep |
| Existing fixed/rendered game loop | `jscene3d-game` |
| Registered type descriptors and catalog | The project/runtime composition component |
| Import orchestration, provenance, and cache policy | A reusable import artifact justified by glTF and WAD adapters |
| glTF format interpretation | `jscene3d-gltf` adapter |
| Generic WAD archive access | Optional `jscene3d-wad` artifact |
| Doom-format decoding and import | Optional `jscene3d-doom-format` artifact |
| Doom gameplay semantics | Doomed Corridors Game Provider and project resources |
| General collision and character motion | `jscene3d-physics` and `jscene3d-game` as already defined |
| Descriptor-driven 3D edit viewport and editor-only aids | Editor application using renderer adapters |
| Child-JVM current-scene and project play | Editor runtime-launch component over the project build and game runtime |
| Export preset validation and dependency closure | Reusable project-export component |
| Java runtime image and native application packaging | Platform build adapter using Maven and JDK packaging tools |
| Editor application | `jscene3d-gui` or a separately named editor application, to be decided |

The native scene model, import orchestration, and editor are substantial enough
that artifact ownership should not be decided merely by placing new packages in
an existing artifact. Each proposed artifact must hide meaningful complexity
behind a small interface and avoid dependency cycles.

## Relationship to current Doomed Corridors code

Doomed Corridors already demonstrates useful separations:

- `project.json` provides validated project metadata and source asset identity;
- `actors.json`, `combat.json`, and `combat-presentation.json` move substantial
  authoring choices out of Java;
- WAD parsing, map decoding, material import, sprite import, and sound import are
  separated from rendering;
- headless map, geometry, collision, and combat models can be tested without a
  window;
- presentation adapts those models to JScene3D rendering and audio.

The principal architectural gap is runtime composition. Startup code currently
knows how to find and assemble the WAD map, imported resources, actor catalog,
combat rules, presentation rules, player, enemies, HUD, input, audio, and
renderer. The Project Manifest points directly at a WAD asset and map rather
than at an engine-native application scene.

The current code should not be discarded. It supplies the concrete behavior
needed to design and test registered types, import adapters, project systems,
and scene composition. The migration should move authored choices and
relationships behind those interfaces while retaining the proven headless
implementations.

## Incremental design and implementation sequence

Implementation should begin only after this draft is challenged and the first
decisions are recorded.

1. Agree on invariants and terminology for Project Manifest, scene definition,
   scene instance, registered type, behavior, project system, and imported
   resource.
2. Confirm JScene3D's coordinate, unit, resource-sharing, mesh-surface, material,
   and shader-parameter contracts.
3. Design the registered type descriptor using at least a spatial scene entry,
   mesh instance, camera, light, collider, HUD entry, behavior, project system,
   and import adapter as examples.
4. Design the smallest scene schema that composes a root, children, scene
   instances, properties, resource references, and connections.
5. Prove directory independence and safe headless loading on synthetic project
   fixtures.
6. Design project runtime composition around the existing `GameApplication`
   and `GameRuntime` rather than adding another loop.
7. Design import orchestration using both the existing glTF loader and current
   WAD pipeline so the seam is based on two real adapters.
8. Separate generic WAD archive behavior from Doom-format interpretation and
   decide which existing Doomed Corridors classes should migrate, if any.
9. Represent the current MAP01 composition without changing its observable
   gameplay.
10. Represent zombieman ammunition drops through registered behavior and project
   data as the first small authoring proof.
11. Represent one door and switch connection as the first imported-interaction
    proof.
12. Add further Doom behaviors only through the proven definitions and runtime
    interfaces.
13. Prove descriptor-driven edit-viewport rendering without project Java
    execution, followed by child-JVM current-scene and project play.
14. Prove a host-platform application-image export containing a linked Java
    runtime, dependency closure, native libraries, and project content.
15. Build the editor shell over those same interfaces after the runtime, import,
    preview, and export models have survived real game features.

Each migration slice should keep the standalone game runnable and provide a
manual smoke test in addition to automated verification.

## Alternatives rejected by this draft

### Prescribed directories

Requiring `main/`, `game/`, or another fixed layout would confuse organization
with semantics and make migration unnecessarily difficult. Explicit references
are sufficient.

### Java-only runtime assembly

Java-only composition is flexible but hides project structure from the editor,
makes common authoring changes require recompilation, and duplicates work when
an editor model is eventually added.

### Data-only games

Requiring every algorithm to be serialized would create a programming language
inside the project format and make advanced behavior harder to implement,
debug, test, and reuse. Java extensions remain first-class.

### Serialized Java class names

Class names expose implementation structure as persistent project interface,
weaken JPMS encapsulation, and make refactoring saved projects difficult.
Stable registered type identifiers provide a better seam.

### Separate scene and prefab formats

Both would initially need the same hierarchy, properties, instances,
connections, references, validation, and overrides. One reusable scene concept
has greater depth and less duplicated interface.

### Converting WAD content once and discarding provenance

This would prevent reliable reimport, obscure licensing and source identity,
and make it difficult to verify that a project corresponds to its pinned WAD.
The WAD remains authoritative.

### Editing generated import output directly

Reimport would overwrite authored work or make generated data no longer
reproducible. Authored composition and overlays must remain separate from the
disposable cache.

### Making Doom behavior generic by renaming it

A `SectorDoor` or Doom linedef special remains Doom-specific even if placed in
`jscene3d-game` under an abstract name. Reusability requires a demonstrated
genre-independent interface, not generic naming.

### Building custom editor panels for every type

This would make ordinary project editing depend on executable editor plugins.
Schemas and descriptors should handle ordinary property editing; custom panels
are reserved for interactions that genuinely need them.

## Questions for the design grill

### Project and packaging

- Q1: Should a project have exactly one principal Game Provider plus optional
   extensions, or should every runtime contribution use one extension list?
- Q2: Are Java extensions resolved only by the packaged Maven application at
   first, or does the editor need an installed-extension catalog immediately?
- Q3: Which metadata must remain visible without trusting or loading extensions?
- Q4: Should `runtime.configuration` be one document or a catalog of independently
   referenced project resources?

### Scene model

- Q5: Is one reusable scene format sufficient for complete screens, 3D worlds,
   actors, and HUD fragments?
- Q6: What is the smallest runtime abstraction that can contain spatial,
   user-interface, audio, physics, and behavior entries without becoming a
   weak universal object?
- Q7: Are behaviors children, a distinct list on an entry, or ordinary registered
   entries with a declared scope?
- Q8: Which override operations are necessary in version 1?
- Q9: How are stable authored identifiers generated and preserved?
- Q10: How are imported identifiers derived so they remain stable across reimport?
- Q11: Should event connections live at scene scope, entry scope, or both?
- Q12: How much payload adaptation is required before a general expression
    language becomes justified?

### Java extensions

- Q13: What project-wide Java lifecycles are demonstrated by current code rather
    than merely anticipated?
- Q14: How should a project system obtain physics, rendering, audio, resources,
    scenes, and events without receiving an oversized runtime context?
- Q15: How are dependencies and ordering between project systems declared?
- Q16: Can type descriptors be inspected without instantiating runtime
    implementations?
- Q17: What migration guarantees apply when a registered type's schema evolves?
- Q18: Should type identifiers include provider identity implicitly or always be
    fully qualified?

### Import and WAD support

- Q19: What exact interface is genuinely shared by glTF and WAD import rather than
    being superficial orchestration?
- Q20: Does import produce serialized cache documents, direct immutable Java
    models, binary cache artifacts, or a combination?
- Q21: How are large imported resource graphs browsed without loading all content?
- Q22: What overlay operations are needed for author modifications to imported
    maps?
- Q23: Which current WAD classes are generic archive capability, which are
    Doom-format interpretation, and which are Doomed Corridors gameplay?
- Q24: Does out-of-the-box distribution include both raw WAD inspection and
    Doom-format import by default?
- Q25: How should PWAD layering and multiple source archives appear in an import
    declaration?

### Editor and trust

- Q26: Is the future editor part of `jscene3d-gui` or a separately packaged
    JScene3D Editor application that consumes `jscene3d-gui`?
- Q27: What can the editor show and modify when a required extension is missing?
- Q28: How does a user explicitly trust a project and its Java extensions?
- Q29: Which editor state is local, and where is that local state stored?
- Q30: What round-trip guarantees prevent the editor from discarding unknown but
    valid extension data?

### Preview, runtime, and export

- Q31: Which standard scene types can the edit viewport render without loading
  project Java, and how are extension-defined types represented when untrusted?
- Q32: What is the exact current-scene play harness for project systems, input,
  scene transitions, and application services?
- Q33: Is a separate game window sufficient for the first editor release, with
  embedded play and remote inspection explicitly deferred?
- Q34: What build interface can launch a Maven project without coupling the
  editor to one POM layout?
- Q35: Does version 0.1 export one generic JScene3D launcher or a project-supplied
  main class?
- Q36: How does an export preset express the transitive content closure while
  retaining source-asset licenses and imported-resource provenance?
- Q37: Which target triples are officially supported, and where are matching
  LWJGL native dependencies selected and verified?
- Q38: Which material and shader properties are portable project data, and which
  are capabilities of one renderer backend?
- Q39: Are instanced mesh transforms authored directly, imported, generated, or
  all three, and what identity is available for editing one instance?

## Decisions that should precede implementation

The grill session should at minimum settle or narrow these points:

- whether the Project Manifest advances to an entry scene and import
  declarations;
- the one-scene-format recommendation;
- the distinction between scene behavior and project system;
- the registered type descriptor and stable identifier strategy;
- serialized connection scope;
- imported resource identity and authored-overlay rules;
- artifact ownership for import orchestration, WAD archive access, and
  Doom-format interpretation;
- the definition of out-of-the-box WAD support;
- extension discovery and trust assumptions for the first editor version;
- the version 0.1 scene-entry, resource, and runtime-system catalogs;
- edit viewport versus current-scene play versus project-play semantics;
- child-JVM preview lifecycle and the boundary of its optional debug protocol;
- export preset ownership, Java packaging strategy, and first supported target;
- a small proof slice that migrates existing behavior without changing the
  current game.

Once those decisions are recorded, schemas and Java interfaces can be designed
against concrete examples before implementation begins.
