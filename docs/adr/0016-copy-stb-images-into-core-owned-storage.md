# Copy STB images into core-owned storage

Version 0.1 `TextureLoader` lives in `jscene3d-lwjgl`, supports PNG and JPEG,
and decodes them to RGBA8 through LWJGL STB. It copies the result once into
core-owned Java storage and frees the STB allocation before returning. Public
interfaces expose neither STB types nor native buffers, and the Texture retains
CPU pixels until terminal close so independent renderers can realize it later.

This preserves the LWJGL-free core and gives callers a simple ownership model.
It costs one CPU copy per loaded image, but avoids retaining native allocations
and prevents renderer timing from controlling decoder-memory lifetime. A more
complex ownership-transfer path requires measured evidence.
