package at.shrueck.net.game.shared;

public record PlayerInputState(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean sprint,
        float cameraYaw
) {

    public static PlayerInputState idle(float cameraYaw) {
        return new PlayerInputState(false, false, false, false, false, cameraYaw);
    }

    public boolean hasMovement() {
        return forward || backward || left || right;
    }

    public boolean sameButtons(PlayerInputState other) {
        if (other == null) {
            return false;
        }
        return forward == other.forward
                && backward == other.backward
                && left == other.left
                && right == other.right
                && sprint == other.sprint;
    }
}