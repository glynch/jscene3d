# Maintain transform matrices automatically

Version 0.1 keeps every transform-derived value current automatically. Public
transform mutators mark the affected local and world state dirty. Local matrix
queries recompose when necessary; world matrix and decomposed world-transform
queries update any required ancestors first; and renderer traversal updates the
required hierarchy before drawing.

`Object3D` exposes neither manual methods such as `updateMatrix()` and
`updateMatrixWorld(boolean)` nor a `matrixAutoUpdate` switch. Those Three.js
interfaces exist partly to coordinate openly mutable transform properties, while
JScene3D controls mutation and can maintain its own invariants. Removing them
also avoids an ambiguous boolean parameter and prevents callers from believing
that setters require a separate synchronization step.

The implementation retains dirty versions so automatic behavior does not mean
recomposing unchanged matrices on every query or frame. Direct arbitrary local
matrices are outside the version 0.1 contract and require a later, separate
design if a concrete use case needs them.
