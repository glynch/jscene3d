# JScene3D editor splash assets

The production editor splash uses independently controllable artwork and live
JavaFX content.

- [`brand/jscene3d-mark.svg`](brand/jscene3d-mark.svg) is the editable source of
  truth for the original geometric product mark.
- `jscene3d-editor/.../splash/jscene3d-mark.png` is the runtime raster derived
  from that SVG.
- `jscene3d-editor/.../splash/viewport-emergence-background.png` is the
  project-neutral production background derived from the accepted Viewport
  Emergence concept.

The background was produced with the built-in image-generation editor using
the accepted concept as its edit target. The production request preserved the
near-black architectural environment and wireframe-to-solid transition while
removing all text, logos, project data, progress UI, and bottom chrome. It also
reserved dark negative space in the upper-left and lower information region for
live JavaFX content.

The final generation request was:

> Derive a clean, text-free, project-neutral production background from the
> accepted Viewport Emergence concept. Preserve its landscape composition,
> near-black architectural environment, wireframe-to-solid transition, cool
> white light, and restrained indigo-violet glow. Reserve dark negative space
> in the upper-left for live product branding and across the lower portion for
> live loading information. Remove every baked-in word, logo, project name,
> path, progress indicator, version label, divider, and UI control, rebuilding
> those regions as continuous background artwork. Add no people, game-specific
> imagery, watermarks, or bright clutter.

Product name, project name, path, loading phase, percentage, progress, and
version are not part of the bitmap. They remain native controls so they can
report real state and remain accessible. Loading failures dismiss the splash
and are reported by the workbench message and Diagnostics interfaces.
