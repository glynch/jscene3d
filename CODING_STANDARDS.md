# JScene3D Coding Standards

These standards apply repository-wide. They describe current engineering
rules and the verification that enforces them.

## Terminology

Use architecture vocabulary consistently:

- **Component** means an entity-composition unit in the project and runtime
  model. Use class, package, subsystem, or service for software structure when
  that is what is meant.
- **Maven module** means one reactor project with its own `pom.xml`.
- **Artifact** means a separately built or published Maven coordinate.
- **JPMS module** means a unit declared by `module-info.java`.
- Qualify the word **module** as Maven or JPMS whenever the distinction is not
  already unambiguous.

The [manual](docs/manual/README.md) defines user-facing concepts, the
[design documents](docs/design/) define current architecture, and the
[architecture decision records](docs/adr/) record durable decisions and their
trade-offs. Do not create a parallel context or handover document as another
terminology authority.

## Mandatory verification

Use focused verification while developing:

- run the tests and static checks for each affected Maven module;
- compile or test direct consumers when a public or cross-module contract
  changes;
- run the relevant integration profile for context-dependent behavior; and
- run Markdownlint and `git diff --check` for documentation-only changes.

Do not run the complete reactor after every small edit merely as an iteration
step. Before work is considered permanent or release-ready, however, the
repository acceptance command is:

```shell
./mvnw clean verify
```

Keep `clean` in this acceptance command. It prevents stale classes, generated
sources, resources, reports, and test output in `target` directories from
making verification pass accidentally.

The ordinary lifecycle checks formatting and reports violations; it does not
rewrite source. Run `./mvnw spotless:apply` explicitly when formatting is
required. Renderer changes additionally use the `render-integration` profile
described under Testing.

## Java source and formatting

- Target Java 21 and use stable features only. Production and test code must
  not require `--enable-preview`.
- Use UTF-8 explicitly for source, resources, reports, and runtime text
  conversion.
- Write generated JSON documents with deterministic, human-readable
  indentation and a terminating newline. Compact JSON is appropriate only for
  an external protocol or a measured performance requirement.
- Spotless with the pinned Palantir Java Format version is the sole mechanical
  Java formatter.
- Spotless removes unused imports. Checkstyle forbids wildcard imports and
  fully qualified Java type references where an import should be used.
- Checkstyle enforces semantic source rules and does not duplicate formatter
  whitespace, import-order, or line-length behavior.
- Do not leave public or protected constructors or methods empty. Perform the
  required initialization, omit the member, or document and implement an
  intentional contract without an empty body.
- Keep methods and constructors at or below seven parameters. Introduce a
  cohesive parameter object or redesign the interface when related inputs
  would otherwise exceed that limit.
- Keep production methods at or below a cognitive complexity of 15. Extract
  cohesive validation or behavior instead of suppressing the finding.
- Do not declare arrays as record components. Records compare arrays by
  identity rather than content. Use an immutable class with defensive copies
  and deliberate equality semantics.
- Use `Math.clamp` instead of nested `Math.min` and `Math.max` expressions.
- Promote at least one operand before integral arithmetic whose result is
  consumed as `float`, `double`, or `long`. Casting the completed result is too
  late to prevent overflow or truncation.
- Centralize repeated domain-unit and coordinate conversions in a focused
  type so callers cannot accidentally perform arithmetic in the source unit.
- Original source files use this short license header with the appropriate
  comment syntax:

  ```text
  Copyright 2026 Graham Lynch
  SPDX-License-Identifier: Apache-2.0
  ```

## Nullness and compiler analysis

- Use JSpecify for Java nullness annotations.
- Mark packages `@NullMarked` when they participate in strict nullness
  analysis. New production packages should opt in unless an existing boundary
  makes that impossible and the exception is documented.
- NullAway runs in JSpecify mode with `OnlyNullMarked=true`; do not assume it
  analyses an unmarked package.
- Error Prone and NullAway error diagnostics fail compilation.
- The compiler requests `-Xlint:all` except `classfile`. Maven currently hides
  ordinary javac warning output because resolved third-party bytecode produces
  warnings without a sufficiently narrow suppression. Error Prone, NullAway,
  Checkstyle, PMD, SpotBugs, and Forbidden APIs remain visible and
  build-enforced.
- Runtime validation remains mandatory at public boundaries despite static
  nullness analysis.
- Suppressions must be narrow, local, and accompanied by a reason.

Do not describe editor-only analysis as authoritative. The Maven compiler
configuration is the repository's nullness and compiler-analysis contract.

## Naming

- Value accessors use concise noun names such as `position()`, `parent()`, and
  `status()`, not JavaBeans `get...()` names.
- Mutators use explicit verbs such as `setPosition(...)`. Meaningful
  operations use domain verbs such as `add(...)`, `remove(...)`, and
  `detach()`.
