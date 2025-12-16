package dev.jalikdev.lowCoreQuests.service;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.config.QuestConfig;
import dev.jalikdev.lowCoreQuests.config.StoryConfig;
import dev.jalikdev.lowCoreQuests.db.QuestRepository;
import dev.jalikdev.lowCoreQuests.db.StoryRepository;
import dev.jalikdev.lowCoreQuests.db.StatsRepository;
import dev.jalikdev.lowCoreQuests.gui.RewardMenu;
import dev.jalikdev.lowCoreQuests.model.*;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.reward.RewardBundle;
import dev.jalikdev.lowCoreQuests.reward.RewardMode;
import dev.jalikdev.lowCoreQuests.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestService {

    private final LowCore core;

    private final QuestConfig questConfig;
    private final StoryConfig storyConfig;

    private final QuestRepository questRepo;
    private final StoryRepository storyRepo;
    private final StatsRepository statsRepo;

    private final Map<UUID, PlayerQuestState> active = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, Integer>> progress = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> completedStoryIndex = new ConcurrentHashMap<>();
    private final Map<UUID, StatsRepository.Stats> statsCache = new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastStructureCheck = new ConcurrentHashMap<>();
    private static final long STRUCTURE_CHECK_COOLDOWN_MS = 3000L;

    private final Random random = new Random();

    public QuestService(LowCore core, QuestConfig questConfig, StoryConfig storyConfig,
                        QuestRepository questRepo, StoryRepository storyRepo, StatsRepository statsRepo) {
        this.core = core;
        this.questConfig = questConfig;
        this.storyConfig = storyConfig;
        this.questRepo = questRepo;
        this.storyRepo = storyRepo;
        this.statsRepo = statsRepo;
    }

    public void load(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            int completed = storyRepo.loadCompletedIndex(uuid);
            completedStoryIndex.put(uuid, completed);

            StatsRepository.Stats s = statsRepo.load(uuid);
            statsCache.put(uuid, s);

            questRepo.loadActive(uuid).ifPresent(state -> {
                active.put(uuid, state);
                Map<Integer, Integer> p = questRepo.loadProgress(uuid, state.questId());
                progress.put(uuid, new ConcurrentHashMap<>(p));
            });
        });
    }

    public PlayerQuestState getActive(UUID uuid) { return active.get(uuid); }

    public Map<Integer, Integer> getProgressMap(UUID uuid) { return progress.getOrDefault(uuid, Map.of()); }

    public int getCompletedStoryIndex(UUID uuid) { return completedStoryIndex.getOrDefault(uuid, 0); }

    public StatsRepository.Stats getStats(UUID uuid) { return statsCache.getOrDefault(uuid, new StatsRepository.Stats(0, 0, 0)); }

    public QuestDefinition getActiveQuestDefinition(UUID uuid) {
        PlayerQuestState st = active.get(uuid);
        if (st == null) return null;
        return getQuestById(st.questId());
    }

    public QuestDefinition getQuestById(String id) {
        QuestDefinition s = storyConfig.isStoryQuest(id) ? findStoryById(id) : null;
        if (s != null) return s;
        return questConfig.get(id);
    }

    private QuestDefinition findStoryById(String id) {
        int idx = storyConfig.getStoryIndex(id);
        if (idx <= 0) return null;
        return storyConfig.getNextByCompletedIndex(idx - 1);
    }

    public QuestDefinition getNextStory(UUID uuid) { return storyConfig.getNextByCompletedIndex(getCompletedStoryIndex(uuid)); }

    public QuestDefinition startNextStory(Player player) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return null;

        QuestDefinition next = getNextStory(uuid);
        if (next == null) return null;

        boolean ok = start(player, next.id());
        return ok ? next : null;
    }

    public QuestDefinition startRandom(Player player) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return null;

        List<QuestDefinition> pool = new ArrayList<>(questConfig.enabledQuests());
        if (pool.isEmpty()) return null;

        QuestDefinition chosen = pool.get(random.nextInt(pool.size()));
        boolean ok = start(player, chosen.id());
        return ok ? chosen : null;
    }

    public boolean start(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return false;

        QuestDefinition def = getQuestById(questId);
        if (def == null || !def.enabled()) return false;

        PlayerQuestState st = new PlayerQuestState(uuid, questId);
        active.put(uuid, st);
        progress.put(uuid, new ConcurrentHashMap<>());

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            questRepo.setActive(uuid, questId);
            questRepo.deleteProgress(uuid, questId);
        });

        return true;
    }

    public boolean cancel(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.remove(uuid);
        progress.remove(uuid);
        if (st == null) return false;

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            questRepo.deleteProgress(uuid, st.questId());
            questRepo.deleteActive(uuid);
        });

        return true;
    }

    public void handleKill(Player player, EntityType killed) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.KILL_MOB) continue;
            if (obj.entityType() == killed) addProgress(player, i, 1);
        }
    }

    public void handleBiome(Player player, Biome biome) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return;

        NamespacedKey current = Registry.BIOME.getKey(biome);
        if (current == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.BIOME) continue;
            if (obj.biomeKey().equals(current)) setProgress(player, i, 1);
        }
    }

    public void handleStructure(Player player) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return;

        boolean hasStructure = def.objectives().stream().anyMatch(o -> o.type() == QuestType.STRUCTURE);
        if (!hasStructure) return;

        long now = System.currentTimeMillis();
        long last = lastStructureCheck.getOrDefault(uuid, 0L);
        if (now - last < STRUCTURE_CHECK_COOLDOWN_MS) return;
        lastStructureCheck.put(uuid, now);

        Location loc = player.getLocation();

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.STRUCTURE) continue;

            int cur = getProgress(uuid, i);
            if (cur >= obj.required()) continue;

            NamespacedKey key = obj.structureKey();
            if (key == null) continue;

            Structure structure = Registry.STRUCTURE.get(key);
            if (structure == null) continue;

            int radius = Math.max(1, obj.searchRadius());
            int near = Math.max(16, obj.nearBlocks());

            Location found = loc.getWorld().locateNearestStructure(loc, structure, radius, false);
            if (found == null) continue;

            if (!found.getWorld().equals(loc.getWorld())) continue;
            if (found.distanceSquared(loc) <= (double) near * (double) near) {
                setProgress(player, i, obj.required());
            }
        }
    }

    public void syncCollectProgress(Player player) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.COLLECT) continue;
            int count = InventoryUtil.count(player, obj.material());
            setProgress(player, i, count);
        }
    }

    public int turnInItems(Player player) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return 0;

        int totalAdded = 0;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition obj = def.objectives().get(i);
            if (obj.type() != QuestType.DELIVER) continue;

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

    public boolean canComplete(Player player) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return false;

        Map<Integer, Integer> p = getProgressMap(uuid);
        for (int i = 0; i < def.objectives().size(); i++) {
            int cur = p.getOrDefault(i, 0);
            if (cur < def.objectives().get(i).required()) return false;
        }
        return true;
    }

    public void completeOrOpenRewards(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return;

        syncCollectProgress(player);

        QuestDefinition def = getQuestById(st.questId());
        if (def == null) return;
        if (!canComplete(player)) return;

        RewardBundle bundle = def.rewards();
        List<Reward> options = bundle.options();

        if (options == null || options.isEmpty()) {
            finishAndMaybeAdvanceStory(uuid, st.questId());
            player.sendMessage(core.getPrefix() + "Quest completed.");
            return;
        }

        if (bundle.mode() == RewardMode.CHOICE && options.size() > 1) {
            player.openInventory(RewardMenu.build(def));
            return;
        }

        if (bundle.mode() == RewardMode.RANDOM) {
            Reward r = options.get(random.nextInt(options.size()));
            r.give(player);
            finishAndMaybeAdvanceStory(uuid, st.questId());
            player.sendMessage(core.getPrefix() + "Quest completed. Reward: " + r.display());
            return;
        }

        for (Reward r : options) r.give(player);
        finishAndMaybeAdvanceStory(uuid, st.questId());
        player.sendMessage(core.getPrefix() + "Quest completed.");
    }

    public void claimChoice(Player player, String questId, int index) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null || !st.questId().equals(questId)) return;

        syncCollectProgress(player);

        QuestDefinition def = getQuestById(questId);
        if (def == null) return;
        if (!canComplete(player)) return;

        List<Reward> options = def.rewards().options();
        if (index < 0 || index >= options.size()) return;

        options.get(index).give(player);
        finishAndMaybeAdvanceStory(uuid, questId);
        player.sendMessage(core.getPrefix() + "Quest completed.");
    }

    private int getProgress(UUID uuid, int idx) {
        return progress.getOrDefault(uuid, Map.of()).getOrDefault(idx, 0);
    }

    private void addProgress(Player player, int idx, int add) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return;
        if (idx < 0 || idx >= def.objectives().size()) return;

        int cur = getProgress(uuid, idx);
        setProgress(player, idx, cur + Math.max(0, add));
    }

    private void setProgress(Player player, int idx, int value) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return;

        QuestDefinition def = getQuestById(st.questId());
        if (def == null) return;

        QuestObjectiveDefinition obj = def.objectives().get(idx);
        int next = Math.min(obj.required(), Math.max(0, value));
        int cur = getProgress(uuid, idx);
        if (next == cur) return;

        progress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(idx, next);
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> questRepo.upsertProgress(uuid, st.questId(), idx, next));
    }

    private void finishAndMaybeAdvanceStory(UUID uuid, String questId) {
        boolean isStory = storyConfig.isStoryQuest(questId);
        int storyIndex = storyConfig.getStoryIndex(questId);

        active.remove(uuid);
        progress.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            questRepo.deleteProgress(uuid, questId);
            questRepo.deleteActive(uuid);

            statsRepo.increment(uuid, isStory);
            StatsRepository.Stats cur = statsCache.getOrDefault(uuid, new StatsRepository.Stats(0, 0, 0));
            statsCache.put(uuid, new StatsRepository.Stats(
                    cur.total() + 1,
                    cur.story() + (isStory ? 1 : 0),
                    cur.random() + (isStory ? 0 : 1)
            ));

            if (isStory && storyIndex > 0) {
                int currentCompleted = storyRepo.loadCompletedIndex(uuid);
                int expectedNext = currentCompleted + 1;

                if (storyIndex == expectedNext) {
                    int newCompleted = currentCompleted + 1;
                    storyRepo.setCompletedIndex(uuid, newCompleted);
                    completedStoryIndex.put(uuid, newCompleted);
                }
            }
        });
    }
}
