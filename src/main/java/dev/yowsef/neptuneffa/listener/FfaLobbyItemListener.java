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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class FfaLobbyItemListener implements Listener {

    public FfaLobbyItemListener() {
        // Periodic check to ensure lobby item is present or removed
        Bukkit.getScheduler().runTaskTimer(NeptuneFFA.getInstance(), () -> {
            int slot = dev.yowsef.neptuneffa.config.FfaConfig.get().getLobbyItemSlot();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (shouldHaveLobbyItem(player)) {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (item == null || !isLobbyItem(item)) {
                        giveLobbyItem(player);
                    }
                } else {
                    removeLobbyItem(player);
                }
            }
        }, 40L, 10L); 
    }

    public static boolean shouldHaveLobbyItem(Player player) {
        dev.yowsef.neptuneffa.config.FfaConfig cfg = dev.yowsef.neptuneffa.config.FfaConfig.get();
        if (!cfg.isLobbyItemEnabled()) return false;

        IProfile profile = API.getProfile(player.getUniqueId());
        if (profile == null) return false;

        // Must be in lobby
        if (!API.isInLobby(profile)) return false;

        // Must NOT be in the kit editor state
        String state = profile.getProfileState();
        if (state != null && (state.equalsIgnoreCase("neptune:in_kiteditor") || state.equalsIgnoreCase("IN_KIT_EDITOR"))) {
            return false;
        }

        // Must NOT be in a Neptune kit setup procedure
        if (isInsideKitProcedure(profile)) {
            return false;
        }

        return true;
    }

    private static boolean isInsideKitProcedure(IProfile profile) {
        try {
            Object kitProcedure = profile.getClass().getMethod("getKitProcedure").invoke(profile);
            if (kitProcedure != null) {
                Object type = kitProcedure.getClass().getMethod("getType").invoke(kitProcedure);
                if (type != null && !type.toString().equalsIgnoreCase("NONE")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null || item.getType() == Material.AIR) return;

            if (isLobbyItem(item) && shouldHaveLobbyItem(event.getPlayer())) {
                new FfaKitSelectorMenu().open(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(NeptuneFFA.getInstance(), () -> {
            Player player = event.getPlayer();
            if (shouldHaveLobbyItem(player)) {
                giveLobbyItem(player);
            } else {
                removeLobbyItem(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTaskLater(NeptuneFFA.getInstance(), () -> {
            Player player = event.getPlayer();
            if (!player.isOnline()) return;
            if (shouldHaveLobbyItem(player)) {
                giveLobbyItem(player);
            } else {
                removeLobbyItem(player);
            }
        }, 2L);
    }

    public static void giveLobbyItem(Player player) {
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

    public static void removeLobbyItem(Player player) {
        boolean changed = false;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isLobbyItem(item)) {
                player.getInventory().setItem(i, null);
                changed = true;
            }
        }
        ItemStack cursor = player.getItemOnCursor();
        if (isLobbyItem(cursor)) {
            player.setItemOnCursor(null);
            changed = true;
        }
        if (changed) {
            player.updateInventory();
        }
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (isLobbyItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
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
