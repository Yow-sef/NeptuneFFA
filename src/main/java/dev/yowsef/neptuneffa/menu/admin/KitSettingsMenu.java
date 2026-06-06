package dev.yowsef.neptuneffa.menu.admin;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.ItemBuilder;
import dev.yowsef.neptuneffa.util.menu.Button;
import dev.yowsef.neptuneffa.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class KitSettingsMenu extends Menu {
    private final IKit kit;

    public KitSettingsMenu(IKit kit) {
        super("Settings: " + kit.getName(), 54);
        this.kit = kit;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        KitFfaSettings settings = FfaConfig.get().getOrCreateKitSettings(kit);

        buttons.put(10, new Button(10) {
            @Override
            public ItemStack getItemStack(Player p) {
                if (API.kitIs(kit, "hidden")) {
                    return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                            .name("&aFFA-Only Kit")
                            .lore("&7Always visible in FFA.")
                            .build();
                } else {
                    Material mat = settings.isEnabled() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                    return new ItemBuilder(mat)
                            .name((settings.isEnabled() ? "&a" : "&c") + "FFA Enabled")
                            .lore("&7Click to toggle")
                            .build();
                }
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                if (!API.kitIs(kit, "hidden")) {
                    settings.setEnabled(!settings.isEnabled());
                    FfaConfig.get().saveKits();
                    FfaSessionService.getInstance().rebuild(kit);
                    open(p); // Refresh
                }
            }
        });

        buttons.put(12, new Button(12) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.MAP)
                        .name("&eAssign Arena")
                        .lore("&7Current: &a" + (settings.getArenaName().isEmpty() ? "None" : settings.getArenaName()), "", "&7Click to change")
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ArenaPickerMenu(kit).open(p);
            }
        });

        buttons.put(14, new Button(14) {
            @Override
            public ItemStack getItemStack(Player p) {
                Material mat = settings.isWorldgen() ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
                return new ItemBuilder(mat)
                        .name("&eToggle Worldgen")
                        .lore("&7Current: " + (settings.isWorldgen() ? "&aON" : "&cOFF"), "", "&7Click to toggle")
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                settings.setWorldgen(!settings.isWorldgen());
                FfaConfig.get().saveKits();
                open(p);
            }
        });

        buttons.put(16, new Button(16) {
            @Override
            public ItemStack getItemStack(Player p) {
                int count = settings.getSpawnPointsRaw().size();
                return new ItemBuilder(Material.ENDER_PEARL)
                        .name("&bEdit Spawn Points")
                        .lore("&7" + (count > 0 ? count + " defined" : "0 defined — using random spawn"), "", "&7Click to edit")
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new SpawnPointMenu(kit).open(p);
            }
        });

        buttons.put(28, new Button(28) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.CLOCK)
                        .name("&6Reset Timer")
                        .lore("&7Current: &e" + settings.getResetIntervalMinutes() + " minutes", "", "&7(Use /ffaadmin reset to force reset or edit kits.yml)")
                        .build();
            }
        });

        buttons.put(30, new Button(30) {
            @Override
            public ItemStack getItemStack(Player p) {
                String delayStr = settings.getRespawnDelayOverride() == -1 ? "Using global (" + FfaConfig.get().getGlobalRespawnDelay() + "s)" : settings.getRespawnDelayOverride() + "s";
                return new ItemBuilder(Material.GHAST_TEAR)
                        .name("&cRespawn Delay Override")
                        .lore("&7Current: &e" + delayStr, "", "&7(Edit kits.yml to change)")
                        .build();
            }
        });

        buttons.put(32, new Button(32) {
            @Override
            public ItemStack getItemStack(Player p) {
                int slot = settings.getGuiSlot();
                String slotStr = slot == -1 ? "Auto" : String.valueOf(slot);
                return new ItemBuilder(Material.CHEST)
                        .name("&eConfigure GUI Slot")
                        .lore(
                                "&7Current: &e" + slotStr,
                                "",
                                "&eClick to adjust:",
                                "&7Left Click: &a+1 slot",
                                "&7Right Click: &c-1 slot",
                                "&7Shift Left Click: &a+9 slots",
                                "&7Shift Right Click: &c-9 slots"
                        )
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                int slot = settings.getGuiSlot();
                int maxSlots = FfaConfig.get().getMenuSize();
                
                int increment = 1;
                if (clickType.isShiftClick()) {
                    increment = 9;
                }
                
                if (clickType.isLeftClick()) {
                    slot += increment;
                } else if (clickType.isRightClick()) {
                    slot -= increment;
                }
                
                if (slot > maxSlots - 1) {
                    slot = -1;
                } else if (slot < -1) {
                    slot = maxSlots - 1;
                }
                
                settings.setGuiSlot(slot);
                FfaConfig.get().saveKits();
                open(p);
            }
        });

        buttons.put(49, new Button(49) {
            @Override
            public ItemStack getItemStack(Player p) {
                return new ItemBuilder(Material.BARRIER).name("&cBack").build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new KitListAdminMenu().open(p);
            }
        });

        return buttons;
    }
}
