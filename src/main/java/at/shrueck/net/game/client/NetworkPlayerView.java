package at.shrueck.net.game.client;

import at.shrueck.net.game.assets.AssetCatalog;
import at.shrueck.net.game.character.AnimatedActor;
import at.shrueck.net.game.shared.AvatarRole;
import at.shrueck.net.game.shared.StudentSkin;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;

public final class NetworkPlayerView extends Node {

    private final AssetManager assetManager;
    private final Vector3f targetPosition = new Vector3f();
    private final Vector3f smoothedPosition = new Vector3f();

    private AnimatedActor actor;
    private AvatarRole activeRole = AvatarRole.UNASSIGNED;
    private StudentSkin selectedStudentSkin = StudentSkin.FJP;
    private StudentSkin activeStudentSkin = StudentSkin.FJP;
    private float targetYaw;
    private float displayedYaw;
    private boolean initialized;

    public NetworkPlayerView(String name, AssetManager assetManager, int visualSeed) {
        super(name);
        this.assetManager = assetManager;
    }

    public void setStudentSkin(StudentSkin studentSkin) {
        if (studentSkin == null || selectedStudentSkin == studentSkin) {
            return;
        }
        selectedStudentSkin = studentSkin;
        if (activeRole != AvatarRole.SHRUECK) {
            ensureActor(activeRole);
        }
    }

    public void apply(ClientGameState.ClientPlayerState state) {
        setStudentSkin(state.studentSkin());
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

    public void setVisible(boolean visible) {
        setCullHint(visible ? CullHint.Inherit : CullHint.Always);
    }

    private void ensureActor(AvatarRole role) {
        AvatarRole resolvedRole = role == null ? AvatarRole.UNASSIGNED : role;
        boolean sameRole = resolvedRole == activeRole;
        boolean sameStudentSkin = resolvedRole == AvatarRole.SHRUECK || activeStudentSkin == selectedStudentSkin;
        if (actor != null && sameRole && sameStudentSkin) {
            return;
        }

        if (actor != null) {
            detachChild(actor);
        }

        if (resolvedRole == AvatarRole.SHRUECK) {
            actor = AssetCatalog.createShrueck(assetManager);
        } else {
            actor = AssetCatalog.createStudent(assetManager, selectedStudentSkin);
            activeStudentSkin = selectedStudentSkin;
        }
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
