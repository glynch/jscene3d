/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.game.FixedUpdate;
import io.github.glynch.jscene3d.game.FrameUpdate;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.runtime.extension.NodeControllerContext;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Objects;

/** Executable test contribution discovered through the production service boundary. */
public final class TestRuntimeExtension implements ProjectRuntimeExtension {
    private static final String PREFIX = "io.github.glynch.runtime-test/";
    private static final RegisteredType GROUP = new RegisteredType(PREFIX + "group-3d", 1);
    private static final RegisteredType TIMER = new RegisteredType(PREFIX + "timer", 1);
    private static final RegisteredType INDICATOR = new RegisteredType(PREFIX + "indicator-3d", 1);
    private static final RegisteredType CONTROLLER = new RegisteredType(PREFIX + "toggle-controller", 1);

    @Override
    public String id() {
        return "io.github.glynch.runtime-test";
    }

    @Override
    public void register(ProjectRuntimeRegistry registry) {
        registry.registerSceneNode(GROUP, this::createGroup);
        registry.registerSceneNode(TIMER, this::createTimer);
        registry.registerSceneNode(INDICATOR, this::createIndicator);
        registry.registerNodeController(CONTROLLER, this::createController);
    }

    /** Creates a root object that also proves frame and render participation. */
    private ProjectRuntimeObject createGroup(SceneNodeContext context) {
        TestRuntimeState.rootLabel = ((ProjectValue.TextValue)
                        Objects.requireNonNull(context.properties().get("label"), "label"))
                .value();
        return new GroupObject(context.nodeDefinition().id());
    }

    /** Creates a fixed-update timer and records its runtime parent. */
    private ProjectRuntimeObject createTimer(SceneNodeContext context) {
        TestRuntimeState.timerParent =
                context.parent().orElseThrow().definition().id();
        return new TimerObject(context.nodeDefinition().id(), context.signal("timeout"));
    }

    /** Creates one mutable test indicator from an authored property. */
    private ProjectRuntimeObject createIndicator(SceneNodeContext context) {
        boolean active = ((ProjectValue.BooleanValue)
                        Objects.requireNonNull(context.properties().get("active"), "active"))
                .value();
        return new IndicatorObject(context.nodeDefinition().id(), active);
    }

    /** Attaches an authored action to the indicator without exposing a Java method in scene data. */
    private ProjectRuntimeObject createController(NodeControllerContext context) {
        IndicatorObject indicator = (IndicatorObject) context.node().object();
        context.action("toggle", indicator::toggle);
        return new RecordingObject("controller");
    }

    /** Records common lifecycle callbacks. */
    private static class RecordingObject implements ProjectRuntimeObject {
        private final String id;

        RecordingObject(String id) {
            this.id = id;
        }

        @Override
        public void start() {
            TestRuntimeState.EVENTS.add("start:" + id);
        }

        @Override
        public void close() {
            TestRuntimeState.EVENTS.add("close:" + id);
        }
    }

    /** Root node participating in frame update and render phases. */
    private static final class GroupObject extends RecordingObject
            implements FrameUpdateParticipant, RenderParticipant {
        GroupObject(String id) {
            super(id);
        }

        @Override
        public void update(FrameUpdate update) {
            TestRuntimeState.EVENTS.add("frame:" + update.fixedUpdateCount());
        }

        @Override
        public void render(FrameUpdate update) {
            TestRuntimeState.EVENTS.add("render:" + update.fixedUpdateCount());
        }
    }

    /** Fixed-update source for the authored timeout connection. */
    private static final class TimerObject extends RecordingObject implements FixedUpdateParticipant {
        private final RuntimeSignal timeout;

        TimerObject(String id, RuntimeSignal timeout) {
            super(id);
            this.timeout = timeout;
        }

        @Override
        public void fixedUpdate(FixedUpdate update) {
            TestRuntimeState.EVENTS.add("fixed:" + update.tick());
            timeout.emit();
        }
    }

    /** Target object mutated by its separately instantiated controller. */
    static final class IndicatorObject extends RecordingObject {
        private boolean active;

        IndicatorObject(String id, boolean active) {
            super(id);
            this.active = active;
        }

        boolean isActive() {
            return active;
        }

        void toggle() {
            active = !active;
            TestRuntimeState.EVENTS.add("toggle:" + active);
        }
    }
}
