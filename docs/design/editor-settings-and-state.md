# Editor Settings and Workspace State

Status: accepted direction recorded on 2026-09-14; implementation is deferred.

## Purpose

The editor persists several kinds of information with different ownership,
portability, and lifetime rules. Treating all of it as settings would mix
source-controlled project behavior with one developer's preferences and with
incidental UI restoration data.

This document defines that classification, its storage ownership, restoration
behavior, and the seams a later implementation should introduce. It does not
implement persistence or finalize the JSON schemas and platform paths.

## State classification

| Kind | Owner and scope | Examples | Portable | User editable |
| --- | --- | --- | --- | --- |
| Project setting | Project, shared by collaborators | asset roots, extension configuration, project exclusions | Yes | Yes |
| User setting | Developer, across workspaces | color theme, editor font, default keybindings | No | Yes |
| Workspace preference | Developer, for one workspace | Build Automatically | No | Yes |
| Workspace state | Editor, for one developer and workspace | open tabs, panel visibility and sizes, selected views | No | Normally no |
| Session state | One running editor process | active build, language-server process, transient focus | No | No |
| Recovery data | Developer, for failure recovery | unsaved working-copy contents | No | Through recovery workflow |

These kinds must not share a document merely because they are all serializable.
Their lifetimes, merge behavior, privacy requirements, and failure handling are
different.

## Project settings

Project settings remain portable authored configuration in
`<project>/.jscene3d/settings.json`. They are part of the project, can be
reviewed in source control, and must not contain machine-specific paths or UI
layout state.

Project settings use declarative `SettingDefinition` contributions and the
generated settings experience described in
[Editor Settings Experience](editor-settings-experience.md). Unknown extension
keys remain preserved when their contributor is temporarily unavailable.

## User settings

User settings express intentional choices which apply across workspaces. The
selected color theme, preferred dark and light themes, editor font family, and
editor font size belong here. Choosing dark or light mode therefore changes a
User setting; opening another project must not unexpectedly select a different
theme.

User settings will use `settings.json` in the platform-appropriate application
configuration directory. Existing Java Preferences values are transitional
storage and must be migrated or used as a one-time fallback when this document
is introduced.

User settings use the same declarative setting definitions, validation,
diagnostics, generated controls, and direct JSON editing as Project settings.
They do not use the workspace-state store.

## Workspace preferences

A Workspace preference is an intentional developer choice which applies only
while working with one project. **Build Automatically** is the first example:
two developers may choose differently, and one developer may choose differently
for two projects, without modifying either project repository.

Workspace preferences are settings, not restoration state. They should
eventually be exposed through the same searchable settings experience when a
setting declares that scope. A per-workspace value overrides the corresponding
User setting when a definition supports both scopes. Definitions which are
inherently global, such as the selected color theme, do not accept a Workspace
override.

The current Java Preferences build value is transitional storage. Migration to
the common settings module must preserve it.

## Workspace state

Workspace state is private, automatically captured information used to restore
the workbench to a useful shape. It includes, where supported:

- open editor identities and their order;
- the selected editor;
- preview and pinned-tab state;
- editor groups and split proportions;
- Primary Side Bar, Secondary Side Bar, and Panel visibility and sizes;
- the active view within each workbench region;
- Explorer expansion and selection state;
- editor cursor, selection, and scroll positions; and
- window bounds and maximized or full-screen state.

Restoration state is descriptive, not authoritative. Missing files, unavailable
extensions, removed views, changed monitors, and newer schema versions must not
prevent a workspace from opening. Invalid entries are ignored with diagnostic
logging, and window bounds are clamped to an available display.

Workspace state must not include active processes, stale diagnostics, or
unsaved contents. Open documents may be restored, but their saved contents are
read from the resources themselves.

## Session state and recovery data

Active builds, JDT LS connections, progress notifications, menus, dialogs,
hover state, and current keyboard focus are Session state. They are recreated
or recomputed and are never restored as though the previous process were still
alive.

