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
}
