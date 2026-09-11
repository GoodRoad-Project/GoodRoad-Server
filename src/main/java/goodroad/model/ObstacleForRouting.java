package goodroad.model;

import java.util.Map;

public record ObstacleForRouting(
        Long id,
        String type,
        double latitude,
        double longitude,
        Short severityEstimate,
        Map<String, Short> obstacleSeverityEstimates
) {
}