/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.internal.ExtensionDescriptorValidator;
import io.github.glynch.jscene3d.project.extension.internal.RawExtensionDescriptor;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.internal.SemanticVersion;
import io.github.glynch.jscene3d.project.internal.SemanticVersionRequirement;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Discovers safe extension descriptors and builds the project's registered-type catalog. */
public final class ExtensionCatalogLoader {
    /** Fixed JAR resource path used for extension descriptor discovery. */
    public static final String DESCRIPTOR_RESOURCE = "META-INF/jscene3d/extension.json";

    private final SemanticVersion engineVersion;
    private final ProjectJsonReader jsonReader;

    /**
     * Creates a loader for one running JScene3D engine version.
     *
     * @param engineVersion semantic engine version used for extension compatibility
     */
    public ExtensionCatalogLoader(String engineVersion) {
        String version = Objects.requireNonNull(engineVersion, "engineVersion");
        this.engineVersion = SemanticVersion.parse(version)
                .orElseThrow(() -> new IllegalArgumentException("engineVersion must be a semantic version"));
        jsonReader = ProjectJsonReader.strict();
    }

    /**
     * Discovers descriptors visible to a class loader without loading extension classes.
     *
     * <p>Only extensions declared by the project are placed in the returned catalog. Descriptor resources are sorted
     * by URI before validation so diagnostics and duplicate resolution are deterministic.
     *
     * @param project validated project declaring required extensions
     * @param classLoader class loader containing resolved extension artifacts
     * @return possibly partial catalog and ordered diagnostics
     */
    public ExtensionCatalogLoadResult load(GameProject project, ClassLoader classLoader) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        ClassLoader validClassLoader = Objects.requireNonNull(classLoader, "classLoader");
        URI manifest = validProject.root().resolve(ProjectLoader.MANIFEST_NAME).toUri();
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        List<URL> resources = discover(validClassLoader, manifest, diagnostics);
        Map<String, LocatedDescriptor> discovered = readDescriptors(resources, manifest, diagnostics);
        List<ExtensionDescriptor> selected = selectDeclared(validProject, discovered, manifest, diagnostics);
        return new ExtensionCatalogLoadResult(new RegisteredTypeCatalog(selected), diagnostics);
    }

    /** Discovers and sorts every fixed-path descriptor resource. */
    private static List<URL> discover(ClassLoader classLoader, URI manifest, List<ProjectDiagnostic> diagnostics) {
        try {
            Enumeration<URL> resources = classLoader.getResources(DESCRIPTOR_RESOURCE);
            List<URL> sorted = Collections.list(resources);
            sorted.sort(Comparator.comparing(URL::toExternalForm));
            Map<String, URL> unique = new LinkedHashMap<>();
            for (URL resource : sorted) {
                unique.putIfAbsent(resource.toExternalForm(), resource);
            }
            return List.copyOf(unique.values());
        } catch (IOException exception) {
            diagnostics.add(error(
                    manifest,
                    ExtensionDiagnosticCode.DISCOVERY_READ_FAILED,
                    "extension descriptors cannot be enumerated: " + exception.getMessage(),
                    ""));
            return List.of();
        }
    }

    /** Reads valid descriptors and reports deterministic duplicate extension identities. */
    private Map<String, LocatedDescriptor> readDescriptors(
            List<URL> resources, URI manifest, List<ProjectDiagnostic> diagnostics) {
        Map<String, LocatedDescriptor> discovered = new LinkedHashMap<>();
        for (URL resource : resources) {
            URI source = resourceUri(resource, manifest, diagnostics);
            Optional<ExtensionDescriptor> descriptor = readDescriptor(resource, source, diagnostics);
            if (descriptor.isEmpty()) {
                continue;
            }
            ExtensionDescriptor value = descriptor.orElseThrow();
            LocatedDescriptor previous = discovered.putIfAbsent(value.id(), new LocatedDescriptor(value, source));
            if (previous != null) {
                diagnostics.add(error(
                        source,
                        ExtensionDiagnosticCode.DESCRIPTOR_DUPLICATE,
                        "multiple descriptors declare extension " + value.id(),
                        "/id"));
            }
        }
        return Collections.unmodifiableMap(discovered);
    }

    /** Parses and semantically validates one descriptor resource. */
    private Optional<ExtensionDescriptor> readDescriptor(
            URL resource, URI source, List<ProjectDiagnostic> diagnostics) {
        try (InputStream input = resource.openStream()) {
            RawExtensionDescriptor raw = jsonReader.read(input, RawExtensionDescriptor.class);
            ExtensionDescriptorValidator.ValidationResult validation =
                    ExtensionDescriptorValidator.validate(raw, source, engineVersion);
            diagnostics.addAll(validation.diagnostics());
            return validation.descriptor();
        } catch (JsonProcessingException exception) {
            diagnostics.add(error(
                    source,
                    ExtensionDiagnosticCode.JSON_INVALID,
                    "extension descriptor is not valid JSON: " + exception.getOriginalMessage(),
                    ""));
        } catch (IOException exception) {
            diagnostics.add(error(
                    source,
                    ExtensionDiagnosticCode.READ_FAILED,
                    "extension descriptor cannot be read: " + exception.getMessage(),
                    ""));
        }
        return Optional.empty();
    }

    /** Selects project-declared extensions and validates their resolved versions. */
    private static List<ExtensionDescriptor> selectDeclared(
            GameProject project,
            Map<String, LocatedDescriptor> discovered,
            URI manifest,
            List<ProjectDiagnostic> diagnostics) {
        List<ExtensionDescriptor> selected = new ArrayList<>();
        List<GameProject.ExtensionRequirement> requirements = project.extensions();
        for (int index = 0; index < requirements.size(); index++) {
            GameProject.ExtensionRequirement requirement = requirements.get(index);
            LocatedDescriptor located = discovered.get(requirement.id());
            String location = "/extensions/" + index;
            if (located == null) {
                diagnostics.add(error(
                        manifest,
                        ExtensionDiagnosticCode.MISSING,
                        "declared extension was not discovered: " + requirement.id(),
                        location));
            } else if (matches(requirement, located.descriptor())) {
                selected.add(located.descriptor());
            } else {
                diagnostics.add(error(
                        located.source(),
                        ExtensionDiagnosticCode.VERSION_INCOMPATIBLE,
                        "project requires " + requirement.requirement() + " but discovered "
                                + located.descriptor().version(),
                        "/version"));
            }
        }
        return List.copyOf(selected);
    }

    /** Returns whether one discovered extension satisfies its project requirement. */
    private static boolean matches(GameProject.ExtensionRequirement requirement, ExtensionDescriptor descriptor) {
        Optional<SemanticVersionRequirement> parsedRequirement =
                SemanticVersionRequirement.parse(requirement.requirement());
        Optional<SemanticVersion> parsedVersion = SemanticVersion.parse(descriptor.version());
        return parsedRequirement.isPresent()
                && parsedVersion.isPresent()
                && parsedRequirement.orElseThrow().includes(parsedVersion.orElseThrow());
    }

    /** Converts a resource URL to its diagnostic URI. */
    private static URI resourceUri(URL resource, URI fallback, List<ProjectDiagnostic> diagnostics) {
        try {
            return resource.toURI();
        } catch (URISyntaxException exception) {
            diagnostics.add(error(
                    fallback,
                    ExtensionDiagnosticCode.RESOURCE_URI_INVALID,
                    "extension descriptor has an invalid resource URI: " + resource,
                    ""));
            return fallback;
        }
    }

    /** Creates one structured error. */
    private static ProjectDiagnostic error(URI source, DiagnosticCode code, String technicalDetail, String location) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, location, Map.of("technicalDetail", technicalDetail));
    }

    /** Couples a validated descriptor to the resource that supplied it. */
    private record LocatedDescriptor(ExtensionDescriptor descriptor, URI source) {}
}
