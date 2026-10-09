package dev.yowsef.neptuneffa.listener;

import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.kit.IKit;
import dev.yowsef.neptuneffa.API;
import dev.yowsef.neptuneffa.NeptuneFFA;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import dev.yowsef.neptuneffa.session.SpawnPointService;
import dev.yowsef.neptuneffa.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

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

    // Override cancellation for damage.
    // Everything damage related lives in this one handler. It used to be split between an
    // EntityDamageEvent and an EntityDamageByEntityEvent handler on the same priority, but bukkit
    // doesnt promise which of those runs first so they kept undoing each other (un-cancelling a hit
    // that was already handled as a death, attacker recorded after the death etc).
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        FfaSession session = getSession(player);
        if (session == null) return;
        IKit kit = session.getKit();

        FfaParticipant p = session.getParticipant(player.getUniqueId());
        if (p == null || p.isInRespawnCountdown() || p.isSpawnProtected()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(false);

        Player attacker = event instanceof EntityDamageByEntityEvent byEntity ? getAttacker(byEntity.getDamager()) : null;
        // hurting yourself (own arrow, own tnt) should never count as a kill
        if (attacker != null && attacker.getUniqueId().equals(player.getUniqueId())) attacker = null;

        if (attacker != null) {
            FfaParticipant attackerP = session.getParticipant(attacker.getUniqueId());
            if (attackerP != null && attackerP.isSpawnProtected()) {
                attackerP.clearSpawnProtection(); // Lose protection when you attack
            }
            p.setLastAttacker(attacker.getUniqueId());
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !API.kitIs(kit, "fallDamage")) {
            event.setCancelled(true);
            return;
        }

        if (!API.kitIs(kit, "damage")) {
            event.setDamage(0);
            return;
        }

        // Only apply multiplier for PvP hits. Has to happen before the lethal check,
        // otherwise a hit that only becomes lethal after scaling kills the player for real
        if (event instanceof EntityDamageByEntityEvent) {
            event.setDamage(event.getDamage() * kit.getDamageMultiplier());
        }

        if (event.getFinalDamage() >= player.getHealth()) {
            if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                return;
            }
            event.setCancelled(true);
            session.onDeath(player, getKiller(p));
        }
    }

    private Player getKiller(FfaParticipant p) {
        if (p == null || p.getValidAttacker() == null) return null;
        return Bukkit.getPlayer(p.getValidAttacker());
    }

    private Player getAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) return shooter;
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player source) return source;
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player source) return source;
        return null;
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
            // cancelling the place gives the item back, so take one tnt ourselves (same as neptune does in matches)
            if (player.getGameMode() != GameMode.CREATIVE) {
                ItemStack hand = player.getInventory().getItem(event.getHand());
                if (hand.getType() == Material.TNT) {
                    hand.setAmount(hand.getAmount() - 1);
                }
            }
            TNTPrimed tnt = event.getBlock().getWorld().spawn(
                    event.getBlock().getLocation().add(0.5, 0.5, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(40);
            tnt.setSource(player);
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
            event.setDropItems(false); 
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        FfaSession session = getSession(player);
        if (session == null) return;

        if (API.kitIs(session.getKit(), "build")) {
            event.setCancelled(false);
        } else {
            event.setCancelled(true);
        }
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
            // knocked into the void still counts for whoever hit you last
            session.onDeath(player, getKiller(participant));
            return;
        }

        if (!isInsideBounds(player.getLocation(), arena)) {
            // Teleport to spawn
            Location spawn = SpawnPointService.get().getSpawn(session.getSettings(), session.getCachedRandomSpawns());
            if (spawn != null) player.teleport(spawn);
        }
    }

    // Override cancellation
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        FfaSession session = getSession(player);
        if (session == null) return;
        event.setCancelled(false);
        // Only allow drops in kits with BUILD enabled
        if (!API.kitIs(session.getKit(), "build")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemSpawn(ItemSpawnEvent event) {
        FfaSession session = FfaSessionService.getInstance()
                .getSessionByLocation(event.getLocation());
        if (session == null) return;
        // Allow items to exist inside FFA arenas  Neptune cancels these (in_game)
        event.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        FfaSession session = getSession(player);
        if (session == null) return;
        event.setCancelled(false);
        // Only allow pickup in kits with BUILD . no reason to pick up items in combat-only kits
        if (!API.kitIs(session.getKit(), "build")) {
            event.setCancelled(true);
        }
    }



    // Fallback for deaths that skip the damage handler (/kill, plugins setting health to 0 etc).
    // Without this the player really dies, loses the kit and is stuck as a participant.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        FfaSession session = getSession(player);
        if (session == null) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        FfaParticipant p = session.getParticipant(player.getUniqueId());
        if (p == null) return;

        event.setCancelled(true);
        event.setReviveHealth(FfaSession.getMaxHealth(player));
        if (p.isInRespawnCountdown()) return;

        // run the normal ffa death once the revive went through (cant teleport a dying player)
        Player killer = getKiller(p);
        Bukkit.getScheduler().runTask(NeptuneFFA.getInstance(), () -> {
            if (player.isOnline() && session.getParticipant(player.getUniqueId()) == p) {
                session.onDeath(player, killer);
            }
        });
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

        String cmd = event.getMessage().toLowerCase().trim();
        // only look at the label, startsWith("/spawn") also caught /spawner, /leaveparty etc
        String label = cmd.split("\\s+")[0];

        FfaSession session = getSession(player);
        if (session == null) return;

        // Combat tag check first. /ffa leave (and the /ffa menu) used to be whitelisted before
        // this check, so you could just leave mid fight with no death
        FfaParticipant p = session.getParticipant(player.getUniqueId());
        if (p != null && p.isCombatTagged() && !player.hasPermission("neptuneffa.admin")) {
            event.setCancelled(true);
            FormatUtil.sendMessage(player, MessagesConfig.COMBAT_NO_COMMANDS);
            return;
        }

        // Handle /leave or /spawn commands to cleanly leave FFA
        if (label.equals("/leave") || label.equals("/spawn")) {
            event.setCancelled(true);
            session.removePlayer(player.getUniqueId(), MessagesConfig.FFA_LEFT, true);
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
