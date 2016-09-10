package de.intektor.pixelshooter_main_server.net.server.util;

import de.intektor.pixelshooter_common.levels.BasicLevelInformation;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Intektor
 */
public class ResultSetReader {

    public static BasicLevelInformation readBasicLevelInfoFromResultSet(ResultSet resultSet) throws SQLException {
        String levelName = resultSet.getString("level_name");
        String official_id = resultSet.getString("official_id");
        String author = resultSet.getBoolean("show_user_email") ? "anonymous" : resultSet.getString("user_email");
        long timeUploaded = resultSet.getLong("time_uploaded");
        long playCount = resultSet.getLong("playcount");
        long downloadCount = resultSet.getLong("downloadcount");
        long totalRating = resultSet.getLong("total_rating");
        float totalRated = resultSet.getLong("total_rated");
        float rating = totalRated / totalRating;
        return new BasicLevelInformation(levelName, author, official_id, timeUploaded, playCount, downloadCount, rating, totalRating);
    }
}
