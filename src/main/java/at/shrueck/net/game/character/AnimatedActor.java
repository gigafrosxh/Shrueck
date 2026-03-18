package at.shrueck.net.game.character;

import at.shrueck.net.game.assets.AssetCatalog.AnimationVariant;
import com.jme3.anim.AnimComposer;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.EnumMap;
import java.util.Map;

public final class AnimatedActor extends Node {

    private final EnumMap<CharacterMode, ModelState> modelStates = new EnumMap<>(CharacterMode.class);
    private final CharacterMode fallbackMode;
    private CharacterMode activeMode;

    public AnimatedActor(
            String name,
            AssetManager assetManager,
            EnumMap<CharacterMode, AnimationVariant> variants,
            CharacterMode fallbackMode
    ) {
        super(name);
        this.fallbackMode = fallbackMode;

        for (Map.Entry<CharacterMode, AnimationVariant> entry : variants.entrySet()) {
            modelStates.put(entry.getKey(), loadModel(assetManager, entry.getValue()));
        }

        if (modelStates.isEmpty()) {
            throw new IllegalArgumentException("AnimatedActor requires at least one model variant.");
        }

        setMode(fallbackMode);
    }

    public void setMode(CharacterMode requestedMode) {
        CharacterMode resolvedMode = resolveMode(requestedMode);
        if (resolvedMode == activeMode) {
            return;
        }

        if (activeMode != null) {
            modelStates.get(activeMode).hide();
        }

        ModelState nextState = modelStates.get(resolvedMode);
        nextState.show();
        activeMode = resolvedMode;
    }

    public void face(Vector3f direction) {
        Vector3f horizontal = direction.clone();
        horizontal.y = 0f;
        if (horizontal.lengthSquared() < FastMath.ZERO_TOLERANCE) {
            return;
        }
        horizontal.normalizeLocal();
        float yaw = FastMath.atan2(horizontal.x, horizontal.z);
        setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
    }

    private CharacterMode resolveMode(CharacterMode requestedMode) {
        if (modelStates.containsKey(requestedMode)) {
            return requestedMode;
        }
        if (modelStates.containsKey(fallbackMode)) {
            return fallbackMode;
        }
        return modelStates.keySet().iterator().next();
    }

    private ModelState loadModel(AssetManager assetManager, AnimationVariant variant) {
        Spatial model = assetManager.loadModel(variant.assetPath());
        model.setLocalScale(variant.uniformScale());
        if (Math.abs(variant.baseYaw()) > FastMath.ZERO_TOLERANCE) {
            model.rotate(0f, variant.baseYaw(), 0f);
        }
        model.setCullHint(CullHint.Always);
        attachChild(model);
        return new ModelState(model, AnimationDriver.discover(model, variant.clipName()));
    }

    private record ModelState(Spatial model, AnimationDriver animationDriver) {

        void show() {
            model.setCullHint(CullHint.Inherit);
            animationDriver.playLoop();
        }

        void hide() {
            model.setCullHint(CullHint.Always);
        }
    }

    private interface AnimationDriver {

        void playLoop();

        static AnimationDriver discover(Spatial spatial, String clipName) {
            AnimComposer composer = findControl(spatial, AnimComposer.class);
            if (composer != null) {
                return () -> composer.setCurrentAction(clipName);
            }

            AnimControl legacyControl = findControl(spatial, AnimControl.class);
            if (legacyControl != null) {
                AnimChannel channel = legacyControl.createChannel();
                return () -> {
                    channel.setAnim(clipName);
                    channel.setLoopMode(LoopMode.Loop);
                    channel.setSpeed(1f);
                };
            }

            return () -> {
            };
        }

        private static <T extends Control> T findControl(Spatial spatial, Class<T> controlType) {
            T directControl = spatial.getControl(controlType);
            if (directControl != null) {
                return directControl;
            }
            if (spatial instanceof Node node) {
                for (Spatial child : node.getChildren()) {
                    T nestedControl = findControl(child, controlType);
                    if (nestedControl != null) {
                        return nestedControl;
                    }
                }
            }
            return null;
        }
    }
}
