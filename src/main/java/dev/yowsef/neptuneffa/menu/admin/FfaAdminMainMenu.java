package dev.yowsef.neptuneffa.menu.admin;

import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class FfaAdminMainMenu extends Menu {

    public FfaAdminMainMenu() {
        super("FFA Admin Menu", 9);
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(2, new Button(2) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.DIAMOND_SWORD)
                        .name("&cKit Settings")
                        .lore("&7Configure FFA settings for each kit.")
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new KitListAdminMenu().open(p);
            }
        });

        buttons.put(4, new Button(4) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.BEACON)
                        .name("&eLive Sessions")
                        .lore("&7View and manage active FFA sessions.")
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new SessionOverviewMenu().open(p);
            }
        });

        buttons.put(6, new Button(6) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.PAPER)
                        .name("&aReload Config")
                        .lore("&7Hot-reload config.yml and kits.yml.")
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                FfaConfig.get().reload();
                FfaSessionService.getInstance().rebuildAll();
                p.sendMessage("§aConfig and sessions reloaded.");
                p.closeInventory();
            }
        });

        return buttons;
    }
}
