/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.audio.AudioCategory;
import io.github.glynch.jscene3d.audio.AudioClip;
import io.github.glynch.jscene3d.audio.AudioEngine;
import io.github.glynch.jscene3d.audio.AudioSource;
import io.github.glynch.jscene3d.game.presentation.LocalSound;
import io.github.glynch.jscene3d.game.presentation.OverlayRegistration;
import io.github.glynch.jscene3d.game.presentation.PcmAudioResource;
import io.github.glynch.jscene3d.game.presentation.PositionalSound;
import io.github.glynch.jscene3d.game.presentation.PositionalSoundAttenuation;
import io.github.glynch.jscene3d.game.presentation.PresentationWorldModule;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Standard desktop ownership of project overlays and local or world-positioned OpenAL sources. */
final class StandardPresentationWorldModule implements PresentationWorldModule {
    private final List<OverlayEntry> overlays = new ArrayList<>();
    private final Set<StandardLocalSound> sounds = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<StandardPositionalSound> positionalSounds = Collections.newSetFromMap(new IdentityHashMap<>());
    private @Nullable AudioEngine audioEngine;
    private boolean closed;

    @Override
    public OverlayRegistration registerOverlay(Overlay overlay) {
        requireOpen();
        OverlayEntry entry = new OverlayEntry(Objects.requireNonNull(overlay, "overlay"));
        overlays.add(entry);
        return entry;
    }

    @Override
    public LocalSound createLocalSound(PcmAudioResource audio, AudioCategory category) {
        requireOpen();
        AudioEngine engine = audioEngine();
        AudioClip clip =
                engine.createClip(Objects.requireNonNull(audio, "audio").audio());
        try {
            AudioSource source = engine.createSource(clip, Objects.requireNonNull(category, "category"));
            source.setRelative(true);
            source.setPosition(new Vector3f());
            StandardLocalSound sound = new StandardLocalSound(clip, source);
            sounds.add(sound);
            return sound;
        } catch (RuntimeException failure) {
            clip.close();
            throw failure;
        }
    }

    @Override
    public PositionalSound createPositionalSound(
            PcmAudioResource audio, AudioCategory category, PositionalSoundAttenuation attenuation) {
        requireOpen();
        AudioEngine engine = audioEngine();
        PcmAudioResource resource = Objects.requireNonNull(audio, "audio");
        if (resource.audio().channels() != 1) {
            throw new IllegalArgumentException("world-positioned audio must be mono");
        }
        AudioClip clip = engine.createClip(resource.audio());
        try {
            AudioSource source = engine.createSource(clip, Objects.requireNonNull(category, "category"));
            source.setRelative(false);
            PositionalSoundAttenuation settings = Objects.requireNonNull(attenuation, "attenuation");
            source.setAttenuation(settings.referenceDistance(), settings.maximumDistance(), settings.rolloffFactor());
            StandardPositionalSound sound = new StandardPositionalSound(clip, source);
            positionalSounds.add(sound);
            return sound;
        } catch (RuntimeException failure) {
            clip.close();
            throw failure;
        }
    }

    @Override
    public void setListenerTransform(Vector3fc position, Vector3fc forward, Vector3fc up) {
        requireOpen();
        if (audioEngine != null) {
            audioEngine.listener().setTransform(position, forward, up);
        }
    }

    @Override
    public void renderOverlays(Renderer renderer) {
        requireOpen();
        Renderer validRenderer = Objects.requireNonNull(renderer, "renderer");
        List.copyOf(overlays).forEach(entry -> validRenderer.render(entry.overlay));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        List.copyOf(sounds).forEach(StandardLocalSound::close);
        List.copyOf(positionalSounds).forEach(StandardPositionalSound::close);
        overlays.clear();
        if (audioEngine != null) {
            audioEngine.close();
            audioEngine = null;
        }
    }

    /** Lazily opens native audio only when a project actually creates a sound. */
    private AudioEngine audioEngine() {
        if (audioEngine == null) {
            audioEngine = AudioEngine.create();
        }
        return audioEngine;
    }

    /** Rejects operations after the owning world has closed this adapter. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("presentation world module is closed");
        }
    }

    /** Idempotent removal handle for one overlay entry. */
    private final class OverlayEntry implements OverlayRegistration {
        private final Overlay overlay;
        private boolean removed;

        private OverlayEntry(Overlay overlay) {
            this.overlay = overlay;
        }

        @Override
        public void close() {
            if (!removed) {
                removed = true;
                overlays.remove(this);
            }
        }
    }

    /** Owns one source and its otherwise unshared native clip. */
    private final class StandardLocalSound implements LocalSound {
        private final AudioClip clip;
        private final AudioSource source;
        private boolean released;

        private StandardLocalSound(AudioClip clip, AudioSource source) {
            this.clip = clip;
            this.source = source;
        }

        @Override
        public void restart() {
            requireOpen();
            if (released) {
                throw new IllegalStateException("local sound is closed");
            }
            source.rewind();
            source.play();
        }

        @Override
        public void close() {
            if (released) {
                return;
            }
            released = true;
            sounds.remove(this);
            source.close();
            clip.close();
        }
    }

    /** Owns one world-positioned source and its otherwise unshared native clip. */
    private final class StandardPositionalSound implements PositionalSound {
        private final AudioClip clip;
        private final AudioSource source;
        private boolean released;

        private StandardPositionalSound(AudioClip clip, AudioSource source) {
            this.clip = clip;
            this.source = source;
        }

        @Override
        public void restart(Vector3fc position) {
            requireOpen();
            if (released) {
                throw new IllegalStateException("positional sound is closed");
            }
            source.setPosition(Objects.requireNonNull(position, "position"));
            source.rewind();
            source.play();
        }

        @Override
        public void close() {
            if (released) {
                return;
            }
            released = true;
            positionalSounds.remove(this);
            source.close();
            clip.close();
        }
    }
}
