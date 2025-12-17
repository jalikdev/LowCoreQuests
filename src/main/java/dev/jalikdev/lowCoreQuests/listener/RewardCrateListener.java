package dev.jalikdev.lowCoreQuests.listener;

import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.util.RewardCrate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RewardCrateListener implements Listener {

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack it = e.getItem();
        if (!RewardCrate.isCrate(it)) return;

        e.setCancelled(true);

        Player p = e.getPlayer();
        List<Reward> rewards = RewardCrate.readRewards(it);
        if (rewards.isEmpty()) {
            RewardCrate.consumeOneInHand(p);
            return;
        }

        for (Reward r : rewards) r.give(p);

        RewardCrate.consumeOneInHand(p);
        p.updateInventory();
    }
}
