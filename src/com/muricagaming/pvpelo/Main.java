package com.muricagaming.pvpelo;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import com.nametagedit.plugin.NametagEdit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.Map.Entry;

public class Main extends JavaPlugin {
	public final Logger logger = Logger.getLogger("Minecraft");
	JoinListener jl;
	DamageListener dl;
	ChatListener cl;
	InventoryListener il;
	HashMap<UUID, Integer> players = new HashMap<>();
	HashMap<UUID, Integer> playersToSort;
	List<Entry<UUID, Integer>> sortedPlayers;
	String prefix;
	String eloColor;
	int initialElo;
	int minElo;
	int maxElo;
	int baseAdjustment;
	int maxAdjustment;
	int maxRatio;
	double maxPortion;
	boolean nametags;
	boolean detectNametagEdit;
	boolean leaderboardGUI;
	Inventory leaderboard;
	OfflinePlayer p;
	String username;
	UUID id;
	int test;
	double testd;

	public void onEnable() {
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		saveConfig();

		// Check for TagAPI. If not found, nametag support disabled
		if (getServer().getPluginManager().getPlugin("NametagEdit") != null) {
			logger.info("[PvP Elo] Detected NametagEdit! Nametag support enabled.");
			detectNametagEdit = true;
		} else {
			logger.info("[PvP Elo] NametagEdit not detected! Nametag support disabled.");
			detectNametagEdit = false;
		}

        loadPlayers(true);

		prefix = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("prefix"))) + ChatColor.RESET + " ";
		eloColor = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("elo-color")));
		initialElo = getConfig().getInt("initial-elo");
		nametags = getConfig().getBoolean("nametags");
		leaderboardGUI = getConfig().getBoolean("leaderboard-gui");

		// Minimum and maximum hard-coded to prevent bullying
		minElo = 1;
		maxElo = Integer.MAX_VALUE;
		baseAdjustment = getConfig().getInt("base-adjustment");
		maxAdjustment = getConfig().getInt("max-adjustment");
		maxRatio = getConfig().getInt("max-ratio");
		maxPortion = getConfig().getDouble("max-portion");

		getConfig().set("max-elo", null);
		getConfig().set("min-elo", null);

		jl = new JoinListener(this);
		cl = new ChatListener(this);
		dl = new DamageListener(this);
		il = new InventoryListener(this);

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(jl, this);
		pm.registerEvents(cl, this);
		pm.registerEvents(dl, this);
		pm.registerEvents(il, this);

		Objects.requireNonNull(getCommand("pvpelo")).setTabCompleter(new Tabby(this));
		Objects.requireNonNull(getCommand("pvpeloadmin")).setTabCompleter(new Tabby(this));

		logger.info("[PvP Elo] PvP Elo has been enabled!");
	}

	public void onDisable() {
		savePlayers(true);
		logger.info("[PvP Elo] PvP Elo has been disabled!");
	}

	@SuppressWarnings( "deprecation" )
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("pvpelo")) {
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("check") && sender instanceof Player) {
					if (args.length == 1) {
						try {
							id = Objects.requireNonNull(getServer().getPlayer(sender.getName())).getUniqueId();
							sender.sendMessage(prefix + ChatColor.DARK_GRAY + "====" + ChatColor.RESET);
							sender.sendMessage(prefix + ChatColor.GREEN + "Your stats: ");
							sender.sendMessage(prefix + "  - Rating: " + players.get(id));
							sender.sendMessage(prefix + "  - Rank: " + getLeaderboardPosition((Player) sender) + " of " + sortedPlayers.size());
							sender.sendMessage(prefix + "  - Better than " + getPercentile((Player) sender) + " of players.");
							sender.sendMessage(prefix + ChatColor.DARK_GRAY + "====" + ChatColor.RESET);
						} catch (Exception e) {
							sender.sendMessage(prefix + "Error: Could not find your rating.");
						}
					} else if (args.length == 2) {
						try {
							p = Bukkit.getServer().getOfflinePlayer(args[1]);
							id = p.getUniqueId();
							if (getElo(p) != -1) {
								sender.sendMessage(prefix + ChatColor.DARK_GRAY + "====" + ChatColor.RESET);
								sender.sendMessage(prefix + ChatColor.GREEN + p.getName() + "'s stats: ");
								sender.sendMessage(prefix + "  - Rating: " + players.get(id));
								sender.sendMessage(prefix + "  - Rank: " + getLeaderboardPosition(p) + " of " + sortedPlayers.size());
								sender.sendMessage(prefix + "- Better than " + getPercentile(p) + " of players.");
								sender.sendMessage(prefix + ChatColor.DARK_GRAY + "====" + ChatColor.RESET);
							} else
								throw new Exception();
						} catch (Exception e) {
							sender.sendMessage(prefix + "Error: Could not find that player's rating.");
						}
					} else
						sender.sendMessage(prefix + "Format: /elo check OR /elo check <player>");
				} else if (args[0].equalsIgnoreCase("top")) {
					if (leaderboardGUI) {
						sender.sendMessage(prefix + "Pulling up the leaderboard...");
						createGUI((Player) sender);
						openLeaderboard((Player) sender);
					} else {
						sortPlayers();

						sender.sendMessage(prefix + "Top 10 players:\n");

						for (int i = 0; i < 10; i++) {
							if (i >= sortedPlayers.size())
								break;
							else if (getServer().getOfflinePlayer(sortedPlayers.get(i).getKey()).getName() != null) {
								if (i == 0)
									sender.sendMessage("- " + ChatColor.YELLOW + getServer().getOfflinePlayer(sortedPlayers.get(i).getKey()).getName() + ChatColor.DARK_GRAY + " | " + ChatColor.translateAlternateColorCodes('&', eloColor) + sortedPlayers.get(i).getValue() + ChatColor.RESET);
								else if (i == 1)
									sender.sendMessage("- " + ChatColor.GRAY + getServer().getOfflinePlayer(sortedPlayers.get(i).getKey()).getName() + ChatColor.DARK_GRAY + " | " + ChatColor.translateAlternateColorCodes('&', eloColor) + sortedPlayers.get(i).getValue() + ChatColor.RESET);
								else if (i == 2)
									sender.sendMessage("- " + ChatColor.GOLD + getServer().getOfflinePlayer(sortedPlayers.get(i).getKey()).getName() + ChatColor.DARK_GRAY + " | " + ChatColor.translateAlternateColorCodes('&', eloColor) + sortedPlayers.get(i).getValue() + ChatColor.RESET);
								else
									sender.sendMessage("- " + getServer().getOfflinePlayer(sortedPlayers.get(i).getKey()).getName() + ChatColor.DARK_GRAY + " | " + ChatColor.translateAlternateColorCodes('&', eloColor) + sortedPlayers.get(i).getValue() + ChatColor.RESET);
							}
						}
					}
				} else
					sender.sendMessage(prefix + "Error: Incorrect arguments!\n" + prefix + "Acceptable options:\n" + prefix + "check, top");
			} else
				sender.sendMessage(prefix + "Error: Incorrect arguments!\n" + prefix + "Acceptable options:\n" + prefix + "check, top");
		} else if (cmd.getName().equalsIgnoreCase("pvpeloadmin")) {
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("adjust")) {
					username = args[1];
					try {
						int change = Integer.parseInt(args[2]);
						p = Bukkit.getServer().getOfflinePlayer(username);
						UUID id = p.getUniqueId();
						setElo(p, players.get(id) + change);
						sender.sendMessage(prefix + "Adjusted " + username + "'s elo by " + change + ".");
						sender.sendMessage(prefix + "New rating: "+ ChatColor.translateAlternateColorCodes('&', eloColor) + getElo(p));
					} catch (Exception e) {
						sender.sendMessage(prefix + "Error: Could not find " + username + " or incorrect int value.");
					}
				} else if (args[0].equalsIgnoreCase("set")) {
					username = args[1];

					try {
						int newElo = Integer.parseInt(args[2]);
						p = getServer().getOfflinePlayer(username);
						UUID id = p.getUniqueId();
						setElo(p, newElo);
						sender.sendMessage(prefix + "Set " + username + "'s rating to " + players.get(id) + ".");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Error: Could not find " + username + " or incorrect int value.");
					}
				} else if (args[0].equalsIgnoreCase("reset")) {
					username = args[1];

					try {
						p = getServer().getOfflinePlayer(username);
						setElo(p, initialElo);
						sender.sendMessage(prefix + "Reset " + username + "'s rating to " + initialElo + ".");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Error: Could not find " + username + ".");
					}
				} else if (args[0].equalsIgnoreCase("resetall")) {
					for (Entry<UUID, Integer> e : players.entrySet()) {
						id = e.getKey();
						if (!Objects.equals(id.toString(), "00000000-0000-0000-0000-000000000000")) {
							p = getServer().getOfflinePlayer(id);
							setElo(p, initialElo);
						}
					}

					sender.sendMessage(prefix + "Reset ALL players' ratings to " + initialElo + ".");
				} else if (args[0].equalsIgnoreCase("initial")) {
					try {
						initialElo = Integer.parseInt(args[1]);
						saveToConfig("initial-elo", initialElo);
						sender.sendMessage(prefix + "Set the initial rating to " + initialElo + ".");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Initial rating: " + initialElo);
					}
				} else if (args[0].equalsIgnoreCase("baseadj")) {
					try {
						test = Integer.parseInt(args[1]);

						if (test <= maxAdjustment) {
							baseAdjustment = test;
							saveToConfig("base-adjustment", baseAdjustment);
							sender.sendMessage(prefix + "Set the base adjustment to " + baseAdjustment + ".");
						} else
							sender.sendMessage(prefix + "Error: Base adjustment cannot be higher than max adjustment.");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Base adjustment: " + baseAdjustment);
					}
				} else if (args[0].equalsIgnoreCase("maxadj")) {
					try {
						test = Integer.parseInt(args[1]);

						if (test >= baseAdjustment) {
							maxAdjustment = test;
							saveToConfig("max-adjustment", maxAdjustment);
							sender.sendMessage(prefix + "Set the max adjustment to " + maxAdjustment + ".");
						} else
							sender.sendMessage(prefix + "Error: Max adjustment cannot be lower than base adjustment.");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Max adjustment: " + maxAdjustment);
					}
				} else if (args[0].equalsIgnoreCase("maxratio")) {
					try {
						maxRatio = Integer.parseInt(args[1]);
						saveToConfig("max-ratio", maxRatio);
						sender.sendMessage(prefix + "Set the max ratio to " + maxRatio + ":1");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Max ratio: " + maxRatio + ":1");
					}
				} else if (args[0].equalsIgnoreCase("maxportion")) {
					try {
						testd = Double.parseDouble(args[1]);

						if (0.0 < testd && testd <= 1.0) {
							maxPortion = testd;
							saveToConfig("max-portion", maxPortion);
							sender.sendMessage(prefix + "Set the max portion to " + 100 * maxPortion + "%");
						}
					} catch (Exception e) {
						sender.sendMessage(prefix + "Max portion: " + 100 * maxPortion + "%");
					}
				} else if (args[0].equalsIgnoreCase("prefix")) {
					try {
						StringBuilder sb = new StringBuilder();

						for (int i = 1; i < args.length; i++)
							if (i != args.length - 1)
								sb.append(args[i]).append(" ");
							else
								sb.append(args[i]);

						prefix = ChatColor.translateAlternateColorCodes('&', sb.toString()) + ChatColor.RESET + " ";
						saveToConfig("prefix", prefix);

						sender.sendMessage(prefix + "New command prefix set!");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Format: /eloadmin prefix <prefix>");
					}
				} else if (args[0].equalsIgnoreCase("color")) {
					try {
						boolean success = false;
						char[] code = args[1].toCharArray();
						char[] validChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'};
						for (char c : validChars)
							if (code[1] == c && code[0] == '&') {
								saveToConfig("elo-color", args[1]);
								eloColor = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("elo-color")));
								sender.sendMessage(prefix + "Set the elo color to " + args[1] + ".");
								updateAllNametags(sender);
								success = true;
								break;
							}
						if (!success)
							sender.sendMessage(prefix + "Invalid input. Only \"&\" codes are accepted.");
					} catch (Exception e) {
						sender.sendMessage(prefix + "Invalid input. Only \"&\" codes are accepted.");
					}
				} else if (args[0].equalsIgnoreCase("nametags")) {
					try {
						if (nametags) {
							saveToConfig("nametags", false);
							nametags = false;
						} else {
							saveToConfig("nametags", true);
							nametags = true;
						}
						sender.sendMessage(prefix + "Nametag status updated to " + nametags + ". Tags will update shortly.");
						updateAllNametags(sender);
					} catch (Exception e) {
						sender.sendMessage(prefix + "Error: Invalid value.");
					}
				}
				else if(args[0].equalsIgnoreCase("gui")) {
					if(leaderboardGUI) {
						leaderboardGUI = false;
						saveToConfig("leaderboard-gui", false);
						sender.sendMessage(prefix + "Leaderboard GUI disabled. /elo top will now display in chat.");
					}
					else {
						leaderboardGUI = true;
						saveToConfig("leaderboard-gui", true);
						sender.sendMessage(prefix + "Leaderboard GUI enabled. /elo top will now show a GUI.");
					}
				}
				else if (args[0].equalsIgnoreCase("reload")) {
					savePlayers(true);
					onEnable();
					sender.sendMessage(prefix + "Plugin config reloaded! Player stats not touched.");
				} else
					sender.sendMessage(prefix + "Error: Incorrect arguments!\n" + prefix + "Acceptable options:\n" + prefix + "adjust, set, reset, resetall, initial, baseadj, maxadj, maxratio, maxportion, prefix, color, nametags, gui");
			} else
				sender.sendMessage(prefix + "Error: Incorrect arguments!\n" + prefix + "Acceptable options:\n" + prefix + "adjust, set, reset, resetall, initial, baseadj, maxadj, maxratio, maxportion, prefix, color, nametags, gui");
		}
		return false;
	}

	private void loadPlayers(boolean init) {
		ConfigurationSection section = getConfig().getConfigurationSection("players");

        assert section != null;
        for (String idString : section.getKeys(false)) {
			players.put(UUID.fromString(idString), section.getInt(idString));
			if (init)
				logger.info("[PvP Elo] LOAD | " + UUID.fromString(idString) + " | " + section.getInt(idString));
		}
	}

	public void savePlayers(boolean shutdown) {

        for (Entry<UUID, Integer> pair : players.entrySet()) {
            saveToConfig("players." + pair.getKey().toString(), pair.getValue());
            if (shutdown)
                logger.info("[PvP Elo] SAVE | " + pair.getKey().toString() + " | " + pair.getValue().toString());
        }
	}


	@SuppressWarnings("unused")
	private void reloadPlayers() {
		savePlayers(false);
		loadPlayers(false);
	}

	public void addPlayer(Player p) {
		players.put(p.getUniqueId(), initialElo);
		saveToConfig("players." + p.getUniqueId(), getElo(p));
		updateNametag(p);
	}

	public int getElo(OfflinePlayer p) {
		return players.getOrDefault(p.getUniqueId(), -1);
	}

	public void setElo(OfflinePlayer p, int elo) {
		if (elo > maxElo)
			elo = maxElo;
		else if (elo < minElo)
			elo = minElo;

		players.put(p.getUniqueId(), elo);
		saveToConfig("players." + p.getUniqueId(), elo);
		updateNametag(getServer().getPlayer(p.getUniqueId()));
	}

	private void saveToConfig(String key, Object s) {
		getConfig().set(key, s);
		saveConfig();
	}

	public void updateNametag(Player p) {
		// Add rating to nametag, if enabled
		if (nametags && detectNametagEdit) {
			NametagEdit.getApi().setSuffix(p, ChatColor.DARK_GRAY + " | " + ChatColor.RESET + eloColor + getElo(p));
		} else if (detectNametagEdit) {
			NametagEdit.getApi().setSuffix(p, null);
		}
	}

	private void updateAllNametags(CommandSender sender) {
		try {
			for (Player p : this.getServer().getOnlinePlayers()) {
				updateNametag(p);
			}
		} catch (Exception e) {
			sender.sendMessage(prefix + "Error: Incompatible version of either ProtocolLib or TagAPI.");
			logger.warning(prefix + "Error: Incompatible version of either ProtocolLib or TagAPI.");
		}
	}

	private void sortPlayers() {
		playersToSort = players;
		playersToSort.remove(UUID.fromString("00000000-0000-0000-0000-000000000000"));
		sortedPlayers = new ArrayList<>(playersToSort.entrySet());

		sortedPlayers.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
	}

	public int getLeaderboardPosition(OfflinePlayer p) {
		sortPlayers();
		int position = 0;

		// Get position via index in list + 1
		for (int i = 0; i < sortedPlayers.size(); i++)
			if (sortedPlayers.get(i).getKey().toString().equalsIgnoreCase(p.getUniqueId().toString()))
				position = i + 1;

		return position;
	}

	public String getPercentile(OfflinePlayer p) {
		String format = "#.#";
		double rawPercentile;
		DecimalFormat df = new DecimalFormat(format);

		rawPercentile = 100.0 - (100.0 * ((double) (getLeaderboardPosition(p) - 1) / (double) players.size()));

		return df.format(rawPercentile) + "%";
	}

	private void createGUI(Player p) {
		leaderboard = Bukkit.createInventory(null, 18, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Leaderboard");
		createLeaderboard(p);
	}

	private void createLeaderboard(Player p) {
		sortPlayers();
		OfflinePlayer op;
		String[] lore = new String[3];
		leaderboard.setMaxStackSize(1);

		for (int i = 0; i < sortedPlayers.size(); i++) {
			if (i > 8)
				break;
			else {
				op = getServer().getOfflinePlayer(sortedPlayers.get(i).getKey());

				lore[0] = ChatColor.WHITE + "Rating: "+ ChatColor.translateAlternateColorCodes('&', eloColor) + getElo(op);
				lore[1] = ChatColor.WHITE + "Rank: " + getLeaderboardPosition(op) + " of " + sortedPlayers.size();
				lore[2] = ChatColor.WHITE + "Percentile: " + getPercentile(op);
				if(i == 0)
					leaderboard.addItem(createHead(ChatColor.YELLOW + op.getName(), lore, op));
				else if (i == 1)
					leaderboard.addItem(createHead(ChatColor.GRAY + op.getName(), lore, op));
				else if (i == 2)
					leaderboard.addItem(createHead(ChatColor.GOLD + op.getName(), lore, op));
				else
					leaderboard.addItem(createHead(ChatColor.AQUA + op.getName(), lore, op));
			}
		}

		for(int i = sortedPlayers.size(); i < 9; i++)
			leaderboard.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1));

		leaderboard.setItem(9, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));
		leaderboard.setItem(10, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));
		leaderboard.setItem(11, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));
		leaderboard.setItem(12, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));

		lore[0] = ChatColor.WHITE + "Rating: " + ChatColor.translateAlternateColorCodes('&', eloColor) + getElo(p);
		lore[1] = ChatColor.WHITE + "Rank: " + getLeaderboardPosition(p) + " of " + sortedPlayers.size();
		lore[2] = ChatColor.WHITE + "Percentile: " + getPercentile(p);
		leaderboard.setItem(13, createHead(ChatColor.GREEN + "Your stats:", lore, p));

		leaderboard.setItem(14, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));
		leaderboard.setItem(15, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));
		leaderboard.setItem(16, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));
		leaderboard.setItem(17, new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1));
	}

	private ItemStack createHead(String name, String[] lore, OfflinePlayer p) {
		ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) head.getItemMeta();

        assert meta != null;
        meta.setDisplayName(ChatColor.RESET + name);
		meta.setOwningPlayer(p);
		meta.setLore(Arrays.asList(lore));
		head.setItemMeta(meta);

		return head;
	}

	private void openLeaderboard(HumanEntity player) {
		player.openInventory(leaderboard);
	}
}
