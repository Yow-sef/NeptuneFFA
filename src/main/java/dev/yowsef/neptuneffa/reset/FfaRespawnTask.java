package dev.yowsef.neptuneffa.reset;

import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.SpawnPointService;
import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@AllArgsConstructor
public class FfaRespawnTask extends BukkitRunnable {
    private final FfaSession session;
    private final Player player;
    private int countdown;

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        // Re-fetch participant each tick
        FfaParticipant participant = session.getParticipant(player.getUniqueId());
        if (participant == null) {
            // Player was removed (disconnect/leave) — clean up spectator mode if still online
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            cancel();
            return;
        }

        participant.setInRespawnCountdown(true);

        if (countdown > 0) {
            player.sendTitle("", ChatColor.GRAY + "Respawning in " + ChatColor.YELLOW + countdown + ChatColor.GRAY + "...", 0, 25, 0);
            player.setGameMode(GameMode.SPECTATOR);
            countdown--;
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            // Teleport to spawn
            player.teleport(SpawnPointService.get().getSpawn(session.getSettings(), session.getCachedRandomSpawns()));
            session.getKit().giveLoadout(player.getUniqueId());
            dev.yowsef.neptuneffa.API.applyShieldPatterns(dev.yowsef.neptuneffa.API.getProfile(player.getUniqueId()), player);

            // Apply spawn protection if configured for this kit
            int protectionSecs = session.getSettings().getSpawnProtectionSeconds();
            if (protectionSecs > 0) {
                participant.applySpawnProtection(protectionSecs);
            }

            participant.setInRespawnCountdown(false);
            cancel();
        }
    }
}
