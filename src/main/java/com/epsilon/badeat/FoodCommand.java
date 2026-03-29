package com.epsilon.badeat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class FoodCommand implements CommandExecutor, TabCompleter {

    private final EpsilonBadEat plugin;
    private final FoodManager manager;

    public FoodCommand(EpsilonBadEat plugin, FoodManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCmd = args[0].toLowerCase();

        if (subCmd.equals("помощь") || subCmd.equals("help")) {
            sendHelp(player);
        } else if (subCmd.equals("проверить") || subCmd.equals("check")) {
            checkFood(player);
        } else if (subCmd.equals("перезагрузить") || subCmd.equals("reload")) {
            if (player.hasPermission("epsilonbadeat.admin")) {
                plugin.reloadConfig();
                player.sendMessage("§aКонфиг перезагружен!");
            } else {
                player.sendMessage("§cУ вас нет прав на это!");
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7--- §cЕда §7---");
        player.sendMessage("§e/еда проверить §7- Узнать срок годности предмета в руке");
        player.sendMessage("§e/еда помощь §7- Показать это сообщение");
        if (player.hasPermission("epsilonbadeat.admin")) {
            player.sendMessage("§e/еда перезагрузить §7- Перезагрузить конфиг");
        }
    }

    private void checkFood(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cВ руке ничего нет!");
            return;
        }

        // Проверяем, еда ли это
        if (manager.getShelfLifeFromConfig(item.getType()) <= 0) {
            player.sendMessage("§7Это не еда или у неё бесконечный срок.");
            return;
        }

        if (manager.isSpoiled(item)) {
            player.sendMessage("§cЭта еда уже испортилась!");
            return;
        }

        // Расчет времени
        // (Упрощенная логика для вывода)
        player.sendMessage("§aЭта еда свежая.");
        // Можно добавить сложный расчет времени до порчи, если нужно
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("помощь");
            completions.add("проверить");
            if (sender.hasPermission("epsilonbadeat.admin")) completions.add("перезагрузить");
        }
        return completions;
    }
}
