package com.fivemcodehub.celestial.command;

import com.fivemcodehub.celestial.combat.CombatTagManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class CombatTagCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final CombatTagManager tags;

    public CombatTagCommand(JavaPlugin plugin, CombatTagManager tags) {
        this.plugin = plugin;
        this.tags = tags;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "clear" -> {
                if (args.length < 2) {
                    sender.sendMessage(MM.deserialize("<red>Specify a player.</red>"));
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(MM.deserialize("<red>Not online.</red>"));
                    return true;
                }
                tags.clear(target.getUniqueId());
                sender.sendMessage(MM.deserialize("<green>Tag cleared.</green>"));
            }
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(MM.deserialize("<green>Configuration reloaded.</green>"));
            }
            default -> sender.sendMessage(MM.deserialize(
                    "<gray>Currently tagged:</gray> <white><n></white>",
                    Placeholder.unparsed("n", String.valueOf(tags.taggedCount()))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return List.of("status", "clear", "reload");
        if (args.length == 2) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
