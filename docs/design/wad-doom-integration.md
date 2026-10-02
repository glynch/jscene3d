# WAD and Doom integration

JScene3D separates archive access, generic project importing, and Doom content
interpretation. This keeps the WAD container reusable, keeps game-format logic
out of the project core, and converts imported content into the same project
model used by other generated assets.

For the surrounding entity, component, and runtime model, see
[Entity-component world architecture](entity-component-world-architecture.md).

## Module responsibilities

The integration is divided across three modules:

- `jscene3d-wad` validates WAD files and exposes their archive directories,
  lumps, provenance, bounded reads, and explicit archive layers. It does not
  interpret Doom maps or game rules.
- `jscene3d-wad-import` adapts an opaque WAD archive to the generic JScene3D
  project-import lifecycle. It exposes the archive and its ordered lumps as
  selectable source items and publishes an archive index plus selected lump
  payloads.
- `jscene3d-doom` discovers and decodes classic Doom maps, imports their
  materials, builds renderer-independent geometry, and publishes selected maps
  as project-native generated definitions and resources. It also supplies the
  generic Doom runtime component implementations used by those definitions.

Both import modules register `ProjectImportExtension` providers. Project import
orchestration, cache publication, and runtime consumption remain generic and do
not depend on WAD- or Doom-specific rules.

## Validated WAD archives

`WadLoader` validates the source before exposing a `WadArchive`. Validation
covers the archive header and kind, directory count and bounds, lump bounds,
names, source readability, and an optional expected SHA-256 digest. Failures
are returned as structured `WadDiagnostic` values.

A valid archive retains `WadProvenance`: the normalized source path, observed
file size, and SHA-256 digest of the complete source. Directory entries remain
in source order, including duplicate names. Name lookup is case-insensitive,
and last-match lookup implements normal WAD replacement ordering within one
archive.

The archive holds no open file handle. Opening a lump creates an independent,
caller-owned stream bounded to that lump's validated range. A caller using
`readAllBytes` must provide an allocation limit. If the source is later
replaced or its size changes, reads fail rather than extending beyond the
validated bounds.

## Archive layering

`WadArchiveLayers` combines independently validated archives in explicit
low-to-high precedence order. Its flattened view retains each archive, layer,
and original directory position. Looking up the last occurrence of a name
therefore selects the highest-precedence matching lump.

Layering remains a container-level operation. It does not infer namespaces,
merge Doom maps, or interpret lump contents. The current WAD and Doom project
importers each load the single source asset named by an import definition;
they do not implicitly construct a layered archive set.

## Generic WAD importing

The WAD importer first loads and validates the declared project source asset.
If the asset declares a SHA-256 digest, that digest is enforced. Inspection
then exposes one archive source item and every lump in directory order.

Lump identities include both the directory index and an encoded name. They are
therefore stable for unchanged input, sensitive to reordering, and unambiguous
when names repeat. Selecting the archive selects all lumps; individual lumps
can also be selected.

Preparation publishes:

- an index describing the validated archive and selected content; and
- each selected lump as an opaque payload containing its exact validated
  bytes.

This importer deliberately does not decode Doom maps. Consumers that need only
archive or lump access can use it without accepting Doom-specific semantics.

## Doom map interpretation

`DoomMapDecoder` discovers conventional `MAP##` and `E#M#` markers in archive
order. Decoding selects the last matching marker and requires the classic ten
map lumps in their defined sequence. It parses the fixed little-endian records
and validates cross-references, BSP data, reject data, and blockmap data.

UDMF and Hexen-format maps are diagnosed as unsupported. Individual map lumps
are also subject to a fixed allocation limit before their bytes are read.
Operational and content failures are reported as source-aware Doom diagnostics
rather than exposed as partially decoded maps.

The decoded `DoomMap`, material data, and geometry data are renderer-independent.
The Doom project importer uses those models to prepare a selected map as a
closed graph of generic project artifacts. The graph can include a generated
entity definition, mesh resources, materials, textures, collision resources,
and their immutable payloads. Supported Doom map semantics are represented by
ordinary entities and components rather than by a title-specific world model.

## Authored and generated content

An import definition is authored project data. It identifies the source asset,
registered importer, ordered selection, and importer settings. The artifacts
produced from it are generated content: they are read-only, reproducible from
the declared source and settings, and disposable as a cache generation.

Generated entity definitions use normal project `AssetId` values and normal
entity/component data. Generated resources use the same resource references
and registered resource types as authored content. Authored wrappers can
reference generated definitions while retaining game-specific behavior and
other authored structure outside the generated graph.

## Publication and cache invariants

Import preparation occurs in an engine-owned staging workspace. The generic
import coordinator validates the selection and artifact graph, records each
artifact's size and SHA-256 digest, and computes fingerprints from the import
definition, source, importer type, and declared dependencies.

Only a complete valid preparation can be published. A generation is immutable
and addressed by its fingerprint. Publication atomically moves the completed
generation into the cache and then atomically replaces the active-generation
pointer while holding import-specific process and file locks. There is no
non-atomic fallback. A failed, invalid, or cancelled preparation therefore
does not replace the previous active generation.

Published-content readers validate the active generation, index, artifact
paths, sizes, and content digests. They expose a read-only view and do not
create staging paths or execute importer code.

## Rendering and collision

The Doom importer publishes rendering and collision independently. Render
meshes are grouped with their material resources, while collision geometry is
published as separate collision resources and referenced through explicit
physics components. A visible surface is not implicitly treated as collision,
and collision is not reconstructed from runtime renderer objects.

This follows the project-wide rule described in
[Entity-component world architecture](entity-component-world-architecture.md):
rendering and collision are separate authored or generated concerns, even when
both originate from the same source map.

## Project and runtime integration

Published Doom content enters the ordinary JScene3D project model rather than
a parallel WAD runtime:

```text
WAD source asset
        ↓ inspect and prepare
published entity definitions and resources
        ↓ read-only project content loading
DefinitionResolver and RuntimeResourceProvider
        ↓
SceneDefinition / EntityDefinition / runtime resources
```

`PublishedProjectContent` combines the authored asset catalog with generated
entity definitions and runtime resources from the same published import cache.
`PublishedRuntimeResources` provides the resource-only form. These load paths
read complete active generations; they neither discover nor execute importers
and do not read the original WAD source.

Consequently, world composition and runtime loading see standard definitions,
components, and resources. WAD validation and Doom decoding remain build or
authoring concerns, while the runtime consumes only published project content.
