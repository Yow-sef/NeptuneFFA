package dev.yowsef.neptuneffa.listener;

import dev.lrxh.api.profile.IProfile;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.NeptuneFFA;
import dev.yowsef.neptuneffa.menu.player.FfaKitSelectorMenu;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class FfaLobbyItemListener implements Listener {

    public FfaLobbyItemListener() {
        // Periodic check to ensure lobby item is present
        Bukkit.getScheduler().runTaskTimer(NeptuneFFA.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                IProfile profile = API.getProfile(player.getUniqueId());
                if (profile != null && API.isInLobby(profile)) {
                    // Get slot from config
                    int slot = dev.yowsef.neptuneffa.config.FfaConfig.get().getLobbyItemSlot();
                    ItemStack item = player.getInventory().getItem(slot);
                    if (item == null || !isLobbyItem(item)) {
                        giveLobbyItem(player);
                    }
                }
            }
        }, 0L, 5L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null || item.getType() == Material.AIR) return;

            if (isLobbyItem(item)) {
                new FfaKitSelectorMenu().open(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(NeptuneFFA.getInstance(), () -> {
            Player player = event.getPlayer();
            IProfile profile = API.getProfile(player.getUniqueId());
            if (profile != null && API.isInLobby(profile)) {
                giveLobbyItem(player);
            }
        }, 1L);
    }

    // Re-give lobby item when player returns
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTaskLater(NeptuneFFA.getInstance(), () -> {
            Player player = event.getPlayer();
            if (!player.isOnline()) return;
            IProfile profile = API.getProfile(player.getUniqueId());
            if (profile != null && API.isInLobby(profile)) {
                giveLobbyItem(player);
            }
        }, 2L);
    }

    public static void giveLobbyItem(Player player) {
        // Read config values
        dev.yowsef.neptuneffa.config.FfaConfig cfg = dev.yowsef.neptuneffa.config.FfaConfig.get();
        if (!cfg.isLobbyItemEnabled()) return;

        int slot = cfg.getLobbyItemSlot();
        Material material = cfg.getLobbyItemMaterial();
        String name = cfg.getLobbyItemName();
        List<String> lore = cfg.getLobbyItemLore();

        ItemStack item = new ItemBuilder(material).name(name).lore(lore).build();

        // Tag item with PDC key
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey ffaKey = new NamespacedKey(NeptuneFFA.getInstance(), "ffa_lobby_item");
            meta.getPersistentDataContainer().set(ffaKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        player.getInventory().setItem(slot, item);
    }

    // Check if item has PDC tag
    public static boolean isLobbyItem(ItemStack item) {
        if (item == null) return false;
        NamespacedKey ffaKey = new NamespacedKey(NeptuneFFA.getInstance(), "ffa_lobby_item");
        return item.hasItemMeta() &&
               item.getItemMeta().getPersistentDataContainer().has(ffaKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (isLobbyItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (isLobbyItem(event.getCurrentItem()) || isLobbyItem(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            int hotkeySlot = event.getHotbarButton();
            if (hotkeySlot >= 0) {
                ItemStack hotkeyItem = event.getWhoClicked().getInventory().getItem(hotkeySlot);
                if (isLobbyItem(hotkeyItem)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
