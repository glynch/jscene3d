/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.iosurface.macos.codeoss;

import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.opengl.GL11.glGetString;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceBridge;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceDescriptor;
import io.github.glynch.jscene3d.iosurface.macos.IOSurfaceRenderSurface;
import io.github.glynch.jscene3d.materials.NormalMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** One-process Code OSS proof host using the real JScene3D scene and renderer. */
public final class JScene3DCodeOssRenderer {
    private static final float ROTATION_PER_FRAME = 0.01f;
    private static final float DRAG_SENSITIVITY = 0.005f;
    private static final PrintStream OUTPUT =
            new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
    private static final PrintStream DIAGNOSTICS =
            new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);

    private JScene3DCodeOssRenderer() {
        throw new AssertionError("JScene3DCodeOssRenderer cannot be instantiated");
    }

    /**
     * Acquires one IOSurface and runs the existing session command protocol.
     *
     * @param arguments Electron bundle ID, physical width and physical height
     */
    public static void main(String[] arguments) {
        if (arguments.length != 3) {
            throw new IllegalArgumentException("Expected Electron bundle ID, width and height");
        }
        int width = Integer.parseInt(arguments[1]);
        int height = Integer.parseInt(arguments[2]);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Initial surface dimensions must be positive");
        }

        log("host startup pid=" + ProcessHandle.current().pid() + " size=" + width + "x" + height);
        long initialSurface = IOSurfaceBridge.lookup(arguments[0]);
        if (initialSurface == 0L) {
            throw new IllegalStateException("Unable to acquire IOSurface through Mach rendezvous");
        }

        boolean surfaceTransferred = false;
        try (Window window = Window.create(64, 64, "JScene3D Code OSS renderer")) {
            log("OpenGL context=" + glGetString(GL_VERSION));
            IOSurfaceDescriptor initialDescriptor = new IOSurfaceDescriptor(initialSurface, width, height);
            surfaceTransferred = true;
            IOSurfaceRenderSurface surface = new IOSurfaceRenderSurface(initialDescriptor);
            log("IOSurfaceRenderSurface created size=" + width + "x" + height);
            try {
                run(surface, width, height);
            } finally {
                // Renderer.close releases the surface; this also covers partial renderer creation.
                surface.release();
                log("IOSurfaceRenderSurface released");
            }
        } finally {
            if (!surfaceTransferred) {
                IOSurfaceBridge.release(initialSurface);
            }
            log("host process exit");
        }
    }

    private static void run(IOSurfaceRenderSurface surface, int width, int height) {
        try (NormalMaterial material = new NormalMaterial();
                BufferGeometry geometry = BoxGeometry.create(2.0f, 2.0f, 2.0f);
                Renderer renderer = Renderer.create(surface)) {
            log("real Renderer created");
            Scene scene = new Scene();
            scene.setBackground(Color.srgb(0x172033));
            Mesh mesh = new Mesh(geometry, material);
            mesh.rotateX(0.3f);
            mesh.rotateY(0.4f);
            scene.add(mesh);
            PerspectiveCamera camera =
                    new PerspectiveCamera((float) Math.toRadians(60.0), (float) width / height, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            log("real scene created cameraAspect=" + camera.aspectRatio());
            sendEvent(RendererProtocol.EVENT_RENDERER_READY);
            log("RENDERER_READY");
            processCommands(surface, renderer, scene, camera, mesh);
        } finally {
            log("real Renderer and scene resource cleanup");
        }
    }

    private static void processCommands(
            IOSurfaceRenderSurface surface, Renderer renderer, Scene scene, PerspectiveCamera camera, Mesh mesh) {
        long frameCount = 0;
        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (input.hasNextLine()) {
                String command = input.nextLine();
                if (command.startsWith(RendererProtocol.COMMAND_FRAME + " ")) {
                    renderFrame(surface, renderer, scene, camera, mesh);
                    frameCount++;
                    if (frameCount <= 3 || frameCount % 600 == 0) {
                        log("real scene frame presented count=" + frameCount);
                    }
                } else if (command.startsWith(RendererProtocol.COMMAND_DRAG + " ")) {
                    applyDrag(mesh, command);
                } else if (command.equals(RendererProtocol.COMMAND_RECEIVE_SURFACE)) {
                    receiveReplacement(surface, camera);
                } else if (command.equals(RendererProtocol.COMMAND_QUIT)) {
                    log("QUIT received");
                    return;
                }
            }
            log("stdin closed");
        }
    }

    private static void renderFrame(
            IOSurfaceRenderSurface surface, Renderer renderer, Scene scene, PerspectiveCamera camera, Mesh mesh) {
        mesh.rotateY(ROTATION_PER_FRAME);
        renderer.render(scene, camera);
        surface.present();
        glFlush();
        sendEvent(RendererProtocol.EVENT_FRAME_READY);
    }

    private static void applyDrag(Mesh mesh, String command) {
        String[] parts = command.split(" ");
        if (parts.length == 3) {
            mesh.rotateY(Float.parseFloat(parts[1]) * DRAG_SENSITIVITY);
            mesh.rotateX(Float.parseFloat(parts[2]) * DRAG_SENSITIVITY);
        }
    }

    private static void receiveReplacement(IOSurfaceRenderSurface surface, PerspectiveCamera camera) {
        IOSurfaceDescriptor replacement = IOSurfaceBridge.receiveSurface();
        if (replacement == null) {
            log("replacement IOSurface receive failed");
            return;
        }
        surface.replaceSurface(replacement);
        camera.setAspectRatio((float) replacement.width() / replacement.height());
        log("IOSurface replaced size=" + replacement.width() + "x" + replacement.height() + " cameraAspect="
                + camera.aspectRatio());
        sendEvent(RendererProtocol.EVENT_SURFACE_READY + " " + replacement.width() + " " + replacement.height());
    }

    private static void sendEvent(String event) {
        OUTPUT.println(event);
    }

    private static void log(String message) {
        DIAGNOSTICS.println("[JScene3D Java] " + message);
    }
}
