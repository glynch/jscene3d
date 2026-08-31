/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

/**
 * Custom GLSL material with immutable program structure and typed mutable uniforms.
 *
 * <p>Shader source targets OpenGL 3.3 Core. A source may include its own {@code #version 330 core}
 * directive; the renderer supplies that directive when it is omitted. The renderer automatically
 * supplies active reserved transform uniforms named {@code modelMatrix}, {@code viewMatrix},
 * {@code projectionMatrix}, {@code modelViewMatrix}, and {@code normalMatrix}. Applications cannot
 * assign those names.
 *
 * <p>Shader source, definitions, and required attributes are immutable after construction, making
 * program reuse deterministic. Uniform values are copied when set, except textures, which remain
 * shared application-owned descriptions. Instances are mutable, shareable, and not thread-safe.
 */
public final class ShaderMaterial extends Material {
    /** Automatic local-to-world transform uniform. */
    public static final String MODEL_MATRIX = "modelMatrix";

    /** Automatic world-to-view transform uniform. */
    public static final String VIEW_MATRIX = "viewMatrix";

    /** Automatic view-to-clip transform uniform. */
    public static final String PROJECTION_MATRIX = "projectionMatrix";

    /** Automatic local-to-view transform uniform. */
    public static final String MODEL_VIEW_MATRIX = "modelViewMatrix";

    /** Automatic inverse-transpose local-normal-to-view transform uniform. */
    public static final String NORMAL_MATRIX = "normalMatrix";

    private static final Set<String> AUTOMATIC_UNIFORMS =
            Set.of(MODEL_MATRIX, VIEW_MATRIX, PROJECTION_MATRIX, MODEL_VIEW_MATRIX, NORMAL_MATRIX);

    private final String vertexShader;
    private final String fragmentShader;
    private final Map<String, String> definitions;
    private final Set<ShaderAttribute> requiredAttributes;
    private final Map<String, ShaderUniform> uniforms;
    private final Map<String, ShaderUniform> uniformsView;

    /**
     * Creates a custom material requiring only vertex positions and defining no macros.
     *
     * @param vertexShader non-blank vertex shader source
     * @param fragmentShader non-blank fragment shader source
     * @throws NullPointerException if a source is {@code null}
     * @throws IllegalArgumentException if a source is blank
     */
    public ShaderMaterial(String vertexShader, String fragmentShader) {
        this(vertexShader, fragmentShader, Map.of(), EnumSet.of(ShaderAttribute.POSITION));
    }

    /** Builds one material from validated immutable program structure. */
    private ShaderMaterial(
            String vertexShader,
            String fragmentShader,
            Map<String, String> definitions,
            Set<ShaderAttribute> requiredAttributes) {
        this.vertexShader = Preconditions.requireNonBlank(vertexShader, "vertexShader");
        this.fragmentShader = Preconditions.requireNonBlank(fragmentShader, "fragmentShader");
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        this.requiredAttributes = Collections.unmodifiableSet(EnumSet.copyOf(requiredAttributes));
        uniforms = new LinkedHashMap<>();
        uniformsView = Collections.unmodifiableMap(uniforms);
    }

    /**
     * Creates a one-use builder for immutable shader definitions and attribute requirements.
     *
     * @param vertexShader non-blank vertex shader source
     * @param fragmentShader non-blank fragment shader source
     * @return new builder
     * @throws NullPointerException if a source is {@code null}
     * @throws IllegalArgumentException if a source is blank
     */
    public static Builder builder(String vertexShader, String fragmentShader) {
        return new Builder(
                Preconditions.requireNonBlank(vertexShader, "vertexShader"),
                Preconditions.requireNonBlank(fragmentShader, "fragmentShader"));
    }

    /**
     * Returns the immutable vertex shader source.
     *
     * @return caller-supplied source
     * @throws IllegalStateException if this material is closed
     */
    public String vertexShader() {
        requireOpen();
        return vertexShader;
    }

    /**
     * Returns the immutable fragment shader source.
     *
     * @return caller-supplied source
     * @throws IllegalStateException if this material is closed
     */
    public String fragmentShader() {
        requireOpen();
        return fragmentShader;
    }

    /**
     * Returns immutable preprocessor definitions in deterministic declaration order.
     *
     * @return definition name-to-value mapping
     * @throws IllegalStateException if this material is closed
     */
    public Map<String, String> definitions() {
        requireOpen();
        return definitions;
    }

    /**
     * Returns immutable required standard attributes, always including positions.
     *
     * @return required attributes
     * @throws IllegalStateException if this material is closed
     */
    public Set<ShaderAttribute> requiredAttributes() {
        requireOpen();
        return requiredAttributes;
    }

