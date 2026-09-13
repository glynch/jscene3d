/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

final class I18nModuleDescriptorTest {
    @Test
    void exportsTheMessageInterfaceAndResourceBundleAdapter() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.exports())
                .extracting(ModuleDescriptor.Exports::source)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.i18n", "io.github.glynch.jscene3d.i18n.resourcebundle");
    }
}
