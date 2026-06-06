package dev.yowsef.neptuneffa.scoreboard;

import dev.lrxh.api.profile.IProfile;
import dev.yowsef.neptuneffa.config.FfaStatsManager;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.util.FormatUtil;
import org.bukkit.entity.Player;

public class FfaPlaceholders {

    public static String resolve(Player player, IProfile profile, String line, FfaSession session, FfaParticipant participant) {
        if (session == null || participant == null || profile == null) return line;

        String kit = session.getKit().getName();

        line = line.replace("<ffa_kit>", session.getKit().getDisplayName());
        line = line.replace("<ffa_arena>", session.getSettings().getArenaName());
        line = line.replace("<ffa_players>", String.valueOf(session.getParticipants().size()));
        line = line.replace("<ffa_reset_timer>", FormatUtil.formatTime(session.getResetTask().getSecondsRemaining()));

        line = line.replace("<ffa_session_kills>", String.valueOf(participant.getSessionKills()));
        line = line.replace("<ffa_session_deaths>", String.valueOf(participant.getSessionDeaths()));
        line = line.replace("<ffa_session_streak>", String.valueOf(participant.getSessionStreak()));
        line = line.replace("<ffa_session_best_streak>", String.valueOf(participant.getSessionBestStreak()));
        line = line.replace("<ffa_session_kdr>", String.format("%.2f", (double) participant.getSessionKills() / Math.max(1, participant.getSessionDeaths())));

        // Load lifetime stats
        FfaStatsManager.PlayerStats stats = FfaStatsManager.get().getStats(player.getUniqueId(), kit);
        int lifetimeKills  = stats.getKills();
        int lifetimeDeaths = stats.getDeaths();

        line = line.replace("<ffa_lifetime_kills>", String.valueOf(lifetimeKills));
        line = line.replace("<ffa_lifetime_deaths>", String.valueOf(lifetimeDeaths));
        line = line.replace("<ffa_lifetime_best_streak>", String.valueOf(stats.getBestStreak()));
        line = line.replace("<ffa_lifetime_sessions>", String.valueOf(stats.getSessions()));
        line = line.replace("<ffa_kdr>", String.format("%.2f", (double) lifetimeKills / Math.max(1, lifetimeDeaths)));

        // Load ranking details
        FfaRankingService ranking = FfaRankingService.getInstance();
        int rank        = ranking.getRank(player.getUniqueId(), kit);
        String topKiller = ranking.getTopKiller(kit);
        int topKills    = ranking.getTopKillerKills(kit);
        int killsToNext = ranking.getKillsToNextRank(player.getUniqueId(), kit);

        line = line.replace("<ffa_rank>", String.valueOf(rank));
        line = line.replace("<ffa_top_killer>", topKiller);
        line = line.replace("<ffa_top_killer_kills>", String.valueOf(topKills));
        line = line.replace("<ffa_kills_to_next_rank>", String.valueOf(killsToNext));

        line = line.replace("<ffa_ping>", String.valueOf(player.getPing()));
        line = line.replace("<ffa_time_in_session>", FormatUtil.formatTime((int) ((System.currentTimeMillis() - participant.getJoinedAt()) / 1000)));

        return line;
    }
}