Unsaved working copies are not ordinary Workspace state. A later recovery
feature may save them in a separate recovery area with explicit document
versions and user-visible recovery or discard behavior. Restoring a tab must
never silently replace its resource with an old unsaved buffer.

Diagnostics derived from a language server or build are recomputed. The editor
may retain output logs separately for troubleshooting, but retained output does
not become the current diagnostic publication.

## Storage ownership

Platform path selection belongs to `ApplicationDirectories` in `jscene3d-core`.
That module currently resolves cache locations; the persistence implementation
should add explicit configuration and durable state locations rather than
placing important state in an evictable cache directory.

The intended logical layout is:

```text
<application configuration>/
  settings.json

<application state>/
  workspaces/
    <workspace identity>/
      state.json
  recovery/
    <workspace identity>/
      ...
```

Exact native directories and filenames will be finalized with the
`ApplicationDirectories` extension. Callers do not assemble platform paths.
The application-state directory is private to the current operating-system
user and is never written inside the project merely for convenience.

One workspace-identity module owns normalization and lookup. The initial
implementation may derive an opaque identity from the normalized absolute
project path, as the automatic-build preference currently does. The design must
allow that strategy to be replaced if projects later receive stable identities
which survive moves. Other modules must not independently hash paths.

## Persistence seams

The shared settings module remains the single interface for declared settings,
effective-value resolution, editing, validation, persistence, and change
events. It hides the User, Workspace, and Project storage adapters from callers.

A separate deep workspace-state module owns:

- workspace identity;
- a versioned snapshot schema;
- namespaced state supplied by workbench regions, editor providers, and
  extensions;
- debounced and atomic writes;
- tolerant reads and schema migration; and
- lifecycle ordering during restoration and shutdown.

Workbench code contributes and consumes meaningful snapshots through that
small interface. Individual panes, tabs, and extensions do not choose files,
parse the root state document, or schedule their own writes. Extension state is
namespaced by stable extension identity so contributors cannot collide.

The settings and workspace-state modules are separate even if both initially
use JSON. A storage adapter is an implementation detail, not a reason to merge
their interfaces.

## Loading and saving lifecycle

User settings load before the first workbench is shown so the saved theme can be
applied without a light-theme flash. Failure falls back to declared defaults
and reports the problem without preventing startup.

Workspace preferences load after the project identity is known and before
dependent commands publish their initial state. Workspace UI restoration begins
after extension discovery and editor-provider registration so saved views and
editors can be resolved. The workbench becomes usable even if some entries
cannot be restored.

State changes update an in-memory snapshot and schedule a debounced atomic
write. An orderly close attempts a final flush without indefinitely blocking
shutdown. Each persisted root contains an explicit schema version; migrations
are centralized and covered by fixture-based tests.

## Initial restoration slice

The first implementation should restore only high-value stable state:

1. window bounds and maximized state;
2. workbench-region visibility and split positions;
3. active Activity Bar and Panel views;
4. open editors, order, preview or pinned state, and selected editor; and
5. Explorer expansion state.

Cursor and scroll restoration, multiple editor groups, extension-contributed
state, output retention, and crash recovery can follow after this slice proves
the persistence and migration seams.

## Verification expectations

Tests should exercise the same interfaces used by the workbench and cover:

- separation of User, Workspace, and Project values;
- stable workspace lookup and absence of raw paths in directory names;
- atomic replacement and recovery from malformed or partially written state;
- migration from every previously shipped schema version and transitional Java
  Preferences values;
- tolerant restoration when resources or contributions no longer exist;
- deterministic save and restore of tab order, selection, and layout; and
- fallback behavior when platform directories are unavailable.

Manual verification must include relaunching the same project, opening a second
project with different workspace state, moving the application between displays,
and confirming that no local UI state modifies the project working tree.

## Deferred decisions

The implementation step will finalize:

- exact platform-native configuration and state directories;
- the first JSON schemas and migration policy;
- workspace identity across project moves and symlinks;
- write debounce and shutdown time limits;
- whether window state is workspace-specific or shared when multiple editor
  windows are supported; and
- retention and pruning of state for projects which no longer exist.

These decisions do not change the accepted classification and ownership rules
above.
