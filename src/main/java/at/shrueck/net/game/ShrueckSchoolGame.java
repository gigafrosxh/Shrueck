package at.shrueck.net.game;

import at.shrueck.net.game.character.PlayerController;
import at.shrueck.net.game.client.ClientGameState;
import at.shrueck.net.game.client.LaunchConfig;
import at.shrueck.net.game.client.MultiplayerClientSession;
import at.shrueck.net.game.client.NetworkPlayerView;
import at.shrueck.net.game.server.LanGameServer;
import at.shrueck.net.game.shared.GameConstants;
import at.shrueck.net.game.shared.PlayerInputState;
import at.shrueck.net.game.shared.SessionPhase;
import at.shrueck.net.game.shared.StudentSkin;
import at.shrueck.net.game.ui.HudController;
import at.shrueck.net.game.world.SchoolLayout;
import at.shrueck.net.game.world.SchoolSceneBuilder;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.gltf.BinLoader;
import com.jme3.scene.plugins.gltf.GlbLoader;
import com.jme3.scene.plugins.gltf.GltfLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ShrueckSchoolGame extends SimpleApplication implements AnalogListener, ActionListener {

    private static final float THIRD_PERSON_CAMERA_DISTANCE = 15.6f;
    private static final float THIRD_PERSON_DEFAULT_PITCH = 30f * FastMath.DEG_TO_RAD;
    private static final float THIRD_PERSON_MIN_PITCH = 16f * FastMath.DEG_TO_RAD;
    private static final float THIRD_PERSON_MAX_PITCH = 55f * FastMath.DEG_TO_RAD;
    private static final float FIRST_PERSON_MIN_PITCH = -70f * FastMath.DEG_TO_RAD;
    private static final float FIRST_PERSON_MAX_PITCH = 70f * FastMath.DEG_TO_RAD;
    private static final float CAMERA_SENSITIVITY = 2.6f;
    private static final float THIRD_PERSON_TARGET_HEIGHT = 1.4f;
    private static final float FIRST_PERSON_TARGET_HEIGHT = 1.65f;
    private static final float CAMERA_WALL_MARGIN = 1.1f;

    private static final String CAMERA_LOOK_LEFT = "camera.look.left";
    private static final String CAMERA_LOOK_RIGHT = "camera.look.right";
    private static final String CAMERA_LOOK_UP = "camera.look.up";
    private static final String CAMERA_LOOK_DOWN = "camera.look.down";
    private static final String CAMERA_TOGGLE_PERSPECTIVE = "camera.perspective.toggle";
    private static final String CAMERA_CAPTURE = "camera.capture";
    private static final String CAMERA_RELEASE = "camera.release";
    private static final String LOBBY_START = "lobby.start";
    private static final String PLAYER_CYCLE_SKIN = "player.skin.cycle";

    private final LaunchConfig launchConfig;
    private final MultiplayerClientSession clientSession;
    private final LanGameServer hostedServer;
    private final Map<Integer, NetworkPlayerView> playerViews = new HashMap<>();

    private SchoolLayout schoolLayout;
    private PlayerController playerController;
    private HudController hudController;
    private ClientGameState currentState = ClientGameState.connecting();
    private ClientGameState appliedState;
    private Vector3f smoothedCameraLocation;
    private boolean mouseCaptured = true;
    private float cameraYaw;
    private float cameraPitch = THIRD_PERSON_DEFAULT_PITCH;
    private float inputSendAccumulator;
    private PlayerInputState lastSentInput = PlayerInputState.idle(0f);
    private StudentSkin localStudentSkin = StudentSkin.FJP;
    private CameraPerspective cameraPerspective = CameraPerspective.THIRD_PERSON;

    public ShrueckSchoolGame(LaunchConfig launchConfig, MultiplayerClientSession clientSession, LanGameServer hostedServer) {
        this.launchConfig = launchConfig;
        this.clientSession = clientSession;
        this.hostedServer = hostedServer;
    }

    @Override
    public void simpleInitApp() {
        registerAssetLoaders();

        setDisplayStatView(false);
        setDisplayFps(false);
        setPauseOnLostFocus(false);
        flyCam.setEnabled(false);
        registerMouseCameraControls();
        registerGameActions();
        captureMouse();
        viewPort.setBackgroundColor(new ColorRGBA(0.16f, 0.20f, 0.26f, 1f));

        schoolLayout = new SchoolSceneBuilder(assetManager).build(rootNode);
        addGlobalLights();

        playerController = new PlayerController(inputManager);
        hudController = new HudController(assetManager, guiNode, cam);

        cam.setFrustumPerspective(45f, (float) cam.getWidth() / cam.getHeight(), 0.1f, 200f);
        smoothedCameraLocation = fallbackCameraTarget().add(thirdPersonCameraOffset());
        updateCamera(1f, true);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (mouseCaptured && inputManager.isCursorVisible()) {
            captureMouse();
        }

        ClientGameState latestState = clientSession.currentState();
        currentState = latestState;
        if (latestState != appliedState) {
            appliedState = latestState;
            applyState(latestState);
        }

        String notice = clientSession.pollNotice();
        if (notice != null) {
            hudController.pushNotice(notice);
        }

        for (NetworkPlayerView playerView : playerViews.values()) {
            playerView.updateVisual(tpf);
        }

        sendLocalInput(tpf);
        updateCamera(tpf, false);
        hudController.render(tpf, currentState, clientSession.playerId(), launchConfig, mouseCaptured, cameraPerspective.label());
    }

    @Override
    public void destroy() {
        try {
            clientSession.close();
        } finally {
            if (hostedServer != null) {
                hostedServer.close();
            }
            super.destroy();
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!mouseCaptured) {
            return;
        }

        switch (name) {
            case CAMERA_LOOK_LEFT -> cameraYaw += value * CAMERA_SENSITIVITY;
            case CAMERA_LOOK_RIGHT -> cameraYaw -= value * CAMERA_SENSITIVITY;
            case CAMERA_LOOK_UP -> cameraPitch = FastMath.clamp(
                    cameraPitch - value * CAMERA_SENSITIVITY,
                    minimumCameraPitch(),
                    maximumCameraPitch()
            );
            case CAMERA_LOOK_DOWN -> cameraPitch = FastMath.clamp(
                    cameraPitch + value * CAMERA_SENSITIVITY,
                    minimumCameraPitch(),
                    maximumCameraPitch()
            );
            default -> {
            }
        }

        if (cameraYaw > FastMath.TWO_PI || cameraYaw < -FastMath.TWO_PI) {
            cameraYaw %= FastMath.TWO_PI;
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }

        switch (name) {
            case CAMERA_CAPTURE -> {
                if (!mouseCaptured) {
                    captureMouse();
                }
            }
            case CAMERA_RELEASE -> releaseMouse();
            case CAMERA_TOGGLE_PERSPECTIVE -> toggleCameraPerspective();
            case LOBBY_START -> {
                if (currentState.phase() == SessionPhase.LOBBY && currentState.hostId() == clientSession.playerId()) {
                    clientSession.requestRoundStart();
                } else if (currentState.phase() == SessionPhase.LOBBY) {
                    hudController.pushNotice("Nur der Host kann starten.");
                }
            }
            case PLAYER_CYCLE_SKIN -> cycleLocalStudentSkin();
            default -> {
            }
        }
    }

    private void applyState(ClientGameState state) {
        Set<Integer> activeIds = new HashSet<>();
        for (ClientGameState.ClientPlayerState player : state.players()) {
            activeIds.add(player.playerId());
            NetworkPlayerView view = playerViews.computeIfAbsent(player.playerId(), this::createPlayerView);
            if (player.playerId() == clientSession.playerId()) {
                view.setStudentSkin(localStudentSkin);
            } else {
                view.setStudentSkin(player.studentSkin());
            }
            view.apply(player);
        }
        syncLocalPlayerPresentation();

        Set<Integer> staleIds = new HashSet<>(playerViews.keySet());
        staleIds.removeAll(activeIds);
        for (Integer staleId : staleIds) {
            NetworkPlayerView staleView = playerViews.remove(staleId);
            if (staleView != null) {
                staleView.removeFromParent();
            }
        }
    }

    private NetworkPlayerView createPlayerView(int playerId) {
        NetworkPlayerView view = new NetworkPlayerView("network-player-" + playerId, assetManager, playerId);
        if (playerId == clientSession.playerId()) {
            view.setStudentSkin(localStudentSkin);
        }
        rootNode.attachChild(view);
        syncLocalPlayerPresentation();
        return view;
    }

    private void cycleLocalStudentSkin() {
        localStudentSkin = localStudentSkin.next();
        clientSession.sendSkinSelection(localStudentSkin);
        NetworkPlayerView localView = playerViews.get(clientSession.playerId());
        if (localView != null) {
            localView.setStudentSkin(localStudentSkin);
        }
        hudController.pushNotice("Spieler-Skin: " + localStudentSkin.label());
    }

    private void sendLocalInput(float tpf) {
        if (currentState.phase() != SessionPhase.RUNNING) {
            inputSendAccumulator = 0f;
            lastSentInput = PlayerInputState.idle(cameraYaw);
            return;
        }

        ClientGameState.ClientPlayerState localPlayer = currentState.findPlayer(clientSession.playerId());
        if (localPlayer == null) {
            return;
        }

        PlayerInputState input = localPlayer.captured()
                ? PlayerInputState.idle(cameraYaw)
                : playerController.snapshot(cameraYaw);

        inputSendAccumulator += tpf;
        boolean changedButtons = !input.sameButtons(lastSentInput);
        boolean changedYaw = Math.abs(input.cameraYaw() - lastSentInput.cameraYaw()) > 0.02f;
        if (inputSendAccumulator >= 1f / GameConstants.SERVER_TICK_RATE || changedButtons || changedYaw) {
            clientSession.sendInput(input);
            inputSendAccumulator = 0f;
            lastSentInput = input;
        }
    }

    private void registerAssetLoaders() {
        assetManager.registerLoader(GltfLoader.class, "gltf");
        assetManager.registerLoader(GlbLoader.class, "glb");
        assetManager.registerLoader(BinLoader.class, "bin");
    }

    private void registerMouseCameraControls() {
        inputManager.addMapping(CAMERA_LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(CAMERA_LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping(CAMERA_LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping(CAMERA_LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(CAMERA_CAPTURE, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(CAMERA_RELEASE, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener(this, CAMERA_LOOK_LEFT, CAMERA_LOOK_RIGHT, CAMERA_LOOK_UP, CAMERA_LOOK_DOWN, CAMERA_CAPTURE, CAMERA_RELEASE);
    }

    private void registerGameActions() {
        inputManager.addMapping(LOBBY_START, new KeyTrigger(KeyInput.KEY_RETURN), new KeyTrigger(KeyInput.KEY_NUMPADENTER));
        inputManager.addMapping(
                CAMERA_TOGGLE_PERSPECTIVE,
                new KeyTrigger(KeyInput.KEY_F3),
                new KeyTrigger(KeyInput.KEY_3),
                new KeyTrigger(KeyInput.KEY_NUMPAD3)
        );
        inputManager.addMapping(
                PLAYER_CYCLE_SKIN,
                new KeyTrigger(KeyInput.KEY_F6),
                new KeyTrigger(KeyInput.KEY_6),
                new KeyTrigger(KeyInput.KEY_NUMPAD6)
        );
        inputManager.addListener(this, LOBBY_START, CAMERA_TOGGLE_PERSPECTIVE, PLAYER_CYCLE_SKIN);
    }

    private void captureMouse() {
        mouseCaptured = true;
        inputManager.setCursorVisible(false);
        if (context != null && context.getMouseInput() != null) {
            context.getMouseInput().setCursorVisible(false);
        }
    }

    private void releaseMouse() {
        mouseCaptured = false;
        inputManager.setCursorVisible(true);
        if (context != null && context.getMouseInput() != null) {
            context.getMouseInput().setCursorVisible(true);
        }
    }

    private void addGlobalLights() {
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(new ColorRGBA(0.24f, 0.25f, 0.28f, 1f));
        rootNode.addLight(ambientLight);

        DirectionalLight sunlight = new DirectionalLight();
        sunlight.setDirection(new Vector3f(-0.35f, -1f, -0.25f).normalizeLocal());
        sunlight.setColor(new ColorRGBA(0.52f, 0.54f, 0.58f, 1f));
        rootNode.addLight(sunlight);
    }

    private void updateCamera(float tpf, boolean instant) {
        Vector3f target = cameraTarget();
        if (cameraPerspective == CameraPerspective.FIRST_PERSON) {
            smoothedCameraLocation = target.clone();
            cam.setLocation(smoothedCameraLocation);
            cam.lookAtDirection(cameraLookDirection(), Vector3f.UNIT_Y);
            return;
        }

        Vector3f desiredLocation = schoolLayout.constrainCamera(target, target.add(thirdPersonCameraOffset()), CAMERA_WALL_MARGIN);
        if (instant || smoothedCameraLocation == null) {
            smoothedCameraLocation = desiredLocation.clone();
        } else {
            float smoothing = FastMath.clamp(1f - FastMath.exp(-11f * tpf), 0f, 1f);
            smoothedCameraLocation.interpolateLocal(desiredLocation, smoothing);
        }
        cam.setLocation(smoothedCameraLocation);
        cam.lookAt(target, Vector3f.UNIT_Y);
    }

    private Vector3f cameraTarget() {
        NetworkPlayerView localView = playerViews.get(clientSession.playerId());
        if (localView != null) {
            return localView.getWorldTranslation().add(0f, cameraTargetHeight(), 0f);
        }
        return fallbackCameraTarget();
    }

    private Vector3f fallbackCameraTarget() {
        return new Vector3f(0f, cameraTargetHeight(), 18f);
    }

    private Vector3f thirdPersonCameraOffset() {
        float horizontalDistance = THIRD_PERSON_CAMERA_DISTANCE * FastMath.cos(cameraPitch);
        return new Vector3f(
                FastMath.sin(cameraYaw) * horizontalDistance,
                THIRD_PERSON_CAMERA_DISTANCE * FastMath.sin(cameraPitch),
                FastMath.cos(cameraYaw) * horizontalDistance
        );
    }

    private Vector3f cameraLookDirection() {
        float horizontalStrength = FastMath.cos(cameraPitch);
        return new Vector3f(
                FastMath.sin(cameraYaw) * horizontalStrength,
                FastMath.sin(cameraPitch),
                FastMath.cos(cameraYaw) * horizontalStrength
        ).normalizeLocal();
    }

    private float minimumCameraPitch() {
        return cameraPerspective == CameraPerspective.FIRST_PERSON ? FIRST_PERSON_MIN_PITCH : THIRD_PERSON_MIN_PITCH;
    }

    private float maximumCameraPitch() {
        return cameraPerspective == CameraPerspective.FIRST_PERSON ? FIRST_PERSON_MAX_PITCH : THIRD_PERSON_MAX_PITCH;
    }

    private float cameraTargetHeight() {
        return cameraPerspective == CameraPerspective.FIRST_PERSON ? FIRST_PERSON_TARGET_HEIGHT : THIRD_PERSON_TARGET_HEIGHT;
    }

    private void toggleCameraPerspective() {
        if (cameraPerspective == CameraPerspective.THIRD_PERSON) {
            cameraPitch = FastMath.clamp(THIRD_PERSON_DEFAULT_PITCH - cameraPitch, FIRST_PERSON_MIN_PITCH, FIRST_PERSON_MAX_PITCH);
            cameraPerspective = CameraPerspective.FIRST_PERSON;
        } else {
            cameraPitch = FastMath.clamp(THIRD_PERSON_DEFAULT_PITCH - cameraPitch, THIRD_PERSON_MIN_PITCH, THIRD_PERSON_MAX_PITCH);
            cameraPerspective = CameraPerspective.THIRD_PERSON;
        }
        syncLocalPlayerPresentation();
        if (hudController != null) {
            hudController.pushNotice("Kamera: " + cameraPerspective.label());
        }
        if (cam != null) {
            updateCamera(1f, true);
        }
    }

    private void syncLocalPlayerPresentation() {
        NetworkPlayerView localView = playerViews.get(clientSession.playerId());
        if (localView != null) {
            localView.setVisible(cameraPerspective == CameraPerspective.THIRD_PERSON);
        }
    }

    private enum CameraPerspective {
        THIRD_PERSON("3rd Person"),
        FIRST_PERSON("1st Person");

        private final String label;

        CameraPerspective(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
