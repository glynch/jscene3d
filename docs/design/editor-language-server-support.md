# Editor language-server support

Status: planned on 2026-09-13. The architectural direction is accepted; exact
interfaces and lower-level types remain subject to implementation review.

## Purpose

JScene3D needs genuine IDE behavior for Java while retaining a language-neutral
editor architecture. Java is supported out of the box. Other languages can be
added later by installed editor extensions without placing language-specific
logic in the workbench or Monaco host.

This document defines the seams, ownership, lifecycle, and delivery sequence for
Language Server Protocol support. It deliberately does not freeze every class,
setting, or extension contribution before the first implementation exists.

## Accepted direction

The initial Java implementation will:

- bundle Eclipse JDT Language Server with the editor;
- run JDT LS as a child process rather than embedding its implementation;
- communicate over standard input and output using LSP4J in Java;
- maintain one language-server session per open project, not one per editor tab;
- start applicable Java support in the background without holding the splash
  screen open;
- synchronize open Java working copies incrementally;
- keep the resource working copy authoritative for unsaved content;
- route every server-requested edit through editor working-copy and filesystem
  modules rather than writing around them;
- keep LSP4J and JDT-specific types out of the public editor interface; and
- keep Monaco responsible for source presentation and interaction, not process
  or protocol lifecycle.

## Architecture

```text
Monaco source editor
        <-> JavaFX/Monaco language adapter
        <-> language-neutral editor interface
        <-> LSP module using LSP4J
        <-> JDT Language Server child process
        <-> Maven project and Java sources
```

The language-neutral editor interface is the external seam. It exposes editor
concepts such as languages, documents, positions, edits, diagnostics, and
navigation targets. It does not expose JSON-RPC messages or LSP4J objects.

The LSP module is deep: it hides process startup, initialization, negotiated
capabilities, request cancellation, protocol conversion, document versions,
server notifications, shutdown, and failure recovery behind that interface.

The Java support adapter selects JDT LS, supplies Java-specific initialization
and configuration, and contributes Java commands and status. Monaco and the
workbench do not know which Java language server is used.

## Provisional module ownership

The following split is the starting direction. Names may change if the first
implementation exposes a cleaner composition.

### `jscene3d-editor-api`

The existing editor interface will contain only the minimum language-support
contribution seam needed by bundled and installed extensions. Stable language
identities continue to use `EditorLanguageId`.

Public editor types must remain toolkit- and protocol-independent. LSP4J, JDT,
JavaFX, WebKit, and Monaco types do not belong in this module.

### `jscene3d-editor-lsp`

This module will own the reusable LSP client implementation:

- server process and JSON-RPC transport;
- LSP4J client lifecycle;
- capability negotiation and request dispatch;
- cancellation and stale-result handling;
- document synchronization;
- translation between LSP and editor values; and
- protocol-level logging and bounded recovery.

It will not contain JavaFX controls or Java-specific JDT settings.

### `jscene3d-editor-java`

This bundled extension will provide Java out of the box. It will own:

- JDT LS distribution discovery and launch configuration;
- Java-project applicability and initialization;
- Maven-aware Java settings;
- Java status and diagnostic ownership;
- Java language commands; and
- packaging metadata and third-party notices for JDT LS.

The server distribution may require build and packaging support outside the
runtime module. That packaging shape will be finalized when a pinned JDT LS
release is selected.

### `jscene3d-editor`

The desktop editor will retain the JavaFX and Monaco adapters. It renders
language results and turns Monaco interactions into asynchronous calls through
the language interface. It will not own JDT LS launch or protocol behavior.

The current Monaco host must remain small. Language features should be divided
among cohesive bridge and conversion modules rather than accumulating in one
Java class or HTML file.

## Project and process lifecycle

Java support becomes applicable when the open project contains recognized Java
source or build markers. The first implementation targets Maven projects and
ordinary Java source files. Exact detection rules remain open.

Applicable Java support starts after the project session is available and does
not block the editor splash or primary workbench. The status bar reports a small
state model such as Starting, Importing project, Ready, and Error.

JDT LS receives the normalized project root as its workspace folder. Its data
directory must be unique to that project and server version. Generated indexes
and configuration are cache data: they are not source assets, must not be
committed, and remain hidden by the Workspace Explorer's default policy.

The editor consumes protocol output from standard output and server logging from
standard error without allowing either stream to block the child process. It
launches the server directly without an intervening shell.

