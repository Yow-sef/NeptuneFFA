package dev.yowsef.neptuneffa.config;

import dev.yowsef.neptuneffa.NeptuneFFA;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    public static String FFA_CANT_DUEL;
    public static String FFA_TARGET_IN_FFA;
    public static String FFA_CANT_QUEUE;
    public static String FFA_LEFT;
    public static String FFA_DIED_LOBBY;
    public static String FFA_MUST_BE_IN_LOBBY;
    public static String FFA_ARENA_NOT_CONFIGURED;
    public static String FFA_ARENA_NO_SPAWN;
    public static String COMBAT_NO_COMMANDS;
    public static String COMBAT_NO_LEAVE;
    public static String PLAYERS_ONLY;
    public static String PLAYER_NOT_FOUND;
    public static String NEPTUNE_UNAVAILABLE;
    public static String FFA_USAGE;
    public static String FFA_JOIN_USAGE;
    public static String FFA_LIST_HEADER;
    public static String FFA_LIST_ENTRY;
    public static String STATS_NO_PERMISSION;
    public static String STATS_SEPARATOR;
    public static String STATS_HEADER;
    public static String STATS_KIT;
    public static String STATS_KILLS;
    public static String STATS_STREAK;
    public static String ADMIN_NO_PERMISSION;
    public static String ADMIN_USAGE;
    public static String ADMIN_RESET_USAGE;
    public static String ADMIN_ADDSPAWN_USAGE;
    public static String ADMIN_CAPTURE_USAGE;
    public static String ADMIN_UNKNOWN_SUBCOMMAND;
    public static String ADMIN_KIT_NOT_FOUND;
    public static String ADMIN_RELOADED;
    public static String ADMIN_RESET_FORCED;
    public static String ADMIN_NO_ACTIVE_SESSION;
    public static String ADMIN_SPAWN_ADDED_KIT;
    public static String ADMIN_ARENA_NOT_READY;
    public static String ADMIN_CAPTURING;
    public static String ADMIN_SPAWN_ADDED;
    public static String ADMIN_SPAWN_REMOVED;
    public static String ADMIN_SPAWN_TELEPORTED;
    public static String MENU_KIT_NAME;
    public static String MENU_KIT_CLOSED_NAME;
    public static List<String> MENU_KIT_CLOSED_LORE;
    public static List<String> MENU_KIT_PLAYING_LORE;
    public static List<String> MENU_KIT_JOIN_LORE;

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
        FFA_CANT_DUEL = color(config.getString("ffa-cant-duel", "&cYou cannot send or accept duels while in FFA!"));
        FFA_TARGET_IN_FFA = color(config.getString("ffa-target-in-ffa", "&cThat player is currently in FFA!"));
        FFA_CANT_QUEUE = color(config.getString("ffa-cant-queue", "&cYou cannot queue while in FFA!"));
        FFA_LEFT = color(config.getString("ffa-left", "&cYou left FFA."));
        FFA_DIED_LOBBY = color(config.getString("ffa-died-lobby", "&cYou died. Use the FFA menu to rejoin."));
        FFA_MUST_BE_IN_LOBBY = color(config.getString("ffa-must-be-in-lobby", "&cYou must be in the lobby to join FFA."));
        FFA_ARENA_NOT_CONFIGURED = color(config.getString("ffa-arena-not-configured", "&cThis FFA arena is not configured. Contact an admin."));
        FFA_ARENA_NO_SPAWN = color(config.getString("ffa-arena-no-spawn", "&cThis FFA arena has no valid spawn configured. Contact an admin."));
        COMBAT_NO_COMMANDS = color(config.getString("combat-no-commands", "&cYou cannot use commands while in combat!"));
        COMBAT_NO_LEAVE = color(config.getString("combat-no-leave", "&cYou cannot leave while in combat!"));
        PLAYERS_ONLY = color(config.getString("players-only", "&cThis command is for players only."));
        PLAYER_NOT_FOUND = color(config.getString("player-not-found", "&cPlayer not found."));
        NEPTUNE_UNAVAILABLE = color(config.getString("neptune-unavailable", "&cNeptune API is not available."));
        FFA_USAGE = color(config.getString("ffa-usage", "&cUsage: /ffa [leave|join <kit>|list|stats [player]]"));
        FFA_JOIN_USAGE = color(config.getString("ffa-join-usage", "&cUsage: /ffa join <kit>"));
        FFA_LIST_HEADER = color(config.getString("ffa-list-header", "&c&lFFA Sessions:"));
        FFA_LIST_ENTRY = color(config.getString("ffa-list-entry", "&7- &e{kit} &7({players} players)"));
        STATS_NO_PERMISSION = color(config.getString("stats-no-permission", "&cYou do not have permission to view others' stats."));
        STATS_SEPARATOR = color(config.getString("stats-separator", "&7&m--------------------"));
        STATS_HEADER = color(config.getString("stats-header", "&c&lFFA Stats: &e{player}"));
        STATS_KIT = color(config.getString("stats-kit", "&fKit: &c{kit}"));
        STATS_KILLS = color(config.getString("stats-kills", "  &7Kills: &a{kills} &7| Deaths: &c{deaths} &7| KDR: &b{kdr}"));
        STATS_STREAK = color(config.getString("stats-streak", "  &7Best Streak: &6{best_streak} &7| Sessions: &e{sessions}"));
        ADMIN_NO_PERMISSION = color(config.getString("admin-no-permission", "&cNo permission."));
        ADMIN_USAGE = color(config.getString("admin-usage", "&cUsage: /ffaadmin [menu|reload|reset <kit>|addspawn <kit>|captureschematic <kit>]"));
        ADMIN_RESET_USAGE = color(config.getString("admin-reset-usage", "&cUsage: /ffaadmin reset <kit>"));
        ADMIN_ADDSPAWN_USAGE = color(config.getString("admin-addspawn-usage", "&cUsage: /ffaadmin addspawn <kit>"));
        ADMIN_CAPTURE_USAGE = color(config.getString("admin-capture-usage", "&cUsage: /ffaadmin captureschematic <kit>"));
        ADMIN_UNKNOWN_SUBCOMMAND = color(config.getString("admin-unknown-subcommand", "&cUnknown subcommand."));
        ADMIN_KIT_NOT_FOUND = color(config.getString("admin-kit-not-found", "&cKit not found."));
        ADMIN_RELOADED = color(config.getString("admin-reloaded", "&aNeptuneFFA reloaded and sessions rebuilt."));
        ADMIN_RESET_FORCED = color(config.getString("admin-reset-forced", "&aForcing reset for kit: {kit}"));
        ADMIN_NO_ACTIVE_SESSION = color(config.getString("admin-no-active-session", "&cNo active session for kit: {kit}"));
        ADMIN_SPAWN_ADDED_KIT = color(config.getString("admin-spawn-added-kit", "&aAdded spawn point for {kit}"));
        ADMIN_ARENA_NOT_READY = color(config.getString("admin-arena-not-ready", "&cArena for this kit is not configured or enabled."));
        ADMIN_CAPTURING = color(config.getString("admin-capturing", "&aCapturing and saving clean schematic for arena: {arena}"));
        ADMIN_SPAWN_ADDED = color(config.getString("admin-spawn-added", "&aSpawn point added."));
        ADMIN_SPAWN_REMOVED = color(config.getString("admin-spawn-removed", "&cSpawn point removed."));
        ADMIN_SPAWN_TELEPORTED = color(config.getString("admin-spawn-teleported", "&aTeleported to spawn #{number}"));
        MENU_KIT_NAME = color(config.getString("menu-kit-name", "&c&l{kit}"));
        MENU_KIT_CLOSED_NAME = color(config.getString("menu-kit-closed-name", "&7{kit}"));
        MENU_KIT_CLOSED_LORE = colorList(config, "menu-kit-closed-lore", List.of("&cThis session is closed."));
        MENU_KIT_PLAYING_LORE = colorList(config, "menu-kit-playing-lore", List.of("&c[Currently Playing]", "", "&7Right-click to leave"));
        MENU_KIT_JOIN_LORE = colorList(config, "menu-kit-join-lore", List.of("&fPlayers: &a{players}", "&fArena: &e{arena}", "&fReset in: &e{reset}", "", "&7Left-click to join"));
    }

    private static List<String> colorList(FileConfiguration config, String key, List<String> def) {
        List<String> raw = config.contains(key) ? config.getStringList(key) : def;
        List<String> out = new ArrayList<>();
        for (String line : raw) out.add(color(line));
        return out;
    }

    private static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
