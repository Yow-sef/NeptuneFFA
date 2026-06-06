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

                try (com.sk89q.worldedit.EditSession source = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))) {
                    com.sk89q.worldedit.function.operation.ForwardExtentCopy copy = new com.sk89q.worldedit.function.operation.ForwardExtentCopy(source, region, clipboard, region.getMinimumPoint());
                    copy.setCopyingEntities(false);
                    com.sk89q.worldedit.function.operation.Operations.complete(copy);
                }

                try (com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter writer = 
                        com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
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
            try (com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader = 
                    com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(file).getReader(new FileInputStream(file))) {
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
                    com.sk89q.worldedit.function.operation.Operations.complete(new com.sk89q.worldedit.session.ClipboardHolder(clipboard)
                            .createPaste(target)
                            .to(clipboard.getOrigin())
                            .ignoreAirBlocks(false)
                            .copyEntities(false)
                            .build());
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
}
