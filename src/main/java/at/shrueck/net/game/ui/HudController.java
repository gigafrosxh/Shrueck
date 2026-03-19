package at.shrueck.net.game.ui;

import at.shrueck.net.game.client.ClientGameState;
import at.shrueck.net.game.client.LaunchConfig;
import at.shrueck.net.game.shared.AvatarRole;
import at.shrueck.net.game.shared.PowerUpType;
import at.shrueck.net.game.shared.RoundWinner;
import at.shrueck.net.game.shared.SessionPhase;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

public final class HudController {

    private final Camera camera;
    private final BitmapText statusText;
    private final BitmapText playerListText;
    private final BitmapText instructionText;
    private final BitmapText bannerText;
    private final BitmapText noticeText;

    private String transientNotice = "";
    private float transientNoticeTime;

    public HudController(AssetManager assetManager, Node guiNode, Camera camera) {
        this.camera = camera;

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        statusText = new BitmapText(font);
        statusText.setColor(ColorRGBA.White);
        statusText.setSize(font.getCharSet().getRenderedSize() * 1.15f);
        guiNode.attachChild(statusText);

        playerListText = new BitmapText(font);
        playerListText.setColor(new ColorRGBA(0.88f, 0.92f, 1f, 1f));
        playerListText.setSize(font.getCharSet().getRenderedSize());
        guiNode.attachChild(playerListText);

        instructionText = new BitmapText(font);
        instructionText.setColor(new ColorRGBA(0.90f, 0.95f, 1f, 1f));
        instructionText.setSize(font.getCharSet().getRenderedSize());
        guiNode.attachChild(instructionText);

        bannerText = new BitmapText(font);
        bannerText.setColor(new ColorRGBA(1f, 0.95f, 0.55f, 1f));
        bannerText.setSize(font.getCharSet().getRenderedSize() * 1.7f);
        guiNode.attachChild(bannerText);

        noticeText = new BitmapText(font);
        noticeText.setColor(new ColorRGBA(1f, 0.80f, 0.55f, 1f));
        noticeText.setSize(font.getCharSet().getRenderedSize());
        guiNode.attachChild(noticeText);
    }

    public void pushNotice(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        transientNotice = text;
        transientNoticeTime = 4f;
    }

    public void render(
            float tpf,
            ClientGameState state,
            int localPlayerId,
            LaunchConfig launchConfig,
            boolean mouseCaptured,
            String cameraPerspectiveLabel
    ) {
        if (transientNoticeTime > 0f) {
            transientNoticeTime = Math.max(0f, transientNoticeTime - tpf);
        }
        if (transientNoticeTime == 0f) {
            transientNotice = "";
        }

        ClientGameState.ClientPlayerState localPlayer = state.findPlayer(localPlayerId);

        statusText.setText(buildStatusText(state, localPlayer, launchConfig));
        playerListText.setText(buildPlayerListText(state, localPlayerId));
        instructionText.setText(buildInstructionText(state, localPlayer, launchConfig.isHost(), mouseCaptured, cameraPerspectiveLabel));
        bannerText.setText(buildBannerText(state, localPlayer));
        noticeText.setText(transientNotice);

        refreshLayout();
    }

    private String buildStatusText(ClientGameState state, ClientGameState.ClientPlayerState localPlayer, LaunchConfig launchConfig) {
        StringBuilder builder = new StringBuilder();
        builder.append(launchConfig.isHost() ? "Host" : "Client")
                .append(": ")
                .append(launchConfig.endpointLabel())
                .append('\n');
        builder.append("Phase: ").append(state.phase().label()).append('\n');
        builder.append("Rolle: ").append(localPlayer == null ? "Verbinde..." : localPlayer.role().label());

        if (state.phase() == SessionPhase.RUNNING || state.phase() == SessionPhase.RESULT) {
            builder.append('\n').append("Verbleibende Schueler: ").append(state.remainingStudents());
            builder.append('\n').append("Timer: ").append(formatTime(state.remainingTimeSeconds()));
            builder.append('\n').append("Power-ups aktiv: ").append(state.powerUps().size());
        } else if (state.phase() == SessionPhase.LOBBY) {
            builder.append('\n').append("Spieler in Lobby: ").append(state.players().size());
        }

        if (localPlayer != null && localPlayer.captured()) {
            builder.append('\n').append("Status: Gefangen");
        }
        if (localPlayer != null && localPlayer.effectMask() != 0) {
            builder.append('\n').append("Effekte: ").append(formatEffects(localPlayer.effectMask()));
        }
        if (state.phase() == SessionPhase.DISCONNECTED && state.disconnectReason() != null) {
            builder.append('\n').append(state.disconnectReason());
        }
        return builder.toString();
    }

    private String buildPlayerListText(ClientGameState state, int localPlayerId) {
        StringBuilder builder = new StringBuilder("Spielerliste");
        for (ClientGameState.ClientPlayerState player : state.players()) {
            builder.append('\n').append(player.playerName());
            if (player.playerId() == localPlayerId) {
                builder.append(" [Du]");
            }
            if (player.host()) {
                builder.append(" [Host]");
            }

            String roleLabel = switch (state.phase()) {
                case LOBBY, CONNECTING -> "wartet";
                case RUNNING, RESULT -> player.role().label();
                case DISCONNECTED -> "offline";
            };
            builder.append(" - ").append(roleLabel);

            if (player.captured()) {
                builder.append(" (gefangen)");
            }
        }
        return builder.toString();
    }

