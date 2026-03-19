package at.shrueck.net.game.shared;

public final class GameConstants {

    public static final String NETWORK_GAME_NAME = "shrueck-lan";
    public static final int NETWORK_VERSION = 3;
    public static final int DEFAULT_PORT = 6143;

    public static final float SERVER_TICK_RATE = 20f;
    public static final float ROUND_DURATION_SECONDS = 150f;
    public static final float RESULT_DURATION_SECONDS = 6f;

    public static final int MIN_PLAYERS_TO_START = 2;

    public static final float PLAYER_RADIUS = 1.15f;
    public static final float CATCH_RADIUS = 2.35f;

    public static final float SHRUECK_WALK_SPEED = 6.5f;
    public static final float SHRUECK_SPRINT_SPEED = 9.5f;
    public static final float STUDENT_WALK_SPEED = 5.8f;
    public static final float STUDENT_SPRINT_SPEED = 8.0f;

    public static final int POWER_UP_MAX_ACTIVE = 3;
    public static final float POWER_UP_PICKUP_RADIUS = 1.45f;
    public static final float POWER_UP_RESPAWN_SECONDS = 7f;
    public static final float POWER_UP_PLAYER_CLEARANCE = 4.6f;
    public static final float POWER_UP_MIN_SPACING = 4.9f;

    public static final float POWER_UP_STUDENT_SPEED_MULTIPLIER = 1.35f;
    public static final float POWER_UP_STUDENT_GROWTH_SCALE_MULTIPLIER = 1.35f;
    public static final float POWER_UP_SHRUECK_SLOW_MULTIPLIER = 0.58f;

    public static final float POWER_UP_STUDENT_SPEED_DURATION_SECONDS = 8f;
    public static final float POWER_UP_STUDENT_GROWTH_DURATION_SECONDS = 10f;
    public static final float POWER_UP_SHRUECK_SLOW_DURATION_SECONDS = 6f;
    public static final float POWER_UP_SHRUECK_STUN_DURATION_SECONDS = 2.5f;

    private GameConstants() {
    }
}
