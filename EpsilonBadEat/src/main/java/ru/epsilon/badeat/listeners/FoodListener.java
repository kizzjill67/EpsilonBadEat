package ru.epsilon.badeat.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import ru.epsilon.badeat.EpsilonBadEat;
import ru.epsilon.badeat.utils.FoodManager;

/**
 * Слушатель событий, связанных с употреблением еды
 */
public class FoodListener implements Listener {

    private final EpsilonBadEat plugin;
    private final FoodManager foodManager;

    public FoodListener(EpsilonBadEat plugin) {
        this.plugin = plugin;
        this.foodManager = plugin.getFoodManager();
    }

    /**
     * Обработка попытки съесть предмет
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Проверяем, является ли предмет едой
        if (!foodManager.isFood(item)) {
            return;
        }

        // Если у предмета нет метки времени, добавляем её
        if (!item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "creation_time"), 
                org.bukkit.persistence.PersistentDataType.LONG)) {
            foodManager.markItemAsFresh(item);
        }

        // Проверяем, не испортилась ли еда
        if (foodManager.isSpoiled(item)) {
            // Отменяем обычное употребление
            event.setCancelled(true);
            
            // Заменяем предмет на испорченный
            ItemStack spoiledItem = foodManager.createSpoiledItem();
            player.getInventory().setItemInMainHand(spoiledItem);
            
            // Вызываем болезнь через EpsilonDoctor
            applyDisease(player);
            
            // Отправляем сообщение
            String message = plugin.getConfig().getString("messages.spoiled-food-eat", "&cВы попытались съесть испорченную еду! Вы заболели.");
            player.sendMessage(Component.text(message).color(NamedTextColor.RED));
        }
    }

    /**
     * Применяет болезнь к игроку через EpsilonDoctor
     */
    private void applyDisease(Player player) {
        if (!plugin.getConfig().getBoolean("epsilon-doctor.enabled", true)) {
            return;
        }

        String diseaseName = plugin.getConfig().getString("epsilon-doctor.disease-on-spoiled", "пищевое_отравление");
        String method = plugin.getConfig().getString("epsilon-doctor.method", "COMMAND");

        if (method.equalsIgnoreCase("API")) {
            // Попытка использования API EpsilonDoctor
            tryApplyAPI(player, diseaseName);
        } else {
            // Использование команды
            String command = plugin.getConfig().getString("epsilon-doctor.command", "doctor infect %player% пищевое_отравление");
            command = command.replace("%player%", player.getName());
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    /**
     * Попытка применения болезни через API EpsilonDoctor
     */
    private void tryApplyAPI(Player player, String diseaseName) {
        try {
            // Пытаемся получить класс API EpsilonDoctor
            Class<?> apiClass = Class.forName("ru.epsilon.doctor.EpsilonDoctorAPI");
            java.lang.reflect.Method applyMethod = apiClass.getMethod("applyDisease", Player.class, String.class);
            applyMethod.invoke(null, player, diseaseName);
        } catch (Exception e) {
            // Если API недоступен, используем команду
            plugin.getLogger().warning("EpsilonDoctor API недоступен, используем команду: " + e.getMessage());
            String command = plugin.getConfig().getString("epsilon-doctor.command", "doctor infect %player% пищевое_отравление");
            command = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
