# Editor visual redesign implementation plan

This plan applies the accepted
[editor visual design](../design/editor-visual-design.md) to the existing
`jscene3d-editor` application. Work proceeds in independently verifiable slices
and preserves the editor's read-only, descriptor-driven architecture.

## Goal

Deliver a coherent near-black and indigo JScene3D editor whose viewport,
Hierarchy, Project browser, Inspector, Diagnostics, status, and splash feel like
one product. The first completed version provides a safe scene preview while
leaving room for an explicit Game mode.

## Implementation principles

- Keep the actual JScene3D renderer in the OpenGLFX viewport.
- Project visible state from existing definitions, descriptors, catalogs, and
  diagnostics.
- Introduce semantic view models only where they protect JavaFX controls from
  project-loading details; do not duplicate the project model.
- Build reusable style classes and cell factories instead of attaching one-off
  inline styles.
- Keep splash copy and progress live even when artwork is rasterized.
- Test projections headlessly and qualify visual behavior in the native editor.

## Slice 1: Theme foundation

Create the shared visual vocabulary before restructuring panels.

Deliverables:

- define semantic looked-up colours on the editor root in `editor.css`;
- establish system-font roles, compact control metrics, dividers, focus rings,
  and diagnostic severity styles;
- add semantic root and region style classes to `EditorApplication`;
- remove existing inline colour and font styling;
- cover normal, hover, pressed, selected, focused, disabled, and read-only
  control states;
- keep the existing layout and behavior working while the theme changes.

Verification:

- load the stylesheet through a focused JavaFX test;
- exercise representative controls under the JavaFX toolkit;
- launch the empty editor and a Doomed Corridors project without CSS warnings;
- confirm keyboard focus remains visible.

## Slice 2: Workspace shell and command scope

Restructure the shell to match the accepted composition without adding new
domain behavior.

Deliverables:

- replace the current top toolbar with compact product, project, and menu chrome;
- create a viewport header containing the world-preview title and local actions;
- provide a home for actual viewport-local commands without implementing
  fictional tools;
- place Hierarchy left, Inspector right, and Project plus Diagnostics below the
  viewport using resizable split panes;
- add the concise bottom status bar;
- persist sensible divider positions for the application session if that can be
  done without introducing a settings subsystem.

Verification:

- test minimum-size behavior and divider bounds;
- manually inspect 1280 by 780 and 1440 by 900 windows;
- resize across supported JavaFX scaling factors while the OpenGL viewport is
  presenting;
- confirm the viewport still releases its resources before its host context.

## Slice 3: Selection and read-only Inspector projection

Make the selected concept's Inspector truthful and useful.

Deliverables:

- introduce one editor selection model shared by Hierarchy and Project views;
- retain the authored source, entity or asset identity, kind, and read-only or
  generated state required for inspection;
- project component descriptor display names, authored properties, defaults,
  constraints, and references into immutable Inspector view data;
- render collapsible component sections and typed read-only property rows;
- distinguish missing extension metadata and unsupported schema versions through
  structured diagnostics rather than empty controls;
- clear or replace selection transactionally when a project is reopened.

Verification:

- unit-test selection transitions and immutable Inspector projections;
- cover local entities, placements, generated definitions, assets, references,
  defaults, missing descriptors, and long values;
- confirm inspection never executes application implementation classes;
- confirm no Inspector interaction mutates project or live preview state.

## Slice 4: Hierarchy and Project presentation

Apply JScene3D-specific visual semantics to project navigation.

Deliverables:

- add reusable cells for world, local entity, placement, generated, disabled,
  and selected hierarchy states;
- add reusable Project cells or cards for Worlds, Entity Definitions, Source
  Assets, and Imports;
- expose generated and read-only state with an icon plus text or tooltip;
- add search only when it filters real projected items;
- retain stable identities for selection and diagnostics while keeping them out
  of ordinary labels;
- provide intentional empty and loading states.

Verification:

- unit-test categorization, filtering, ordering, and selection retention;
- open Doomed Corridors and verify its real MAP01 roots and mixed asset kinds;
- test duplicate labels, long names, generated imports, and disabled entities;
- confirm opening definitions does not instantiate them.

## Slice 5: Diagnostics drawer and status

Replace the permanently tall diagnostic list and verbose status sentence with a
collapsible, structured presentation.

Deliverables:

- add a Diagnostics tab or drawer with total and severity counts;
- render severity, stable code, source, location, message, and expandable detail;
- allow a relevant hierarchy, asset, or Inspector item to expose associated
  diagnostics where stable addressing permits it;
- keep concise project and preview state in the status bar;
- preserve project-open timing detail outside the permanent status line.

Verification:

- unit-test count aggregation and stable ordering;
- cover no-diagnostic, warning-only, error, and mixed states;
- verify long sources and technical details remain accessible;
- confirm a failed preview leaves actionable diagnostics visible.

## Slice 6: Production brand and splash assets

Turn the selected splash composition into independently controllable artwork and
live UI.

Deliverables:

- recreate and accept the geometric product mark as an original SVG;
- create a text-free, project-neutral Viewport Emergence background with safe
  regions for live content;
- derive required raster sizes from the approved source while retaining the
  editable master;
- replace the current card composition with the accepted full-frame layout;
- render product name, project name, path, loading phase, progress, and version
  as JavaFX nodes;
