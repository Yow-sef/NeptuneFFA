package dev.yowsef.neptuneffa.scoreboard;

import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.config.FfaStatsManager;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FfaPlaceholderExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "neptuneffa";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Yowsef";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return null;
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) return null;

        // Determine if a kit parameter is specified at the end of params (e.g. session_kills_nodebuff)
        String stat = params.toLowerCase();
        IKit targetKit = null;

        if (API.isAvailable()) {
            java.util.Collection<IKit> kits = API.get().getKitService().getAllKits();
            for (IKit kit : kits) {
                String kitSuffix = "_" + kit.getName().toLowerCase();
                if (stat.endsWith(kitSuffix)) {
                    targetKit = kit;
                    stat = stat.substring(0, stat.length() - kitSuffix.length());
                    break;
                }
            }
        }

        // If no kit was specified as suffix, fall back to the player's active FFA session kit
        FfaSession session = FfaSessionService.getInstance().getSession(player);
        if (targetKit == null) {
            if (session != null) {
                targetKit = session.getKit();
            }
        } else {
            // If kit suffix was specified, fetch the running session for that kit
            session = FfaSessionService.getInstance().getSession(targetKit.getName());
        }

        FfaParticipant participant = null;
        if (session != null) {
            participant = session.getParticipant(player.getUniqueId());
        }

        switch (stat) {
            case "kit":
                return targetKit != null ? targetKit.getDisplayName() : "None";
            case "arena":
                return (session != null) ? session.getSettings().getArenaName() : "None";
            case "players":
                return (session != null) ? String.valueOf(session.getParticipants().size()) : "0";
            case "reset_timer":
                return (session != null && session.getResetTask() != null) 
                        ? FormatUtil.formatTime(session.getResetTask().getSecondsRemaining()) : "00:00";
            case "session_kills":
                return participant != null ? String.valueOf(participant.getSessionKills()) : "0";
            case "session_deaths":
                return participant != null ? String.valueOf(participant.getSessionDeaths()) : "0";
            case "session_streak":
                return participant != null ? String.valueOf(participant.getSessionStreak()) : "0";
            case "session_best_streak":
                return participant != null ? String.valueOf(participant.getSessionBestStreak()) : "0";
            case "session_kdr":
                if (participant == null) return "0.00";
                return String.format("%.2f", (double) participant.getSessionKills() / Math.max(1, participant.getSessionDeaths()));
            case "lifetime_kills":
                if (targetKit == null) return "0";
                return String.valueOf(FfaStatsManager.get().getStats(player.getUniqueId(), targetKit.getName()).getKills());
            case "lifetime_deaths":
                if (targetKit == null) return "0";
                return String.valueOf(FfaStatsManager.get().getStats(player.getUniqueId(), targetKit.getName()).getDeaths());
            case "lifetime_best_streak":
                if (targetKit == null) return "0";
                return String.valueOf(FfaStatsManager.get().getStats(player.getUniqueId(), targetKit.getName()).getBestStreak());
            case "lifetime_sessions":
                if (targetKit == null) return "0";
                return String.valueOf(FfaStatsManager.get().getStats(player.getUniqueId(), targetKit.getName()).getSessions());
            case "kdr":
                if (targetKit == null) return "0.00";
                FfaStatsManager.PlayerStats stats = FfaStatsManager.get().getStats(player.getUniqueId(), targetKit.getName());
                return String.format("%.2f", (double) stats.getKills() / Math.max(1, stats.getDeaths()));
            case "rank":
                if (targetKit == null) return "0";
                return String.valueOf(FfaRankingService.getInstance().getRank(player.getUniqueId(), targetKit.getName()));
            case "top_killer":
                if (targetKit == null) return "None";
                return FfaRankingService.getInstance().getTopKiller(targetKit.getName());
            case "top_killer_kills":
                if (targetKit == null) return "0";
                return String.valueOf(FfaRankingService.getInstance().getTopKillerKills(targetKit.getName()));
            case "kills_to_next_rank":
                if (targetKit == null) return "0";
                return String.valueOf(FfaRankingService.getInstance().getKillsToNextRank(player.getUniqueId(), targetKit.getName()));
            case "ping":
                return String.valueOf(player.getPing());
            case "time_in_session":
                return participant != null 
                        ? FormatUtil.formatTime((int) ((System.currentTimeMillis() - participant.getJoinedAt()) / 1000)) : "00:00";
            default:
                return null;
        }
    }
}
