/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocolVersion;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Tests lifecycle and failure behavior at the renderer protocol boundary. */
final class RendererProtocolServerTest {
    @Test
    void reportsReadyFramesSurfaceReplacementAndOrderlyShutdown() throws IOException {
        FakeRendererSession session = new FakeRendererSession();
        ProtocolResult result = run(session, "FRAME 7\nDRAG 2 -3\nRECEIVE_SURFACE\nSHUTDOWN\nFRAME 8\n");

        assertThat(result.events())
                .containsExactly(
                        "PROTOCOL_VERSION 1.1",
                        "RENDERER_READY",
                        "FRAME_READY",
                        "SURFACE_READY 800 600",
                        "SHUTDOWN_READY");
        assertThat(session.renderedFrames).isEqualTo(1);
        assertThat(session.dragRequests).isEqualTo(1);
        assertThat(session.replacementRequests).isEqualTo(1);
        session.close();
        assertThat(session.closed).isTrue();
    }

    @Test
    void enforcesPauseAndResumeState() throws IOException {
        FakeRendererSession session = new FakeRendererSession();
        ProtocolResult result = run(session, "PAUSE\nFRAME 1\nPAUSE\nRESUME\nRESUME\nFRAME 2\nSHUTDOWN\n");

        assertThat(result.events())
                .containsExactly(
                        "PROTOCOL_VERSION 1.1",
                        "RENDERER_READY",
                        "PAUSED",
                        "ERROR INVALID_STATE frame-paused",
                        "ERROR INVALID_STATE already-paused",
                        "RESUMED",
                        "ERROR INVALID_STATE not-paused",
                        "FRAME_READY",
                        "SHUTDOWN_READY");
        assertThat(session.renderedFrames).isEqualTo(1);
    }

    @Test
    void preservesStageOneQuitWithoutWritingAProductShutdownAcknowledgement() throws IOException {
        ProtocolResult result = run(new FakeRendererSession(), "QUIT\n");

        assertThat(result.events()).containsExactly("PROTOCOL_VERSION 1.1", "RENDERER_READY");
    }

    @Test
    void reportsMalformedAndUnknownInputWithoutStoppingTheSession() throws IOException {
        ProtocolResult result = run(new FakeRendererSession(), "FRAME\nUNKNOWN\nFRAME 3\nSHUTDOWN\n");

        assertThat(result.events())
                .containsExactly(
                        "PROTOCOL_VERSION 1.1",
                        "RENDERER_READY",
                        "ERROR MALFORMED_REQUEST command",
                        "ERROR UNKNOWN_COMMAND command",
                        "FRAME_READY",
                        "SHUTDOWN_READY");
        assertThat(result.diagnostics()).contains("Rejected command");
    }

    @Test
    void replacesACompleteSafeSceneViewSnapshot() throws IOException {
        FakeRendererSession session = new FakeRendererSession();
        String json =
                "{\"sceneAssetId\":\"e890c4c3-fb32-49d8-88b8-4e04e7a29656\"," + "\"revision\":3,\"occurrences\":[]}";
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        ProtocolResult result = run(session, "SCENE_SNAPSHOT " + encoded + "\nSHUTDOWN\n");

        assertThat(result.events()).containsExactly("PROTOCOL_VERSION 1.1", "RENDERER_READY", "SHUTDOWN_READY");
        assertThat(session.snapshot)
                .isNotNull()
                .returns(3L, SceneViewSnapshot::revision)
                .returns(
                        "e890c4c3-fb32-49d8-88b8-4e04e7a29656",
                        value -> value.scene().toString());
    }

    @Test
    void reportsUnexpectedRuntimeFailureBeforePropagatingIt() {
        FakeRendererSession session = new FakeRendererSession();
        session.failFrames = true;
        StringWriter output = new StringWriter();
        RendererProtocolServer server = new RendererProtocolServer(
                session,
                RendererProtocolVersion.CURRENT,
                new PrintWriter(output, true),
                new PrintWriter(new StringWriter(), true));

        assertThatIllegalStateException()
                .isThrownBy(() -> server.run(new BufferedReader(new StringReader("FRAME 9\n"))))
                .withMessage("frame failed");
        assertThat(output.toString().lines())
                .containsExactly("PROTOCOL_VERSION 1.1", "RENDERER_READY", "ERROR RUNTIME_FAILURE frame");
    }

    @Test
    void reportsNativeLinkageFailureBeforePropagatingIt() {
        FakeRendererSession session = new FakeRendererSession();
        session.failNativeFrames = true;
        StringWriter output = new StringWriter();
        RendererProtocolServer server = new RendererProtocolServer(
                session,
                RendererProtocolVersion.CURRENT,
                new PrintWriter(output, true),
                new PrintWriter(new StringWriter(), true));

        assertThatExceptionOfType(UnsatisfiedLinkError.class)
                .isThrownBy(() -> server.run(new BufferedReader(new StringReader("FRAME 9\n"))))
                .withMessage("native frame failed");
        assertThat(output.toString().lines())
                .containsExactly("PROTOCOL_VERSION 1.1", "RENDERER_READY", "ERROR RUNTIME_FAILURE frame");
    }

    private static ProtocolResult run(FakeRendererSession session, String input) throws IOException {
        StringWriter output = new StringWriter();
        StringWriter diagnostics = new StringWriter();
        RendererProtocolServer server = new RendererProtocolServer(
                session,
                RendererProtocolVersion.CURRENT,
                new PrintWriter(output, true),
                new PrintWriter(diagnostics, true));
        server.run(new BufferedReader(new StringReader(input)));
        return new ProtocolResult(output.toString().lines().toList(), diagnostics.toString());
    }

    private record ProtocolResult(List<String> events, String diagnostics) {}

    private static final class FakeRendererSession implements RendererSession {
        private int renderedFrames;
        private int replacementRequests;
        private int dragRequests;
        private boolean failFrames;
        private boolean failNativeFrames;
        private boolean closed;
        private @Nullable SceneViewSnapshot snapshot;

        @Override
        public void renderFrame() {
            if (failNativeFrames) {
                throw new UnsatisfiedLinkError("native frame failed");
            }
            if (failFrames) {
                throw new IllegalStateException("frame failed");
            }
            renderedFrames++;
        }

        @Override
        public SurfaceSize receiveReplacementSurface() {
            replacementRequests++;
            return new SurfaceSize(800, 600);
        }

        @Override
        public void applyValidationDrag(float horizontal, float vertical) {
            dragRequests++;
        }

        @Override
        public void replaceSceneViewSnapshot(SceneViewSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
