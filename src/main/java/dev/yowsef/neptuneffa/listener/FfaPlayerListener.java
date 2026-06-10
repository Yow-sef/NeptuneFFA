package dev.yowsef.neptuneffa.listener;

import dev.lrxh.api.events.QueueJoinEvent;
import dev.lrxh.api.profile.IProfile;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.FfaStatsManager;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.scoreboard.FfaRankingService;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class FfaPlayerListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FfaSession session = FfaSessionService.getInstance().getSession(player);
        if (session == null) return;

        FfaParticipant p = session.getParticipant(player.getUniqueId());

        // Handle combat log directly to avoid starting respawn task
        if (p != null && p.isCombatTagged() && !p.isInRespawnCountdown()) {
            Player killer = p.getValidAttacker() != null
                    ? org.bukkit.Bukkit.getPlayer(p.getValidAttacker()) : null;

            if (killer != null) {
                FfaParticipant killerP = session.getParticipant(killer.getUniqueId());
                if (killerP != null) {
                    killerP.recordKill();

                    // Update FfaStatsManager for killer
                    FfaStatsManager.PlayerStats killerStats =
                            FfaStatsManager.get().getStats(killer.getUniqueId(), session.getKit().getName());
                    killerStats.setKills(killerStats.getKills() + 1);
                    if (killerP.getSessionStreak() > killerStats.getBestStreak()) {
                        killerStats.setBestStreak(killerP.getSessionStreak());
                    }

                    // Update victim deaths in FfaStatsManager
                    if (p != null) {
                        FfaStatsManager.PlayerStats victimStats =
                                FfaStatsManager.get().getStats(player.getUniqueId(), session.getKit().getName());
                        victimStats.setDeaths(victimStats.getDeaths() + 1);
                    }

                    // Update ranking
                    FfaRankingService.getInstance().update(killer.getUniqueId(), session.getKit().getName());
                }
            }
        }

        // toLobby = false because player is already disconnecting
        session.removePlayer(player.getUniqueId(), "", false);
    }

    @EventHandler
    public void onQueueJoin(QueueJoinEvent event) {
        Player player = event.getPlayer();
        IProfile profile = API.getProfile(player.getUniqueId());
        if (profile != null && profile.hasState("IN_FFA")) {
            event.setCancelled(true);
            dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(player, MessagesConfig.FFA_NOT_IN_FFA);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();
        if (message.startsWith("/")) {
            message = message.substring(1);
        }

        String[] parts = message.split("\\s+");
        if (parts.length == 0) return;

        String label = parts[0].toLowerCase();
        if (label.equals("queue") || label.equals("quickqueue")) {
            if (FfaSessionService.getInstance().getSession(player) != null) {
                event.setCancelled(true);
                dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(player, MessagesConfig.FFA_CANT_QUEUE);
                return;
            }
        }

        if (label.equals("duel") || label.equals("1v1")) {
            // Check if sender is in FFA
            if (FfaSessionService.getInstance().getSession(player) != null) {
                event.setCancelled(true);
                dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(player, MessagesConfig.FFA_CANT_DUEL);
                return;
            }

            if (parts.length > 1) {
                String sub = parts[1].toLowerCase();
                if (sub.equals("accept-uuid") || sub.equals("deny-uuid")) {
                    // Check if the sender of the duel request is in FFA
                    if (parts.length > 2) {
                        try {
                            java.util.UUID senderUuid = java.util.UUID.fromString(parts[2]);
                            Player sender = org.bukkit.Bukkit.getPlayer(senderUuid);
                            if (sender != null && FfaSessionService.getInstance().getSession(sender) != null) {
                                event.setCancelled(true);
                                dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(player, MessagesConfig.FFA_TARGET_IN_FFA);
                                return;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                } else if (sub.equals("specific")) {
                    // Format: /duel specific <player> <kit> <rounds>
                    if (parts.length > 2) {
                        Player target = org.bukkit.Bukkit.getPlayer(parts[2]);
                        if (target != null && FfaSessionService.getInstance().getSession(target) != null) {
                            event.setCancelled(true);
                            dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(player, MessagesConfig.FFA_TARGET_IN_FFA);
                            return;
                        }
                    }
                } else {
                    // Format: /duel <player>
                    Player target = org.bukkit.Bukkit.getPlayer(parts[1]);
                    if (target != null && FfaSessionService.getInstance().getSession(target) != null) {
                        event.setCancelled(true);
                        dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(player, MessagesConfig.FFA_TARGET_IN_FFA);
                        return;
                    }
                }
            }
        }
    }
}
