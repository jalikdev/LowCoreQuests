package dev.jalikdev.lowCoreQuests.db;

import dev.jalikdev.lowCore.LowCore;

import java.sql.*;
import java.util.UUID;

public class StoryRepository {

    private final LowCore core;

    public StoryRepository(LowCore core) {
        this.core = core;
    }

    public void init() {
        try (Statement st = core.getDatabaseManager().getConnection().createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lowcore_story_progress (
                  uuid TEXT PRIMARY KEY,
                  completed_index INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int loadCompletedIndex(UUID uuid) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "SELECT completed_index FROM lowcore_story_progress WHERE uuid=?")) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                return rs.getInt("completed_index");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCompletedIndex(UUID uuid, int completedIndex) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement("""
            INSERT INTO lowcore_story_progress(uuid, completed_index)
            VALUES(?,?)
            ON CONFLICT(uuid) DO UPDATE SET completed_index=excluded.completed_index
            """)) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, Math.max(0, completedIndex));
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
