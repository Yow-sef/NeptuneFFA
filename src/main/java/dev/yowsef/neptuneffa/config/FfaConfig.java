package dev.yowsef.neptuneffa.config;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.NeptuneFFA;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class FfaConfig {
    // Singleton
    private static final FfaConfig INSTANCE = new FfaConfig();

    private final Map<String, KitFfaSettings> kitSettings = new HashMap<>();
    private int globalRespawnDelay;
    private boolean trackNeptuneKitStats;
    private int combatTagDurationSeconds;
    private String combatTagActionbarMessage;
    private String scoreboardTitle;
    private List<String> scoreboardLines;
    // Reset warning seconds
    private List<Integer> resetWarnSeconds;

    private String menuTitle;
    private int menuSize;
    private boolean menuFillerEnabled;
    private Material menuFillerMaterial;
    private String menuFillerName;

    // Cached lobby item config values
    private boolean lobbyItemEnabled;
    private int lobbyItemSlot;
    private Material lobbyItemMaterial;
    private String lobbyItemName;
    private List<String> lobbyItemLore;

    private FfaConfig() {
        reload();
    }

    public static FfaConfig get() {
        return INSTANCE;
    }

    public static void updateConfig(String fileName) {
        File file = new File(NeptuneFFA.getInstance().getDataFolder(), fileName);
        if (!file.exists()) {
            NeptuneFFA.getInstance().saveResource(fileName, false);
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        InputStream defaultStream = NeptuneFFA.getInstance().getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!defaultConfig.isConfigurationSection(key)) {
                    if (!config.contains(key)) {
                        config.set(key, defaultConfig.get(key));
                        changed = true;
                    }
                }
            }
            if (changed) {
                try {
                    config.save(file);
                    NeptuneFFA.getInstance().getLogger().info("Updated configuration file " + fileName + " with new default values.");
                } catch (IOException e) {
                    NeptuneFFA.getInstance().getLogger().severe("Could not save updated configuration file " + fileName + ": " + e.getMessage());
                }
            }
        }
    }

    public void reload() {
        updateConfig("config.yml");
        NeptuneFFA.getInstance().saveDefaultConfig();
        NeptuneFFA.getInstance().reloadConfig();
        FileConfiguration config = NeptuneFFA.getInstance().getConfig();

        this.globalRespawnDelay = config.getInt("ffa.respawn-delay-seconds", 5);
        this.trackNeptuneKitStats = config.getBoolean("ffa.track-neptune-kit-stats", true);
        this.combatTagDurationSeconds = config.getInt("ffa.combat-tag.duration-seconds", 15);
        this.combatTagActionbarMessage = config.getString("ffa.combat-tag.actionbar-message", "&c&lCombat Tag: &e{seconds}s");
        this.scoreboardTitle = config.getString("ffa.scoreboard.title", "&c&lFFA");
        this.scoreboardLines = config.getStringList("ffa.scoreboard.lines");
        // Load reset warning seconds
        this.resetWarnSeconds = config.getIntegerList("ffa.reset-warning-seconds");
        if (this.resetWarnSeconds == null || this.resetWarnSeconds.isEmpty()) {
            this.resetWarnSeconds = List.of(60, 30, 10, 5, 4, 3, 2, 1);
        }

        this.menuTitle = config.getString("ffa.menu.title", "Free For All");
        this.menuSize = config.getInt("ffa.menu.size", 54);
        this.menuFillerEnabled = config.getBoolean("ffa.menu.filler.enabled", true);
        try {
            this.menuFillerMaterial = Material.valueOf(config.getString("ffa.menu.filler.material", "BLACK_STAINED_GLASS_PANE"));
        } catch (IllegalArgumentException e) {
            this.menuFillerMaterial = Material.BLACK_STAINED_GLASS_PANE;
        }
        this.menuFillerName = config.getString("ffa.menu.filler.name", " ");

        // Cache lobby item values
        this.lobbyItemEnabled = config.getBoolean("ffa.lobby-item.enabled", true);
        this.lobbyItemSlot = config.getInt("ffa.lobby-item.slot", 8);
        try {
            this.lobbyItemMaterial = Material.valueOf(config.getString("ffa.lobby-item.material", "DIAMOND_SWORD"));
        } catch (IllegalArgumentException e) {
            this.lobbyItemMaterial = Material.DIAMOND_SWORD;
        }
        this.lobbyItemName = config.getString("ffa.lobby-item.name", "&c&lFree For All &7(Right Click)");
        this.lobbyItemLore = config.getStringList("ffa.lobby-item.lore");

        loadKits();
    }

    private void loadKits() {
        kitSettings.clear();
        File kitsFile = new File(NeptuneFFA.getInstance().getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            NeptuneFFA.getInstance().saveResource("kits.yml", false);
        }
        FileConfiguration kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);

        if (kitsConfig.getConfigurationSection("kits") == null) return;

        boolean needsSave = false;
        for (String key : kitsConfig.getConfigurationSection("kits").getKeys(false)) {
            KitFfaSettings settings = new KitFfaSettings(key);
            settings.setEnabled(kitsConfig.getBoolean("kits." + key + ".enabled"));
            settings.setArenaName(kitsConfig.getString("kits." + key + ".arena", ""));
            settings.setWorldgen(kitsConfig.getBoolean("kits." + key + ".worldgen"));
            settings.setResetIntervalMinutes(kitsConfig.getInt("kits." + key + ".reset-interval-minutes", 10));
            settings.setRespawnDelayOverride(kitsConfig.getInt("kits." + key + ".respawn-delay-override", -1));
            settings.setGuiSlot(kitsConfig.getInt("kits." + key + ".gui-slot", -1));
            settings.setSpawnPointsRaw(kitsConfig.getStringList("kits." + key + ".spawn-points"));
            
            String path = "kits." + key + ".";
            
            if (!kitsConfig.contains(path + "broadcast-join")) {
                settings.setBroadcastJoin(true);
                needsSave = true;
            } else {
                settings.setBroadcastJoin(kitsConfig.getBoolean(path + "broadcast-join"));
            }
            
            if (!kitsConfig.contains(path + "broadcast-leave")) {
                settings.setBroadcastLeave(true);
                needsSave = true;
            } else {
                settings.setBroadcastLeave(kitsConfig.getBoolean(path + "broadcast-leave"));
            }

            if (!kitsConfig.contains(path + "heal-on-kill")) {
                settings.setHealOnKill(false);
                needsSave = true;
            } else {
                settings.setHealOnKill(kitsConfig.getBoolean(path + "heal-on-kill"));
            }

            if (!kitsConfig.contains(path + "spawn-protection-seconds")) {
                settings.setSpawnProtectionSeconds(-1);
                needsSave = true;
            } else {
                settings.setSpawnProtectionSeconds(kitsConfig.getInt(path + "spawn-protection-seconds"));
            }

            if (!kitsConfig.contains(path + "respawn-in-arena")) {
                settings.setRespawnInArena(true);
                needsSave = true;
            } else {
                settings.setRespawnInArena(kitsConfig.getBoolean(path + "respawn-in-arena"));
            }
            
            kitSettings.put(key, settings);
        }
        if (needsSave) {
            saveKits();
        }
    }

    public void saveKits() {
        File kitsFile = new File(NeptuneFFA.getInstance().getDataFolder(), "kits.yml");
        FileConfiguration kitsConfig = new YamlConfiguration();

        for (KitFfaSettings settings : kitSettings.values()) {
            String path = "kits." + settings.getKitName();
            kitsConfig.set(path + ".enabled", settings.isEnabled());
            kitsConfig.set(path + ".arena", settings.getArenaName());
            kitsConfig.set(path + ".worldgen", settings.isWorldgen());
            kitsConfig.set(path + ".reset-interval-minutes", settings.getResetIntervalMinutes());
            kitsConfig.set(path + ".respawn-delay-override", settings.getRespawnDelayOverride());
            kitsConfig.set(path + ".gui-slot", settings.getGuiSlot());
            kitsConfig.set(path + ".spawn-points", settings.getSpawnPointsRaw());
            kitsConfig.set(path + ".broadcast-join", settings.isBroadcastJoin());
            kitsConfig.set(path + ".broadcast-leave", settings.isBroadcastLeave());
            kitsConfig.set(path + ".heal-on-kill", settings.isHealOnKill());
            kitsConfig.set(path + ".spawn-protection-seconds", settings.getSpawnProtectionSeconds());
            kitsConfig.set(path + ".respawn-in-arena", settings.isRespawnInArena());
        }

        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public KitFfaSettings getKitSettings(IKit kit) {
        return kitSettings.get(kit.getName());
    }

    public KitFfaSettings getOrCreateKitSettings(IKit kit) {
        return kitSettings.computeIfAbsent(kit.getName(), k -> new KitFfaSettings(kit.getName()));
    }
}
