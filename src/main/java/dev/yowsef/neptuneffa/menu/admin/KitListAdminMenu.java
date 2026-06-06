package dev.yowsef.neptuneffa.menu.admin;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class KitListAdminMenu extends PaginatedMenu {

    public KitListAdminMenu() {
        super("Kit Settings");
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        if (!API.isAvailable()) {
            return buttons;
        }

        int index = 0;
        for (IKit kit : API.get().getKitService().getAllKits()) {
            buttons.put(index++, new Button(0) {
                @Override
                public ItemStack getItemStack(Player p) {
                    return new ItemBuilder(kit.getIcon())
                            .name("&e" + kit.getDisplayName())
                            .lore("&7Click to edit FFA settings")
                            .build();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    new KitSettingsMenu(kit).open(p);
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
                new FfaAdminMainMenu().open(p);
            }
        });
        return buttons;
    }
}
