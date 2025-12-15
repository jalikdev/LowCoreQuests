package dev.jalikdev.lowCoreQuests.service;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.config.QuestConfig;
import dev.jalikdev.lowCoreQuests.db.QuestRepository;
import dev.jalikdev.lowCoreQuests.gui.RewardMenu;
import dev.jalikdev.lowCoreQuests.model.*;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestService {

    private final LowCore core;
    private final QuestConfig config;
    private final QuestRepository repo;

    private final Map<UUID, PlayerQuestState> active = new ConcurrentHashMap<>();

    public QuestService(LowCore core, QuestConfig config, QuestRepository repo) {
        this.core = core;
        this.config = config;
        this.repo = repo;
    }

    public QuestDefinition getQuest(String id) {
        return config.get(id);
    }

    public Collection<QuestDefinition> enabledQuests() {
        return config.enabledQuests();
    }

    public PlayerQuestState getActive(UUID uuid) {
        return active.get(uuid);
    }

    public void load(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> repo.load(uuid).ifPresent(q -> active.put(uuid, q)));
    }

    public void flushAll() {
        for (PlayerQuestState st : active.values()) repo.upsert(st);
    }

    public boolean start(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return false;

        QuestDefinition def = config.get(questId);
        if (def == null || !def.enabled()) return false;

        PlayerQuestState st = new PlayerQuestState(uuid, questId, 0);
        active.put(uuid, st);
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> repo.upsert(st));
        return true;
    }

    public boolean cancel(Player player) {
        UUID uuid = player.getUniqueId();
        if (!active.containsKey(uuid)) return false;

        active.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> repo.delete(uuid));
        return true;
    }

    public void addProgress(Player player, int add) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return;

        int req = def.objective().required();
        int newProg = Math.min(req, st.progress() + Math.max(0, add));

        PlayerQuestState updated = new PlayerQuestState(uuid, st.questId(), newProg);
        active.put(uuid, updated);
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> repo.upsert(updated));
    }

    public boolean canComplete(Player player) {
        PlayerQuestState st = active.get(player.getUniqueId());
        if (st == null) return false;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return false;

        return st.progress() >= def.objective().required();
    }

    public int turnInItems(Player player) {
        PlayerQuestState st = active.get(player.getUniqueId());
        if (st == null) return 0;

        QuestDefinition def = config.get(st.questId());
        if (def == null || def.type() != QuestType.ITEM) return 0;

        var mat = def.objective().material();
        int required = def.objective().required();
        int remaining = Math.max(0, required - st.progress());
        if (remaining == 0) return 0;

        int removed = InventoryUtil.remove(player, mat, remaining);
        if (removed > 0) addProgress(player, removed);
        return removed;
    }

    public void completeOrOpenRewards(Player player) {
        PlayerQuestState st = active.get(player.getUniqueId());
        if (st == null) return;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return;

        if (st.progress() < def.objective().required()) return;

        if (def.rewards().choice() && def.rewards().options().size() > 1) {
            player.openInventory(RewardMenu.build(core, def));
            return;
        }

        if (!def.rewards().options().isEmpty()) {
            def.rewards().options().getFirst().give(player);
        }

        finish(player.getUniqueId());
        player.sendMessage(core.getPrefix() + "Quest completed.");
    }

    public void claimChoice(Player player, String questId, int index) {
        PlayerQuestState st = active.get(player.getUniqueId());
        if (st == null || !st.questId().equals(questId)) return;

        QuestDefinition def = config.get(questId);
        if (def == null) return;

        if (st.progress() < def.objective().required()) return;

        if (index < 0 || index >= def.rewards().options().size()) return;

        def.rewards().options().get(index).give(player);

        finish(player.getUniqueId());
        player.sendMessage(core.getPrefix() + "Quest completed.");
    }

    private void finish(UUID uuid) {
        active.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> repo.delete(uuid));
    }
}
