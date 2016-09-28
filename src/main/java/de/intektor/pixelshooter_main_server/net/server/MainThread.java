package de.intektor.pixelshooter_main_server.net.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static de.intektor.pixelshooter_main_server.Main.logger;

/**
 * @author Intektor
 */
public class MainThread extends Thread {

    public volatile MainServer server;
    private String ip;
    private String username;
    private String password;
    public volatile Queue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();

    public MainThread(MainServer server, String ip, String username, String password) {
        this.server = server;
        this.ip = ip;
        this.username = username;
        this.password = password;
    }

    public volatile Connection connection;

    public List<String> tagList = new ArrayList<String>();

    @Override
    public void run() {
        try {
            connectToSQLServer();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS `downloadcounttable` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `player_uuid` varchar(45) NOT NULL,\n" +
                    "  `official_id` varchar(45) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;\n").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS `pixelshooterleveldatabase` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `official_id` varchar(45) NOT NULL,\n" +
                    "  `user_email` varchar(45) NOT NULL,\n" +
                    "  `time_uploaded` bigint(20) NOT NULL,\n" +
                    "  `level_name` varchar(45) NOT NULL,\n" +
                    "  `show_user_email` bit(1) NOT NULL,\n" +
                    "  `private` bit(1) NOT NULL,\n" +
                    "  `downloadcount` bigint(20) DEFAULT '0',\n" +
                    "  `playcount` bigint(20) DEFAULT '0',\n" +
                    "  `total_rating` double DEFAULT '0',\n" +
                    "  `total_rated` int(11) DEFAULT '0',\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  UNIQUE KEY `id_UNIQUE` (`id`)\n" +
                    ") ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;\n").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS `playcounttable` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `player_uuid` varchar(45) NOT NULL,\n" +
                    "  `official_id` varchar(45) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;\n").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS `ratingtable` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `user_email` varchar(45) NOT NULL,\n" +
                    "  `official_id` varchar(45) NOT NULL,\n" +
                    "  `rated` int(11) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;\n").execute();

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Exception!", e);
        }

        Scanner scanner = new Scanner(MainThread.class.getResourceAsStream("/TagList.txt"));
        while (scanner.hasNext()) {
            tagList.add(scanner.next());
        }

        while (server.runServer) {
            serverTick();
        }
    }


    public synchronized void serverTick() {
        Runnable t;
        while ((t = tasks.poll()) != null) {
            t.run();
        }
    }


    public synchronized void addScheduledTask(Runnable task) {
        tasks.offer(task);
    }

    public void checkConnection() {
        try {
            if (connection.isClosed()) {
                logger.info("The SQL connection was closed after an exception thrown before!");
                logger.info("Trying to recreate the connection to the SQL server!");
                do {
                    connectToSQLServer();
                } while (connection.isClosed());
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Exception!", e);
        }
    }

    public void connectToSQLServer() {
        Properties properties = new Properties();
        properties.setProperty("useSSL", "false");
        properties.setProperty("serverTimezone", "GMT");
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + ip + "/pixelshooter?user=" + username + "&password=" + password, properties);
        } catch (SQLException ignored) {

        }

    }
}

