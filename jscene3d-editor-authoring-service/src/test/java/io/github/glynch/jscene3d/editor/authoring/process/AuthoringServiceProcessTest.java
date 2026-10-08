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
import java.util.ArrayList;
import java.util.List;
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

    /** Exercises atomic replacement and orderly shutdown through the real framed service process. */
    @Test
    void runsAuthoringLifecycleInSeparateProcess() throws Exception {
        Path first = temporaryDirectory.resolve("first");
        Path invalid = temporaryDirectory.resolve("invalid");
        Path second = temporaryDirectory.resolve("second");
        AuthoringTestProject.write(first, "first.j3d", "First Project");
        AuthoringTestProject.write(second, "second.j3d", "Second Project");
        Process process = startService();
        try {
            ContentLengthMessageWriter writer = new ContentLengthMessageWriter(process.getOutputStream());
            ContentLengthMessageReader reader = new ContentLengthMessageReader(process.getInputStream());

            writer.writeMessage(request(1, "initialize", initializeParams()));
            JsonNode initialize = response(reader);
            assertThat(initialize.at("/result/processKind").asText()).isEqualTo("authoring");
            assertThat(initialize.at("/result/protocolVersion/major").asInt()).isEqualTo(1);
            assertThat(initialize.at("/result/protocolVersion/minor").asInt()).isZero();
            assertThat(initialize.at("/result/contractIdentity").asText())
                    .isEqualTo(AuthoringServiceBuildInfo.current().contractIdentity());
            assertThat(initialize.at("/result/buildIdentity").asText())
                    .isEqualTo(AuthoringServiceBuildInfo.current().buildIdentity());
            assertThat(initialize.at("/result/capabilities"))
                    .extracting(JsonNode::asText)
                    .contains("project/open", "project/replace", "project/close");

            ObjectNode openParams = JSON.createObjectNode().put("path", first.toString());
            writer.writeMessage(request(2, "project/open", openParams));
            JsonNode openResponse = response(reader);
            assertThat(openResponse.at("/result/opened").asBoolean()).isTrue();
            assertThat(openResponse.at("/result/project/name").asText()).isEqualTo("First Project");
            assertThat(openResponse.at("/result/project/descriptor").asText()).endsWith("first.j3d");
            assertThat(openResponse.at("/result/diagnostics")).isEmpty();
            long firstGeneration = openResponse.at("/result/projectGeneration").asLong();

            ObjectNode staleParams = replaceParams(firstGeneration + 1, second);
            writer.writeMessage(request(3, "project/replace", staleParams));
            JsonNode stale = response(reader);
            assertThat(stale.at("/result/outcome").asText()).isEqualTo("conflict");
            assertThat(stale.at("/result/failureCode").asText()).isEqualTo("authoring.project.generationConflict");

            writer.writeMessage(request(4, "project/replace", replaceParams(firstGeneration, invalid)));
            JsonNode rejected = response(reader);
            assertThat(rejected.at("/result/outcome").asText()).isEqualTo("candidateRejected");
            assertThat(rejected.at("/result/diagnostics/0/code").asText()).isEqualTo("project.directory.missing");

            writer.writeMessage(request(5, "project/replace", replaceParams(firstGeneration, second)));
            JsonNode replaced = response(reader);
            assertThat(replaced.at("/result/outcome").asText()).isEqualTo("replaced");
            assertThat(replaced.at("/result/project/name").asText()).isEqualTo("Second Project");
            assertThat(replaced.at("/result/projectGeneration").asLong()).isNotEqualTo(firstGeneration);
            long secondGeneration = replaced.at("/result/projectGeneration").asLong();

            writer.writeMessage(request(6, "project/close", JSON.createObjectNode()));
            JsonNode close = response(reader);
            assertThat(close.at("/result/closed").asBoolean()).isTrue();
            assertThat(close.at("/result/invalidatedProjectGeneration").asLong())
                    .isEqualTo(secondGeneration);

            writer.writeMessage(request(7, "service/shutdown", JSON.createObjectNode()));
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

    /** Loads required installed descriptor metadata without resolving its declared runtime provider. */
    @Test
    void opensProjectWithConfiguredInstalledExtensionMetadata() throws Exception {
        String extensionId = "example.installed";
        Path project = temporaryDirectory.resolve("project");
        Path unusedMetadata = temporaryDirectory.resolve("unused");
        Path installedMetadata = temporaryDirectory.resolve("installed");
        AuthoringTestProject.writeRequiringInstalledExtension(project, "installed.j3d", extensionId);
        AuthoringTestProject.writeInstalledExtensionMetadata(unusedMetadata, "example.unused");
        AuthoringTestProject.writeInstalledExtensionMetadata(installedMetadata, extensionId);
        Process process = startService(List.of(unusedMetadata, installedMetadata));
        try {
            ContentLengthMessageWriter writer = new ContentLengthMessageWriter(process.getOutputStream());
            ContentLengthMessageReader reader = new ContentLengthMessageReader(process.getInputStream());
            writer.writeMessage(request(1, "initialize", initializeParams()));
            response(reader);

            ObjectNode openParams = JSON.createObjectNode().put("path", project.toString());
            writer.writeMessage(request(2, "project/open", openParams));
            JsonNode opened = response(reader);

            assertThat(opened.at("/result/opened").asBoolean())
                    .withFailMessage(opened::toPrettyString)
                    .isTrue();
            assertThat(opened.at("/result/diagnostics")).isEmpty();
        } finally {
            stopService(process);
        }
    }

    /** Keeps a genuinely absent installed extension as a structured project diagnostic. */
    @Test
    void reportsMissingInstalledExtensionWithoutStoppingService() throws Exception {
        Path project = temporaryDirectory.resolve("project");
        AuthoringTestProject.writeRequiringInstalledExtension(project, "missing.j3d", "example.missing");
        Process process = startService();
        try {
            ContentLengthMessageWriter writer = new ContentLengthMessageWriter(process.getOutputStream());
            ContentLengthMessageReader reader = new ContentLengthMessageReader(process.getInputStream());
            writer.writeMessage(request(1, "initialize", initializeParams()));
            response(reader);

            writer.writeMessage(
                    request(2, "project/open", JSON.createObjectNode().put("path", project.toString())));
            JsonNode opened = response(reader);

            assertThat(opened.at("/result/opened").asBoolean()).isTrue();
            assertThat(opened.at("/result/diagnostics"))
                    .extracting(diagnostic -> diagnostic.path("code").asText())
                    .contains("extension.missing");
            assertThat(process.isAlive()).isTrue();
        } finally {
            stopService(process);
        }
    }

    /** Reports an unavailable configured artifact through existing project diagnostics. */
    @Test
    void reportsUnavailableInstalledExtensionMetadataWithoutStoppingService() throws Exception {
        Path project = temporaryDirectory.resolve("project");
        Path missingArtifact = temporaryDirectory.resolve("not-installed");
        AuthoringTestProject.writeRequiringInstalledExtension(project, "missing.j3d", "example.missing");
        Process process = startService(List.of(missingArtifact));
        try {
            ContentLengthMessageWriter writer = new ContentLengthMessageWriter(process.getOutputStream());
            ContentLengthMessageReader reader = new ContentLengthMessageReader(process.getInputStream());
            writer.writeMessage(request(1, "initialize", initializeParams()));
            response(reader);

            writer.writeMessage(
                    request(2, "project/open", JSON.createObjectNode().put("path", project.toString())));
            JsonNode rejected = response(reader);

            assertThat(rejected.at("/result/diagnostics"))
                    .extracting(diagnostic -> diagnostic.path("code").asText())
                    .contains("editor.extension.metadata", "extension.missing");
            assertThat(process.isAlive()).isTrue();
        } finally {
            stopService(process);
        }
    }

    /** Rejects malformed process configuration before accepting protocol input. */
    @Test
    void rejectsUnsupportedConfigurationArgument() throws Exception {
        Process process = startServiceWithArguments(List.of("--unsupported"));
        try {
            assertThat(process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                    .isTrue();
            assertThat(process.exitValue()).isNotZero();
            assertThat(stderr(process)).contains("Unsupported authoring service argument: --unsupported");
        } finally {
            if (process.isAlive()) {
                process.destroy();
                process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    /** Rejects a configured metadata option whose artifact path is blank. */
    @Test
    void rejectsBlankExtensionMetadataConfigurationArgument() throws Exception {
        Process process = startServiceWithArguments(List.of("--extension-metadata="));
        try {
            assertThat(process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                    .isTrue();
            assertThat(process.exitValue()).isNotZero();
            assertThat(stderr(process)).contains("extension metadata path must not be blank");
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
        assertThat(build.protocolVersion()).isEqualTo("1.0");
        assertThat(build.contractIdentity()).isEqualTo("jscene3d-editor-development");
        assertThat(build.buildIdentity()).isNotBlank().doesNotContain("${");
    }

    /** Starts the service main class with Surefire's resolved production module path. */
    private static Process startService() throws IOException {
        return startService(List.of());
    }

    /** Starts the service with explicit installed extension metadata artifacts. */
    private static Process startService(List<Path> installedExtensionMetadata) throws IOException {
        List<String> arguments = installedExtensionMetadata.stream()
                .map(path -> "--extension-metadata=" + path)
                .toList();
        return startServiceWithArguments(arguments);
    }

    /** Starts the service with explicit application arguments after the JPMS entry point. */
    private static Process startServiceWithArguments(List<String> arguments) throws IOException {
        String modulePath = Objects.requireNonNull(System.getProperty("jdk.module.path"));
        Path javaExecutable = Path.of(Objects.requireNonNull(System.getProperty("java.home")), "bin", "java");
        String main = "io.github.glynch.jscene3d.editor.authoring.service/" + AuthoringServiceMain.class.getName();
        List<String> command =
                new ArrayList<>(List.of(javaExecutable.toString(), "--module-path", modulePath, "--module", main));
        command.addAll(arguments);
        return new ProcessBuilder(command).start();
    }

    /** Requests orderly shutdown when possible and otherwise terminates the child process. */
    private static void stopService(Process process) throws Exception {
        if (!process.isAlive()) {
            return;
        }
        ContentLengthMessageWriter writer = new ContentLengthMessageWriter(process.getOutputStream());
        ContentLengthMessageReader reader = new ContentLengthMessageReader(process.getInputStream());
        writer.writeMessage(request(99, "service/shutdown", JSON.createObjectNode()));
        response(reader);
        if (!process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroy();
            process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** Creates initialization parameters for the current protocol major. */
    private static ObjectNode initializeParams() {
        AuthoringServiceBuildInfo build = AuthoringServiceBuildInfo.current();
        ObjectNode version = JSON.createObjectNode().put("major", 1).put("minor", 0);
        ObjectNode params = JSON.createObjectNode()
                .put("contractIdentity", build.contractIdentity())
                .put("buildIdentity", build.buildIdentity())
                .put("clientLanguage", "en-GB");
        return params.set("protocolVersion", version);
    }

    /** Creates replacement parameters for one expected active generation and candidate path. */
    private static ObjectNode replaceParams(long expectedProjectGeneration, Path path) {
        return JSON.createObjectNode()
                .put("expectedProjectGeneration", expectedProjectGeneration)
                .put("path", path.toString());
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
