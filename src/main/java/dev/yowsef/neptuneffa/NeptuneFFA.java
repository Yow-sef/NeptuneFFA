package dev.yowsef.neptuneffa;

import dev.yowsef.neptuneffa.config.FfaConfig;
import dev.yowsef.neptuneffa.config.FfaStatsManager;
import dev.yowsef.neptuneffa.config.MessagesConfig;
import dev.yowsef.neptuneffa.listener.FfaPlayerListener;
import dev.yowsef.neptuneffa.listener.FfaRuleListener;
import dev.yowsef.neptuneffa.scoreboard.FfaPlaceholders;
import dev.yowsef.neptuneffa.scoreboard.FfaRankingService;
import dev.yowsef.neptuneffa.session.FfaParticipant;
import dev.yowsef.neptuneffa.session.FfaSession;
import dev.yowsef.neptuneffa.session.FfaSessionService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class NeptuneFFA extends JavaPlugin {

    @Getter
    private static NeptuneFFA instance;

    @Override
    public void onEnable() {
        instance = this;

        // Load configs
        FfaConfig.get();
        FfaStatsManager.get();
        MessagesConfig.load();

        // Initialize services
        // Load rankings class
        FfaRankingService.getInstance();
        new FfaSessionService();

        // Periodically save stats
        Bukkit.getScheduler().runTaskTimer(this, () -> FfaStatsManager.get().saveAllAsync(), 6000L, 6000L); // 5 mins

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new FfaRuleListener(), this);
        Bukkit.getPluginManager().registerEvents(new FfaPlayerListener(), this);
        Bukkit.getPluginManager().registerEvents(new dev.yowsef.neptuneffa.util.menu.MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new dev.yowsef.neptuneffa.listener.FfaLobbyItemListener(), this);

        // Register commands
        dev.yowsef.neptuneffa.command.FfaCommand ffaCommand = new dev.yowsef.neptuneffa.command.FfaCommand();
        getCommand("ffa").setExecutor(ffaCommand);
        getCommand("ffa").setTabCompleter(ffaCommand);

        dev.yowsef.neptuneffa.command.FfaAdminCommand ffaAdminCommand = new dev.yowsef.neptuneffa.command.FfaAdminCommand();
        getCommand("ffaadmin").setExecutor(ffaAdminCommand);
        getCommand("ffaadmin").setTabCompleter(ffaAdminCommand);

        // Start Combat Tag Task
        new dev.yowsef.neptuneffa.session.FfaCombatTagTask().runTaskTimer(this, 0L, 20L); // run every second to update timer

        // Start Menu Auto-Update Task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof dev.yowsef.neptuneffa.util.menu.Menu menu) {
                    if (menu.isAutoUpdate()) {
                        menu.update(player);
                    }
                }
            }
        }, 20L, 20L); // run every second to refresh open menus

        // Register scoreboard
        API.get().getScoreboardService().registerScoreboard("IN_FFA", profile -> {
            Player player = profile.getPlayer();
            FfaSession session = FfaSessionService.getInstance().getSession(player);
            if (session == null) return new ArrayList<>();
            FfaParticipant participant = session.getParticipant(player.getUniqueId());
            if (participant == null) return new ArrayList<>();

            List<String> lines = new ArrayList<>();
            for (String line : FfaConfig.get().getScoreboardLines()) {
                lines.add(FfaPlaceholders.resolve(player, profile, line, session, participant));
            }
            if (lines.size() > 15) {
                return lines.subList(0, 15);
            }
            return lines;
        });

        // Register PlaceholderAPI expansion if present
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new dev.yowsef.neptuneffa.scoreboard.FfaPlaceholderExpansion().register();
        }

        getLogger().info("NeptuneFFA enabled!");
    }

    @Override
    public void onDisable() {
        // Shut down all sessions
        if (FfaSessionService.getInstance() != null) {
            FfaSessionService.getInstance().shutdownAll();
        }
        // Save stats to disk
        FfaStatsManager.get().saveAllSync();
        getLogger().info("NeptuneFFA disabled!");
    }
}
