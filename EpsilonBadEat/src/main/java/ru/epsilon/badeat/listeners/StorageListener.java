package ru.epsilon.badeat.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import ru.epsilon.badeat.EpsilonBadEat;
import ru.epsilon.badeat.utils.FoodManager;

import java.util.List;
import java.util.Set;

/**
 * Слушатель событий, связанных с хранением еды в контейнерах (бочках)
 */
public class StorageListener implements Listener {

    private final EpsilonBadEat plugin;
    private final FoodManager foodManager;
    private final Set<Material> containerTypes;
    private final List<Material> requiredBlocks;
    private final int storageHeight;
    private final double storageMultiplier;
    private final boolean storageEnabled;

    public StorageListener(EpsilonBadEat plugin) {
        this.plugin = plugin;
        this.foodManager = plugin.getFoodManager();
        
        // Загрузка настроек хранения из конфига
        storageEnabled = plugin.getConfig().getBoolean("storage.enabled", true);
        storageHeight = plugin.getConfig().getInt("storage.height", 3);
        storageMultiplier = plugin.getConfig().getDouble("storage.multiplier", 2.0);
        
        // Загрузка типов контейнеров
        containerTypes = loadContainerTypes();
        
        // Загрузка требуемых блоков
        requiredBlocks = loadRequiredBlocks();
    }

    /**
     * Загружает типы контейнеров из конфигурации
     */
    private Set<Material> loadContainerTypes() {
        Set<Material> types = new java.util.HashSet<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("storage.container-types");
        
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(section.getString(key));
                if (material != null) {
                    types.add(material);
                }
            }
        } else {
            // Если секция не найдена, пробуем загрузить как список
            List<String> typeList = plugin.getConfig().getStringList("storage.container-types");
            for (String typeName : typeList) {
                Material material = Material.matchMaterial(typeName.toUpperCase());
                if (material != null) {
                    types.add(material);
                }
            }
        }
        
        return types;
    }

    /**
     * Загружает список требуемых блоков для проверки "бочки"
     */
    private List<Material> loadRequiredBlocks() {
        List<Material> blocks = new java.util.ArrayList<>();
        List<String> blockNames = plugin.getConfig().getStringList("storage.required-blocks");
        
        for (String blockName : blockNames) {
            Material material = Material.matchMaterial(blockName.toUpperCase());
            if (material != null) {
                blocks.add(material);
            }
        }
        
        return blocks;
    }

    /**
     * Проверяет, находится ли контейнер под нужным количеством блоков
     */
    private boolean isValidStorageLocation(Block containerBlock) {
        if (!storageEnabled) {
            return false;
        }

        Location loc = containerBlock.getLocation();
        
        // Проверяем блоки над контейнером
        for (int i = 1; i <= storageHeight; i++) {
            Block aboveBlock = loc.getBlock().getRelative(0, i, 0);
            
            // Если блок не входит в список разрешённых, возвращаем false
            if (!requiredBlocks.contains(aboveBlock.getType())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Обработка перемещения предметов между инвентарями
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!storageEnabled) {
            return;
        }

        ItemStack item = event.getItem();
        
        // Проверяем, является ли предмет едой
        if (!foodManager.isFood(item)) {
            return;
        }

        // Проверяем, является ли destination контейнером
        if (event.getDestination().getHolder() instanceof Container container) {
            Block containerBlock = container.getBlock();
            
            // Проверяем тип контейнера
            if (containerTypes.contains(containerBlock.getType())) {
                // Проверяем, является ли это "бочкой" (под слоем блоков)
                if (isValidStorageLocation(containerBlock)) {
                    // Применяем бонус хранения
                    foodManager.applyStorageBonus(item, storageMultiplier);
                } else {
                    // Если вынули из бочки или положили в обычное место
                    foodManager.removeStorageBonus(item);
                }
            }
        }

        // Проверяем источник
        if (event.getSource().getHolder() instanceof Container container) {
            Block containerBlock = container.getBlock();
            
            // Если предмет был в бочке и теперь перемещается
            if (containerTypes.contains(containerBlock.getType())) {
                // При перемещении из бочки сбрасываем бонус
                foodManager.removeStorageBonus(item);
            }
        }
    }

    /**
     * Обработка установки блока (для обновления статуса бочки)
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!storageEnabled) {
            return;
        }

        Block placedBlock = event.getBlock();
        
        // Проверяем, не установлен ли блок над контейнером
        for (int i = 1; i <= storageHeight + 1; i++) {
            Block belowBlock = placedBlock.getRelative(0, -i, 0);
            
            if (belowBlock.getState() instanceof Container container) {
                if (containerTypes.contains(belowBlock.getType())) {
                    // Обновляем предметы в контейнере
                    updateContainerItems(container);
                }
            }
        }
    }

    /**
     * Обработка разрушения блока (для обновления статуса бочки)
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!storageEnabled) {
            return;
        }

        Block brokenBlock = event.getBlock();
        
        // Проверяем, не был ли блок частью "бочки"
        for (int i = 1; i <= storageHeight + 1; i++) {
            Block belowBlock = brokenBlock.getRelative(0, -i, 0);
            
            if (belowBlock.getState() instanceof Container container) {
                if (containerTypes.contains(belowBlock.getType())) {
                    // Обновляем предметы в контейнере
                    updateContainerItems(container);
                }
            }
        }
    }

    /**
     * Обновляет статус предметов в контейнере
     */
    private void updateContainerItems(Container container) {
        org.bukkit.inventory.Inventory inventory = container.getInventory();
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            
            if (item != null && foodManager.isFood(item)) {
                if (isValidStorageLocation(container.getBlock())) {
                    foodManager.applyStorageBonus(item, storageMultiplier);
                } else {
                    foodManager.removeStorageBonus(item);
                }
            }
        }
    }
}
