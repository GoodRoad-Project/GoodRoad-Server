package goodroad.service;

import com.graphhopper.ResponsePath;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.shapes.GHPoint;
import goodroad.model.PathResponse;
import goodroad.model.ResponseInfo;
import goodroad.model.RouteRequest;
import goodroad.model.RouteResponse;
import goodroad.obstacle.ObstacleDBService;
import goodroad.model.ObstacleForRouting;
import goodroad.routing.CustomModelFactory;
import goodroad.routing.RoutingBoundingBox;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private static final double BBOX_PADDING_DEGREES = 0.01;

    private final EmbeddedGraphHopperService graphHopperService;

    private final CustomModelFactory customModelFactory;

    private final ObstacleDBService obstacleDBService;

    public RouteService(
            EmbeddedGraphHopperService graphHopperService,
            CustomModelFactory customModelFactory,
            ObstacleDBService obstacleDBService
    ) {
        this.graphHopperService = graphHopperService;
        this.customModelFactory = customModelFactory;
        this.obstacleDBService = obstacleDBService;
    }

    public RouteResponse buildThreeRoutes(
            RouteRequest request
    ) {

        log.info("Building route: start={}, end={}",
                request.getStart(), request.getEnd());

        long startTime = System.nanoTime();

        log.info("Parsing start and end points");
        GHPoint start =
                parsePoint(request.getStart());

        GHPoint end =
                parsePoint(request.getEnd());
        log.info("Points parsed: start={}, end={}", start, end);

        Locale locale =
                parseLocale(request.getLocale());
        log.info("Locale: {}", locale);

        log.info("Calculating bounding box");
        RoutingBoundingBox bbox =
                RoutingBoundingBox.around(
                        start.getLat(),
                        start.getLon(),
                        end.getLat(),
                        end.getLon(),
                        BBOX_PADDING_DEGREES
                );
        log.info("Bounding box: minLat={}, maxLat={}, minLon={}, maxLon={}",
                bbox.minLat(), bbox.maxLat(), bbox.minLon(), bbox.maxLon());


        log.info("Fetching obstacles from database");
        List<ObstacleForRouting> obstacles =
                obstacleDBService.findForRouting(
                        bbox.minLat(),
                        bbox.maxLat(),
                        bbox.minLon(),
                        bbox.maxLon()
                );

        log.info("Found {} obstacles for routing", obstacles.size());

        log.info("Building fast model");
        CustomModel fastModel = customModelFactory.buildFast();
        log.info("Fast model built");

        log.info("Building balanced model with {} obstacles", obstacles.size());
        CustomModel balancedModel = customModelFactory.buildBalanced(
                obstacles,
                request
        );
        log.info("Balanced model built");

        log.info("Building safe model with {} obstacles", obstacles.size());
        CustomModel safeModel = customModelFactory.buildSafe(
                obstacles,
                request
        );
        log.info("Safe model built");

        log.info("Calling GraphHopper for FAST route");
        ResponsePath fastPath = graphHopperService.route(
                start,
                end,
                fastModel,
                locale
        );
        log.info("FAST route completed: distance={}, time={}ms",
                fastPath.getDistance(), fastPath.getTime());

        log.info("Calling GraphHopper for BALANCED route");
        ResponsePath balancedPath = graphHopperService.route(
                start,
                end,
                balancedModel,
                locale
        );
        log.info("BALANCED route completed: distance={}, time={}ms",
                balancedPath.getDistance(), balancedPath.getTime());

        log.info("Calling GraphHopper for SAFE route");
        ResponsePath safePath = graphHopperService.route(
                start,
                end,
                safeModel,
                locale
        );
        log.info("SAFE route completed: distance={}, time={}ms",
                safePath.getDistance(), safePath.getTime());

        log.info("Converting paths to response");
        List<PathResponse> paths =
                List.of(
                        toPathResponse(fastPath, "fast"),
                        toPathResponse(balancedPath, "balanced"),
                        toPathResponse(safePath, "safe")
                );

        double took = (System.nanoTime() - startTime) / 1_000_000.0;
        log.info("Total route building time: {} ms", took);

        return new RouteResponse(
                UUID.randomUUID().toString(),
                paths,
                new ResponseInfo(took)
        );
    }

    private PathResponse toPathResponse(
            ResponsePath path,
            String routeType
    ) {
        log.info("Converting {} route to PathResponse", routeType);
        if (path == null) {
            log.error("Path is null for route type: {}", routeType);
            throw new RuntimeException("Path is null for " + routeType);
        }

        String geojson = toGeoJson(path);
        log.info("{} route converted: distance={}, time={}ms",
                routeType, path.getDistance(), path.getTime());

        return new PathResponse(
                path.getDistance(),
                path.getTime(),
                true,
                geojson,
                List.of(),
                routeType
        );
    }

    private String toGeoJson(
            ResponsePath path
    ) {
        log.info("Converting path to GeoJSON");

        var points = path.getPoints();
        if (points == null || points.size() == 0) {
            log.error("Points are null or empty");
            throw new RuntimeException("No points in path");
        }
        log.info("Path has {} points", points.size());

        StringBuilder coordinates = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                coordinates.append(",");
            }
            coordinates
                    .append("[")
                    .append(points.getLon(i))
                    .append(",")
                    .append(points.getLat(i))
                    .append("]");
        }
        coordinates.append("]");

        String result = """
            {
              "type": "LineString",
              "coordinates": %s
            }
            """
                .formatted(coordinates);

        log.info("GeoJSON converted successfully");
        return result;
    }

    private GHPoint parsePoint(
            String value
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Point is empty"
            );
        }

        String[] parts =
                value.trim().split(",");

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Point must be in format: lat,lon"
            );
        }

        try {

            double lat = Double.parseDouble(parts[0].trim());

            double lon = Double.parseDouble(parts[1].trim());

            if (lat < -90 || lat > 90) {
                throw new IllegalArgumentException(
                        "Latitude must be between -90 and 90"
                );
            }

            if (lon < -180 || lon > 180) {
                throw new IllegalArgumentException(
                        "Longitude must be between -180 and 180"
                );
            }

            return new GHPoint(lat, lon);

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid coordinates: " + value,
                    e
            );
        }
    }

    private Locale parseLocale(
            String locale
    ) {

        if (locale == null
                || locale.isBlank()) {

            return Locale.forLanguageTag("ru");
        }

        return Locale.forLanguageTag(locale);
    }
}