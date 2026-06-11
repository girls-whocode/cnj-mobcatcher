package net.cnjgaming.mobcatcher.command;

import net.cnjgaming.mobcatcher.CNJMobCatcherPlugin;
import net.cnjgaming.mobcatcher.manager.CatcherItemManager;
import net.cnjgaming.mobcatcher.manager.RecipeManager;
import net.cnjgaming.mobcatcher.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MobCatcherCommand implements CommandExecutor, TabCompleter {

    private final CNJMobCatcherPlugin plugin;
    private final CatcherItemManager catcherItemManager;
    private final RecipeManager recipeManager;

    public MobCatcherCommand(CNJMobCatcherPlugin plugin, CatcherItemManager catcherItemManager, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.catcherItemManager = catcherItemManager;
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cnj.mobcatcher.admin")) {
            MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_NO_PERMISSION, "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_USAGE_HEADER, "&eUsage:");
            for (String line : plugin.getConfig().getStringList("messages.commands.usage-lines")) {
                sender.sendMessage(MessageUtil.color(line));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            try {
                recipeManager.registerRecipes();
                MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_RELOAD_SUCCESS, "&aCNJMobCatcher config reloaded.");
            } catch (Exception ex) {
                MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_RELOAD_FAILED, "&cFailed to reload crafting recipe: %error%", "%error%", ex.getMessage());
                plugin.getLogger().warning("Recipe reload failed: " + ex.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_GIVE_USAGE, "&eUsage: /mobcatcher give <player> <uses>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_PLAYER_NOT_FOUND, "&cPlayer not found.");
                return true;
            }

            int uses;
            try {
                uses = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_INVALID_USES_NUMBER, "&cUses must be a number.");
                return true;
            }

            List<Integer> allowed = plugin.getConfig().getIntegerList("uses.allowed-give-amounts");
            if (uses != -1 && !allowed.contains(uses)) {
                MessageUtil.send(
                    plugin,
                    sender,
                    MessageUtil.Keys.CMD_INVALID_USES_VALUE,
                    "&cAllowed uses: %allowed% or -1 for unlimited.",
                    "%allowed%",
                    allowed.toString()
                );
                return true;
            }

            ItemStack catcher = catcherItemManager.createEmptyCatcher(uses);
            target.getInventory().addItem(catcher);

            MessageUtil.send(
                plugin,
                sender,
                MessageUtil.Keys.CMD_GIVE_SUCCESS,
                "&aGave &f%player% &aa Mob Catcher with &e%uses% &auses.",
                "%player%",
                target.getName(),
                "%uses%",
                uses == -1 ? "Unlimited" : String.valueOf(uses)
            );
            return true;
        }

        MessageUtil.send(plugin, sender, MessageUtil.Keys.CMD_UNKNOWN_SUBCOMMAND, "&cUnknown subcommand.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            out.add("give");
            out.add("reload");
            return out;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                out.add(player.getName());
            }
            return out;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            out.add("1");
            out.add("3");
            out.add("5");
            out.add("10");
            out.add("25");
            out.add("-1");
            return out;
        }

        return out;
    }
}