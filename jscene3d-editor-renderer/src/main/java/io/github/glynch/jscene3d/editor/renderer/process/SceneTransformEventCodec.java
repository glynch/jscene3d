/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.ComponentIdentity;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

/** Base64url JSON codec for renderer-to-authoring transform intents. */
final class SceneTransformEventCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SceneTransformEventCodec() {
        throw new AssertionError("SceneTransformEventCodec cannot be instantiated");
    }

    static String encode(SceneTransformEvent event) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("phase", event.phase().name().toLowerCase(Locale.ROOT));
        root.put("editId", event.editId());
        root.put("revision", event.revision());
        root.put("propertyId", event.propertyId());
        ComponentIdentity identity = event.identity();
        root.put("definitionAssetId", identity.occurrence().rootDefinition().toString());
        ArrayNode path = root.putArray("entityPath");
        identity.occurrence().entityPath().forEach(entity -> path.add(entity.toString()));
        root.put("authoredDefinitionAssetId", identity.scope().definition().toString());
        root.put("authoredEntityId", identity.authoredEntity().toString());
        root.put("componentId", identity.component().toString());
        Vector3 value = event.value();
        ArrayNode literals = root.putArray("literals");
        literals.add(value.x().toPlainString());
        literals.add(value.y().toPlainString());
        literals.add(value.z().toPlainString());
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MAPPER.writeValueAsBytes(root));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode Scene transform event", exception);
        }
    }
}
