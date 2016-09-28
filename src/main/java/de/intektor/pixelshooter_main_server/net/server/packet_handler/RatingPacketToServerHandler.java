package de.intektor.pixelshooter_main_server.net.server.packet_handler;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.net.packet.RatingPacketToServer;
import de.intektor.pixelshooter_common.packet.PacketHandler;
import de.intektor.pixelshooter_main_server.Main;
import de.intektor.pixelshooter_main_server.net.server.TokenVerifier;

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
public class RatingPacketToServerHandler implements PacketHandler<RatingPacketToServer> {

    @Override
    public void handlePacket(final RatingPacketToServer packet, Socket socketFrom, Side from) {
        Main.server.mainThread.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Connection connection = Main.server.mainThread.connection;
                GoogleIdToken token = TokenVerifier.isTokenLegit(packet.idToken);
                if (token != null) {
                    try {
                        PreparedStatement lookup = connection.prepareStatement("SELECT * FROM ratingtable WHERE official_id LIKE ? AND user_email LIKE ?");
                        lookup.setString(1, packet.officialID);
                        lookup.setString(2, token.getPayload().getEmail());
                        ResultSet lookupQuery = lookup.executeQuery();
                        if (!lookupQuery.next()) {
                            PreparedStatement insert = connection.prepareStatement("INSERT INTO ratingtable (user_email, official_id, rated) VALUES(?, ?, ?)");
                            insert.setString(1, token.getPayload().getEmail());
                            insert.setString(2, packet.officialID);
                            insert.setInt(3, packet.starsRated);
                            insert.execute();
                            PreparedStatement update = connection.prepareStatement("UPDATE pixelshooterleveldatabase SET total_rating=total_rating + 1, total_rated=total_rated + ? WHERE official_id LIKE ?");
                            update.setInt(1, packet.starsRated);
                            update.setString(2, packet.officialID);
                            update.execute();
                        }
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "Exception!", e);
                        Main.server.mainThread.checkConnection();
                    }
                }
            }
        });
    }
}
