package dev.jalikdev.lowCoreQuests.listener;

import dev.jalikdev.lowCoreQuests.service.QuestService;
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
        service.handleKill(killer, e.getEntityType());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;

        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        service.handleBiome(e.getPlayer(), e.getTo().getBlock().getBiome());
    }
}
