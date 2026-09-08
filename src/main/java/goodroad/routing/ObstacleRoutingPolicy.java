package goodroad.routing;

public record ObstacleRoutingPolicy(
        String obstacleType,
        short maxAllowedSeverity
) {
}