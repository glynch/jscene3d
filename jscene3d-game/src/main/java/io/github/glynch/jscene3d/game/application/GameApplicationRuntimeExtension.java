/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.application;

import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import java.util.Objects;

/** Executable factories corresponding exactly to {@link GameApplicationDescriptors}. */
public final class GameApplicationRuntimeExtension implements ComponentRuntimeExtension {
    /** Creates a stateless built-in runtime extension. */
    public GameApplicationRuntimeExtension() {
        // Stateless extension.
    }

    @Override
    public String id() {
        return GameApplicationDescriptors.extensionId();
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .register(
                        GameApplicationDescriptors.commandBindingType(),
                        context -> new CommandBinding(
                                context.world().requireModule(InputWorldModule.class),
                                context.world().requireModule(ApplicationControl.class),
                                new InputAction(context.properties().text(GameApplicationDescriptors.actionProperty())),
                                ApplicationCommand.fromId(
                                        context.properties().text(GameApplicationDescriptors.commandProperty()))));
    }

    /** Converts one semantic action transition into a deferred host command. */
    private static final class CommandBinding implements ComponentUpdateCallbacks {
        private final InputWorldModule input;
        private final ApplicationControl application;
        private final InputAction action;
        private final ApplicationCommand command;

        private CommandBinding(
                InputWorldModule input,
                ApplicationControl application,
                InputAction action,
                ApplicationCommand command) {
            this.input = Objects.requireNonNull(input, "input");
            this.application = Objects.requireNonNull(application, "application");
            this.action = Objects.requireNonNull(action, "action");
            this.command = Objects.requireNonNull(command, "command");
        }

        @Override
        public void onBeforePhysics(FixedUpdateContext update) {
            Objects.requireNonNull(update, "update");
            if (input.snapshot().wasPressed(action)) {
                application.request(command);
            }
        }
    }
}
