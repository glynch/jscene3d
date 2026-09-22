#ifndef JSCENE3D_IOSURFACE_H
#define JSCENE3D_IOSURFACE_H

#include <IOSurface/IOSurface.h>
#include <mach/mach.h>

IOSurfaceRef jscene3d_iosurface_from_mach_port(
        mach_port_t surface_port);

void jscene3d_iosurface_bind_to_texture(
        IOSurfaceRef surface,
        int width,
        int height);

void jscene3d_iosurface_release(
        IOSurfaceRef surface);

#endif