    /**
     * Returns the stable unmodifiable live view of application uniforms.
     *
     * @return uniforms in deterministic declaration order
     * @throws IllegalStateException if this material is closed
     */
    public Map<String, ShaderUniform> uniforms() {
        requireOpen();
        return uniformsView;
    }

    /**
     * Returns a named application uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @return stable uniform view, or {@code null} when absent
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is invalid or reserved
     * @throws IllegalStateException if this material is closed
     */
    public @Nullable ShaderUniform uniform(String name) {
        requireOpen();
        return uniforms.get(requireUniformName(name));
    }

    /**
     * Sets one finite {@code float} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value finite scalar value
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if the name is invalid or reserved, the value is not finite,
     *     or the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, float value) {
        float validValue = Preconditions.requireFinite(value, "value");
        updateFloatUniform(name, ShaderUniformType.FLOAT, validValue, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Sets one {@code int} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value integer value
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if the name is invalid or reserved, or the uniform was
     *     previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, int value) {
        ShaderUniform uniform = requireUniform(name, ShaderUniformType.INTEGER);
        if (uniform.set(value)) {
            markChanged();
        }
    }

    /**
     * Sets one {@code boolean} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value boolean value
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if the name is invalid or reserved, or the uniform was
     *     previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, boolean value) {
        ShaderUniform uniform = requireUniform(name, ShaderUniformType.BOOLEAN);
        if (uniform.set(value)) {
            markChanged();
        }
    }

    /**
     * Sets a finite {@code vec2} uniform without requiring a temporary vector.
     *
     * @param name valid non-reserved GLSL identifier
     * @param x finite X component
     * @param y finite Y component
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, float x, float y) {
        updateFloatUniform(
                name,
                ShaderUniformType.VECTOR2,
                Preconditions.requireFinite(x, "x"),
                Preconditions.requireFinite(y, "y"),
                0.0f,
                0.0f);
    }

    /**
     * Copies one finite {@code vec2} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value vector to copy
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, Vector2fc value) {
        Vector2fc validValue = Objects.requireNonNull(value, "value");
        setUniform(name, validValue.x(), validValue.y());
    }

    /**
     * Sets a finite {@code vec3} uniform without requiring a temporary vector.
     *
     * @param name valid non-reserved GLSL identifier
     * @param x finite X component
     * @param y finite Y component
     * @param z finite Z component
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, float x, float y, float z) {
        updateFloatUniform(
                name,
                ShaderUniformType.VECTOR3,
                Preconditions.requireFinite(x, "x"),
                Preconditions.requireFinite(y, "y"),
                Preconditions.requireFinite(z, "z"),
                0.0f);
    }

    /**
     * Copies one finite {@code vec3} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value vector to copy
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, Vector3fc value) {
        Vector3fc validValue = Preconditions.requireFinite(value, "value");
        setUniform(name, validValue.x(), validValue.y(), validValue.z());
    }

    /**
     * Sets a finite {@code vec4} uniform without requiring a temporary vector.
     *
     * @param name valid non-reserved GLSL identifier
     * @param x finite X component
     * @param y finite Y component
     * @param z finite Z component
     * @param w finite W component
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, float x, float y, float z, float w) {
        updateFloatUniform(
                name,
                ShaderUniformType.VECTOR4,
                Preconditions.requireFinite(x, "x"),
                Preconditions.requireFinite(y, "y"),
                Preconditions.requireFinite(z, "z"),
                Preconditions.requireFinite(w, "w"));
    }

    /**
     * Copies one finite {@code vec4} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value vector to copy
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, Vector4fc value) {
        Vector4fc validValue = Objects.requireNonNull(value, "value");
        setUniform(name, validValue.x(), validValue.y(), validValue.z(), validValue.w());
    }

    /**
     * Copies one finite column-major {@code mat3} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value matrix to copy
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, Matrix3fc value) {
        Matrix3fc validValue = Objects.requireNonNull(value, "value");
        ShaderUniform uniform = requireUniform(name, ShaderUniformType.MATRIX3);
        if (uniform.set(validValue)) {
            markChanged();
        }
    }

    /**
     * Copies one finite column-major {@code mat4} uniform.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value matrix to copy
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a component or name is invalid, the name is reserved, or
     *     the uniform was previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, Matrix4fc value) {
        Matrix4fc validValue = Objects.requireNonNull(value, "value");
        ShaderUniform uniform = requireUniform(name, ShaderUniformType.MATRIX4);
        if (uniform.set(validValue)) {
            markChanged();
        }
    }

    /**
     * Copies one linear-sRGB {@link Color} uniform supplied to GLSL as {@code vec3}.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value immutable color value
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the name is invalid or reserved, or the uniform was
     *     previously declared with another type
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, Color value) {
        Color validValue = Objects.requireNonNull(value, "value");
        updateFloatUniform(
                name, ShaderUniformType.COLOR, validValue.red(), validValue.green(), validValue.blue(), 0.0f);
    }

    /**
     * Sets one shared {@link Texture} uniform without transferring ownership.
     *
     * @param name valid non-reserved GLSL identifier
     * @param value open texture to share
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code value} is closed
     * @throws IllegalStateException if this material is closed
     */
    public void setUniform(String name, Texture value) {
        Texture validValue = Objects.requireNonNull(value, "value");
        if (validValue.isClosed()) {
            throw new IllegalArgumentException("value must be open");
        }
        ShaderUniform uniform = requireUniform(name, ShaderUniformType.TEXTURE);
        if (uniform.set(validValue)) {
            markChanged();
        }
    }

