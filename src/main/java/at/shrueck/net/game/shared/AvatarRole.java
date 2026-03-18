package at.shrueck.net.game.shared;

public enum AvatarRole {
    UNASSIGNED(0, "Lobby"),
    SHRUECK(1, "Shrueck"),
    STUDENT(2, "Schueler");

    private final int code;
    private final String label;

    AvatarRole(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AvatarRole fromCode(int code) {
        for (AvatarRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return UNASSIGNED;
    }
}