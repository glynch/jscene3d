# JScene3D Editor UX

## Purpose

This document defines the user experience for the first version of the JScene3D Editor.

It translates the JScene3D domain model and editor user stories into a coherent authoring environment.

The editor is built on Code OSS, but JScene3D should present itself as a dedicated 3D/game authoring environment rather than as a lightly customized source-code editor.

This document focuses on the core workflows required for the first usable editor.

Detailed domain semantics are defined separately in:

```text
docs/design/domain-model.md
```

User requirements and acceptance criteria are defined in:

```text
docs/design/editor-user-stories.md
```

Visual references are stored in:

```text
docs/design/mockups/
```

## Design Principles

The editor should prioritize semantic JScene3D concepts over their underlying physical representation.

Developers primarily work with:

- Projects
- Scenes
- Entities
- EntityDefinitions
- Components
- Resources
- runtime state

Files, JSON, Java source, Maven configuration, generated cache content, and other implementation details remain accessible where appropriate but are not the primary authoring model.

The editor should preserve the strengths of Code OSS for Java development while providing a purpose-built environment for JScene3D authoring.

## Authoring and Runtime Separation

The editor must make a clear distinction between authored content and live runtime state.

Authoring operates on persistent definitions such as:

```text
SceneDefinition
EntityDefinition
Entity
Component configuration
```

Runtime execution operates on live objects instantiated into a World.

The editor must never make runtime state appear to be authored state or silently persist runtime changes into authored content.

This distinction should be visible throughout the user experience.

## Overall Workbench Layout

The default Scene-authoring workspace uses a layout optimized for 3D authoring:

```text
┌────────────────┬──────────────────────────────────┬────────────────┐
│                │                                  │                │
│   Hierarchy    │                                  │                │
│                │          Scene View              │   Inspector    │
│                │                                  │                │
├────────────────┤                                  │                │
│                │                                  │                │
│    Project     │                                  │                │
│                │                                  │                │
└────────────────┴──────────────────────────────────┴────────────────┘
```

The default regions are:

- Hierarchy — upper left
- Project — lower left
- Scene View — central editor area
- Inspector — right

The Scene View receives the largest portion of the workspace.

This structure is intentionally similar to established 3D authoring tools because the arrangement provides a clear relationship between Scene structure, visual content, reusable Project content, and selected-object properties.

JScene3D uses its own terminology, domain model, visual styling, and workflows.

## Code OSS Workbench Integration

JScene3D retains useful Code OSS workbench capabilities rather than replacing the entire workbench.

The Activity Bar remains available on the far left.

Standard editor tabs remain available in the central editor area.

Java and other source files open in normal Code OSS text editors.

Workbench infrastructure such as:

- source control
- terminal
- Java language support
- diagnostics
- source navigation
- extensions

remains available where appropriate.

JScene3D-specific authoring views should nevertheless be the default experience when working with a JScene3D Project.

Generic Code OSS views should not appear temporarily during JScene3D startup or Project loading merely because JScene3D-specific state has not finished initializing.

## JScene3D Visual Direction

The first editor mockups establish a dark visual direction using:

- dark charcoal and slate surfaces
- restrained borders and separators
- blue as the primary selection and action accent
- clear semantic status colors where necessary
- compact controls
- high information density without excessive visual decoration

The visual direction is intentionally darker and more specialized than the current default Code OSS appearance.

The mockups define a design direction rather than an exact pixel-level theme specification.

The JScene3D theme should eventually be implemented using normal workbench theming mechanisms wherever practical.

## Primary Authoring Context

A Scene is the primary spatial authoring context.

When a Scene is active, the principal relationship is:

```text
Hierarchy
    ↕
Scene View
    ↕
Inspector
```

The Hierarchy presents the authored Entity structure.

The Scene View presents the spatial representation of that authored content.

The Inspector presents the selected Entity and its Components.

These surfaces operate on the same underlying authored state rather than maintaining independent copies.

The Project view remains available alongside them for navigating Scenes, EntityDefinitions, and other Project content.

## Central Editor Area

The central editor area supports multiple kinds of JScene3D and source-code editors.

Examples include:

- Scene Editor
- EntityDefinition Editor
- Java source editor
- structured source representation
- other specialized editors introduced later

Multiple editors may be open simultaneously using normal workbench tabs.

The active editor determines the appropriate supporting JScene3D context.

For example, activating a Scene Editor makes that Scene the active authoring Scene for the Hierarchy and Inspector.

Opening or activating an editor does not alter Project runtime configuration such as the Main Scene.

## Bottom Panel

A bottom panel is available for information such as:

- diagnostics
- runtime console
- build output
- problems
- profiling information

The bottom panel is not required to remain permanently open.

The default Scene-authoring workspace should prioritize vertical space for the Scene View.

The panel may open automatically when information requiring the developer's attention is produced, subject to the normal notification and diagnostics design.

## Core Mockup

The primary visual reference for the overall authoring workspace is:

```text
docs/design/mockups/scene-authoring.png
```

This mockup establishes the baseline layout and visual direction from which the other first-version editor states are derived.

## Welcome State

When no Project is open, JScene3D presents a dedicated Welcome experience.

