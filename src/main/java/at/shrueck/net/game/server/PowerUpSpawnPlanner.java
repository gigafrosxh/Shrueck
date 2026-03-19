package at.shrueck.net.game.server;

import at.shrueck.net.game.shared.GameConstants;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

final class PowerUpSpawnPlanner {

    private PowerUpSpawnPlanner() {
    }

    static Vector3f chooseSpawnPoint(
            List<Vector3f> candidatePoints,
            Collection<Vector3f> activePowerUpPositions,
            Collection<Vector3f> playerPositions,
            Random random
    ) {
        List<Vector3f> safeCandidates = new ArrayList<>();
        List<Vector3f> fallbackCandidates = new ArrayList<>();

        for (Vector3f candidate : candidatePoints) {
            if (isTooClose(candidate, activePowerUpPositions, GameConstants.POWER_UP_MIN_SPACING)) {
                continue;
            }
            fallbackCandidates.add(candidate);
            if (!isTooClose(candidate, playerPositions, GameConstants.POWER_UP_PLAYER_CLEARANCE)) {
                safeCandidates.add(candidate);
            }
        }

        List<Vector3f> resolvedPool = safeCandidates.isEmpty() ? fallbackCandidates : safeCandidates;
        if (resolvedPool.isEmpty()) {
            return null;
        }

        return resolvedPool.get(random.nextInt(resolvedPool.size())).clone();
    }

    private static boolean isTooClose(Vector3f candidate, Collection<Vector3f> positions, float minDistance) {
        float minDistanceSquared = minDistance * minDistance;
        for (Vector3f position : positions) {
            float dx = candidate.x - position.x;
            float dz = candidate.z - position.z;
            if (dx * dx + dz * dz < minDistanceSquared) {
                return true;
            }
        }
        return false;
    }
}
