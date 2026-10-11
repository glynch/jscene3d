/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocol;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import java.util.Optional;

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
            case RendererProtocol.COMMAND_SCENE_PICK -> parseScenePick(parts);
            case RendererProtocol.COMMAND_SCENE_SELECT -> parseSceneSelect(parts);
            case RendererProtocol.COMMAND_SCENE_NAVIGATE -> parseSceneNavigate(parts);
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

    private static ScenePick parseScenePick(String[] parts) throws RendererProtocolException {
        if (parts.length != 5 || parts[1].isEmpty()) {
            throw RendererProtocolException.malformed("SCENE_PICK requires a token, revision, and two coordinates");
        }
        try {
            long revision = Long.parseLong(parts[2]);
            float horizontal = Float.parseFloat(parts[3]);
            float vertical = Float.parseFloat(parts[4]);
            if (revision < 0
                    || !Float.isFinite(horizontal)
                    || !Float.isFinite(vertical)
                    || horizontal < -1.0f
                    || horizontal > 1.0f
                    || vertical < -1.0f
                    || vertical > 1.0f) {
                throw RendererProtocolException.malformed("SCENE_PICK arguments are outside their valid range");
            }
            return new ScenePick(parts[1], revision, horizontal, vertical);
        } catch (NumberFormatException exception) {
            throw RendererProtocolException.malformed(
                    "SCENE_PICK requires numeric revision and coordinates", exception);
        }
    }

    private static SceneSelect parseSceneSelect(String[] parts) throws RendererProtocolException {
        if (parts.length != 3 || parts[2].isEmpty()) {
            throw RendererProtocolException.malformed("SCENE_SELECT requires a revision and occurrence");
        }
        try {
            long revision = Long.parseLong(parts[1]);
            if (revision < 0) {
                throw RendererProtocolException.malformed("SCENE_SELECT revision must be non-negative");
            }
            return new SceneSelect(
                    revision,
                    "NONE".equals(parts[2])
                            ? Optional.empty()
                            : Optional.of(SceneViewOccurrenceCodec.decode(parts[2])));
        } catch (NumberFormatException exception) {
            throw RendererProtocolException.malformed("SCENE_SELECT revision must be numeric", exception);
        } catch (IllegalArgumentException exception) {
            throw RendererProtocolException.malformed("SCENE_SELECT occurrence is invalid", exception);
        }
    }

    private static SceneNavigate parseSceneNavigate(String[] parts) throws RendererProtocolException {
        if (parts.length < 2) {
            throw RendererProtocolException.malformed("SCENE_NAVIGATE requires an action");
        }
        try {
            return new SceneNavigate(
                    switch (parts[1]) {
                        case "ORBIT" ->
                            new SceneViewNavigation.Orbit(
                                    finite(parts, 2, 6, "ORBIT"), finite(parts, 3, 6, "ORBIT"),
                                    positiveInteger(parts, 4, "ORBIT"), positiveInteger(parts, 5, "ORBIT"));
                        case "PAN" ->
                            new SceneViewNavigation.Pan(
                                    finite(parts, 2, 6, "PAN"), finite(parts, 3, 6, "PAN"),
                                    positiveInteger(parts, 4, "PAN"), positiveInteger(parts, 5, "PAN"));
                        case "ZOOM" -> new SceneViewNavigation.Zoom(finite(parts, 2, 3, "ZOOM"));
                        case "GIZMO_CLICK" ->
                            new SceneViewNavigation.GizmoClick(
                                    finite(parts, 2, 6, "GIZMO_CLICK"), finite(parts, 3, 6, "GIZMO_CLICK"),
                                    positiveInteger(parts, 4, "GIZMO_CLICK"), positiveInteger(parts, 5, "GIZMO_CLICK"));
                        case "FOCUS_SELECTED" -> noNavigationArguments(parts, new SceneViewNavigation.FocusSelected());
                        case "FRAME_ALL" -> noNavigationArguments(parts, new SceneViewNavigation.FrameAll());
                        case "TOGGLE_PROJECTION" ->
                            noNavigationArguments(parts, new SceneViewNavigation.ToggleProjection());
                        default ->
                            throw RendererProtocolException.malformed("Unknown SCENE_NAVIGATE action: " + parts[1]);
                    });
        } catch (NumberFormatException | IndexOutOfBoundsException exception) {
            throw RendererProtocolException.malformed("SCENE_NAVIGATE arguments are invalid", exception);
        }
    }

    private static float finite(String[] parts, int index, int expectedLength, String action)
            throws RendererProtocolException {
        if (parts.length != expectedLength) {
            throw RendererProtocolException.malformed(action + " arguments are invalid");
        }
        float value = Float.parseFloat(parts[index]);
        if (!Float.isFinite(value)) {
            throw RendererProtocolException.malformed(action + " requires finite numbers");
        }
        return value;
    }

    private static int positiveInteger(String[] parts, int index, String action) throws RendererProtocolException {
        int value = Integer.parseInt(parts[index]);
        if (value <= 0) {
            throw RendererProtocolException.malformed(action + " requires positive viewport dimensions");
        }
        return value;
    }

    private static <T extends SceneViewNavigation> T noNavigationArguments(String[] parts, T navigation)
            throws RendererProtocolException {
        if (parts.length != 2) {
            throw RendererProtocolException.malformed(parts[1] + " does not accept arguments");
        }
        return navigation;
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

    record ScenePick(String requestToken, long revision, float horizontal, float vertical) implements RendererCommand {}

    record SceneSelect(long revision, Optional<CompositionOccurrenceId> occurrence) implements RendererCommand {}

    record SceneNavigate(SceneViewNavigation navigation) implements RendererCommand {}

    record ReceiveSurface() implements RendererCommand {}

    record Pause() implements RendererCommand {}

    record Resume() implements RendererCommand {}

    record Shutdown() implements RendererCommand {}

    record Quit() implements RendererCommand {}
}
