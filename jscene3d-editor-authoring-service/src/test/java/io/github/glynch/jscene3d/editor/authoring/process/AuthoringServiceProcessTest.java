/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.process;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glynch.jscene3d.editor.authoring.protocol.framing.ContentLengthMessageReader;
import io.github.glynch.jscene3d.editor.authoring.protocol.framing.ContentLengthMessageWriter;
import io.github.glynch.jscene3d.editor.authoring.testing.AuthoringTestProject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Proves the complete framed protocol against a separately spawned service JVM. */
final class AuthoringServiceProcessTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(10);

    @TempDir
    private Path temporaryDirectory;

    /** Spawns, initializes, opens, closes, shuts down, and cleanly reaps the real service process. */
    @Test
    void runsAuthoringLifecycleInSeparateProcess() throws Exception {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        Process process = startService();
        try {
            ContentLengthMessageWriter writer = new ContentLengthMessageWriter(process.getOutputStream());
            ContentLengthMessageReader reader = new ContentLengthMessageReader(process.getInputStream());

            writer.writeMessage(request(1, "initialize", initializeParams()));
            JsonNode initialize = response(reader);
            assertThat(initialize.at("/result/processKind").asText()).isEqualTo("authoring");
            assertThat(initialize.at("/result/capabilities"))
                    .extracting(JsonNode::asText)
                    .contains("project/open", "project/close");

            ObjectNode openParams = JSON.createObjectNode().put("path", temporaryDirectory.toString());
            writer.writeMessage(request(2, "project/open", openParams));
            JsonNode openResponse = response(reader);
            assertThat(openResponse.at("/result/opened").asBoolean()).isTrue();
            assertThat(openResponse.at("/result/project/name").asText()).isEqualTo("Small Authoring Project");
            assertThat(openResponse.at("/result/project/descriptor").asText())
                    .endsWith(AuthoringTestProject.DESCRIPTOR);
            assertThat(openResponse.at("/result/diagnostics")).isEmpty();

            writer.writeMessage(request(3, "project/close", JSON.createObjectNode()));
            JsonNode close = response(reader);
            assertThat(close.at("/result/closed").asBoolean()).isTrue();

            writer.writeMessage(request(4, "service/shutdown", JSON.createObjectNode()));
            JsonNode shutdown = response(reader);
            assertThat(shutdown.at("/result/shutdown").asBoolean()).isTrue();

            assertThat(process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                    .withFailMessage("Authoring service did not exit after orderly shutdown")
                    .isTrue();
            assertThat(process.exitValue())
                    .withFailMessage(() -> stderr(process))
                    .isZero();
            assertThat(reader.readMessage()).isEmpty();
        } finally {
            if (process.isAlive()) {
                process.destroy();
                process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    /** Reads versions filtered into the built service artifact. */
    @Test
    void readsEmbeddedBuildVersions() {
        AuthoringServiceBuildInfo build = AuthoringServiceBuildInfo.current();

        assertThat(build.serviceVersion()).isEqualTo("0.1.0-SNAPSHOT");
        assertThat(build.engineVersion()).isEqualTo("0.1.0-SNAPSHOT");
    }

    /** Starts the service main class with Surefire's resolved production module path. */
    private static Process startService() throws IOException {
        String modulePath = Objects.requireNonNull(System.getProperty("jdk.module.path"));
        Path javaExecutable = Path.of(Objects.requireNonNull(System.getProperty("java.home")), "bin", "java");
        String main = "io.github.glynch.jscene3d.editor.authoring.service/" + AuthoringServiceMain.class.getName();
        return new ProcessBuilder(javaExecutable.toString(), "--module-path", modulePath, "--module", main).start();
    }

    /** Creates initialization parameters for the current protocol major. */
    private static ObjectNode initializeParams() {
        ObjectNode version = JSON.createObjectNode().put("major", 1).put("minor", 0);
        return JSON.createObjectNode().set("protocolVersion", version);
    }

    /** Serializes one JSON-RPC-style request. */
    private static String request(int id, String method, JsonNode params) throws IOException {
        ObjectNode request = JSON.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.set("params", params);
        return JSON.writeValueAsString(request);
    }

    /** Reads and parses one required framed response. */
    private static JsonNode response(ContentLengthMessageReader reader) throws IOException {
        return JSON.readTree(reader.readMessage().orElseThrow());
    }

    /** Returns child stderr for a failed-exit assertion. */
    private static String stderr(Process process) {
        try {
            return new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "Could not read authoring service stderr: " + exception;
        }
    }
}
