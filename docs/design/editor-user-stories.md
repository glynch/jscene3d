# JScene3D Editor User Stories

## Launch JScene3D With No Project Open

As a JScene3D developer, I want JScene3D to open into a clear JScene3D-specific start screen when no Project is open, so that I can immediately choose what I want to work on.

### Acceptance Criteria

- The JScene3D splash screen appears during application startup.
- After startup, a JScene3D Welcome screen is shown.
- The Welcome screen provides:
  - Create Project
  - Open Project
  - Clone Repository
  - Recent Projects
  - Get Started — provides access to tutorials, documentation, and other learning resources.
- The JScene3D Project activity is selected by default.
- Generic Code OSS empty-editor content is not shown.
- Chat is not shown.
- Explorer is not selected automatically.
- No Project-dependent view displays misleading placeholder content when no Project is open.
- The application remains in this state until the developer creates or opens a Project.

## Create a New Project

As a JScene3D developer, I want to create a new Project with minimal required information so that I can begin authoring without unnecessary setup.

### Acceptance Criteria

- The Create Project workflow asks for:
  - Project name
  - Project location
  - Project/package ID
- JScene3D creates the required Project structure and configuration.
- The Project opens automatically after creation.
- Creating a Scene is not required during Project creation.
- A newly created Project may have no Main Scene.
- A Project without a Main Scene is fully usable for authoring.
- The developer can create or import Scenes after Project creation.
- `Run Project` is disabled while no Main Scene is configured.
- Once a Main Scene is selected, `Run Project` becomes available.
- Java/Maven configuration that JScene3D can determine itself is created automatically rather than requested from the developer during initial Project creation.

## Open an Existing Project

As a JScene3D developer, I want to open an existing Project so that I can continue working with its authored content and source code.

### Acceptance Criteria

- The developer can open a Project from:
  - Open Project on the Welcome screen.
  - A Recent Projects entry.
  - The appropriate application menu or command.
  - A Project path or Project descriptor passed when launching JScene3D from the command line.
- Command-line launch supports selecting either:
  - a JScene3D Project descriptor; or
  - a directory containing a JScene3D Project.
- All entry points use the same Project-opening and loading lifecycle.
- JScene3D validates that the selected location contains a valid Project descriptor.
- Opening a Project does not require the Project to have a Main Scene.
- The editor begins loading the selected Project.
- The previous Project, if any, is closed before the new Project becomes active.
- Invalid or unsupported Projects produce a clear diagnostic rather than opening a partially initialized workspace.
- Successfully opening the Project proceeds into the Project loading state.

## Observe Project Loading

As a JScene3D developer, I want clear feedback while a Project is loading so that I know JScene3D is working and do not mistake temporary initialization states for the loaded Project.

### Acceptance Criteria

- JScene3D enters an explicit Project loading state after a Project has been selected and validated.
- The Project being loaded is clearly identified.
- A visible progress indicator communicates that loading is in progress.
- Project-dependent views do not display temporary empty or placeholder states while their data is still loading.
- Generic messages such as `Drag a view here to display` are not shown during Project loading.
- Unrelated Code OSS views do not appear temporarily while JScene3D views are being initialized.
- Previously restored Project content is not shown as if it belongs to the Project currently being loaded.
- The loading state remains visually stable until the Project either becomes ready or loading fails.
- If loading fails, JScene3D presents a clear error state and does not leave the Project partially active.
- Successful loading transitions directly to the Project-ready state.

## Arrive at the Project-Ready State

As a JScene3D developer, I want the editor to enter a stable Project-ready state after loading so that I can immediately continue authoring.

### Acceptance Criteria

- The Project loading state ends only after the Project is ready for normal authoring operations.
- The JScene3D Project activity is selected.
- The semantic Project view contains the loaded Project's available authored content.
- Project-dependent views receive the loaded Project context before being displayed as ready.
- If previous editor workspace state exists for the Project, restoration begins from that state.
- If no previous workspace state exists, JScene3D presents an appropriate initial Project workspace.
- A Project with no Scenes can still reach the Project-ready state.
- A Project with no Main Scene can still reach the Project-ready state.
- `Run Project` is disabled when no Main Scene is configured.
- No unrelated Code OSS view is briefly selected or displayed during the transition from loading to ready.
- No temporary empty-view placeholder is shown as part of the transition.
- Once ready, the Project remains active until the developer closes it, opens another Project, or exits JScene3D.

## Restore the Previous Authoring Workspace

As a JScene3D developer, I want my previous authoring workspace restored when I reopen a Project so that I can continue working from where I left off.

### Acceptance Criteria

- JScene3D persists editor workspace state separately from Project runtime configuration.
- When a previously opened Project is reopened, JScene3D restores appropriate workspace state, including:
  - previously open editor tabs;
  - the previously active editor tab;
  - JScene3D view layout and visibility;
  - relevant expanded or collapsed view state;
  - Scene authoring viewpoints where available.
- Restoring workspace state does not change the Project's Main Scene.
- A Scene does not need to be the Main Scene to be restored as an open editor.
- Missing, deleted, or no-longer-valid content does not prevent the rest of the workspace from being restored.
- Restoration occurs as part of the Project loading process rather than visibly opening and rearranging views after the Project appears ready.
- The developer does not see unrelated Code OSS views or temporary placeholder states during restoration.
- If no saved workspace state exists, JScene3D opens the normal initial Project workspace.

## Discover Scenes in a Project

As a JScene3D developer, I want to see the Scenes available in my Project so that I can find and open the Scene I want to work on.

### Acceptance Criteria

- All authored SceneDefinitions in the Project are discoverable through the semantic Project view.
- Scenes are identified from JScene3D project metadata rather than requiring them to exist in a particular physical directory.
- Each Scene is presented using its authored name rather than requiring the developer to work from its filename.
- The Project's Main Scene is clearly identifiable.
- A Scene does not need to be the Main Scene to appear in the Project view.
- Moving a Scene to another valid physical location does not change its identity or remove it from the semantic Project view.
- Renaming a Scene's display name does not change its identity.
- Generated/imported content remains distinguishable from directly authored Scenes.
- A Project containing no Scenes presents an appropriate empty state from which a Scene can be created or imported.

## Open a Scene

As a JScene3D developer, I want to open a Scene from my Project so that I can view and edit its authored content.

### Acceptance Criteria

- A Scene can be opened from the semantic Project view.
- Opening a Scene creates or activates a Scene Editor tab for that Scene.
- Multiple Scenes can be open simultaneously in separate editor tabs.
- Opening a Scene that is already open activates its existing editor rather than opening a duplicate.
- The opened Scene becomes the active Scene for Scene-specific views such as the Hierarchy and Inspector.
- Opening a Scene does not change the Project's Main Scene.
- Opening a Scene does not create a runtime World or execute runtime Component lifecycle behaviour.
- Switching between open Scene tabs updates Scene-specific views to represent the newly active Scene.
- Closing a Scene Editor tab does not remove the Scene from the Project.

## Create a Scene

As a JScene3D developer, I want to create a new Scene so that I can author a new composition of Entities and Components within my Project.

### Acceptance Criteria

- A new Scene can be created from the semantic Project view.
- The developer provides a name for the Scene.
- JScene3D allocates a new stable `AssetId` for the Scene.
- JScene3D creates a valid authored SceneDefinition.
- The Scene is saved at an appropriate default or developer-selected physical location within the Project.
- The new Scene appears automatically in the semantic Project view.
- The new Scene can be opened immediately in the Scene Editor.
- Creating a Scene does not automatically make it the Main Scene.
- If the Project has no Main Scene, the developer may explicitly choose to make the new Scene the Main Scene.
- Creating a Scene does not create or start a runtime World.
- The default contents of a newly created Scene are not assumed by this user story and will be defined separately.

## Set the Main Scene

As a JScene3D developer, I want to designate a Scene as the Project's Main Scene so that JScene3D knows which Scene to instantiate when the Project runs.

### Acceptance Criteria

- Any authored Scene in the Project can be designated as the Main Scene.
- Setting the Main Scene is an explicit action.
- Only one Scene can be the Main Scene at a time.
- The semantic Project view clearly identifies the current Main Scene.
- Changing the Main Scene updates the Project configuration.
- The Main Scene relationship uses the Scene's stable `AssetId`, not its physical path.
- Moving or renaming the Main Scene does not change its Main Scene status.
- Setting a Scene as the Main Scene does not open, close, or activate that Scene.
- Setting the Main Scene does not start the Project.
- `Run Project` is enabled when a valid Main Scene is configured.
- The Main Scene designation can be changed or cleared.
- If the Main Scene is cleared, the Project remains usable for authoring but `Run Project` becomes disabled.

## Rename a Scene

As a JScene3D developer, I want to rename a Scene so that its authored name can evolve without changing its identity.

### Acceptance Criteria

- An authored Scene can be renamed from the semantic Project view.
- Renaming changes the Scene's authored display name.
- Renaming does not change the Scene's `AssetId`.
- References to the Scene remain valid after the rename.
- If the Scene is the Main Scene, it remains the Main Scene.
- Renaming the authored Scene name does not inherently require renaming or moving its physical file.
- Any separate physical file rename is treated as a distinct operation.
- An invalid Scene name is rejected with a clear validation message.
- The renamed Scene is reflected immediately wherever its authored name is presented.

## Move a Scene

As a JScene3D developer, I want to move a Scene within the Project's physical structure so that I can reorganize my Project without changing the Scene's identity.

### Acceptance Criteria

