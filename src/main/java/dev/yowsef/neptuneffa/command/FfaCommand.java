package dev.yowsef.neptuneffa.command;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.menu.player.FfaKitSelectorMenu;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import org.bukkit.Bukkit;
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
            sendMessage(sender, MessagesConfig.PLAYERS_ONLY);
            return true;
        }

        if (args.length == 0) {
            new FfaKitSelectorMenu().open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                if (args.length < 2) {
                    sendMessage(player, MessagesConfig.FFA_JOIN_USAGE);
                    return true;
                }

                dev.lrxh.api.profile.IProfile joinProfile = API.getProfile(player.getUniqueId());
                if (joinProfile != null && !API.isInLobby(joinProfile)) {
                    sendMessage(player, MessagesConfig.FFA_MUST_BE_IN_LOBBY);
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
                FfaParticipant participant = currentSession.getParticipant(player.getUniqueId());
                if (participant != null && participant.isCombatTagged() && !player.hasPermission("neptuneffa.admin")) {
                    sendMessage(player, MessagesConfig.COMBAT_NO_LEAVE);
                    return true;
                }
                currentSession.removePlayer(player.getUniqueId(), MessagesConfig.FFA_LEFT, true);
                break;
            case "list":
                sendMessage(player, MessagesConfig.FFA_LIST_HEADER);
                for (FfaSession s : FfaSessionService.getInstance().getSessions()) {
                    if (s.isOpen()) {
                        sendMessage(player, MessagesConfig.FFA_LIST_ENTRY
                                .replace("{kit}", s.getKit().getDisplayName())
                                .replace("{players}", String.valueOf(s.getParticipants().size())));
                    }
                }
                break;
            case "stats":
                Player target = player;
                if (args.length > 1) {
                    if (!player.hasPermission("neptuneffa.stats.others")) {
                        sendMessage(player, MessagesConfig.STATS_NO_PERMISSION);
                        return true;
                    }
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sendMessage(player, MessagesConfig.PLAYER_NOT_FOUND);
                        return true;
                    }
                }
                sendMessage(player, MessagesConfig.STATS_SEPARATOR);
                if (!API.isAvailable()) {
                    sendMessage(player, MessagesConfig.NEPTUNE_UNAVAILABLE);
                    sendMessage(player, MessagesConfig.STATS_SEPARATOR);
                    break;
                }
                sendMessage(player, MessagesConfig.STATS_HEADER.replace("{player}", target.getName()));
                for (IKit kit : API.get().getKitService().getAllKits()) {
                    if (FfaSessionService.getInstance().isKitFfaEligible(kit)) {
                        dev.yowsef.neptuneffa.config.FfaStatsManager.PlayerStats stats = dev.yowsef.neptuneffa.config.FfaStatsManager.get().getStats(target.getUniqueId(), kit.getName());
                        if (stats.getSessions() > 0) {
                            sendMessage(player, MessagesConfig.STATS_KIT.replace("{kit}", kit.getDisplayName()));
                            sendMessage(player, MessagesConfig.STATS_KILLS
                                    .replace("{kills}", String.valueOf(stats.getKills()))
                                    .replace("{deaths}", String.valueOf(stats.getDeaths()))
                                    .replace("{kdr}", String.format("%.2f", (double) stats.getKills() / Math.max(1, stats.getDeaths()))));
                            sendMessage(player, MessagesConfig.STATS_STREAK
                                    .replace("{best_streak}", String.valueOf(stats.getBestStreak()))
                                    .replace("{sessions}", String.valueOf(stats.getSessions())));
                        }
                    }
                }
                sendMessage(player, MessagesConfig.STATS_SEPARATOR);
                break;
            default:
                sendMessage(player, MessagesConfig.FFA_USAGE);
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
                java.util.List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(org.bukkit.entity.Player::getName)
                        .toList();
                org.bukkit.util.StringUtil.copyPartialMatches(args[1], players, completions);
            }
        }
        java.util.Collections.sort(completions);
        return completions;
    }
}
