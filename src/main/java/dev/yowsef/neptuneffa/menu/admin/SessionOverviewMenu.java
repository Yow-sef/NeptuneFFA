package dev.yowsef.neptuneffa.menu.admin;

import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SessionOverviewMenu extends PaginatedMenu {

    public SessionOverviewMenu() {
        super("Live Sessions");
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        int index = 0;
        for (FfaSession session : FfaSessionService.getInstance().getSessions()) {
            if (!session.isOpen()) continue;

            buttons.put(index++, new Button(0) {
                @Override
                public ItemStack getItemStack(Player p) {
                    return new ItemBuilder(session.getKit().getIcon())
                            .name("&e" + session.getKit().getDisplayName())
                            .lore(
                                    "&7Players: &a" + session.getParticipants().size(),
                                    "&7Arena: &e" + session.getSettings().getArenaName(),
                                    "&7Reset in: &c" + FormatUtil.formatTime(session.getResetTask().getSecondsRemaining()),
                                    "&7Worldgen: " + (session.getSettings().isWorldgen() ? "&aON" : "&cOFF"),
                                    "",
                                    "&cRight-Click to force reset"
                            )
                            .build();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    if (clickType.isRightClick()) {
                        session.getResetTask().setSecondsRemaining(0);
                        dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(p, "&aForced immediate reset for " + session.getKit().getName());
                        p.closeInventory();
                    }
                }
            });
        }

        return buttons;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = super.getButtons(player);
        buttons.put(49, new Button(49) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.BARRIER).name("&cBack").build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new FfaAdminMainMenu().open(p);
            }
        });
        return buttons;
    }
}
