package dev.yowsef.neptuneffa.command;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabExecutor;

public class FfaAdminCommand implements TabExecutor {

    private void sendMessage(CommandSender sender, String message) {
        dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(sender, message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("neptuneffa.admin")) {
            sendMessage(sender, "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                new dev.yowsef.neptuneffa.menu.admin.FfaAdminMainMenu().open(player);
            } else {
                sendMessage(sender, "&cUsage: /ffaadmin [menu|reload|reset <kit>|addspawn <kit>|captureschematic <kit>]");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu":
            case "gui":
                if (!(sender instanceof Player player)) {
                    sendMessage(sender, "&cPlayers only.");
                    return true;
                }
                new dev.yowsef.neptuneffa.menu.admin.FfaAdminMainMenu().open(player);
                break;
            case "reload":
                FfaConfig.get().reload();
                dev.yowsef.neptuneffa.config.MessagesConfig.load();
                FfaSessionService.getInstance().rebuildAll();
                sendMessage(sender, "&aNeptuneFFA reloaded and sessions rebuilt.");
                break;
            case "reset":
                if (args.length < 2) {
                    sendMessage(sender, "&cUsage: /ffaadmin reset <kit>");
                    return true;
                }
                FfaSession session = FfaSessionService.getInstance().getSession(args[1]);
                if (session != null && session.getResetTask() != null) {
                    session.getResetTask().setSecondsRemaining(0);
                    sendMessage(sender, "&aForcing reset for kit: " + args[1]);
                } else {
                    sendMessage(sender, "&cNo active session for kit: " + args[1]);
                }
                break;
            case "addspawn":
                if (!(sender instanceof Player player)) {
                    sendMessage(sender, "&cPlayers only.");
                    return true;
                }
                if (args.length < 2) {
                    sendMessage(sender, "&cUsage: /ffaadmin addspawn <kit>");
                    return true;
                }
                if (!API.isAvailable()) {
                    sendMessage(sender, "&cNeptune API is not available.");
                    return true;
                }
                IKit kit = API.get().getKitService().getAllKits().stream()
                        .filter(k -> k.getName().equalsIgnoreCase(args[1]))
                        .findFirst().orElse(null);
                if (kit == null) {
                    sendMessage(sender, "&cKit not found.");
                    return true;
                }
                KitFfaSettings settings = FfaConfig.get().getOrCreateKitSettings(kit);
                settings.getSpawnPointsRaw().add(KitFfaSettings.serializeLocation(player.getLocation()));
                // Invalidate spawn cache
                settings.invalidateSpawnCache();
                FfaConfig.get().saveKits();
                sendMessage(sender, "&aAdded spawn point for " + kit.getName());
                break;
            case "captureschematic":
                if (args.length < 2) {
                    sendMessage(sender, "&cUsage: /ffaadmin captureschematic <kit>");
                    return true;
                }
                if (!API.isAvailable()) {
                    sendMessage(sender, "&cNeptune API is not available.");
                    return true;
                }
                IKit schematicKit = API.get().getKitService().getAllKits().stream()
                        .filter(k -> k.getName().equalsIgnoreCase(args[1]))
                        .findFirst().orElse(null);
                if (schematicKit == null) {
                    sendMessage(sender, "&cKit not found.");
                    return true;
                }
                KitFfaSettings schematicSettings = FfaConfig.get().getOrCreateKitSettings(schematicKit);
                dev.lrxh.api.arena.IArena schematicArena = schematicSettings.resolveArena();
                if (schematicArena == null || !schematicArena.isSetup() || !schematicArena.isEnabled()) {
                    sendMessage(sender, "&cArena for this kit is not configured or enabled.");
                    return true;
                }
                dev.yowsef.neptuneffa.util.FfaArenaRestorer.captureAndSave(schematicArena);
                sendMessage(sender, "&aCapturing and saving clean schematic for arena: " + schematicArena.getName());
                break;
            default:
                sendMessage(sender, "&cUnknown subcommand.");
                break;
        }

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> completions = new java.util.ArrayList<>();
        if (!sender.hasPermission("neptuneffa.admin")) {
            return completions;
        }
        if (args.length == 1) {
            java.util.List<String> subs = java.util.List.of("menu", "gui", "reload", "reset", "addspawn", "captureschematic");
            org.bukkit.util.StringUtil.copyPartialMatches(args[0], subs, completions);
        } else if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("addspawn") || args[0].equalsIgnoreCase("captureschematic")) && API.isAvailable()) {
                java.util.List<String> kits = API.get().getKitService().getAllKits().stream()
                        .map(dev.lrxh.api.kit.IKit::getName)
                        .toList();
                org.bukkit.util.StringUtil.copyPartialMatches(args[1], kits, completions);
            }
        }
        java.util.Collections.sort(completions);
        return completions;
    }
}
