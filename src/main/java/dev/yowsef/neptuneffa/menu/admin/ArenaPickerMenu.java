package dev.yowsef.neptuneffa.menu.admin;

import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaPickerMenu extends PaginatedMenu {
    private final IKit kit;

    public ArenaPickerMenu(IKit kit) {
        super("Assign Arena: " + kit.getName());
        this.kit = kit;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<IArena> arenas = API.get().getArenaService().getAllArenas().stream()
                .filter(IArena::isSetup)
                .toList();

        KitFfaSettings settings = FfaConfig.get().getOrCreateKitSettings(kit);

        int index = 0;
        for (IArena arena : arenas) {
            boolean isSelected = settings.getArenaName().equalsIgnoreCase(arena.getName());
            // two kits on one arena means one kits reset pastes over the other kits players
            String usedBy = isSelected ? null : findKitUsing(arena);

            buttons.put(index++, new Button(0) {
                @Override
                public ItemStack getItemStack(Player p) {
                    if (usedBy != null) {
                        return new ItemBuilder(Material.BARRIER)
                                .name("&c" + arena.getDisplayName())
                                .lore("&7Already used by FFA kit &c" + usedBy)
                                .build();
                    }
                    return new ItemBuilder(isSelected ? Material.MAP : Material.PAPER)
                            .name((isSelected ? "&a" : "&e") + arena.getDisplayName())
                            .lore(isSelected ? "&7Currently selected" : "&7Click to select")
                            .build();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    if (usedBy != null) return;
                    settings.setArenaName(arena.getName());
                    FfaConfig.get().saveKits();
                    FfaSessionService.getInstance().rebuild(kit);
                    new KitSettingsMenu(kit).open(p);
                }
            });
        }

        return buttons;
    }

    private String findKitUsing(IArena arena) {
        for (KitFfaSettings other : FfaConfig.get().getKitSettings().values()) {
            if (other.getKitName().equalsIgnoreCase(kit.getName())) continue;
            if (other.getArenaName().equalsIgnoreCase(arena.getName())) {
                return other.getKitName();
            }
        }
        return null;
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
