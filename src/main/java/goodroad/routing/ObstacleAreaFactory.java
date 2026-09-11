package goodroad.routing;

import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.JsonFeatureCollection;
import goodroad.model.ObstacleForRouting;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.List;

public class ObstacleAreaFactory {

    private final GeometryFactory geometryFactory =
            new GeometryFactory();

    public JsonFeature createArea(
            ObstacleForRouting obstacle,
            double radiusMeters
    ) {

        double lat = obstacle.latitude();
        double lon = obstacle.longitude();

        double latDelta =
                radiusMeters / 111_000.0;

        double lonDelta =
                radiusMeters /
                        (111_000.0 *
                                Math.cos(Math.toRadians(lat)));

        Coordinate[] coordinates =
                new Coordinate[]{
                        new Coordinate(
                                lon - lonDelta,
                                lat - latDelta
                        ),
                        new Coordinate(
                                lon + lonDelta,
                                lat - latDelta
                        ),
                        new Coordinate(
                                lon + lonDelta,
                                lat + latDelta
                        ),
                        new Coordinate(
                                lon - lonDelta,
                                lat + latDelta
                        ),
                        new Coordinate(
                                lon - lonDelta,
                                lat - latDelta
                        )
                };

        Polygon polygon =
                geometryFactory.createPolygon(coordinates);

        return new JsonFeature(
                "obstacle_" + obstacle.id(),
                "Feature",
                null,
                polygon,
                new HashMap<>()
        );
    }

    public JsonFeatureCollection createAreas(
            List<ObstacleForRouting> obstacles,
            double radiusMeters
    ) {

        JsonFeatureCollection collection =
                new JsonFeatureCollection();

        if (obstacles == null || obstacles.isEmpty()) {
            return collection;
        }

        for (ObstacleForRouting obstacle : obstacles) {

            collection.getFeatures().add(
                    createArea(
                            obstacle,
                            radiusMeters
                    )
            );
        }

        return collection;
    }
}