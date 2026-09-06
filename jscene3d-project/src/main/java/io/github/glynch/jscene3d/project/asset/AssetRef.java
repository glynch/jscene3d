/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePortableLocator;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Persistent typed reference to a project asset.
 *
 * <p>The optional path hint is diagnostic information only. Equality and resolution use the stable asset ID.
 * The type parameter prevents callers from interchanging references to different asset kinds.
 *
 * @param <T> referenced asset kind
 */
@SuppressWarnings("java:S2326") // T is deliberately a compile-time-only phantom type.
public final class AssetRef<T> {
    private final AssetId id;
    private final @Nullable String pathHint;

    /** Stores one reference with an optional validated diagnostic hint. */
    private AssetRef(AssetId id, @Nullable String pathHint) {
        this.id = Objects.requireNonNull(id, "id");
        this.pathHint = pathHint == null ? null : requirePortableLocator(pathHint, "pathHint");
    }

    /**
     * Creates a reference containing only authoritative identity.
     *
     * @param <T> referenced asset kind
     * @param id asset identity
     * @return typed asset reference
     */
    public static <T> AssetRef<T> to(AssetId id) {
        return new AssetRef<>(id, null);
    }

    /**
     * Creates a reference with a non-authoritative project-relative path hint.
     *
     * @param <T> referenced asset kind
     * @param id asset identity
     * @param pathHint portable diagnostic path hint
     * @return typed asset reference
     */
    public static <T> AssetRef<T> to(AssetId id, String pathHint) {
        return new AssetRef<>(id, pathHint);
    }

    /**
     * Returns the authoritative asset identity.
     *
     * @return asset identity
     */
    public AssetId id() {
        return id;
    }

    /**
     * Returns the non-authoritative diagnostic path hint.
     *
     * @return optional path hint
     */
    public Optional<String> pathHint() {
        return Optional.ofNullable(pathHint);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof AssetRef<?> reference && id.equals(reference.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return pathHint == null ? "AssetRef[id=" + id + ']' : "AssetRef[id=" + id + ", pathHint=" + pathHint + ']';
    }
}
