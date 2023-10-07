package com.muricagaming.pvpelo;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor;

public class ChatListener implements Listener {
	Main main;
	String eloDisplay;
	
	public ChatListener(Main main) {this.main = main;}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onChat(AsyncPlayerChatEvent event) {
		eloDisplay = main.eloColor + main.getElo(event.getPlayer()) + ChatColor.RESET;
		event.setFormat(event.getFormat().replaceAll("varElo", eloDisplay));
	}
}
