package dev.jalikdev.lowCoreQuests.listener;

import dev.jalikdev.lowCoreQuests.service.QuestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class QuestJoinListener implements Listener {

    private final QuestService service;

    public QuestJoinListener(QuestService service) {
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        service.load(e.getPlayer().getUniqueId());
    }
}
