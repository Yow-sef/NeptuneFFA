package dev.yowsef.neptuneffa.config;

import dev.yowsef.neptuneffa.NeptuneFFA;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessagesConfig {

    public static String FFA_JOIN;
    public static String FFA_LEAVE;
    public static String FFA_KILL;
    public static String FFA_RESET_WARN;
    public static String FFA_RESET_KICK;
    public static String FFA_RESET_OPEN;
    public static String FFA_RESPAWN;
    public static String FFA_NO_SESSION;
    public static String FFA_NOT_IN_FFA;
    public static String FFA_ALREADY_IN;

    public static void load() {
        FfaConfig.updateConfig("messages.yml");
        File file = new File(NeptuneFFA.getInstance().getDataFolder(), "messages.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        FFA_JOIN = color(config.getString("ffa-join", "&a{player} &7joined FFA &e({kit})&7."));
        FFA_LEAVE = color(config.getString("ffa-leave", "&c{player} &7left FFA &e({kit})&7."));
        FFA_KILL = color(config.getString("ffa-kill", "&c{killer} &7killed &c{victim} &8[&e{victim_session_kills} kills&8]"));
        FFA_RESET_WARN = color(config.getString("ffa-reset-warn", "&6[FFA] &eArena resets in &c{seconds}s&e!"));
        FFA_RESET_KICK = color(config.getString("ffa-reset-kick", "&6[FFA] &eArena is resetting. Returning you to lobby."));
        FFA_RESET_OPEN = color(config.getString("ffa-reset-open", "&6[FFA] &eArena &a{arena} &ehas reopened!"));
        FFA_RESPAWN = color(config.getString("ffa-respawn", "&7Respawning in &e{seconds}&7..."));
        FFA_NO_SESSION = color(config.getString("ffa-no-session", "&cNo FFA session is open for that kit."));
        FFA_NOT_IN_FFA = color(config.getString("ffa-not-in-ffa", "&cYou are not in an FFA session."));
        FFA_ALREADY_IN = color(config.getString("ffa-already-in", "&cYou are already in an FFA session."));
    }

    private static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
