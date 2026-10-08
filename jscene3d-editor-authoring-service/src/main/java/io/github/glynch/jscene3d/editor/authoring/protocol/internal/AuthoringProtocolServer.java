/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionMutationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOperationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionRestoreParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.InitializeParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.InitializeResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectReplaceParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProtocolVersion;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ShutdownResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ViewportLaunchParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.framing.ContentLengthMessageReader;
import io.github.glynch.jscene3d.editor.authoring.protocol.framing.ContentLengthMessageWriter;
import io.github.glynch.jscene3d.editor.authoring.service.AuthoringProjectService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Serial JSON-RPC-style dispatcher for one owning stdio connection. */
public final class AuthoringProtocolServer {
    private static final System.Logger DEFAULT_LOGGER = System.getLogger(AuthoringProtocolServer.class.getName());
    private static final String JSON_RPC_VERSION = "2.0";
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;
    private static final int INCOMPATIBLE_PROTOCOL = -32001;
    private static final int NOT_INITIALIZED = -32002;
    private static final int ALREADY_INITIALIZED = -32003;
    private static final List<String> CAPABILITIES = AuthoringProtocolMethod.capabilities();

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final AuthoringProjectService service;
    private final String serviceVersion;
    private final String engineVersion;
    private final String contractIdentity;
    private final String buildIdentity;
    private final String connectionGeneration;
    private final System.Logger logger;

    private boolean initialized;
    private boolean shutdownRequested;
    private @Nullable Locale clientLocale;

    /**
     * Creates one connection-scoped protocol dispatcher.
     *
     * @param service retained authoring project owner
     * @param serviceVersion authoring-service implementation version
     * @param engineVersion JScene3D engine compatibility version
     * @param contractIdentity stable internal development contract identity
     * @param buildIdentity source-derived development build identity
     * @param connectionGeneration unique owning-connection generation
     */
    public AuthoringProtocolServer(
            AuthoringProjectService service,
            String serviceVersion,
            String engineVersion,
            String contractIdentity,
            String buildIdentity,
            String connectionGeneration) {
        this(
                service,
                serviceVersion,
                engineVersion,
                contractIdentity,
                buildIdentity,
                connectionGeneration,
                DEFAULT_LOGGER);
    }