Project close requests the LSP `shutdown` operation followed by `exit`. A bounded
timeout may then terminate an unresponsive process. Unexpected termination
produces a diagnostic and visible Java status. Any automatic restart policy must
be bounded so a broken server cannot enter an invisible restart loop.

## JDT LS distribution and Java runtimes

Java support is out of the box, so a released editor will contain a pinned,
checksum-verified JDT LS distribution. Developers will not be required to
install a separate `jdtls` command.

The packaged editor can run JDT LS with its included Java 21 runtime. The runtime
used to launch the language server and the JDK used to compile a project are
separate concerns. Later Java settings can describe additional project JDKs
without changing the editor's own runtime.

Packaged server files are immutable. Platform configuration and workspace data
which JDT LS must modify are staged in an editor-managed cache. A release must
include the required JDT LS license and notice material.

The selected JDT LS version, cache location, update policy, project-JDK discovery,
and concurrent-editor behavior remain implementation decisions.

## Document synchronization

The editor working copy is the source of truth for an open document. The
language-server session mirrors that state using versioned notifications:

- `didOpen` sends the current content and initial version;
- `didChange` sends ordered incremental Monaco changes and a new version;
- `didSave` follows a successful working-copy save; and
- `didClose` follows closure of the final editor for that document.

Monaco changes include UTF-16 positions and replacement text. The editor already
uses zero-based UTF-16 positions for diagnostics, so one position convention can
be retained across the seam.

Requests record the document version which produced them. Results which cannot
be applied safely to the current version are discarded or re-requested. Closing
a document or cancelling a Monaco request cancels outstanding LSP4J futures when
the protocol permits it.

File changes outside an open working copy, dynamic watched-file registration,
multi-root workspaces, and external-change reconciliation remain later design
work. Maven build-file saves must eventually notify Java support so the project
model can be refreshed. Project compilation and runnable build freshness belong
to the separate [Editor project builds](editor-project-builds.md) module; the
language-server session is not the editor's project-build coordinator.

## Monaco bridge

Monaco continues to provide lexical highlighting and the source-editing user
interface. Language features are registered using Monaco providers for
completion, hover, definitions, formatting, code actions, and related behavior.

Requests across JavaFX WebKit must be asynchronous. A Monaco provider creates a
request identity and Promise, the Java adapter starts the corresponding language
request, and Java later resolves or rejects that Promise on the JavaFX thread.
Monaco cancellation cancels the matching Java request.

Messages crossing the WebView seam use editor-owned values rather than exposing
raw LSP payloads as a public contract. Feature-specific converters remain small
and independently testable. No request may block the JavaFX application thread.

Cross-file navigation must open the destination through the existing editor-file
module and reveal its source range. Monaco must not create an independent tab or
document lifecycle.

## Diagnostics

JDT LS diagnostics are translated into the existing editor diagnostic model and
published through one Java-owned diagnostic collection. The Diagnostics panel
therefore receives Java errors without a parallel Java-specific problems model.

Open Monaco editors observe the same diagnostic state for their URI and render
markers and squiggles. Diagnostic replacement is version-aware, and closing or
restarting Java support clears stale diagnostics it owns.

Initialization failures, process termination, unsupported server requests, and
invalid configuration are also reported through editor messages, status, or
diagnostics according to whether the problem belongs to a source resource or the
language-server session.

## Server-requested edits

JDT LS can request edits affecting one or many resources. All such edits pass
through an editor-owned edit application module which:

- validates target URIs and expected document versions;
- updates open working copies rather than writing behind them;
- preserves dirty state and normal Save behavior;
- opens or confirms affected resources when the interaction requires it; and
- delegates create, rename, or delete operations to the workspace filesystem
  module.

Unsupported edit kinds fail visibly and atomically. Early slices may reject
multi-resource edits until the shared edit transaction is implemented; they
must not partially apply them.

## Feature sequence

### Slice 1: useful Java vertical slice

- introduce the language seam and LSP lifecycle with tests;
- stage and launch bundled JDT LS;
- initialize one non-blocking Maven project session;
- synchronize open Java documents incrementally;
- publish Java diagnostics to the Diagnostics panel and Monaco markers;
- provide Monaco completion, including the standard completion shortcut;
- expose Java lifecycle state in the status bar; and
- shut down cleanly with the project.

This slice establishes visible IDE value while exercising the complete path from
Monaco through LSP4J to the real Java project.

### Command palette after successful integration

Once Slice 1 is working reliably, the generic command palette described in
[Editor Command Surfaces and Keybindings](editor-command-surfaces-and-keybindings.md)
can expose operational Java commands without adding permanent toolbar buttons or
Java-specific workbench menus.

