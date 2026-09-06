/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.PhysicsStepWorldModule;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.runtime.WorldModuleCloseException;
import io.github.glynch.jscene3d.project.runtime.WorldModuleUpdateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Owns exact world-module lookup and reverse binding-order cleanup after successful composition. */
final class WorldModules {
    private final List<WorldModuleBinding<?>> bindings;
    private final Map<Class<? extends WorldModule>, WorldModule> modules;
    private boolean closed;

    /** Copies and validates host bindings without taking ownership until the world is published. */
    WorldModules(Collection<WorldModuleBinding<?>> values) {
        List<WorldModuleBinding<?>> copied = new ArrayList<>();
        Map<Class<? extends WorldModule>, WorldModule> indexed = new LinkedHashMap<>();
        Set<WorldModule> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        int index = 0;
        for (WorldModuleBinding<?> binding : Objects.requireNonNull(values, "modules")) {
            WorldModuleBinding<?> validBinding = Objects.requireNonNull(binding, "modules entry");
            Class<? extends WorldModule> type = validBinding.type();
            WorldModule module = validBinding.module();
            if (indexed.putIfAbsent(type, module) != null) {
                throw invalid("world module interface is bound more than once: " + type.getName(), index);
            }
            if (!identities.add(module)) {
                throw invalid(
                        "world module adapter is bound more than once: "
                                + module.getClass().getName(),
                        index);
            }
            copied.add(validBinding);
            index++;
        }
        bindings = List.copyOf(copied);
        modules = Collections.unmodifiableMap(indexed);
    }

    /** Returns one exact bound module while this successfully composed world remains open. */
    <T extends WorldModule> Optional<T> find(Class<T> type) {
        requireOpen();
        Class<T> validType = Objects.requireNonNull(type, "type");
        return Optional.ofNullable(modules.get(validType)).map(validType::cast);
    }

    /** Returns one required exact binding or identifies the missing interface. */
    <T extends WorldModule> T require(Class<T> type) {
        Class<T> validType = Objects.requireNonNull(type, "type");
        return find(validType).orElseThrow(() -> new MissingWorldModuleException(validType));
    }

    /** Advances every physics-capable module in deterministic host binding order. */
    void advancePhysics(FixedUpdateContext update) {
        requireOpen();
        FixedUpdateContext validUpdate = Objects.requireNonNull(update, "update");
        for (WorldModuleBinding<?> binding : bindings) {
            if (binding.module() instanceof PhysicsStepWorldModule physics) {
                try {
                    physics.stepPhysics(validUpdate);
                } catch (RuntimeException failure) {
                    throw new WorldModuleUpdateException(binding.type(), validUpdate.tick(), failure);
                }
            }
        }
    }

    /** Closes every owned module in reverse binding order while retaining the first failure. */
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException failure = null;
        for (int index = bindings.size() - 1; index >= 0; index--) {
            WorldModuleBinding<?> binding = bindings.get(index);
            try {
                binding.module().close();
            } catch (RuntimeException closeFailure) {
                failure = accumulate(failure, new WorldModuleCloseException(binding.type(), closeFailure));
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Produces one component-independent structured composition failure. */
    private static RuntimeCompositionException invalid(String message, int index) {
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.WORLD_MODULE_DUPLICATE, message, "/worldModules/" + index);
    }

    /** Retains first-failure precedence and suppresses later reverse-order cleanup failures. */
    private static RuntimeException accumulate(@Nullable RuntimeException existing, RuntimeException additional) {
        if (existing == null) {
            return additional;
        }
        existing.addSuppressed(additional);
        return existing;
    }

    /** Rejects lookup after world cleanup has begun. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("world is closed");
        }
    }
}
