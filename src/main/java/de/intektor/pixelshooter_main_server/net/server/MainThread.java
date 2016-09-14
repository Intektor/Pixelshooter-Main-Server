package de.intektor.pixelshooter_main_server.net.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

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
            Properties properties = new Properties();
            properties.setProperty("useSSL", "false");
            properties.setProperty("serverTimezone", "GMT");
            connection = DriverManager.getConnection("jdbc:mysql://" + ip + "/pixelshooter?user=" + username + "&password=" + password, properties);
        } catch (SQLException e) {
            e.printStackTrace();
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
}

