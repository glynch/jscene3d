#ifndef JSCENE3D_CONTROL_CHANNEL_H
#define JSCENE3D_CONTROL_CHANNEL_H

#include <IOSurface/IOSurface.h>
#include <mach/mach.h>

void jscene3d_control_channel_set(
        mach_port_t receive_port);

mach_port_t jscene3d_control_channel_get(void);

IOSurfaceRef
jscene3d_control_channel_receive_surface(
        int *width,
        int *height);

void jscene3d_control_channel_close(void);

#endif