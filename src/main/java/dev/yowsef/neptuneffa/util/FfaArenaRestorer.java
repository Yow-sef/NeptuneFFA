package dev.yowsef.neptuneffa.util;

import dev.lrxh.api.arena.IArena;
import dev.yowsef.neptuneffa.NeptuneFFA;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FfaArenaRestorer {

    public static boolean isFaweAvailable() {
        return Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    public static File getSchematicFile(String arenaName) {
        File folder = new File(NeptuneFFA.getInstance().getDataFolder(), "schematics");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, arenaName + ".schem");
    }

    public static void captureAndSave(IArena arena) {
        if (arena == null || arena.getMin() == null || arena.getMax() == null) return;
        if (!isFaweAvailable()) return;

        String arenaName = arena.getName();
        File file = getSchematicFile(arenaName);
        Location min = arena.getMin();
        Location max = arena.getMax();
        World world = min.getWorld();

        Bukkit.getScheduler().runTaskAsynchronously(NeptuneFFA.getInstance(), () -> {
            try {
                com.sk89q.worldedit.regions.CuboidRegion region = new com.sk89q.worldedit.regions.CuboidRegion(
                        com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world),
                        com.sk89q.worldedit.math.BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ()),
                        com.sk89q.worldedit.math.BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ())
                );
                com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard clipboard = new com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard(region);
                clipboard.setOrigin(region.getMinimumPoint());

                synchronized (FfaArenaRestorer.class) {
                    try (com.sk89q.worldedit.EditSession source = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))) {
                        com.sk89q.worldedit.function.operation.ForwardExtentCopy copy = new com.sk89q.worldedit.function.operation.ForwardExtentCopy(source, region, clipboard, region.getMinimumPoint());
                        copy.setCopyingEntities(false);
                        com.sk89q.worldedit.function.operation.Operations.complete(copy);
                    }
                }

                com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat format = null;
                if (file.exists()) {
                    format = com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(file);
                }
                if (format == null) {
                    // Fall back to Sponge schematic format explicitly
                    format = com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat.SPONGE_SCHEMATIC;
                }
                try (FileOutputStream fos = new FileOutputStream(file);
                     com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter writer = format.getWriter(fos)) {
                    writer.write(clipboard);
                }
                NeptuneFFA.getInstance().getLogger().info("Successfully captured and saved clean schematic for arena: " + arenaName);
            } catch (Exception e) {
                NeptuneFFA.getInstance().getLogger().severe("Failed to capture schematic for arena: " + arenaName);
                e.printStackTrace();
            }
        });
    }

    public static boolean restoreFromSchematic(IArena arena) {
        if (arena == null || arena.getMin() == null || arena.getMax() == null) return false;
        if (!isFaweAvailable()) return false;

        String arenaName = arena.getName();
        File file = getSchematicFile(arenaName);
        if (!file.exists()) return false;

        Location min = arena.getMin();
        World world = min.getWorld();

        try {
            com.sk89q.worldedit.extent.clipboard.Clipboard clipboard;
            com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat format =
                    com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(file);
            if (format == null) {
                NeptuneFFA.getInstance().getLogger().severe(
                    "Cannot determine schematic format for file: " + file.getName() + " — skipping restore for arena: " + arenaName);
                return false;
            }
            try (FileInputStream fis = new FileInputStream(file);
                 com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader = format.getReader(fis)) {
                clipboard = reader.read();
            }

            if (clipboard == null) return false;

            synchronized (FfaArenaRestorer.class) {
                try (com.sk89q.worldedit.EditSession target = com.sk89q.worldedit.WorldEdit.getInstance().newEditSessionBuilder()
                        .world(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))
                        .changeSetNull()
                        .fastMode(true)
                        .checkMemory(false)
                        .limitUnlimited()
                        .build()) {
                        // Use arena.getMin() directly — never trust clipboard.getOrigin() after disk round-trip
                        com.sk89q.worldedit.math.BlockVector3 pasteTarget = com.sk89q.worldedit.math.BlockVector3.at(
                            min.getBlockX(), min.getBlockY(), min.getBlockZ()
                        );
                        try (com.sk89q.worldedit.session.ClipboardHolder holder = new com.sk89q.worldedit.session.ClipboardHolder(clipboard)) {
                            com.sk89q.worldedit.function.operation.Operation op = holder
                                .createPaste(target)
                                .to(pasteTarget)
                                .ignoreAirBlocks(false)
                                .copyEntities(false)
                                .build();
                            com.sk89q.worldedit.function.operation.Operations.complete(op);
                        }
                }
            }
            NeptuneFFA.getInstance().getLogger().info("Successfully restored clean schematic for arena: " + arenaName);
            return true;
        } catch (Exception e) {
            NeptuneFFA.getInstance().getLogger().severe("Failed to restore schematic for arena: " + arenaName);
            e.printStackTrace();
            return false;
        }
    }

    public static void clearEntities(IArena arena) {
        if (arena == null) return;
        Location min = arena.getMin();
        Location max = arena.getMax();
        if (min == null || max == null || min.getWorld() == null) return;

        World world = min.getWorld();
        double minX = Math.min(min.getX(), max.getX());
        double maxX = Math.max(min.getX(), max.getX()) + 1.0;
        double minY = Math.min(min.getY(), max.getY());
        double maxY = Math.max(min.getY(), max.getY()) + 1.0;
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxZ = Math.max(min.getZ(), max.getZ()) + 1.0;

        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double cz = (minZ + maxZ) / 2.0;
        double rx = (maxX - minX) / 2.0;
        double ry = (maxY - minY) / 2.0;
        double rz = (maxZ - minZ) / 2.0;

        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(new Location(world, cx, cy, cz), rx, ry, rz)) {
            if (entity instanceof org.bukkit.entity.Player) continue;

            if (entity instanceof org.bukkit.entity.EnderCrystal
                    || entity instanceof org.bukkit.entity.Item
                    || entity instanceof org.bukkit.entity.Projectile
                    || entity instanceof org.bukkit.entity.TNTPrimed
                    || entity instanceof org.bukkit.entity.FallingBlock
                    || entity instanceof org.bukkit.entity.AreaEffectCloud) {
                entity.remove();
            }
        }
    }
}
