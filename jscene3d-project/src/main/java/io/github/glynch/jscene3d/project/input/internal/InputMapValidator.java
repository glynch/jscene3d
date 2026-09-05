/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.input.InputMapDiagnosticCode;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.JsonPointers;
import io.github.glynch.jscene3d.project.internal.ProjectIdentifiers;
import io.github.glynch.jscene3d.project.internal.ProjectSchemaReferences;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Converts nullable Input Map JSON into one validated immutable definition. */
public final class InputMapValidator {
    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_URI = "https://jscene3d.org/schemas/input-map-1.json";
    private static final String LOCAL_SCHEMA_REFERENCE = "schema/input-map-1.schema.json";

    private final GameProject project;
    private final Path source;
    private final DiagnosticCollector diagnostics;

    /** Stores one input-map validation context. */
    private InputMapValidator(GameProject project, Path source) {
        this.project = project;
        this.source = source;
        diagnostics = new DiagnosticCollector(source);
    }

    /**
     * Validates one raw input-map definition.
     *
     * @param raw nullable deserialization model
     * @param project containing validated project
     * @param source canonical definition source path
     * @return validated definition or ordered diagnostics
     */
    public static ValidationResult validate(RawInputMap raw, GameProject project, Path source) {
        InputMapValidator validator = new InputMapValidator(project, source);
        Optional<InputMapDefinition> definition = validator.validate(raw);
        return new ValidationResult(definition, validator.diagnostics.diagnostics());
    }

    /** Validates fields in deterministic document order. */
    private Optional<InputMapDefinition> validate(RawInputMap raw) {
        validateSchema(raw.schema(), raw.schemaVersion());
        Map<String, List<InputBinding>> actions = validateActions(raw.actions());
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(new InputMapDefinition(source, actions));
    }

    /** Validates the authoritative version and optional schema reference. */
    private void validateSchema(@Nullable String schema, int schemaVersion) {
        if (schemaVersion != SCHEMA_VERSION) {
            diagnostics.error(
                    InputMapDiagnosticCode.SCHEMA_UNSUPPORTED,
                    "schemaVersion must be " + SCHEMA_VERSION + ": " + schemaVersion,
                    "/schemaVersion");
        }
        if (schema != null
                && !ProjectSchemaReferences.matches(
                        project.root(), source, schema, SCHEMA_URI, LOCAL_SCHEMA_REFERENCE)) {
            diagnostics.warning(
                    InputMapDiagnosticCode.SCHEMA_URI_INVALID,
                    "$schema does not identify the bundled Input Map version 1 schema",
                    "/$schema");
        }
    }

    /** Validates the non-empty semantic action index. */
    private Map<String, List<InputBinding>> validateActions(
            @Nullable Map<String, @Nullable List<RawInputMap.@Nullable Binding>> rawActions) {
        if (rawActions == null) {
            diagnostics.error(InputMapDiagnosticCode.ACTIONS_REQUIRED, "actions must be an object", "/actions");
            return Map.of();
        }
        if (rawActions.isEmpty()) {
            diagnostics.error(InputMapDiagnosticCode.ACTIONS_EMPTY, "actions must not be empty", "/actions");
            return Map.of();
        }
        Map<String, List<InputBinding>> actions = new LinkedHashMap<>();
        rawActions.forEach((action, bindings) -> validateAction(action, bindings, actions));
        return actions;
    }

    /** Validates one semantic action and its ordered physical bindings. */
    private void validateAction(
            String action,
            @Nullable List<RawInputMap.@Nullable Binding> rawBindings,
            Map<String, List<InputBinding>> actions) {
        String location = "/actions/" + JsonPointers.escapeSegment(action);
        boolean validAction = ProjectIdentifiers.isLocalId(action);
        if (!validAction) {
            diagnostics.error(
                    InputMapDiagnosticCode.ACTION_ID_INVALID,
                    "action identifiers must be portable lowercase identifiers",
                    location);
        }
        if (rawBindings == null || rawBindings.isEmpty()) {
            diagnostics.error(
                    InputMapDiagnosticCode.BINDINGS_EMPTY,
                    "an action requires at least one physical binding",
                    location);
            return;
        }
        List<InputBinding> bindings = new ArrayList<>();
        LinkedHashSet<InputBinding> unique = new LinkedHashSet<>();
        for (int index = 0; index < rawBindings.size(); index++) {
            String bindingLocation = location + '/' + index;
            Optional<InputBinding> candidate = validateBinding(rawBindings.get(index), bindingLocation);
            if (candidate.isPresent()) {
                InputBinding binding = candidate.orElseThrow();
                if (unique.add(binding)) {
                    bindings.add(binding);
                } else {
                    diagnostics.error(
                            InputMapDiagnosticCode.BINDING_DUPLICATE,
                            "physical binding is duplicated: " + binding.control(),
                            bindingLocation);
                }
            }
        }
        if (validAction && !bindings.isEmpty()) {
            actions.put(action, List.copyOf(bindings));
        }
    }

    /** Validates one device-specific physical binding. */
    private Optional<InputBinding> validateBinding(RawInputMap.@Nullable Binding raw, String location) {
        if (raw == null) {
            diagnostics.error(InputMapDiagnosticCode.BINDING_REQUIRED, "binding must be an object", location);
            return Optional.empty();
        }
        if (raw.device() == null || raw.device().isBlank()) {
            diagnostics.error(
                    InputMapDiagnosticCode.DEVICE_UNSUPPORTED,
                    "binding.device must be keyboard or mouse",
                    location + "/device");
            return Optional.empty();
        }
        return switch (raw.device()) {
            case "keyboard" -> keyboardBinding(raw, location);
            case "mouse" -> mouseBinding(raw, location);
            default -> {
                diagnostics.error(
                        InputMapDiagnosticCode.DEVICE_UNSUPPORTED,
                        "unsupported input device: " + raw.device(),
                        location + "/device");
                yield Optional.empty();
            }
        };
    }

    /** Validates one keyboard-key binding. */
    private Optional<InputBinding> keyboardBinding(RawInputMap.Binding raw, String location) {
        if (raw.button() != null) {
            diagnostics.error(
                    InputMapDiagnosticCode.CONTROL_CONFLICT,
                    "a keyboard binding cannot declare button",
                    location + "/button");
        }
        return control(raw.key(), InputBinding.Device.KEYBOARD, location + "/key");
    }

    /** Validates one mouse-button binding. */
    private Optional<InputBinding> mouseBinding(RawInputMap.Binding raw, String location) {
        if (raw.key() != null) {
            diagnostics.error(
                    InputMapDiagnosticCode.CONTROL_CONFLICT, "a mouse binding cannot declare key", location + "/key");
        }
        return control(raw.button(), InputBinding.Device.MOUSE_BUTTON, location + "/button");
    }

    /** Creates a binding when its device-specific control name is present. */
    private Optional<InputBinding> control(@Nullable String control, InputBinding.Device device, String location) {
        if (control == null || control.isBlank()) {
            diagnostics.error(
                    InputMapDiagnosticCode.CONTROL_REQUIRED, "the physical control name must not be blank", location);
            return Optional.empty();
        }
        return Optional.of(new InputBinding(device, control));
    }

    /** Validated input map and ordered diagnostics returned to the public loader.
     *
     * @param definition validated definition when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<InputMapDefinition> definition, List<ProjectDiagnostic> diagnostics) {}
}
