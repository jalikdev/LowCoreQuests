package dev.jalikdev.lowCoreQuests.config;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.LowCoreQuests;
import dev.jalikdev.lowCoreQuests.model.*;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.reward.RewardBundle;
import dev.jalikdev.lowCoreQuests.reward.RewardMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class StoryConfig {

    private final LowCoreQuests addon;
    private final LowCore core;

    private boolean enabled = true;
    private List<QuestDefinition> line = new ArrayList<>();
    private Map<String, Integer> indexById = new HashMap<>();

    public StoryConfig(LowCoreQuests addon, LowCore core) {
        this.addon = addon;
        this.core = core;
    }

    public File storyFile() {
        return new File(core.getDataFolder(), "story.yml");
    }

    public void ensureStoryFileInLowCoreFolder() {
        if (!core.getDataFolder().exists()) core.getDataFolder().mkdirs();
        File file = storyFile();
        if (file.exists()) return;

        try (InputStream in = addon.getResource("story.yml")) {
            if (in == null) return;
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(storyFile());

        this.enabled = cfg.getBoolean("story.enabled", true);

        List<Map<?, ?>> rawLine = cfg.getMapList("story.line");
        List<QuestDefinition> parsed = new ArrayList<>();
        Map<String, Integer> idxMap = new HashMap<>();

        int idx = 1;
        for (Map<?, ?> raw : rawLine) {
            if (!(raw instanceof Map<?, ?> m)) continue;

            String id = str(m.get("id"));
            if (id == null || id.isBlank()) continue;

            boolean qEnabled = bool(m.get("enabled"), true);

            String name = str(m.get("name"));
            if (name == null) name = id;

            List<String> description = listStr(m.get("description"));

            Difficulty difficulty;
            try { difficulty = Difficulty.valueOf(str(m.get("difficulty")).toUpperCase(Locale.ROOT)); }
            catch (Exception e) { difficulty = Difficulty.EASY; }

            QuestCompletionMode completionMode = QuestCompletionMode.ALL;

            List<QuestObjectiveDefinition> objectives = parseObjectives(listMap(m.get("objectives")));
            if (objectives.isEmpty()) continue;

            RewardBundle rewards = parseRewards(map(m.get("rewards")));

            QuestDefinition def = new QuestDefinition(id, qEnabled, name, description, difficulty, completionMode, objectives, rewards);

            if (qEnabled) {
                parsed.add(def);
                idxMap.put(id, idx);
                idx++;
            }
        }

        this.line = parsed;
        this.indexById = idxMap;
    }

    public boolean isEnabled() { return enabled; }

    public boolean isStoryQuest(String id) { return indexById.containsKey(id); }

    public int getStoryIndex(String id) { return indexById.getOrDefault(id, -1); }

    public QuestDefinition getNextByCompletedIndex(int completedIndex) {
        if (!enabled) return null;
        int next = completedIndex + 1;
        int listIndex = next - 1;
        if (listIndex < 0 || listIndex >= line.size()) return null;
        return line.get(listIndex);
    }

    private List<QuestObjectiveDefinition> parseObjectives(List<Map<?, ?>> rawList) {
        List<QuestObjectiveDefinition> out = new ArrayList<>();

        for (Map<?, ?> raw : rawList) {
            Object t = raw.get("type");
            Object targetObj = raw.get("target");
            if (t == null || !(targetObj instanceof Map<?, ?> target)) continue;

            QuestType type;
            try { type = QuestType.valueOf(String.valueOf(t).toUpperCase(Locale.ROOT)); }
            catch (Exception e) { continue; }

            int amount = Math.max(1, intVal(target.get("amount"), 1));
            String display = target.get("display") == null ? null : String.valueOf(target.get("display"));

            if (type == QuestType.DELIVER || type == QuestType.COLLECT || type == QuestType.BREAK) {
                String matName = target.get("material") == null ? "STONE" : String.valueOf(target.get("material"));
                Material mat = Material.matchMaterial(matName);
                if (mat == null) continue;

                if (type == QuestType.COLLECT) out.add(QuestObjectiveDefinition.collect(mat, amount, display));
                else if (type == QuestType.DELIVER) out.add(QuestObjectiveDefinition.deliver(mat, amount, display));
                else out.add(QuestObjectiveDefinition.breakBlock(mat, amount, display));

                continue;
            }

            if (type == QuestType.BIOME) {
                String biome = target.get("biome") == null ? "plains" : String.valueOf(target.get("biome"));
                NamespacedKey key = keyFrom(biome);
                if (Registry.BIOME.get(key) == null) continue;
                out.add(QuestObjectiveDefinition.biome(key, amount, display));
                continue;
            }

            if (type == QuestType.KILL_MOB) {
                String ent = target.get("entity") == null ? "ZOMBIE" : String.valueOf(target.get("entity"));
                EntityType entity;
                try { entity = EntityType.valueOf(ent.toUpperCase(Locale.ROOT)); }
                catch (Exception e) { continue; }
                out.add(QuestObjectiveDefinition.kill(entity, amount, display));
                continue;
            }

            if (type == QuestType.STRUCTURE) {
                String s = target.get("structure") == null ? "village" : String.valueOf(target.get("structure"));
                NamespacedKey key = keyFrom(s);
                if (Registry.STRUCTURE.get(key) == null) continue;

                int searchRadius = clamp(intVal(target.get("searchRadius"), 2), 1, 8);
                int nearBlocks = clamp(intVal(target.get("nearBlocks"), 80), 16, 512);

                out.add(QuestObjectiveDefinition.structure(key, amount, searchRadius, nearBlocks, display));
            }
        }

        return out;
    }

    private RewardBundle parseRewards(Map<?, ?> rewardsMap) {
        if (rewardsMap == null) return new RewardBundle(RewardMode.ALL, List.of());

        String modeRaw = str(rewardsMap.get("mode"));
        RewardMode mode;
        try { mode = RewardMode.valueOf((modeRaw == null ? "ALL" : modeRaw).toUpperCase(Locale.ROOT)); }
        catch (Exception e) { mode = RewardMode.ALL; }

        List<Map<?, ?>> optionsRaw = listMap(rewardsMap.get("options"));
        List<Reward> options = new ArrayList<>();
        for (Map<?, ?> r : optionsRaw) {
            Reward reward = Reward.fromMap(r);
            if (reward != null) options.add(reward);
        }

        return new RewardBundle(mode, options);
    }

    private static NamespacedKey keyFrom(String s) {
        if (s == null) return NamespacedKey.minecraft("village");
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.contains(":")) return NamespacedKey.fromString(v);
        return NamespacedKey.minecraft(v);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int intVal(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }

    private static boolean bool(Object o, boolean def) {
        if (o == null) return def;
        if (o instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> map(Object o) {
        if (o instanceof Map<?, ?> m) return m;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<?, ?>> listMap(Object o) {
        if (o instanceof List<?> l) {
            List<Map<?, ?>> out = new ArrayList<>();
            for (Object x : l) if (x instanceof Map<?, ?> m) out.add(m);
            return out;
        }
        return List.of();
    }

    private static List<String> listStr(Object o) {
        if (o instanceof List<?> l) {
            List<String> out = new ArrayList<>();
            for (Object x : l) out.add(String.valueOf(x));
            return out;
        }
        return List.of();
    }
}
