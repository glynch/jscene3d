/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.InspectorSnapshot;
import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProjection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorProperty;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorSection;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorValue;
import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyNumericBound;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Maps Java-authoritative Inspector projections to locale-resolved protocol snapshots. */
final class InspectorProtocolMapper {
    private static final MessageSource MESSAGES = new ResourceBundleMessageSource(
            AuthoringText.class.getModule(), "io.github.glynch.jscene3d.editor.presentation.messages");

    private final DefinitionProtocolMapper definitions;

    InspectorProtocolMapper(DefinitionProtocolMapper definitions) {
        this.definitions = definitions;
    }

    InspectorSnapshot snapshot(long revision, InspectorProjection projection, Locale locale) {
        return new InspectorSnapshot(
                revision,
                definitions.target(projection.target()),
                resolve(projection.title(), locale),
                projection.definitionOrigin().name().toLowerCase(Locale.ROOT),
                projection.provenance().name().toLowerCase(Locale.ROOT),
                projection.editable(),
                projection.sections().stream()
                        .map(section -> group(section, locale))
                        .toList());
    }

    private InspectorSnapshot.TargetGroup group(InspectorSection section, Locale locale) {
        return new InspectorSnapshot.TargetGroup(
                section.identity(),
                section.kind().name().toLowerCase(Locale.ROOT),
                resolve(section.label(), locale),
                section.description().map(value -> resolve(value, locale)).orElse(null),
                section.componentId().map(Object::toString).orElse(null),
                section.componentType()
                        .map(InspectorProtocolMapper::componentType)
                        .orElse(null),
                section.metadataAvailable() ? "available" : "unavailable",
                section.editable(),
                section.properties().stream()
                        .map(property -> property(property, locale))
                        .toList());
    }

    private InspectorSnapshot.Property property(InspectorProperty property, Locale locale) {
        InspectorProperty.Presentation presentation = property.presentation();
        InspectorProperty.State state = property.state();
        return new InspectorSnapshot.Property(
                property.identity(),
                resolve(presentation.label(), locale),
                presentation.description().map(value -> resolve(value, locale)).orElse(null),
                valueKind(presentation.valueKind()),
                presentation.required(),
                new InspectorSnapshot.Constraints(
                        presentation
                                .constraints()
                                .elementKind()
                                .map(InspectorProtocolMapper::valueKind)
                                .orElse(null),
                        presentation.constraints().exactElementCount().orElse(null),
                        presentation.constraints().acceptedReferenceKinds().stream()
                                .map(InspectorProtocolMapper::referenceKind)
                                .sorted()
                                .toList(),
                        new InspectorSnapshot.EditorSemantics(
                                presentation.constraints().editor().semantic().serializedName(),
                                presentation
                                        .constraints()
                                        .editor()
                                        .minimum()
                                        .map(this::numericBound)
                                        .orElse(null),
                                presentation
                                        .constraints()
                                        .editor()
                                        .maximum()
                                        .map(this::numericBound)
                                        .orElse(null))),
                new InspectorSnapshot.PropertyState(
                        state.authoredValue().map(value -> value(value, locale)).orElse(null),
                        state.defaultValue().map(value -> value(value, locale)).orElse(null),
                        state.effectiveValue()
                                .map(value -> value(value, locale))
                                .orElse(null),
                        state.origin().name().toLowerCase(Locale.ROOT),
                        state.validity().name().toLowerCase(Locale.ROOT).replace('_', '-'),
                        state.editable(),
                        state.modified()),
                property.mutationTarget().map(this::mutationTarget).orElse(null));
    }

