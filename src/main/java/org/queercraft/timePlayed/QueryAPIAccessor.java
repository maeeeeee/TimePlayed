package org.queercraft.timePlayed;

import com.djrapitops.plan.query.CommonQueries;
import com.djrapitops.plan.query.QueryService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.UUID;
import java.util.logging.Logger;

public class QueryAPIAccessor {

    private static final Logger logger = Logger.getLogger(QueryAPIAccessor.class.getName());
    private final QueryService queryService;

    public QueryAPIAccessor(QueryService queryService) {
        logger.info("Initializing QueryAPIAccessor.");
        this.queryService = queryService;
        try {
            ensureDBSchemaMatch();
            logger.info("Database schema validated successfully.");
        } catch (IllegalStateException e) {
            logger.severe("Database schema validation failed: " + e.getMessage());
            throw e; // Re-throwing to ensure proper exception handling in the caller
        }
    }

    private void ensureDBSchemaMatch() {
        logger.info("Ensuring database schema matches expected structure.");
        CommonQueries queries = queryService.getCommonQueries();
        boolean hasSessionsTable = queries.doesDBHaveTable("plan_sessions");
        boolean hasUuidColumn = queries.doesDBHaveTableColumn("plan_sessions", "user_id");
        if (!hasSessionsTable || !hasUuidColumn) {
            String errorMessage = "Different table schema detected: "
                    + "plan_sessions table exists: " + hasSessionsTable
                    + ", uuid column exists: " + hasUuidColumn;
            logger.warning(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    public long getPlaytimeTotal(UUID playerUUID) {
        //Get Plan user id for selected player
        String user_id = queryService.query(
                "SELECT id FROM plan_users WHERE uuid=?",
                (PreparedStatement statement) -> {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet results = statement.executeQuery()) {
                        return results.next() ? results.getString("id") : null;
                    }
                }
        );
        //Get total recorded playtime
        String totalPlaytime = queryService.query(
                "SELECT SUM(survival_time + creative_time + adventure_time + spectator_time) AS total_time FROM plan_world_times WHERE user_id=?",
                (PreparedStatement statement) -> {
                    statement.setString(1, user_id);
                    try (ResultSet results = statement.executeQuery()) {
                        return results.next() ? results.getString("total_time") : null;
                    }
                }
        );
        return Long.parseLong(totalPlaytime);
    }

    public long getPlaytimeLastMonth(UUID playerUUID) {
        LocalDateTime startOfThisMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        long startOfThisMonthMillis = startOfThisMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        LocalDateTime startOfLastMonth = LocalDateTime.now()
                .minusMonths(1) // Subtract one month
                .withDayOfMonth(1) // Set to the first day of the month
                .withHour(0) // Set hour to 0
                .withMinute(0) // Set minute to 0
                .withSecond(0) // Set second to 0
                .withNano(0); // Set nanoseconds to 0
        long startOfLastMonthMillis = startOfLastMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(IllegalStateException::new);
        return queryService.getCommonQueries().fetchPlaytime(
                playerUUID, serverUUID, startOfLastMonthMillis, startOfThisMonthMillis
        );
    }

    public long getPlaytimeThisMonth(UUID playerUUID) {
        long now = System.currentTimeMillis();
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        long startOfMonthMillis = startOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(IllegalStateException::new);
        return queryService.getCommonQueries().fetchPlaytime(
                playerUUID, serverUUID, startOfMonthMillis, now
        );
    }

    public long getPlaytimeThisWeek(UUID playerUUID) {
        long now = System.currentTimeMillis();
        LocalDateTime startOfWeek = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        long startOfWeekMillis = startOfWeek.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(IllegalStateException::new);
        return queryService.getCommonQueries().fetchPlaytime(
                playerUUID, serverUUID, startOfWeekMillis, now
        );
    }

    public long getPlaytimeToday(UUID playerUUID) {
        long now = System.currentTimeMillis();

        //This is fucking bullshit but eh
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfToday = calendar.getTimeInMillis();

        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(IllegalStateException::new);
        return queryService.getCommonQueries().fetchPlaytime(
                playerUUID, serverUUID, startOfToday, now
        );
    }

}