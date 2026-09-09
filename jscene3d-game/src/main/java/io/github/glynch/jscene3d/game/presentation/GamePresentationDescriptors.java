/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Safe metadata for generic game-presentation resources. */
public final class GamePresentationDescriptors {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.presentation";
    private static final RegisteredType PCM_AUDIO_RESOURCE = new RegisteredType(EXTENSION_ID + "/pcm-audio", 1);
    private static final ExtensionDescriptor DESCRIPTOR = new ExtensionDescriptor(
            EXTENSION_ID,
            "1.0.0",
            ">=0.1.0 <0.2.0",
            DescriptorPresentation.named("JScene3D game presentation"),
            List.of(pcmAudioDescriptor()));

    /** Prevents construction of this stable metadata container. */
    private GamePresentationDescriptors() {
        throw new AssertionError("GamePresentationDescriptors cannot be instantiated");
    }

    /**
     * Returns the exact version-one signed PCM resource type.
     *
     * @return registered PCM resource type
     */
    public static RegisteredType pcmAudioResourceType() {
        return PCM_AUDIO_RESOURCE;
    }

    /**
     * Returns safe metadata for generic game-presentation resources.
     *
     * @return game-presentation extension descriptor
     */
    public static ExtensionDescriptor extensionDescriptor() {
        return DESCRIPTOR;
    }

    /** Describes signed interleaved PCM backed by an opaque little-endian payload. */
    private static RegisteredTypeDescriptor pcmAudioDescriptor() {
        return new RegisteredTypeDescriptor(
                PCM_AUDIO_RESOURCE,
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.described("PCM audio", "Immutable signed 16-bit interleaved PCM audio"),
                List.of(
                        required("payload", ProjectValueKind.REFERENCE),
                        required("channels", ProjectValueKind.NUMBER),
                        required("sample-rate", ProjectValueKind.NUMBER),
                        required("sample-count", ProjectValueKind.NUMBER)),
                List.of(),
                List.of(),
                List.of());
    }

    /** Creates one required resource property descriptor. */
    private static PropertyDescriptor required(String id, ProjectValueKind kind) {
        return PropertyDescriptor.required(id, kind, DescriptorPresentation.named(id), Map.of(), Set.of());
    }
}
