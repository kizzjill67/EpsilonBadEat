package ru.epsilon.badeat.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.epsilon.badeat.EpsilonBadEat;
import ru.epsilon.badeat.utils.FoodManager;

/**
 * Обработчик команды /еда
 */
public class FoodCommand implements CommandExecutor {

    private final EpsilonBadEat plugin;

    public FoodCommand(EpsilonBadEat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&7[&cЕда&7] ");
        Component prefixComponent = Component.text(prefix).color(NamedTextColor.GRAY);

        if (args.length == 0) {
            sender.sendMessage(Component.text("Использование:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/еда проверить - Проверить срок годности предмета в руке").color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("/еда перезагрузить - Перезагрузить конфигурацию").color(NamedTextColor.WHITE));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String checkCmd = plugin.getConfig().getString("messages.check-command", "проверить").toLowerCase();
        String reloadCmd = plugin.getConfig().getString("messages.reload-command", "перезагрузить").toLowerCase();

        if (subCommand.equals(checkCmd) || subCommand.equals("check")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Эту команду может использовать только игрок!").color(NamedTextColor.RED));
                return true;
            }

            if (!player.hasPermission("epsilonbadeat.use")) {
                String noPermMsg = plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав на эту команду!");
                player.sendMessage(Component.text(noPermMsg).color(NamedTextColor.RED));
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                String notFoodMsg = plugin.getConfig().getString("messages.not-food", "&cЭто не еда!");
                player.sendMessage(Component.text(notFoodMsg).color(NamedTextColor.RED));
                return true;
            }

            FoodManager foodManager = plugin.getFoodManager();
            
            // Проверяем, является ли предмет едой
            if (!foodManager.isFood(item)) {
                String notFoodMsg = plugin.getConfig().getString("messages.not-food", "&cЭто не еда!");
                player.sendMessage(Component.text(notFoodMsg).color(NamedTextColor.RED));
                return true;
            }

            // Получаем оставшееся время
            long remainingTime = foodManager.getRemainingTime(item);
            
            if (remainingTime <= 0) {
                player.sendMessage(Component.text("Эта еда уже испортилась!").color(NamedTextColor.RED));
            } else {
                String timeLeft = formatTime(remainingTime);
                String checkMsg = plugin.getConfig().getString("messages.food-check", "&aСрок годности: &e%time_left%");
                checkMsg = checkMsg.replace("%time_left%", timeLeft);
                player.sendMessage(Component.text(checkMsg).color(NamedTextColor.GREEN));
            }
            return true;
        }

        if (subCommand.equals(reloadCmd) || subCommand.equals("reload")) {
            if (!sender.hasPermission("epsilonbadeat.admin")) {
                String noPermMsg = plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав на эту команду!");
                sender.sendMessage(Component.text(noPermMsg).color(NamedTextColor.RED));
                return true;
            }

            plugin.reloadConfigAndRestart();
            String reloadMsg = plugin.getConfig().getString("messages.config-reloaded", "&aКонфигурация перезагружена!");
            sender.sendMessage(Component.text(reloadMsg).color(NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Неизвестная команда. Используйте /еда для помощи.").color(NamedTextColor.RED));
        return true;
    }

    /**
     * Форматирует время в читаемый вид
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append("д ");
            hours %= 24;
        }
        if (hours > 0) {
            result.append(hours).append("ч ");
            minutes %= 60;
        }
        if (minutes > 0) {
            result.append(minutes).append("м ");
            seconds %= 60;
        }
        if (seconds > 0 || result.isEmpty()) {
            result.append(seconds).append("с");
        }

        return result.toString().trim();
    }
}