- An authored Scene can be moved to another valid physical location within the Project.
- Moving the Scene does not change its `AssetId`.
- References to the Scene remain valid after the move.
- If the Scene is the Main Scene, it remains the Main Scene.
- The Scene's authored display name does not change as a consequence of moving its file.
- The semantic Project view continues to present the Scene according to its semantic type rather than its physical location.
- If any remaining path-based references are affected by the move, JScene3D updates them where it can do so safely or reports them clearly.
- A Scene cannot be moved outside the boundaries in which it can remain part of the Project without an explicit operation that changes its Project ownership.
- Moving a Scene does not require closing it if it is currently open.

## Duplicate a Scene

As a JScene3D developer, I want to duplicate an authored Scene so that I can use an existing Scene as the starting point for a new independently editable Scene.

### Acceptance Criteria

- An authored Scene can be duplicated from the semantic Project view.
- The duplicate is a new independent SceneDefinition.
- JScene3D allocates a new `AssetId` for the duplicated Scene.
- The original Scene remains unchanged.
- The duplicate receives its own authored name and physical representation.
- JScene3D handles EntityIds, ComponentIds, and internal references according to the defined Scene-duplication identity rules.
- References within the duplicated Scene continue to resolve to the appropriate duplicated or shared content.
- External references to reusable Assets remain valid where those Assets are intentionally shared.
- Duplicating the Main Scene does not automatically make the duplicate the Main Scene.
- The duplicate appears automatically in the semantic Project view.
- The duplicated Scene can be opened and edited independently of the original.
- JScene3D does not perform a naive filesystem copy that leaves two SceneDefinitions with the same `AssetId`.

## Delete a Scene

As a JScene3D developer, I want to delete an authored Scene so that I can remove content that is no longer required by my Project.

### Acceptance Criteria

- An authored Scene can be deleted from the semantic Project view.
- JScene3D asks for confirmation before permanently deleting the Scene.
- JScene3D identifies references to the Scene that would become invalid as a result of deletion.
- The developer is warned about affected references before deletion proceeds.
- Deleting a Scene removes its authored definition from the Project.
- If the Scene is currently open, its Scene Editor is closed as part of the deletion.
- Deleting a Scene does not affect other Scenes or Assets unless explicitly confirmed as part of the operation.
- If the Scene is the Main Scene, JScene3D requires the developer to clear or replace the Main Scene designation before deletion can complete.
- After deletion, the Scene no longer appears in the semantic Project view.
- Deleting the last Scene does not invalidate the Project; the Project remains usable for authoring with no Scenes and no Main Scene.

## Close a Scene

As a JScene3D developer, I want to close a Scene that I am no longer actively editing so that I can keep my authoring workspace focused without affecting the Project content.

### Acceptance Criteria

- An open Scene can be closed by closing its Scene Editor tab.
- Closing a Scene does not delete or otherwise modify the SceneDefinition.
- Closing a Scene does not change its `AssetId`.
- Closing the Main Scene does not change its Main Scene designation.
- Closing a Scene does not affect other open Scenes.
- If the Scene has unsaved changes, the developer is given the opportunity to save, discard, or cancel the close operation.
- After closing the active Scene, another open Scene may become active.
- Scene-specific context reflects the newly active Scene, or an appropriate no-active-Scene state when no Scene remains open.
- A closed Scene remains available in the semantic Project view and can be opened again.

## Save a Scene

As a JScene3D developer, I want changes to an authored Scene to be saved so that my work is persisted reliably.

### Acceptance Criteria

- Changes to a Scene cause that Scene to enter a modified/dirty state.
- The Scene Editor tab clearly indicates when the Scene has unsaved changes.
- The developer can explicitly save the active Scene.
- `Save All` saves changes across all modified authored content.
- Saving persists the current authored SceneDefinition to its physical representation.
- Changes made through any authoring surface participate in the same Scene save state.
- Saving does not change the Scene's `AssetId`.
- Saving the Main Scene does not have different persistence semantics from saving any other Scene.
- Saving a Scene does not create or modify runtime World state.
- If saving fails, the Scene remains marked as modified and the developer receives a clear diagnostic.
- Successfully saving clears the Scene's modified state.

## Undo and Redo Scene Changes

As a JScene3D developer, I want to undo and redo changes made while authoring a Scene so that I can safely experiment and recover from unwanted changes.

### Acceptance Criteria

- Scene authoring operations participate in the editor's undo/redo history.
- Undo and redo operate on semantic authoring operations rather than serialized file edits.
- Supported operations include, where applicable:
  - changing Entity properties;
  - changing Component properties;
  - transforming an Entity;
  - adding or removing an Entity;
  - adding or removing a Component;
  - reparenting an Entity;
  - renaming an Entity.
- A single logical operation is represented as a single undoable action where appropriate.
- Continuous interactions such as dragging a transform gizmo do not create an excessive number of individual undo steps.
- Undo immediately updates all representations of the affected authored state.
- Redo restores the corresponding undone operation.
- Undoing back to the last saved state clears the Scene's modified indicator.
- Redoing a change beyond the saved state marks the Scene as modified again.
- Undo and redo do not affect runtime World state.

## Select an Entity

As a JScene3D developer, I want to select an Entity in the active Scene so that I can inspect and modify that Entity and its Components.

### Acceptance Criteria

- An Entity can be selected from the Scene Hierarchy.
- A spatial Entity can also be selected directly from the Scene view.
- Selecting an Entity establishes a single shared selection for the active Scene.
- The selected Entity is reflected consistently in:
  - the Scene Hierarchy;
  - the Scene view;
  - the Inspector.
- The Inspector displays the selected Entity and its attached Components.
- Selecting another Entity replaces the current Entity selection.
- Clicking an appropriate empty area clears the selection.
- Switching to another open Scene does not cause an Entity from the previous Scene to appear selected in the newly active Scene.
- Selection is authoring state and does not modify the SceneDefinition.
- Selecting an Entity does not execute runtime Component behaviour.

## Create an Entity

As a JScene3D developer, I want to create an Entity in the active Scene so that I can add new objects or logical behaviour to the Scene.

### Acceptance Criteria

- A new Entity can be created within the active Scene.
- The developer can create:
  - a root Entity; or
  - a child Entity beneath an existing Entity.
- JScene3D allocates a new `EntityId` for the Entity.
- The Entity receives an authored name.
- The new Entity appears immediately in the Scene Hierarchy.
- A newly created child Entity is parented to the selected parent Entity.
- Creating an Entity modifies the active SceneDefinition and marks the Scene as modified.
- Creating an Entity is undoable.
- The new Entity can have Components added to it.
- Whether a newly created Entity receives a Transform automatically depends on the final spatial/non-spatial Entity model.
- Creating an Entity does not execute runtime Component behaviour.

## Rename an Entity

As a JScene3D developer, I want to rename an Entity so that its authored name clearly describes its purpose without changing its identity.

### Acceptance Criteria

- An Entity can be renamed within the active Scene.
- Renaming changes the Entity's authored name.
- Renaming does not change its `EntityId`.
- References to the Entity remain valid after the rename.
- Renaming a parent Entity does not affect the identity of its child Entities.
- The new name is reflected consistently wherever the Entity is represented.
- Renaming modifies the SceneDefinition and marks the Scene as modified.
- Renaming an Entity is undoable.
- Invalid Entity names are rejected with a clear validation message.

## Reparent an Entity

As a JScene3D developer, I want to change an Entity's parent so that I can reorganize the Scene hierarchy and establish the appropriate parent/child relationship.

### Acceptance Criteria

- An Entity can be moved beneath another Entity in the active Scene.
- A child Entity can be moved back to the Scene root.
- Reparenting does not change the Entity's `EntityId`.
- The Entity's descendants remain attached to it when it is reparented.
- References to the Entity and its descendants remain valid.
- Reparenting cannot create a cyclic Entity hierarchy.
- For a spatial Entity, JScene3D provides well-defined transform behaviour when the parent changes.
- The final choice between preserving world-space Transform or preserving local Transform during reparenting will be defined separately.
- Reparenting modifies the SceneDefinition and marks the Scene as modified.
- Reparenting is undoable.
- The updated hierarchy is reflected immediately wherever the Scene's Entity hierarchy is represented.

## Delete an Entity

As a JScene3D developer, I want to delete an Entity from a Scene so that I can remove objects or behaviour that are no longer required.

### Acceptance Criteria

- An Entity can be deleted from the active Scene.
- Deleting an Entity also removes its attached Components.
- If the Entity has child Entities, the developer is clearly informed that the subtree will also be deleted.
- Deleting an Entity removes its descendants when the deletion proceeds.
- JScene3D identifies authored references and connections that would become invalid as a result of the deletion.
- The developer is warned about affected references before deletion proceeds.
- Deleting an Entity modifies the SceneDefinition and marks the Scene as modified.
- Deleting an Entity is undoable.
- Undo restores the Entity, its Components, descendants, and relationships removed as part of the operation.
- Deleting an Entity does not affect unrelated Entities or Assets.

## Create an EntityDefinition

As a JScene3D developer, I want to create a reusable EntityDefinition so that I can define Entity content once and use it in multiple Scenes.

### Acceptance Criteria

- A new EntityDefinition can be created independently of any Scene.
- The developer provides a name for the EntityDefinition.
- JScene3D allocates a new stable `AssetId` for the EntityDefinition.
- JScene3D creates a valid authored EntityDefinition.
- The EntityDefinition is saved at an appropriate default or developer-selected physical location within the Project.
- The EntityDefinition appears automatically in the semantic Project view.
- The EntityDefinition can contain:
  - an Entity;
  - Components attached to that Entity;
  - child Entities and their Components.
- Changes to the EntityDefinition are shared by all placements that reference that definition.
- Creating an EntityDefinition does not automatically place it into any Scene.
- Creating an EntityDefinition does not execute runtime Component behaviour.

## Edit an EntityDefinition

