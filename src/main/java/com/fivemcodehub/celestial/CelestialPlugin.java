package com.fivemcodehub.celestial;

import com.fivemcodehub.celestial.combat.CombatTagManager;
import com.fivemcodehub.celestial.command.CombatTagCommand;
import com.fivemcodehub.celestial.listener.CombatListener;
import com.fivemcodehub.celestial.listener.LogoutListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Combat tagging with logout consequences.
 *
 * <p>Combat logging is a design problem, not a detection problem: the client
 * simply closes the connection, and the server cannot prevent that. So instead
 * of trying to block the disconnect, this applies the outcome the player was
 * trying to escape — their items drop and death is recorded on reconnect.
 */
public final class CelestialPlugin extends JavaPlugin {

    private CombatTagManager tags;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.tags = new CombatTagManager(this);
        this.tags.start();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new CombatListener(this, tags), this);
        pm.registerEvents(new LogoutListener(this, tags), this);

        var cmd = getCommand("combattag");
        if (cmd != null) {
            CombatTagCommand executor = new CombatTagCommand(this, tags);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("CelestialCombatTag enabled ("
                + getConfig().getInt("tag.duration-seconds", 15) + "s tag)");
    }

    @Override
    public void onDisable() {
        if (tags != null) tags.shutdown();
    }
}
