# Use radians for every public angle

Every public angle, including camera field of view, is expressed in radians so generic angle handling remains consistent and unit mistakes are easier to avoid. This deliberately differs from Three.js camera APIs, which use degrees for field of view while using radians for rotations; any future degree convenience must identify degrees explicitly rather than overload an otherwise unit-ambiguous method.