As a JScene3D developer, I want to edit a reusable EntityDefinition so that changes to shared Entity content are reflected wherever that definition is used.

### Acceptance Criteria

- An authored EntityDefinition can be opened for editing independently of any Scene.
- The EntityDefinition's Entity hierarchy and Components can be modified.
- Supported changes include, where applicable:
  - adding, removing, and renaming Entities;
  - changing parent/child relationships;
  - adding and removing Components;
  - changing Component properties.
- Editing the EntityDefinition does not change its `AssetId`.
- Changes to the EntityDefinition apply to all placements that reference it.
- Placement-specific values are not unintentionally replaced by changes to the shared definition.
- Editing the EntityDefinition marks it as modified until saved.
- EntityDefinition changes participate in undo and redo.
- Editing an EntityDefinition does not execute runtime Component behaviour.

## Place an EntityDefinition in a Scene

As a JScene3D developer, I want to place a reusable EntityDefinition into a Scene so that I can use shared Entity content without duplicating its definition.

### Acceptance Criteria

- An EntityDefinition can be placed into the active Scene.
- JScene3D creates a placement that references the EntityDefinition by its stable `AssetId`.
- Multiple placements of the same EntityDefinition can exist within one Scene.
- The same EntityDefinition can be placed in multiple Scenes.
- Each placement has its own identity within the containing Scene.
- Placement-specific values can differ between instances where supported by the EntityDefinition's contract.
- Changes to the shared EntityDefinition are reflected by all of its placements.
- Creating or modifying one placement does not modify the shared EntityDefinition.
- Adding a placement modifies the SceneDefinition and marks the Scene as modified.
- Adding a placement is undoable.
- Placing an EntityDefinition does not execute runtime Component behaviour.

## Create an EntityDefinition from a Local Entity

As a JScene3D developer, I want to convert an Entity authored locally in a Scene into a reusable EntityDefinition so that I can reuse that content elsewhere without recreating it.

### Acceptance Criteria

- A local Entity can be used to create a new EntityDefinition.
- The Entity's Components and child Entity hierarchy are included in the new EntityDefinition.
- JScene3D allocates a new stable `AssetId` for the EntityDefinition.
- The developer provides a name and appropriate physical location for the new EntityDefinition.
- The new EntityDefinition appears in the semantic Project view.
- The original local Entity is replaced by a placement referencing the new EntityDefinition.
- The Scene retains the Entity's existing placement and spatial relationship after conversion.
- References and connections involving the original Entity hierarchy remain valid or are safely rewritten as part of the conversion.
- The operation modifies the SceneDefinition and creates a new authored Asset.
- The operation is undoable as a single logical authoring action.
- Subsequent changes to the EntityDefinition are reflected wherever that definition is placed.

## Navigate from a Placement to its EntityDefinition

As a JScene3D developer, I want to navigate from a placement in a Scene to its referenced EntityDefinition so that I can inspect or modify the shared content that the placement uses.

### Acceptance Criteria

- A placement clearly retains its relationship to the referenced EntityDefinition.
- The developer can navigate directly from a selected placement to its EntityDefinition.
- The referenced EntityDefinition opens in its appropriate authoring context.
- Navigating to the EntityDefinition does not modify the placement or Scene.
- The developer can distinguish between:
  - properties belonging to the shared EntityDefinition;
  - values belonging specifically to the placement.
- Changes made to the EntityDefinition follow normal shared-definition semantics and are reflected by all placements that reference it.
- Navigating to a definition does not execute runtime Component behaviour.

## Distinguish Shared Definition State from Placement State

As a JScene3D developer, I want to understand which values belong to a shared EntityDefinition and which belong to a particular placement so that I can make changes at the correct scope.

### Acceptance Criteria

- A placement retains a clear distinction between:
  - content defined by the referenced EntityDefinition;
  - values specific to that placement.
- Editing shared definition state changes the EntityDefinition and therefore affects all placements that reference it.
- Editing placement-specific state changes only that placement.
- Placement-specific values are limited to values explicitly supported by the EntityDefinition's contract.
- Placement-specific state does not silently become part of the shared EntityDefinition.
- Shared definition changes do not unintentionally remove valid placement-specific values.
- JScene3D does not treat placement-specific values as arbitrary overrides of any property within the EntityDefinition.
- The distinction remains valid when the same EntityDefinition is placed multiple times within one Scene or across multiple Scenes.

## Delete a Placement

As a JScene3D developer, I want to remove a placement from a Scene so that I can remove one use of a reusable EntityDefinition without affecting the shared definition or its other placements.

### Acceptance Criteria

- A placement can be deleted from the active Scene.
- Deleting a placement removes only that placement from the Scene.
- The referenced EntityDefinition is not deleted or modified.
- Other placements referencing the same EntityDefinition are unaffected.
- JScene3D identifies authored references and connections that would become invalid when the placement is removed.
- The developer is warned about affected references before deletion proceeds.
- Deleting a placement modifies the SceneDefinition and marks the Scene as modified.
- Deleting a placement is undoable.
- Undo restores the placement and its placement-specific state.

## Duplicate a Placement

As a JScene3D developer, I want to duplicate a placement so that I can create another instance of the same reusable EntityDefinition within a Scene.

### Acceptance Criteria

- A placement can be duplicated within the active Scene.
- The duplicate references the same EntityDefinition as the original placement.
- The duplicate receives its own placement identity within the Scene.
- Placement-specific values are copied from the original placement.
- The referenced EntityDefinition is not duplicated or modified.
- Subsequent changes to the shared EntityDefinition are reflected by both placements.
- Subsequent changes to placement-specific state affect only the placement being changed.
- Duplicating a placement modifies the SceneDefinition and marks the Scene as modified.
- Duplicating a placement is undoable.

## Move a Placement

As a JScene3D developer, I want to move a placement within a Scene hierarchy so that I can organize reusable content under the appropriate parent Entity.

### Acceptance Criteria

- A placement can be moved beneath an appropriate Entity in the active Scene.
- A placement can be moved back to the Scene root.
- Moving a placement does not change the referenced EntityDefinition.
- Moving a placement does not change its placement identity.
- Placement-specific values are preserved.
- The placement's descendants remain associated with it where applicable.
- The operation cannot create an invalid or cyclic hierarchy.
- For spatial content, JScene3D provides well-defined transform behaviour when the placement's parent changes.
- The final choice between preserving world-space Transform or local Transform during reparenting will be defined separately.
- Moving a placement modifies the SceneDefinition and marks the Scene as modified.
- Moving a placement is undoable.

## Delete an EntityDefinition

As a JScene3D developer, I want to delete an EntityDefinition that is no longer required so that unused reusable content can be removed from the Project safely.

### Acceptance Criteria

- An authored EntityDefinition can be deleted from the Project.
- JScene3D identifies placements and other authored references that depend on the EntityDefinition.
- An EntityDefinition with existing usages is not silently deleted.
- The developer is informed which Scenes or other authored content reference the EntityDefinition.
- The developer must resolve dependent usages before deletion can complete.
- Deleting an EntityDefinition removes its authored definition from the Project.
- Deleting an EntityDefinition does not delete unrelated Assets.
- After deletion, the EntityDefinition is no longer available as reusable Project content.
- If the EntityDefinition is currently open for editing, its editor is closed as part of the deletion.

## Add a Component to an Entity

As a JScene3D developer, I want to add a Component to an Entity so that I can give that Entity additional data or behaviour.

### Acceptance Criteria

- A Component can be added to an authored Entity.
- The developer can choose from Component types available to the Project.
- Available Component types include appropriate built-in, extension-provided, and Project-provided Components.
- JScene3D uses Component metadata to determine whether the selected Component can be attached to the Entity.
- Component multiplicity and dependency rules are respected where defined.
- JScene3D allocates a new `ComponentId` for the Component instance.
- The new Component is initialized with its defined authored defaults.
- Adding a Component modifies the containing authored definition and marks it as modified.
- Adding a Component is undoable.
- Adding an authored Component does not instantiate its runtime implementation or execute runtime lifecycle behaviour.

## Remove a Component from an Entity

As a JScene3D developer, I want to remove a Component from an Entity so that I can remove data or behaviour that the Entity no longer requires.

### Acceptance Criteria

- A removable Component can be removed from an authored Entity.
- Removing a Component does not change the Entity's `EntityId`.
- JScene3D respects Component rules that prevent removal of required Components.
- JScene3D identifies other Components, references, or connections that depend on the Component being removed.
- The developer is warned about affected dependencies before removal proceeds.
- Removing a Component removes its `ComponentId` from the authored definition.
- Removing a Component modifies the containing authored definition and marks it as modified.
- Removing a Component is undoable.
- Undo restores the Component, its authored properties, identity, and relationships removed as part of the operation.
- Removing an authored Component does not execute runtime lifecycle behaviour.

## Edit Component Properties

As a JScene3D developer, I want to edit the authorable properties of a Component so that I can configure its data and behaviour for the Entity to which it belongs.

### Acceptance Criteria

- The authorable properties of a Component can be edited according to its declared property metadata.
- Properties not exposed as authorable are not directly editable.
- The appropriate value types and constraints are respected, including where applicable:
  - numbers;
  - booleans;
  - strings;
  - enums;
  - vectors;
  - colors;
  - Asset references;
  - Entity references;
  - Component references;
  - collections.
- Invalid values are rejected or clearly reported.
- Read-only properties cannot be modified.
- Changing a property modifies the containing authored definition and marks it as modified.
- Property changes are undoable and redoable.
- References are stored using the appropriate stable identity rather than display names where the domain model provides one.
- Editing authored Component properties does not instantiate the runtime Component or execute runtime lifecycle behaviour.

## Edit an Entity Transform

