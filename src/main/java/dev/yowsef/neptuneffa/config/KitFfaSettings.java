package dev.yowsef.neptuneffa.config;

import dev.lrxh.api.arena.IArena;
import dev.yowsef.neptuneffa.API;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

@Data
public class KitFfaSettings {
    private final String kitName;
    private boolean enabled;
    private String arenaName = "";
    private boolean worldgen;
    private int resetIntervalMinutes = 10;
    private int respawnDelayOverride = -1;
    private int guiSlot = -1;
    private List<String> spawnPointsRaw = new ArrayList<>();

    private transient IArena cachedArena;
    private transient List<Location> cachedSpawnPoints;

    public IArena resolveArena() {
        if (cachedArena != null) return cachedArena;
        if (arenaName.isEmpty()) return null;
        cachedArena = API.get().getArenaService().getAllArenas().stream()
                .filter(a -> a.getName().equalsIgnoreCase(arenaName))
                .findFirst()
                .orElse(null);
        return cachedArena;
    }

    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
        this.cachedArena = null; // Invalidate cache
    }

    public List<Location> resolveSpawnPoints() {
        if (cachedSpawnPoints != null) return cachedSpawnPoints;
        List<Location> locations = new ArrayList<>();
        for (String raw : spawnPointsRaw) {
            String[] parts = raw.split(",");
            if (parts.length >= 6) {
                try {
                    locations.add(new Location(
                            Bukkit.getWorld(parts[0]),
                            Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]),
                            Double.parseDouble(parts[3]),
                            Float.parseFloat(parts[4]),
                            Float.parseFloat(parts[5])
                    ));
                } catch (NumberFormatException ignored) {}
            }
        }
        cachedSpawnPoints = locations;
        return locations;
    }

    public void invalidateSpawnCache() {
        this.cachedSpawnPoints = null;
    }

    public static String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }
}
