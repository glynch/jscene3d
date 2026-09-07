/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import io.github.glynch.jscene3d.platform.GamepadState;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** World-scoped adapter that samples an authored input map and publishes semantic state. */
public final class ProjectInput implements InputWorldModule {
    private final InputMap inputMap;
    private ActionSnapshot snapshot = ActionSnapshot.empty();
    private boolean closed;

    /** Compiles one validated project input definition.
     *
     * @param definition validated project input map
     */
    public ProjectInput(InputMapDefinition definition) {
        inputMap = InputMap.compile(Objects.requireNonNull(definition, "definition"));
    }

    /** Creates an input module with no authored actions.
     *
     * <p>This is used by standard hosts for projects which do not declare an input map. The module
     * still provides a stable empty snapshot to components.
     *
     * @return new open empty input module
     */
    public static ProjectInput empty() {
        return new ProjectInput(InputMap.empty());
    }

    /** Stores one already compiled map. */
    private ProjectInput(InputMap inputMap) {
        this.inputMap = Objects.requireNonNull(inputMap, "inputMap");
    }

    /** Samples keyboard and mouse state for the current update.
     *
     * @param input current window input
     * @param capture host-interface ownership for this update
     * @return published semantic snapshot
     */
    public ActionSnapshot sample(InputState input, InputCapture capture) {
        return sample(input, null, capture);
    }

    /** Samples keyboard, mouse, and one runtime-assigned gamepad for the current update.
     *
     * @param input current window input
     * @param gamepad assigned gamepad state, or {@code null}
     * @param capture host-interface ownership for this update
     * @return published semantic snapshot
     */
    public ActionSnapshot sample(InputState input, @Nullable GamepadState gamepad, InputCapture capture) {
        requireOpen();
        snapshot = inputMap.sample(input, gamepad, capture);
        return snapshot;
    }

    /** Publishes semantic state supplied by tests, replay, or another non-window host.
     *
     * @param input immutable semantic snapshot
     */
    public void publish(ActionSnapshot input) {
        requireOpen();
        snapshot = Objects.requireNonNull(input, "input");
    }

    @Override
    public ActionSnapshot snapshot() {
        requireOpen();
        return snapshot;
    }

    /** Makes closure terminal and idempotent without owning physical devices. */
    @Override
    public void close() {
        closed = true;
        snapshot = ActionSnapshot.empty();
    }

    /** Rejects use after world ownership has ended. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("project input is closed");
        }
    }
}
