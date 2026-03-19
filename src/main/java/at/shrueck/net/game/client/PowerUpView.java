package at.shrueck.net.game.client;

import at.shrueck.net.game.shared.PowerUpType;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

public final class PowerUpView extends Node {

    private final Vector3f baseTranslation = new Vector3f();
    private float animationTime;

    public PowerUpView(AssetManager assetManager, PowerUpType type) {
        super("power-up-" + type.name().toLowerCase());
        attachChild(createCore(assetManager, type));
        attachChild(createBaseRing(assetManager, type));
    }

    public void apply(ClientGameState.PowerUpState state) {
        baseTranslation.set(state.x(), 0.9f, state.z());
        updateTransform();
    }

    public void updateVisual(float tpf) {
        animationTime += tpf;
        rotate(0f, 1.85f * tpf, 0f);
        updateTransform();
    }

    private void updateTransform() {
        float bob = FastMath.sin(animationTime * 2.6f) * 0.14f;
        setLocalTranslation(baseTranslation.x, baseTranslation.y + bob, baseTranslation.z);
    }

    private Geometry createCore(AssetManager assetManager, PowerUpType type) {
        Geometry geometry = switch (type) {
            case STUDENT_SPEED -> new Geometry("power-up-speed", new Sphere(16, 16, 0.38f));
            case STUDENT_GROWTH -> new Geometry("power-up-growth", new Box(0.34f, 0.34f, 0.34f));
            case SHRUECK_SLOW -> new Geometry("power-up-slow", new Cylinder(16, 16, 0.30f, 0.46f, true));
            case SHRUECK_STUN -> new Geometry("power-up-stun", new Box(0.42f, 0.12f, 0.12f));
        };
        geometry.setMaterial(createMaterial(assetManager, coreColor(type), 0.55f));
        if (type == PowerUpType.SHRUECK_STUN) {
            geometry.rotate(0f, 0f, FastMath.QUARTER_PI);
        }
        return geometry;
    }

    private Geometry createBaseRing(AssetManager assetManager, PowerUpType type) {
        Geometry ring = new Geometry("power-up-ring", new Cylinder(20, 20, 0.54f, 0.04f, true));
        ring.setMaterial(createMaterial(assetManager, coreColor(type).mult(0.9f), 0.28f));
        ring.setLocalTranslation(0f, -0.42f, 0f);
        ring.rotate(FastMath.HALF_PI, 0f, 0f);
        return ring;
    }

    private Material createMaterial(AssetManager assetManager, ColorRGBA color, float ambientStrength) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color.mult(ambientStrength));
        material.setColor("Specular", ColorRGBA.White.mult(0.12f));
        material.setFloat("Shininess", 8f);
        return material;
    }

    private ColorRGBA coreColor(PowerUpType type) {
        return switch (type) {
            case STUDENT_SPEED -> new ColorRGBA(0.35f, 0.86f, 0.98f, 1f);
            case STUDENT_GROWTH -> new ColorRGBA(0.52f, 0.95f, 0.44f, 1f);
            case SHRUECK_SLOW -> new ColorRGBA(0.96f, 0.76f, 0.28f, 1f);
            case SHRUECK_STUN -> new ColorRGBA(0.98f, 0.46f, 0.38f, 1f);
        };
    }
}
