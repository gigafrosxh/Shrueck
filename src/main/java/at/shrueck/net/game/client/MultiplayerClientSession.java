package at.shrueck.net.game.client;

import at.shrueck.net.game.character.CharacterMode;
import at.shrueck.net.game.net.NetworkProtocol;
import at.shrueck.net.game.net.NetworkProtocol.JoinAcceptedMessage;
import at.shrueck.net.game.net.NetworkProtocol.JoinLobbyRequestMessage;
import at.shrueck.net.game.net.NetworkProtocol.JoinRejectedMessage;
import at.shrueck.net.game.net.NetworkProtocol.PlayerInputMessage;
import at.shrueck.net.game.net.NetworkProtocol.PlayerStateSnapshot;
import at.shrueck.net.game.net.NetworkProtocol.ServerNoticeMessage;
import at.shrueck.net.game.net.NetworkProtocol.StartRoundRequestMessage;
import at.shrueck.net.game.net.NetworkProtocol.StateSyncMessage;
import at.shrueck.net.game.net.NetworkProtocol.UpdateSkinSelectionMessage;
import at.shrueck.net.game.shared.AvatarRole;
import at.shrueck.net.game.shared.GameConstants;
import at.shrueck.net.game.shared.PlayerInputState;
import at.shrueck.net.game.shared.RoundWinner;
import at.shrueck.net.game.shared.SessionPhase;
import at.shrueck.net.game.shared.StudentSkin;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class MultiplayerClientSession implements MessageListener<Client>, ClientStateListener, AutoCloseable {

    private final Client client;
    private final LaunchConfig launchConfig;
    private final AtomicReference<ClientGameState> latestState = new AtomicReference<>(ClientGameState.connecting());
    private final AtomicReference<String> pendingNotice = new AtomicReference<>();

    private volatile int playerId = -1;

    public MultiplayerClientSession(LaunchConfig launchConfig) throws IOException {
        this.launchConfig = launchConfig;
        NetworkProtocol.registerAll();
        this.client = connectWithRetry(launchConfig);
        client.addClientStateListener(this);
        client.addMessageListener(
                this,
                JoinAcceptedMessage.class,
                JoinRejectedMessage.class,
                StateSyncMessage.class,
                ServerNoticeMessage.class
        );
    }

    public void start() {
        client.start();
    }

    public int playerId() {
        return playerId;
    }

    public ClientGameState currentState() {
        return latestState.get();
    }

    public String pollNotice() {
        return pendingNotice.getAndSet(null);
    }

    public void sendInput(PlayerInputState input) {
        if (input == null || !client.isConnected()) {
            return;
        }
        client.send(new PlayerInputMessage(
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.sprint(),
                input.cameraYaw()
        ));
    }

    public void sendSkinSelection(StudentSkin studentSkin) {
        if (studentSkin == null || !client.isConnected()) {
            return;
        }
        client.send(new UpdateSkinSelectionMessage(studentSkin.code()));
    }

    public void requestRoundStart() {
        if (client.isConnected()) {
            client.send(new StartRoundRequestMessage());
        }
    }

    @Override
    public void clientConnected(Client source) {
        source.send(new JoinLobbyRequestMessage(launchConfig.playerName()));
    }

    @Override
    public void clientDisconnected(Client source, DisconnectInfo info) {
        latestState.set(ClientGameState.disconnected("Verbindung zum Server beendet."));
    }

    @Override
    public void messageReceived(Client source, Message message) {
        if (message instanceof JoinAcceptedMessage accepted) {
            playerId = accepted.playerId;
            pendingNotice.set("Du bist der Lobby beigetreten.");
            return;
        }

        if (message instanceof JoinRejectedMessage rejected) {
            pendingNotice.set(rejected.reason);
            latestState.set(ClientGameState.disconnected(rejected.reason));
            close();
            return;
        }

        if (message instanceof ServerNoticeMessage notice) {
            pendingNotice.set(notice.text);
            return;
        }

        if (message instanceof StateSyncMessage stateSyncMessage) {
            latestState.set(convertState(stateSyncMessage));
        }
    }

    @Override
    public void close() {
        if (client.isConnected()) {
            client.close();
        }
    }

    private ClientGameState convertState(StateSyncMessage message) {
        List<ClientGameState.ClientPlayerState> players = new ArrayList<>();
        if (message.players != null) {
            for (PlayerStateSnapshot player : message.players) {
                players.add(new ClientGameState.ClientPlayerState(
                        player.playerId,
                        player.playerName,
                        player.host,
                        AvatarRole.fromCode(player.roleCode),
                        player.captured,
                        player.x,
                        player.z,
                        player.yaw,
                        decodeMode(player.modeCode),
                        StudentSkin.fromCode(player.skinCode)
                ));
            }
        }

        return new ClientGameState(
                SessionPhase.fromCode(message.phaseCode),
                message.hostId,
                message.shrueckId,
                RoundWinner.fromCode(message.winnerCode),
                message.remainingTimeSeconds,
                message.remainingStudents,
                players,
                null
        );
    }

    private CharacterMode decodeMode(int modeCode) {
        CharacterMode[] modes = CharacterMode.values();
        if (modeCode < 0 || modeCode >= modes.length) {
            return CharacterMode.IDLE;
        }
        return modes[modeCode];
    }

    private static Client connectWithRetry(LaunchConfig launchConfig) throws IOException {
        IOException lastFailure = null;
        int attempts = launchConfig.isHost() || launchConfig.isLoopbackConnection() ? 15 : 4;
        long retryDelayMillis = launchConfig.isHost() || launchConfig.isLoopbackConnection() ? 200L : 350L;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return Network.connectToServer(
                        GameConstants.NETWORK_GAME_NAME,
                        GameConstants.NETWORK_VERSION,
                        launchConfig.connectHost(),
                        launchConfig.port(),
                        launchConfig.port()
                );
            } catch (IOException exception) {
                lastFailure = exception;
                if (attempt == attempts) {
                    break;
                }
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    IOException aborted = new IOException("Verbindungsaufbau unterbrochen.", interruptedException);
                    aborted.addSuppressed(exception);
                    throw aborted;
                }
            }
        }

        String message = "Verbindung zu " + launchConfig.connectHost() + ":" + launchConfig.port() + " fehlgeschlagen.";
        if (lastFailure != null && lastFailure.getMessage() != null && !lastFailure.getMessage().isBlank()) {
            message += " " + lastFailure.getMessage();
        }
        throw new IOException(message, lastFailure);
    }
}