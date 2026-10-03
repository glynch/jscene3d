/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Session-owned reference-counted cache which preserves resources across atomic snapshot replacement. */
final class SceneViewResourceCache implements AutoCloseable {
    private final SceneViewResourceResolver resolver;
    private final Map<ResourceReference, Entry<BufferGeometry>> meshes = new LinkedHashMap<>();
    private final Map<ResourceReference, Entry<Material>> materials = new LinkedHashMap<>();
    private boolean closed;

    SceneViewResourceCache(SceneViewResourceResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    Retained<BufferGeometry> retainMesh(ResourceReference reference) {
        return retain(meshes, Objects.requireNonNull(reference, "reference"), resolver::acquireMesh);
    }

    Retained<Material> retainMaterial(ResourceReference reference) {
        return retain(materials, Objects.requireNonNull(reference, "reference"), resolver::acquireMaterial);
    }

    private <T> Retained<T> retain(
            Map<ResourceReference, Entry<T>> entries, ResourceReference reference, Acquirer<T> acquirer) {
        requireOpen();
        Entry<T> entry = entries.get(reference);
        if (entry == null) {
            entry = new Entry<>(Objects.requireNonNull(acquirer.acquire(reference), "resource lease"));
            entries.put(reference, entry);
        }
        entry.users++;
        Entry<T> retainedEntry = entry;
        return new Retained<>(entry.lease.value(), () -> release(entries, reference, retainedEntry));
    }

    private static <T> void release(
            Map<ResourceReference, Entry<T>> entries, ResourceReference reference, Entry<T> entry) {
        if (entry.users < 1 || entries.get(reference) != entry) {
            throw new IllegalStateException("Scene View resource cache retention is inconsistent");
        }
        entry.users--;
        if (entry.users == 0) {
            entries.remove(reference);
            entry.lease.close();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException failure = closeEntries(materials, null);
        failure = closeEntries(meshes, failure);
        if (failure != null) {
            throw failure;
        }
    }

    private static @Nullable RuntimeException closeEntries(
            Map<ResourceReference, ? extends Entry<?>> entries, @Nullable RuntimeException retained) {
        RuntimeException failure = retained;
        for (Entry<?> entry : entries.values()) {
            try {
                entry.lease.close();
            } catch (RuntimeException exception) {
                failure = retainFailure(failure, exception);
            }
        }
        entries.clear();
        return failure;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Scene View resource cache is closed");
        }
    }

    private static RuntimeException retainFailure(@Nullable RuntimeException retained, RuntimeException next) {
        if (retained == null) {
            return next;
        }
        retained.addSuppressed(next);
        return retained;
    }

    /** One resource cache entry and its active graph-retention count. */
    private static final class Entry<T> {
        private final SceneViewResourceResolver.Lease<T> lease;
        private int users;

        private Entry(SceneViewResourceResolver.Lease<T> lease) {
            this.lease = Objects.requireNonNull(lease, "lease");
        }
    }

    /** One independently releasable graph retention. */
    static final class Retained<T> implements AutoCloseable {
        private final T value;
        private final Runnable release;
        private boolean closed;

        private Retained(T value, Runnable release) {
            this.value = Objects.requireNonNull(value, "value");
            this.release = Objects.requireNonNull(release, "release");
        }

        T value() {
            if (closed) {
                throw new IllegalStateException("Scene View resource retention is closed");
            }
            return value;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            release.run();
        }
    }

    @FunctionalInterface
    private interface Acquirer<T> {
        SceneViewResourceResolver.Lease<T> acquire(ResourceReference reference);
    }
}
