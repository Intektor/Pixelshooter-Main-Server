package de.intektor.pixelshooter_main_server.net.server.packet_handler;

import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.net.packet.LevelActionPacketToServer;
import de.intektor.pixelshooter_common.packet.PacketHandler;
import de.intektor.pixelshooter_main_server.Main;

import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import static de.intektor.pixelshooter_main_server.Main.logger;

/**
 * @author Intektor
 */
public class LevelActionPacketToServerHandler implements PacketHandler<LevelActionPacketToServer> {

    @Override
    public void handlePacket(final LevelActionPacketToServer packet, Socket socketFrom, Side from) {
        Main.server.mainThread.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Connection connection = Main.server.mainThread.connection;
                try {
                    String table_name = packet.action == LevelActionPacketToServer.Action.PLAY ? "playcounttable" : "downloadcounttable";
                    PreparedStatement check = connection.prepareStatement("SELECT * FROM " + table_name + " WHERE official_id LIKE ? AND player_uuid LIKE ?");
                    check.setString(1, packet.officialID);
                    check.setString(2, packet.playerUUID.toString());
                    ResultSet resultSet = check.executeQuery();
                    if (!resultSet.next()) {
                        PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table_name + " (player_uuid, official_id) VALUES(?, ?)");
                        insert.setString(1, packet.playerUUID.toString());
                        insert.setString(2, packet.officialID);
                        insert.execute();
                        String field = packet.action == LevelActionPacketToServer.Action.PLAY ? "playcount" : "downloadcount";
                        PreparedStatement update = connection.prepareStatement("UPDATE pixelshooterleveldatabase SET " + field + "=" + field + " + 1 WHERE official_id LIKE ?");
                        update.setString(1, packet.officialID);
                        update.execute();
                    }
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Exception!", e);
                    Main.server.mainThread.checkConnection();
                }
            }
        });
    }
}
