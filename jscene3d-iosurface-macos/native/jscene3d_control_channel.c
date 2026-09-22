#include "jscene3d_control_channel.h"

#include <IOSurface/IOSurface.h>
#include <mach/mach.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#define JSCENE3D_SURFACE_MESSAGE_ID \
        ((mach_msg_id_t) 'jsrf')

#define JSCENE3D_CONTROL_MESSAGE_BUFFER_SIZE 1024

typedef struct {
    mach_msg_header_t header;
    mach_msg_body_t body;
    mach_msg_port_descriptor_t surface_port;
    int32_t width;
    int32_t height;
} jscene3d_surface_message_t;

static mach_port_t control_port =
        MACH_PORT_NULL;

void jscene3d_control_channel_set(
        mach_port_t receive_port) {
    jscene3d_control_channel_close();

    control_port =
            receive_port;
}

mach_port_t jscene3d_control_channel_get(void) {
    return control_port;
}

IOSurfaceRef
jscene3d_control_channel_receive_surface(
        int *width,
        int *height) {
    if (control_port == MACH_PORT_NULL) {
        return NULL;
    }

    uint8_t buffer[
            JSCENE3D_CONTROL_MESSAGE_BUFFER_SIZE];

    memset(
            buffer,
            0,
            sizeof(buffer));

    mach_msg_header_t *header =
            (mach_msg_header_t *) buffer;

    mach_msg_return_t result =
            mach_msg(
                    header,
                    MACH_RCV_MSG,
                    0,
                    sizeof(buffer),
                    control_port,
                    MACH_MSG_TIMEOUT_NONE,
                    MACH_PORT_NULL);

    if (result != MACH_MSG_SUCCESS) {
        fprintf(
                stderr,
                "JScene3D control receive failed: "
                "0x%x\n",
                result);
        return NULL;
    }

    if (header->msgh_size
            < sizeof(jscene3d_surface_message_t)) {
        fprintf(
                stderr,
                "JScene3D control message too small: "
                "%u\n",
                header->msgh_size);

        mach_msg_destroy(header);
        return NULL;
    }

    jscene3d_surface_message_t *message =
            (jscene3d_surface_message_t *) buffer;

    if (message->header.msgh_id
            != JSCENE3D_SURFACE_MESSAGE_ID) {
        fprintf(
                stderr,
                "Unexpected JScene3D control "
                "message ID: %d\n",
                message->header.msgh_id);

        mach_msg_destroy(
                &message->header);

        return NULL;
    }

    if ((message->header.msgh_bits
                    & MACH_MSGH_BITS_COMPLEX) == 0
            || message->body.msgh_descriptor_count != 1
            || message->surface_port.type
                    != MACH_MSG_PORT_DESCRIPTOR) {
        fprintf(
                stderr,
                "Invalid JScene3D surface message\n");

        mach_msg_destroy(
                &message->header);

        return NULL;
    }

    if (message->surface_port.disposition
                != MACH_MSG_TYPE_PORT_SEND
            && message->surface_port.disposition
                != MACH_MSG_TYPE_PORT_SEND_ONCE) {
        fprintf(
                stderr,
                "Unexpected JScene3D surface "
                "port disposition: %u\n",
                message->surface_port.disposition);

        mach_msg_destroy(
                &message->header);

        return NULL;
    }

    mach_port_t surface_port =
            message->surface_port.name;

    message->surface_port.name =
            MACH_PORT_NULL;

    if (width != NULL) {
        *width =
                message->width;
    }

    if (height != NULL) {
        *height =
                message->height;
    }

    IOSurfaceRef surface =
            IOSurfaceLookupFromMachPort(
                    surface_port);

    mach_port_deallocate(
            mach_task_self(),
            surface_port);

    mach_msg_destroy(
            &message->header);

    if (surface == NULL) {
        fprintf(
                stderr,
                "Control-channel "
                "IOSurfaceLookupFromMachPort "
                "failed\n");
        return NULL;
    }

    printf(
            "JNI received control-channel "
            "IOSurface: %zux%zu\n",
            IOSurfaceGetWidth(surface),
            IOSurfaceGetHeight(surface));
    fflush(stdout);

    return surface;
}

void jscene3d_control_channel_close(void) {
    if (control_port == MACH_PORT_NULL) {
        return;
    }

    mach_port_mod_refs(
            mach_task_self(),
            control_port,
            MACH_PORT_RIGHT_RECEIVE,
            -1);

    control_port =
            MACH_PORT_NULL;
}