package at.shrueck.net.game.net;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;

public final class NetworkProtocol {

    private static boolean registered;

    private NetworkProtocol() {
    }

    public static synchronized void registerAll() {
        if (registered) {
            return;
        }

        Serializer.registerClass(JoinLobbyRequestMessage.class);
        Serializer.registerClass(JoinAcceptedMessage.class);
        Serializer.registerClass(JoinRejectedMessage.class);
        Serializer.registerClass(PlayerInputMessage.class);
        Serializer.registerClass(StartRoundRequestMessage.class);
        Serializer.registerClass(ServerNoticeMessage.class);
        Serializer.registerClass(StateSyncMessage.class);
        Serializer.registerClass(PlayerStateSnapshot.class);

        registered = true;
    }

    @Serializable
    public static class JoinLobbyRequestMessage extends AbstractMessage {

        public String playerName;

        public JoinLobbyRequestMessage() {
        }

        public JoinLobbyRequestMessage(String playerName) {
            this.playerName = playerName;
        }
    }

    @Serializable
    public static class JoinAcceptedMessage extends AbstractMessage {

        public int playerId;

        public JoinAcceptedMessage() {
        }

        public JoinAcceptedMessage(int playerId) {
            this.playerId = playerId;
        }
    }

    @Serializable
    public static class JoinRejectedMessage extends AbstractMessage {

        public String reason;

        public JoinRejectedMessage() {
        }

        public JoinRejectedMessage(String reason) {
            this.reason = reason;
        }
    }

    @Serializable
    public static class PlayerInputMessage extends AbstractMessage {

        public boolean forward;
        public boolean backward;
        public boolean left;
        public boolean right;
        public boolean sprint;
        public float cameraYaw;

        public PlayerInputMessage() {
        }

        public PlayerInputMessage(boolean forward, boolean backward, boolean left, boolean right, boolean sprint, float cameraYaw) {
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
            this.sprint = sprint;
            this.cameraYaw = cameraYaw;
        }
    }

    @Serializable
    public static class StartRoundRequestMessage extends AbstractMessage {

        public StartRoundRequestMessage() {
        }
    }

    @Serializable
    public static class ServerNoticeMessage extends AbstractMessage {

        public String text;

        public ServerNoticeMessage() {
        }

        public ServerNoticeMessage(String text) {
            this.text = text;
        }
    }

    @Serializable
    public static class StateSyncMessage extends AbstractMessage {

        public int phaseCode;
        public int hostId;
        public int shrueckId;
        public int winnerCode;
        public float remainingTimeSeconds;
        public int remainingStudents;
        public PlayerStateSnapshot[] players;

        public StateSyncMessage() {
        }

        public StateSyncMessage(
                int phaseCode,
                int hostId,
                int shrueckId,
                int winnerCode,
                float remainingTimeSeconds,
                int remainingStudents,
                PlayerStateSnapshot[] players
        ) {
            this.phaseCode = phaseCode;
            this.hostId = hostId;
            this.shrueckId = shrueckId;
            this.winnerCode = winnerCode;
            this.remainingTimeSeconds = remainingTimeSeconds;
            this.remainingStudents = remainingStudents;
            this.players = players;
        }
    }

    @Serializable
    public static class PlayerStateSnapshot {

        public int playerId;
        public String playerName;
        public boolean host;
        public int roleCode;
        public boolean captured;
        public float x;
        public float z;
        public float yaw;
        public int modeCode;

        public PlayerStateSnapshot() {
        }

        public PlayerStateSnapshot(
                int playerId,
                String playerName,
                boolean host,
                int roleCode,
                boolean captured,
                float x,
                float z,
                float yaw,
                int modeCode
        ) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.host = host;
            this.roleCode = roleCode;
            this.captured = captured;
            this.x = x;
            this.z = z;
            this.yaw = yaw;
            this.modeCode = modeCode;
        }
    }
}