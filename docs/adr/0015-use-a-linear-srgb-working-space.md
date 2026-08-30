# Use a linear-sRGB working color space

JScene3D stores colors in a linear-sRGB working space. Immutable `Color` values
are created through explicit `srgb(...)` or `linear(...)` factories rather than
an ambiguous `rgb(...)` factory. Base-color images default to sRGB texture
formats that convert while sampling; data textures default to linear. Alpha is
separate and linear. The renderer requests an sRGB-capable default framebuffer,
enables linear-to-sRGB output conversion when available, and reports the actual
capability.

This provides consistent texture, blending, and future lighting behavior at the
cost of making input encoding explicit. Treating stored display values as if
they were linear would make simple code shorter but would embed incorrect color
math that later material systems could not repair compatibly.
