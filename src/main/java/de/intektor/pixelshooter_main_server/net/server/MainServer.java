package de.intektor.pixelshooter_main_server.net.server;

import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.net.packet.ServerShutdownPacketToClient;
import de.intektor.pixelshooter_common.packet.Packet;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_common.packet.PacketRegistry;

import javax.net.ssl.*;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Intektor
 */
public class MainServer {

    private int port;

    volatile boolean runServer = true;

    public volatile List<SSLSocket> connectionList = Collections.synchronizedList(new ArrayList<SSLSocket>());

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
            new Thread() {
                @Override
                public void run() {
                    boolean active = true;
                    while (runServer && active) {
                        try {
                            Packet packet = PacketHelper.readPacket(new DataInputStream(clientSocket.getInputStream()));
                            if (onPacketReceivedPRE(clientSocket, packet)) {
                                PacketRegistry.INSTANCE.getHandlerForPacketClass(packet.getClass()).newInstance().handlePacket(packet, clientSocket, Side.CLIENT);
                                onPacketReceivedPOST(clientSocket, packet);
                            }
                        } catch (Exception e) {
                            active = false;
                            connectionList.remove(clientSocket);
                        }
                    }
                }
            }.start();
        }
    }

    public boolean onPacketReceivedPRE(Socket connection, Packet packet) {
        return true;
    }


    public void onPacketReceivedPOST(Socket connection, Packet packet) {

    }

    public void stopServer() {
        runServer = false;
        for (SSLSocket sslSocket : connectionList) {
            PacketHelper.sendPacket(new ServerShutdownPacketToClient(), sslSocket);
        }
    }

    public static SSLContext getSSLContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            KeyStore ks = KeyStore.getInstance("jks");
            FileInputStream stream = new FileInputStream("keystore.jks");
            ks.load(stream, "HartesPasswort123$$".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "HartesPasswort123$$".toCharArray());
            sslContext.init(kmf.getKeyManagers(), trustAllCerts, null);
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
