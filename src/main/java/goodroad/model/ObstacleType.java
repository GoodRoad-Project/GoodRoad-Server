package GoodRoad.model;

import GoodRoad.api.ApiErrors.ApiException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum ObstacleType {
    CURB,
    STAIRS,
    ROAD_SLOPE,
    POTHOLES,
    SAND,
    GRAVEL;

    public static String normalize(String raw) {
        if (raw == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_TYPE_INVALID", "Obstacle type is invalid");
        }

        String s = raw.trim().toUpperCase(Locale.ROOT);

        return switch (s) {
            case "CURB" -> CURB.name();
            case "STAIRS" -> STAIRS.name();
            case "ROAD_SLOPE" -> ROAD_SLOPE.name();
            case "POTHOLES" -> POTHOLES.name();
            case "SAND" -> SAND.name();
            case "GRAVEL" -> GRAVEL.name();
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_TYPE_INVALID", "Obstacle type is invalid");
        };
    }

    public static List<String> normalizeMany(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_LIST_EMPTY", "Obstacle list is empty");
        }

        Set<String> out = new LinkedHashSet<>();
        for (String raw : rawValues) {
            out.add(normalize(raw));
        }
        return new ArrayList<>(out);
    }

    public static List<String> allNames() {
        List<String> out = new ArrayList<>();
        for (ObstacleType value : values()) {
            out.add(value.name());
        }
        return out;
    }
}