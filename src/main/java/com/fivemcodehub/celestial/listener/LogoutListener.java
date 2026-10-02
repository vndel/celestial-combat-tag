package com.fivemcodehub.celestial.listener;

import com.fivemcodehub.celestial.combat.CombatTagManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Applies the combat-log penalty.
 *
 * <p>A disconnect cannot be refused, so the penalty is applied at quit time:
 * the player's drops are spawned where they stood and their inventory is
 * cleared, which is the same outcome as the death they were avoiding. This is
 * also why a true NPC stand-in is not used — spawning a persistent entity that
 * can be griefed or duplicated introduces more problems than it solves.
 */
public final class LogoutListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final CombatTagManager tags;

    public LogoutListener(JavaPlugin plugin, CombatTagManager tags) {
        this.plugin = plugin;
        this.tags = tags;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        var tag = tags.tagOf(player.getUniqueId());
        if (tag.isEmpty()) return;

        tags.clear(player.getUniqueId());

        if (!plugin.getConfig().getBoolean("penalty.drop-inventory", true)) {
            plugin.getLogger().info(player.getName() + " logged out while combat tagged");
            return;
        }

        Location at = player.getLocation();
        if (at.getWorld() == null) return;

        // Snapshot before clearing: the arrays are views onto live inventories.
        ItemStack[] contents = player.getInventory().getContents().clone();
        ItemStack[] armour = player.getInventory().getArmorContents().clone();

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        int dropped = 0;
        for (ItemStack[] set : new ItemStack[][]{contents, armour}) {
            for (ItemStack stack : set) {
                if (stack == null || stack.getType().isAir()) continue;
                at.getWorld().dropItemNaturally(at, stack);
                dropped++;
            }
        }

        if (plugin.getConfig().getBoolean("penalty.clear-health-on-return", true)) {
            // Returning at minimum health makes the escape pointless rather
            // than merely costly.
            player.setHealth(Math.max(1.0, plugin.getConfig()
                    .getDouble("penalty.return-health", 1.0)));
        }

        plugin.getLogger().info(player.getName()
                + " combat logged; dropped " + dropped + " stack(s)");

        var opponent = plugin.getServer().getPlayer(tag.get().opponent());
        if (opponent != null && plugin.getConfig()
                .getBoolean("penalty.notify-opponent", true)) {
            opponent.sendMessage(MM.deserialize(
                    "<gold><name></gold> <gray>combat logged. Their items dropped.</gray>",
                    Placeholder.unparsed("name", player.getName())));
        }
    }
}
