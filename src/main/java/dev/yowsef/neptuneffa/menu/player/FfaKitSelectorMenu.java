package dev.yowsef.neptuneffa.menu.player;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.PaginatedMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FfaKitSelectorMenu extends PaginatedMenu {

    public FfaKitSelectorMenu() {
        super(FormatUtil.color(FfaConfig.get().getMenuTitle()), FfaConfig.get().getMenuSize());
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        if (!API.isAvailable()) {
            return buttons;
        }

        List<IKit> kits = API.get().getKitService().getAllKits().stream()
                .filter(k -> FfaSessionService.getInstance().isKitFfaEligible(k))
                .toList();

        // 1. Place kits with specific slots configured
        Map<IKit, Integer> autoKits = new HashMap<>();
        for (IKit kit : kits) {
            KitFfaSettings settings = FfaConfig.get().getKitSettings(kit);
            if (settings != null && settings.getGuiSlot() >= 0) {
                buttons.put(settings.getGuiSlot(), new FfaKitButton(settings.getGuiSlot(), kit));
            } else {
                autoKits.put(kit, -1);
            }
        }

        // 2. Place auto-allocated kits (guiSlot == -1) in first available empty slots
        for (IKit kit : autoKits.keySet()) {
            int emptySlot = -1;
            for (int i = 0; ; i++) {
                if (!buttons.containsKey(i)) {
                    emptySlot = i;
                    break;
                }
            }
            buttons.put(emptySlot, new FfaKitButton(emptySlot, kit));
        }

        return buttons;
    }

    @Override
    public boolean isFillerEnabled() {
        return FfaConfig.get().isMenuFillerEnabled();
    }
}
