package goodroad.service;

import goodroad.model.RouteRequest;
import goodroad.model.RouteResponse;
import goodroad.model.PathResponse;
import goodroad.model.gh.Path;
import goodroad.obstacle.ObstacleDBService;
import org.springframework.stereotype.Service;
import goodroad.model.gh.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class RouteService {

    private final GraphHopperService graphHopperService;
    private final ObstacleDBService obstacleDBService;

    public RouteService(GraphHopperService graphHopperService, ObstacleDBService obstacleDBService) {
        this.graphHopperService = graphHopperService;
        this.obstacleDBService = obstacleDBService;
    }

    private List<ObstacleDBService.ObstacleMapItemResp> getObstacleInArea(String start, String end) {
        String[] startParts = start.split(",");
        String[] endParts = end.split(",");

        double startLat = Double.parseDouble(startParts[0]);
        double startLon = Double.parseDouble(startParts[1]);
        double endLat = Double.parseDouble(endParts[0]);
        double endLon = Double.parseDouble(endParts[1]);

        double minLat = Math.min(startLat, endLat) - 0.01;
        double maxLat = Math.max(startLat, endLat) + 0.01;
        double minLon = Math.min(startLon, endLon) - 0.01;
        double maxLon = Math.max(startLon, endLon) + 0.01;

        return obstacleDBService.listInBox(minLat, maxLat, minLon, maxLon);
    }

    private List<RouteRequest.RouteObstaclePolicy> getRouteObstaclePolicies(RouteRequest request) {
        if (request.getObstaclePolicies() != null && !request.getObstaclePolicies().isEmpty()) {
            return request.getObstaclePolicies();
        }

        List<RouteRequest.RouteObstaclePolicy> policies = new ArrayList<>();
        if (request.isAvoidStairs()) {
            policies.add(policy("STAIRS", (short) 0));
        }
        if (request.getMaxCurbHeight() != null) {
            policies.add(policy("CURB", request.getMaxCurbHeight().shortValue()));
        }
        if (request.getMaxSlopeAngle() != null) {
            policies.add(policy("ROAD_SLOPE", request.getMaxSlopeAngle().shortValue()));
        }
        if (request.isAvoidBadRoad()) {
            policies.add(policy("POTHOLES", (short) 0));
        }
        if (request.getAvoidSurfaceTypes() != null) {
            for (String surface : request.getAvoidSurfaceTypes()) {
                policies.add(policy(surface, (short) 0));
            }
        }
        return policies;
    }

    private RouteRequest.RouteObstaclePolicy policy(String obstacleType, Short maxAllowedSeverity) {
        RouteRequest.RouteObstaclePolicy policy = new RouteRequest.RouteObstaclePolicy();
        policy.setObstacleType(obstacleType);
        policy.setMaxAllowedSeverity(maxAllowedSeverity);
        return policy;
    }

    private boolean shouldAvoidForUser(
            ObstacleDBService.ObstacleMapItemResp obstacle,
            RouteRequest.RouteObstaclePolicy policy
    ) {
        if (policy.getObstacleType() == null || policy.getMaxAllowedSeverity() == null) {
            return false;
        }

        Short obstacleSeverity = obstacle.obstacleSeverityEstimates().get(policy.getObstacleType());
        if (obstacleSeverity == null) {
            return false;
        }

        return obstacleSeverity > policy.getMaxAllowedSeverity();
    }

    private List<ObstacleDBService.ObstacleMapItemResp> getAvoidedObstacles(
            List<ObstacleDBService.ObstacleMapItemResp> obstacles,
            RouteRequest request
    ) {
        List<RouteRequest.RouteObstaclePolicy> policies = getRouteObstaclePolicies(request);
        if (policies.isEmpty()) {
            return List.of();
        }

        List<ObstacleDBService.ObstacleMapItemResp> out = new ArrayList<>();
        for (ObstacleDBService.ObstacleMapItemResp obstacle : obstacles) {
            for (RouteRequest.RouteObstaclePolicy policy : policies) {
                if (shouldAvoidForUser(obstacle, policy)) {
                    out.add(obstacle);
                    break;
                }
            }
        }
        return out;
    }

    private Map<String, Object> buildModelWithObstacles(List<ObstacleDBService.ObstacleMapItemResp> obstacles, RouteRequest request) {
        List<Map<String, Object>> conditions = new ArrayList<>();

        for (ObstacleDBService.ObstacleMapItemResp obstacle : obstacles) {
            switch (obstacle.type()) {
                case "STAIRS":
                    conditions.add(Map.of("if", "road_class == STEPS", "multiply_by", "0"));
                    break;
                case "POTHOLES":
                    conditions.add(Map.of("if", "surface == POTHOLES", "multiply_by", "0"));
                    break;
                case "ROAD_SLOPE":
                    if (request.getMaxSlopeAngle() != null) {
                        conditions.add(Map.of("if", "max_slope > " + request.getMaxSlopeAngle(), "multiply_by", "0"));
                    }
                    break;
                case "SAND":
                case "GRAVEL":
                    conditions.add(Map.of("if", "surface == " + obstacle.type(), "multiply_by", "0"));
                    break;
                case "CURB":
                    conditions.add(Map.of("if", "barrier == KERB", "multiply_by", "0"));
                    break;
            }
        }

        if (!conditions.isEmpty()) {
            return Map.of("priority", conditions);
        }
        return null;
    }

    public RouteResponse buildThreeRoutes(RouteRequest request) {
        List<ObstacleDBService.ObstacleMapItemResp> obstacles = getObstacleInArea(request.getStart(), request.getEnd());
        List<ObstacleDBService.ObstacleMapItemResp> avoidedObstacles = getAvoidedObstacles(obstacles, request);

        Map<String, Object> fastModel = null;
        Map<String, Object> balancedModel = buildModelWithObstacles(avoidedObstacles, request);
        Map<String, Object> safeModel = buildModelWithObstacles(avoidedObstacles, request);

        CompletableFuture<GraphHopperResponse> fastFuture = CompletableFuture.supplyAsync(() ->
                graphHopperService.getRoute(request.getStart(), request.getEnd(), "foot", true, "ru", fastModel)
        );
        CompletableFuture<GraphHopperResponse> balancedFuture = CompletableFuture.supplyAsync(() ->
                graphHopperService.getRoute(request.getStart(), request.getEnd(), "foot", true, "ru", balancedModel)
        );
        CompletableFuture<GraphHopperResponse> safeFuture = CompletableFuture.supplyAsync(() ->
                graphHopperService.getRoute(request.getStart(), request.getEnd(), "foot", true, "ru", safeModel)
        );

        CompletableFuture.allOf(fastFuture, balancedFuture, safeFuture).join();

        GraphHopperResponse fastResponse = fastFuture.join();
        GraphHopperResponse balancedResponse = balancedFuture.join();
        GraphHopperResponse safeResponse = safeFuture.join();

        Path fastPath = fastResponse != null && fastResponse.getPaths() != null && !fastResponse.getPaths().isEmpty()
                ? fastResponse.getPaths().get(0) : null;
        Path balancedPath = balancedResponse != null && balancedResponse.getPaths() != null && !balancedResponse.getPaths().isEmpty()
                ? balancedResponse.getPaths().get(0) : null;
        Path safePath = safeResponse != null && safeResponse.getPaths() != null && !safeResponse.getPaths().isEmpty()
                ? safeResponse.getPaths().get(0) : null;

        List<PathResponse> paths = new ArrayList<>();
        if (fastPath != null) paths.add(toPathResponse(fastPath, "fast"));
        if (balancedPath != null) paths.add(toPathResponse(balancedPath, "balanced"));
        if (safePath != null) paths.add(toPathResponse(safePath, "safe"));

        return new RouteResponse(UUID.randomUUID().toString(), paths, null);
    }

    private PathResponse toPathResponse(Path ghPath, String routeType) {
        PathResponse response = new PathResponse();
        response.setDistance(ghPath.getDistance());
        response.setTime(ghPath.getTime());
        response.setPoints(ghPath.getPoints());
        response.setPointsEncoded(true);
        response.setRouteType(routeType);
        response.setObstacles(new ArrayList<>());
        return response;
    }
}