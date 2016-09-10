package de.intektor.pixelshooter_main_server;

import de.intektor.pixelshooter_common.PixelShooterCommon;
import de.intektor.pixelshooter_common.net.packet.*;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_common.packet.PacketRegistry;
import de.intektor.pixelshooter_main_server.net.server.MainServer;
import de.intektor.pixelshooter_main_server.net.server.packet_handler.*;

import javax.net.ssl.SSLSocket;
import java.util.Scanner;

/**
 * @author Intektor
 */
public class Main {

    public static volatile MainServer server;

    public static void main(String[] args) {
        PixelShooterCommon.init();
        PacketRegistry.INSTANCE.registerHandlerForPacket(PublishLevelPacketToServer.class, PublishLevelPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(BrowseCommunityLevelsLevelRequestToServer.class, BrowseCommunityLevelsLevelRequestToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(RequestLevelDataPacketToServer.class, RequestLevelDataPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(LevelActionPacketToServer.class, LevelActionPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(RatingPacketToServer.class, RatingPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(ClientVersionPacketToServer.class, ClientVersionPacketToServerHandler.class);

        String sqlIP = "localhost";
        String sqlUsername = "root";
        String sqlPassword = "123456";

        for (int i = 0; i < args.length; i += 2) {
            String argument = args[i];
            String value = args[i + 1];
            if (argument.equals("-sqlip")) {
                sqlIP = value.substring(1);
            } else if (argument.equals("-sqlusername")) {
                sqlUsername = value.substring(1);
            } else if (argument.equals("-sqluserpassword")) {
                sqlPassword = value.substring(1);
            }
        }

        final String finalSqlIP = sqlIP;
        final String finalSqlUsername = sqlUsername;
        final String finalSqlPassword = sqlPassword;
        new Thread() {
            @Override
            public void run() {
                server = new MainServer(22198);
                try {
                    server.start(finalSqlIP, finalSqlUsername, finalSqlPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                    for (SSLSocket sslSocket : server.connectionList) {
                        PacketHelper.sendPacket(new ServerShutdownPacketToClient(), sslSocket);
                    }
                }
            }
        }.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (scanner.nextLine().equals("stop")) {
                server.stopServer();
                System.out.println("Stopping server");
                System.exit(0);
            } else {
                System.out.println("Type stop to stop the server!");
            }
        }

    }
}
