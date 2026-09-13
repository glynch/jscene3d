# Editor Workspace Explorer

Status: planned on 2026-09-13.

## Purpose

The Workspace Explorer presents the files beneath the open project root while
omitting generated, private, and editor-owned content that is normally noise.
Projects can override those defaults without weakening the Explorer's filesystem
safety rules.

This document records the intended settings-backed visibility behavior. The
current Explorer implements only its hard-coded defaults and safety rules.

## Visibility terms

- A **default exclusion** is supplied by the editor or a build adapter and hides
  a path unless a project inclusion overrides it.
- A **project inclusion** is a project setting that makes a normally excluded
  path visible. It is an exception list, not a whitelist; unmatched files remain
  visible.
- A **project exclusion** is a project setting that hides a path and takes
  precedence over project inclusions and default exclusions.
- A **safety rule** prevents the Explorer from traversing a path regardless of
  settings. Paths outside the normalized project root and symbolic links are
  never exposed.

## Settings contract

The generated Project Settings surface will declare two project-scoped settings:

- `jscene3d.explorer.inclusions`: glob patterns that override default exclusions;
- `jscene3d.explorer.exclusions`: glob patterns that hide matching paths.

They are persisted through the existing `settings` object in
`.jscene3d/settings.json`:

```json
{
  "$schema": "https://jscene3d.org/schemas/project-settings-1.json",
  "schemaVersion": 1,
  "settings": {
    "jscene3d.explorer.inclusions": [
      ".git"
    ],
    "jscene3d.explorer.exclusions": [
      "**/generated",
      "**/*.class"
    ]
  }
}
```

The settings system does not yet have a list-of-strings value family. Implementing
this contract therefore requires a declarative string-list setting type, its
validation and generated editor control, rather than parsing untyped arrays inside
the Explorer.

Invalid patterns produce project-setting diagnostics and do not silently alter
visibility. Unknown settings continue to follow the preservation behavior defined
for the project-settings document.

## Glob behavior

Patterns are matched against normalized workspace-relative paths using `/` as the
separator on every platform. Absolute patterns, empty patterns, and patterns that
escape through `..` are invalid.

The supported syntax is deliberately small:

- `*` matches zero or more characters within one path segment;
- `?` matches one character within one path segment;
- `**` matches across path segments;
- an exact path such as `.git` matches that entry;
- when a pattern matches a directory, its decision applies to the complete
  subtree.

An inclusion for `.git` therefore exposes the root `.git` directory and its
descendants even though `.git` is a default exclusion. An exclusion such as
`.git/config` can still hide that path because exclusions have higher precedence.

## Resolution order

The Explorer resolves each candidate in this order:

1. Reject a path that violates a safety rule.
2. Hide a path matching any project exclusion.
3. Show a path matching any project inclusion.
4. Hide a path matching any default exclusion.
5. Show the path.

Consequently, when the same path matches both a project inclusion and a project
exclusion, the exclusion wins. Project inclusions can override defaults but cannot
override safety rules.

## Default exclusions

The editor retains these built-in defaults:

- directories named `.git` or `target` wherever they occur;
- files named `.DS_Store` wherever they occur;
- the project-relative `.jscene3d/cache` tree.

The resolved `jscene3d.cache.location` should also contribute its directory as a
default exclusion when it differs from `.jscene3d/cache`. Build adapters can
contribute additional default exclusions, such as their generated-output
directories, through the same policy composition. Project inclusions can reveal
these contributed defaults.

## Ownership and refresh

The project-settings module owns declaration, loading, validation, diagnostics,
and persistence. The project session supplies typed inclusion and exclusion lists
to the Workspace Explorer policy; the Explorer does not parse settings JSON.

The policy combines hard-coded defaults, settings-derived defaults, build-adapter
defaults, project inclusions, and project exclusions into one immutable snapshot.
When a relevant project setting changes, the Explorer rebuilds that snapshot and
refreshes its tree without reopening the project.
