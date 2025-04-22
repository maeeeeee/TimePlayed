package org.queercraft.timePlayed;

import com.djrapitops.plan.query.QueryService;
import com.djrapitops.plan.query.CommonQueries;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
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

    public long getPlaytimeLast30d(UUID playerUUID) {
        long now = System.currentTimeMillis();
        long monthAgo = now - TimeUnit.DAYS.toMillis(30L);
        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(IllegalStateException::new);
        return queryService.getCommonQueries().fetchPlaytime(
                playerUUID, serverUUID, monthAgo, now
        );
    }

    public long getPlaytimeLast7d(UUID playerUUID) {
        long now = System.currentTimeMillis();
        long weekAgo = now - TimeUnit.DAYS.toMillis(7L);
        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(IllegalStateException::new);
        return queryService.getCommonQueries().fetchPlaytime(
                playerUUID, serverUUID, weekAgo, now
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