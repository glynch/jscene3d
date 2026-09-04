/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises immutable import values and their boundary validation. */
final class ImportModelTest {
    private static final String SHA_256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final RegisteredType RESOURCE_TYPE = new RegisteredType("io.github.glynch.import-test/resource", 1);

    /** Preserves all artifact descriptor metadata and value semantics. */
    @Test
    void describesArtifacts() {
        ImportArtifactDescriptor scene = ImportArtifactDescriptor.scene("scenes/main", List.of("resources/world"));
        ImportArtifactDescriptor resource =
                ImportArtifactDescriptor.resource("resources/world", RESOURCE_TYPE, List.of("payloads/world"));
        ImportArtifactDescriptor payload =
                ImportArtifactDescriptor.payload("payloads/world", "application/octet-stream");
        ImportArtifactDescriptor equivalent =
                ImportArtifactDescriptor.resource("resources/world", RESOURCE_TYPE, List.of("payloads/world"));

        assertThat(scene.kind()).isEqualTo(ImportArtifactKind.SCENE);
        assertThat(scene.mediaType()).contains("application/json");
        assertThat(resource.resourceType()).contains(RESOURCE_TYPE);
        assertThat(resource.references()).containsExactly("payloads/world");
        assertThat(payload.kind()).isEqualTo(ImportArtifactKind.PAYLOAD);
        assertThat(payload.resourceType()).isEmpty();
        assertThat(resource).isEqualTo(equivalent).hasSameHashCodeAs(equivalent);
        assertThat(resource).isNotEqualTo(scene).isNotEqualTo(null);
        assertThat(resource.toString()).contains("resources/world", "RESOURCE");
    }

    /** Rejects non-portable identities, duplicate references, and invalid media types. */
    @Test
    void rejectsInvalidArtifactDescriptors() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportArtifactDescriptor.payload("/absolute", "text/plain"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportArtifactDescriptor.payload("folder\\output", "text/plain"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportArtifactDescriptor.payload("parent/../output", "text/plain"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportArtifactDescriptor.payload("output/", "text/plain"));
        assertThatIllegalArgumentException().isThrownBy(() -> ImportArtifactDescriptor.payload("output", " "));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportArtifactDescriptor.scene("scene", List.of("same", "same")));
    }

    /** Preserves inspected source metadata, relationships, and stable value semantics. */
    @Test
    void describesSourceItems() {
        SourceItemRelation relation = new SourceItemRelation("uses", "materials/metal");
        Map<String, ProjectValue> properties = Map.of("animated", new ProjectValue.BooleanValue(true));
        SourceItem item = new SourceItem(
                "meshes/body", "io.github.glynch.import-test/mesh", "Body", false, properties, List.of(relation));
        SourceItem equivalent = new SourceItem(
                "meshes/body", "io.github.glynch.import-test/mesh", "Body", false, properties, List.of(relation));

        assertThat(item.identity()).isEqualTo("meshes/body");
        assertThat(item.kind()).isEqualTo("io.github.glynch.import-test/mesh");
        assertThat(item.displayName()).isEqualTo("Body");
        assertThat(item.isSelectable()).isFalse();
        assertThat(item.properties()).containsEntry("animated", new ProjectValue.BooleanValue(true));
        assertThat(item.relations()).containsExactly(relation);
        assertThat(item).isEqualTo(equivalent).hasSameHashCodeAs(equivalent);
        assertThat(item.toString()).contains("meshes/body", "Body");
    }

