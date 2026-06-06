package dev.yowsef.neptuneffa.session;

import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.data.IKitData;
import dev.lrxh.api.kit.IKit;
import dev.lrxh.api.profile.IProfile;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.NeptuneFFA;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.FfaStatsManager;
import dev.yowsef.neptuneffa.config.KitFfaSettings;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.reset.FfaResetTask;
import dev.yowsef.neptuneffa.reset.FfaRespawnTask;
import dev.yowsef.neptuneffa.scoreboard.FfaRankingService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import dev.yowsef.neptuneffa.util.FormatUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class FfaSession {
    private final IKit kit;
    private final KitFfaSettings settings;
    private final List<FfaParticipant> participants = new CopyOnWriteArrayList<>();
    // Quick lookup by UUID
    private final Map<UUID, FfaParticipant> participantMap = new ConcurrentHashMap<>();
    private final Set<Location> placedBlocks = ConcurrentHashMap.newKeySet();
    private boolean open;
    // Prevent reopening replaced sessions
    private boolean destroyed = false;
    private FfaResetTask resetTask;

    public IArena getArena() {
        return settings.resolveArena();
    }

    // Cache of random spawn locations
    private List<Location> cachedRandomSpawns = List.of();

    public FfaSession(IKit kit, KitFfaSettings settings) {
        this.kit = kit;
        this.settings = settings;
    }

    public IKit getKit() {
        return API.getUpToDateKit(this.kit);
    }

    public void open() {
        if (destroyed || open) return;
        open = true;
        placedBlocks.clear();

        IArena baseArena = settings.resolveArena();
        if (baseArena != null) {
            try {
                Method setInUseMethod = baseArena.getClass().getMethod("setInUse", boolean.class);
                setInUseMethod.invoke(baseArena, true);
            } catch (Exception ignored) {}
        }

        // Pre-build random spawn cache
        cachedRandomSpawns = SpawnPointService.get().buildSpawnCache(settings, 20);

        resetTask = new FfaResetTask(this);
        resetTask.runTaskTimer(NeptuneFFA.getInstance(), 0L, 20L);
    }

    // Mark session as destroyed
    public void destroy() {
        this.destroyed = true;
    }

    public void close(String kickMessage) {
        if (!open) return;
        open = false;
        if (resetTask != null) resetTask.cancel();
        for (FfaParticipant p : List.copyOf(participants)) {
            removePlayer(p.getUuid(), kickMessage, true);
        }
        participants.clear();
        participantMap.clear();
        placedBlocks.clear();

        IArena baseArena = settings.resolveArena();
        if (baseArena != null) {
            try {
                Method setInUseMethod = baseArena.getClass().getMethod("setInUse", boolean.class);
                setInUseMethod.invoke(baseArena, false);
            } catch (Exception ignored) {}
        }
    }

    public void addPlayer(Player player) {
        if (!open) return;
        if (getParticipant(player.getUniqueId()) != null) return;

        // Ensure arena is fully configured
        IArena arena = getArena();
        if (arena == null || !arena.isSetup() || !arena.isEnabled()) {
            FormatUtil.sendMessage(player, "&cThis FFA arena is not configured. Contact an admin.");
            return;
        }

        Location spawn = SpawnPointService.get().getSpawn(settings, cachedRandomSpawns);
        if (spawn == null) {
            FormatUtil.sendMessage(player, "&cThis FFA arena has no valid spawn configured. Contact an admin.");
            return;
        }

        IProfile profile = API.getProfile(player.getUniqueId());
        if (profile == null) return;
        profile.setState("IN_FFA");
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);

        FfaParticipant p = new FfaParticipant(player.getUniqueId(), kit.getName());
        participants.add(p);
        participantMap.put(player.getUniqueId(), p);
        FfaSessionService.getInstance().addActivePlayer(player.getUniqueId(), this);

        player.teleport(spawn);
        kit.giveLoadout(player.getUniqueId());
        API.applyShieldPatterns(profile, player);

        FfaStatsManager.PlayerStats stats = FfaStatsManager.get().getStats(player.getUniqueId(), kit.getName());
        stats.setSessions(stats.getSessions() + 1);

        broadcastToSession(MessagesConfig.FFA_JOIN
                .replace("{player}", player.getName())
                .replace("{kit}", kit.getDisplayName()));
    }

    public void removePlayer(UUID uuid, String message, boolean toLobby) {
        FfaParticipant p = getParticipant(uuid);
        if (p == null) return;
        participants.remove(p);
        participantMap.remove(uuid);

        FfaSessionService.getInstance().removeActivePlayer(uuid);

        Player player = Bukkit.getPlayer(uuid);
        IProfile profile = API.getProfile(uuid);

        if (player != null) {
            if (!message.isEmpty()) FormatUtil.sendMessage(player, message);
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        }

        // Transition profile back to lobby state
        if (toLobby && profile != null) {
            profile.toLobby(); // handles state transition internally
        } else if (!toLobby && profile != null && profile.hasState("IN_FFA")) {
            // Set fallback state if disconnected
            profile.setState("neptune:in_lobby");
        }
    }

    public void onDeath(Player victim, @Nullable Player killer) {
        FfaParticipant victimP = getParticipant(victim.getUniqueId());
        if (victimP == null || victimP.isInRespawnCountdown()) return;

        victimP.recordDeath();
        incrementPersistent(victim.getUniqueId(), "ffa_deaths_" + kit.getName(), 1);

        // Record death in stats
        FfaStatsManager.PlayerStats victimStats = FfaStatsManager.get().getStats(victim.getUniqueId(), kit.getName());
        victimStats.setDeaths(victimStats.getDeaths() + 1);

        if (FfaConfig.get().isTrackNeptuneKitStats()) {
            IKitData kd = API.getKitData(victim.getUniqueId(), kit);
            if (kd != null) kd.setDeaths(kd.getDeaths() + 1);
        }

        if (killer != null) {
            FfaParticipant killerP = getParticipant(killer.getUniqueId());
            if (killerP != null) {
                killerP.recordKill();
                incrementPersistent(killer.getUniqueId(), "ffa_kills_" + kit.getName(), 1);

                // Record kill and streak in stats
                FfaStatsManager.PlayerStats killerStats = FfaStatsManager.get().getStats(killer.getUniqueId(), kit.getName());
                killerStats.setKills(killerStats.getKills() + 1);
                if (killerP.getSessionStreak() > killerStats.getBestStreak()) {
                    killerStats.setBestStreak(killerP.getSessionStreak());
                }

                int lifeBS = getPersistent(killer.getUniqueId(), "ffa_best_streak_" + kit.getName());
                if (killerP.getSessionStreak() > lifeBS) {
                    setPersistent(killer.getUniqueId(), "ffa_best_streak_" + kit.getName(), killerP.getSessionStreak());
                }

                if (FfaConfig.get().isTrackNeptuneKitStats()) {
                    IKitData kd = API.getKitData(killer.getUniqueId(), kit);
                    if (kd != null) kd.setKills(kd.getKills() + 1);
                }

                // Update rank leaderboards
                FfaRankingService.getInstance().update(killer.getUniqueId(), kit.getName());
            }
        }

        broadcastToSession(MessagesConfig.FFA_KILL
                .replace("{killer}", killer != null ? killer.getName() : "Void")
                .replace("{victim}", victim.getName())
                .replace("{victim_session_kills}", String.valueOf(victimP.getSessionKills())));

        int delay = settings.getRespawnDelayOverride() != -1 ? settings.getRespawnDelayOverride() : FfaConfig.get().getGlobalRespawnDelay();
        new FfaRespawnTask(this, victim, delay).runTaskTimer(NeptuneFFA.getInstance(), 0L, 20L);
    }

    public FfaParticipant getParticipant(UUID uuid) {
        return participantMap.get(uuid);
    }

    public void broadcastToSession(String message) {
        for (FfaParticipant p : participants) {
            Player player = Bukkit.getPlayer(p.getUuid());
            if (player != null) FormatUtil.sendMessage(player, message);
        }
    }

    private int incrementPersistent(UUID uuid, String key, int amount) {
        // Fetch once, get and increment
        IProfile profile = API.getProfile(uuid);
        if (profile == null) return 0;
        Object val = profile.getGameData().getPersistentData(key);
        int current = val instanceof Integer ? (Integer) val : 0;
        profile.getGameData().setPersistentData(key, current + amount);
        return current + amount;
    }

    private int getPersistent(UUID uuid, String key) {
        IProfile profile = API.getProfile(uuid);
        if (profile == null) return 0;
        Object val = profile.getGameData().getPersistentData(key);
        return val instanceof Integer ? (Integer) val : 0;
    }

    private void setPersistent(UUID uuid, String key, int value) {
        IProfile profile = API.getProfile(uuid);
        if (profile != null) {
            profile.getGameData().setPersistentData(key, value);
        }
    }
}
