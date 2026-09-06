/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceCloseException;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Owns one world's shared leases and entity-scoped resource participation. */
final class WorldResources {
    private final RuntimeResourceProvider provider;
    private final Map<ResourceReference, RetainedResource> retained = new LinkedHashMap<>();
    private final Map<InternalEntity, Set<ResourceReference>> entityReferences = new LinkedHashMap<>();
    private boolean closed;

    /** Stores the caller-owned provider without acquiring a resource. */
    WorldResources(RuntimeResourceProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /** Resolves one resource and attributes its retention to the constructing entity. */
    <T> T resolve(InternalEntity owner, ResourceReference reference, Class<T> valueType, String componentLocation) {
        requireOpen();
        InternalEntity validOwner = Objects.requireNonNull(owner, "owner");
        ResourceReference validReference = Objects.requireNonNull(reference, "reference");
        Class<T> validValueType = Objects.requireNonNull(valueType, "valueType");
        String validLocation = Objects.requireNonNull(componentLocation, "componentLocation");
        RetainedResource resource = retained.get(validReference);
        if (resource == null) {
            resource = acquire(validReference, validValueType, validLocation);
            retained.put(validReference, resource);
        }
        T value = cast(resource.value(), validValueType, validReference, validLocation);
        Set<ResourceReference> references =
                entityReferences.computeIfAbsent(validOwner, ignored -> new LinkedHashSet<>());
        if (references.add(validReference)) {
            resource.retainOwner();
        }
        return value;
    }

    /** Releases ownership scopes for a removed subtree and closes newly unreferenced leases in reverse order. */
    void release(Set<InternalEntity> entities) {
        requireOpen();
        for (InternalEntity entity : Objects.requireNonNull(entities, "entities")) {
            Set<ResourceReference> references = entityReferences.remove(entity);
            if (references != null) {
                references.forEach(reference -> Objects.requireNonNull(retained.get(reference), "retained resource")
                        .releaseOwner());
            }
        }
        closeUnreferenced();
    }

    /** Closes every retained lease in reverse acquisition order without closing the provider. */
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        entityReferences.clear();
        @Nullable RuntimeException failure = null;
        List<Map.Entry<ResourceReference, RetainedResource>> resources = new ArrayList<>(retained.entrySet());
        for (int index = resources.size() - 1; index >= 0; index--) {
            Map.Entry<ResourceReference, RetainedResource> resource = resources.get(index);
            failure = close(resource.getKey(), resource.getValue(), failure);
        }
        retained.clear();
        if (failure != null) {
            throw failure;
        }
    }

    /** Acquires and validates one lease while compensating a provider contract failure. */
    private <T> RetainedResource acquire(ResourceReference reference, Class<T> valueType, String componentLocation) {
        @Nullable RuntimeResourceLease<T> lease = null;
        try {
            lease = Objects.requireNonNull(provider.acquire(reference, valueType), "resource lease");
            if (lease.isClosed()) {
                throw new IllegalStateException("resource provider returned a closed lease");
            }
            T value = valueType.cast(Objects.requireNonNull(lease.value(), "resource lease value"));
            return new RetainedResource(value, lease);
        } catch (RuntimeException failure) {
            closeRejectedLease(lease, failure);
            throw acquisitionFailure(reference, componentLocation, failure);
        }
    }

    /** Casts one existing shared value or reports an incompatible repeated request. */
    private static <T> T cast(Object value, Class<T> valueType, ResourceReference reference, String componentLocation) {
        try {
            return valueType.cast(value);
        } catch (ClassCastException failure) {
            throw acquisitionFailure(reference, componentLocation, failure);
        }
    }

    /** Closes a newly acquired lease which cannot enter the world-owned cache. */
    private static void closeRejectedLease(
            @Nullable RuntimeResourceLease<?> lease, RuntimeException acquisitionFailure) {
        if (lease == null || lease.isClosed()) {
            return;
        }
        try {
            lease.close();
        } catch (RuntimeException closeFailure) {
            acquisitionFailure.addSuppressed(closeFailure);
        }
    }

    /** Closes each unreferenced lease in reverse acquisition order and removes it from the cache. */
    private void closeUnreferenced() {
        @Nullable RuntimeException failure = null;
        List<Map.Entry<ResourceReference, RetainedResource>> resources = new ArrayList<>(retained.entrySet());
        for (int index = resources.size() - 1; index >= 0; index--) {
            Map.Entry<ResourceReference, RetainedResource> resource = resources.get(index);
            if (!resource.getValue().isRetained()) {
                retained.remove(resource.getKey());
                failure = close(resource.getKey(), resource.getValue(), failure);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Closes one lease while retaining first-failure precedence. */
    private static @Nullable RuntimeException close(
            ResourceReference reference, RetainedResource resource, @Nullable RuntimeException existing) {
        try {
            resource.lease().close();
            return existing;
        } catch (RuntimeException closeFailure) {
            RuntimeException wrapped = new RuntimeResourceCloseException(reference, closeFailure);
            if (existing == null) {
                return wrapped;
            }
            existing.addSuppressed(wrapped);
            return existing;
        }
    }

    /** Creates a resource-specific structured composition failure. */
    private static RuntimeCompositionException acquisitionFailure(
            ResourceReference reference, String componentLocation, RuntimeException failure) {
        String detail = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.RESOURCE_ACQUISITION_FAILED,
                "failed to acquire " + reference + ": " + detail,
                componentLocation + "/resources/" + escape(reference.toString()));
    }

    /** Escapes a resource reference for use as one JSON Pointer location segment. */
    private static String escape(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    /** Rejects acquisition after cleanup begins. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("world resources are closed");
        }
    }

    /** One provider lease shared by every owning entity in this world. */
    private static final class RetainedResource {
        private final Object value;
        private final RuntimeResourceLease<?> lease;
        private int ownerCount;

        /** Stores one validated open lease. */
        private RetainedResource(Object value, RuntimeResourceLease<?> lease) {
            this.value = Objects.requireNonNull(value, "value");
            this.lease = Objects.requireNonNull(lease, "lease");
        }

        /** Returns the shared immutable value. */
        private Object value() {
            return value;
        }

        /** Returns the independently releasable provider lease. */
        private RuntimeResourceLease<?> lease() {
            return lease;
        }

        /** Adds one entity ownership scope. */
        private void retainOwner() {
            ownerCount++;
        }

        /** Removes one entity ownership scope. */
        private void releaseOwner() {
            if (ownerCount < 1) {
                throw new IllegalStateException("runtime resource has no entity owner to release");
            }
            ownerCount--;
        }

        /** Returns whether any live entity still owns this lease. */
        private boolean isRetained() {
            return ownerCount > 0;
        }
    }
}