    private InspectorSnapshot.Value value(InspectorValue value, Locale locale) {
        return switch (value) {
            case InspectorValue.NullValue ignored -> new InspectorSnapshot.NullValue("null");
            case InspectorValue.BooleanValue(boolean booleanValue) ->
                new InspectorSnapshot.BooleanValue("boolean", booleanValue);
            case InspectorValue.NumberValue(BigDecimal number) ->
                new InspectorSnapshot.NumberValue("number", number.toPlainString());
            case InspectorValue.TextValue(String text) -> new InspectorSnapshot.TextValue("text", text);
            case InspectorValue.ArrayValue(List<InspectorValue> entries) ->
                new InspectorSnapshot.ArrayValue(
                        "array",
                        entries.stream().map(entry -> value(entry, locale)).toList());
            case InspectorValue.ObjectValue(Map<String, InspectorValue> entries) -> {
                LinkedHashMap<String, InspectorSnapshot.Value> values = new LinkedHashMap<>();
                entries.forEach((key, entry) -> values.put(key, value(entry, locale)));
                yield new InspectorSnapshot.ObjectValue("object", values);
            }
            case InspectorValue.ReferenceValue(
                    ResourceReference reference,
                    AuthoringText label,
                    InspectorValue.Resolution resolution,
                    Optional<URI> revealUri) ->
                new InspectorSnapshot.ReferenceValue(
                        "reference",
                        referenceKind(reference.kind()),
                        reference.locator(),
                        resolve(label, locale),
                        resolution(resolution),
                        revealUri.map(Object::toString).orElse(null));
            case InspectorValue.EntityTargetValue(
                    EntityId entity,
                    AuthoringText label,
                    InspectorValue.Resolution resolution,
                    Optional<HierarchyOccurrenceId> occurrence) ->
                new InspectorSnapshot.EntityTargetValue(
                        "entity-target",
                        entity.toString(),
                        resolve(label, locale),
                        resolution(resolution),
                        occurrence.map(definitions::occurrence).orElse(null));
            case InspectorValue.ComponentTargetValue(
                    ComponentTarget target,
                    AuthoringText entityLabel,
                    AuthoringText componentLabel,
                    Optional<ComponentType> targetType,
                    InspectorValue.Resolution resolution,
                    Optional<HierarchyOccurrenceId> occurrence) ->
                new InspectorSnapshot.ComponentTargetValue(
                        "component-target",
                        target.entity().toString(),
                        target.component().toString(),
                        resolve(entityLabel, locale),
                        resolve(componentLabel, locale),
                        targetType.map(InspectorProtocolMapper::componentType).orElse(null),
                        resolution(resolution),
                        occurrence.map(definitions::occurrence).orElse(null));
        };
    }

    private InspectorSnapshot.MutationTarget mutationTarget(InspectorMutationTarget target) {
        return switch (target) {
            case InspectorMutationTarget.EntityEnabled(HierarchyOccurrenceId occurrence, EntityId entity) ->
                new InspectorSnapshot.EntityEnabledMutation(
                        "entity-enabled", definitions.occurrence(occurrence), entity.toString());
            case InspectorMutationTarget.ComponentProperty(
                    HierarchyOccurrenceId occurrence,
                    EntityId entity,
                    ComponentId component,
                    PropertyId property) ->
                new InspectorSnapshot.ComponentPropertyMutation(
                        "component-property",
                        definitions.occurrence(occurrence),
                        entity.toString(),
                        component.toString(),
                        property.value());
        };
    }

    private InspectorSnapshot.NumericBound numericBound(PropertyNumericBound bound) {
        return new InspectorSnapshot.NumericBound(bound.value().toPlainString(), bound.isInclusive());
    }

    private static InspectorSnapshot.ComponentTypeDto componentType(ComponentType type) {
        return new InspectorSnapshot.ComponentTypeDto(type.id().value(), type.version());
    }

    private static String resolve(AuthoringText text, Locale locale) {
        return text.resolve(MESSAGES, locale);
    }

    private static String valueKind(ProjectValueKind kind) {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String referenceKind(ResourceReference.Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private static String resolution(InspectorValue.Resolution resolution) {
        return resolution.name().toLowerCase(Locale.ROOT);
    }
}
