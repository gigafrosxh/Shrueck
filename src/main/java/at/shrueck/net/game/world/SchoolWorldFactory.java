package at.shrueck.net.game.world;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;

public final class SchoolWorldFactory {

    private SchoolWorldFactory() {
    }

    public static SchoolLayout createDefaultLayout() {
        return new SchoolLayout(
                -32f,
                32f,
                -28f,
                28f,
                createObstacles(),
                new Vector3f(0f, 0f, 25f),
                createStudentSpawns(),
                createPatrolPoints(),
                createLobbySpawns()
        );
    }

    private static List<SchoolLayout.Obstacle> createObstacles() {
        List<SchoolLayout.Obstacle> obstacles = new ArrayList<>();

        addObstacle(obstacles, 0f, 19f, 5.5f, 1.4f);
        addObstacle(obstacles, -18f, 22f, 2.6f, 0.9f);
        addObstacle(obstacles, 18f, 22f, 2.6f, 0.9f);

        addObstacle(obstacles, -10.5f, -14f, 1.1f, 5f);
        addObstacle(obstacles, -10.5f, 14f, 1.1f, 5f);
        addObstacle(obstacles, 10.5f, -14f, 1.1f, 5f);
        addObstacle(obstacles, 10.5f, 14f, 1.1f, 5f);

        addObstacle(obstacles, -18f, 0f, 2.2f, 1.2f);
        addObstacle(obstacles, 18f, 0f, 2.2f, 1.2f);

        addClassroomClusterObstacles(obstacles, -19f, -16f);
        addClassroomClusterObstacles(obstacles, 19f, -16f);
        addClassroomClusterObstacles(obstacles, -19f, 16f);
        addClassroomClusterObstacles(obstacles, 19f, 16f);

        addObstacle(obstacles, -8f, -22f, 2.2f, 1.2f);
        addObstacle(obstacles, 8f, -22f, 2.2f, 1.2f);
        addObstacle(obstacles, 0f, -8f, 2f, 2f);

        return obstacles;
    }

    private static List<Vector3f> createStudentSpawns() {
        return List.of(
                new Vector3f(-24f, 0f, -22f),
                new Vector3f(-24f, 0f, -8f),
                new Vector3f(-24f, 0f, 8f),
                new Vector3f(24f, 0f, -22f),
                new Vector3f(24f, 0f, -8f),
                new Vector3f(24f, 0f, 8f),
                new Vector3f(-6f, 0f, 0f),
                new Vector3f(6f, 0f, 0f)
        );
    }

    private static List<Vector3f> createPatrolPoints() {
        return List.of(
                new Vector3f(-24f, 0f, -18f),
                new Vector3f(-16f, 0f, -20f),
                new Vector3f(16f, 0f, -20f),
                new Vector3f(24f, 0f, -18f),
                new Vector3f(-24f, 0f, 18f),
                new Vector3f(-16f, 0f, 20f),
                new Vector3f(16f, 0f, 20f),
                new Vector3f(24f, 0f, 18f),
                new Vector3f(-24f, 0f, 0f),
                new Vector3f(24f, 0f, 0f),
                new Vector3f(0f, 0f, -22f),
                new Vector3f(0f, 0f, 22f),
                new Vector3f(-6f, 0f, -4f),
                new Vector3f(6f, 0f, 4f),
                new Vector3f(0f, 0f, 0f)
        );
    }

    private static List<Vector3f> createLobbySpawns() {
        return List.of(
                new Vector3f(-12f, 0f, 24f),
                new Vector3f(-8f, 0f, 24f),
                new Vector3f(-4f, 0f, 24f),
                new Vector3f(0f, 0f, 24f),
                new Vector3f(4f, 0f, 24f),
                new Vector3f(8f, 0f, 24f),
                new Vector3f(12f, 0f, 24f),
                new Vector3f(-10f, 0f, 21f),
                new Vector3f(-6f, 0f, 21f),
                new Vector3f(-2f, 0f, 21f),
                new Vector3f(2f, 0f, 21f),
                new Vector3f(6f, 0f, 21f),
                new Vector3f(10f, 0f, 21f)
        );
    }

    private static void addClassroomClusterObstacles(List<SchoolLayout.Obstacle> obstacles, float centerX, float centerZ) {
        addObstacle(obstacles, centerX - 3.2f, centerZ - 2.5f, 1.5f, 0.95f);
        addObstacle(obstacles, centerX + 3.2f, centerZ - 2.5f, 1.5f, 0.95f);
        addObstacle(obstacles, centerX - 3.2f, centerZ + 1.2f, 1.5f, 0.95f);
        addObstacle(obstacles, centerX + 3.2f, centerZ + 1.2f, 1.5f, 0.95f);
        addObstacle(obstacles, centerX, centerZ + 4.2f, 3.2f, 1.1f);
    }

    private static void addObstacle(List<SchoolLayout.Obstacle> obstacles, float centerX, float centerZ, float halfX, float halfZ) {
        obstacles.add(new SchoolLayout.Obstacle(centerX, centerZ, halfX, halfZ));
    }
}