    private String buildInstructionText(
            ClientGameState state,
            ClientGameState.ClientPlayerState localPlayer,
            boolean localHostProcess,
            boolean mouseCaptured,
            String cameraPerspectiveLabel
    ) {
        String mouseHint = mouseCaptured ? "Esc loest Maus" : "Linksklick captured Maus";
        String skinHint = "F6/6 wechselt Skin";
        String cameraHint = "F3/3 Kamera: " + cameraPerspectiveLabel;

        return switch (state.phase()) {
            case CONNECTING -> "Verbinde mit dem Server...";
            case LOBBY -> state.hostId() == (localPlayer == null ? -1 : localPlayer.playerId())
                    ? "Enter startet die Runde | " + cameraHint + " | " + skinHint + " | Maus dreht | " + mouseHint
                    : "Warte auf den Host | " + cameraHint + " | " + skinHint + " | Maus dreht | " + mouseHint;
            case RUNNING -> buildRunningInstructions(localPlayer, mouseHint, skinHint, cameraHint);
            case RESULT -> "Naechste Lobby in wenigen Sekunden | " + cameraHint + " | " + skinHint + " | Maus dreht | " + mouseHint;
            case DISCONNECTED -> localHostProcess
                    ? "Starte das Spiel neu, um wieder eine Lobby zu hosten."
                    : "Starte den Client neu, um erneut beizutreten.";
        };
    }

    private String buildRunningInstructions(
            ClientGameState.ClientPlayerState localPlayer,
            String mouseHint,
            String skinHint,
            String cameraHint
    ) {
        if (localPlayer == null) {
            return "WASD/Pfeile bewegen | Shift sprintet | " + cameraHint + " | " + skinHint + " | " + mouseHint;
        }
        if (localPlayer.role() == AvatarRole.SHRUECK) {
            return "WASD/Pfeile bewegen | Shift sprintet | Fange alle Schueler | Meide Power-ups | " + cameraHint + " | " + mouseHint;
        }
        if (localPlayer.captured()) {
            return "Du wurdest gefangen. Beobachte den Rest der Runde | " + cameraHint + " | " + mouseHint;
        }
        return "WASD/Pfeile bewegen | Shift sprintet | Sammle Power-ups | " + cameraHint + " | " + skinHint + " | Ueberlebe bis der Timer endet | " + mouseHint;
    }

    private String buildBannerText(ClientGameState state, ClientGameState.ClientPlayerState localPlayer) {
        if (state.phase() == SessionPhase.CONNECTING) {
            return "Verbinde mit Lobby...";
        }
        if (state.phase() == SessionPhase.DISCONNECTED) {
            return "Verbindung beendet";
        }
        if (state.phase() != SessionPhase.RESULT) {
            return "";
        }

        boolean localWon = didLocalPlayerWin(localPlayer, state.winner());
        String title = localWon ? "Sieg!" : "Niederlage!";
        String detail = state.winner() == RoundWinner.SHRUECK
                ? "Shrueck hat alle Schueler gefangen."
                : "Mindestens ein Schueler hat bis zum Ende ueberlebt.";
        return title + "\n" + detail;
    }

    private boolean didLocalPlayerWin(ClientGameState.ClientPlayerState localPlayer, RoundWinner winner) {
        if (localPlayer == null) {
            return false;
        }
        return winner == RoundWinner.SHRUECK && localPlayer.role() == AvatarRole.SHRUECK
                || winner == RoundWinner.STUDENTS && localPlayer.role() == AvatarRole.STUDENT && !localPlayer.captured();
    }

    private String formatTime(float remainingTimeSeconds) {
        int totalSeconds = Math.max(0, Math.round(remainingTimeSeconds));
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void refreshLayout() {
        int statusLines = countLines(statusText.getText());
        statusText.setLocalTranslation(20f, camera.getHeight() - 20f, 0f);
        instructionText.setLocalTranslation(
                20f,
                camera.getHeight() - 20f - statusText.getLineHeight() * (statusLines + 0.9f),
                0f
        );
        playerListText.setLocalTranslation(
                camera.getWidth() - playerListText.getLineWidth() - 24f,
                camera.getHeight() - 20f,
                0f
        );
        bannerText.setLocalTranslation(
                (camera.getWidth() - bannerText.getLineWidth()) * 0.5f,
                camera.getHeight() * 0.62f,
                0f
        );
        noticeText.setLocalTranslation(
                (camera.getWidth() - noticeText.getLineWidth()) * 0.5f,
                camera.getHeight() * 0.14f,
                0f
        );
    }

    private String formatEffects(int effectMask) {
        StringBuilder builder = new StringBuilder();
        for (PowerUpType type : PowerUpType.values()) {
            if ((effectMask & type.effectMask()) == 0) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(type.label());
        }
        return builder.isEmpty() ? "-" : builder.toString();
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        int lines = 1;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}
