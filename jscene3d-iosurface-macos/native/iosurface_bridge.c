#include <jni.h>

#include <IOSurface/IOSurface.h>
#include <mach/mach.h>
#include <stdint.h>
#include <stdio.h>

#include "jscene3d_control_channel.h"
#include "jscene3d_iosurface.h"
#include "jscene3d_rendezvous.h"

JNIEXPORT jlong JNICALL
Java_io_github_glynch_jscene3d_iosurface_macos_IOSurfaceBridge_lookup(
        JNIEnv *env,
        jclass clazz,
        jstring parentBundleId) {
    if (parentBundleId == NULL) {
        return 0;
    }

    const char *parent_bundle_id =
            (*env)->GetStringUTFChars(
                    env,
                    parentBundleId,
                    NULL);

    if (parent_bundle_id == NULL) {
        return 0;
    }

    jscene3d_rendezvous_ports_t ports =
            jscene3d_rendezvous_acquire(
                    parent_bundle_id);

    (*env)->ReleaseStringUTFChars(
            env,
            parentBundleId,
            parent_bundle_id);

    if (ports.surface_port == MACH_PORT_NULL) {
        if (ports.control_port != MACH_PORT_NULL) {
            mach_port_mod_refs(
                    mach_task_self(),
                    ports.control_port,
                    MACH_PORT_RIGHT_RECEIVE,
                    -1);
        }

        return 0;
    }

    if (ports.control_port == MACH_PORT_NULL) {
        fprintf(
                stderr,
                "JNI did not receive JScene3D "
                "control receive right\n");

        mach_port_deallocate(
                mach_task_self(),
                ports.surface_port);

        return 0;
    }

    jscene3d_control_channel_set(
            ports.control_port);

    fprintf(
            stderr,
            "JNI received JScene3D control "
            "receive right: %u\n",
            ports.control_port);
    fflush(stderr);

    IOSurfaceRef surface =
            jscene3d_iosurface_from_mach_port(
                    ports.surface_port);

    if (surface == NULL) {
        jscene3d_control_channel_close();
        return 0;
    }

    return (jlong) (uintptr_t) surface;
}

JNIEXPORT void JNICALL
Java_io_github_glynch_jscene3d_iosurface_macos_IOSurfaceBridge_bindToTexture(
        JNIEnv *env,
        jclass clazz,
        jlong surfaceHandle,
        jint width,
        jint height) {
    IOSurfaceRef surface =
            (IOSurfaceRef) (uintptr_t)
                    surfaceHandle;

    jscene3d_iosurface_bind_to_texture(
            surface,
            width,
            height);
}

JNIEXPORT jobject JNICALL
Java_io_github_glynch_jscene3d_iosurface_macos_IOSurfaceBridge_receiveSurface(
        JNIEnv *env,
        jclass clazz) {
    int width = 0;
    int height = 0;

    IOSurfaceRef surface =
            jscene3d_control_channel_receive_surface(
                    &width,
                    &height);

    if (surface == NULL) {
        fprintf(
                stderr,
                "JNI failed to receive replacement surface\n");
        return NULL;
    }

    jclass descriptor_class =
            (*env)->FindClass(
                    env,
                    "io/github/glynch/jscene3d/iosurface/macos/IOSurfaceDescriptor");

    if (descriptor_class == NULL) {
        jscene3d_iosurface_release(surface);
        return NULL;
    }

    jmethodID constructor =
            (*env)->GetMethodID(
                    env,
                    descriptor_class,
                    "<init>",
                    "(JII)V");

    if (constructor == NULL) {
        jscene3d_iosurface_release(surface);
        return NULL;
    }

    jobject descriptor =
            (*env)->NewObject(
                    env,
                    descriptor_class,
                    constructor,
                    (jlong) (uintptr_t) surface,
                    (jint) width,
                    (jint) height);

    if (descriptor == NULL) {
        jscene3d_iosurface_release(surface);
        return NULL;
    }

    return descriptor;
}

JNIEXPORT void JNICALL
Java_io_github_glynch_jscene3d_iosurface_macos_IOSurfaceBridge_release(
        JNIEnv *env,
        jclass clazz,
        jlong surfaceHandle) {
    IOSurfaceRef surface =
            (IOSurfaceRef) (uintptr_t)
                    surfaceHandle;

    jscene3d_iosurface_release(
            surface);
}
