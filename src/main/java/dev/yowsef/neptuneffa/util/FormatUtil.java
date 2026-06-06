package dev.yowsef.neptuneffa.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

public class FormatUtil {

    // Cache reflection lookups
    private static final Method NEPTUNE_SEND_MSG;
    private static final Method CC_RETURN_MESSAGE;
    private static final Method PLAYER_SEND_ACTION_BAR;

    static {
        Method sendMsg = null;
        Method returnMsg = null;
        Method sendActionBar = null;
        try {
            Class<?> pu = Class.forName("dev.lrxh.neptune.utils.PlayerUtil");
            sendMsg = pu.getMethod("sendMessage", UUID.class, String.class);
        } catch (Exception e) {
            Logger.getLogger("NeptuneFFA").warning("[NeptuneFFA] PlayerUtil.sendMessage not found via reflection: " + e.getMessage());
        }
        try {
            Class<?> cc = Class.forName("dev.lrxh.neptune.utils.CC");
            returnMsg = cc.getMethod("returnMessage", String.class);
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            // Resolve Adventure API sendActionBar
            sendActionBar = org.bukkit.entity.Player.class.getMethod("sendActionBar", componentClass);
        } catch (Exception e) {
            Logger.getLogger("NeptuneFFA").warning("[NeptuneFFA] CC / Adventure reflection not found: " + e.getMessage());
        }
        NEPTUNE_SEND_MSG = sendMsg;
        CC_RETURN_MESSAGE = returnMsg;
        PLAYER_SEND_ACTION_BAR = sendActionBar;
    }

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) return;
        if (sender instanceof Player player && NEPTUNE_SEND_MSG != null) {
            try {
                NEPTUNE_SEND_MSG.invoke(null, player.getUniqueId(), message);
                return;
            } catch (Exception ignored) {}
        }
        sender.sendMessage(color(message));
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        if (CC_RETURN_MESSAGE != null && PLAYER_SEND_ACTION_BAR != null) {
            try {
                Object component = CC_RETURN_MESSAGE.invoke(null, message);
                PLAYER_SEND_ACTION_BAR.invoke(player, component);
                return;
            } catch (Exception ignored) {}
        }
        // Fallback to legacy action bar
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(color(message)));
    }
}
