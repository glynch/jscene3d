package io.github.glynch.jscene3d.doom.material;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Imported map materials, when valid, and ordered diagnostics suitable for tools and GUIs.
 *
 * @param materials imported material set, or empty when import failed
 * @param diagnostics ordered import diagnostics
 */
public record DoomMaterialImportResult(Optional<DoomMapMaterials> materials, List<DoomMaterialDiagnostic> diagnostics) {
    /** Creates an immutable import result. */
    public DoomMaterialImportResult {
        Objects.requireNonNull(materials, "materials");
        diagnostics = List.copyOf(diagnostics);
        if (materials.isPresent()
                && diagnostics.stream().anyMatch(item -> item.severity() == DoomMaterialDiagnostic.Severity.ERROR)) {
            throw new IllegalArgumentException("materials cannot accompany error diagnostics");
        }
    }

    /**
     * Returns whether import produced a usable material set.
     *
     * @return {@code true} when materials are present
     */
    public boolean isValid() {
        return materials.isPresent();
    }
}
