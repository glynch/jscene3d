/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.input.InputActionDefinition;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.input.InputMapDiagnosticCode;
import io.github.glynch.jscene3d.project.input.InputValueType;
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
    private static final float DEFAULT_DEAD_ZONE = 0.15F;

    private final GameProject project;
    private final Path source;
    private final DiagnosticCollector diagnostics;

    /** Stores one input-map validation context. */
    private InputMapValidator(GameProject project, Path source) {
        this.project = project;
        this.source = source;
        diagnostics = new DiagnosticCollector(source);
    }

    /** Validates one raw input-map definition.
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
        Map<String, InputActionDefinition> actions = validateActions(raw.actions());
        return diagnostics.hasErrors() ? Optional.empty() : Optional.of(new InputMapDefinition(source, actions));
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
    private Map<String, InputActionDefinition> validateActions(
            @Nullable Map<String, RawInputMap.@Nullable Action> rawActions) {
        if (rawActions == null) {
            diagnostics.error(InputMapDiagnosticCode.ACTIONS_REQUIRED, "actions must be an object", "/actions");
            return Map.of();
        }
        if (rawActions.isEmpty()) {
            diagnostics.error(InputMapDiagnosticCode.ACTIONS_EMPTY, "actions must not be empty", "/actions");
            return Map.of();
        }
        Map<String, InputActionDefinition> actions = new LinkedHashMap<>();
        rawActions.forEach((action, raw) -> validateAction(action, raw, actions));
        return actions;
    }

    /** Validates one typed semantic action and its ordered physical bindings. */
    private void validateAction(
            String action, RawInputMap.@Nullable Action raw, Map<String, InputActionDefinition> actions) {
        String location = "/actions/" + JsonPointers.escapeSegment(action);
        boolean validAction = ProjectIdentifiers.isLocalId(action);
        if (!validAction) {
            diagnostics.error(
                    InputMapDiagnosticCode.ACTION_ID_INVALID,
                    "action identifiers must be portable lowercase identifiers",
                    location);
        }
        if (raw == null) {
            diagnostics.error(InputMapDiagnosticCode.BINDING_REQUIRED, "action must be an object", location);
            return;
        }
        Optional<InputValueType> valueType = valueType(raw.valueType(), location + "/valueType");
        List<InputBinding> bindings = validateBindings(raw.bindings(), valueType, location + "/bindings");
        if (validAction && valueType.isPresent() && !bindings.isEmpty()) {
            actions.put(action, new InputActionDefinition(valueType.orElseThrow(), bindings));
        }
    }

    /** Converts one portable value-type name. */
    private Optional<InputValueType> valueType(@Nullable String raw, String location) {
        InputValueType value =
                switch (raw == null ? "" : raw) {
                    case "button" -> InputValueType.BUTTON;
                    case "axis-1d" -> InputValueType.AXIS_1D;
                    case "axis-2d" -> InputValueType.AXIS_2D;
                    default -> null;
                };
        if (value == null) {
            diagnostics.error(
                    InputMapDiagnosticCode.VALUE_TYPE_UNSUPPORTED,
                    "valueType must be button, axis-1d, or axis-2d: " + raw,
                    location);
        }
        return Optional.ofNullable(value);
    }

    /** Validates a non-empty unique binding list. */
    private List<InputBinding> validateBindings(
            @Nullable List<RawInputMap.@Nullable Binding> rawBindings,
            Optional<InputValueType> valueType,
            String location) {
        if (rawBindings == null || rawBindings.isEmpty()) {
            diagnostics.error(
                    InputMapDiagnosticCode.BINDINGS_EMPTY,
                    "an action requires at least one physical binding",
                    location);
            return List.of();
        }
        List<InputBinding> bindings = new ArrayList<>();
        LinkedHashSet<InputBinding> unique = new LinkedHashSet<>();
        for (int index = 0; index < rawBindings.size(); index++) {
            String bindingLocation = location + '/' + index;
            validateBinding(rawBindings.get(index), bindingLocation).ifPresent(binding -> {
                if (validateValueType(valueType, binding, bindingLocation) && unique.add(binding)) {
                    bindings.add(binding);
                } else if (valueType.orElse(null) == binding.valueType()) {
                    diagnostics.error(
                            InputMapDiagnosticCode.BINDING_DUPLICATE,
                            "physical binding is duplicated: " + binding,
                            bindingLocation);
                }
            });
        }
        return bindings;
    }

    /** Reports bindings whose output shape differs from their action. */
    private boolean validateValueType(Optional<InputValueType> valueType, InputBinding binding, String location) {
        if (valueType.isPresent() && valueType.orElseThrow() != binding.valueType()) {
            diagnostics.error(
                    InputMapDiagnosticCode.VALUE_TYPE_MISMATCH,
                    "binding produces " + binding.valueType() + " but action requires " + valueType.orElseThrow(),
                    location);
            return false;
        }
        return true;
    }

    /** Validates one device-specific physical binding. */
    private Optional<InputBinding> validateBinding(RawInputMap.@Nullable Binding raw, String location) {
        if (raw == null) {
            diagnostics.error(InputMapDiagnosticCode.BINDING_REQUIRED, "binding must be an object", location);
            return Optional.empty();
        }
        try {
            return switch (raw.device() == null ? "" : raw.device()) {
                case "keyboard" -> keyboardBinding(raw, location);
                case "mouse" -> mouseBinding(raw, location);
                case "gamepad" -> gamepadBinding(raw, location);
                default -> unsupportedDevice(raw.device(), location);
            };
        } catch (IllegalArgumentException failure) {
            diagnostics.error(InputMapDiagnosticCode.CONFIGURATION_INVALID, failure.getMessage(), location);
            return Optional.empty();
        }
    }

    /** Creates one keyboard key or four-key directional composite. */
    private Optional<InputBinding> keyboardBinding(RawInputMap.Binding raw, String location) {
        if ("directional".equals(raw.control())) {
            return Optional.of(new InputBinding.DirectionalKeys(
                    required(raw.up(), location + "/up"),
                    required(raw.down(), location + "/down"),
                    required(raw.left(), location + "/left"),
                    required(raw.right(), location + "/right")));
        }
        return Optional.of(new InputBinding.KeyboardKey(required(raw.control(), location + "/control")));
    }

    /** Creates one mouse button or relative pointer-delta binding. */
    private Optional<InputBinding> mouseBinding(RawInputMap.Binding raw, String location) {
        String control = required(raw.control(), location + "/control");
        return Optional.of(
                "delta".equals(control)
                        ? new InputBinding.MouseDelta(orDefault(raw.scaleX(), 1.0F), orDefault(raw.scaleY(), 1.0F))
                        : new InputBinding.MouseButton(control));
    }

    /** Creates one standard gamepad button, axis, or stick binding. */
    private Optional<InputBinding> gamepadBinding(RawInputMap.Binding raw, String location) {
        String control = required(raw.control(), location + "/control");
        if (control.endsWith("-stick")) {
            return Optional.of(new InputBinding.GamepadStick(
                    control, orDefault(raw.deadZone(), DEFAULT_DEAD_ZONE), Boolean.TRUE.equals(raw.invertY())));
        }
        if (control.endsWith("-x") || control.endsWith("-y") || control.endsWith("-trigger")) {
            return Optional.of(new InputBinding.GamepadAxis(
                    control, orDefault(raw.deadZone(), DEFAULT_DEAD_ZONE), orDefault(raw.scale(), 1.0F)));
        }
        return Optional.of(new InputBinding.GamepadButton(control));
    }

    /** Reports one unsupported device. */
    private Optional<InputBinding> unsupportedDevice(@Nullable String device, String location) {
        diagnostics.error(
                InputMapDiagnosticCode.DEVICE_UNSUPPORTED,
                "binding.device must be keyboard, mouse, or gamepad: " + device,
                location + "/device");
        return Optional.empty();
    }

    /** Requires one non-blank physical control name. */
    private String required(@Nullable String control, String location) {
        if (control == null || control.isBlank()) {
            diagnostics.error(
                    InputMapDiagnosticCode.CONTROL_REQUIRED, "the physical control name must not be blank", location);
            throw new IllegalArgumentException("the physical control name must not be blank");
        }
        return control;
    }

    /** Uses an authored option or its format default. */
    private static float orDefault(@Nullable Float value, float defaultValue) {
        return value == null ? defaultValue : value;
    }

    /** Validated input map and ordered diagnostics returned to the public loader.
     *
     * @param definition validated definition when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<InputMapDefinition> definition, List<ProjectDiagnostic> diagnostics) {}
}
