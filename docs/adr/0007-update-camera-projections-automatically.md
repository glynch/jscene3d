# Update camera projections automatically

Version 0.1 provides `PerspectiveCamera` and `OrthographicCamera` with validated controlled mutation, atomic setters for dependent projection values, and lazily recomputed projection matrices. Unlike Three.js, callers never mutate projection fields directly or invoke `updateProjectionMatrix()` manually; this gives up explicit recomputation timing to prevent invalid intermediate state and forgotten updates, while stereo and other specialized camera compositions wait for features and examples that require them.
