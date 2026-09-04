/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.runtime.RuntimePayload;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;
import io.github.glynch.jscene3d.project.scene.SceneConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Exercises synchronous endpoint routing independently of scene factories. */
final class EndpointRouterTest {
    private static final RegisteredType PAYLOAD_TYPE = new RegisteredType("example.runtime/payload", 1);
    private static final RegisteredType OTHER_TYPE = new RegisteredType("example.runtime/other", 1);

    /** Dispatches no-payload connections in declaration order and respects enabled state. */
    @Test
    void dispatchesNoPayloadRoutesInOrder() {
        EndpointRouter router = new EndpointRouter();
        AtomicBoolean sourceEnabled = new AtomicBoolean(true);
        AtomicBoolean secondEnabled = new AtomicBoolean(true);
        List<String> calls = new ArrayList<>();
        RuntimeSignal signal = router.signal("source", noPayload("timeout"), sourceEnabled::get);
        router.action("first", noPayload("run"), () -> true, () -> calls.add("first"));
        router.action("second", noPayload("run"), secondEnabled::get, () -> calls.add("second"));
        router.connect(List.of(
                connection("source", "timeout", "first", "run"), connection("source", "timeout", "second", "run")));
        router.activate();

        signal.emit();
        secondEnabled.set(false);
        signal.emit();
        sourceEnabled.set(false);
        signal.emit();

        assertThat(calls).containsExactly("first", "second", "first");
        router.deactivate();
        assertThatThrownBy(signal::emit).isInstanceOf(IllegalStateException.class);
    }

    /** Requires exact registered payload identity at both signal and action boundaries. */
    @Test
    void dispatchesOnlyExactPayloadType() {
        EndpointRouter router = new EndpointRouter();
        AtomicReference<RuntimePayload> received = new AtomicReference<>();
        RuntimeSignal signal = router.signal("source", withPayload("changed"), () -> true);
        router.action("target", withPayload("accept"), () -> true, received::set);
        router.connect(List.of(connection("source", "changed", "target", "accept")));
        router.activate();
        RuntimePayload payload = new RuntimePayload(PAYLOAD_TYPE, "value");

        signal.emit(payload);

        assertThat(received).hasValue(payload);
        assertThatThrownBy(signal::emit).isInstanceOf(IllegalArgumentException.class);
        RuntimePayload wrongPayload = new RuntimePayload(OTHER_TYPE, "wrong");
        assertThatThrownBy(() -> signal.emit(wrongPayload)).isInstanceOf(IllegalArgumentException.class);
    }

    /** Rejects endpoint implementation shapes that contradict their safe descriptors. */
    @Test
    void rejectsInvalidActionImplementationsAndConnections() {
        EndpointRouter router = new EndpointRouter();
        EndpointDescriptor plain = noPayload("plain");
        EndpointDescriptor typed = withPayload("typed");

        assertThatThrownBy(() -> router.action("node", typed, () -> true, () -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> router.action("node", plain, () -> true, ignored -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        router.action("node", plain, () -> true, () -> {});
        assertThatThrownBy(() -> router.action("node", plain, () -> true, () -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        List<SceneConnection> missing = List.of(connection("source", "ready", "missing", "run"));
        assertThatThrownBy(() -> router.connect(missing))
                .isInstanceOf(RuntimeCompositionException.class)
                .hasMessageContaining("no runtime implementation");
    }

    /** Creates a no-payload endpoint descriptor. */
    private static EndpointDescriptor noPayload(String id) {
        return EndpointDescriptor.withoutPayload(id, DescriptorPresentation.named(id));
    }

    /** Creates a payload-bearing endpoint descriptor. */
    private static EndpointDescriptor withPayload(String id) {
        return EndpointDescriptor.withPayload(id, PAYLOAD_TYPE, DescriptorPresentation.named(id));
    }

    /** Creates one concise authored connection. */
    private static SceneConnection connection(String fromNode, String signal, String toNode, String action) {
        return new SceneConnection(
                new SceneConnection.SignalEndpoint(fromNode, signal),
                new SceneConnection.ActionEndpoint(toNode, action));
    }
}