Candidate Java contributions include:

- `Java: Restart Java Language Server`;
- `Java: Reload Java Project`;
- `Java: Clean Java Language Server Workspace`;
- `Java: Show Java Language Server Logs`;
- `Java: Open Java Settings`;
- `Java: Force Java Compilation`, when supported cleanly by JDT LS; and
- `Java: Import Java Projects into Workspace`, when multi-project workspace
  support exists.

These are examples rather than a frozen initial command set. Each command is
owned by Java support and uses ordinary command registration and contextual
availability. The palette knows only its localized presentation metadata and
stable command ID.

Restart operates on the current project's language-server session. Cleaning the
workspace is a guarded recovery operation: it confirms with the developer,
stops the server, removes only the resolved JDT LS data directory, and starts a
fresh session. Show Logs remains available when initialization has failed.

### Slice 2: navigation and information

- hover and signature help;
- Go to Definition and platform-native modified-click navigation;
- cross-file source opening at a range;
- references and implementations; and
- document symbols suitable for a future Outline view.

### Slice 3: source transformations

- document and selection formatting;
- quick fixes and source actions;
- rename symbol;
- safe multi-resource workspace edits; and
- Maven project refresh after relevant build changes.

### Slice 4: richer Java intelligence

- semantic tokens;
- inlay hints;
- code lenses;
- call and type hierarchy; and
- additional JDT LS capabilities justified by editor workflows.

Exact feature order can change in response to implementation dependencies and
developer feedback. Each slice should remain releasable and use small,
behaviorally coherent commits.

## Testing and verification

The language-neutral interface is the primary test seam. Tests will use a fake
language adapter to verify Monaco-independent orchestration and a controllable
LSP endpoint or child process to verify initialization, synchronization,
cancellation, diagnostics, and shutdown.

Normal unit tests must not download or start JDT LS. A separate integration or
packaging profile will exercise the pinned real distribution against a small
Maven workspace. Native qualification will confirm diagnostics, completion,
startup responsiveness, process cleanup, and packaged runtime discovery.

Failure tests will cover malformed protocol messages, early process exit,
initialization timeout, unsupported requests, stale document versions,
cancellation, and shutdown timeout.

## Extension direction

Java is a bundled extension using the same eventual language contribution seam
intended for installed language extensions. An extension may associate file
languages with a language adapter and contribute commands, configuration,
diagnostics, and status without constructing JavaFX controls.

Installing or executing third-party language servers introduces package trust,
executable provenance, permissions, updates, and sandboxing concerns. Those
belong to the future extension lifecycle design and are not solved by accepting
arbitrary command lines from project files.

## Related behavior and non-goals

[Editor Source Navigation](editor-source-navigation.md) defines navigation from
an authored entity or component to its associated behavior source. That lookup
and LSP symbol navigation share the final editor-navigation adapter but remain
separate responsibilities.

[Editor Command Surfaces and Keybindings](editor-command-surfaces-and-keybindings.md)
defines how language actions appear in context menus and shortcuts. Language
support contributes command behavior and availability; presentation remains an
editor concern.

JDT LS understands Maven projects, but it is not general XML language support.
The built-in XML editor retains syntax presentation for `pom.xml`; XML schema
completion and XML-specific diagnostics can be supplied by a later language
extension.

Debugging is not part of LSP. Run, Debug, breakpoints, and process control require
a separate Debug Adapter Protocol design. Play, Pause, and Stop for a JScene3D
game preview are also separate from both LSP and Java debugging.

## Open decisions

The following are intentionally deferred until their implementation slices:

- exact public language contribution and document-session types;
- exact names of the provisional Maven modules;
- the pinned JDT LS release and distribution build mechanism;
- cache paths, eviction, and simultaneous windows on one project;
- project JDK discovery and configuration;
- detailed client capabilities and JDT-specific initialization options;
- watched-file registration and build refresh policy;
- the bridge message encoding and feature-specific result values;
- language-server logs and troubleshooting user experience;
- precedence when more than one extension supports a language; and
- trust and installation rules for external language servers.

These decisions must preserve the accepted direction and should be recorded as
the implementation reveals their actual constraints.

## References

- [Eclipse JDT Language Server](https://github.com/eclipse-jdtls/eclipse.jdt.ls)
- [Eclipse LSP4J](https://github.com/eclipse-lsp4j/lsp4j)
- [Monaco Editor language interfaces](https://microsoft.github.io/monaco-editor/typedoc/modules/languages.html)
- [Language Server Protocol specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/)
