/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.audio.PcmAudio;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Exercises the complete version-one game-presentation resource storage boundary. */
final class GamePresentationResourceTest {
    private static final ResourceReference PAYLOAD = ResourceReference.imported("presentation/pistol.pcm16le");
    private static final ResourceReference IMAGE_PAYLOAD = ResourceReference.imported("presentation/digit.rgba8");

    /** Round-trips signed stereo PCM and enforces terminal resource ownership. */
    @Test
    void roundTripsPcmAudioResource() throws IOException {
        PcmAudio source = PcmAudio.stereo16(11_025, new short[] {-32_768, -1, 0, 32_767});
        ByteArrayOutputStream definitionOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream payloadOutput = new ByteArrayOutputStream();

        GamePresentationResourceWriter.writePcmAudioDefinition(definitionOutput, source, PAYLOAD);
        GamePresentationResourceWriter.writePcmAudioPayload(payloadOutput, source);
        PcmAudioResource resource = loader().load(
                        definition(2, 11_025, 4), reference -> new ByteArrayInputStream(payloadOutput.toByteArray()));

        assertThat(new String(definitionOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("io.github.glynch.jscene3d.presentation/pcm-audio")
                .contains("\"$ref\" : \"import:presentation/pistol.pcm16le\"");
        assertThat(resource.audio()).isEqualTo(source);
        resource.close();
        resource.close();
        assertThat(resource.isClosed()).isTrue();
        assertThatThrownBy(resource::audio)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Rejects invalid metadata and payload envelopes before publishing runtime state. */
    @Test
    void rejectsInvalidPcmAudioContent() {
        RuntimeResourceLoader<PcmAudioResource> resourceLoader = loader();
        ResourceDefinition unsupportedChannels = definition(3, 11_025, 3);
        ResourceDefinition incompleteFrames = definition(2, 11_025, 3);
        ResourceDefinition wrongPayload = withProperty("payload", new ProjectValue.TextValue("wrong"));
        ResourceDefinition shortPayload = definition(1, 11_025, 2);
        ResourceDefinition longPayload = definition(1, 11_025, 2);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> resourceLoader.load(unsupportedChannels, reference -> InputStream.nullInputStream()))
                .withMessageContaining("one or two");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resourceLoader.load(incompleteFrames, reference -> InputStream.nullInputStream()))
                .withMessageContaining("complete frames");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resourceLoader.load(wrongPayload, reference -> InputStream.nullInputStream()))
                .withMessageContaining("must be a reference");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resourceLoader.load(shortPayload, reference -> new ByteArrayInputStream(new byte[3])))
                .withMessageContaining("length must be 4 bytes");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resourceLoader.load(longPayload, reference -> new ByteArrayInputStream(new byte[5])))
                .withMessageContaining("length must be 4 bytes");
    }

    /** Publishes the exact descriptor type and loader contract. */
    @Test
    void describesPcmAudioResource() {
        assertThat(GamePresentationResourceLoaders.all())
                .extracting(RuntimeResourceLoader::type)
                .containsExactly(
                        GamePresentationDescriptors.pcmAudioResourceType(),
                        GamePresentationDescriptors.overlayImageResourceType());
        assertThat(GamePresentationDescriptors.extensionDescriptor().types())
                .extracting(descriptor -> descriptor.type())
                .containsExactly(
                        GamePresentationDescriptors.pcmAudioResourceType(),
                        GamePresentationDescriptors.overlayImageResourceType());
        assertThat(GamePresentationDescriptors.extensionDescriptor().components())
                .extracting(component -> component.type())
                .containsExactly(
                        GamePresentationDescriptors.screenCanvasType(),
                        GamePresentationDescriptors.screenRegionType(),
                        GamePresentationDescriptors.screenImageType(),
                        GamePresentationDescriptors.bitmapNumberType());
    }

    /** Round-trips one immutable screen image independently of any 3D texture resource. */
    @Test
    void roundTripsOverlayImageResource() throws IOException {
        byte[] pixels = {1, 2, 3, 4, 5, 6, 7, 8};
        ByteArrayOutputStream definitionOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream payloadOutput = new ByteArrayOutputStream();

        GamePresentationResourceWriter.writeOverlayImageDefinition(definitionOutput, 2, 1, IMAGE_PAYLOAD);
        GamePresentationResourceWriter.writeOverlayImagePayload(payloadOutput, 2, 1, pixels);
        OverlayImageResource resource = overlayImageLoader()
                .load(imageDefinition(2, 1), reference -> new ByteArrayInputStream(payloadOutput.toByteArray()));

        assertThat(new String(definitionOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("io.github.glynch.jscene3d.presentation/overlay-image")
                .contains("\"$ref\" : \"import:presentation/digit.rgba8\"");
        assertThat(resource.image().width()).isEqualTo(2);
        assertThat(resource.image().height()).isEqualTo(1);
        resource.close();
        assertThatThrownBy(resource::image).isInstanceOf(IllegalStateException.class);
    }

    /** Returns the typed PCM loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<PcmAudioResource> loader() {
        return (RuntimeResourceLoader<PcmAudioResource>)
                GamePresentationResourceLoaders.all().getFirst();
    }

    /** Returns the typed overlay-image loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<OverlayImageResource> overlayImageLoader() {
        return (RuntimeResourceLoader<OverlayImageResource>)
                GamePresentationResourceLoaders.all().get(1);
    }

    /** Builds one PCM resource definition with caller-selected format metadata. */
    private static ResourceDefinition definition(int channels, int sampleRate, int sampleCount) {
        Map<String, ProjectValue> properties = new LinkedHashMap<>();
        properties.put("payload", new ProjectValue.ReferenceValue(PAYLOAD));
        properties.put("channels", number(channels));
        properties.put("sample-rate", number(sampleRate));
        properties.put("sample-count", number(sampleCount));
        return new ResourceDefinition(
                URI.create("import:presentation/pistol"),
                GamePresentationDescriptors.pcmAudioResourceType(),
                properties);
    }

    /** Builds one otherwise-valid definition with one caller-selected property replacement. */
    private static ResourceDefinition withProperty(String name, ProjectValue value) {
        Map<String, ProjectValue> properties =
                new LinkedHashMap<>(definition(1, 11_025, 2).properties());
        properties.put(name, value);
        return new ResourceDefinition(
                URI.create("import:presentation/pistol"),
                GamePresentationDescriptors.pcmAudioResourceType(),
                properties);
    }

    /** Builds one overlay-image resource definition with caller-selected dimensions. */
    private static ResourceDefinition imageDefinition(int width, int height) {
        return new ResourceDefinition(
                URI.create("import:presentation/digit"),
                GamePresentationDescriptors.overlayImageResourceType(),
                Map.of(
                        "payload", new ProjectValue.ReferenceValue(IMAGE_PAYLOAD),
                        "width", number(width),
                        "height", number(height)));
    }

    /** Creates one portable integer value. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }
}