As a JScene3D developer, I want to edit a spatial Entity's Transform so that I can position, rotate, and scale it within its parent coordinate system.

### Acceptance Criteria

- A spatial Entity's authored Transform can be modified.
- The developer can edit:
  - position;
  - rotation/orientation;
  - scale.
- Transform values are local to the Entity's parent.
- Changing a parent Entity's Transform affects the resulting world Transform of its spatial descendants.
- Transform changes preserve the Entity's `EntityId`.
- Transform changes modify the containing authored definition and mark it as modified.
- Transform changes are undoable and redoable.
- Continuous Transform manipulation can be represented as a single logical undoable operation where appropriate.
- All authoring representations of the Transform reflect the same underlying authored state.
- Editing an authored Transform does not create or modify runtime World state.

## Enable or Disable an Entity

As a JScene3D developer, I want to enable or disable an Entity so that I can control whether it participates when the authored content is instantiated at runtime without deleting it.

### Acceptance Criteria

- An authored Entity can be enabled or disabled.
- The enabled state is persisted as part of the authored definition.
- Disabling an Entity does not remove the Entity, its Components, or its child Entities.
- Disabling an Entity does not change its `EntityId`.
- References to a disabled Entity remain valid.
- The semantics of how a disabled parent affects its descendants are defined consistently by the runtime model.
- Changing the enabled state modifies the containing authored definition and marks it as modified.
- Changing the enabled state is undoable and redoable.
- Changing the authored enabled state does not itself execute runtime lifecycle behaviour.

## Connect Component Signals and Actions

As a JScene3D developer, I want to connect a Component's signal to another Component's action so that Entities can communicate without requiring direct coupling between their Component implementations.

### Acceptance Criteria

- A signal exposed by a Component can be connected to a compatible action exposed by another Component.
- The signal and action may belong to Components on different Entities within the applicable authored graph.
- Connections use stable Entity, Component, and endpoint identities rather than display names.
- JScene3D validates that the selected signal and action are compatible.
- Multiple connections may originate from the same signal where permitted by the domain model.
- Removing or changing an Entity, Component, or endpoint that participates in a connection identifies the affected connection.
- Creating or removing a connection modifies the containing authored definition and marks it as modified.
- Creating and removing connections are undoable and redoable.
- Authoring a connection does not invoke the signal, action, or runtime Component behaviour.

## Change an Entity's Component Composition

As a JScene3D developer, I want to change the combination of Components attached to an Entity so that I can evolve the Entity's capabilities while maintaining a valid composition.

### Acceptance Criteria

- An Entity's Component composition can change over time through normal add and remove operations.
- JScene3D validates the resulting Component composition against declared Component requirements and incompatibilities.
- Adding or removing a Component does not change the Entity's `EntityId`.
- Existing unaffected Components retain their `ComponentId` values and authored properties.
- JScene3D identifies dependencies that would become invalid before allowing a Component composition change.
- A composition change does not silently remove or modify unrelated Components.
- The resulting authored Entity remains valid according to the available Component metadata.
- Component composition changes modify the containing authored definition and mark it as modified.
- Component composition changes participate in undo and redo.
- Changing authored Component composition does not execute runtime Component behaviour.

## Duplicate an Entity

As a JScene3D developer, I want to duplicate a local Entity so that I can create a new independent Entity based on existing authored content.

### Acceptance Criteria

- A local Entity can be duplicated within its containing authored definition.
- The duplicate receives a new `EntityId`.
- The Entity's Components are duplicated with new `ComponentId` values.
- Child Entities are duplicated recursively with new `EntityId` and `ComponentId` values.
- Authored Component property values are copied to the duplicate.
- Internal references within the duplicated subtree are rewritten to refer to the corresponding duplicated Entities and Components where appropriate.
- References to Assets outside the duplicated subtree continue to reference the same shared Assets.
- The duplicate is independent of the original Entity; subsequent changes to either do not affect the other.
- Duplicating an Entity modifies the containing authored definition and marks it as modified.
- Duplicating an Entity is undoable as a single logical authoring operation.

## Edit a Component Reference

As a JScene3D developer, I want to assign references from a Component property to other Entities, Components, or Assets so that Components can refer safely to other authored content.

### Acceptance Criteria

- A Component property declared as a reference can be assigned an appropriate target.
- Supported reference targets include, where defined by the property metadata:
  - Assets;
  - Entities;
  - Components;
  - Resources.
- Only targets compatible with the property's declared type or capability can be assigned.
- Entity and Component references use stable identity rather than display names or hierarchy paths.
- Asset references use stable asset identity where the referenced asset type supports it.
- A reference can be cleared when the property is optional.
- Required references cannot be left unresolved without producing a validation problem.
- Renaming a referenced object does not break an identity-based reference.
- JScene3D identifies references whose targets have been deleted or otherwise become unavailable.
- Changing a reference modifies the containing authored definition and marks it as modified.
- Reference changes are undoable and redoable.
- Editing an authored reference does not execute runtime Component behaviour.

## Edit a Component Collection Property

As a JScene3D developer, I want to edit collection-valued Component properties so that I can configure Components that require multiple values or references.

### Acceptance Criteria

- A Component property declared as a collection can contain multiple values.
- Collection element types and constraints are defined by the Component's property metadata.
- The developer can add and remove collection elements where the property is editable.
- Collection elements can be reordered where ordering is semantically meaningful.
- Each collection element is validated according to its declared type and constraints.
- Collections of references follow the same stable-identity rules as individual reference properties.
- Duplicate values are allowed or rejected according to the property's declared semantics.
- Required minimum or maximum collection sizes are enforced where declared.
- Editing a collection modifies the containing authored definition and marks it as modified.
- Collection changes are undoable and redoable.
- Editing an authored collection does not execute runtime Component behaviour.

## Reset a Component Property

As a JScene3D developer, I want to reset a Component property to its defined default value so that I can easily undo explicit customization without needing to know the default value myself.

### Acceptance Criteria

- An editable Component property can be reset when its Component type defines a default value.
- Resetting restores the value defined by the Component's authoring metadata or domain contract.
- JScene3D distinguishes between an explicitly authored value and the applicable default where that distinction exists in the persisted model.
- Resetting a reference property restores its defined default, including `null` or no reference where appropriate.
- Resetting a collection property restores its defined default collection.
- A property already at its default does not produce an unnecessary authored change.
- Resetting a property modifies the containing authored definition when its persisted state changes.
- Resetting a property is undoable and redoable.
- Resetting authored state does not execute runtime Component behaviour.

## Edit a Component Enum Property

As a JScene3D developer, I want to select a value for an enum-valued Component property so that I can configure the Component using the values supported by its domain contract.

### Acceptance Criteria

- A Component property declared as an enum exposes the values defined by its property metadata.
- Only valid enum values can be assigned.
- The authored value uses the stable persisted representation defined by the Component contract rather than a presentation label.
- Human-readable labels may differ from the persisted enum value.
- An unknown persisted enum value is reported rather than silently replaced.
- Changing the enum value modifies the containing authored definition and marks it as modified.
- Changing the enum value is undoable and redoable.
- Editing an authored enum property does not execute runtime Component behaviour.

## Edit a Component Numeric Property

As a JScene3D developer, I want to edit a numeric Component property so that I can configure numerical values within the constraints defined by the Component.

### Acceptance Criteria

- Numeric properties support the numeric type declared by their property metadata.
- Numeric constraints are enforced where defined, including:
  - minimum value;
  - maximum value;
  - valid numeric range;
  - integer versus floating-point values.
- A property may define a recommended editing increment or precision without changing the underlying value semantics.
- Values can be entered directly rather than requiring interaction with a graphical control.
- Invalid numeric input is clearly reported and does not silently replace the existing valid value.
- Numeric values are persisted using the representation defined by the Component contract.
- Changing a numeric property modifies the containing authored definition and marks it as modified.
- Numeric property changes are undoable and redoable.
- Editing an authored numeric property does not execute runtime Component behaviour.

## Edit a Component Vector Property

As a JScene3D developer, I want to edit a vector-valued Component property so that I can configure multidimensional values such as positions, directions, scales, and other vector data.

### Acceptance Criteria

- A vector property exposes the number of components defined by its property type.
- Vector components can be edited individually.
- Component labels reflect the semantics defined by the property metadata, such as `X`, `Y`, and `Z` where appropriate.
- Numeric constraints are applied to individual components where defined.
- The vector is treated as one authored property even when its components are edited independently.
- Invalid component values are clearly reported and do not silently replace valid values.
- Vector values are persisted using the representation defined by the Component contract.
- Changing a vector property modifies the containing authored definition and marks it as modified.
- Vector property changes are undoable and redoable.
- Editing an authored vector property does not execute runtime Component behaviour.

## Edit a Component Color Property

As a JScene3D developer, I want to edit a color-valued Component property so that I can configure colors without manually entering their underlying numeric representation.

### Acceptance Criteria

- A color property exposes the color representation defined by its property metadata.
- Individual color channels can be edited directly.
- A color may include an alpha channel where supported by the property type.
- Color values are validated according to the property's defined range and representation.
- The authored value is persisted using the representation defined by the Component contract.
- Changing a color property modifies the containing authored definition and marks it as modified.
- Color property changes are undoable and redoable.
- Editing an authored color property does not execute runtime Component behaviour.

## Edit a Component Boolean Property

As a JScene3D developer, I want to edit a boolean Component property so that I can enable or disable a Component-specific option.

### Acceptance Criteria

