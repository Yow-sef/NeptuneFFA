package dev.yowsef.neptuneffa.menu.player;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FfaKitButton extends Button {
    private final IKit kit;

    public FfaKitButton(int slot, IKit kit) {
        super(slot);
        this.kit = kit;
    }

    @Override
    public ItemStack getItemStack(Player player) {
        FfaSession session = FfaSessionService.getInstance().getSession(kit.getName());
        if (session == null || !session.isOpen()) {
            return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .name("&7" + kit.getDisplayName())
                    .lore("&cThis session is closed.")
                    .build();
        }

        boolean playing = session.getParticipant(player.getUniqueId()) != null;
        
        List<String> lore = new ArrayList<>();
        if (playing) {
            lore.add("&c[Currently Playing]");
            lore.add("");
            lore.add("&7Right-click to leave");
        } else {
            lore.add("&fPlayers: &a" + session.getParticipants().size());
            lore.add("&fArena: &e" + session.getSettings().getArenaName());
            lore.add("&fReset in: &e" + FormatUtil.formatTime(session.getResetTask().getSecondsRemaining()));
            lore.add("");
            lore.add("&7Left-click to join");
        }

        return new ItemBuilder(kit.getIcon())
                .name("&c&l" + kit.getDisplayName())
                .lore(lore)
                .build();
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        FfaSession session = FfaSessionService.getInstance().getSession(kit.getName());
        if (session == null || !session.isOpen()) return;

        boolean playing = session.getParticipant(player.getUniqueId()) != null;

        if (clickType.isLeftClick() && !playing) {
            dev.lrxh.api.profile.IProfile profile = API.getProfile(player.getUniqueId());
            if (profile != null && !API.isInLobby(profile)) {
                player.sendMessage("§cYou must be in the lobby to join FFA.");
                player.closeInventory();
                return;
            }
            player.closeInventory();
            session.addPlayer(player);
        } else if (clickType.isRightClick() && playing) {
            player.closeInventory();
            session.removePlayer(player.getUniqueId(), "&cYou left FFA.", true);
        }
    }
}
