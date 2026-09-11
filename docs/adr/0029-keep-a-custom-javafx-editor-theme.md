# Keep a custom JavaFX editor theme

Status: accepted on 2026-09-11.

The JScene3D editor will retain its purpose-built JavaFX CSS theme rather than
adopt AtlantaFX. A temporary A/B spike established that AtlantaFX 2.1.0 is
technically compatible with the editor's Java 21 and JavaFX 21.0.12 runtime
when its transitive JavaFX dependency is excluded, but its stronger control
containers, larger spacing, and general-purpose desktop styling make the
workspace feel blockier than the accepted near-black and indigo design. The
custom theme provides the preferred compact density, continuous panel surfaces,
thin structural dividers, and restrained use of indigo while allowing the
viewport and project content to remain visually dominant.

The editor therefore accepts responsibility for styling and qualifying every
required JavaFX control state. A third-party theme should only be reconsidered
if it supports this visual language without requiring extensive overriding; a
technically compatible dependency alone is not sufficient reason to adopt it.
