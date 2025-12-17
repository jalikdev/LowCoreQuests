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
import dev.jalikdev.lowCoreQuests.util.RewardCrate;
import dev.jalikdev.lowCoreQuests.util.StructureUtil;
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
    private static final double EXTREME_CHANCE = 0.03;

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
            completedStoryIndex.put(uuid, storyRepo.loadCompletedIndex(uuid));
            statsCache.put(uuid, statsRepo.load(uuid));

            questRepo.loadActive(uuid).ifPresent(state -> {
                active.put(uuid, state);
                progress.put(uuid, new ConcurrentHashMap<>(questRepo.loadProgress(uuid, state.questId())));
            });
        });
    }

    public PlayerQuestState getActive(UUID uuid) {
        return active.get(uuid);
    }

    public Map<Integer, Integer> getProgressMap(UUID uuid) {
        return progress.getOrDefault(uuid, Map.of());
    }

    public StatsRepository.Stats getStats(UUID uuid) {
        return statsCache.getOrDefault(uuid, new StatsRepository.Stats(0, 0, 0));
    }

    public QuestDefinition getActiveQuestDefinition(UUID uuid) {
        PlayerQuestState st = active.get(uuid);
        return st == null ? null : getQuestById(st.questId());
    }

    public QuestDefinition getQuestById(String id) {
        if (storyConfig.isStoryQuest(id)) {
            int idx = storyConfig.getStoryIndex(id);
            if (idx > 0) return storyConfig.getNextByCompletedIndex(idx - 1);
        }
        return questConfig.get(id);
    }

    public QuestDefinition getNextStory(UUID uuid) {
        return storyConfig.getNextByCompletedIndex(completedStoryIndex.getOrDefault(uuid, 0));
    }

    public QuestDefinition startNextStory(Player player) {
        if (active.containsKey(player.getUniqueId())) return null;
        QuestDefinition next = getNextStory(player.getUniqueId());
        if (next == null) return null;
        return start(player, next.id()) ? next : null;
    }

    public QuestDefinition startRandom(Player player) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return null;

        List<QuestDefinition> all = new ArrayList<>(questConfig.enabledQuests());
        if (all.isEmpty()) return null;

        List<QuestDefinition> extreme = new ArrayList<>();
        List<QuestDefinition> normal = new ArrayList<>();

        for (QuestDefinition q : all) {
            if (q.difficulty() == Difficulty.EXTREME) extreme.add(q);
            else normal.add(q);
        }

        QuestDefinition chosen;
        if (!extreme.isEmpty() && random.nextDouble() < EXTREME_CHANCE) {
            chosen = extreme.get(random.nextInt(extreme.size()));
        } else {
            List<QuestDefinition> base = normal.isEmpty() ? all : normal;
            chosen = base.get(random.nextInt(base.size()));
        }

        return start(player, chosen.id()) ? chosen : null;
    }

    public boolean start(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) return false;

        QuestDefinition def = getQuestById(questId);
        if (def == null || !def.enabled()) return false;

        active.put(uuid, new PlayerQuestState(uuid, questId));
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
            questRepo.deleteActive(uuid);
            questRepo.deleteProgress(uuid, st.questId());
        });

        return true;
    }

    public void handleKill(Player player, EntityType type) {
        QuestDefinition def = getActiveQuestDefinition(player.getUniqueId());
        if (def == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition o = def.objectives().get(i);
            if (o.type() == QuestType.KILL_MOB && o.entityType() == type) addProgress(player, i, 1);
        }
    }

    public void handleBreak(Player player, org.bukkit.Material mat) {
        QuestDefinition def = getActiveQuestDefinition(player.getUniqueId());
        if (def == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition o = def.objectives().get(i);
            if (o.type() == QuestType.BREAK && o.material() == mat) addProgress(player, i, 1);
        }
    }

    public void handleBiome(Player player, Biome biome) {
        QuestDefinition def = getActiveQuestDefinition(player.getUniqueId());
        if (def == null) return;

        NamespacedKey key = Registry.BIOME.getKey(biome);
        if (key == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition o = def.objectives().get(i);
            if (o.type() == QuestType.BIOME && key.equals(o.biomeKey())) setProgress(player, i, 1);
        }
    }

    public void handleStructure(Player player) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return;

        long now = System.currentTimeMillis();
        long last = lastStructureCheck.getOrDefault(uuid, 0L);
        if (now - last < STRUCTURE_CHECK_COOLDOWN_MS) return;
        lastStructureCheck.put(uuid, now);

        Location loc = player.getLocation();

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition o = def.objectives().get(i);
            if (o.type() != QuestType.STRUCTURE) continue;
            if (getProgress(uuid, i) >= o.required()) continue;

            Structure structure = Registry.STRUCTURE.get(o.structureKey());
            if (structure == null) continue;

            Object result = loc.getWorld().locateNearestStructure(loc, structure, o.searchRadius(), false);
            Location found = StructureUtil.extractLocation(result, loc.getWorld());
            if (found == null) continue;

            int near = Math.max(16, o.nearBlocks());
            if (found.distanceSquared(loc) <= (double) near * near) {
                setProgress(player, i, o.required());
            }
        }
    }

    public void syncCollectProgress(Player player) {
        QuestDefinition def = getActiveQuestDefinition(player.getUniqueId());
        if (def == null) return;

        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition o = def.objectives().get(i);
            if (o.type() == QuestType.COLLECT) {
                setProgress(player, i, InventoryUtil.count(player, o.material()));
            }
        }
    }

    public int turnInItems(Player player) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return 0;

        int total = 0;
        for (int i = 0; i < def.objectives().size(); i++) {
            QuestObjectiveDefinition o = def.objectives().get(i);
            if (o.type() != QuestType.DELIVER) continue;

            int cur = getProgress(uuid, i);
            int need = Math.max(0, o.required() - cur);
            if (need <= 0) continue;

            int removed = InventoryUtil.remove(player, o.material(), need);
            if (removed > 0) {
                addProgress(player, i, removed);
                total += removed;
            }
        }
        return total;
    }

    public boolean canComplete(Player player) {
        QuestDefinition def = getActiveQuestDefinition(player.getUniqueId());
        if (def == null) return false;

        Map<Integer, Integer> p = getProgressMap(player.getUniqueId());
        for (int i = 0; i < def.objectives().size(); i++) {
            if (p.getOrDefault(i, 0) < def.objectives().get(i).required()) return false;
        }
        return true;
    }

    public void completeOrOpenRewards(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerQuestState st = active.get(uuid);
        if (st == null) return;

        syncCollectProgress(player);
        if (!canComplete(player)) return;

        QuestDefinition def = getQuestById(st.questId());
        RewardBundle bundle = def.rewards();
        List<Reward> options = bundle.options();

        if (bundle.mode() == RewardMode.CHOICE && options.size() > 1) {
            player.openInventory(RewardMenu.build(def));
            return;
        }

        List<Reward> give;
        if (bundle.mode() == RewardMode.RANDOM) {
            give = List.of(options.get(random.nextInt(options.size())));
        } else {
            give = options;
        }

        RewardCrate.giveCrateOrDrop(player, RewardCrate.create("&aReward Crate", give));
        finish(uuid, st.questId());
    }

    public void claimChoice(Player player, String questId, int index) {
        Reward chosen = getQuestById(questId).rewards().options().get(index);
        RewardCrate.giveCrateOrDrop(player, RewardCrate.create("&aReward Crate", List.of(chosen)));
        finish(player.getUniqueId(), questId);
    }

    private int getProgress(UUID uuid, int idx) {
        return progress.getOrDefault(uuid, Map.of()).getOrDefault(idx, 0);
    }

    private void addProgress(Player player, int idx, int add) {
        setProgress(player, idx, getProgress(player.getUniqueId(), idx) + add);
    }

    private void setProgress(Player player, int idx, int value) {
        UUID uuid = player.getUniqueId();
        QuestDefinition def = getActiveQuestDefinition(uuid);
        if (def == null) return;

        int capped = Math.min(def.objectives().get(idx).required(), Math.max(0, value));
        progress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(idx, capped);

        Bukkit.getScheduler().runTaskAsynchronously(core,
                () -> questRepo.upsertProgress(uuid, def.id(), idx, capped));
    }

    private void finish(UUID uuid, String questId) {
        boolean isStory = storyConfig.isStoryQuest(questId);
        int storyIndex = storyConfig.getStoryIndex(questId);

        active.remove(uuid);
        progress.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            questRepo.deleteActive(uuid);
            questRepo.deleteProgress(uuid, questId);

            statsRepo.increment(uuid, isStory);
            StatsRepository.Stats s = statsCache.getOrDefault(uuid, new StatsRepository.Stats(0, 0, 0));
            statsCache.put(uuid, new StatsRepository.Stats(
                    s.total() + 1,
                    s.story() + (isStory ? 1 : 0),
                    s.random() + (isStory ? 0 : 1)
            ));

            if (isStory && storyIndex > 0) {
                storyRepo.setCompletedIndex(uuid, storyIndex);
                completedStoryIndex.put(uuid, storyIndex);
            }
        });
    }
}
