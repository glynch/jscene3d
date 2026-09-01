JScene3D Example Browser — macOS ARM64

Open Terminal in this directory and run:

    ./run.sh

The script verifies the host platform and starts the bundled example browser
with the included Java 21 runtime and the JVM option required by GLFW on
macOS. No system Java installation is required.

If the included runtime is unavailable, the launcher can use an installed
Java 21 or newer runtime and will display a warning. If necessary, download
Temurin 21 for macOS ARM64 from:

    https://adoptium.net/temurin/releases/?version=21&os=mac&arch=aarch64

Licensing and third-party notices are included inside jscene3d-examples.jar.
