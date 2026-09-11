/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/** Projects validated project data into immutable, non-executable Inspector data. */
final class EditorInspectorProjector {
    private EditorInspectorProjector() {}

    /** Projects the opened world root. */
    static EditorSelection world(WorldDefinition world, Path source, Path projectRoot) {
        List<EditorInspectorView.Property> properties = List.of(
                textProperty("asset-id", "Asset ID", world.id().toString()),
                numberProperty("root-count", "Root entities", world.roots().size()),
                numberProperty(
                        "connection-count", "Connections", world.connections().size()));
        EditorInspectorView view = view(
                world.name(),
                "World definition",
                source,
                projectRoot,
                world.id().toString(),
                false,
                List.of(section("World", properties)));
        return selection(EditorSelection.Kind.WORLD, source, world.id().toString(), view);
    }

    /** Projects one local or generated entity and its descriptor-backed components. */
    static EditorSelection entity(
            LocalEntity entity, Path source, Path projectRoot, RegisteredTypeCatalog types, boolean generated) {
        String title = entity.name().orElse("Unnamed entity");
        List<EditorInspectorView.Section> sections = new ArrayList<>();
        sections.add(section(
                "Entity",
                List.of(
                        booleanProperty("enabled", "Enabled", entity.isEnabled()),
                        numberProperty(
                                "child-count", "Children", entity.children().size()))));
        sections.addAll(componentSections(entity.components(), types));
        EditorInspectorView view = view(
                title,
                generated ? "Generated entity" : "Local entity",
                source,
                projectRoot,
                entity.id().toString(),
                generated,
                sections);
        EditorSelection.Kind kind =
                generated ? EditorSelection.Kind.GENERATED_ENTITY : EditorSelection.Kind.LOCAL_ENTITY;
        return selection(kind, source, entity.id().toString(), view);
    }

    /** Projects an authored reusable-definition placement and its realized root components. */
    static EditorSelection placement(
            EntityPlacement placement,
            Optional<EntityDefinition> definition,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated) {
        String title = placement
                .name()
                .orElseGet(() -> definition.map(EntityDefinition::name).orElse("Unavailable definition"));
        List<EditorInspectorView.Property> placementProperties = new ArrayList<>();
        placementProperties.add(textProperty(
                "definition", "Definition", placement.definition().id().toString()));
        placementProperties.add(booleanProperty("enabled", "Enabled", placement.isEnabled()));
        placement.arguments().forEach((id, value) -> placementProperties.add(authoredProperty(id, value)));
        List<EditorInspectorView.Section> sections = new ArrayList<>();
        sections.add(section("Placement", placementProperties));
        definition.ifPresent(
                value -> sections.addAll(componentSections(value.root().components(), types)));
        EditorInspectorView view = view(
                title,
                generated ? "Generated placement" : "Entity-definition placement",
                source,
                projectRoot,
                placement.id().toString(),
                generated,
                sections);
        return selection(EditorSelection.Kind.PLACEMENT, source, placement.id().toString(), view);
    }

    /** Projects one reusable entity-definition asset. */
    static EditorSelection entityDefinition(
            EntityDefinition definition, Path source, Path projectRoot, RegisteredTypeCatalog types) {
        List<EditorInspectorView.Section> sections = new ArrayList<>();
        sections.add(section(
                "Definition",
                List.of(
                        textProperty("asset-id", "Asset ID", definition.id().toString()),
                        numberProperty(
                                "parameters",
                                "Parameters",
                                definition.contract().parameters().size()),
                        numberProperty(
                                "connections",
                                "Connections",
                                definition.connections().size()))));
        sections.addAll(componentSections(definition.root().components(), types));
        EditorInspectorView view = view(
                definition.name(),
                "Entity definition",
                source,
                projectRoot,
                definition.id().toString(),
                false,
                sections);
        return selection(EditorSelection.Kind.ASSET, source, definition.id().toString(), view);
    }

    /** Projects one world-definition asset. */
    static EditorSelection worldDefinition(WorldDefinition world, Path source, Path projectRoot) {
        EditorInspectorView view = view(
                world.name(),
                "World definition",
                source,
                projectRoot,
                world.id().toString(),
                false,
                List.of(section(
                        "Definition",
                        List.of(
                                textProperty("asset-id", "Asset ID", world.id().toString()),
                                numberProperty(
                                        "root-count",
                                        "Root entities",
                                        world.roots().size()),
                                numberProperty(
                                        "connection-count",
                                        "Connections",
                                        world.connections().size())))));
        return selection(EditorSelection.Kind.ASSET, source, world.id().toString(), view);
    }

