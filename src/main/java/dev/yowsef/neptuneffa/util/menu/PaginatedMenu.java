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

    // Default size 54 slots
    public PaginatedMenu(String title) {
        this(title, 54);
    }

    public PaginatedMenu(String title, int size) {
        super(title, size);
    }

    // Content slots = all rows except the last one (navigation row)
    private int getContentSlots() {
        return getSize() - 9;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        Map<Integer, Button> allButtons = getAllPagesButtons(player);

        int contentSlots = getContentSlots();

        // Calculate total pages from all buttons
        int maxPages = allButtons.isEmpty() ? 1
                : (int) Math.ceil((double) allButtons.size() / contentSlots);

        // Clamp current page
        if (page > maxPages) page = maxPages;
        if (page < 1) page = 1;

        int minIndex = (page - 1) * contentSlots;
        int maxIndex = page * contentSlots;

        // Slice the correct page range from all buttons
        for (Map.Entry<Integer, Button> entry : allButtons.entrySet()) {
            int index = entry.getKey();
            if (index >= minIndex && index < maxIndex) {
                int slot = index - minIndex;
                Button button = entry.getValue();
                button.setSlot(slot);
                buttons.put(slot, button);
            }
        }
        // previous page
        int prevSlot = getSize() - 9;
        // Next page
        int nextSlot = getSize() - 1;

        if (page > 1) {
            buttons.put(prevSlot, new Button(prevSlot) {
                @Override
                public ItemStack getItemStack(Player p) {
                    return new ItemBuilder(Material.ARROW)
                            .name("&aPrevious Page")
                            .lore("&7Page " + (page - 1) + " of " + maxPages)
                            .build();
                }
                @Override
                public void onClick(Player p, ClickType clickType) {
                    page--;
                    open(p);
                }
            });
        }

        if (page < maxPages) {
            buttons.put(nextSlot, new Button(nextSlot) {
                @Override
                public ItemStack getItemStack(Player p) {
                    return new ItemBuilder(Material.ARROW)
                            .name("&aNext Page")
                            .lore("&7Page " + (page + 1) + " of " + maxPages)
                            .build();
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
