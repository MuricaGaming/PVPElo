package com.muricagaming.pvpelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class Tabby implements TabCompleter {
    List<String> completions;
    private static final String[] PLAYER_COMMANDS = { "check", "top"};
    private static final String[] ADMIN_COMMANDS = { "adjust", "set", "reset", "resetall", "initial", "baseadj", "maxadj", "maxratio", "maxportion", "prefix", "color", "nametags", "gui", "world", "allworlds", "disabledworldmessage" };
    private List<String> onlinePlayers;
    private Main main;

    public Tabby(Main main) {this.main = main;}
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        completions = new ArrayList<>();
        onlinePlayers = new ArrayList<>();

        for(Player p: main.getServer().getOnlinePlayers()) {
            onlinePlayers.add(p.getName());
        }

        if(args.length == 1 && command.getName().equalsIgnoreCase("pvpeloadmin"))
            StringUtil.copyPartialMatches(args[0], Arrays.asList(ADMIN_COMMANDS), completions);
        else if(args.length == 1)
            StringUtil.copyPartialMatches(args[0], Arrays.asList(PLAYER_COMMANDS), completions);
        else if (args.length == 2) {
                if(args[0].equalsIgnoreCase("adjust") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("check"))
                    StringUtil.copyPartialMatches(args[1], onlinePlayers, completions);
                else if(args[0].equalsIgnoreCase("world")) {
                    ArrayList<String> worldNames = new ArrayList<>();
                    for (World w: main.getServer().getWorlds())
                        worldNames.add(w.getName());
                    StringUtil.copyPartialMatches(args[1], worldNames, completions);
                }
        }

        Collections.sort(completions);
        return completions;
    }
}
