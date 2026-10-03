/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocol;

/** Validated command received from one owning native renderer session. */
sealed interface RendererCommand {
    static RendererCommand parse(String line) throws RendererProtocolException {
        String[] parts = line.split(" ", -1);
        if (parts.length == 0 || parts[0].isEmpty()) {
            throw RendererProtocolException.malformed("Command must not be blank");
        }
        return switch (parts[0]) {
            case RendererProtocol.COMMAND_FRAME -> parseFrame(parts);
            case RendererProtocol.COMMAND_RECEIVE_SURFACE -> noArguments(parts, new ReceiveSurface());
            case RendererProtocol.COMMAND_PAUSE -> noArguments(parts, new Pause());
            case RendererProtocol.COMMAND_RESUME -> noArguments(parts, new Resume());
            case RendererProtocol.COMMAND_SHUTDOWN -> noArguments(parts, new Shutdown());
            case RendererProtocol.COMMAND_QUIT -> noArguments(parts, new Quit());
            case RendererProtocol.COMMAND_DRAG -> parseDrag(parts);
            case RendererProtocol.COMMAND_SCENE_SNAPSHOT -> parseSceneSnapshot(parts);
            default -> throw RendererProtocolException.unknown("Unknown renderer command: " + parts[0]);
        };
    }

    private static Frame parseFrame(String[] parts) throws RendererProtocolException {
        if (parts.length != 2 || parts[1].isEmpty() || !parts[1].chars().allMatch(Character::isDigit)) {
            throw RendererProtocolException.malformed("FRAME requires one unsigned request token");
        }
        return new Frame(parts[1]);
    }

    private static Drag parseDrag(String[] parts) throws RendererProtocolException {
        if (parts.length != 3) {
            throw RendererProtocolException.malformed("DRAG requires two finite numbers");
        }
        try {
            float horizontal = Float.parseFloat(parts[1]);
            float vertical = Float.parseFloat(parts[2]);
            if (!Float.isFinite(horizontal) || !Float.isFinite(vertical)) {
                throw RendererProtocolException.malformed("DRAG requires two finite numbers");
            }
            return new Drag(horizontal, vertical);
        } catch (NumberFormatException exception) {
            throw RendererProtocolException.malformed("DRAG requires two finite numbers", exception);
        }
    }

    private static SceneSnapshot parseSceneSnapshot(String[] parts) throws RendererProtocolException {
        if (parts.length != 2 || parts[1].isEmpty()) {
            throw RendererProtocolException.malformed("SCENE_SNAPSHOT requires one encoded snapshot");
        }
        return new SceneSnapshot(parts[1]);
    }

    private static <T extends RendererCommand> T noArguments(String[] parts, T command)
            throws RendererProtocolException {
        if (parts.length != 1) {
            throw RendererProtocolException.malformed(parts[0] + " does not accept arguments");
        }
        return command;
    }

    record Frame(String requestToken) implements RendererCommand {}

    record Drag(float horizontal, float vertical) implements RendererCommand {}

    record SceneSnapshot(String encodedSnapshot) implements RendererCommand {}

    record ReceiveSurface() implements RendererCommand {}

    record Pause() implements RendererCommand {}

    record Resume() implements RendererCommand {}

    record Shutdown() implements RendererCommand {}

    record Quit() implements RendererCommand {}
}
