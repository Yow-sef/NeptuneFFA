package dev.yowsef.neptuneffa.util.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Menu menu) {
            event.setCancelled(true);
            
            if (event.getClickedInventory() != event.getInventory()) {
                return;
            }
            
            if (event.getWhoClicked() instanceof Player player) {
                Button button = menu.getButton(event.getSlot());
                if (button != null) {
                    button.onClick(player, event.getClick());
                }
            }
        }
    }
}
