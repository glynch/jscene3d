#include "jscene3d_iosurface.h"

#include <CoreFoundation/CoreFoundation.h>
#include <OpenGL/CGLCurrent.h>
#include <OpenGL/CGLIOSurface.h>
#include <OpenGL/gl.h>
#include <OpenGL/glext.h>
#include <stdint.h>
#include <stdio.h>

IOSurfaceRef jscene3d_iosurface_from_mach_port(
        mach_port_t surface_port) {
    if (surface_port == MACH_PORT_NULL) {
        return NULL;
    }

    fprintf(
            stderr,
            "JNI received IOSurface Mach port: %u\n",
            surface_port);
    fflush(stderr);

    IOSurfaceRef surface =
            IOSurfaceLookupFromMachPort(
                    surface_port);

    mach_port_deallocate(
            mach_task_self(),
            surface_port);

    if (surface == NULL) {
        fprintf(
                stderr,
                "IOSurfaceLookupFromMachPort failed\n");
        return NULL;
    }

    fprintf(
            stderr,
            "JNI IOSurface lookup succeeded: "
            "%zux%zu\n",
            IOSurfaceGetWidth(surface),
            IOSurfaceGetHeight(surface));
    fflush(stderr);

    return surface;
}

void jscene3d_iosurface_bind_to_texture(
        IOSurfaceRef surface,
        int width,
        int height) {
    if (surface == NULL) {
        return;
    }

    CGLContextObj context =
            CGLGetCurrentContext();

    if (context == NULL) {
        return;
    }

    CGLTexImageIOSurface2D(
            context,
            GL_TEXTURE_RECTANGLE_ARB,
            GL_RGBA,
            width,
            height,
            GL_BGRA,
            GL_UNSIGNED_INT_8_8_8_8_REV,
            surface,
            0);
}

void jscene3d_iosurface_release(
        IOSurfaceRef surface) {
    if (surface != NULL) {
        CFRelease(surface);
    }
}
