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

        buttons.put(20, new Button(20) {
            @Override
            public ItemStack getItemStack(Player p) {
                Material mat = settings.isBroadcastJoin()
                        ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                return new ItemBuilder(mat)
                        .name("&eBroadcast Join Message")
                        .lore(
                            "&7Current: " + (settings.isBroadcastJoin() ? "&aON" : "&cOFF"),
                            "",
                            "&7When ON, a message is sent to the session",
                            "&7when a player joins.",
                            "",
                            "&7Click to toggle"
                        )
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                settings.setBroadcastJoin(!settings.isBroadcastJoin());
                FfaConfig.get().saveKits();
                open(p);
            }
        });

        buttons.put(22, new Button(22) {
            @Override
            public ItemStack getItemStack(Player p) {
                Material mat = settings.isBroadcastLeave()
                        ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                return new ItemBuilder(mat)
                        .name("&eBroadcast Leave Message")
                        .lore(
                            "&7Current: " + (settings.isBroadcastLeave() ? "&aON" : "&cOFF"),
                            "",
                            "&7When ON, a message is sent to the session",
                            "&7when a player leaves.",
                            "",
                            "&7Click to toggle"
                        )
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                settings.setBroadcastLeave(!settings.isBroadcastLeave());
                FfaConfig.get().saveKits();
                open(p);
            }
        });

        buttons.put(24, new Button(24) {
            @Override
            public ItemStack getItemStack(Player p) {
                Material mat = settings.isHealOnKill()
                        ? Material.GOLDEN_APPLE : Material.APPLE;
                return new ItemBuilder(mat)
                        .name("&eHeal on Kill")
                        .lore(
                            "&7Current: " + (settings.isHealOnKill() ? "&aON" : "&cOFF"),
                            "",
                            "&7When ON, the killer's health, food,",
                            "&7and saturation are fully restored",
                            "&7after getting a kill.",
                            "",
                            "&7Click to toggle"
                        )
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                settings.setHealOnKill(!settings.isHealOnKill());
                FfaConfig.get().saveKits();
                open(p);
            }
        });

        buttons.put(26, new Button(26) {
            @Override
            public ItemStack getItemStack(Player p) {
                Material mat = settings.isRekitOnKill()
                        ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD;
                return new ItemBuilder(mat)
                        .name("&eRekit on Kill")
                        .lore(
                            "&7Current: " + (settings.isRekitOnKill() ? "&aON" : "&cOFF"),
                            "",
                            "&7When ON, the killer instantly receives",
                            "&7a fresh kit with full durability after",
                            "&7each kill.",
                            "",
                            "&7Runs before Heal on Kill.",
                            "",
                            "&7Click to toggle"
                        )
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                settings.setRekitOnKill(!settings.isRekitOnKill());
                FfaConfig.get().saveKits();
                open(p);
            }
        });

        buttons.put(34, new Button(34) {
            @Override
            public ItemStack getItemStack(Player p) {
                int secs = settings.getSpawnProtectionSeconds();
                String display = secs <= 0 ? "&cDisabled" : "&a" + secs + "s";
                return new ItemBuilder(Material.SHIELD)
                        .name("&bSpawn Protection")
                        .lore(
                            "&7Current: " + display,
                            "",
                            "&7How long a freshly respawned player",
                            "&7cannot be damaged. Attacking removes",
                            "&7the protection immediately.",
                            "",
                            "&aLeft-Click: &7+1 second",
                            "&cRight-Click: &7-1 second",
                            "&aShift+Left: &7+5 seconds",
                            "&cShift+Right: &7-5 seconds"
                        )
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                int secs = settings.getSpawnProtectionSeconds();
                int delta = clickType.isShiftClick() ? 5 : 1;
                if (clickType.isLeftClick()) secs += delta;
                else if (clickType.isRightClick()) secs -= delta;
                // Clamp: -1 = disabled, max 30 seconds
                if (secs < -1) secs = -1;
                if (secs > 30) secs = 30;
                settings.setSpawnProtectionSeconds(secs);
                FfaConfig.get().saveKits();
                open(p);
            }
        });

        buttons.put(36, new Button(36) {
            @Override
            public ItemStack getItemStack(Player p) {
                boolean inArena = settings.isRespawnInArena();
                Material mat = inArena ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
                return new ItemBuilder(mat)
                        .name("&eRespawn Location")
                        .lore(
                            "&7Current: " + (inArena ? "&aInside Arena" : "&cNeptune Lobby"),
                            "",
                            "&aInside Arena: &7Players respawn in the FFA",
                            "&7arena after a configurable countdown.",
                            "",
                            "&cNeptune Lobby: &7Players are sent back to",
                            "&7the lobby on death and must manually",
                            "&7rejoin through the FFA menu.",
                            "",
                            "&7Click to toggle"
                        )
                        .build();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                settings.setRespawnInArena(!settings.isRespawnInArena());
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
