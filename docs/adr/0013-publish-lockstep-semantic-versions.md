# Publish lockstep semantic versions to Maven Central

JScene3D begins at `0.1.0` and publishes all caller-facing artifacts under
`io.github.glynch` to Maven Central with one lockstep version. Pre-1.0 patch
releases preserve source and binary compatibility; explicitly approved breaking
changes are reserved for minor releases and include migration notes and prior
deprecation where practical. Version `1.0.0` begins the normal stable SemVer
compatibility promise.

Release bundles include source and Javadoc JARs, required metadata, checksums,
GPG signatures, and supply-chain reports. Each immutable published version has
a signed Git tag, and pre-1.0 deployments require manual approval in the Central
Publisher Portal. Lockstep releases may republish an unchanged artifact when
only its peer changes, but users receive one coherent compatibility coordinate
and avoid cross-artifact version matrices during early development.
