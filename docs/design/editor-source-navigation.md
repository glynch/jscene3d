# Editor source navigation

Status: direction recorded on 2026-09-13; source-association details are
deferred until implementation.

## Purpose

Developers should be able to move from an authored entity or component in the
Hierarchy or Inspector to the Java source which supplies its behavior. This is
a convenience path into the same source editor and language navigation used by
the Workspace Explorer; it is not a second Java editor.

This document captures the navigation seam and user experience while leaving
the source-association format open until Java LSP and the project extension
model can be considered together.

## JScene3D model

JScene3D does not attach one script to an entity in the Godot sense. An entity
contains component definitions identified by stable `ComponentTypeId` values,
and an entity may contain several behavioral components. Runtime factories map
those component types to ordinary Java implementations.

Authored definitions and `ComponentTypeDescriptor` metadata remain inert.
Serialized project files do not contain Java implementation class names and
must not acquire them merely to support editor navigation. Navigation therefore
starts from a semantic authored target, usually a component type or component
instance, rather than treating the entity record as a Java class reference.

## Navigation flow

The intended flow is:

```text
Hierarchy or Inspector target
        -> source-reference provider
        -> language-neutral source target
        -> language/navigation adapter
        -> existing editor tab at a file, symbol, or range
```

The source-reference provider is the seam between the authored model and source
tooling. It accepts semantic project targets and returns zero or more source
targets. Callers do not search source roots, inspect implementation classes, or
understand Java LSP.

A source target should be capable of describing a resource URI and, when known,
a symbol or source range. The exact type, cardinality, error model, and extension
contribution interface will be finalized during implementation.

The language/navigation adapter resolves and opens the target. A simple initial
Java adapter may locate a source beneath configured source roots. Java LSP should
later become authoritative for symbol resolution, definitions, generated
sources, and dependency sources where it has that information.

## User experience

When one source target is available, **Open Source** opens it directly. When
several behavioral components or source targets are available, the interaction
offers a short labelled list rather than choosing arbitrarily. When no source is
known, the action is absent or disabled with an explanation; the exact
presentation can be decided with the context-menu implementation.

The action should be available from:

- an authored entity's Hierarchy context menu;
- a component section or code affordance in the Inspector; and
- a future command-palette entry when the current selection supplies a valid
  semantic target.

Opening source uses the normal preview/pinned editor behavior. A single click or
ordinary navigation may use preview, while an explicit open or repeated
interaction can pin according to the editor's existing file-opening policy.

## Responsibilities

The Hierarchy and Inspector identify the semantic target and invoke a command.
They do not know how a component type maps to Java.

The source-reference module owns source associations and resolution order behind
one interface. This provides locality for source mapping and lets future
language or extension adapters participate without changing the Hierarchy.

The editor file module owns tab identity, preview and pinning, dirty state, and
resource opening. The language adapter owns symbol-aware navigation inside the
opened resource. Java LSP owns Java semantic answers once available.

## Open design decisions

The following decisions are deliberately not fixed by this document:

- how an extension associates a `ComponentTypeId` with source without placing
  an implementation class name in authored project data;
- whether the first association is contributed metadata, generated build
  metadata, runtime-factory metadata exposed safely to tooling, or a composition
  of those sources;
- the exact language-neutral source-target interface;
- how multiple implementation, test, generated, or dependency targets rank;
- how stale or unavailable associations are diagnosed;
- whether an entity-level action lists every source-backed component or only
  components classified as behavior; and
- which source-root information is provided by the build adapter before LSP is
  ready.

The preferred starting candidate is an editor/tooling contribution keyed by
`ComponentTypeId`, because it preserves inert project descriptors and keeps Java
implementation details out of serialized entity data. That candidate must be
validated against the Java extension loading and LSP designs before it becomes
an accepted contract.

## Relationship to Java LSP

Entity-to-source navigation and LSP share the final navigation adapter but solve
different lookup problems. The source-reference provider answers which source
is associated with an authored component. LSP answers where a Java symbol is
defined and supplies completion, references, rename, diagnostics, and other
language features. The language-server architecture and delivery sequence are
recorded in
[Editor Language-server Support](editor-language-server-support.md).

The first source-association design should therefore avoid duplicating a Java
index. Java LSP remains the next major editor capability.
