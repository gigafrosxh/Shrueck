package at.shrueck.net.game.world;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SchoolLayout {

    private final float minX;
    private final float maxX;
    private final float minZ;
    private final float maxZ;
    private final List<Obstacle> obstacles;
    private final Vector3f playerSpawn;
    private final List<Vector3f> studentSpawns;
    private final List<Vector3f> patrolPoints;
    private final List<Vector3f> lobbySpawns;

    public SchoolLayout(
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            List<Obstacle> obstacles,
            Vector3f playerSpawn,
            List<Vector3f> studentSpawns,
            List<Vector3f> patrolPoints,
            List<Vector3f> lobbySpawns
    ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.obstacles = List.copyOf(obstacles);
        this.playerSpawn = playerSpawn.clone();
        this.studentSpawns = copyPoints(studentSpawns);
        this.patrolPoints = copyPoints(patrolPoints);
        this.lobbySpawns = copyPoints(lobbySpawns);
    }

    public Vector3f playerSpawn() {
        return playerSpawn.clone();
    }

    public List<Vector3f> studentSpawns() {
        return copyPoints(studentSpawns);
    }

    public List<Vector3f> lobbySpawns() {
        return copyPoints(lobbySpawns);
    }

    public Vector3f randomPatrolPoint(Random random) {
        return patrolPoints.get(random.nextInt(patrolPoints.size())).clone();
    }

    public Vector3f resolveMovement(Vector3f currentPosition, Vector3f delta, float radius) {
        Vector3f fullMove = clamp(currentPosition.add(delta), radius);
        if (isWalkable(fullMove, radius)) {
            return fullMove;
        }

        Vector3f slideX = clamp(new Vector3f(currentPosition.x + delta.x, 0f, currentPosition.z), radius);
        if (!isWalkable(slideX, radius)) {
            slideX = new Vector3f(currentPosition.x, 0f, currentPosition.z);
        }

        Vector3f slideZ = clamp(new Vector3f(slideX.x, 0f, currentPosition.z + delta.z), radius);
        if (!isWalkable(slideZ, radius)) {
            return new Vector3f(slideX.x, 0f, slideX.z);
        }

        return slideZ;
    }

    public Vector3f constrainCamera(Vector3f target, Vector3f desiredLocation, float wallMargin) {
        Vector3f delta = desiredLocation.subtract(target);
        float maxTravel = 1f;

        if (Math.abs(delta.x) > FastMath.ZERO_TOLERANCE) {
            if (delta.x > 0f) {
                maxTravel = Math.min(maxTravel, (maxX - wallMargin - target.x) / delta.x);
            } else {
                maxTravel = Math.min(maxTravel, (minX + wallMargin - target.x) / delta.x);
            }
        }

        if (Math.abs(delta.z) > FastMath.ZERO_TOLERANCE) {
            if (delta.z > 0f) {
                maxTravel = Math.min(maxTravel, (maxZ - wallMargin - target.z) / delta.z);
            } else {
                maxTravel = Math.min(maxTravel, (minZ + wallMargin - target.z) / delta.z);
            }
        }

        maxTravel = FastMath.clamp(maxTravel, 0f, 1f);
        return target.add(delta.mult(maxTravel));
    }

    private boolean isWalkable(Vector3f position, float radius) {
        if (position.x < minX + radius || position.x > maxX - radius) {
            return false;
        }
        if (position.z < minZ + radius || position.z > maxZ - radius) {
            return false;
        }
        for (Obstacle obstacle : obstacles) {
            if (obstacle.intersectsCircle(position, radius)) {
                return false;
            }
        }
        return true;
    }

    private Vector3f clamp(Vector3f position, float radius) {
        return new Vector3f(
                FastMath.clamp(position.x, minX + radius, maxX - radius),
                0f,
                FastMath.clamp(position.z, minZ + radius, maxZ - radius)
        );
    }

    private static List<Vector3f> copyPoints(List<Vector3f> points) {
        List<Vector3f> copies = new ArrayList<>(points.size());
        for (Vector3f point : points) {
            copies.add(point.clone());
        }
        return copies;
    }

    public record Obstacle(float centerX, float centerZ, float halfX, float halfZ) {

        public boolean intersectsCircle(Vector3f point, float radius) {
            float closestX = FastMath.clamp(point.x, centerX - halfX, centerX + halfX);
            float closestZ = FastMath.clamp(point.z, centerZ - halfZ, centerZ + halfZ);
            float dx = point.x - closestX;
            float dz = point.z - closestZ;
            return dx * dx + dz * dz < radius * radius;
        }
    }
}