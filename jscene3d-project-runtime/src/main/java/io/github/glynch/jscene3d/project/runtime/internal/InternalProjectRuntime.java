/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.game.FixedUpdate;
import io.github.glynch.jscene3d.game.FrameUpdate;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateParticipant;
import io.github.glynch.jscene3d.project.runtime.FrameUpdateParticipant;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntime;
import io.github.glynch.jscene3d.project.runtime.RenderParticipant;
import io.github.glynch.jscene3d.project.runtime.RuntimeNode;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Default composed project application. */
final class InternalProjectRuntime implements ProjectRuntime {
    private final GameProject project;
    private final SceneDefinition scene;
    private final RuntimeNode root;
    private final List<LifecycleEntry> lifecycle;
    private final Map<String, RuntimeNode> nodes;
    private final EndpointRouter router;
    private boolean started;
    private boolean closed;

    /** Creates one fully composed but not yet started runtime. */
    InternalProjectRuntime(
            GameProject project,
            SceneDefinition scene,
            RuntimeNode root,
            List<LifecycleEntry> lifecycle,
            Map<String, RuntimeNode> nodes,
            EndpointRouter router) {
        this.project = Objects.requireNonNull(project, "project");
        this.scene = Objects.requireNonNull(scene, "scene");
        this.root = Objects.requireNonNull(root, "root");
        this.lifecycle = List.copyOf(lifecycle);
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        this.router = Objects.requireNonNull(router, "router");
    }

    @Override
    public GameProject project() {
        return project;
    }

    @Override
    public SceneDefinition scene() {
        return scene;
    }

    @Override
    public RuntimeNode root() {
        return root;
    }

    @Override
    public Optional<RuntimeNode> findNode(String id) {
        return Optional.ofNullable(nodes.get(Preconditions.requireNonBlank(id, "id")));
    }

    @Override
    public void start() {
        requireOpen();
        if (started) {
            throw new IllegalStateException("project runtime has already started");
        }
        router.activate();
        try {
            for (LifecycleEntry entry : lifecycle) {
                entry.object().start();
            }
            started = true;
        } catch (RuntimeException exception) {
            closeAfterFailure(exception);
            throw exception;
        }
    }

    @Override
    public void fixedUpdate(FixedUpdate update) {
        Objects.requireNonNull(update, "update");
        requireRunning();
        for (LifecycleEntry entry : lifecycle) {
            if (entry.enabled() && entry.object() instanceof FixedUpdateParticipant participant) {
                participant.fixedUpdate(update);
            }
        }
    }

    @Override
    public void update(FrameUpdate update) {
        Objects.requireNonNull(update, "update");
        requireRunning();
        for (LifecycleEntry entry : lifecycle) {
            if (entry.enabled() && entry.object() instanceof FrameUpdateParticipant participant) {
                participant.update(update);
            }
        }
    }

    @Override
    public void render(FrameUpdate update) {
        Objects.requireNonNull(update, "update");
        requireRunning();
        for (LifecycleEntry entry : lifecycle) {
            if (entry.enabled() && entry.object() instanceof RenderParticipant participant) {
                participant.render(update);
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        router.deactivate();
        RuntimeException failure = null;
        for (int index = lifecycle.size() - 1; index >= 0; index--) {
            try {
                lifecycle.get(index).object().close();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Closes every owned object while retaining the startup failure as primary. */
    private void closeAfterFailure(RuntimeException failure) {
        try {
            close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    /** Requires runtime startup before callbacks. */
    private void requireRunning() {
        requireOpen();
        if (!started) {
            throw new IllegalStateException("project runtime has not started");
        }
    }

    /** Requires a runtime that has not begun terminal closure. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("project runtime is closed");
        }
    }
}
