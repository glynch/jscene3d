# Build exports from an assembled application directory

JScene3D will assemble one relocatable target-platform application directory
containing relative launchers, application and engine JARs, native dependencies,
authored project data, and published imported content. Platform-native bundles,
installers, and distribution archives consume that directory and add their
runtime, launcher metadata, signing, and container instead of independently
selecting game content. The first sequence is application directory, macOS
application bundle, and DMG; other desktop platforms follow the same rule on
their respective build hosts, while mobile platforms require dedicated hosts.
A conventional executable JAR and a self-extracting restart bootstrap are not
supported because native-library and runtime setup must be complete before the
generic project launcher starts.
