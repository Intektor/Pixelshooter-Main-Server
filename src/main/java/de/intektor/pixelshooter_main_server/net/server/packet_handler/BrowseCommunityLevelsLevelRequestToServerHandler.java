package de.intektor.pixelshooter_main_server.net.server.packet_handler;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.levels.BasicLevelInformation;
import de.intektor.pixelshooter_common.net.packet.BrowseCommunityLevelRequestResponseToClient;
import de.intektor.pixelshooter_common.net.packet.BrowseCommunityLevelsLevelRequestToServer;
import de.intektor.pixelshooter_common.net.packet.BrowseCommunityLevelsLevelRequestToServer.Function;
import de.intektor.pixelshooter_common.net.packet.BrowseCommunityLevelsLevelRequestToServer.Order.OrderType;
import de.intektor.pixelshooter_common.net.packet.BrowseCommunityLevelsLevelRequestToServer.Type;
import de.intektor.pixelshooter_common.packet.PacketHandler;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_main_server.Main;
import de.intektor.pixelshooter_main_server.net.server.TokenVerifier;
import de.intektor.pixelshooter_main_server.net.server.util.ResultSetReader;

import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static de.intektor.pixelshooter_main_server.Main.logger;

/**
 * @author Intektor
 */
public class BrowseCommunityLevelsLevelRequestToServerHandler implements PacketHandler<BrowseCommunityLevelsLevelRequestToServer> {

    @Override
    public void handlePacket(final BrowseCommunityLevelsLevelRequestToServer packet, final Socket socketFrom, final Side from) {
        Main.server.mainThread.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Connection connection = Main.server.mainThread.connection;
                try {
                    String typeFieldName = "";
                    switch (packet.type) {
                        case BEST_RATED:
                            typeFieldName = "total_rated / nullif(total_rating, 0)";
                            break;
                        case MOST_DOWNLOADS:
                            typeFieldName = "downloadcount";
                            break;
                        case PLAY_COUNT:
                            typeFieldName = "playcount";
                            break;
                        case NEWEST:
                            typeFieldName = "time_uploaded";
                            break;
                        case OLDEST:
                            typeFieldName = "time_uploaded";
                            break;
                    }

                    String moreQuestion = "";

                    if (packet.order.type == OrderType.MORE) {
                        PreparedStatement getByOfficialID = connection.prepareStatement("SELECT * FROM pixelshooterleveldatabase WHERE official_id LIKE ?");
                        getByOfficialID.setString(1, packet.order.lastOfficialID);
                        ResultSet resultSet = getByOfficialID.executeQuery();
                        resultSet.next();
                        BasicLevelInformation info = ResultSetReader.readBasicLevelInfoFromResultSet(resultSet);
                        switch (packet.type) {
                            case BEST_RATED:
                                moreQuestion = "AND total_rated / nullif(total_rating, 0) < " + info.rating;
                                break;
                            case MOST_DOWNLOADS:
                                moreQuestion = "AND downloadcount < " + info.downloadCount;
                                break;
                            case PLAY_COUNT:
                                moreQuestion = "AND playcount < " + info.playCount;
                                break;
                            case NEWEST:
                                moreQuestion = "AND time_uploaded > " + info.timeUploaded;
                                break;
                            case OLDEST:
                                moreQuestion = "AND time_uploaded < " + info.timeUploaded;
                                break;
                        }
                    }

                    boolean desc = packet.type == Type.OLDEST || packet.type == Type.BEST_RATED || packet.type == Type.PLAY_COUNT || packet.type == Type.MOST_DOWNLOADS;

                    String searchQuestion = "";
                    if (packet.function == Function.SEARCH) {
                        searchQuestion = " AND level_name LIKE '" + packet.order.search + "%' AND user_email LIKE '" + packet.order.userFilter + "%' ";
                    }

                    String privacy = " AND private = 0";
                    String userLevelQuestion = "";
                    if (packet.userData.getBoolean("loggedIn")) {
                        GoogleIdToken idToken = TokenVerifier.isTokenLegit(packet.userData.getString("idToken"));
                        if (packet.function == Function.USER_LEVEL && idToken != null) {
                            privacy = "";
                            userLevelQuestion = " AND user_email LIKE '" + idToken.getPayload().getEmail() + "'";
                        }
                    }

                    PreparedStatement search = connection.prepareStatement("SELECT * FROM pixelshooterleveldatabase WHERE ? - time_uploaded < ?" + privacy + moreQuestion + searchQuestion + userLevelQuestion + " ORDER BY " + typeFieldName + (desc ? " DESC" : " ASC") + " LIMIT 30");
                    search.setLong(1, System.currentTimeMillis());
                    search.setLong(2, packet.time.getTimeInMilliSec());

                    ResultSet resultSet = search.executeQuery();
                    List<BasicLevelInformation> levelInfoList = new ArrayList<BasicLevelInformation>();
                    while (resultSet.next()) {
                        BasicLevelInformation e = ResultSetReader.readBasicLevelInfoFromResultSet(resultSet);
                        levelInfoList.add(e);
                    }
                    BrowseCommunityLevelRequestResponseToClient response = new BrowseCommunityLevelRequestResponseToClient(levelInfoList, packet.order, levelInfoList.size() == 30 ? BrowseCommunityLevelRequestResponseToClient.SupplyType.MORE : BrowseCommunityLevelRequestResponseToClient.SupplyType.LIMIT);
                    PacketHelper.sendPacket(response, socketFrom);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Exception!", e);
                    Main.server.mainThread.checkConnection();
                }
            }
        });
    }
}
