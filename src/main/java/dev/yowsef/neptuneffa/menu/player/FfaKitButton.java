package dev.yowsef.neptuneffa.menu.player;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.MessagesConfig;
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
                    .name(MessagesConfig.MENU_KIT_CLOSED_NAME.replace("{kit}", kit.getDisplayName()))
                    .lore(MessagesConfig.MENU_KIT_CLOSED_LORE)
                    .build();
        }

        boolean playing = session.getParticipant(player.getUniqueId()) != null;
        
        List<String> lore = new ArrayList<>();
        if (playing) {
            lore.addAll(MessagesConfig.MENU_KIT_PLAYING_LORE);
        } else {
            String players = String.valueOf(session.getParticipants().size());
            String arena = session.getSettings().getArenaName();
            String reset = FormatUtil.formatTime(session.getResetTask().getSecondsRemaining());
            for (String line : MessagesConfig.MENU_KIT_JOIN_LORE) {
                lore.add(line.replace("{players}", players).replace("{arena}", arena).replace("{reset}", reset));
            }
        }

        return new ItemBuilder(kit.getIcon())
                .name(MessagesConfig.MENU_KIT_NAME.replace("{kit}", kit.getDisplayName()))
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
                FormatUtil.sendMessage(player, MessagesConfig.FFA_MUST_BE_IN_LOBBY);
                player.closeInventory();
                return;
            }
            player.closeInventory();
            session.addPlayer(player);
        } else if (clickType.isRightClick() && playing) {
            player.closeInventory();
            // same rule as the command block, no leaving mid fight through the menu
            FfaParticipant participant = session.getParticipant(player.getUniqueId());
            if (participant.isCombatTagged() && !player.hasPermission("neptuneffa.admin")) {
                FormatUtil.sendMessage(player, MessagesConfig.COMBAT_NO_LEAVE);
                return;
            }
            session.removePlayer(player.getUniqueId(), MessagesConfig.FFA_LEFT, true);
        }
    }
}
