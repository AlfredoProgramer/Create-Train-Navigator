package de.mrjulsen.crn.client;

import java.util.function.Supplier;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlayScreen;
import de.mrjulsen.crn.client.gui.screen.LoadingScreen;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.RouteOverlaySettingsScreen;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public class ClientWrapper {
    
    public static void showNavigatorGui(Level level) {
        Minecraft.getInstance().setScreen(new LoadingScreen());
        GlobalSettingsManager.syncToClient(() -> {
            ClientTrainStationSnapshot.syncToClient(() -> {
                Minecraft.getInstance().setScreen(new NavigatorScreen(level));
            });
        });
    }

    public static void showRouteOverlaySettingsGui(RouteDetailsOverlayScreen overlay) {
        Minecraft.getInstance().setScreen(new RouteOverlaySettingsScreen(overlay));
    }

    public static void handleErrorMessagePacket(ServerErrorPacket packet, Supplier<NetworkEvent.Context> ctx) {        
        Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, Constants.TEXT_SERVER_ERROR, Utils.text(packet.message)));   
    }
}
