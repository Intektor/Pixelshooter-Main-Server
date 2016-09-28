package de.intektor.pixelshooter_main_server;

import de.intektor.pixelshooter_common.PixelShooterCommon;
import de.intektor.pixelshooter_common.net.packet.*;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_common.packet.PacketRegistry;
import de.intektor.pixelshooter_main_server.net.server.MainServer;
import de.intektor.pixelshooter_main_server.net.server.packet_handler.*;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.*;

/**
 * @author Intektor
 */
public class Main {

    public static volatile MainServer server;
    public static Logger logger = Logger.getLogger("main.logger");

    public static SimpleDateFormat simpleDateFormat;

    public static void main(String[] args) throws IOException {
        FileHandler fileHandler = new FileHandler("log.txt");
        fileHandler.setFormatter(new SimpleFormatter());
        StreamHandler streamHandler = new StreamHandler(System.out, new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.addHandler(streamHandler);

        simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd:HH:mm:ss");
        logger.info("Starting server at " + simpleDateFormat.format(new Date()));

        PixelShooterCommon.init();
        PacketRegistry.INSTANCE.registerHandlerForPacket(PublishLevelPacketToServer.class, PublishLevelPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(BrowseCommunityLevelsLevelRequestToServer.class, BrowseCommunityLevelsLevelRequestToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(RequestLevelDataPacketToServer.class, RequestLevelDataPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(LevelActionPacketToServer.class, LevelActionPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(RatingPacketToServer.class, RatingPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(ClientVersionPacketToServer.class, ClientVersionPacketToServerHandler.class);
        PacketRegistry.INSTANCE.registerHandlerForPacket(CampaignWorldsUpdateRequestPacketToServer.class, CampaignWorldsUpdateRequestPacketToServerHandler.class);

        String sqlIP = "localhost";
        String sqlUsername = "root";
        String sqlPassword = "123456";

        InputStream config = Main.class.getResourceAsStream("/start.psconfig");
        if (config != null) {
            Scanner scanner = new Scanner(config);
            sqlIP = scanner.next();
            sqlUsername = scanner.next();
            sqlPassword = scanner.next();
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
                    logger.log(Level.WARNING, "Exception!", e);
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
                logger.info("Server was closed by the admin at: " + simpleDateFormat.format(new Date()));
                try {
                    server.mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.exit(0);
                }
            } else {
                System.out.println("Type stop to stop the server!");
            }
        }

    }
}
