package org.queercraft.timePlayed;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.query.QueryService;

import java.util.Optional;
import java.util.logging.Logger;

public class PlanHook {

    private static final Logger logger = Logger.getLogger(PlanHook.class.getName());

    public PlanHook() {
    }

    public Optional<QueryAPIAccessor> hookIntoPlan() {
        logger.info("Attempting to hook into Plan.");
        if (!areAllCapabilitiesAvailable()) {
            logger.severe("Required capabilities are not available. Hooking into Plan failed.");
            return Optional.empty();
        }
        QueryAPIAccessor accessor = createQueryAPIAccessor();
        if (accessor == null) {
            logger.warning("Failed to create QueryAPIAccessor. Hooking into Plan failed.");
        } else {
            logger.info("Successfully hooked into Plan.");
        }
        return Optional.ofNullable(accessor);
    }

    private boolean areAllCapabilitiesAvailable() {
        logger.info("Checking if all required capabilities are available.");
        CapabilityService capabilities = CapabilityService.getInstance();
        boolean hasQueryApi = capabilities.hasCapability("QUERY_API");
        if (!hasQueryApi) {
            logger.warning("Capability QUERY_API is not available.");
        }
        return hasQueryApi;
    }

    private QueryAPIAccessor createQueryAPIAccessor() {
        try {
            logger.info("Creating QueryAPIAccessor instance.");
            return new QueryAPIAccessor(QueryService.getInstance());
        } catch (IllegalStateException planIsNotEnabled) {
            logger.severe("Plan is not enabled. Exception: " + planIsNotEnabled.getMessage());
            return null;
        }
    }
}