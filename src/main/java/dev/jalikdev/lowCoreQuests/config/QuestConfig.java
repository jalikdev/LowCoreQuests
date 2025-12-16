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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.structure.Structure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class QuestConfig {

    private final LowCoreQuests addon;
    private final LowCore core;

    private Map<String, QuestDefinition> quests = new LinkedHashMap<>();

    public QuestConfig(LowCoreQuests addon, LowCore core) {
        this.addon = addon;
        this.core = core;
    }

    public File questsFile() {
        return new File(core.getDataFolder(), "quests.yml");
    }

    public void ensureQuestsFileInLowCoreFolder() {
        if (!core.getDataFolder().exists()) core.getDataFolder().mkdirs();
        File file = questsFile();
        if (file.exists()) return;

        try (InputStream in = addon.getResource("quests.yml")) {
            if (in == null) return;
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(questsFile());
        ConfigurationSection root = cfg.getConfigurationSection("quests");

        Map<String, QuestDefinition> map = new LinkedHashMap<>();

        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection q = root.getConfigurationSection(id);
                if (q == null) continue;

                QuestDefinition def = parseQuest(id, q);
                if (def != null) map.put(id, def);
            }
        }

        this.quests = map;
    }

    private QuestDefinition parseQuest(String id, ConfigurationSection q) {
        boolean enabled = q.getBoolean("enabled", true);
        String name = q.getString("name", id);
        List<String> description = q.getStringList("description");

        Difficulty difficulty;
        try { difficulty = Difficulty.valueOf(q.getString("difficulty", "EASY").toUpperCase(Locale.ROOT)); }
        catch (Exception e) { difficulty = Difficulty.EASY; }

        QuestCompletionMode completionMode = QuestCompletionMode.ALL;

        List<QuestObjectiveDefinition> objectives = parseObjectives(q.getMapList("objectives"));
        if (objectives.isEmpty()) return null;

        RewardBundle rewards = parseRewards(q.getConfigurationSection("rewards"));

        return new QuestDefinition(id, enabled, name, description, difficulty, completionMode, objectives, rewards);
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

            if (type == QuestType.DELIVER || type == QuestType.COLLECT) {
                String matName = target.get("material") == null ? "STONE" : String.valueOf(target.get("material"));
                Material mat = Material.matchMaterial(matName);
                if (mat == null) continue;
                out.add(QuestObjectiveDefinition.item(type, mat, amount, display));
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

    private RewardBundle parseRewards(ConfigurationSection sec) {
        if (sec == null) return new RewardBundle(RewardMode.ALL, List.of());

        String modeRaw = sec.getString("mode", "ALL").toUpperCase(Locale.ROOT);
        RewardMode mode;
        try { mode = RewardMode.valueOf(modeRaw); } catch (Exception e) { mode = RewardMode.ALL; }

        List<Map<?, ?>> list = sec.getMapList("options");
        List<Reward> options = new ArrayList<>();
        for (Map<?, ?> raw : list) {
            Reward r = Reward.fromMap(raw);
            if (r != null) options.add(r);
        }

        return new RewardBundle(mode, options);
    }

    public QuestDefinition get(String id) {
        return quests.get(id);
    }

    public Collection<QuestDefinition> enabledQuests() {
        List<QuestDefinition> out = new ArrayList<>();
        for (QuestDefinition q : quests.values()) if (q.enabled()) out.add(q);
        return out;
    }
}
