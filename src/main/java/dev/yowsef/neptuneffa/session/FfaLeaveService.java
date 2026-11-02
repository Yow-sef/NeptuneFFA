package dev.yowsef.neptuneffa.session;

import dev.yowsef.neptuneffa.NeptuneFFA;
import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Single place for the leave rules: combat tag refusal, admin bypass, delay countdown and its cancel cases.
public class FfaLeaveService implements Listener {
    private static final FfaLeaveService INSTANCE = new FfaLeaveService();

    private final Map<UUID, Countdown> countdowns = new HashMap<>();

    private FfaLeaveService() {}

    public static FfaLeaveService get() {
        return INSTANCE;
    }

    private static final class Countdown {
        final FfaSession session;
        final FfaParticipant participant;
        int remaining;
        BukkitTask task;

        Countdown(FfaSession session, FfaParticipant participant, int remaining) {
            this.session = session;
            this.participant = participant;
            this.remaining = remaining;
        }
    }

    public boolean isLeaving(UUID uuid) {
        return countdowns.containsKey(uuid);
    }

    public void requestLeave(Player player, FfaSession session) {
        UUID uuid = player.getUniqueId();
        FfaParticipant participant = session.getParticipant(uuid);
        if (participant == null) return;

        if (player.hasPermission("neptuneffa.admin")) {
            leaveNow(player, session);
            return;
        }

        if (participant.isCombatTagged()) {
            long seconds = (participant.getCombatTagRemaining() / 1000) + 1;
            FormatUtil.sendMessage(player, MessagesConfig.LEAVE_IN_COMBAT.replace("{seconds}", String.valueOf(seconds)));
            return;
        }

        // waiting out a respawn countdown, nothing to protect
        int delay = FfaConfig.get().getLeaveDelaySeconds();
        if (delay <= 0 || participant.isInRespawnCountdown()) {
            leaveNow(player, session);
            return;
        }

        if (countdowns.containsKey(uuid)) {
            FormatUtil.sendMessage(player, MessagesConfig.LEAVE_ALREADY);
            return;
        }

        Countdown countdown = new Countdown(session, participant, delay);
        countdowns.put(uuid, countdown);
        sendCountdown(player, delay);
        countdown.task = Bukkit.getScheduler().runTaskTimer(NeptuneFFA.getInstance(), () -> tick(uuid, countdown), 20L, 20L);
    }

    private void leaveNow(Player player, FfaSession session) {
        cancel(player.getUniqueId(), null);
        session.removePlayer(player.getUniqueId(), MessagesConfig.FFA_LEFT, true);
    }

    private void tick(UUID uuid, Countdown countdown) {
        if (countdowns.get(uuid) != countdown) return;

        Player player = Bukkit.getPlayer(uuid);
        FfaParticipant participant = countdown.session.getParticipant(uuid);
        if (player == null || !player.isOnline() || participant != countdown.participant) {
            cancel(uuid, null);
            return;
        }
        // safety net, tag/death are also cancelled right when they happen
        if (participant.isCombatTagged() || participant.isInRespawnCountdown()) {
            cancel(uuid, MessagesConfig.LEAVE_CANCELLED);
            return;
        }

        countdown.remaining--;
        if (countdown.remaining <= 0) {
            countdowns.remove(uuid);
            countdown.task.cancel();
            countdown.session.removePlayer(uuid, MessagesConfig.FFA_LEFT, true);
            return;
        }
        sendCountdown(player, countdown.remaining);
    }

    private void sendCountdown(Player player, int seconds) {
        String msg = MessagesConfig.LEAVE_COUNTDOWN.replace("{seconds}", String.valueOf(seconds));
        if (FfaConfig.get().isLeaveCountdownActionbar()) {
            FormatUtil.sendActionBar(player, msg);
        } else {
            FormatUtil.sendMessage(player, msg);
        }
    }

    // message == null cancels silently
    public void cancel(UUID uuid, String message) {
        Countdown countdown = countdowns.remove(uuid);
        if (countdown == null) return;
        if (countdown.task != null) countdown.task.cancel();
        if (message == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) FormatUtil.sendMessage(player, message);
    }

    public void cancelAll() {
        for (Countdown countdown : countdowns.values()) {
            if (countdown.task != null) countdown.task.cancel();
        }
        countdowns.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (countdowns.isEmpty() || !FfaConfig.get().isLeaveCancelOnMove()) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return; // head rotation / sub-block movement
        }
        cancel(event.getPlayer().getUniqueId(), MessagesConfig.LEAVE_CANCELLED_MOVE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (countdowns.isEmpty() || !FfaConfig.get().isLeaveCancelOnDamage()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getFinalDamage() <= 0) return;
        cancel(player.getUniqueId(), MessagesConfig.LEAVE_CANCELLED_DAMAGE);
    }
}
