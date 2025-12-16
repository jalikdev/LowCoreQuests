package dev.jalikdev.lowCoreQuests.gui;

import dev.jalikdev.lowCoreQuests.model.QuestDefinition;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RewardMenu {

    public static final String TITLE = "Select Reward";

    public static final NamespacedKey KEY_REWARD_INDEX = new NamespacedKey("lowcorequests", "lcq_reward_idx");
    public static final NamespacedKey KEY_QUEST_ID = new NamespacedKey("lowcorequests", "lcq_reward_qid");

    public static Inventory build(QuestDefinition def) {
        Inventory inv = Bukkit.createInventory(null, 27, Text.c("&a" + TITLE));
        fill(inv);

        List<Reward> opts = def.rewards().options();
        int count = Math.min(opts.size(), 7);
        int[] slots = slotsWithGap(count);

        for (int i = 0; i < count; i++) {
            Reward r = opts.get(i);

            ItemStack it = icon(r);
            ItemMeta meta = it.getItemMeta();

            meta.setDisplayName(Text.c("&f" + r.display()));

            List<String> lore = new ArrayList<>();
            lore.add(Text.c("&7Pick exactly one"));
            lore.add(" ");

            if (r instanceof Reward.BundleReward br) {
                lore.add(Text.c("&7Contains:"));
                int shown = 0;
                for (Reward inside : br.rewards()) {
                    lore.add(Text.c("&f- &7" + inside.display()));
                    shown++;
                    if (shown >= 6) break;
                }
                if (br.rewards().size() > 6) lore.add(Text.c("&7..."));
                lore.add(" ");
            }

            lore.add(Text.c("&aClick to claim"));
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(KEY_REWARD_INDEX, PersistentDataType.INTEGER, i);
            meta.getPersistentDataContainer().set(KEY_QUEST_ID, PersistentDataType.STRING, def.id());
            it.setItemMeta(meta);

            inv.setItem(slots[i], it);
        }

        return inv;
    }

    private static int[] slotsWithGap(int count) {
        return switch (count) {
            case 1 -> new int[]{13};
            case 2 -> new int[]{12, 14};
            case 3 -> new int[]{11, 13, 15};
            case 4 -> new int[]{10, 12, 14, 16};
            case 5 -> new int[]{10, 11, 13, 15, 16};
            case 6 -> new int[]{10, 11, 12, 14, 15, 16};
            default -> new int[]{10, 11, 12, 13, 14, 15, 16};
        };
    }

    private static ItemStack icon(Reward r) {
        if (r instanceof Reward.XpReward) return new ItemStack(Material.EXPERIENCE_BOTTLE);
        if (r instanceof Reward.ItemReward) return new ItemStack(Material.CHEST);
        if (r instanceof Reward.CommandReward) return new ItemStack(Material.COMMAND_BLOCK);
        if (r instanceof Reward.BundleReward) return new ItemStack(Material.BUNDLE);
        return new ItemStack(Material.CHEST);
    }

    private static void fill(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);
    }
}

