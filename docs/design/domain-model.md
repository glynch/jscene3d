# JScene3D Domain Model

## Purpose

This document defines the core domain model of JScene3D.

It establishes the meaning and relationships of the principal concepts used by the engine, project system, authoring infrastructure, and runtime.

It deliberately does not define editor layout, views, workflows, interaction design, or user interface behaviour. Those concerns belong in separate editor design documentation.

## Core Domain Model

At the authored-content level, the primary model is:

```text
Project
  └── SceneDefinition
       └── Entity
            └── Component
```

A Project contains one or more SceneDefinitions.

A SceneDefinition describes an authored composition of Entities.

Entities form hierarchical relationships and acquire capabilities, state, and behaviour through Components.

At runtime, authored content is instantiated into a World:

```text
Project
   ↓
World
   ↓
instantiate SceneDefinition
   ↓
live Entities and Components
```

`SceneDefinition` and `World` are deliberately different concepts.

## Project

A Project is the top-level definition of a JScene3D application.

It establishes the boundary within which authored content, assets, extensions, resources, imports, and application-specific Java code are resolved.

A Project may contain multiple SceneDefinitions and other reusable authored definitions and resources.

A Project designates one SceneDefinition as its Main Scene.

Project configuration includes application-level information required to construct and run the application. It does not represent transient authoring-tool state.

## SceneDefinition

A `SceneDefinition` is an authored composition of Entities and their Components.

It describes content that can later be instantiated into a runtime World.

Examples of SceneDefinitions may include:

- a main menu
- a game level
- an indoor environment
- a test environment
- another independently authored composition

A Project may contain any number of SceneDefinitions.

A SceneDefinition has persistent asset identity independent of its display name and physical location.

The existing older `project.scene.SceneDefinition` typed-node/controller model is a separate obsolete model and is not the `SceneDefinition` described by this document.

## World

A `World` is a live runtime environment.

It owns runtime state and coordinates execution of instantiated JScene3D content.

Its responsibilities may include:

- live Entities and Components
- Component lifecycle processing
- runtime modules
- resources
- spawning
- signals and actions
- other simulation and application state

A World is created for runtime execution.

A SceneDefinition does not itself become a World. Its authored content is instantiated into a World.

## SceneDefinition and World

A SceneDefinition and a World represent different layers of the system.

A SceneDefinition answers:

> What has been authored?

A World answers:

> What is currently running?

Conceptually:

```text
SceneDefinition
    ↓ instantiate
World
    └── live Entities
         └── live Components
```

A SceneDefinition is persistent authored data.

A World is executable runtime state.

Multiple runtime objects may ultimately originate from the same authored definition without changing that definition's identity.

This distinction also allows authored content to be inspected, validated, transformed, and persisted without requiring runtime execution.

## Main Scene

A Project designates one SceneDefinition as its `mainScene`.

The Main Scene defines the initial authored composition instantiated when application execution begins.

Conceptually:

```text
Project.mainScene
        ↓
SceneDefinition AssetId
        ↓
instantiate
        ↓
World
```

The Main Scene is a normal SceneDefinition. Its special role is only that it provides the initial Scene content for application startup.

The `mainScene` relationship should use the SceneDefinition's stable asset identity rather than its filesystem path.

Moving or renaming the SceneDefinition therefore does not change which Scene is the Main Scene.

## Entity

An `Entity` is an individually identifiable object within an authored definition or runtime World.

An Entity provides the structural identity to which Components and child Entities belong.

An Entity has concepts including:

- persistent Entity identity within its authored graph
- name
- enabled state
- parent/child relationships
- attached Components

Capabilities and behaviour are provided primarily through composition of Components rather than increasingly specialized Entity subclasses.

For example, a Player may be represented conceptually as:

```text
Player
  Transform
  Collision Shape
  Character Body
  Character Controller
  Player State
  Weapon Controller
```

`Player` is the Entity.

The attached Components describe what that Entity is and what it can do.

## Entity Hierarchy

Entities form hierarchical parent/child relationships.

For example:

```text
Player
└── Gun
    └── Muzzle
```

The hierarchy expresses structural ownership and, for spatial Entities, provides the basis for hierarchical transformation.

