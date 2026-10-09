package dev.yowsef.neptuneffa.reset;

import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.SpawnPointService;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import lombok.AllArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@AllArgsConstructor
public class FfaRespawnTask extends BukkitRunnable {
    private final FfaSession session;
    private final Player player;
    // the participant this countdown belongs to. if the player leaves and joins again
    // they get a new participant and this task must not touch them anymore
    private final FfaParticipant owner;
    private int countdown;

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        // Re-fetch participant each tick
        FfaParticipant participant = session.getParticipant(player.getUniqueId());
        if (participant != owner) {
            // Player was removed (disconnect/leave) — clean up spectator mode if still online
            if (participant == null && player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            cancel();
            return;
        }

        participant.setInRespawnCountdown(true);

        if (countdown > 0) {
            // ffa-respawn from messages.yml was loaded but never used, the title was hardcoded
            player.sendTitle("", MessagesConfig.FFA_RESPAWN.replace("{seconds}", String.valueOf(countdown)), 0, 25, 0);
            player.setGameMode(GameMode.SPECTATOR);
            countdown--;
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            // Teleport to spawn
            Location spawn = SpawnPointService.get().getSpawn(session.getSettings(), session.getCachedRandomSpawns());
            if (spawn != null) player.teleport(spawn);
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
