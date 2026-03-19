package at.shrueck.net.game.server;

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
import at.shrueck.net.game.shared.MovementMath;
import at.shrueck.net.game.shared.PlayerInputState;
import at.shrueck.net.game.shared.PowerUpType;
import at.shrueck.net.game.shared.RoundWinner;
import at.shrueck.net.game.shared.SessionPhase;
import at.shrueck.net.game.shared.StudentSkin;
import at.shrueck.net.game.world.SchoolLayout;
import at.shrueck.net.game.world.SchoolWorldFactory;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class LanGameServer implements MessageListener<HostedConnection>, ConnectionListener, AutoCloseable {

    private final SchoolLayout schoolLayout = SchoolWorldFactory.createDefaultLayout();
    private final Queue<Runnable> commands = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private final int port;
    private final ScheduledExecutorService executor;

    private final Map<Integer, ServerPlayer> players = new LinkedHashMap<>();
    private final Map<Integer, ActivePowerUp> activePowerUps = new LinkedHashMap<>();
    private final List<PendingPowerUpSpawn> pendingPowerUpSpawns = new ArrayList<>();

    private Server server;
    private SessionPhase phase = SessionPhase.LOBBY;
    private RoundWinner winner = RoundWinner.NONE;
    private int hostId = -1;
    private int shrueckId = -1;
    private int joinSequence;
    private float remainingTimeSeconds;
    private long roundDeadlineNanos;
    private long resultDeadlineNanos;
    private boolean snapshotDirty = true;
    private int nextPowerUpId;

    public LanGameServer(int port) {
        this.port = port;
        this.executor = Executors.newSingleThreadScheduledExecutor(new ServerThreadFactory());
    }

    public void start() throws IOException {
        NetworkProtocol.registerAll();
        server = Network.createServer(
                GameConstants.NETWORK_GAME_NAME,
                GameConstants.NETWORK_VERSION,
                port,
                -1
        );
        server.addConnectionListener(this);
        server.addMessageListener(
                this,
                JoinLobbyRequestMessage.class,
                PlayerInputMessage.class,
                UpdateSkinSelectionMessage.class,
                StartRoundRequestMessage.class
        );
        server.start();

        long tickMillis = Math.max(10L, Math.round(1000f / GameConstants.SERVER_TICK_RATE));
        executor.scheduleAtFixedRate(this::tickSafely, 0L, tickMillis, TimeUnit.MILLISECONDS);
    }

    public int port() {
        return port;
    }

    @Override
    public void messageReceived(HostedConnection source, Message message) {
        commands.add(() -> handleMessage(source, message));
    }

    @Override
    public void connectionAdded(Server source, HostedConnection connection) {
    }

    @Override
    public void connectionRemoved(Server source, HostedConnection connection) {
        commands.add(() -> handleDisconnect(connection));
    }

    @Override
    public void close() {
        executor.shutdownNow();
        if (server != null) {
            server.close();
        }
    }

    private void tickSafely() {
        try {
            tick(1f / GameConstants.SERVER_TICK_RATE);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void tick(float tpf) {
        drainCommands();
        long now = System.nanoTime();

        if (phase == SessionPhase.RUNNING) {
            remainingTimeSeconds = Math.max(0f, (roundDeadlineNanos - now) / 1_000_000_000f);
            simulatePlayers(tpf, now);
            applyPowerUpLogic(now);
            spawnReadyPowerUps(now);
            applyCatchLogic(now);
            if (phase == SessionPhase.RUNNING && remainingTimeSeconds <= 0f && remainingStudentsCount() > 0) {
                finishRound(RoundWinner.STUDENTS);
            }
            broadcastSnapshot();
            snapshotDirty = false;
            return;
        }

        if (phase == SessionPhase.RESULT && now >= resultDeadlineNanos) {
            returnToLobby();
        }

        if (snapshotDirty) {
            broadcastSnapshot();
            snapshotDirty = false;
        }
    }

    private void handleMessage(HostedConnection source, Message message) {
        if (message instanceof JoinLobbyRequestMessage joinLobbyRequestMessage) {
            handleJoinRequest(source, joinLobbyRequestMessage);
            return;
        }

        if (message instanceof PlayerInputMessage playerInputMessage) {
            handleInput(source, playerInputMessage);
            return;
        }

        if (message instanceof UpdateSkinSelectionMessage updateSkinSelectionMessage) {
            handleSkinSelection(source, updateSkinSelectionMessage);
            return;
        }

        if (message instanceof StartRoundRequestMessage) {
            handleStartRequest(source);
        }
    }

    private void handleJoinRequest(HostedConnection source, JoinLobbyRequestMessage message) {
        if (phase != SessionPhase.LOBBY) {
            source.send(new JoinRejectedMessage("Neue Spieler koennen nur waehrend der Lobby beitreten."));
            return;
        }
        if (players.containsKey(source.getId())) {
            return;
        }

        ServerPlayer player = new ServerPlayer(
                source,
                source.getId(),
                ++joinSequence,
                sanitizePlayerName(message.playerName, source.getId())
        );
        players.put(player.playerId, player);

        if (hostId < 0) {
            hostId = player.playerId;
        }

        placePlayersInLobby();
        source.send(new JoinAcceptedMessage(player.playerId));
        snapshotDirty = true;
    }

    private void handleInput(HostedConnection source, PlayerInputMessage message) {
        ServerPlayer player = players.get(source.getId());
        if (player == null) {
            return;
        }
        player.input = new PlayerInputState(
                message.forward,
                message.backward,
                message.left,
                message.right,
                message.sprint,
                message.cameraYaw
        );
    }

    private void handleSkinSelection(HostedConnection source, UpdateSkinSelectionMessage message) {
        ServerPlayer player = players.get(source.getId());
        if (player == null) {
            return;
        }

        StudentSkin requestedSkin = StudentSkin.fromCode(message.skinCode);
        if (player.studentSkin != requestedSkin) {
            player.studentSkin = requestedSkin;
            snapshotDirty = true;
        }
    }

    private void handleStartRequest(HostedConnection source) {
        if (source.getId() != hostId) {
            sendNotice(source, "Nur der Host kann die Runde starten.");
            return;
        }
        if (phase != SessionPhase.LOBBY) {
            sendNotice(source, "Die Lobby ist gerade nicht startbereit.");
            return;
        }
        if (players.size() < GameConstants.MIN_PLAYERS_TO_START) {
            sendNotice(source, "Mindestens 2 Spieler werden benoetigt.");
            return;
        }
        startRound();
    }

    private void handleDisconnect(HostedConnection connection) {
        ServerPlayer removedPlayer = players.remove(connection.getId());
        if (removedPlayer == null) {
            return;
        }

        if (removedPlayer.playerId == hostId) {
            reassignHost();
        }

        if (phase == SessionPhase.RUNNING) {
            if (removedPlayer.role == AvatarRole.SHRUECK) {
                finishRound(RoundWinner.STUDENTS);
            } else if (removedPlayer.role == AvatarRole.STUDENT && remainingStudentsCount() == 0) {
                finishRound(RoundWinner.SHRUECK);
            }
        } else if (phase == SessionPhase.LOBBY) {
            placePlayersInLobby();
        }

        if (players.isEmpty()) {
            hostId = -1;
            shrueckId = -1;
            winner = RoundWinner.NONE;
            phase = SessionPhase.LOBBY;
        }

        snapshotDirty = true;
    }

    private void startRound() {
        resetRoundEffectsAndPowerUps();
        winner = RoundWinner.NONE;
        phase = SessionPhase.RUNNING;
        remainingTimeSeconds = GameConstants.ROUND_DURATION_SECONDS;
        roundDeadlineNanos = System.nanoTime() + (long) (GameConstants.ROUND_DURATION_SECONDS * 1_000_000_000L);

        List<ServerPlayer> sortedPlayers = sortedPlayers();
        ServerPlayer shrueck = sortedPlayers.get(random.nextInt(sortedPlayers.size()));
        shrueckId = shrueck.playerId;

        List<Vector3f> studentSpawns = schoolLayout.studentSpawns();
        int studentSpawnIndex = 0;

        for (ServerPlayer player : sortedPlayers) {
            player.captured = false;
            player.input = PlayerInputState.idle(0f);
            player.effects.clear();

            if (player.playerId == shrueckId) {
                player.role = AvatarRole.SHRUECK;
                player.position.set(schoolLayout.playerSpawn());
                player.yaw = 0f;
                player.mode = CharacterMode.IDLE;
            } else {
                player.role = AvatarRole.STUDENT;
                Vector3f spawn = studentSpawns.get(studentSpawnIndex % studentSpawns.size());
                player.position.set(spawn);
                player.yaw = 0f;
                player.mode = CharacterMode.SPECIAL;
                studentSpawnIndex++;
            }
        }

        queueInitialPowerUpSpawns(System.nanoTime());
        spawnReadyPowerUps(System.nanoTime());

        snapshotDirty = true;
    }

    private void finishRound(RoundWinner winner) {
        if (phase != SessionPhase.RUNNING) {
            return;
        }

        this.winner = Objects.requireNonNull(winner);
        this.phase = SessionPhase.RESULT;
        this.resultDeadlineNanos = System.nanoTime() + (long) (GameConstants.RESULT_DURATION_SECONDS * 1_000_000_000L);
        activePowerUps.clear();
        pendingPowerUpSpawns.clear();

        for (ServerPlayer player : players.values()) {
            player.input = PlayerInputState.idle(player.input.cameraYaw());
            player.effects.clear();
            player.mode = MovementMath.resultMode(player.role, winner, player.captured);
        }

        snapshotDirty = true;
    }

    private void returnToLobby() {
        phase = SessionPhase.LOBBY;
        winner = RoundWinner.NONE;
        shrueckId = -1;
        remainingTimeSeconds = 0f;
        resetRoundEffectsAndPowerUps();
        placePlayersInLobby();
        snapshotDirty = true;
    }

    private void placePlayersInLobby() {
        List<Vector3f> lobbySpawns = schoolLayout.lobbySpawns();
        List<ServerPlayer> sortedPlayers = sortedPlayers();
        for (int index = 0; index < sortedPlayers.size(); index++) {
            ServerPlayer player = sortedPlayers.get(index);
            Vector3f spawn = lobbySpawns.get(index % lobbySpawns.size());
            player.position.set(spawn);
            player.yaw = FastMath.PI;
            player.role = AvatarRole.UNASSIGNED;
            player.captured = false;
            player.input = PlayerInputState.idle(0f);
            player.effects.clear();
            player.mode = CharacterMode.SPECIAL;
        }
    }

    private void reassignHost() {
        hostId = sortedPlayers().stream()
                .mapToInt(player -> player.playerId)
                .findFirst()
                .orElse(-1);
    }

    private void simulatePlayers(float tpf, long nowNanos) {
        for (ServerPlayer player : players.values()) {
            if (player.role == AvatarRole.UNASSIGNED) {
                continue;
            }
            if (player.role == AvatarRole.STUDENT && player.captured) {
                player.mode = CharacterMode.SPECIAL;
                continue;
            }
            if (player.effects.preventsMovement(player.role, nowNanos)) {
                player.input = PlayerInputState.idle(player.input.cameraYaw());
                player.mode = player.effects.resolveMode(player.role, CharacterMode.STUNNED, false, nowNanos);
                continue;
            }

            Vector3f currentPosition = player.position.clone();
            Vector3f delta = MovementMath.resolveMoveDelta(
                    player.input,
                    player.role,
                    tpf,
                    player.effects.speedMultiplier(player.role, nowNanos)
            );
            Vector3f nextPosition = schoolLayout.resolveMovement(currentPosition, delta, GameConstants.PLAYER_RADIUS);
            Vector3f actualMovement = nextPosition.subtract(currentPosition);
            player.position.set(nextPosition);

            if (actualMovement.lengthSquared() > 0.0001f) {
                player.yaw = MovementMath.yawFromDirection(actualMovement, player.yaw);
                player.mode = player.effects.resolveMode(player.role, MovementMath.moveMode(player.input.sprint()), true, nowNanos);
            } else {
                player.mode = player.effects.resolveMode(player.role, MovementMath.idleModeForRole(player.role), false, nowNanos);
            }
        }
    }

    private void applyPowerUpLogic(long nowNanos) {
        if (activePowerUps.isEmpty()) {
            return;
        }

        ServerPlayer shrueck = players.get(shrueckId);
        List<Integer> collectedPowerUps = new ArrayList<>();

        for (ActivePowerUp powerUp : activePowerUps.values()) {
            ServerPlayer collector = findCollector(powerUp);
            if (collector == null) {
                continue;
            }

            collectedPowerUps.add(powerUp.powerUpId);
            scheduleRespawn(nowNanos + (long) (GameConstants.POWER_UP_RESPAWN_SECONDS * 1_000_000_000L));
            applyPowerUpEffect(powerUp.type, collector, shrueck, nowNanos);
        }

        if (collectedPowerUps.isEmpty()) {
            return;
        }

        for (Integer powerUpId : collectedPowerUps) {
            activePowerUps.remove(powerUpId);
        }
        snapshotDirty = true;
    }

    private void applyCatchLogic(long nowNanos) {
        ServerPlayer shrueck = players.get(shrueckId);
        if (shrueck == null) {
            finishRound(RoundWinner.STUDENTS);
            return;
        }
        if (shrueck.effects.preventsMovement(shrueck.role, nowNanos)) {
            return;
        }

        boolean anyCapture = false;
        for (ServerPlayer player : players.values()) {
            if (player.role != AvatarRole.STUDENT || player.captured) {
                continue;
            }
            if (flatDistanceSquared(shrueck.position, player.position) <= GameConstants.CATCH_RADIUS * GameConstants.CATCH_RADIUS) {
                player.captured = true;
                player.input = PlayerInputState.idle(player.input.cameraYaw());
                player.mode = CharacterMode.SPECIAL;
                anyCapture = true;
            }
        }

        if (remainingStudentsCount() == 0) {
            finishRound(RoundWinner.SHRUECK);
        } else if (anyCapture) {
            snapshotDirty = true;
        }
    }

    private void spawnReadyPowerUps(long nowNanos) {
        if (phase != SessionPhase.RUNNING || pendingPowerUpSpawns.isEmpty()) {
            return;
        }

        int index = 0;
        while (index < pendingPowerUpSpawns.size() && activePowerUps.size() < GameConstants.POWER_UP_MAX_ACTIVE) {
            PendingPowerUpSpawn pendingSpawn = pendingPowerUpSpawns.get(index);
            if (pendingSpawn.readyAtNanos > nowNanos) {
                index++;
                continue;
            }

            Vector3f spawnPosition = PowerUpSpawnPlanner.chooseSpawnPoint(
                    schoolLayout.powerUpSpawns(),
                    activePowerUpPositions(),
                    playerPositions(),
                    random
            );
            if (spawnPosition == null) {
                index++;
                continue;
            }

            int powerUpId = ++nextPowerUpId;
            activePowerUps.put(powerUpId, new ActivePowerUp(powerUpId, PowerUpType.random(random), spawnPosition));
            pendingPowerUpSpawns.remove(index);
            snapshotDirty = true;
        }
    }

    private int remainingStudentsCount() {
        int remainingStudents = 0;
        for (ServerPlayer player : players.values()) {
            if (player.role == AvatarRole.STUDENT && !player.captured) {
                remainingStudents++;
            }
        }
        return remainingStudents;
    }

    private void broadcastSnapshot() {
        if (server == null || players.isEmpty()) {
            return;
        }
        long nowNanos = System.nanoTime();
        PlayerStateSnapshot[] snapshots = sortedPlayers().stream()
                .map(player -> toSnapshot(player, nowNanos))
                .toArray(PlayerStateSnapshot[]::new);
        NetworkProtocol.PowerUpSnapshot[] powerUps = activePowerUps.values().stream()
                .map(this::toPowerUpSnapshot)
                .toArray(NetworkProtocol.PowerUpSnapshot[]::new);
        StateSyncMessage snapshot = new StateSyncMessage(
                phase.code(),
                hostId,
                shrueckId,
                winner.code(),
                remainingTimeSeconds,
                remainingStudentsCount(),
                snapshots,
                powerUps
        );
        for (ServerPlayer player : players.values()) {
            player.connection.send(snapshot);
        }
    }

    private PlayerStateSnapshot toSnapshot(ServerPlayer player, long nowNanos) {
        return new PlayerStateSnapshot(
                player.playerId,
                player.playerName,
                player.playerId == hostId,
                player.role.code(),
                player.captured,
                player.position.x,
                player.position.z,
                player.yaw,
                player.mode.ordinal(),
                player.studentSkin.code(),
                player.effects.visualScaleMultiplier(player.role, nowNanos),
                player.effects.effectMask(player.role, nowNanos)
        );
    }

    private NetworkProtocol.PowerUpSnapshot toPowerUpSnapshot(ActivePowerUp powerUp) {
        return new NetworkProtocol.PowerUpSnapshot(
                powerUp.powerUpId,
                powerUp.type.code(),
                powerUp.position.x,
                powerUp.position.z
        );
    }

    private List<ServerPlayer> sortedPlayers() {
        return players.values().stream()
                .sorted(Comparator.comparingInt(player -> player.joinOrder))
                .toList();
    }

    private void sendNotice(HostedConnection connection, String text) {
        connection.send(new ServerNoticeMessage(text));
    }

    private void applyPowerUpEffect(PowerUpType type, ServerPlayer collector, ServerPlayer shrueck, long nowNanos) {
        switch (type) {
            case STUDENT_SPEED -> {
                collector.effects.apply(type, nowNanos);
                sendNotice(collector.connection, type.label() + ": 8s schneller.");
            }
            case STUDENT_GROWTH -> {
                collector.effects.apply(type, nowNanos);
                sendNotice(collector.connection, type.label() + ": 10s groesser.");
            }
            case SHRUECK_SLOW -> {
                if (shrueck != null) {
                    shrueck.effects.apply(type, nowNanos);
                    sendNotice(collector.connection, type.label() + ": Shrueck wird 6s verlangsamt.");
                    sendNotice(shrueck.connection, "Du wurdest 6s verlangsamt.");
                }
            }
            case SHRUECK_STUN -> {
                if (shrueck != null) {
                    shrueck.effects.apply(type, nowNanos);
                    sendNotice(collector.connection, type.label() + ": Shrueck ist 2.5s gestunnt.");
                    sendNotice(shrueck.connection, "Du bist kurz gestunnt.");
                }
            }
        }
    }

    private ServerPlayer findCollector(ActivePowerUp powerUp) {
        ServerPlayer bestCollector = null;
        float bestDistanceSquared = Float.MAX_VALUE;

        for (ServerPlayer player : players.values()) {
            if (player.role != AvatarRole.STUDENT || player.captured) {
                continue;
            }

            float distanceSquared = flatDistanceSquared(player.position, powerUp.position);
            if (distanceSquared > GameConstants.POWER_UP_PICKUP_RADIUS * GameConstants.POWER_UP_PICKUP_RADIUS) {
                continue;
            }
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestCollector = player;
            }
        }

        return bestCollector;
    }

    private List<Vector3f> activePowerUpPositions() {
        List<Vector3f> positions = new ArrayList<>(activePowerUps.size());
        for (ActivePowerUp powerUp : activePowerUps.values()) {
            positions.add(powerUp.position);
        }
        return positions;
    }

    private List<Vector3f> playerPositions() {
        List<Vector3f> positions = new ArrayList<>(players.size());
        for (ServerPlayer player : players.values()) {
            if (player.role != AvatarRole.UNASSIGNED && !player.captured) {
                positions.add(player.position);
            }
        }
        return positions;
    }

    private void queueInitialPowerUpSpawns(long nowNanos) {
        pendingPowerUpSpawns.clear();
        for (int index = 0; index < GameConstants.POWER_UP_MAX_ACTIVE; index++) {
            pendingPowerUpSpawns.add(new PendingPowerUpSpawn(nowNanos));
        }
    }

    private void scheduleRespawn(long readyAtNanos) {
        pendingPowerUpSpawns.add(new PendingPowerUpSpawn(readyAtNanos));
    }

    private void resetRoundEffectsAndPowerUps() {
        activePowerUps.clear();
        pendingPowerUpSpawns.clear();
        nextPowerUpId = 0;
        for (ServerPlayer player : players.values()) {
            player.effects.clear();
        }
    }

    private void drainCommands() {
        Runnable command;
        while ((command = commands.poll()) != null) {
            command.run();
        }
    }

    private static String sanitizePlayerName(String rawName, int playerId) {
        String sanitized = rawName == null ? "" : rawName.strip();
        if (sanitized.isEmpty()) {
            sanitized = "Spieler " + playerId;
        }
        sanitized = sanitized.replace('\n', ' ').replace('\r', ' ');
        return sanitized.length() > 18 ? sanitized.substring(0, 18) : sanitized;
    }

    private static float flatDistanceSquared(Vector3f first, Vector3f second) {
        float dx = first.x - second.x;
        float dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    private static final class ServerPlayer {

        private final HostedConnection connection;
        private final int playerId;
        private final int joinOrder;
        private final String playerName;
        private final Vector3f position = new Vector3f();

        private AvatarRole role = AvatarRole.UNASSIGNED;
        private StudentSkin studentSkin = StudentSkin.FJP;
        private boolean captured;
        private float yaw;
        private CharacterMode mode = CharacterMode.SPECIAL;
        private PlayerInputState input = PlayerInputState.idle(0f);
        private final PlayerEffectState effects = new PlayerEffectState();

        private ServerPlayer(HostedConnection connection, int playerId, int joinOrder, String playerName) {
            this.connection = connection;
            this.playerId = playerId;
            this.joinOrder = joinOrder;
            this.playerName = playerName;
        }
    }

    private record ActivePowerUp(int powerUpId, PowerUpType type, Vector3f position) {
    }

    private record PendingPowerUpSpawn(long readyAtNanos) {
    }

    private static final class ServerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "shrueck-server");
            thread.setDaemon(true);
            return thread;
        }
    }
}
