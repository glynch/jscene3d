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

    private GlfwRuntime() {}

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

    static synchronized void requireActiveOwnerThread() {
        if (referenceCount == 0) {
            throw new IllegalStateException("No open JScene3D windows");
        }
        requireOwnerThread(Thread.currentThread());
    }

    static synchronized void clearLastError() {
        requireActiveOwnerThread();
        lastError = null;
    }

    static synchronized String failureMessage(String operation) {
        return lastError == null ? operation : operation + ": " + lastError;
    }

    private static void requireOwnerThread(Thread currentThread) {
        if (!currentThread.equals(ownerThread)) {
            throw new IllegalStateException("Window operations must run on the creating thread");
        }
    }

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