- A boolean property supports the two values defined by its property type: `true` and `false`.
- The current authored value is clearly represented.
- Changing the value persists the corresponding boolean value rather than a presentation-specific representation.
- A boolean property's value is distinct from the enabled state of the Entity or Component itself.
- Changing a boolean property modifies the containing authored definition and marks it as modified.
- Boolean property changes are undoable and redoable.
- Editing an authored boolean property does not execute runtime Component behaviour.

## Edit a Component String Property

As a JScene3D developer, I want to edit a string-valued Component property so that I can configure textual values required by the Component.

### Acceptance Criteria

- A string property accepts textual input according to its property metadata.
- Empty strings are allowed or rejected according to the property's declared constraints.
- Length or format constraints are enforced where defined.
- Multiline text is supported where declared by the property metadata.
- The authored string is persisted without presentation-specific formatting.
- Invalid values are clearly reported and do not silently replace the existing valid value.
- Changing a string property modifies the containing authored definition and marks it as modified.
- String property changes are undoable and redoable.
- Editing an authored string property does not execute runtime Component behaviour.

## Edit a Component Asset Reference Property

As a JScene3D developer, I want to assign an Asset to a Component property so that the Component can use reusable Project content without depending on its physical location.

### Acceptance Criteria

- A Component property declared as an Asset reference can reference a compatible Asset.
- Only Assets compatible with the property's declared type or capability can be assigned.
- The reference uses the Asset's stable `AssetId` where the referenced Asset participates in the persistent Asset identity model.
- Renaming or moving the referenced Asset does not break an identity-based reference.
- The current referenced Asset can be cleared when the property is optional.
- A required Asset reference that cannot be resolved produces a validation problem.
- Deleting a referenced Asset identifies the affected Component property.
- Changing an Asset reference modifies the containing authored definition and marks it as modified.
- Asset-reference changes are undoable and redoable.
- Editing an authored Asset reference does not load or execute runtime Component behaviour.

## Edit a Component Entity Reference Property

As a JScene3D developer, I want to assign an Entity to a Component property so that the Component can refer reliably to another Entity within the applicable authored graph.

### Acceptance Criteria

- A Component property declared as an Entity reference can reference a compatible Entity.
- Only Entities compatible with constraints declared by the property metadata can be assigned.
- The reference uses the target Entity's stable `EntityId` rather than its name or hierarchy path.
- Renaming or reparenting the referenced Entity does not break the reference.
- The reference can be cleared when the property is optional.
- A required Entity reference that cannot be resolved produces a validation problem.
- Deleting a referenced Entity identifies the affected Component property.
- An Entity cannot be selected when it is outside the reference scope permitted by the authored model.
- Changing an Entity reference modifies the containing authored definition and marks it as modified.
- Entity-reference changes are undoable and redoable.
- Editing an authored Entity reference does not execute runtime Component behaviour.

## Edit a Component Component Reference Property

As a JScene3D developer, I want to assign another Component to a Component property so that Components can collaborate through stable authored references.

### Acceptance Criteria

- A Component property declared as a Component reference can reference a compatible Component.
- Only Components compatible with the property's declared type or capability can be assigned.
- The reference identifies the target using its `EntityId` and `ComponentId` rather than display names or Component ordering.
- Renaming or reparenting the target Entity does not break the reference.
- Changes to unrelated Components on the target Entity do not break the reference.
- The reference can be cleared when the property is optional.
- A required Component reference that cannot be resolved produces a validation problem.
- Removing the referenced Component or deleting its owning Entity identifies the affected Component property.
- A Component cannot be selected when it is outside the reference scope permitted by the authored model.
- Changing a Component reference modifies the containing authored definition and marks it as modified.
- Component-reference changes are undoable and redoable.
- Editing an authored Component reference does not execute runtime Component behaviour.

## Edit a Component Resource Reference Property

As a JScene3D developer, I want to assign a Resource to a Component property so that the Component can use compatible authored or imported resource data.

### Acceptance Criteria

- A Component property declared as a Resource reference can reference a compatible Resource.
- Only Resources compatible with the property's declared resource type can be assigned.
- The reference uses the appropriate JScene3D resource-reference mechanism for the target.
- Authored, source-backed, and import-generated Resources remain distinguishable according to their domain semantics.
- An import-generated Resource may remain linked to its Import while referenced by authored content.
- The reference can be cleared when the property is optional.
- A required Resource reference that cannot be resolved produces a validation problem.
- Removing or invalidating the referenced Resource identifies the affected Component property.
- Changing a Resource reference modifies the containing authored definition and marks it as modified.
- Resource-reference changes are undoable and redoable.
- Editing an authored Resource reference does not execute runtime Component behaviour.

## Edit a Component Optional Property

As a JScene3D developer, I want to set or clear an optional Component property so that I can distinguish between explicitly configured values and the absence of a value.

### Acceptance Criteria

- A Component property may be declared optional by its property metadata.
- An optional property can contain a valid value or be unset.
- Unsetting a property is distinct from assigning an arbitrary placeholder value.
- When unset, the Component's defined domain semantics determine the resulting behaviour.
- A required property cannot be unset.
- The persisted representation preserves the distinction between an unset property and an explicitly authored value where that distinction is meaningful.
- Setting or clearing an optional property modifies the containing authored definition when its persisted state changes.
- Setting and clearing an optional property are undoable and redoable.
- Editing an optional authored property does not execute runtime Component behaviour.

## Validate Component Properties

As a JScene3D developer, I want invalid Component configuration to be identified while authoring so that I can correct problems before running the Project.

### Acceptance Criteria

- JScene3D validates authored Component properties against the Component's declared metadata and domain rules.
- Validation can identify problems including:
  - missing required values;
  - values outside permitted constraints;
  - invalid enum values;
  - unresolved references;
  - incompatible reference targets;
  - invalid collection contents.
- Validation can include rules involving multiple properties when the Component contract requires them.
- Validation problems identify the affected Component and property where applicable.
- Invalid authored state is not silently corrected or replaced with a default value.
- Validation does not require instantiating or executing the runtime Component implementation.
- Validation results update when the relevant authored state changes.
- Resolving the underlying problem removes the corresponding validation issue.

## Validate an Entity's Component Composition

As a JScene3D developer, I want invalid combinations of Components on an Entity to be identified while authoring so that I can correct structural problems before running the Project.

### Acceptance Criteria

- JScene3D validates an Entity's Component composition against declared Component metadata and domain rules.
- Validation can identify problems including:
  - missing required Components or capabilities;
  - incompatible Components;
  - violations of Component multiplicity rules;
  - unresolved dependencies between Components.
- Validation considers the complete Component composition of the Entity where necessary.
- Validation problems identify the affected Entity and Components.
- Invalid composition is not silently corrected unless the domain contract explicitly defines an automatic resolution.
- Validation does not require executing runtime Component behaviour.
- Validation results update when Components are added, removed, or otherwise changed.
- Resolving the underlying composition problem removes the corresponding validation issue.

## Validate a Scene

As a JScene3D developer, I want a SceneDefinition to be validated while authoring so that structural and reference problems can be identified before the Scene is instantiated at runtime.

### Acceptance Criteria

- JScene3D validates the authored structure of a SceneDefinition.
- Validation can identify problems including:
  - invalid Entity hierarchy;
  - cyclic parent/child relationships;
  - duplicate Entity or Component identities within their applicable scope;
  - invalid Component configuration;
  - invalid Component composition;
  - unresolved Entity or Component references;
  - invalid signal/action connections;
  - unresolved Asset or Resource references;
  - invalid or unresolved EntityDefinition placements;
  - invalid placement arguments.
- Validation problems identify the affected Scene, Entity, Component, property, placement, or connection where applicable.
- Multiple validation problems can be reported without stopping at the first problem where safe to do so.
- Scene validation does not require creating a runtime World or executing runtime Component behaviour.
- Validation results update when relevant authored state changes.
- Resolving an underlying problem removes the corresponding validation issue.

## Validate a Project

As a JScene3D developer, I want the Project to be validated while authoring so that project-level problems can be identified before attempting to run the application.

### Acceptance Criteria

- JScene3D validates Project-level configuration and authored content.
- Validation can identify problems including:
  - invalid Project configuration;
  - an invalid or unresolved Main Scene;
  - duplicate Asset identities;
  - invalid SceneDefinitions;
  - invalid EntityDefinitions;
  - unresolved Asset references;
  - unavailable Component or Resource types;
  - invalid extension metadata;
  - invalid Import Definitions;
  - missing or invalid generated import content.
- A Project without a Main Scene is valid for authoring and does not produce an error solely for that reason.
- The absence of a Main Scene prevents `Run Project` rather than preventing the Project from being opened or edited.
- Validation problems identify the affected Project content where applicable.
- Multiple validation problems can be reported without stopping at the first problem where safe to do so.
- Project validation does not require creating a runtime World or executing application Component behaviour.
- Validation results update when relevant Project or authored state changes.
- Resolving an underlying problem removes the corresponding validation issue.

## Run the Project

As a JScene3D developer, I want to run my Project so that I can execute and test the application beginning from its configured Main Scene.

### Acceptance Criteria

- `Run Project` is available when the Project has a valid Main Scene.
- `Run Project` is disabled when no Main Scene is configured or the configured Main Scene cannot be resolved.
- Running the Project creates a runtime World.
- The Project's Main Scene is instantiated into the World as the initial Scene content.
- Runtime Component lifecycle behaviour begins only after runtime instantiation.
- The Scene currently active for authoring does not determine which Scene is run.
- Running the Project does not modify authored SceneDefinitions or EntityDefinitions.
- Authored unsaved changes are handled according to the Project's defined run/save policy.
- Runtime state remains separate from authored state.
- Runtime failures are reported without corrupting authored Project content.
- The running Project can be stopped explicitly.
- Stopping the Project disposes of the runtime World and its live runtime state without closing the authored Project.

