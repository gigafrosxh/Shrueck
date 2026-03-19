package at.shrueck.net.game.server;

import at.shrueck.net.game.shared.AvatarRole;
import at.shrueck.net.game.shared.MovementMath;
import at.shrueck.net.game.shared.PlayerInputState;
import at.shrueck.net.game.shared.PowerUpType;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerUpMovementIntegrationTest {

    @Test
    void speedBoostIncreasesStudentTravelUntilItExpires() {
        PlayerEffectState effects = new PlayerEffectState();
        PlayerInputState input = new PlayerInputState(true, false, false, false, false, 0f);
        long nowNanos = 4_000_000_000L;

        Vector3f normalDelta = MovementMath.resolveMoveDelta(input, AvatarRole.STUDENT, 1f, 1f);
        effects.apply(PowerUpType.STUDENT_SPEED, nowNanos);
        Vector3f boostedDelta = MovementMath.resolveMoveDelta(
                input,
                AvatarRole.STUDENT,
                1f,
                effects.speedMultiplier(AvatarRole.STUDENT, nowNanos + 1L)
        );
        Vector3f expiredDelta = MovementMath.resolveMoveDelta(
                input,
                AvatarRole.STUDENT,
                1f,
                effects.speedMultiplier(AvatarRole.STUDENT, nowNanos + 8_000_000_001L)
        );

        assertTrue(boostedDelta.length() > normalDelta.length());
        assertEquals(normalDelta.length(), expiredDelta.length(), 0.0001f);
    }

    @Test
    void stunStopsShrueckBeforeSlowTakesOver() {
        PlayerEffectState effects = new PlayerEffectState();
        PlayerInputState input = new PlayerInputState(true, false, false, false, true, 0f);
        long nowNanos = 5_000_000_000L;

        effects.apply(PowerUpType.SHRUECK_SLOW, nowNanos);
        effects.apply(PowerUpType.SHRUECK_STUN, nowNanos);

        Vector3f stunnedDelta = effects.preventsMovement(AvatarRole.SHRUECK, nowNanos + 1L)
                ? Vector3f.ZERO.clone()
                : MovementMath.resolveMoveDelta(input, AvatarRole.SHRUECK, 1f, effects.speedMultiplier(AvatarRole.SHRUECK, nowNanos + 1L));
        Vector3f slowedDelta = MovementMath.resolveMoveDelta(input, AvatarRole.SHRUECK, 1f, effects.speedMultiplier(AvatarRole.SHRUECK, nowNanos + 2_500_000_001L));
        Vector3f normalDelta = MovementMath.resolveMoveDelta(input, AvatarRole.SHRUECK, 1f, 1f);

        assertEquals(0f, stunnedDelta.length(), 0.0001f);
        assertTrue(slowedDelta.length() > 0f);
        assertTrue(slowedDelta.length() < normalDelta.length());
    }
}
