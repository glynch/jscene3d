# Generate texture mipmaps by default

Version 0.1 textures accept a single base image and default to
`MipmapMode.GENERATE`. Each renderer generates the complete mipmap chain when it
first realizes the texture and regenerates it once after a pixel-data version
change. The default minification filter is trilinear
`LINEAR_MIPMAP_LINEAR`; the default magnification filter is `LINEAR`.

This improves the ordinary rendering quality of distant and oblique textured
surfaces at the cost of roughly one-third additional GPU texture storage and a
small realization-time generation cost. CPU pixel storage remains the base
image only.

`MipmapMode.NONE` supports pixel art, interface artwork, and specialized memory
budgets. It requires a non-mipmap minification filter. Texture construction or
configuration commit rejects incompatible combinations rather than silently
substituting a different sampler configuration. Caller-supplied mip levels are
outside the version 0.1 contract.
