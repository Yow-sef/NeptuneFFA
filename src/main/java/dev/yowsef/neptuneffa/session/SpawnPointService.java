package dev.yowsef.neptuneffa.session;

import dev.lrxh.api.arena.IArena;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SpawnPointService {
    private static final SpawnPointService INSTANCE = new SpawnPointService();
    private final Map<String, Integer> roundRobinIndex = new HashMap<>();
    private final Random random = new Random();

    private SpawnPointService() {}

    public static SpawnPointService get() {
        return INSTANCE;
    }

    /**
     * Get spawn location.
     */
    public Location getSpawn(KitFfaSettings settings) {
        return getSpawn(settings, List.of());
    }

    /**
     * Get spawn location using cached random spawns.
     */
    public Location getSpawn(KitFfaSettings settings, List<Location> cachedRandomSpawns) {
        IArena arena = settings.resolveArena();
        if (arena == null || !arena.isEnabled() || !arena.isSetup()) {
            return org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation(); // fallback
        }

        List<Location> points = settings.resolveSpawnPoints();
        if (!points.isEmpty()) {
            return nextRoundRobin(settings.getKitName(), points);
        }

        // Use cached randoms if available
        if (!cachedRandomSpawns.isEmpty()) {
            return cachedRandomSpawns.get(random.nextInt(cachedRandomSpawns.size()));
        }

        // Fallback to live scan, then the old fallback spot
        Location scanned = randomInBounds(arena);
        if (scanned != null) return scanned;
        return arena.getMin() != null && arena.getMin().getWorld() != null ? arena.getMin().clone().add(0, 2, 0) : null;
    }

    /**
     * Build cache of random spawn locations.
     */
    public List<Location> buildSpawnCache(KitFfaSettings settings, int count) {
        IArena arena = settings.resolveArena();
        List<Location> result = new ArrayList<>();
        if (arena == null || !arena.isEnabled() || !arena.isSetup()) return result;

        int attempts = 0;
        while (result.size() < count && attempts++ < count * 10) {
            Location loc = randomInBounds(arena);
            if (loc != null) result.add(loc);
        }
        return result;
    }

    private Location nextRoundRobin(String kitName, List<Location> points) {
        int index = roundRobinIndex.getOrDefault(kitName, 0);
        Location loc = points.get(index % points.size());
        roundRobinIndex.put(kitName, index + 1);
        return loc;
    }

    private Location randomInBounds(IArena arena) {
        if (arena == null) return null;
        Location min = arena.getMin();
        Location max = arena.getMax();
        if (min == null || max == null || min.getWorld() == null) return null;

        // pick a block inside the bounds. the old version added 0.5 to a random double, which could
        // land half a block outside the arena and onMove would teleport the player again and again
        int minX = Math.min(min.getBlockX(), max.getBlockX()), maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY()), maxY = Math.max(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ()), maxZ = Math.max(min.getBlockZ(), max.getBlockZ());
        int x = minX + random.nextInt(maxX - minX + 1);
        int z = minZ + random.nextInt(maxZ - minZ + 1);

        // stand on y+1, so start low enough that the player is still inside the bounds
        for (int y = maxY - 1; y >= minY; y--) {
            Location loc = new Location(min.getWorld(), x, y, z);
            if (isSafe(loc)) {
                return loc.add(0.5, 1, 0.5);
            }
        }
        return null;
    }

    private boolean isSafe(Location loc) {
        return loc.getBlock().getType().isSolid() &&
                loc.clone().add(0, 1, 0).getBlock().getType() == Material.AIR &&
                loc.clone().add(0, 2, 0).getBlock().getType() == Material.AIR;
    }
}