The Welcome screen provides:

- Create Project
- Open Project
- Clone Repository
- Recent Projects
- Get Started

`Get Started` provides the future entry point for tutorials, documentation, examples, and other learning material.

The Welcome screen occupies the central editor area and is the primary focus of the workbench.

Project-dependent authoring views do not display misleading empty states when no Project exists.

The editor should not show generic Code OSS empty-editor content, Chat, or automatically select Explorer.

Visual reference:

```text
docs/design/mockups/welcome.png
```

## Create Project

`Create Project` opens a focused Project-creation dialog.

The first version requests only information necessary to establish the Project:

- Project name
- location
- Project/package ID

Java and Maven configuration that JScene3D can determine itself is created automatically.

The developer is not required to configure a Main Scene or create an initial Scene during Project creation.

A newly created Project may therefore contain no Scenes and have no Main Scene.

After creation, the new Project enters the same loading lifecycle as an existing Project being opened.

Visual reference:

```text
docs/design/mockups/create-project.png
```

## Open Project

An existing Project can be opened from:

- the Welcome screen
- Recent Projects
- the appropriate application menu or command
- a Project path supplied when launching JScene3D from the command line

JScene3D accepts either an appropriate Project descriptor or a directory containing a JScene3D Project.

All entry points converge on the same Project-opening lifecycle.

The selected Project is validated before becoming active.

A Project does not require a Main Scene merely to be opened for authoring.

The native operating-system file or folder picker may be used for selecting an existing Project. A custom JScene3D Open Project dialog is not required for the first version.

## Project Loading

Opening or creating a Project enters an explicit loading state.

Project loading must appear as a deliberate JScene3D operation rather than exposing intermediate Code OSS initialization states.

The loading experience should clearly identify the Project being opened.

A visible progress indication communicates that work is occurring.

Broad loading phases may be shown where useful, but implementation details should not overwhelm the developer.

Examples of useful phases may include:

```text
Reading Project
Loading Scenes
Loading Entity Definitions
Preparing workspace
```

The exact phases depend on the implementation and should only be displayed when they represent meaningful progress.

The developer must not see temporary states such as:

```text
Drag a view here to display
```

Nor should unrelated Explorer, Chat, empty editor, or other Code OSS views flash into view while JScene3D initializes.

Project-dependent views should remain in an explicit loading state until their data is available.

Visual reference:

```text
docs/design/mockups/project-loading.png
```

## Project Loading Completion

The Project does not become visually ready until the minimum state required for normal authoring has been established.

The transition should conceptually be:

```text
Welcome
   ↓
Project Loading
   ↓
Project Ready
```

rather than:

```text
Welcome
   ↓
partially initialized workbench
   ↓
empty views
   ↓
views rearrange
   ↓
Project Ready
```

When loading succeeds, JScene3D transitions directly to either:

- the restored authoring workspace; or
- the appropriate initial Project state when there is no previous workspace to restore.

If loading fails, JScene3D presents a clear failure state rather than leaving a partially active Project.

## Project Open with No Scene Open

A valid Project does not require an open Scene.

This state occurs naturally when:

- a new Project has just been created;
- a Project contains no Scenes;
- the developer has closed all Scene editors;
- there is no previous Scene workspace to restore.

The semantic Project view remains available and populated.

The central editor area presents a JScene3D-specific Project state rather than generic Code OSS empty-editor content.

The primary actions are:

- Create Scene
- Open Scene

Other useful Project-level actions may be available without overwhelming the primary workflow.

The Hierarchy does not pretend that an active Scene exists.

The Inspector presents an appropriate no-selection state.

A Project with no Main Scene remains fully usable for authoring.

`Run Project` remains unavailable until a valid Main Scene is configured.

Visual reference:

```text
docs/design/mockups/project-open-no-scene.png
```

## Semantic Project View

The Project view presents JScene3D content according to its semantic role rather than merely reproducing the filesystem.

The first version should prioritize core concepts such as:

- Scenes
- Entity Definitions
- relevant Resources
- Project source/code entry points where useful

A Scene is recognized as a Scene because of its JScene3D domain type, not because it happens to reside in a directory named `scenes`.

Likewise, an EntityDefinition does not depend on being stored in a particular directory.

The physical Project structure may follow recommended conventions, but the semantic Project view should not make those conventions mandatory.

The Project view provides the primary navigation surface for opening JScene3D authored content.

## Project View and Explorer

The semantic Project view and Code OSS Explorer serve different purposes.

The Project view answers questions such as:

```text
What Scenes does this Project contain?
What reusable EntityDefinitions are available?
What JScene3D content can I author?
```

Explorer answers questions about physical files and directories.

Explorer remains available for developers who need filesystem-level access, Java source navigation, configuration files, or other physical Project content.

It should not be necessary to use Explorer for normal Scene and Entity authoring.

Where appropriate, semantic Project items may provide actions such as:

```text
Reveal in Explorer
Open Source
```

This preserves access to the underlying representation without making it the primary authoring model.

## Main Scene Presentation

The Project view clearly identifies the Project's Main Scene when one is configured.

Main Scene status is a property of Project runtime configuration rather than editor state.

