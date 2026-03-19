package at.shrueck.net.game.server;

import at.shrueck.net.game.character.CharacterMode;
import at.shrueck.net.game.shared.AvatarRole;
import at.shrueck.net.game.shared.GameConstants;
import at.shrueck.net.game.shared.PowerUpType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerEffectStateTest {

    @Test
    void studentBuffsApplyIndependentlyAndExpireCleanly() {
        PlayerEffectState effects = new PlayerEffectState();
        long nowNanos = 1_000_000_000L;

        effects.apply(PowerUpType.STUDENT_SPEED, nowNanos);
        effects.apply(PowerUpType.STUDENT_GROWTH, nowNanos);

        assertEquals(GameConstants.POWER_UP_STUDENT_SPEED_MULTIPLIER, effects.speedMultiplier(AvatarRole.STUDENT, nowNanos + 1L));
        assertEquals(GameConstants.POWER_UP_STUDENT_GROWTH_SCALE_MULTIPLIER, effects.visualScaleMultiplier(AvatarRole.STUDENT, nowNanos + 1L));
        assertEquals(
                PowerUpType.STUDENT_SPEED.effectMask() | PowerUpType.STUDENT_GROWTH.effectMask(),
                effects.effectMask(AvatarRole.STUDENT, nowNanos + 1L)
        );

        long afterSpeedEnds = nowNanos + secondsToNanos(GameConstants.POWER_UP_STUDENT_SPEED_DURATION_SECONDS) + 1L;
        assertEquals(1f, effects.speedMultiplier(AvatarRole.STUDENT, afterSpeedEnds));
        assertEquals(GameConstants.POWER_UP_STUDENT_GROWTH_SCALE_MULTIPLIER, effects.visualScaleMultiplier(AvatarRole.STUDENT, afterSpeedEnds));

        long afterGrowthEnds = nowNanos + secondsToNanos(GameConstants.POWER_UP_STUDENT_GROWTH_DURATION_SECONDS) + 1L;
        assertEquals(1f, effects.visualScaleMultiplier(AvatarRole.STUDENT, afterGrowthEnds));
        assertEquals(0, effects.effectMask(AvatarRole.STUDENT, afterGrowthEnds));
    }

    @Test
    void shrueckSlowAndStunCanOverlapWithoutConflict() {
        PlayerEffectState effects = new PlayerEffectState();
        long nowNanos = 2_000_000_000L;

        effects.apply(PowerUpType.SHRUECK_SLOW, nowNanos);
        effects.apply(PowerUpType.SHRUECK_STUN, nowNanos);

        assertTrue(effects.preventsMovement(AvatarRole.SHRUECK, nowNanos + 1L));
        assertEquals(CharacterMode.STUNNED, effects.resolveMode(AvatarRole.SHRUECK, CharacterMode.MOVE, true, nowNanos + 1L));

        long afterStunEnds = nowNanos + secondsToNanos(GameConstants.POWER_UP_SHRUECK_STUN_DURATION_SECONDS) + 1L;
        assertFalse(effects.preventsMovement(AvatarRole.SHRUECK, afterStunEnds));
        assertEquals(GameConstants.POWER_UP_SHRUECK_SLOW_MULTIPLIER, effects.speedMultiplier(AvatarRole.SHRUECK, afterStunEnds));
        assertEquals(CharacterMode.DEBUFF, effects.resolveMode(AvatarRole.SHRUECK, CharacterMode.MOVE, true, afterStunEnds));

        long afterSlowEnds = nowNanos + secondsToNanos(GameConstants.POWER_UP_SHRUECK_SLOW_DURATION_SECONDS) + 1L;
        assertEquals(1f, effects.speedMultiplier(AvatarRole.SHRUECK, afterSlowEnds));
        assertEquals(CharacterMode.MOVE, effects.resolveMode(AvatarRole.SHRUECK, CharacterMode.MOVE, true, afterSlowEnds));
    }

    @Test
    void clearRemovesAllActiveEffects() {
        PlayerEffectState effects = new PlayerEffectState();
        long nowNanos = 3_000_000_000L;

        effects.apply(PowerUpType.STUDENT_SPEED, nowNanos);
        effects.apply(PowerUpType.SHRUECK_STUN, nowNanos);
        effects.clear();

        assertEquals(1f, effects.speedMultiplier(AvatarRole.STUDENT, nowNanos + 1L));
        assertEquals(1f, effects.visualScaleMultiplier(AvatarRole.STUDENT, nowNanos + 1L));
        assertFalse(effects.preventsMovement(AvatarRole.SHRUECK, nowNanos + 1L));
        assertEquals(0, effects.effectMask(AvatarRole.STUDENT, nowNanos + 1L));
    }

    private long secondsToNanos(float seconds) {
        return (long) (seconds * 1_000_000_000L);
    }
}
