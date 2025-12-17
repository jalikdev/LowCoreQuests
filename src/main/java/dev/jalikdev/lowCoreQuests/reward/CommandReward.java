package dev.jalikdev.lowCoreQuests.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CommandReward implements Reward {

    private final String command;

    public CommandReward(String command) {
        this.command = command == null ? "" : command;
    }

    @Override
    public void give(Player player) {
        String cmd = command.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    @Override
    public String display() {
        return "Command";
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "COMMAND");
        m.put("command", command);
        return m;
    }
}
