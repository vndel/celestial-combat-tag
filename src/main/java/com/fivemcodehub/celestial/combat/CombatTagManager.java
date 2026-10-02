package com.fivemcodehub.celestial.combat;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who is in combat, with whom, and for how long.
 *
 * <p>Stores the opponent alongside the expiry so a logout can be attributed,
 * and so the action bar can name the actual threat rather than a generic
 * "in combat" string.
 */
public final class CombatTagManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public record Tag(UUID opponent, long expiresAt) {
        public long remainingMillis() {
            return Math.max(0L, expiresAt - System.currentTimeMillis());
        }

        public boolean expired() {
            return remainingMillis() == 0L;
        }
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Tag> tagged = new ConcurrentHashMap<>();
    private BukkitTask displayTask;

    public CombatTagManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Every 10 ticks is frequent enough for a smooth countdown without
        // sending a packet per player per tick.
        this.displayTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tickDisplays, 10L, 10L);
    }

    private void tickDisplays() {
        String template = plugin.getConfig().getString("tag.action-bar",
                "<red>In combat</red> <gray>|</gray> <white><seconds>s</white>");

        tagged.forEach((uuid, tag) -> {
            Player player = plugin.getServer().getPlayer(uuid);

            if (tag.expired()) {
                tagged.remove(uuid);
                if (player != null && plugin.getConfig().getBoolean("tag.notify-expiry", true)) {
                    player.sendActionBar(MM.deserialize(
                            "<green>Combat ended.</green>"));
                }
                return;
            }

            if (player == null || !player.isOnline()) return;

            long seconds = (tag.remainingMillis() + 999L) / 1000L;
            String opponentName = Optional.ofNullable(
                            plugin.getServer().getPlayer(tag.opponent()))
                    .map(Player::getName)
                    .orElse("unknown");

            player.sendActionBar(MM.deserialize(template,
                    Placeholder.unparsed("seconds", String.valueOf(seconds)),
                    Placeholder.unparsed("opponent", opponentName)));
        });
    }

    /** Tags both participants. Bypass permission is honoured per player. */
    public void tag(Player attacker, Player victim) {
        long duration = plugin.getConfig().getInt("tag.duration-seconds", 15) * 1000L;
        long expiry = System.currentTimeMillis() + duration;

        if (!attacker.hasPermission("celestial.bypass")) {
            tagged.put(attacker.getUniqueId(), new Tag(victim.getUniqueId(), expiry));
        }
        if (!victim.hasPermission("celestial.bypass")) {
            tagged.put(victim.getUniqueId(), new Tag(attacker.getUniqueId(), expiry));
        }
    }

    public boolean isTagged(UUID uuid) {
        Tag tag = tagged.get(uuid);
        if (tag == null) return false;
        if (tag.expired()) {
            tagged.remove(uuid);
            return false;
        }
        return true;
    }

    public Optional<Tag> tagOf(UUID uuid) {
        return Optional.ofNullable(tagged.get(uuid)).filter(t -> !t.expired());
    }

    public void clear(UUID uuid) {
        tagged.remove(uuid);
    }

    public int taggedCount() {
        return (int) tagged.values().stream().filter(t -> !t.expired()).count();
    }

    public void shutdown() {
        if (displayTask != null) displayTask.cancel();
        // Clear on shutdown: a restart is not the player's fault, so nobody
        // should be penalised for a disconnect the operator caused.
        tagged.clear();
    }
}
