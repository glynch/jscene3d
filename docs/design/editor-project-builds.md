# Editor project builds

Status: accepted direction recorded on 2026-09-14; implementation is in progress.

## Purpose

The editor needs a reliable development build before it can run an authored
project. Building must remain responsive, report useful diagnostics, and avoid
starting redundant processes while files are being saved. A future Play command
must be able to ask for one current successful build without knowing whether the
project uses Maven or another build system.

This document defines automatic and manual build behavior, project freshness,
menu and status presentation, diagnostics, output, cancellation, and the seam a
future runtime launcher will consume. It does not define Play, Pause, or Stop.

## Accepted behavior

The initial implementation will:

- enable automatic building by default for each newly opened workspace;
- trigger automatic builds after build-relevant files are successfully saved,
  not after each editor change;
- open the workbench without waiting for a background build;
- run at most one build for a project at a time;
- coalesce saved changes which arrive while a build is queued or running;
- associate every result with the saved-project revision it built;
- retain unsaved working copies without presenting them as built;
- publish structured build diagnostics and preserve complete build output;
- use a Maven adapter first while keeping build orchestration independent of
  Maven goals and output directories; and
- expose one successful build snapshot which a later Play implementation can
  consume.

Automatic describes when a build is requested. Background describes where it
runs. Manual and automatic builds both execute away from the JavaFX Application
Thread.

## Project menu

The workbench will add a top-level **Project** menu after **Edit** and before the
future **Run** menu. It will contain:

1. **Build Project**
2. **Rebuild Project**
3. **Cancel Build**
4. **Show Build Output**
5. a separator
6. **Build Automatically**, presented as a checked menu item

Command availability is defined as follows:

| Command | Availability |
| --- | --- |
| Build Project | A project is open and no build is running. |
| Rebuild Project | A project is open and no build is running. |
| Cancel Build | A build is running or starting. |
| Show Build Output | Output exists for at least one build in the current project session. |
| Build Automatically | A project is open; its checked state reflects the current workspace preference. |

**Build Project** requests an ordinary development build without cleaning. It
remains available when automatic building is enabled so a developer can retry a
failure or explicitly verify the project. **Rebuild Project** performs the
adapter's clean development build. A separate command which merely cleans and
leaves the project unbuilt is not part of the initial interface.

These operations are registered once under stable command identities. The menu,
future command palette, status actions, and future keybindings invoke those
commands rather than owning build behavior. The command model must support a
genuine checkable state for **Build Automatically**; the JavaFX adapter must not
simulate it by placing a check mark in the title.

No JavaFX-only accelerator will be introduced for the first slice. Default and
custom build shortcuts will use the shared keybinding direction described in
[Editor command surfaces and keybindings](editor-command-surfaces-and-keybindings.md).

All command titles, confirmations, status text, and errors resolve through
`MessageSource`.

## Automatic-build preference

**Build Automatically** is an untracked developer preference scoped to the
current workspace. It is not authored project configuration and must not modify
`jscene3d.json`, `.jscene3d/settings.json`, or another repository file.

A workspace without an existing preference defaults to enabled. The editor
stores the preference in its platform-appropriate user state, keyed by stable
workspace identity. Different projects can therefore make different choices on
the same machine without sharing those choices through source control.

The checked Project-menu command is the initial editing surface. A later
settings experience may expose the default used for new workspaces, but it must
read and write the same settings module rather than create another preference.

Disabling automatic building does not cancel a build which is already running.
It prevents subsequent automatic requests. The developer may cancel the current
build explicitly.

## Build triggers

The build adapter classifies which saved resources affect its development
output. The initial Maven adapter is expected to consider at least:

- Java sources and `module-info.java`;
- `pom.xml`, Maven wrapper files, and Maven configuration;
- generated-source inputs;
- project resources processed by the Maven lifecycle; and
- other inputs declared relevant by the active project build.

The coordinator must not hard-code `src/main/java` as the complete input set.
Saving unrelated documentation or editor-user state does not request a build.

