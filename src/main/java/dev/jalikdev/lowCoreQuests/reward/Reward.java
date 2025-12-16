package dev.jalikdev.lowCoreQuests.reward;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface Reward {

    String display();
    void give(Player player);

    static Reward fromMap(Map<?, ?> map) {
        Object t = map.get("type");
        if (t == null) return null;

        String type = String.valueOf(t).toUpperCase(Locale.ROOT);

        if (type.equals("XP")) {
            int amount = intVal(map.get("amount"), 0);
            return new XpReward(amount);
        }

        if (type.equals("COMMAND")) {
            Object c = map.get("command");
            String cmd = c == null ? "" : String.valueOf(c);
            return new CommandReward(cmd);
        }

        if (type.equals("ITEM")) {
            Object m = map.get("material");
            String matName = m == null ? "STONE" : String.valueOf(m);
            Material mat = Material.matchMaterial(matName);
            if (mat == null) return null;

            int amount = Math.max(1, intVal(map.get("amount"), 1));
            return new ItemReward(mat, amount);
        }

        if (type.equals("BUNDLE")) {
            Object nameObj = map.get("name");
            String name = nameObj == null ? "&fReward Bundle" : String.valueOf(nameObj);

            Object rewardsObj = map.get("rewards");
            if (!(rewardsObj instanceof List<?> list)) return null;

            List<Reward> rewards = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> child) {
                    Reward r = Reward.fromMap(child);
                    if (r != null) rewards.add(r);
                }
            }

            if (rewards.isEmpty()) return null;
            return new BundleReward(name, rewards);
        }

        return null;
    }

    private static int intVal(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    record XpReward(int amount) implements Reward {
        @Override public String display() { return amount + " XP"; }
        @Override public void give(Player player) { player.giveExp(Math.max(0, amount)); }
    }

    record CommandReward(String command) implements Reward {
        @Override public String display() { return "Command"; }
        @Override public void give(Player player) {
            if (command == null || command.isBlank()) return;
            String cmd = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    record ItemReward(Material material, int amount) implements Reward {
        @Override public String display() { return amount + "x " + material.name(); }
        @Override public void give(Player player) { player.getInventory().addItem(new ItemStack(material, amount)); }
    }

    record BundleReward(String name, List<Reward> rewards) implements Reward {
        @Override public String display() { return name; }
        @Override public void give(Player player) {
            for (Reward r : rewards) r.give(player);
        }
    }
}
