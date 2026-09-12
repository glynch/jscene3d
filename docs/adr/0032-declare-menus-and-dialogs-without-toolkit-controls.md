# Declare menus and dialogs without toolkit controls

Status: accepted on 2026-09-12.

## Context

The editor initially constructed File and Edit menu items directly inside the
JavaFX workspace. Although extensions could already register command behavior and
command placements, the host discarded command metadata and exposed no observable
enabled state or menu renderer. Project Settings was consequently placed in File,
and adding Play controls would have required another independent command-state
implementation.

Core editor modules and extensions both need menus. Extensions also need a safe
way to request ordinary modal interactions without constructing JavaFX controls or
depending on the desktop adapter.

## Decision

Commands retain their stable identity, title, implementation, and mutable enabled
state in the extension host. Every placement of a command observes that same
state. A disabled command cannot be invoked through programmatic dispatch.

Top-level menus are declarative contributions identified by command-location IDs.
An extension can place a registered command in an existing editor-owned menu, or
register its own top-level menu and place commands there. Placements supply a
group and order. The JavaFX adapter inserts separators between adjacent groups
and creates all toolkit controls from complete immutable menu snapshots.

The initial editor-owned menus are:

- JScene3D: About JScene3D, Settings, and Quit JScene3D;
- File: Open Project and Save;
- Edit: Undo and Redo.

Settings opens the generated project-settings editor surface. About uses a modal
dialog. Window close and Quit share one dirty-resource guard offering Save, Don't
Save, and Cancel. Save operates on every dirty registered working copy; a failed
save leaves the editor open.

Modal dialogs are declared as toolkit-independent text plus ordered semantic
buttons. `EditorWindow` presents the declaration and returns the selected stable
button identity. The JavaFX adapter owns modality, platform button roles, and
visual presentation.

## Consequences

Extensions do not construct menu items, separators, dialogs, or other JavaFX
objects. Core commands and extension commands follow the same registration,
placement, execution, and enablement path. The Play, Pause, and Stop command set
can use this state model without introducing a second toolbar-specific mechanism.

Menu IDs, command IDs, groups, ordering, and dialog button IDs are compatibility
contracts. Keyboard shortcuts remain editor-owned presentation policy in this
slice; extension keybinding contribution requires a separate conflict and
customisation design.
