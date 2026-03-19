package at.shrueck.net.game.server;

import at.shrueck.net.game.character.CharacterMode;
import at.shrueck.net.game.shared.AvatarRole;
import at.shrueck.net.game.shared.GameConstants;
import at.shrueck.net.game.shared.PowerUpType;

final class PlayerEffectState {

    private long studentSpeedUntilNanos;
    private long studentGrowthUntilNanos;
    private long shrueckSlowUntilNanos;
    private long shrueckStunUntilNanos;

    void clear() {
        studentSpeedUntilNanos = 0L;
        studentGrowthUntilNanos = 0L;
        shrueckSlowUntilNanos = 0L;
        shrueckStunUntilNanos = 0L;
    }

    void apply(PowerUpType type, long nowNanos) {
        long durationNanos = (long) (type.durationSeconds() * 1_000_000_000L);
        long expiresAt = nowNanos + durationNanos;
        switch (type) {
            case STUDENT_SPEED -> studentSpeedUntilNanos = Math.max(studentSpeedUntilNanos, expiresAt);
            case STUDENT_GROWTH -> studentGrowthUntilNanos = Math.max(studentGrowthUntilNanos, expiresAt);
            case SHRUECK_SLOW -> shrueckSlowUntilNanos = Math.max(shrueckSlowUntilNanos, expiresAt);
            case SHRUECK_STUN -> shrueckStunUntilNanos = Math.max(shrueckStunUntilNanos, expiresAt);
        }
    }

    float speedMultiplier(AvatarRole role, long nowNanos) {
        if (role == AvatarRole.STUDENT && isActive(studentSpeedUntilNanos, nowNanos)) {
            return GameConstants.POWER_UP_STUDENT_SPEED_MULTIPLIER;
        }
        if (role == AvatarRole.SHRUECK && isActive(shrueckSlowUntilNanos, nowNanos)) {
            return GameConstants.POWER_UP_SHRUECK_SLOW_MULTIPLIER;
        }
        return 1f;
    }

    float visualScaleMultiplier(AvatarRole role, long nowNanos) {
        if (role == AvatarRole.STUDENT && isActive(studentGrowthUntilNanos, nowNanos)) {
            return GameConstants.POWER_UP_STUDENT_GROWTH_SCALE_MULTIPLIER;
        }
        return 1f;
    }

    boolean preventsMovement(AvatarRole role, long nowNanos) {
        return role == AvatarRole.SHRUECK && isActive(shrueckStunUntilNanos, nowNanos);
    }

    int effectMask(AvatarRole role, long nowNanos) {
        int mask = 0;
        if (role == AvatarRole.STUDENT && isActive(studentSpeedUntilNanos, nowNanos)) {
            mask |= PowerUpType.STUDENT_SPEED.effectMask();
        }
        if (role == AvatarRole.STUDENT && isActive(studentGrowthUntilNanos, nowNanos)) {
            mask |= PowerUpType.STUDENT_GROWTH.effectMask();
        }
        if (role == AvatarRole.SHRUECK && isActive(shrueckSlowUntilNanos, nowNanos)) {
            mask |= PowerUpType.SHRUECK_SLOW.effectMask();
        }
        if (role == AvatarRole.SHRUECK && isActive(shrueckStunUntilNanos, nowNanos)) {
            mask |= PowerUpType.SHRUECK_STUN.effectMask();
        }
        return mask;
    }

    CharacterMode resolveMode(AvatarRole role, CharacterMode baseMode, boolean moving, long nowNanos) {
        if (role == AvatarRole.SHRUECK && isActive(shrueckStunUntilNanos, nowNanos)) {
            return CharacterMode.STUNNED;
        }
        if (role == AvatarRole.SHRUECK && moving && isActive(shrueckSlowUntilNanos, nowNanos)) {
            return CharacterMode.DEBUFF;
        }
        return baseMode;
    }

    boolean hasAnyEffect(AvatarRole role, long nowNanos) {
        return effectMask(role, nowNanos) != 0;
    }

    private static boolean isActive(long expiresAtNanos, long nowNanos) {
        return expiresAtNanos > nowNanos;
    }
}
