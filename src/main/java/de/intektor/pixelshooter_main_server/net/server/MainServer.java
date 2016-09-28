package de.intektor.pixelshooter_main_server.net.server;

import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.net.packet.ServerShutdownPacketToClient;
import de.intektor.pixelshooter_common.packet.Packet;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_common.packet.PacketRegistry;

import javax.net.ssl.*;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Intektor
 */
public class MainServer {

    private int port;

    volatile boolean runServer = true;

    public volatile List<SSLSocket> connectionList = Collections.synchronizedList(new ArrayList<SSLSocket>());

    public volatile Map<SSLSocket, ConnectionThread> threadMap = new ConcurrentHashMap<SSLSocket, ConnectionThread>();

    public MainServer(int port) {
        this.port = port;
    }

    public volatile MainThread mainThread;

    public void start(String ip, String username, String password) throws Exception {
        mainThread = new MainThread(this, ip, username, password);
        mainThread.start();

        SSLServerSocket serverSocket = (SSLServerSocket) getSSLContext().getServerSocketFactory().createServerSocket(port);
        serverSocket.setSoTimeout(0);
        while (runServer) {
            final SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
            connectionList.add(clientSocket);
            ConnectionThread t = new ConnectionThread(this, clientSocket);
            t.start();
            threadMap.put(clientSocket, t);
        }
    }

    public boolean onPacketReceivedPRE(Socket connection, Packet packet) {
        return true;
    }


    public void onPacketReceivedPOST(Socket connection, Packet packet) {

    }

    public synchronized void removeConnection(SSLSocket socket) {
        connectionList.remove(socket);
        threadMap.remove(socket);
    }

    public void stopServer() {
        runServer = false;
        try {
            mainThread.connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (final ConnectionThread t : threadMap.values()) {
            t.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    PacketHelper.sendPacket(new ServerShutdownPacketToClient(), t.clientSocket);
                }
            });
        }
    }

    public static SSLContext getSSLContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            KeyStore ks = KeyStore.getInstance("jks");
            InputStream stream = MainServer.class.getResourceAsStream("/keystore.jks");
            ks.load(stream, "HartesPasswort123$$".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "HartesPasswort123$$".toCharArray());
            sslContext.init(kmf.getKeyManagers(), trustAllCerts, null);
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ConnectionThread extends Thread {

        MainServer server;

        public SSLSocket clientSocket;

        public ConnectionThread(MainServer server, SSLSocket socket) {
            this.server = server;
            this.clientSocket = socket;
        }

        Queue<Runnable> scheduledTasks = new LinkedBlockingDeque<Runnable>();

        @Override
        public void run() {
            boolean active = true;
            while (server.runServer && active) {
                try {
                    Packet packet = PacketHelper.readPacket(new DataInputStream(clientSocket.getInputStream()));
                    if (server.onPacketReceivedPRE(clientSocket, packet)) {
                        PacketRegistry.INSTANCE.getHandlerForPacketClass(packet.getClass()).newInstance().handlePacket(packet, clientSocket, Side.CLIENT);
                        server.onPacketReceivedPOST(clientSocket, packet);
                    }
                    Runnable t;
                    while ((t = scheduledTasks.poll()) != null) {
                        t.run();
                    }
                } catch (Throwable t) {
                    active = false;
                    server.removeConnection(clientSocket);
                }
            }
        }

        public synchronized void addScheduledTask(Runnable task) {
            scheduledTasks.add(task);
        }
    }
}