Opening, closing, or activating a Scene Editor does not change which Scene is the Main Scene.

The developer can explicitly designate a Scene as the Main Scene.

When no Main Scene exists:

```text
Run Project = unavailable
```

When a valid Main Scene exists:

```text
Run Project = available
```

The Project remains valid for authoring in either state.

## Workspace Restoration

JScene3D restores useful editor workspace state when a previously opened Project is reopened.

Restorable state includes, where practical:

- open editor tabs
- active editor tab
- JScene3D view layout
- relevant expanded/collapsed state
- Scene authoring viewpoints

Workspace state is editor state and remains separate from Project runtime configuration.

Restoration occurs as part of Project loading.

The developer should not watch the editor open unrelated views and then visibly rearrange itself into the restored layout.

Missing or deleted content should not prevent the remainder of the workspace from being restored.

When no previous workspace state exists, JScene3D uses the normal initial Project workspace.

## Scene Creation

A new Scene is created through a dedicated Scene-creation workflow rather than by creating or editing its serialized representation directly.

The developer provides:

- Scene name
- template
- physical location

JScene3D allocates the Scene's persistent identity and creates the corresponding SceneDefinition.

Creating a Scene does not automatically make it the Project's Main Scene.

If the Project does not yet have a Main Scene, the developer may explicitly designate the new Scene later.

Visual reference:

```text
docs/design/mockups/new-scene.png
```

## Scene Templates

Scene creation uses templates so that JScene3D does not need to assume that every new Scene requires the same initial content.

The first version provides:

### Empty

Creates a SceneDefinition containing no Entities.

### Basic 3D

Creates a SceneDefinition containing a minimal starting point for spatial authoring:

```text
Camera
Directional Light
```

Templates create normal authored Scene content. They do not introduce special runtime Scene types.

Additional templates may be introduced later without changing the fundamental Scene model.

## Scene Editor

Opening a Scene creates or activates a Scene Editor tab in the central editor area.

Multiple Scenes may be open simultaneously.

Only one Scene Editor is active at a time.

The active Scene determines the context presented by:

- Hierarchy
- Scene View
- Inspector selection

Opening a Scene does not:

- change the Main Scene
- run the Project
- create a runtime World
- execute runtime Component behaviour

Opening a Scene that is already open activates its existing editor rather than creating an unnecessary duplicate.

## Scene and Game Views

The central Scene workspace provides distinct Scene and Game contexts.

### Scene

The Scene view is the visual authoring environment.

It uses an editor-owned camera and allows the developer to navigate freely around authored spatial content.

Moving the Scene-view camera does not modify an authored Camera Entity.

### Game

The Game view represents runtime/application rendering rather than Scene authoring.

It becomes particularly relevant while a Project or Scene is running.

The distinction is:

```text
Scene → authoring view

Game  → runtime/application view
```

The exact Game-view behaviour depends on runtime state and available application cameras.

## Scene View

The Scene View is the dominant area of the Scene-authoring workspace.

It presents a visual representation of the active SceneDefinition.

The developer can use it to:

- navigate around the Scene
- select spatial Entities
- inspect spatial relationships
- translate Entities
- rotate Entities
- scale Entities

The Scene View operates on the same authored state represented by the Hierarchy and Inspector.

A change made visually is immediately reflected by the corresponding authored properties.

## Scene View Camera

The Scene View uses its own editor camera.

This camera exists for authoring navigation and is not an Entity or Component within the SceneDefinition.

Typical navigation includes:

- orbit
- pan
- zoom
- focus on selection
- free navigation where appropriate

Scene-view camera state may be persisted as editor workspace state.

It is not persisted as part of the SceneDefinition unless a future explicit feature introduces saved authoring viewpoints.

## Scene Manipulation Tools

The Scene View provides compact tools for common spatial authoring operations.

The first version should support:

- selection
- move
- rotate
- scale

The controls should remain easily accessible without consuming substantial viewport space.

A compact floating Scene toolbar is the current design direction.

Additional controls may later include concepts such as:

- local/world orientation
- pivot modes
- snapping
- visualization options

These are not required to block the first Scene-authoring implementation.

## Transform Gizmos

Selecting a spatial Entity allows its Transform to be manipulated visually.

The appropriate gizmo is shown according to the active manipulation tool.

Transform manipulation modifies the Entity's authored Transform.

Conceptually:

```text
Scene gizmo
    ↓
authored Transform
    ↓
Inspector
```

and:

```text
Inspector
    ↓
authored Transform
    ↓
Scene representation
```

There is only one underlying authored Transform state.

Continuous manipulation should participate sensibly in undo/redo rather than producing an excessive number of independent history entries.

## Hierarchy

The Hierarchy occupies the upper-left portion of the default Scene-authoring workspace.

It presents the Entity hierarchy of the active Scene.

For example:

```text
MAP01
├── Player
│   ├── Player View
│   └── Weapon
├── Geometry
├── Lighting
└── Game State
```

The Hierarchy represents Scene structure.

It does not represent:

- the complete Project
- Components as child nodes
- the physical filesystem
- the runtime World while in normal authoring mode