- preserve the configured minimum visibility without delaying loading work;
- dismiss the splash after a failed load, report the error through the
  workbench message interface, and retain details in Diagnostics.

Verification:

- retain and extend the existing splash timing and lifecycle tests;
- test startup with no project, successful project open, reopening a project,
  and project-load failure;
- verify long names and paths, progress transitions, and fade cancellation;
- inspect packaged execution so asset resolution does not depend on the source
  checkout.

## Slice 7: Native visual qualification

Complete the redesign only after testing the composed product rather than
isolated controls.

Deliverables:

- capture reference screenshots for empty, loading, opened, warning, and failure
  states at the 1440 by 900 reference size;
- inspect focus order, mouse behavior, text truncation, high-DPI scaling, and
  viewport resizing;
- run the full ordinary verification lifecycle and the relevant native editor
  smoke path;
- update the manual and README screenshots or descriptions that become stale;
- record any platform-specific deviations rather than hiding them in CSS.

Verification commands begin with:

```shell
./mvnw clean verify
./tools/scripts/run-editor.sh /path/to/doomed-corridors
```

Native packaging and render-integration checks remain part of platform
qualification where their existing profiles apply.

## Slice 8: Session workbench placement

Separate extension-declared default locations from the current workbench
layout, then make the current placement adjustable without coupling extensions
to JavaFX.

Deliverables:

- retain a default container on each view contribution;
- resolve current placements in a toolkit-independent workbench module;
- expose one Customize Layout surface for region visibility, primary-side-bar
  position, and movement of ordinary views among the primary side bar, secondary
  side bar, and lower panel;
- pin Activity Bar-owned views to the primary sidebar and omit them from movable
  layout choices;
- update every container from the same placement snapshot;
- retain moves for the editor session without treating JavaFX nodes as layout
  state.

Verification:

- unit-test default resolution, moves, invalid destinations, and removed views;
- confirm Activity Bar-owned Hierarchy and Extensions views cannot be moved;
- move Project, Inspector, and Diagnostics among the available containers in the
  native editor;
- confirm selection, diagnostic filters, and view commands still operate after
  a move;
- restart the editor and confirm defaults are restored until settings-backed
  persistence is implemented as its own slice.

## Slice 9: Activity Bar and bundled Extensions view

Add the smallest workbench navigation and extension-catalogue slice which uses
the same extension contracts intended for community contributions.

Deliverables:

- add toolkit-independent Activity Bar contributions backed by registered,
  pinned primary-sidebar view containers;
- contribute Scene and Extensions activities from bundled editor extensions;
- replace the complete Activity Bar-owned primary-side-bar content when a
  different container is selected;
- keep the selected container independent from primary-side-bar visibility;
- allow the active activity to collapse and restore the primary side bar;
- expose safe extension metadata through the editor extension context;
- list activated bundled extensions in the Extensions view and open the selected
  extension's metadata in a central editor tab;
- keep marketplace and install actions absent until a real package, trust, and
  registry design exists.

Verification:

- unit-test activity registration, extension metadata observation, layout
  visibility, primary-side-bar position, and reset behavior;
- switch repeatedly between Scene and Extensions and collapse the active view;
- inspect installed extensions, search the list, and open more than one detail;
- customize region visibility and view placement, restore defaults, then restart
  and confirm changes remain session-only.

## Recorded follow-up work

- add `.jscene3d/settings.json` project settings before persisting layout or
  extension settings;
- design theme contributions for dark/light color themes and installable icon
  themes without exposing JavaFX CSS names as the extension contract;
- audit all non-test source files over 300 lines and split orchestration classes
  where cohesive modules can own the extracted policy;
- design the local extension package and community-registry lifecycle before
  adding install, update, disable, trust, or removal controls.

## Slice 10: Commands, menus, and modal window lifecycle

Route core and extension actions through one toolkit-independent command model
before adding executable preview transport.

Deliverables:

- retain command metadata and observable enabled state in the extension host;
- generate JScene3D, File, Edit, and extension-owned menus from declarations;
- allow extensions to place commands in existing menus or declare a top-level
  menu without exposing JavaFX;
- open generated Settings in the editor area and About as a modal dialog;
- route native window close and Quit through the same dirty-resource guard;
- save every dirty registered working copy when Save is selected.

Verification:

- unit-test menu ordering, grouping, extension placement, and command state;
- unit-test modal declaration validation and every dirty-close result;
- confirm disabled commands cannot execute through programmatic dispatch;
- inspect all three core menus, About, Settings, and dirty Quit in the native
  editor.

## Suggested change sequence

Keep commits small and behaviorally coherent:

1. Add theme tokens and remove inline styling.
2. Restructure the shell without changing project projections.
3. Add selection and immutable Inspector projections.
4. Add JScene3D-specific Hierarchy and Project cells.
5. Add the Diagnostics drawer and compact status.
6. Add the accepted brand mark and production splash artwork.
7. Perform native visual qualification and documentation updates.
8. Add session workbench placement and validate the interaction before
   persisting it.
9. Add Activity Bar contributions and the installed bundled-extensions view.
10. Generate menus from command contributions and protect dirty window close.

Each change should include its tests and leave the editor launchable. The
selected mockups guide visual acceptance, while the project architecture and
descriptor contracts remain authoritative for behavior.
