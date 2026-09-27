/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glynch.jscene3d.editor.authoring.protocol.framing.ProtocolFramingException;
import io.github.glynch.jscene3d.editor.authoring.service.AuthoringProjectService;
import io.github.glynch.jscene3d.editor.authoring.testing.AuthoringTestProject;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Exercises initialization, compatibility, dispatch ordering, and connection cleanup. */
final class AuthoringProtocolServerTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AuthoringProjectService service = new AuthoringProjectService(
            new EditorProjectLoader("0.1.0-SNAPSHOT", AuthoringProtocolServerTest.class.getClassLoader()));
    private final RecordingLogger logger = new RecordingLogger();
    private final AuthoringProtocolServer server =
            new AuthoringProtocolServer(service, "1.2.0-test", "0.1.0-SNAPSHOT", "connection-test", logger);

    @TempDir
    private Path temporaryDirectory;

    /** Releases service state even when an assertion fails. */
    @AfterEach
    void closeService() {
        service.close();
    }

    /** Negotiates the supported protocol and advertises only implemented authoring capabilities. */
    @Test
    void initializesAuthoringConnection() throws IOException {
        JsonNode response = response(initialize(1, 0));

        assertThat(response.path("id").asInt()).isEqualTo(1);
        assertThat(response.path("connectionGeneration").asText()).isEqualTo("connection-test");
        assertThat(response.at("/result/protocolVersion/major").asInt()).isEqualTo(1);
        assertThat(response.at("/result/protocolVersion/minor").asInt()).isZero();
        assertThat(response.at("/result/processKind").asText()).isEqualTo("authoring");
        assertThat(response.at("/result/serviceVersion").asText()).isEqualTo("1.2.0-test");
        assertThat(response.at("/result/engineVersion").asText()).isEqualTo("0.1.0-SNAPSHOT");
        assertThat(response.at("/result/capabilities"))
                .extracting(JsonNode::asText)
                .containsExactly(
                        "project/open",
                        "project/replace",
                        "project/close",
                        "definition/open",
                        "inspector/read",
                        "service/shutdown");
        assertThat(server.isInitialized()).isTrue();
        assertThat(server.clientLocale()).contains(Locale.forLanguageTag("en-GB"));
    }

    /** Rejects an incompatible major without initializing the connection. */
    @Test
    void rejectsIncompatibleMajorVersion() throws IOException {
        JsonNode response = response(initialize(2, 0));

        assertThat(response.at("/error/code").asInt()).isEqualTo(-32001);
        assertThat(response.at("/error/data/requested/major").asInt()).isEqualTo(2);
        assertThat(response.at("/error/data/supported/major").asInt()).isEqualTo(1);
        assertThat(server.isInitialized()).isFalse();
    }

    /** Negotiates the implemented minor when the client supports a newer compatible minor. */
    @Test
    void negotiatesCompatibleMinorVersion() throws IOException {
        JsonNode response = response(initialize(1, 7));

        assertThat(response.at("/result/protocolVersion/major").asInt()).isEqualTo(1);
        assertThat(response.at("/result/protocolVersion/minor").asInt()).isEqualTo(2);
        assertThat(server.isInitialized()).isTrue();
    }

    /** Rejects a second initialization without replacing negotiated connection state. */
    @Test
    void rejectsRepeatedInitialization() throws IOException {
        response(initialize(1, 0));

        JsonNode repeated = response(initialize(1, 0));

        assertThat(repeated.at("/error/code").asInt()).isEqualTo(-32003);
        assertThat(server.isInitialized()).isTrue();
    }

    /** Rejects project operations before initialization. */
    @Test
    void rejectsProjectRequestBeforeInitialization() throws IOException {
        JsonNode response = response(request(1, "project/close", "{}"));

        assertThat(response.at("/error/code").asInt()).isEqualTo(-32002);

        response(initialize(1, 0));
        JsonNode closed = response(request(2, "project/close", "{}"));

        assertThat(closed.at("/result/closed").asBoolean()).isFalse();
    }

    /** Preserves the initialization requirement before revealing whether an unknown method exists. */
    @Test
    void rejectsUnknownRequestBeforeInitialization() throws IOException {
        JsonNode response = response(request(1, "project/unknown", "{}"));

        assertThat(response.at("/error/code").asInt()).isEqualTo(-32002);
        assertThat(response.at("/error/data").isMissingNode()).isTrue();
    }

    /** Returns a JSON-RPC parse error for invalid JSON without exposing a stack trace. */
    @Test
    void reportsInvalidJson() throws IOException {
        JsonNode response = response("{not-json");

        assertThat(response.path("id").isNull()).isTrue();
        assertThat(response.at("/error/code").asInt()).isEqualTo(-32700);
        assertThat(response.toString()).doesNotContain("Exception", "stackTrace");
    }

    /** Rejects malformed JSON-RPC envelopes before dispatch. */
    @ParameterizedTest
    @MethodSource("invalidRequests")
    void reportsInvalidRequest(String request, String expectedId) throws IOException {
        JsonNode response = response(request);

        assertThat(response.at("/error/code").asInt()).isEqualTo(-32600);
        assertThat(response.path("id").toString()).isEqualTo(expectedId);
    }

    /** Rejects missing or structurally invalid request parameters. */
    @Test
    void reportsInvalidParams() throws IOException {
        JsonNode missingInitialize = response("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}");
        JsonNode scalarInitialize = response(request(2, "initialize", "true"));
        response(initialize(1, 0));
        JsonNode missingOpen = response("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"project/open\"}");
        JsonNode invalidOpen = response(request(4, "project/open", "{\"path\":\"   \"}"));
        JsonNode invalidReplace =
                response(request(5, "project/replace", "{\"expectedProjectGeneration\":0,\"path\":\"/project\"}"));

        assertThat(missingInitialize.at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(scalarInitialize.at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(missingOpen.at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(invalidOpen.at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(invalidReplace.at("/error/code").asInt()).isEqualTo(-32602);
    }

    /** Rejects absent, blank, and malformed client language tags without retaining a locale. */
    @Test
    void rejectsInvalidClientLanguage() throws IOException {
        JsonNode missing = response(request(1, "initialize", "{\"protocolVersion\":{\"major\":1,\"minor\":0}}"));
        JsonNode blank = response(
                request(2, "initialize", "{\"protocolVersion\":{\"major\":1,\"minor\":0},\"clientLanguage\":\" \"}"));
        JsonNode malformed = response(request(
                3, "initialize", "{\"protocolVersion\":{\"major\":1,\"minor\":0},\"clientLanguage\":\"en_US\"}"));

        assertThat(missing.at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(blank.at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(malformed.at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(server.clientLocale()).isEmpty();
    }

    /** Dispatches replacement as a structured domain result rather than a protocol failure. */
    @Test
    void dispatchesProjectReplacement() throws IOException {
        Path first = temporaryDirectory.resolve("first");
        Path invalid = temporaryDirectory.resolve("invalid");
        AuthoringTestProject.write(first, "first.j3d");
        response(initialize(1, 0));
        JsonNode opened = response(request(2, "project/open", "{\"path\":\"" + first + "\"}"));
        long generation = opened.at("/result/projectGeneration").asLong();

        JsonNode replacement = response(request(
                3,
                "project/replace",
                "{\"expectedProjectGeneration\":" + generation + ",\"path\":\"" + invalid + "\"}"));

        assertThat(replacement.at("/result/outcome").asText()).isEqualTo("candidateRejected");
        assertThat(replacement.at("/result/diagnostics/0/code").asText()).isEqualTo("project.directory.missing");

        JsonNode closed = response(request(4, "project/close", "{}"));

        assertThat(closed.at("/result/closed").asBoolean()).isTrue();
        assertThat(closed.at("/result/invalidatedProjectGeneration").asLong()).isEqualTo(generation);
    }

    /** Serializes a complete ordered hierarchy snapshot with stable semantic identities. */
    @Test
    void dispatchesDefinitionOpen() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        response(initialize(1, 1));
        JsonNode opened = response(request(2, "project/open", "{\"path\":\"" + temporaryDirectory + "\"}"));
        long generation = opened.at("/result/projectGeneration").asLong();

        JsonNode response = response(request(
                3,
                "definition/open",
                "{\"expectedProjectGeneration\":" + generation + ",\"assetId\":\"" + AuthoringTestProject.WORLD_ASSET_ID
                        + "\"}"));

        assertThat(response.at("/result/opened").asBoolean()).isTrue();
        assertThat(response.at("/result/projectGeneration").asLong()).isEqualTo(generation);
        assertThat(response.at("/result/definition/context/assetId").asText())
                .isEqualTo(AuthoringTestProject.WORLD_ASSET_ID);
        assertThat(response.at("/result/definition/context/kind").asText()).isEqualTo("world-definition");
        assertThat(response.at("/result/definition/context/origin").asText()).isEqualTo("authored");
        assertThat(response.at("/result/definition/roots/0/occurrence/entityPath/0")
                        .asText())
                .isEqualTo(AuthoringTestProject.ENTITY_ID);
        assertThat(response.at("/result/definition/roots/0/target/source").asText())
                .endsWith("worlds/main.world.json");
    }

    /** Dispatches one complete Inspector snapshot using the Java-issued hierarchy target. */
    @Test
    void dispatchesInspectorRead() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        response(initialize(1, 2));
        JsonNode opened = response(request(2, "project/open", "{\"path\":\"" + temporaryDirectory + "\"}"));
        long generation = opened.at("/result/projectGeneration").asLong();
        JsonNode definition = response(request(
                3,
                "definition/open",
                "{\"expectedProjectGeneration\":" + generation + ",\"assetId\":\"" + AuthoringTestProject.WORLD_ASSET_ID
                        + "\"}"));
        long revision = definition.at("/result/definition/revision").asLong();
        JsonNode target = definition.at("/result/definition/roots/0/target");
        String params = "{\"expectedProjectGeneration\":" + generation + ",\"expectedDefinitionRevision\":" + revision
                + ",\"target\":" + target + "}";

        JsonNode inspection = response(request(4, "inspector/read", params));

        assertThat(inspection.at("/result/read").asBoolean()).isTrue();
        assertThat(inspection.at("/result/projectGeneration").asLong()).isEqualTo(generation);
        assertThat(inspection.at("/result/snapshot/revision").asLong()).isEqualTo(revision);
        assertThat(inspection.at("/result/snapshot/title").asText()).isEqualTo("Player");
        assertThat(inspection.at("/result/snapshot/groups/0/kind").asText()).isEqualTo("entity");
        assertThat(inspection
                        .at("/result/snapshot/groups/0/properties/0/state/effectiveValue/kind")
                        .asText())
                .isEqualTo("boolean");
    }

    /** Ignores unknown optional initialization fields as required for additive evolution. */
    @Test
    void ignoresUnknownInitializationFields() throws IOException {
        String params = "{\"protocolVersion\":{\"major\":1,\"minor\":0,\"patch\":4},"
                + "\"clientLanguage\":\"en\",\"clientName\":\"test\"}";

        JsonNode response = response(request(1, "initialize", params));

        assertThat(response.path("result").isObject()).isTrue();
        assertThat(server.isInitialized()).isTrue();
    }

    /** Reports unknown methods only after initialization has established the connection. */
    @Test
    void reportsUnknownMethod() throws IOException {
        response(initialize(1, 0));

        JsonNode response = response(request(2, "project/unknown", "{}"));

        assertThat(response.at("/error/code").asInt()).isEqualTo(-32601);
        assertThat(response.at("/error/data").asText()).isEqualTo("project/unknown");
    }

    /** Converts an unexpected closed-service failure into the defined internal error response. */
    @Test
    void reportsUnexpectedServiceFailure() throws IOException {
        response(initialize(1, 0));
        service.close();

        JsonNode response = response(request(2, "project/close", "{}"));

        assertThat(response.at("/error/code").asInt()).isEqualTo(-32603);
        assertThat(response.at("/error/data").isMissingNode()).isTrue();
        assertThat(server.isShutdownRequested()).isTrue();
        assertThat(service.isClosed()).isTrue();
        assertThat(logger.level).isEqualTo(System.Logger.Level.ERROR);
        assertThat(logger.message).isEqualTo("Unexpected authoring request failure");
        assertThat(logger.thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authoring project service is closed");
    }

    /** Accepts shutdown, closes project ownership, and marks the dispatcher terminal. */
    @Test
    void shutsDownService() throws IOException {
        JsonNode response = response(request(1, "service/shutdown", "{}"));

        assertThat(response.at("/result/shutdown").asBoolean()).isTrue();
        assertThat(server.isShutdownRequested()).isTrue();
        assertThat(service.isClosed()).isTrue();
    }

    /** Runs framed dispatch, writes its response, and cleans up at terminal shutdown. */
    @Test
    void runsFramedRequestLoop() throws IOException {
        ByteArrayOutputStream framedRequest = new ByteArrayOutputStream();
        byte[] payload = request(1, "service/shutdown", "{}").getBytes(StandardCharsets.UTF_8);
        framedRequest.write(("Content-Length: " + payload.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        framedRequest.write(payload);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        server.run(new ByteArrayInputStream(framedRequest.toByteArray()), output);

        assertThat(output.toString(StandardCharsets.UTF_8)).contains("Content-Length:", "\"shutdown\":true");
        assertThat(service.isClosed()).isTrue();
    }

    /** Accepts the notification envelope without emitting a response or mutating service state. */
    @Test
    void acceptsOneWayNotificationEnvelope() throws IOException {
        Optional<String> response =
                server.processMessage("{\"jsonrpc\":\"2.0\",\"method\":\"client/ready\",\"params\":{}}");

        assertThat(response).isEmpty();
        assertThat(server.isInitialized()).isFalse();

        response(initialize(1, 0));
        JsonNode closed = response(request(2, "project/close", "{}"));

        assertThat(closed.at("/result/closed").asBoolean()).isFalse();
    }

    /** Treats clean owning-connection EOF as orderly process cleanup. */
    @Test
    void cleansUpOnEof() throws IOException {
        server.run(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());

        assertThat(service.isClosed()).isTrue();
    }

    /** Cleans service state when malformed transport input terminates the connection. */
    @Test
    void cleansUpOnMalformedFrame() {
        ByteArrayInputStream input =
                new ByteArrayInputStream("Content-Length: nope\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> server.run(input, output)).isInstanceOf(ProtocolFramingException.class);
        assertThat(service.isClosed()).isTrue();
    }

    /** Creates one initialize request. */
    private static String initialize(int major, int minor) {
        return request(
                1,
                "initialize",
                "{\"protocolVersion\":{\"major\":" + major + ",\"minor\":" + minor + "},\"clientLanguage\":\"en-GB\"}");
    }

    /** Creates one JSON-RPC-style request. */
    private static String request(int id, String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"" + method + "\",\"params\":" + params + "}";
    }

    /** Parses the response from one decoded request. */
    private JsonNode response(String request) throws IOException {
        return JSON.readTree(server.processMessage(request).orElseThrow());
    }

    /** Supplies structurally invalid envelopes and their preserved response identifiers. */
    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("null", "null"),
                Arguments.of("[]", "null"),
                Arguments.of("{\"jsonrpc\":2,\"id\":1,\"method\":\"project/close\"}", "1"),
                Arguments.of("{\"jsonrpc\":\"1.0\",\"id\":2,\"method\":\"project/close\"}", "2"),
                Arguments.of("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":4}", "3"),
                Arguments.of("{\"jsonrpc\":\"2.0\",\"id\":true,\"method\":\"project/close\"}", "true"));
    }

    /** Captures expected server diagnostics without writing them to the test process console. */
    private static final class RecordingLogger implements System.Logger {
        private @Nullable Level level;
        private @Nullable String message;
        private @Nullable Throwable thrown;

        @Override
        public String getName() {
            return "authoring-protocol-test";
        }

        @Override
        public boolean isLoggable(Level candidate) {
            return true;
        }

        @Override
        public void log(Level eventLevel, ResourceBundle bundle, String eventMessage, Throwable eventThrown) {
            level = eventLevel;
            message = eventMessage;
            thrown = eventThrown;
        }

        @Override
        public void log(Level eventLevel, ResourceBundle bundle, String format, Object... params) {
            level = eventLevel;
            message = format;
            thrown = null;
        }
    }
}
