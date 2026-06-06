package dev.yowsef.neptuneffa.session;

import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FfaCombatTagTask extends BukkitRunnable {
    @Override
    public void run() {
        for (FfaSession session : FfaSessionService.getInstance().getSessions()) {
            if (!session.isOpen()) continue;
            for (FfaParticipant p : session.getParticipants()) {
                if (p.isCombatTagged() && !p.isInRespawnCountdown()) {
                    Player player = Bukkit.getPlayer(p.getUuid());
                    if (player != null) {
                        long remaining = (p.getCombatTagRemaining() / 1000) + 1; // +1 to avoid 0s
                        String msg = FfaConfig.get().getCombatTagActionbarMessage()
                                .replace("{seconds}", String.valueOf(remaining));
                        FormatUtil.sendActionBar(player, msg);
                    }
                }
            }
        }
    }
}
