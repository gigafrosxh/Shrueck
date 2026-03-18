package at.shrueck.net.game.character;

import at.shrueck.net.game.shared.PlayerInputState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

public final class PlayerController implements ActionListener {

    private static final String MOVE_FORWARD = "player.move.forward";
    private static final String MOVE_BACKWARD = "player.move.backward";
    private static final String MOVE_LEFT = "player.move.left";
    private static final String MOVE_RIGHT = "player.move.right";
    private static final String SPRINT = "player.move.sprint";

    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean sprint;

    public PlayerController(InputManager inputManager) {
        register(inputManager);
    }

    public PlayerInputState snapshot(float cameraYaw) {
        return new PlayerInputState(forward, backward, left, right, sprint, cameraYaw);
    }

    public void reset() {
        forward = false;
        backward = false;
        left = false;
        right = false;
        sprint = false;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case MOVE_FORWARD -> forward = isPressed;
            case MOVE_BACKWARD -> backward = isPressed;
            case MOVE_LEFT -> left = isPressed;
            case MOVE_RIGHT -> right = isPressed;
            case SPRINT -> sprint = isPressed;
            default -> {
            }
        }
    }

    private void register(InputManager inputManager) {
        inputManager.addMapping(MOVE_FORWARD, new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(MOVE_BACKWARD, new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping(MOVE_LEFT, new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(SPRINT, new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addListener(this, MOVE_FORWARD, MOVE_BACKWARD, MOVE_LEFT, MOVE_RIGHT, SPRINT);
    }
}