package dev.yowsef.neptuneffa.listener;

import dev.lrxh.api.events.MatchReadyEvent;
import dev.lrxh.api.events.QueueJoinEvent;
import dev.lrxh.api.match.participant.IParticipant;
import dev.lrxh.api.profile.IProfile;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;

public class FfaPlayerListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FfaSession session = FfaSessionService.getInstance().getSession(player);
        if (session == null) return;

        FfaParticipant p = session.getParticipant(player.getUniqueId());

        // Handle combat log directly to avoid starting respawn task.
        // Goes through the same recordDeath as a normal kill so neptune kit data / persistent data get it too
        if (p != null && p.isCombatTagged() && !p.isInRespawnCountdown()) {
            Player killer = p.getValidAttacker() != null
                    ? Bukkit.getPlayer(p.getValidAttacker()) : null;

            if (killer != null && session.getParticipant(killer.getUniqueId()) != null) {
                session.recordDeath(p, killer);
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
            FormatUtil.sendMessage(player, MessagesConfig.FFA_CANT_QUEUE);
        }
    }

    // Catch-all for matches that would pull someone out of FFA. The command checks below only
    // see /duel and /queue typed by the player themself, not party queues, GUI accepts, aliases etc.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMatchReady(MatchReadyEvent event) {
        List<Player> inFfa = new ArrayList<>();
        for (IParticipant participant : event.getMatch().getParticipants()) {
            Player player = Bukkit.getPlayer(participant.getPlayerUUID());
            if (player != null && FfaSessionService.getInstance().getSession(player) != null) {
                inFfa.add(player);
            }
        }
        if (inFfa.isEmpty()) return;

        event.setCancelled(true);
        for (IParticipant participant : event.getMatch().getParticipants()) {
            Player player = Bukkit.getPlayer(participant.getPlayerUUID());
            if (player == null) continue;
            FormatUtil.sendMessage(player,
                    inFfa.contains(player) ? MessagesConfig.FFA_CANT_DUEL : MessagesConfig.FFA_TARGET_IN_FFA);
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
                FormatUtil.sendMessage(player, MessagesConfig.FFA_CANT_QUEUE);
                return;
            }
        }

        if (label.equals("duel") || label.equals("1v1")) {
            // Check if sender is in FFA
            if (FfaSessionService.getInstance().getSession(player) != null) {
                event.setCancelled(true);
                FormatUtil.sendMessage(player, MessagesConfig.FFA_CANT_DUEL);
                return;
            }

            if (parts.length > 1) {
                String sub = parts[1].toLowerCase();
                if (sub.equals("accept-uuid") || sub.equals("deny-uuid")) {
                    // Check if the sender of the duel request is in FFA
                    if (parts.length > 2) {
                        try {
                            java.util.UUID senderUuid = java.util.UUID.fromString(parts[2]);
                            Player sender = Bukkit.getPlayer(senderUuid);
                            if (sender != null && FfaSessionService.getInstance().getSession(sender) != null) {
                                event.setCancelled(true);
                                FormatUtil.sendMessage(player, MessagesConfig.FFA_TARGET_IN_FFA);
                                return;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                } else if (sub.equals("specific")) {
                    // Format: /duel specific <player> <kit> <rounds>
                    if (parts.length > 2) {
                        Player target = Bukkit.getPlayerExact(parts[2]);
                        if (target != null && FfaSessionService.getInstance().getSession(target) != null) {
                            event.setCancelled(true);
                            FormatUtil.sendMessage(player, MessagesConfig.FFA_TARGET_IN_FFA);
                            return;
                        }
                    }
                } else {
                    // Format: /duel <player>
                    Player target = Bukkit.getPlayerExact(parts[1]);
                    if (target != null && FfaSessionService.getInstance().getSession(target) != null) {
                        event.setCancelled(true);
                        FormatUtil.sendMessage(player, MessagesConfig.FFA_TARGET_IN_FFA);
                        return;
                    }
                }
            }
        }
    }
}
