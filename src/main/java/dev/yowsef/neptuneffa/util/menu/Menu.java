package dev.yowsef.neptuneffa.util.menu;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public abstract class Menu implements InventoryHolder {
    @Getter private final String title;
    @Getter private final int size;
    private final Map<Integer, Button> buttons = new HashMap<>();
    // Store last created inventory
    private Inventory lastInventory;

    public Menu(String title, int size) {
        this.title = title;
        this.size = size;
    }

    public boolean isAutoUpdate() {
        return false;
    }

    public abstract Map<Integer, Button> getButtons(Player player);

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(this, size, title);
        // Track last opened inventory
        this.lastInventory = inventory;

        buttons.clear();
        buttons.putAll(getButtons(player));
        
        buttons.forEach((slot, button) -> {
            if (slot < size) {
                inventory.setItem(slot, button.getItemStack(player));
            }
        });

        player.openInventory(inventory);
    }

    public void update(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory.getHolder() != this) return;

        buttons.clear();
        buttons.putAll(getButtons(player));

        // clear slots that dont have a button anymore (closed session dropping out of the list
        // left its old icon behind)
        for (int slot = 0; slot < size; slot++) {
            if (!buttons.containsKey(slot)) inventory.setItem(slot, null);
        }
        
        buttons.forEach((slot, button) -> {
            if (slot < size) {
                inventory.setItem(slot, button.getItemStack(player));
            }
        });
    }

    // close every open plugin menu, used on reload so nobody keeps editing old settings objects
    public static void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
    }

    public Button getButton(int slot) {
        return buttons.get(slot);
    }

    // Return stored inventory
    @Override
    public Inventory getInventory() {
        return lastInventory;
    }
}