    /** Projects one authoritative source asset declared by the manifest. */
    static EditorSelection sourceAsset(GameProject.AssetSource asset, Path projectRoot) {
        List<EditorInspectorView.Property> properties = new ArrayList<>();
        properties.add(textProperty("identity", "Identity", asset.id()));
        properties.add(textProperty("type", "Type", asset.type()));
        asset.sha256().ifPresent(hash -> properties.add(textProperty("sha256", "SHA-256", hash)));
        EditorInspectorView view = view(
                asset.id(),
                "Source asset",
                asset.path(),
                projectRoot,
                asset.id(),
                false,
                List.of(section("Asset", properties)));
        return selection(EditorSelection.Kind.ASSET, asset.path(), asset.id(), view);
    }

    /** Projects one deterministic source-import definition and its authored settings. */
    static EditorSelection importDefinition(ImportDefinition definition, Path projectRoot) {
        List<EditorInspectorView.Property> properties = new ArrayList<>();
        properties.add(textProperty("identity", "Identity", definition.id()));
        properties.add(textProperty("importer", "Importer", definition.importer()));
        properties.add(textProperty("asset", "Source asset", definition.asset().id()));
        properties.add(numberProperty(
                "selection-count", "Selected items", definition.selection().size()));
        definition.settings().forEach((id, value) -> properties.add(authoredProperty(new PropertyId(id), value)));
        EditorInspectorView view = view(
                definition.id(),
                "Import definition",
                definition.source(),
                projectRoot,
                definition.id(),
                false,
                List.of(section("Import", properties)));
        return selection(EditorSelection.Kind.ASSET, definition.source(), definition.id(), view);
    }

    /** Projects component definitions in authored order using exact descriptor versions. */
    private static List<EditorInspectorView.Section> componentSections(
            List<ComponentDefinition> components, RegisteredTypeCatalog types) {
        return components.stream()
                .map(component -> componentSection(component, types))
                .toList();
    }

    /** Projects one component while retaining authored values even if metadata is unavailable. */
    private static EditorInspectorView.Section componentSection(
            ComponentDefinition component, RegisteredTypeCatalog types) {
        ComponentType type = new ComponentType(component.type(), component.typeVersion());
        return types.findComponent(type)
                .map(descriptor -> descriptorSection(component, descriptor.presentation(), descriptor.properties()))
                .orElseGet(() -> missingDescriptorSection(component));
    }

    /** Applies descriptor presentation, property order, defaults, and constraints. */
    private static EditorInspectorView.Section descriptorSection(
            ComponentDefinition component,
            DescriptorPresentation presentation,
            Map<PropertyId, PropertyDescriptor> descriptors) {
        List<EditorInspectorView.Property> properties = new ArrayList<>();
        for (Map.Entry<PropertyId, PropertyDescriptor> entry : descriptors.entrySet()) {
            PropertyId id = entry.getKey();
            PropertyDescriptor descriptor = entry.getValue();
            Optional<ProjectValue> authored =
                    Optional.ofNullable(component.properties().get(id));
            Optional<ProjectValue> displayed = authored.or(descriptor::defaultValue);
            EditorInspectorView.ValueOrigin origin;
            if (authored.isPresent()) {
                origin = EditorInspectorView.ValueOrigin.AUTHORED;
            } else if (displayed.isPresent()) {
                origin = EditorInspectorView.ValueOrigin.DEFAULT;
            } else {
                origin = EditorInspectorView.ValueOrigin.UNSET;
            }
            properties.add(new EditorInspectorView.Property(
                    id.value(),
                    descriptor.presentation().displayName(),
                    descriptor.valueKind(),
                    displayed.map(EditorInspectorProjector::format).orElse("Not set"),
                    origin,
                    descriptor.isRequired(),
                    descriptor.presentation().description(),
                    constraints(descriptor)));
        }
        component.properties().forEach((id, value) -> {
            if (!descriptors.containsKey(id)) {
                properties.add(authoredProperty(id, value));
            }
        });
        return new EditorInspectorView.Section(
                presentation.displayName(), presentation.description(), true, properties);
    }

    /** Preserves authored values when exact safe descriptor metadata could not be resolved. */
    private static EditorInspectorView.Section missingDescriptorSection(ComponentDefinition component) {
        List<EditorInspectorView.Property> properties = component.properties().entrySet().stream()
                .map(entry -> authoredProperty(entry.getKey(), entry.getValue()))
                .toList();
        return new EditorInspectorView.Section(
                component.type().value(),
                Optional.of("Descriptor metadata unavailable for type version " + component.typeVersion()),
                false,
                properties);
    }

    /** Retains structural constraints without teaching the editor component-specific semantics. */
    private static Map<String, String> constraints(PropertyDescriptor descriptor) {
        Map<String, String> constraints = new LinkedHashMap<>();
        descriptor.elementKind().ifPresent(kind -> constraints.put("Element type", label(kind)));
        if (!descriptor.acceptedReferenceKinds().isEmpty()) {
            String accepted = descriptor.acceptedReferenceKinds().stream()
                    .map(kind -> kind.prefix().substring(0, kind.prefix().length() - 1))
                    .sorted()
                    .collect(Collectors.joining(", "));
            constraints.put("Accepted references", accepted);
        }
        descriptor.editorMetadata().forEach((key, value) -> constraints.put(key, format(value)));
        return constraints;
    }

