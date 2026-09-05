/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.RotationOrder;
import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.CollisionFilter;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.StaticBody;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeContext;
import io.github.glynch.jscene3d.project.runtime.scene3d.Scene3dRenderHost;
import io.github.glynch.jscene3d.project.runtime.scene3d.Scene3dRuntimeObject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.Map;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Per-runtime composition state hidden behind the built-in extension interface. */
public final class Scene3dComposition {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    private final Scene3dRenderHost host;
    private final PhysicsWorld physicsWorld;
    private final Scene scene = new Scene();
    private @Nullable PerspectiveCamera activeCamera;
    private boolean rootCreated;

    /**
     * Creates empty composition state for one render host.
     *
     * @param host render submission adapter
     * @param physicsWorld physics destination for declarative collision nodes
     */
    public Scene3dComposition(Scene3dRenderHost host, PhysicsWorld physicsWorld) {
        this.host = Objects.requireNonNull(host, "host");
        this.physicsWorld = Objects.requireNonNull(physicsWorld, "physicsWorld");
    }

    /**
     * Registers all built-in version-one scene and resource factories.
     *
     * @param registry active extension-registration scope
     */
    public void register(ProjectRuntimeRegistry registry) {
        registry.registerSceneNode(BuiltInTypes.GROUP_3D, this::createGroup);
        registry.registerSceneNode(BuiltInTypes.MESH_INSTANCE_3D, this::createMesh);
        registry.registerSceneNode(BuiltInTypes.PERSPECTIVE_CAMERA_3D, this::createCamera);
        registry.registerSceneNode(BuiltInTypes.AMBIENT_LIGHT_3D, this::createAmbientLight);
        registry.registerSceneNode(BuiltInTypes.STATIC_BODY_3D, this::createStaticBody);
        registry.registerSceneNode(BuiltInTypes.COLLISION_SHAPE_3D, this::createCollisionShape);
        registry.registerResource(BuiltInTypes.BOX_GEOMETRY_3D, this::createBoxGeometry);
        registry.registerResource(BuiltInTypes.LAMBERT_MATERIAL_3D, this::createLambertMaterial);
        registry.registerResource(BuiltInTypes.BOX_SHAPE_3D, this::createBoxShape);
        registry.registerResource(BuiltInTypes.SPHERE_SHAPE_3D, this::createSphereShape);
        registry.registerResource(BuiltInTypes.CAPSULE_SHAPE_3D, this::createCapsuleShape);
    }

    /** Requires the composed scene to have exactly one selected camera. */
    void requireActiveCamera() {
        if (activeCamera == null) {
            throw new IllegalStateException("the declarative 3d scene has no active camera");
        }
    }

    /** Submits the composed scene through its selected camera. */
    void render() {
        host.render(scene, Objects.requireNonNull(activeCamera, "activeCamera"));
    }

    /** Creates either the required scene root or one nested transform group. */
    private ProjectRuntimeObject createGroup(SceneNodeContext context) {
        Map<String, ProjectValue> properties = context.properties();
        Object3D object;
        ProjectRuntimeObject runtimeObject;
        if (context.parent().isEmpty()) {
            if (rootCreated) {
                throw new IllegalStateException("the declarative scene already has a root");
            }
            rootCreated = true;
            scene.setBackground(Scene3dValues.color(properties, "background"));
            object = scene;
            runtimeObject = new SceneRootRuntimeObject(scene, this);
        } else {
            object = new Object3D();
            runtimeObject = new SpatialRuntimeObject(object);
        }
        configureSpatial(object, properties, context.isEnabled());
        attach(context, object);
        return runtimeObject;
    }

    /** Creates a mesh that retains shared geometry and material resources. */
    private ProjectRuntimeObject createMesh(SceneNodeContext context) {
        BufferGeometry geometry = context.resolveResource(
                Scene3dValues.reference(context.properties(), "geometry"), BufferGeometry.class);
        Material material =
                context.resolveResource(Scene3dValues.reference(context.properties(), "material"), Material.class);
        Mesh mesh = new Mesh(geometry, material);
        configureSpatial(mesh, context.properties(), context.isEnabled());
        attach(context, mesh);
        return new SpatialRuntimeObject(mesh);
    }

    /** Creates and optionally selects one perspective camera. */
    private ProjectRuntimeObject createCamera(SceneNodeContext context) {
        Map<String, ProjectValue> properties = context.properties();
        PerspectiveCamera camera = new PerspectiveCamera(
                Scene3dValues.number(properties, "field-of-view-degrees") * DEGREES_TO_RADIANS,
                1.0f,
                Scene3dValues.number(properties, "near"),
                Scene3dValues.number(properties, "far"));
        camera.setPosition(Scene3dValues.vector3(properties, "position"));
        camera.setScale(Scene3dValues.vector3(properties, "scale"));
        camera.setVisible(context.isEnabled());
        Vector3f target = Scene3dValues.vector3(properties, "target");
        camera.lookAt(target);
        attach(context, camera);
        if (Scene3dValues.bool(properties, "active")) {
            selectCamera(camera);
        }
        return new SpatialRuntimeObject(camera);
    }

