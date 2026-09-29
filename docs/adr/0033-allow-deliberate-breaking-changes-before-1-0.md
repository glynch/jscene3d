# Allow deliberate breaking changes before 1.0

Status: accepted on 2026-09-29.

This decision supersedes the pre-1.0 compatibility policy in
[ADR 0013](0013-publish-lockstep-semantic-versions.md). ADR 0013's lockstep
versioning, immutable-publication, and Maven Central direction remains
accepted.

## Context

JScene3D remains under substantial architectural development across its engine
APIs, project formats, Code OSS editor behavior, and Maven and JPMS module
boundaries. Requiring every pre-1.0 patch release to preserve source and binary
compatibility would either retain provisional contracts prematurely or turn
minor-version selection into a substitute for judging each change on its
technical merits.

The absence of a compatibility promise must not make breaking changes casual.
Uncoordinated changes would still leave the reactor, examples, schemas,
documentation, and known consumers describing different systems.

## Decision

Before 1.0, APIs, project formats, editor behavior, and Maven or JPMS module
boundaries may change without backward compatibility. No pre-1.0 patch or
minor release is assumed to preserve those contracts solely because of its
version number.

Breaking changes remain deliberate and coherent. The same change updates all
affected implementation, tests, examples, schemas, and documentation, and
provides migration guidance when known external consumers need it.

Version 1.0 establishes the normal stable Semantic Versioning compatibility
expectation.

## Consequences

Pre-1.0 consumers must pin and qualify the JScene3D version they use and may
need to migrate between releases. The project can continue correcting
provisional boundaries without preserving obsolete interfaces solely for
compatibility. Lockstep versions still give the repository one coherent set of
artifact coordinates for each release.
