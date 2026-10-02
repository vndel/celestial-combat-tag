package com.fivemcodehub.celestial.listener;

import com.fivemcodehub.celestial.combat.CombatTagManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class CombatListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final CombatTagManager tags;

    public CombatListener(JavaPlugin plugin, CombatTagManager tags) {
        this.plugin = plugin;
        this.tags = tags;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        // Self-damage, and damage between the same player via projectile, is
        // not combat.
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        tags.tag(attacker, victim);
    }

    /** Unwraps projectiles so a bow shot tags the shooter, not the arrow. */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player direct) return direct;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /**
     * Blocks escape commands while tagged.
     *
     * <p>Matched on the root command only, with aliases normalised, because
     * checking {@code message.startsWith("/home")} would also block
     * {@code /homes} and miss {@code /HOME}.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!tags.isTagged(player.getUniqueId())) return;
        if (player.hasPermission("celestial.admin")) return;

        String root = event.getMessage().substring(1).split(" ", 2)[0]
                .toLowerCase(Locale.ROOT);
        // Strip a plugin-qualified prefix such as "essentials:home".
        int colon = root.indexOf(':');
        if (colon >= 0) root = root.substring(colon + 1);

        if (!plugin.getConfig().getStringList("tag.blocked-commands").contains(root)) return;

        event.setCancelled(true);
        player.sendMessage(MM.deserialize(plugin.getConfig().getString(
                "messages.command-blocked",
                "<red>You cannot use that while in combat.</red>")));
    }
}
