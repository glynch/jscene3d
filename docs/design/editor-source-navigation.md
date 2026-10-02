# Editor source navigation

Source navigation connects JScene3D's semantic project model to the native Code
OSS editor. It lets a developer move from an entity or component in the
Hierarchy or Inspector to the source associated with that behavior. It does not
create a second text editor or a second Java language service.

For the wider frontend/service boundary, see
[Editor architecture](editor-architecture.md).

## Current foundations

JScene3D does not attach one script to an entity. An entity contains component
instances identified by `ComponentId`; each instance has an exact, versioned
`ComponentType`. Runtime extensions map those types to ordinary Java
implementations.

The editor already carries the stable identities needed to ask a future source
navigation operation an unambiguous question:

- `HierarchyOccurrenceId` identifies one occurrence by its containing
  definition `AssetId` and authored entity/placement path.
- `InspectorTarget` combines the authoritative definition source URI, domain
  identity, and optional hierarchy occurrence.
- Inspector component groups expose the exact component-type identifier and
  configuration version obtained from Java descriptor metadata.
- Definition and hierarchy protocol projections retain those Java-owned
  identities, while the Code OSS frontend owns the active selection.

Those identities are scoped by the active Java connection and project
generation. A selection from a replaced project or restarted service must not
be rebound to a coincidentally matching file or component in the new session.

`ComponentTypeDescriptor` and `ExtensionDescriptor` are safe, inert metadata;
they do not currently expose an implementation-source association. The
authoring protocol likewise has no source-navigation request, and the Code OSS
extension does not currently contribute an **Open Source** command. Source
navigation is therefore a design contract built on implemented identity and
selection foundations, not a claim that the user-facing action already exists.

## Source associations

An implementation-source association belongs with the safe component and
extension metadata that identifies the behavior. It must not be written into an
authored Scene or entity as an implementation class name. Keeping the
association outside project content preserves the descriptor/runtime boundary
and allows the authoring service to resolve navigation without loading arbitrary
game runtime implementations.

The association must be keyed by exact semantic identity, normally the
versioned `ComponentType` and its owning extension. Its projected navigation
target should be language-neutral and stable. Depending on what the extension
can publish, a target may identify:

- a source URI and optional range;
- a source URI and symbol; or
- a Java symbol which installed Code OSS Java tooling can resolve to project,
  generated, engine, extension, or dependency source.

The final metadata shape remains an implementation decision. Whatever shape is
chosen must be safe to load alongside the existing descriptors, deterministic,
and useful without constructing a runtime extension. Display labels and origin
information should be retained so Code OSS can distinguish project-specific
source from engine or extension source when several targets are available.

## Navigation boundary

The architectural flow is:

```text
Hierarchy / Inspector selection
        ↓
stable Java-owned semantic identity
        ↓ authoring protocol
Code OSS frontend
        ↓
native Code OSS editor and Java navigation
```

Java owns the JScene3D-specific part of the lookup. Given the expected project
generation and the selected hierarchy or Inspector identity, the authoring
service resolves the applicable component descriptors and their source
associations. An Inspector component section can name its exact component type
directly. An entity-level Hierarchy action may resolve several component-backed
targets and return all of them rather than choosing one arbitrarily.

The protocol should return zero, one, or several structured source targets. It
must not transfer Java objects, executable factories, or filesystem-search
instructions to the frontend. Project-relative locations must use the same
canonical path and project-boundary checks as other Java-owned project paths.

Code OSS owns presentation and navigation. It invokes the authoring operation
for the current selection, offers a labelled picker when several targets are
returned, and opens the selected result through native Code OSS URI, text
document, editor, and reveal facilities. Preview, pinning, tab identity, dirty
state, and focus remain workbench behavior.

## Java language tooling

The JScene3D product includes the Red Hat Java extension and the usual Code OSS
Java tooling as required extensions. That environment owns Java workspace
discovery, symbol definitions, dependency and generated-source lookup, and the
normal Java editor experience.

The JScene3D authoring service is not a language server. It should identify the
semantic source target when JScene3D knowledge is required, then let Code OSS
and its registered Java providers perform symbol-aware navigation. There is no
embedded Monaco editor, custom JDT integration, or JScene3D-specific Java index
in this design.

For project-specific source already represented by a workspace URI, Code OSS
can open the resource directly. For engine or extension code, the association
may instead lead through Java symbol navigation so the installed tooling can
select an attached source, generated source, or dependency view that it knows
how to provide.

## User experience

The source action belongs in both authoring surfaces:

- a Hierarchy entity action resolves the source-backed components attached to
  that semantic occurrence; and
- an Inspector component action resolves the source for that exact component
  type.

One resolved target opens immediately. Several targets produce a short picker
whose labels identify the component and source origin. Opening uses normal Code
OSS preview and pinned-editor behavior.

Unavailable navigation is non-destructive:

- no association means the action is absent or disabled with a concise reason;
- a stale project generation or selection is rejected rather than rebound;
- a missing project file or unavailable extension/dependency source is reported
  without changing the selection; and
- Java and Code OSS do not guess by scanning for similar filenames or class
  names.

An unavailable source is not a project-validation failure. It is a navigation
result which the frontend can explain through its normal notification or output
surface. Project and authored-definition diagnostics remain reserved for
problems in the project content itself.

## Responsibility summary

Java owns:

- stable JScene3D identity and generation checks;
- safe component/extension source associations;
- association resolution and deterministic target ordering; and
- project-path validation where a target refers to project source.

The Code OSS frontend owns:

- Hierarchy and Inspector commands and selection context;
- target choice and unavailable-source presentation;
- native resource opening, ranges, preview, pinning, and focus; and
- delegation of Java symbol navigation to installed Code OSS Java tooling.

This separation keeps source navigation consistent with the rest of the
editor: Java remains authoritative for JScene3D semantics, while Code OSS
remains authoritative for workbench and language-tool interaction.
