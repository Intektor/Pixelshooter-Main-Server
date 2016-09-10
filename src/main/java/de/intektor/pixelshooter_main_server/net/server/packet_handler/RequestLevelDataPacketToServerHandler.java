package de.intektor.pixelshooter_main_server.net.server.packet_handler;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.files.pstf.PSTagCompound;
import de.intektor.pixelshooter_common.levels.BasicLevelInformation;
import de.intektor.pixelshooter_common.net.packet.InternalServerErrorWhileGettingLevelDataPacketToClient;
import de.intektor.pixelshooter_common.net.packet.RequestLevelDataPacketResponsePacketToClient;
import de.intektor.pixelshooter_common.net.packet.RequestLevelDataPacketToServer;
import de.intektor.pixelshooter_common.packet.PacketHandler;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_main_server.Main;
import de.intektor.pixelshooter_main_server.net.server.TokenVerifier;
import de.intektor.pixelshooter_main_server.net.server.util.ResultSetReader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Intektor
 */
public class RequestLevelDataPacketToServerHandler implements PacketHandler<RequestLevelDataPacketToServer> {

    @Override
    public void handlePacket(final RequestLevelDataPacketToServer packet, final Socket socketFrom, Side from) {
        Main.server.mainThread.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                File file = new File("pixelshooter_world_files/" + packet.officialID);
                PSTagCompound tag = new PSTagCompound();
                try {
                    BasicLevelInformation info = null;
                    boolean prevRated = false;
                    int ratedStars = 0;
                    try {
                        Connection connection = Main.server.mainThread.connection;
                        PreparedStatement lookUp = connection.prepareStatement("SELECT * FROM pixelshooterleveldatabase WHERE official_id LIKE ?");
                        lookUp.setString(1, packet.officialID);
                        ResultSet resultSet = lookUp.executeQuery();
                        resultSet.next();
                        info = ResultSetReader.readBasicLevelInfoFromResultSet(resultSet);

                        GoogleIdToken idToken = TokenVerifier.isTokenLegit(packet.userInfoTag.getString("idToken"));
                        if (idToken != null) {
                            PreparedStatement ratingLookup = connection.prepareStatement("SELECT * FROM ratingtable WHERE user_email LIKE ? AND official_id LIKE ?");
                            ratingLookup.setString(1, idToken.getPayload().getEmail());
                            ratingLookup.setString(2, packet.officialID);
                            ResultSet ratingResult = ratingLookup.executeQuery();
                            if (ratingResult.next()) {
                                prevRated = true;
                                ratedStars = ratingResult.getInt("rated");
                            }
                        }
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                    }
                    FileInputStream in = new FileInputStream(file);
                    tag.readFromStream(new DataInputStream(in));
                    PSTagCompound levelTag = tag.getTag("level_tag");
                    PSTagCompound infoTag = tag.getTag("info_tag");

                    PSTagCompound sendInfoTag = new PSTagCompound();
                    sendInfoTag.setString("level_name", infoTag.getString("level_name"));
                    sendInfoTag.setString("written_info", infoTag.getString("written_info"));
                    sendInfoTag.setString("official_id", packet.officialID);

                    PacketHelper.sendPacket(new RequestLevelDataPacketResponsePacketToClient(info, infoTag.getString("written_info"), levelTag, prevRated, ratedStars), socketFrom);
                } catch (IOException e) {
                    PacketHelper.sendPacket(new InternalServerErrorWhileGettingLevelDataPacketToClient(), socketFrom);
                }
            }
        });
    }
}
