package de.intektor.pixelshooter_main_server.net.server.packet_handler;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.files.pstf.PSTagCompound;
import de.intektor.pixelshooter_common.net.packet.BadAccessTokenPacketToClient;
import de.intektor.pixelshooter_common.net.packet.InternalServerErrorWhilePublishingPacketToClient;
import de.intektor.pixelshooter_common.net.packet.LevelPublishedPacketToClient;
import de.intektor.pixelshooter_common.net.packet.PublishLevelPacketToServer;
import de.intektor.pixelshooter_common.packet.PacketHandler;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_main_server.Main;
import de.intektor.pixelshooter_main_server.net.server.TokenVerifier;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Intektor
 */
public class PublishLevelPacketToServerHandler implements PacketHandler<PublishLevelPacketToServer> {

    @Override
    public void handlePacket(PublishLevelPacketToServer packet, Socket socketFrom, Side from) {
        GoogleIdToken idToken = TokenVerifier.isTokenLegit(packet.infoTag.getString("idToken"));
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            Connection connection = Main.server.mainThread.connection;
            try {
                boolean unUsed = false;
                String officialID = "";
                while (!unUsed) {
                    officialID = generateRandomID();
                    PreparedStatement uuidCheck = connection.prepareStatement("SELECT * FROM pixelshooterleveldatabase WHERE official_id LIKE ?");
                    uuidCheck.setString(1, officialID);

                    ResultSet resultSet = uuidCheck.executeQuery();
                    if (!resultSet.next()) unUsed = true;
                }

                String sql = "INSERT INTO pixelshooterleveldatabase(official_id, user_email, time_uploaded, level_name, private, show_user_email) VALUES (?, ?, ?, ?, ?, ?);";
                PreparedStatement insert = connection.prepareStatement(sql);
                insert.setString(1, officialID);
                insert.setString(2, payload.getEmail());
                insert.setLong(3, System.currentTimeMillis());
                insert.setString(4, packet.infoTag.getString("level_name"));
                insert.setBoolean(5, packet.infoTag.getBoolean("private"));
                insert.setBoolean(6, packet.infoTag.getBoolean("show_email"));

                insert.execute();

                File file = new File("pixelshooter_world_files");
                file.mkdirs();
                file = new File("pixelshooter_world_files/" + officialID);
                PSTagCompound tag = new PSTagCompound();
                tag.setTag("level_tag", packet.levelTag);
                tag.setTag("info_tag", packet.infoTag);
                try {
                    tag.writeToStream(new DataOutputStream(new FileOutputStream(file)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                PacketHelper.sendPacket(new LevelPublishedPacketToClient(), socketFrom);
            } catch (SQLException e) {
                PacketHelper.sendPacket(new InternalServerErrorWhilePublishingPacketToClient(), socketFrom);
                e.printStackTrace();
            }
        } else {
            PacketHelper.sendPacket(new BadAccessTokenPacketToClient(), socketFrom);
        }
    }

    public String generateRandomID() {
        SecureRandom random = new SecureRandom();
        String output = "";
        for (int i = 0; i < 5; i++) {
            output += Main.server.mainThread.tagList.get(random.nextInt(Main.server.mainThread.tagList.size()));
        }
        return output;
    }
}
