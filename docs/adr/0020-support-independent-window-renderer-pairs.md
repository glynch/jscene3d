# Support independent window-renderer pairs

Version 0.1 supports multiple independent Window-Renderer Pairs in one JVM,
operated sequentially on the same render thread. Each JScene3D Window owns an
unshared OpenGL context, and exactly one Renderer owns that context. Renderer
operations that require OpenGL ensure their associated context is current, so
applications do not manually switch contexts.

A Resource Description may be used by more than one pair. Each Renderer lazily
creates, versions, and deletes its own context-specific GPU Realization. Closing
one pair does not disrupt another; an internal reference-counted GLFW runtime
terminates GLFW only after the final Window closes.

Version 0.1 does not support OpenGL object sharing between contexts, multiple
Renderers on one context, moving a Renderer between contexts, caller-created
contexts, background upload contexts, or concurrent render threads. These
features require synchronization and deletion contracts beyond the initial
single-render-thread model.