A child Entity has one parent within a particular hierarchy.

An Entity may itself contain child Entities, allowing arbitrarily deep authored structures.

Components are attached to Entities but are not themselves child Entities.

The Entity hierarchy is composition rather than Java class inheritance.

## Transform

A Transform describes the spatial state of an Entity.

Its fundamental authored properties include:

- position
- rotation/orientation
- scale

A Transform may be represented through the Component architecture, but spatial transformation is a fundamental capability rather than a separate Entity in the hierarchy.

Transform state is local to the Entity's parent.

For a root spatial Entity, the local Transform is relative to the Scene/world origin.

## Transform Hierarchy

Spatial child Entities inherit transformation transitively through their ancestors.

Conceptually:

```text
worldTransform(child)
    = worldTransform(parent) × localTransform(child)
```

For example:

```text
Player
└── Gun
    └── Muzzle
```

The Gun's local Transform describes its position, rotation, and scale relative to the Player.

The Muzzle's local Transform describes its spatial relationship to the Gun.

The resulting Muzzle world Transform therefore incorporates:

```text
Player Transform
    × Gun local Transform
    × Muzzle local Transform
```

This allows a child to remain positioned relative to its parent while the parent moves or rotates.

The relationship is implemented through composition, not Java inheritance.

## Spatial and Non-Spatial Entities

Not every Entity necessarily represents something spatial.

Entities may also represent logical concepts such as:

- application state
- control coordination
- game rules
- signal coordination
- presentation state
- other non-spatial behaviour

Spatial capability should therefore be modeled through composition rather than by requiring a spatial Entity subclass.

Whether every Entity should contain a Transform or only Entities requiring spatial capability should contain one remains a domain-model decision to be finalized against the existing Component architecture.

A non-spatial Entity should not require meaningless spatial state merely for structural consistency.

## Component

A `Component` is a piece of data and/or behaviour attached to an Entity that gives that Entity a particular capability.

Examples may include:

```text
Transform
Mesh Renderer
Camera
Light
Collision Shape
Rigid Body
Audio Source
Character Controller
application-specific behaviour
```

Components allow Entities to be assembled through composition rather than through deep Java inheritance hierarchies.

An Entity may contain multiple Components providing independent or cooperating capabilities.

Components are Java-backed domain concepts. Their runtime implementations may contain executable behaviour, while their authored configuration is represented as persistent data.

## Component Ownership

A Component belongs to exactly one Entity.

The owning Entity is established by JScene3D when the Component is attached.

A runtime Component has access to its owning Entity, allowing it to interact with appropriate Entity state and related Components.

Conceptually:

```text
Entity
├── Component A
├── Component B
└── Component C
```

Each Component has one owner:

```text
Component → owning Entity
```

Ownership is controlled by the engine.

A Component does not arbitrarily reassign itself to another Entity.

Moving a Component between Entities, where supported, is therefore an Entity/domain operation rather than mutation of the Component's owner reference by the Component itself.

## Component Multiplicity

An Entity may contain multiple Components.

Whether multiple Components of the same Component type are valid may depend on the semantics of that Component type.

Some capabilities naturally imply a single instance, while others may support multiple instances.

This constraint should be part of the Component type's domain metadata rather than a universal assumption made by consumers of the model.

The precise multiplicity contract remains to be defined against the existing JScene3D Component architecture.

## Component Dependencies

A Component may depend on capabilities supplied by other Components.

For example, a Component concerned with spatial rendering or physics may require transformation capability.

Such relationships should be explicit domain metadata where they need to be enforced or understood by JScene3D.

Potential dependency semantics include:

- required Component types
- incompatible Component types
- required capabilities
- optional cooperating Components

The exact dependency model remains to be designed.

It should not depend on hard-coded knowledge in individual consumers of the Component model.

## Component Lifecycle

Components may participate in runtime lifecycle processing.

The existing JScene3D lifecycle architecture should remain the starting point for the final lifecycle contract rather than introducing an unrelated replacement API.

Relevant lifecycle concepts include operations associated with:

- attachment
- initialization
- activation/start
- runtime updates
- enable/disable transitions
- detachment
- disposal or cleanup

The engine owns lifecycle sequencing and invokes Component lifecycle behaviour at the appropriate runtime stage.

