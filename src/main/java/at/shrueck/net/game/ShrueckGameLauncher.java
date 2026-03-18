package at.shrueck.net.game;

import at.shrueck.net.game.client.LaunchConfig;
import at.shrueck.net.game.client.LauncherDialog;
import at.shrueck.net.game.client.MultiplayerClientSession;
import at.shrueck.net.game.net.NetworkProtocol;
import at.shrueck.net.game.server.LanGameServer;
import com.jme3.system.AppSettings;

public final class ShrueckGameLauncher {

    private ShrueckGameLauncher() {
    }

    public static void main(String[] args) {
        LaunchConfig launchConfig;
        try {
            launchConfig = args.length == 0 ? LauncherDialog.chooseLaunchConfig() : LaunchConfig.fromArgs(args);
        } catch (IllegalArgumentException exception) {
            LauncherDialog.showStartupError(exception.getMessage());
            return;
        }

        if (launchConfig == null) {
            return;
        }

        NetworkProtocol.registerAll();

        LanGameServer hostedServer = null;
        MultiplayerClientSession clientSession = null;
        try {
            if (launchConfig.isHost()) {
                hostedServer = new LanGameServer(launchConfig.port());
                hostedServer.start();
            }

            clientSession = new MultiplayerClientSession(launchConfig);
            clientSession.start();

            AppSettings settings = new AppSettings(true);
            settings.setTitle(launchConfig.isHost() ? "Shrueck LAN Host" : "Shrueck LAN Client");
            settings.setResolution(1600, 900);
            settings.setSamples(4);
            settings.setVSync(true);

            ShrueckSchoolGame game = new ShrueckSchoolGame(launchConfig, clientSession, hostedServer);
            game.setShowSettings(false);
            game.setSettings(settings);
            game.start();
        } catch (Exception exception) {
            if (clientSession != null) {
                clientSession.close();
            }
            if (hostedServer != null) {
                hostedServer.close();
            }
            exception.printStackTrace();
            LauncherDialog.showStartupError("Start fehlgeschlagen: " + exception.getMessage());
        }
    }
}