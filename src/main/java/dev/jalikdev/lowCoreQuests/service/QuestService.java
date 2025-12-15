package dev.jalikdev.lowCoreQuests.service;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.config.QuestConfig;
import dev.jalikdev.lowCoreQuests.db.QuestRepository;
import dev.jalikdev.lowCoreQuests.gui.RewardMenu;
import dev.jalikdev.lowCoreQuests.model.PlayerQuestState;
import dev.jalikdev.lowCoreQuests.model.QuestDefinition;
import dev.jalikdev.lowCoreQuests.model.QuestObjectiveDefinition;
import dev.jalikdev.lowCoreQuests.model.QuestType;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.reward.RewardBundle;
import dev.jalikdev.lowCoreQuests.reward.RewardMode;
import dev.jalikdev.lowCoreQuests.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestService {

    private final LowCore core;
    private final QuestConfig config;
    private final QuestRepository repo;

    private final Map<UUID, PlayerQuestState> active = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, Integer>> progress = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public QuestService(LowCore core, QuestConfig config, QuestRepository repo) {
        this.core = core;
        this.config = config;
        this.repo = repo;
    }

    public QuestDefinition getQuest(String id) {
        return config.get(id);
    }

    public PlayerQuestState getActive(UUID uuid) {
        return active.get(uuid);
    }

    public Map<Integer, Integer> getProgressMap(UUID uuid) {
        return progress.getOrDefault(uuid, new HashMap<>());
    }

    public int getProgress(UUID uuid, int idx) {
        return getProgressMap(uuid).getOrDefault(idx, 0);
    }

    public void load(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            repo.loadActive(uuid).ifPresent(state -> {
                active.put(uuid, state);
                Map<Integer, Integer> p = repo.loadProgress(uuid, state.questId());
                progress.put(uuid, p);
            });
        });
    }

    public QuestDefinition startRandom(Player player) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return null;

        List<QuestDefinition> pool = new ArrayList<>(config.enabledQuests());
        if (pool.isEmpty()) return null;

        QuestDefinition chosen = pool.get(random.nextInt(pool.size()));
        if (!start(player, chosen.id())) return null;
        return chosen;
    }

    public boolean start(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return false;

        QuestDefinition def = config.get(questId);
        if (def == null || !def.enabled()) return false;

        PlayerQuestState st = new PlayerQuestState(uuid, questId);
        active.put(uuid, st);
        progress.put(uuid, new ConcurrentHashMap<>());

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            repo.setActive(uuid, questId);
            repo.deleteProgress(uuid, questId);
        });

        return true;
    }

    public boolean cancel(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.remove(uuid);
        progress.remove(uuid);
        if (st == null) return false;

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            repo.deleteProgress(uuid, st.questId());
            repo.deleteActive(uuid);
        });

        return true;
    }

    public boolean canComplete(Player player) {
        PlayerQuestState st = active.get(player.getUniqueId());
        if (st == null) return false;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return false;

        Map<Integer, Integer> p = getProgressMap(player.getUniqueId());
        List<QuestObjectiveDefinition> objs = def.objectives();

        for (int i = 0; i < objs.size(); i++) {
            int cur = p.getOrDefault(i, 0);
            if (cur < objs.get(i).required()) return false;
        }
        return true;
    }

    public void addProgress(Player player, int idx, int add) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return;

        if (idx < 0 || idx >= def.objectives().size()) return;

        QuestObjectiveDefinition obj = def.objectives().get(idx);
        int req = obj.required();
        int cur = getProgress(uuid, idx);
        int next = Math.min(req, cur + Math.max(0, add));
        if (next == cur) return;

        progress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(idx, next);

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> repo.upsertProgress(uuid, st.questId(), idx, next));
    }

    public int turnInItems(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return 0;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return 0;

        int totalAdded = 0;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.ITEM) continue;

            int cur = getProgress(uuid, i);
            int remaining = Math.max(0, obj.required() - cur);
            if (remaining <= 0) continue;

            int removed = InventoryUtil.remove(player, obj.material(), remaining);
            if (removed > 0) {
                addProgress(player, i, removed);
                totalAdded += removed;
            }
        }

        return totalAdded;
    }

    public void handleKill(Player player, org.bukkit.entity.EntityType killed) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.KILL_MOB) continue;
            if (obj.entityType() == killed) addProgress(player, i, 1);
        }
    }

    public void handleBiome(Player player, org.bukkit.block.Biome biome) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return;

        NamespacedKey current = Registry.BIOME.getKey(biome);
        if (current == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.BIOME) continue;
            if (obj.biomeKey().equals(current)) addProgress(player, i, 1);
        }
    }

    public void completeOrOpenRewards(Player player) {
        PlayerQuestState st = active.get(player.getUniqueId());
        if (st == null) return;

        QuestDefinition def = config.get(st.questId());
        if (def == null) return;

        if (!canComplete(player)) return;

        RewardBundle bundle = def.rewards();
        List<Reward> options = bundle.options();

        if (options == null || options.isEmpty()) {
            finish(player.getUniqueId(), st.questId());
            player.sendMessage(core.getPrefix() + "Quest completed.");
            return;
        }

        if (bundle.mode() == RewardMode.CHOICE && options.size() > 1) {
            player.openInventory(RewardMenu.build(core, def));
            return;
        }

        if (bundle.mode() == RewardMode.RANDOM) {
            Reward r = options.get(random.nextInt(options.size()));
            r.give(player);
            finish(player.getUniqueId(), st.questId());
            player.sendMessage(core.getPrefix() + "Quest completed. Reward: " + r.display());
            return;
        }

        for (Reward r : options) r.give(player);
        finish(player.getUniqueId(), st.questId());
        player.sendMessage(core.getPrefix() + "Quest completed.");
    }

    public void claimChoice(Player player, String questId, int index) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null || !st.questId().equals(questId)) return;

        QuestDefinition def = config.get(questId);
        if (def == null) return;

        if (!canComplete(player)) return;

        List<Reward> options = def.rewards().options();
        if (index < 0 || index >= options.size()) return;

        options.get(index).give(player);
        finish(uuid, questId);
        player.sendMessage(core.getPrefix() + "Quest completed.");
    }

    private void finish(UUID uuid, String questId) {
        active.remove(uuid);
        progress.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            repo.deleteProgress(uuid, questId);
            repo.deleteActive(uuid);
        });
    }
}
