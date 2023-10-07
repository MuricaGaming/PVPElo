package com.muricagaming.pvpelo;

import com.lkeehl.tagapi.api.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
	Main main;
	Tag nametag;
	Player p;

	public JoinListener(Main main) {this.main = main;}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onJoin(PlayerJoinEvent event) {
		// Get the Player's info
		p = event.getPlayer();

		// Check if they are already ranked. If not, add them to the HashMap and config with initial elo value
		if(!main.players.containsKey(event.getPlayer().getUniqueId())) {
			// Add Player to the config and active HashMap, then log in console
			main.addPlayer(p);
			main.logger.info("[PvP Elo] Added player " + p.getName() + " to the rankings with initial rating of " + main.getElo(p));
		}
	}
}