Components belong to their Entity and are presented through the Inspector.

## Hierarchy Selection

Selecting an Entity in the Hierarchy selects that same authored Entity in the Scene authoring context.

For a spatial Entity, the Scene View reflects the selection visually.

The Inspector displays the selected Entity and its Components.

Likewise, selecting an Entity directly in the Scene View updates the Hierarchy selection.

Conceptually:

```text
Hierarchy
    ↕
Selection
    ↕
Scene View
    ↓
Inspector
```

Selection belongs to the active Scene.

Switching to another Scene does not incorrectly carry an Entity selection from the previous Scene into the newly active Scene.

## Hierarchy Entity Creation

The Hierarchy provides a compact `+` action for adding content to the active Scene.

The first-version menu includes concepts such as:

```text
Empty Entity

3D
    Cube
    Sphere
    Plane
    Camera

Light
    Directional Light
    Point Light
    Spot Light

Place Entity Definition…

Create Entity Definition…
```

The exact grouping may evolve during implementation, but the menu should remain based on JScene3D semantic concepts.

Visual reference:

```text
docs/design/mockups/add-entity-menu.png
```

## Primitive Entities

Convenience entries such as:

```text
Cube
Sphere
Plane
```

do not represent special Entity subclasses.

They are authoring shortcuts that create an Entity with an appropriate initial Component composition.

For example, a Cube may be created with the Components necessary to provide:

- spatial Transform
- mesh representation
- rendering

Additional default Components should only be added where they are part of the defined creation preset.

The resulting object remains a normal Entity composed from Components.

## Entity Creation and Parenting

Creating an Entity while no parent is selected creates a root Entity.

Creating a child beneath an existing Entity establishes the corresponding authored parent/child relationship.

New Entities are selected after creation so that their configuration can be edited immediately.

Reparenting through the Hierarchy changes the authored Entity hierarchy.

The editor prevents cyclic hierarchy relationships.

The exact spatial Transform policy during reparenting remains governed by the domain decision on preserving local versus world Transform.

## EntityDefinitions in the Hierarchy

A placement of an EntityDefinition participates in the Scene's authored structure but remains distinguishable from a purely local Entity.

The Hierarchy should communicate that the content originates from a reusable definition without making the hierarchy difficult to read.

The exact iconography or decoration will be finalized during implementation.

The developer should be able to distinguish:

```text
local Entity
```

from:

```text
placement of shared EntityDefinition
```

and navigate to the shared definition when necessary.

Detailed placement workflows beyond those required for the first editor may be deferred.

## Scene Authoring Visual Reference

The primary Scene-authoring reference remains:

```text
docs/design/mockups/scene-authoring.png
```

The Entity creation interaction is illustrated by:

```text
docs/design/mockups/add-entity-menu.png
```

## Inspector

The Inspector occupies the right side of the default authoring workspace.

It presents the authored state of the currently selected object.

When an Entity is selected, the Inspector presents:

- Entity-level information
- Transform where applicable
- attached Components
- authorable Component properties
- Component actions
- Add Component

Components are displayed as sections rather than as children in the Hierarchy.

## Entity Inspector

The top of the Entity Inspector identifies the selected Entity and provides appropriate Entity-level state.

The Inspector then presents its Components as clearly separated sections.

For example:

```text
Crate

Transform
    Position
    Rotation
    Scale

Mesh Renderer
    Mesh
    Material
    Cast Shadows

Box Collider
    Center
    Size

Crate Behaviour
    Breakable
    Health

[ Add Component ]
```

The exact controls shown for each Component are determined by its authoring metadata.

## Transform Inspector

For a spatial Entity, Transform receives first-class presentation.

The initial presentation should expose:

```text
Position    X   Y   Z
Rotation    X   Y   Z
Scale       X   Y   Z
```

These values represent the Entity's local Transform relative to its parent.

Transform changes made in the Inspector immediately update the Scene View.

Transform changes made through Scene gizmos immediately update the Inspector.

The Inspector should provide an author-friendly representation even when the runtime uses a different mathematical representation internally.

## Component Sections

Each Component attached to the Entity appears as a distinct Inspector section.

A Component section includes:

- Component display name
- authorable properties
- relevant Component actions
- validation state where necessary

Sections may be collapsible to manage vertical space.

The Inspector does not expose arbitrary Java implementation fields.

Only properties defined by JScene3D's Component authoring metadata are presented.

## Property Controls

The Inspector chooses property controls according to Java-provided property metadata.

The first version should support the property forms required by existing JScene3D Components, including where applicable:

- boolean
- integer
- floating-point number
- string
- enum
- vectors
- colors
- Asset references
- Entity references
- Component references
- Resource references
- collections

Controls should reflect semantic metadata rather than merely the serialized JSON representation.

For example, a color property should be presented as a color rather than requiring the developer to edit an arbitrary numeric array.

## Property Validation

Component properties are validated according to their declared metadata and domain rules.

Validation may identify:

- missing required values
- invalid numeric ranges
- invalid enum values
- unresolved references
- incompatible reference targets
- invalid collection contents
- other property-specific constraints

Problems should be presented close to the affected property where practical.

The Inspector may provide:

- inline validation messages
- warning/error decoration
- tooltips or details

Invalid authored state should not be silently corrected or replaced with defaults.

Java remains authoritative for domain validation. The frontend presents validation results rather than independently implementing Component-specific domain rules.

## Reference Properties

Reference properties should provide semantic selection rather than requiring manual entry of IDs or paths.

For example, an Entity reference should allow selection of a compatible Entity.

A Component reference should allow selection of a compatible Component.

An Asset or Resource reference should allow selection from compatible Project content.

The persisted stable identity remains an implementation detail of the authoring operation.

The developer should normally work with meaningful names and semantic types.

## Add Component

The bottom of the Entity Inspector provides an `Add Component` action.

Selecting it opens a searchable Component picker.

The picker presents Component types available to the Project from sources such as:

- JScene3D built-in Components
- installed extensions
- Project-defined Components
- Project dependencies

Components may be grouped by capability or source to make discovery easier.

The initial grouping may include concepts such as:

```text
Rendering
Physics
Audio
Camera
Input
Project Components
```

The final taxonomy should be driven by the actual Component catalog rather than hard-coded assumptions.

Visual reference:

```text
docs/design/mockups/add-component.png
```

## Component Picker

The Component picker provides:

- search
- Component name
- category or source
- short description where metadata provides one
- selection
- confirmation

Component metadata determines whether a Component is valid for the selected Entity.

Multiplicity, dependency, and compatibility constraints should be enforced through domain metadata rather than frontend-specific rules.

Adding a Component creates normal authored Component configuration and does not instantiate or execute the runtime implementation.

## Removing Components

Removable Components provide an appropriate removal action.

Before removal, JScene3D checks for relationships that would become invalid, including:

- Component dependencies
- Component references
- signal/action connections

Components that are required by the domain model cannot be removed through the normal operation.

Removing a Component participates in the normal authored undo/redo model.

## EntityDefinition Authoring

EntityDefinitions are reusable authored content and can be edited independently of any Scene.

Opening an EntityDefinition creates an EntityDefinition Editor in the central editor area.

The editor uses familiar authoring concepts:

- Entity hierarchy
- visual preview where spatial content exists
- Inspector
- Components

The workspace must clearly identify that the developer is editing a reusable definition rather than a Scene instance.

Visual reference:

```text
docs/design/mockups/entity-definition.png
```

## EntityDefinition Editor

The EntityDefinition Editor presents the reusable Entity structure owned by the definition.

Changes made here affect all placements that reference the EntityDefinition.

The editor should therefore make the shared nature of the content clear.

The EntityDefinition Editor does not provide Scene/Game semantics because it is not itself a Scene.

Spatial EntityDefinitions may still use a 3D preview/authoring viewport so that their Entity hierarchy and Components can be manipulated visually.

## EntityDefinition Placement

A Scene may contain placements referencing reusable EntityDefinitions.

A placement references the shared definition rather than copying its contents into the Scene.

Placement-specific state remains distinct from shared definition state.

The first editor version should support the essential workflow:

```text
EntityDefinition
      ↓
Place in Scene
      ↓
Placement
```

Changes to the EntityDefinition affect its placements.

Changes to placement-specific values affect only the corresponding placement.

## Local Entity to EntityDefinition

The editor should support creating reusable content from an Entity that was originally authored locally within a Scene.

Conceptually:

```text
Local Entity
     ↓
Create EntityDefinition
     ↓
Shared EntityDefinition
     +
Scene Placement
```

The resulting definition receives its own Asset identity.

The original Scene location is retained through a placement referencing the newly created definition.

Identity and reference rewriting follow the domain rules defined for this conversion.

The detailed confirmation UX does not need to block the initial Scene-authoring implementation if the underlying workflow is not part of the first milestone.

## Java Component Source

When a Component type is implemented by Project Java source, the developer can navigate from its semantic representation to the Java implementation.

The Java file opens in the normal Code OSS text editor.

Normal Java tooling remains available there.

This preserves an important JScene3D principle:

```text
visual/semantic authoring
        +
normal Java development
```

rather than attempting to replace Java development with a proprietary scripting editor.

Editing Java source does not execute the Component.

Updated implementation and metadata become available through the normal build and refresh lifecycle.

## Serialized Source Access

Advanced developers may inspect the serialized representation of authored content.

A semantic item may provide an action such as:

```text
Open Source
```

or:

```text
View Source
```

The serialized representation opens in an appropriate text editor.

Structured JScene3D source may initially be read-only where direct editing could bypass semantic validation.

Direct editing, where supported, should require an explicit advanced workflow.

Normal authoring should not require editing serialized JSON manually.

## Generated Content

Generated/imported content remains distinguishable from authored Project content.

Generated content is read-only because its external source and Import remain authoritative.

The first editor version does not require the full generated-content conversion workflow to be implemented.

The existing import architecture may continue to supply generated definitions and Resources to authored content.

Detailed inspection, conversion, and dependency-management UX is deferred beyond the core first-version editor unless required by an existing Project workflow.

## Runtime Mode

Running a Project or Scene transitions JScene3D from normal authoring into a runtime context.