- Boolean predicates use `is...()` where grammatically appropriate.
- Use `of(...)` to compose existing values, `from(...)` for conversion, and
  `load(...)` for I/O. Prefer a descriptive factory when `of(...)` would hide
  intent.
- Builders use `builder()` and `toBuilder()`. Builder methods use noun names.
- Do not add builders or fluent chaining mechanically; use them only when they
  make genuinely complex construction clearer.
- Avoid boolean parameters and overloads whose meaning is unclear at the call
  site. Prefer named value types, enums, or builders.
- Use `Path`, `URI`, `Duration`, `Instant`, and other domain-appropriate Java
  types instead of string or primitive substitutes.
- Treat acronyms as Java words in identifiers: `GltfLoader`, `OpenGlRenderer`,
  and `LwjglWindow`, not `GLTFLoader`, `OpenGLRenderer`, or `LWJGLWindow`.
- Avoid vague `Util`, `Common`, `Manager`, `Service`, or `Core` containers. A
  qualified manager or service name is acceptable when it represents a real
  lifecycle or architectural boundary rather than miscellaneous operations.
- Do not bury a generally reusable operation in a private helper merely
  because it currently has one caller. Put generic argument checks in a
  focused internal `Preconditions` type; keep class-specific invariant
  validation and behavior in the owning class.
- Each production artifact or cohesive package family owns its internal
  precondition policy. Reuse that policy instead of duplicating generic checks
  in public model classes.

## Static verification

The ordinary `clean verify` lifecycle currently includes:

- Spotless formatting checks during `validate` using Palantir Java Format;
- Maven Enforcer checks for Java and Maven versions, pinned plugin versions,
  duplicate dependency versions, dependency and reactor convergence, and
  upper dependency bounds;
- Java compilation with Error Prone and NullAway;
- JUnit tests on the JPMS module path;
- Checkstyle over production and test source;
- PMD cognitive-complexity analysis over production source;
- high-confidence SpotBugs analysis;
- Forbidden APIs checks over production and test bytecode;
- JaCoCo reports and the coverage checks configured by each artifact;
- strict public and protected Javadoc with all doclint checks enabled; and
- Markdownlint through the `jscene3d-documentation` Maven module.

Tool and plugin versions are pinned centrally in the root `pom.xml`; the
Markdown toolchain is pinned in `pom.xml`, `package.json`, and the lockfile.
Do not duplicate version numbers here.

The project-specific Forbidden APIs signatures require explicit character
sets and prohibit JScene3D code from constructing threads directly. Callers or
explicit host boundaries own execution threads.

SBOM generation, vulnerability scanning, license scanning, API-baseline
comparison, signing, and deployment are not currently bound to ordinary
`verify`. Do not describe planned or inactive tools as build-enforced.

## Packages and JPMS

- General-purpose Java artifacts declare genuine JPMS modules. The
  `jscene3d-documentation` POM and the platform-specific
  `jscene3d-iosurface-macos` proof adapter are current explicit exceptions.
- Follow the `io.github.glynch.jscene3d...` naming family for JPMS modules and
  Java packages.
- Establish responsibility-based subpackages when introducing a Maven module.
  Do not collect unrelated interfaces, coordinators, adapters, launchers,
  parsers, persistence, and UI types in one root package.
- A root package contains only `package-info.java` and types that genuinely
  form the artifact's central interface. Tests mirror the responsibility
  packages they exercise.
- Export only intentional caller packages. Keep implementation in unexported
  `.internal` packages and prefer package-private implementation types.
- Use `requires transitive` only when an exported public or protected API
  exposes the required module. Keep implementation dependencies
  non-transitive. Use `requires static` for compile-time-only annotation
  dependencies such as JSpecify.
- Never create split packages across artifacts.
- Forbid package, Maven-artifact, and JPMS dependency cycles.
- Avoid broad `opens`; qualify reflective access narrowly when unavoidable.
- Protect important exports, non-exports, and transitivity decisions with
  focused module-descriptor tests.
- Do not introduce an `@InternalApi` escape hatch unless cross-artifact
  implementation collaboration genuinely requires technical accessibility.

The current Maven-module responsibilities belong in the
[module overview](docs/design/module-overview.md), not in this standards file.

## Public interfaces and values

- Every public or protected element in an exported package is supported caller
  interface.
- Classes are final unless inheritance is an intentional, documented extension
  point. Scene abstractions such as `Object3D`, `Camera`, and `Material` are
  deliberate exceptions.
- Every exported type and public or protected member has Javadoc covering
  applicable invariants, lifecycle, thread rules, ownership, failures, and
  performance behavior.
- Package-private production types, constructors, and methods have concise
  Javadoc when they carry a non-obvious internal contract. Private helpers
  need Javadoc only when their name and signature do not explain that contract.
