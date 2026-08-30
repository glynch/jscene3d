# Keep window options focused

`WindowOptions` is a final, immutable value with a builder, value equality, and
documented defaults: a `1280` by `720` logical size, the title `JScene3D`,
enabled vertical synchronization, and zero requested MSAA samples. Windows are
resizable and initially hidden in version 0.1.

The builder rejects non-positive dimensions, null titles, titles containing a
null character, negative preferred framebuffer sample counts, and null
vertical-synchronization values when they are supplied. An empty title is
valid. `preferredFramebufferSampleCount(int)` is a soft MSAA request;
`Window.framebufferSampleCount()` reports the actual result, and a different
platform-provided count is not an argument failure.

Version 0.1 does not expose framebuffer bit counts, fullscreen modes,
decorations, native handles, visibility choices, or debug-context switches.
These settings add public compatibility obligations without an initial example
that needs them. Vertical synchronization is also mutable through `Window`
because changing the swap interval does not require window recreation.
