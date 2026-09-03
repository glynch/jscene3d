/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import io.github.glynch.jscene3d.audio.internal.DecodedAudio;
import io.github.glynch.jscene3d.audio.internal.OggVorbisDecoder;
import io.github.glynch.jscene3d.audio.internal.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.system.MemoryUtil;

/**
 * Owns one OpenAL device and context, decoded clips, playback sources, volume groups, and listener.
 *
 * <p>The engine buffers complete clips before uploading them to OpenAL. It can decode Ogg Vorbis
 * resources or accept immutable signed 16-bit PCM produced by application asset pipelines.
 * Streaming is intentionally outside this interface. Every engine and its child objects are
 * thread-confined: all methods must be called on the thread that called {@link #create()}.
 */
public final class AudioEngine implements AutoCloseable {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final long device;
    private final long context;
    private final Thread ownerThread;
    private final ALCCapabilities alcCapabilities;
    private final ALCapabilities alCapabilities;
    private final Map<AudioCategory, Float> categoryGains = new EnumMap<>(AudioCategory.class);
    private final Set<AudioClip> clips = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AudioSource> sources = Collections.newSetFromMap(new IdentityHashMap<>());
    private final AudioListener listener;

    private float masterGain = 1.0F;
    private boolean closed;

    /** Stores an initialized native context and establishes default volume and distance behavior. */
    private AudioEngine(
            long device,
            long context,
            ALCCapabilities alcCapabilities,
            ALCapabilities alCapabilities,
            Thread ownerThread) {
        this.device = device;
        this.context = context;
        this.alcCapabilities = alcCapabilities;
        this.alCapabilities = alCapabilities;
        this.ownerThread = ownerThread;
        listener = new AudioListener(this);
        for (AudioCategory category : AudioCategory.values()) {
            categoryGains.put(category, 1.0F);
        }
        AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE_CLAMPED);
        checkOpenAl("configure distance model");
    }

    /**
     * Opens the default OpenAL device and creates an engine on the current thread.
     *
     * @return initialized audio engine
     * @throws IllegalStateException if no playback device or context can be created
     */
    public static AudioEngine create() {
        long device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == MemoryUtil.NULL) {
            throw new IllegalStateException("Could not open the default OpenAL playback device");
        }
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        long context = ALC10.alcCreateContext(device, (IntBuffer) null);
        if (context == MemoryUtil.NULL) {
            ALC10.alcCloseDevice(device);
            throw new IllegalStateException("Could not create an OpenAL playback context");
        }
        if (!ALC10.alcMakeContextCurrent(context)) {
            ALC10.alcDestroyContext(context);
            ALC10.alcCloseDevice(device);
            throw new IllegalStateException("Could not activate the OpenAL playback context");
        }
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        return new AudioEngine(device, context, alcCapabilities, alCapabilities, Thread.currentThread());
    }

    /**
     * Decodes one required classpath Ogg Vorbis resource into a reusable OpenAL buffer.
     *
     * @param resourceAnchor class or module used to resolve the resource
     * @param resourceName absolute or anchor-relative classpath resource name
     * @return engine-owned decoded clip
     * @throws IllegalArgumentException if the resource is absent or unsupported
     * @throws UncheckedIOException if the resource cannot be read
     */
    public AudioClip loadClip(Class<?> resourceAnchor, String resourceName) {
        requireUsable();
        Class<?> validAnchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");
        String validName = requireNonBlank(resourceName, "resourceName");
        byte[] encodedBytes = readResource(validAnchor, validName);
        ByteBuffer encodedAudio = MemoryUtil.memAlloc(encodedBytes.length);
        try {
            encodedAudio.put(encodedBytes).flip();
            try (DecodedAudio decodedAudio = OggVorbisDecoder.decode(encodedAudio)) {
                return uploadClip(decodedAudio);
            }
        } finally {
            MemoryUtil.memFree(encodedAudio);
        }
    }

    /**
     * Uploads complete in-memory signed 16-bit PCM into a reusable OpenAL buffer.
     *
     * @param audio immutable mono or stereo PCM
     * @return engine-owned clip
     */
    public AudioClip createClip(PcmAudio audio) {
        requireUsable();
        PcmAudio validAudio = Objects.requireNonNull(audio, "audio");
        short[] samples = validAudio.samples();
        ShortBuffer nativeSamples = MemoryUtil.memAllocShort(samples.length);
        try {
            nativeSamples.put(samples).flip();
            return uploadClip(nativeSamples, validAudio.channels(), validAudio.sampleRate());
        } finally {
            MemoryUtil.memFree(nativeSamples);
        }
    }

    /**
     * Creates an independently controlled playback source for a clip and volume category.
     *
     * @param clip open clip created by this engine
     * @param category volume category assigned to the source
     * @return independently controlled source
     */
    public AudioSource createSource(AudioClip clip, AudioCategory category) {
        requireUsable();
        AudioClip validClip = Objects.requireNonNull(clip, "clip");
        AudioCategory validCategory = Objects.requireNonNull(category, "category");
        int bufferId = validClip.bufferId(this);
        int sourceId = AL10.alGenSources();
        AudioSource source = new AudioSource(this, validClip, validCategory, sourceId);
        try {
            checkOpenAl("create audio source");
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
            checkOpenAl("attach audio clip");
            sources.add(source);
            applyGain(source);
            return source;
        } catch (RuntimeException failure) {
            sources.remove(source);
            AL10.alDeleteSources(sourceId);
            throw failure;
        }
    }

    /**
     * Returns the single listener controlled by this engine.
     *
     * @return engine listener facade
     */
    public AudioListener listener() {
        requireUsable();
        return listener;
    }

    /**
     * Sets the master volume applied by OpenAL after all source and category gains.
     *
     * @param value finite volume in the inclusive unit interval
     */
    public void setMasterGain(float value) {
        requireUsable();
        masterGain = Preconditions.requireUnitInterval(value, "value");
        AL10.alListenerf(AL10.AL_GAIN, masterGain);
        checkOpenAl("change master gain");
    }

    /**
     * Returns the current master volume in the inclusive unit interval.
     *
     * @return current master volume
     */
    public float masterGain() {
        requireUsable();
        return masterGain;
    }

    /**
     * Sets the independently adjustable volume for one category.
     *
     * @param category category to adjust
     * @param value finite volume in the inclusive unit interval
     */
    public void setCategoryGain(AudioCategory category, float value) {
        requireUsable();
        AudioCategory validCategory = Objects.requireNonNull(category, "category");
        float validValue = Preconditions.requireUnitInterval(value, "value");
        categoryGains.put(validCategory, validValue);
        sources.stream().filter(source -> source.category() == validCategory).forEach(this::applyGain);
    }

    /**
     * Returns the current volume for one category.
     *
     * @param category category to query
     * @return current category volume
     */
    public float categoryGain(AudioCategory category) {
        requireUsable();
        return categoryGains.get(Objects.requireNonNull(category, "category"));
    }

    /**
     * Returns whether this engine has released its OpenAL context and device.
     *
     * @return whether the engine is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /** Releases sources, clips, the OpenAL context, and finally the playback device. */
    @Override
    public void close() {
        requireOwnerThread();
        if (closed) {
            return;
        }
        bindContext();
        List.copyOf(sources).forEach(AudioSource::close);
        List.copyOf(clips).forEach(AudioClip::close);
        AL.setCurrentThread(null);
        AL.setCurrentProcess(null);
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
        ALC.setCapabilities(null);
        closed = true;
    }

    /** Runs one source mutation after ownership, lifetime, and native context validation. */
    void useSource(AudioSource source, Runnable operation, String action) {
        requireSource(source);
        Objects.requireNonNull(operation, "operation").run();
        checkOpenAl(action);
    }

    /** Runs one source query after ownership, lifetime, and native context validation. */
    int querySource(AudioSource source, IntSupplier operation, String action) {
        requireSource(source);
        int result = Objects.requireNonNull(operation, "operation").getAsInt();
        checkOpenAl(action);
        return result;
    }

    /** Runs one floating-point source query after ownership, lifetime, and context validation. */
    double querySource(AudioSource source, DoubleSupplier operation, String action) {
        requireSource(source);
        double result = Objects.requireNonNull(operation, "operation").getAsDouble();
        checkOpenAl(action);
        return result;
    }

    /** Runs one listener mutation in the active engine context. */
    void useListener(Runnable operation, String action) {
        requireUsable();
        Objects.requireNonNull(operation, "operation").run();
        checkOpenAl(action);
    }

    /** Recalculates one source's effective gain after local or category changes. */
    void applyGain(AudioSource source) {
        applyGain(source, source.gain());
    }

    /** Applies a validated prospective source gain without changing Java state on native failure. */
    void applyGain(AudioSource source, float sourceGain) {
        requireSource(source);
        float effectiveGain = sourceGain * categoryGains.get(source.category());
        AL10.alSourcef(source.sourceId(this), AL10.AL_GAIN, effectiveGain);
        checkOpenAl("change effective source gain");
    }

    /** Stops and deletes one source, allowing idempotent handle cleanup. */
    void destroySource(AudioSource source) {
        AudioSource validSource = Objects.requireNonNull(source, "source");
        validSource.requireOwnedBy(this);
        requireOwnerThread();
        if (validSource.isClosed()) {
            return;
        }
        requireUsable();
        int sourceId = validSource.sourceId(this);
        AL10.alSourceStop(sourceId);
        AL10.alDeleteSources(sourceId);
        checkOpenAl("delete audio source");
        sources.remove(validSource);
        validSource.markClosed();
    }

    /** Deletes one unreferenced clip, allowing idempotent handle cleanup. */
    void destroyClip(AudioClip clip) {
        AudioClip validClip = Objects.requireNonNull(clip, "clip");
        validClip.requireOwnedBy(this);
        requireOwnerThread();
        if (validClip.isClosed()) {
            return;
        }
        requireUsable();
        boolean referenced = sources.stream().anyMatch(source -> source.clip() == validClip);
        if (referenced) {
            throw new IllegalStateException("Close every source using an audio clip before closing the clip");
        }
        AL10.alDeleteBuffers(validClip.bufferId(this));
        checkOpenAl("delete audio clip");
        clips.remove(validClip);
        validClip.markClosed();
    }

    /** Reads an encoded resource completely while preserving its source in failures. */
    private static byte[] readResource(Class<?> resourceAnchor, String resourceName) {
        try (InputStream input = resourceAnchor.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalArgumentException("Audio resource does not exist: " + resourceName);
            }
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read audio resource: " + resourceName, exception);
        }
    }

    /** Uploads validated decoded PCM and records its immutable format metadata. */
    private AudioClip uploadClip(DecodedAudio decodedAudio) {
        return uploadClip(decodedAudio.samples(), decodedAudio.channels(), decodedAudio.sampleRate());
    }

    /** Uploads a native signed 16-bit PCM view and records its immutable format metadata. */
    private AudioClip uploadClip(ShortBuffer samples, int channels, int sampleRate) {
        int frameCount = samples.remaining() / channels;
        long durationNanos = Math.multiplyExact(frameCount, NANOS_PER_SECOND) / sampleRate;
        int bufferId = AL10.alGenBuffers();
        AudioClip clip = new AudioClip(this, bufferId, channels, sampleRate, Duration.ofNanos(durationNanos));
        try {
            checkOpenAl("create audio clip buffer");
            int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            AL10.alBufferData(bufferId, format, samples, sampleRate);
            checkOpenAl("upload decoded audio clip");
            clips.add(clip);
            return clip;
        } catch (RuntimeException failure) {
            AL10.alDeleteBuffers(bufferId);
            throw failure;
        }
    }

    /** Validates an owned live source and activates this engine's OpenAL context. */
    private void requireSource(AudioSource source) {
        AudioSource validSource = Objects.requireNonNull(source, "source");
        validSource.requireOwnedBy(this);
        validSource.requireOpen();
        requireUsable();
    }

    /** Validates engine lifetime, thread confinement, and native context selection. */
    private void requireUsable() {
        requireOwnerThread();
        if (closed) {
            throw new IllegalStateException("Audio engine is closed");
        }
        bindContext();
    }

    /** Rejects calls from any thread other than the engine's creating thread. */
    private void requireOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Audio engine methods must run on its creating thread");
        }
    }

    /** Makes this engine's context and cached LWJGL capabilities current. */
    private void bindContext() {
        ALC.setCapabilities(alcCapabilities);
        if (ALC10.alcGetCurrentContext() != context && !ALC10.alcMakeContextCurrent(context)) {
            throw new IllegalStateException("Could not activate the OpenAL playback context");
        }
        AL.setCurrentProcess(alCapabilities);
        AL.setCurrentThread(alCapabilities);
    }

    /** Converts the current OpenAL error flag into an operation-specific exception. */
    private static void checkOpenAl(String action) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            throw new IllegalStateException("Could not " + action + "; OpenAL error: " + error);
        }
    }

    /** Returns a non-blank string for resource diagnostics. */
    private static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }
}
