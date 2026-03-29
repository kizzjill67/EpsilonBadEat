package ru.epsilon.badeat.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Менеджер конфигурации плагина
 */
public class ConfigManager {

    private final Plugin plugin;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
    }

    /**
     * Сохраняет конфигурацию по умолчанию, если файл не существует
     */
    public void saveDefaultConfig() {
        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveDefaultConfig();
        }
    }

    /**
     * Перезагружает конфигурацию из файла
     */
    public void reloadConfig() {
        plugin.reloadConfig();
    }

    /**
     * Получает конфигурацию
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Сохраняет текущую конфигурацию в файл
     */
    public void saveConfig() {
        try {
            plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить config.yml: " + e.getMessage());
        }
    }
}
