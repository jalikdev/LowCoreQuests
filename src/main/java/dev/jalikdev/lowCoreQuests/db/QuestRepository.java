package dev.jalikdev.lowCoreQuests.db;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.model.PlayerQuestState;

import java.sql.*;
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
                  quest_id TEXT NOT NULL,
                  progress INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<PlayerQuestState> load(UUID uuid) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "SELECT quest_id, progress FROM lowcore_quests_active WHERE uuid=?")) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PlayerQuestState(uuid, rs.getString("quest_id"), rs.getInt("progress")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsert(PlayerQuestState st) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement("""
            INSERT INTO lowcore_quests_active(uuid, quest_id, progress)
            VALUES(?,?,?)
            ON CONFLICT(uuid) DO UPDATE SET quest_id=excluded.quest_id, progress=excluded.progress
            """)) {

            ps.setString(1, st.uuid().toString());
            ps.setString(2, st.questId());
            ps.setInt(3, st.progress());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(UUID uuid) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "DELETE FROM lowcore_quests_active WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
