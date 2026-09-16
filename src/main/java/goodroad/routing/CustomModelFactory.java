package goodroad.routing;

import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.JsonFeatureCollection;
import goodroad.model.ObstacleForRouting;
import goodroad.model.RouteRequest;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.MULTIPLY;

@Service
public class CustomModelFactory {

    private final ObstacleAreaFactory obstacleAreaFactory =
            new ObstacleAreaFactory();

    private static final double OBSTACLE_AREA_RADIUS_METERS = 1.0;

    public CustomModel buildFast() {

        CustomModel model = baseModel();

        return model;
    }

    public CustomModel buildBalanced(
            List<ObstacleForRouting> obstacles,
            RouteRequest request
    ) {

        CustomModel model = baseModel();

        addDatabaseObstacleRules(
                model,
                obstacles,
                request,
                0.10
        );

        return model;
    }

    public CustomModel buildSafe(
            List<ObstacleForRouting> obstacles,
            RouteRequest request
    ) {

        CustomModel model = baseModel();

        addDatabaseObstacleRules(
                model,
                obstacles,
                request,
                0.0
        );

        return model;
    }

    private CustomModel baseModel() {
        return new CustomModel();
    }

    private void addDatabaseObstacleRules(
            CustomModel model,
            List<ObstacleForRouting> obstacles,
            RouteRequest request,
            double defaultMultiplier
    ) {

        if (obstacles == null || obstacles.isEmpty()) {
            return;
        }

        JsonFeatureCollection areas =
                obstacleAreaFactory.createAreas(
                        obstacles,
                        OBSTACLE_AREA_RADIUS_METERS
                );

        model.setAreas(areas);

        for (ObstacleForRouting obstacle : obstacles) {

            String areaName = "obstacle_" + obstacle.id();

            double multiplier =
                    calculateMultiplier(
                            obstacle,
                            request,
                            defaultMultiplier
                    );

            model.addToPriority(
                    If(
                            "in_" + areaName,
                            MULTIPLY,
                            String.valueOf(multiplier)
                    )
            );
        }
    }

    private double calculateMultiplier(
            ObstacleForRouting obstacle,
            RouteRequest request,
            double defaultMultiplier
    ) {

        if (request == null) {
            return defaultMultiplier;
        }

        String obstacleType =
                normalize(obstacle.type());

        if (request.isAvoidStairs()
                && "STAIRS".equals(obstacleType)) {

            return 0.0;
        }

        if (request.getAvoidSurfaceTypes() != null
                && request.getAvoidSurfaceTypes()
                .stream()
                .map(this::normalize)
                .anyMatch(obstacleType::equals)) {

            return 0.0;
        }

        if (request.getObstaclePolicies() != null) {

            for (RouteRequest.RouteObstaclePolicy policy
                    : request.getObstaclePolicies()) {

                if (policy == null
                        || policy.getObstacleType() == null) {
                    continue;
                }

                if (!obstacleType.equals(
                        normalize(policy.getObstacleType()))) {
                    continue;
                }

                Short severity = obstacle.obstacleSeverityEstimates()
                                .get(obstacleType);

                if (severity == null) {
                    severity =
                            obstacle.severityEstimate();
                }

                if (severity == null) {
                    return defaultMultiplier;
                }

                Short maxAllowed = policy.getMaxAllowedSeverity();

                if (maxAllowed != null && severity > maxAllowed) {
                    return 0.0;
                }
            }
        }

        return defaultMultiplier;
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toUpperCase();
    }
}