package dev.yowsef.neptuneffa.scoreboard;

import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FfaRankingService {
    private static final FfaRankingService INSTANCE = new FfaRankingService();

    // Synchronized rankings cache to prevent ConcurrentModificationException
    private final Map<String, List<RankEntry>> kitRankings = new ConcurrentHashMap<>();

    private FfaRankingService() {}

    public static FfaRankingService getInstance() {
        return INSTANCE;
    }

    public void update(UUID uuid, String kitName) {
        Bukkit.getScheduler().runTaskAsynchronously(dev.yowsef.neptuneffa.NeptuneFFA.getInstance(), () -> {
            int kills = dev.yowsef.neptuneffa.config.FfaStatsManager.get().getStats(uuid, kitName).getKills();
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName();
            if (name == null) return;

            // Synchronize writes
            synchronized (kitRankings) {
                List<RankEntry> ranks = kitRankings.computeIfAbsent(kitName, k -> new ArrayList<>());
                ranks.removeIf(e -> e.uuid.equals(uuid));
                ranks.add(new RankEntry(uuid, name, kills));
                ranks.sort((a, b) -> Integer.compare(b.kills, a.kills));
            }
        });
    }

    public int getRank(UUID uuid, String kitName) {
        // Synchronize reads
        synchronized (kitRankings) {
            List<RankEntry> ranks = kitRankings.get(kitName);
            if (ranks == null) return -1;
            for (int i = 0; i < ranks.size(); i++) {
                if (ranks.get(i).uuid.equals(uuid)) return i + 1;
            }
        }
        return -1;
    }

    public String getTopKiller(String kitName) {
        // Synchronize reads
        synchronized (kitRankings) {
            List<RankEntry> ranks = kitRankings.get(kitName);
            return (ranks != null && !ranks.isEmpty()) ? ranks.get(0).name : "None";
        }
    }

    public int getTopKillerKills(String kitName) {
        // Synchronize reads
        synchronized (kitRankings) {
            List<RankEntry> ranks = kitRankings.get(kitName);
            return (ranks != null && !ranks.isEmpty()) ? ranks.get(0).kills : 0;
        }
    }

    // Get kills needed to reach the next rank
    public int getKillsToNextRank(UUID uuid, String kitName) {
        // Synchronize reads
        synchronized (kitRankings) {
            List<RankEntry> ranks = kitRankings.get(kitName);
            if (ranks == null || ranks.isEmpty()) return 1;
            int currentRank = getRankUnderLock(uuid, ranks);
            // Unranked
            if (currentRank == -1) {
                return ranks.get(ranks.size() - 1).kills + 1;
            }
            // Already #1
            if (currentRank <= 1) return 0;
            // currentRank > ranks.size() guard
            if (currentRank > ranks.size()) return 0;
            return ranks.get(currentRank - 2).kills - ranks.get(currentRank - 1).kills + 1;
        }
    }

    // Caller must hold lock
    private int getRankUnderLock(UUID uuid, List<RankEntry> ranks) {
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).uuid.equals(uuid)) return i + 1;
        }
        return -1;
    }

    public static class RankEntry {
        UUID uuid;
        String name;
        int kills;

        public RankEntry(UUID uuid, String name, int kills) {
            this.uuid = uuid;
            this.name = name;
            this.kills = kills;
        }
    }
}