## Stop the Running Project

As a JScene3D developer, I want to stop a running Project so that I can return to authoring without closing or reopening the Project.

### Acceptance Criteria

- A running Project can be stopped explicitly.
- Stopping begins an orderly shutdown of the runtime World.
- Runtime Component shutdown and cleanup behaviour is invoked according to the runtime lifecycle contract.
- Runtime resources and processes associated with the running Project are released.
- Stopping the Project does not close the authored Project.
- Open SceneDefinitions and EntityDefinitions remain available for authoring after runtime shutdown.
- Runtime state is discarded and is not silently written back into authored content.
- Authored state remains unchanged by the stop operation.
- After shutdown completes, `Run Project` becomes available again when a valid Main Scene remains configured.
- Runtime shutdown failures are reported without corrupting authored Project content.

## Handle Runtime Failure

As a JScene3D developer, I want runtime failures to be isolated from authored Project state so that I can diagnose execution problems and safely return to authoring.

### Acceptance Criteria

- A failure in the running Project does not corrupt or modify authored Project content.
- JScene3D clearly reports that runtime execution has failed.
- Available failure information identifies the relevant runtime error and originating Component or subsystem where possible.
- A runtime failure does not cause the authored Project to close.
- JScene3D performs orderly runtime cleanup where possible after a failure.
- Runtime processes and resources are released when the failed runtime terminates.
- Open SceneDefinitions and EntityDefinitions remain available for authoring.
- Runtime state from the failed execution is discarded.
- After runtime cleanup completes, the Project can be run again when its Main Scene remains valid.
- Runtime diagnostics remain available long enough for the developer to investigate the failure.

## Run an Arbitrary Scene

As a JScene3D developer, I want to run an arbitrary Scene independently of the Project's Main Scene so that I can test the Scene I am currently developing.

### Acceptance Criteria

- An authored Scene can be run without changing the Project's Main Scene.
- `Run Scene` identifies explicitly which Scene will be executed.
- Running a Scene creates a runtime World for that execution.
- The selected Scene is instantiated into the runtime World.
- Runtime Component lifecycle behaviour executes normally.
- Running a Scene does not modify the Project's `mainScene` configuration.
- Running a Scene does not modify the authored SceneDefinition.
- Runtime state remains separate from authored state.
- The running Scene can be paused, resumed, and stopped using the same runtime controls as a normally running Project.
- Dependencies required by the Scene are resolved from the containing Project.
- If the Scene cannot be executed independently because required runtime configuration is unavailable, JScene3D reports the problem clearly rather than silently falling back to the Main Scene.

## Pause and Resume Runtime Execution

As a JScene3D developer, I want to pause and resume a running Project or Scene so that I can inspect runtime state at a specific point without terminating the runtime World.

### Acceptance Criteria

- A running Project or Scene can be paused.
- Pausing keeps the runtime World and its live Entities and Components alive.
- Simulation and normal runtime update processing stop while paused.
- Runtime state is preserved while paused.
- The developer can inspect live runtime state while execution is paused.
- The developer can modify supported live runtime values while execution is paused.
- A paused runtime can be resumed.
- Resuming continues execution from the preserved runtime state rather than creating a new World.
- A paused runtime can be stopped without first being resumed.
- Pausing or resuming does not modify authored Project content.
- Runtime failures while pausing or resuming are reported without corrupting authored state.

## Inspect Live Runtime State

As a JScene3D developer, I want to inspect the live state of Entities and Components while a Project or Scene is running so that I can understand and debug runtime behaviour.

### Acceptance Criteria

- The developer can inspect the live runtime World while execution is running or paused.
- Live runtime Entities can be inspected independently of their authored definitions.
- The developer can inspect the live Components attached to a runtime Entity.
- Runtime Component properties and other explicitly inspectable runtime state can be viewed.
- Runtime values reflect the current live state rather than the original authored values.
- Runtime-created Entities and Components can be inspected even when they have no directly corresponding authored object.
- Where a runtime object originates from authored content, JScene3D retains enough provenance to identify that relationship where possible.
- Inspecting runtime state does not modify authored content.
- Live inspection does not require pausing execution unless a particular operation requires a stable runtime state.
- When the runtime World is stopped, its live inspection state is discarded with the runtime.

## Modify Live Runtime State

As a JScene3D developer, I want to modify supported live Entity and Component values while a Project or Scene is running so that I can experiment with runtime behaviour without changing authored content.

### Acceptance Criteria

- Supported live runtime values can be modified while execution is running or paused.
- Runtime editing operates on live Entity and Component instances rather than their authored definitions.
- Runtime-editable values are determined by the runtime domain contract and are not assumed to include every internal Java field.
- Changes take effect in the current runtime World.
- Runtime changes do not automatically modify the originating SceneDefinition, EntityDefinition, or other authored content.
- Stopping the runtime discards runtime-only changes.
- Running the Project or Scene again begins from authored state rather than values changed during the previous execution.
- Runtime-created Entities and Components can be modified where their runtime contract permits it.
- Invalid runtime values are rejected or clearly reported.
- Runtime modification remains available while paused for values that can safely be changed in that state.
- Any future operation for explicitly applying selected runtime changes back to authored content is a separate workflow and is not implicit in live editing.

## Restart Runtime Execution

As a JScene3D developer, I want to restart a running Project or Scene so that I can quickly return to its initial authored state and test again.

### Acceptance Criteria

- A running or paused Project or Scene can be restarted.
- Restarting disposes of the current runtime World and its live state.
- Runtime-only modifications made during the current execution are discarded.
- Runtime-created Entities and Components are discarded.
- A new runtime World is created.
- When restarting a Project, the configured Main Scene is instantiated again.
- When restarting an arbitrary Scene, the same Scene is instantiated again.
- The new runtime begins from the current authored Project content.
- Restarting does not modify authored content.
- Runtime lifecycle shutdown and startup behaviour occurs according to the runtime lifecycle contract.
- If restart fails, JScene3D reports the failure without corrupting authored Project content.

## Apply Runtime Changes to Authored Content

As a JScene3D developer, I want to explicitly apply selected runtime changes back to authored content so that useful adjustments made while testing can be retained.

### Acceptance Criteria

- Applying runtime changes to authored content is an explicit operation.
- Runtime changes are never written back to authored content automatically.
- Only runtime objects with a valid relationship to authored content are eligible for application.
- JScene3D identifies which supported values differ between the live runtime object and its authored source.
- The developer can choose which eligible changes to apply.
- Applying a change updates the appropriate authored SceneDefinition, EntityDefinition, placement, or Component property.
- Runtime-created objects with no authored counterpart are not automatically added to authored content.
- Changes that cannot be mapped safely to authored content are identified and cannot be applied implicitly.
- Applying changes marks the affected authored content as modified.
- Applied changes participate in the normal authored undo/redo model.
- Applying changes does not terminate or recreate the current runtime World unless required by a particular change.
- Stopping the runtime without applying changes discards all remaining runtime-only modifications.

## Inspect Runtime Hierarchy

As a JScene3D developer, I want to inspect the hierarchy of live runtime Entities so that I can understand the actual structure of the running World.

### Acceptance Criteria

- The live runtime World exposes its current Entity hierarchy for inspection.
- The runtime hierarchy reflects the actual live state rather than merely reproducing the authored Scene hierarchy.
- Entities created dynamically at runtime appear in the runtime hierarchy.
- Entities removed at runtime disappear from the runtime hierarchy.
- Runtime reparenting is reflected in the hierarchy.
- Disabled runtime Entities remain identifiable where they continue to exist.
- Selecting a runtime Entity allows its live Components and inspectable runtime state to be examined.
- Where a runtime Entity originates from authored content, its authored provenance can be identified where possible.
- Runtime Entities with no authored counterpart are clearly distinguishable from authored-origin Entities.
- Inspecting the runtime hierarchy does not modify authored content.
- The runtime hierarchy exists only for the lifetime of the runtime World.

## Select a Runtime Entity

As a JScene3D developer, I want to select an Entity in the running World so that I can inspect and interact with its live runtime state.

### Acceptance Criteria

- A live Entity can be selected from the runtime hierarchy.
- A visible runtime Entity can also be selected from the runtime view where supported.
- Selecting a runtime Entity establishes a runtime selection distinct from authored Scene selection.
- The selected runtime Entity's live Components and inspectable state are available for inspection.
- Runtime values shown for the selected Entity reflect its current live state.
- Runtime-created Entities can be selected even when they have no authored counterpart.
- Where the selected runtime Entity originated from authored content, its authored provenance can be identified where possible.
- Selecting another runtime Entity replaces the current runtime selection.
- Runtime selection can be cleared.
- Selecting a runtime Entity does not modify authored content.
- Runtime selection is discarded when the runtime World is destroyed.

## Inspect Runtime Component State

As a JScene3D developer, I want to inspect the live Components of a runtime Entity so that I can understand how its behaviour and state evolve during execution.

### Acceptance Criteria

- The live Components attached to the selected runtime Entity can be inspected.
- Runtime Component state is distinct from the Component's authored configuration.
- Inspectable runtime values reflect their current live values.
- Components added dynamically at runtime can be inspected.
- Components removed at runtime no longer appear as part of the Entity's live state.
- Runtime-only state may be exposed for inspection even when it has no corresponding authored property, where explicitly permitted by the Component's runtime metadata.
- Internal implementation fields are not exposed automatically.
- Where a runtime Component originates from an authored Component, its authored provenance can be identified where possible.
- Inspecting runtime Component state does not modify authored content.
- Runtime Component inspection ends when the corresponding runtime object or World no longer exists.

## Observe Runtime Diagnostics

As a JScene3D developer, I want to observe diagnostics produced while a Project or Scene is running so that I can identify and investigate runtime problems.

