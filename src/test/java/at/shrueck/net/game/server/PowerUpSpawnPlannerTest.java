package at.shrueck.net.game.server;

import com.jme3.math.Vector3f;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerUpSpawnPlannerTest {

    @Test
    void prefersSafePointsAwayFromPlayersAndExistingPowerUps() {
        Vector3f spawn = PowerUpSpawnPlanner.chooseSpawnPoint(
                List.of(point(0f, 0f), point(5f, 0f), point(12f, 0f)),
                List.of(point(5f, 0f)),
                List.of(point(0f, 0f)),
                new Random(7L)
        );

        assertNotNull(spawn);
        assertEquals(12f, spawn.x, 0.0001f);
        assertEquals(0f, spawn.z, 0.0001f);
    }

    @Test
    void fallsBackToAnyFreePointIfEveryPointIsNearPlayers() {
        Vector3f spawn = PowerUpSpawnPlanner.chooseSpawnPoint(
                List.of(point(0f, 0f), point(8f, 0f)),
                List.of(),
                List.of(point(1f, 0f), point(7f, 0f)),
                new Random(3L)
        );

        assertNotNull(spawn);
        assertTrue(spawn.x == 0f || spawn.x == 8f);
    }

    @Test
    void returnsNullIfNoSpawnPointRemainsFree() {
        Vector3f spawn = PowerUpSpawnPlanner.chooseSpawnPoint(
                List.of(point(0f, 0f)),
                List.of(point(0.5f, 0f)),
                List.of(),
                new Random(1L)
        );

        assertNull(spawn);
    }

    private Vector3f point(float x, float z) {
        return new Vector3f(x, 0f, z);
    }
}