Runtime mode creates a live World containing instantiated Entities and Components.

The editor must clearly communicate that the developer is now observing live runtime state rather than editing the persistent authored definitions.

The transition does not close the Project or its authored editors.

## Runtime Controls

The primary runtime controls are always readily accessible.

The first version supports:

```text
Run
Pause
Resume
Step
Stop
Restart
```

`Run Project` starts execution from the configured Main Scene.

`Run Scene` starts execution using an explicitly selected Scene without changing the Project's Main Scene.

The current runtime state should be immediately understandable from the controls.

## Run Project

`Run Project` is available when the Project has a valid Main Scene.

Conceptually:

```text
Run Project
    ↓
create World
    ↓
instantiate mainScene
    ↓
begin runtime lifecycle
```

The Scene currently active for authoring does not determine what `Run Project` executes.

A Project without a Main Scene remains valid for authoring, but `Run Project` is unavailable.

## Run Scene

`Run Scene` allows an arbitrary authored Scene to be tested independently of the Project's Main Scene.

Running a Scene:

- does not change `mainScene`;
- creates a runtime World;
- instantiates the selected Scene;
- resolves dependencies from the containing Project;
- begins normal runtime lifecycle processing.

If the Scene cannot execute independently because required runtime configuration is unavailable, the editor reports the problem rather than silently running another Scene.

## Game View

During runtime execution, the Game view presents the running application's visual output.

The Game tab becomes the primary runtime visual context.

The Scene view may remain available for authoring or inspection where appropriate, but authored and runtime state must remain visually distinguishable.

Visual reference:

```text
docs/design/mockups/runtime-game-view.png
```

## Runtime Hierarchy

Runtime mode provides access to the hierarchy of live Entities in the World.

This hierarchy reflects actual runtime state rather than simply reproducing the authored Scene hierarchy.

It may therefore contain:

- instantiated authored Entities;
- Entities created dynamically at runtime;
- runtime-reparented Entities;
- other live runtime structure.

Entities removed during execution disappear from the runtime hierarchy.

Where possible, runtime objects retain provenance linking them to the authored content from which they originated.

## Runtime Selection

Selecting a live Entity establishes runtime selection.

Runtime selection is distinct from authored Scene selection.

The selected runtime Entity can be inspected without implying that its live state is being persisted into its originating SceneDefinition.

Runtime-created Entities with no authored counterpart can also be selected and inspected.

Runtime selection exists only for the lifetime of the corresponding runtime World.

## Runtime Inspector

When a runtime Entity is selected, the Inspector clearly identifies that it is presenting live runtime state.

The Runtime Inspector may show:

- live Transform values;
- live Component state;
- runtime-only inspectable properties;
- authored provenance where available.

Runtime values reflect the current state of the running application rather than the original authored configuration.

Internal Java implementation fields are not exposed automatically.

Runtime inspection follows explicitly defined runtime metadata and inspection contracts.

## Live Runtime Editing

Supported live runtime values may be edited while testing.

Runtime edits modify only the current runtime World.

For example:

```text
Authored health = 100

Run Project

Live health = 45
```

Changing the live value to:

```text
Live health = 75
```

does not change:

```text
Authored health = 100
```

Stopping or restarting the runtime discards runtime-only changes unless the developer explicitly applies supported changes back to authored content.

## Runtime Value Presentation

The Inspector must make runtime values visually distinguishable from authored values.

A developer should not need to infer whether a value being changed belongs to:

```text
authored content
```

or:

```text
live runtime state
```

Runtime status should therefore be communicated through clear labels, state indicators, and Inspector presentation.

The exact styling may evolve, but ambiguity between authored and runtime editing is unacceptable.

## Pause and Resume

A running Project or Scene can be paused.

Pausing keeps the runtime World alive while normal simulation/update processing is suspended.

While paused, the developer can:

- inspect the runtime hierarchy;
- inspect live Component state;
- modify supported runtime values;
- use runtime stepping.

Resuming continues execution from the preserved runtime state rather than creating a new World.

A paused runtime can also be stopped or restarted directly.

## Runtime Stepping

While paused, the developer can advance runtime execution in controlled steps.

After a step completes, execution returns to the paused state.

This allows the developer to inspect state changes incrementally.

The exact definition of one step depends on the final runtime lifecycle and its relationship to update, fixed-update, physics, and rendering cycles.

The first editor should expose the control without inventing lifecycle semantics independently of the Java runtime.

## Paused Runtime State

The paused state should be visually unmistakable.

The editor should clearly indicate:

```text
PAUSED
```

The Game view remains frozen at the current runtime state.

Runtime hierarchy and Inspector remain available.

Runtime-editable values remain editable where the runtime contract permits them.

The Step control becomes relevant while paused.

Visual reference:

```text
docs/design/mockups/runtime-paused-live-inspection.png
```

## Apply Runtime Changes

Runtime changes are never persisted automatically.

Where a live value can be safely mapped back to authored content, JScene3D may provide an explicit:

```text
Apply to Authored…
```

workflow.

The developer chooses which eligible changes to apply.

Applied changes become normal authored modifications and participate in authored undo/redo and save state.

Changes that cannot be mapped safely are not applied implicitly.

