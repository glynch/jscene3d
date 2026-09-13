# Editor Settings Experience

Status: planned on 2026-09-13.

## Purpose

The editor needs one searchable settings experience for user preferences and
project configuration, with graphical controls and direct JSON editing backed by
the same values. This document extends the project-only decision in
[ADR 0031](../adr/0031-generate-settings-from-shared-declarations.md); it does not
describe functionality that is already implemented.

## Settings scopes

The Settings editor will expose two explicit scopes:

- **User** settings apply across projects and remain available when no project is
  loaded. Appearance, source-editor font, and minimap visibility belong here.
- **Project** settings apply only to the loaded project and continue to use
  `<project>/.jscene3d/settings.json`.

Each `SettingDefinition` will declare the scopes in which it is valid. A setting
may support User, Project, or both scopes. When both are supported, a project
value overrides the user value, which overrides the declared default. Settings
that are inherently user-specific, such as the selected editor theme, will not
accept project overrides.

User settings will use a `settings.json` in the platform-appropriate JScene3D
application-data directory. The storage adapter owns platform path resolution;
callers and extensions do not construct that path. When this is implemented, the
existing Java Preferences appearance values must be migrated or used as a
one-time fallback so an editor update does not silently discard the selected
theme and font.

## Generated settings editor

The current Appearance and Project tabs will become User and Project. Appearance
will be a category of User settings rather than a separate settings system. The
selected scope determines which values and JSON document are being edited.

A search field above the scope tabs will filter the generated rows. Matching is
case-insensitive and covers the display name, stable setting key, description,
category, and contributing owner or extension. Groups with no matching rows are
hidden, and an intentional no-results state is shown when appropriate.

An **Open Settings (JSON)** action beside the search field will open the selected
scope's document in the ordinary source editor. The Project action is unavailable
without a loaded project; the User action is always available.

## One settings module

Graphical controls, direct JSON editing, effective-value resolution, validation,
persistence, change events, and diagnostics must remain behind one settings
module interface. Individual settings panes and extensions must not parse or
write settings documents themselves.

The module will use the resource working-copy seam accepted in
[ADR 0030](../adr/0030-model-editable-content-as-resource-working-copies.md).
Opening a settings document therefore participates in the same tab identity,
dirty state, Save command, close guard, and external-change handling as other
editable text resources. Graphical edits update that same working copy rather
than writing around an open source editor.

The first direct-editing implementation applies new values when a valid document
is saved. Invalid JSON or invalid known values publish diagnostics while the last
valid configuration remains active. Unknown keys remain preserved, following the
existing project-settings behavior. A graphical edit must never overwrite an
unresolved dirty JSON edit; it must operate through the shared working copy or
report the conflict.

## Extension contributions

Core modules and extensions continue to contribute ordinary declarative
`SettingDefinition` values rather than controls, serializers, or search entries.
The generated editor and JSON validation consequently gain extension settings
without extension-specific JavaFX code.

Color-theme contributions remain separate from settings declarations. The
selected theme is a User setting whose value refers to a registered theme
identity. Installable icon themes will use their own contribution interface and
selection setting rather than sharing color-theme tokens.

## Initial user settings

The first User settings migrated onto this module will be:

- selected color theme;
- source-editor font family;
- source-editor font size;
- `editor.minimap.enabled`, defaulting to `true`.

Changing any of these values through either editor updates the affected open
surfaces after the new configuration has been validated.

## Deferred implementation sequence

1. Add scoped definitions and user-settings persistence to the settings module.
2. Adapt appearance preferences to User settings and migrate existing values.
3. Generate the User and Project settings scopes from the same registry.
4. Add shared search and the scope-sensitive **Open Settings (JSON)** action.
5. Route both JSON documents through the resource working-copy model.
6. Add minimap visibility as an ordinary User setting consumed live by Monaco.

This sequence is intentionally deferred while Java source editing and language
support remain the higher-priority editor work.
