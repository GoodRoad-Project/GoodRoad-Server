package goodroad.routing;

public record RoutingBoundingBox(
        double minLat,
        double maxLat,
        double minLon,
        double maxLon
) {

    public static RoutingBoundingBox around(
            double startLat,
            double startLon,
            double endLat,
            double endLon,
            double paddingDegrees
    ) {

        return new RoutingBoundingBox(
                Math.min(startLat, endLat) - paddingDegrees,
                Math.max(startLat, endLat) + paddingDegrees,
                Math.min(startLon, endLon) - paddingDegrees,
                Math.max(startLon, endLon) + paddingDegrees
        );
    }
}
