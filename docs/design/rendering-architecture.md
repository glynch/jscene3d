# Rendering architecture

JScene3D separates renderer-independent scene and resource descriptions from
the OpenGL objects needed to draw them. This document describes that boundary,
the current synchronization model, and the warmed-up steady state. It is not a
rendering feature reference; see [Rendering fundamentals](../manual/fundamentals.md)
and [Rendering](../manual/rendering.md) for the public API and capabilities.

## Ownership boundary

The core model contains no OpenGL identifiers. `BufferGeometry`, its buffer
attributes and index data, materials, textures, environment maps, and scene
objects describe what should be rendered. Applications own the closeable
descriptions and may share them among scene objects.

`Renderer` owns the OpenGL state and GPU objects created for one
`RenderSurface`. The surface gives the renderer exclusive access to a
host-owned context and presentation framebuffer; it does not transfer ownership
of that context or presentation lifecycle. This preserves the boundary recorded
by [ADR 0002](../adr/0002-publish-core-and-lwjgl-artifacts.md) and
[ADR 0017](../adr/0017-give-the-renderer-exclusive-opengl-state-ownership.md).

A renderer creates a GPU realization only when a draw first needs the
description. The same geometry or texture used by two renderers therefore has
two independent context-local realizations. No OpenGL identifier is shared
through the core description. This follows
[ADR 0020](../adr/0020-support-independent-window-renderer-pairs.md).

```text
renderer-independent descriptions
                ↓ first use
per-renderer, context-local realizations
                ↓ version comparison
synchronize changed state only
                ↓
reuse programs, resources, and submission capacity
                ↓
draw
```

## Version-driven synchronization

Controlled mutation makes renderer-visible changes observable without exposing
mutable backing storage. The general rule comes from
[ADR 0004](../adr/0004-control-public-state-mutation.md): the owning description
validates a change, updates derived state, and advances the relevant version.
The renderer compares that source version with the version already represented
by its own context-local resource.

### Geometry

`BufferAttribute` and `IndexBuffer` versions identify changed vertex and index
payloads. A renderer creates buffers lazily, retains them for that description,
and uploads an attribute or index again only when its identity or version has
changed. Geometry structure, bounds, morph targets, and related derived state
have their own invalidation rules; a single geometry-wide upload is not used as
a substitute for those more precise versions.

This is the current realization of
[ADR 0009](../adr/0009-control-geometry-data-mutation.md). Controlled scalar
changes and scoped edits preserve validation and advance versions without
requiring an extra public copy during every draw.

### Textures

`Texture` separates image, sampler, and texture-coordinate transform versions:

- an image-version change uploads the base image again and invalidates the
  generated mipmap chain;
- a sampler-version change reapplies filtering and wrapping without uploading
  image pixels; and
- a transform-version change updates the cached CPU-side UV transform. Built-in
  materials pass that transform and coordinate-origin state to the shader
  without redefining the texture image.

Mipmap regeneration follows image changes rather than every bind, as recorded
by [ADR 0019](../adr/0019-generate-texture-mipmaps-by-default.md).

### Instances and morph targets

Instanced meshes version their matrix, optional color, and custom instance
attributes independently. Morph-target resources compare the geometry and
target-attribute versions, while morph weights have their own versioned update
path. Unchanged instance and morph data therefore does not require another
buffer upload merely because the containing object is drawn again.

Versioning is targeted rather than universal. Material parameters and ordinary
draw uniforms are applied as needed during submission; a material version is
not a promise that every material has a one-to-one persistent GPU object.

## Program and submission reuse

Built-in shader programs are created lazily and retained by their renderer.
Custom `ShaderMaterial` programs are shared within that renderer when their
immutable shader sources, definitions, required attributes, and instancing
structure produce the same program key. Application uniform values remain
material state and are uploaded for the active draw; program reuse does not
mean uniforms become immutable.

The current OpenGL renderer also retains render-list capacity and reuses
render-item instances after each frame. That pooling supports the steady-state
goal, but its concrete collections, field names, sorting implementation, and
pass sequence are implementation details rather than public architecture.

## Cleanup

Resource descriptions are application-owned, shareable, and terminally
closeable under
[ADR 0003](../adr/0003-close-resource-descriptions-terminally.md). Removing one
mesh does not close its geometry, material, or textures.

The current renderer checks for closed geometry, texture, and environment
descriptions at a render boundary and deletes their context-local realizations
while its context is active. It also releases instance and morph resources when
their renderer-owned lifetime ends. `Renderer.close()` deletes every remaining
GPU realization, cached program, render target, and other context-local object,
then releases its exclusive surface access. The host still owns and destroys
the surface or window.

The architectural requirement is deterministic cleanup on the owning context.
The current maps, end-of-frame retention rules, and closed-description scan are
implementation mechanisms, not interfaces that other modules may depend on.

## Warmed-up steady state

For an unchanged scene after the required resources and shader variants have
been realized:

- geometry, instance, morph, and texture image data are not uploaded again;
- unchanged sampler state is not reapplied;
- existing shader programs are not recompiled or relinked; and
- retained render-list capacity and reusable scratch values avoid rebuilding
  equivalent allocation-heavy infrastructure for every frame.

Rendering still performs traversal, culling, ordering, state binding, uniform
updates, and draw submission. Newly visible resources, mutations, surface-size
changes, new shader variants, and feature-specific render targets may allocate
or synchronize work. The architecture seeks an allocation-conscious steady
state; it does not promise that every possible frame performs zero allocation
or zero CPU work.

Renderer statistics expose uploads, uploaded bytes, program counts, and active
resource counts so tests and applications can observe this behavior without
depending on internal cache structures.

## Contract and implementation detail

The durable contract is:

- core descriptions remain independent of OpenGL;
- every renderer owns its context-local realizations;
- controlled mutation makes synchronization observable;
- unchanged versioned payloads are not repeatedly uploaded;
- program realizations are reused within their owning renderer; and
- cleanup happens explicitly with the correct context active.

Identity maps, exact program keys, pooling containers, texture-unit assignments,
and pipeline stage ordering are current implementation details. They may evolve
without changing the ownership and synchronization architecture described
above.
