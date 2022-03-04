package wm.vdr.leashplayers;

import org.bukkit.plugin.java.JavaPlugin;

public final class LeashPlayers extends JavaPlugin {

    public static LeashPlayers instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new Listeners(), this);
        getCommand("leashplayer").setExecutor(new Executors());
        getCommand("leashplayer").setTabCompleter(new Executors());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
