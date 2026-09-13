# Editor command surfaces and keybindings

Status: direction recorded on 2026-09-13; detailed interaction design is
deferred until implementation.

## Purpose

The editor needs consistent commands across menus, toolbars, context menus, and
keyboard shortcuts. This document captures the agreed direction without fixing
the complete command catalogue, shortcut syntax, or extension interface before
those features are implemented.

The command module should remain deep: callers name an operation and its exact
semantic target, while command lookup, enablement, presentation, localization,
and dispatch remain behind a small toolkit-independent interface.

## Existing foundation

The current editor already provides the foundation for this work:

- commands have stable identities and one registered implementation;
- command placements target editor-owned locations and carry group and ordering
  metadata;
- view item context menus can filter placements by a semantic context value;
- `EditorCommandContext.argument()` carries the exact logical item that invoked
  a command, independently of mutable global selection;
- every placement observes the registered command's enabled state; and
- preview and pinned file openings share one file-editor path.

[ADR 0032](../adr/0032-declare-menus-and-dialogs-without-toolkit-controls.md)
remains authoritative for declarative commands, placements, and toolkit
ownership.

## Command-first interaction model

An operation is registered once under a stable command ID. Menus, toolbars,
context menus, and keybindings invoke that command rather than implementing the
operation themselves. JavaFX adapters render the presentation but do not own
command behavior.

An item-oriented invocation supplies the exact semantic argument from the
surface that was used. Examples include a workspace entry, open editor tab,
authored entity, component instance, or text selection. Commands must not infer
that target from a global selection which may have changed before execution.

Availability is contextual. A command can be absent, disabled, or enabled based
on the target kind, editor state, working-copy state, language capability, and
platform capability. The final condition language and contribution interface
remain open design decisions.

## Initial command surfaces

The following catalogue records useful first-party behavior rather than a
promise to reproduce every Visual Studio Code command.

### Workspace Explorer

- Open and Open With;
- Copy Path and Copy Relative Path;
- Reveal in Finder on macOS, Reveal in File Explorer on Windows, and the
  corresponding localized platform label elsewhere;
- Rename and Delete;
- New File and New Folder where the selected target permits creation.

Destructive filesystem commands will resolve and validate their targets before
changing the workspace. Delete requires an explicit confirmation interaction
and should use a recoverable platform operation when practical.

### Editor tabs

- Pin or Unpin;
- Close, Close Others, Close to the Right, and Close All;
- Copy Path;
- Reveal in Explorer.

These commands operate on the tab that opened the context menu, not merely the
currently selected tab. Dirty-resource handling continues to use the shared
working-copy and close-guard modules.

### Text editors

- Undo and Redo;
- Cut, Copy, Paste, and Select All;
- language commands contributed when supported, initially Go to Definition,
  Find References, Rename Symbol, Format Document, and Quick Fix.

The text editor supplies document, cursor, range, and language context. A
language adapter, such as Java LSP, decides which language commands are
available and performs their semantic work.

## Command palette

The command palette will be a generic searchable presentation of explicitly
placed editor commands. It will not implement operations or contain knowledge
of Java, LSP, project formats, or JavaFX-specific command behavior.

Extensions place suitable registered commands in a command-palette location.
The palette invokes the same stable command IDs used by menus, context menus,
toolbars, and keybindings. Internal commands are not exposed merely because they
are registered.

Search will consider the localized title, category, contributed keywords, and
stable command ID. Categories permit presentation such as `Java: Restart Java
Language Server` without requiring every command implementation to construct
that prefix. Context conditions and command state determine whether a result is
hidden, disabled with an explanation, or available.

The initial palette should support keyboard navigation, incremental filtering,
empty and no-results states, and invocation of the selected command. Long-running
commands report progress through editor facilities rather than freezing or
keeping the palette open.

The conventional Command/Control+Shift+P shortcut is expected, but it should use
the same platform-neutral keybinding direction described below. Ranking, recent
commands, aliases, argument collection, and extension metadata are deferred
until implementation.

The palette itself is deferred until the first Java LSP integration is working.
This lets the LSP slice validate its lifecycle and commands before introducing a
new global command surface.

## Keybinding direction

Keybindings invoke stable command IDs; they do not point directly at Java
handlers. Their representation must be toolkit-independent and support a
platform-neutral primary modifier so the same declaration can mean Command on
macOS and Control on Windows or Linux.

Built-in and extension defaults must be distinguishable from user overrides.
User choices take precedence over defaults. Conflicting active bindings should
produce an actionable diagnostic rather than invoke an arbitrary command.
Context conditions determine when a binding applies so, for example, a text
editing shortcut does not activate in the Hierarchy.

The following details are intentionally deferred:

- chord and modifier syntax;
- conflict resolution between built-in and extension defaults;
- storage and editing of user overrides;
- the public extension contribution interface;
- keyboard-layout and accessibility behavior; and
- the initial default shortcut set.

These decisions should be made with the settings module and native JavaFX event
adapter in view when keybindings are implemented.

## Ownership and localization

Command IDs and semantic behavior belong to the module which owns the
operation. Editor surfaces contribute placements and contextual arguments.
JavaFX owns native controls and input-event adaptation.

All user-visible command titles, confirmations, and errors resolve through the
shared `MessageSource` interface. Platform-specific behavior uses the shared
`OperatingSystem` value; operating-system strings and localized labels must not
be embedded in command implementations.

## Deferred implementation

The current Explorer context invocation is a first vertical slice. The command
palette, broader catalogue, editor and tab context locations, keybinding
registry, customization, and language-aware commands remain deferred. Their
exact interfaces will be finalized when each slice is implemented, using the
principles in this document as constraints rather than treating the lists above
as a frozen specification.
