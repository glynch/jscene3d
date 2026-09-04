/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies value semantics and invariants of the public project model. */
final class ProjectModelTest {
    private static final Path ROOT = Path.of("/project");

    /** Keeps evolving runtime and project-file configurations as immutable values. */
    @Test
    void givesProjectConfigurationsValueSemantics() {
        GameProject.RuntimeConfiguration runtime = new GameProject.RuntimeConfiguration(
                "example.game", ROOT.resolve("main.scene.json"), Optional.empty(), Optional.empty());
        GameProject.RuntimeConfiguration sameRuntime = new GameProject.RuntimeConfiguration(
                "example.game", ROOT.resolve("main.scene.json"), Optional.empty(), Optional.empty());
        GameProject.RuntimeConfiguration differentRuntime = new GameProject.RuntimeConfiguration(
                "example.other", ROOT.resolve("main.scene.json"), Optional.empty(), Optional.empty());
        GameProject.ProjectFiles files = new GameProject.ProjectFiles(
                List.of(), List.of(ROOT.resolve("game.import.json")), List.of(ROOT.resolve("desktop.json")));
        GameProject.ProjectFiles sameFiles = new GameProject.ProjectFiles(
                List.of(), List.of(ROOT.resolve("game.import.json")), List.of(ROOT.resolve("desktop.json")));

        assertThat(runtime)
                .isEqualTo(sameRuntime)
                .hasSameHashCodeAs(sameRuntime)
                .isNotEqualTo(differentRuntime);
        assertThat(runtime.toString()).contains("applicationExtension=example.game", "main.scene.json");
        assertThat(files).isEqualTo(sameFiles).hasSameHashCodeAs(sameFiles).isNotEqualTo(runtime);
        assertThat(files.toString()).contains("game.import.json", "desktop.json");
    }

    /** Gives controller, scene-node, and scene definitions structural value semantics. */
    @Test
    void givesSceneDefinitionsValueSemantics() {
        ControllerDefinition controller = new ControllerDefinition(
                new RegisteredType("example.game/controller", 1),
                Map.of("speed", new ProjectValue.NumberValue(BigDecimal.ONE)));
        SceneNodeDefinition node = new SceneNodeDefinition(
                "root",
                Optional.of("Root"),
                true,
                new SceneNodeDefinition.TypedNode(new RegisteredType("example.game/group-3d", 1), Map.of()),
                Optional.of(controller),
                List.of());
        SceneNodeDefinition sameNode = new SceneNodeDefinition(
                "root",
                Optional.of("Root"),
                true,
                new SceneNodeDefinition.TypedNode(new RegisteredType("example.game/group-3d", 1), Map.of()),
                Optional.of(controller),
                List.of());
        SceneDefinition scene = new SceneDefinition(ROOT.resolve("main.scene.json"), "main", node, List.of());
        SceneDefinition sameScene = new SceneDefinition(ROOT.resolve("main.scene.json"), "main", sameNode, List.of());

        assertThat(controller).isEqualTo(new ControllerDefinition(controller.type(), controller.properties()));
        assertThat(controller.hashCode())
                .isEqualTo(new ControllerDefinition(controller.type(), controller.properties()).hashCode());
        assertThat(controller.toString()).contains("controller", "speed");
        assertThat(node).isEqualTo(sameNode).hasSameHashCodeAs(sameNode);
        assertThat(node.toString()).contains("id=root", "Root");
        assertThat(scene).isEqualTo(sameScene).hasSameHashCodeAs(sameScene);
        assertThat(scene.toString()).contains("id=main", "main.scene.json");
    }

    /** Defensively copies collections exposed by scene definitions. */
    @Test
    void copiesSceneCollections() {
        Map<String, ProjectValue> mutableProperties = new LinkedHashMap<>();
        mutableProperties.put("enabled", new ProjectValue.BooleanValue(true));
        List<ProjectValue> mutableValues = new ArrayList<>();
        mutableValues.add(new ProjectValue.TextValue("first"));
        ControllerDefinition controller =
                new ControllerDefinition(new RegisteredType("example.game/controller", 1), mutableProperties);
        ProjectValue.ArrayValue array = new ProjectValue.ArrayValue(mutableValues);
        ProjectValue.ObjectValue object = new ProjectValue.ObjectValue(mutableProperties);

        mutableProperties.clear();
        mutableValues.clear();

        assertThat(controller.properties()).containsKey("enabled");
        assertThat(array.values()).containsExactly(new ProjectValue.TextValue("first"));
        assertThat(object.values()).containsKey("enabled");
        assertThatThrownBy(controller.properties()::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(array.values()::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Rejects invalid registered types and resource-reference paths. */
    @Test
    void rejectsInvalidScalarValues() {
        Path relative = Path.of("relative.file");

        assertThatThrownBy(() -> new RegisteredType("", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegisteredType("example.game/type", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceReference.project("file", relative))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceReference.asset(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(ResourceReference.project("file", ROOT.resolve("file")).toString())
                .isEqualTo("project:file");
        assertThat(ResourceReference.Kind.IMPORT.prefix()).isEqualTo("import:");
    }

    /** Rejects invalid scene-definition invariants. */
    @Test
    void rejectsInvalidSceneDefinitions() {
        RegisteredType type = new RegisteredType("example.game/group-3d", 1);
        SceneNodeDefinition.TypedNode typed = new SceneNodeDefinition.TypedNode(type, Map.of());
        SceneNodeDefinition node = newNode(type);
        Path relative = Path.of("relative.scene.json");
        Optional<String> noName = Optional.empty();
        Optional<ControllerDefinition> noController = Optional.empty();
        List<SceneNodeDefinition> noChildren = List.of();
        Map<String, ProjectValue> noOverrides = Map.of();
        List<SceneConnection> noConnections = List.of();

        assertThatThrownBy(() -> new SceneNodeDefinition(" ", noName, true, typed, noController, noChildren))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SceneNodeDefinition.SceneInstance(relative, noOverrides))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SceneDefinition(relative, "main", node, noConnections))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SceneConnection.SignalEndpoint("root", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Creates one valid node for constructor-invariant tests. */
    private static SceneNodeDefinition newNode(RegisteredType type) {
        return new SceneNodeDefinition(
                "root",
                Optional.empty(),
                true,
                new SceneNodeDefinition.TypedNode(type, Map.of()),
                Optional.empty(),
                List.of());
    }
}
