# JScene3D Coding Standards

These standards apply from the first implementation commit. They contain only
rules relevant to JScene3D.

## Terminology

Use the architecture vocabulary consistently:

- **Component** means a logical class, package, or subsystem with an interface
  and implementation.
- **Artifact** means a separately built and published Maven dependency.
- **JPMS Module** means a unit declared by `module-info.java`.
- Avoid the unqualified term **module**, because it is ambiguous in this
  project.

The definitions in `CONTEXT.md` are authoritative for domain terminology.

## Mandatory verification

Before a permanent commit, run:

```shell
./mvnw clean verify
```

Always include `clean`. Verification must begin from an empty Maven build-output
state and must not succeed because stale classes, generated sources, resources,
reports, or test output remain in `target` directories.

Every reliably machine-checkable standard belongs in the normal `verify`
lifecycle. CI checks and reports source formatting; it never rewrites a branch
automatically.

## Java source and formatting

- Target Java 21 and use stable features only. Production and test code must not
  require `--enable-preview`.
- Use UTF-8 explicitly for source, resources, reports, and runtime text
  conversion.
- Spotless with a pinned Palantir Java Format version is the only mechanical
  Java formatter.
- Spotless removes unused imports. Checkstyle forbids wildcard imports.
- Checkstyle enforces semantic source rules and must not duplicate formatter
  whitespace, import-order, or line-length behavior.
- `./mvnw clean verify` checks formatting and never rewrites source.
- Run `./mvnw spotless:apply` explicitly to format source.
- Do not leave public constructors or methods empty. Perform their required
  initialization, omit them, or document why an intentionally empty body is
  part of the public contract.
- Original source files use this short license header with the appropriate
  comment syntax:

  ```text
  Copyright 2026 Graham Lynch
  SPDX-License-Identifier: Apache-2.0
  ```

## Nullness and compiler analysis

- Mark production packages `@NullMarked` with JSpecify.
- Run Error Prone and NullAway in JSpecify mode over null-marked code, with
  their error diagnostics failing the build.
- Compile with all Java compiler lint warnings enabled. Global `-Werror` is not
  used while JOML's JPMS descriptor has an optional static dependency on the
  JDK Vector incubator module: javac emits an unsuppressible mandatory warning
  whenever that descriptor is resolved. The warning remains visible, and this
  exception must be removed if JOML or javac makes the diagnostic individually
  suppressible.
- Runtime validation remains mandatory at public boundaries despite static
  nullness analysis.
- Suppressions must be narrow and include a reason.

VS Code's separate null-analysis mode is disabled so it does not compete with
the build-authoritative NullAway configuration.

## Naming

- Value accessors use concise noun names such as `position()`, `parent()`, and
  `status()`, not JavaBeans `get...()` names.
- Mutators use explicit verbs such as `setPosition(...)`. Meaningful operations
  use domain verbs such as `add(...)`, `remove(...)`, and `detach()`.
- Boolean predicates use `is...()` where grammatically appropriate.
- Use `of(...)` to compose existing values, `from(...)` for conversion, and
  `load(...)` for I/O. Prefer a more descriptive factory when `of(...)` would
  hide intent.
- Builders use `builder()` and `toBuilder()`. Builder methods use noun names.
- Do not add builders or fluent chaining mechanically; use them only when they
  reduce genuinely complex construction.
- Avoid boolean parameters and overloads whose meaning is unclear at the call
  site. Prefer named value types, enums, or builders.
- Use `Path`, `URI`, `Duration`, `Instant`, and other domain-appropriate Java
  types instead of string or primitive substitutes.
- Treat acronyms as Java words in identifiers: `GltfLoader`, `OpenGlRenderer`,
  and `LwjglWindow`, not `GLTFLoader`, `OpenGLRenderer`, or `LWJGLWindow`.
- Do not create generic `Util`, `Common`, `Manager`, `Service`, or `Core`
  container classes. This rule does not prohibit the accepted
  `jscene3d-core` artifact name.
- Do not bury a generally reusable operation in a private helper merely because
  it currently has one caller. Put generic argument checks in a focused internal
  `Preconditions` component; keep class-specific invariant validation and
  implementation behavior in the owning class.

## Static verification

The normal `clean verify` lifecycle must eventually include:

- Spotless formatting checks.
- Java compiler linting, Error Prone, and NullAway.
- The checked-in semantic Checkstyle rules.
- SpotBugs restricted to high-confidence findings.
- Maven Enforcer checks for Java and Maven versions, plugin versions, dependency
  convergence, reactor convergence, and upper dependency bounds.
