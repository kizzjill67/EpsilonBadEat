package com.epsilon.badeat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class FoodManager {

    private final EpsilonBadEat plugin;
    private final NamespacedKey keyCreationTime;
    private final NamespacedKey keyShelfLife;

    public FoodManager(EpsilonBadEat plugin) {
        this.plugin = plugin;
        this.keyCreationTime = new NamespacedKey(plugin, "creation_time");
        this.keyShelfLife = new NamespacedKey(plugin, "shelf_life");
    }

    // Применяет метку времени к новой еде
    public void markItemAsFresh(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        
        // Проверяем, есть ли срок годности в конфиге для этого предмета
        long shelfLifeMs = getShelfLifeFromConfig(item.getType());
        if (shelfLifeMs <= 0) return; // Еда не портится

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        long now = System.currentTimeMillis();

        pdc.set(keyCreationTime, PersistentDataType.LONG, now);
        pdc.set(keyShelfLife, PersistentDataType.LONG, shelfLifeMs);

        item.setItemMeta(meta);
    }

    // Получает срок годности из конфига
    public long getShelfLifeFromConfig(Material material) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("food-shelf-life");
        if (section == null) return 0;

        String timeStr = section.getString(material.name());
        if (timeStr == null) return 0;

        return parseTime(timeStr);
    }

    // Парсит строку времени (2d, 12h, 30m) в миллисекунды
    private long parseTime(String timeStr) {
        timeStr = timeStr.toLowerCase();
        long multiplier = 1;
        if (timeStr.endsWith("d")) {
            multiplier = 24 * 60 * 60 * 1000; // День
            timeStr = timeStr.replace("d", "");
        } else if (timeStr.endsWith("h")) {
            multiplier = 60 * 60 * 1000; // Час
            timeStr = timeStr.replace("h", "");
        } else if (timeStr.endsWith("m")) {
            multiplier = 60 * 1000; // Минута
            timeStr = timeStr.replace("m", "");
        }
        
        try {
            return Long.parseLong(timeStr) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Проверяет, испортилась ли еда
    public boolean isSpoiled(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(keyCreationTime, PersistentDataType.LONG)) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        long creationTime = pdc.get(keyCreationTime, PersistentDataType.LONG);
        long shelfLife = pdc.get(keyShelfLife, PersistentDataType.LONG);
        
        // Если срок 0, значит бесконечный
        if (shelfLife == 0) return false;

        return System.currentTimeMillis() > (creationTime + shelfLife);
    }

    // Превращает еду в гнилую плоть
    public ItemStack spoilItem(ItemStack original) {
        ItemStack rotten = new ItemStack(Material.ROTTEN_FLESH, original.getAmount());
        ItemMeta meta = rotten.getItemMeta();
        
        String name = plugin.getConfig().getString("spoiled-item.display-name", "&cИспорченная еда");
        meta.setDisplayName(plugin.getServer().getColorChar() + name.replace("&", "§")); // Упрощенная замена цветов
        
        List<String> lore = plugin.getConfig().getStringList("spoiled-item.lore");
        if (!lore.isEmpty()) {
             // Тут можно добавить логику цветов для лора, если нужно
             meta.setLore(lore);
        }

        rotten.setItemMeta(meta);
        return rotten;
    }

    // Проверяет, находится ли контейнер в "холодильнике" (под блоками)
    public boolean isContainerInStorage(Block containerBlock) {
        if (!plugin.getConfig().getBoolean("storage.enabled")) return false;

        int height = plugin.getConfig().getInt("storage.height");
        List<String> allowedBlocks = plugin.getConfig().getStringList("storage.required-blocks");

        Location loc = containerBlock.getLocation();
        
        for (int i = 1; i <= height; i++) {
            Block above = loc.getBlock().getRelative(0, i, 0);
            String typeName = above.getType().name();
            
            // Проверяем, есть ли текущий блок в списке разрешенных
            boolean found = false;
            for (String allowed : allowedBlocks) {
                if (typeName.equals(allowed)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false; // Если хоть один блок не подходит, бонус не действует
        }
        return true;
    }

    // Применяет бонус хранения (удлиняет срок)
    public void applyStorageBonus(ItemStack item) {
        if (!plugin.getConfig().getBoolean("storage.enabled")) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(keyCreationTime, PersistentDataType.LONG)) return;

        long originalShelfLife = pdc.get(keyShelfLife, PersistentDataType.LONG);
        if (originalShelfLife <= 0) return;

        double multiplier = plugin.getConfig().getDouble("storage.multiplier", 1.0);
        
        // Обновляем срок годности в метаданных
        pdc.set(keyShelfLife, PersistentDataType.LONG, (long)(originalShelfLife * multiplier));
        item.setItemMeta(meta);
    }
    
    // Удаляет бонус (если вынули из бочки - логика упрощена: бонус сбрасывается к оригиналу при следующем получении, 
    // но для простоты реализации мы просто не применяем бонус, если предмет не в бочке. 
    // В данной версии бонус применяется "навечно" при попадании в бочку, чтобы не усложнять код для новичка).
}