One logical Save All operation may write several resources. Automatic building
uses a short coalescing window so this produces one request. A build-relevant
external filesystem change also marks the project stale and requests an
automatic build when filesystem change observation is available.

When a project opens, build freshness is initially unknown. With automatic
building enabled, the editor queues a normal background development build after
the project session is available. Opening the project and presenting its first
preview do not wait for that build. The build adapter may determine that its
existing outputs require no work. This is the initial cross-session freshness
policy; a persisted editor fingerprint should be added only if measurement
shows that the no-op build cost justifies the added invalidation rules.

## Build kinds and Maven behavior

The generic interface defines two initial build intents:

- **incremental development build**, used by automatic and **Build Project**
  requests; and
- **clean development build**, used by **Rebuild Project**.

These are semantic intents rather than Maven commands. The Maven adapter selects
the project wrapper, reactor root, goals, JDK, environment, and output locations.
Other callers do not construct Maven command lines or inspect `target/classes`.

The Maven adapter prefers `mvnw` or `mvnw.cmd` supplied by the project. A later
configured or system Maven fallback belongs entirely inside that adapter. The
ordinary development build does not clean; Maven and its configured plug-ins
decide which work can be reused. The editor will measure real project build
times before introducing a Maven daemon or an additional fast compiler.

## Revisions and state

The coordinator exposes this project build state:

- `UNKNOWN`: no result has established freshness for this session;
- `CURRENT`: the latest saved build-relevant revision succeeded;
- `STALE`: saved build-relevant input is newer than the accepted result;
- `QUEUED`: a request exists but its process has not started;
- `BUILDING`: the adapter is executing a build; and
- `FAILED`: the latest completed saved revision failed.

Cancellation is an outcome rather than a persistent freshness state. After a
cancelled build, the project is `STALE` unless an independently verified current
result exists.

Unsaved build-relevant working copies are tracked separately. A project may
have a `CURRENT` saved build while also having unsaved editor changes. This
distinction is required for the future **Play Saved Version** behavior.

Every request captures the current saved-project revision. If a relevant save
occurs while that request is running, its result may still be reported but
cannot make the project `CURRENT`. When automatic building is enabled, the
coordinator queues exactly one follow-up request for the newest saved revision.
Further saves update that pending revision rather than starting more processes.

Only a successful result for the newest saved revision replaces the current
successful build snapshot. A failed, cancelled, or obsolete result never
silently makes older output current.

## The build seam

One deep project-build module hides:

- saved-revision and freshness tracking;
- automatic-build policy;
- request coalescing and serialization;
- adapter selection;
- process lifetime and cancellation;
- progress, diagnostics, and output publication; and
- successful build snapshots.

Callers request an incremental or clean development build and observe immutable
state and results. They do not coordinate processes, compare timestamps, parse
tool output, or locate artifacts themselves. Tests cross the same interface
using a controllable build adapter.

A build adapter is selected for the open project. It owns build-system-specific
applicability, relevant-input classification, invocation, cancellation,
diagnostic translation, and construction of the successful build snapshot.
Maven is the first bundled adapter. A second build system can later use the same
seam without changing menus, status presentation, or Play coordination.

The successful snapshot identifies the exact saved-project revision and carries
the build-system-neutral launch information required by the future runtime
module. It does not expose a Maven process or require the runtime module to infer
classpath entries from a conventional directory name.

## Source and package organization

The first implementation should use focused packages within the existing editor
modules. It must not create a new Maven module merely to reduce the number of
classes in an existing package.

If dependency direction or extension packaging later justifies a dedicated
build module, implementation classes must not be accumulated in that module's
root package. The package structure is established with the first commit and
separates responsibilities such as:

- the small public build interface and immutable values;
- coordination and revision tracking;
- build-process execution;
- diagnostics and output translation;
- Maven-specific adaptation; and
- toolkit-specific presentation.

The root package may contain `package-info.java` and types which genuinely form
the module's public interface. It is not a catch-all location for coordinators,
process launchers, parsers, adapters, JavaFX controls, and persistence. Tests
mirror the responsibility packages they exercise. Package names will be chosen
when the implementation exists; this rule does not require speculative empty
packages.