- Configuration types such as `WindowOptions` and `RendererOptions` are final,
  immutable values with builders and value equality.
- Mutable scene nodes and resource descriptions remain intentionally mutable
  through controlled methods; do not apply immutability mechanically.
- Defensively copy caller-provided arrays and collections unless a documented
  ownership-transfer interface exists.
- Use records for genuinely closed value aggregates with stable components,
  not configuration or domain types expected to grow incompatibly.
- Use `Optional<T>` only when absence is a meaningful return value. Do not use
  it for parameters.
- A new public type must hide meaningful complexity or represent necessary
  domain vocabulary. Do not publish pass-through wrappers.
- A public capability includes focused interface-level tests and Javadoc.
  Add or update a runnable example when visual or interactive behavior is part
  of the capability; supporting DTOs and internal plumbing do not each require
  an example.

## Interface compatibility

JScene3D is pre-1.0. APIs, project formats, editor behavior, and Maven or JPMS
module boundaries may change without backward compatibility before 1.0.

Breaking changes must still be deliberate and coherent: update affected
callers, tests, examples, schemas, and documentation in the same change, and
provide migration guidance when external users are known to depend on the old
contract. Do not preserve an obsolete API merely to imply a compatibility
promise that the project has not made.

The repository contains `config/revapi/analysis.xml`, but no Revapi plugin is
currently bound to the Maven lifecycle. `clean verify` therefore does not
compare the exported API with a released baseline. Do not claim automated API
compatibility enforcement until such a check is configured and proven.

## Testing

- Use JUnit Jupiter and AssertJ.
- Do not adopt a mocking framework by default. Prefer real values and
  deterministic fakes at established seams.
- Test public behavior headlessly wherever native rendering, audio, or another
  platform context is not essential.
- Never use arbitrary sleeps. Use deterministic coordination for asynchronous
  and concurrent behavior.
- Tests that need temporary files or directories receive a JUnit Jupiter
  `@TempDir Path`. Do not hard-code operating-system temporary paths or create
  unmanaged temporary test locations directly.
- Keep an exception assertion's executable lambda to one invocation that may
  throw. Construct inputs and callbacks before the assertion.
- Keep each test method below 25 assertion invocations. Split broader
  scenarios into focused tests instead of hiding assertions in helpers.
- Compile runnable examples during ordinary verification.
- Run context-dependent OpenGL tests with:

  ```shell
  ./mvnw clean verify -Prender-integration
  ```

- OpenGL integration tests use hidden contexts, deterministic framebuffer
  rendering and pixel readback, repeated create/close cycles, and resource-leak
  assertions.
- Run the rendering profile for renderer changes on the current Verified
  Platform. Other native targets are not verified merely because dependency
  classifiers exist for them.

JaCoCo instruments and reports Java modules through the parent build. Example
artifacts explicitly skip percentage gates, and each library module's POM is
the authority for any non-zero line and branch minimums. The parent defaults
remain zero for modules that have not opted into a floor, so do not describe
coverage enforcement as universal.

`jscene3d-lwjgl` has additional treatment: ordinary verification enforces
per-class floors for its headless control and input core, while the
`render-integration` profile enables a whole-artifact coverage floor using the
native integration execution data. Raise coverage floors as coverage improves;
never lower one merely to make a change pass. Require explicit branch coverage
for hierarchy-cycle rejection, lifecycle transitions, public validation, and
resource cleanup regardless of aggregate percentages.

## Dependencies and diagnostics

- Manage dependency and plugin versions centrally in the root POM. Maven
  Enforcer protects convergence and upper bounds.
- A production dependency must provide a capability that is costly or risky to
  implement locally. Do not add utility libraries for clear Java 21
  functionality.
- Give dependencies the narrowest correct Maven scope and JPMS requirement.
  Test, example, optional integration, and platform-native dependencies do not
  leak transitively without a documented reason.
- A dependency exposed through supported API may require JPMS transitivity;
  implementation-only dependencies must remain non-transitive.
- Keep engine abstractions free of third-party implementation types. In
  particular, exported non-LWJGL APIs must not expose raw LWJGL or OpenGL
  handles.
- Do not add application frameworks or a logging facade without an explicit
  architectural decision.
- Use `System.Logger` for sparse diagnostics and actionable platform warnings.
  Do not automatically log an exception that is returned or thrown to the
  caller.
- Keep dependency upgrades separate from feature changes.

See the [module overview](docs/design/module-overview.md) for the current
dependency responsibilities. Do not reproduce a historical dependency graph
in this file.

## Failure handling

- Use standard unchecked exceptions for caller contract violations:
  `NullPointerException` for prohibited nulls, `IllegalArgumentException` for
  invalid values or relationships, `IndexOutOfBoundsException` for invalid
  indices, and `IllegalStateException` for closed resources, wrong-thread
  calls, or invalid lifecycle state.