    /** Validates quantified and unquantified progress ranges. */
    @Test
    void validatesProgress() {
        ImportProgress phase = ImportProgress.phase(ImportPhase.READING, "Reading source");
        ImportProgress quantified =
                ImportProgress.quantified(ImportPhase.DECODING, "Decoding mesh", 2L, 5L, Optional.of("meshes/body"));

        assertThat(phase.completedWork()).isEmpty();
        assertThat(phase.totalWork()).isEmpty();
        assertThat(quantified.phase()).isEqualTo(ImportPhase.DECODING);
        assertThat(quantified.description()).isEqualTo("Decoding mesh");
        assertThat(quantified.completedWork()).hasValue(2L);
        assertThat(quantified.totalWork()).hasValue(5L);
        assertThat(quantified.sourceItem()).contains("meshes/body");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportProgress.quantified(ImportPhase.WRITING, "Writing", -1L, 5L, Optional.empty()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportProgress.quantified(ImportPhase.WRITING, "Writing", 6L, 5L, Optional.empty()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportProgress.quantified(ImportPhase.WRITING, "Writing", 0L, 0L, Optional.empty()));
    }

    /** Enforces the relationship between import state and a published fingerprint. */
    @Test
    void validatesImportStatus() {
        ProjectDiagnostic diagnostic = diagnostic(ProjectDiagnostic.Severity.WARNING);
        ImportStatus blocked = new ImportStatus(ImportState.BLOCKED, Optional.of(SHA_256), List.of(diagnostic));
        ImportStatus current = new ImportStatus(ImportState.CURRENT, Optional.of(SHA_256), List.of());

        assertThat(blocked.state()).isEqualTo(ImportState.BLOCKED);
        assertThat(blocked.publishedFingerprint()).contains(SHA_256);
        assertThat(blocked.diagnostics()).containsExactly(diagnostic);
        assertThat(current.state()).isEqualTo(ImportState.CURRENT);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImportStatus(ImportState.CURRENT, Optional.empty(), List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImportStatus(ImportState.STALE, Optional.empty(), List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImportStatus(ImportState.MISSING, Optional.of(SHA_256), List.of()));
    }

    /** Keeps engine-owned import diagnostic keys unique and fallbacks non-blank. */
    @Test
    void validatesDiagnosticCatalog() {
        assertThat(ImportDiagnosticCode.values())
                .extracting(ImportDiagnosticCode::code)
                .doesNotHaveDuplicates()
                .allSatisfy(code -> assertThat(code).isNotBlank());
        assertThat(ImportDiagnosticCode.values())
                .extracting(ImportDiagnosticCode::defaultMessage)
                .allSatisfy(message -> assertThat(message).isNotBlank());
    }

    /** Copies inspection and artifact metadata at the public boundary. */
    @Test
    void describesInspectionAndArtifactMetadata(@TempDir Path temporaryDirectory) {
        SourceItem item = sourceItem("entry");
        ProjectDiagnostic warning = diagnostic(ProjectDiagnostic.Severity.WARNING);
        Path dependency = temporaryDirectory.resolve("dependency.bin");
        SourceInspection inspection = new SourceInspection(
                new RegisteredType("io.github.glynch.import-test/importer", 1),
                SHA_256,
                Map.of(dependency, SHA_256.toUpperCase(Locale.ROOT)),
                List.of(warning),
                List.of(item));
        ImportedArtifactMetadata metadata =
                new ImportedArtifactMetadata(ImportArtifactDescriptor.payload("payload", "text/plain"), SHA_256, 12L);

        assertThat(inspection.sourceFingerprint()).isEqualTo(SHA_256);
        assertThat(inspection.dependencies()).containsEntry(dependency, SHA_256);
        assertThat(inspection.diagnostics()).containsExactly(warning);
        assertThat(inspection.items()).containsExactly(item);
        assertThat(inspection.isValid()).isTrue();
        assertThat(metadata.identity()).isEqualTo("payload");
        assertThat(metadata.descriptor().mediaType()).contains("text/plain");
        assertThat(metadata.contentFingerprint()).isEqualTo(SHA_256);
        assertThat(metadata.size()).isEqualTo(12L);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImportedArtifactMetadata(metadata.descriptor(), SHA_256, -1L));
    }

    /** Rejects duplicate source identities and reports terminal inspection diagnostics. */
    @Test
    void rejectsInvalidInspections() {
        SourceItem item = sourceItem("entry");
        ProjectDiagnostic error = diagnostic(ProjectDiagnostic.Severity.ERROR);
        RegisteredType importer = new RegisteredType("io.github.glynch.import-test/importer", 1);
        SourceInspection invalid = new SourceInspection(importer, SHA_256, Map.of(), List.of(error), List.of(item));
        List<SourceItem> duplicates = List.of(item, item);
        Path relative = Path.of("dependency.bin");

        assertThat(invalid.isValid()).isFalse();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SourceInspection(importer, SHA_256, Map.of(), List.of(), duplicates));
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new SourceInspection(importer, SHA_256, Map.of(relative, SHA_256), List.of(), List.of()));
    }

    /** Creates one reusable inspected source item. */
    private static SourceItem sourceItem(String identity) {
        return new SourceItem(identity, "io.github.glynch.import-test/entry", "Entry", true, Map.of(), List.of());
    }

    /** Creates one reusable project diagnostic. */
    private static ProjectDiagnostic diagnostic(ProjectDiagnostic.Severity severity) {
        return new ProjectDiagnostic(
                severity,
                ImportDiagnosticCode.INSPECTION_FAILED,
                URI.create("file:///import.json"),
                "",
                Map.of("technicalDetail", "test diagnostic"));
    }
}
