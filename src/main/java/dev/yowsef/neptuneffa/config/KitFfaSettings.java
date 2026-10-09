package dev.yowsef.neptuneffa.config;

import dev.lrxh.api.arena.IArena;
import dev.yowsef.neptuneffa.API;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

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
    private boolean broadcastJoin = true;
    private boolean broadcastLeave = true;
    private boolean healOnKill = false;
    private int spawnProtectionSeconds = -1;
    private boolean respawnInArena = true;
    private boolean rekitOnKill = false;

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
            Location loc = parseLocation(raw);
            if (loc != null) locations.add(loc);
        }
        cachedSpawnPoints = locations;
        return locations;
    }

    // null if malformed or the world isnt loaded (a null world location blew up the join + spawn menu)
    public static Location parseLocation(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split(",");
        if (parts.length < 6) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(
                    world,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void invalidateSpawnCache() {
        this.cachedSpawnPoints = null;
    }

    public static String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }
}
