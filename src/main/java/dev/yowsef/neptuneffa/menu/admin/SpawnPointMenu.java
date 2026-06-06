package dev.yowsef.neptuneffa.menu.admin;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.PaginatedMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnPointMenu extends PaginatedMenu {
    private final IKit kit;

    public SpawnPointMenu(IKit kit) {
        super("Spawns: " + kit.getName());
        this.kit = kit;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        KitFfaSettings settings = FfaConfig.get().getOrCreateKitSettings(kit);
        List<Location> spawns = settings.resolveSpawnPoints();
        List<String> rawSpawns = settings.getSpawnPointsRaw();

        buttons.put(0, new Button(0) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.EMERALD)
                        .name("&aAdd Spawn Here")
                        .lore("&7Click to add your current", "&7location as a spawn point.")
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                rawSpawns.add(KitFfaSettings.serializeLocation(p.getLocation()));
                // Invalidate spawn cache
                settings.invalidateSpawnCache();
                FfaConfig.get().saveKits();
                dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(p, "&aSpawn point added.");
                open(p); // Refresh
            }
        });

        int index = 1;
        for (int i = 0; i < spawns.size(); i++) {
            final int listIndex = i;
            final Location loc = spawns.get(i);
            
            buttons.put(index++, new Button(0) {
                @Override
                public ItemStack getItemStack(Player p) {
                    return new ItemBuilder(Material.ENDER_PEARL)
                            .name("&eSpawn #" + (listIndex + 1))
                            .lore(
                                    "&7World: &f" + loc.getWorld().getName(),
                                    "&7X: &f" + String.format("%.2f", loc.getX()),
                                    "&7Y: &f" + String.format("%.2f", loc.getY()),
                                    "&7Z: &f" + String.format("%.2f", loc.getZ()),
                                    "",
                                    "&aLeft-Click to teleport",
                                    "&cRight-Click to remove"
                            )
                            .build();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    if (clickType.isLeftClick()) {
                        p.teleport(loc);
                        dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(p, "&aTeleported to spawn #" + (listIndex + 1));
                    } else if (clickType.isRightClick()) {
                        rawSpawns.remove(listIndex);
                        // Invalidate spawn cache
                        settings.invalidateSpawnCache();
                        FfaConfig.get().saveKits();
                        dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(p, "&cSpawn point removed.");
                        open(p);
                    }
                }
            });
        }

        return buttons;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = super.getButtons(player);
        buttons.put(49, new Button(49) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.BARRIER).name("&cBack").build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new KitSettingsMenu(kit).open(p);
            }
        });
        return buttons;
    }
}
