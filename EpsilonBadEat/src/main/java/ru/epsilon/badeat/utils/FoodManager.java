package ru.epsilon.badeat.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import ru.epsilon.badeat.EpsilonBadEat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Менеджер для управления сроками годности еды
 */
public class FoodManager {

    private final EpsilonBadEat plugin;
    private final Map<Material, Long> shelfLifeMap;
    
    // Ключи PDC
    private final NamespacedKey creationTimeKey;
    private final NamespacedKey shelfLifeKey;
    private final NamespacedKey storageBonusKey;

    public FoodManager(EpsilonBadEat plugin) {
        this.plugin = plugin;
        this.shelfLifeMap = new HashMap<>();
        
        // Инициализация ключей PDC
        creationTimeKey = new NamespacedKey(plugin, "creation_time");
        shelfLifeKey = new NamespacedKey(plugin, "shelf_life");
        storageBonusKey = new NamespacedKey(plugin, "storage_bonus");
        
        // Загрузка сроков годности из конфига
        loadShelfLifeMap();
    }

    /**
     * Загружает сроки годности из конфигурации
     */
    public void loadShelfLifeMap() {
        shelfLifeMap.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("food-shelf-life");
        
        if (section == null) {
            return;
        }

        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            String timeStr = section.getString(key);
            if (timeStr == null) continue;

            Material material = Material.matchMaterial(key.toUpperCase());
            if (material == null) {
                // Пробуем найти материал с префиксом itemsadder:
                if (key.toLowerCase().startsWith("itemsadder:")) {
                    // ItemsAdder предметы обрабатываются отдельно
                    continue;
                }
                plugin.getLogger().warning("Неизвестный материал: " + key);
                continue;
            }

            long milliseconds = parseTime(timeStr);
            if (milliseconds > 0) {
                shelfLifeMap.put(material, milliseconds);
            }
        }
    }

    /**
     * Перезагружает карту сроков годности
     */
    public void reloadShelfLifeMap() {
        loadShelfLifeMap();
    }

    /**
     * Парсит строку времени в миллисекунды
     * Форматы: 30m, 2h, 5d, 0 (бесконечно)
     */
    private long parseTime(String timeStr) {
        if (timeStr.equals("0")) {
            return -1; // Бесконечный срок
        }

        try {
            if (timeStr.endsWith("m")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 60 * 1000;
            } else if (timeStr.endsWith("h")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 60 * 60 * 1000;
            } else if (timeStr.endsWith("d")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 24 * 60 * 60 * 1000;
            } else {
                // По умолчанию считаем секундами
                return Long.parseLong(timeStr) * 1000;
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Неверный формат времени: " + timeStr);
            return 0;
        }
    }

    /**
     * Проверяет, является ли предмет едой
     */
    public boolean isFood(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        Material type = item.getType();
        
        // Проверяем по списку из конфига
        if (shelfLifeMap.containsKey(type)) {
            return true;
        }

        // Проверяем, является ли материал съедобным (ванильная проверка)
        return type.isEdible();
    }

    /**
     * Добавляет метаданные к предмету еды при создании
     */
    public void markItemAsFresh(ItemStack item) {
        if (!isFood(item)) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        long currentTime = System.currentTimeMillis();
        Long shelfLife = shelfLifeMap.get(item.getType());

        if (shelfLife == null) {
            // Если материала нет в конфиге, используем дефолтное значение (1 день)
            shelfLife = 24L * 60 * 60 * 1000;
        }

        pdc.set(creationTimeKey, PersistentDataType.LONG, currentTime);
        pdc.set(shelfLifeKey, PersistentDataType.LONG, shelfLife);
        pdc.set(storageBonusKey, PersistentDataType.DOUBLE, 1.0);

        item.setItemMeta(item.getItemMeta());
    }

    /**
     * Проверяет, испортился ли предмет
     * @return true если предмет испортился
     */
    public boolean isSpoiled(ItemStack item) {
        if (!isFood(item)) {
            return false;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (!pdc.has(creationTimeKey, PersistentDataType.LONG)) {
            // Если нет метки времени, считаем предмет свежим и добавляем метку
            markItemAsFresh(item);
            return false;
        }

        long creationTime = pdc.get(creationTimeKey, PersistentDataType.LONG);
        long shelfLife = pdc.get(shelfLifeKey, PersistentDataType.LONG);
        double bonus = pdc.getOrDefault(storageBonusKey, PersistentDataType.DOUBLE, 1.0);

        long expirationTime = creationTime + (long)(shelfLife * bonus);
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Получает оставшееся время до порчи в миллисекундах
     */
    public long getRemainingTime(ItemStack item) {
        if (!isFood(item)) {
            return 0;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (!pdc.has(creationTimeKey, PersistentDataType.LONG)) {
            return -1; // Нет данных, считаем бесконечным
        }

        long creationTime = pdc.get(creationTimeKey, PersistentDataType.LONG);
        long shelfLife = pdc.get(shelfLifeKey, PersistentDataType.LONG);
        double bonus = pdc.getOrDefault(storageBonusKey, PersistentDataType.DOUBLE, 1.0);

        long expirationTime = creationTime + (long)(shelfLife * bonus);
        long remaining = expirationTime - System.currentTimeMillis();

        return Math.max(0, remaining);
    }

    /**
     * Применяет бонус хранения (для бочки)
     */
    public void applyStorageBonus(ItemStack item, double multiplier) {
        if (!isFood(item)) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (!pdc.has(creationTimeKey, PersistentDataType.LONG)) {
            markItemAsFresh(item);
            pdc = item.getItemMeta().getPersistentDataContainer();
        }

        pdc.set(storageBonusKey, PersistentDataType.DOUBLE, multiplier);
        item.setItemMeta(item.getItemMeta());
    }

    /**
     * Сбрасывает бонус хранения (когда предмет вынут из бочки)
     */
    public void removeStorageBonus(ItemStack item) {
        if (!isFood(item)) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (!pdc.has(creationTimeKey, PersistentDataType.LONG)) {
            return;
        }

        // Сохраняем оставшееся время
        long remainingTime = getRemainingTime(item);
        
        // Сбрасываем бонус
        pdc.set(storageBonusKey, PersistentDataType.DOUBLE, 1.0);
        
        // Пересчитываем время создания, чтобы оставшееся время сохранилось
        long currentTime = System.currentTimeMillis();
        long newCreationTime = currentTime - (shelfLifeMap.getOrDefault(item.getType(), 24L * 60 * 60 * 1000) - remainingTime);
        pdc.set(creationTimeKey, PersistentDataType.LONG, newCreationTime);
        
        item.setItemMeta(item.getItemMeta());
    }

    /**
     * Создаёт испорченный предмет
     */
    public ItemStack createSpoiledItem() {
        String materialName = plugin.getConfig().getString("spoiled-item.material", "ROTTEN_FLESH");
        Material material = Material.matchMaterial(materialName);
        
        if (material == null) {
            material = Material.ROTTEN_FLESH;
        }

        ItemStack spoiledItem = new ItemStack(material);
        
        // Устанавливаем имя
        String displayName = plugin.getConfig().getString("spoiled-item.display-name", "&cИспорченная еда");
        // Здесь можно добавить поддержку цветов через MiniMessage или ChatColor
        
        // Устанавливаем лор
        var lore = plugin.getConfig().getStringList("spoiled-item.lore");
        
        return spoiledItem;
    }

    /**
     * Периодическая проверка порчи еды
     */
    public void checkSpoilage(boolean onlineOnly) {
        // Проверка будет реализована в слушателях событий
        // Этот метод может использоваться для фоновой проверки
    }
}
