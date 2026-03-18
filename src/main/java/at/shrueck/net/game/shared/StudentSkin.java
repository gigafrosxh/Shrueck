package at.shrueck.net.game.shared;

public enum StudentSkin {
    FJP(0, "fjp"),
    CH(1, "ch");

    private final int code;
    private final String label;

    StudentSkin(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public StudentSkin next() {
        StudentSkin[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static StudentSkin fromCode(int code) {
        for (StudentSkin skin : values()) {
            if (skin.code == code) {
                return skin;
            }
        }
        return FJP;
    }
}