- Introduce a library-specific exception hierarchy only when concrete
  operational failures benefit callers through shared handling. Caller
  contract exceptions do not need to inherit from it.
- Focused operational exceptions expose actionable domain information rather
  than merely renaming a standard exception.
- Exception messages identify the relevant object, offending value, required
  relationship, processing stage, or platform error where applicable.
- Preserve causes and native diagnostic logs.
- Catch the narrowest useful exception type. Never catch and discard an
  exception.
- Restore interrupt status when interruption cannot be propagated directly.
- Do not return `null` or partial success after a failure.

## Lifecycle and threading

- Document thread safety and thread affinity on every relevant exported type.
  Scene objects are not automatically thread-safe.
- The caller or an explicit application host owns execution threads. Library
  components do not create background threads implicitly.
- Renderer, window, audio, and native-surface operations obey their documented
  owning-thread or context requirements.
- Document ownership for every closeable value, including whether the caller,
  aggregate, or runtime host closes it.
- Public resource-owning `close()` operations are idempotent and terminal:
  cleanup occurs at most once and repeated close is a no-op.
- Stateful resources reject operations after closure with
  `IllegalStateException`, except for `close()` and any documented lifecycle
  query. Expose `isClosed()` when callers genuinely need to query state; do not
  add it mechanically to one-shot registrations or internal cleanup handles.
- Aggregate shutdown attempts all owned cleanup and preserves meaningful
  failures rather than abandoning later resources after the first exception.
- Do not depend on finalizers. A `Cleaner` may report a leak but is not the
  normal cleanup path for native or GPU resources.

## Editor configuration

The checked-in `.vscode/settings.json` configures the ordinary repository
development workspace. It is not configuration for the Code OSS-based
JScene3D product.

The checked-in settings currently:

- point the Java Checkstyle extension at the repository configuration and
  pinned Checkstyle version;
- disable Java format-on-save so the editor does not compete with Spotless;
- exclude Maven output and log paths from relevant file, watcher, and search
  views; and
- associate the extension-descriptor JSON schema with its test fixture.

Do not claim that editor settings configure Maven import, dependency-source
download, or NullAway unless those settings are checked in. Maven remains the
authoritative verification environment.

## Documentation

Use the current documentation hierarchy:

- `README.md` is the concise GitHub landing page.
- `docs/manual/` contains learning-oriented user and developer manuals.
- `docs/design/` contains current detailed architecture and design.
- `docs/adr/` records hard-to-reverse, non-obvious architectural decisions and
  their trade-offs.
- `CODING_STANDARDS.md` contains repository-wide engineering rules.

Do not duplicate detailed architecture in this file. Link to the appropriate
current document instead.

- Generate public and protected production Javadoc during `clean verify` with
  all doclint checks enabled. Errors and warnings fail the build.
- Keep internal contracts documented in source even when they are not part of
  generated caller Javadoc.
- Keep examples, schemas, commands, and links synchronized with the source
  they describe.
- Markdownlint covers checked-in Markdown during ordinary verification.
- Internal comments explain non-obvious reasoning, not line-by-line mechanics.

## Change hygiene

- Each change has one coherent purpose.
- Behavior changes include tests and caller documentation in the same change.
- Keep formatting-only changes, dependency upgrades, and unrelated refactors
  separate.
- Use focused verification while iterating. Before work becomes permanent or
  release-ready, run `./mvnw clean verify` and every additional profile relevant
  to the change.
- Use Conventional Commits for permanent commits and pull-request titles. The
  subject form is `<type>(<optional scope>): <description>`.
- Reference an ADR for a public-interface decision when it is hard to reverse,
  non-obvious, and carries real trade-offs. Otherwise explain the reasoning in
  the change itself.
- TODOs reference an issue and describe the missing behavior.
- Generated files are reproducible and never edited manually.
- Suppressions are narrow, justified locally, and reviewed. Do not maintain a
  broad exclusion solely to make verification pass.
- Preserve unrelated working-tree changes. Do not reset, restore, clean,
  overwrite, stage, or commit them as part of a focused task.

## Releases

- The repository is currently pre-1.0 and uses `0.1.0-SNAPSHOT` across the
  reactor.
- Use lockstep Semantic Versions for published JScene3D artifacts and never
  replace or mutate a published coordinate.
- Before 1.0, compatibility is not guaranteed across releases. Breaking
  changes remain deliberate and update affected callers and documentation.
- Declare the project license as Apache License 2.0 (`Apache-2.0`) in source and
  Maven metadata.
- Release-only SBOM, vulnerability, license, signing, source/Javadoc bundle,
  Maven Central deployment, and tagging automation are not currently bound to
  the root Maven build. Do not claim or invoke a release profile until that
  workflow exists and has been verified.
