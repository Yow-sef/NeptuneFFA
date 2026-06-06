package dev.yowsef.neptuneffa.config;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.NeptuneFFA;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

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

    public void reload() {
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

        for (String key : kitsConfig.getConfigurationSection("kits").getKeys(false)) {
            KitFfaSettings settings = new KitFfaSettings(key);
            settings.setEnabled(kitsConfig.getBoolean("kits." + key + ".enabled"));
            settings.setArenaName(kitsConfig.getString("kits." + key + ".arena", ""));
            settings.setWorldgen(kitsConfig.getBoolean("kits." + key + ".worldgen"));
            settings.setResetIntervalMinutes(kitsConfig.getInt("kits." + key + ".reset-interval-minutes", 10));
            settings.setRespawnDelayOverride(kitsConfig.getInt("kits." + key + ".respawn-delay-override", -1));
            settings.setGuiSlot(kitsConfig.getInt("kits." + key + ".gui-slot", -1));
            settings.setSpawnPointsRaw(kitsConfig.getStringList("kits." + key + ".spawn-points"));
            kitSettings.put(key, settings);
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
