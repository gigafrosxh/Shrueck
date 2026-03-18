package at.shrueck.net.game.shared;

import at.shrueck.net.game.character.CharacterMode;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public final class MovementMath {

    private MovementMath() {
    }

    public static Vector3f resolveMoveDelta(PlayerInputState input, AvatarRole role, float tpf) {
        Vector3f direction = resolveDirection(input);
        if (direction.lengthSquared() <= FastMath.ZERO_TOLERANCE) {
            return Vector3f.ZERO.clone();
        }
        float speed = speedFor(role, input.sprint());
        return direction.mult(speed * tpf);
    }

    public static Vector3f resolveDirection(PlayerInputState input) {
        float horizontalAxis = 0f;
        float verticalAxis = 0f;

        if (input.left()) {
            horizontalAxis -= 1f;
        }
        if (input.right()) {
            horizontalAxis += 1f;
        }
        if (input.forward()) {
            verticalAxis += 1f;
        }
        if (input.backward()) {
            verticalAxis -= 1f;
        }

        if (horizontalAxis == 0f && verticalAxis == 0f) {
            return Vector3f.ZERO.clone();
        }

        Vector3f cameraForward = new Vector3f(
                -FastMath.sin(input.cameraYaw()),
                0f,
                -FastMath.cos(input.cameraYaw())
        );
        Vector3f cameraRight = new Vector3f(-cameraForward.z, 0f, cameraForward.x);
        return cameraRight.mult(horizontalAxis).addLocal(cameraForward.mult(verticalAxis)).normalizeLocal();
    }

    public static float speedFor(AvatarRole role, boolean sprint) {
        if (role == AvatarRole.SHRUECK) {
            return sprint ? GameConstants.SHRUECK_SPRINT_SPEED : GameConstants.SHRUECK_WALK_SPEED;
        }
        return sprint ? GameConstants.STUDENT_SPRINT_SPEED : GameConstants.STUDENT_WALK_SPEED;
    }

    public static CharacterMode idleModeForRole(AvatarRole role) {
        return role == AvatarRole.SHRUECK ? CharacterMode.IDLE : CharacterMode.SPECIAL;
    }

    public static CharacterMode moveMode(boolean sprint) {
        return sprint ? CharacterMode.FAST : CharacterMode.MOVE;
    }

    public static CharacterMode resultMode(AvatarRole role, RoundWinner winner, boolean captured) {
        if (role == AvatarRole.SHRUECK) {
            return winner == RoundWinner.SHRUECK ? CharacterMode.SPECIAL : CharacterMode.IDLE;
        }
        if (captured) {
            return CharacterMode.SPECIAL;
        }
        return winner == RoundWinner.STUDENTS ? CharacterMode.SPECIAL : CharacterMode.MOVE;
    }

    public static float yawFromDirection(Vector3f direction, float fallbackYaw) {
        Vector3f horizontal = direction.clone();
        horizontal.y = 0f;
        if (horizontal.lengthSquared() <= FastMath.ZERO_TOLERANCE) {
            return fallbackYaw;
        }
        horizontal.normalizeLocal();
        return FastMath.atan2(horizontal.x, horizontal.z);
    }
}