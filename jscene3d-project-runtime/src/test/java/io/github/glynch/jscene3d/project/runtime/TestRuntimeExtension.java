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
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executable test contribution discovered through the production service boundary. */
public final class TestRuntimeExtension implements ProjectRuntimeExtension {
    private static final String PREFIX = "io.github.glynch.runtime-test/";
    private static final RegisteredType GROUP = new RegisteredType(PREFIX + "group-3d", 1);
    private static final RegisteredType TIMER = new RegisteredType(PREFIX + "timer", 1);
    private static final RegisteredType INDICATOR = new RegisteredType(PREFIX + "indicator-3d", 1);
    private static final RegisteredType CONTROLLER = new RegisteredType(PREFIX + "toggle-controller", 1);
    private static final RegisteredType RESOURCE_CONSUMER = new RegisteredType(PREFIX + "resource-consumer-3d", 1);
    private static final RegisteredType SHARED_DATA = new RegisteredType(PREFIX + "shared-data", 1);
    private static final RegisteredType TEXT_DATA = new RegisteredType(PREFIX + "text-data", 1);
    private static final Map<String, FixedUpdatePhase> FIXED_UPDATE_PHASES = Map.of(
            "before-physics", FixedUpdatePhase.BEFORE_PHYSICS,
            "physics", FixedUpdatePhase.PHYSICS,
            "after-physics", FixedUpdatePhase.AFTER_PHYSICS);

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
        registry.registerSceneNode(RESOURCE_CONSUMER, this::createResourceConsumer);
        registry.registerResource(SHARED_DATA, this::createSharedData);
        registry.registerResource(TEXT_DATA, context -> "text");
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
        String phaseName = ((ProjectValue.TextValue)
                        Objects.requireNonNull(context.properties().get("phase"), "phase"))
                .value();
        FixedUpdatePhase phase = Objects.requireNonNull(FIXED_UPDATE_PHASES.get(phaseName), "known timer phase");
        return new TimerObject(context.nodeDefinition().id(), context.signal("timeout"), phase);
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

    /** Creates one node retaining a shared resolved resource. */
    private ProjectRuntimeObject createResourceConsumer(SceneNodeContext context) {
        ResourceReference reference = ((ProjectValue.ReferenceValue)
                        Objects.requireNonNull(context.properties().get("resource"), "resource"))
                .reference();
        return new ResourceConsumerObject(
                context.nodeDefinition().id(), context.resolveResource(reference, SharedData.class));
    }

    /** Creates one resource and resolves its optional declared dependency. */
    private Object createSharedData(ResourceFactoryContext context) {
        String label = ((ProjectValue.TextValue)
                        Objects.requireNonNull(context.properties().get("label"), "label"))
                .value();
        ProjectValue dependencyValue = context.properties().get("dependency");
        Optional<SharedData> dependency = Optional.empty();
        if (dependencyValue instanceof ProjectValue.ReferenceValue referenceValue) {
            dependency = Optional.of(context.resolveResource(referenceValue.reference(), SharedData.class));
        }
        return new SharedData(label, dependency);
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
        private final String timerId;
        private final RuntimeSignal timeout;
        private final FixedUpdatePhase phase;

        TimerObject(String id, RuntimeSignal timeout, FixedUpdatePhase phase) {
            super(id);
            timerId = id;
            this.timeout = timeout;
            this.phase = phase;
        }

        @Override
        public FixedUpdatePhase fixedUpdatePhase() {
            return phase;
        }

        @Override
        public void fixedUpdate(FixedUpdate update) {
            TestRuntimeState.EVENTS.add(
                    "fixed:" + phase.name().toLowerCase(Locale.ROOT) + ':' + timerId + ':' + update.tick());
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

    /** Scene object retaining but not owning one resolved resource. */
    static final class ResourceConsumerObject extends RecordingObject {
        private final SharedData resource;

        ResourceConsumerObject(String id, SharedData resource) {
            super(id);
            this.resource = resource;
        }

        SharedData resource() {
            return resource;
        }
    }

    /** Owned resource value used to observe sharing, dependencies, and closure. */
    static final class SharedData implements AutoCloseable {
        private final String label;
        private final Optional<SharedData> dependency;
        private int closeCount;

        SharedData(String label, Optional<SharedData> dependency) {
            this.label = label;
            this.dependency = dependency;
        }

        String label() {
            return label;
        }

        Optional<SharedData> dependency() {
            return dependency;
        }

        int closeCount() {
            return closeCount;
        }

        @Override
        public void close() {
            closeCount++;
            TestRuntimeState.EVENTS.add("close-resource:" + label);
        }
    }
}
