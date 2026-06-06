package dev.yowsef.neptuneffa.session;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class FfaParticipant {
    private final UUID uuid;
    private final String kitName;
    private int sessionKills;
    private int sessionDeaths;
    private int sessionStreak;
    private int sessionBestStreak;
    @Setter private boolean inRespawnCountdown;
    private final long joinedAt;
    
    private UUID lastAttacker;
    private long lastAttackedTime;

    public FfaParticipant(UUID uuid, String kitName) {
        this.uuid = uuid;
        this.kitName = kitName;
        this.joinedAt = System.currentTimeMillis();
    }

    public void recordKill() {
        sessionKills++;
        sessionStreak++;
        if (sessionStreak > sessionBestStreak) sessionBestStreak = sessionStreak;
    }

    public void recordDeath() {
        sessionDeaths++;
        sessionStreak = 0;
        lastAttacker = null;
    }

    public void setLastAttacker(UUID uuid) {
        this.lastAttacker = uuid;
        this.lastAttackedTime = System.currentTimeMillis();
    }

    public boolean isCombatTagged() {
        long durationMs = dev.yowsef.neptuneffa.config.FfaConfig.get().getCombatTagDurationSeconds() * 1000L;
        return lastAttacker != null && (System.currentTimeMillis() - lastAttackedTime <= durationMs);
    }

    public long getCombatTagRemaining() {
        if (!isCombatTagged()) return 0;
        long durationMs = dev.yowsef.neptuneffa.config.FfaConfig.get().getCombatTagDurationSeconds() * 1000L;
        return durationMs - (System.currentTimeMillis() - lastAttackedTime);
    }

    public UUID getValidAttacker() {
        if (!isCombatTagged()) return null;
        return lastAttacker;
    }
}
