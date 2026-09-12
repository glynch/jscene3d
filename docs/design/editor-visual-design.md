# JScene3D editor visual design

Status: accepted on 2026-09-11.

This document records the visual direction for the native JScene3D editor. It
turns the selected mockups into an implementable product system without changing
the editor's project model or promising unsupported authoring behavior.

The accepted workspace direction is
[the near-black and indigo editor concept](images/jscene3d-editor-concept-indigo.png).
The accepted splash direction is
[Viewport Emergence](images/jscene3d-editor-splash-viewport-emergence.png).
The derived production artwork and original vector mark are recorded in
[the splash asset manifest](editor-splash-assets.md).
The splash explorations remain available under
[`images/editor-splash-concepts`](images/editor-splash-concepts/) as design
history.

## Product truth

The visual design must communicate the editor that JScene3D actually owns:

- JavaFX owns the native application shell and ordinary controls.
- OpenGLFX embeds the real JScene3D renderer in the central viewport.
- The editor reads authored assets, safe extension descriptors, published
  imports, and the selected world without executing application behavior.
- The scene preview is distinct from a running Game view. The workbench must
  make that distinction through the view name, not an unexplained status badge.
- Hierarchy entries distinguish local authored entities, reusable definition
  placements, and generated read-only placements.
- The inspector projects descriptor metadata and authored values. It does not
  expose arbitrary Java implementation classes or pretend that runtime state is
  authored state.
- Diagnostics are structured project data rather than an undifferentiated log.

The selected concept depicts the next useful read-only editor slice, including
selection and descriptor inspection. It is not evidence that those interactions
already exist in the current implementation.

## Visual character

JScene3D uses a quiet, technical visual language. Near-black neutral surfaces
allow rendered project content to dominate. Indigo identifies selection, focus,
active tools, and product identity. Borders are thin and structural rather than
decorative. Depth comes from small surface-value changes, not large shadows or
raised chrome.

The editor should feel precise and spatial without imitating another engine's
brand, terminology, or exact controls.

## Colour system

The following values are the implementation starting point. They may be tuned
after testing on the verified macOS display path, but their semantic roles must
remain stable.

| Token | Value | Use |
| --- | --- | --- |
| `canvas` | `#0D1118` | Window background and deepest recesses |
| `chrome` | `#121722` | Menu, title, and status chrome |
| `panel` | `#171D28` | Hierarchy, Inspector, and Project surfaces |
| `panel-raised` | `#1D2431` | Headers, tabs, fields, and raised controls |
| `hover` | `#252D3B` | Hovered rows and controls |
| `selected` | `#45409A` | Selected rows and active regions |
| `accent` | `#7562F3` | Active tabs, progress, focus, and brand detail |
| `accent-hover` | `#8876FF` | Hovered active controls |
| `focus` | `#9A8BFF` | Keyboard-focus ring |
| `divider` | `#303746` | One-pixel panel and control dividers |
| `field` | `#121821` | Read-only and editable field wells |
| `text-strong` | `#EDF0F6` | Titles and selected primary text |
| `text` | `#C9CED8` | Ordinary labels and values |
| `text-muted` | `#8E97A8` | Secondary metadata and disabled text |
| `success` | `#62C9A5` | Successful validation and ready state |
| `warning` | `#E2B45E` | Warning diagnostics |
| `error` | `#FF7182` | Error diagnostics and failed loading |

JavaFX CSS should define colours as looked-up colours on the editor root. Rules
should consume semantic names rather than repeat raw colour literals.

## Typography

Use the JavaFX system font for controls unless a bundled, licensed family is
accepted separately. Platform consistency and reliable packaging are more
important than matching an image-generated typeface exactly.

| Role | Size | Weight |
| --- | --- | --- |
| Product identity | 24 px | Semibold |
| Selected object title | 18 px | Semibold |
| Panel heading | 13 px | Semibold |
| Control and body text | 12 px | Regular |
| Secondary metadata | 11 px | Regular |
| Status and badge text | 10 px | Medium |

Use tabular numerals for dense numeric properties when the selected system font
supports them. Do not use condensed text merely to fit excessive information.

## Spacing and geometry

Use a four-pixel base grid. Preferred increments are 4, 8, 12, 16, 24, and
32 pixels.

| Element | Target |
| --- | --- |
| Menu bar height | 30 px |
| Viewport toolbar or tab height | 36 px |
| Panel header height | 34 px |
| Tree and list row height | 28 px |
| Text field and compact button height | 28 px |
| Status bar height | 24 px |
| Ordinary panel padding | 8-12 px |
| Divider width | 1 px |
| Focus ring | 1 px inner plus 1 px outer contrast |

At the 1440 by 900 reference size, the Hierarchy should begin near 240 pixels
wide and the Inspector near 320 pixels wide. The Project browser should begin
near 250 pixels high. Split panes remain resizable, and the viewport receives
remaining space. These are starting proportions, not fixed dimensions.

## Workspace composition

### Application chrome

The top chrome contains product and project context plus compact declarative
menus. JScene3D owns About, Settings, and Quit; File owns project/file lifecycle
commands; Edit owns Undo and Redo. Extensions place registered commands into
these menus or declare a new top-level menu without constructing JavaFX controls.
The chrome does not contain Play, Pause, or Step transport while the editor has
no executable game-preview contract.

Viewport-specific commands belong inside the viewport header. This keeps camera,
framing, grid, bounds, collision-overlay, and projection controls scoped to the
surface they affect.

### Hierarchy

The Hierarchy presents the opened authored world. Icons and restrained metadata
must distinguish:

- the world root;
- a local authored entity;
- a reusable definition placement;
- a generated read-only placement;
- an initially disabled entry.

Generated content uses a lock or generated marker rather than a disabled colour.
Stable IDs remain hidden during ordinary browsing and available through advanced
inspection or diagnostics.

### Preview

The central region uses the opened world name followed by `Preview`, for example
`MAP01 Preview`. The viewport is the strongest visual region and must not be
surrounded by heavy frames. A future Game mode must be explicit rather than
making the ordinary scene-preview heading carry an implementation label.

Inspection overlays may use the indigo accent. Red, green, and blue remain
reserved for conventional spatial axes where needed and must not become general
interface accents.

### Inspector

The Inspector begins with the selected item's name, authored kind, source, and
read-only status. Component sections use descriptor display names and contain
authored values plus descriptor defaults. Sections are collapsible and remain
visually lighter than standalone panels.

Read-only fields retain normal text contrast. They must not look disabled, but
they also must not show an insertion caret, editable hover treatment, or commit
affordance. References use author-facing asset names with stable identities
available on demand.

### Project browser

The Project browser groups content by JScene3D concepts:

- Worlds;
- Entity Definitions;
- Source Assets;
- Imports.

Cards and rows expose the asset name, kind, provenance where useful, and
generated or read-only state. The browser must not introduce `Prefabs`,
`Scripts`, or filesystem categories borrowed from other editors unless JScene3D
later adopts an explicit domain concept with that name.

### Diagnostics and status

Diagnostics live in a collapsible bottom region. Its tab shows total and
severity counts without permanently consuming viewport height. Each diagnostic
retains severity, stable code, source, location, message, and structured detail.

The status bar reports concise editor state such as `Ready`, `Scene preview`,
and `0 errors`. Detailed project-open timings belong in diagnostics, telemetry,
or an expandable detail view rather than a long persistent sentence.

### Contributed view placement

Views declare default workbench containers. The workbench owns their current
placement and can move ordinary views among the primary side bar, secondary side
bar, and lower panel. A view referenced by an Activity Bar contribution is pinned
to its declared primary-sidebar location for the lifetime of that activity.
JavaFX containers render the resolved layout; they do not define it.

An editor-owned Customize Layout surface controls region visibility, primary
side-bar position, and placement for movable contributed views. Activity-owned
views are omitted because their Activity Bar registration is their stable
navigation location. Views do not grow separate context-menu implementations of
layout policy. Session movement is the first validation step. Project settings
can later persist the same toolkit-independent placement data after the
interaction has been accepted.

### Activities and extensions

The narrow Activity Bar switches between infrequently co-visible primary views
without baking those views into the JavaFX shell. Each activity contributes an
identity, title, semantic icon, order, and ordered primary-sidebar views.
Selecting the active activity again collapses or restores the primary side bar.
Activity entries and their views remain pinned until the owning extension is
deactivated.

Scene and Extensions are bundled contributions using the same contracts intended
for future editor extensions. The Extensions view initially reports extensions
activated in the current window and opens selected metadata in the central editor
area. Installing local packages, discovering a community registry, dependency
resolution, trust, signatures, permissions, and updates require a real extension
distribution design before actionable controls are presented.

## Interaction states

Every reusable control style must cover:

- normal;
- hover;
- pressed;
- selected;
- keyboard focused;
- disabled;
- read-only;
- warning;
- error.

Selection and focus are different states. Selection uses a filled indigo
surface; keyboard focus uses a visible focus ring that remains detectable on a
selected element. Colour is not the only indicator for generated content,
severity, or focus.

## Editor splash

Viewport Emergence is the accepted composition. Wireframe primitives resolve
into solid, lit geometry to represent descriptor-authored content becoming a
renderer preview.

The selected image is a design reference, not a production splash bitmap. Its
project name, filesystem path, loading phase, progress, version, and product text
are illustrative. Production uses:

1. a text-free, project-neutral background artwork asset;
2. a separately authored vector JScene3D mark;
3. live JavaFX labels for product, project, path, loading phase, and version;
4. the existing truthful phase progress supplied by editor loading;
5. a JavaFX progress indicator and failure presentation.

The bottom information region should remain readable across supported aspect
ratios. Artwork must provide safe negative space for the product identity in the
upper-left and the loading information below. Cropping must preserve the
wireframe-to-rendered transition.

The splash supports three states:

- editor startup before a project is known;
- named project loading with actual phase progress;
- persistent project-load failure with a concise error and access to details.

The JScene3D editor splash is separate from a project's own launch splash.
Doomed Corridors may be named as the project being opened, but its game artwork
and title treatment do not become editor branding.

## Brand assets

The geometric mark shown in the selected mockups establishes direction only.
Image-generated pixels are not a production logo source. Before implementation,
the mark must be deliberately reconstructed as an original SVG, reviewed at
small sizes, and accepted as a separate brand asset. The SVG is the source of
truth for application icons and raster exports.

## Accessibility and qualification

- Ordinary text targets at least 4.5:1 contrast against its surface.
- Large text and essential graphical indicators target at least 3:1.
- Keyboard focus remains visible throughout the shell.
- Controls do not depend on colour alone to communicate state.
- Text and icons remain usable at JavaFX scaling factors used by qualified
  displays.
- Long project names, paths, component names, and diagnostic locations truncate
  predictably and expose their complete value.
- Empty, loading, successful, warning, error, and unavailable-extension states
  receive deliberate layouts.

Visual acceptance begins on the verified macOS ARM64 platform. Windows and Linux
remain subject to the same rendering, scaling, font, focus, and packaging
qualification required by the editor architecture.
