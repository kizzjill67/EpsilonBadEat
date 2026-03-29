package com.epsilon.badeat;

import org.bukkit.plugin.java.JavaPlugin;

public class EpsilonBadEat extends JavaPlugin {

    private FoodManager foodManager;
    private FoodListener foodListener;
    private FoodCommand foodCommand;

    @Override
    public void onEnable() {
        // Сохраняем конфиг по умолчанию, если его нет
        saveDefaultConfig();

        // Инициализация менеджера
        this.foodManager = new FoodManager(this);

        // Регистрация событий
        this.foodListener = new FoodListener(this, foodManager);
        getServer().getPluginManager().registerEvents(foodListener, this);

        // Регистрация команд
        this.foodCommand = new FoodCommand(this, foodManager);
        getCommand("еда").setExecutor(foodCommand);
        getCommand("еда").setTabCompleter(foodCommand);
        
        // Алиас для английской команды /food
        getCommand("food").setExecutor(foodCommand);
        getCommand("food").setTabCompleter(foodCommand);

        getLogger().info("EpsilonBadEat успешно запущен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EpsilonBadEat отключен.");
    }

    public FoodManager getFoodManager() {
        return foodManager;
    }
}
