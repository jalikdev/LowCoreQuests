package dev.jalikdev.lowCoreQuests.reward;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public record XpReward(int amount) implements Reward {

    public XpReward {
        amount = Math.max(1, amount);
    }

    @Override
    public void give(Player player) {
        player.giveExp(amount);
    }

    @Override
    public String display() {
        return amount + " XP";
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "XP");
        m.put("amount", amount);
        return m;
    }
}