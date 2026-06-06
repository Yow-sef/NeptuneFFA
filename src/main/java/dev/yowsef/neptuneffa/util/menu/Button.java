package dev.yowsef.neptuneffa.util.menu;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@Getter @Setter
public abstract class Button {
    private int slot;

    public Button(int slot) {
        this.slot = slot;
    }

    public abstract ItemStack getItemStack(Player player);

    public void onClick(Player player, ClickType clickType) {}
}
