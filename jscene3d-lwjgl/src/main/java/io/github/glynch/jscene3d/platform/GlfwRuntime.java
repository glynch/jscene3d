/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

/** Process-wide GLFW ownership shared by open JScene3D windows. */
final class GlfwRuntime {
    private static int referenceCount;
    private static @Nullable Thread ownerThread;
    private static @Nullable GLFWErrorCallback errorCallback;
    private static @Nullable GLFWErrorCallback previousErrorCallback;
    private static @Nullable String lastError;

    /** Prevents instantiation of this process-wide runtime manager. */
    private GlfwRuntime() {}

    /** Acquires one window reference and initializes GLFW for the first reference. */
    static synchronized void acquire() {
        Thread currentThread = Thread.currentThread();
        if (referenceCount > 0) {
            requireOwnerThread(currentThread);
            referenceCount++;
            return;
        }

        ownerThread = currentThread;
        lastError = null;
        errorCallback = GLFWErrorCallback.create((errorCode, descriptionAddress) -> lastError = "GLFW error 0x"
                + Integer.toHexString(errorCode)
                + ": "
                + GLFWErrorCallback.getDescription(descriptionAddress));
        previousErrorCallback = GLFW.glfwSetErrorCallback(errorCallback);

        if (!GLFW.glfwInit()) {
            String message = failureMessage("Could not initialize GLFW");
            restoreErrorCallback();
            ownerThread = null;
            throw new IllegalStateException(message);
        }

        referenceCount = 1;
    }

    /** Releases one window reference and terminates GLFW after the final reference. */
    static synchronized void release() {
        requireActiveOwnerThread();
        referenceCount--;
        if (referenceCount != 0) {
            return;
        }

        GLFW.glfwTerminate();
        restoreErrorCallback();
        ownerThread = null;
        lastError = null;
    }

    /** Requires an active GLFW runtime owned by the calling thread. */
    static synchronized void requireActiveOwnerThread() {
        if (referenceCount == 0) {
            throw new IllegalStateException("No open JScene3D windows");
        }
        requireOwnerThread(Thread.currentThread());
    }

    /** Clears the most recently captured GLFW error after validating thread ownership. */
    static synchronized void clearLastError() {
        requireActiveOwnerThread();
        lastError = null;
    }

    /** Combines an operation description with the most recently captured GLFW error. */
    static synchronized String failureMessage(String operation) {
        return lastError == null ? operation : operation + ": " + lastError;
    }

    /** Requires the supplied thread to own the active process-wide runtime. */
    private static void requireOwnerThread(Thread currentThread) {
        if (!currentThread.equals(ownerThread)) {
            throw new IllegalStateException("Window operations must run on the creating thread");
        }
    }

    /** Restores the previous GLFW error callback and frees the runtime callback. */
    private static void restoreErrorCallback() {
        GLFW.glfwSetErrorCallback(previousErrorCallback);
        previousErrorCallback = null;
        GLFWErrorCallback callbackToFree = errorCallback;
        errorCallback = null;
        if (callbackToFree != null) {
            callbackToFree.free();
        }
    }
}