- Forbidden-API checks over production and test code using the checked-in
  project signatures plus the bundled unsafe, deprecated, non-portable,
  reflection, and `System.out` signatures.

The project-specific forbidden APIs require explicit character sets and prevent
JScene3D components from constructing threads directly. Callers own execution
threads.

## Packages and JPMS

- Every published artifact includes `module-info.java` and remains usable on the
  ordinary classpath.
- `jscene3d-core` uses JPMS module name
  `io.github.glynch.jscene3d.core`.
- `jscene3d-lwjgl` uses JPMS module name
  `io.github.glynch.jscene3d.lwjgl`.
- Export only intentional caller packages.
- Keep implementation in unexported `.internal` packages and prefer
  package-private implementation types.
- Never use split packages across artifacts.
- Forbid package, artifact, and JPMS dependency cycles.
- Avoid broad `opens`; qualify reflective access narrowly when unavoidable.
- Build minimal external consumer fixtures on both the module path and classpath
  during `./mvnw clean verify`.
- Do not introduce an `@InternalApi` escape hatch unless cross-artifact
  implementation collaboration genuinely requires technical accessibility.

## Public interfaces and values

- Every public or protected element in an exported package is supported caller
  interface.
- Classes are final unless inheritance is an intentional, documented extension
  point. Scene abstractions such as `Object3D`, `Camera`, and `Material` may be
  designed exceptions.
- Every exported type and public method has Javadoc covering the applicable
  invariants, lifecycle, thread rules, ownership, failures, and performance
  behavior.
- Every package-private production type, constructor, and method has concise
  Javadoc describing its internal contract. Private helpers have Javadoc when
  their name and signature do not fully explain their role.
- Configuration types such as `WindowOptions` and `RendererOptions` are final,
  immutable classes with builders and value equality.
- Mutable scene nodes and Resource Descriptions remain intentionally mutable
  through their controlled methods; immutability is not applied mechanically.
- Defensively copy caller-provided arrays and collections unless an explicitly
  documented ownership-transfer interface exists.
- Use records only for genuinely closed scalar tuples, not configuration or
  domain types expected to evolve.
- Use `Optional<T>` only when absence is a meaningful return value. Do not use
  it for parameters.
- A new public type must hide meaningful complexity or represent necessary
  domain vocabulary. Do not publish pass-through wrappers.
- A public feature includes interface-level tests, Javadoc, and its focused
  runnable Feature Example in the same coherent change.

## Interface compatibility

After the first published release establishes a comparison baseline, Revapi or
an equivalent checker compares supported exported interfaces against the latest
release. Unexported `internal` packages are excluded using the checked-in Revapi
configuration. Supported elements must never be excluded merely to make a
compatibility check pass.

Patch releases fail verification on source or binary incompatibility. Pre-1.0
minor releases may approve breaking changes explicitly, with migration notes and
prior deprecation where practical.

## Testing

- Use JUnit Jupiter and AssertJ.
- Do not adopt a mocking framework by default. Prefer real values and
  deterministic fakes at established seams.
- Test the scene graph, transforms, validation, and Resource Descriptions
  headlessly through their public interfaces.
- Never use arbitrary sleeps. Use deterministic coordination when concurrency
  eventually requires it.
- Keep an exception assertion's executable lambda to one invocation that may
  throw. Construct inputs and callbacks before the assertion.
- Compile every Feature Example during ordinary verification.
- Run context-dependent OpenGL tests with:

  ```shell
  ./mvnw clean verify -Prender-integration
  ```

- OpenGL integration tests use hidden contexts, deterministic framebuffer
  rendering and pixel readback, repeated create/close cycles, and resource-leak
  assertions.
- Run the rendering profile locally for renderer changes and across every
  Provisional Platform during qualification.
- Produce JaCoCo coverage reports without imposing a repository-wide percentage
  gate in version 0.1. Require explicit branch coverage for hierarchy-cycle
  rejection, lifecycle transitions, public validation, and renderer cleanup.
- Keep JMH benchmarks in a separate profile. Benchmarks do not run during
  ordinary verification.

## Dependencies and diagnostics

- `jscene3d-core` initially has only JOML and JSpecify as production
  dependencies.
- Because JOML types appear in the supported public interface, the core JPMS
  module requires JOML transitively.
- `jscene3d-lwjgl` depends on core and only the required LWJGL components: LWJGL
  core, GLFW, OpenGL, and STB.
