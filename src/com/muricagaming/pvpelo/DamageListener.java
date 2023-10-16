package com.muricagaming.pvpelo;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DamageListener implements Listener {
	Main main;
	Player victim;
	Player killer;
	int victimElo;
	int killerElo;
	double ratio;
	int adjustment;
	World world;
	public DamageListener(Main main) {this.main = main;}

	@EventHandler(priority = EventPriority.HIGHEST)
	private void onDeath(PlayerDeathEvent event) {
		if(event.getEntityType() == EntityType.PLAYER && event.getEntity().getKiller() instanceof Player) {
			// Get the involved Players and their elos
			victim = event.getEntity();
			killer = victim.getKiller();
			victimElo = main.getElo(victim);
			killerElo = main.getElo(killer);
			world = victim.getWorld();

			// Check if world is enabled for elo tracking. Skips all logic if not enabled.
			if (main.enabledWorlds.contains(world)) {

				// Calculate the loss and gain of elo for each, based on the rank difference
				// Multiplies rating ratio by the configurable base adjustment
				ratio = (double) victimElo / (double) killerElo;
				adjustment = (int) (ratio * main.baseAdjustment);

			/* Cap adjustment to the configurable max adjustment
			   Prevent elo generation if the victim is already at 1 elo
			   Hard cap adjustment at 50% of victim's rating to prevent issues
			   involving two low-rated players with a large ratio */
				if (victimElo == 1)
					adjustment = 0;
				else if ((double) killerElo / (double) victimElo > (double) main.maxRatio)
					adjustment = 0;
				else if (adjustment > (int) (main.maxPortion * (double) victimElo))
					adjustment = (int) (main.maxPortion * (double) victimElo);
				else if (adjustment > main.maxAdjustment)
					adjustment = main.maxAdjustment;

				// Adjust the elo of each Player
				main.setElo(victim, victimElo - adjustment);
				main.setElo(killer, killerElo + adjustment);

				// Notify each Player of the adjustment
				victim.sendMessage(main.prefix + "Adjustment: " + ChatColor.RED + "-" + adjustment + ChatColor.GRAY + " -> " + ChatColor.translateAlternateColorCodes('&', main.eloColor) + main.getElo(victim) + ChatColor.RESET + " for losing to " + killer.getName() + ".");
				killer.sendMessage(main.prefix + "Adjustment: " + ChatColor.GREEN + "+" + adjustment + ChatColor.GRAY + " -> " + ChatColor.translateAlternateColorCodes('&', main.eloColor) + main.getElo(killer) + ChatColor.RESET + " for defeating " + victim.getName() + ".");
			} else {
				if (main.disabledWorldMessage) {
					victim.sendMessage(main.prefix + "Elo tracking is not enabled in this world.");
					killer.sendMessage(main.prefix + "Elo tracking is not enabled in this world.");
				}
				main.logger.info(main.consolePrefix + victim.getName() + " lost combat in a non-tracked world. Skipping elo adjustment.");
			}
		}
	}
}
