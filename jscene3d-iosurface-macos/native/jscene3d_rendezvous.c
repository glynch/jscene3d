#include "jscene3d_rendezvous.h"

#include <mach/mig.h>
#include <servers/bootstrap.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define JSCENE3D_SURFACE_KEY ((uint32_t) 'js3d')
#define JSCENE3D_CONTROL_KEY ((uint32_t) 'jctl')

#define MACH_RENDEZVOUS_REQUEST_ID \
        ((mach_msg_id_t) 'mrzv')

#define MACH_RENDEZVOUS_RESPONSE_ID \
        ((mach_msg_id_t) 'MRZV')

#define MAX_RENDEZVOUS_PORTS 8

static size_t align_up(
        size_t value,
        size_t alignment) {
    return (value + alignment - 1)
            & ~(alignment - 1);
}

static size_t response_size(
        size_t port_count) {
    return align_up(
            sizeof(mach_msg_base_t)
                    + port_count
                            * sizeof(
                                    mach_msg_port_descriptor_t)
                    + port_count
                            * sizeof(uint32_t)
                    + sizeof(uint64_t),
            sizeof(uint32_t));
}

static void release_received_port(
        mach_port_t name,
        mach_msg_type_name_t disposition) {
    mach_port_right_t right;

    switch (disposition) {
        case MACH_MSG_TYPE_PORT_RECEIVE:
            right = MACH_PORT_RIGHT_RECEIVE;
            break;

        case MACH_MSG_TYPE_PORT_SEND:
            right = MACH_PORT_RIGHT_SEND;
            break;

        case MACH_MSG_TYPE_PORT_SEND_ONCE:
            right = MACH_PORT_RIGHT_SEND_ONCE;
            break;

        default:
            return;
    }

    mach_port_mod_refs(
            mach_task_self(),
            name,
            right,
            -1);
}

