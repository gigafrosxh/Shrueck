package at.shrueck.net.game.shared;

public enum SessionPhase {
    CONNECTING(0, "Verbinde"),
    LOBBY(1, "Lobby"),
    RUNNING(2, "Runde laeuft"),
    RESULT(3, "Ergebnis"),
    DISCONNECTED(4, "Getrennt");

    private final int code;
    private final String label;

    SessionPhase(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static SessionPhase fromCode(int code) {
        for (SessionPhase phase : values()) {
            if (phase.code == code) {
                return phase;
            }
        }
        return CONNECTING;
    }
}