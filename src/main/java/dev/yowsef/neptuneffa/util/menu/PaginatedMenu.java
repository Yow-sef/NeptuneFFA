package dev.yowsef.neptuneffa.util.menu;

import dev.yowsef.neptuneffa.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class PaginatedMenu extends Menu {
    private int page = 1;

    public PaginatedMenu(String title) {
        super(title, 54);
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        Map<Integer, Button> allButtons = getAllPagesButtons(player);

        int maxPages = (int) Math.ceil(allButtons.size() / 45.0);
        if (maxPages == 0) maxPages = 1;

        int minIndex = (page - 1) * 45;
        int maxIndex = page * 45;

        for (Map.Entry<Integer, Button> entry : allButtons.entrySet()) {
            int ind = entry.getKey();
            if (ind >= minIndex && ind < maxIndex) {
                int slot = ind - minIndex;
                Button button = entry.getValue();
                button.setSlot(slot);
                buttons.put(slot, button);
            }
        }

        if (page > 1) {
            buttons.put(45, new Button(45) {
                @Override
                public ItemStack getItemStack(Player p) {
                    return new ItemBuilder(Material.ARROW).name("&aPrevious Page").build();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    page--;
                    open(p);
                }
            });
        }

        if (page < maxPages) {
            buttons.put(53, new Button(53) {
                @Override
                public ItemStack getItemStack(Player p) {
                    return new ItemBuilder(Material.ARROW).name("&aNext Page").build();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    page++;
                    open(p);
                }
            });
        }

        return buttons;
    }

    public abstract Map<Integer, Button> getAllPagesButtons(Player player);
}
