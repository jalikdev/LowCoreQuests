package dev.jalikdev.lowCoreQuests.reward;

import dev.jalikdev.lowCoreQuests.util.RewardCrate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ItemReward implements Reward {

    private final Material material;
    private final int amount;

    public ItemReward(Material material, int amount) {
        this.material = material;
        this.amount = Math.max(1, amount);
    }

    @Override
    public void give(Player player) {
        RewardCrate.giveItemsOrDrop(player, new ItemStack(material, amount));
    }

    @Override
    public String display() {
        return amount + "x " + material.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "ITEM");
        m.put("material", material.name());
        m.put("amount", amount);
        return m;
    }

    public static Reward fromMap(Map<?, ?> raw) {
        Object matObj = raw.get("material");
        Material mat = Material.matchMaterial(matObj == null ? "STONE" : String.valueOf(matObj));
        if (mat == null) return null;

        int amount = Reward.parseInt(raw.get("amount"), 1);
        return new ItemReward(mat, Math.max(1, amount));
    }
}