Runtime-created objects do not automatically become authored Entities.

A complete Apply-to-Authored workflow may be implemented after the core runtime inspection capability if necessary for first-version scope.

## Stop Runtime

Stopping runtime execution disposes of the current World and its live state.

Runtime-created Entities and Components are discarded.

Runtime-only property changes are discarded unless explicitly applied to authored content.

Stopping does not:

- close the Project;
- close authored Scene editors;
- modify the Main Scene;
- write runtime state into authored content.

After runtime cleanup completes, the developer returns directly to authoring.

## Restart Runtime

Restarting destroys the current runtime World and creates a new one.

For `Run Project`, restart begins again from the Project's Main Scene.

For `Run Scene`, restart begins again from the same explicitly selected Scene.

The new runtime uses the current authored state.

Runtime-only changes from the previous execution are discarded.

Restart should therefore provide a fast testing loop:

```text
Run
    ↓
test
    ↓
Restart
    ↓
test again
```

## Return to Authoring

Ending runtime execution returns the developer to the existing authoring workspace.

Open authored editors remain open.

The previous authoring context is restored where practical.

Runtime-specific state is removed, including:

- runtime hierarchy;
- runtime selection;
- live Component state;
- runtime-only values.

Runtime diagnostics may remain available for investigation after execution has ended.

Returning to authoring should not unnecessarily reload the Project.

## Runtime Diagnostics

Runtime diagnostics are distinct from authoring validation diagnostics.

Runtime errors and warnings should identify relevant runtime context where available, including:

- subsystem;
- Entity;
- Component;
- authored provenance.

Runtime diagnostics remain available long enough for the developer to investigate failures after execution ends.

A runtime failure should not close or corrupt the authored Project.

## Runtime Failure

If the running application fails, JScene3D should isolate the failure from authored state.

The editor performs runtime cleanup where possible and returns the Project to a usable authoring state.

Open authored content remains available.

Runtime-only state is discarded.

The developer should be able to correct the problem and run again without reopening the Project.

## First-Version Runtime Scope

The first-version runtime UX should prioritize:

- Run Project;
- Run Scene;
- Stop;
- Pause and Resume;
- Step;
- Restart;
- Game View;
- runtime hierarchy;
- live Entity selection;
- live Component inspection;
- supported live-value editing;
- runtime diagnostics.

More advanced runtime tooling can be deferred, including:

- comprehensive profiling;
- sophisticated runtime-to-authored diffing;
- complex Apply-to-Authored workflows;
- runtime object capture;
- advanced debugging visualizations.

The goal of the first version is a coherent authoring/test/debug loop rather than a complete professional profiler or debugger.

## Validation and Diagnostics

JScene3D should identify authoring problems before runtime wherever possible.

Validation operates on authored Project content without creating a runtime World or executing application Component behaviour.

Validation exists at several scopes:

```text
Property
Component
Entity
Scene
Project
```

Problems should be associated with the most specific semantic object available.

## Component Validation

Component validation may involve relationships between multiple properties or requirements that cannot be expressed by a single property control.

A Component can therefore report validation problems associated with the Component as a whole.

Validation remains Java-owned.

The frontend presents the resulting diagnostics rather than independently implementing Component-specific domain rules.

## Entity Validation

Entity validation includes the validity of its Component composition.

Problems may include:

- missing required Components or capabilities
- incompatible Components
- multiplicity violations
- unresolved Component dependencies
- invalid Component configuration

Validation updates as the Entity's composition changes.

## Scene Validation

Scene validation considers the authored SceneDefinition as a whole.

Problems may include:

- invalid Entity hierarchy
- cyclic relationships
- duplicate identities within their applicable scope
- invalid Components
- unresolved Entity or Component references
- invalid signal/action connections
- unresolved Asset or Resource references
- invalid EntityDefinition placements
- invalid placement arguments

Scene validation does not require running the Scene.

## Project Validation

Project validation considers Project-wide configuration and relationships.

Problems may include:

- invalid Project configuration
- invalid Main Scene reference
- duplicate Asset identities
- invalid SceneDefinitions
- invalid EntityDefinitions
- unresolved Assets
- unavailable Component or Resource types
- invalid extension metadata
- invalid Imports
- unavailable generated content

A Project without a Main Scene is valid for authoring.

The absence of a Main Scene disables `Run Project`; it does not make the Project invalid merely to open and edit.

## Diagnostic Presentation

Diagnostics should be presented through the semantic authoring context wherever possible.

For example, a problem involving:

```text
Scene
  → Entity
      → Component
          → Property
```

should allow the developer to navigate to that semantic location.

The existing Code OSS Problems infrastructure may be reused where appropriate, but JScene3D diagnostics should not be reduced to filesystem paths when richer semantic context is available.

Detailed diagnostic presentation can evolve after the first version.

## Save State

Authored editors participate in normal modified/dirty state.

Changes made through:

- Scene manipulation
- Hierarchy operations
- Inspector property editing
- Component operations
- other semantic authoring operations

all modify the corresponding authored definition.

The editor tab indicates unsaved changes.

Normal Save and Save All workflows persist authored content.