    /**
     * Removes an application uniform without closing a retained texture.
     *
     * @param name valid non-reserved GLSL identifier
     * @return {@code true} when a value was removed
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is invalid or reserved
     * @throws IllegalStateException if this material is closed
     */
    public boolean removeUniform(String name) {
        requireOpen();
        if (uniforms.remove(requireUniformName(name)) != null) {
            markChanged();
            return true;
        }
        return false;
    }

    /** Updates one float-component uniform without allocating temporary value objects. */
    private void updateFloatUniform(
            String name, ShaderUniformType type, float first, float second, float third, float fourth) {
        ShaderUniform uniform = requireUniform(name, type);
        if (uniform.set(first, second, third, fourth)) {
            markChanged();
        }
    }

    /** Returns an existing uniform or declares one while preventing implicit type changes. */
    private ShaderUniform requireUniform(String name, ShaderUniformType type) {
        requireOpen();
        String validName = requireUniformName(name);
        ShaderUniform existing = uniforms.get(validName);
        if (existing != null) {
            if (existing.type() != type) {
                throw new IllegalArgumentException(
                        "Uniform " + validName + " is already declared as " + existing.type() + ", not " + type);
            }
            return existing;
        }
        ShaderUniform created = new ShaderUniform(type);
        uniforms.put(validName, created);
        return created;
    }

    /** Validates an application-controlled uniform name. */
    private static String requireUniformName(String name) {
        String validName = Preconditions.requireIdentifier(name, "name");
        if (AUTOMATIC_UNIFORMS.contains(validName)) {
            throw new IllegalArgumentException("Uniform name is reserved for automatic renderer state: " + validName);
        }
        return validName;
    }

    /** Builds one immutable custom-shader program structure. */
    public static final class Builder {
        private final String vertexShader;
        private final String fragmentShader;
        private final Map<String, String> definitions = new LinkedHashMap<>();
        private final EnumSet<ShaderAttribute> requiredAttributes = EnumSet.of(ShaderAttribute.POSITION);

        private boolean built;

        /** Restricts builder creation to {@link ShaderMaterial#builder(String, String)}. */
        private Builder(String vertexShader, String fragmentShader) {
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }

        /**
         * Adds or replaces a preprocessor definition with value {@code 1}.
         *
         * @param name valid non-reserved GLSL identifier
         * @return this builder
         * @throws NullPointerException if {@code name} is {@code null}
         * @throws IllegalArgumentException if {@code name} is invalid
         * @throws IllegalStateException if this builder has already built a material
         */
        public Builder define(String name) {
            return define(name, "1");
        }

        /**
         * Adds or replaces one single-line preprocessor definition.
         *
         * @param name valid non-reserved GLSL identifier
         * @param value non-blank single-line replacement text
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the name or value is invalid
         * @throws IllegalStateException if this builder has already built a material
         */
        public Builder define(String name, String value) {
            requireNotBuilt();
            String validName = Preconditions.requireIdentifier(name, "name");
            String validValue = Preconditions.requireNonBlank(value, "value");
            if (validValue.indexOf('\n') >= 0 || validValue.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("value must be a single line");
            }
            definitions.put(validName, validValue);
            return this;
        }

        /**
         * Requires one standard geometry attribute in addition to positions.
         *
         * @param attribute supported standard attribute
         * @return this builder
         * @throws NullPointerException if {@code attribute} is {@code null}
         * @throws IllegalStateException if this builder has already built a material
         */
        public Builder requireAttribute(ShaderAttribute attribute) {
            requireNotBuilt();
            requiredAttributes.add(Objects.requireNonNull(attribute, "attribute"));
            return this;
        }

        /**
         * Builds the configured open material and prevents builder reuse.
         *
         * @return new custom shader material
         * @throws IllegalStateException if this builder has already built a material
         */
        public ShaderMaterial build() {
            requireNotBuilt();
            built = true;
            return new ShaderMaterial(vertexShader, fragmentShader, definitions, requiredAttributes);
        }

        /** Rejects reuse after one material has been built. */
        private void requireNotBuilt() {
            if (built) {
                throw new IllegalStateException("ShaderMaterial.Builder has already built a material");
            }
        }
    }
}