The same rule applies if build support extends an existing Maven module: new
classes go into cohesive responsibility packages instead of enlarging that
module's current root package by default.

## Diagnostics and build output

Each build owns a replaceable diagnostic publication. Structured source errors
and warnings use the existing Diagnostics view and source-range model. A failure
which cannot be attached to one source range is attached to the project or its
build descriptor. Build diagnostics identify their source as the project build,
independently of JDT language diagnostics.

Inline diagnostics are applied only when their saved revision still corresponds
to the displayed document. A diagnostic for an older saved version must not
place a misleading marker onto a newer unsaved working copy.

Complete standard output, standard error, command context, duration, and exit
outcome are preserved in a **Build** output channel. If a reusable Output view
does not yet exist, the build slice introduces the minimum generic output-channel
interface and presentation rather than a Maven-only text control. Runtime and
language support may later contribute their own channels through the same seam.

The persistent status item presents concise states such as **Build required**,
**Build queued**, **Building**, **Build succeeded**, and **Build failed**. Its
busy presentation indicates progress without claiming a meaningful percentage
when the adapter cannot provide one. Invoking a failed status opens Diagnostics;
**Show Build Output** remains available for complete details.

## Failure and cancellation

Build failures do not close the project or block editing. The latest applicable
failure replaces prior build diagnostics, preserves its complete output, and
leaves the project unable to provide a current successful snapshot.

Cancellation asks the adapter to stop the complete build process tree within a
bounded interval. Failure to terminate is reported visibly. Project close also
cancels or terminates its active build before releasing build state. No callback
from a closed project may update the subsequently opened project.

The last successful output may remain on disk after a later failure. It is not
treated as current and will not be selected silently by ordinary Play.

## Future Play contract

The later Play command asks the build coordinator for a current successful
snapshot. If the saved project is stale, that request starts or joins the
required build even when automatic building is disabled. Play begins only after
the requested saved revision succeeds.

When build-relevant working copies are unsaved, the editor asks:

> Some files have unsaved changes. Which version would you like to run?

The choices are:

- **Save All and Play**, which saves all relevant working copies and builds that
  new saved revision before playing;
- **Play Saved Version**, which leaves working copies untouched and builds the
  existing on-disk revision if required; and
- **Cancel**.

The ordinary Play command never falls back silently to a previous successful
build. An explicit **Run Last Successful Build** command may be considered later
if a demonstrated workflow requires it.

## Verification

Coordinator tests use a controllable adapter and clock to verify:

- automatic requests after relevant saves only;
- Save All coalescing;
- one active build per project;
- exactly one follow-up request after changes during a build;
- stale, obsolete, failed, and cancelled result handling;
- automatic-build preference changes;
- project-close isolation; and
- current successful snapshot selection.

Maven integration tests use a small temporary project and its wrapper to verify
successful incremental and clean builds, compilation failure diagnostics,
complete output capture, cancellation, and wrapper selection. Unit tests do not
download Maven distributions or use a developer's repository implicitly.

Manual qualification against Doomed Corridors and Beacon Garden will verify:

1. Open the project and confirm the workbench appears before the initial
   background build finishes.
2. Confirm the status reaches **Build succeeded** and **Show Build Output** opens
   the complete build log.
3. Type without saving and confirm no build starts.
4. Save one valid Java change and confirm one automatic build runs.
5. Save several files together and confirm they are coalesced into one request.
6. Save a compilation error and confirm build failure appears in Diagnostics and
   at the correct source range when the saved revision is displayed.
7. Fix and save the error and confirm diagnostics clear after a successful
   automatic build.
8. Disable **Project > Build Automatically**, save a relevant file, and confirm
   the status becomes **Build required** without starting a process.
9. Invoke **Project > Build Project** and confirm the saved revision becomes
   current.
10. Start a build, invoke **Cancel Build**, and confirm no build process remains
    and the project returns to a stale state.
11. Invoke **Rebuild Project** and confirm the adapter performs a clean
    development build.

The build feature is complete only when both consumer projects pass these flows
without relying on the JScene3D reactor checkout.
