package at.shrueck.net.game.shared;

public enum RoundWinner {
    NONE(0, "Offen"),
    SHRUECK(1, "Shrueck"),
    STUDENTS(2, "Schueler");

    private final int code;
    private final String label;

    RoundWinner(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static RoundWinner fromCode(int code) {
        for (RoundWinner winner : values()) {
            if (winner.code == code) {
                return winner;
            }
        }
        return NONE;
    }
}