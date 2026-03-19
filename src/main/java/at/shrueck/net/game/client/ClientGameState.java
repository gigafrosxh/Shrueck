package at.shrueck.net.game.client;

import at.shrueck.net.game.character.CharacterMode;
import at.shrueck.net.game.shared.AvatarRole;
import at.shrueck.net.game.shared.PowerUpType;
import at.shrueck.net.game.shared.RoundWinner;
import at.shrueck.net.game.shared.SessionPhase;
import at.shrueck.net.game.shared.StudentSkin;
import java.util.List;

public record ClientGameState(
        SessionPhase phase,
        int hostId,
        int shrueckId,
        RoundWinner winner,
        float remainingTimeSeconds,
        int remainingStudents,
        List<ClientPlayerState> players,
        List<PowerUpState> powerUps,
        String disconnectReason
) {

    public ClientGameState {
        phase = phase == null ? SessionPhase.CONNECTING : phase;
        winner = winner == null ? RoundWinner.NONE : winner;
        players = List.copyOf(players == null ? List.of() : players);
        powerUps = List.copyOf(powerUps == null ? List.of() : powerUps);
    }

    public static ClientGameState connecting() {
        return new ClientGameState(SessionPhase.CONNECTING, -1, -1, RoundWinner.NONE, 0f, 0, List.of(), List.of(), null);
    }

    public static ClientGameState disconnected(String reason) {
        return new ClientGameState(SessionPhase.DISCONNECTED, -1, -1, RoundWinner.NONE, 0f, 0, List.of(), List.of(), reason);
    }

    public ClientPlayerState findPlayer(int playerId) {
        for (ClientPlayerState player : players) {
            if (player.playerId() == playerId) {
                return player;
            }
        }
        return null;
    }

    public record ClientPlayerState(
            int playerId,
            String playerName,
            boolean host,
            AvatarRole role,
            boolean captured,
            float x,
            float z,
            float yaw,
            CharacterMode mode,
            StudentSkin studentSkin,
            float visualScale,
            int effectMask
    ) {
    }

    public record PowerUpState(
            int powerUpId,
            PowerUpType type,
            float x,
            float z
    ) {
    }
}
