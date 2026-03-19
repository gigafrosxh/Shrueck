package at.shrueck.net.game.shared;

import java.util.Random;

public enum PowerUpType {
    STUDENT_SPEED(0, "Sprint-Saft", AvatarRole.STUDENT, 1),
    STUDENT_GROWTH(1, "Riesenpause", AvatarRole.STUDENT, 1 << 1),
    SHRUECK_SLOW(2, "Kaugummi-Falle", AvatarRole.SHRUECK, 1 << 2),
    SHRUECK_STUN(3, "Trittfalle", AvatarRole.SHRUECK, 1 << 3);

    private final int code;
    private final String label;
    private final AvatarRole targetRole;
    private final int effectMask;

    PowerUpType(int code, String label, AvatarRole targetRole, int effectMask) {
        this.code = code;
        this.label = label;
        this.targetRole = targetRole;
        this.effectMask = effectMask;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public AvatarRole targetRole() {
        return targetRole;
    }

    public int effectMask() {
        return effectMask;
    }

    public float durationSeconds() {
        return switch (this) {
            case STUDENT_SPEED -> GameConstants.POWER_UP_STUDENT_SPEED_DURATION_SECONDS;
            case STUDENT_GROWTH -> GameConstants.POWER_UP_STUDENT_GROWTH_DURATION_SECONDS;
            case SHRUECK_SLOW -> GameConstants.POWER_UP_SHRUECK_SLOW_DURATION_SECONDS;
            case SHRUECK_STUN -> GameConstants.POWER_UP_SHRUECK_STUN_DURATION_SECONDS;
        };
    }

    public static PowerUpType random(Random random) {
        PowerUpType[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static PowerUpType fromCode(int code) {
        for (PowerUpType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return STUDENT_SPEED;
    }
}
