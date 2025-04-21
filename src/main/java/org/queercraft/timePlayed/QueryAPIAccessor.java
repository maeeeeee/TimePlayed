package org.queercraft.timePlayed;

import com.djrapitops.plan.query.QueryService;
import com.djrapitops.plan.query.CommonQueries;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

public class QueryAPIAccessor {

    private final QueryService queryService;

    public QueryAPIAccessor(QueryService queryService) {
        this.queryService = queryService;
        ensureDBSchemaMatch();
    }

    private void ensureDBSchemaMatch() {
        CommonQueries queries = queryService.getCommonQueries();
        if (
                !queries.doesDBHaveTable("plan_sessions")
                        || !queries.doesDBHaveTableColumn("plan_sessions", "uuid")
        ) {
            throw new IllegalStateException("Different table schema");
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