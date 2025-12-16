package dev.jalikdev.lowCoreQuests.db;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.model.PlayerQuestState;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class QuestRepository {

    private final LowCore core;

    public QuestRepository(LowCore core) {
        this.core = core;
    }

    public void init() {
        try (Statement st = core.getDatabaseManager().getConnection().createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lowcore_quests_active (
                  uuid TEXT PRIMARY KEY,
                  quest_id TEXT NOT NULL
                )
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lowcore_quests_progress (
                  uuid TEXT NOT NULL,
                  quest_id TEXT NOT NULL,
                  idx INTEGER NOT NULL,
                  progress INTEGER NOT NULL,
                  PRIMARY KEY (uuid, quest_id, idx)
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<PlayerQuestState> loadActive(UUID uuid) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "SELECT quest_id FROM lowcore_quests_active WHERE uuid=?")) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PlayerQuestState(uuid, rs.getString("quest_id")));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, Integer> loadProgress(UUID uuid, String questId) {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "SELECT idx, progress FROM lowcore_quests_progress WHERE uuid=? AND quest_id=?")) {

            ps.setString(1, uuid.toString());
            ps.setString(2, questId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt("idx"), rs.getInt("progress"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public void setActive(UUID uuid, String questId) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement("""
            INSERT INTO lowcore_quests_active(uuid, quest_id)
            VALUES(?,?)
            ON CONFLICT(uuid) DO UPDATE SET quest_id=excluded.quest_id
            """)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, questId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteActive(UUID uuid) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "DELETE FROM lowcore_quests_active WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsertProgress(UUID uuid, String questId, int idx, int progress) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement("""
            INSERT INTO lowcore_quests_progress(uuid, quest_id, idx, progress)
            VALUES(?,?,?,?)
            ON CONFLICT(uuid, quest_id, idx) DO UPDATE SET progress=excluded.progress
            """)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, questId);
            ps.setInt(3, idx);
            ps.setInt(4, progress);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteProgress(UUID uuid, String questId) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "DELETE FROM lowcore_quests_progress WHERE uuid=? AND quest_id=?")) {

            ps.setString(1, uuid.toString());
            ps.setString(2, questId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
