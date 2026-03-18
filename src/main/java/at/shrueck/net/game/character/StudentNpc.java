package at.shrueck.net.game.character;

import at.shrueck.net.game.world.SchoolLayout;
import com.jme3.math.Vector3f;
import java.util.Random;

public final class StudentNpc {

    private static final float NPC_RADIUS = 0.95f;
    private static final float WANDER_SPEED = 3f;
    private static final float PANIC_SPEED = 5.5f;
    private static final float ALERT_DISTANCE = 10f;

    private final AnimatedActor actor;
    private final boolean specialIdle;
    private final Random random;

    private Vector3f targetPoint;
    private float waitTimer;
    private boolean captured;

    public StudentNpc(AnimatedActor actor, boolean specialIdle, long seed) {
        this.actor = actor;
        this.specialIdle = specialIdle;
        this.random = new Random(seed);
    }

    public void update(float tpf, Vector3f playerPosition, SchoolLayout schoolLayout) {
        if (captured) {
            return;
        }

        Vector3f currentPosition = actor.getLocalTranslation().clone();
        float distanceToPlayer = flatDistance(currentPosition, playerPosition);

        if (distanceToPlayer <= ALERT_DISTANCE) {
            fleeFromPlayer(tpf, currentPosition, playerPosition, schoolLayout);
            return;
        }

        if (waitTimer > 0f) {
            waitTimer -= tpf;
            actor.setMode(specialIdle ? CharacterMode.SPECIAL : CharacterMode.MOVE);
            return;
        }

        if (targetPoint == null || flatDistance(currentPosition, targetPoint) < 1.2f) {
            targetPoint = schoolLayout.randomPatrolPoint(random);
            waitTimer = specialIdle ? 1.2f + random.nextFloat() * 1.6f : 0.35f + random.nextFloat() * 0.8f;
            actor.setMode(specialIdle ? CharacterMode.SPECIAL : CharacterMode.MOVE);
            return;
        }

        Vector3f toTarget = targetPoint.subtract(currentPosition);
        toTarget.y = 0f;
        Vector3f direction = toTarget.normalize();
        move(tpf, currentPosition, direction, WANDER_SPEED, schoolLayout, CharacterMode.MOVE);
    }

    public void capture() {
        captured = true;
        actor.removeFromParent();
    }

    public boolean isCaptured() {
        return captured;
    }

    public Vector3f position() {
        return actor.getLocalTranslation();
    }

    private void fleeFromPlayer(float tpf, Vector3f currentPosition, Vector3f playerPosition, SchoolLayout schoolLayout) {
        Vector3f awayFromPlayer = currentPosition.subtract(playerPosition);
        awayFromPlayer.y = 0f;
        if (awayFromPlayer.lengthSquared() < 0.0001f) {
            awayFromPlayer.set(random.nextFloat() - 0.5f, 0f, random.nextFloat() - 0.5f);
        }
        Vector3f direction = awayFromPlayer.normalize();
        move(tpf, currentPosition, direction, PANIC_SPEED, schoolLayout, CharacterMode.FAST);
        waitTimer = 0f;
    }

    private void move(
            float tpf,
            Vector3f currentPosition,
            Vector3f direction,
            float speed,
            SchoolLayout schoolLayout,
            CharacterMode mode
    ) {
        Vector3f delta = direction.mult(speed * tpf);
        Vector3f nextPosition = schoolLayout.resolveMovement(currentPosition, delta, NPC_RADIUS);
        Vector3f actualMovement = nextPosition.subtract(currentPosition);

        if (actualMovement.lengthSquared() < 0.0001f) {
            targetPoint = null;
            return;
        }

        actor.setLocalTranslation(nextPosition);
        actor.face(actualMovement);
        actor.setMode(mode);
    }

    private static float flatDistance(Vector3f first, Vector3f second) {
        float dx = first.x - second.x;
        float dz = first.z - second.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