    /** Creates one hierarchy-attached ambient light. */
    private ProjectRuntimeObject createAmbientLight(SceneNodeContext context) {
        Map<String, ProjectValue> properties = context.properties();
        AmbientLight light = new AmbientLight(
                Scene3dValues.color(properties, "color"), Scene3dValues.number(properties, "intensity"));
        light.setVisible(context.isEnabled());
        attach(context, light);
        return new SpatialRuntimeObject(light);
    }

    /** Creates an immovable body whose collision shapes are declared by child nodes. */
    private ProjectRuntimeObject createStaticBody(SceneNodeContext context) {
        Object3D object = new Object3D();
        configurePose(object, context.properties(), context.isEnabled());
        attach(context, object);
        StaticBody body = physicsWorld.addStaticBody(
                object.worldPosition(new Vector3f()), object.worldQuaternion(new Quaternionf()));
        body.setEnabled(context.isEnabled());
        return new StaticBodyRuntimeObject(object, physicsWorld, body);
    }

    /** Creates one collider attached directly to its authored static-body parent. */
    private ProjectRuntimeObject createCollisionShape(SceneNodeContext context) {
        ProjectRuntimeObject parent = context.parent().orElseThrow().object();
        if (!(parent instanceof StaticBodyRuntimeObject staticBody)) {
            throw new IllegalArgumentException("collision-shape-3d requires a direct static-body-3d parent");
        }
        CollisionShape shape =
                context.resolveResource(Scene3dValues.reference(context.properties(), "shape"), CollisionShape.class);
        Object3D object = new Object3D();
        configurePose(object, context.properties(), context.isEnabled());
        attach(context, object);
        Collider collider = staticBody.body().addCollider(shape, object.position(), object.quaternion());
        collider.setCollisionFilter(new CollisionFilter(
                Scene3dValues.integer(context.properties(), "category-bits"),
                Scene3dValues.integer(context.properties(), "mask-bits")));
        collider.setEnabled(context.isEnabled());
        return new CollisionShapeRuntimeObject(object, staticBody.body(), collider);
    }

    /** Creates one runtime-owned generated box geometry. */
    private Object createBoxGeometry(ResourceFactoryContext context) {
        Map<String, ProjectValue> properties = context.properties();
        return BoxGeometry.create(
                Scene3dValues.number(properties, "width"),
                Scene3dValues.number(properties, "height"),
                Scene3dValues.number(properties, "depth"));
    }

    /** Creates one runtime-owned diffuse material. */
    private Object createLambertMaterial(ResourceFactoryContext context) {
        return new LambertMaterial(Scene3dValues.color(context.properties(), "color"));
    }

    /** Creates one immutable box collision shape. */
    private Object createBoxShape(ResourceFactoryContext context) {
        Map<String, ProjectValue> properties = context.properties();
        return new BoxShape(
                Scene3dValues.number(properties, "width"),
                Scene3dValues.number(properties, "height"),
                Scene3dValues.number(properties, "depth"));
    }

    /** Creates one immutable sphere collision shape. */
    private Object createSphereShape(ResourceFactoryContext context) {
        return new SphereShape(Scene3dValues.number(context.properties(), "radius"));
    }

    /** Creates one immutable Y-aligned capsule collision shape. */
    private Object createCapsuleShape(ResourceFactoryContext context) {
        Map<String, ProjectValue> properties = context.properties();
        return new CapsuleShape(
                Scene3dValues.number(properties, "radius"), Scene3dValues.number(properties, "segment-length"));
    }

    /** Applies the common authored transform and effective visibility. */
    private static void configureSpatial(Object3D object, Map<String, ProjectValue> properties, boolean visible) {
        configurePose(object, properties, visible);
        object.setScale(Scene3dValues.vector3(properties, "scale"));
    }

    /** Applies an authored position and orientation without unsupported collision scaling. */
    private static void configurePose(Object3D object, Map<String, ProjectValue> properties, boolean visible) {
        Vector3f position = Scene3dValues.vector3(properties, "position");
        Vector3f rotation = Scene3dValues.vector3(properties, "rotation-degrees");
        object.setPosition(position);
        object.setRotationFromEuler(
                rotation.x * DEGREES_TO_RADIANS,
                rotation.y * DEGREES_TO_RADIANS,
                rotation.z * DEGREES_TO_RADIANS,
                RotationOrder.XYZ);
        object.setVisible(visible);
    }

    /** Attaches a non-root scene object to its authored spatial parent. */
    private static void attach(SceneNodeContext context, Object3D object) {
        if (context.parent().isEmpty()) {
            return;
        }
        ProjectRuntimeObject parentObject = context.parent().orElseThrow().object();
        if (!(parentObject instanceof Scene3dRuntimeObject spatialParent)) {
            throw new IllegalArgumentException("3d scene nodes require a spatial runtime parent");
        }
        spatialParent.object3d().add(object);
    }

    /** Selects exactly one active perspective camera. */
    private void selectCamera(PerspectiveCamera camera) {
        if (activeCamera != null) {
            throw new IllegalArgumentException("the declarative 3d scene contains multiple active cameras");
        }
        activeCamera = camera;
    }
}
