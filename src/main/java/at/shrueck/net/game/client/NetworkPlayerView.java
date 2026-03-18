package at.shrueck.net.game.client;

import at.shrueck.net.game.assets.AssetCatalog;
import at.shrueck.net.game.character.AnimatedActor;
import at.shrueck.net.game.shared.AvatarRole;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public final class NetworkPlayerView extends Node {

    private final AssetManager assetManager;
    private final int visualSeed;
    private final Vector3f targetPosition = new Vector3f();
    private final Vector3f smoothedPosition = new Vector3f();

    private AnimatedActor actor;
    private AvatarRole activeRole = AvatarRole.UNASSIGNED;
    private float targetYaw;
    private float displayedYaw;
    private boolean initialized;

    public NetworkPlayerView(String name, AssetManager assetManager, int visualSeed) {
        super(name);
        this.assetManager = assetManager;
        this.visualSeed = visualSeed;
    }

    public void apply(ClientGameState.ClientPlayerState state) {
        ensureActor(state.role());
        targetPosition.set(state.x(), 0f, state.z());
        targetYaw = state.yaw();
        actor.setMode(state.mode());

        if (!initialized) {
            smoothedPosition.set(targetPosition);
            displayedYaw = targetYaw;
            setLocalTranslation(smoothedPosition);
            setLocalRotation(yawRotation(displayedYaw));
            initialized = true;
        }
    }

    public void updateVisual(float tpf) {
        if (!initialized) {
            return;
        }
        float factor = FastMath.clamp(1f - FastMath.exp(-12f * tpf), 0f, 1f);
        smoothedPosition.interpolateLocal(targetPosition, factor);
        displayedYaw = interpolateAngle(displayedYaw, targetYaw, factor);
        setLocalTranslation(smoothedPosition);
        setLocalRotation(yawRotation(displayedYaw));
    }

    private void ensureActor(AvatarRole role) {
        AvatarRole resolvedRole = role == null ? AvatarRole.UNASSIGNED : role;
        if (actor != null && resolvedRole == activeRole) {
            return;
        }

        if (actor != null) {
            detachChild(actor);
        }

        actor = resolvedRole == AvatarRole.SHRUECK
                ? AssetCatalog.createShrueck(assetManager)
                : AssetCatalog.createStudent(assetManager, true);
        actor.setLocalTranslation(Vector3f.ZERO);
        attachChild(actor);
        activeRole = resolvedRole;
    }

    private static Quaternion yawRotation(float yaw) {
        return new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y);
    }

    private static float interpolateAngle(float current, float target, float factor) {
        float delta = target - current;
        while (delta > FastMath.PI) {
            delta -= FastMath.TWO_PI;
        }
        while (delta < -FastMath.PI) {
            delta += FastMath.TWO_PI;
        }
        return current + delta * factor;
    }
}