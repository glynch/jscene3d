# Editor fundamentals

The [project and game fundamentals](project-fundamentals.md) explain the
authored project model on which the JScene3D Editor operates. The editor lets
you inspect that model, diagnose it, and edit supported authored properties
without turning the project into a running game.

## One model, two responsibilities

```text
Code OSS JScene3D frontend
        ↓ authoring protocol
Java authoring service
        ↓
EditorProjectSession / project model
```

Code OSS supplies the workbench: project commands, views, editor tabs,
selection, native Problems integration, and standard document actions. The
persistent Java authoring service owns project discovery, loading, validation,
definition identity, hierarchy and Inspector projections, and mutations.

This boundary keeps one authoritative interpretation of JScene3D content. The
frontend presents Java-owned state and sends semantic authoring requests; it
does not reproduce project rules in TypeScript.

## Main editor surfaces

- **JScene3D Project** summarizes the open project, including its identity,
  descriptor and root paths, optional Main Scene, and authored and projected
  asset counts.
- **Hierarchy** shows the entities and placements in the active definition.
  Selecting an occurrence establishes the current authoring selection.
- **Inspector** appears in the Secondary Side Bar and projects the selected
  entity's components and properties. Supported property editors submit
  candidate values to Java for authoritative validation and mutation.
- **Problems** contains structured project and definition diagnostics produced
  by Java. Diagnostics retain their source file, severity, code, message, and,
  where available, JSON location. The **JScene3D** Output channel records
  service and workflow details rather than authoring problems.
- A **JScene3D Authored Definition** editor opens `*.scene.json` and
  `*.entity.json` files. Its tab anchors the definition's document lifecycle
  and coordinates the Hierarchy and Inspector. The editor body is currently a
  concise definition summary, not a general JSON editor or graphical canvas.

The views are related rather than independent: opening a definition supplies
the Hierarchy, a Hierarchy selection supplies the Inspector, and Java
diagnostics appear through the workbench's native Problems surface.

## The safe authoring boundary

An editor project session loads the information needed to understand a project:

- project metadata and settings from the `.j3d` descriptor;
- inert extension descriptors describing registered types and components;
- authored Scene and entity definitions;
- import definitions and already-published generated content.

It does not load runtime extensions, execute arbitrary game implementation
code, or construct and activate a live `World`. This is the practical value of
the descriptor/runtime distinction introduced in
[project and game fundamentals](project-fundamentals.md): the editor can
understand a title-specific component through its descriptor without loading
the Java class that implements that component during play.

## Authored and generated definitions

Authored definitions are source-backed and can have editable working copies.
Generated definitions come from imported or generated project content and are
read-only in the authoring session.

Source-preserving editing means that changing an understood property patches
the retained JSON document. It does not rebuild the file from a smaller domain
projection, so unrelated members and extension-owned content are not silently
discarded. Serialization preserves semantic object and array content and exact
decimal values, although whitespace and numeric spelling can be normalized.

The current Inspector exposes editing only for the semantic property editors
it implements. Other projected values remain read-only; the presence of a
value in the Inspector does not by itself imply that the frontend can edit it.

## The authoring lifecycle

Each open authored definition has a Java-owned working copy and a monotonically
advancing revision. A successful edit validates against the current revision,
produces a new revision, and updates the document's dirty state. Invalid or
stale edits are rejected without changing the working copy.

The custom editor connects that model to Code OSS document behavior:

1. **Working copy** holds the current source-preserving authored document.
2. **Validation** checks proposed semantic changes before accepting them.
3. **Revision** identifies the state against which a request was made.
4. **Dirty state** records whether the current content differs from its saved
   baseline.
5. **Undo and redo** apply Java-owned semantic history through the native
   editor actions.
6. **Save** writes the validated working copy using conflict-aware, atomic
   persistence.
7. **Revert** reloads the persisted source and resets the working-copy history.
8. **Backup and restore** integrate with workbench recovery so an unsaved
   working copy can survive editor restoration.

Save, revert, undo, redo, dirty-close prompting, and backup recovery are wired
through the authored-definition custom editor. Save As is not supported, and
the available editing controls remain limited to the property editors exposed
by the current Inspector.

## Native viewport boundary

JScene3D's native viewport path is designed around the real engine renderer,
not a browser reimplementation of the scene:

```text
JScene3D / LWJGL / OpenGL
        ↓
native shared rendering surface
        ↓
Code OSS / Electron viewport
```

The Java rendering layer provides the native shared-surface support needed for
this path. The current Code OSS extension does not yet contribute or mount a
visible native viewport, however. Today, an authored-definition tab provides
the document lifecycle and drives the Hierarchy and Inspector; it should not be
mistaken for a rendered game view. The low-level native sharing mechanism is an
integration concern rather than part of the authoring model described here.

## Further reading

The [entity-component world architecture](../design/entity-component-world-architecture.md)
defines the complete project and runtime model.
