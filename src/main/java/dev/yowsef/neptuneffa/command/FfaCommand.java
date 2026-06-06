package dev.yowsef.neptuneffa.command;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.menu.player.FfaKitSelectorMenu;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabExecutor;

public class FfaCommand implements TabExecutor {

    private void sendMessage(CommandSender sender, String message) {
        dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(sender, message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "This command is for players only.");
            return true;
        }

        if (args.length == 0) {
            new FfaKitSelectorMenu().open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                if (args.length < 2) {
                    sendMessage(player, "&cUsage: /ffa join <kit>");
                    return true;
                }

                dev.lrxh.api.profile.IProfile joinProfile = API.getProfile(player.getUniqueId());
                if (joinProfile != null && !API.isInLobby(joinProfile)) {
                    sendMessage(player, "&cYou must be in the lobby to join FFA.");
                    return true;
                }

                String kitName = args[1];
                FfaSession session = FfaSessionService.getInstance().getSession(kitName);
                if (session == null || !session.isOpen()) {
                    sendMessage(player, MessagesConfig.FFA_NO_SESSION);
                    return true;
                }
                if (session.getParticipant(player.getUniqueId()) != null) {
                    sendMessage(player, MessagesConfig.FFA_ALREADY_IN);
                    return true;
                }
                session.addPlayer(player);
                break;
            case "leave":
                FfaSession currentSession = FfaSessionService.getInstance().getSession(player);
                if (currentSession == null) {
                    sendMessage(player, MessagesConfig.FFA_NOT_IN_FFA);
                    return true;
                }
                currentSession.removePlayer(player.getUniqueId(), "&cYou left FFA.", true);
                break;
            case "list":
                sendMessage(player, "&c&lFFA Sessions:");
                for (FfaSession s : FfaSessionService.getInstance().getSessions()) {
                    if (s.isOpen()) {
                        sendMessage(player, "&7- &e" + s.getKit().getDisplayName() + " &7(" + s.getParticipants().size() + " players)");
                    }
                }
                break;
            case "stats":
                Player target = player;
                if (args.length > 1) {
                    if (!player.hasPermission("neptuneffa.stats.others")) {
                        sendMessage(player, "&cYou do not have permission to view others' stats.");
                        return true;
                    }
                    target = org.bukkit.Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sendMessage(player, "&cPlayer not found.");
                        return true;
                    }
                }
                sendMessage(player, "&7&m--------------------");
                if (!API.isAvailable()) {
                    sendMessage(player, "&cNeptune API is not available.");
                    sendMessage(player, "&7&m--------------------");
                    break;
                }
                sendMessage(player, "&c&lFFA Stats: &e" + target.getName());
                for (IKit kit : API.get().getKitService().getAllKits()) {
                    if (FfaSessionService.getInstance().isKitFfaEligible(kit)) {
                        dev.yowsef.neptuneffa.config.FfaStatsManager.PlayerStats stats = dev.yowsef.neptuneffa.config.FfaStatsManager.get().getStats(target.getUniqueId(), kit.getName());
                        if (stats.getSessions() > 0) {
                            sendMessage(player, "&fKit: &c" + kit.getDisplayName());
                            sendMessage(player, "  &7Kills: &a" + stats.getKills() + " &7| Deaths: &c" + stats.getDeaths() + " &7| KDR: &b" + String.format("%.2f", (double) stats.getKills() / Math.max(1, stats.getDeaths())));
                            sendMessage(player, "  &7Best Streak: &6" + stats.getBestStreak() + " &7| Sessions: &e" + stats.getSessions());
                        }
                    }
                }
                sendMessage(player, "&7&m--------------------");
                break;
            default:
                sendMessage(player, "&cUsage: /ffa [leave|join <kit>|list|stats]");
                break;
        }

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> completions = new java.util.ArrayList<>();
        if (args.length == 1) {
            java.util.List<String> subs = java.util.List.of("join", "leave", "list", "stats");
            org.bukkit.util.StringUtil.copyPartialMatches(args[0], subs, completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") && API.isAvailable()) {
                java.util.List<String> kits = API.get().getKitService().getAllKits().stream()
                        .filter(k -> FfaSessionService.getInstance().isKitFfaEligible(k))
                        .map(dev.lrxh.api.kit.IKit::getName)
                        .toList();
                org.bukkit.util.StringUtil.copyPartialMatches(args[1], kits, completions);
            } else if (args[0].equalsIgnoreCase("stats") && sender.hasPermission("neptuneffa.stats.others")) {
                java.util.List<String> players = org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(org.bukkit.entity.Player::getName)
                        .toList();
                org.bukkit.util.StringUtil.copyPartialMatches(args[1], players, completions);
            }
        }
        java.util.Collections.sort(completions);
        return completions;
    }
}
