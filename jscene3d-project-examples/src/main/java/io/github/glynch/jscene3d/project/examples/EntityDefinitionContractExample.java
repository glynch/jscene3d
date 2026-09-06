/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Writes a reusable beacon definition with a deliberately exported public contract. */
public final class EntityDefinitionContractExample {
    private static final EntityId ROOT = EntityId.from("853f50a0-17dc-46ac-9f04-f772e54c44b2");
    private static final ComponentId LIGHT = ComponentId.from("b991ca3e-66bb-4ef0-a682-74773bbef0d0");

    /** Prevents instantiation of this application entry point. */
    private EntityDefinitionContractExample() {
        throw new AssertionError("EntityDefinitionContractExample cannot be instantiated");
    }

    /**
     * Writes the example definition to the supplied file.
     *
     * @param arguments one output-file path
     * @throws IOException when the definition cannot be written
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected one output-file path");
        }
        DefinitionWriter.write(Path.of(arguments[0]), beaconDefinition());
    }

    /** Creates the immutable example definition and its stable public targets. */
    private static EntityDefinition beaconDefinition() {
        PropertyId color = new PropertyId("color");
        PropertyId mesh = new PropertyId("mesh");
        EndpointId extinguished = new EndpointId("extinguished");
        EndpointId extinguish = new EndpointId("extinguish");
        ComponentDefinition light = new ComponentDefinition(
                LIGHT,
                new ComponentTypeId("io.github.glynch.jscene3d/light-3d"),
                1,
                Map.of(color, new ProjectValue.TextValue("warm-white")));
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        color,
                        ProjectValueKind.TEXT,
                        EntityContract.Requirement.OPTIONAL,
                        PropertyTarget.component(ROOT, LIGHT, color))),
                List.of(new EntityContract.Signal(extinguished, EndpointTarget.component(ROOT, LIGHT, extinguished))),
                List.of(new EntityContract.Action(extinguish, EndpointTarget.component(ROOT, LIGHT, extinguish))),
                List.of(new CapabilityId("io.github.glynch.beacon/illuminates")),
                List.of(new EntityContract.Attachment(
                        new AttachmentPointId("light-origin"), SpatialTarget.component(ROOT, LIGHT))),
                List.of(new EntityContract.ResourceBinding(
                        mesh,
                        EntityContract.Requirement.OPTIONAL,
                        Set.of(ResourceReference.Kind.IMPORT),
                        PropertyTarget.component(ROOT, LIGHT, mesh))));
        LocalEntity root = new LocalEntity(ROOT, "Beacon", true, List.of(light), List.of());
        return new EntityDefinition(
                AssetId.from("4c189475-9845-4810-b7f9-af744d3cc726"), "Beacon", contract, List.of(), root);
    }
}