Saving does not affect runtime state.

## Undo and Redo

Semantic authoring operations participate in undo and redo.

Examples include:

- property changes
- Transform changes
- Entity creation and deletion
- Component addition and removal
- Entity reparenting
- Entity rename

Undo/redo should operate on logical authoring operations rather than textual JSON changes.

A continuous interaction such as dragging a Transform gizmo should normally become one meaningful undo operation rather than hundreds of tiny value changes.

All views representing the affected authored state update together after undo or redo.

## External File Changes

JScene3D Projects remain normal development Projects whose files may change outside the semantic editor.

External changes may result from:

- source-control operations
- command-line tools
- manual file editing
- Java builds
- filesystem operations
- other development tools

JScene3D must detect relevant external changes and avoid silently overwriting them.

If an authored file has no conflicting unsaved editor state, it may be reloaded safely.

If both external and editor changes exist, the developer must be informed of the conflict.

## Java Development

JScene3D retains Code OSS as a full Java development environment.

Project Java source follows the normal Maven layout and opens in normal text editors.

Java development capabilities may include:

- completion
- navigation
- diagnostics
- refactoring
- testing
- debugging

The semantic authoring environment complements these capabilities rather than replacing them.

## Java Build

JScene3D should provide a convenient way to build Project Java code using the Project's Maven configuration.

Where a Maven Wrapper exists, it should be preferred.

Build progress and diagnostics should remain visible.

A successful build can make updated:

- Component implementations
- Component metadata
- extension metadata
- Resource types
- importer types

available to subsequent authoring and runtime operations.

A failed build should not replace the last known usable state with incomplete build output.

## Source Control

Code OSS source-control capabilities remain available.

JScene3D-specific authored files remain ordinary Project files suitable for version control.

Semantic authoring operations should produce deterministic, reviewable persisted changes where practical.

Source-control operations that modify Project content participate in the same external-change handling as other filesystem changes.

The first-version JScene3D UX does not require replacing Code OSS source-control tooling with a custom implementation.

## Explorer

Explorer remains available as the physical filesystem view.

It is useful for:

- Java source
- configuration files
- physical asset organization
- advanced file operations
- troubleshooting

Explorer is not the default semantic representation of JScene3D authored content.

Normal Scene, Entity, and Component authoring should not require developers to navigate raw files.

## Command Palette

Code OSS provides a large command surface that is broader than the JScene3D authoring experience.

The first-version editor should expose JScene3D commands required by its supported workflows.

A broader command-palette audit is required separately to determine which inherited Code OSS commands:

- remain useful;
- should be hidden from normal JScene3D workflows;
- should be renamed or regrouped;
- are implementation-oriented and should not be exposed.

This audit is intentionally separate from the core Scene Editor implementation.

## Chat and AI Surfaces

Generic Code OSS Chat surfaces are not part of the default JScene3D authoring experience.

They should not appear automatically in:

- Welcome
- Project loading
- Project-ready layouts
- Scene authoring

Any future AI-assisted JScene3D functionality should be designed deliberately rather than inherited accidentally from upstream Code OSS UI.

## First-Version Scope

The first usable JScene3D Editor should establish the core authoring loop:

```text
Launch JScene3D
    ↓
Create/Open Project
    ↓
Create/Open Scene
    ↓
Create/select Entities
    ↓
Add/configure Components
    ↓
Manipulate Scene
    ↓
Save
    ↓
Run Project or Scene
    ↓
Inspect/test runtime
    ↓
Stop
    ↓
Continue authoring
```

The goal is a coherent end-to-end workflow rather than exhaustive support for every future domain capability.

## Deferred UX

The following areas are deliberately deferred unless required to complete the core workflow:

- comprehensive generated-content management
- full Make Editable conversion workflow
- sophisticated imported-content dependency handling
- advanced profiling
- complete runtime-to-authored diff/application workflow
- advanced Scene templates
- complex EntityDefinition override workflows
- comprehensive Project search
- specialized Resource editors
- advanced transform snapping and pivot systems
- extensive command-palette cleanup
- tutorial/Get Started content
- final theme polish
- advanced diagnostics presentation

Deferred features should not force premature complexity into the first editor architecture.

## Core Mockup Set

The first-version UX is primarily guided by the following mockups:

```text
docs/design/mockups/welcome.png
docs/design/mockups/create-project.png
docs/design/mockups/project-loading.png
docs/design/mockups/project-open-no-scene.png
docs/design/mockups/new-scene.png
docs/design/mockups/scene-authoring.png
docs/design/mockups/add-entity-menu.png
docs/design/mockups/add-component.png
docs/design/mockups/entity-definition.png
docs/design/mockups/runtime-game-view.png
docs/design/mockups/runtime-paused-live-inspection.png
```

Additional mockups may exist for future workflows but are not required to define the first-version implementation.

## Design Authority

The implementation should be guided in this order:

```text
Domain Model
    ↓
User Stories
    ↓
Editor UX
    ↓
Core Mockups
    ↓
Implementation
```

Where implementation constraints reveal a conflict or missing design decision, the design should be revisited explicitly rather than allowing incidental implementation behaviour to become the UX specification.
