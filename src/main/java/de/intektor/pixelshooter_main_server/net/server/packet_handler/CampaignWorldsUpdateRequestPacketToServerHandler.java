package de.intektor.pixelshooter_main_server.net.server.packet_handler;

import de.intektor.pixelshooter_common.common.Side;
import de.intektor.pixelshooter_common.common.Version;
import de.intektor.pixelshooter_common.files.pstf.PSTagCompound;
import de.intektor.pixelshooter_common.net.packet.CampaignWorldsUpdateRequestPacketToServer;
import de.intektor.pixelshooter_common.net.packet.CampaignWorldsUpdateRequestResponseToClient;
import de.intektor.pixelshooter_common.packet.Packet;
import de.intektor.pixelshooter_common.packet.PacketHandler;
import de.intektor.pixelshooter_common.packet.PacketHelper;
import de.intektor.pixelshooter_main_server.Main;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static de.intektor.pixelshooter_common.net.packet.CampaignWorldsUpdateRequestResponseToClient.*;

/**
 * @author Intektor
 */
public class CampaignWorldsUpdateRequestPacketToServerHandler implements PacketHandler<CampaignWorldsUpdateRequestPacketToServer> {

    @Override
    public void handlePacket(final CampaignWorldsUpdateRequestPacketToServer packet, final Socket socketFrom, Side from) {
        Main.server.mainThread.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream fileIn = new FileInputStream("c_files/campaign.version");
                    Scanner scanner = new Scanner(fileIn);
                    Version version = new Version(scanner.nextInt(), scanner.nextInt(), scanner.nextInt());
                    Packet sendingPacket = null;
                    if (!version.equals(packet.version)) {
                        File cFilesFolder = new File("c_files");
                        File[] files = cFilesFolder.listFiles();
                        if (files != null) {
                            List<File> subFolders = new ArrayList<File>();
                            for (File file : files) {
                                if (file.isDirectory()) {
                                    subFolders.add(file);
                                }
                            }
                            List<CampaignWorld> worlds = new ArrayList<CampaignWorld>();
                            for (File subFolder : subFolders) {
                                File[] files1 = subFolder.listFiles();
                                List<PSTagCompound> levels = new ArrayList<PSTagCompound>();
                                if (files1 != null) {
                                    for (File file : files1) {
                                        DataInputStream in = new DataInputStream(new FileInputStream(file));
                                        PSTagCompound tag = new PSTagCompound();
                                        tag.readFromStream(in);
                                        levels.add(tag);
                                    }
                                }
                                worlds.add(new CampaignWorld(levels, Integer.parseInt(subFolder.getName())));
                            }
                            sendingPacket = new CampaignWorldsUpdateRequestResponseToClient(worlds, true, version);
                        }
                    }
                    if (sendingPacket == null) {
                        sendingPacket = new CampaignWorldsUpdateRequestResponseToClient(new ArrayList<CampaignWorld>(), false, version);
                    }
                    PacketHelper.sendPacket(sendingPacket, socketFrom);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
