package dev.yowsef.neptuneffa.config;

import dev.yowsef.neptuneffa.NeptuneFFA;
import lombok.Data;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    private void writeSnapshot(Map<UUID, Map<String, PlayerStats>> snapshot) {
        // Fresh YamlConfiguration
        YamlConfiguration saveConfig = YamlConfiguration.loadConfiguration(file);
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
        try {
            saveConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Data
    public static class PlayerStats {
        private int kills;
        private int deaths;
        private int bestStreak;
        private int sessions;
    }
}
