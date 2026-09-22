#ifndef JSCENE3D_RENDEZVOUS_H
#define JSCENE3D_RENDEZVOUS_H

#include <mach/mach.h>

typedef struct {
    mach_port_t surface_port;
    mach_port_t control_port;
} jscene3d_rendezvous_ports_t;

jscene3d_rendezvous_ports_t
jscene3d_rendezvous_acquire(
        const char *parent_bundle_id);

#endif