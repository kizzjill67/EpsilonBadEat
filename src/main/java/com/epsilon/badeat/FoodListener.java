package com.epsilon.badeat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent; // Для новых версий PlayerAttemptPickupItemEvent
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class FoodListener implements Listener {

    private final EpsilonBadEat plugin;
    private final FoodManager manager;

    public FoodListener(EpsilonBadEat plugin, FoodManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // 1. Когда игрок берет предмет с земли
    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        manager.markItemAsFresh(item);
    }

    // 2. Когда игрок крафтит или перекладывает еду (упрощенно: проверяем инвентарь при клике)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            
            if (cursor != null) manager.markItemAsFresh(cursor);
            if (current != null) manager.markItemAsFresh(current);
            
            // Если перекладываем в бочку/холодильник
            if (event.getInventory().getHolder() instanceof Container) {
                Block block = ((Container) event.getInventory().getHolder()).getBlock();
                if (manager.isContainerInStorage(block)) {
                    if (cursor != null) manager.applyStorageBonus(cursor);
                    if (current != null) manager.applyStorageBonus(current);
                }
            }
        }
    }
    
    // 3. Проверка при открытии инвентаря (чтобы обновить бонусы)
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof Container) {
            Block block = ((Container) inv.getHolder()).getBlock();
            if (manager.isContainerInStorage(block)) {
                for (ItemStack item : inv.getContents()) {
                    if (item != null) manager.applyStorageBonus(item);
                }
            }
        }
    }

    // 4. Попытка съесть еду
    @EventHandler
    public void onEat(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (manager.isSpoiled(item)) {
            event.setCancelled(true); // Отменяем поедание
            
            // Сообщаем игроку
            String msg = plugin.getConfig().getString("messages.spoiled-food-eat", "&cВы попытались съесть испорченную еду!");
           player.sendMessage(msg.replace("&", "§"));

            // Интеграция с EpsilonDoctor через консоль
            if (plugin.getConfig().getBoolean("epsilon-doctor.enabled")) {
                String disease = plugin.getConfig().getString("epsilon-doctor.disease-on-spoiled", "пищевое_отравление");
                String commandTemplate = plugin.getConfig().getString("epsilon-doctor.command", "doctor infect %player% " + disease);
                String command = commandTemplate.replace("%player%", player.getName());
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }

            // Заменяем еду на гнилую плоть в руке
            ItemStack rotten = manager.spoilItem(item);
            player.getInventory().setItemInMainHand(rotten);
            player.updateInventory();
        }
    }
    
    // 5. Установка блока (проверка, не стала ли бочка холодильником)
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        // Проверяем блоки под новым блоком (вдруг поставили крышку над бочкой)
        checkBelowForContainer(placed.getLocation().subtract(0, 1, 0));
    }
    
    // 6. Ломание блока (вдруг сняли крышку)
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
         checkBelowForContainer(event.getBlock().getLocation().subtract(0, 1, 0));
    }

    private void checkBelowForContainer(Block block) {
        if (block.getType().equals(Material.BARREL) || block.getType().equals(Material.CHEST)) {
             // Тут можно добавить логику пересчета бонусов для всех предметов внутри, 
             // если бочка перестала быть "холодильником".
             // Для простоты в этой версии бонус применяется только при взаимодействии.
        }
    }
}
