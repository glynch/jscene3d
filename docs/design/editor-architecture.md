# JScene3D Editor architecture

The JScene3D Editor combines a Code OSS workbench with Java-owned authoring and
rendering systems. This document describes the boundaries that keep those parts
coherent. For the learning-oriented view, see
[Editor fundamentals](../manual/editor-fundamentals.md). For the data and
runtime model on which the editor operates, see
[Project and game fundamentals](../manual/project-fundamentals.md).

## Code OSS as the workbench

The original custom JavaFX editor has been retired. Code OSS now supplies a
mature editor workbench, including windows, views, tabs, commands, diagnostics,
workspace integration, document lifecycle, and extension infrastructure. This
avoids maintaining another general-purpose IDE shell.

The pivot did not move JScene3D semantics into the browser. Project loading,
authoring rules, and the engine renderer remain Java responsibilities. Code OSS
provides the product shell and interaction layer around those capabilities.
The superseded [JavaFX editor ADR](../adr/0027-use-javafx-for-the-visual-editor.md)
is retained only as the record of the former decision.

## Authoring architecture

```text
Code OSS JScene3D frontend
        ↓ Content-Length-framed authoring protocol
Java authoring service
        ↓
editor-authoring / project model
```

The JScene3D extension supervises one persistent, headless Java authoring
service for its lifetime. Requests, responses, and notifications are JSON
messages carried over standard input and output with byte-accurate UTF-8
`Content-Length` framing. This protocol is a process boundary: it carries
structured authoring intent and projections, not Java objects or renderer
frames.

Code OSS owns workbench concerns:

- commands and project-open interactions;
- the Project, Hierarchy, Inspector, Problems, and custom-editor presentation;
- active selection and view coordination;
- native custom-document dirty, undo/redo, save, revert, close, and recovery
  integration; and
- authoring-service process supervision and operational output.

Java owns JScene3D authoring semantics:

- descriptor discovery and project interpretation;
- authored and published definition loading;
- extension type and property metadata;
- hierarchy and Inspector projections;
- validation and semantic mutation;
- working copies, revisions, history, persistence, and recovery; and
- authoritative diagnostics and project/definition identity.

The frontend therefore does not parse a definition into a competing mutable
domain model. It displays Java projections and submits semantic operations
against explicit identities and revisions.

### Safe project interpretation

The authoring service assembles an `EditorProjectSession` from the `.j3d`
descriptor, project settings, authored assets, extension descriptors, import
definitions, and already-published generated content. Extension descriptors
describe stable types, properties, constraints, capabilities, and editor
metadata without requiring the service to instantiate the corresponding game
runtime implementations.

The service does not load runtime extensions, invoke import providers, or
construct and activate a live `World`. Import definitions are read
structurally, and only their previously published definitions are projected
into the editor. Generated definitions are immutable in the authoring session;
only source-backed authored definitions can acquire editable working copies.

This boundary is the editor application of the descriptor/runtime separation
defined by the
[entity-component world architecture](entity-component-world-architecture.md).

## Authored-document lifecycle

An editable definition begins as a source-preserving authored document. Java
retains its trusted source identity, validated JSON tree, immutable domain
projection, and persisted-source fingerprint as one coherent state. A semantic
mutation patches a copy of the retained tree and becomes current only after the
normal definition and component validation path accepts the complete result.
The file is not reconstructed from a reduced projection, so unrelated and
extension-owned JSON content remains represented.

The lifecycle contract is:

```text
persisted authored document
        ↓ open
working copy + revision + history
        ↓ semantic mutation
validation
        ↓ accepted
new revision + derived dirty state
        ↓
undo/redo | save | revert | backup/restore
```

The important invariants are:

- A request names one authored definition and the revision it expects.
- Validation is authoritative in Java and failure leaves the working copy
  unchanged.
- Accepted content changes and history transitions advance revision
  monotonically; stale requests cannot overwrite newer state.
- Dirty state is derived by comparing the current authored tree with the
  persisted semantic baseline rather than trusted from the frontend.
- Undo and redo restore accepted semantic states while preserving the same
  source identity.
- Save verifies that the physical source still matches the retained baseline,
  writes atomically, and establishes a new persisted baseline without
  discarding valid history.
- Revert reloads and validates physical source, then abandons the superseded
  edit history as one authoritative transition.
- Backup captures recovery state without changing content, revision, history,
  or disk. Restore validates project, asset, source-baseline, and content
  identity before changing a newly opened working copy.

The Code OSS custom editor bridges those operations into the native document
lifecycle. A successful Inspector mutation becomes a native custom-document
edit whose undo and redo callbacks return to Java. Save, revert, dirty-close
prompting, and backup recovery likewise delegate to Java rather than maintaining
parallel frontend state. The architectural contract remains the required
behavior even when a defect in one integration path temporarily violates it.

## Identity and session boundaries

An authored-definition editor resource combines three identities:

- **Java connection generation** identifies one authoring-service process and
  protocol connection.
