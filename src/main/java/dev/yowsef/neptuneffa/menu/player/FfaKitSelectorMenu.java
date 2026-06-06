package dev.yowsef.neptuneffa.menu.player;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FfaKitSelectorMenu extends Menu {

    public FfaKitSelectorMenu() {
        super(FormatUtil.color(FfaConfig.get().getMenuTitle()), FfaConfig.get().getMenuSize());
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        if (!API.isAvailable()) {
            return buttons;
        }

        List<IKit> kits = API.get().getKitService().getAllKits().stream()
                .filter(k -> FfaSessionService.getInstance().isKitFfaEligible(k))
                .toList();

        int size = FfaConfig.get().getMenuSize();

        // 1. Place kits with specific slots configured
        Map<IKit, Integer> autoKits = new HashMap<>();
        for (IKit kit : kits) {
            KitFfaSettings settings = FfaConfig.get().getKitSettings(kit);
            if (settings != null && settings.getGuiSlot() >= 0 && settings.getGuiSlot() < size) {
                buttons.put(settings.getGuiSlot(), new FfaKitButton(settings.getGuiSlot(), kit));
            } else {
                autoKits.put(kit, -1);
            }
        }

        // 2. Place auto-allocated kits (guiSlot == -1) in first available empty slots
        for (IKit kit : autoKits.keySet()) {
            int emptySlot = -1;
            for (int i = 0; i < size; i++) {
                if (!buttons.containsKey(i)) {
                    emptySlot = i;
                    break;
                }
            }
            if (emptySlot != -1) {
                buttons.put(emptySlot, new FfaKitButton(emptySlot, kit));
            }
        }

        // 3. Fill remaining slots with the filler item if enabled
        if (FfaConfig.get().isMenuFillerEnabled()) {
            for (int i = 0; i < size; i++) {
                if (!buttons.containsKey(i)) {
                    buttons.put(i, new Button(i) {
                        @Override
                        public ItemStack getItemStack(Player p) {
                            return new ItemBuilder(FfaConfig.get().getMenuFillerMaterial())
                                    .name(FormatUtil.color(FfaConfig.get().getMenuFillerName()))
                                    .build();
                        }

                        @Override
                        public void onClick(Player p, ClickType clickType) {
                            // Do nothing
                        }
                    });
                }
            }
        }

        return buttons;
    }
}
