# Open the repository after the Solar System Viewer works

JScene3D remains private during foundation development, with macOS ARM64 on the
maintainer's M1 MacBook Pro as its only Verified Platform. Windows x86-64, Linux
x86-64 under X11 and Wayland, and macOS x86-64 remain Provisional Platforms and
are not advertised as supported merely because LWJGL publishes native binaries.

The repository becomes public once the interactive Solar System Viewer runs
through the public interface and demonstrates hierarchy, cameras, generated
geometry, materials, rendering, window/input behavior, and clean shutdown. At
that Public Preview Gate, standard public GitHub Actions runners qualify the
provisional desktop matrix without consuming the maintainer's private-repository
minute allowance. A platform is promoted to Supported only after deterministic
context creation, rendering, pixel readback, and cleanup tests pass there.

Opening earlier would expose an unproven scaffold; waiting for 0.1 completion
would prevent free cross-platform qualification before the support promises are
made. The working integration example provides a useful, inspectable midpoint
while preserving an evidence-based release matrix.