- **Project generation** identifies one accepted open-project session within
  that connection.
- **`AssetId`** identifies the definition within the project's semantic asset
  space.

The connection generation is newly created when the Java service starts. The
project generation changes when the active project session is replaced. The
`AssetId` remains the content identity used by Java operations. Together they
prevent a resource URI or open tab from accidentally changing meaning when a
service restarts, a project is replaced, or the same project is reopened.

Frontend definition mappings are cleared when project generation changes, and
the connection generation is embedded in each editor resource. A stale tab is
therefore shown as belonging to an inactive session rather than silently bound
to a new working copy with a coincidentally matching path or `AssetId`.
Recovery is a distinct, explicit path: a workbench backup may be restored only
after the definition is opened in the current project session and Java accepts
the backup against that session's identity and persisted source baseline.

## Renderer integration

Rendering is a separate privileged boundary from the authoring protocol. Its
purpose is to present output from the real JScene3D Java renderer without
reading every framebuffer back through the CPU and without implementing a
second renderer in browser JavaScript.

### Standalone Electron/LWJGL proof

The first standalone proof established the basic macOS transport. Electron
allocated a fixed BGRA `IOSurface`, imported it through Electron's shared-texture
API, and launched a separate Java/LWJGL process. Java looked up the same surface,
bound it to an OpenGL texture and framebuffer, and rendered a cube into it. The
Electron renderer received a `VideoFrame` from the imported shared texture and
presented it in a canvas. That proof established cross-process GPU-backed
surface sharing and input routing, but its singleton surface, fixed dimensions,
and line-oriented demo control were not a product architecture.

### Code OSS rendering proof

The later Code OSS proof established that the same approach could operate
inside the Code OSS/Electron environment. A privileged built-in editor pane was
paired with a custom Electron browser-process binding. The binding managed
concurrent renderer sessions, supervised Java children, allocated and resized
surfaces, and exposed Electron-compatible shared-texture handles.

The durable Java side is `IOSurfaceRenderSurface`, an implementation of the
renderer's host-owned `RenderSurface` contract. The proof host creates the real
JScene3D `Renderer`, `Scene`, camera, geometry, and material rather than a
parallel browser scene. Surface replacement preserves the renderer session
while updating the framebuffer and camera dimensions.

On macOS, the proven presentation path is:

```text
JScene3D
   ↓
LWJGL / OpenGL
   ↓
IOSurface-backed RenderSurface
   ↓
Electron / Chromium native integration
   ↓
imported shared texture / VideoFrame
   ↓
WebGPU external-texture presentation
```

Electron owns IOSurface allocation, renderer-session supervision, and the
browser-compatible import handle. A Mach rendezvous transfers the initial
surface right to Java; a separate Mach control channel transfers replacement
surfaces during resize. Java owns its acquired surface references, the OpenGL
texture/framebuffer adapters, and the renderer's exclusive access. After Java
signals a completed frame, Chromium imports the same GPU-backed surface,
obtains a `VideoFrame`, and WebGPU samples it as an external texture. Pixel
buffers are not copied through a JavaScript or Java CPU readback loop.

The proof is macOS-specific. IOSurface and Mach details remain confined to the
platform adapter and privileged Electron boundary rather than leaking into the
portable project or authoring models. The renderer continues to follow its
[exclusive OpenGL ownership decision](../adr/0017-give-the-renderer-exclusive-opengl-state-ownership.md).

## Current integration status

The native rendering architecture is proven by the standalone Electron/LWJGL
and Code OSS rendering POCs. The Java IOSurface adapter and the Electron
renderer-session implementation remain source evidence for that path.

The active JScene3D Code OSS extension does **not** currently contribute or
mount the native viewport. Its authored-definition custom editor is a
script-free definition summary and document-lifecycle anchor that coordinates
the Hierarchy and Inspector. The active editor UI must not be described as
already presenting renderer frames merely because the separate rendering
architecture has been proven.

Authoring and rendering consequently remain distinct integration planes:

```text
authoring control plane: Code OSS extension ↔ Java authoring service
rendering data plane:    Java renderer → IOSurface → Electron/Chromium
```

Joining those planes into a user-facing viewport must preserve their current
ownership rules; the rendering proof does not move project semantics into
Electron, and the authoring service does not become a renderer host.

## Related decisions

- [Use hierarchical entity-component worlds](../adr/0026-use-hierarchical-entity-component-worlds.md)
  defines the authored and runtime model projected by the editor.
- [Model editable content as resource working copies](../adr/0030-model-editable-content-as-resource-working-copies.md)
  records the resource-scoped working-copy principle behind editor document
  state.
- [Give the renderer exclusive OpenGL state ownership](../adr/0017-give-the-renderer-exclusive-opengl-state-ownership.md)
  defines the renderer/surface ownership boundary used by native integration.
- [Use JavaFX for the visual editor](../adr/0027-use-javafx-for-the-visual-editor.md)
  is superseded and retained as historical architectural evidence.
