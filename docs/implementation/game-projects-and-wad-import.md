# Game projects and WAD import

This document defines the project-loading and content-import direction for
JScene3D Game Applications. The first demanding consumer is Doomed Corridors, a
Doom-compatible game that uses a pinned Freedoom Phase 2 WAD. The authoring GUI
will follow the runtime and import foundations, but it must use the same
headless interfaces rather than acquire separate project-loading behavior.

## Product direction

A JScene3D game is a project rather than one hard-coded Java entry point. A
project combines descriptive metadata, engine compatibility, a Game Provider,
startup configuration, input configuration, source assets, legal notices, and
eventually export configuration. The project definition locates those parts; it
does not attempt to encode every scene, resource, or game rule in one file.

The future GUI will open a project, inspect its metadata, validate it, import
changed source assets, and launch it through the same modules available to
headless tools and standalone applications. Loading project metadata must not
create a window, initialize graphics or audio, execute Game Provider code, or
import source assets.

## Artifact responsibilities

- `jscene3d-project` owns the versioned Project Manifest, immutable project
  descriptors, safe project-relative path resolution, validation, engine
  compatibility checks, and structured diagnostics.
- `jscene3d-game` continues to own only genre-independent lifecycle, fixed and
  rendered updates, semantic input, character movement integration, and
  Physics Bindings.
- Source-format importers convert external content into engine-native runtime
  representations. An importer interface will be promoted into a reusable
  artifact only after at least two concrete formats demonstrate the seam.
- Doomed Corridors initially owns its WAD reader, Doom map model, WAD import,
  Doom-compatible rules, campaign state, presentation, and Freedoom content.
- The future editor is a separate application over the project, import, scene,
  and runtime interfaces. It does not own alternate implementations of them.

No Doom-specific type, WAD concept, project, weapon, enemy, sector rule, or
asset belongs in `jscene3d-game` or `jscene3d-physics`.

## Project Manifest version 1

The root `project.json` is UTF-8 JSON. Its first schema separates stable project
identity from operational runtime configuration. `schemaVersion` describes the
manifest format, while `identity.version` describes the game release.

Projects may point `$schema` either at the canonical schema URI or at a vendored
project-relative copy for offline editor validation. `schemaVersion` remains the
authoritative runtime discriminator. A vendored schema should be checked against the
copy bundled in `jscene3d-project` so it cannot drift silently.

Required information is:

- schema version;
- stable project identifier, display name, and project version;
- compatible JScene3D engine versions;
- stable Game Provider identifier;
- startup asset and target;
- at least one source asset.

Optional information includes creation and release dates, description, a
directly readable project icon, structured authors, links, project license,
third-party notices, credits, catalog tags, player counts, content warnings,
and namespaced extension data. Large credits and per-asset provenance remain in
referenced files rather than the manifest.

The project icon is a direct project-relative PNG or JPEG so a project browser
can display it without loading plugins or running asset import. Local editor
state, generated caches, secrets, signing credentials, absolute machine paths,
and recently opened files never belong in the shared manifest.

Unknown top-level fields are errors so misspellings cannot silently disappear.
Format-specific data will eventually live under namespaced extensions that can
be preserved without teaching the generic project model about every importer.

## Project-loader interface

The project module presents one primary operation conceptually equivalent to:

```java
ProjectLoadResult load(Path projectDirectory);
```

The result contains either a validated immutable project descriptor or no
project, plus ordered structured diagnostics. Diagnostics identify severity,
a stable code, a human-readable message, and a JSON location suitable for both
command-line output and GUI navigation. Expected project errors are data, not
exceptions. Programmer errors remain exceptions.

The implementation hides JSON parsing, defaults, schema evolution, path
normalization, compatibility checks, and diagnostic ordering. Callers and tests
observe behavior only through the project-loader interface.

## Source assets and imported cache

Source assets remain authoritative and immutable during import. Derived output
belongs in a disposable cache and must be reproducible from the source bytes,
import settings, and importer version. Deleting the cache must never destroy
project-authored information.

Every source asset has a stable identifier, type, project-relative path, and
optional SHA-256 digest. Importers must retain provenance detailed enough to
trace a runtime resource back to its source file and, where applicable, its WAD
lump or source object.

The manifest refers to an asset-source file once the list becomes large. The
first schema may embed the small list needed to prove the complete loading
slice, but the model must not assume that all assets remain inline forever.

## Freedoom and WAD direction

The initial compatibility target is a pinned Freedoom Phase 2 `freedoom2.wad`,
beginning with `MAP01` and progressively covering all 32 maps using vanilla
Doom II data and gameplay semantics. Boom, MBF, Hexen-format, UDMF, and GZDoom
extensions are outside the initial compatibility target.

The WAD remains the authoritative source for maps, textures, flats, sprites,
sounds, music, palettes, and other Freedoom content. It is not downloaded at
runtime. The project records its pinned release, digest, license, notices, and
credits. The importer never mutates the WAD.

WAD parsing and Doom compatibility are separate responsibilities. The reader
first exposes validated lump and map data without rendering or game rules. The
importer later converts that data into engine-native geometry, textures, audio,
collision descriptions, and Doom-specific metadata. The Doomed Corridors Game
Provider interprets object types, actor state machines, weapons, inventory,
linedef specials, sector behavior, exits, and campaign progression.

## Delivery sequence

1. Implement Project Manifest version 1 and its headless loader in
   `jscene3d-project`.
2. Load a real Doomed Corridors project as the first external consumer.
3. Pin Freedoom Phase 2 and implement headless WAD inspection with synthetic
   fixtures and a real-WAD integration test.
4. Decode `MAP01` into an immutable Doom map model.
5. Generate and render static level geometry and textures.
6. Generate collision and spawn the player from WAD thing data.
7. Implement one complete enemy and weapon combat loop.
8. Add doors, switches, lifts, teleports, exits, and sector effects.
9. Complete actors, weapons, inventory, HUD, and campaign progression required
   by the pinned Freedoom campaign.
10. Introduce the disposable imported-resource cache from demonstrated import
    requirements.
11. Build the authoring GUI over the proven project, import, scene, and runtime
    interfaces.