Lifecycle behaviour belongs to live runtime Components.

Authored Component data does not execute lifecycle behaviour merely because it is loaded, inspected, validated, or modified.

## Authorable Component Properties

A Component explicitly defines which properties form part of its authorable configuration.

Arbitrary Java implementation fields are not automatically part of the authored domain model.

JScene3D should use Java annotations to declare authorable properties and associated metadata.

Conceptually:

```java
@EditorProperty(displayName = "Intensity", min = 0)
private float intensity;

@EditorProperty(displayName = "Color")
private Color color;
```

The exact annotation names and API remain to be designed.

The existing JScene3D property metadata and projection infrastructure should be evolved to support this model rather than replaced with an independent metadata mechanism.

## Property Metadata

Authorable properties require structured metadata describing their domain semantics.

Depending on the property, metadata may include:

- stable property identity
- display name
- description
- value type
- editable or read-only state
- required/optional state
- default value
- numeric constraints
- enum choices
- vector semantics
- color semantics
- Entity references
- Component references
- asset references
- collection semantics

This metadata forms part of the safe authoring representation of a Component type.

Java remains authoritative for the Component's domain semantics and validation rules.

## Component Type Identity

A Component type requires stable identity independent of its Java implementation class name where appropriate.

JScene3D currently uses registered type identifiers such as:

```text
io.github.glynch.jscene3d.spatial3d/transform-3d
io.github.glynch.jscene3d.physics3d/character-body-3d
```

These identifiers allow persisted authored data to refer to Component types without treating a Java class name as the serialized domain contract.

A Component instance therefore has two distinct forms of identity:

```text
Component type identity
    → what kind of Component this is

ComponentId
    → which particular Component instance this is
```

The distinction should be preserved.

## EntityDefinition

An `EntityDefinition` is a reusable authored definition of Entity content.

It allows an Entity structure and its Components to be defined once and referenced from other authored definitions.

Conceptually:

```text
EntityDefinition: Player
└── Player
    ├── Components
    └── Child Entities
```

An EntityDefinition is a first-class project asset with stable `AssetId` identity.

It is distinct from an Entity authored locally inside a SceneDefinition.

EntityDefinitions provide reusable composition without requiring reusable game objects to be represented through Java inheritance.

## Local Entities

A local Entity is authored directly as part of its containing definition.

For example:

```text
SceneDefinition: MAP01
├── Player
├── Lighting
└── Game State
```

These Entities belong directly to the MAP01 SceneDefinition.

Their `EntityId` values identify them within that authored graph.

Local Entities may contain Components and child Entities.

They do not require a separate project-level EntityDefinition merely to exist.

## Placements

A placement introduces reusable authored definition content into another authored definition.

Rather than copying the referenced definition's contents into the containing definition, the placement identifies the reusable definition through stable asset identity.

Conceptually:

```text
SceneDefinition: MAP01

MAP01 Geometry
    → EntityDefinition AssetId A

MAP01 Actors
    → EntityDefinition AssetId B
```

A placement may also provide placement-specific information such as:

- identity within the containing graph
- name
- enabled state
- arguments or parameters

The referenced definition remains independently identifiable authored content.

The exact semantics of overrides, nested placements, and placement-specific transformation require further design.

## Reusable Composition

EntityDefinitions and placements provide JScene3D's foundation for reusable authored composition.

This serves a similar general purpose to reusable-object or prefab systems in other engines without requiring JScene3D to adopt another engine's terminology or exact semantics.

The model should support reuse while preserving the distinction between:

```text
definition
```

and:

```text
placement of that definition
```

Changes to a reusable definition may therefore affect the places where that definition is used, subject to whatever override/parameter model is eventually defined.

## Asset

An `Asset` is a persistently identifiable piece of project content that participates in JScene3D's asset model.

First-class authored definitions such as SceneDefinitions and EntityDefinitions are Assets.

An Asset has semantic identity independent of:

- display name
- filename
- directory
- current filesystem path

Not every file within a Project is necessarily an Asset.

For example, raw source files, generated payloads, Java source, and internal project files may participate in other resource or build models without automatically becoming first-class JScene3D Assets.

## AssetId

