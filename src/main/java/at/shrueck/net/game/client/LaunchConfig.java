package at.shrueck.net.game.client;

import at.shrueck.net.game.shared.GameConstants;

public record LaunchConfig(LaunchMode mode, String playerName, String host, int port) {

    public LaunchConfig {
        mode = mode == null ? LaunchMode.JOIN : mode;
        playerName = sanitizePlayerName(playerName);
        host = sanitizeHost(host);
        port = normalizePort(port);
    }

    public boolean isHost() {
        return mode == LaunchMode.HOST;
    }

    public String connectHost() {
        return isHost() ? "localhost" : host;
    }

    public String endpointLabel() {
        return connectHost() + ":" + port;
    }

    public static LaunchConfig fromArgs(String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Starte ohne Parameter fuer den Dialog oder nutze: host <Name> [Port] | join <Name> <Host> [Port]");
        }

        String mode = args[0].trim().toLowerCase();
        return switch (mode) {
            case "host" -> {
                String playerName = args.length >= 2 ? args[1] : defaultPlayerName();
                int port = args.length >= 3 ? parsePort(args[2]) : GameConstants.DEFAULT_PORT;
                yield new LaunchConfig(LaunchMode.HOST, playerName, "localhost", port);
            }
            case "join" -> {
                if (args.length < 3) {
                    throw new IllegalArgumentException("Join erwartet: join <Name> <Host> [Port]");
                }
                String playerName = args[1];
                String host = args[2];
                int port = args.length >= 4 ? parsePort(args[3]) : GameConstants.DEFAULT_PORT;
                yield new LaunchConfig(LaunchMode.JOIN, playerName, host, port);
            }
            default -> throw new IllegalArgumentException("Unbekannter Modus: " + args[0]);
        };
    }

    static String defaultPlayerName() {
        String userName = System.getProperty("user.name", "Spieler").trim();
        if (userName.isEmpty()) {
            userName = "Spieler";
        }
        return sanitizePlayerName(userName);
    }

    static int parsePort(String value) {
        try {
            return normalizePort(Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ungueltiger Port: " + value, exception);
        }
    }

    static String sanitizePlayerName(String value) {
        String sanitized = value == null ? "" : value.strip();
        if (sanitized.isEmpty()) {
            return defaultPlayerNameFallback();
        }
        sanitized = sanitized.replace('\n', ' ').replace('\r', ' ');
        return sanitized.length() > 18 ? sanitized.substring(0, 18) : sanitized;
    }

    private static String sanitizeHost(String value) {
        String sanitized = value == null ? "localhost" : value.strip();
        return sanitized.isEmpty() ? "localhost" : sanitized;
    }

    private static int normalizePort(int value) {
        if (value < 1024 || value > 65535) {
            throw new IllegalArgumentException("Port muss zwischen 1024 und 65535 liegen.");
        }
        return value;
    }

    private static String defaultPlayerNameFallback() {
        return "Spieler";
    }

    public enum LaunchMode {
        HOST,
        JOIN
    }
}