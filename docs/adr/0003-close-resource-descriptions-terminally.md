# Close resource descriptions terminally

`BufferGeometry`, `Texture`, and `Material` will be application-owned, shareable Resource Descriptions implementing `AutoCloseable`; `close()` is terminal, invalidates their use by every mesh and renderer, and notifies each renderer to queue deletion of its own GPU Realization on the context-owning thread. Removing a mesh never closes shared descriptions, `Renderer.close()` deletes all remaining realizations, and version 0.1 will not expose per-renderer `release(resource)` semantics because released-but-reusable state would make ownership ambiguous.

Every public `close()` is idempotent: the first valid call ends the lifetime and
schedules or performs cleanup at most once, while later calls are no-ops. Each
closeable type exposes `isClosed()`, and other operations after closure throw
`IllegalStateException`. The first close of a renderer or window still obeys its
thread-affinity contract.
