/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies stable public contracts and their hierarchy-bound targets. */
final class EntityContractModelTest {
    private static final AssetId DEFINITION_ID = AssetId.from("4c189475-9845-4810-b7f9-af744d3cc726");
    private static final AssetId NESTED_ID = AssetId.from("8d95e0d5-7cac-42a6-bceb-931437834532");
    private static final EntityId ROOT_ID = EntityId.from("853f50a0-17dc-46ac-9f04-f772e54c44b2");
    private static final EntityId PLACEMENT_ID = EntityId.from("722c16bf-922b-41a5-9499-b23b68d8ab8e");
    private static final ComponentId COMPONENT_ID = ComponentId.from("b991ca3e-66bb-4ef0-a682-74773bbef0d0");
    private static final ComponentId MISSING_COMPONENT = ComponentId.from("4b2fefc2-4d1e-441d-814d-fe94b5b23f09");

    /** Represents each deliberately exported member without exposing hierarchy paths. */
    @Test
    void modelsACompleteStableContract() {
        PropertyId speed = new PropertyId("speed");
        PropertyId mesh = new PropertyId("mesh");
        EndpointId fired = new EndpointId("fired");
        EndpointId stop = new EndpointId("stop");
        RegisteredType event = new RegisteredType("io.github.glynch.game/projectile-event", 1);
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        speed,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.REQUIRED,
                        PropertyTarget.component(ROOT_ID, COMPONENT_ID, speed))),
                List.of(new EntityContract.Signal(
                        fired, event, EndpointTarget.component(ROOT_ID, COMPONENT_ID, fired))),
                List.of(new EntityContract.Action(stop, event, EndpointTarget.component(ROOT_ID, COMPONENT_ID, stop))),
                List.of(new CapabilityId("io.github.glynch.game/projectile")),
                List.of(new EntityContract.Attachment(
                        new AttachmentPointId("trail"),
                        SpatialTarget.componentAttachment(
                                ROOT_ID, COMPONENT_ID, new AttachmentPointId("trail-origin")))),
                List.of(new EntityContract.ResourceBinding(
                        mesh,
                        EntityContract.Requirement.REQUIRED,
                        Set.of(ResourceReference.Kind.IMPORT),
                        PropertyTarget.component(ROOT_ID, COMPONENT_ID, mesh))));
        SignalConnection connection = new SignalConnection(
                EndpointTarget.component(ROOT_ID, COMPONENT_ID, fired),
                EndpointTarget.component(ROOT_ID, COMPONENT_ID, stop));
        EntityDefinition definition =
                new EntityDefinition(DEFINITION_ID, "Projectile", contract, List.of(connection), root(List.of()));
        EntityContract.Parameter parameter =
                contract.parameters().values().iterator().next();
        EntityContract.ResourceBinding resourceBinding =
                contract.resourceBindings().values().iterator().next();
        List<CapabilityId> capabilities = contract.capabilities();
        Set<ResourceReference.Kind> acceptedKinds = resourceBinding.acceptedKinds();

        assertThat(definition.contract()).isEqualTo(contract);
        assertThat(definition.connections()).containsExactly(connection);
        assertThat(contract.parameters().keySet()).containsExactly(speed);
        assertThat(contract.signals().keySet()).containsExactly(fired);
        assertThat(contract.actions().keySet()).containsExactly(stop);
        assertThat(contract.attachments().keySet()).containsExactly(new AttachmentPointId("trail"));
        assertThat(contract.resourceBindings().keySet()).containsExactly(mesh);
        assertThat(parameter.accepts(new ProjectValue.NumberValue(BigDecimal.ONE)))
                .isTrue();
        assertThat(resourceBinding.accepts(
                        new ProjectValue.ReferenceValue(ResourceReference.imported("projectile/mesh"))))
                .isTrue();
        assertThatThrownBy(capabilities::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(acceptedKinds::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Rejects duplicate public arguments even when they use different declaration categories. */
    @Test
    void rejectsDuplicateContractIdentities() {
        PropertyId shared = new PropertyId("content");
        PropertyTarget target = PropertyTarget.component(ROOT_ID, COMPONENT_ID, shared);
        List<EntityContract.Parameter> parameters = List.of(new EntityContract.Parameter(
                shared, ProjectValueKind.TEXT, EntityContract.Requirement.OPTIONAL, target));
        List<EntityContract.ResourceBinding> bindings = List.of(
                new EntityContract.ResourceBinding(shared, EntityContract.Requirement.OPTIONAL, Set.of(), target));
        List<EntityContract.Signal> noSignals = List.of();
        List<EntityContract.Action> noActions = List.of();
        List<CapabilityId> noCapabilities = List.of();
        List<EntityContract.Attachment> noAttachments = List.of();

        assertThatThrownBy(() ->
                        new EntityContract(parameters, noSignals, noActions, noCapabilities, noAttachments, bindings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicated", shared.toString());
    }

    /** Requires local contract and connection targets to resolve by stable entity and component identity. */
    @Test
    void rejectsDanglingOrEncapsulationBreakingTargets() {
        PropertyId speed = new PropertyId("speed");
        EntityContract dangling = new EntityContract(
                List.of(new EntityContract.Parameter(
                        speed,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.OPTIONAL,
                        PropertyTarget.component(ROOT_ID, MISSING_COMPONENT, speed))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        EntityPlacement placement = new EntityPlacement(PLACEMENT_ID, true, AssetRef.to(NESTED_ID), Map.of());
        EntityContract reachesInsidePlacement = new EntityContract(
                List.of(new EntityContract.Parameter(
                        speed,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.OPTIONAL,
                        PropertyTarget.component(PLACEMENT_ID, COMPONENT_ID, speed))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        LocalEntity root = root(List.of());
        LocalEntity rootWithPlacement = root(List.of(placement));
        List<SignalConnection> noConnections = List.of();

        assertThatThrownBy(() -> new EntityDefinition(DEFINITION_ID, "Invalid", dangling, noConnections, root))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown component id", MISSING_COMPONENT.toString());
        assertThatThrownBy(() -> new EntityDefinition(
                        DEFINITION_ID, "Invalid", reachesInsidePlacement, noConnections, rootWithPlacement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot address a component inside a placement");
    }

    /** Gives each contract identity family its intended validation rule. */
    @Test
    void validatesContractIdentitySyntax() {
        assertThatThrownBy(() -> new PropertyId("not valid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EndpointId("not/valid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttachmentPointId("not valid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CapabilityId("projectile")).isInstanceOf(IllegalArgumentException.class);
    }

    /** Supports payload-free endpoints and rejects values outside an argument declaration. */
    @Test
    void modelsPayloadFreeEndpointsAndRejectedValues() {
        EndpointId changed = new EndpointId("changed");
        EndpointTarget target = EndpointTarget.component(ROOT_ID, COMPONENT_ID, changed);
        EntityContract.Signal signal = new EntityContract.Signal(changed, target);
        EntityContract.Signal equalSignal = new EntityContract.Signal(changed, target);
        EntityContract.Action action = new EntityContract.Action(changed, target);
        EntityContract.Action equalAction = new EntityContract.Action(changed, target);
        PropertyId mesh = new PropertyId("mesh");
        EntityContract.ResourceBinding importedMesh = new EntityContract.ResourceBinding(
                mesh,
                EntityContract.Requirement.OPTIONAL,
                Set.of(ResourceReference.Kind.IMPORT),
                PropertyTarget.component(ROOT_ID, COMPONENT_ID, mesh));
        EntityContract.ResourceBinding anyResource = new EntityContract.ResourceBinding(
                mesh,
                EntityContract.Requirement.OPTIONAL,
                Set.of(),
                PropertyTarget.component(ROOT_ID, COMPONENT_ID, mesh));

        assertThat(signal.payload()).isEmpty();
        assertThat(action.payload()).isEmpty();
        assertThat(signal).isEqualTo(equalSignal).hasSameHashCodeAs(equalSignal);
        assertThat(action).isEqualTo(equalAction).hasSameHashCodeAs(equalAction);
        assertThat(importedMesh.accepts(new ProjectValue.TextValue("mesh"))).isFalse();
        assertThat(importedMesh.accepts(new ProjectValue.ReferenceValue(ResourceReference.asset("mesh"))))
                .isFalse();
        assertThat(anyResource.accepts(new ProjectValue.ReferenceValue(ResourceReference.asset("mesh"))))
                .isTrue();
        assertThat(signal.toString()).contains("changed");
        assertThat(action.toString()).contains("changed");
    }

    /** Creates a local root with the component used by contract targets. */
    private static LocalEntity root(List<EntityEntry> children) {
        ComponentDefinition component = new ComponentDefinition(
                COMPONENT_ID, new ComponentTypeId("io.github.glynch.game/projectile"), 1, Map.of());
        return new LocalEntity(ROOT_ID, "Projectile", true, List.of(component), children);
    }
}
