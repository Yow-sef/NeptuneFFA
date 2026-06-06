package dev.yowsef.neptuneffa.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {
    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material != null ? material : Material.DIAMOND_SWORD);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack != null ? itemStack.clone() : new ItemStack(Material.DIAMOND_SWORD);
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return this;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return this;
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(List.of(lore));
    }

    public ItemStack build() {
        return itemStack;
    }
}