`AssetId` is the persistent identity of a JScene3D Asset.

For authored definitions it is represented by a UUID stored with the definition.

Conceptually:

```text
SceneDefinition
    AssetId: A

EntityDefinition
    AssetId: B
```

The `AssetId` remains stable when the Asset is:

- renamed
- moved to another valid location
- given a different display name

Physical location is therefore metadata about an Asset rather than its identity.

Duplicating an Asset is different from moving it. A duplicate represents a new Asset and must receive a new `AssetId`.

## EntityId

`EntityId` is the persistent identity of an Entity within an authored graph.

It is distinct from the Entity's name.

For example:

```text
Entity
    EntityId: E
    name: "Player"
```

Renaming `Player` does not change `EntityId`.

Entity identity allows Components and other authored relationships to refer to a particular Entity without depending on its display name.

EntityIds are scoped within authored definition structures rather than representing independent project-level Assets.

## ComponentId

`ComponentId` identifies a particular Component instance attached to an Entity.

It is distinct from the Component's registered type identity.

For example:

```text
Component
    ComponentId: C
    type: io.github.glynch.jscene3d.spatial3d/transform-3d
```

The type identifies what kind of Component it is.

The ComponentId identifies that particular instance.

Component identity allows authored references, signals, actions, and other relationships to target a specific Component without depending on Component ordering or display labels.

## Identity Scope

JScene3D uses identity at several different scopes.

Conceptually:

```text
Project
    Project identity

Asset
    AssetId

Entity within authored graph
    EntityId

Component attached to Entity
    ComponentId

Registered type
    type identity
```

These identities serve different purposes and should not be conflated.

An Entity does not require a globally project-unique `AssetId` merely because it has an `EntityId`.

Likewise, a Component type identifier does not identify a particular Component instance.

## Asset References

References to first-class Assets should use stable asset identity rather than physical location wherever possible.

An `AssetRef` identifies an Asset using its `AssetId`.

A physical path may exist as metadata or a non-authoritative resolution hint, but it is not the identity of the referenced Asset.

Conceptually:

```text
AssetRef
    ↓
AssetId
    ↓
Asset Catalog
    ↓
current physical location
```

This allows an Asset to move or be renamed without invalidating semantic relationships that refer to it.

## Entity and Component References

References to Entities and Components use their corresponding persistent identities within the appropriate authored graph.

An Entity reference identifies an `EntityId`.

A Component reference identifies the appropriate Entity and `ComponentId`.

Conceptually:

```text
Entity target
    EntityId

Component target
    EntityId
    ComponentId
```

This avoids references based on names, hierarchy paths, or Component ordering.

Names and hierarchy structure may change while the referenced object retains its identity.

## Resource References

JScene3D currently supports multiple resource-reference namespaces with different semantics.

Important forms include:

```text
asset:
import:
project:
```

These do not all represent the same kind of identity.

`asset:` identifies a source asset declared by the Project using its project-local symbolic identifier.

`import:` identifies generated content through an import identity and importer-defined artifact identity.

`project:` identifies content by a path relative to the Project root.

These reference forms should remain semantically distinct unless a future domain-model change provides a reason to unify them.

## Path-Based References

Some JScene3D content is currently addressed directly by physical path.

A path-based reference is inherently different from a stable identity reference.

For example:

```text
project:application/branding/images/title.png
```

identifies a physical project-relative location.

Moving or renaming that file changes the target of the reference.

Path-based references remain reasonable for content whose identity is intentionally its project-relative location.

They should not be used for first-class Assets that already possess stable `AssetId` identity.

## Asset Catalog

The Asset Catalog is the project-level mapping between persistent Asset identity and the Asset's current metadata and location.

Conceptually:

```text
AssetId
    ↓
Asset Metadata
    ├── Asset kind
    ├── format version
    └── current source location
```

The catalog allows physical location to change without changing semantic identity.

The Asset Catalog is derived from Project content rather than being the persistent source of Asset identity.

The Asset itself remains authoritative for its `AssetId`.

## Asset Discovery

First-class authored Assets may be discovered independently of fixed directory names.

The current JScene3D model recursively discovers authored SceneDefinitions and EntityDefinitions within the Project boundary.

