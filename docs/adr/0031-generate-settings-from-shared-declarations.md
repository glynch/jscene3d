# Generate settings from shared declarations

Status: accepted on 2026-09-12.

## Context

Core editor features and extensions both need configuration. Requiring each
contributor to build its own settings UI would duplicate validation, persistence,
layout, accessibility, and future search or scope behavior. Hard-coding core
settings into a separate Java document and screen would create the same split from
the opposite direction.

Extension metadata is already discovered from
`META-INF/jscene3d/extension.json` before extension code is placed on an execution
path. Settings need to preserve that safe discovery boundary.

## Decision

Core modules and extensions use one declarative `SettingDefinition` model. Each
definition supplies a stable namespaced key, value type, default, label,
description, category, project scope, order, and type-specific constraints. The
toolkit-independent `jscene3d-configuration` module owns these contracts so project
loading, the extension API, and the JavaFX editor can depend on them without
reversing module dependencies.

Extension descriptors contribute settings through an optional top-level
`settings` array. The project loader validates those declarations without executing
extension code and combines them with core declarations in one `SettingRegistry`.
Duplicate keys are rejected.

`.jscene3d/settings.json` is a generic Jackson-bound document whose `settings`
object maps stable keys to JSON values. Unknown keys are preserved so temporarily
missing extensions do not destroy user configuration. A known invalid value emits
a diagnostic and resolves to its declared default.

The editor generates the Project Settings UI from the registry. Boolean,
enumeration, integer, number, string, and path settings receive built-in controls.
Changes are validated and atomically persisted immediately; contributors do not
provide JavaFX controls or serializers. Reset removes the project override and
reveals the declared default.

Activated extensions receive a read-only `EditorConfiguration` through their
context, including typed lookup and typed change events. Mutation remains owned by
editor commands and generated UI.

Only project scope is implemented now. Additional scopes will extend resolution
explicitly rather than pretending that user, workspace, language, or resource
scope already exists.

## Consequences

Core settings and extension settings have identical validation, presentation, and
persistence behavior. New ordinary settings require declarations rather than new
screens. Custom UI remains appropriate only for workflows that are not settings.

The registry and value families are public extension seams, so their compatibility
and diagnostics require the same care as project file formats. Adding a new value
family requires coordinated validator and generated-control support. Setting keys
must remain stable because persisted projects and extension code refer to them.
