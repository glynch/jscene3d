# JScene3D Editor Sandbox

This is the standard, intentionally small Project for manual JScene3D Editor
development and testing. It is deterministic test content, not a game or a
showcase.

- **Main** is the configured Main Scene. It contains a local Cube, a placement
  of the reusable Crate Entity Definition, and a Directional Light.
- **Second** contains a differently positioned Sphere and Directional Light so
  opening or switching Scene tabs is visually obvious.
- **Crate** is a reusable spatial Entity Definition with a box mesh and its own
  material.

Keep this Project small. Add content only when it supports a stable editor
workflow that cannot be exercised by Main, Second, or Crate.