    /** Creates one ordinary authored property without descriptor presentation metadata. */
    private static EditorInspectorView.Property authoredProperty(PropertyId id, ProjectValue value) {
        return new EditorInspectorView.Property(
                id.value(),
                displayName(id.value()),
                ProjectValueKind.of(value),
                format(value),
                EditorInspectorView.ValueOrigin.AUTHORED,
                false,
                Optional.empty(),
                Map.of());
    }

    /** Creates one text-valued summary row. */
    private static EditorInspectorView.Property textProperty(String id, String name, String value) {
        return summaryProperty(id, name, ProjectValueKind.TEXT, value);
    }

    /** Creates one numeric summary row. */
    private static EditorInspectorView.Property numberProperty(String id, String name, int value) {
        return summaryProperty(id, name, ProjectValueKind.NUMBER, Integer.toString(value));
    }

    /** Creates one boolean summary row. */
    private static EditorInspectorView.Property booleanProperty(String id, String name, boolean value) {
        return summaryProperty(id, name, ProjectValueKind.BOOLEAN, Boolean.toString(value));
    }

    /** Creates a projected summary row backed by immutable loaded data. */
    private static EditorInspectorView.Property summaryProperty(
            String id, String name, ProjectValueKind kind, String value) {
        return new EditorInspectorView.Property(
                id, name, kind, value, EditorInspectorView.ValueOrigin.AUTHORED, false, Optional.empty(), Map.of());
    }

    /** Creates one ordinary Inspector section. */
    private static EditorInspectorView.Section section(String title, List<EditorInspectorView.Property> properties) {
        return new EditorInspectorView.Section(title, Optional.empty(), true, properties);
    }

    /** Creates the common immutable view header. */
    private static EditorInspectorView view(
            String title,
            String kind,
            Path source,
            Path projectRoot,
            String identity,
            boolean generated,
            List<EditorInspectorView.Section> sections) {
        return new EditorInspectorView(title, kind, displaySource(source, projectRoot), identity, generated, sections);
    }

    /** Creates a source-scoped stable selection key because entity IDs are asset-local. */
    private static EditorSelection selection(
            EditorSelection.Kind kind, Path source, String identity, EditorInspectorView view) {
        String key = kind.name() + ':' + source.toAbsolutePath().normalize() + ':' + identity;
        return new EditorSelection(kind, key, view);
    }

    /** Displays project-local sources without discarding the complete normalized source in the selection key. */
    private static String displaySource(Path source, Path projectRoot) {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        return normalizedSource.startsWith(normalizedRoot)
                ? normalizedRoot.relativize(normalizedSource).toString()
                : normalizedSource.toString();
    }

    /** Formats closed portable values without depending on a JSON implementation. */
    static String format(ProjectValue value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case ProjectValue.NullValue ignored -> "null";
            case ProjectValue.BooleanValue booleanValue -> Boolean.toString(booleanValue.value());
            case ProjectValue.NumberValue numberValue -> numberValue.value().toPlainString();
            case ProjectValue.TextValue textValue -> textValue.value();
            case ProjectValue.ReferenceValue referenceValue ->
                referenceValue.reference().toString();
            case ProjectValue.EntityTargetValue targetValue ->
                targetValue.entity().toString();
            case ProjectValue.ComponentTargetValue targetValue ->
                targetValue.target().entity() + "/" + targetValue.target().component();
            case ProjectValue.ArrayValue arrayValue -> join(arrayValue.values(), "[", "]");
            case ProjectValue.ObjectValue objectValue -> joinObject(objectValue.values());
        };
    }

    /** Joins a value sequence using compact JSON-like punctuation. */
    private static String join(List<ProjectValue> values, String prefix, String suffix) {
        StringJoiner joiner = new StringJoiner(", ", prefix, suffix);
        values.stream().map(EditorInspectorProjector::format).forEach(joiner::add);
        return joiner.toString();
    }

    /** Joins an ordered property map using compact JSON-like punctuation. */
    private static String joinObject(Map<String, ProjectValue> values) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        values.forEach((key, value) -> joiner.add(key + ": " + format(value)));
        return joiner.toString();
    }

    /** Converts a stable kebab-case identity into a fallback author-facing label. */
    private static String displayName(String identity) {
        StringJoiner words = new StringJoiner(" ");
        for (String part : identity.split("-")) {
            words.add(part);
        }
        String value = words.toString();
        return value.isEmpty() ? identity : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /** Formats a structural value kind for secondary metadata. */
    static String label(ProjectValueKind kind) {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