jscene3d_rendezvous_ports_t
jscene3d_rendezvous_acquire(
        const char *parent_bundle_id) {
    jscene3d_rendezvous_ports_t result = {
            MACH_PORT_NULL,
            MACH_PORT_NULL
    };

    char bootstrap_name[1024];

    int length = snprintf(
            bootstrap_name,
            sizeof(bootstrap_name),
            "%s.MachPortRendezvousServer.%d",
            parent_bundle_id,
            getppid());

    if (length < 0
            || (size_t) length
                    >= sizeof(bootstrap_name)) {
        fprintf(
                stderr,
                "Unable to construct rendezvous "
                "bootstrap name\n");
        return result;
    }

    printf(
            "JNI bootstrap name: %s\n",
            bootstrap_name);
    fflush(stdout);

    mach_port_t server_port =
            MACH_PORT_NULL;

    kern_return_t kr =
            bootstrap_look_up(
                    bootstrap_port,
                    bootstrap_name,
                    &server_port);

    if (kr != KERN_SUCCESS) {
        fprintf(
                stderr,
                "bootstrap_look_up failed: %s (%d)\n",
                mach_error_string(kr),
                kr);
        return result;
    }

    const size_t buffer_size =
            response_size(
                    MAX_RENDEZVOUS_PORTS)
                    + sizeof(
                            mach_msg_audit_trailer_t);

    uint8_t *buffer =
            calloc(
                    1,
                    buffer_size);

    if (buffer == NULL) {
        fprintf(
                stderr,
                "Unable to allocate rendezvous "
                "message buffer\n");

        mach_port_deallocate(
                mach_task_self(),
                server_port);

        return result;
    }

    mach_msg_base_t *message =
            (mach_msg_base_t *) buffer;

    message->header.msgh_bits =
            MACH_MSGH_BITS(
                    MACH_MSG_TYPE_MOVE_SEND,
                    MACH_MSG_TYPE_MAKE_SEND_ONCE);

    message->header.msgh_size =
            sizeof(mach_msg_header_t);

    message->header.msgh_remote_port =
            server_port;

    message->header.msgh_local_port =
            mig_get_reply_port();

    message->header.msgh_id =
            MACH_RENDEZVOUS_REQUEST_ID;

    mach_port_t reply_port =
            message->header.msgh_local_port;

    mach_msg_option_t options =
            MACH_SEND_MSG
                    | MACH_RCV_MSG
                    | MACH_RCV_TRAILER_TYPE(
                            MACH_MSG_TRAILER_FORMAT_0)
                    | MACH_RCV_TRAILER_ELEMENTS(
                            MACH_RCV_TRAILER_AUDIT);

    mach_msg_return_t mr =
            mach_msg(
                    &message->header,
                    options,
                    message->header.msgh_size,
                    (mach_msg_size_t) buffer_size,
                    reply_port,
                    MACH_MSG_TIMEOUT_NONE,
                    MACH_PORT_NULL);

    if (mr != MACH_MSG_SUCCESS) {
        fprintf(
                stderr,
                "mach_msg rendezvous failed: "
                "%s (0x%x)\n",
                mach_error_string(mr),
                mr);

        free(buffer);
        return result;
    }

    if (message->header.msgh_id
            == MACH_NOTIFY_SEND_ONCE) {
        fprintf(
                stderr,
                "No rendezvous ports registered "
                "for Java process\n");

        free(buffer);
        return result;
    }

    if (message->header.msgh_id
            != MACH_RENDEZVOUS_RESPONSE_ID) {
        fprintf(
                stderr,
                "Unexpected rendezvous response "
                "ID: %d\n",
                message->header.msgh_id);

        mach_msg_destroy(
                &message->header);

        free(buffer);
        return result;
    }

    if ((message->header.msgh_bits
            & MACH_MSGH_BITS_COMPLEX) == 0) {
        fprintf(
                stderr,
                "Rendezvous response is not a "
                "complex Mach message\n");

        free(buffer);
        return result;
    }

    size_t port_count =
            message->body
                    .msgh_descriptor_count;

    if (port_count > MAX_RENDEZVOUS_PORTS) {
        fprintf(
                stderr,
                "Too many rendezvous ports: %zu\n",
                port_count);

        mach_msg_destroy(
                &message->header);

        free(buffer);
        return result;
    }

    uint8_t *cursor =
            buffer
                    + sizeof(
                            mach_msg_base_t);

    mach_msg_port_descriptor_t *descriptors =
            (mach_msg_port_descriptor_t *)
                    cursor;

    cursor +=
            port_count
                    * sizeof(
                            mach_msg_port_descriptor_t);

    uint32_t *keys =
            (uint32_t *) cursor;

    for (size_t i = 0;
         i < port_count;
         ++i) {
        mach_msg_port_descriptor_t *descriptor =
                &descriptors[i];

        if (keys[i] == JSCENE3D_SURFACE_KEY) {
            if (descriptor->disposition
                        != MACH_MSG_TYPE_PORT_SEND
                    && descriptor->disposition
                        != MACH_MSG_TYPE_PORT_SEND_ONCE) {
                fprintf(
                        stderr,
                        "JScene3D surface port has "
                        "unexpected disposition: %u\n",
                        descriptor->disposition);
                continue;
            }

            result.surface_port =
                    descriptor->name;

            descriptor->name =
                    MACH_PORT_NULL;

            continue;
        }

        if (keys[i] == JSCENE3D_CONTROL_KEY) {
            if (descriptor->disposition
                    != MACH_MSG_TYPE_PORT_RECEIVE) {
                fprintf(
                        stderr,
                        "JScene3D control port has "
                        "unexpected disposition: %u\n",
                        descriptor->disposition);
                continue;
            }

            result.control_port =
                    descriptor->name;

            descriptor->name =
                    MACH_PORT_NULL;

            continue;
        }

        release_received_port(
                descriptor->name,
                descriptor->disposition);

        descriptor->name =
                MACH_PORT_NULL;
    }

    mach_msg_destroy(
            &message->header);

    free(buffer);

    return result;
}