package at.shrueck.net.game.client;

import at.shrueck.net.game.shared.GameConstants;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final class LauncherDialog {

    private LauncherDialog() {
    }

    public static LaunchConfig chooseLaunchConfig() {
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

    private static LaunchConfig createHostConfig() {
        String playerName = promptText("Spielername", LaunchConfig.defaultPlayerName());
        if (playerName == null) {
            return null;
        }
        Integer port = promptPort(GameConstants.DEFAULT_PORT);
        if (port == null) {
            return null;
        }
        return new LaunchConfig(LaunchConfig.LaunchMode.HOST, playerName, "localhost", port);
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
}