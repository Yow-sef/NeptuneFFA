package dev.yowsef.neptuneffa.config;

import dev.yowsef.neptuneffa.NeptuneFFA;
import lombok.Data;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class FfaStatsManager {
    // Singleton
    private static final FfaStatsManager INSTANCE = new FfaStatsManager();

    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Map<String, PlayerStats>> cache = new ConcurrentHashMap<>();

    private FfaStatsManager() {
        file = new File(NeptuneFFA.getInstance().getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FfaStatsManager get() {
        return INSTANCE;
    }

    public PlayerStats getStats(UUID uuid, String kitName) {
        Map<String, PlayerStats> playerMap = cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        // Load stats from config if not cached
        return playerMap.computeIfAbsent(kitName, k -> {
            PlayerStats stats = new PlayerStats();
            String path = uuid.toString() + "." + kitName;
            stats.setKills(config.getInt(path + ".kills", 0));
            stats.setDeaths(config.getInt(path + ".deaths", 0));
            stats.setBestStreak(config.getInt(path + ".best_streak", 0));
            stats.setSessions(config.getInt(path + ".sessions", 0));
            return stats;
        });
    }

    // Save cache to disk asynchronously
    public void saveAllAsync() {
        // Take snapshot of the current cache
        Map<UUID, Map<String, PlayerStats>> snapshot = new HashMap<>();
        for (Map.Entry<UUID, Map<String, PlayerStats>> entry : cache.entrySet()) {
            snapshot.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        // Write snapshot to disk
        Bukkit.getScheduler().runTaskAsynchronously(NeptuneFFA.getInstance(), () -> writeSnapshot(snapshot));
    }

    public void saveAllSync() {
        writeSnapshot(new HashMap<>(cache));
    }

    // synchronized: the 5 min async save and the sync save on shutdown could write the file at the same time
    private synchronized void writeSnapshot(Map<UUID, Map<String, PlayerStats>> snapshot) {
        // Fresh YamlConfiguration
        YamlConfiguration saveConfig = new YamlConfiguration();
        if (file.exists()) {
            try {
                saveConfig.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                // loadConfiguration() would hand back an empty config here and the save below
                // would then wipe everyone who isnt cached. dont touch the file in that case
                NeptuneFFA.getInstance().getLogger().severe("Could not read stats.yml, skipping save so it doesnt get wiped: " + e.getMessage());
                return;
            }
        }
        for (Map.Entry<UUID, Map<String, PlayerStats>> playerEntry : snapshot.entrySet()) {
            for (Map.Entry<String, PlayerStats> kitEntry : playerEntry.getValue().entrySet()) {
                String path = playerEntry.getKey().toString() + "." + kitEntry.getKey();
                PlayerStats stats = kitEntry.getValue();
                saveConfig.set(path + ".kills", stats.getKills());
                saveConfig.set(path + ".deaths", stats.getDeaths());
                saveConfig.set(path + ".best_streak", stats.getBestStreak());
                saveConfig.set(path + ".sessions", stats.getSessions());
            }
        }
        // write to a temp file first and swap it in, a crash mid write left a half written stats.yml
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            saveConfig.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Every stored kill count, newest value wins (cache over the file loaded at startup).
     * Used to fill the leaderboards on startup, they used to be empty until someone got a kill.
     */
    public void forEachKills(KillsConsumer consumer) {
        Map<UUID, Map<String, Integer>> all = new HashMap<>();
        for (String uuidKey : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection section = config.getConfigurationSection(uuidKey);
            if (section == null) continue;
            for (String kitName : section.getKeys(false)) {
                all.computeIfAbsent(uuid, k -> new HashMap<>()).put(kitName, section.getInt(kitName + ".kills", 0));
            }
        }
        for (Map.Entry<UUID, Map<String, PlayerStats>> entry : cache.entrySet()) {
            for (Map.Entry<String, PlayerStats> kitEntry : entry.getValue().entrySet()) {
                all.computeIfAbsent(entry.getKey(), k -> new HashMap<>()).put(kitEntry.getKey(), kitEntry.getValue().getKills());
            }
        }
        all.forEach((uuid, kits) -> kits.forEach((kitName, kills) -> consumer.accept(uuid, kitName, kills)));
    }

    public interface KillsConsumer {
        void accept(UUID uuid, String kitName, int kills);
    }

    @Data
    public static class PlayerStats {
        private int kills;
        private int deaths;
        private int bestStreak;
        private int sessions;
    }
}
