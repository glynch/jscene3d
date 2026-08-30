# Give the renderer exclusive OpenGL state ownership

Each version 0.1 `Renderer` exclusively owns the OpenGL state of its associated
context for the renderer's entire lifetime. Applications must not issue direct
LWJGL or OpenGL calls against that context. If they do, rendering behavior is
unspecified because those calls may invalidate the renderer's cached state.

The renderer neither snapshots nor restores arbitrary external OpenGL state.
`ShaderMaterial` is the supported customization boundary in version 0.1. This
keeps rendering efficient and the state contract testable without attempting an
expensive and inevitably incomplete preservation scheme.

An explicit state-invalidation or raw-interoperation boundary may be introduced
later when a concrete application requires it. Doing so will require its own
state, threading, and lifecycle contract.
