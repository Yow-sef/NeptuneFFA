package dev.yowsef.neptuneffa.listener;

import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.NeptuneFFA;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.session.SpawnPointService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.*;

public class FfaRuleListener implements Listener {

    private boolean isInFfa(Player player) {
        return FfaSessionService.getInstance().getSession(player) != null;
    }

    private FfaSession getSession(Player player) {
        return FfaSessionService.getInstance().getSession(player);
    }

    // Override cancellation for FFA players
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        FfaSession session = getSession(player);
        if (session == null) return;

        event.setCancelled(false); // Take ownership for FFA players

        if (!API.kitIs(session.getKit(), "hunger")) {
            event.setCancelled(true);
        }
    }

    // Override cancellation for damage
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        FfaSession session = getSession(player);
        if (session == null) return;
        IKit kit = session.getKit();

        event.setCancelled(false);

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !API.kitIs(kit, "fallDamage")) {
            event.setCancelled(true);
            return;
        }

        if (!API.kitIs(kit, "damage")) {
            event.setDamage(0);
            return;
        }

        if (event.getFinalDamage() >= player.getHealth()) {
            if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                return;
            }
            event.setCancelled(true);
            player.setHealth(20.0f);
            FfaParticipant p = session.getParticipant(player.getUniqueId());
            Player killer = null;
            if (p != null && p.getValidAttacker() != null) {
                killer = Bukkit.getPlayer(p.getValidAttacker());
            }
            session.onDeath(player, killer);
            return;
        }

        // Only apply multiplier for PvP hits
        if (event instanceof EntityDamageByEntityEvent) {
            event.setDamage(event.getDamage() * kit.getDamageMultiplier());
        }
    }

    // Override cancellation
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        FfaSession session = getSession(victim);
        if (session == null) return;

        event.setCancelled(false);

        FfaParticipant victimP = session.getParticipant(victim.getUniqueId());
        if (victimP != null && victimP.isInRespawnCountdown()) {
            event.setCancelled(true);
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player) {
            attacker = (Player) arrow.getShooter();
        }

        if (attacker != null && victimP != null) {
            victimP.setLastAttacker(attacker.getUniqueId());
        }
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        FfaSession session = getSession(player);
        if (session == null) return;

        if (event.getRegainReason() == RegainReason.SATIATED && !API.kitIs(session.getKit(), "saturationHeal")) {
            event.setCancelled(true);
        }
    }

    // Override cancellation
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        FfaSession session = getSession(player);
        if (session == null) return;

        event.setCancelled(false);

        IArena arena = session.getArena();

        if (!API.kitIs(session.getKit(), "build")) {
            event.setCancelled(true);
            return;
        }

        if (arena != null) {
            if (event.getBlock().getY() >= arena.getBuildLimit()) {
                event.setCancelled(true);
                return;
            }
            if (!isInsideBounds(event.getBlock().getLocation(), arena)) {
                event.setCancelled(true);
                return;
            }
        }

        // Auto-ignite TNT
        if (event.getBlock().getType() == Material.TNT && API.kitIs(session.getKit(), "autoIgnite")) {
            event.setCancelled(true);
            TNTPrimed tnt = event.getBlock().getWorld().spawn(
                    event.getBlock().getLocation().add(0.5, 0.5, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(40);
            return;
        }

        session.getPlacedBlocks().add(event.getBlock().getLocation());
    }

    // Override cancellation
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        FfaSession session = getSession(player);
        if (session == null) return;

        event.setCancelled(false);

        IArena arena = session.getArena();

        if (API.kitIs(session.getKit(), "build") && session.getPlacedBlocks().contains(event.getBlock().getLocation())) {
            session.getPlacedBlocks().remove(event.getBlock().getLocation());
            return;
        }

        if (arena != null && API.kitIs(session.getKit(), "arenaBreak") && arena.getWhitelistedBlocks().contains(event.getBlock().getType())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEnderPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        FfaSession session = getSession(player);
        if (session == null) return;

        if (event.getItem() != null && event.getItem().getType() == Material.ENDER_PEARL && API.kitIs(session.getKit(), "enderpearlCooldown")) {
            if (player.getCooldown(Material.ENDER_PEARL) > 0) {
                event.setCancelled(true);
            } else {
                Bukkit.getScheduler().runTaskLater(NeptuneFFA.getInstance(), () -> player.setCooldown(Material.ENDER_PEARL, 15 * 20), 1L);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // Ignore sub-block movements and head rotations
        }

        Player player = event.getPlayer();
        FfaSession session = getSession(player);
        if (session == null) return;

        // Skip bounds check if respawning
        FfaParticipant participant = session.getParticipant(player.getUniqueId());
        if (participant == null || participant.isInRespawnCountdown()) return;

        IArena arena = session.getArena();
        if (arena == null) return;

        if (player.getLocation().getY() <= arena.getDeathY()) {
            session.onDeath(player, null);
            return;
        }

        if (!isInsideBounds(player.getLocation(), arena)) {
            // Teleport to spawn
            player.teleport(SpawnPointService.get().getSpawn(session.getSettings(), session.getCachedRandomSpawns()));
        }
    }

    // Override cancellation
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        FfaSession session = getSession(player);
        if (session == null) return;
        event.setCancelled(false);
        event.setCancelled(true);
    }



    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (isInFfa(event.getEntity())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Check session by location
        FfaSession owningSession = FfaSessionService.getInstance().getSessionByLocation(event.getLocation());
        if (owningSession == null) return; // Explosion not inside any FFA arena

        event.blockList().removeIf(block -> {
            if (owningSession.getPlacedBlocks().contains(block.getLocation())) {
                owningSession.getPlacedBlocks().remove(block.getLocation());
                return false; // Keep this block in the explosion (it was player-placed)
            }
            return true; // Remove from explosion — original terrain should not be destroyed
        });
    }

    // Whitelist /ffa leave
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isInFfa(player)) return;

        // Allow /ffa leave
        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/ffa leave") || cmd.equals("/ffa")) return;

        FfaSession session = getSession(player);
        if (session == null) return;

        FfaParticipant p = session.getParticipant(player.getUniqueId());
        if (p != null && p.isCombatTagged() && !player.hasPermission("neptuneffa.admin")) {
            event.setCancelled(true);
            dev.yowsef.neptuneffa.util.FormatUtil.sendMessage(player, "&cYou cannot use commands while in combat!");
        }
    }

    private boolean isInsideBounds(Location loc, IArena arena) {
        if (arena == null) return false;
        Location min = arena.getMin();
        Location max = arena.getMax();
        if (min == null || max == null) return true;
        
        if (loc.getWorld() == null || min.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(min.getWorld().getName())) return false;
        
        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
}
