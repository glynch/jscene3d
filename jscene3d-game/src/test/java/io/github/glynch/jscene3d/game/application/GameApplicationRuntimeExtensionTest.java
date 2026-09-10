/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Specifies generic descriptor-authored application-command dispatch. */
final class GameApplicationRuntimeExtensionTest {
    private static final InputAction MENU = new InputAction("menu");

    /** Registers the command binding and requests its command only on the authored semantic press. */
    @Test
    void dispatchesAuthoredCommandFromSemanticInput() {
        ProjectInput input = ProjectInput.empty();
        RecordingApplicationControl application = new RecordingApplicationControl();
        GameApplicationRuntimeExtension extension = new GameApplicationRuntimeExtension();
        Map<ComponentType, ComponentFactory<?>> factories = new LinkedHashMap<>();
        extension.register(factories::put);

        assertThat(extension.id()).isEqualTo(GameApplicationDescriptors.extensionId());
        assertThat(factories).containsOnlyKeys(GameApplicationDescriptors.commandBindingType());

        ComponentUpdateCallbacks binding = (ComponentUpdateCallbacks)
                factories.get(GameApplicationDescriptors.commandBindingType()).create(context(input, application));
        binding.onBeforePhysics(update());
        assertThat(application.requested).isNull();

        input.publish(ActionSnapshot.builder().pressed(MENU).build());
        binding.onBeforePhysics(update());
        assertThat(application.requested).isEqualTo(ApplicationCommand.SHOW_MENU);
    }

    /** Exposes complete safe metadata and rejects unknown authored command identities. */
    @Test
    void describesAndValidatesApplicationCommands() {
        assertThat(GameApplicationDescriptors.extensionDescriptor().id())
                .isEqualTo(GameApplicationDescriptors.extensionId());
        assertThat(GameApplicationDescriptors.extensionDescriptor().components())
                .singleElement()
                .extracting(descriptor -> descriptor.type())
                .isEqualTo(GameApplicationDescriptors.commandBindingType());
        assertThat(ApplicationCommand.values())
                .extracting(ApplicationCommand::id)
                .containsExactly("show-menu", "return-to-menu", "new-game", "resume", "quit");
        assertThat(ApplicationCommand.fromId("return-to-menu")).isEqualTo(ApplicationCommand.RETURN_TO_MENU);
        assertThat(ApplicationCommand.fromId("resume")).isEqualTo(ApplicationCommand.RESUME);
        assertThatThrownBy(() -> ApplicationCommand.fromId("restart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported application command: restart");
    }

    /** Creates a factory context containing the two generic world modules and authored properties. */
    private static ComponentFactoryContext context(ProjectInput input, ApplicationControl application) {
        ComponentProperties properties = new ComponentProperties(Map.of(
                GameApplicationDescriptors.actionProperty(), new ProjectValue.TextValue(MENU.name()),
                GameApplicationDescriptors.commandProperty(),
                        new ProjectValue.TextValue(ApplicationCommand.SHOW_MENU.id())));
        World world = (World) Proxy.newProxyInstance(
                GameApplicationRuntimeExtensionTest.class.getClassLoader(),
                new Class<?>[] {World.class},
                (proxy, method, arguments) -> {
                    if (!method.getName().equals("requireModule")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return arguments[0] == InputWorldModule.class ? input : application;
                });
        return (ComponentFactoryContext) Proxy.newProxyInstance(
                GameApplicationRuntimeExtensionTest.class.getClassLoader(),
                new Class<?>[] {ComponentFactoryContext.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "world" -> world;
                    case "properties" -> properties;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    /** Supplies one valid deterministic update context. */
    private static FixedUpdateContext update() {
        return new FixedUpdateContext(0L, Duration.ofMillis(20L), Duration.ZERO);
    }

    /** Captures the latest requested host command. */
    private static final class RecordingApplicationControl implements ApplicationControl {
        private ApplicationCommand requested;

        @Override
        public boolean canResume() {
            return false;
        }

        @Override
        public void request(ApplicationCommand command) {
            requested = command;
        }

        @Override
        public void close() {
            // Test adapter owns no resources.
        }
    }
}
