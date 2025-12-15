package dev.jalikdev.lowCoreQuests.listener;

import dev.jalikdev.lowCoreQuests.model.*;
import dev.jalikdev.lowCoreQuests.service.QuestService;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class QuestProgressListener implements Listener {

    private final QuestService service;

    public QuestProgressListener(QuestService service) {
        this.service = service;
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        PlayerQuestState st = service.getActive(killer.getUniqueId());
        if (st == null) return;

        QuestDefinition q = service.getQuest(st.questId());
        if (q == null || q.type() != QuestType.KILL_MOB) return;

        if (e.getEntityType() == q.objective().entityType()) {
            service.addProgress(killer, 1);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;

        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player player = e.getPlayer();

        PlayerQuestState st = service.getActive(player.getUniqueId());
        if (st == null) return;

        QuestDefinition q = service.getQuest(st.questId());
        if (q == null || q.type() != QuestType.BIOME) return;

        NamespacedKey current = Registry.BIOME.getKey(e.getTo().getBlock().getBiome());
        if (current == null) return;

        if (current.equals(q.objective().biomeKey())) {
            if (st.progress() < q.objective().required()) {
                service.addProgress(player, 1);
            }
        }
    }
}
