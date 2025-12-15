package dev.jalikdev.lowCoreQuests.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static int remove(Player player, Material mat, int max) {
        if (mat == null || max <= 0) return 0;

        int toRemove = max;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != mat) continue;

            int take = Math.min(toRemove, it.getAmount());
            it.setAmount(it.getAmount() - take);
            toRemove -= take;

            if (it.getAmount() <= 0) contents[i] = null;
            if (toRemove <= 0) break;
        }

        player.getInventory().setContents(contents);
        return max - toRemove;
    }
}