- Exported packages never expose LWJGL types.
- Test, benchmark, and optional integration dependencies do not leak
  transitively.
- A production dependency must provide a capability that is costly or risky to
  implement locally. Do not add utility libraries for clear Java 21
  functionality.
- Do not add application frameworks or a logging facade.
- Use `System.Logger` only for sparse diagnostics and actionable platform
  warnings. Do not automatically log an exception that is returned or thrown to
  the caller.
- Manage dependency and plugin versions centrally. Enforce dependency
  convergence and upper bounds.
- Keep dependency upgrades separate from feature changes.
- Release verification generates a CycloneDX SBOM and performs vulnerability
  and license checks. These network- or database-dependent checks do not run in
  every ordinary local build.

## Failure handling

- Use standard unchecked exceptions for caller contract violations:
  `NullPointerException` for prohibited nulls, `IllegalArgumentException` for
  invalid values or relationships, `IndexOutOfBoundsException` for invalid
  indices, and `IllegalStateException` for closed resources, wrong-thread calls,
  or invalid lifecycle state.
- `JScene3DException` is not prohibited. Introduce a library-specific root only
  if concrete operational failures such as context creation, shader compilation,
  or rendering demonstrate that callers benefit from one shared catch point.
  Caller contract exceptions do not need to inherit from it.
- Focused operational exceptions expose actionable domain information rather
  than merely renaming a standard exception.
- Exception messages identify the relevant object, offending value, required
  relationship, shader stage, or platform error where applicable.
- Preserve causes and native diagnostic logs.
- Catch the narrowest useful exception type. Never catch and discard an
  exception.
- Restore interrupt status when interruption cannot be propagated directly.
- Do not return `null` or partial success after a failure.

## Lifecycle and threading

- Scene objects are not automatically thread-safe. Document thread safety and
  thread affinity on every relevant exported type.
- The caller owns execution threads. JScene3D components do not create
  background threads implicitly.
- Renderer and window operations obey their documented context-owning or main
  thread requirements.
- Every public `close()` is idempotent and terminal. Cleanup occurs at most once,
  repeated close is a no-op, and each closeable type exposes `isClosed()`.
- Operations other than `close()` and `isClosed()` fail with
  `IllegalStateException` after closure.
- Do not depend on finalizers. A `Cleaner` may report a leak but is not the
  normal cleanup path for native or GPU resources.

## Editor configuration

The checked-in VS Code settings:

- Load the Maven build automatically and download dependency sources.
- Use the same checked-in Checkstyle configuration and version as Maven.
- Disable format-on-save so the editor does not compete with Spotless.
- Exclude Maven `target` directories from watching, searching, and Java resource
  discovery.

## Documentation

- Every supported public capability has an automated contract or compatibility
  test.
- Compile caller examples as tests. Derive README snippets from those examples
  so documentation cannot silently drift.
- Keep `CONTEXT.md` focused on domain language and free of implementation
  detail.
- Record only hard-to-reverse, non-obvious decisions with real trade-offs as
  ADRs.
- Internal comments explain non-obvious reasoning, not line-by-line mechanics.

## Change hygiene

- Each change has one coherent purpose.
- Behavior changes include tests and caller documentation in the same change.
- Keep formatting-only changes, dependency upgrades, and unrelated refactors
  separate.
- Permanent commits pass `./mvnw clean verify` and any additional profile
  relevant to the change.
- Use Conventional Commits for permanent commits and pull-request titles. The
  subject form is `<type>(<optional scope>): <description>`.
- Public-interface changes reference an ADR when the decision is hard to reverse
  and non-obvious; otherwise the change explains why an ADR is unnecessary.
- TODOs reference an issue and describe the missing behavior.
- Generated files are reproducible and never edited manually.
- Suppressions are narrow, justified locally, and reviewed. Do not maintain a
  broad exclusion file merely to make verification pass.

## Releases

- Start at `0.1.0` and follow Semantic Versioning.
- Version all published JScene3D artifacts in lockstep.
- Preserve source and binary compatibility in `0.x` patch releases.
- Never replace or mutate a published version.
- Release bundles include sources, Javadoc, required Maven metadata, checksums,
  GPG signatures, license information, and the release SBOM.
- Declare the project license as Apache License 2.0 (`Apache-2.0`) and verify it
  during release builds.
- Publish pre-1.0 deployments to Maven Central with manual Portal approval.
- Run `./mvnw clean deploy -Prelease` from the release commit.
- Create an immutable signed tag such as `v0.1.0` for each release.