### Acceptance Criteria

- Runtime diagnostics are available while execution is running, paused, and after execution has stopped.
- Diagnostics can include:
  - errors;
  - warnings;
  - relevant runtime messages.
- Diagnostics identify the originating runtime subsystem, Entity, or Component where that information is available.
- Where a runtime object has authored provenance, the diagnostic can identify the corresponding authored content where possible.
- Diagnostics from runtime-created objects remain understandable even when no authored counterpart exists.
- Runtime diagnostics are distinct from authoring and validation diagnostics.
- A runtime failure does not cause existing diagnostics to disappear immediately.
- Diagnostics from separate runtime executions can be distinguished.
- Observing diagnostics does not modify runtime or authored state.

## View Runtime Performance Information

As a JScene3D developer, I want to observe useful runtime performance information so that I can identify performance problems while testing my Project.

### Acceptance Criteria

- Runtime performance information can be observed while a Project or Scene is running.
- Available information may include, where supported:
  - frame rate;
  - frame time;
  - update time;
  - render time;
  - Entity and Component counts;
  - draw calls;
  - memory/resource information.
- Performance information reflects the current runtime execution rather than authored state.
- Performance observation does not modify runtime or authored content.
- Performance information can continue to be inspected while execution is paused where the metric remains meaningful.
- JScene3D does not require every runtime subsystem or renderer to expose the same metrics.
- Unsupported metrics are omitted rather than represented by misleading values.
- Detailed profiling capabilities beyond basic runtime performance information may be provided by a separate profiling workflow.

## Step Through Runtime Updates

As a JScene3D developer, I want to advance a paused runtime in controlled steps so that I can inspect how runtime state changes over time.

### Acceptance Criteria

- A paused runtime can be advanced without fully resuming normal execution.
- A step advances the runtime according to a clearly defined runtime update boundary.
- The runtime returns to the paused state after the step completes.
- Live Entity and Component state can be inspected after each step.
- Runtime hierarchy changes caused by the step are reflected immediately.
- Runtime diagnostics produced during the step remain available for inspection.
- Runtime-only modifications remain preserved across steps.
- Stepping does not modify authored content.
- Stopping or restarting while paused after a step follows the normal runtime lifecycle.
- The precise definition of a runtime step, including its relationship to update, fixed-update, physics, and rendering cycles, will be defined with the runtime lifecycle model.

## Restart from the Current Scene

As a JScene3D developer, I want to restart runtime execution from the Scene I am currently testing so that I can quickly repeat a test without returning to the Project's Main Scene.

### Acceptance Criteria

- When an arbitrary Scene is being run, the developer can restart that Scene directly.
- Restarting destroys the current runtime World and all runtime-only state.
- A new runtime World is created using the same Scene that initiated the current execution.
- The Scene is instantiated from the current authored state.
- Runtime-only property changes are discarded unless they were explicitly applied to authored content.
- Runtime-created Entities and Components are discarded.
- The Project's Main Scene configuration is not changed.
- Restarting does not require the Scene to be the Project's Main Scene.
- Runtime lifecycle shutdown and startup behaviour follows the normal runtime lifecycle contract.
- Runtime inspection state is reset to correspond to the newly created runtime World.
- Restarting does not modify authored content.

## Return from Runtime to Authoring

As a JScene3D developer, I want to return cleanly to my authoring context after runtime execution ends so that I can continue working without rebuilding my workspace.

### Acceptance Criteria

- Stopping, completing, or terminating runtime execution returns JScene3D to the authoring state.
- Open authored SceneDefinitions and EntityDefinitions remain open.
- The previously active authoring context is restored where appropriate.
- Authored selection and Scene authoring state are preserved where practical.
- Runtime hierarchy, runtime selection, and other live runtime-only state are removed.
- Runtime-only value changes are discarded unless explicitly applied to authored content.
- Runtime-created Entities and Components do not appear in authored content.
- Authoring views reflect the current authored state rather than the final runtime state.
- Runtime diagnostics from the completed execution remain available for investigation.
- Returning to authoring does not reload the Project unnecessarily.
- The Project can be edited or run again immediately after runtime cleanup completes.

## Handle External Changes to Authored Content

As a JScene3D developer, I want JScene3D to detect when authored content changes outside the editor so that external tools and source-control operations do not cause my work to be silently overwritten.

### Acceptance Criteria

- JScene3D detects when an open authored file is changed, moved, renamed, or deleted externally.
- If the corresponding authored content has no unsaved editor changes, JScene3D can reload the external change safely.
- If both the editor and the external file have changed, JScene3D does not silently overwrite either version.
- Conflicting changes are clearly reported to the developer.
- External moves and renames preserve stable asset identity when the authored `AssetId` remains unchanged.
- External additions of valid authored Assets become discoverable by the Project.
- External deletion of an Asset identifies references that have become unresolved.
- Source-control operations that modify Project content are treated consistently with other external filesystem changes.
- Reloading externally changed authored content updates validation and dependent authoring state.
- External changes do not execute runtime Component behaviour.

## Handle External Changes to Java Source and Extension Metadata

As a JScene3D developer, I want changes to my Java Components and extension metadata to be detected so that the authoring environment remains synchronized with the Project's available domain types.

### Acceptance Criteria

- JScene3D detects relevant changes to Project Java source and extension metadata.
- Changes to Component authoring metadata can be reflected without requiring the Project to be closed and reopened where safely possible.
- Newly available Component types become discoverable after the appropriate build or metadata refresh.
- Removed Component types cause existing authored usages to be reported as unresolved rather than silently discarded.
- Changes to Component property metadata cause affected authored content to be revalidated.
- Changes to registered Resource or importer types cause affected Project content to be revalidated.
- Java compilation failures are reported clearly and do not invalidate the last known usable authoring metadata unnecessarily.
- Authoring infrastructure does not load or execute arbitrary Project Component implementations merely to detect metadata changes.
- Changes requiring a rebuild clearly indicate that requirement.
- Refreshing Java or extension metadata does not modify authored Project content.

## Rebuild Project Java Code

As a JScene3D developer, I want to rebuild the Project's Java code from within JScene3D so that changes to Components and other application code become available without leaving the authoring environment.

### Acceptance Criteria

- The Project's Java code can be rebuilt using the Project's configured Maven build.
- JScene3D uses the Maven Wrapper when the Project provides one.
- Build progress is visible while the build is running.
- Build output and diagnostics remain available for inspection.
- Compilation errors identify the relevant Java source location where available.
- A failed build does not replace the last known usable compiled runtime or authoring metadata with incomplete output.
- A successful build makes updated Component and extension metadata available to the authoring environment.
- A successful build makes updated runtime implementations available for subsequent runtime execution.
- JScene3D revalidates affected authored content after updated metadata becomes available.
- Building does not automatically start or restart runtime execution.
- Building does not modify authored SceneDefinitions, EntityDefinitions, or other authored content.

## Open Java Source for a Component

As a JScene3D developer, I want to navigate from a Project-defined Component to its Java source so that I can modify its implementation using the normal Java development environment.

### Acceptance Criteria

- A Project-defined Component can be navigated to its Java source when source is available.
- JScene3D resolves the Component type to the corresponding source file and declaration.
- The Java source opens in the standard Code OSS text editor.
- Normal Java language features remain available, including navigation, completion, diagnostics, refactoring, and debugging where supported.
- Navigating to Java source does not modify the authored Entity or Component instance.
- Multiple authored Component instances of the same type navigate to the same Component implementation source.
- Components supplied only as compiled dependencies are handled appropriately when source is unavailable.
- Returning from Java source to authored content does not require reopening the Project.
- Editing Java source does not execute the Component implementation.
- Java source changes become available to authoring metadata and subsequent runtime execution through the appropriate build/refresh lifecycle.

## Open the Source Representation of Authored Content

As a JScene3D developer, I want to inspect the serialized source representation of authored content so that I can understand or troubleshoot the underlying Project data when necessary.

### Acceptance Criteria

- Authored content with a serialized source representation can be opened from its semantic representation.
- The source opens in an appropriate text editor.
- The developer can navigate from a SceneDefinition, EntityDefinition, Resource, Import Definition, or other supported authored object to its underlying source.
- Opening the source does not create a separate semantic Asset or change the Asset's identity.
- The source representation corresponds to the same authored content used by JScene3D.
- Structured source may be read-only by default where direct editing could bypass semantic validation or managed configuration.
- Where direct source editing is supported, it requires an explicit editing workflow.
- Changes made through supported direct source editing are detected and processed through the normal external-change and validation lifecycle.
- Generated/imported artifacts remain read-only and are not treated as authored source merely because their serialized representation can be inspected.
- Inspecting source does not execute runtime Component behaviour.

## Reveal Authored Content in Explorer

As a JScene3D developer, I want to reveal authored content in the physical filesystem so that I can access its underlying files when necessary.

### Acceptance Criteria

- Authored content with a physical Project location can be revealed in Explorer.
- Revealing an item selects its corresponding physical file where possible.
- The semantic Project view remains the primary representation of JScene3D content.
- Revealing a file does not change the Asset's identity or semantic classification.
- Moving or renaming files directly through Explorer is treated as an external or physical Project change and processed through the normal change-detection lifecycle.
- Generated/imported content is not presented as ordinary authored Project content merely because its cache files exist physically.
- Content without a directly meaningful physical Project file is handled appropriately rather than exposing internal implementation paths.
- Revealing content does not modify authored or runtime state.

## Import External Content

As a JScene3D developer, I want to import content from an external source so that I can use supported external formats within my Project.

### Acceptance Criteria

