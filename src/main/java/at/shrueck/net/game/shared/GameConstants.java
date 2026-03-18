package at.shrueck.net.game.shared;

public final class GameConstants {

    public static final String NETWORK_GAME_NAME = "shrueck-lan";
    public static final int NETWORK_VERSION = 1;
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

    private GameConstants() {
    }
}