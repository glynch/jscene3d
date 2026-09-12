# Model editable content as resource working copies

Status: accepted on 2026-09-12.

## Context

The first editable inspector property introduced dirty state, undo/redo, and saving
inside `EditorProjectSession`. That was sufficient for one world property, but it
would not scale to project settings, Java source files, newly created files, or
multiple open worlds. A project workspace is context for many resources; it is not
itself one document with one saved baseline.

The editor also needs precise change notifications. Generic `Runnable` observers do
not communicate which resource or lifecycle transition changed, and a single global
observable would couple unrelated editor services.

## Decision

Editable content is represented by an `EditorWorkingCopy` identified by an
`EditorWorkingCopyId`: a resource URI plus a stable working-copy type. Each working
copy owns its current content, saved baseline, dirty state, save/revert behavior, and
typed content, dirty, and save events.

An `EditorWorkingCopyRegistry` owns registrations and derives aggregate workspace
state such as whether anything is dirty, the dirty-resource count, and the ordered
set of dirty working copies. It forwards resource events without becoming the owner
of resource content or persistence.

The authored startup world is the first working-copy implementation.
`EditorProjectSession` coordinates project services and projects that world into
the hierarchy; it no longer owns the world's mutable revision or saved baseline.
Undo/redo entries carry the affected working-copy identity as well as a user-facing
label. This provides the resource seam required for a later workspace-level
undo/redo service.

Events use the typed `EditorEvent<T>` contract and removable
`EditorRegistration` subscriptions. Publishers keep their emit operation private
to the implementation that owns the state. There is no global editor event bus.

Dirty presentation follows the same scopes:

- the startup-world editor tab shows a prominent solid dot when that resource is
  dirty;
- the edited hierarchy item shows its solid modified marker immediately beside its
  label;
- the project name becomes visually stronger and gains `*` when any registered
  working copy is dirty.

Project configuration, semantic file creation/deletion/rename, external file-system
watching, workspace edits, command enablement, and the future cross-resource
undo/redo service remain separate modules. They will communicate through typed
resource events and working-copy registration instead of accumulating in
`EditorProjectSession`.

## Consequences

Settings documents, Java sources, worlds, and other editable resource types can add
working-copy implementations without changing the aggregate dirty-state model.
Multiple working-copy types may safely address the same URI. Views can subscribe to
the narrow lifecycle they need and can dispose subscriptions explicitly.

The current save command still saves the startup world because that is the only
editable resource. When more resource types become editable, save-one and save-all
commands will operate through the registry rather than adding more state to the
project session.

Resource identities must be canonical and stable. Implementations are responsible
for firing content events after content changes, dirty events only when the dirty
value changes, and save events only after successful persistence.