- The developer can create an Import using a source format supported by an available JScene3D importer.
- The Import identifies:
  - its source asset;
  - the importer;
  - selected source items where supported;
  - importer-specific settings.
- The developer can inspect selectable content exposed by the importer before choosing what to import where supported.
- Importing produces generated JScene3D content appropriate to the source and importer.
- Generated content may include:
  - EntityDefinitions;
  - Resources;
  - payloads;
  - other supported generated artifacts.
- Generated content remains linked to the Import and its external source.
- Generated content is read-only and is not treated as directly authored Project content.
- Importing does not overwrite unrelated authored content.
- Import progress is visible for operations that take meaningful time.
- Import failures produce clear diagnostics without replacing a previously valid published generation.
- A successful Import makes its generated content available to the Project.
- Re-import uses the normal generated-content and cache lifecycle rather than modifying generated artifacts in place.

## Re-import External Content

As a JScene3D developer, I want to re-import external content when its source or import configuration changes so that generated Project content reflects the current authoritative source.

### Acceptance Criteria

- An existing Import can be re-imported.
- JScene3D determines whether the existing generated publication is current before performing unnecessary regeneration.
- Changes to relevant import inputs cause a new generated publication to be produced.
- Relevant inputs include, where applicable:
  - external source content;
  - importer version;
  - selected source items;
  - import settings;
  - importer-declared dependencies.
- Re-import produces a new immutable generated publication rather than modifying the active generated artifacts in place.
- The new publication becomes active only after it has been generated and validated successfully.
- If re-import fails, the previous valid publication remains available.
- Stable generated identities are preserved where the importer-defined source identities remain stable.
- Generated objects that no longer exist in the source are no longer provided by the new publication.
- Authored Project content is not overwritten by re-import.
- Content previously converted into independent authored content is not affected by re-import.
- Re-import progress is visible for operations that take meaningful time.
- Re-import diagnostics identify problems with the source, importer, configuration, or generated content where possible.

## Convert Generated Content to Authored Content

As a JScene3D developer, I want to convert generated imported content into independently authored Project content so that I can modify it without remaining dependent on the external source.

### Acceptance Criteria

- Supported generated content can be explicitly converted into authored Project content.
- Conversion is an explicit operation and never occurs automatically.
- The developer is informed that the converted content will no longer receive updates from the generated source.
- The resulting content becomes normal Project-owned authored content.
- JScene3D assigns new authored Asset identity where required to avoid collisions with the generated source.
- The converted content can be modified using the same authoring operations as equivalent content created directly within the Project.
- Re-importing the original external source does not overwrite or modify the converted authored content.
- The original Import and generated content remain unchanged unless explicitly removed separately.
- Other consumers of the original generated content are not automatically redirected to the authored copy.
- Conversion preserves the semantic content and relationships that can be transferred safely.
- Conversion does not silently leave import-backed dependencies while presenting the result as fully independent.
- If complete conversion requires additional dependent generated content, JScene3D identifies those dependencies before conversion proceeds.
- The exact dependency-closure and identity-remapping rules follow the imported-content conversion model defined for JScene3D.

## Inspect Generated Imported Content

As a JScene3D developer, I want to inspect generated content produced by an Import so that I can understand what JScene3D created from the external source before deciding how to use it.

### Acceptance Criteria

- The generated artifacts belonging to an Import can be discovered and inspected.
- Generated content remains clearly identified as import-owned and read-only.
- The developer can inspect generated:
  - EntityDefinitions;
  - Resources;
  - payload metadata;
  - other supported artifact types.
- Generated EntityDefinitions can be inspected structurally, including their Entities and Components.
- Generated Resources expose their type and authoring-relevant metadata where available.
- The Import from which a generated artifact originated can be identified.
- Generated content is not presented as directly authored Project content.
- Inspecting generated content does not create an authored copy.
- Inspecting generated content does not modify the generated cache or active Import generation.
- Where supported, generated content can be used by normal authored composition without first being converted to authored content.
- Generated content eligible for conversion provides access to the explicit conversion workflow.

## Remove an Import

As a JScene3D developer, I want to remove an Import that is no longer required so that unused external-source dependencies and generated content can be removed from the Project safely.

### Acceptance Criteria

- An Import can be removed from the Project.
- JScene3D identifies authored content that references generated artifacts belonging to the Import.
- An Import with active authored dependencies is not silently removed.
- The developer is informed which authored content depends on the Import.
- Dependent references must be resolved before removal can complete.
- Removing the Import removes its Import Definition from the Project.
- Generated content belonging exclusively to the removed Import is no longer available to Project resolution.
- Authored content previously converted from generated content is unaffected because it is independent of the Import.
- Removing an Import does not delete or modify the original external source asset unless that source is explicitly removed separately.
- Removing an Import does not affect unrelated Imports or authored Assets.
- Obsolete generated cache data may be reclaimed according to the Project's cache-management policy.

## Refresh an Import

As a JScene3D developer, I want to refresh an Import's status so that I can determine whether its generated content is current without unnecessarily regenerating it.

### Acceptance Criteria

- JScene3D can determine the current status of an Import.
- Import status distinguishes at least:
  - current;
  - stale;
  - missing generated publication;
  - invalid or failed.
- Status is determined from the Import's authoritative inputs and the currently published generation.
- Checking Import status does not regenerate content when the existing publication is current.
- When an Import is stale, the developer can explicitly initiate re-import.
- A stale Import does not silently replace its last valid generated publication.
- Import status updates when relevant source content, configuration, selection, importer version, or declared dependencies change.
- Status diagnostics identify why an Import cannot be considered current where that information is available.
- Refreshing Import status does not modify authored Project content.

## Add an External Source Asset

As a JScene3D developer, I want to add an external source asset to my Project so that it can be referenced directly or used as input to an Import.

### Acceptance Criteria

- A supported external file can be added as a Project source asset.
- The source asset receives a project-local symbolic identity.
- The source asset records its physical source location.
- The source asset records its declared source type.
- Adding a source asset does not automatically create an Import.
- Multiple Imports can use the same source asset where appropriate.
- Moving the underlying file through a supported Project operation preserves the source asset's symbolic identity and updates its physical location.
- Content referring to the source asset through its symbolic identity remains valid when its physical location changes through a supported operation.
- Removing or making the underlying source file unavailable produces a clear validation problem.
- Adding a source asset does not copy, unpack, or transform it unless explicitly required by the source-asset workflow.
- Adding a source asset modifies Project configuration and marks the appropriate Project content as modified.

## Remove an External Source Asset

As a JScene3D developer, I want to remove an external source asset that is no longer required so that unused source dependencies can be removed from the Project safely.

### Acceptance Criteria

- An external source asset can be removed from the Project.
- JScene3D identifies Imports and authored content that reference the source asset.
- A source asset with active dependencies is not silently removed.
- The developer is informed which Project content depends on the source asset.
- Dependent usages must be resolved before removal can complete.
- Removing the source asset removes its declaration from the Project.
- Removing the source asset does not automatically delete the underlying external file.
- Generated content belonging to an Import that depends on the removed source asset cannot remain valid once that dependency is removed.
- Independently authored content previously converted from generated content is unaffected.
- Removing a source asset does not affect unrelated source assets, Imports, or authored Assets.
- Removing a source asset modifies Project configuration and marks the appropriate Project content as modified.

## Replace an External Source Asset

As a JScene3D developer, I want to replace the physical content of an existing source asset so that Imports and references can use an updated source without changing the source asset's Project identity.

### Acceptance Criteria

- An existing source asset can be updated to reference replacement source content.
- The source asset retains its project-local symbolic identity.
- Existing references to the source asset remain valid.
- JScene3D detects when the replacement content changes the authoritative source bytes.
- Imports depending on the source asset are identified as stale when their relevant inputs have changed.
- Existing valid generated publications are not silently overwritten during source replacement.
- Re-import remains an explicit operation using the normal Import lifecycle.
- Replacing source content does not modify independently authored content.
- Independently authored content previously converted from generated content remains unaffected.
- If the replacement source is invalid or incompatible with its declared source type, JScene3D reports the problem clearly.
- Replacing a source asset modifies the appropriate Project configuration or source state and triggers relevant validation.

## Inspect Source Asset Details

As a JScene3D developer, I want to inspect an external source asset so that I can understand its identity, source type, physical location, and current status.

### Acceptance Criteria

- A source asset's Project-local symbolic identity can be inspected.
- The source asset's declared source type is available.
- Its current physical source location is available.
- Integrity information such as a declared or calculated content digest is available where applicable.
- JScene3D indicates whether the physical source currently exists and is valid for its declared source type.
- Imports that depend on the source asset can be identified.
- The developer can distinguish the source asset itself from generated content produced from it.
- Inspecting a source asset does not cause an Import to run.
- Inspecting a source asset does not unpack or transform its content unless an explicit inspection capability provided by its source type requires it.
- Inspecting a source asset does not modify authored or generated Project content.

## Inspect External Source Contents

As a JScene3D developer, I want to inspect the contents of a supported external source so that I can understand what content is available before deciding what to import.

### Acceptance Criteria

- A source type may provide structured inspection of its external content.
- Inspection exposes source items using semantic information provided by the source type or importer rather than presenting only the raw file structure.
- Source items have stable importer-owned identities where the source format permits them.
- The developer can inspect available source items without creating generated Project content.
- Inspection may expose information such as:
  - item name;
  - item kind;
  - hierarchy or grouping;
  - other format-specific metadata.
- Different importers may expose different selectable views of the same source asset.
- Inspection does not modify the external source.
- Inspection does not modify authored Project content.
- Inspection does not replace or invalidate an existing generated publication.
- Large external sources can be inspected without requiring all source data to become persistent Project content.
- Unsupported or invalid source content produces clear diagnostics.
