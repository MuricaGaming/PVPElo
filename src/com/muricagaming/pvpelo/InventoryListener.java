package com.muricagaming.pvpelo;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryListener implements Listener {
    Main main;
    public InventoryListener(Main main) {this.main = main;}

    @EventHandler (priority = EventPriority.HIGHEST)
    private void onInventoryClick(InventoryClickEvent e) {
        if(!e.getInventory().equals(main.leaderboard))
            return;
        else
            e.setCancelled(true);
    }
}
