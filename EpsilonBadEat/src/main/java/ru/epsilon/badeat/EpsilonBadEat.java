package ru.epsilon.badeat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.epsilon.badeat.listeners.FoodListener;
import ru.epsilon.badeat.listeners.StorageListener;
import ru.epsilon.badeat.commands.FoodCommand;
import ru.epsilon.badeat.utils.FoodManager;
import ru.epsilon.badeat.utils.ConfigManager;

/**
 * Основной класс плагина EpsilonBadEat
 * Реализует механику порчи еды с интеграцией EpsilonDoctor
 */
public class EpsilonBadEat extends JavaPlugin {

    private static EpsilonBadEat instance;
    private ConfigManager configManager;
    private FoodManager foodManager;
    private int checkTaskId = -1;

    @Override
    public void onEnable() {
        instance = this;
        
        // Инициализация менеджеров
        configManager = new ConfigManager(this);
        foodManager = new FoodManager(this);
        
        // Регистрация слушателей событий
        Bukkit.getPluginManager().registerEvents(new FoodListener(this), this);
        Bukkit.getPluginManager().registerEvents(new StorageListener(this), this);
        
        // Регистрация команд
        FoodCommand foodCommand = new FoodCommand(this);
        getCommand("еда").setExecutor(foodCommand);
        getCommand("edible").setExecutor(foodCommand);
        getCommand("food").setExecutor(foodCommand);
        
        // Запуск периодической проверки
        startPeriodicCheck();
        
        getLogger().info("Плагин EpsilonBadEat успешно загружен!");
    }

    @Override
    public void onDisable() {
        // Остановка задач
        if (checkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(checkTaskId);
        }
        
        getLogger().info("Плагин EpsilonBadEat отключен!");
    }

    /**
     * Запускает периодическую проверку порчи еды
     */
    private void startPeriodicCheck() {
        int intervalTicks = getConfig().getInt("check-interval-ticks", 400);
        boolean onlineOnly = getConfig().getBoolean("check-online-players-only", true);
        
        checkTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            foodManager.checkSpoilage(onlineOnly);
        }, intervalTicks, intervalTicks).getTaskId();
    }

    /**
     * Перезагружает конфигурацию и перезапускает проверку
     */
    public void reloadConfigAndRestart() {
        configManager.reloadConfig();
        foodManager.reloadShelfLifeMap();
        
        // Перезапуск задачи проверки
        if (checkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(checkTaskId);
        }
        startPeriodicCheck();
    }

    public static EpsilonBadEat getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FoodManager getFoodManager() {
        return foodManager;
    }
}
