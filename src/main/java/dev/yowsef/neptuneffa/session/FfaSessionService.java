package dev.yowsef.neptuneffa.session;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.NeptuneFFA;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FfaSessionService {
    @Getter private static FfaSessionService instance;
    private final Map<String, FfaSession> sessions = new HashMap<>();
    private final Map<UUID, FfaSession> activePlayers = new ConcurrentHashMap<>();

    public FfaSessionService() {
        instance = this;
        rebuildAll();
    }

    public void rebuildAll() {
        sessions.values().forEach(s -> s.close(""));
        sessions.clear();
        activePlayers.clear();

        if (!API.isAvailable()) {
            NeptuneFFA.getInstance().getLogger().severe("Neptune API is not available yet! FFA sessions will not be loaded. Please run '/ffaadmin reload' once Neptune is fully loaded.");
            return;
        }

        for (IKit kit : API.get().getKitService().getAllKits()) {
            rebuild(kit);
        }
    }

    public void rebuild(IKit kit) {
        // Destroy old session
        FfaSession old = sessions.remove(kit.getName());
        if (old != null) {
            old.close("");
            old.destroy();
        }

        if (isKitFfaEligible(kit)) {
            KitFfaSettings settings = FfaConfig.get().getOrCreateKitSettings(kit);
            dev.lrxh.api.arena.IArena arena = settings.resolveArena();

            if (arena != null && arena.isSetup() && arena.isEnabled()) {
                FfaSession session = new FfaSession(kit, settings);
                sessions.put(kit.getName(), session);

                java.io.File schemFile = dev.yowsef.neptuneffa.util.FfaArenaRestorer.getSchematicFile(arena.getName());
                if (schemFile.exists()) {
                    org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(NeptuneFFA.getInstance(), () -> {
                        boolean restored = dev.yowsef.neptuneffa.util.FfaArenaRestorer.restoreFromSchematic(arena);
                        org.bukkit.Bukkit.getScheduler().runTask(NeptuneFFA.getInstance(), () -> {
                            if (restored) {
                                arena.setMin(arena.getMin());
                            }
                            session.open();
                        });
                    });
                } else {
                    dev.yowsef.neptuneffa.util.FfaArenaRestorer.captureAndSave(arena);
                    session.open();
                }
                return;
            }

            FfaSession session = new FfaSession(kit, settings);
            sessions.put(kit.getName(), session);
            session.open();
        }
    }

    // Shut down all sessions
    public void shutdownAll() {
        sessions.values().forEach(s -> s.close(""));
        sessions.clear();
        activePlayers.clear();
    }

    public FfaSession getSession(String kitName) {
        return sessions.get(kitName);
    }

    public FfaSession getSession(Player player) {
        return activePlayers.get(player.getUniqueId());
    }

    public void addActivePlayer(UUID uuid, FfaSession session) {
        activePlayers.put(uuid, session);
    }

    public void removeActivePlayer(UUID uuid) {
        activePlayers.remove(uuid);
    }

    public Collection<FfaSession> getSessions() {
        return sessions.values();
    }

    /**
     * Get session by location.
     */
    public FfaSession getSessionByLocation(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (FfaSession session : sessions.values()) {
            dev.lrxh.api.arena.IArena arena = session.getArena();
            if (arena == null) continue;
            org.bukkit.Location min = arena.getMin();
            org.bukkit.Location max = arena.getMax();
            if (min == null || max == null || min.getWorld() == null) continue;
            if (!loc.getWorld().getName().equalsIgnoreCase(min.getWorld().getName())) continue;
            int minX = Math.min(min.getBlockX(), max.getBlockX());
            int maxX = Math.max(min.getBlockX(), max.getBlockX());
            int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
            int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());
            int x = loc.getBlockX(), z = loc.getBlockZ();
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) return session;
        }
        return null;
    }

    public boolean isKitFfaEligible(IKit kit) {
        if (API.kitIs(kit, "hidden")) return true;
        KitFfaSettings settings = FfaConfig.get().getKitSettings(kit);
        return settings != null && settings.isEnabled();
    }
}
