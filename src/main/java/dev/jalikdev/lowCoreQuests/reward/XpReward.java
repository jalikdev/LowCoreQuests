package dev.jalikdev.lowCoreQuests.reward;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class XpReward implements Reward {

    private final int amount;

    public XpReward(int amount) {
        this.amount = Math.max(1, amount);
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