The future SceneDefinition model should retain this property.

A SceneDefinition does not become a Scene because it is stored in a directory named:

```text
scenes/
```

Likewise, an EntityDefinition does not depend on being stored in:

```text
entities/
```

Physical organization and semantic classification are separate concerns.

## Physical Asset Organization

JScene3D does not require type-specific directories for authored Assets unless a particular technical subsystem genuinely requires a fixed location.

Projects may adopt conventional structures such as:

```text
scenes/
entities/
resources/
imports/
assets/
```

but these are organizational conventions rather than part of Asset identity or classification.

A Project may instead organize authored content according to application-specific domains.

For example:

```text
episode1/
    map01/
    map02/

player/

enemies/

ui/
```

The domain model remains the same regardless of valid physical organization.

## Resource

A `Resource` is structured data consumed by Components or other JScene3D systems that is not necessarily an independently instantiated Entity definition.

Examples may include:

- collision shapes
- materials
- images
- audio
- models
- presentation data
- other extension-defined resource types

Resources have registered semantic types and versions.

For example, a resource may declare:

```text
type: io.github.glynch.jscene3d.physics3d/capsule-collision-shape-3d
typeVersion: 1
```

Resources are not currently uniform with SceneDefinitions and EntityDefinitions in their identity model.

Whether independently authored Resources should eventually become first-class Assets with `AssetId` identity remains an open domain decision.

## Source Asset

A source asset is project content supplied as input to JScene3D or an importer.

Source assets are explicitly declared by the Project and have project-local symbolic identities.

Examples may include:

- source images
- audio
- models
- archives
- external game data
- other importer inputs

A source asset is distinct from a generated Resource or generated EntityDefinition produced from that source.

Conceptually:

```text
Source Asset
    ↓
Importer
    ↓
Generated JScene3D Content
```

The symbolic source-asset identity provides indirection between consumers and the source asset's physical path.

## Import

An `Import` defines a transformation from source content into JScene3D content.

An Import identifies:

- its own project-local import identity
- its source
- the importer responsible for processing that source
- selection information where appropriate
- importer-specific settings

Conceptually:

```text
Import Definition
    ├── source
    ├── importer
    ├── selection
    └── settings
          ↓
       Importer
          ↓
    Published Artifacts
```

Imports are explicit Project concepts rather than implicit filesystem scanning operations.

## Generated Content

Importers may generate JScene3D content such as:

- EntityDefinitions
- Resources
- payloads

Generated content is derived from source content and importer configuration rather than directly authored by the developer.

Generated content should have stable logical identity across re-import where the source and importer-defined identity remain stable.

Generated content is distinct from directly authored content even when both ultimately participate in the same runtime domain.

Generated publication/cache data is engine-managed and is not itself the authoritative manually authored source.

## Generated Entity Definitions

An importer may produce EntityDefinitions.

Generated EntityDefinitions participate in definition resolution alongside authored EntityDefinitions.

Their stable `AssetId` values may be derived deterministically from stable import identity and importer-defined source identity.

This allows references to generated definitions to survive regeneration when the logical imported object remains the same.

Generated and authored definitions therefore share the same semantic role while differing in provenance.

## Authored and Generated Provenance

The origin of content is distinct from its semantic type.

For example:

```text
EntityDefinition
    provenance: authored
```

and:

```text
EntityDefinition
    provenance: generated
```

are both EntityDefinitions.

The distinction describes where they came from, not what they fundamentally are.

This principle allows runtime and composition systems to operate on semantic domain concepts without requiring every consumer to understand the physical mechanism that produced them.

## Authoring Model

The authoring model is the persistent, non-executable representation of JScene3D application content.

It includes concepts such as:

- SceneDefinitions
- EntityDefinitions
- Entities
- Component configuration
- placements
- connections
- Resources
- Imports
- asset references

Authoring data describes what should exist when content is instantiated.

Loading authoring data does not imply creating live runtime Components or executing application behaviour.

## Runtime Model

The runtime model is the live executable representation created from authored definitions and Project configuration.

Runtime construction resolves authored definitions and creates the corresponding live Entities, Components, resources, and relationships inside a World.

Conceptually:

```text
Authored definition
       ↓
   instantiate
       ↓
Live runtime objects
```

