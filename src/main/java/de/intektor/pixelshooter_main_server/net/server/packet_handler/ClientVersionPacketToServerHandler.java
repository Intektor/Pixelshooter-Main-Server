package de.intektor.pixelshooter_main_server.net.server.packet_handler;

import de.intektor.pixelshooter_common.common.ClientVersion;
import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.net.packet.ClientVersionPacketToServer;
import de.intektor.pixelshooter_common.net.packet.UnsupportedClientVersionPacketToClient;
import de.intektor.pixelshooter_common.packet.PacketHandler;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_main_server.Main;
import de.intektor.pixelshooter_main_server.net.server.MainThread;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Intektor
 */
public class ClientVersionPacketToServerHandler implements PacketHandler<ClientVersionPacketToServer> {

    @Override
    public void handlePacket(final ClientVersionPacketToServer packet, final Socket socketFrom, Side from) {
        final MainThread thread = Main.server.mainThread;
        thread.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (!isVersionAcceptable(packet.version)) {
                    PacketHelper.sendPacket(new UnsupportedClientVersionPacketToClient(), socketFrom);
                    thread.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                socketFrom.close();
                                Main.server.connectionList.remove(socketFrom);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    public boolean isVersionAcceptable(ClientVersion version) {
        if (version.major == 1) {
            if (version.minor >= 0) {
                return true;
            }
        }
        return false;
    }
}