    /**
     * Creates one connection-scoped protocol dispatcher with an explicit diagnostic sink.
     *
     * @param service retained authoring project owner
     * @param serviceVersion authoring-service implementation version
     * @param engineVersion JScene3D engine compatibility version
     * @param contractIdentity stable internal development contract identity
     * @param buildIdentity source-derived development build identity
     * @param connectionGeneration unique owning-connection generation
     * @param logger sparse unexpected-failure diagnostic sink
     */
    AuthoringProtocolServer(
            AuthoringProjectService service,
            String serviceVersion,
            String engineVersion,
            String contractIdentity,
            String buildIdentity,
            String connectionGeneration,
            System.Logger logger) {
        this.service = Objects.requireNonNull(service, "service");
        this.serviceVersion = Objects.requireNonNull(serviceVersion, "serviceVersion");
        this.engineVersion = Objects.requireNonNull(engineVersion, "engineVersion");
        this.contractIdentity = Objects.requireNonNull(contractIdentity, "contractIdentity");
        this.buildIdentity = Objects.requireNonNull(buildIdentity, "buildIdentity");
        this.connectionGeneration = Objects.requireNonNull(connectionGeneration, "connectionGeneration");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Owns framed input until shutdown or EOF and always cleans project-scoped state.
     *
     * @param input protocol input
     * @param output protocol-only output
     * @throws IOException when framing or transport fails
     */
    public void run(InputStream input, OutputStream output) throws IOException {
        ContentLengthMessageReader reader = new ContentLengthMessageReader(input);
        ContentLengthMessageWriter writer = new ContentLengthMessageWriter(output);
        try {
            while (!shutdownRequested) {
                Optional<String> message = reader.readMessage();
                if (message.isEmpty()) {
                    return;
                }
                Optional<String> response = processMessage(message.orElseThrow());
                if (response.isPresent()) {
                    writer.writeMessage(response.orElseThrow());
                }
            }
        } finally {
            service.close();
        }
    }

    /**
     * Dispatches one decoded JSON message and returns its response text.
     *
     * @param message decoded JSON request
     * @return response text, or empty for a one-way notification
     */
    public Optional<String> processMessage(String message) {
        Objects.requireNonNull(message, "message");
        @Nullable JsonNode request;
        try {
            request = mapper.readTree(message);
        } catch (JsonProcessingException exception) {
            return Optional.of(serialize(error(null, PARSE_ERROR, "Parse error", null)));
        }

        @Nullable JsonNode id = request == null || !request.isObject() ? null : request.get("id");
        if (!validMessage(request, id)) {
            return Optional.of(serialize(error(id, INVALID_REQUEST, "Invalid request", null)));
        }
        JsonNode validRequest = Objects.requireNonNull(request);
        if (id == null) {
            return Optional.empty();
        }
        JsonNode validId = Objects.requireNonNull(id);

        try {
            return Optional.of(serialize(dispatch(validRequest, validId)));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return Optional.of(serialize(error(validId, INVALID_PARAMS, "Invalid params", exception.getMessage())));
        } catch (RuntimeException exception) {
            shutdownRequested = true;
            service.close();
            logger.log(System.Logger.Level.ERROR, "Unexpected authoring request failure", exception);
            return Optional.of(serialize(error(validId, INTERNAL_ERROR, "Internal error", null)));
        }
    }

    /**
     * Returns whether initialization negotiation completed successfully.
     *
     * @return whether initialization completed
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns whether the owning connection accepted orderly shutdown.
     *
     * @return whether orderly shutdown was requested
     */
    public boolean isShutdownRequested() {
        return shutdownRequested;
    }

    /**
     * Returns the validated display locale retained for this initialized connection.
     *
     * @return initialized client locale, or empty before initialization
     */
    public Optional<Locale> clientLocale() {
        return Optional.ofNullable(clientLocale);
    }

    /** Dispatches one structurally valid request. */
    private ObjectNode dispatch(JsonNode request, JsonNode id) throws JsonProcessingException {
        String wireMethod = request.path("method").textValue();
        Optional<AuthoringProtocolMethod> method = AuthoringProtocolMethod.fromWireName(wireMethod);
        if (method.isEmpty()) {
            return initialized
                    ? error(id, METHOD_NOT_FOUND, "Method not found", wireMethod)
                    : error(id, NOT_INITIALIZED, "Service is not initialized", null);
        }
        AuthoringProtocolMethod resolvedMethod = method.orElseThrow();
        if (resolvedMethod.requiresInitialization() && !initialized) {
            return error(id, NOT_INITIALIZED, "Service is not initialized", null);
        }
        return switch (resolvedMethod) {
            case INITIALIZE -> initialize(id, request.get("params"));
            case PROJECT_OPEN ->
                success(id, service.openProject(readParams(request, ProjectOpenParams.class), initializedLocale()));
            case PROJECT_REPLACE ->
                success(
                        id,
                        service.replaceProject(readParams(request, ProjectReplaceParams.class), initializedLocale()));
            case PROJECT_CLOSE -> success(id, service.closeProject());
            case VIEWPORT_PREPARE_LAUNCH ->
                success(
                        id,
                        service.prepareViewportLaunch(
                                readParams(request, ViewportLaunchParams.class), initializedLocale()));
            case SCENE_VIEW_READ ->
                success(id, service.readSceneView(readParams(request, SceneViewReadParams.class), initializedLocale()));
            case DEFINITION_OPEN ->
                success(
                        id,
                        service.openDefinition(readParams(request, DefinitionOpenParams.class), initializedLocale()));
            case DEFINITION_MUTATE ->
                success(
                        id,
                        service.mutateDefinition(
                                readParams(request, DefinitionMutationParams.class), initializedLocale()));
            case DEFINITION_UNDO ->
                success(
                        id,
                        service.undoDefinition(
                                readParams(request, DefinitionOperationParams.class), initializedLocale()));
            case DEFINITION_REDO ->
                success(
                        id,
                        service.redoDefinition(
                                readParams(request, DefinitionOperationParams.class), initializedLocale()));
            case DEFINITION_SAVE ->
                success(
                        id,
                        service.saveDefinition(
                                readParams(request, DefinitionOperationParams.class), initializedLocale()));
            case DEFINITION_REVERT ->
                success(
                        id,
                        service.revertDefinition(
                                readParams(request, DefinitionOperationParams.class), initializedLocale()));
            case DEFINITION_BACKUP ->
                success(id, service.backupDefinition(readParams(request, DefinitionOperationParams.class)));
            case DEFINITION_RESTORE_BACKUP ->
                success(
                        id,
                        service.restoreDefinition(
                                readParams(request, DefinitionRestoreParams.class), initializedLocale()));
            case INSPECTOR_READ ->
                success(id, service.readInspector(readParams(request, InspectorReadParams.class), initializedLocale()));
            case SERVICE_SHUTDOWN -> shutdown(id);
        };
    }

    /** Performs terminal service shutdown. */
    private ObjectNode shutdown(JsonNode id) {
        shutdownRequested = true;
        service.close();
        return success(id, new ShutdownResult(true));
    }

    /** Negotiates one compatible protocol connection. */
    private ObjectNode initialize(JsonNode id, @Nullable JsonNode params) throws JsonProcessingException {
        if (initialized) {
            return error(id, ALREADY_INITIALIZED, "Service is already initialized", null);
        }
        if (params == null || !params.isObject()) {
            throw new IllegalArgumentException("params must be an object");
        }
        InitializeParams offered = mapper.treeToValue(params, InitializeParams.class);
        ProtocolVersion requested = offered.protocolVersion();
        if (!requested.equals(ProtocolVersion.CURRENT)
                || !contractIdentity.equals(offered.contractIdentity())
                || !buildIdentity.equals(offered.buildIdentity())) {
            ObjectNode data = JsonNodeFactory.instance.objectNode();
            data.set("requested", mapper.valueToTree(requested));
            data.set("supported", mapper.valueToTree(ProtocolVersion.CURRENT));
            data.put("requestedContractIdentity", offered.contractIdentity());
            data.put("supportedContractIdentity", contractIdentity);
            data.put("requestedBuildIdentity", offered.buildIdentity());
            data.put("supportedBuildIdentity", buildIdentity);
            return error(id, INCOMPATIBLE_PROTOCOL, "Incompatible authoring protocol or development build", data);
        }
        clientLocale = offered.clientLocale();
        initialized = true;
        return success(
                id,
                new InitializeResult(
                        ProtocolVersion.CURRENT,
                        contractIdentity,
                        buildIdentity,
                        "authoring",
                        serviceVersion,
                        engineVersion,
                        CAPABILITIES));
    }

    /** Reads required object parameters as one explicit wire DTO. */
    private <T> T readParams(JsonNode request, Class<T> type) throws JsonProcessingException {
        JsonNode params = request.get("params");
        if (params == null || !params.isObject()) {
            throw new IllegalArgumentException("params must be an object");
        }
        return mapper.treeToValue(params, type);
    }

    /** Returns the locale established by successful initialization. */
    private Locale initializedLocale() {
        return Objects.requireNonNull(clientLocale, "clientLocale");
    }

    /** Validates the minimal request or notification envelope before method dispatch. */
    private static boolean validMessage(@Nullable JsonNode request, @Nullable JsonNode id) {
        return request != null
                && request.isObject()
                && request.path("jsonrpc").isTextual()
                && JSON_RPC_VERSION.equals(request.path("jsonrpc").textValue())
                && request.path("method").isTextual()
                && (id == null || id.isTextual() || id.isIntegralNumber());
    }

    /** Creates a successful response carrying the owning connection generation. */
    private ObjectNode success(JsonNode id, Object result) {
        ObjectNode response = baseResponse(id);
        response.set("result", mapper.valueToTree(result));
        return response;
    }

    /** Creates a protocol or compatibility error without exposing implementation failures. */
    private ObjectNode error(@Nullable JsonNode id, int code, String message, @Nullable Object data) {
        ObjectNode response = baseResponse(id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.set("data", mapper.valueToTree(data));
        }
        return response;
    }

    /** Creates the common response envelope. */
    private ObjectNode baseResponse(@Nullable JsonNode id) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.set("id", id == null ? JsonNodeFactory.instance.nullNode() : id);
        response.put("connectionGeneration", connectionGeneration);
        return response;
    }

    /** Serializes one already-mapped response. */
    private String serialize(ObjectNode response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Protocol response could not be serialized", exception);
        }
    }
}