Runtime objects may contain state that does not belong in the authored definition.

For example, a runtime Component may contain transient state accumulated during execution while its authored Component configuration contains only the initial values required to construct it.

## Authoring and Runtime Separation

The authoring and runtime models must remain explicitly separated.

Operations such as:

- loading a Project
- discovering Assets
- reading a SceneDefinition
- inspecting Component metadata
- validating authored properties
- modifying authored definitions

must not require application runtime code to execute.

Runtime execution begins only when JScene3D explicitly constructs a runtime environment and instantiates authored content.

This separation allows project content to be safely processed without trusting or executing arbitrary application behaviour.

## Component Metadata and Runtime Implementations

A Component type has both authoring information and a runtime implementation.

These are related but serve different purposes.

Conceptually:

```text
Component Type

├── Authoring Metadata
│   ├── type identity
│   ├── properties
│   ├── constraints
│   ├── capabilities
│   └── presentation metadata
│
└── Runtime Implementation
    └── Java Component
```

Authoring infrastructure consumes declarative metadata.

Runtime infrastructure loads the trusted Java implementation when executable content is instantiated.

The authoring model therefore does not require loading arbitrary Component implementation classes merely to understand their authored configuration.

## Extension Metadata

Extensions provide declarative metadata describing the JScene3D domain types they contribute.

This may include:

- Component types
- Component properties
- capabilities
- resource types
- importer types
- settings
- presentation metadata

Extension metadata forms part of the safe boundary between authored content and executable Java implementation.

Application-specific Components should participate in the same model as built-in Components.

The eventual annotation system may contribute to producing this metadata, but persisted and authoring-facing contracts should not depend on executing application classes.

## Signals and Actions

Components may expose communication endpoints through signals and actions.

A signal represents an event emitted by a Component.

An action represents an operation that can be invoked on a Component.

Authored connections link a signal endpoint to an action endpoint.

Conceptually:

```text
Component A
    signal: fired
        ↓
Connection
        ↓
Component B
    action: receive-fired
```

Connections identify their endpoints using stable Entity and Component identities together with stable endpoint identities.

Signals and actions allow Components to cooperate without requiring direct Java coupling between every participating Component type.

## Connections

A `Connection` is an authored relationship between a signal endpoint and an action endpoint.

Conceptually:

```text
Connection
├── Signal Target
│   ├── EntityId
│   ├── ComponentId
│   └── EndpointId
│
└── Action Target
    ├── EntityId
    ├── ComponentId
    └── EndpointId
```

Connections belong to authored composition rather than being inferred from physical Entity hierarchy.

The Entity hierarchy describes structural composition.

Connections describe behavioural communication.

These are independent relationships.

## Project Identity

A Project has identity distinct from the Assets contained within it.

The current Project identity uses a reverse-domain-style identifier.

For example:

```text
io.github.example.mygame
```

Project identity provides a namespace and stable application-level identity.

It should not depend on the Project directory name or physical filesystem location.

Asset, Entity, Component, extension, and registered-type identities remain separate identity domains.

## Registered Type Identity

JScene3D uses stable registered identifiers for extensible domain types.

A registered type identifier should describe the semantic contract rather than merely expose the implementation class name.

Conceptually:

```text
namespace/type-name
```

For example:

```text
io.github.glynch.jscene3d.spatial3d/transform-3d
```

Registered type identity allows persisted authored data to remain decoupled from Java package/class implementation details.

Registered types may also carry an explicit type version to support evolution of their persisted representation.

## Type Versioning

Persisted extensible types may have a `typeVersion`.

For example:

```text
type: io.github.glynch.jscene3d.physics3d/character-body-3d
typeVersion: 1
```

The type identifier answers:

> What semantic type is this?

The type version answers:

> Which persisted contract for that type is this data using?

Type-version evolution is separate from the overall document format version of the containing authored file.

This distinction should be preserved.

## Document Format Versioning

Persisted JScene3D documents have format versions independent of individual Component or Resource type versions.

For example, a SceneDefinition document may have:

```text
formatVersion: 1
```

while Components contained within it independently declare their own:

```text
typeVersion: 1
```

Document format versioning governs the structure of the containing serialized document.

