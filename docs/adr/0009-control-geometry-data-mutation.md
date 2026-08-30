# Control geometry data mutation

Version 0.1 keeps NIO buffers and mutable backing arrays out of JScene3D's
public geometry interface. Attribute and index construction accepts primitive
arrays and copies them once into library-owned storage. Controlled scalar
setters cover small changes, while a scoped edit callback mutates that storage
directly and records one version change for a batch. The renderer has a private
zero-copy read path and does not copy unchanged data each frame.

This prevents external mutation from bypassing validation, bounds invalidation,
and GPU-upload versioning. It also avoids exposing NIO position and limit state
as part of the library interface. The design pays one defensive copy at public
construction and adds method-call overhead while generating data. A lower-level
streaming interface can be introduced later if profiling a real workload shows
that these costs are material.
