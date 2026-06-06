package dev.yowsef.neptuneffa.reset;

import dev.yowsef.neptuneffa.NeptuneFFA;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.session.FfaSession;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class FfaResetTask extends BukkitRunnable {
    private final FfaSession session;
    @Getter @Setter private int secondsRemaining;
    // Warning intervals
    private final Set<Integer> warnAt;

    public FfaResetTask(FfaSession session) {
        this.session = session;
        this.secondsRemaining = session.getSettings().getResetIntervalMinutes() * 60;
        // Use config warning seconds
        this.warnAt = new HashSet<>(FfaConfig.get().getResetWarnSeconds());
    }

    @Override
    public void run() {
        if (warnAt.contains(secondsRemaining)) {
            session.broadcastToSession(MessagesConfig.FFA_RESET_WARN
                    .replace("{seconds}", String.valueOf(secondsRemaining)));
        }

        if (secondsRemaining <= 0) {
            doReset();
            cancel();
            return;
        }
        secondsRemaining--;
    }

    private void doReset() {
        session.close(MessagesConfig.FFA_RESET_KICK);
        dev.lrxh.api.arena.IArena arena = session.getSettings().resolveArena();
        if (arena != null && arena.isSetup() && arena.isEnabled()) {
            java.io.File schemFile = dev.yowsef.neptuneffa.util.FfaArenaRestorer.getSchematicFile(arena.getName());
            if (schemFile.exists()) {
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(NeptuneFFA.getInstance(), () -> {
                    boolean restored = dev.yowsef.neptuneffa.util.FfaArenaRestorer.restoreFromSchematic(arena);
                    org.bukkit.Bukkit.getScheduler().runTask(NeptuneFFA.getInstance(), () -> {
                        if (restored) {
                            arena.setMin(arena.getMin());
                        }
                        session.open();
                    });
                });
                return;
            }
        }

        if (session.getSettings().isWorldgen() && arena != null) {
            arena.restore();
            Bukkit.getScheduler().runTaskLater(NeptuneFFA.getInstance(), () -> {
                arena.setMin(arena.getMin());
                session.open();
            }, 60L);
        } else {
            Bukkit.getScheduler().runTaskLater(NeptuneFFA.getInstance(), session::open, 60L);
        }
    }
}