Type versioning governs the persisted contract of a registered extensible type.

These versioning domains should remain distinct.

## Serialization

Serialization is the persistent representation of the domain model.

Serialized documents should preserve semantic identity and relationships rather than deriving them from incidental document ordering or physical location.

For example, persisted authored content may include:

- AssetId
- EntityId
- ComponentId
- registered type identity
- type version
- properties
- child relationships
- placements
- connections
- asset references

The serialized format is a representation of the domain model rather than the definition of the domain model itself.

Domain concepts should therefore not be constrained unnecessarily by incidental details of the current JSON representation.

## Scene Serialization

The current authored Scene concept is serialized as `SceneDefinition`.

The future terminology refactor will replace this with Scene terminology.

This affects persisted concepts including the current:

```text
*.scene.json
"assetType": "scene-definition"
scene-definition schema
SCENE_DEFINITION asset kind
```

The intended future terminology is conceptually:

```text
*.scene.json
"assetType": "scene-definition"
SceneDefinition
SCENE_DEFINITION
```

The exact compatibility and migration strategy remains to be designed.

Existing stable `AssetId` values should be preserved through this terminology migration.

## Legacy SceneDefinition

JScene3D currently contains an older `project.scene.SceneDefinition` model based on an earlier typed-node/controller architecture.

That model predates the current hierarchical Entity/Component `SceneDefinition` architecture and is not part of the domain model defined here.

It should be retired before the current `SceneDefinition` assumes the `SceneDefinition` name.

This avoids retaining two unrelated concepts with the same domain terminology.

## Terminology

The intended core terminology is:

```text
Project
SceneDefinition
World
Entity
Component
EntityDefinition
Placement
Asset
Resource
Import
```

The distinction between the two most easily confused terms is:

```text
SceneDefinition = authored composition
World           = live runtime environment
```

The Project's runtime entry point is:

```text
mainScene
```

## Open Domain Decisions

The following domain questions remain intentionally unresolved:

- whether every Entity has a Transform or only spatial Entities have one
- the exact Component lifecycle contract
- Component multiplicity rules and how they are declared
- Component dependency/capability metadata
- the exact Java annotation API for authorable properties
- whether independently authored Resources should receive `AssetId` identity
- the precise semantics of placement overrides and parameters
- nested EntityDefinition/placement semantics
- duplication rules for EntityIds, ComponentIds, and internal references
- the final compatibility strategy for `SceneDefinition` to `SceneDefinition`
- whether generated SceneDefinitions should be supported
- whether additional currently path-based project relationships should become identity-based

These decisions should be resolved separately before implementation where they affect the corresponding subsystem.

## Imported Content Ownership

Imported content has a different ownership model from directly authored Project content.

An external source such as a WAD, glTF file, or another supported format is processed by an Import Definition and importer.

Conceptually:

```text
External Source
      ↓
Import Definition
      ↓
Importer
      ↓
Generated Content
```

Generated content may include:

- EntityDefinitions
- Resources
- payloads
- other supported generated artifacts

The external source and Import Definition remain authoritative for this content.

Generated artifacts are derived content rather than authored Project content.

## Linked Generated Content

Generated content remains linked to its import.

Conceptually:

```text
External Source
      ↓
Import
      ↓
Generated Content
```

In this mode:

- the external source remains authoritative;
- generated content is read-only;
- generated artifacts may be regenerated when import inputs change;
- re-import may replace the active generated publication;
- generated content should not be manually modified;
- stable generated identities should be preserved across re-import where the importer's source identities remain stable.

The generated cache is an implementation mechanism for storing and reusing these derived artifacts. It is not an authoring location.

Opening or running a Project should consume an existing valid publication rather than unnecessarily processing the external source again.

## Authored Content Created from Imported Content

JScene3D should support a future explicit operation that converts generated content into normal authored Project content.

The final terminology for this operation remains to be chosen. Possible terms include:

```text
Make Editable
Convert to Authored
Extract to Project
```

Conceptually:

```text
External Source
      ↓
Import
      ↓
Generated Content
      ↓
explicit conversion
      ↓
Authored Project Content
```

The operation represents an ownership transition.

Before conversion:

```text
external source owns the content
```

After conversion:

