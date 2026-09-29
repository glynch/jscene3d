# Standardize coordinate and matrix conventions

Status: accepted on 2026-09-29.

## Context

Scene transforms cross the renderer, cameras, shaders, asset loaders, authored
project data, physics integration, and source-format adapters. If those
boundaries infer different handedness, up directions, multiplication order, or
angle units, data can remain locally valid while producing mirrored, rotated,
or incorrectly projected results elsewhere.

The conventions are already implemented by `Object3D`, the camera types,
shader matrix upload, glTF loading, spatial project adapters, and physics. They
need one explicit engine-wide record so new integrations do not rediscover the
rules from individual implementations.

## Decision

JScene3D uses a right-handed coordinate system with positive Y as the normal
world-space up direction. Cameras look along their local negative Z axis.

An `Object3D` local transform is composed as:

```text
local = translation × rotation × scale
```

With the column-vector convention used by JOML, scale is applied first, then
rotation, then translation. A child's world transform is:

```text
childWorld = parentWorld × childLocal
```

Matrices follow JOML multiplication semantics. Matrix scalar arrays and GLSL
matrix uniforms use JOML/OpenGL-compatible column-major order. Perspective and
orthographic cameras use right-handed projections and the OpenGL normalized
device depth interval from negative one at the near plane to positive one at
the far plane.

Renderer-level Java APIs express angles in radians, as established by
[ADR 0006](0006-use-radians-for-every-public-angle.md). Serialized or authored
formats may use another unit only when the unit is explicit in the schema or
name. Current authored spatial properties use degree-named values and convert
them to radians or quaternions at the runtime boundary.

Asset loaders and source-format adapters preserve these conventions when the
source already matches them and perform an explicit conversion when it does
not. Project spatial data and physics integration share the same right-handed,
Y-up runtime space rather than defining parallel coordinate systems.

## Consequences

Scene objects, cameras, shaders, collision data, and imported content can share
transforms without implicit axis swaps. Format-specific conversions remain
visible at their boundary and are covered by focused tests.

The convention constrains future renderer backends and importers, but it does
not constrain their internal storage beyond producing the same observable
transform and projection behavior. Large-world or floating-origin strategies
are separate decisions and are not implied by this ADR.
