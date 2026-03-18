package at.shrueck.net.game.client;

import at.shrueck.net.game.shared.GameConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final class LauncherDialog {

    private LauncherDialog() {
    }

    public static LaunchConfig chooseLaunchConfig() {
        if (isMacOs()) {
            return chooseMacLaunchConfig();
        }

        installSystemLookAndFeel();

        Object[] options = {"Host", "Join", "Abbrechen"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Shrueck LAN Multiplayer starten",
                "Shrueck LAN",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            return createHostConfig();
        }
        if (choice == 1) {
            return createJoinConfig();
        }
        return null;
    }

    public static void showStartupError(String message) {
        if (isMacOs()) {
            try {
                runAppleScript(
                        "display alert " + appleQuote("Shrueck LAN")
                                + " message " + appleQuote(message)
                                + " as critical"
                );
                return;
            } catch (Exception ignored) {
            }
        }

        try {
            JOptionPane.showMessageDialog(null, message, "Shrueck LAN", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ignored) {
            System.err.println(message);
        }
    }

    private static LaunchConfig chooseMacLaunchConfig() {
        try {
            String choice = runAppleScript(
                    "button returned of (display dialog "
                            + appleQuote("Shrueck LAN Multiplayer starten")
                            + " buttons {\"Abbrechen\", \"Join\", \"Host\"} default button \"Host\")"
            );
            if (choice == null || choice.equals("Abbrechen")) {
                return null;
            }
            if (choice.equals("Host")) {
                return createMacHostConfig();
            }
            if (choice.equals("Join")) {
                return createMacJoinConfig();
            }
            return null;
        } catch (IOException interrupted) {
            return null;
        } catch (Exception exception) {
            throw new IllegalStateException("macOS-Startdialog fehlgeschlagen: " + exception.getMessage(), exception);
        }
    }

    private static LaunchConfig createMacHostConfig() throws IOException, InterruptedException {
        String playerName = promptMacText("Spielername", LaunchConfig.defaultPlayerName());
        if (playerName == null) {
            return null;
        }
        Integer port = promptMacPort(GameConstants.DEFAULT_PORT);
        if (port == null) {
            return null;
        }
        return new LaunchConfig(LaunchConfig.LaunchMode.HOST, playerName, "127.0.0.1", port);
    }

    private static LaunchConfig createMacJoinConfig() throws IOException, InterruptedException {
        String playerName = promptMacText("Spielername", LaunchConfig.defaultPlayerName());
        if (playerName == null) {
            return null;
        }
        String host = promptMacText("Host-IP oder localhost", "localhost");
        if (host == null) {
            return null;
        }
        Integer port = promptMacPort(GameConstants.DEFAULT_PORT);
        if (port == null) {
            return null;
        }
        return new LaunchConfig(LaunchConfig.LaunchMode.JOIN, playerName, host, port);
    }

    private static String promptMacText(String label, String defaultValue) throws IOException, InterruptedException {
        try {
            return runAppleScript(
                    "text returned of (display dialog "
                            + appleQuote(label)
                            + " default answer "
                            + appleQuote(defaultValue)
                            + ")"
            );
        } catch (IOException exception) {
            return null;
        }
    }

    private static Integer promptMacPort(int defaultPort) throws InterruptedException {
        while (true) {
            String value;
            try {
                value = runAppleScript(
                        "text returned of (display dialog "
                                + appleQuote("Port")
                                + " default answer "
                                + appleQuote(Integer.toString(defaultPort))
                                + ")"
                );
            } catch (IOException exception) {
                return null;
            }

            try {
                return LaunchConfig.parsePort(value);
            } catch (IllegalArgumentException exception) {
                try {
                    runAppleScript(
                            "display alert "
                                    + appleQuote("Port ungueltig")
                                    + " message "
                                    + appleQuote(exception.getMessage())
                    );
                } catch (IOException ignored) {
                    return null;
                }
            }
        }
    }

    private static LaunchConfig createHostConfig() {
        String playerName = promptText("Spielername", LaunchConfig.defaultPlayerName());
        if (playerName == null) {
            return null;
        }
        Integer port = promptPort(GameConstants.DEFAULT_PORT);
        if (port == null) {
            return null;
        }
        return new LaunchConfig(LaunchConfig.LaunchMode.HOST, playerName, "127.0.0.1", port);
    }

    private static LaunchConfig createJoinConfig() {
        String playerName = promptText("Spielername", LaunchConfig.defaultPlayerName());
        if (playerName == null) {
            return null;
        }
        String host = promptText("Host-IP oder localhost", "localhost");
        if (host == null) {
            return null;
        }
        Integer port = promptPort(GameConstants.DEFAULT_PORT);
        if (port == null) {
            return null;
        }
        return new LaunchConfig(LaunchConfig.LaunchMode.JOIN, playerName, host, port);
    }

    private static String promptText(String label, String defaultValue) {
        Object value = JOptionPane.showInputDialog(
                null,
                label,
                defaultValue
        );
        if (value == null) {
            return null;
        }
        return value.toString().strip();
    }

    private static Integer promptPort(int defaultPort) {
        while (true) {
            String value = promptText("Port", Integer.toString(defaultPort));
            if (value == null) {
                return null;
            }
            try {
                return LaunchConfig.parsePort(value);
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        exception.getMessage(),
                        "Port ungueltig",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private static void installSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private static boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    private static String runAppleScript(String script) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(List.of("osascript", "-e", script)).start();
        String stdout;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            stdout = reader.lines().reduce((first, second) -> first + "\n" + second).orElse("").strip();
        }
        String stderr;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            stderr = reader.lines().reduce((first, second) -> first + "\n" + second).orElse("").strip();
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(stderr.isBlank() ? "osascript exit " + exitCode : stderr);
        }
        return stdout;
    }

    private static String appleQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}