```text
Project owns the authored content
```

The resulting content participates in the normal authored domain model and may include SceneDefinitions, EntityDefinitions, Entities, Components, Resources, or other appropriate authored concepts.

## Independence After Conversion

Converted content is independent of the original import.

The original external source is no longer authoritative for the converted content.

Subsequent re-import of the source must not overwrite or modify the authored copy.

For example:

```text
WAD
 ↓
generated MAP01
 ↓
Make Editable
 ↓
authored MAP01 SceneDefinition
```

Once MAP01 has become authored content, its Entities and Components can be modified according to the normal authored domain model.

Changes to `freedoom2.wad` and subsequent re-import do not alter that authored SceneDefinition.

The original WAD and Import Definition may remain in the Project when other content continues to depend on them, but the converted content itself does not remain semantically linked to its generated source.

## Imported Content Is Not an Override Layer

The domain model does not currently include a general authored override or patch layer over generated content.

In particular, it does not define a model such as:

```text
Generated Content
       +
Authored Overrides
```

Generated content is either used as generated, with the external source remaining authoritative, or explicitly converted into Project-owned authored content.

This keeps content ownership unambiguous and avoids introducing source/override precedence, merge semantics, orphaned overrides, or reconciliation rules without a demonstrated requirement.

A future override model may be considered if concrete use cases establish that the two ownership modes are insufficient.

## Import Granularity

Importers should be capable of producing semantically useful generated artifacts rather than unnecessarily monolithic generated content.

For example, an importer may produce reusable EntityDefinitions, Resources, and other independently identifiable artifacts from a larger external source.

This allows generated content to participate naturally in JScene3D composition while remaining import-owned.

The appropriate granularity is importer- and format-dependent and is not prescribed universally by the domain model.

## Conversion Granularity

The exact granularity of conversion from generated to authored content remains unresolved.

Potential possibilities include converting:

- an entire generated SceneDefinition
- an individual generated EntityDefinition
- a Resource
- a group of related generated artifacts
- a complete dependency closure

The domain model does not yet prescribe which of these must be supported.

## Conversion Identity

Conversion creates authored Project content rather than changing the ownership of the existing generated artifact in place.

The authored result must therefore have identity appropriate to independently owned Project content.

The precise identity rules remain to be designed, including:

- allocation of the new `AssetId`;
- treatment of existing EntityIds;
- treatment of existing ComponentIds;
- rewriting of internal references;
- handling references between multiple converted Assets.

The generated artifact and authored result must not accidentally occupy the same Asset identity while both exist in the Project.

## Conversion Dependencies

Generated content may depend on other generated artifacts.

For example:

```text
Generated EntityDefinition
    ↓
Generated Material Resource
    ↓
Generated Texture Resource
    ↓
Generated Payload
```

The conversion model must eventually define whether dependencies are:

- converted together with the selected content;
- left linked to the original import;
- selectable by the developer;
- handled according to another explicit rule.

A fully independent authored conversion cannot silently retain dependencies on import-owned content while presenting itself as completely detached.

The exact dependency-closure policy remains an open design decision.

## Import Provenance

Generated content has import provenance.

Its provenance includes concepts such as:

- Import identity
- source identity
- generated artifact identity
- import generation
- importer identity/version

This provenance explains where generated content originated and how it can be regenerated.

Authored content created through conversion may optionally retain informational provenance describing where it originally came from.

Such historical provenance must not imply an ongoing synchronization relationship with the source.

## Open Imported-Content Decisions

The following questions remain intentionally unresolved:

- final terminology for the conversion operation;
- which generated artifact types can be converted;
- conversion granularity;
- dependency-closure behaviour;
- whether partially linked conversion is ever supported;
- allocation of new AssetIds;
- preservation or regeneration of EntityIds and ComponentIds;
- internal and external reference rewriting;
- whether consumers of generated content can explicitly be redirected to an authored conversion;
- how historical import provenance is represented;
- whether generated SceneDefinitions should become a supported importer artifact type;
- how generated content should be garbage-collected when imports or generations become obsolete;
- whether future concrete requirements justify an override/patch model.

These questions should be resolved when the conversion workflow is designed rather than being embedded prematurely into the core import architecture.
